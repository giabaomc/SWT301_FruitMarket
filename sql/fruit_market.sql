/* Fruit marketplace schema for SQL Server. */

USE [master];
GO

IF DB_ID(N'Assignment') IS NULL
BEGIN
    CREATE DATABASE [Assignment];
END;
GO

USE [Assignment];
GO

IF OBJECT_ID('dbo.tblOrderDetails', 'U') IS NOT NULL DROP TABLE dbo.tblOrderDetails;
IF OBJECT_ID('dbo.tblOrders', 'U') IS NOT NULL DROP TABLE dbo.tblOrders;
IF OBJECT_ID('dbo.tblProducts', 'U') IS NOT NULL DROP TABLE dbo.tblProducts;
IF OBJECT_ID('dbo.tblCategories', 'U') IS NOT NULL DROP TABLE dbo.tblCategories;
IF OBJECT_ID('dbo.tblUsers', 'U') IS NOT NULL DROP TABLE dbo.tblUsers;
GO

CREATE TABLE dbo.tblUsers (
    email NVARCHAR(100) NOT NULL PRIMARY KEY,
    password NVARCHAR(255) NOT NULL,
    fullName NVARCHAR(100) NOT NULL,
    role NVARCHAR(20) NOT NULL CONSTRAINT DF_tblUsers_role DEFAULT 'customer',
    CONSTRAINT CK_tblUsers_role CHECK (role IN ('customer', 'admin'))
);
GO

CREATE TABLE dbo.tblCategories (
    categoryID VARCHAR(10) NOT NULL PRIMARY KEY,
    categoryName NVARCHAR(100) NOT NULL UNIQUE,
    [describe] NVARCHAR(255) NULL
);
GO

CREATE TABLE dbo.tblProducts (
    productID INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    productName NVARCHAR(150) NOT NULL,
    image NVARCHAR(500) NULL,
    price DECIMAL(18,2) NOT NULL,
    quantity INT NOT NULL,
    categoryID VARCHAR(10) NOT NULL,
    importDate DATE NOT NULL,
    expiryDate DATE NOT NULL,
    status INT NOT NULL CONSTRAINT DF_tblProducts_status DEFAULT 1,
    CONSTRAINT FK_tblProducts_category FOREIGN KEY (categoryID) REFERENCES dbo.tblCategories(categoryID),
    CONSTRAINT CK_tblProducts_price CHECK (price >= 0),
    CONSTRAINT CK_tblProducts_quantity CHECK (quantity >= 0),
    CONSTRAINT CK_tblProducts_dates CHECK (expiryDate >= importDate),
    CONSTRAINT CK_tblProducts_status CHECK (status IN (0, 1))
);
GO

CREATE TABLE dbo.tblOrders (
    orderID INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    email NVARCHAR(100) NOT NULL,
    orderDate DATETIME2 NOT NULL CONSTRAINT DF_tblOrders_orderDate DEFAULT SYSDATETIME(),
    totalAmount DECIMAL(18,2) NOT NULL,
    status NVARCHAR(20) NOT NULL CONSTRAINT DF_tblOrders_status DEFAULT 'pending',
    CONSTRAINT FK_tblOrders_user FOREIGN KEY (email) REFERENCES dbo.tblUsers(email)
);
GO

CREATE TABLE dbo.tblOrderDetails (
    orderID INT NOT NULL,
    productID INT NOT NULL,
    quantity INT NOT NULL,
    price DECIMAL(18,2) NOT NULL,
    CONSTRAINT PK_tblOrderDetails PRIMARY KEY (orderID, productID),
    CONSTRAINT FK_tblOrderDetails_order FOREIGN KEY (orderID) REFERENCES dbo.tblOrders(orderID),
    CONSTRAINT FK_tblOrderDetails_product FOREIGN KEY (productID) REFERENCES dbo.tblProducts(productID),
    CONSTRAINT CK_tblOrderDetails_quantity CHECK (quantity > 0),
    CONSTRAINT CK_tblOrderDetails_price CHECK (price >= 0)
);
GO

