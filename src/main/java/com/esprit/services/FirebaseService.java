package com.esprit.services;

import com.esprit.models.Conversation;
import com.esprit.models.Message;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class FirebaseService {
    private static final String DATABASE_URL =
            "https://pidev-643da-default-rtdb.europe-west1.firebasedatabase.app/";

    // Create or overwrite a conversation object
    public static void createConversation(Conversation convo) throws Exception {
        sendPutRequest("conversations/" + convo.conversationId + ".json", convo);
    }

    // Post a new message under a given conversation
    public static void sendMessage(String conversationId, Message message) throws Exception {
        sendPostRequest("conversations/" + conversationId + "/messages.json", message);
    }

    // Update specific fields of a conversation without affecting other fields like messages
    public static void updateConversationStatus(String conversationId, String status) throws Exception {
        URL url = new URL(DATABASE_URL + "conversations/" + conversationId + "/status.json");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("PUT");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");

        try (OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream())) {
            writer.write(new Gson().toJson(status));
            writer.flush();
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new Exception("Failed to update conversation status. Server returned: " + responseCode);
        }
    }

    // Method to send PATCH requests
    private static void sendPatchRequest(String endpoint, Object data) throws Exception {
        URL url = new URL(DATABASE_URL + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("PATCH");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");

        try (OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream())) {
            writer.write(new Gson().toJson(data));
            writer.flush();
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new Exception("PATCH request failed. Server returned: " + responseCode);
        }
    }

    // Fetch all messages in a conversation
    public static List<Message> getMessages(String conversationId) throws Exception {
        URL url = new URL(DATABASE_URL + "conversations/" + conversationId + "/messages.json");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        try (Reader reader = new InputStreamReader(conn.getInputStream())) {
            Type type = new TypeToken<Map<String, Message>>() {}.getType();
            Map<String, Message> map = new Gson().fromJson(reader, type);
            if (map == null) return Collections.emptyList();

            List<Message> list = new ArrayList<>(map.values());
            // Populate messageId from the key
            map.forEach((key, msg) -> msg.messageId = key);
            list.sort(Comparator.comparingLong(m -> m.timestamp));
            return list;
        }
    }

    // Read the status ("open" / "validated" / "cancelled")
    public static String getConversationStatus(String conversationId) throws Exception {
        URL url = new URL(DATABASE_URL + "conversations/" + conversationId + "/status.json");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line = br.readLine();
            return line != null ? line.replace("\"", "") : null;
        }
    }

    // Fetch all conversations where currentUser is involved
    public static List<Conversation> getConversationsForUser(int userId) throws Exception {
        // Instead of using Firebase queries that require indexes, we'll fetch all conversations
        // and filter them in the application
        List<Conversation> result = new ArrayList<>();

        try {
            URL url = new URL(DATABASE_URL + "conversations.json");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            try (Reader r = new InputStreamReader(conn.getInputStream())) {
                Type t = new TypeToken<Map<String, Conversation>>() {}.getType();
                Map<String, Conversation> allConversations = new Gson().fromJson(r, t);

                if (allConversations != null) {
                    for (Map.Entry<String, Conversation> entry : allConversations.entrySet()) {
                        String key = entry.getKey();
                        Conversation convo = entry.getValue();
                        convo.conversationId = key;

                        // Filter conversations where the user is involved
                        if (convo.postOwnerId == userId || convo.interestedUserId == userId) {
                            result.add(convo);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching conversations: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }

        return result;
    }

    // Fetch a single conversation
    public static Conversation getConversation(String conversationId) throws Exception {
        URL url = new URL(DATABASE_URL + "conversations/" + conversationId + ".json");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        try (Reader r = new InputStreamReader(conn.getInputStream())) {
            Conversation convo = new Gson().fromJson(r, Conversation.class);
            if (convo != null) convo.conversationId = conversationId;
            return convo;
        }
    }

    // Delete a conversation (and all its messages)
    public static void deleteConversation(String conversationId) throws Exception {
        URL url = new URL(DATABASE_URL + "conversations/" + conversationId + ".json");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("DELETE");
        int code = conn.getResponseCode();
        if (code >= 400) throw new IOException("Delete failed HTTP " + code);
        conn.getInputStream().close();
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private static void sendPutRequest(String path, Object data) throws Exception {
        HttpURLConnection conn = openJsonConnection("PUT", path);
        try (Writer w = new OutputStreamWriter(conn.getOutputStream())) {
            new Gson().toJson(data, w);
        }
        checkForError(conn);
    }

    private static void sendPostRequest(String path, Object data) throws Exception {
        HttpURLConnection conn = openJsonConnection("POST", path);
        try (Writer w = new OutputStreamWriter(conn.getOutputStream())) {
            new Gson().toJson(data, w);
        }
        checkForError(conn);
    }

    private static HttpURLConnection openJsonConnection(String method, String path) throws Exception {
        URL url = new URL(DATABASE_URL + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        return conn;
    }

    private static void checkForError(HttpURLConnection conn) throws Exception {
        int code = conn.getResponseCode();
        if (code >= 400) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line).append('\n');
                throw new IOException("Firebase HTTP " + code + ":\n" + sb);
            }
        }
        conn.getInputStream().close();
    }

    // This method isn't used in the updated code but is kept for reference
    private static Map<String, Conversation> getConversationMap(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        try (Reader r = new InputStreamReader(conn.getInputStream())) {
            Type t = new TypeToken<Map<String, Conversation>>() {}.getType();
            Map<String, Conversation> m = new Gson().fromJson(r, t);
            if (m != null) {
                // populate each conversationId from its key
                m.forEach((key, convo) -> convo.conversationId = key);
                return m;
            }
        }
        return Collections.emptyMap();
    }

    // Add this method to your FirebaseService class
    public static boolean checkConversationExists(String conversationId) throws Exception {
        try {
            URL url = new URL(DATABASE_URL + "conversations/" + conversationId + ".json");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String response = reader.readLine();
                    // If response is "null", the conversation doesn't exist
                    return response != null && !response.equals("null");
                }
            }
            return false;
        } catch (Exception e) {
            System.err.println("Error checking conversation existence: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    /*------------------------------------------------------------*/

}