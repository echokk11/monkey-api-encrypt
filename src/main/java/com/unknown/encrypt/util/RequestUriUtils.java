package com.unknown.encrypt.util;


import com.unknown.encrypt.springboot.HttpMethodTypePrefixConstant;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;

public class RequestUriUtils {

    private static String separator = "/";

    public static String getApiUri(Class<?> clz, Method method, String contextPath) {
        String methodType = "";
        StringBuilder uri = new StringBuilder();

        RequestMapping reqMapping = AnnotationUtils.findAnnotation(clz, RequestMapping.class);
        String classUri = firstMapping(reqMapping);
        if (StringUtils.hasText(classUri)) {
            uri.append(formatUri(classUri));
        }


        GetMapping getMapping = AnnotationUtils.findAnnotation(method, GetMapping.class);
        PostMapping postMapping = AnnotationUtils.findAnnotation(method, PostMapping.class);
        RequestMapping requestMapping = AnnotationUtils.findAnnotation(method, RequestMapping.class);
        PutMapping putMapping = AnnotationUtils.findAnnotation(method, PutMapping.class);
        DeleteMapping deleteMapping = AnnotationUtils.findAnnotation(method, DeleteMapping.class);

        String getUri = firstMapping(getMapping);
        String postUri = firstMapping(postMapping);
        String putUri = firstMapping(putMapping);
        String deleteUri = firstMapping(deleteMapping);
        String requestUri = firstMapping(requestMapping);

        if (getMapping != null) {
            methodType = HttpMethodTypePrefixConstant.GET;
            appendUri(uri, getUri);

        } else if (postMapping != null) {
            methodType = HttpMethodTypePrefixConstant.POST;
            appendUri(uri, postUri);

        } else if (putMapping != null) {
            methodType = HttpMethodTypePrefixConstant.PUT;
            appendUri(uri, putUri);

        } else if (deleteMapping != null) {
            methodType = HttpMethodTypePrefixConstant.DELETE;
            appendUri(uri, deleteUri);

        } else if (requestMapping != null) {
            RequestMethod requestMethod = RequestMethod.GET;
            if (requestMapping.method().length > 0) {
                requestMethod = requestMapping.method()[0];
            }

            methodType = requestMethod.name().toLowerCase() + ":";
            appendUri(uri, requestUri);

        }

        if (StringUtils.hasText(contextPath) && !separator.equals(contextPath)) {
            if (contextPath.endsWith(separator)) {
                contextPath = contextPath.substring(0, contextPath.length() - 1);
            }
            return methodType + contextPath + uri.toString();
        }

        return methodType + uri.toString();
    }

    private static String formatUri(String uri) {
        if (uri.startsWith(separator)) {
            return uri;
        }
        return separator + uri;
    }

    private static void appendUri(StringBuilder uri, String path) {
        if (StringUtils.hasText(path)) {
            uri.append(formatUri(path));
        }
    }

    private static String firstMapping(RequestMapping mapping) {
        if (mapping == null) {
            return null;
        }
        return firstText(mapping.value(), mapping.path());
    }

    private static String firstMapping(GetMapping mapping) {
        if (mapping == null) {
            return null;
        }
        return firstText(mapping.value(), mapping.path());
    }

    private static String firstMapping(PostMapping mapping) {
        if (mapping == null) {
            return null;
        }
        return firstText(mapping.value(), mapping.path());
    }

    private static String firstMapping(PutMapping mapping) {
        if (mapping == null) {
            return null;
        }
        return firstText(mapping.value(), mapping.path());
    }

    private static String firstMapping(DeleteMapping mapping) {
        if (mapping == null) {
            return null;
        }
        return firstText(mapping.value(), mapping.path());
    }

    private static String firstText(String[] values, String[] paths) {
        if (values != null && values.length > 0 && StringUtils.hasText(values[0])) {
            return values[0];
        }
        if (paths != null && paths.length > 0 && StringUtils.hasText(paths[0])) {
            return paths[0];
        }
        return null;
    }
}
