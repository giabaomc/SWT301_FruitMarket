<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%-- Shared header include. Parent page must declare a String pageTitle before including this file. --%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title><%=pageTitle%></title>
        <link rel="stylesheet" href="css/style.css">
    </head>
    <body>
        <header class="site-header">
            <div class="header-inner">
                <a class="brand" href="index.html">Vườn Quả Tươi</a>
                <nav class="main-nav">
                    <a href="productjsp?service=listProduct">Danh sách hoa quả</a>
                    <a href="cart?service=showCart">Giỏ hàng</a>
                    <%
                        String headerEmail = (String) session.getAttribute("email");
                        if (headerEmail != null) {
                    %>
                    <span class="nav-user">Xin chào, <%=headerEmail%></span>
                    <%
                    } else {
                    %>
                    <a href="productjsp?service=login">Đăng nhập</a>
                    <%
                        }
                    %>
                </nav>
            </div>
        </header>
        <main class="container">
