package com.campusconnect.controller;

import com.campusconnect.model.User;
import com.campusconnect.service.DataStore;
import com.campusconnect.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;

@WebServlet(name = "AuthServlet", urlPatterns = {"/api/auth/*"})
public class AuthServlet extends HttpServlet {
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
        String pathInfo = req.getPathInfo(); // e.g. "/me"

        if ("/me".equals(pathInfo)) {
            HttpSession session = req.getSession(false);
            User user = session != null ? (User) session.getAttribute("currentUser") : null;
            if (user != null) {
                resp.getWriter().write(String.format("{\"success\":true,\"user\":%s}", JsonUtil.userToJson(user)));
            } else {
                resp.getWriter().write("{\"success\":false,\"user\":null,\"message\":\"Not authenticated\"}");
            }
        } else {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write("{\"success\":false,\"message\":\"Endpoint not found\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        String pathInfo = req.getPathInfo(); // "/login", "/register", "/demo", "/logout"

        if ("/login".equals(pathInfo)) {
            handleLogin(req, resp);
        } else if ("/register".equals(pathInfo)) {
            handleRegister(req, resp);
        } else if ("/demo".equals(pathInfo)) {
            handleDemoLogin(req, resp);
        } else if ("/logout".equals(pathInfo)) {
            handleLogout(req, resp);
        } else {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write("{\"success\":false,\"message\":\"Endpoint not found\"}");
        }
    }

    private void handleLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, String> body = readJsonBody(req);
        String email = body.getOrDefault("email", req.getParameter("email"));
        String password = body.getOrDefault("password", req.getParameter("password"));
        String roleHint = body.getOrDefault("roleHint", req.getParameter("roleHint"));

        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"success\":false,\"message\":\"Email and password are required.\"}");
            return;
        }

        User user = dataStore.getUserByEmail(email);
        if (user == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().write("{\"success\":false,\"message\":\"No account found with this email address.\"}");
            return;
        }

        if (!password.equals(user.getPassword())) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().write("{\"success\":false,\"message\":\"Invalid password. Please try again.\"}");
            return;
        }

        if (roleHint != null && !roleHint.trim().isEmpty() && !user.getRole().equalsIgnoreCase(roleHint.trim())) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            resp.getWriter().write(String.format("{\"success\":false,\"message\":\"Access denied. Account does not have %s permissions.\"}", roleHint));
            return;
        }

        HttpSession session = req.getSession(true);
        session.setAttribute("currentUser", user);

        resp.getWriter().write(String.format("{\"success\":true,\"user\":%s}", JsonUtil.userToJson(user)));
    }

    private void handleRegister(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, String> body = readJsonBody(req);
        String name = body.getOrDefault("name", req.getParameter("name"));
        String email = body.getOrDefault("email", req.getParameter("email"));
        String password = body.getOrDefault("password", req.getParameter("password"));
        String studentId = body.getOrDefault("studentId", req.getParameter("studentId"));
        String department = body.getOrDefault("department", req.getParameter("department"));

        if (name == null || name.trim().isEmpty() || email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"success\":false,\"message\":\"Name, email, and password are required.\"}");
            return;
        }

        if (dataStore.getUserByEmail(email) != null) {
            resp.setStatus(HttpServletResponse.SC_CONFLICT);
            resp.getWriter().write("{\"success\":false,\"message\":\"An account with this email already exists.\"}");
            return;
        }

        String userId = "usr_" + System.currentTimeMillis();
        if (studentId == null || studentId.trim().isEmpty()) {
            studentId = "ID-" + (int)(1000 + Math.random() * 9000);
        }

        User newUser = new User(
                userId,
                name.trim(),
                email.trim(),
                password,
                "student",
                studentId.trim(),
                department != null && !department.trim().isEmpty() ? department.trim() : "General",
                null,
                null
        );

        dataStore.addUser(newUser);

        HttpSession session = req.getSession(true);
        session.setAttribute("currentUser", newUser);

        resp.setStatus(HttpServletResponse.SC_CREATED);
        resp.getWriter().write(String.format("{\"success\":true,\"user\":%s}", JsonUtil.userToJson(newUser)));
    }

    private void handleDemoLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, String> body = readJsonBody(req);
        String role = body.getOrDefault("role", req.getParameter("role"));
        if (role == null || role.trim().isEmpty()) role = "student";

        User demoUser = dataStore.getDemoUser(role);
        if (demoUser != null) {
            HttpSession session = req.getSession(true);
            session.setAttribute("currentUser", demoUser);
            resp.getWriter().write(String.format("{\"success\":true,\"user\":%s}", JsonUtil.userToJson(demoUser)));
        } else {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write("{\"success\":false,\"message\":\"Demo account not found for role: " + role + "\"}");
        }
    }

    private void handleLogout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        resp.getWriter().write("{\"success\":true,\"message\":\"Successfully logged out\"}");
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
