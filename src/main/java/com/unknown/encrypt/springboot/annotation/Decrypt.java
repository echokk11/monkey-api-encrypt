package com.unknown.encrypt.springboot.annotation;

import java.lang.annotation.*;

/**
 *
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Decrypt {

	String value() default "";

	/**
	 * URL 参数解密，多个参数用逗号分隔，比如 name,age。
	 *
	 * @return 解密参数信息
	 */
	String decryptParam() default "";

	/**
	 * @return 解密参数信息
	 * @deprecated use {@link #decryptParam()}.
	 */
	@Deprecated
	String decyptParam() default "";
	
}
