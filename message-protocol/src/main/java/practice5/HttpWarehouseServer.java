package practice5;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import practice4.Database;
import practice4.ProductRepository;
import practice4.ProductService;

import java.io.IOException;
import java.net.InetSocketAddress;

public class HttpWarehouseServer {
    private HttpServer server;
    private final int port;
    private final Database database;

    public HttpWarehouseServer(int port, Database database) {
        this.port = port;
        this.database = database;
    }

    public void start() throws IOException {
        ProductRepository repository = new ProductRepository(database);
        ProductService productService = new ProductService(repository);

        server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/login", new LoginHandler());

        HttpContext productsContext = server.createContext("/products", new ProductHandler(productService));
        productsContext.setAuthenticator(new JwtAuthenticator());

        server.setExecutor(null);
        server.start();
        System.out.println("HTTP Server started on port " + port);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("HTTP Server stopped");
        }
    }
}