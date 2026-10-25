/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package controllers;

import dao.CategoryDAO;
import dao.ProductDAO;
import dao.UserDAO;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.sql.Date;
import java.util.Vector;
import models.Category;
import models.Product;

/**
 *
 * @author Admin
 */
@WebServlet(name = "ProductJSP", urlPatterns = {"/productjsp"})
public class ProductJSP extends HttpServlet {

    String sql = "SELECT *  FROM tblProducts";
    String sql2 = "SELECT *  FROM tblCategories";

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        HttpSession session = request.getSession(true);
        boolean isAdmin = "admin".equals(session.getAttribute("role"));
        ProductDAO dao = new ProductDAO();
        Vector<Product> vector;
        CategoryDAO cDao = new CategoryDAO();
        Vector<Category> cVector = cDao.getAllCategory(sql2);
       
        String service = request.getParameter("service");
        if (service == null) {
            service = "listProduct";
        }
        if(service.equals("login")){
            String submit = request.getParameter("submit");
            if(submit==null){//show login.jsp
                request.getRequestDispatcher("jsp/login.jsp").forward(request, response);
            }else{
                String email = request.getParameter("email");
                String pass = request.getParameter("pass");
                UserDAO uDao = new UserDAO();
                String role = uDao.checkLogin(email, pass);
                if(role!=null){//login thanh cong
                    //add vao session
                    session.setAttribute("email", email);
                    session.setAttribute("role", role);
                    response.sendRedirect("productjsp");
                }else{//wrong username or pass
                    String mess = "Login Failed!";
                    request.setAttribute("mess", mess);
                    request.getRequestDispatcher("jsp/login.jsp").forward(request, response);
                }
            }
            
        }
        if(service.equals("deleteProduct")){
            if (isAdmin) {
                int productId = Integer.parseInt(request.getParameter("productID"));
                dao.deleteProduct(productId);
            }
            response.sendRedirect("productjsp");
        }
        if (service.equals("updateProduct")) {
            if (!isAdmin) {
                response.sendRedirect("productjsp");
            } else {
                String submit = request.getParameter("submit");
                int productID = Integer.parseInt(request.getParameter("productID"));
                if (submit == null) {//show updateProduct.jsp
                    request.setAttribute("cVector", cVector);
                    Product p = dao.searchProduct(productID);
                    request.setAttribute("p", p);
                    request.getRequestDispatcher("jsp/updateProduct.jsp").forward(request, response);
                } else {
                    String productName = request.getParameter("productName"),
                            image = request.getParameter("image");
                    double price = Double.parseDouble(request.getParameter("price"));
                    int quantity = Integer.parseInt(request.getParameter("quantity"));
                    String categoryID = request.getParameter("categoryID");
                    Date importDate = Date.valueOf(request.getParameter("importDate")),
                            expiryDate = Date.valueOf(request.getParameter("expiryDate"));
                    int status = Integer.parseInt(request.getParameter("status"));
                    Product p = new Product(productID,productName, image, price, quantity, categoryID, importDate, expiryDate, status);
                    dao.updateProduct(p);
                    response.sendRedirect("productjsp");
                }
            }
        }
        if (service.equals("addProduct")) {
            if (!isAdmin) {
                response.sendRedirect("productjsp");
            } else {
                String submit = request.getParameter("submit");
                if (submit == null) {//show addProduct.jsp
                    request.setAttribute("cVector", cVector);
                    request.getRequestDispatcher("jsp/addProduct.jsp").forward(request, response);
                } else {
                    String productName = request.getParameter("productName"),
                            image = request.getParameter("image");
                    double price = Double.parseDouble(request.getParameter("price"));
                    int quantity = Integer.parseInt(request.getParameter("quantity"));
                    String categoryID = request.getParameter("categoryID");
                    Date importDate = Date.valueOf(request.getParameter("importDate")),
                            expiryDate = Date.valueOf(request.getParameter("expiryDate"));
                    int status = Integer.parseInt(request.getParameter("status"));
                    Product p = new Product(productName, image, price, quantity, categoryID, importDate, expiryDate, status);
                    dao.insertProduct(p);
                    response.sendRedirect("productjsp");
                }
            }
        }
        if (service.equals("listProduct")) {
            String submit = request.getParameter("submit");
            String pName = request.getParameter("productName");
            //Step 1: Set data
            if (submit == null) {//list all products
                vector = dao.getAllProduct(sql);
            } else {//search
                vector = dao.searchProductByName(pName);
            }

            //Step 2: Set Attribute to send data
            request.setAttribute("vector", vector);
            request.setAttribute("pageTitle", "Cua hang hoa qua");
            request.setAttribute("tableTitle", "Danh sach hoa qua");
            //Step 3: Select View to send data
            request.getRequestDispatcher("jsp/productjsp.jsp").forward(request, response);
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
