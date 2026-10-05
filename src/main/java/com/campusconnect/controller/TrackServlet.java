package com.campusconnect.controller;

import com.campusconnect.model.Ticket;
import com.campusconnect.service.DataStore;
import com.campusconnect.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(name = "TrackServlet", urlPatterns = {"/api/track"})
public class TrackServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private DataStore dataStore;

    @Override
    public void init() throws ServletException {
        super.init();
        dataStore = DataStore.getInstance();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");

        String ticketId = req.getParameter("id");
        if (ticketId == null || ticketId.trim().isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"success\":false,\"message\":\"Ticket ID parameter is required.\"}");
            return;
        }

        Ticket ticket = dataStore.getTicketById(ticketId.trim());
        if (ticket != null) {
            resp.getWriter().write(String.format("{\"success\":true,\"ticket\":%s}", JsonUtil.ticketToJson(ticket)));
        } else {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write(String.format("{\"success\":false,\"message\":\"No grievance ticket found matching ID '%s'.\"}", JsonUtil.escapeJson(ticketId.trim())));
        }
    }
}
