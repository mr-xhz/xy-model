package com.xy.model.annotation;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.InjectionMetadata;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import com.xy.model.core.ModelFactory;
import com.xy.utils.util.StringUtil;

public class ModelAutowiredAnnotationBeanPostProcessor extends InstantiationAwareBeanPostProcessorAdapter
	implements MergedBeanDefinitionPostProcessor, PriorityOrdered, BeanFactoryAware {

	protected final Log logger = LogFactory.getLog(getClass());
	
	private int order = Ordered.LOWEST_PRECEDENCE - 2;
	
	private final Set<Class<? extends Annotation>> autowiredAnnotationTypes =
			new LinkedHashSet<Class<? extends Annotation>>();
	
	private ConfigurableListableBeanFactory beanFactory;
	
	private final Map<String, InjectionMetadata> InjectionMetadataCache =
			new ConcurrentHashMap<String, InjectionMetadata>(256);
	

	/**
	 * Create a new AutowiredAnnotationBeanPostProcessor
	 * for Spring's standard {@link Autowired} annotation.
	 * <p>Also supports JSR-330's {@link javax.inject.Inject} annotation, if available.
	 */
	@SuppressWarnings("unchecked")
	public ModelAutowiredAnnotationBeanPostProcessor() {
		this.autowiredAnnotationTypes.add(ModelAutowired.class);
		try {
			this.autowiredAnnotationTypes.add((Class<? extends Annotation>)
					ClassUtils.forName("javax.inject.Inject", ModelAutowiredAnnotationBeanPostProcessor.class.getClassLoader()));
			logger.info("JSR-330 'javax.inject.Inject' annotation found and supported for autowiring");
		}
		catch (ClassNotFoundException ex) {
			// JSR-330 API not available - simply skip.
		}
	}
	
	
	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		if (!(beanFactory instanceof ConfigurableListableBeanFactory)) {
			throw new IllegalArgumentException(
					"AutowiredAnnotationBeanPostProcessor requires a ConfigurableListableBeanFactory");
		}
		this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
	}

	private AnnotationAttributes findAutowiredAnnotation(AccessibleObject ao) {
		if (ao.getAnnotations().length > 0) {
			for (Class<? extends Annotation> type : this.autowiredAnnotationTypes) {
				AnnotationAttributes attributes = AnnotatedElementUtils.getMergedAnnotationAttributes(ao, type);
				if (attributes != null) {
					return attributes;
				}
			}
		}
		return null;
	}
	
	private InjectionMetadata findAutowiringMetadata(String beanName, Class<?> clazz, PropertyValues pvs) {
		// Fall back to class name as cache key, for backwards compatibility with custom callers.
		String cacheKey = (StringUtil.isNotEmpty(beanName) ? beanName : clazz.getName());
		// Quick check on the concurrent map first, with minimal locking.
		InjectionMetadata metadata = this.InjectionMetadataCache.get(cacheKey);
		if (InjectionMetadata.needsRefresh(metadata, clazz)) {
			synchronized (this.InjectionMetadataCache) {
				metadata = this.InjectionMetadataCache.get(cacheKey);
				if (InjectionMetadata.needsRefresh(metadata, clazz)) {
					if (metadata != null) {
						metadata.clear(pvs);
					}
					try {
						metadata = buildAutowiringMetadata(clazz);
						this.InjectionMetadataCache.put(cacheKey, metadata);
					}
					catch (NoClassDefFoundError err) {
						throw new IllegalStateException("Failed to introspect bean class [" + clazz.getName() +
								"] for autowiring metadata: could not find class that it depends on", err);
					}
				}
			}
		}
		return metadata;
	}
	
	
	@Override
	public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
		if (beanType != null) {
			InjectionMetadata metadata = findAutowiringMetadata(beanName, beanType, null);
			metadata.checkConfigMembers(beanDefinition);
		}
	}
	
	
	private InjectionMetadata buildAutowiringMetadata(final Class<?> clazz) {
		LinkedList<InjectionMetadata.InjectedElement> elements = new LinkedList<InjectionMetadata.InjectedElement>();
		Class<?> targetClass = clazz;

		do {
			final LinkedList<InjectionMetadata.InjectedElement> currElements =
					new LinkedList<InjectionMetadata.InjectedElement>();

			ReflectionUtils.doWithLocalFields(targetClass, new ReflectionUtils.FieldCallback() {
				@Override
				public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
					AnnotationAttributes ann = findAutowiredAnnotation(field);
					if (ann != null) {
						if (Modifier.isStatic(field.getModifiers())) {
							if (logger.isWarnEnabled()) {
								logger.warn("ModelAutowired annotation is not supported on static fields: " + field);
							}
							return;
						}
						currElements.add(new ModelAutowiredFieldElement(field));
					}
				}
			});
			

			elements.addAll(0, currElements);
			targetClass = targetClass.getSuperclass();
		}
		while (targetClass != null && targetClass != Object.class);

		return new InjectionMetadata(clazz, elements);
	}
	
	/**
	 * Class representing injection information about an annotated field.
	 */
	private class ModelAutowiredFieldElement extends InjectionMetadata.InjectedElement {


		public ModelAutowiredFieldElement(Field field) {
			super(field, null);
		}

		@Override
		protected void inject(Object bean, String beanName, PropertyValues pvs) throws Throwable {
			Field field = (Field) this.member;
			if(!(field.getGenericType() instanceof ParameterizedType)){
				throw new IllegalStateException(field.getName() + " is not a model!!!");
			}
			Class<?> clazz = (Class<?>)((ParameterizedType)(field.getGenericType())).getActualTypeArguments()[0];
			
			ReflectionUtils.makeAccessible(field);
			field.set(bean, ModelFactory.getModel(clazz));
		}
	}
	
	@Override
	public PropertyValues postProcessPropertyValues(
			PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) throws BeansException {
		InjectionMetadata metadata = findAutowiringMetadata(beanName, bean.getClass(), pvs);
		try {
			metadata.inject(bean, beanName, pvs);
		}
		catch (BeanCreationException ex) {
			throw ex;
		}
		catch (Throwable ex) {
			throw new BeanCreationException(beanName, "ModelAutowired of autowired dependencies failed", ex);
		}
		return pvs;
	}

}
