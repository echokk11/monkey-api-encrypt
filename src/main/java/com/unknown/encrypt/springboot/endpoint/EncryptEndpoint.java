package com.unknown.encrypt.springboot.endpoint;

import com.unknown.encrypt.core.EncryptionRuleRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
@Endpoint(id = "encrypt-config")
public class EncryptEndpoint {

    @Autowired
    private EncryptionRuleRegistry encryptionRuleRegistry;

    @ReadOperation
    public Map<String, Object> encryptConfig() {
        Map<String, Object> data = new HashMap<>();
        data.put("responseEncryptUriList", encryptionRuleRegistry.getResponseEncryptUriList());
        data.put("responseEncryptUriIgnoreList", encryptionRuleRegistry.getResponseEncryptUriIgnoreList());
        data.put("requestDecryptUriList", encryptionRuleRegistry.getRequestDecryptUriList());
        data.put("requestDecryptUriIgnoreList", encryptionRuleRegistry.getRequestDecryptUriIgnoreList());
        data.put("requestDecryptParams", encryptionRuleRegistry.getRequestDecryptParams());
        return data;
    }

}
