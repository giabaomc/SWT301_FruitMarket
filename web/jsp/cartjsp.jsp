<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="models.Cart, java.util.Vector" %>
<%
    //Get data From controller
    Vector<Cart> vector = (Vector<Cart>)request.getAttribute("vector");
    String pageTitle = "Giỏ hàng hoa quả";
%>
<%@include file="common/header.jsp" %>
        <h1 class="page-title">Giỏ hàng hoa quả</h1>
        <div class="table-wrap">
            <table>
                <thead>
                    <tr>
                        <th>Mã</th><th>Hoa quả</th><th>Giá (đ/kg)</th>
                        <th>Số lượng (kg)</th><th>Thành tiền</th><th>Xóa</th>
                    </tr>
                </thead>
                <tbody>
                    <%
                       double total = 0;
                       if (vector == null || vector.isEmpty()) {
                    %>
                    <tr>
                        <td colspan="6" class="empty-state">Giỏ hàng của bạn đang trống.</td>
                    </tr>
                    <%
                       } else {
                       for (Cart c : vector)  {
                            total+=c.getPrice()*c.getQuantity();
                    %>
                    <tr>
                        <td><%=c.getProductID()%></td>
                        <td><%=c.getProductName()%></td>
                        <td><%=c.getPrice()%></td>
                        <td><%=c.getQuantity()%></td>
                        <td><%=c.getPrice()*c.getQuantity()%></td>
                        <td><a class="btn btn-sm btn-danger" href="cart?service=remove&productId=<%=c.getProductID()%>">Xóa</a></td>
                    </tr>
                    <%}
                       }%>
                </tbody>
            </table>
        </div>
        <div class="cart-summary">
            <span class="cart-total">Tổng tiền: <%=total%> VND</span>
        </div>
        <div class="page-actions">
            <a class="btn btn-outline" href="productjsp">Tiếp tục mua hoa quả</a>
            <a class="btn btn-danger" href="cart?service=removeAll">Xóa toàn bộ giỏ hàng</a>
        </div>
<%@include file="common/footer.jsp" %>
