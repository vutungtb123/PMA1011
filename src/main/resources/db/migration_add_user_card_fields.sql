-- ============================================================
-- MIGRATION: Thêm cột thẻ thư viện vào bảng Users
-- Chạy trong SSMS sau khi schema đã tồn tại
-- (ddl-auto=none → phải chạy thủ công)
-- ============================================================

USE SchoolLibraryDB;
GO

-- Thêm cột mã học sinh (duy nhất)
IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
               WHERE TABLE_NAME = 'Users' AND COLUMN_NAME = 'StudentId')
BEGIN
    ALTER TABLE Users ADD StudentId NVARCHAR(50) NULL;
    PRINT 'Added column: StudentId';
END
GO

-- Tạo filtered unique index cho StudentId (bỏ qua NULL, chỉ enforce unique trên giá trị thực)
-- NOTE: SQL Server không cho phép UNIQUE CONSTRAINT khi có nhiều NULL → dùng filtered index
IF NOT EXISTS (SELECT 1 FROM sys.indexes
               WHERE name = 'UIX_Users_StudentId' AND object_id = OBJECT_ID('Users'))
BEGIN
    CREATE UNIQUE INDEX UIX_Users_StudentId
        ON Users(StudentId)
        WHERE StudentId IS NOT NULL;
    PRINT 'Added filtered unique index: UIX_Users_StudentId';
END
GO

-- Thêm cột ảnh thẻ học sinh
IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
               WHERE TABLE_NAME = 'Users' AND COLUMN_NAME = 'PhotoUrl')
BEGIN
    ALTER TABLE Users ADD PhotoUrl NVARCHAR(255) NULL;
    PRINT 'Added column: PhotoUrl';
END
GO

-- Thêm cột dữ liệu QR code
IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
               WHERE TABLE_NAME = 'Users' AND COLUMN_NAME = 'CardQrData')
BEGIN
    ALTER TABLE Users ADD CardQrData NVARCHAR(255) NULL;
    PRINT 'Added column: CardQrData';
END
GO

PRINT '=== Migration hoàn tất! ===';
GO
