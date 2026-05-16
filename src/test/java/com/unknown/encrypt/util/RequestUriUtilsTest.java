package com.unknown.encrypt.util;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.assertj.core.api.Assertions.assertThat;

class RequestUriUtilsTest {

    @Test
    void derivesMethodPrefixWhenMethodMappingHasNoPath() throws Exception {
        assertThat(RequestUriUtils.getApiUri(SampleController.class,
                SampleController.class.getMethod("list"),
                null)).isEqualTo("get:/api/users");
    }

    @Test
    void derivesMethodPrefixAndPathWhenMethodMappingHasPath() throws Exception {
        assertThat(RequestUriUtils.getApiUri(SampleController.class,
                SampleController.class.getMethod("create"),
                null)).isEqualTo("post:/api/users/create");
    }

    @RequestMapping("/api/users")
    static class SampleController {

        @GetMapping
        public void list() {
        }

        @PostMapping("/create")
        public void create() {
        }
    }
}
