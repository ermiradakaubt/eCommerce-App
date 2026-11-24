import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testet për OrderService, të organizuara sipas Arrange-Act-Assert.
 *
 * Test smells të larguara nga versioni origjinal:
 *  - Eager Test: "testEverything" testonte 4 skenarë njëherësh → u nda në teste të fokusuara.
 *  - Meaningless Assertions: assertTrue(true), assertNotNull(order) → u zëvendësuan me pohime reale.
 *  - Exception Swallowing: try/catch bosh + System.out.println → assertThrows.
 *  - Mutable Shared Fixture: "order" ndryshohej mes skenarëve → porosi e re për çdo test.
 *  - Testing Implementation: verify(times(2)) mbi thirrje të brendshme → u hoq.
 *  - Mystery Guest: varësi nga System.getProperty("user.name") → testi u hoq.
 *  - Testing Getters/Setters: testOrderStatus nuk testonte OrderService → u hoq.
 *  - Missing Stub: createOrder pa stub për save() → u shtua stub-i.
 *  - Poor Naming & Magic Numbers: emra si "testEverything", vlera si 100.0 → emra përshkrues + konstante.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService")
class OrderServiceTest {

    private static final String ORDER_ID = "ORD-100";
    private static final String PRODUCT_ID = "P01";
    private static final int QUANTITY = 2;
    private static final double UNIT_PRICE = 50.0;
    private static final double EXPECTED_TOTAL = QUANTITY * UNIT_PRICE;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentGateway paymentGateway;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("Porosia e vlefshme kthehet e paguar me totalin e saktë")
    void placeOrder_validOrder_returnsPaidOrderWithCorrectTotal() {
        // Arrange
        Order order = orderWithQuantity(QUANTITY);
        givenProductInStockAndPaymentAccepted(order);

        // Act
        Order result = orderService.placeOrder(order);

        // Assert
        assertAll(
                () -> assertEquals(ORDER_ID, result.getId()),
                () -> assertEquals(EXPECTED_TOTAL, result.getTotal()),
                () -> assertTrue(result.isPaid())
        );
    }

    @Test
    @DisplayName("Porosia e vlefshme dërgon email konfirmimi")
    void placeOrder_validOrder_sendsConfirmationEmail() {
        // Arrange
        Order order = orderWithQuantity(QUANTITY);
        givenProductInStockAndPaymentAccepted(order);

        // Act
        orderService.placeOrder(order);

        // Assert
        verify(emailService).sendConfirmation(ORDER_ID);
    }

    @ParameterizedTest(name = "sasia = {0}")
    @ValueSource(ints = {0, -1})
    @DisplayName("Sasia jo-pozitive refuzohet dhe klienti nuk ngarkohet")
    void placeOrder_nonPositiveQuantity_throwsAndDoesNotCharge(int invalidQuantity) {
        // Arrange
        Order order = orderWithQuantity(invalidQuantity);

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> orderService.placeOrder(order));

        // Assert
        assertEquals("Quantity must be positive", exception.getMessage());
        // Kontrollojmë vetëm efektet e dukshme (pagesë, ruajtje, email), jo thirrjet e brendshme
        verifyNoInteractions(paymentGateway, orderRepository, emailService);
    }

    @Test
    @DisplayName("Produkti pa stok kthen null dhe klienti nuk ngarkohet")
    void placeOrder_productUnavailable_returnsNullAndDoesNotCharge() {
        // Arrange
        Order order = orderWithQuantity(QUANTITY);
        when(inventoryService.isAvailable(PRODUCT_ID)).thenReturn(false);

        // Act
        Order result = orderService.placeOrder(order);

        // Assert
        assertNull(result);
        verifyNoInteractions(paymentGateway, orderRepository, emailService);
    }

    @Test
    @DisplayName("Pagesa e refuzuar hedh përjashtim; porosia nuk ruhet dhe email nuk dërgohet")
    void placeOrder_paymentDeclined_throwsAndDoesNotSaveOrNotify() {
        // Arrange
        Order order = orderWithQuantity(QUANTITY);
        when(inventoryService.isAvailable(PRODUCT_ID)).thenReturn(true);
        when(paymentGateway.processPayment(EXPECTED_TOTAL)).thenReturn(false);

        // Act
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> orderService.placeOrder(order));

        // Assert
        assertEquals("Payment failed", exception.getMessage());
        assertFalse(order.isPaid());
        verifyNoInteractions(orderRepository, emailService);
    }

    @Test
    @DisplayName("createOrder ruan porosinë pa e paguar")
    void createOrder_savesOrderWithoutPayment() {
        // Arrange
        Order order = orderWithQuantity(QUANTITY);
        when(orderRepository.save(order)).thenReturn(order);

        // Act
        Order result = orderService.createOrder(order);

        // Assert
        assertEquals(ORDER_ID, result.getId());
        assertEquals(Order.Status.CREATED, result.getStatus());
        verifyNoInteractions(paymentGateway);
    }

    // --- Helpers ---

    // Porosi e re për çdo test → asnjë gjendje e ndarë mes skenarëve
    private static Order orderWithQuantity(int quantity) {
        return new Order(ORDER_ID, PRODUCT_ID, quantity, UNIT_PRICE);
    }

    private void givenProductInStockAndPaymentAccepted(Order order) {
        when(inventoryService.isAvailable(PRODUCT_ID)).thenReturn(true);
        when(paymentGateway.processPayment(EXPECTED_TOTAL)).thenReturn(true);
        when(orderRepository.save(order)).thenReturn(order);
    }
}
