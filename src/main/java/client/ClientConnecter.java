package client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class ClientConnecter {
    private final String baseUrl;
    private final String username;
    private final String password;
    private final HttpClient client;
    private final String params;

    public ClientConnecter(String baseUrl, String username, String password) {
        this.baseUrl = baseUrl;
        this.username = username;
        this.password = password;
        this.client = HttpClient.newHttpClient();
        this.params = "?username=" + username + "&password=" + password;
    }

    public List<SessionSummary> listSessions() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/sessions/" + username + "?password=" + password))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
        return JsonUtil.GSON.fromJson(response.body(), new TypeToken<List<SessionSummary>>() {
        }.getType());
    }

    public GameState newGame(UUID sessionId) throws Exception {
        System.out.println(baseUrl + "/" + sessionId + "/reset" + params);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/" + sessionId + "/reset" + params))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
        return GameState.fromJson(response.body());
    }

    public void finishGame(UUID sessionId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/" + sessionId + "/finish" + params))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        client.send(request, BodyHandlers.ofString());
    }

    public GameState startGame() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/start" + params))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        System.out.println("Request: " + request);
        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
        return GameState.fromJson(response.body());
    }

    public GameState placeBet(UUID sessionId, int amount) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/" + sessionId + "/bet/" + amount + params))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
        return GameState.fromJson(response.body());
    }

    public GameState hit(UUID sessionId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/" + sessionId + "/hit" + params))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
        return GameState.fromJson(response.body());
    }

    public GameState stand(UUID sessionId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/" + sessionId + "/stand" + params))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
        return GameState.fromJson(response.body());
    }

    public GameState resumeSession(UUID sessionId) throws Exception {
        System.out.println("I am inside resume session with the session id: " + sessionId);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/resume/" + sessionId + params))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
        return new Gson().fromJson(response.body(), GameState.class);
    }
}
