package com.campusconnect.controller;

import com.campusconnect.model.Stats;
import com.campusconnect.service.DataStore;
import com.campusconnect.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(name = "StatsServlet", urlPatterns = {"/api/stats"})
public class StatsServlet extends HttpServlet {
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
        Stats stats = dataStore.getStats();
        resp.getWriter().write(JsonUtil.statsToJson(stats));
    }
}
