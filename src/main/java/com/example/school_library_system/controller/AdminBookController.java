package com.example.school_library_system.controller;

import com.example.school_library_system.entity.Book;
import com.example.school_library_system.entity.BookCopy;
import com.example.school_library_system.entity.BookSamplePage;
import com.example.school_library_system.service.BookCopyService;
import com.example.school_library_system.service.BookService;
import com.example.school_library_system.service.CategoryService;
import com.example.school_library_system.service.WarehouseReceiptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin/book")
public class AdminBookController {

    @Autowired
    private BookService bookService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private BookCopyService bookCopyService;

    @Autowired
    private WarehouseReceiptService warehouseReceiptService;

    @GetMapping({"", "/"})
    public String listBooks(Model model) {
        model.addAttribute("books", bookService.getAllBooks());
        return "admin/book/list";
    }

    @GetMapping("/new")
    public String showNewForm(Model model) {
        model.addAttribute("book", new Book());
        model.addAttribute("categories", categoryService.getAllCategories());
        return "admin/book/form";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Integer id, Model model) {
        model.addAttribute("book", bookService.getBookById(id));
        model.addAttribute("categories", categoryService.getAllCategories());
        return "admin/book/form";
    }

    @PostMapping("/save")
    public String saveBook(@ModelAttribute Book book, @RequestParam("coverFile") MultipartFile coverFile, RedirectAttributes redirectAttributes) {
        bookService.saveBook(book, coverFile);
        redirectAttributes.addFlashAttribute("message", "Lưu thông tin sách thành công!");
        return "redirect:/admin/book";
    }

    @PostMapping("/delete/{id}")
    public String deleteBook(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        bookService.deleteBook(id);
        redirectAttributes.addFlashAttribute("message", "Đã xóa sách khỏi thư viện!");
        return "redirect:/admin/book";
    }

    // --- Kiểm tra phiếu kho realtime (AJAX) ---
    @GetMapping("/api/check-warehouse")
    @ResponseBody
    public ResponseEntity<?> checkWarehouse(@RequestParam Integer bookId) {
        boolean hasReceipt = warehouseReceiptService.hasWarehouseReceipt(bookId);
        int warehouseQty = warehouseReceiptService.getTotalWarehouseQuantity(bookId);
        // Tổng bản sao hiện có trong hệ thống (mọi trạng thái)
        long existingCopies = bookCopyService.countAllCopiesByBookId(bookId);

        Map<String, Object> result = new HashMap<>();
        result.put("hasWarehouse", hasReceipt);
        result.put("warehouseQty", warehouseQty);
        result.put("existingCopies", existingCopies);
        result.put("remainingSlots", Math.max(0, warehouseQty - (int) existingCopies));
        return ResponseEntity.ok(result);
    }

    // --- Thêm hàng loạt bản sao từ danh sách sách có sẵn ---
    @GetMapping("/bulk-copies")
    public String showBulkCopiesForm(Model model) {
        model.addAttribute("allBooks", bookService.getAllBooks());
        return "admin/book/bulk-copies";
    }

    @PostMapping("/bulk-copies/save")
    @ResponseBody
    public ResponseEntity<?> saveBulkCopies(
            @RequestParam Integer bookId,
            @RequestParam int quantity) {
        try {
            if (quantity < 1 || quantity > 100) {
                throw new IllegalArgumentException("Số lượng phải từ 1 đến 100.");
            }
            int created = bookCopyService.addBulkCopies(bookId, quantity);
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.put("created", created);
            resp.put("message", "Đã tạo thành công " + created + " bản sao mới lên kệ!");
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", false);
            resp.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(resp);
        }
    }

    // --- Quản lý Nội dung 10 trang đọc thử ---
    @GetMapping("/{id}/samples")
    public String manageSamples(@PathVariable Integer id, Model model) {
        Book book = bookService.getBookById(id);
        if(book == null) return "redirect:/admin/book";

        model.addAttribute("book", book);
        model.addAttribute("samplePages", bookService.getSamplePages(id));
        // savedPage được truyền từ redirectAttributes sau khi lưu thành công
        // (đã tự động đưa vào model qua flash attribute)
        return "admin/book/manage-samples";
    }

