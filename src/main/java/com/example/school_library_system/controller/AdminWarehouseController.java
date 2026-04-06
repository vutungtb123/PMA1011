package com.example.school_library_system.controller;

import com.example.school_library_system.dto.WarehouseReceiptDto;
import com.example.school_library_system.entity.WarehouseReceipt;
import com.example.school_library_system.service.BookService;
import com.example.school_library_system.service.WarehouseReceiptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/warehouse")
public class AdminWarehouseController {

    @Autowired
    private WarehouseReceiptService warehouseReceiptService;

    @Autowired
    private BookService bookService;

    // 1. Hiển thị danh sách phiếu nhập
    @GetMapping({"", "/"})
    public String listReceipts(Model model) {
        List<WarehouseReceipt> receipts = warehouseReceiptService.getAllReceipts();
        model.addAttribute("receipts", receipts);
        return "admin/warehouse/receipt-list";
    }

    // 2. Trang tạo phiếu nhập mới
    @GetMapping("/new")
    public String createReceiptForm(Model model) {
        model.addAttribute("allBooks", bookService.getAllBooks());
        return "admin/warehouse/receipt-create";
    }

    // 3. Xử lý lưu phiếu nhập (AJAX)
    @PostMapping("/api/save")
    @ResponseBody
    public ResponseEntity<?> saveReceipt(@RequestBody WarehouseReceiptDto dto) {
        try {
            WarehouseReceipt saved = warehouseReceiptService.saveReceipt(dto);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("receiptId", saved.getReceiptId());
            response.put("message", "Lưu phiếu nhập kho thành công.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Lỗi khi lưu phiếu nhập: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // 4. Trang hiển thị chi tiết phiếu nhập (Read-Only)
    @GetMapping("/{id}")
    public String viewReceiptDetail(@PathVariable Integer id, Model model) {
        WarehouseReceipt receipt = warehouseReceiptService.getReceiptById(id);
        if(receipt == null) {
            return "redirect:/admin/warehouse";
        }
        model.addAttribute("receipt", receipt);
        return "admin/warehouse/receipt-detail";
    }
}
