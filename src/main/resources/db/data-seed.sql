-- ============================================================
-- SEED DATA - SCHOOL LIBRARY SYSTEM (SQL SERVER)
-- Database: SchoolLibraryDB
-- Thứ tự: ReaderGroups → Users → Categories → Books →
--         BookCategories → BookCopies → WarehouseReceipts → WarehouseReceiptDetails
-- ============================================================

USE SchoolLibraryDB;
GO

-- ============================================================
-- Xóa data cũ theo thứ tự FK (tránh lỗi duplicate khi chạy lại)
-- ============================================================
DELETE FROM BorrowDetails;
DELETE FROM BorrowRecords;
DELETE FROM WaitlistRecords;
DELETE FROM BookSamplePages;
DELETE FROM BookCopies;
DELETE FROM BookCategories;
DELETE FROM WarehouseReceiptDetails;
DELETE FROM WarehouseReceipts;
DELETE FROM Books;
DELETE FROM Categories;
DELETE FROM Users;
DELETE FROM ReaderGroups;

-- Reset IDENTITY columns
DBCC CHECKIDENT ('ReaderGroups', RESEED, 0);
DBCC CHECKIDENT ('Users', RESEED, 0);
DBCC CHECKIDENT ('Categories', RESEED, 0);
DBCC CHECKIDENT ('Books', RESEED, 0);
DBCC CHECKIDENT ('WarehouseReceipts', RESEED, 0);
DBCC CHECKIDENT ('WarehouseReceiptDetails', RESEED, 0);
DBCC CHECKIDENT ('BookCopies', RESEED, 0);
GO


-- ============================================================
-- 1. READER GROUPS
-- ============================================================
INSERT INTO ReaderGroups (GroupName, MaxBorrowLimit, MaxBorrowDays) VALUES
(N'Học sinh',   5, 14),
(N'Giáo viên', 10, 30);
GO

