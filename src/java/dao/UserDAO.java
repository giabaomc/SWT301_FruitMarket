/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dao;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserDAO extends DBContext {

    String sql = "Select *\n"
            + "From tblUsers\n"
            + "Where email=? and password=?";

    /**
     * Checks the email/password against tblUsers.
     * @return the user's role ("admin" or "customer") if the credentials are valid, null otherwise.
     */
    public String checkLogin(String email, String pass) {
        String role = null;
        try {
            PreparedStatement ptm = connection.prepareStatement(sql);
            ptm.setString(1, email);
            ptm.setString(2, pass);
            ResultSet rs = ptm.executeQuery();
            if(rs.next()){
                role = rs.getString("role");
            }
        } catch (SQLException ex) {
            Logger.getLogger(UserDAO.class.getName()).log(Level.SEVERE, null, ex);
        }
        return role;

    }

}
