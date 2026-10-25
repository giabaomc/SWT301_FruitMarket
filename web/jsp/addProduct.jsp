<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="models.Category, java.util.Vector" %>
<%
    //Get data from controller
    Vector<Category> cVector = (Vector<Category>)request.getAttribute("cVector");
    String pageTitle = "Thêm hoa quả";
%>
<%@include file="common/header.jsp" %>
        <h1 class="page-title">Thêm hoa quả</h1>
        <div class="form-card">
            <form action="productjsp" method="get">
                <div class="form-row">
                    <label for="txtname">Tên hoa quả</label>
                    <input type="text" name="productName" id="txtname">
                </div>
                <div class="form-row">
                    <label for="txtimage">Đường dẫn hình ảnh</label>
                    <input type="text" name="image" id="txtimage">
                </div>
                <div class="form-row">
                    <label for="txtprice">Giá (đ/kg)</label>
                    <input type="number" min="0" step="0.01" name="price" id="txtprice" required>
                </div>
                <div class="form-row">
                    <label for="txtquantity">Số lượng (kg)</label>
                    <input type="number" min="0" name="quantity" id="txtquantity" required>
                </div>
                <div class="form-row">
                    <label for="txtcategoryID">Loại hoa quả</label>
                    <select name="categoryID" id="txtcategoryID">
                        <% for (Category c : cVector) {%>
                        <option value="<%=c.getCategoryID()%>"><%=c.getCategoryName()%></option>
                        <%}%>
                    </select>
                </div>
                <div class="form-row">
                    <label for="txtimportDate">Ngày nhập</label>
                    <input type="date" name="importDate" id="txtimportDate">
                </div>
                <div class="form-row">
                    <label for="txtexpiryDate">Hạn dùng</label>
                    <input type="date" name="expiryDate" id="txtexpiryDate" required>
                </div>
                <div class="form-row">
                    <label>Trạng thái</label>
                    <div class="radio-group">
                        <label><input type="radio" name="status" value="1" checked="">Active</label>
                        <label><input type="radio" name="status" value="0">DeActive</label>
                    </div>
                </div>
                <div class="form-actions">
                    <input type="submit" name="submit" id="txtsubmit" value="Thêm hoa quả" class="btn btn-accent">
                    <input type="reset" value="Reset" class="btn btn-outline">
                    <input type="hidden" name="service" value="addProduct">
                </div>
            </form>
        </div>
<%@include file="common/footer.jsp" %>
