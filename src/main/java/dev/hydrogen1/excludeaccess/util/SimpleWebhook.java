package dev.hydrogen1.excludeaccess.util;

import com.google.gson.JsonObject;
import lombok.val;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Objects;

/**
 * A simple webhook client.
 */
public final class SimpleWebhook {
    private final String url;

    /**
     * Create a new webhook client.
     * @param url The webhook URL
     */
    public SimpleWebhook(String url) {
        this.url = url;
    }

    /**
     * Send a message to the webhook.
     * @param text The message
     * @throws IOException If a network error occurs
     */
    public void send(@NonNull String text) throws IOException {
        Objects.requireNonNull(text);
        val json = new JsonObject();
        json.addProperty("content", text);

        // Send the message
        val connection = (HttpURLConnection) URI.create(this.url).toURL().openConnection();
        connection.addRequestProperty("Content-Type", "application/json");
        connection.addRequestProperty("User-Agent", "SimpleWebhook/1.0");
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");

        val output = connection.getOutputStream();
        output.write(json.toString().getBytes());
        output.flush();
        output.close();

        connection.getInputStream().close(); // Ignore the response
        connection.disconnect();
    }
}
