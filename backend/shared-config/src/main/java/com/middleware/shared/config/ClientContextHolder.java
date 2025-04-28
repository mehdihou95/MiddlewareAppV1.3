package com.middleware.shared.config;

import com.middleware.shared.model.Client;

/**
 * Holds the client context for the current thread.
 */
public class ClientContextHolder {
    private static final ThreadLocal<Client> CONTEXT = new ThreadLocal<>();

    public static void setClient(Client client) {
        CONTEXT.set(client);
    }

    public static Client getClient() {
        return CONTEXT.get();
    }

    public static Long getClientId() {
        Client client = CONTEXT.get();
        return client != null ? client.getId() : null;
    }

    public static void clear() {
        CONTEXT.remove();
    }

    private ClientContextHolder() {
        // Private constructor to prevent instantiation
    }
} 
