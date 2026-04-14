package com.example.school_library_system.controller;

import com.example.school_library_system.entity.Book;
import com.example.school_library_system.entity.BookSamplePage;
import com.example.school_library_system.entity.BorrowDetail;
import com.example.school_library_system.entity.BorrowRecord;
import com.example.school_library_system.entity.Category;
import com.example.school_library_system.repository.CategoryRepository;
import com.example.school_library_system.repository.BookCopyRepository;
import com.example.school_library_system.repository.BookRepository;
import com.example.school_library_system.repository.BookSamplePageRepository;
import com.example.school_library_system.security.CustomUserDetails;
import com.example.school_library_system.service.BorrowService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Controller
@RequiredArgsConstructor
public class UserBookController {

    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;
    private final BookSamplePageRepository samplePageRepository;
    private final CategoryRepository categoryRepository;
    private final BorrowService borrowService;

    /**
     * Trang chủ: hiển thị danh sách sách dưới dạng card (4 cột x 10 hàng = 40 sách/trang)
     */
    @GetMapping("/dashboard")
    public String home(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(defaultValue = "0") int page,
            Model model,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        int pageSize = 40;
        Page<Book> bookPage;
        
        Category currentCategory = null;
        if (categoryId != null) {
            bookPage = bookRepository.searchByCategoryAndTitleOrAuthor(
                    categoryId, keyword, PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt")));
            currentCategory = categoryRepository.findById(categoryId).orElse(null);
        } else {
            bookPage = bookRepository.searchByTitleOrAuthor(
                    keyword, PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        }
        
        List<Category> allCategories = categoryRepository.findAll();

        // Lấy danh sách bookId mà user đang mượn/đặt chỗ để hiển thị badge lock
        Integer userId = userDetails.getUser().getUserId();
        List<Integer> activeBorrowedBookIds = borrowService.getActiveBorrowedBookIds(userId);

        model.addAttribute("bookPage", bookPage);
        model.addAttribute("allCategories", allCategories);
        model.addAttribute("currentCategoryId", categoryId);
        model.addAttribute("currentCategory", currentCategory);
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", bookPage.getTotalPages());
        model.addAttribute("activeBorrowedBookIds", activeBorrowedBookIds);

        return "user/home";
    }

    /**
     * Chi tiết sách: tên, tác giả, số bản còn sẵn, nút đọc thử + mượn sách
     */
    @GetMapping("/book/{id}")
    public String bookDetail(@PathVariable Integer id, Model model,
                             @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (id == null) return "redirect:/dashboard";
        Book book = bookRepository.findById(id).orElse(null);
        if (book == null) {
            return "redirect:/dashboard";
        }

        long availableCopies = bookCopyRepository.countByBookBookIdAndPhysicalStatus(id, "Sẵn sàng");
        Integer userId = userDetails.getUser().getUserId();
        boolean alreadyBorrowed = borrowService.hasReaderBorrowedBook(userId, id);
        // Lấy trạng thái cụ thể ("Đã đặt chỗ", "Đang mượn", null)
        String activeStatus = borrowService.getActiveStatusForBook(userId, id);
        long activeBorrowCount = borrowService.countActiveBorrows(userId);
        boolean hasUnpaidViolation = borrowService.hasUnpaidViolation(userId);

        model.addAttribute("book", book);
        model.addAttribute("availableCopies", availableCopies);
        model.addAttribute("alreadyBorrowed", alreadyBorrowed);
        model.addAttribute("activeStatus", activeStatus);
        model.addAttribute("activeBorrowCount", activeBorrowCount);
        model.addAttribute("maxBorrows", 5);
        model.addAttribute("allCategories", categoryRepository.findAll());
        model.addAttribute("hasUnpaidViolation", hasUnpaidViolation);

        return "user/book-detail";
    }

    // Khóa tránh spam request mượn sách đồng thời
    private final Set<Integer> processingUsers = ConcurrentHashMap.newKeySet();

    /**
     * Xử lý mượn sách
     */
    @PostMapping("/book/{id}/borrow")
    public String borrowBook(@PathVariable Integer id,
                             @AuthenticationPrincipal CustomUserDetails userDetails,
                             RedirectAttributes redirectAttributes) {
        Integer userId = userDetails.getUser().getUserId();

        // Kiểm tra vi phạm chưa nộp phạt — chặn trước khi tiếp tục
        if (borrowService.hasUnpaidViolation(userId)) {
            redirectAttributes.addFlashAttribute("borrowError",
                    "⚠️ Tài khoản của bạn đang bị khóa do có khoản phạt chưa thanh toán. Vui lòng đến quầy thủ thư để nộp phạt trước.");
            return "redirect:/book/" + id;
        }

        // Cấp độ bảo vệ 1: Ngăn submit đồng thời (Double-Submit / Spam clicks)
        if (!processingUsers.add(userId)) {
            redirectAttributes.addFlashAttribute("borrowError", "Vui lòng đợi một chút, hệ thống đang xử lý yêu cầu mượn trước đó của bạn!");
            return "redirect:/book/" + id;
        }
                             
        try {
            BorrowRecord record = borrowService.borrowBookForReader(userId, id);
            redirectAttributes.addFlashAttribute("borrowSuccess", true);
            redirectAttributes.addFlashAttribute("borrowRecord", record);
            return "redirect:/phieu-dat-cho";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("borrowError", e.getMessage());
            return "redirect:/book/" + id;
        } finally {
            processingUsers.remove(userId);
        }
    }

    /**
     * [Redirect backward-compat] /my-books → /phieu-dat-cho
     */
    @GetMapping("/my-books")
    public String myBooksRedirect() {
        return "redirect:/phieu-dat-cho";
    }

    // =============================================
    // TRANG 1: PHỆU ĐẶT CHỔ (chờ lấy sách)
    // =============================================
    @GetMapping("/phieu-dat-cho")
    public String reservations(Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Integer userId = userDetails.getUser().getUserId();
        List<BorrowRecord> pendingRecords = borrowService.getReaderBorrows(userId).stream()
                .filter(r -> "Đã đặt chỗ".equals(r.getRecordStatus()))
                .collect(Collectors.toList());
        model.addAttribute("pendingRecords", pendingRecords);
        model.addAttribute("allCategories", categoryRepository.findAll());
        return "user/reservations";
    }

    // =============================================
    // TRANG 2: THEO DÕI MƯỢN SÁCH (đang mượn)
    // =============================================
    @GetMapping("/dang-muon")
    public String activeBorrows(Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Integer userId = userDetails.getUser().getUserId();
        List<BorrowRecord> activeRecords = borrowService.getReaderBorrows(userId).stream()
                .filter(r -> "Đang mượn".equals(r.getRecordStatus()))
                .collect(Collectors.toList());
        model.addAttribute("activeRecords", activeRecords);
        model.addAttribute("allCategories", categoryRepository.findAll());
        return "user/active-borrows";
    }

    // =============================================
    // TRANG 3: LỊCH SỬ MƯỢN
    // =============================================
    @GetMapping("/lich-su-muon")
    public String borrowHistory(Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Integer userId = userDetails.getUser().getUserId();
        List<BorrowRecord> historyRecords = borrowService.getReaderBorrows(userId).stream()
                .filter(r -> !"Đã đặt chỗ".equals(r.getRecordStatus()) && !"Đang mượn".equals(r.getRecordStatus()))
                .collect(Collectors.toList());
        model.addAttribute("historyRecords", historyRecords);
        model.addAttribute("allCategories", categoryRepository.findAll());
        return "user/borrow-history";
    }

    // =============================================
    // TRANG 4: LỊCH SỬ VI PHẠM & PHẠT
    // =============================================
    @GetMapping("/lich-su-phat")
    public String fineHistory(Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Integer userId = userDetails.getUser().getUserId();

        // Lấy tất cả BorrowRecord đã kết thúc của user
        List<BorrowRecord> finishedRecords = borrowService.getReaderBorrows(userId).stream()
                .filter(r -> "Đã trả".equals(r.getRecordStatus()) || "Đã hủy".equals(r.getRecordStatus()))
                .collect(Collectors.toList());

        // Lọc chỉ những BorrowDetail có vi phạm (trả muộn, hư hỏng, hoặc có ghi tiền phạt)
        List<BorrowDetail> violationDetails = finishedRecords.stream()
                .flatMap(r -> r.getDetails().stream())
                .filter(d -> "Trả muộn".equals(d.getReturnPhysicalState())
                        || "Hư hỏng".equals(d.getReturnPhysicalState())
                        || (d.getAssessedFine() != null && d.getAssessedFine().compareTo(BigDecimal.ZERO) > 0)
                        || (d.getViolationNote() != null && !d.getViolationNote().isBlank()))
                .sorted((a, b) -> {
                    // Sắp xếp mới nhất lên đầu
                    if (a.getReturnDate() == null && b.getReturnDate() == null) return 0;
                    if (a.getReturnDate() == null) return 1;
                    if (b.getReturnDate() == null) return -1;
                    return b.getReturnDate().compareTo(a.getReturnDate());
                })
                .collect(Collectors.toList());

        // Tổng tiền phạt
        BigDecimal totalFine = violationDetails.stream()
                .map(d -> d.getAssessedFine() != null ? d.getAssessedFine() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("violationDetails", violationDetails);
        model.addAttribute("totalFine", totalFine);
        model.addAttribute("allCategories", categoryRepository.findAll());
        return "user/fine-history";
    }

    /**
     * Trang đọc thử
     */
    @GetMapping("/book/{id}/read")
    public String bookReader(@PathVariable Integer id, Model model) {
        if (id == null) return "redirect:/dashboard";
        Book book = bookRepository.findById(id).orElse(null);
        if (book == null) {
            return "redirect:/dashboard";
        }

        List<BookSamplePage> pages = samplePageRepository.findByBookBookIdOrderByPageNumberAsc(id);

        model.addAttribute("book", book);
        model.addAttribute("samplePages", pages);

        return "user/book-reader";
    }

    /**
     * API: Tìm kiếm sách live-search trả JSON
     */
    @GetMapping("/api/books/search")
    @ResponseBody
    public List<java.util.Map<String, Object>> liveSearch(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) Integer categoryId) {

        Page<Book> bookPage;
        if (categoryId != null) {
            bookPage = bookRepository.searchByCategoryAndTitleOrAuthor(
                    categoryId, keyword, PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt")));
        } else {
            bookPage = bookRepository.searchByTitleOrAuthor(
                    keyword, PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt")));
        }

        return bookPage.getContent().stream().map(book -> {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("bookId", book.getBookId());
            map.put("title", book.getTitle());
            map.put("author", book.getAuthor());
            map.put("coverImage", book.getCoverImage());
            return map;
        }).collect(Collectors.toList());
    }
}
