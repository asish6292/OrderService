package com.example.OrderService.service;

import com.example.OrderService.exception.CustomException;
import com.example.OrderService.external.client.PaymentService;
import com.example.OrderService.external.client.ProductService;
import com.example.OrderService.entity.OrderEntity;
import com.example.OrderService.external.model.PaymentDetails;
import com.example.OrderService.external.request.PaymentRequest;
import com.example.OrderService.model.OrderRequest;
import com.example.OrderService.model.OrderResponse;
import com.example.OrderService.model.ProductDetails;
import com.example.OrderService.repository.OrderRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

@Service
@Log4j2
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public long placeOrder(OrderRequest orderRequest) {
        log.info("Calling product service for productId:{} and quantity: {} ", orderRequest.getProductId(), orderRequest.getQuantity());
        //product service call to reduce the quantity
        productService.reduceQuantity(orderRequest.getProductId(), orderRequest.getQuantity());

        log.info("Placing order request with status CREATED for: {}", orderRequest);
        OrderEntity orderEntity = OrderEntity.builder()
                .amount(orderRequest.getTotalAmount())
                .orderStatus("CREATED")
                .productId(orderRequest.getProductId())
                .orderDate(Instant.now())
                .quantity(orderRequest.getQuantity())
                .build();
        orderEntity = orderRepository.save(orderEntity);

        log.info("Calling payment service to complete the payment");
        PaymentRequest paymentRequest = PaymentRequest.builder()
                .orderId(orderEntity.getId())
                .paymentMode(orderRequest.getPaymentMode())
                .amount(orderRequest.getTotalAmount())
                .build();

        String orderStatus = null;
        try {
            paymentService.doPayment(paymentRequest);
            log.info("Payment done successfully, changing order status to PLACED");
            orderStatus = "PLACED";
        } catch (Exception e) {
            log.info("Payment failed, changing order status to PAYMENT_FAILED");
            orderStatus = "PAYMENT_FAILED";
        }
        orderEntity.setOrderStatus(orderStatus);
        orderRepository.save(orderEntity);
        log.info("Order placed with status: {} and with orderId: {}", orderStatus, orderEntity.getId());
        return orderEntity.getId();
    }

    @Override
    public OrderResponse getOrderDetails(long orderId) {
        log.info("Getting order details for id: {}", orderId);
        OrderEntity orderEntity = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException("Order not found for the order id" + orderId, "ORDER_ID_NOT_FOUND", 404));
        log.info("Order details fetched successfully for orderId: {}", orderId);

        log.info("Invoking product service for product id: {} ", orderEntity.getProductId());
        ProductDetails productDetails = restTemplate.getForObject("http://PRODUCT-SERVICE/product/" + orderEntity.getProductId(), ProductDetails.class);

        log.info("Invoking payment service for payment details for orderId:{}", orderId);
        PaymentDetails paymentDetails = restTemplate.getForObject("http://PAYMENT-SERVICE/payment/" + orderId, PaymentDetails.class);

        return OrderResponse.builder()
                .orderId(orderEntity.getId())
                .orderStatus(orderEntity.getOrderStatus())
                .orderDate(orderEntity.getOrderDate())
                .amount(orderEntity.getAmount())
                .productDetails(productDetails)
                .paymentDetails(paymentDetails)
                .build();
    }
}
