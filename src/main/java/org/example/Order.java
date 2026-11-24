public class Order {

    public enum Status { CREATED, PAID }

    private final String id;
    private final String productId;
    private final int quantity;
    private final double unitPrice;
    private Status status = Status.CREATED;

    public Order(String id, String productId, int quantity, double unitPrice) {
        this.id = id;
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public String getId() {
        return id;
    }

    public String getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    // Totali llogaritet gjithmonë nga sasia dhe çmimi, nuk ruhet veçmas
    public double getTotal() {
        return quantity * unitPrice;
    }

    public Status getStatus() {
        return status;
    }

    public boolean isPaid() {
        return status == Status.PAID;
    }

    public void markAsPaid() {
        this.status = Status.PAID;
    }
}
