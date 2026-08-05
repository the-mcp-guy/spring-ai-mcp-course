package com.themcpguy.supportdesk.orders.mcp;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Class 14: that the annotation scanner found everything and described it correctly.
 *
 * <p>The unit tests say the methods work. They say nothing about whether MCP ever sees
 * them, which is what an annotation on a class that is not a bean looks like.
 */
@SpringBootTest
class McpRegistrationTest {

    @Autowired
    ObjectProvider<List<SyncToolSpecification>> toolSpecs;

    private List<SyncToolSpecification> tools() {
        return toolSpecs.getObject();
    }

    private SyncToolSpecification tool(String name) {
        return tools().stream()
                .filter(spec -> spec.tool().name().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No tool registered called " + name));
    }

    @Test
    void everyToolIsRegistered() {
        Set<String> names = tools().stream()
                .map(spec -> spec.tool().name())
                .collect(Collectors.toSet());

        assertThat(names).containsExactlyInAnyOrder(
                "get_order", "get_customer_orders", "get_orders_by_status",
                "update_order_status", "recheck_shipments", "cancel_order");
    }

    @Test
    void cancelOrderIsMarkedDestructive() {
        var annotations = tool("cancel_order").tool().annotations();

        assertThat(annotations.destructiveHint()).isTrue();
        assertThat(annotations.readOnlyHint()).isFalse();
        assertThat(annotations.idempotentHint()).isFalse();
    }

    @Test
    void theLookupsAreMarkedReadOnly() {
        for (String name : List.of("get_order", "get_customer_orders", "get_orders_by_status")) {
            var annotations = tool(name).tool().annotations();
            assertThat(annotations.readOnlyHint()).as(name).isTrue();
            assertThat(annotations.destructiveHint()).as(name).isFalse();
        }
    }

    @Test
    void recheckReachesOutsideOurOwnData() {
        assertThat(tool("recheck_shipments").tool().annotations().openWorldHint()).isTrue();
    }

    @Test
    void getOrderDeclaresItsParameter() {
        assertThat(properties("get_order")).containsKey("orderId");
        assertThat(required("get_order")).contains("orderId");
    }

    @Test
    void getOrderCarriesAnOutputSchema() {
        assertThat(tool("get_order").tool().outputSchema()).isNotNull();
    }

    @Test
    void theRequestContextDoesNotLeakIntoTheSchema() {
        // recheck_shipments takes only an McpSyncRequestContext, so its schema is empty.
        assertThat(properties("recheck_shipments")).isEmpty();

        // cancel_order takes a context and an orderId; only the orderId is declared.
        assertThat(properties("cancel_order")).containsOnlyKeys("orderId");
    }

    /** Tool.inputSchema() is a plain Map in SDK 2.0.0, not a typed record. */
    @SuppressWarnings("unchecked")
    private Map<String, Object> properties(String toolName) {
        Object properties = tool(toolName).tool().inputSchema().get("properties");
        return properties == null ? Map.of() : (Map<String, Object>) properties;
    }

    @SuppressWarnings("unchecked")
    private List<String> required(String toolName) {
        Object required = tool(toolName).tool().inputSchema().get("required");
        return required == null ? List.of() : (List<String>) required;
    }
}
