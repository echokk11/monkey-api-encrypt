package com.unknown.encrypt.springboot.annotation;

import java.lang.annotation.*;

/**
 *
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EncryptIgnore {

	String value() default "";
	
}
