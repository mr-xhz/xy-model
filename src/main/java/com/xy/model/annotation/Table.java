package com.xy.model.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.TYPE })
public @interface Table {
	/**
	 * @Description 表名
	 * @author xhz
	 * @date 2018年8月27日 下午8:45:04
	 * @return
	 * @lastModifier
	 */
	String value();
	/**
	 * @Description 前缀
	 * @author xhz
	 * @date 2018年8月27日 下午8:45:22
	 * @return
	 * @lastModifier
	 */
	String prefix() default "";
	/**
	 * @Description 是否自动创建
	 * @author xhz
	 * @date 2018年8月27日 下午8:45:44
	 * @return
	 * @lastModifier
	 */
	boolean create() default false;
	
	/**
	 * @Description 注释
	 * @author xhz
	 * @date 2018年8月27日 下午8:46:54
	 * @return
	 * @lastModifier
	 */
	String comment() default "";
	
	/**
	 * @Description 字符集
	 * @author xhz
	 * @date 2018年8月27日 下午8:46:59
	 * @return
	 * @lastModifier
	 */
	String charset() default "utf8";
	
	/**
	 * @Description 引擎
	 * @author xhz
	 * @date 2018年8月27日 下午8:47:06
	 * @return
	 * @lastModifier
	 */
	String engine() default "InnoDB";
}
