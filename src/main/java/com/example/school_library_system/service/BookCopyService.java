package com.example.school_library_system.service;

import com.example.school_library_system.entity.Book;
import com.example.school_library_system.entity.BookCopy;
import com.example.school_library_system.entity.BorrowDetail;
import com.example.school_library_system.entity.BorrowRecord;
import com.example.school_library_system.repository.BookCopyRepository;
import com.example.school_library_system.repository.BookRepository;
import com.example.school_library_system.repository.BorrowDetailRepository;
import com.example.school_library_system.repository.BorrowRecordRepository;
import com.example.school_library_system.repository.WarehouseReceiptDetailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class BookCopyService {

    @Autowired
    private BookCopyRepository bookCopyRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BorrowDetailRepository borrowDetailRepository;

    @Autowired
    private BorrowRecordRepository borrowRecordRepository;

    @Autowired
    private WarehouseReceiptDetailRepository warehouseReceiptDetailRepository;

    @Autowired
    private WarehouseReceiptService warehouseReceiptService;

    public List<BookCopy> getCopiesByBookId(Integer bookId) {
        return bookCopyRepository.findByBookBookId(bookId);
    }

    public BookCopy getCopyById(Integer copyId) {
        if (copyId == null) return null;
        return bookCopyRepository.findById(copyId).orElse(null);
    }

    @Transactional
    public void saveCopy(BookCopy copy) {
        bookCopyRepository.save(Objects.requireNonNull(copy, "BookCopy must not be null"));
    }

    /** Tổng số bản sao của sách (mọi trạng thái) */
    public long countAllCopiesByBookId(Integer bookId) {
        return bookCopyRepository.countByBookBookId(bookId);
    }

    /** Đếm bản sao đang ở trạng thái Bảo trì (hỏng nặng/mất) */
    public long countDamagedCopiesByBookId(Integer bookId) {
        return bookCopyRepository.countByBookBookIdAndPhysicalStatus(bookId, "Bảo trì");
    }

    /** Kiểm tra mã vạch đã tồn tại chưa */
    public boolean isBarcodeExists(String barcode) {
        return bookCopyRepository.existsByBarcode(barcode);
    }

    /**
     * Tạo hàng loạt bản sao sách với barcode tự động sinh.
     * Format barcode: BK{bookId}-{3 số thứ tự}-{4 ký tự UUID ngẫu nhiên}
     * Ví dụ: BK12-001-A3F9
     *
     * @param bookId   ID sách cần tạo bản sao
     * @param quantity Số lượng bản sao cần tạo
     * @return Danh sách barcode của các bản sao đã tạo thành công
     */
    @Transactional
    public List<String> addBulkCopies(Integer bookId, int quantity) {
        Objects.requireNonNull(bookId, "bookId must not be null");
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sách với ID: " + bookId));

        // === GATE 1: Kiểm tra phiếu nhập kho (CHỈ theo BookID — không fallback title) ===
        boolean hasReceipt = warehouseReceiptDetailRepository.existsByBookBookId(bookId);
        if (!hasReceipt) {
            throw new IllegalStateException(
                "Sách \"" + book.getTitle() + "\" chưa có phiếu nhập kho. " +
                "Vui lòng vào Quản Lý Kho → Tạo phiếu nhập trước khi lên kệ."
            );
        }

        // === GATE 2: Kiểm tra số lượng không vượt quá số lượng trong kho ===
        int warehouseQty = warehouseReceiptService.getTotalWarehouseQuantity(bookId);
        long existingCopies = bookCopyRepository.countByBookBookId(bookId);
        int remainingSlots = (int) Math.max(0, warehouseQty - existingCopies);

        if (quantity > remainingSlots) {
            throw new IllegalStateException(
                "Vượt số lượng trong kho! " +
                "Phiếu nhập ghi " + warehouseQty + " cuốn, " +
                "đã đăng ký " + existingCopies + " bản sao. " +
                "Còn có thể thêm tối đa " + remainingSlots + " bản sao nữa."
            );
        }

        List<String> createdBarcodes = new ArrayList<>();
        for (int i = 0; i < quantity; i++) {
            // Số thứ tự = tổng bản sao hiện có + vị trí trong vòng lặp
            long currentCount = bookCopyRepository.countByBookBookId(bookId) + createdBarcodes.size() + 1;

            // Sinh barcode duy nhất — retry nếu cực hiếm UUID trùng
            String barcode;
            int attempt = 0;
            do {
                String uniqueSuffix = java.util.UUID.randomUUID().toString().substring(0, 4).toUpperCase();
                barcode = String.format("BK%d-%03d-%s", bookId, currentCount, uniqueSuffix);
                attempt++;
                if (attempt > 10) throw new IllegalStateException("Không thể sinh mã vạch duy nhất sau 10 lần thử. Vui lòng thử lại.");
            } while (bookCopyRepository.existsByBarcode(barcode));

            BookCopy copy = new BookCopy();
            copy.setBook(book);
            copy.setBarcode(barcode);
            copy.setPhysicalStatus("Sẵn sàng");
            bookCopyRepository.save(copy);
            createdBarcodes.add(barcode);
        }
        return createdBarcodes;
    }

    @Transactional
    public void deactivateCopy(Integer copyId) {
        BookCopy copy = getCopyById(copyId);
        if(copy == null) return;

        // 1. Chuyển trạng thái sách thành 'Hỏng' / 'Bảo trì'
        copy.setPhysicalStatus("Bảo trì");
        bookCopyRepository.save(copy);

        // 2. Tìm tất cả BorrowDetails của CopyID này nằm trong đơn mượn 'Đã đặt chỗ' 
        // (Đây là logic bạn yêu cầu: sửa "Đã đặt chỗ" sang "Đã hủy")
        List<BorrowDetail> reservedDetails = borrowDetailRepository.findByBookCopyCopyIdAndBorrowRecordRecordStatus(copyId, "Đã đặt chỗ");
        
        for (BorrowDetail detail : reservedDetails) {
            BorrowRecord record = detail.getBorrowRecord();
            
            // Đổi trạng thái từ "Đã đặt chỗ" sang "Đã hủy"
            record.setRecordStatus("Đã hủy");
            
            // Ghi note đỏ vào record hoặc detail. 
            // Ở đây vì hủy nguyên đơn do cuốn đó hỏng, ta ghi vào Record:
            record.setCancellationReason("Xin lỗi, cuốn sách này vừa phát hiện bị hỏng vật lý nên không thể cho mượn");
            
            // Lưu lại đơn
            borrowRecordRepository.save(record);

            // NOTE: Email notification is currently simulated via console output
            System.out.println("====== SYSTEM NOTIFICATION ======");
            System.out.println("Email sent to: " + record.getUser().getEmail());
            System.out.println("Subject: Lệnh mượn sách đã bị hủy!");
            System.out.println("Body: Chào " + record.getUser().getFullName() + ", lệnh mượn mã # " + record.getBorrowId() + " vừa bị hủy. Lý do: Xin lỗi, cuốn sách này vừa phát hiện bị hỏng vật lý nên không thể cho mượn.");
            System.out.println("=================================");
        }
    }

    @Transactional
    public void deleteCopy(Integer copyId) {
        if (copyId == null) return;
        bookCopyRepository.deleteById(copyId);
    }
}
