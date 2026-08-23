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

@SpringBootTest
class McpRegistrationTest {

    @Autowired
    ObjectProvider<List<SyncToolSpecification>> toolSpecs;

    @Test
    void shouldRegisterEveryTool() {
        Set<String> names = toolSpecs.getObject().stream()
                .map(spec -> spec.tool().name())
                .collect(Collectors.toSet());

        assertThat(names).containsExactlyInAnyOrder(
                "get_order", "get_customer_orders", "get_orders_by_status",
                "update_order_status", "recheck_shipments", "cancel_order");
    }

    @Test
    void shouldMarkCancelOrderAsDestructive() {
        var annotations = tool("cancel_order").tool().annotations();

        assertThat(annotations.destructiveHint()).isTrue();
        assertThat(annotations.readOnlyHint()).isFalse();
    }

    @Test
    void shouldDeclareOnlyTheOrderIdParameter() {
        assertThat(properties("get_order")).containsOnlyKeys("orderId");
    }

    @Test
    void shouldKeepTheRequestContextOutOfTheSchema() {
        // recheck_shipments takes only an McpSyncRequestContext.
        assertThat(properties("recheck_shipments")).isEmpty();

        // cancel_order takes a context and an orderId; only the orderId is declared.
        assertThat(properties("cancel_order")).containsOnlyKeys("orderId");
    }

    private SyncToolSpecification tool(String name) {
        return toolSpecs.getObject().stream()
                .filter(spec -> spec.tool().name().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No tool registered called " + name));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> properties(String toolName) {
        Object properties = tool(toolName).tool().inputSchema().get("properties");
        return properties == null ? Map.of() : (Map<String, Object>) properties;
    }
}
