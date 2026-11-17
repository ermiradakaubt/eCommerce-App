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

    // Testi 2 – Produkti nuk ekziston
    @Test
    void shouldReturnProductNotFoundWhenProductDoesNotExist() {

        // STUB
        when(productRepository.findById("P99"))
                .thenReturn(null);

        // ACT
        String result =
                orderService.placeOrder("P99", 1);

        // ASSERT
        assertEquals("Product not found", result);

        // MOCK VERIFICATION: no product, so no payment is attempted
        verifyNoInteractions(paymentService);
    }

    // Testi 3 – Nuk ka stok të mjaftueshëm
    @Test
    void shouldRejectOrderWhenStockIsInsufficient() {

        // STUB
        when(productRepository.findById("P01"))
                .thenReturn(new Product("P01", 100, 5));

        // ACT
        String result =
                orderService.placeOrder("P01", 6);

        // ASSERT
        assertEquals("Invalid quantity or insufficient stock", result);

        // MOCK VERIFICATION: the order stops before payment
        verifyNoInteractions(paymentService);
    }

    // Testi 4 – Pagesa dështon
    @Test
    void shouldReturnPaymentFailedWhenPaymentIsDeclined() {

        // STUB
        when(productRepository.findById("P01"))
                .thenReturn(new Product("P01", 100, 10));

        when(paymentService.pay(200.0))
                .thenReturn(false);

        // ACT
        String result =
                orderService.placeOrder("P01", 2);

        // ASSERT
        assertEquals("Payment failed", result);

        // MOCK VERIFICATION: payment was attempted exactly once with the right total
        verify(paymentService).pay(200.0);
    }
}