-- ============================================================
-- 2. USERS
-- Password bên dưới là BCrypt của "123456"
-- ============================================================
INSERT INTO Users (GroupID, FullName, Email, PasswordHash, Role, IsActive, CreatedAt) VALUES
(NULL, N'Quản trị viên', N'admin@library.com',    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh7i', 'ROLE_ADMIN', 1, GETDATE()),
(1,   N'Nguyễn Văn An', N'an.nguyen@school.edu',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh7i', 'ROLE_USER',  1, GETDATE()),
(1,   N'Trần Thị Bình', N'binh.tran@school.edu',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh7i', 'ROLE_USER',  1, GETDATE()),
(1,   N'Lê Minh Châu',  N'chau.le@school.edu',    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh7i', 'ROLE_USER',  1, GETDATE());
GO

-- ============================================================
-- 3. CATEGORIES (15 thể loại)
-- ============================================================
INSERT INTO Categories (CategoryName, Description) VALUES
(N'Văn học Việt Nam',        N'Các tác phẩm văn học của các nhà văn Việt Nam'),
(N'Văn học nước ngoài',      N'Các tác phẩm văn học dịch từ nước ngoài'),
(N'Khoa học tự nhiên',       N'Vật lý, Hóa học, Sinh học, Toán học'),
(N'Khoa học xã hội',         N'Lịch sử, Địa lý, Tâm lý học'),
(N'Kỹ năng sống',            N'Sách phát triển bản thân và kỹ năng mềm'),
(N'Lịch sử - Địa lý',       N'Lịch sử Việt Nam và thế giới, địa lý'),
(N'Toán học',                N'Giáo trình và bài tập toán các cấp'),
(N'Tin học - Công nghệ',     N'Lập trình, công nghệ thông tin'),
(N'Triết học',               N'Triết học Đông - Tây'),
(N'Kinh tế',                 N'Kinh tế học, tài chính, quản trị'),
(N'Thiếu nhi',               N'Truyện tranh và sách cho trẻ em'),
(N'Khoa học viễn tưởng',     N'Sci-fi và fantasy'),
(N'Tâm lý - Giáo dục',      N'Sách tâm lý và phương pháp giáo dục'),
(N'Sức khỏe - Y học',        N'Dinh dưỡng, y học phổ thông'),
(N'Nghệ thuật - Âm nhạc',   N'Hội họa, âm nhạc, điện ảnh');
GO

-- ============================================================
-- Books 1-10
INSERT INTO Books (Title, Author, Publisher, Summary, CreatedAt) VALUES
(N'Dế Mèn Phiêu Lưu Ký', N'Tô Hoài', N'NXB Kim Đồng', N'Cuộc phiêu lưu ly kỳ của chú dế mèn dũng cảm qua nhiều vùng đất lạ.', GETDATE()),
(N'Số Đỏ', N'Vũ Trọng Phụng', N'NXB Hội Nhà Văn', N'Tiểu thuyết châm biếm xã hội Việt Nam thời Pháp thuộc.', GETDATE()),
(N'Chí Phèo', N'Nam Cao', N'NXB Văn Học', N'Câu chuyện bi thảm về người nông dân bị xã hội đẩy vào con đường tội lỗi.', GETDATE()),
(N'Tắt Đèn', N'Ngô Tất Tố', N'NXB Văn Học', N'Hình ảnh người nông dân Việt Nam trước Cách mạng tháng Tám.', GETDATE()),
(N'Truyện Kiều', N'Nguyễn Du', N'NXB Giáo Dục', N'Áng thơ bất hủ của nền văn học Việt Nam viết về số phận người phụ nữ.', GETDATE()),
(N'Người Lái Đò Sông Đà', N'Nguyễn Tuân', N'NXB Văn Học', N'Tùy bút về vẻ đẹp hùng vĩ của thiên nhiên Tây Bắc và người lao động.', GETDATE()),
(N'Làng', N'Kim Lân', N'NXB Văn Học', N'Tình yêu làng quê và lòng yêu nước của người nông dân trong kháng chiến.', GETDATE()),
(N'Hai Đứa Trẻ', N'Thạch Lam', N'NXB Hội Nhà Văn', N'Bức tranh buồn về cuộc sống nơi phố huyện nghèo.', GETDATE()),
(N'Chiến Tranh và Hòa Bình', N'Leo Tolstoy', N'NXB Văn Học', N'Sử thi vĩ đại về nước Nga trong cuộc chiến tranh Napoleon.', GETDATE()),
(N'Những Người Khốn Khổ', N'Victor Hugo', N'NXB Văn Học', N'Câu chuyện về lòng nhân ái và công lý trong xã hội Pháp thế kỷ 19.', GETDATE());
GO

-- Books 11-20
INSERT INTO Books (Title, Author, Publisher, Summary, CreatedAt) VALUES
(N'Tội Ác và Hình Phạt', N'Fyodor Dostoevsky', N'NXB Văn Học', N'Cuộc đấu tranh tâm lý gay gắt của một sinh viên nghèo sau khi phạm tội.', GETDATE()),
(N'Bắt Trẻ Đồng Xanh', N'J.D. Salinger', N'NXB Hội Nhà Văn', N'Hành trình tìm kiếm bản thân của một thiếu niên nổi loạn.', GETDATE()),
(N'Nhà Giả Kim', N'Paulo Coelho', N'NXB Văn Học', N'Hành trình theo đuổi giấc mơ và tìm kiếm kho báu của chàng trai trẻ Santiago.', GETDATE()),
(N'Hoàng Tử Bé', N'Antoine de Saint-Exupery', N'NXB Kim Đồng', N'Câu chuyện triết học nhẹ nhàng về tình bạn và ý nghĩa cuộc sống.', GETDATE()),
(N'Đắc Nhân Tâm', N'Dale Carnegie', N'NXB Tổng Hợp', N'Nghệ thuật thu phục lòng người và xây dựng mối quan hệ thành công.', GETDATE()),
(N'Lược Sử Thời Gian', N'Stephen Hawking', N'NXB Trẻ', N'Giải thích các khái niệm vật lý hiện đại cho người đọc phổ thông.', GETDATE()),
(N'Sapiens: Lược Sử Loài Người', N'Yuval Noah Harari', N'NXB Tri Thức', N'Lịch sử phát triển của loài người từ thời nguyên thủy đến hiện đại.', GETDATE()),
(N'Vũ Trụ Trong Vỏ Hạt Dẻ', N'Stephen Hawking', N'NXB Trẻ', N'Khám phá các lý thuyết vật lý tiên tiến qua ngôn ngữ đại chúng.', GETDATE()),
(N'Nguồn Gốc Các Loài', N'Charles Darwin', N'NXB Giáo Dục', N'Lý thuyết tiến hóa nổi tiếng nhất trong lịch sử khoa học.', GETDATE()),
(N'Toán Học Cho Mọi Người', N'John Allen Paulos', N'NXB Giáo Dục', N'Giúp người đọc hiểu và yêu thích toán học trong cuộc sống hàng ngày.', GETDATE());
GO

-- Books 21-30
INSERT INTO Books (Title, Author, Publisher, Summary, CreatedAt) VALUES
(N'Việt Nam Sử Lược', N'Trần Trọng Kim', N'NXB Tổng Hợp', N'Cái nhìn tổng quan về lịch sử Việt Nam từ thời dựng nước đến đầu thế kỷ 20.', GETDATE()),
(N'Đất Rừng Phương Nam', N'Đoàn Giỏi', N'NXB Kim Đồng', N'Cuộc phiêu lưu của cậu bé An trên vùng đất Nam Bộ thời kháng chiến.', GETDATE()),
(N'Lịch Sử Thế Giới Cổ Đại', N'Vũ Dương Ninh', N'NXB Giáo Dục', N'Lịch sử các nền văn minh cổ đại trên thế giới.', GETDATE()),
(N'Hồ Chí Minh Toàn Tập', N'Hồ Chí Minh', N'NXB Chính Trị QG', N'Tuyển tập các bài viết và bài nói của Chủ tịch Hồ Chí Minh.', GETDATE()),
(N'Thói Quen Thứ 7', N'Stephen Covey', N'NXB Tổng Hợp', N'Bảy thói quen của người thành đạt - công thức sống hiệu quả.', GETDATE()),
(N'Nghĩ Giàu Làm Giàu', N'Napoleon Hill', N'NXB Lao Động', N'Nguyên tắc thành công tài chính dựa trên tư duy tích cực.', GETDATE()),
(N'Atomic Habits', N'James Clear', N'NXB Hà Nội', N'Phương pháp xây dựng thói quen tốt và từ bỏ thói quen xấu.', GETDATE()),
(N'Mindset - Tâm Lý Học Thành Công', N'Carol S. Dweck', N'NXB Lao Động', N'Sự khác biệt giữa tư duy cố định và tư duy phát triển.', GETDATE()),
(N'Lập Trình Python Cơ Bản', N'Eric Matthes', N'NXB Thông Tin TT', N'Hướng dẫn lập trình Python từ cơ bản đến nâng cao với bài tập thực hành.', GETDATE()),
(N'Clean Code', N'Robert C. Martin', N'NXB Thông Tin TT', N'Nghệ thuật viết code sạch, dễ đọc và dễ bảo trì.', GETDATE());
GO

-- Books 31-40
INSERT INTO Books (Title, Author, Publisher, Summary, CreatedAt) VALUES
(N'Cấu Trúc Dữ Liệu và Giải Thuật', N'Thomas H. Cormen', N'NXB Giáo Dục', N'Giáo trình kinh điển về cấu trúc dữ liệu và thuật toán.', GETDATE()),
(N'Học Java Qua Ví Dụ', N'Nguyễn Văn Hiệp', N'NXB Thông Tin TT', N'Học lập trình Java từng bước qua các ví dụ thực tế.', GETDATE()),
(N'Harry Potter và Hòn Đá Phù Thủy', N'J.K. Rowling', N'NXB Trẻ', N'Cuộc phiêu lưu của cậu bé phù thủy Harry Potter tại trường Hogwarts.', GETDATE()),
(N'Thám Tử Conan - Tập 1', N'Gosho Aoyama', N'NXB Kim Đồng', N'Câu chuyện phá án ly kỳ của thám tử nhí Conan Edogawa.', GETDATE()),
(N'Doraemon - Tập 1', N'Fujiko F. Fujio', N'NXB Kim Đồng', N'Những câu chuyện vui nhộn của mèo máy Doraemon và cậu bé Nobita.', GETDATE()),
(N'Cậu Bé Mang Tên Nhân Vật', N'Nguyễn Nhật Ánh', N'NXB Trẻ', N'Cuốn sách thiếu nhi đầy màu sắc về tình bạn và ước mơ thuở nhỏ.', GETDATE()),
(N'Tuổi Thơ Dữ Dội', N'Phùng Quán', N'NXB Kim Đồng', N'Câu chuyện về những cậu bé thiếu sinh quân dũng cảm trong kháng chiến.', GETDATE()),
(N'Mắt Biếc', N'Nguyễn Nhật Ánh', N'NXB Trẻ', N'Chuyện tình cảm động về tình yêu học trò và những ký ức tuổi thơ.', GETDATE()),
(N'Cho Tôi Xin Một Vé Đi Tuổi Thơ', N'Nguyễn Nhật Ánh', N'NXB Trẻ', N'Hành trình trở về tuổi thơ đầy tiếng cười và kỷ niệm đẹp.', GETDATE()),
(N'Tôi Thấy Hoa Vàng Trên Cỏ Xanh', N'Nguyễn Nhật Ánh', N'NXB Trẻ', N'Câu chuyện về tình anh em và tình yêu thương ở làng quê Việt Nam.', GETDATE());
GO

-- Books 41-50
INSERT INTO Books (Title, Author, Publisher, Summary, CreatedAt) VALUES
(N'Dune - Xứ Cát', N'Frank Herbert', N'NXB Trẻ', N'Sử thi viễn tưởng vĩ đại về hành tinh sa mạc và cuộc đấu tranh quyền lực.', GETDATE()),
(N'Ender''s Game', N'Orson Scott Card', N'NXB Trẻ', N'Cậu bé thiên tài được đào tạo để chống lại sự xâm lược của người ngoài hành tinh.', GETDATE()),
(N'Cha Giàu Cha Nghèo', N'Robert Kiyosaki', N'NXB Trẻ', N'Bài học về tài chính và đầu tư từ hai người cha có quan điểm đối lập.', GETDATE()),
(N'Kinh Tế Học Hài Hước', N'Steven Levitt', N'NXB Trẻ', N'Khám phá kinh tế học ẩn sau các hiện tượng xã hội thú vị.', GETDATE()),
(N'Dinh Dưỡng Thông Minh', N'Michael Greger', N'NXB Tổng Hợp', N'Hướng dẫn dinh dưỡng dựa trên bằng chứng khoa học.', GETDATE()),
(N'Ngủ Đủ Giấc', N'Matthew Walker', N'NXB Lao Động', N'Tại sao giấc ngủ quan trọng và cách ngủ đúng cách để có sức khỏe tốt.', GETDATE()),
(N'Câu Chuyện Âm Nhạc', N'Howard Goodall', N'NXB Trẻ', N'Lịch sử phát triển âm nhạc từ cổ đại đến hiện đại.', GETDATE()),
(N'Nghệ Thuật Hội Họa', N'Ernst Gombrich', N'NXB Mỹ Thuật', N'Lịch sử hội họa toàn cầu từ thời tiền sử đến nghệ thuật đương đại.', GETDATE()),
(N'Thế Giới Của Sophie', N'Jostein Gaarder', N'NXB Hội Nhà Văn', N'Lịch sử triết học phương Tây qua câu chuyện cô bé Sophie.', GETDATE()),
(N'Đạo Đức Kinh', N'Lão Tử', N'NXB Văn Học', N'Triết lý sống thuận theo tự nhiên của Đạo Gia.');
GO



-- ============================================================
-- 5. BOOK CATEGORIES (quan hệ nhiều-nhiều)
-- ============================================================
INSERT INTO BookCategories (BookID, CategoryID) VALUES
-- Dế Mèn Phiêu Lưu Ký: Văn học VN + Thiếu nhi
(1,1),(1,11),
-- Số Đỏ: Văn học VN
(2,1),
-- Chí Phèo: Văn học VN
(3,1),
-- Tắt Đèn: Văn học VN + Khoa học xã hội
(4,1),(4,4),
-- Truyện Kiều: Văn học VN + Nghệ thuật
(5,1),(5,15),
-- Người Lái Đò Sông Đà: Văn học VN + Lịch sử
(6,1),(6,6),
-- Làng: Văn học VN
(7,1),
-- Hai Đứa Trẻ: Văn học VN + Tâm lý
(8,1),(8,13),
-- Chiến Tranh và Hòa Bình: Văn học nước ngoài + Lịch sử
(9,2),(9,6),
-- Những Người Khốn Khổ: Văn học nước ngoài + Khoa học xã hội
(10,2),(10,4),
-- Tội Ác và Hình Phạt: Văn học nước ngoài + Tâm lý
(11,2),(11,13),
-- Bắt Trẻ Đồng Xanh: Văn học nước ngoài + Tâm lý
(12,2),(12,13),
-- Nhà Giả Kim: Văn học nước ngoài + Kỹ năng sống
(13,2),(13,5),
-- Hoàng Tử Bé: Văn học nước ngoài + Thiếu nhi
(14,2),(14,11),
-- Đắc Nhân Tâm: Kỹ năng sống + Tâm lý
(15,5),(15,13),
-- Lược Sử Thời Gian: Khoa học tự nhiên
(16,3),
-- Sapiens: Khoa học xã hội + Lịch sử
(17,4),(17,6),
-- Vũ Trụ Trong Vỏ Hạt Dẻ: Khoa học tự nhiên
(18,3),
-- Nguồn Gốc Các Loài: Khoa học tự nhiên + Khoa học xã hội
(19,3),(19,4),
-- Toán Học Cho Mọi Người: Toán học + Khoa học tự nhiên
(20,7),(20,3),
-- Việt Nam Sử Lược: Lịch sử + Khoa học xã hội
(21,6),(21,4),
-- Đất Rừng Phương Nam: Văn học VN + Thiếu nhi
(22,1),(22,11),
-- Lịch Sử Thế Giới Cổ Đại: Lịch sử
(23,6),
-- Hồ Chí Minh Toàn Tập: Lịch sử + Khoa học xã hội
(24,6),(24,4),
-- Thói Quen Thứ 7: Kỹ năng sống
(25,5),
-- Nghĩ Giàu Làm Giàu: Kỹ năng sống + Kinh tế
(26,5),(26,10),
-- Atomic Habits: Kỹ năng sống + Tâm lý
(27,5),(27,13),
-- Mindset: Tâm lý + Kỹ năng sống
(28,13),(28,5),
-- Lập Trình Python: Tin học
(29,8),
-- Clean Code: Tin học
(30,8),
-- Cấu Trúc Dữ Liệu và Giải Thuật: Tin học + Toán học
(31,8),(31,7),
-- Học Java Qua Ví Dụ: Tin học
(32,8),
-- Harry Potter: Thiếu nhi + Khoa học viễn tưởng
(33,11),(33,12),
-- Thám Tử Conan: Thiếu nhi
(34,11),
-- Doraemon: Thiếu nhi
(35,11),
-- Cậu Bé Mang Tên Nhân Vật: Thiếu nhi + Văn học VN
(36,11),(36,1),
-- Tuổi Thơ Dữ Dội: Văn học VN + Lịch sử
(37,1),(37,6),
-- Mắt Biếc: Văn học VN + Tâm lý
(38,1),(38,13),
-- Cho Tôi Xin Một Vé Đi Tuổi Thơ: Văn học VN + Thiếu nhi
(39,1),(39,11),
-- Tôi Thấy Hoa Vàng Trên Cỏ Xanh: Văn học VN + Thiếu nhi
(40,1),(40,11),
-- Dune - Xứ Cát: Khoa học viễn tưởng
(41,12),
-- Ender's Game: Khoa học viễn tưởng + Tâm lý
(42,12),(42,13),
-- Cha Giàu Cha Nghèo: Kinh tế + Kỹ năng sống
(43,10),(43,5),
-- Kinh Tế Học Hài Hước: Kinh tế + Khoa học xã hội
(44,10),(44,4),
-- Dinh Dưỡng Thông Minh: Sức khỏe + Khoa học tự nhiên
(45,14),(45,3),
-- Ngủ Đủ Giấc: Sức khỏe
(46,14),
-- Câu Chuyện Âm Nhạc: Nghệ thuật + Lịch sử
(47,15),(47,6),
-- Nghệ Thuật Hội Họa: Nghệ thuật
(48,15),
-- Thế Giới Của Sophie: Triết học + Tâm lý
(49,9),(49,13),
-- Đạo Đức Kinh: Triết học
(50,9);
GO

-- ============================================================
-- 6. WAREHOUSE RECEIPTS (3 phiếu nhập kho)
-- ============================================================
INSERT INTO WarehouseReceipts (ImportDate, DeclaredTotalQuantity, DeclaredTotalPrice, ActualTotalQuantity, ActualTotalPrice, CreatedAt) VALUES
('2025-01-10 08:00:00', 100, 8500000.00, 100, 8500000.00, '2025-01-10 08:00:00'),
('2025-03-15 09:00:00',  80, 6200000.00,  80, 6200000.00, '2025-03-15 09:00:00'),
('2025-06-20 10:00:00',  70, 5950000.00,  70, 5950000.00, '2025-06-20 10:00:00');
GO

-- ============================================================
-- 7. WAREHOUSE RECEIPT DETAILS
-- Phiếu 1: sách 1-20 | Phiếu 2: sách 21-35 | Phiếu 3: sách 36-50
-- ============================================================
INSERT INTO WarehouseReceiptDetails (ReceiptID, BookID, BookTitle, Author, Quantity, Price, TotalPrice) VALUES
-- Phiếu nhập 1
(1, 1,  N'Dế Mèn Phiêu Lưu Ký',            N'Tô Hoài',               5, 65000.00,   325000.00),
(1, 2,  N'Số Đỏ',                            N'Vũ Trọng Phụng',        3, 75000.00,   225000.00),
(1, 3,  N'Chí Phèo',                         N'Nam Cao',               4, 60000.00,   240000.00),
(1, 4,  N'Tắt Đèn',                          N'Ngô Tất Tố',            4, 60000.00,   240000.00),
(1, 5,  N'Truyện Kiều',                      N'Nguyễn Du',             5, 80000.00,   400000.00),
(1, 6,  N'Người Lái Đò Sông Đà',            N'Nguyễn Tuân',           3, 70000.00,   210000.00),
(1, 7,  N'Làng',                             N'Kim Lân',               4, 55000.00,   220000.00),
(1, 8,  N'Hai Đứa Trẻ',                     N'Thạch Lam',             4, 55000.00,   220000.00),
(1, 9,  N'Chiến Tranh và Hòa Bình',         N'Leo Tolstoy',           3, 195000.00,  585000.00),
(1, 10, N'Những Người Khốn Khổ',            N'Victor Hugo',           3, 180000.00,  540000.00),
(1, 11, N'Tội Ác và Hình Phạt',             N'Fyodor Dostoevsky',     3, 150000.00,  450000.00),
(1, 12, N'Bắt Trẻ Đồng Xanh',              N'J.D. Salinger',         3, 120000.00,  360000.00),
(1, 13, N'Nhà Giả Kim',                     N'Paulo Coelho',          5, 95000.00,   475000.00),
(1, 14, N'Hoàng Tử Bé',                     N'Antoine de Saint-Exupéry', 5, 75000.00, 375000.00),
(1, 15, N'Đắc Nhân Tâm',                    N'Dale Carnegie',         5, 105000.00,  525000.00),
(1, 16, N'Lược Sử Thời Gian',               N'Stephen Hawking',       3, 130000.00,  390000.00),
(1, 17, N'Sapiens: Lược Sử Loài Người',     N'Yuval Noah Harari',     4, 135000.00,  540000.00),
(1, 18, N'Vũ Trụ Trong Vỏ Hạt Dẻ',         N'Stephen Hawking',       3, 130000.00,  390000.00),
(1, 19, N'Nguồn Gốc Các Loài',              N'Charles Darwin',        3, 120000.00,  360000.00),
(1, 20, N'Toán Học Cho Mọi Người',          N'John Allen Paulos',     3, 100000.00,  300000.00),
-- Phiếu nhập 2
(2, 21, N'Việt Nam Sử Lược',               N'Trần Trọng Kim',        4, 90000.00,   360000.00),
(2, 22, N'Đất Rừng Phương Nam',             N'Đoàn Giỏi',            5, 70000.00,   350000.00),
(2, 23, N'Lịch Sử Thế Giới Cổ Đại',        N'Vũ Dương Ninh',         3, 110000.00,  330000.00),
(2, 24, N'Hồ Chí Minh Toàn Tập',           N'Hồ Chí Minh',          3, 250000.00,  750000.00),
(2, 25, N'Thói Quen Thứ 7',                 N'Stephen Covey',         5, 115000.00,  575000.00),
(2, 26, N'Nghĩ Giàu Làm Giàu',             N'Napoleon Hill',         5, 95000.00,   475000.00),
(2, 27, N'Atomic Habits',                    N'James Clear',           5, 125000.00,  625000.00),
(2, 28, N'Mindset - Tâm Lý Học Thành Công', N'Carol S. Dweck',        4, 110000.00,  440000.00),
(2, 29, N'Lập Trình Python Cơ Bản',         N'Eric Matthes',          3, 175000.00,  525000.00),
(2, 30, N'Clean Code',                       N'Robert C. Martin',      3, 180000.00,  540000.00),
(2, 31, N'Cấu Trúc Dữ Liệu và Giải Thuật', N'Thomas H. Cormen',      3, 210000.00,  630000.00),
(2, 32, N'Học Java Qua Ví Dụ',              N'Nguyễn Văn Hiệp',      4, 120000.00,  480000.00),
(2, 33, N'Harry Potter và Hòn Đá Phù Thủy', N'J.K. Rowling',          5, 130000.00,  650000.00),
(2, 34, N'Thám Tử Conan - Tập 1',           N'Gosho Aoyama',          5, 55000.00,   275000.00),
(2, 35, N'Doraemon - Tập 1',                N'Fujiko F. Fujio',       5, 45000.00,   225000.00),
-- Phiếu nhập 3
(3, 36, N'Cậu Bé Mang Tên Nhân Vật',       N'Nguyễn Nhật Ánh',      4, 80000.00,   320000.00),
(3, 37, N'Tuổi Thơ Dữ Dội',                 N'Phùng Quán',           4, 75000.00,   300000.00),
(3, 38, N'Mắt Biếc',                        N'Nguyễn Nhật Ánh',      5, 85000.00,   425000.00),
(3, 39, N'Cho Tôi Xin Một Vé Đi Tuổi Thơ', N'Nguyễn Nhật Ánh',      5, 80000.00,   400000.00),
(3, 40, N'Tôi Thấy Hoa Vàng Trên Cỏ Xanh', N'Nguyễn Nhật Ánh',      5, 85000.00,   425000.00),
(3, 41, N'Dune - Xứ Cát',                   N'Frank Herbert',         3, 165000.00,  495000.00),
(3, 42, N'Ender''s Game',                    N'Orson Scott Card',      3, 140000.00,  420000.00),
(3, 43, N'Cha Giàu Cha Nghèo',              N'Robert Kiyosaki',       5, 110000.00,  550000.00),
(3, 44, N'Kinh Tế Học Hài Hước',            N'Steven Levitt',         4, 115000.00,  460000.00),
(3, 45, N'Dinh Dưỡng Thông Minh',           N'Michael Greger',        3, 120000.00,  360000.00),
(3, 46, N'Ngủ Đủ Giấc',                     N'Matthew Walker',        4, 125000.00,  500000.00),
(3, 47, N'Câu Chuyện Âm Nhạc',              N'Howard Goodall',        3, 130000.00,  390000.00),
(3, 48, N'Nghệ Thuật Hội Họa',              N'Ernst Gombrich',        3, 145000.00,  435000.00),
(3, 49, N'Thế Giới Của Sophie',             N'Jostein Gaarder',       4, 120000.00,  480000.00),
(3, 50, N'Đạo Đức Kinh',                    N'Lão Tử',               3, 65000.00,   195000.00);
GO

-- ============================================================
-- 8. BOOK COPIES (bản sao sách vật lý)
-- ============================================================
INSERT INTO BookCopies (BookID, Barcode, PhysicalStatus) VALUES
(1,  'BC-001-01', N'Sẵn sàng'), (1,  'BC-001-02', N'Sẵn sàng'),
(2,  'BC-002-01', N'Sẵn sàng'), (2,  'BC-002-02', N'Sẵn sàng'),
(3,  'BC-003-01', N'Sẵn sàng'), (3,  'BC-003-02', N'Sẵn sàng'),
(4,  'BC-004-01', N'Sẵn sàng'), (4,  'BC-004-02', N'Sẵn sàng'),
(5,  'BC-005-01', N'Sẵn sàng'), (5,  'BC-005-02', N'Sẵn sàng'),
(6,  'BC-006-01', N'Sẵn sàng'), (6,  'BC-006-02', N'Sẵn sàng'),
(7,  'BC-007-01', N'Sẵn sàng'), (7,  'BC-007-02', N'Sẵn sàng'),
(8,  'BC-008-01', N'Sẵn sàng'), (8,  'BC-008-02', N'Sẵn sàng'),
(9,  'BC-009-01', N'Sẵn sàng'), (9,  'BC-009-02', N'Sẵn sàng'),
(10, 'BC-010-01', N'Sẵn sàng'), (10, 'BC-010-02', N'Sẵn sàng'),
(11, 'BC-011-01', N'Sẵn sàng'), (11, 'BC-011-02', N'Sẵn sàng'),
(12, 'BC-012-01', N'Sẵn sàng'),
(13, 'BC-013-01', N'Sẵn sàng'), (13, 'BC-013-02', N'Sẵn sàng'), (13, 'BC-013-03', N'Sẵn sàng'),
(14, 'BC-014-01', N'Sẵn sàng'), (14, 'BC-014-02', N'Sẵn sàng'),
(15, 'BC-015-01', N'Sẵn sàng'), (15, 'BC-015-02', N'Sẵn sàng'), (15, 'BC-015-03', N'Sẵn sàng'),
(16, 'BC-016-01', N'Sẵn sàng'), (16, 'BC-016-02', N'Sẵn sàng'),
(17, 'BC-017-01', N'Sẵn sàng'), (17, 'BC-017-02', N'Sẵn sàng'),
(18, 'BC-018-01', N'Sẵn sàng'),
(19, 'BC-019-01', N'Sẵn sàng'), (19, 'BC-019-02', N'Sẵn sàng'),
(20, 'BC-020-01', N'Sẵn sàng'),
(21, 'BC-021-01', N'Sẵn sàng'), (21, 'BC-021-02', N'Sẵn sàng'),
(22, 'BC-022-01', N'Sẵn sàng'), (22, 'BC-022-02', N'Sẵn sàng'), (22, 'BC-022-03', N'Sẵn sàng'),
(23, 'BC-023-01', N'Sẵn sàng'),
(24, 'BC-024-01', N'Sẵn sàng'), (24, 'BC-024-02', N'Sẵn sàng'),
(25, 'BC-025-01', N'Sẵn sàng'), (25, 'BC-025-02', N'Sẵn sàng'),
(26, 'BC-026-01', N'Sẵn sàng'), (26, 'BC-026-02', N'Sẵn sàng'),
(27, 'BC-027-01', N'Sẵn sàng'), (27, 'BC-027-02', N'Sẵn sàng'), (27, 'BC-027-03', N'Sẵn sàng'),
(28, 'BC-028-01', N'Sẵn sàng'),
(29, 'BC-029-01', N'Sẵn sàng'), (29, 'BC-029-02', N'Sẵn sàng'),
(30, 'BC-030-01', N'Sẵn sàng'),
(31, 'BC-031-01', N'Sẵn sàng'), (31, 'BC-031-02', N'Sẵn sàng'),
(32, 'BC-032-01', N'Sẵn sàng'), (32, 'BC-032-02', N'Sẵn sàng'),
(33, 'BC-033-01', N'Sẵn sàng'), (33, 'BC-033-02', N'Sẵn sàng'), (33, 'BC-033-03', N'Sẵn sàng'),
(34, 'BC-034-01', N'Sẵn sàng'), (34, 'BC-034-02', N'Sẵn sàng'),
(35, 'BC-035-01', N'Sẵn sàng'), (35, 'BC-035-02', N'Sẵn sàng'),
(36, 'BC-036-01', N'Sẵn sàng'),
(37, 'BC-037-01', N'Sẵn sàng'), (37, 'BC-037-02', N'Sẵn sàng'),
(38, 'BC-038-01', N'Sẵn sàng'), (38, 'BC-038-02', N'Sẵn sàng'), (38, 'BC-038-03', N'Sẵn sàng'),
(39, 'BC-039-01', N'Sẵn sàng'), (39, 'BC-039-02', N'Sẵn sàng'),
(40, 'BC-040-01', N'Sẵn sàng'), (40, 'BC-040-02', N'Sẵn sàng'), (40, 'BC-040-03', N'Sẵn sàng'),
(41, 'BC-041-01', N'Sẵn sàng'),
(42, 'BC-042-01', N'Sẵn sàng'), (42, 'BC-042-02', N'Sẵn sàng'),
(43, 'BC-043-01', N'Sẵn sàng'), (43, 'BC-043-02', N'Sẵn sàng'), (43, 'BC-043-03', N'Sẵn sàng'),
(44, 'BC-044-01', N'Sẵn sàng'), (44, 'BC-044-02', N'Sẵn sàng'),
(45, 'BC-045-01', N'Sẵn sàng'),
(46, 'BC-046-01', N'Sẵn sàng'), (46, 'BC-046-02', N'Sẵn sàng'),
(47, 'BC-047-01', N'Sẵn sàng'),
(48, 'BC-048-01', N'Sẵn sàng'), (48, 'BC-048-02', N'Sẵn sàng'),
(49, 'BC-049-01', N'Sẵn sàng'), (49, 'BC-049-02', N'Sẵn sàng'),
(50, 'BC-050-01', N'Sẵn sàng');
GO

-- Bật lại FK constraints
EXEC sp_MSforeachtable 'ALTER TABLE ? WITH CHECK CHECK CONSTRAINT ALL';
GO

PRINT 'Seed data inserted successfully!';
