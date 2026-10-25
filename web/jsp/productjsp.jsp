<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="java.util.Vector,models.Product" %>
<%
    //Code java here
    //Get data From controller
    Vector<Product> vector = (Vector<Product>)request.getAttribute("vector");
    String pageTitle = (String)request.getAttribute("pageTitle");
    String tableTitle = (String)request.getAttribute("tableTitle");
    String pName = (String)request.getParameter("productName");
    if(pName==null){
        pName="";
    }
    boolean isAdmin = "admin".equals(session.getAttribute("role"));
%>
<%@include file="common/header.jsp" %>
        <div class="toolbar">
            <form class="search-form" action="productjsp" method="get">
                <input type="text" name="productName" id="txtName" placeholder="Tìm hoa quả..." value="<%=pName%>">
                <input type="submit" name="submit" value="Tìm" id="txtSubmit" class="btn">
                <input type="hidden" name="service" value="listProduct">
                <% if (!pName.isEmpty()) { %>
                <a class="btn btn-outline" href="productjsp?service=listProduct">Xóa lọc</a>
                <% } %>
            </form>
            <% if (isAdmin) { %>
            <a class="btn btn-accent" href="productjsp?service=addProduct">+ Thêm hoa quả</a>
            <% } %>
        </div>

        <div class="table-wrap">
            <table>
                <caption><%=tableTitle%></caption>
                <thead>
                    <tr>
                        <th>Mã</th><th>Tên hoa quả</th><th>Hình ảnh</th><th>Giá (đ/kg)</th>
                        <th>Tồn kho (kg)</th><th>Loại</th><th>Ngày nhập</th><th>Hạn dùng</th>
                        <th>Trạng thái</th><th>Thao tác</th>
                    </tr>
                </thead>
                <tbody>
                    <% if (vector == null || vector.isEmpty()) { %>
                    <tr>
                        <td colspan="10" class="empty-state">Không tìm thấy hoa quả nào.</td>
                    </tr>
                    <% } else {
                        for (Product p : vector) {%>
                    <tr>
                        <td><%=p.getProductID()%></td>
                        <td><%=p.getProductName()%></td>
                        <td>
                            <% if (p.getImage() != null && !p.getImage().isEmpty()) { %>
                            <img class="thumb" src="<%=p.getImage()%>" alt="<%=p.getProductName()%>">
                            <% } %>
                        </td>
                        <td><%=p.getPrice()%></td>
                        <td><%=p.getQuantity()%></td>
                        <td><%=p.getCategoryID()%></td>
                        <td><%=p.getImportDate()%></td>
                        <td><%=p.getExpiryDate()%></td>
                        <td>
                            <% if (p.getStatus() == 1) { %>
                            <span class="badge badge-active">Active</span>
                            <% } else { %>
                            <span class="badge badge-inactive">DeActive</span>
                            <% } %>
                        </td>
                        <td>
                            <div class="actions-cell">
                                <% if (isAdmin) { %>
                                <a class="btn btn-sm" href="productjsp?service=updateProduct&productID=<%=p.getProductID()%>">Sửa</a>
                                <a class="btn btn-sm btn-danger" href="productjsp?service=deleteProduct&productID=<%=p.getProductID()%>" onclick="return confirm('Xóa hoa quả này?');">Xóa</a>
                                <% } %>
                                <a class="btn btn-sm btn-accent" href="cart?service=add2cart&productID=<%=p.getProductID()%>">Thêm vào giỏ</a>
                            </div>
                        </td>
                    </tr>
                    <%}
                       }%>
                </tbody>
            </table>
        </div>
<%@include file="common/footer.jsp" %>
