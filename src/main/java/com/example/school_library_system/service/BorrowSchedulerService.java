package com.example.school_library_system.service;

import com.example.school_library_system.entity.BorrowRecord;
import com.example.school_library_system.entity.BorrowDetail;
import com.example.school_library_system.repository.BookCopyRepository;
import com.example.school_library_system.repository.BorrowRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BorrowSchedulerService {

    @Autowired
    private BorrowRecordRepository borrowRecordRepository;

    @Autowired
    private BookCopyRepository bookCopyRepository;

    @Autowired
    private EmailService emailService;

    // Chay vao luc 8h sang moi ngay
    @Scheduled(cron = "0 0 8 * * ?")
    public void autoSendReminderEmail() {
        System.out.println("--- BAT DAU CHAY CRONJOB: KIEM TRA HAN TRA SACH ---");
        
        List<BorrowRecord> activeBorrows = borrowRecordRepository.findByRecordStatus("Dang muon");
        LocalDateTime now = LocalDateTime.now();

        for (BorrowRecord record : activeBorrows) {
            // Bỏ qua phiếu chưa có hạn trả ("Đã đặt chỗ" chưa xác nhận)
            if (record.getDueDate() == null) continue;
            if (record.getIsReminded() == null || !record.getIsReminded()) {
                if (record.getDueDate().isBefore(now.plusDays(1))) {
                    try {
                        String email = record.getUser().getEmail();
                        if (email != null && !email.isEmpty()) {
                            String subject = "[THONG BAO TU DONG] SAP DEN HAN TRA SACH THU VIEN - Phieu #" + record.getBorrowId();
                            String body = "Chao " + record.getUser().getFullName() + ",\n\n"
                                    + "He thong tu dong thong bao phieu muon #" + record.getBorrowId() + " cua ban chi con chua toi 1 ngay la den han tra.\n"
                                    + "Vui long mang gui tra sach truoc ngay: " + record.getDueDate() + ".\n\n"
                                    + "Ghi chu: Day la ra soat tu dong cua he thong luc 8h sang.\n"
                                    + "Tran trong.";
                            
                            emailService.sendTextEmail(email, subject, body);
                            System.out.println("Da tu dong gui Email bao sap het han cho User: " + email);
                            
                            record.setIsReminded(true);
                            borrowRecordRepository.save(record);
                        }
                    } catch (Exception e) {
                        System.err.println("Loi gui auto mail cho phieu " + record.getBorrowId() + ": " + e.getMessage());
                    }
                }
            }
        }
        System.out.println("--- KET THUC CHAY CRONJOB ---");
    }

    // Chay moi gio de kiem tra va huy cac don muon dat cho qua 24h
    @Scheduled(fixedRate = 3600000)
    public void autoCancelExpiredReservations() {
        System.out.println("--- BAT DAU CHAY CRONJOB: HUY DON DAT CHO QUA 24H ---");
        List<BorrowRecord> pendingBorrows = borrowRecordRepository.findByRecordStatus("\u0110\u00e3 \u0111\u1eb7t ch\u1ed7");
        LocalDateTime now = LocalDateTime.now();

        for (BorrowRecord record : pendingBorrows) {
            // Nếu borrowDate null = đặt chỗ nhưng chưa có mốc đặt chỗ — bỏ qua, admin xử lý thủ công
            if (record.getBorrowDate() == null) continue;
            if (record.getBorrowDate().plusHours(24).isBefore(now)) {
                
                if (record.getDetails() != null) {
                    for (BorrowDetail detail : record.getDetails()) {
                        var bookCopy = detail.getBookCopy();
                        if (bookCopy != null) {
                            bookCopy.setPhysicalStatus("Sẵn sàng");
                            bookCopyRepository.save(bookCopy);
                        }
                    }
                }
                
                record.setRecordStatus("\u0110\u00e3 h\u1ee7y");
                borrowRecordRepository.save(record);
                
                System.out.println("Da tu dong huy don dat cho #" + record.getBorrowId() + " do qua han 24h khong lay sach.");
            }
        }
        System.out.println("--- KET THUC CRONJOB HUY DON DAT CHO ---");
    }
}
