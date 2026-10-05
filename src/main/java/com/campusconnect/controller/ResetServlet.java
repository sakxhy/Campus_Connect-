package com.campusconnect.controller;

import com.campusconnect.service.DataStore;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(name = "ResetServlet", urlPatterns = {"/api/reset"})
public class ResetServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        DataStore.getInstance().resetToInitialData();
        resp.getWriter().write("{\"success\":true,\"message\":\"Data store reset to default seed records successfully.\"}");
    }
}
