package com.example.school_library_system.controller;

import com.example.school_library_system.dto.DirectBorrowDto;
import com.example.school_library_system.entity.BookCopy;
import com.example.school_library_system.entity.User;
import com.example.school_library_system.repository.BookCopyRepository;
import com.example.school_library_system.repository.UserRepository;
import com.example.school_library_system.repository.WarehouseReceiptDetailRepository;
import com.example.school_library_system.service.BookService;
import com.example.school_library_system.service.BorrowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin/borrow")
public class AdminBorrowController {

    @Autowired
    private BorrowService borrowService;

    @Autowired
    private BookService bookService;

    @Autowired
    private WarehouseReceiptDetailRepository warehouseDetailRepo;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookCopyRepository bookCopyRepository;

    @GetMapping({"", "/"})
    public String listBorrows(Model model) {
        var activeBorrows = borrowService.getAllBorrowRecords().stream()
                .filter(r -> "Đã đặt chỗ".equals(r.getRecordStatus()) || "Đang mượn".equals(r.getRecordStatus()))
                .toList();
        model.addAttribute("borrows", activeBorrows);
        model.addAttribute("now", LocalDateTime.now());

        // Build map: bookId -> giá nhập kho (dùng cho tính phạt hư hỏng phía frontend)
        // Ưu tiên tìm theo BookID; nếu null (dữ liệu kho cũ không liên kết BookID) thì fallback theo tên sách
        Map<Integer, BigDecimal> bookPriceMap = new HashMap<>();
        activeBorrows.forEach(br -> br.getDetails().forEach(det -> {
            Integer bookId = det.getBookCopy().getBook().getBookId();
            if (!bookPriceMap.containsKey(bookId)) {
                BigDecimal price = warehouseDetailRepo.findLatestPriceByBookId(bookId);
                if (price == null || price.compareTo(BigDecimal.ZERO) == 0) {
                    // Fallback: tìm theo tên sách (dữ liệu kho cũ có BookID = NULL)
                    String bookTitle = det.getBookCopy().getBook().getTitle();
                    price = warehouseDetailRepo.findLatestPriceByBookTitle(bookTitle);
                }
                bookPriceMap.put(bookId, price != null ? price : BigDecimal.ZERO);
            }
        }));
        model.addAttribute("bookPriceMap", bookPriceMap);
        return "admin/borrow/list";
    }

    @GetMapping("/history")
    public String borrowHistory(Model model) {
        // Chỉ hiển thị phiếu đã trả hoặc đã hủy
        var historyBorrows = borrowService.getAllBorrowRecords().stream()
                .filter(r -> "Đã trả".equals(r.getRecordStatus()) || "Đã hủy".equals(r.getRecordStatus()))
                .toList();
        model.addAttribute("borrows", historyBorrows);
        return "admin/borrow/history";
    }

    @GetMapping("/new")
    public String showNewBorrowForm(Model model) {
        model.addAttribute("borrowDto", new DirectBorrowDto());
        model.addAttribute("books", bookService.getAllBooks());
        // Pass reserved borrows so the scan page can preload book checklists
        var reservedBorrows = borrowService.getAllBorrowRecords().stream()
                .filter(r -> "Đã đặt chỗ".equals(r.getRecordStatus()))
                .toList();
        model.addAttribute("borrows", reservedBorrows);
        return "admin/borrow/scan";
    }

    // ══════════ QR SCAN API (JSON) ══════════

