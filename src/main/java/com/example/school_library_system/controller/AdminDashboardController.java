package com.example.school_library_system.controller;

import com.example.school_library_system.repository.BorrowDetailRepository;
import com.example.school_library_system.repository.BorrowRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;
import java.util.*;

@Controller
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final BorrowDetailRepository borrowDetailRepository;
    private final BorrowRecordRepository borrowRecordRepository;

    @GetMapping({"", "/"})
    public String dashboard(Model model) {

        // ── 1. Top 10 sách mượn nhiều nhất ──────────────────────────────
        List<Object[]> topBooksRaw = borrowDetailRepository.findTopBorrowedBooks(PageRequest.of(0, 10));
        List<String> topBookLabels = new ArrayList<>();
        List<Long>   topBookCounts = new ArrayList<>();
        for (Object[] row : topBooksRaw) {
            topBookLabels.add((String) row[0]);
            topBookCounts.add(((Number) row[1]).longValue());
        }

        // ── 2. Lượt mượn theo tháng (12 tháng gần nhất) ─────────────────
        List<Object[]> rawMonthly = borrowRecordRepository.findMonthlyBorrowStats();
        // Tạo map year-month → count
        Map<String, Long> monthMap = new LinkedHashMap<>();
        for (Object[] row : rawMonthly) {
            int year  = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            long cnt  = ((Number) row[2]).longValue();
            monthMap.put(year + "-" + String.format("%02d", month), cnt);
        }
        // Lấy 12 tháng gần nhất
        List<String> monthLabels = new ArrayList<>();
        List<Long>   monthCounts = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 11; i >= 0; i--) {
            LocalDate d = today.minusMonths(i);
            String key = d.getYear() + "-" + String.format("%02d", d.getMonthValue());
            String label = "T" + d.getMonthValue() + "/" + d.getYear();
            monthLabels.add(label);
            monthCounts.add(monthMap.getOrDefault(key, 0L));
        }

        // ── 3. Top 3 học sinh mượn nhiều nhất ────────────────────────────
        List<Object[]> topReadersRaw = borrowRecordRepository.findTopReaders(PageRequest.of(0, 3));
        List<Map<String, Object>> topReaders = new ArrayList<>();
        int rank = 1;
        for (Object[] row : topReadersRaw) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("rank", rank++);
            entry.put("name", (String) row[0]);
            entry.put("count", ((Number) row[1]).longValue());
            topReaders.add(entry);
        }

        model.addAttribute("topBookLabels",  topBookLabels);
        model.addAttribute("topBookCounts",  topBookCounts);
        model.addAttribute("monthLabels",    monthLabels);
        model.addAttribute("monthCounts",    monthCounts);
        model.addAttribute("topReaders",     topReaders);
        // Flag: có dữ liệu mượn theo tháng không
        boolean hasMonthlyData = monthCounts.stream().anyMatch(c -> c > 0);
        model.addAttribute("hasMonthlyData", hasMonthlyData);

        return "admin/dashboard";
    }
}
