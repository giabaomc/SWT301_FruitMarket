/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dao;

import models.Cart;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CartDAO extends DBContext {

    public Cart getCart(int productId) {
        Cart cart = null;
        String sql = "Select productID, productName, price, quantity\n"
                + "From tblProducts\n"
                + "Where productID=?";
        try {
            PreparedStatement ptm = connection.prepareStatement(sql);
            ptm.setInt(1, productId);
            ResultSet rs = ptm.executeQuery();
            if(rs.next()){
                cart = new Cart(rs.getInt(1), 
                        rs.getString(2),
                        rs.getDouble(3) ,
                        0);
            }
        } catch (SQLException ex) {
            Logger.getLogger(CartDAO.class.getName()).log(Level.SEVERE, null, ex);
        }
        return cart;
        
    }

}
