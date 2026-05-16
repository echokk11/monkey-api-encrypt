package com.unknown.encrypt.core;

import com.unknown.encrypt.util.AesEncryptUtils;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EncryptionFilterTest {

    private static final String KEY = "d7b85f6e214abcdad7b85f6e214abcda";

    @Test
    void explicitModeDoesNotProcessUnmatchedRequests() throws Exception {
        EncryptionConfig config = config();
        EncryptionFilter filter = new EncryptionFilter(config, EncryptionRuleRegistry.fromConfig(config), null);
        MockHttpServletRequest request = request("GET", "/plain");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, writeJson("{\"ok\":true}"));

        assertThat(response.getContentAsString()).isEqualTo("{\"ok\":true}");
    }

    @Test
    void allModeDecryptsPostBodyAndEncryptsResponse() throws Exception {
        EncryptionConfig config = config();
        config.setDefaultMode(EncryptionDefaultMode.ALL);
        EncryptionFilter filter = new EncryptionFilter(config, EncryptionRuleRegistry.fromConfig(config), null);
        String plainRequest = "{\"name\":\"alice\"}";
        MockHttpServletRequest request = request("POST", "/users");
        request.setContent(AesEncryptUtils.aesEncrypt(plainRequest, KEY).getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            String body = new String(servletRequest.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            assertThat(body).isEqualTo(plainRequest);
            servletResponse.setContentType("application/json");
            servletResponse.getWriter().write("{\"ok\":true}");
        });

        assertThat(AesEncryptUtils.aesDecrypt(response.getContentAsString(), KEY)).isEqualTo("{\"ok\":true}");
    }

    @Test
    void queryParamConfigDecryptsOnlyConfiguredParams() throws Exception {
        EncryptionConfig config = config();
        Map<String, java.util.List<String>> params = new LinkedHashMap<>();
        params.put("get:/search", Collections.singletonList("keyword"));
        config.setRequestDecryptParams(params);
        EncryptionRuleRegistry registry = EncryptionRuleRegistry.fromConfig(config);
        EncryptionFilter filter = new EncryptionFilter(config, registry, null);
        MockHttpServletRequest request = request("GET", "/search");
        request.addParameter("keyword", AesEncryptUtils.aesEncrypt("secret", KEY));
        request.addParameter("page", "1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
            assertThat(httpRequest.getParameter("keyword")).isEqualTo("secret");
            assertThat(httpRequest.getParameter("page")).isEqualTo("1");
            servletResponse.getWriter().write("ok");
        });

        assertThat(response.getContentAsString()).isEqualTo("ok");
    }

    @Test
    void ignoreRulesOverrideAllMode() throws Exception {
        EncryptionConfig config = config();
        config.setDefaultMode(EncryptionDefaultMode.ALL);
        config.setRequestDecryptUriIgnoreList(Collections.singletonList("post:/public"));
        config.setResponseEncryptUriIgnoreList(Collections.singletonList("post:/public"));
        EncryptionRuleRegistry registry = EncryptionRuleRegistry.fromConfig(config);
        EncryptionFilter filter = new EncryptionFilter(config, registry, null);
        MockHttpServletRequest request = request("POST", "/public");
        request.setContent("plain".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            assertThat(new String(servletRequest.getInputStream().readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("plain");
            servletResponse.getWriter().write("raw");
        });

        assertThat(response.getContentAsString()).isEqualTo("raw");
    }

    @Test
    void binaryResponseIsNotEncrypted() throws Exception {
        EncryptionConfig config = config();
        config.setResponseEncryptUriList(Collections.singletonList("get:/download"));
        EncryptionRuleRegistry registry = EncryptionRuleRegistry.fromConfig(config);
        EncryptionFilter filter = new EncryptionFilter(config, registry, null);
        MockHttpServletRequest request = request("GET", "/download");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
            httpResponse.setContentType("application/octet-stream");
            httpResponse.getOutputStream().write(new byte[]{1, 2, 3});
        });

        assertThat(response.getContentAsByteArray()).containsExactly(1, 2, 3);
    }

    @Test
    void errorResponseIsNotEncryptedByDefault() throws Exception {
        EncryptionConfig config = config();
        config.setResponseEncryptUriList(Collections.singletonList("get:/error"));
        EncryptionRuleRegistry registry = EncryptionRuleRegistry.fromConfig(config);
        EncryptionFilter filter = new EncryptionFilter(config, registry, null);
        MockHttpServletRequest request = request("GET", "/error");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
            httpResponse.setStatus(500);
            httpResponse.getWriter().write("failed");
        });

        assertThat(response.getContentAsString()).isEqualTo("failed");
    }

    private EncryptionConfig config() {
        EncryptionConfig config = new EncryptionConfig();
        config.setKey(KEY);
        return config;
    }

    private MockHttpServletRequest request(String method, String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        return request;
    }

    private FilterChain writeJson(String body) {
        return (servletRequest, servletResponse) -> {
            servletResponse.setContentType("application/json");
            servletResponse.getWriter().write(body);
        };
    }
}
