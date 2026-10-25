<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%
    String mess = (String)request.getAttribute("mess");
    String pageTitle = "Đăng nhập";
%>
<%@include file="common/header.jsp" %>
        <h1 class="page-title">Đăng nhập</h1>
        <% if (mess != null) { %>
        <p class="alert alert-error"><%=mess%></p>
        <% } %>
        <div class="form-card">
            <form action="productjsp" method="POST">
                <div class="form-row">
                    <label for="txtemail">Email</label>
                    <input type="text" name="email" id="txtemail" required="">
                </div>
                <div class="form-row">
                    <label for="txtpass">Mật khẩu</label>
                    <input type="password" name="pass" id="txtpass" required="">
                </div>
                <div class="form-actions">
                    <input type="submit" name="submit" value="Login" class="btn">
                    <input type="reset" value="Reset" class="btn btn-outline">
                    <input type="hidden" name="service" value="login">
                </div>
            </form>
        </div>
<%@include file="common/footer.jsp" %>
