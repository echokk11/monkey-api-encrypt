package com.unknown.encrypt.core;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.*;

@ConfigurationProperties(prefix = "spring.encrypt")
public class EncryptionConfig {

	private boolean enabled = true;

	private EncryptionDefaultMode defaultMode = EncryptionDefaultMode.EXPLICIT;

	private String key = "d7b85f6e214abcdad7b85f6e214abcda";

	private List<String> responseEncryptUriList = new ArrayList<>();

	private List<String> requestDecryptUriList = new ArrayList<>();

	private List<String> responseEncryptUriIgnoreList = new ArrayList<>();

	private List<String> requestDecryptUriIgnoreList = new ArrayList<>();

	private String responseCharset = "UTF-8";

	private boolean debug = false;

	private String[] urlPatterns = new String[] { "/*" };

	private int order = 1;

	private Map<String, List<String>> requestDecryptParams = new LinkedHashMap<>();

	private boolean encryptErrorResponses = false;

	private boolean skipAttachmentResponses = true;

	private List<String> skipResponseContentTypes = new ArrayList<>(
			Arrays.asList("application/octet-stream", "text/event-stream")
	);

	public EncryptionConfig() {
	}

	public EncryptionConfig(String key, List<String> responseEncryptUriList, List<String> requestDecryptUriList,
			String responseCharset, boolean debug) {
		this.key = key;
		this.responseEncryptUriList = nullSafeList(responseEncryptUriList);
		this.requestDecryptUriList = nullSafeList(requestDecryptUriList);
		this.responseCharset = responseCharset;
		this.debug = debug;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public EncryptionDefaultMode getDefaultMode() {
		return defaultMode;
	}

	public void setDefaultMode(EncryptionDefaultMode defaultMode) {
		this.defaultMode = defaultMode == null ? EncryptionDefaultMode.EXPLICIT : defaultMode;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public List<String> getResponseEncryptUriList() {
		return responseEncryptUriList;
	}

	public void setResponseEncryptUriList(List<String> responseEncryptUriList) {
		this.responseEncryptUriList = nullSafeList(responseEncryptUriList);
	}

	public List<String> getRequestDecryptUriList() {
		return requestDecryptUriList;
	}

	public void setRequestDecryptUriList(List<String> requestDecryptUriList) {
		this.requestDecryptUriList = nullSafeList(requestDecryptUriList);
	}

	public String getResponseCharset() {
		return responseCharset;
	}

	public void setResponseCharset(String responseCharset) {
		this.responseCharset = responseCharset;
	}

	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public String[] getUrlPatterns() {
		return urlPatterns;
	}

	public void setUrlPatterns(String[] urlPatterns) {
		this.urlPatterns = urlPatterns == null ? new String[] { "/*" } : urlPatterns;
	}

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	public List<String> getResponseEncryptUriIgnoreList() {
		return responseEncryptUriIgnoreList;
	}

	public void setResponseEncryptUriIgnoreList(List<String> responseEncryptUriIgnoreList) {
		this.responseEncryptUriIgnoreList = nullSafeList(responseEncryptUriIgnoreList);
	}

	public List<String> getRequestDecryptUriIgnoreList() {
		return requestDecryptUriIgnoreList;
	}

	public void setRequestDecryptUriIgnoreList(List<String> requestDecryptUriIgnoreList) {
		this.requestDecryptUriIgnoreList = nullSafeList(requestDecryptUriIgnoreList);
	}

	public Map<String, List<String>> getRequestDecryptParams() {
		return requestDecryptParams;
	}

	public void setRequestDecryptParams(Map<String, List<String>> requestDecryptParams) {
		this.requestDecryptParams = nullSafeMap(requestDecryptParams);
	}

	public void setRequestDecryptParam(Map<String, List<String>> requestDecryptParams) {
		setRequestDecryptParams(requestDecryptParams);
	}

	public void setRequestDecyptParam(Map<String, List<String>> requestDecryptParams) {
		setRequestDecryptParams(requestDecryptParams);
	}

	public boolean isEncryptErrorResponses() {
		return encryptErrorResponses;
	}

	public void setEncryptErrorResponses(boolean encryptErrorResponses) {
		this.encryptErrorResponses = encryptErrorResponses;
	}

	public boolean isSkipAttachmentResponses() {
		return skipAttachmentResponses;
	}

	public void setSkipAttachmentResponses(boolean skipAttachmentResponses) {
		this.skipAttachmentResponses = skipAttachmentResponses;
	}

	public List<String> getSkipResponseContentTypes() {
		return skipResponseContentTypes;
	}

	public void setSkipResponseContentTypes(List<String> skipResponseContentTypes) {
		this.skipResponseContentTypes = nullSafeList(skipResponseContentTypes);
	}

	private List<String> nullSafeList(List<String> list) {
		return list == null ? new ArrayList<>() : list;
	}

	private Map<String, List<String>> nullSafeMap(Map<String, List<String>> map) {
		return map == null ? new LinkedHashMap<>() : map;
	}
}
