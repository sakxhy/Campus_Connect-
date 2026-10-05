package com.campusconnect.util;

import com.campusconnect.model.*;

import java.util.*;

/**
 * High-performance, lightweight JSON utility for serializing and parsing
 * API requests and responses without external third-party dependencies.
 */
public class JsonUtil {

    public static String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (ch < 32 || ch >= 127) {
                        String hex = Integer.toHexString(ch);
                        sb.append("\\u");
                        for (int k = 0; k < 4 - hex.length(); k++) sb.append('0');
                        sb.append(hex);
                    } else {
                        sb.append(ch);
                    }
            }
        }
        return sb.toString();
    }

    public static String userToJson(User u) {
        if (u == null) return "null";
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"id\":\"").append(escapeJson(u.getId())).append("\",");
        sb.append("\"name\":\"").append(escapeJson(u.getName())).append("\",");
        sb.append("\"email\":\"").append(escapeJson(u.getEmail())).append("\",");
        sb.append("\"role\":\"").append(escapeJson(u.getRole())).append("\",");
        sb.append("\"studentId\":").append(u.getStudentId() != null ? "\"" + escapeJson(u.getStudentId()) + "\"" : "null").append(",");
        sb.append("\"department\":").append(u.getDepartment() != null ? "\"" + escapeJson(u.getDepartment()) + "\"" : "null").append(",");
        sb.append("\"phone\":").append(u.getPhone() != null ? "\"" + escapeJson(u.getPhone()) + "\"" : "null").append(",");
        sb.append("\"title\":").append(u.getTitle() != null ? "\"" + escapeJson(u.getTitle()) + "\"" : "null");
        sb.append("}");
        return sb.toString();
    }

    public static String timelineItemToJson(TimelineItem t) {
        if (t == null) return "null";
        return String.format("{\"status\":\"%s\",\"label\":\"%s\",\"date\":\"%s\",\"note\":\"%s\"}",
                escapeJson(t.getStatus()),
                escapeJson(t.getLabel()),
                escapeJson(t.getDate()),
                escapeJson(t.getNote()));
    }

    public static String commentToJson(Comment c) {
        if (c == null) return "null";
        return String.format("{\"id\":\"%s\",\"author\":\"%s\",\"role\":\"%s\",\"text\":\"%s\",\"date\":\"%s\"}",
                escapeJson(c.getId()),
                escapeJson(c.getAuthor()),
                escapeJson(c.getRole()),
                escapeJson(c.getText()),
                escapeJson(c.getDate()));
    }

    public static String userSummaryToJson(UserSummary s) {
        if (s == null) return "null";
        return String.format("{\"name\":\"%s\",\"email\":\"%s\",\"studentId\":\"%s\"}",
                escapeJson(s.getName()),
                escapeJson(s.getEmail()),
                escapeJson(s.getStudentId()));
    }

    public static String ticketToJson(Ticket t) {
        if (t == null) return "null";
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"id\":\"").append(escapeJson(t.getId())).append("\",");
        sb.append("\"title\":\"").append(escapeJson(t.getTitle())).append("\",");
        sb.append("\"category\":\"").append(escapeJson(t.getCategory())).append("\",");
        sb.append("\"priority\":\"").append(escapeJson(t.getPriority())).append("\",");
        sb.append("\"status\":\"").append(escapeJson(t.getStatus())).append("\",");
        sb.append("\"location\":\"").append(escapeJson(t.getLocation())).append("\",");
        sb.append("\"description\":\"").append(escapeJson(t.getDescription())).append("\",");
        sb.append("\"submittedBy\":").append(userSummaryToJson(t.getSubmittedBy())).append(",");
        sb.append("\"assignedTo\":\"").append(escapeJson(t.getAssignedTo())).append("\",");
        sb.append("\"createdAt\":\"").append(escapeJson(t.getCreatedAt())).append("\",");
        sb.append("\"updatedAt\":\"").append(escapeJson(t.getUpdatedAt())).append("\",");

        // Timeline array
        sb.append("\"timeline\":[");
        List<TimelineItem> timeline = t.getTimeline();
        if (timeline != null) {
            for (int i = 0; i < timeline.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(timelineItemToJson(timeline.get(i)));
            }
        }
        sb.append("],");

        // Comments array
        sb.append("\"comments\":[");
        List<Comment> comments = t.getComments();
        if (comments != null) {
            for (int i = 0; i < comments.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(commentToJson(comments.get(i)));
            }
        }
        sb.append("]");

        sb.append("}");
        return sb.toString();
    }

    public static String ticketListToJson(List<Ticket> list) {
        if (list == null) return "[]";
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(ticketToJson(list.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    public static String statsToJson(Stats s) {
        if (s == null) return "{}";
        return String.format("{\"total\":%d,\"open\":%d,\"inProgress\":%d,\"resolved\":%d,\"closed\":%d,\"urgent\":%d}",
                s.getTotal(), s.getOpen(), s.getInProgress(), s.getResolved(), s.getClosed(), s.getUrgent());
    }

    /**
     * Parses simple flat JSON string into key-value map.
     */
    public static Map<String, String> parseJsonMap(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null || json.trim().isEmpty()) return map;

        String trimmed = json.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
        }

        boolean inQuotes = false;
        boolean escape = false;
        StringBuilder currentToken = new StringBuilder();
        List<String> pairs = new ArrayList<>();

        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (escape) {
                currentToken.append(c);
                escape = false;
            } else if (c == '\\') {
                currentToken.append(c);
                escape = true;
            } else if (c == '"') {
                inQuotes = !inQuotes;
                currentToken.append(c);
            } else if (c == ',' && !inQuotes) {
                pairs.add(currentToken.toString().trim());
                currentToken.setLength(0);
            } else {
                currentToken.append(c);
            }
        }
        if (currentToken.length() > 0) {
            pairs.add(currentToken.toString().trim());
        }

        for (String pair : pairs) {
            int colonIdx = -1;
            boolean q = false;
            boolean esc = false;
            for (int j = 0; j < pair.length(); j++) {
                char ch = pair.charAt(j);
                if (esc) {
                    esc = false;
                } else if (ch == '\\') {
                    esc = true;
                } else if (ch == '"') {
                    q = !q;
                } else if (ch == ':' && !q) {
                    colonIdx = j;
                    break;
                }
            }

            if (colonIdx != -1) {
                String key = cleanJsonToken(pair.substring(0, colonIdx).trim());
                String val = cleanJsonToken(pair.substring(colonIdx + 1).trim());
                map.put(key, val);
            }
        }

        return map;
    }

    private static String cleanJsonToken(String s) {
        if (s == null) return "";
        s = s.trim();
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
            s = s.substring(1, s.length() - 1);
        }
        return unescapeJson(s);
    }

    public static String unescapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        boolean escape = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (escape) {
                switch (c) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case 'u':
                        if (i + 4 < s.length()) {
                            String hex = s.substring(i + 1, i + 5);
                            try {
                                sb.append((char) Integer.parseInt(hex, 16));
                                i += 4;
                            } catch (NumberFormatException e) {
                                sb.append(c);
                            }
                        } else {
                            sb.append(c);
                        }
                        break;
                    default: sb.append(c); break;
                }
                escape = false;
            } else if (c == '\\') {
                escape = true;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
