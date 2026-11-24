public class OrderService {

    private final OrderRepository orderRepository;
    private final PaymentGateway paymentGateway;
    private final InventoryService inventoryService;
    private final EmailService emailService;

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

    /**
     * Përpunon porosinë: validon sasinë, kontrollon stokun, kryen pagesën,
     * ruan porosinë dhe dërgon email konfirmimi.
     *
     * @return porosia e ruajtur, ose null nëse produkti nuk është në stok
     * @throws IllegalArgumentException nëse sasia nuk është pozitive
     * @throws IllegalStateException nëse pagesa dështon
     */
    public Order placeOrder(Order order) {

        if (order.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        if (!inventoryService.isAvailable(order.getProductId())) {
            return null;
        }

        if (!paymentGateway.processPayment(order.getTotal())) {
            throw new IllegalStateException("Payment failed");
        }

        order.markAsPaid();
        Order saved = orderRepository.save(order);
        emailService.sendConfirmation(saved.getId());

        return saved;
    }

    /** Ruan porosinë pa e paguar (statusi mbetet CREATED). */
    public Order createOrder(Order order) {
        return orderRepository.save(order);
    }
}
