package com.example.school_library_system.controller;

import com.example.school_library_system.entity.Book;
import com.example.school_library_system.entity.BookSamplePage;
import com.example.school_library_system.entity.BorrowRecord;
import com.example.school_library_system.entity.BookCopy;
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

import java.util.stream.Collectors;
import java.util.List;

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
            Model model) {

        int pageSize = 40;
        Page<Book> bookPage;
        
        if (categoryId != null) {
            bookPage = bookRepository.searchByCategoryAndTitleOrAuthor(
                    categoryId, keyword, PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        } else {
            bookPage = bookRepository.searchByTitleOrAuthor(
                    keyword, PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        }
        
        List<Category> allCategories = categoryRepository.findAll();

        model.addAttribute("bookPage", bookPage);
        model.addAttribute("allCategories", allCategories);
        model.addAttribute("currentCategoryId", categoryId);
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", bookPage.getTotalPages());

        return "user/home";
    }

    /**
     * Chi tiết sách: tên, tác giả, số bản còn sẵn, nút đọc thử + mượn sách
     */
    @GetMapping("/book/{id}")
    public String bookDetail(@PathVariable Integer id, Model model,
                             @AuthenticationPrincipal CustomUserDetails userDetails) {
        Book book = bookRepository.findById(id).orElse(null);
        if (book == null) {
            return "redirect:/dashboard";
        }

        long availableCopies = bookCopyRepository.countByBookBookIdAndPhysicalStatus(id, "Sẵn sàng");
        Integer userId = userDetails.getUser().getUserId();
        boolean alreadyBorrowed = borrowService.hasReaderBorrowedBook(userId, id);
        long activeBorrowCount = borrowService.countActiveBorrows(userId);

        model.addAttribute("book", book);
        model.addAttribute("availableCopies", availableCopies);
        model.addAttribute("alreadyBorrowed", alreadyBorrowed);
        model.addAttribute("activeBorrowCount", activeBorrowCount);
        model.addAttribute("maxBorrows", 5);

        return "user/book-detail";
    }

    /**
     * Xử lý mượn sách
     */
    @PostMapping("/book/{id}/borrow")
    public String borrowBook(@PathVariable Integer id,
                             @AuthenticationPrincipal CustomUserDetails userDetails,
                             RedirectAttributes redirectAttributes) {
        try {
            Integer userId = userDetails.getUser().getUserId();
            BorrowRecord record = borrowService.borrowBookForReader(userId, id);
            redirectAttributes.addFlashAttribute("borrowSuccess", true);
            redirectAttributes.addFlashAttribute("borrowRecord", record);
            return "redirect:/my-books";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("borrowError", e.getMessage());
            return "redirect:/book/" + id;
        }
    }

    /**
     * Trang Sách của tôi (My Books)
     */
    @GetMapping("/my-books")
    public String myBooks(Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Integer userId = userDetails.getUser().getUserId();
        List<BorrowRecord> allRecords = borrowService.getReaderBorrows(userId);
        
        List<BorrowRecord> pendingRecords = allRecords.stream()
                .filter(r -> "Đã đặt chỗ".equals(r.getRecordStatus()))
                .collect(Collectors.toList());
                
        List<BorrowRecord> activeRecords = allRecords.stream()
                .filter(r -> "Đang mượn".equals(r.getRecordStatus()))
                .collect(Collectors.toList());

        List<BorrowRecord> historyRecords = allRecords.stream()
                .filter(r -> !"Đã đặt chỗ".equals(r.getRecordStatus()) && !"Đang mượn".equals(r.getRecordStatus()))
                .collect(Collectors.toList());

        model.addAttribute("pendingRecords", pendingRecords);
        model.addAttribute("activeRecords", activeRecords);
        model.addAttribute("historyRecords", historyRecords);
        
        return "user/my-books";
    }

    /**
     * Trang đọc thử
     */
    @GetMapping("/book/{id}/read")
    public String bookReader(@PathVariable Integer id, Model model) {
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
