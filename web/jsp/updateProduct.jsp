<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="java.util.Vector, models.Product, models.Category" %>
<%
    //Get data from controller
    Product p = (Product)request.getAttribute("p");
    Vector<Category> cVector = (Vector<Category>)request.getAttribute("cVector");
    String pageTitle = "Sửa hoa quả";
%>
<%@include file="common/header.jsp" %>
        <h1 class="page-title">Sửa hoa quả</h1>
        <div class="form-card">
            <form action="productjsp" method="get">
                <div class="form-row">
                    <label for="txtId">Mã hoa quả</label>
                    <input type="text" name="productID" id="txtId" value="<%=p.getProductID()%>" readonly="">
                </div>
                <div class="form-row">
                    <label for="txtname">Tên hoa quả</label>
                    <input type="text" name="productName" id="txtname" value="<%=p.getProductName()%>">
                </div>
                <div class="form-row">
                    <label for="txtimage">Đường dẫn hình ảnh</label>
                    <input type="text" name="image" id="txtimage" value="<%=p.getImage()%>">
                </div>
                <div class="form-row">
                    <label for="txtprice">Giá (đ/kg)</label>
                    <input type="text" name="price" id="txtprice" value="<%=p.getPrice()%>">
                </div>
                <div class="form-row">
                    <label for="txtquantity">Số lượng (kg)</label>
                    <input type="text" name="quantity" id="txtquantity" value="<%=p.getQuantity()%>">
                </div>
                <div class="form-row">
                    <label for="txtcategoryID">Loại hoa quả</label>
                    <select name="categoryID" id="txtcategoryID">
                        <% for (Category c : cVector) {%>
                        <option value="<%=c.getCategoryID()%>"
                                <%=c.getCategoryID().equals(p.getCategoryID())?"selected":""%>><%=c.getCategoryName()%></option>
                        <%}%>
                    </select>
                </div>
                <div class="form-row">
                    <label for="txtimportDate">Ngày nhập</label>
                    <input type="date" name="importDate" id="txtimportDate" value="<%=p.getImportDate()%>">
                </div>
                <div class="form-row">
                    <label for="txtexpiryDate">Hạn dùng</label>
                    <input type="date" name="expiryDate" id="txtexpiryDate" value="<%=p.getExpiryDate()%>" required>
                </div>
                <div class="form-row">
                    <label>Trạng thái</label>
                    <div class="radio-group">
                        <label><input type="radio" name="status" value="1" <%=p.getStatus()==1?"checked":""%>>Active</label>
                        <label><input type="radio" name="status" value="0" <%=p.getStatus()==0?"checked":""%>>DeActive</label>
                    </div>
                </div>
                <div class="form-actions">
                    <input type="submit" name="submit" id="txtsubmit" value="Lưu thay đổi" class="btn btn-accent">
                    <input type="reset" value="Reset" class="btn btn-outline">
                    <input type="hidden" name="service" value="updateProduct">
                </div>
            </form>
        </div>
<%@include file="common/footer.jsp" %>
