package practice5;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import practice4.Product;
import practice4.ProductService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;

public class ProductHandler implements HttpHandler {
    private final ProductService productService;
    private final ObjectMapper mapper = new ObjectMapper();

    public ProductHandler(ProductService productService) {
        this.productService = productService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        try {
            if ("GET".equalsIgnoreCase(method)) handleGet(exchange, path);
            else if ("PUT".equalsIgnoreCase(method)) handlePut(exchange);
            else if ("POST".equalsIgnoreCase(method)) handlePost(exchange, path);
            else if ("DELETE".equalsIgnoreCase(method)) handleDelete(exchange, path);
            else sendResponse(exchange, 405, new ErrorResponse("Method Not Allowed"));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            sendResponse(exchange, 400, new ErrorResponse("Invalid JSON format"));
        } catch (Exception e) { 
            sendResponse(exchange, 500, new ErrorResponse("Internal Server Error: " + e.getMessage())); 
        }
    }

    private void handleGet(HttpExchange exchange, String path) throws IOException {
        Long id = extractId(path);
        if (id == null) {
            sendResponse(exchange, 400, new ErrorResponse("Invalid ID"));
            return;
        }

        Optional<Product> product = productService.getById(id);
        if (product.isPresent()) sendResponse(exchange, 200, product.get());
        else sendResponse(exchange, 404, new ErrorResponse("Product not found"));
    }

    private void handlePut(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            Product newProduct = mapper.readValue(is, Product.class);

            if (newProduct.getName() == null || newProduct.getName().isBlank()) {
                sendResponse(exchange, 400, new ErrorResponse("Product name cannot be empty"));
                return;
            }

            Optional<Product> existing = productService.getByName(newProduct.getName());
            if (existing.isPresent()) {
                sendResponse(exchange, 400, new ErrorResponse("Product with this name already exists"));
                return;
            }

            Product created = productService.create(newProduct);
            sendResponse(exchange, 201, created);
        } catch (IllegalArgumentException e) { sendResponse(exchange, 400, new ErrorResponse(e.getMessage())); }
    }

    private void handlePost(HttpExchange exchange, String path) throws IOException {
        Long id = extractId(path);
        if (id == null) {
            sendResponse(exchange, 400, new ErrorResponse("Invalid ID"));
            return;
        }

        try (InputStream is = exchange.getRequestBody()) {
            Product productToUpdate = mapper.readValue(is, Product.class);
            productToUpdate.setId(id);

            Optional<Product> existing = productService.getById(id);
            if (existing.isEmpty()) {
                sendResponse(exchange, 404, new ErrorResponse("Product not found"));
                return;
            }

            boolean updated = productService.update(productToUpdate);
            if (updated) sendResponse(exchange, 200, productToUpdate);
            else sendResponse(exchange, 500, new ErrorResponse("Failed to update product"));
        } catch (IllegalArgumentException e) { sendResponse(exchange, 400, new ErrorResponse(e.getMessage())); }
    }

    private void handleDelete(HttpExchange exchange, String path) throws IOException {
        Long id = extractId(path);
        if (id == null) {
            sendResponse(exchange, 400, new ErrorResponse("Invalid ID"));
            return;
        }

        Optional<Product> existing = productService.getById(id);
        if (existing.isPresent()) {
            boolean deleted = productService.delete(existing.get().getName());

            if (deleted) sendResponse(exchange, 204, null);
            else sendResponse(exchange, 500, new ErrorResponse("Failed to delete product"));
        } else {
            sendResponse(exchange, 404, new ErrorResponse("Product not found"));
        }
    }

    private Long extractId(String path) {
        String[] parts = path.split("/");
        if (parts.length >= 3)
            try {
                return Long.parseLong(parts[parts.length - 1]);
            } catch (NumberFormatException e) { return null; }

        return null;
    }

    private void sendResponse(HttpExchange exchange, int statusCode, Object responseObj) throws IOException {
        if (responseObj == null) {
            exchange.sendResponseHeaders(statusCode, -1);
            return;
        }

        String jsonResponse = mapper.writeValueAsString(responseObj);
        byte[] bytes = jsonResponse.getBytes();
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
    }
}