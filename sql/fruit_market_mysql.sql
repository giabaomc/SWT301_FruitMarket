-- ==============================================================
-- Fruit Market Schema for MySQL / MySQL Workbench
-- Converted from SQL Server (sql/fruit_market.sql)
-- ==============================================================

-- 1. Create and select Database
DROP DATABASE IF EXISTS Assignment;
CREATE DATABASE Assignment CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE Assignment;

-- 2. Drop existing tables in reverse order of foreign keys
DROP TABLE IF EXISTS tblOrderDetails;
DROP TABLE IF EXISTS tblOrders;
DROP TABLE IF EXISTS tblProducts;
DROP TABLE IF EXISTS tblCategories;
DROP TABLE IF EXISTS tblUsers;

-- 3. Create Tables

-- Table tblUsers
CREATE TABLE tblUsers (
    email VARCHAR(100) NOT NULL PRIMARY KEY,
    password VARCHAR(255) NOT NULL,
    fullName VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'customer',
    CONSTRAINT CK_tblUsers_role CHECK (role IN ('customer', 'admin'))
);

-- Table tblCategories
CREATE TABLE tblCategories (
    categoryID VARCHAR(10) NOT NULL PRIMARY KEY,
    categoryName VARCHAR(100) NOT NULL UNIQUE,
    `describe` VARCHAR(255) NULL
);

-- Table tblProducts
CREATE TABLE tblProducts (
    productID INT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    productName VARCHAR(150) NOT NULL,
    image VARCHAR(500) NULL,
    price DECIMAL(18,2) NOT NULL,
    quantity INT NOT NULL,
    categoryID VARCHAR(10) NOT NULL,
    importDate DATE NOT NULL,
    expiryDate DATE NOT NULL,
    status INT NOT NULL DEFAULT 1,
    CONSTRAINT FK_tblProducts_category FOREIGN KEY (categoryID) REFERENCES tblCategories(categoryID),
    CONSTRAINT CK_tblProducts_price CHECK (price >= 0),
    CONSTRAINT CK_tblProducts_quantity CHECK (quantity >= 0),
    CONSTRAINT CK_tblProducts_dates CHECK (expiryDate >= importDate),
    CONSTRAINT CK_tblProducts_status CHECK (status IN (0, 1))
);

-- Table tblOrders
CREATE TABLE tblOrders (
    orderID INT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    email VARCHAR(100) NOT NULL,
    orderDate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    totalAmount DECIMAL(18,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    CONSTRAINT FK_tblOrders_user FOREIGN KEY (email) REFERENCES tblUsers(email)
);

-- Table tblOrderDetails
CREATE TABLE tblOrderDetails (
    orderID INT NOT NULL,
    productID INT NOT NULL,
    quantity INT NOT NULL,
    price DECIMAL(18,2) NOT NULL,
    PRIMARY KEY (orderID, productID),
    CONSTRAINT FK_tblOrderDetails_order FOREIGN KEY (orderID) REFERENCES tblOrders(orderID),
    CONSTRAINT FK_tblOrderDetails_product FOREIGN KEY (productID) REFERENCES tblProducts(productID),
    CONSTRAINT CK_tblOrderDetails_quantity CHECK (quantity > 0),
    CONSTRAINT CK_tblOrderDetails_price CHECK (price >= 0)
);

-- 4. Seed Data

-- Insert Users
INSERT INTO tblUsers (email, password, fullName, role) VALUES
('admin@vuonqua.local', '123456', 'Quan tri vien', 'admin'),
('customer@vuonqua.local', '123456', 'Khach hang mau', 'customer'),
('lan@example.com', '123456', 'Nguyen Thi Lan', 'customer'),
('minh@example.com', '123456', 'Tran Van Minh', 'customer'),
('hoa@example.com', '123456', 'Le Thi Hoa', 'customer'),
('nam@example.com', '123456', 'Pham Van Nam', 'customer'),
('ngoc@example.com', '123456', 'Vo Ngoc Anh', 'customer'),
('thuy@example.com', '123456', 'Hoang Thi Thuy', 'customer'),
('quang@example.com', '123456', 'Do Minh Quang', 'customer'),
('phuong@example.com', '123456', 'Bui Mai Phuong', 'customer');

-- Insert Categories
INSERT INTO tblCategories (categoryID, categoryName, `describe`) VALUES
('CAM', 'Cam', 'Cam tuoi theo mua'),
('DUA', 'Dua hau', 'Dua hau ngot, giong hat'),
('NHO', 'Nho', 'Nho xanh va nho do'),
('DAU', 'Dau tay', 'Dau tay tuoi'),
('XOAI', 'Xoai', 'Xoai chin tu nhien'),
('CHUOI', 'Chuoi', 'Chuoi tieu Viet Nam'),
('TAO', 'Tao', 'Tao gion ngot'),
('LE', 'Le', 'Le Han Quoc tuoi mat'),
('OI', 'Oi', 'Oi giong dai loan'),
('THOM', 'Thom', 'Thom mien Tay');

-- Insert Products
INSERT INTO tblProducts
(productName, image, price, quantity, categoryID, importDate, expiryDate, status) VALUES
('Cam sanh mien Tay', 'images/cam-sanh.jpg', 35000, 100, 'CAM', '2026-08-20', '2026-08-30', 1),
('Dua hau khong hat', 'images/dua-hau.jpg', 22000, 80, 'DUA', '2026-08-20', '2026-08-28', 1),
('Nho xanh Nam Phi', 'images/nho-xanh.jpg', 95000, 45, 'NHO', '2026-08-21', '2026-09-02', 1),
('Dau tay Da Lat', 'images/dau-tay.jpg', 180000, 25, 'DAU', '2026-08-22', '2026-08-27', 1),
('Xoai cat Hoa Loc', 'images/xoai-cat.jpg', 65000, 60, 'XOAI', '2026-08-20', '2026-09-05', 1),
('Chuoi tieu', 'images/chuoi.jpg', 28000, 90, 'CHUOI', '2026-08-21', '2026-08-29', 1),
('Tao Fuji', 'images/tao-fuji.jpg', 75000, 50, 'TAO', '2026-08-21', '2026-09-06', 1),
('Le Han Quoc', 'images/le-han-quoc.jpg', 85000, 40, 'LE', '2026-08-22', '2026-09-07', 1),
('Oi dai loan', 'images/oi.jpg', 42000, 70, 'OI', '2026-08-22', '2026-08-31', 1),
('Thom mat', 'images/thom.jpg', 30000, 55, 'THOM', '2026-08-23', '2026-09-01', 1);

-- Insert Orders
INSERT INTO tblOrders (email, orderDate, totalAmount, status) VALUES
('customer@vuonqua.local', '2026-08-23 08:15:00', 35000, 'completed'),
('lan@example.com', '2026-08-23 09:20:00', 44000, 'completed'),
('minh@example.com', '2026-08-23 10:30:00', 95000, 'shipping'),
('hoa@example.com', '2026-08-23 11:45:00', 180000, 'pending'),
('nam@example.com', '2026-08-24 08:10:00', 65000, 'completed'),
('ngoc@example.com', '2026-08-24 09:25:00', 56000, 'shipping'),
('thuy@example.com', '2026-08-24 13:40:00', 75000, 'pending'),
('quang@example.com', '2026-08-24 15:00:00', 85000, 'completed'),
('phuong@example.com', '2026-08-25 08:35:00', 42000, 'pending'),
('customer@vuonqua.local', '2026-08-25 10:05:00', 30000, 'pending');

-- Insert OrderDetails
INSERT INTO tblOrderDetails (orderID, productID, quantity, price) VALUES
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
