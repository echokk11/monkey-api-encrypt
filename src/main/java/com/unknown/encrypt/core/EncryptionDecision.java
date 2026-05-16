package com.unknown.encrypt.core;

import java.util.Collections;
import java.util.List;

public class EncryptionDecision {

    private final boolean decryptRequestBody;

    private final boolean encryptResponse;

    private final List<String> decryptParams;

    public EncryptionDecision(boolean decryptRequestBody, boolean encryptResponse, List<String> decryptParams) {
        this.decryptRequestBody = decryptRequestBody;
        this.encryptResponse = encryptResponse;
        this.decryptParams = decryptParams == null ? Collections.emptyList() : decryptParams;
    }

    public boolean isDecryptRequestBody() {
        return decryptRequestBody;
    }

    public boolean isEncryptResponse() {
        return encryptResponse;
    }

    public List<String> getDecryptParams() {
        return decryptParams;
    }

    public boolean requiresRequestWrapper() {
        return decryptRequestBody || !decryptParams.isEmpty();
    }

    public boolean requiresProcessing() {
        return requiresRequestWrapper() || encryptResponse;
    }
}
