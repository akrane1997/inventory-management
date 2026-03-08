package com.korber.inventory_service.factory;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Factory that resolves the appropriate InventoryHandler by strategy name.
 * To add a new strategy (e.g., LIFO), simply implement InventoryHandler
 * and annotate it with @Component("LIFO") — no changes needed here.
 */
@Component
@RequiredArgsConstructor
public class InventoryHandlerFactory {

    private final Map<String, InventoryHandler> handlers;

    /**
     * Returns the handler for the given strategy key.
     * Defaults to FEFO if the requested strategy is not found.
     */
    public InventoryHandler getHandler(String strategy) {
        return handlers.getOrDefault(strategy, handlers.get("FEFO"));
    }
}
