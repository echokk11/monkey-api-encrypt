package com.unknown.encrypt.springboot.init;

import com.unknown.encrypt.core.EncryptionConfig;
import com.unknown.encrypt.core.EncryptionRuleRegistry;
import com.unknown.encrypt.springboot.annotation.Decrypt;
import com.unknown.encrypt.springboot.annotation.DecryptIgnore;
import com.unknown.encrypt.springboot.annotation.Encrypt;
import com.unknown.encrypt.springboot.annotation.EncryptIgnore;
import com.unknown.encrypt.util.RequestUriUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Controller;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.*;

public class ApiEncryptDataInit {

    private static final Logger logger = LoggerFactory.getLogger(ApiEncryptDataInit.class);

    public EncryptionRuleRegistry createRuleRegistry(ApplicationContext context, EncryptionConfig config) {
        AnnotationRules annotationRules = collectAnnotationRules(context);
        return new EncryptionRuleRegistry(
                merge(config.getResponseEncryptUriList(), annotationRules.responseEncryptUriList),
                merge(config.getRequestDecryptUriList(), annotationRules.requestDecryptUriList),
                merge(config.getResponseEncryptUriIgnoreList(), annotationRules.responseEncryptUriIgnoreList),
                merge(config.getRequestDecryptUriIgnoreList(), annotationRules.requestDecryptUriIgnoreList),
                mergeParams(config.getRequestDecryptParams(), annotationRules.requestDecryptParams)
        );
    }

    private AnnotationRules collectAnnotationRules(ApplicationContext context) {
        String contextPath = context.getEnvironment().getProperty("server.servlet.context-path");
        Map<String, Object> beanMap = context.getBeansWithAnnotation(Controller.class);
        AnnotationRules rules = new AnnotationRules();

        for (Object bean : beanMap.values()) {
            Class<?> beanClass = ClassUtils.getUserClass(bean);
            Method[] methods = beanClass.getMethods();
            for (Method method : methods) {
                collectEncryptRule(beanClass, method, contextPath, rules);
                collectDecryptRule(beanClass, method, contextPath, rules);
                collectEncryptIgnoreRule(beanClass, method, contextPath, rules);
                collectDecryptIgnoreRule(beanClass, method, contextPath, rules);
            }
        }

        return rules;
    }

    private void collectEncryptRule(Class<?> beanClass, Method method, String contextPath, AnnotationRules rules) {
        Encrypt encrypt = AnnotationUtils.findAnnotation(method, Encrypt.class);
        if (encrypt == null) {
            return;
        }

        String uri = resolveUri(encrypt.value(), beanClass, method, contextPath);
        logger.debug("Encrypt URI: {}", uri);
        rules.responseEncryptUriList.add(uri);
    }

    private void collectDecryptRule(Class<?> beanClass, Method method, String contextPath, AnnotationRules rules) {
        Decrypt decrypt = AnnotationUtils.findAnnotation(method, Decrypt.class);
        if (decrypt == null) {
            return;
        }

        String uri = resolveUri(decrypt.value(), beanClass, method, contextPath);
        String decryptParam = resolveDecryptParam(decrypt);
        if (StringUtils.hasText(decryptParam)) {
            rules.requestDecryptParams.put(uri, splitParams(decryptParam));
        }

        logger.debug("Decrypt URI: {}", uri);
        rules.requestDecryptUriList.add(uri);
    }

    private void collectEncryptIgnoreRule(Class<?> beanClass, Method method, String contextPath, AnnotationRules rules) {
        EncryptIgnore encryptIgnore = AnnotationUtils.findAnnotation(method, EncryptIgnore.class);
        if (encryptIgnore == null) {
            return;
        }

        String uri = resolveUri(encryptIgnore.value(), beanClass, method, contextPath);
        logger.debug("EncryptIgnore URI: {}", uri);
        rules.responseEncryptUriIgnoreList.add(uri);
    }

    private void collectDecryptIgnoreRule(Class<?> beanClass, Method method, String contextPath, AnnotationRules rules) {
        DecryptIgnore decryptIgnore = AnnotationUtils.findAnnotation(method, DecryptIgnore.class);
        if (decryptIgnore == null) {
            return;
        }

        String uri = resolveUri(decryptIgnore.value(), beanClass, method, contextPath);
        logger.debug("DecryptIgnore URI: {}", uri);
        rules.requestDecryptUriIgnoreList.add(uri);
    }

    private String resolveUri(String configuredUri, Class<?> beanClass, Method method, String contextPath) {
        if (StringUtils.hasText(configuredUri)) {
            return configuredUri;
        }
        return RequestUriUtils.getApiUri(beanClass, method, contextPath);
    }

    @SuppressWarnings("deprecation")
    private String resolveDecryptParam(Decrypt decrypt) {
        if (StringUtils.hasText(decrypt.decryptParam())) {
            return decrypt.decryptParam();
        }
        return decrypt.decyptParam();
    }

    private List<String> splitParams(String params) {
        List<String> result = new ArrayList<>();
        Arrays.stream(params.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .forEach(result::add);
        return result;
    }

    private List<String> merge(List<String> configuredRules, List<String> annotationRules) {
        List<String> merged = new ArrayList<>();
        if (configuredRules != null) {
            merged.addAll(configuredRules);
        }
        if (annotationRules != null) {
            merged.addAll(annotationRules);
        }
        return merged;
    }

    private Map<String, List<String>> mergeParams(Map<String, List<String>> configuredParams,
                                                  Map<String, List<String>> annotationParams) {
        Map<String, List<String>> merged = new LinkedHashMap<>();
        if (configuredParams != null) {
            configuredParams.forEach((key, value) -> merged.put(normalizeParamUri(key), splitParamList(value)));
        }
        if (annotationParams != null) {
            annotationParams.forEach((key, value) -> merged.put(key, splitParamList(value)));
        }
        return merged;
    }

    private String normalizeParamUri(String uri) {
        return uri == null ? null : uri.replace("$", ":");
    }

    private List<String> splitParamList(List<String> params) {
        List<String> result = new ArrayList<>();
        if (params == null) {
            return result;
        }
        for (String param : params) {
            if (param == null) {
                continue;
            }
            Arrays.stream(param.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .forEach(result::add);
        }
        return result;
    }

    private static class AnnotationRules {
        private final List<String> responseEncryptUriList = new ArrayList<>();
        private final List<String> requestDecryptUriList = new ArrayList<>();
        private final List<String> responseEncryptUriIgnoreList = new ArrayList<>();
        private final List<String> requestDecryptUriIgnoreList = new ArrayList<>();
        private final Map<String, List<String>> requestDecryptParams = new LinkedHashMap<>();
    }
}
