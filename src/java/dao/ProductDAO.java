/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import models.Product;

public class ProductDAO extends DBContext {

    public Vector<Product> getAllProduct(String sql) {
        Vector<Product> vector = new Vector<>();
        try {
            PreparedStatement ptm = connection.prepareStatement(sql);
            ResultSet rs = ptm.executeQuery();
            while (rs.next()) {
                Product p = new Product(rs.getInt(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getDouble(4),
                        rs.getInt(5),
                        rs.getString(6),
                        rs.getDate(7),
                        rs.getDate(8),
                        rs.getInt(9));
                vector.add(p);
            }
        } catch (SQLException ex) {
            ex.getStackTrace();
        }
        return vector;
    }

    public Vector<Product> searchProductByName(String productName) {
        Vector<Product> products = new Vector<>();
        String sql = "SELECT * FROM tblProducts WHERE productName LIKE ? AND status = 1";
        try {
            PreparedStatement ptm = connection.prepareStatement(sql);
            ptm.setString(1, "%" + productName + "%");
            ResultSet rs = ptm.executeQuery();
            while (rs.next()) {
                products.add(new Product(rs.getInt(1), rs.getString(2), rs.getString(3),
                        rs.getDouble(4), rs.getInt(5), rs.getString(6), rs.getDate(7),
                        rs.getDate(8), rs.getInt(9)));
            }
        } catch (SQLException ex) {
            Logger.getLogger(ProductDAO.class.getName()).log(Level.SEVERE, null, ex);
        }
        return products;
    }

    public Product searchProduct(int productId) {
        String sql = "Select * From tblProducts\n"
                + "Where productID = ?";
        try {
            PreparedStatement ptm = connection.prepareStatement(sql);
            ptm.setInt(1, productId);
            ResultSet rs = ptm.executeQuery();
            if (rs.next()) {
                Product p = new Product(productId,
                        rs.getString("productName"),
                        rs.getString(3),
                        rs.getDouble(4),
                        rs.getInt(5),
                        rs.getString(6),
                        rs.getDate(7),
                        rs.getDate(8),
                        rs.getInt(9));
                return p;
            }
        } catch (SQLException ex) {
            ex.getStackTrace();
        }
        return null;
    }

    public int insertProduct(Product p) {
        int n = 0;
        String sql = "INSERT INTO [dbo].[tblProducts]\n"
                + "           ([productName]\n"
                + "           ,[image]\n"
                + "           ,[price]\n"
                + "           ,[quantity]\n"
                + "           ,[categoryID]\n"
                + "           ,[importDate]\n"
                + "           ,[expiryDate]\n"
                + "           ,[status])\n"
                + "     VALUES(?,?,?,?,?,?,?,?)";
        try {
            PreparedStatement ptm = connection.prepareStatement(sql);
            ptm.setString(1, p.getProductName());
            ptm.setString(2, p.getImage());
            ptm.setDouble(3, p.getPrice());
            ptm.setInt(4, p.getQuantity());
            ptm.setString(5, p.getCategoryID());
            ptm.setDate(6, p.getImportDate());
            ptm.setDate(7, p.getExpiryDate());
            ptm.setInt(8, p.getStatus());
            n = ptm.executeUpdate();
        } catch (SQLException ex) {
            ex.getStackTrace();
        }
        return n;
    }

    public int updateProduct(Product p) {
        int n = 0;
        String sql = "UPDATE [dbo].[tblProducts]\n"
                + "   SET [productName] = ?\n"
                + "      ,[image] = ?\n"
                + "      ,[price] = ?\n"
                + "      ,[quantity] = ?\n"
                + "      ,[categoryID] = ?\n"
                + "      ,[importDate] = ?\n"
                + "      ,[expiryDate] = ?\n"
                + "      ,[status] = ?\n"
                + " WHERE productID = ?";
        try {
            PreparedStatement ptm = connection.prepareStatement(sql);
            ptm.setString(1, p.getProductName());
            ptm.setString(2, p.getImage());
            ptm.setDouble(3, p.getPrice());
            ptm.setInt(4, p.getQuantity());
            ptm.setString(5, p.getCategoryID());
            ptm.setDate(6, p.getImportDate());
            ptm.setDate(7, p.getExpiryDate());
            ptm.setInt(8, p.getStatus());
            ptm.setInt(9, p.getProductID());
            n = ptm.executeUpdate();
        } catch (SQLException ex) {
            ex.getStackTrace();
        }
        return n;
    }

    public void changeStatus(int productId, int newStatus) {
        //TODO
        String sql = "Update tblProducts\n"
                + "Set status = ?\n"
                + "Where productID=?";
        try {
            PreparedStatement ptm = connection.prepareStatement(sql);
            ptm.setInt(1, newStatus);
            ptm.setInt(2, productId);
            ptm.executeUpdate();
        } catch (SQLException ex) {
            Logger.getLogger(ProductDAO.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public int deleteProduct(int productId) {
        int n = 0;
        String sql = " Delete From tblProducts\n"
                + " Where productID = ?";
        try {
            //Search productId trong bang OrderDetail
            //Neu tim thay=> khong xoa Product ma chuyen Status = 0
            //Neu khong tim thay=>xoa Product khoi bang tblProducts
            PreparedStatement ptm1 = connection.prepareStatement("Select * From tblOrderDetails\n"
                    + " Where productID = ?");
            ptm1.setInt(1, productId);
            ResultSet rs = ptm1.executeQuery();
            if (rs.next()) {
                //Khong xoa, status = 0;
                changeStatus(productId, 0);
            } else {
                //co xoa
                PreparedStatement ptm = connection.prepareStatement(sql);
                ptm.setInt(1, productId);
                n = ptm.executeUpdate();
            }
        } catch (SQLException ex) {
            ex.getStackTrace();
        }
        return n;
    }

}
