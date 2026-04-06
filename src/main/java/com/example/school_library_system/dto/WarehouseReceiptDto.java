package com.example.school_library_system.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class WarehouseReceiptDto {
    private LocalDateTime importDate;
    private Integer declaredTotalQuantity;
    private BigDecimal declaredTotalPrice;
    private List<WarehouseReceiptDetailDto> details;

    public LocalDateTime getImportDate() { return importDate; }
    public void setImportDate(LocalDateTime importDate) { this.importDate = importDate; }

    public Integer getDeclaredTotalQuantity() { return declaredTotalQuantity; }
    public void setDeclaredTotalQuantity(Integer declaredTotalQuantity) { this.declaredTotalQuantity = declaredTotalQuantity; }

    public BigDecimal getDeclaredTotalPrice() { return declaredTotalPrice; }
    public void setDeclaredTotalPrice(BigDecimal declaredTotalPrice) { this.declaredTotalPrice = declaredTotalPrice; }

    public List<WarehouseReceiptDetailDto> getDetails() { return details; }
    public void setDetails(List<WarehouseReceiptDetailDto> details) { this.details = details; }
}
