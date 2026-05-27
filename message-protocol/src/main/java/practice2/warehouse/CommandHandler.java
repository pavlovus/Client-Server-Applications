package practice2.warehouse;

import practice1.Message;

public class CommandHandler {
    private final Warehouse warehouse;

    public CommandHandler(Warehouse warehouse) { this.warehouse = warehouse; }

    public String handle(Message message) {
        CommandType command = CommandType.fromCode(message.getCType());
        String[] parts = message.getMessageContent().split(":");

        return switch (command) {
            case GET_QUANTITY -> {
                int quantity = warehouse.getProductQuantity(parts[0]);
                yield quantity != -1 ? "OK:" + quantity : "ERROR: PRODUCT DOES NOT EXIST";
            }
            case DELETE_FROM_STOCK -> {
                boolean success = warehouse.deleteFromStock(parts[0], Integer.parseInt(parts[1]));
                yield success ? "OK" : "ERROR: INSUFFICIENT STOCK";
            }
            case ADD_TO_STOCK -> {
                int newQuantity = warehouse.addToStock(parts[0], Integer.parseInt(parts[1]));
                yield "OK:" + newQuantity;
            }
            case ADD_GROUP -> {
                boolean success = warehouse.addEmptyGroup(parts[0]);
                yield success ? "OK" : "ERROR: GROUP ALREADY EXISTS";
            }
            case ADD_PRODUCT_TO_GROUP -> {
                boolean success = warehouse.addProductToGroup(parts[0], parts[1]);
                yield success ? "OK" : "ERROR: PRODUCT ALREADY IN GROUP";
            }
            case SET_PRICE -> {
                boolean success = warehouse.setProductPrice(parts[0], Double.parseDouble(parts[1]));
                yield success ? "OK" : "ERROR: PRODUCT DOES NOT EXIST";
            }
        };
    }
}