package com.unknown.encrypt.springboot.autoconfigure;

import com.unknown.encrypt.algorithm.EncryptAlgorithm;
import com.unknown.encrypt.core.EncryptionConfig;
import com.unknown.encrypt.core.EncryptionFilter;
import com.unknown.encrypt.springboot.endpoint.EncryptEndpoint;
import com.unknown.encrypt.springboot.init.ApiEncryptDataInit;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.DispatcherServlet;


/**
 *
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({EncryptionFilter.class, DispatcherServlet.class})
@ConditionalOnProperty(prefix = "spring.encrypt", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(EncryptionConfig.class)
public class EncryptAutoConfiguration {

    private final EncryptionConfig encryptionConfig;

    private final EncryptAlgorithm encryptAlgorithm;

    /**
     * 用于解决@PathVariable风格的URI
     */
    private final DispatcherServlet dispatcherServlet;

    public EncryptAutoConfiguration(EncryptionConfig encryptionConfig,
                                    ObjectProvider<EncryptAlgorithm> encryptAlgorithm,
                                    ObjectProvider<DispatcherServlet> dispatcherServlet) {
        this.encryptionConfig = encryptionConfig;
        this.encryptAlgorithm = encryptAlgorithm.getIfAvailable();
        this.dispatcherServlet = dispatcherServlet.getIfAvailable();
    }

    /**
     * @return 过滤器
     */
    @Bean
    @ConditionalOnMissingBean
    public EncryptionFilter encryptionFilter() {
        if (encryptAlgorithm != null) {
            return new EncryptionFilter(encryptionConfig, encryptAlgorithm, dispatcherServlet);
        }
        return new EncryptionFilter(encryptionConfig, dispatcherServlet);
    }

    @Bean
    @ConditionalOnMissingBean(name = "encryptionFilterRegistration")
    public FilterRegistrationBean<EncryptionFilter> encryptionFilterRegistration(EncryptionFilter encryptionFilter) {
        FilterRegistrationBean<EncryptionFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(encryptionFilter);
        registration.addUrlPatterns(encryptionConfig.getUrlPatterns());
        registration.setName("EncryptionFilter");
        registration.setOrder(encryptionConfig.getOrder());
        return registration;
    }

    @Bean
    @ConditionalOnMissingBean
    public ApiEncryptDataInit apiEncryptDataInit() {
        return new ApiEncryptDataInit();
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.boot.actuate.endpoint.annotation.Endpoint")
    static class EncryptEndpointConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public EncryptEndpoint encryptEndpoint() {
            return new EncryptEndpoint();
        }
    }
}
