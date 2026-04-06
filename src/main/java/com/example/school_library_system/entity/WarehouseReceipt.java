package com.example.school_library_system.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "WarehouseReceipts")
public class WarehouseReceipt {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ReceiptID")
    private Integer receiptId;

    @Column(name = "ImportDate", nullable = false)
    private LocalDateTime importDate;

    @Column(name = "DeclaredTotalQuantity", nullable = false)
    private Integer declaredTotalQuantity;

    @Column(name = "DeclaredTotalPrice", nullable = false)
    private BigDecimal declaredTotalPrice;

    @Column(name = "ActualTotalQuantity", nullable = false)
    private Integer actualTotalQuantity;

    @Column(name = "ActualTotalPrice", nullable = false)
    private BigDecimal actualTotalPrice;

    @Column(name = "CreatedAt")
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "warehouseReceipt", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WarehouseReceiptDetail> details = new ArrayList<>();

    // Getters and Setters
    public Integer getReceiptId() { return receiptId; }
    public void setReceiptId(Integer receiptId) { this.receiptId = receiptId; }

    public LocalDateTime getImportDate() { return importDate; }
    public void setImportDate(LocalDateTime importDate) { this.importDate = importDate; }

    public Integer getDeclaredTotalQuantity() { return declaredTotalQuantity; }
    public void setDeclaredTotalQuantity(Integer declaredTotalQuantity) { this.declaredTotalQuantity = declaredTotalQuantity; }

    public BigDecimal getDeclaredTotalPrice() { return declaredTotalPrice; }
    public void setDeclaredTotalPrice(BigDecimal declaredTotalPrice) { this.declaredTotalPrice = declaredTotalPrice; }

    public Integer getActualTotalQuantity() { return actualTotalQuantity; }
    public void setActualTotalQuantity(Integer actualTotalQuantity) { this.actualTotalQuantity = actualTotalQuantity; }

    public BigDecimal getActualTotalPrice() { return actualTotalPrice; }
    public void setActualTotalPrice(BigDecimal actualTotalPrice) { this.actualTotalPrice = actualTotalPrice; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<WarehouseReceiptDetail> getDetails() { return details; }
    public void setDetails(List<WarehouseReceiptDetail> details) { 
        this.details = details;
        if(details != null) {
            for(WarehouseReceiptDetail detail : details) {
                detail.setWarehouseReceipt(this);
            }
        }
    }
}
