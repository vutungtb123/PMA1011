package com.example.school_library_system.controller;

import com.example.school_library_system.entity.Book;
import com.example.school_library_system.entity.BookCopy;
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
import java.util.List;
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
        List<Book> books = bookService.getAllBooks();
        model.addAttribute("books", books);

        // Trạng thái kho cho mỗi sách — dùng hiển thị badge trong danh sách
        Map<Integer, Boolean> warehouseStatusMap = new java.util.HashMap<>();
        Map<Integer, Integer> warehouseQtyMap   = new java.util.HashMap<>();
        Map<Integer, Long>    copyCountMap       = new java.util.HashMap<>();
        for (Book b : books) {
            boolean has = warehouseReceiptService.hasWarehouseReceipt(b.getBookId());
            int     qty = has ? warehouseReceiptService.getTotalWarehouseQuantity(b.getBookId()) : 0;
            long    cnt = bookCopyService.countAllCopiesByBookId(b.getBookId());
            warehouseStatusMap.put(b.getBookId(), has);
            warehouseQtyMap.put(b.getBookId(), qty);
            copyCountMap.put(b.getBookId(), cnt);
        }
        model.addAttribute("warehouseStatusMap", warehouseStatusMap);
        model.addAttribute("warehouseQtyMap",   warehouseQtyMap);
        model.addAttribute("copyCountMap",       copyCountMap);
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

    /**
     * Quick-create: Tạo sách nhanh ngay từ form phiếu nhập kho.
     * Sách được tạo vào catalog nhưng chưa có barcode — cần phiếu kho mới lên kệ được.
     */
    @PostMapping("/api/quick-create")
    @ResponseBody
    public ResponseEntity<?> quickCreateBook(@RequestBody Map<String, String> payload) {
        try {
            String title = payload.getOrDefault("title", "").trim();
            if (title.isEmpty()) {
                Map<String, Object> err = new HashMap<>();
                err.put("success", false);
                err.put("message", "Tên sách không được để trống!");
                return ResponseEntity.badRequest().body(err);
            }

            Book book = new Book();
            book.setTitle(title);
            book.setAuthor(payload.getOrDefault("author", "").trim());
            book.setPublisher(payload.getOrDefault("publisher", "").trim());

            Book saved = bookService.saveBook(book, null);

            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.put("bookId", saved.getBookId());
            resp.put("title", saved.getTitle());
            resp.put("author", saved.getAuthor() != null ? saved.getAuthor() : "");
            resp.put("message", "✅ Đã tạo sách \"" + saved.getTitle() + "\" vào catalogue!");
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Lỗi tạo sách: " + e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
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
        result.put("damagedCount", bookCopyService.countDamagedCopiesByBookId(bookId));
        return ResponseEntity.ok(result);
    }

    // --- Thêm hàng loạt bản sao từ danh sách sách có sẵn ---
    @GetMapping("/bulk-copies")
    public String showBulkCopiesForm(Model model) {
        List<Book> allBooks = bookService.getAllBooks();
        model.addAttribute("allBooks", allBooks);

        // Tính trạng thái kho cho từng sách để hiển thị trong bảng
        Map<Integer, Integer> warehouseQtyMap = new java.util.HashMap<>();
        Map<Integer, Long> copyCountMap = new java.util.HashMap<>();
        for (Book b : allBooks) {
            int wQty = warehouseReceiptService.getTotalWarehouseQuantity(b.getBookId());
            long copies = bookCopyService.countAllCopiesByBookId(b.getBookId());
            warehouseQtyMap.put(b.getBookId(), wQty);
            copyCountMap.put(b.getBookId(), copies);
        }
        model.addAttribute("warehouseQtyMap", warehouseQtyMap);
        model.addAttribute("copyCountMap", copyCountMap);
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
            List<String> barcodes = bookCopyService.addBulkCopies(bookId, quantity);
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.put("created", barcodes.size());
            resp.put("barcodes", barcodes);
            resp.put("message", "Đã tạo thành công " + barcodes.size() + " bản sao mới lên kệ!");
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
        if (book == null) return "redirect:/admin/book";

        boolean hasWarehouse = warehouseReceiptService.hasWarehouseReceipt(id);
        // Tổng phiếu kho khai báo
        int warehouseQty = hasWarehouse ? warehouseReceiptService.getTotalWarehouseQuantity(id) : 0;
        // Tổng mã vạch đã tạo (mọi trạng thái)
        long existingCopies = bookCopyService.countAllCopiesByBookId(id);
        // Bản sao dư so với phiếu kho (dữ liệu cũ tạo trước khi có validation)
        long orphanedCopies = (hasWarehouse && existingCopies > warehouseQty) ? existingCopies - warehouseQty : 0;
        // Slot còn lại = phiếu kho - số mã vạch đã tạo
        int remainingSlots = hasWarehouse ? (int) Math.max(0, warehouseQty - existingCopies) : 0;

        model.addAttribute("book",           book);
        model.addAttribute("copies",         bookCopyService.getCopiesByBookId(id));
        model.addAttribute("newCopy",        new BookCopy());
        model.addAttribute("damagedCount",   bookCopyService.countDamagedCopiesByBookId(id));
        model.addAttribute("hasWarehouse",   hasWarehouse);
        model.addAttribute("warehouseQty",   warehouseQty);
        model.addAttribute("existingCopies", existingCopies);
        model.addAttribute("remainingSlots", remainingSlots);
        model.addAttribute("orphanedCopies", orphanedCopies);
        return "admin/book/manage-copies";
    }

    @PostMapping("/{id}/copies/save")
    public String saveCopy(@PathVariable Integer id, @ModelAttribute BookCopy copy, RedirectAttributes redirectAttributes) {
        // Chặn null sách
        Book book = bookService.getBookById(id);
        if (book == null) return "redirect:/admin/book";
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

        // === GATE 2: Kiểm tra phiếu nhập kho (ĐÂY LÀ GATE BẪT BUỘC) ===
        // Chỉ kiểm tra theo BookID — không chấp nhận fallback hay bypass
        boolean hasReceipt = warehouseReceiptService.hasWarehouseReceipt(id);
        if (!hasReceipt) {
            redirectAttributes.addFlashAttribute("error",
                "❌ KHAI BAN: cuốn sách \"" + book.getTitle() + "\" chưa có phiếu nhập kho! " +
                "Vui lòng vào mục Kho → Tạo phiếu nhập trước rồi mới có thể lên kệ.");
            return "redirect:/admin/book/" + id + "/copies";
        }

        // === GATE 3: Kiểm tra số lượng không vượt quá phiếu kho ===
        int warehouseQty = warehouseReceiptService.getTotalWarehouseQuantity(id);
        long existingCopies = bookCopyService.countAllCopiesByBookId(id);
        if (existingCopies >= warehouseQty) {
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
