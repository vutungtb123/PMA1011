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
import com.example.school_library_system.repository.BorrowDetailRepository;
import com.example.school_library_system.repository.BorrowRecordRepository;
import com.example.school_library_system.repository.UserRepository;
import com.example.school_library_system.repository.WaitlistRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
    private BorrowDetailRepository borrowDetailRepository;

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
        Integer readerId = Objects.requireNonNull(dto.getReaderId(), "readerId must not be null");
        User user = userRepository.findById(readerId)
                .orElseThrow(() -> new Exception("Không tìm thấy Mã Người Dùng #" + readerId));

        // 2. Chuẩn bị Phiếu mượn
        BorrowRecord record = new BorrowRecord();
        record.setUser(user);
        record.setBorrowDate(LocalDateTime.now());
        if (dto.getBorrowMinutes() != null && dto.getBorrowMinutes() > 0) {
            record.setDueDate(LocalDateTime.now().plusMinutes(dto.getBorrowMinutes()));
        } else {
            int days = (dto.getBorrowDays() != null && dto.getBorrowDays() > 0) ? dto.getBorrowDays() : 7;
            record.setDueDate(LocalDateTime.now().plusDays(days));
        }
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
    public void confirmBorrowRequest(Integer borrowId, Integer borrowDays, Integer borrowMinutes) throws Exception {
        Objects.requireNonNull(borrowId, "borrowId must not be null");
        BorrowRecord record = borrowRecordRepository.findById(borrowId)
                .orElseThrow(() -> new Exception("Đơn #" + borrowId + " không tồn tại."));

        if (!"Đã đặt chỗ".equals(record.getRecordStatus())) {
            throw new Exception("Đơn này không ở trạng thái Chờ duyệt.");
        }

        record.setRecordStatus("Đang mượn");
        record.setBorrowDate(LocalDateTime.now());

        // Nếu nhập số phút (test mode) → dùng phút; ngược lại dùng ngày
        if (borrowMinutes != null && borrowMinutes > 0) {
            record.setDueDate(LocalDateTime.now().plusMinutes(borrowMinutes));
        } else {
            int days = (borrowDays != null && borrowDays > 0) ? borrowDays : 7;
            record.setDueDate(LocalDateTime.now().plusDays(days));
        }

        borrowRecordRepository.save(record);
    }

    @Transactional
    public void sendManualReminder(Integer borrowId) throws Exception {
        Objects.requireNonNull(borrowId, "borrowId must not be null");
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
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(bookId, "bookId must not be null");
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

        Book book = bookRepository.findById(Objects.requireNonNull(bookId))
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
     * Hàm trả sách dùng cho backend admin call.
     * damagePercentage: 0=không hỏng, 20/50/70=hỏng nhẹ (vẫn mượn được), 100/200=hỏng nặng/mất (→Bảo trì).
     */
    @Transactional
    public void returnBook(Integer borrowId, boolean lateReturn, boolean damagedBook,
                           String violationNote, BigDecimal lateFine, int damagePercentage) throws Exception {
        Objects.requireNonNull(borrowId, "borrowId must not be null");
        BorrowRecord record = borrowRecordRepository.findById(borrowId)
                .orElseThrow(() -> new Exception("Không tìm thấy đơn ID #" + borrowId));

        if ("Đã trả".equals(record.getRecordStatus()) || "Đã hủy".equals(record.getRecordStatus())) {
            throw new Exception("Đơn này đã kết thúc trước đó.");
        }

        record.setRecordStatus("Đã trả");
        borrowRecordRepository.save(record);

        // Xây dựng trạng thái chi tiết trả sách
        String physicalState;
        if (damagedBook) {
            physicalState = (damagePercentage >= 100) ? "Hư hỏng nặng" : "Hư hỏng nhẹ";
        } else if (lateReturn) {
            physicalState = "Trả muộn";
        } else {
            physicalState = "Nguyên vẹn";
        }

        // Xây dựng ghi chú vi phạm tổng hợp
        StringBuilder noteBuilder = new StringBuilder();
        if (lateReturn) noteBuilder.append("Trả muộn hạn. ");
        if (damagedBook) {
            if (damagePercentage >= 100) {
                noteBuilder.append("Sách bị hư hỏng nặng / mất (" + damagePercentage + "%). ");
            } else {
                noteBuilder.append("Sách bị hư hỏng nhẹ (" + damagePercentage + "%). ");
            }
        }
        if (violationNote != null && !violationNote.trim().isEmpty()) {
            noteBuilder.append("Ghi chú: ").append(violationNote.trim());
        }
        String finalNote = noteBuilder.toString().trim();

        // Đưa sách về kệ + ghi nhận trạng thái trả
        if (record.getDetails() != null) {
            for (BorrowDetail detail : record.getDetails()) {
                // Ghi nhận ngày trả và tình trạng
                detail.setReturnDate(LocalDateTime.now());
                detail.setReturnPhysicalState(physicalState);
                if (!finalNote.isEmpty()) {
                    detail.setViolationNote(finalNote);
                }
                // Lưu tiền phạt (phạt trễ + phạt hỏng đã cộng từ controller)
                if ((lateReturn || damagedBook) && lateFine != null && lateFine.compareTo(BigDecimal.ZERO) > 0) {
                    detail.setAssessedFine(lateFine);
                }

                if (detail.getBookCopy() != null) {
                    BookCopy copy = detail.getBookCopy();
                    if (damagedBook && damagePercentage >= 100) {
                        // Hỏng nặng hoặc mất sách → Bảo trì, KHÔNG thể mượn nữa
                        copy.setPhysicalStatus("Bảo trì");
                    } else {
                        // Hỏng nhẹ (20/50/70%) hoặc nguyên vẹn → vẫn Sẵn sàng cho mượn
                        copy.setPhysicalStatus("Sẵn sàng");
                    }
                    bookCopyRepository.save(copy);

                    // AUTO-ASSIGN hàng chờ — chỉ khi sách thực sự sẵn sàng
                    if ("Sẵn sàng".equals(copy.getPhysicalStatus())) {
                        processWaitlistForBook(copy.getBook(), copy);
                    }
                }
            }
        }
    }

    /**
     * Lấy tất cả BorrowDetail có vi phạm
     */
    public List<BorrowDetail> getAllViolations() {
        return borrowDetailRepository.findAllWithViolations();
    }

    // ========== USER SELF-SERVICE BORROW ==========

    private static final int MAX_ACTIVE_BORROWS = 5;

    /**
     * Mượn sách từ phía người dùng (self-service).
     *
     * Logic gộp phiếu: nếu user đã có phiếu "Đã đặt chỗ" chưa được admin xác nhận
     * (borrowDate == null), sách mới sẽ được THÊM VÀO phiếu đó thay vì tạo phiếu mới.
     * Điều này giúp admin chỉ thấy 1 hàng cho user trên màn hình quản lý.
     */
    @Transactional
    public BorrowRecord borrowBookForReader(Integer userId, Integer bookId) throws Exception {
        // 0. Chặn nếu user còn vi phạm chưa nộp phạt
        if (borrowDetailRepository.hasUnpaidViolation(userId)) {
            throw new Exception("Tài khoản của bạn đang có khoản phạt chưa thanh toán. Vui lòng đến quầy thủ thư để nộp phạt trước khi mượn sách.");
        }

        // 1. Kiểm tra giới hạn tối đa 5 cuốn
        long activeBorrows = borrowRecordRepository.countActiveBooksByReaderId(userId);
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

        // 4. Đổi trạng thái bản sao → Đang mượn (giữ chỗ)
        availableCopy.setPhysicalStatus("Đang mượn");
        bookCopyRepository.save(availableCopy);

        // 5. Tạo BorrowDetail mới cho cuốn này
        BorrowDetail newDetail = new BorrowDetail();
        newDetail.setBookCopy(availableCopy);

        // 6. Kiểm tra xem user đã có phiếu "Đã đặt chỗ" chưa xác nhận chưa?
        List<BorrowRecord> pendingRecords = borrowRecordRepository.findPendingByUserId(userId);

        if (!pendingRecords.isEmpty()) {
            // ── Có phiếu cũ → GỘP vào phiếu đó ──
            BorrowRecord existingRecord = pendingRecords.get(0);
            
            // Cấp độ bảo vệ 2: Chặn triệt để việc có 2 quyển của cùng một đầu sách (bookId)
            // nằm trong cùng một phiếu chờ, quét ngay trên bộ nhớ để bỏ qua giới hạn delay DB
            boolean isAlreadyInPending = existingRecord.getDetails().stream()
                    .anyMatch(d -> d.getBookCopy().getBook().getBookId().equals(bookId));
            if (isAlreadyInPending) {
                // Hoàn trả sách khả dụng về kệ do đã có lỗi phi logic
                availableCopy.setPhysicalStatus("Sẵn sàng");
                bookCopyRepository.save(availableCopy);
                throw new Exception("Hệ thống phát hiện quyển sách này đã lọt vào phiếu đặt chỗ của bạn rồi!");
            }
            
            newDetail.setBorrowRecord(existingRecord);
            existingRecord.getDetails().add(newDetail);
            return borrowRecordRepository.save(existingRecord);

        } else {
            // ── Chưa có phiếu nào → tạo phiếu mới ──
            User user = userRepository.findById(Objects.requireNonNull(userId, "userId must not be null"))
                    .orElseThrow(() -> new Exception("Không tìm thấy tài khoản!"));

            BorrowRecord newRecord = new BorrowRecord();
            newRecord.setUser(user);
            // borrowDate và dueDate để null cho đến khi admin xác nhận giao sách
            newRecord.setRecordStatus("Đã đặt chỗ");
            newDetail.setBorrowRecord(newRecord);
            newRecord.getDetails().add(newDetail);

            return borrowRecordRepository.save(newRecord);
        }
    }

    /**
     * Kiểm tra user đã mượn 1 đầu sách hay chưa (cả Đã đặt chỗ lẫn Đang mượn)
     */
    public boolean hasReaderBorrowedBook(Integer userId, Integer bookId) {
        return borrowRecordRepository.countActiveByReaderIdAndBookId(userId, bookId) > 0;
    }

    /**
     * Lấy trạng thái cụ thể của user với 1 cuốn sách (null nếu chưa mượn)
     * Trả về: "Đã đặt chỗ", "Đang mượn", hoặc null
     */
    public String getActiveStatusForBook(Integer userId, Integer bookId) {
        List<String> statuses = borrowRecordRepository.findActiveStatusByReaderIdAndBookId(userId, bookId);
        return statuses.isEmpty() ? null : statuses.get(0);
    }

    /**
     * Đếm tổng số cuốn sách đang mượn hoạt động (Đang mượn / Đã đặt chỗ)
     */
    public long countActiveBorrows(Integer userId) {
        return borrowRecordRepository.countActiveBooksByReaderId(userId);
    }

    /**
     * Lấy danh sách bookId mà user đang mượn/đặt chỗ (để lock nút trên trang home)
     */
    public List<Integer> getActiveBorrowedBookIds(Integer userId) {
        return borrowRecordRepository.findActiveBorrowedBookIdsByUserId(userId);
    }

    /**
     * Lấy toàn bộ phiếu mượn của user, sắp xếp giảm dần theo ngày
     */
    public List<BorrowRecord> getReaderBorrows(Integer userId) {
        return borrowRecordRepository.findByUserUserIdOrderByBorrowIdDesc(userId);
    }

    /**
     * Kiểm tra user hiện có vi phạm chưa nộp phạt không
     */
    public boolean hasUnpaidViolation(Integer userId) {
        return borrowDetailRepository.hasUnpaidViolation(userId);
    }

    /**
     * Admin xác nhận độc giả đã nộp phạt — đánh dấu finePaid = true
     */
    @Transactional
    public void settleViolationFine(Integer detailId) throws Exception {
        Objects.requireNonNull(detailId, "detailId must not be null");
        BorrowDetail detail = borrowDetailRepository.findById(detailId)
                .orElseThrow(() -> new Exception("Không tìm thấy chi tiết phiếu #" + detailId));
        detail.setFinePaid(true);
        borrowDetailRepository.save(detail);
    }
}
