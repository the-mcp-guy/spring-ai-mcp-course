package com.themcpguy.supportdesk.orders.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.themcpguy.supportdesk.orders.domain.Order;
import com.themcpguy.supportdesk.orders.service.OrderService;

/**
 * The REST API the application had before any of this was about AI.
 *
 * <p>It is untouched by every class in the course. From Class 2 the same data is also
 * reachable over MCP, and the two sit side by side.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/{orderId}")
    public Order getOrder(@PathVariable String orderId) {
        return orderService.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No order with ID '%s'. Check the ID and try again.".formatted(orderId)));
    }

    @GetMapping
    public List<Order> search(@RequestParam(required = false) String customerId,
                              @RequestParam(required = false) String status) {
        if (customerId != null) {
            return orderService.findByCustomerId(customerId);
        }
        if (status != null) {
            return orderService.findByStatus(status);
        }
        return orderService.findByStatus("PENDING");
    }

    public record ApiError(String message) {}

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError notFound(IllegalArgumentException e) {
        return new ApiError(e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError conflict(IllegalStateException e) {
        return new ApiError(e.getMessage());
    }
}
