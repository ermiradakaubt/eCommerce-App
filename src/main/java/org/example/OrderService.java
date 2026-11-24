public class OrderService {

    private ProductRepository productRepository;
    private PaymentService paymentService;

    private OrderRepository orderRepository;
    private PaymentGateway paymentGateway;
    private InventoryService inventoryService;
    private EmailService emailService;

    public OrderService(
            ProductRepository productRepository,
            PaymentService paymentService) {

        this.productRepository = productRepository;
        this.paymentService = paymentService;
    }

    public OrderService(
            OrderRepository orderRepository,
            PaymentGateway paymentGateway,
            InventoryService inventoryService,
            EmailService emailService) {

        this.orderRepository = orderRepository;
        this.paymentGateway = paymentGateway;
        this.inventoryService = inventoryService;
        this.emailService = emailService;
    }

    public String placeOrder(String productId, int quantity) {

        Product product =
                productRepository.findById(productId);

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

    public Order placeOrder(Order order) {

        if (order.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        if (!inventoryService.isAvailable(order.getProductId())) {
            return null;
        }

        if (!paymentGateway.processPayment(order.getTotal())) {
            throw new IllegalStateException("Payment failed");
        }

        order.setPaid(true);
        order.setStatus("PAID");

        Order saved = orderRepository.save(order);
        emailService.sendConfirmation(saved.getId());

        return saved;
    }
}