INSERT INTO dbo.tblUsers (email, password, fullName, role) VALUES
(N'admin@vuonqua.local', N'123456', N'Quan tri vien', N'admin'),
(N'customer@vuonqua.local', N'123456', N'Khach hang mau', N'customer'),
(N'lan@example.com', N'123456', N'Nguyen Thi Lan', N'customer'),
(N'minh@example.com', N'123456', N'Tran Van Minh', N'customer'),
(N'hoa@example.com', N'123456', N'Le Thi Hoa', N'customer'),
(N'nam@example.com', N'123456', N'Pham Van Nam', N'customer'),
(N'ngoc@example.com', N'123456', N'Vo Ngoc Anh', N'customer'),
(N'thuy@example.com', N'123456', N'Hoang Thi Thuy', N'customer'),
(N'quang@example.com', N'123456', N'Do Minh Quang', N'customer'),
(N'phuong@example.com', N'123456', N'Bui Mai Phuong', N'customer');

INSERT INTO dbo.tblCategories (categoryID, categoryName, [describe]) VALUES
('CAM', N'Cam', N'Cam tuoi theo mua'),
('DUA', N'Dua hau', N'Dua hau ngot, giong hat'),
('NHO', N'Nho', N'Nho xanh va nho do'),
('DAU', N'Dau tay', N'Dau tay tuoi'),
('XOAI', N'Xoai', N'Xoai chin tu nhien'),
('CHUOI', N'Chuoi', N'Chuoi tieu Viet Nam'),
('TAO', N'Tao', N'Tao gion ngot'),
('LE', N'Le', N'Le Han Quoc tuoi mat'),
('OI', N'Oi', N'Oi giong dai loan'),
('THOM', N'Thom', N'Thom mien Tay');

INSERT INTO dbo.tblProducts
(productName, image, price, quantity, categoryID, importDate, expiryDate, status) VALUES
(N'Cam sanh mien Tay', N'images/cam-sanh.jpg', 35000, 100, 'CAM', '2026-08-20', '2026-08-30', 1),
(N'Dua hau khong hat', N'images/dua-hau.jpg', 22000, 80, 'DUA', '2026-08-20', '2026-08-28', 1),
(N'Nho xanh Nam Phi', N'images/nho-xanh.jpg', 95000, 45, 'NHO', '2026-08-21', '2026-09-02', 1),
(N'Dau tay Da Lat', N'images/dau-tay.jpg', 180000, 25, 'DAU', '2026-08-22', '2026-08-27', 1),
(N'Xoai cat Hoa Loc', N'images/xoai-cat.jpg', 65000, 60, 'XOAI', '2026-08-20', '2026-09-05', 1),
(N'Chuoi tieu', N'images/chuoi.jpg', 28000, 90, 'CHUOI', '2026-08-21', '2026-08-29', 1),
(N'Tao Fuji', N'images/tao-fuji.jpg', 75000, 50, 'TAO', '2026-08-21', '2026-09-06', 1),
(N'Le Han Quoc', N'images/le-han-quoc.jpg', 85000, 40, 'LE', '2026-08-22', '2026-09-07', 1),
(N'Oi dai loan', N'images/oi.jpg', 42000, 70, 'OI', '2026-08-22', '2026-08-31', 1),
(N'Thom mat', N'images/thom.jpg', 30000, 55, 'THOM', '2026-08-23', '2026-09-01', 1);
GO

INSERT INTO dbo.tblOrders (email, orderDate, totalAmount, status) VALUES
(N'customer@vuonqua.local', '2026-08-23T08:15:00', 35000, N'completed'),
(N'lan@example.com', '2026-08-23T09:20:00', 44000, N'completed'),
(N'minh@example.com', '2026-08-23T10:30:00', 95000, N'shipping'),
(N'hoa@example.com', '2026-08-23T11:45:00', 180000, N'pending'),
(N'nam@example.com', '2026-08-24T08:10:00', 65000, N'completed'),
(N'ngoc@example.com', '2026-08-24T09:25:00', 56000, N'shipping'),
(N'thuy@example.com', '2026-08-24T13:40:00', 75000, N'pending'),
(N'quang@example.com', '2026-08-24T15:00:00', 85000, N'completed'),
(N'phuong@example.com', '2026-08-25T08:35:00', 42000, N'pending'),
(N'customer@vuonqua.local', '2026-08-25T10:05:00', 30000, N'pending');

INSERT INTO dbo.tblOrderDetails (orderID, productID, quantity, price) VALUES
(1, 1, 1, 35000),
(2, 2, 2, 22000),
(3, 3, 1, 95000),
(4, 4, 1, 180000),
(5, 5, 1, 65000),
(6, 6, 2, 28000),
(7, 7, 1, 75000),
(8, 8, 1, 85000),
(9, 9, 1, 42000),
(10, 10, 1, 30000);
GO
