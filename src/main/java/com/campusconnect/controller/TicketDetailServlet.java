package com.campusconnect.controller;

import com.campusconnect.model.Comment;
import com.campusconnect.model.Ticket;
import com.campusconnect.model.User;
import com.campusconnect.service.DataStore;
import com.campusconnect.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;

@WebServlet(name = "TicketDetailServlet", urlPatterns = {"/api/ticket/*"})
public class TicketDetailServlet extends HttpServlet {
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
        String pathInfo = req.getPathInfo(); // e.g. "/TKT-2026-101"

        if (pathInfo == null || pathInfo.length() <= 1) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"success\":false,\"message\":\"Ticket ID missing\"}");
            return;
        }

        String ticketId = pathInfo.substring(1).trim();
        Ticket ticket = dataStore.getTicketById(ticketId);

        if (ticket == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write("{\"success\":false,\"message\":\"Ticket not found\"}");
            return;
        }

        resp.getWriter().write(String.format("{\"success\":true,\"ticket\":%s}", JsonUtil.ticketToJson(ticket)));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        String pathInfo = req.getPathInfo(); // e.g. "/TKT-2026-101/status" or "/TKT-2026-101/assign" or "/TKT-2026-101/comments"

        if (pathInfo == null || pathInfo.length() <= 1) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"success\":false,\"message\":\"Invalid endpoint\"}");
            return;
        }

        String[] parts = pathInfo.substring(1).split("/");
        String ticketId = parts[0];
        String action = parts.length > 1 ? parts[1] : "";

        Ticket ticket = dataStore.getTicketById(ticketId);
        if (ticket == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write("{\"success\":false,\"message\":\"Ticket not found\"}");
            return;
        }

        HttpSession session = req.getSession(false);
        User currentUser = session != null ? (User) session.getAttribute("currentUser") : null;
        Map<String, String> body = readJsonBody(req);

        if ("status".equalsIgnoreCase(action)) {
            String newStatus = body.getOrDefault("status", req.getParameter("status"));
            String remark = body.getOrDefault("remark", req.getParameter("remark"));

            if (newStatus == null || newStatus.trim().isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"success\":false,\"message\":\"New status is required.\"}");
                return;
            }

            Ticket updated = dataStore.updateTicketStatus(ticketId, newStatus, remark, currentUser);
            resp.getWriter().write(String.format("{\"success\":true,\"ticket\":%s}", JsonUtil.ticketToJson(updated)));

        } else if ("assign".equalsIgnoreCase(action)) {
            String assignee = body.getOrDefault("assignedTo", req.getParameter("assignedTo"));
            if (assignee == null || assignee.trim().isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"success\":false,\"message\":\"Assignee department or staff is required.\"}");
                return;
            }

            Ticket updated = dataStore.assignTicket(ticketId, assignee);
            resp.getWriter().write(String.format("{\"success\":true,\"ticket\":%s}", JsonUtil.ticketToJson(updated)));

        } else if ("comments".equalsIgnoreCase(action)) {
            String text = body.getOrDefault("text", req.getParameter("text"));
            if (text == null || text.trim().isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"success\":false,\"message\":\"Comment text cannot be empty.\"}");
                return;
            }

            Comment comment = dataStore.addComment(ticketId, text, currentUser);
            resp.getWriter().write(String.format("{\"success\":true,\"comment\":%s}", JsonUtil.commentToJson(comment)));

        } else {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write("{\"success\":false,\"message\":\"Unknown action for ticket\"}");
        }
    }

    private Map<String, String> readJsonBody(HttpServletRequest req) {
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader reader = req.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (Exception ignored) {}
        return JsonUtil.parseJsonMap(sb.toString());
    }
}
