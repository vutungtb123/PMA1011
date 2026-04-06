package com.example.school_library_system.service;

import com.example.school_library_system.dto.DirectBorrowDto;
import com.example.school_library_system.entity.Book;
import com.example.school_library_system.entity.BookCopy;
import com.example.school_library_system.entity.BorrowDetail;
import com.example.school_library_system.entity.BorrowRecord;
import com.example.school_library_system.entity.User;
import com.example.school_library_system.entity.WaitlistRecord;
import com.example.school_library_system.repository.BookRepository;
import com.example.school_library_system.repository.BookCopyRepository;
import com.example.school_library_system.repository.BorrowRecordRepository;
import com.example.school_library_system.repository.UserRepository;
import com.example.school_library_system.repository.WaitlistRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class BorrowService {

    @Autowired
    private BorrowRecordRepository borrowRecordRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookCopyRepository bookCopyRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private WaitlistRecordRepository waitlistRecordRepository;

    @Autowired
    private EmailService emailService;

    public List<BorrowRecord> getAllBorrowRecords() {
        return borrowRecordRepository.findAll();
    }

    public List<BorrowRecord> getPendingBorrows() {
        return borrowRecordRepository.findByRecordStatus("Đã đặt chỗ");
    }

    public List<BorrowRecord> getActiveBorrows() {
        return borrowRecordRepository.findByRecordStatus("Đang mượn");
    }

    @Transactional
    public BorrowRecord createDirectBorrowRecord(DirectBorrowDto dto) throws Exception {
        // 1. Tìm người dùng (User)
        User user = userRepository.findById(dto.getReaderId())
                .orElseThrow(() -> new Exception("Không tìm thấy Mã Người Dùng #" + dto.getReaderId()));

        // 2. Chuẩn bị Phiếu mượn
        BorrowRecord record = new BorrowRecord();
        record.setUser(user);
        record.setBorrowDate(LocalDateTime.now());
        record.setDueDate(LocalDateTime.now().plusDays(dto.getBorrowDays()));
        record.setRecordStatus("Đang mượn");
        List<BorrowDetail> details = new ArrayList<>();

        // 3. Quét từng BookID được chọn
        if (dto.getBookIds() == null || dto.getBookIds().isEmpty()) {
            throw new Exception("Chưa chọn sách nào để mượn!");
        }

        for (Integer bookId : dto.getBookIds()) {
            List<BookCopy> copies = bookCopyRepository.findByBookBookId(bookId);
            
            // Tìm 1 cuốn đang 'Sẵn sàng'
            BookCopy availableCopy = copies.stream()
                    .filter(c -> "Sẵn sàng".equals(c.getPhysicalStatus()))
                    .findFirst()
                    .orElse(null);

            if (availableCopy == null) {
                throw new Exception("Sách có ID #" + bookId + " hiện đã hết bản sao sẵn sàng trên kệ!");
            }

            // Đổi trạng thái cuốn sách thành Đang mượn
            availableCopy.setPhysicalStatus("Đang mượn");
            bookCopyRepository.save(availableCopy);

            // Gắn vào Phiếu mượn
            BorrowDetail detail = new BorrowDetail();
            detail.setBookCopy(availableCopy);
            detail.setBorrowRecord(record);
            
            details.add(detail);
        }

        record.setDetails(details);

        // Lưu toàn bộ (cascade)
        return borrowRecordRepository.save(record);
    }

    @Transactional
    public void confirmBorrowRequest(Integer borrowId, Integer borrowDays) throws Exception {
        BorrowRecord record = borrowRecordRepository.findById(borrowId)
                .orElseThrow(() -> new Exception("Đơn #" + borrowId + " không tồn tại."));

        if (!"Đã đặt chỗ".equals(record.getRecordStatus())) {
            throw new Exception("Đơn này không ở trạng thái Chờ duyệt.");
        }

        int days = (borrowDays != null && borrowDays > 0) ? borrowDays : 7;

        // Set thời gian thực tế khi giao sách
        record.setRecordStatus("Đang mượn");
        record.setBorrowDate(LocalDateTime.now());
        record.setDueDate(LocalDateTime.now().plusDays(days));
        
        borrowRecordRepository.save(record);
    }

    @Transactional
    public void sendManualReminder(Integer borrowId) throws Exception {
        BorrowRecord record = borrowRecordRepository.findById(borrowId)
                .orElseThrow(() -> new Exception("Không tìm thấy Phiếu #" + borrowId));

        if (!"Đang mượn".equals(record.getRecordStatus())) {
            throw new Exception("Phiếu này đã thanh lý hoặc hủy, không cần nhắc nợ.");
        }

        User user = record.getUser();
        if (user.getEmail() != null) {
            String subject = "[THƯ VIỆN] NHẮC NHỞ TRẢ SÁCH SẮP HẾT HẠN - Phiếu #" + record.getBorrowId();
            String body = "Chào " + user.getFullName() + ",\n\n"
                        + "Thư viện xin thông báo phiếu mượn #" + record.getBorrowId() + " của bạn sắp đến hạn trả.\n"
                        + "Bạn có hạn trả đến ngày: " + record.getDueDate() + ".\n"
                        + "Xin vui lòng mang sách đến quầy thủ thư để hoàn tất thủ tục trả, tránh bị phạt.\n\n"
                        + "Trân trọng.";
            emailService.sendTextEmail(user.getEmail(), subject, body);
            
            record.setIsReminded(true);
            borrowRecordRepository.save(record);
        } else {
            throw new Exception("Người dùng này không đăng ký Email, vui lòng gọi điện.");
        }
    }

    // ========== WAITLIST LOGIC (QUEUE) ==========
    
    @Transactional
    public WaitlistRecord registerWaitlist(Integer userId, Integer bookId) throws Exception {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception("Không tìm thấy tài khoản người dùng!"));

        // Đã mượn hoặc đã đặt chỗ rồi thì không cho đợi
        long alreadyBorrowed = borrowRecordRepository.countActiveByReaderIdAndBookId(userId, bookId);
        if (alreadyBorrowed > 0) {
            throw new Exception("Quý khách đang mượn cuốn sách này rồi.");
        }

        // Đã xếp hàng rồi thì không xếp hàng thêm
        long alreadyWaiting = waitlistRecordRepository.countByUserUserIdAndBookBookIdAndStatus(userId, bookId, "Đang chờ");
        if (alreadyWaiting > 0) {
            throw new Exception("Quý khách đã đăng ký chờ cuốn sách này rồi.");
        }

        // Kiểm tra đúng là sách hết bản sao sẵn sàng
        long available = bookCopyRepository.countByBookBookIdAndPhysicalStatus(bookId, "Sẵn sàng");
        if (available > 0) {
            throw new Exception("Sách hiện đang có sẵn trên kệ, bạn có thể trở về trang chi tiết và mượn ngay.");
        }

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new Exception("Không tìm thấy đầu sách!"));

        WaitlistRecord wl = new WaitlistRecord();
        wl.setUser(user);
        wl.setBook(book);
        wl.setStatus("Đang chờ");
        return waitlistRecordRepository.save(wl);
    }
    
    @Transactional
    public void processWaitlistForBook(Book book, BookCopy availableCopy) {
        // Lấy 1 tài khoản đăng ký sớm nhất
        List<WaitlistRecord> waitlist = waitlistRecordRepository.findByBookBookIdAndStatusOrderByRegisterDateAsc(book.getBookId(), "Đang chờ");
        if (waitlist.isEmpty()) {
            return;
        }

        WaitlistRecord nextInLine = waitlist.get(0);

        try {
            // Chuyển sách về đang mượn / đặt chỗ
            availableCopy.setPhysicalStatus("Đang mượn");
            bookCopyRepository.save(availableCopy);

            // Tạo BorrowRecord (Đã đặt chỗ) cho người này
            BorrowRecord record = new BorrowRecord();
            record.setUser(nextInLine.getUser());
            record.setRecordStatus("Đã đặt chỗ");
            
            BorrowDetail detail = new BorrowDetail();
            detail.setBookCopy(availableCopy);
            detail.setBorrowRecord(record);
            record.getDetails().add(detail);
            
            borrowRecordRepository.save(record);

            // Cập nhật Waitlist queue status
            nextInLine.setStatus("Đã hoàn tất");
            waitlistRecordRepository.save(nextInLine);

            // Tự động Gửi email báo hỷ
            if (nextInLine.getUser().getEmail() != null) {
                String subject = "[THƯ VIỆN] SÁCH BẠN YÊU CẦU ĐÃ CÓ - " + book.getTitle();
                String body = "Chào " + nextInLine.getUser().getFullName() + ",\n\n"
                        + "Thư viện xin trân trọng thông báo cuốn sách: '" + book.getTitle() + "' mà bạn "
                        + "quan tâm nay đã CÓ SẴN do một người đọc khác vừa mới gửi trả thư viện.\n\n"
                        + "Theo thứ tự ưu tiên, hệ thống đã TỰ ĐỘNG lấy cuốn sách này và lên đơn mượn ưu tiên cho tên của bạn.\n\n"
                        + "Vui lòng vào trang 'Sách của tôi' trên web để xem vị trí đơn, và ghé thư viện nhận sách trong vòng 24 giờ tới nếu không sách sẽ bị thu hồi chuyển cho người đứng hàng sau.\n\n"
                        + "Trân trọng.";
                emailService.sendTextEmail(nextInLine.getUser().getEmail(), subject, body);
            }
        } catch (Exception e) {
            System.err.println("Lỗi Auto-Assign cho sách " + book.getBookId() + ": " + e.getMessage());
        }
    }

    /**
     * Hàm trả sách dùng cho backend admin call (hoặc cron job giả lập)
     */
    @Transactional
    public void returnBook(Integer borrowId) throws Exception {
        BorrowRecord record = borrowRecordRepository.findById(borrowId)
                .orElseThrow(() -> new Exception("Không tìm thấy đơn ID #" + borrowId));

        if ("Đã trả".equals(record.getRecordStatus()) || "Đã hủy".equals(record.getRecordStatus())) {
            throw new Exception("Đơn này đã kết thúc trước đó.");
        }

        record.setRecordStatus("Đã trả");
        borrowRecordRepository.save(record);

        // Đưa sách về kệ
        if (record.getDetails() != null) {
            for (BorrowDetail detail : record.getDetails()) {
                if (detail.getBookCopy() != null) {
                    BookCopy copy = detail.getBookCopy();
                    copy.setPhysicalStatus("Sẵn sàng");
                    bookCopyRepository.save(copy);
                    
                    // Call AUTO-ASSIGN (Kiểm tra hàng chờ)
                    processWaitlistForBook(copy.getBook(), copy);
                }
            }
        }
    }

    // ========== USER SELF-SERVICE BORROW ==========

    private static final int MAX_ACTIVE_BORROWS = 5;

    /**
     * Mượn sách từ phía người dùng (self-service)
     */
    @Transactional
    public BorrowRecord borrowBookForReader(Integer userId, Integer bookId) throws Exception {
        // 1. Kiểm tra giới hạn tối đa 5 cuốn
        long activeBorrows = borrowRecordRepository.countActiveByReaderId(userId);
        if (activeBorrows >= MAX_ACTIVE_BORROWS) {
            throw new Exception("Bạn đã mượn tối đa " + MAX_ACTIVE_BORROWS + " cuốn sách. Vui lòng trả sách trước khi mượn thêm.");
        }

        // 2. Kiểm tra đã mượn đầu sách này chưa
        long alreadyBorrowed = borrowRecordRepository.countActiveByReaderIdAndBookId(userId, bookId);
        if (alreadyBorrowed > 0) {
            throw new Exception("Bạn đã mượn cuốn sách này rồi!");
        }

        // 3. Tìm 1 bản sao sẵn sàng
        List<BookCopy> copies = bookCopyRepository.findByBookBookId(bookId);
        BookCopy availableCopy = copies.stream()
                .filter(c -> "Sẵn sàng".equals(c.getPhysicalStatus()))
                .findFirst()
                .orElseThrow(() -> new Exception("Cuốn sách này hiện không còn bản nào sẵn sàng!"));

        // 4. Đổi trạng thái cuốn sách
        availableCopy.setPhysicalStatus("Đang mượn");
        bookCopyRepository.save(availableCopy);

        // 5. Tạo phiếu mượn — chỉ đặt chỗ, chưa set thời gian
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception("Không tìm thấy tài khoản!"));

        BorrowRecord record = new BorrowRecord();
        record.setUser(user);
        // borrowDate và dueDate để null cho đến khi admin xác nhận giao sách
        record.setRecordStatus("Đã đặt chỗ");

        BorrowDetail detail = new BorrowDetail();
        detail.setBookCopy(availableCopy);
        detail.setBorrowRecord(record);
        record.getDetails().add(detail);

        return borrowRecordRepository.save(record);
    }

    /**
     * Kiểm tra user đã mượn 1 đầu sách hay chưa
     */
    public boolean hasReaderBorrowedBook(Integer userId, Integer bookId) {
        return borrowRecordRepository.countActiveByReaderIdAndBookId(userId, bookId) > 0;
    }

    /**
     * Đếm số lượt mượn đang hoạt động
     */
    public long countActiveBorrows(Integer userId) {
        return borrowRecordRepository.countActiveByReaderId(userId);
    }

    /**
     * Lấy toàn bộ phiếu mượn của user, sắp xếp giảm dần theo ngày
     */
    public List<BorrowRecord> getReaderBorrows(Integer userId) {
        return borrowRecordRepository.findByUserUserIdOrderByBorrowIdDesc(userId);
    }
}
