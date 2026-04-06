package com.example.school_library_system.dto;

import java.util.List;

public class DirectBorrowDto {
    private Integer readerId;
    private Integer borrowDays = 7;
    private List<Integer> bookIds;

    public Integer getReaderId() { return readerId; }
    public void setReaderId(Integer readerId) { this.readerId = readerId; }

    public Integer getBorrowDays() { return borrowDays; }
    public void setBorrowDays(Integer borrowDays) { this.borrowDays = borrowDays; }

    public List<Integer> getBookIds() { return bookIds; }
    public void setBookIds(List<Integer> bookIds) { this.bookIds = bookIds; }
}
