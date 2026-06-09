package practice2.warehouse;

public enum CommandType {
    GET_QUANTITY(1),
    DELETE_FROM_STOCK(2),
    ADD_TO_STOCK(3),
    ADD_GROUP(4),
    ADD_PRODUCT_TO_GROUP(5),
    SET_PRICE(6),
    CREATE(7),
    GET(8),
    UPDATE(9),
    DELETE(10),
    SEARCH(11);

    private final int code;

    CommandType(int code) { this.code = code; }

    public int getCode() { return code; }

    public static CommandType fromCode(int code) {
        for (CommandType type : values()) {
            if (type.code == code) return type;
        }
        throw new IllegalArgumentException("Невідомий тип команди: " + code);
    }
}