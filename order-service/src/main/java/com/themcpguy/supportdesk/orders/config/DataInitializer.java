package com.themcpguy.supportdesk.orders.config;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.themcpguy.supportdesk.orders.domain.OrderStatus;
import com.themcpguy.supportdesk.orders.entity.CustomerEntity;
import com.themcpguy.supportdesk.orders.entity.OrderEntity;
import com.themcpguy.supportdesk.orders.entity.OrderItemEmbeddable;
import com.themcpguy.supportdesk.orders.entity.ShipmentEmbeddable;
import com.themcpguy.supportdesk.orders.repository.CustomerRepository;
import com.themcpguy.supportdesk.orders.repository.OrderRepository;

/**
 * Seeds the in-memory database at startup.
 *
 * <p>Everything here is derived from the index rather than generated randomly, so every
 * run produces the same 200 orders. The first three are fixed because the course quotes
 * them: ORD-10001 is Ana Ruiz's shipped keyboard order, ORD-10002 is the pending order
 * Class 12 cancels, and ORD-10003 has been delivered.
 */
@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final int ORDER_COUNT = 200;
    private static final int FIXED_ORDERS = 4;
    private static final LocalDate FIRST_ORDER_DATE = LocalDate.of(2026, 1, 6);

    private static final String[][] CATALOGUE = {
            { "PROD-001", "Wireless Keyboard", "89.99" },
            { "PROD-002", "USB-C Hub", "45.00" },
            { "PROD-003", "Mechanical Switch Kit", "34.99" },
            { "PROD-004", "4K Monitor", "599.00" },
            { "PROD-005", "Laptop Stand", "54.50" },
            { "PROD-006", "Noise-Cancelling Headphones", "229.00" },
            { "PROD-007", "Webcam 1080p", "78.25" },
            { "PROD-008", "Desk Mat", "29.99" },
            { "PROD-009", "Portable SSD 1TB", "134.00" },
            { "PROD-010", "Ergonomic Mouse", "62.40" },
    };

    private static final String[] CARRIERS = { "DHL", "UPS", "Royal Mail", "DPD" };

    private static final String[] FIRST_NAMES = {
            "Ana", "Marcus", "Priya", "Tomas", "Leila", "Kenji", "Sofia", "Daniel",
            "Amara", "Viktor", "Chloe", "Rahul", "Elena", "Jonas", "Nadia", "Ibrahim",
            "Freya", "Mateo", "Sinead", "Kwame", "Ingrid"
    };

    private static final String[] LAST_NAMES = {
            "Ruiz", "Adeyemi", "Sharma", "Novak", "Haddad", "Watanabe", "Rossi", "Berg",
            "Okafor", "Petrov", "Dubois", "Nair", "Costa", "Lindqvist", "Karim", "Aziz",
            "Andersen", "Silva", "O'Brien", "Mensah", "Larsen"
    };

    @Bean
    CommandLineRunner seedData(CustomerRepository customers, OrderRepository orders) {
        return args -> {
            if (orders.count() > 0) {
                return;
            }

            List<CustomerEntity> saved = customers.saveAll(buildCustomers());
            orders.saveAll(buildOrders(saved));

            log.info("Seeded {} customers and {} orders", saved.size(), orders.count());
        };
    }

    private List<CustomerEntity> buildCustomers() {
        List<CustomerEntity> customers = new ArrayList<>();
        for (int i = 1; i <= 42; i++) {
            String id = "CUST-%02d".formatted(i);
            String first = FIRST_NAMES[(i - 1) % FIRST_NAMES.length];
            String last = LAST_NAMES[(i * 5) % LAST_NAMES.length];
            String tier = i % 10 == 0 ? "GOLD" : (i % 3 == 0 ? "SILVER" : "STANDARD");
            customers.add(new CustomerEntity(id, first + " " + last,
                    "%s.%s@example.com".formatted(first.toLowerCase(),
                            last.toLowerCase().replace("'", "")),
                    tier));
        }

        // The two the course quotes by name.
        customers.set(41, new CustomerEntity("CUST-42", "Ana Ruiz", "ana.ruiz@example.com", "GOLD"));
        customers.set(16, new CustomerEntity("CUST-17", "Marcus Adeyemi", "marcus.adeyemi@example.com", "STANDARD"));
        return customers;
    }

    private List<OrderEntity> buildOrders(List<CustomerEntity> customers) {
        List<OrderStatus> statuses = statusPlan();
        List<OrderEntity> orders = new ArrayList<>();

        for (int i = 0; i < ORDER_COUNT; i++) {
            String orderId = "ORD-%d".formatted(10001 + i);
            // The first four are replaced below; the plan covers the other 196.
            OrderStatus status = i < FIXED_ORDERS ? OrderStatus.PENDING : statuses.get(i - FIXED_ORDERS);
            CustomerEntity customer = customers.get((i * 13) % customers.size());
            LocalDate created = FIRST_ORDER_DATE.plusDays((i * 3L) % 150);

            List<OrderItemEmbeddable> items = buildItems(i);
            BigDecimal total = items.stream()
                    .map(item -> item.toDomain().lineTotal())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            orders.add(new OrderEntity(orderId, status, customer, items, total,
                    created.atStartOfDay().toInstant(ZoneOffset.UTC),
                    created.plusDays(2).atStartOfDay().toInstant(ZoneOffset.UTC),
                    buildShipment(i, status, created)));
        }

        orders.set(0, fixedFirstOrder(customers));
        orders.set(1, fixedSecondOrder(customers));
        orders.set(2, fixedThirdOrder(customers));
        orders.set(3, fixedFourthOrder(customers));
        return orders;
    }

    /**
     * Statuses for the 196 generated orders, mixed by a fixed stride so they are not in
     * blocks. The four fixed orders add one SHIPPED, one PENDING and two DELIVERED, so
     * the totals across all 200 come to 87 shipped, 50 delivered, 30 pending,
     * 25 processing and 8 cancelled.
     */
    private List<OrderStatus> statusPlan() {
        int generated = ORDER_COUNT - FIXED_ORDERS;

        List<OrderStatus> plan = new ArrayList<>();
        add(plan, OrderStatus.SHIPPED, 86);
        add(plan, OrderStatus.DELIVERED, 48);
        add(plan, OrderStatus.PENDING, 29);
        add(plan, OrderStatus.PROCESSING, 25);
        add(plan, OrderStatus.CANCELLED, 8);

        List<OrderStatus> mixed = new ArrayList<>(java.util.Collections.nCopies(generated, OrderStatus.PENDING));
        for (int i = 0; i < generated; i++) {
            mixed.set((i * 61) % generated, plan.get(i));
        }
        return mixed;
    }

    private void add(List<OrderStatus> plan, OrderStatus status, int count) {
        for (int i = 0; i < count; i++) {
            plan.add(status);
        }
    }

    private List<OrderItemEmbeddable> buildItems(int index) {
        int lines = 1 + (index % 3);
        List<OrderItemEmbeddable> items = new ArrayList<>();
        for (int line = 0; line < lines; line++) {
            String[] product = CATALOGUE[(index * 3 + line) % CATALOGUE.length];
            items.add(new OrderItemEmbeddable(product[0], product[1],
                    1 + ((index + line) % 2), new BigDecimal(product[2])));
        }
        return items;
    }

    private ShipmentEmbeddable buildShipment(int index, OrderStatus status, LocalDate created) {
        if (status != OrderStatus.SHIPPED && status != OrderStatus.DELIVERED) {
            return new ShipmentEmbeddable(null, null, null, null);
        }
        LocalDate shipped = created.plusDays(2);
        return new ShipmentEmbeddable(
                CARRIERS[index % CARRIERS.length],
                "%s-%05d".formatted(CARRIERS[index % CARRIERS.length].substring(0, 3).toUpperCase(),
                        80000 + index),
                shipped,
                shipped.plusDays(3 + (index % 4)));
    }

    private OrderEntity fixedFirstOrder(List<CustomerEntity> customers) {
        List<OrderItemEmbeddable> items = List.of(
                new OrderItemEmbeddable("PROD-001", "Wireless Keyboard", 1, new BigDecimal("89.99")),
                new OrderItemEmbeddable("PROD-002", "USB-C Hub", 2, new BigDecimal("45.00")));

        return new OrderEntity("ORD-10001", OrderStatus.SHIPPED, byId(customers, "CUST-42"), items,
                new BigDecimal("179.99"),
                Instant.parse("2026-05-10T09:00:00Z"),
                Instant.parse("2026-05-12T14:30:00Z"),
                new ShipmentEmbeddable("DHL", "DHL-88213",
                        LocalDate.of(2026, 5, 12), LocalDate.of(2026, 5, 14)));
    }

    private OrderEntity fixedSecondOrder(List<CustomerEntity> customers) {
        List<OrderItemEmbeddable> items = List.of(
                new OrderItemEmbeddable("PROD-003", "Mechanical Switch Kit", 1, new BigDecimal("34.99")));

        return new OrderEntity("ORD-10002", OrderStatus.PENDING, byId(customers, "CUST-17"), items,
                new BigDecimal("34.99"),
                Instant.parse("2026-05-15T16:45:00Z"),
                Instant.parse("2026-05-15T16:45:00Z"),
                new ShipmentEmbeddable(null, null, null, null));
    }

    private OrderEntity fixedThirdOrder(List<CustomerEntity> customers) {
        List<OrderItemEmbeddable> items = List.of(
                new OrderItemEmbeddable("PROD-004", "4K Monitor", 1, new BigDecimal("599.00")));

        return new OrderEntity("ORD-10003", OrderStatus.DELIVERED, byId(customers, "CUST-42"), items,
                new BigDecimal("599.00"),
                Instant.parse("2026-05-01T11:00:00Z"),
                Instant.parse("2026-05-08T09:15:00Z"),
                new ShipmentEmbeddable("UPS", "UPS-80412",
                        LocalDate.of(2026, 5, 3), LocalDate.of(2026, 5, 8)));
    }

    /**
     * An order from before the 2024 policy change, for Class 10. The carrier is one the
     * current notes no longer cover, and the returns window that applied to it is the
     * 14-day one in the archived process, not today's 30 days.
     */
    private OrderEntity fixedFourthOrder(List<CustomerEntity> customers) {
        List<OrderItemEmbeddable> items = List.of(
                new OrderItemEmbeddable("PROD-001", "Wireless Keyboard", 1, new BigDecimal("89.99")));

        return new OrderEntity("ORD-10004", OrderStatus.DELIVERED, byId(customers, "CUST-17"), items,
                new BigDecimal("89.99"),
                Instant.parse("2023-11-14T10:20:00Z"),
                Instant.parse("2023-11-20T08:05:00Z"),
                new ShipmentEmbeddable("Parcelforce", "PF-40817326",
                        LocalDate.of(2023, 11, 15), LocalDate.of(2023, 11, 20)));
    }

    private CustomerEntity byId(List<CustomerEntity> customers, String id) {
        return customers.stream()
                .filter(customer -> customer.getCustomerId().equals(id))
                .findFirst()
                .orElseThrow();
    }
}
