package com.esprit.services;

import io.github.cdimascio.dotenv.Dotenv;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebPurifyModerationService {
    private static final Dotenv dotenv = Dotenv.load();
    private static final String API_KEY = dotenv.get("WEBPURIFY_API_KEY");
    private static final String API_ENDPOINT = "https://api1.webpurify.com/services/rest/";

    private static final Set<String> INAPPROPRIATE_WORDS = new HashSet<>(Arrays.asList(
            "inappropriate", "offensive", "curse", "badword", "profanity",
            // Add more inappropriate words here
            "damn", "hell", "ass", "shit", "fuck"
    ));

    /**
     * Check if content is appropriate using WebPurify API
     *
     * @param content Text to check
     * @return true if content is appropriate, false otherwise
     * @throws Exception if API call fails
     */
    public boolean isAppropriateContent(String content) throws Exception {
        String encodedText = URLEncoder.encode(content, StandardCharsets.UTF_8.toString());
        String urlString = API_ENDPOINT +
                "?method=webpurify.live.check" +
                "&api_key=" + API_KEY +
                "&text=" + encodedText +
                "&format=json";

        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            String responseStr = response.toString();

            Pattern pattern = Pattern.compile("\"found\"\\s*:\\s*\"(\\d+)\"");
            Matcher matcher = pattern.matcher(responseStr);

            if (matcher.find()) {
                int found = Integer.parseInt(matcher.group(1));
                return found == 0; // Return true if no inappropriate content found
            } else {
                // If still failing, try to extract just from the visible structure
                System.out.println("Response from WebPurify: " + responseStr);

                // Fallback to a more relaxed pattern if the first one doesn't match
                pattern = Pattern.compile("found\"?\\s*:?\\s*\"?(\\d+)\"?");
                matcher = pattern.matcher(responseStr);

                if (matcher.find()) {
                    int found = Integer.parseInt(matcher.group(1));
                    return found == 0;
                }

                // If response contains "found":"0", assume it's clean
                if (responseStr.contains("\"found\":\"0\"") || responseStr.contains("\"found\": \"0\"")) {
                    return true;
                }

                throw new Exception("Could not parse 'found' field in WebPurify response: " + responseStr);
            }
        }
        throw new Exception("Failed to check content: HTTP error code " + responseCode);
    }

    /**
     * Local fallback method to check content if the API call fails
     *
     * @param content Text to check
     * @return true if content seems appropriate, false if it contains inappropriate words
     */
    public boolean checkContentLocally(String content) {
        if (content == null || content.trim().isEmpty()) {
            return true;
        }

        // Convert to lowercase for case-insensitive matching
        String lowerCaseContent = content.toLowerCase();

        // Check each word in our inappropriate word list
        for (String word : INAPPROPRIATE_WORDS) {
            // Use word boundary checks for whole word matching
            String regex = "\\b" + Pattern.quote(word.toLowerCase()) + "\\b";
            if (Pattern.compile(regex).matcher(lowerCaseContent).find()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Check if content is approximately appropriate - a simple check based on common patterns
     * This could be expanded with more sophisticated checks
     *
     * @param content Text to check
     * @return true if content seems appropriate, false otherwise
     */
    public boolean isContentApproximatelyAppropriate(String content) {
        // Check for excessive punctuation/capitalization (common in offensive content)
        Pattern excessivePunctuation = Pattern.compile("(\\!{3,}|\\?{3,})");
        if (excessivePunctuation.matcher(content).find()) {
            return false;
        }

        // Check for email solicitation
        Pattern emailPattern = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}");
        if (emailPattern.matcher(content).find()) {
            // If email is found, check if it's in a solicitation context
            String lowerContent = content.toLowerCase();
            if (lowerContent.contains("contact me at") ||
                    lowerContent.contains("email me") ||
                    lowerContent.contains("send me")) {
                return false;
            }
        }

        // Check for direct payment requests or suspicious offers
        String lowerContent = content.toLowerCase();
        if (lowerContent.contains("send money") ||
                lowerContent.contains("wire transfer") ||
                lowerContent.contains("western union") ||
                lowerContent.contains("paypal me")) {
            return false;
        }

        return true;
    }
}