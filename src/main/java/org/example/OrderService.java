public class OrderService {

    private final ProductRepository productRepository;
    private final PaymentService paymentService;

    public OrderService(
            ProductRepository productRepository,
            PaymentService paymentService) {

        this.productRepository = productRepository;
        this.paymentService = paymentService;
    }

    public String placeOrder(String productId, int quantity) {

        Product product = productRepository.findById(productId);

        if (product == null) {
            return "Product not found";
        }

        if (quantity <= 0 || quantity > product.getStock()) {
            return "Invalid quantity or insufficient stock";
        }

        double total = product.getPrice() * quantity;

        boolean paid = paymentService.pay(total);

        if (!paid) {
            return "Payment failed";
        }

        return "Order confirmed";
    }
}