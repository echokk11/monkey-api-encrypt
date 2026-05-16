package com.unknown.encrypt.core;

import java.util.*;

public class EncryptionRuleRegistry {

    private final List<String> responseEncryptUriList;

    private final List<String> requestDecryptUriList;

    private final List<String> responseEncryptUriIgnoreList;

    private final List<String> requestDecryptUriIgnoreList;

    private final Map<String, List<String>> requestDecryptParams;

    public EncryptionRuleRegistry(List<String> responseEncryptUriList,
                                  List<String> requestDecryptUriList,
                                  List<String> responseEncryptUriIgnoreList,
                                  List<String> requestDecryptUriIgnoreList,
                                  Map<String, List<String>> requestDecryptParams) {
        this.responseEncryptUriList = immutableList(responseEncryptUriList);
        this.requestDecryptUriList = immutableList(requestDecryptUriList);
        this.responseEncryptUriIgnoreList = immutableList(responseEncryptUriIgnoreList);
        this.requestDecryptUriIgnoreList = immutableList(requestDecryptUriIgnoreList);
        this.requestDecryptParams = immutableMap(requestDecryptParams);
    }

    public static EncryptionRuleRegistry fromConfig(EncryptionConfig config) {
        return new EncryptionRuleRegistry(
                config.getResponseEncryptUriList(),
                config.getRequestDecryptUriList(),
                config.getResponseEncryptUriIgnoreList(),
                config.getRequestDecryptUriIgnoreList(),
                config.getRequestDecryptParams()
        );
    }

    public List<String> getResponseEncryptUriList() {
        return responseEncryptUriList;
    }

    public List<String> getRequestDecryptUriList() {
        return requestDecryptUriList;
    }

    public List<String> getResponseEncryptUriIgnoreList() {
        return responseEncryptUriIgnoreList;
    }

    public List<String> getRequestDecryptUriIgnoreList() {
        return requestDecryptUriIgnoreList;
    }

    public Map<String, List<String>> getRequestDecryptParams() {
        return requestDecryptParams;
    }

    public List<String> getRequestDecryptParams(String uri) {
        List<String> params = requestDecryptParams.get(uri);
        return params == null ? Collections.emptyList() : params;
    }

    public boolean hasExplicitEncryptOrDecryptRules() {
        return !responseEncryptUriList.isEmpty() || !requestDecryptUriList.isEmpty();
    }

    private List<String> immutableList(List<String> list) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(list));
    }

    private Map<String, List<String>> immutableMap(Map<String, List<String>> map) {
        if (map == null || map.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, List<String>> copy = new LinkedHashMap<>();
        map.forEach((key, value) -> copy.put(key, immutableList(value)));
        return Collections.unmodifiableMap(copy);
    }
}
