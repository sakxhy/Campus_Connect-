package com.campusconnect.model;

import java.io.Serializable;

public class Stats implements Serializable {
    private static final long serialVersionUID = 1L;

    private int total;
    private int open;
    private int inProgress;
    private int resolved;
    private int closed;
    private int urgent;

    public Stats() {}

    public Stats(int total, int open, int inProgress, int resolved, int closed, int urgent) {
        this.total = total;
        this.open = open;
        this.inProgress = inProgress;
        this.resolved = resolved;
        this.closed = closed;
        this.urgent = urgent;
    }

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }

    public int getOpen() { return open; }
    public void setOpen(int open) { this.open = open; }

    public int getInProgress() { return inProgress; }
    public void setInProgress(int inProgress) { this.inProgress = inProgress; }

    public int getResolved() { return resolved; }
    public void setResolved(int resolved) { this.resolved = resolved; }

    public int getClosed() { return closed; }
    public void setClosed(int closed) { this.closed = closed; }

    public int getUrgent() { return urgent; }
    public void setUrgent(int urgent) { this.urgent = urgent; }
}
