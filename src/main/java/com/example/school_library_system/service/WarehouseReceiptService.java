package com.example.school_library_system.service;

import com.example.school_library_system.dto.WarehouseReceiptDetailDto;
import com.example.school_library_system.dto.WarehouseReceiptDto;
import com.example.school_library_system.entity.Book;
import com.example.school_library_system.entity.WarehouseReceipt;
import com.example.school_library_system.entity.WarehouseReceiptDetail;
import com.example.school_library_system.repository.BookRepository;
import com.example.school_library_system.repository.WarehouseReceiptDetailRepository;
import com.example.school_library_system.repository.WarehouseReceiptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class WarehouseReceiptService {

    @Autowired
    private WarehouseReceiptRepository repository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private WarehouseReceiptDetailRepository detailRepository;

    @Transactional
    public WarehouseReceipt saveReceipt(WarehouseReceiptDto dto) {
        WarehouseReceipt receipt = new WarehouseReceipt();
        receipt.setImportDate(dto.getImportDate());
        receipt.setDeclaredTotalQuantity(dto.getDeclaredTotalQuantity());
        receipt.setDeclaredTotalPrice(dto.getDeclaredTotalPrice());

        int actualQuantity = 0;
        BigDecimal actualPrice = BigDecimal.ZERO;
        List<WarehouseReceiptDetail> details = new ArrayList<>();

        if (dto.getDetails() != null) {
            for (WarehouseReceiptDetailDto dDto : dto.getDetails()) {
                // Validate: mỗi dòng chi tiết phải gắn với một đầu sách cụ thể
                if (dDto.getBookId() == null) {
                    throw new IllegalArgumentException(
                        "Mỗi dòng chi tiết phiếu phải chọn đầu sách từ hệ thống. " +
                        "Vui lòng chọn sách cho dòng: '" + dDto.getBookTitle() + "'"
                    );
                }

                Integer bookId = Objects.requireNonNull(dDto.getBookId());
                Book book = bookRepository.findById(bookId)
                        .orElseThrow(() -> new RuntimeException(
                            "Không tìm thấy sách ID=" + bookId
                        ));

                WarehouseReceiptDetail detail = new WarehouseReceiptDetail();
                detail.setBook(book);                          // FK liên kết
                detail.setBookTitle(book.getTitle());          // Lưu tên tại thời điểm nhập kho
                detail.setAuthor(book.getAuthor());
                detail.setQuantity(dDto.getQuantity());
                detail.setPrice(dDto.getPrice());

                BigDecimal itemTotal = dDto.getPrice().multiply(BigDecimal.valueOf(dDto.getQuantity()));
                detail.setTotalPrice(itemTotal);

                actualQuantity += dDto.getQuantity();
                actualPrice = actualPrice.add(itemTotal);

                details.add(detail);
            }
        }

        receipt.setActualTotalQuantity(actualQuantity);
        receipt.setActualTotalPrice(actualPrice);
        receipt.setDetails(details);

        return repository.save(receipt);
    }

    public List<WarehouseReceipt> getAllReceipts() {
        return repository.findAll();
    }

    public WarehouseReceipt getReceiptById(Integer id) {
        if (id == null) return null;
        return repository.findById(id).orElse(null);
    }

    /**
     * Kiểm tra sách đã có phiếu nhập kho chưa.
     * CHỈ kiểm tra theo BookID — không fallback theo tên để tránh lấy phiếu của sách khác.
     */
    public boolean hasWarehouseReceipt(Integer bookId) {
        if (bookId == null) return false;
        return detailRepository.existsByBookBookId(bookId);
    }

    /**
     * Lấy tổng số lượng sách trong kho theo bookId.
     * CHỈ tính theo BookID — không fallback theo tên để tránh cộng số lượng của sách khác.
     */
    public int getTotalWarehouseQuantity(Integer bookId) {
        if (bookId == null) return 0;
        return detailRepository.sumQuantityByBookId(bookId);
    }
}