    /**
     * Tra cứu thông tin độc giả từ dữ liệu QR thẻ thư viện.
     * QR format: LIB-USR-{userId}-{studentId}
     */
    @GetMapping("/api/lookup-card")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> lookupCard(@RequestParam String qr) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (qr == null || qr.isBlank()) {
                result.put("found", false);
                result.put("error", "Dữ liệu QR rỗng");
                return ResponseEntity.ok(result);
            }
            User user = userRepository.findByCardQrData(qr.trim()).orElse(null);
            if (user == null) {
                result.put("found", false);
                result.put("error", "Không tìm thấy thẻ thư viện cho QR: " + qr);
                return ResponseEntity.ok(result);
            }
            result.put("found", true);
            result.put("userId", user.getUserId());
            result.put("fullName", user.getFullName());
            result.put("email", user.getEmail());
            result.put("studentId", user.getStudentId());
            result.put("photoUrl", user.getPhotoUrl());
            result.put("isActive", user.getIsActive());
            result.put("groupName", user.getReaderGroup() != null ? user.getReaderGroup().getGroupName() : null);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("found", false);
            result.put("error", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    /**
     * Tra cứu thông tin bản sao sách từ barcode (dùng khi quét QR sách).
     */
    @GetMapping("/api/lookup-book")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> lookupBook(@RequestParam String barcode) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (barcode == null || barcode.isBlank()) {
                result.put("found", false);
                result.put("error", "Barcode rỗng");
                return ResponseEntity.ok(result);
            }
            BookCopy copy = bookCopyRepository.findByBarcode(barcode.trim()).orElse(null);
            if (copy == null) {
                result.put("found", false);
                result.put("error", "Không tìm thấy sách với mã: " + barcode);
                return ResponseEntity.ok(result);
            }
            result.put("found", true);
            result.put("copyId", copy.getCopyId());
            result.put("barcode", copy.getBarcode());
            result.put("bookId", copy.getBook().getBookId());
            result.put("title", copy.getBook().getTitle());
            result.put("author", copy.getBook().getAuthor());
            result.put("physicalStatus", copy.getPhysicalStatus());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("found", false);
            result.put("error", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.ok(result);
        }
    }    /**
     * Tra cứu phiếu "Đã đặt chỗ" của user (nếu có) — trả về danh sách sách cần giao.
     */
    @GetMapping("/api/reserved-borrow")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getReservedBorrow(@RequestParam Integer userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            var reserved = borrowService.getAllBorrowRecords().stream()
                    .filter(r -> "Đã đặt chỗ".equals(r.getRecordStatus())
                            && r.getUser() != null
                            && r.getUser().getUserId().equals(userId))
                    .findFirst().orElse(null);

            if (reserved == null) {
                result.put("found", false);
                return ResponseEntity.ok(result);
            }

            result.put("found", true);
            result.put("borrowId", reserved.getBorrowId());
            result.put("status", reserved.getRecordStatus());

            var books = new java.util.ArrayList<Map<String, Object>>();
            if (reserved.getDetails() != null) {
                for (var det : reserved.getDetails()) {
                    Map<String, Object> b = new HashMap<>();
                    b.put("copyId",  det.getBookCopy().getCopyId());
                    b.put("bookId",  det.getBookCopy().getBook().getBookId());
                    b.put("title",   det.getBookCopy().getBook().getTitle());
                    b.put("barcode", det.getBookCopy().getBarcode());
                    books.add(b);
                }
            }
            result.put("books", books);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("found", false);
            result.put("error", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.ok(result);
        }
    }


