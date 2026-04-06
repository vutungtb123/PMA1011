package com.example.school_library_system.controller;

import com.example.school_library_system.dto.DirectBorrowDto;
import com.example.school_library_system.service.BookService;
import com.example.school_library_system.service.BorrowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/borrow")
public class AdminBorrowController {

    @Autowired
    private BorrowService borrowService;

    @Autowired
    private BookService bookService;

    @GetMapping({"", "/"})
    public String listBorrows(Model model) {
        model.addAttribute("borrows", borrowService.getAllBorrowRecords());
        return "admin/borrow/list";
    }

    @GetMapping("/new")
    public String showNewBorrowForm(Model model) {
        model.addAttribute("borrowDto", new DirectBorrowDto());
        model.addAttribute("books", bookService.getAllBooks()); // Cho phép chọn đầu sách
        return "admin/borrow/form";
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
                                RedirectAttributes redirectAttributes) {
        try {
            borrowService.confirmBorrowRequest(id, borrowDays);
            redirectAttributes.addFlashAttribute("message",
                "✅ Đã xác nhận giao sách cho Phiếu #" + id + ". Hạn trả: " + borrowDays + " ngày kể từ hôm nay.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi xác nhận: " + e.getMessage());
        }
        return "redirect:/admin/borrow";
    }

    /**
     * Admin đánh dấu sách đã được trả
     */
    @PostMapping("/{id}/return")
    public String returnBook(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            borrowService.returnBook(id);
            redirectAttributes.addFlashAttribute("message", "📚 Phiếu #" + id + " đã được đánh dấu Đã Trả.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/borrow";
    }
}
