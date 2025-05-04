package com.esprit.main;

import com.esprit.models.Conversation;
import com.esprit.models.Message;
import com.esprit.services.FirebaseService;
import com.google.gson.Gson;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class FirebaseTest {
    private static final String TEST_CONVO = "testPost_testUser";

    public static void main(String[] args) {
        try {
            // 1. Create a test conversation
            Conversation convo = new Conversation(
                    TEST_CONVO,
                    1,
                    2,
                    3,
                    "open"
            );
            FirebaseService.createConversation(convo);
            System.out.println("✅ Created conversation");

            // 2. Send a test message
            Message msg = new Message(
                    null,
                    3,
                    "This is a test message",
                    System.currentTimeMillis()
            );
            FirebaseService.sendMessage(TEST_CONVO, msg);
            System.out.println("✅ Sent message");

            // 3. Fetch it back
            List<Message> messages = FirebaseService.getMessages(TEST_CONVO);
            System.out.println("🔄 Fetched messages:");
            for (Message m : messages) {
                System.out.printf("  [%d] %s: %s%n", m.timestamp, m.senderId, m.content);
            }

            // 4. (Optional) Clean up:
            // You can delete the node via REST or in console to keep things tidy.
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

