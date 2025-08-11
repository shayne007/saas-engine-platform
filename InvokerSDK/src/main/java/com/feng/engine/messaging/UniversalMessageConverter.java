package com.feng.engine.messaging;

import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * TODO
 *
 * @since 2025/8/11
 */
@Component
public class UniversalMessageConverter {

    public byte[] serialize(UniversalMessage message) {
        // Use a standard format (e.g., Avro, Protobuf, or JSON)
        return JsonMessageFormat.serialize(message);
    }

    public UniversalMessage deserialize(byte[] data, Map<String, String> headers) {
        UniversalMessage baseMessage = JsonMessageFormat.deserialize(data);

        // Merge provider-specific headers
        return baseMessage.toBuilder()
            .headers(mergeHeaders(baseMessage.getHeaders(), headers))
            .build();
    }

    private Map<String, String> mergeHeaders(Map<String, String> messageHeaders,
        Map<String, String> providerHeaders) {
        Map<String, String> merged = new HashMap<>(messageHeaders);

        // Map provider-specific headers to universal format
        providerHeaders.forEach((key, value) -> {
            String universalKey = mapHeaderKey(key);
            if (universalKey != null) {
                merged.put(universalKey, value);
            }
        });

        return merged;
    }

    private String mapHeaderKey(String providerKey) {
        // Map provider-specific header keys to universal format
        switch (providerKey) {
            case "__timestamp": return "TIMESTAMP";
            case "__partition": return "PARTITION";
            case "__offset": return "OFFSET";
            default: return providerKey.startsWith("__") ? null : providerKey;
        }
    }
}

