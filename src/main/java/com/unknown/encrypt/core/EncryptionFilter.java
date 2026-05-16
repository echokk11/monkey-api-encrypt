package com.unknown.encrypt.core;

import com.unknown.encrypt.algorithm.AesEncryptAlgorithm;
import com.unknown.encrypt.algorithm.EncryptAlgorithm;
import com.unknown.encrypt.util.RequestUriUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

public class EncryptionFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(EncryptionFilter.class);

    private final EncryptionConfig encryptionConfig;

    private final EncryptionRuleRegistry ruleRegistry;

    private final EncryptAlgorithm encryptAlgorithm;

    private final DispatcherServlet dispatcherServlet;

    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    public EncryptionFilter() {
        this(new EncryptionConfig());
    }

    public EncryptionFilter(EncryptionConfig config) {
        this(config, EncryptionRuleRegistry.fromConfig(config), new AesEncryptAlgorithm(), null);
    }

    public EncryptionFilter(EncryptionConfig config, DispatcherServlet dispatcherServlet) {
        this(config, EncryptionRuleRegistry.fromConfig(config), new AesEncryptAlgorithm(), dispatcherServlet);
    }

    public EncryptionFilter(EncryptionConfig config,
                            EncryptionRuleRegistry ruleRegistry,
                            DispatcherServlet dispatcherServlet) {
        this(config, ruleRegistry, new AesEncryptAlgorithm(), dispatcherServlet);
    }

    public EncryptionFilter(EncryptionConfig config,
                            EncryptAlgorithm encryptAlgorithm,
                            DispatcherServlet dispatcherServlet) {
        this(config, EncryptionRuleRegistry.fromConfig(config), encryptAlgorithm, dispatcherServlet);
    }

    public EncryptionFilter(EncryptionConfig config,
                            EncryptionRuleRegistry ruleRegistry,
                            EncryptAlgorithm encryptAlgorithm,
                            DispatcherServlet dispatcherServlet) {
        this.encryptionConfig = config == null ? new EncryptionConfig() : config;
        this.ruleRegistry = ruleRegistry == null ? EncryptionRuleRegistry.fromConfig(this.encryptionConfig) : ruleRegistry;
        this.encryptAlgorithm = encryptAlgorithm == null ? new AesEncryptAlgorithm() : encryptAlgorithm;
        this.dispatcherServlet = dispatcherServlet;
    }

    public EncryptionFilter(String key) {
        this(configWithKey(key));
    }

    public EncryptionFilter(String key, List<String> responseEncryptUriList, List<String> requestDecryptUriList,
                            String responseCharset, boolean debug) {
        this(new EncryptionConfig(key, responseEncryptUriList, requestDecryptUriList, responseCharset, debug));
    }

    private static EncryptionConfig configWithKey(String key) {
        EncryptionConfig config = new EncryptionConfig();
        config.setKey(key);
        return config;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String uri = request.getRequestURI();
        logger.debug("RequestURI: {}", uri);

        if (encryptionConfig.isDebug()) {
            chain.doFilter(request, response);
            return;
        }

        EncryptionDecision decision = decide(request);
        if (!decision.requiresProcessing()) {
            chain.doFilter(request, response);
            return;
        }

        EncryptionRequestWrapper requestWrapper = null;
        HttpServletRequest activeRequest = request;
        if (decision.requiresRequestWrapper()) {
            requestWrapper = new EncryptionRequestWrapper(request);
            processDecryption(requestWrapper, request, decision);
            activeRequest = requestWrapper;
        }

        EncryptionResponseWrapper responseWrapper = null;
        HttpServletResponse activeResponse = response;
        if (decision.isEncryptResponse()) {
            responseWrapper = new EncryptionResponseWrapper(response);
            activeResponse = responseWrapper;
        }

        chain.doFilter(activeRequest, activeResponse);

        if (decision.isEncryptResponse()) {
            writeResponse(responseWrapper, response);
        }
    }

    private EncryptionDecision decide(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String methodType = request.getMethod();
        String prefixUri = methodType.toLowerCase(Locale.ROOT) + ":" + uri;
        String apiUri = resolveApiUri(request);

        boolean decryptRequestBody = matches(ruleRegistry.getRequestDecryptUriList(), uri, prefixUri, apiUri);
        boolean encryptResponse = matches(ruleRegistry.getResponseEncryptUriList(), uri, prefixUri, apiUri);

        if (encryptionConfig.getDefaultMode() == EncryptionDefaultMode.ALL
                && !ruleRegistry.hasExplicitEncryptOrDecryptRules()) {
            decryptRequestBody = true;
            encryptResponse = true;
        }

        List<String> decryptParams = findDecryptParams(uri, prefixUri, apiUri);

        boolean decryptIgnored = matches(ruleRegistry.getRequestDecryptUriIgnoreList(), uri, prefixUri, apiUri);
        if (decryptIgnored) {
            decryptRequestBody = false;
            decryptParams = Collections.emptyList();
        }

        boolean encryptIgnored = matches(ruleRegistry.getResponseEncryptUriIgnoreList(), uri, prefixUri, apiUri);
        if (encryptIgnored) {
            encryptResponse = false;
        }

        if (StringUtils.endsWithIgnoreCase(methodType, RequestMethod.GET.name())) {
            decryptRequestBody = false;
        }

        return new EncryptionDecision(decryptRequestBody, encryptResponse, decryptParams);
    }

    private void processDecryption(EncryptionRequestWrapper requestWrapper,
                                   HttpServletRequest request,
                                   EncryptionDecision decision) {
        String requestData = requestWrapper.getRequestData();
        logger.debug("RequestData: {}", requestData);
        try {
            if (decision.isDecryptRequestBody()) {
                String decryptRequestData = encryptAlgorithm.decrypt(requestData, encryptionConfig.getKey());
                logger.debug("DecryptRequestData: {}", decryptRequestData);
                requestWrapper.setRequestData(decryptRequestData);
            }

            if (!decision.getDecryptParams().isEmpty()) {
                requestWrapper.setParamMap(decryptParams(request, decision.getDecryptParams()));
            }
        } catch (Exception e) {
            logger.error("请求数据解密失败", e);
            throw new IllegalStateException(e);
        }
    }

    private Map<String, String> decryptParams(HttpServletRequest request, List<String> decryptParams) throws Exception {
        Map<String, String> paramMap = new HashMap<>();
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String paramName = parameterNames.nextElement();
            if (!decryptParams.contains(paramName)) {
                continue;
            }

            String paramValue = request.getParameter(paramName);
            if (paramValue != null) {
                paramMap.put(paramName, encryptAlgorithm.decrypt(paramValue, encryptionConfig.getKey()));
            }
        }
        return paramMap;
    }

    private void writeResponse(EncryptionResponseWrapper responseWrapper, HttpServletResponse response) throws IOException {
        if (shouldSkipResponseEncryption(response)) {
            writePlainContent(responseWrapper, response);
            return;
        }
        writeEncryptContent(responseWrapper.getResponseData(), response);
    }

    private boolean shouldSkipResponseEncryption(HttpServletResponse response) {
        int status = response.getStatus();
        if (status == HttpServletResponse.SC_NO_CONTENT || status == HttpServletResponse.SC_NOT_MODIFIED) {
            return true;
        }
        if (isRedirect(status)) {
            return true;
        }
        if (!encryptionConfig.isEncryptErrorResponses() && status >= 400) {
            return true;
        }
        if (encryptionConfig.isSkipAttachmentResponses() && isAttachment(response.getHeader("Content-Disposition"))) {
            return true;
        }
        String contentType = response.getContentType();
        if (contentType == null) {
            return false;
        }
        String normalizedContentType = contentType.toLowerCase(Locale.ROOT);
        return encryptionConfig.getSkipResponseContentTypes().stream()
                .filter(StringUtils::hasText)
                .map(type -> type.toLowerCase(Locale.ROOT))
                .anyMatch(normalizedContentType::startsWith);
    }

    private boolean isRedirect(int status) {
        return status >= 300 && status < 400;
    }

    private boolean isAttachment(String contentDisposition) {
        return contentDisposition != null
                && contentDisposition.toLowerCase(Locale.ROOT).contains("attachment");
    }

    private void writePlainContent(EncryptionResponseWrapper responseWrapper, HttpServletResponse response) throws IOException {
        byte[] responseBytes = responseWrapper.getResponseBytes();
        if (response.getStatus() == HttpServletResponse.SC_NO_CONTENT
                || response.getStatus() == HttpServletResponse.SC_NOT_MODIFIED) {
            response.setContentLength(0);
            return;
        }
        response.setContentLength(responseBytes.length);
        ServletOutputStream out = response.getOutputStream();
        out.write(responseBytes);
        out.flush();
    }

    private void writeEncryptContent(String responseData, HttpServletResponse response) throws IOException {
        logger.debug("ResponseData: {}", responseData);
        ServletOutputStream out = null;
        try {
            responseData = encryptAlgorithm.encrypt(responseData, encryptionConfig.getKey());
            logger.debug("EncryptResponseData: {}", responseData);
            response.setCharacterEncoding(encryptionConfig.getResponseCharset());
            byte[] responseBytes = responseData.getBytes(Charset.forName(encryptionConfig.getResponseCharset()));
            response.setContentLength(responseBytes.length);
            out = response.getOutputStream();
            out.write(responseBytes);
        } catch (Exception e) {
            logger.error("响应数据加密失败", e);
            throw new IllegalStateException(e);
        } finally {
            if (out != null) {
                out.flush();
            }
        }
    }

    private boolean matches(List<String> list, String uri, String prefixUri, String apiUri) {
        if (CollectionUtils.isEmpty(list)) {
            return false;
        }
        for (String rule : list) {
            if (!StringUtils.hasText(rule)) {
                continue;
            }
            if (rule.equals(uri) || rule.equals(prefixUri) || rule.equals(apiUri)) {
                return true;
            }
            if (antPathMatcher.match(rule, uri)
                    || antPathMatcher.match(rule, prefixUri)
                    || (apiUri != null && antPathMatcher.match(rule, apiUri))) {
                return true;
            }
        }
        return false;
    }

    private List<String> findDecryptParams(String uri, String prefixUri, String apiUri) {
        List<String> params = ruleRegistry.getRequestDecryptParams(prefixUri);
        if (!params.isEmpty()) {
            return params;
        }
        params = ruleRegistry.getRequestDecryptParams(uri);
        if (!params.isEmpty()) {
            return params;
        }
        if (apiUri != null) {
            params = ruleRegistry.getRequestDecryptParams(apiUri);
        }
        return params;
    }

    private String resolveApiUri(HttpServletRequest request) {
        try {
            HandlerExecutionChain handler = getHandler(request);
            if (handler == null || !(handler.getHandler() instanceof HandlerMethod)) {
                return null;
            }
            HandlerMethod handlerMethod = (HandlerMethod) handler.getHandler();
            return RequestUriUtils.getApiUri(
                    handlerMethod.getBeanType(),
                    handlerMethod.getMethod(),
                    request.getContextPath()
            );
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    protected HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
        if (Objects.isNull(dispatcherServlet)) {
            return null;
        }
        if (dispatcherServlet.getHandlerMappings() != null) {
            for (HandlerMapping mapping : dispatcherServlet.getHandlerMappings()) {
                HandlerExecutionChain handler = mapping.getHandler(request);
                if (handler != null) {
                    return handler;
                }
            }
        }
        return null;
    }
}
