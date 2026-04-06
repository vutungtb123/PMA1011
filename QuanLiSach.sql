-- ============================================================
-- SCHEMA - SCHOOL LIBRARY SYSTEM (SQL SERVER)
-- Chạy file này trong SSMS để tạo toàn bộ bảng
-- Thứ tự: không có FK → có FK
-- ============================================================

USE SchoolLibraryDB;
GO

-- Xóa bảng cũ nếu tồn tại (theo thứ tự FK ngược)
DROP TABLE IF EXISTS BorrowDetails;
DROP TABLE IF EXISTS BorrowRecords;
DROP TABLE IF EXISTS WaitlistRecords;
DROP TABLE IF EXISTS BookSamplePages;
DROP TABLE IF EXISTS BookCopies;
DROP TABLE IF EXISTS BookCategories;
DROP TABLE IF EXISTS WarehouseReceiptDetails;
DROP TABLE IF EXISTS WarehouseReceipts;
DROP TABLE IF EXISTS Books;
DROP TABLE IF EXISTS Categories;
DROP TABLE IF EXISTS Users;
DROP TABLE IF EXISTS ReaderGroups;
GO

-- ============================================================
-- 1. ReaderGroups
-- ============================================================
CREATE TABLE ReaderGroups (
    GroupID        INT IDENTITY(1,1) PRIMARY KEY,
    GroupName      NVARCHAR(50)  NOT NULL,
    MaxBorrowLimit INT           NOT NULL,
    MaxBorrowDays  INT           NOT NULL
);
GO

-- ============================================================
-- 2. Users
-- ============================================================
CREATE TABLE Users (
    UserID        INT IDENTITY(1,1) PRIMARY KEY,
    GroupID       INT          NULL REFERENCES ReaderGroups(GroupID),
    FullName      NVARCHAR(100) NOT NULL,
    Email         NVARCHAR(100) NOT NULL UNIQUE,
    PasswordHash  NVARCHAR(255) NOT NULL,
    Role          NVARCHAR(20)  NOT NULL DEFAULT 'ROLE_USER',
    OTPCode       NVARCHAR(10)  NULL,
    OTPExpiredAt  DATETIME2     NULL,
    UIPreferences NVARCHAR(MAX) NULL,
    IsActive      BIT           NOT NULL DEFAULT 1,
    CreatedAt     DATETIME2     NULL
);
GO

-- ============================================================
-- 3. Categories
-- ============================================================
CREATE TABLE Categories (
    CategoryID   INT IDENTITY(1,1) PRIMARY KEY,
    CategoryName NVARCHAR(100) NOT NULL UNIQUE,
    Description  NVARCHAR(255) NULL
);
GO

-- ============================================================
-- 4. Books
-- ============================================================
CREATE TABLE Books (
    BookID       INT IDENTITY(1,1) PRIMARY KEY,
    Title        NVARCHAR(200) NOT NULL,
    Author       NVARCHAR(100) NULL,
    Publisher    NVARCHAR(100) NULL,
    CoverImage   NVARCHAR(255) NULL,
    Summary      NVARCHAR(MAX) NULL,
    SampleReadLink NVARCHAR(MAX) NULL,
    CreatedAt    DATETIME2     NULL
);
GO

-- ============================================================
-- 5. BookCategories (Many-to-Many)
-- ============================================================
CREATE TABLE BookCategories (
    BookID     INT NOT NULL REFERENCES Books(BookID),
    CategoryID INT NOT NULL REFERENCES Categories(CategoryID),
    PRIMARY KEY (BookID, CategoryID)
);
GO

-- ============================================================
-- 6. BookCopies
-- ============================================================
CREATE TABLE BookCopies (
    CopyID         INT IDENTITY(1,1) PRIMARY KEY,
    BookID         INT          NOT NULL REFERENCES Books(BookID),
    Barcode        NVARCHAR(50) NOT NULL UNIQUE,
    PhysicalStatus NVARCHAR(50) NULL DEFAULT N'Sẵn sàng'
);
GO

-- ============================================================
-- 7. BookSamplePages
-- ============================================================
CREATE TABLE BookSamplePages (
    PageID     INT IDENTITY(1,1) PRIMARY KEY,
    BookID     INT          NOT NULL REFERENCES Books(BookID),
    PageNumber INT          NOT NULL,
    IsImage    BIT          NOT NULL DEFAULT 0,
    Content    NVARCHAR(MAX) NULL
);
GO

-- ============================================================
-- 8. WarehouseReceipts
-- ============================================================
CREATE TABLE WarehouseReceipts (
    ReceiptID              INT IDENTITY(1,1) PRIMARY KEY,
    ImportDate             DATETIME2      NOT NULL,
    DeclaredTotalQuantity  INT            NOT NULL,
    DeclaredTotalPrice     DECIMAL(18,2)  NOT NULL,
    ActualTotalQuantity    INT            NOT NULL,
    ActualTotalPrice       DECIMAL(18,2)  NOT NULL,
    CreatedAt              DATETIME2      NULL
);
GO

-- ============================================================
-- 9. WarehouseReceiptDetails
-- ============================================================
CREATE TABLE WarehouseReceiptDetails (
    DetailID   INT IDENTITY(1,1) PRIMARY KEY,
    ReceiptID  INT           NOT NULL REFERENCES WarehouseReceipts(ReceiptID),
    BookID     INT           NULL     REFERENCES Books(BookID),
    BookTitle  NVARCHAR(200) NOT NULL,
    Author     NVARCHAR(100) NULL,
    Quantity   INT           NOT NULL,
    Price      DECIMAL(18,2) NOT NULL,
    TotalPrice DECIMAL(18,2) NOT NULL
);
GO

-- ============================================================
-- 10. BorrowRecords
-- ============================================================
CREATE TABLE BorrowRecords (
    BorrowID           INT IDENTITY(1,1) PRIMARY KEY,
    UserID             INT           NOT NULL REFERENCES Users(UserID),
    BorrowDate         DATETIME2     NULL,
    DueDate            DATETIME2     NOT NULL,
    RecordStatus       NVARCHAR(50)  NULL DEFAULT N'Đang mượn',
    CancellationReason NVARCHAR(500) NULL,
    IsReminded         BIT           NULL DEFAULT 0
);
GO

-- ============================================================
-- 11. BorrowDetails
-- ============================================================
CREATE TABLE BorrowDetails (
    DetailID            INT IDENTITY(1,1) PRIMARY KEY,
    BorrowID            INT           NOT NULL REFERENCES BorrowRecords(BorrowID),
    CopyID              INT           NOT NULL REFERENCES BookCopies(CopyID),
    ReturnDate          DATETIME2     NULL,
    ReturnPhysicalState NVARCHAR(50)  NULL,
    ViolationNote       NVARCHAR(500) NULL,
    AssessedFine        DECIMAL(18,2) NULL DEFAULT 0
);
GO

-- ============================================================
-- 12. WaitlistRecords
-- ============================================================
CREATE TABLE WaitlistRecords (
    WaitlistID   INT IDENTITY(1,1) PRIMARY KEY,
    UserID       INT          NOT NULL REFERENCES Users(UserID),
    BookID       INT          NOT NULL REFERENCES Books(BookID),
    RegisterDate DATETIME2    NOT NULL DEFAULT GETDATE(),
    Status       NVARCHAR(50) NULL DEFAULT N'Đang chờ'
);
GO

PRINT 'Tạo toàn bộ bảng thành công!';
