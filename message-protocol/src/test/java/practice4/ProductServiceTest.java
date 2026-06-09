package practice4;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ProductServiceTest {
    private ProductService service;
    private Path tempDbFile;

    @BeforeEach
    void setUp() throws Exception {
        tempDbFile = Files.createTempFile("product_service_test_", ".db");
        Database db = new Database("jdbc:sqlite:" + tempDbFile.toAbsolutePath());
        service = new ProductService(new ProductRepository(db));
    }

    @AfterEach
    void tearDown() throws Exception { Files.deleteIfExists(tempDbFile); }

    @Nested
    class CreateTests {
        @Test
        void createAssignsId() {
            Product p = service.create(new Product("гречка", "крупи", 100, 49.99));
            assertTrue(p.getId() > 0);
            assertEquals("гречка", p.getName());
        }

        @Test
        void shouldNotCreateBlankName() {
            assertThrows(IllegalArgumentException.class, () -> service.create(new Product("", "крупи", 10, 5.0)));
        }

        @Test
        void shouldNotCreateNegativeQuantity() {
            assertThrows(IllegalArgumentException.class, () -> service.create(new Product("рис", "крупи", -1, 5.0)));
        }

        @Test
        void shouldNotCreateNegativePrice() {
            assertThrows(IllegalArgumentException.class, () -> service.create(new Product("рис", "крупи", 10, -1.0)));
        }
    }

    @Nested
    class ReadTests {

        @Test
        void shouldFindById() {
            Product created = service.create(new Product("рис", "крупи", 50, 30.0));
            Optional<Product> found = service.getById(created.getId());
            assertTrue(found.isPresent());
            assertEquals("рис", found.get().getName());
        }

        @Test
        void shouldNotFindByIdMissing() { assertTrue(service.getById(9999L).isEmpty()); }

        @Test
        void shouldFindByName() {
            service.create(new Product("цукор", "бакалія", 200, 25.0));
            assertTrue(service.getByName("цукор").isPresent());
        }

        @Test
        void shouldNotFindByNameMissing() { assertTrue(service.getByName("невідомий").isEmpty()); }
    }

    @Nested
    class UpdateTests {
        @Test
        void shouldUpdateExisting() {
            Product p = service.create(new Product("борошно", "бакалія", 10, 15.0));
            p.setQuantity(999);
            p.setPrice(20.0);
            assertTrue(service.update(p));
            assertEquals(999, service.getById(p.getId()).get().getQuantity());
        }

        @Test
        void shouldNotUpdateMissing() {
            Product ghost = new Product("ghost", null, 0, 0);
            ghost.setId(9999L);
            assertFalse(service.update(ghost));
        }

        @Test
        void shouldNotUpdateNoId() {
            assertThrows(IllegalArgumentException.class, () -> service.update(new Product("рис", null, 1, 1.0)));
        }
    }

    @Nested
    class DeleteTests {
        @Test
        void shouldDeleteExisting() {
            service.create(new Product("гречка", null, 10, 10.0));
            assertTrue(service.delete("гречка"));
            assertTrue(service.getByName("гречка").isEmpty());
        }

        @Test
        void shouldNotDeleteMissing() { assertFalse(service.delete("невідомий")); }

        @Test
        void shouldNotDeleteBlank() { assertThrows(IllegalArgumentException.class, () -> service.delete("")); }
    }

    @Nested
    class SearchTests {
        @BeforeEach
        void seed() {
            service.create(new Product("гречка","крупи",   100, 49.99));
            service.create(new Product("рис","крупи",    50, 30.00));
            service.create(new Product("цукор","бакалія", 200, 25.00));
            service.create(new Product("борошно","бакалія",  80, 18.00));
            service.create(new Product("олія","бакалія",  30, 75.00));
        }

        @Test
        void noFiltersReturnsAll() {
            PageResult<Product> result = service.search( ProductFilter.builder().pageSize(10).build() );
            assertEquals(5, result.getTotalCount());
            assertEquals(5, result.getItems().size());
        }

        @Test
        void shouldFilterByCategory() {
            PageResult<Product> result = service.search( ProductFilter.builder().category("крупи").pageSize(10).build() );
            assertEquals(2, result.getTotalCount());
            assertTrue(result.getItems().stream().allMatch(p -> "крупи".equals(p.getCategory())));
        }

        @Test
        void shouldFilterByNamePartial() {
            PageResult<Product> result = service.search( ProductFilter.builder().name("ри").pageSize(10).build() );
            assertEquals(1, result.getTotalCount());
            assertEquals("рис", result.getItems().getFirst().getName());
        }

        @Test
        void shouldFilterByMinPriceOnly() {
            PageResult<Product> result = service.search( ProductFilter.builder().minPrice(50.0).pageSize(10).build() );
            assertEquals(1, result.getTotalCount());
            assertEquals("олія", result.getItems().getFirst().getName());
        }

        @Test
        void shouldFilterByPriceRange() {
            PageResult<Product> result = service.search( ProductFilter.builder().minPrice(20.0).maxPrice(50.0).pageSize(10).build() );
            assertEquals(3, result.getTotalCount());
        }

        @Test
        void shouldFilterByQuantityRange() {
            PageResult<Product> result = service.search( ProductFilter.builder().minQuantity(50).maxQuantity(100).pageSize(10).build() );
            assertEquals(3, result.getTotalCount());
        }

        @Test
        void combinedFilterShouldWork() {
            PageResult<Product> result = service.search(ProductFilter.builder().category("бакалія").minPrice(30.0).pageSize(10).build());
            assertEquals(1, result.getTotalCount());
            assertEquals("олія", result.getItems().getFirst().getName());
        }

        @Test
        void shouldDoPaginationFirstPage() {
            PageResult<Product> page0 = service.search(ProductFilter.builder().page(0).pageSize(2).build());
            assertEquals(5, page0.getTotalCount());
            assertEquals(2, page0.getItems().size());
            assertEquals(3, page0.getTotalPages());
            assertTrue(page0.hasNextPage());
        }

        @Test
        void shouldDoPaginationLastPage() {
            PageResult<Product> page2 = service.search(ProductFilter.builder().page(2).pageSize(2).build());
            assertEquals(1, page2.getItems().size());
            assertFalse(page2.hasNextPage());
        }

        @Test
        void shouldFilterNoResults() {
            PageResult<Product> result = service.search(ProductFilter.builder().minPrice(1000.0).pageSize(10).build());
            assertTrue(result.isEmpty());
            assertEquals(0, result.getTotalCount());
        }

        @ParameterizedTest(name = "parse рядок: {0}")
        @CsvSource({
                "name=рис;page=0;pageSize=10,           1",
                "category=бакалія;page=0;pageSize=10,   3",
                "minPrice=3;page=0;pageSize=10,          5",
                "minQty=100;maxQty=200;page=0;pageSize=10, 2"
        })
        void shouldParseBasedFilter(String filterStr, int expectedCount) {
            ProductFilter filter = ProductFilter.parse(filterStr.trim());
            PageResult<Product> result = service.search(filter);
            assertEquals(expectedCount, result.getTotalCount());
        }
    }

    @Nested
    class StockTests {
        @Test
        void shouldAddToStock() {
            service.create(new Product("гречка", null, 10, 0));
            int newQty = service.addToStock("гречка", 50);
            assertEquals(60, newQty);
        }

        @Test
        void addToStockShouldCreateIfAbsent() {
            int qty = service.addToStock("новинка", 25);
            assertEquals(25, qty);
        }

        @Test
        void shouldDeleteFromStock() {
            service.create(new Product("рис", null, 100, 0));
            assertTrue(service.deleteFromStock("рис", 40));
            assertEquals(60, service.getByName("рис").get().getQuantity());
        }

        @Test
        void shouldNotDeleteFromStockIfInsufficientStock() {
            service.create(new Product("цукор", null, 5, 0));
            assertFalse(service.deleteFromStock("цукор", 100));
            assertEquals(5, service.getByName("цукор").get().getQuantity());
        }

        @Test
        void shouldNotAddNegative() {
            assertThrows(IllegalArgumentException.class, () -> service.addToStock("рис", -10));
        }

        @Test
        void shouldNotAddZero() {
            assertThrows(IllegalArgumentException.class, () -> service.addToStock("рис", 0));
        }

        @Test
        void shouldNotDeleteZero() {
            assertThrows(IllegalArgumentException.class, () -> service.deleteFromStock("рис", 0));
        }

        @Test
        void shouldNotDeleteFromStockNonExistingProduct() { assertFalse(service.deleteFromStock("невідомий", 10)); }

        @Test
        void shouldCreateProductWithZeroPriceAndNullCategory() {
            service.addToStock("новинка", 25);
            Product p = service.getByName("новинка").get();
            assertEquals(0.0, p.getPrice(), 0.001);
            assertNull(p.getCategory());
        }
    }

    @Nested
    class PriceTests {
        @Test
        void shouldSetPrice() {
            service.create(new Product("гречка", null, 10, 0));
            assertTrue(service.setPrice("гречка", 55.50));
            assertEquals(55.50, service.getByName("гречка").get().getPrice(), 0.001);
        }

        @Test
        void shouldNotSetPriceMissing() { assertFalse(service.setPrice("невідомий", 10.0)); }

        @Test
        void shouldNotSetNegativePrice() {
            assertThrows(IllegalArgumentException.class, () -> service.setPrice("рис", -5.0));
        }

        @Test
        void shouldSetPriceZero() {
            service.create(new Product("гречка", null, 10, 5.0));
            assertTrue(service.setPrice("гречка", 0.0));
            assertEquals(0.0, service.getByName("гречка").get().getPrice(), 0.001);
        }
    }

    @Nested
    class CategoryTests {
        @Test
        void shouldAddNewCategory() { assertTrue(service.addCategory("овочі")); }

        @Test
        void shouldNotAddDuplicateCategory() {
            service.addCategory("крупи");
            assertFalse(service.addCategory("крупи"));
        }

        @Test
        void blankCategoryShouldThrowException() {
            assertThrows(IllegalArgumentException.class, () -> service.addCategory(""));
            assertThrows(IllegalArgumentException.class, () -> service.addCategory("   "));
        }

        @Test
        void shouldAssignCategoryToProduct() {
            service.create(new Product("гречка", null, 10, 0));
            service.addCategory("крупи");
            assertTrue(service.assignCategory("гречка", "крупи"));
            assertEquals("крупи", service.getByName("гречка").get().getCategory());
        }

        @Test
        void shouldNotAssignCategoryNonExistingProduct() {
            service.addCategory("крупи");
            assertFalse(service.assignCategory("невідомий", "крупи"));
        }
    }
}