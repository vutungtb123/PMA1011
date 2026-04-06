package com.example.school_library_system.dto;

import java.math.BigDecimal;

public class WarehouseReceiptDetailDto {
    private Integer bookId;    // FK tới Books (bắt buộc khi tạo phiếu mới)
    private String bookTitle;
    private String author;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal totalPrice;

    public Integer getBookId() { return bookId; }
    public void setBookId(Integer bookId) { this.bookId = bookId; }

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
