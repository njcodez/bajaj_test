package com.example.bajajtest.dto;

/**
 * Simple DTO that models a common response shape.
 * Note: StartupRunner performs robust parsing of raw JSON, so this class is optional
 * but included because you asked for it.
 */
public class WebhookResponse {
    private Data data;
    private String webhook;
    private String accessToken;

    public WebhookResponse() {
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public String getWebhook() {
        return webhook;
    }

    public void setWebhook(String webhook) {
        this.webhook = webhook;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public static class Data {
        private String webhook;
        private String accessToken;

        public Data() {
        }

        public String getWebhook() {
            return webhook;
        }

        public void setWebhook(String webhook) {
            this.webhook = webhook;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }
    }
}
