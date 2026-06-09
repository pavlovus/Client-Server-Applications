package practice2.warehouse;

import practice1.Message;
import practice4.PageResult;
import practice4.Product;
import practice4.ProductFilter;
import practice4.ProductService;

import java.util.Optional;
import java.util.stream.Collectors;

public class CommandHandler {
    private final ProductService productService;

    public CommandHandler(ProductService productService) {
        this.productService = productService;
    }

    public String handle(Message message) {
        CommandType command = CommandType.fromCode(message.getCType());
        String content = message.getMessageContent();

        try {
            return switch (command) {

                case GET_QUANTITY -> {
                    Optional<Product> p = productService.getByName(content.trim());
                    yield p.map(pr -> "OK:" + pr.getQuantity()).orElse("ERROR: PRODUCT DOES NOT EXIST");
                }
                case DELETE_FROM_STOCK -> {
                    String[] parts = content.split(":", 2);
                    boolean ok = productService.deleteFromStock(parts[0], Integer.parseInt(parts[1]));
                    yield ok ? "OK" : "ERROR: INSUFFICIENT STOCK";
                }
                case ADD_TO_STOCK -> {
                    String[] parts = content.split(":", 2);
                    int newQty = productService.addToStock(parts[0], Integer.parseInt(parts[1]));
                    yield "OK:" + newQty;
                }
                case ADD_GROUP -> {
                    boolean ok = productService.addCategory(content.trim());
                    yield ok ? "OK" : "ERROR: GROUP ALREADY EXISTS";
                }
                case ADD_PRODUCT_TO_GROUP -> {
                    String[] parts = content.split(":", 2);
                    boolean ok = productService.assignCategory(parts[1], parts[0]);
                    yield ok ? "OK" : "ERROR: OPERATION FAILED";
                }
                case SET_PRICE -> {
                    String[] parts = content.split(":", 2);
                    boolean ok = productService.setPrice(parts[0], Double.parseDouble(parts[1]));
                    yield ok ? "OK" : "ERROR: PRODUCT DOES NOT EXIST";
                }

                case CREATE -> {
                    Product toCreate = Product.deserialize(content);
                    Product created  = productService.create(toCreate);
                    yield "OK:" + created.serialize();
                }
                case GET -> {
                    long id = Long.parseLong(content.trim());
                    Optional<Product> found = productService.getById(id);
                    yield found.map(p -> "OK:" + p.serialize()).orElse("ERROR: PRODUCT DOES NOT EXIST");
                }
                case UPDATE -> {
                    Product toUpdate = Product.deserialize(content);
                    boolean ok = productService.update(toUpdate);
                    yield ok ? "OK" : "ERROR: PRODUCT DOES NOT EXIST";
                }
                case DELETE -> {
                    boolean ok = productService.delete(content.trim());
                    yield ok ? "OK" : "ERROR: PRODUCT DOES NOT EXIST";
                }
                case SEARCH -> {
                    ProductFilter filter  = ProductFilter.parse(content);
                    PageResult<Product> page = productService.search(filter);

                    if (page.isEmpty()) {
                        yield "OK:EMPTY";
                    } else {
                        String items = page.getItems().stream().map(Product::serialize).collect(Collectors.joining(","));
                        yield "OK:" + page.getTotalCount() + ":" + page.getPage() + ":" + page.getPageSize() + ":" + items;
                    }
                }
            };
        } catch (IllegalArgumentException e) {return "ERROR: INVALID ARGUMENTS — " + e.getMessage();
        } catch (Exception e) {return "ERROR: INTERNAL — " + e.getMessage();}
    }
}