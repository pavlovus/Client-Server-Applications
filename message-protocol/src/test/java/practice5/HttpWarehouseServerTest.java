package practice5;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import practice4.Database;
import practice4.Product;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class HttpWarehouseServerTest {
    private static HttpWarehouseServer server;
    private static final int PORT = 8081;
    private static String validToken;
    private static long createdProductId;

    @BeforeAll
    public static void setup() throws Exception {
        Files.deleteIfExists(Path.of("test_warehouse.db"));

        Database database = new Database("jdbc:sqlite:test_warehouse.db");
        server = new HttpWarehouseServer(PORT, database);
        server.start();

        RestAssured.baseURI = "http://localhost";
        RestAssured.port = PORT;
    }

    @AfterAll
    public static void tearDown() throws IOException {
        if (server != null) server.stop();
        Files.deleteIfExists(Path.of("test_warehouse.db"));
    }

    @Test
    @Order(1)
    public void shouldLoginWithCorrectCredentials() {
        LoginRequest req = new LoginRequest();
        req.setUsername("user");
        req.setPassword("password");

        validToken = given()
                .contentType(ContentType.JSON)
                .body(req)
                .when()
                .post("/login")
                .then()
                .statusCode(200)
                .body("token", notNullValue())
                .extract().path("token");
    }

    @Test
    @Order(2)
    public void shouldNotLoginWithWrongPassword() {
        LoginRequest req = new LoginRequest();
        req.setUsername("user");
        req.setPassword("password1");

        given()
                .contentType(ContentType.JSON)
                .body(req)
                .when()
                .post("/login")
                .then()
                .statusCode(401);
    }

    @Test
    @Order(3)
    public void shouldNotAccessWhenUnauthorized() {
        given()
                .when()
                .get("/products/1")
                .then()
                .statusCode(401);
    }

    @Test
    @Order(4)
    public void shouldCreateProduct() {
        Product p = new Product("яблуко", "фрукти", 100, 25);

        Number idNum = given()
                .header("Authorization", "Bearer " + validToken)
                .contentType(ContentType.JSON)
                .body(p)
                .when()
                .put("/products")
                .then()
                .statusCode(201)
                .body("name", equalTo("яблуко"))
                .body("id", greaterThan(0))
                .extract().path("id");
        createdProductId = idNum.longValue();
    }

    @Test
    @Order(5)
    public void shouldNotCreateProductDuplicateName() {
        Product p = new Product("яблуко", "фрукти", 50, 30);

        given()
                .header("Authorization", "Bearer " + validToken)
                .contentType(ContentType.JSON)
                .body(p)
                .when()
                .put("/products")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(6)
    public void shouldGetProduct() {
        given()
                .header("Authorization", "Bearer " + validToken)
                .when()
                .get("/products/" + createdProductId)
                .then()
                .statusCode(200)
                .body("name", equalTo("яблуко"))
                .body("category", equalTo("фрукти"))
                .body("quantity", equalTo(100));
    }

    @Test
    @Order(7)
    public void shouldUpdateProduct() {
        Product update = new Product("яблуко", "фрукти", 150, 28);

        given()
                .header("Authorization", "Bearer " + validToken)
                .contentType(ContentType.JSON)
                .body(update)
                .when()
                .post("/products/" + createdProductId)
                .then()
                .statusCode(200)
                .body("quantity", equalTo(150))
                .body("price", equalTo(28.0f));
    }

    @Test
    @Order(8)
    public void shouldDeleteProduct() {
        given()
                .header("Authorization", "Bearer " + validToken)
                .when()
                .delete("/products/" + createdProductId)
                .then()
                .statusCode(204);
    }

    @Test
    @Order(9)
    public void shouldNotGetDeletedProduct() {
        given()
                .header("Authorization", "Bearer " + validToken)
                .when()
                .get("/products/" + createdProductId)
                .then()
                .statusCode(404);
    }

    @Test
    @Order(10)
    public void shouldReturn404WhenUpdatingNonExistentProduct() {
        Product update = new Product("неіснуючий", "категорія_67", 10, 50);
        given()
                .header("Authorization", "Bearer " + validToken)
                .contentType(ContentType.JSON)
                .body(update)
                .when()
                .post("/products/999999")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(11)
    public void shouldReturn400WhenCreatingProductWithNegativePrice() {
        Product p = new Product("поганий_продукт_228", "фрукти", 100, -10);
        given()
                .header("Authorization", "Bearer " + validToken)
                .contentType(ContentType.JSON)
                .body(p)
                .when()
                .put("/products")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(12)
    public void shouldReturn400WhenGetWithInvalidIdFormat() {
        given()
                .header("Authorization", "Bearer " + validToken)
                .when()
                .get("/products/invalid_id")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(13)
    public void shouldReturn405ForInvalidMethod() {
        given()
                .when()
                .get("/login")
                .then()
                .statusCode(405);
    }

    @Test
    @Order(14)
    public void shouldReturn401ForInvalidToken() {
        given()
                .header("Authorization", "Bearer invalid_token_format_228")
                .when()
                .get("/products/1")
                .then()
                .statusCode(401);
    }
    @Test
    @Order(15)
    public void shouldReturn400WhenCreatingProductWithEmptyName() {
        Product p = new Product("", "фрукти", 100, 25);
        given()
                .header("Authorization", "Bearer " + validToken)
                .contentType(ContentType.JSON)
                .body(p)
                .when()
                .put("/products")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(16)
    public void shouldReturn400WhenCreatingProductWithNegativeQuantity() {
        Product p = new Product("поганий_продукт_228", "фрукти", -10, 67);
        given()
                .header("Authorization", "Bearer " + validToken)
                .contentType(ContentType.JSON)
                .body(p)
                .when()
                .put("/products")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(17)
    public void shouldReturn401WhenAuthHeaderDoesNotStartWithBearer() {
        given()
                .header("Authorization", "Basic " + validToken)
                .when()
                .get("/products/1")
                .then()
                .statusCode(401);
    }

    @Test
    @Order(18)
    public void shouldReturn400ForMalformedJson() {
        given()
                .header("Authorization", "Bearer " + validToken)
                .contentType(ContentType.JSON)
                .body("{ \"name\": \"Apple\", ")
                .when()
                .put("/products")
                .then()
                .statusCode(400);
    }
}