    @PostMapping("/{id}/samples/save")
    public String saveSamplePage(
            @PathVariable Integer id,
            @RequestParam("pageNumber") Integer pageNumber,
            @RequestParam("isImage") Boolean isImage,
            @RequestParam(value = "contentText", required = false) String contentText,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestParam(value = "nextPage", required = false) Integer nextPage,
            RedirectAttributes redirectAttributes) {

        bookService.saveSamplePage(id, pageNumber, isImage, contentText, imageFile);
        redirectAttributes.addFlashAttribute("message", "Lưu nội dung trang " + pageNumber + " thành công!");
        // Truyền nextPage để JS tự động chuyển sang trang tiếp theo sau redirect
        if (nextPage != null && nextPage >= 1 && nextPage <= 10) {
            redirectAttributes.addFlashAttribute("savedPage", nextPage);
        }
        return "redirect:/admin/book/" + id + "/samples";
    }

    @PostMapping("/samples/delete/{pageId}/{bookId}")
    public String deleteSamplePage(@PathVariable Integer pageId, @PathVariable Integer bookId, RedirectAttributes redirectAttributes) {
        bookService.deleteSamplePage(pageId);
        redirectAttributes.addFlashAttribute("message", "Đã xóa trang đọc thử!");
        return "redirect:/admin/book/" + bookId + "/samples";
    }

    // --- Quản lý các Bản sao Vật lý (Copies) ---
    @GetMapping("/{id}/copies")
    public String manageCopies(@PathVariable Integer id, Model model) {
        Book book = bookService.getBookById(id);
        if(book == null) return "redirect:/admin/book";

        model.addAttribute("book", book);
        model.addAttribute("copies", bookCopyService.getCopiesByBookId(id));
        model.addAttribute("newCopy", new BookCopy());
        return "admin/book/manage-copies";
    }

    @PostMapping("/{id}/copies/save")
    public String saveCopy(@PathVariable Integer id, @ModelAttribute BookCopy copy, RedirectAttributes redirectAttributes) {
        Book book = bookService.getBookById(id);
        copy.setBook(book);
        if (copy.getPhysicalStatus() == null || copy.getPhysicalStatus().isEmpty()) {
            copy.setPhysicalStatus("Sẵn sàng");
        }

        // === GATE 1: Kiểm tra barcode trùng ===
        if (copy.getBarcode() != null && !copy.getBarcode().isBlank()) {
            if (bookCopyService.isBarcodeExists(copy.getBarcode().trim())) {
                redirectAttributes.addFlashAttribute("error",
                    "❌ Mã vạch \"" + copy.getBarcode().trim() + "\" đã tồn tại trong hệ thống! Vui lòng nhập mã vạch khác.");
                return "redirect:/admin/book/" + id + "/copies";
            }
        }

        // === GATE 2: Kiểm tra giới hạn kho ===
        int warehouseQty = warehouseReceiptService.getTotalWarehouseQuantity(id);
        long existingCopies = bookCopyService.countAllCopiesByBookId(id);
        int remainingSlots = (int) Math.max(0, warehouseQty - existingCopies);
        if (warehouseQty > 0 && remainingSlots <= 0) {
            redirectAttributes.addFlashAttribute("error",
                "⛔ Đã đạt giới hạn kho! Phiếu nhập ghi " + warehouseQty + " cuốn, " +
                "đã đăng ký " + existingCopies + " bản sao. Không thể thêm mã vạch mới.");
            return "redirect:/admin/book/" + id + "/copies";
        }

        bookCopyService.saveCopy(copy);
        redirectAttributes.addFlashAttribute("message", "✅ Đã thêm mã vạch \"" + copy.getBarcode() + "\" thành công!");
        return "redirect:/admin/book/" + id + "/copies";
    }

    @PostMapping("/{bookId}/copies/deactivate/{copyId}")
    public String deactivateCopy(@PathVariable Integer bookId, @PathVariable Integer copyId, RedirectAttributes redirectAttributes) {
        bookCopyService.deactivateCopy(copyId);
        redirectAttributes.addFlashAttribute("alertWarning", "Vô hiệu hóa cuốn sách thành công! Phiếu mượn Đang chờ của Độc giả đã bị Hủy và gửi Email thông báo.");
        return "redirect:/admin/book/" + bookId + "/copies";
    }

    @PostMapping("/{bookId}/copies/delete/{copyId}")
    public String deleteCopy(@PathVariable Integer bookId, @PathVariable Integer copyId, RedirectAttributes redirectAttributes) {
        bookCopyService.deleteCopy(copyId);
        redirectAttributes.addFlashAttribute("message", "Đã thanh lý/xóa mã vạch khỏi hệ thống!");
        return "redirect:/admin/book/" + bookId + "/copies";
    }
}
