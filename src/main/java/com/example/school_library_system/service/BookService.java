package com.example.school_library_system.service;

import com.example.school_library_system.entity.Book;
import com.example.school_library_system.entity.BookSamplePage;
import com.example.school_library_system.repository.BookRepository;
import com.example.school_library_system.repository.BookSamplePageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;

@Service
public class BookService {

    @Autowired
    private BookRepository bookRepository;
    
    @Autowired
    private BookSamplePageRepository samplePageRepository;

    @Autowired
    private FileStorageService fileStorageService;

    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    public Book getBookById(Integer id) {
        if (id == null) return null;
        return bookRepository.findById(id).orElse(null);
    }

    @Transactional
    public Book saveBook(Book book, MultipartFile coverImageFile) {
        if (coverImageFile != null && !coverImageFile.isEmpty()) {
            // Nếu đang edit (book đã có ID) và có ảnh bìa cũ → xóa ảnh cũ trước
            Integer bookId = book.getBookId();
            if (bookId != null) {
                Book existing = bookRepository.findById(bookId).orElse(null);
                if (existing != null && existing.getCoverImage() != null) {
                    fileStorageService.deleteFile(existing.getCoverImage());
                }
            }
            String imageUrl = fileStorageService.storeFile(coverImageFile, "books");
            book.setCoverImage(imageUrl);
        }
        return bookRepository.save(Objects.requireNonNull(book, "Book must not be null"));
    }

    @Transactional
    public void deleteBook(Integer id) {
        if (id == null) return;
        Book book = bookRepository.findById(id).orElse(null);
        if (book == null) return;

        // 1. Xóa ảnh bìa sách cũ khỏi đĩa
        if (book.getCoverImage() != null) {
            fileStorageService.deleteFile(book.getCoverImage());
        }

        // 2. Xóa toàn bộ ảnh của các trang đọc thử thuộc sách này
        List<BookSamplePage> pages = samplePageRepository.findByBookBookIdOrderByPageNumberAsc(id);
        for (BookSamplePage page : pages) {
            if (Boolean.TRUE.equals(page.getIsImage()) && page.getContent() != null) {
                fileStorageService.deleteFile(page.getContent());
            }
        }

        // 3. Xóa bản ghi DB (cascade sẽ xóa pages + copies)
        bookRepository.deleteById(Objects.requireNonNull(id));
    }

    // -- Sample Pages --
    public List<BookSamplePage> getSamplePages(Integer bookId) {
        return samplePageRepository.findByBookBookIdOrderByPageNumberAsc(bookId);
    }

    @Transactional
    public void saveSamplePage(Integer bookId, Integer pageNumber, Boolean isImage, String contentText, MultipartFile imageFile) {
        Book book = getBookById(bookId);
        if (book == null) return;
        
        List<BookSamplePage> existingPages = getSamplePages(bookId);
        BookSamplePage page = existingPages.stream()
                .filter(p -> p.getPageNumber().equals(pageNumber))
                .findFirst()
                .orElse(new BookSamplePage());

        page.setBook(book);
        page.setPageNumber(pageNumber);
        page.setIsImage(isImage);

        if (isImage) {
            if (imageFile != null && !imageFile.isEmpty()) {
                // Nếu trang này đã có ảnh cũ → xóa ảnh cũ trước khi lưu ảnh mới
                if (Boolean.TRUE.equals(page.getIsImage()) && page.getContent() != null) {
                    fileStorageService.deleteFile(page.getContent());
                }
                String imgUrl = fileStorageService.storeFile(imageFile, "books");
                page.setContent(imgUrl);
            }
            // Nếu không upload file mới thì giữ nguyên ảnh cũ
        } else {
            // Chuyển từ ảnh → text: xóa file ảnh cũ nếu có
            if (Boolean.TRUE.equals(page.getIsImage()) && page.getContent() != null) {
                fileStorageService.deleteFile(page.getContent());
            }
            page.setContent(contentText);
        }

        samplePageRepository.save(page);
    }

    @Transactional
    public void deleteSamplePage(Integer pageId) {
        if (pageId == null) return;
        samplePageRepository.findById(pageId).ifPresent(page -> {
            // Nếu trang là ảnh → xóa file ảnh khỏi đĩa trước
            if (Boolean.TRUE.equals(page.getIsImage()) && page.getContent() != null) {
                fileStorageService.deleteFile(page.getContent());
            }
            samplePageRepository.deleteById(Objects.requireNonNull(pageId));
        });
    }
}
