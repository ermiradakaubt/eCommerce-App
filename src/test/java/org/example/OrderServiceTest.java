import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OrderServiceTest {

    private ProductRepository productRepository;

    private PaymentService paymentService;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        paymentService = mock(PaymentService.class);
        orderService = new OrderService(productRepository, paymentService);
    }

    @Test
    void shouldConfirmOrderSuccessfully() {
        // Arrange
        when(productRepository.findById("P01"))
                .thenReturn(new Product("P01", 100, 10));
        when(paymentService.pay(200.0)).thenReturn(true);

        // Act
        String result = orderService.placeOrder("P01", 2);

        // Assert
        assertEquals("Order confirmed", result);
        verify(paymentService).pay(200.0);
    }

    @Test
    void shouldRejectOrderWhenProductDoesNotExist() {
        // Arrange
        when(productRepository.findById("P99")).thenReturn(null);

        // Act
        String result = orderService.placeOrder("P99", 1);

        // Assert
        assertEquals("Product not found", result);
        verifyNoInteractions(paymentService);
    }

    @Test
    void shouldRejectOrderWhenQuantityExceedsStock() {
        // Arrange
        when(productRepository.findById("P02"))
                .thenReturn(new Product("P02", 25, 3));

        // Act
        String result = orderService.placeOrder("P02", 4);

        // Assert
        assertEquals("Invalid quantity or insufficient stock", result);
        verifyNoInteractions(paymentService);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void shouldRejectOrderWhenQuantityIsNotPositive(int quantity) {
        // Arrange
        when(productRepository.findById("P02"))
                .thenReturn(new Product("P02", 25, 3));

        // Act
        String result = orderService.placeOrder("P02", quantity);

        // Assert
        assertEquals("Invalid quantity or insufficient stock", result);
        verifyNoInteractions(paymentService);
    }

    @Test
    void shouldReportFailureWhenPaymentIsDeclined() {
        // Arrange
        when(productRepository.findById("P03"))
                .thenReturn(new Product("P03", 12.50, 5));
        when(paymentService.pay(37.50)).thenReturn(false);

        // Act
        String result = orderService.placeOrder("P03", 3);

        // Assert
        assertEquals("Payment failed", result);
        verify(paymentService).pay(37.50);
    }
}
