
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/*
 * SMELL FIXED - Mystery / mismatched name:
 *   the class was named "OrderServiceTest_2" inside a file called
 *   OrderServiceTest.java. Renamed to match the file so the test is easy to find.
 *
 * Every test below follows the Arrange - Act - Assert (AAA) pattern:
 *   one behaviour per test, a descriptive name, and only the assertions
 *   that belong to that behaviour.
 */
class OrderServiceTest {

    private OrderService orderService;

    private OrderRepository orderRepository;
    private PaymentGateway paymentGateway;
    private InventoryService inventoryService;
    private EmailService emailService;

    private Order order;

    // Shared Arrange: fresh mocks and a fresh order before EVERY test,
    // so no test depends on another test's state.
    @BeforeEach
    void setup() {
        orderRepository = mock(OrderRepository.class);
        paymentGateway = mock(PaymentGateway.class);
        inventoryService = mock(InventoryService.class);
        emailService = mock(EmailService.class);

        orderService = new OrderService(
                orderRepository,
                paymentGateway,
                inventoryService,
                emailService
        );

        order = new Order("ORD-100", "P01", 2, 50.0);
    }

    /*
     * SMELL FIXED - The Free Ride / Eager Test:
     *   the old "testEverything()" verified the happy path AND three other
     *   scenarios in a single method. Each scenario is now its own test.
     * SMELL FIXED - Assertion Roulette:
     *   assertions now carry messages, so a failure says exactly what broke.
     */
    @Test
    void placeOrder_whenProductAvailableAndPaymentSucceeds_confirmsOrder() {
        // Arrange
        when(inventoryService.isAvailable("P01")).thenReturn(true);
        when(paymentGateway.processPayment(100.0)).thenReturn(true);
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // Act
        Order result = orderService.placeOrder(order);

        // Assert
        assertNotNull(result, "A confirmed order should be returned");
        assertEquals("ORD-100", result.getId(), "Order id must be preserved");
        assertEquals(100.0, result.getTotal(), "Total = price 50.0 x quantity 2");
        assertTrue(result.isPaid(), "Order should be marked as paid");
        verify(orderRepository).save(any(Order.class));
        verify(emailService).sendConfirmation("ORD-100");
    }

    /*
     * SMELL FIXED - Exception Swallowing / Conditional Test Logic:
     *   the old code wrapped the call in try/catch and ignored the exception,
     *   so the test passed whether or not it was thrown. We now ASSERT the
     *   exception with assertThrows.
     * SMELL FIXED - Irrelevant Assertion:
     *   the old "assertTrue(true)" / "assertNotNull(order)" checked nothing
     *   about the behaviour under test.
     */
    @Test
    void placeOrder_whenQuantityIsNegative_isRejected() {
        // Arrange
        order.setQuantity(-1);

        // Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> orderService.placeOrder(order),
                "A negative quantity must be rejected");
    }

    // Boundary of the same rule, kept as its own focused test (value 0).
    @Test
    void placeOrder_whenQuantityIsZero_isRejected() {
        // Arrange
        order.setQuantity(0);

        // Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> orderService.placeOrder(order),
                "A zero quantity must be rejected");
    }

    /*
     * SMELL FIXED - Overspecified / implementation-detail verification:
     *   the old mega-test did "verify(paymentGateway, times(1))" with a
     *   comment "Check internal implementation". Here we verify only the
     *   meaningful behaviour: when the product is out of stock, payment is
     *   NEVER attempted.
     */
    @Test
    void placeOrder_whenProductOutOfStock_returnsNullAndSkipsPayment() {
        // Arrange
        when(inventoryService.isAvailable("P01")).thenReturn(false);

        // Act
        Order result = orderService.placeOrder(order);

        // Assert
        assertNull(result, "No order should be created when out of stock");
        verify(paymentGateway, never()).processPayment(anyDouble());
    }

    /*
     * SMELL FIXED - Lucky Test / tautological assertion:
     *   the old "testPaymentFailure()" swallowed the call and ended with
     *   "assertTrue(true)", verifying nothing. We now assert the real outcome:
     *   a declined payment must not confirm the order.
     */
    @Test
    void placeOrder_whenPaymentDeclined_doesNotConfirmOrder() {
        // Arrange
        when(inventoryService.isAvailable("P01")).thenReturn(true);
        when(paymentGateway.processPayment(100.0)).thenReturn(false);

        // Act
        Order result = orderService.placeOrder(order);

        // Assert
        assertNull(result, "A declined payment should not produce an order");
        verify(emailService, never()).sendConfirmation(anyString());
    }

    /*
     * SMELL FIXED - filler assertion:
     *   the old "testOrderCreation()" mixed a real check with "assertTrue(true)".
     *   Removed the filler; kept only the meaningful assertions.
     */
    @Test
    void createOrder_returnsOrderWithGivenId() {
        // Act
        Order result = orderService.createOrder(order);

        // Assert
        assertNotNull(result, "createOrder should return an order");
        assertEquals("ORD-100", result.getId(), "Order id must match the input");
    }

    /*
     * REMOVED - two tests were deleted because they are pure test smells,
     * not behaviour worth testing:
     *
     *   testOrderStatus()        -> SMELL: Testing getters/setters of the Order
     *                               data class, not the OrderService. It tests
     *                               the language, not our logic.
     *
     *   testOrderWithCurrentUser() -> SMELL: The Local Hero / environment
     *                               dependency. It asserted
     *                               System.getProperty("user.name") equals
     *                               "student", so it only passes on one machine.
     */
}