    @PostMapping("/save")
    public String saveDirectBorrow(@ModelAttribute DirectBorrowDto borrowDto, RedirectAttributes redirectAttributes) {
        try {
            borrowService.createDirectBorrowRecord(borrowDto);
            redirectAttributes.addFlashAttribute("message", "Tạo phiếu mượn trực tiếp thành công! Trạng thái sách đã chuyển sang Đang mượn.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/admin/borrow/new";
        }
        return "redirect:/admin/borrow";
    }

    /**
     * Admin xác nhận giao sách cho độc giả — chuyển từ "Đã đặt chỗ" → "Đang mượn"
     * borrowDays: số ngày mượn (admin có thể tùy chỉnh, mặc định 7)
     */
    @PostMapping("/{id}/confirm")
    public String confirmBorrow(@PathVariable Integer id,
                                @RequestParam(defaultValue = "7") Integer borrowDays,
                                @RequestParam(defaultValue = "0") Integer borrowMinutes,
                                RedirectAttributes redirectAttributes) {
        try {
            borrowService.confirmBorrowRequest(id, borrowDays, borrowMinutes);
            String msg;
            if (borrowMinutes != null && borrowMinutes > 0) {
                msg = "✅ Đã xác nhận giao sách cho Phiếu #" + id + ". Hạn trả: " + borrowMinutes + " phút (TEST MODE).";
            } else {
                msg = "✅ Đã xác nhận giao sách cho Phiếu #" + id + ". Hạn trả: " + borrowDays + " ngày kể từ hôm nay.";
            }
            redirectAttributes.addFlashAttribute("message", msg);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi xác nhận: " + e.getMessage());
        }
        return "redirect:/admin/borrow";
    }

    /**
     * Admin đánh dấu sách đã được trả — có kiểm tra tình trạng sách
     */
    @PostMapping("/{id}/return")
    public String returnBook(@PathVariable Integer id,
                             @RequestParam(defaultValue = "false") boolean lateReturn,
                             @RequestParam(defaultValue = "false") boolean damagedBook,
                             @RequestParam(required = false) String violationNote,
                             @RequestParam(defaultValue = "0") long lateFineAmount,
                             @RequestParam(defaultValue = "0") long damageFineAmount,
                             @RequestParam(defaultValue = "0") int damagePercentage,
                             RedirectAttributes redirectAttributes) {
        try {
            long totalFine = lateFineAmount + damageFineAmount;
            borrowService.returnBook(id, lateReturn, damagedBook, violationNote, BigDecimal.valueOf(totalFine), damagePercentage);
            String statusMsg = "📚 Phiếu #" + id + " đã được đánh dấu Đã Trả.";
            if (lateReturn || damagedBook) {
                statusMsg += " ⚠️ Vi phạm đã được ghi nhận:";
                if (lateReturn)   statusMsg += " [Trả muộn - Phạt: " + String.format("%,.0f", (double) lateFineAmount) + "đ]";
                if (damagedBook)  statusMsg += " [Sách hư hỏng - Phạt: " + String.format("%,.0f", (double) damageFineAmount) + "đ]";
            }
            redirectAttributes.addFlashAttribute("message", statusMsg);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/borrow";
    }

    /**
     * Trang quản lý danh sách vi phạm
     */
    @GetMapping("/violations")
    public String violationsList(Model model) {
        var allViolations = borrowService.getAllViolations();
        // Tab "Đang nợ": có tiền phạt và chưa được xác nhận nộp phạt
        var owing = allViolations.stream()
                .filter(d -> d.getAssessedFine() != null
                        && d.getAssessedFine().compareTo(BigDecimal.ZERO) > 0
                        && (d.getFinePaid() == null || !d.getFinePaid()))
                .toList();
        // Tab "Đã xử lý": đã nộp phạt hoặc không có tiền phạt (ở cảnh báo)
        var resolved = allViolations.stream()
                .filter(d -> d.getAssessedFine() == null
                        || d.getAssessedFine().compareTo(BigDecimal.ZERO) == 0
                        || Boolean.TRUE.equals(d.getFinePaid()))
                .toList();
        model.addAttribute("owingList", owing);
        model.addAttribute("resolvedList", resolved);
        model.addAttribute("totalOwing", owing.stream()
                .map(d -> d.getAssessedFine() != null ? d.getAssessedFine() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return "admin/borrow/violations";
    }

    /**
     * Admin xác nhận độc giả đã nộp phạt — chuyển record sang tab "Đã xử lý"
     */
    @PostMapping("/violations/{detailId}/settle")
    public String settleViolation(@PathVariable Integer detailId,
                                  RedirectAttributes redirectAttributes) {
        try {
            borrowService.settleViolationFine(detailId);
            redirectAttributes.addFlashAttribute("message", "✅ Đã xác nhận nộp phạt thành công! Vi phạm đã được chuyển sang tab Đã xử lý.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/borrow/violations";
    }
}
