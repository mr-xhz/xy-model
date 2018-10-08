package com.xy.model.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Column {

	/**
	 * @Description 字段名字
	 * @author xhz
	 * @date 2018年8月27日 下午8:36:48
	 * @return
	 * @lastModifier
	 */
	String name() default "";
	/**
	 * @Description 数据库类型
	 * @author xhz
	 * @date 2018年8月27日 下午8:36:48
	 * @return
	 * @lastModifier
	 */
	String jdbcType() default "";
	
	/**
	 * @Description 是否主键
	 * @author xhz
	 * @date 2018年8月27日 下午8:36:55
	 * @return
	 * @lastModifier
	 */
	boolean priKey() default false;
	
	/**
	 * @Description 是否自增长
	 * @author xhz
	 * @date 2018年8月27日 下午8:37:04
	 * @return
	 * @lastModifier
	 */
	boolean autoIncrement() default true;
	
	/**
	 * @Description 字符集
	 * @author xhz
	 * @date 2018年8月27日 下午8:37:39
	 * @return
	 * @lastModifier
	 */
	String charset() default "";
	
	/**
	 * @Description 注释
	 * @author xhz
	 * @date 2018年8月27日 下午8:38:13
	 * @return
	 * @lastModifier
	 */
	String comment() default "";
	
	/**
	 * @Description 是否为空
	 * @author xhz
	 * @date 2018年8月27日 下午8:42:43
	 * @return
	 * @lastModifier
	 */
	boolean notNull() default true;
	
	/**
	 * @Description 默认值
	 * @author xhz
	 * @date 2018年8月27日 下午8:42:57
	 * @return
	 * @lastModifier
	 */
	String def() default "";
}
