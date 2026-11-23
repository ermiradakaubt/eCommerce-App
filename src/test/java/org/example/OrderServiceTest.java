import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private OrderService orderService;

    @Test
    void shouldConfirmOrderSuccessfully() {

        // STUB
        when(productRepository.findById("P01"))
                .thenReturn(new Product("P01", 100, 10));

        when(paymentService.pay(200.0))
                .thenReturn(true);

        // ACT
        String result =
                orderService.placeOrder("P01", 2);

        // ASSERT
        assertEquals("Order confirmed", result);

        // MOCK VERIFICATION
        verify(paymentService).pay(200.0);
    }

    @Test
    void shouldRejectOrderWhenProductDoesNotExist() {
        when(productRepository.findById("P99")).thenReturn(null);

        String result = orderService.placeOrder("P99", 1);

        assertEquals("Product not found", result);
        verifyNoInteractions(paymentService);
    }

    @Test
    void shouldRejectOrderWhenQuantityExceedsStock() {
        when(productRepository.findById("P02"))
                .thenReturn(new Product("P02", 25, 3));

        String result = orderService.placeOrder("P02", 4);

        assertEquals("Invalid quantity or insufficient stock", result);
        verifyNoInteractions(paymentService);
    }

    @Test
    void shouldReportFailureWhenPaymentIsDeclined() {
        when(productRepository.findById("P03"))
                .thenReturn(new Product("P03", 12.50, 5));
        when(paymentService.pay(37.50)).thenReturn(false);

        String result = orderService.placeOrder("P03", 3);

        assertEquals("Payment failed", result);
        verify(paymentService).pay(37.50);
    }
}
