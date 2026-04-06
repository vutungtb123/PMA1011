package com.example.school_library_system.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;


@Entity
@Table(name = "WarehouseReceiptDetails")
public class WarehouseReceiptDetail {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DetailID")
    private Integer detailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ReceiptID", nullable = false)
    @JsonIgnore
    private WarehouseReceipt warehouseReceipt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BookID", nullable = true)
    @JsonIgnore
    private com.example.school_library_system.entity.Book book;

    @Column(name = "BookTitle", nullable = false)
    private String bookTitle;

    @Column(name = "Author")
    private String author;

    @Column(name = "Quantity", nullable = false)
    private Integer quantity;

    @Column(name = "Price", nullable = false)
    private BigDecimal price;

    @Column(name = "TotalPrice", nullable = false)
    private BigDecimal totalPrice;

    // Getters and Setters
    public Integer getDetailId() { return detailId; }
    public void setDetailId(Integer detailId) { this.detailId = detailId; }

    public WarehouseReceipt getWarehouseReceipt() { return warehouseReceipt; }
    public void setWarehouseReceipt(WarehouseReceipt warehouseReceipt) { this.warehouseReceipt = warehouseReceipt; }

    public Book getBook() { return book; }
    public void setBook(Book book) { this.book = book; }

    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }
}
