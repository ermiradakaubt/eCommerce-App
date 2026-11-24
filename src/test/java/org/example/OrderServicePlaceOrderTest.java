import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Versioni i pastruar i OrderServiceTest_2: një test për një sjellje,
 * secili me pjesët Arrange, Act, Assert.
 */
@ExtendWith(MockitoExtension.class)
class OrderServicePlaceOrderTest {

    private static final String ORDER_ID = "ORD-100";
    private static final String PRODUCT_ID = "P01";
    private static final double UNIT_PRICE = 50.0;

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

    // çdo test merr porosinë e vet të re, pa gjendje të përbashkët
    private Order anOrder(int quantity) {
        return new Order(ORDER_ID, PRODUCT_ID, quantity, UNIT_PRICE);
    }

    // ---------- rasti i suksesshëm ----------

    @Test
    void placeOrder_returnsPaidOrder_whenProductIsAvailableAndPaymentSucceeds() {
        // Arrange
        Order order = anOrder(2);
        when(inventoryService.isAvailable(PRODUCT_ID)).thenReturn(true);
        when(paymentGateway.processPayment(100.0)).thenReturn(true);
        when(orderRepository.save(order)).thenReturn(order);

        // Act
        Order result = orderService.placeOrder(order);

        // Assert
        assertEquals(ORDER_ID, result.getId());
        assertEquals(100.0, result.getTotal(), 0.001);
        assertTrue(result.isPaid());
        assertEquals("PAID", result.getStatus());
    }

    @Test
    void placeOrder_savesOrderAndSendsConfirmation_whenPaymentSucceeds() {
        // Arrange
        Order order = anOrder(2);
        when(inventoryService.isAvailable(PRODUCT_ID)).thenReturn(true);
        when(paymentGateway.processPayment(100.0)).thenReturn(true);
        when(orderRepository.save(order)).thenReturn(order);

        // Act
        orderService.placeOrder(order);

        // Assert
        verify(orderRepository).save(order);
        verify(emailService).sendConfirmation(ORDER_ID);
    }

    // ---------- shuma e paguar ----------

    @ParameterizedTest
    @CsvSource({
            "1, 50.0, 50.0",
            "2, 50.0, 100.0",
            "5, 2.5, 12.5"
    })
    void placeOrder_chargesQuantityTimesUnitPrice(int quantity, double unitPrice, double expectedTotal) {
        // Arrange
        Order order = new Order(ORDER_ID, PRODUCT_ID, quantity, unitPrice);
        when(inventoryService.isAvailable(PRODUCT_ID)).thenReturn(true);
        when(paymentGateway.processPayment(expectedTotal)).thenReturn(true);
        when(orderRepository.save(order)).thenReturn(order);

        // Act
        Order result = orderService.placeOrder(order);

        // Assert
        assertEquals(expectedTotal, result.getTotal(), 0.001);
        verify(paymentGateway).processPayment(expectedTotal);
    }

    // ---------- sasia e pavlefshme ----------

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -100})
    void placeOrder_throwsIllegalArgument_whenQuantityIsNotPositive(int quantity) {
        // Arrange
        Order order = anOrder(quantity);

        // Act
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> orderService.placeOrder(order));

        // Assert
        assertEquals("Quantity must be greater than zero", thrown.getMessage());
        verifyNoInteractions(inventoryService, paymentGateway, orderRepository, emailService);
    }

    // ---------- produkti i padisponueshëm ----------

    @Test
    void placeOrder_returnsNullAndDoesNotCharge_whenProductIsNotAvailable() {
        // Arrange
        Order order = anOrder(2);
        when(inventoryService.isAvailable(PRODUCT_ID)).thenReturn(false);

        // Act
        Order result = orderService.placeOrder(order);

        // Assert
        assertNull(result);
        verifyNoInteractions(paymentGateway, orderRepository, emailService);
    }

    // ---------- pagesa dështon ----------

    @Test
    void placeOrder_throwsIllegalState_whenPaymentFails() {
        // Arrange
        Order order = anOrder(2);
        when(inventoryService.isAvailable(PRODUCT_ID)).thenReturn(true);
        when(paymentGateway.processPayment(100.0)).thenReturn(false);

        // Act
        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> orderService.placeOrder(order));

        // Assert
        assertEquals("Payment failed", thrown.getMessage());
        verify(orderRepository, never()).save(any(Order.class));
        verify(emailService, never()).sendConfirmation(any());
    }
}
