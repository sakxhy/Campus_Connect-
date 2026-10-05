package com.campusconnect.controller;

import com.campusconnect.model.Ticket;
import com.campusconnect.model.User;
import com.campusconnect.service.DataStore;
import com.campusconnect.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@WebServlet(name = "TicketServlet", urlPatterns = {"/api/tickets"})
public class TicketServlet extends HttpServlet {
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

        String studentEmail = req.getParameter("studentEmail");
        String status = req.getParameter("status");
        String category = req.getParameter("category");
        String priority = req.getParameter("priority");
        String search = req.getParameter("search");

        List<Ticket> tickets = dataStore.getTickets(studentEmail, status, category, priority, search);
        String json = JsonUtil.ticketListToJson(tickets);
        resp.getWriter().write(json);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");

        HttpSession session = req.getSession(false);
        User currentUser = session != null ? (User) session.getAttribute("currentUser") : null;

        Map<String, String> body = readJsonBody(req);
        String title = body.getOrDefault("title", req.getParameter("title"));
        String category = body.getOrDefault("category", req.getParameter("category"));
        String priority = body.getOrDefault("priority", req.getParameter("priority"));
        String location = body.getOrDefault("location", req.getParameter("location"));
        String description = body.getOrDefault("description", req.getParameter("description"));

        if (title == null || title.trim().isEmpty() || description == null || description.trim().isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"success\":false,\"message\":\"Title and description are required.\"}");
            return;
        }

        // If user is not signed in via session, fall back to default student demo user
        if (currentUser == null) {
            currentUser = dataStore.getDemoUser("student");
        }

        Ticket created = dataStore.createTicket(title, category, priority, location, description, currentUser);

        resp.setStatus(HttpServletResponse.SC_CREATED);
        resp.getWriter().write(String.format("{\"success\":true,\"ticket\":%s}", JsonUtil.ticketToJson(created)));
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
