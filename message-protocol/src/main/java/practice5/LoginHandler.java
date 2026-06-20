package practice5;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class LoginHandler implements HttpHandler {
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, new ErrorResponse("Method Not Allowed"));
            return;
        }

        try (InputStream is = exchange.getRequestBody()) {
            LoginRequest request = mapper.readValue(is, LoginRequest.class);

            // Хардкод єдиного користувача за яким буде проходити аутентифікація, оскільки не потрібно реалізовувати їхню реєстрацію
            // Дані за якими буде проходити: логін - user, пароль - password
            if ("user".equals(request.getUsername()) && "password".equals(request.getPassword())) {
                String token = JwtUtil.generateToken(request.getUsername());
                sendResponse(exchange, 200, new LoginResponse(token));
            } else sendResponse(exchange, 401, new ErrorResponse("Invalid username or password"));
        } catch (Exception e) { sendResponse(exchange, 400, new ErrorResponse("Bad Request: " + e.getMessage())); }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, Object responseObj) throws IOException {
        String jsonResponse = mapper.writeValueAsString(responseObj);
        byte[] bytes = jsonResponse.getBytes();
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
    }
}
