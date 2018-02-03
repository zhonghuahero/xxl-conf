package com.xxl.conf.core.spring;

import com.xxl.conf.core.XxlConfClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionVisitor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurablePropertyResolver;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringValueResolver;

import java.util.Properties;

/**
 * rewrite PropertyPlaceholderConfigurer
 *
 * @version 1.0
 * @author xuxueli 2015-9-12 19:42:49
 *
 */
public class XxlConfFactory extends PropertySourcesPlaceholderConfigurer {
	private static Logger logger = LoggerFactory.getLogger(XxlConfFactory.class);

	/**
	 * xxl conf bean definition visitor
	 *
	 * @return
	 */
	private BeanDefinitionVisitor getXxlConfBeanDefinitionVisitor(){
		// init value resolver
		StringValueResolver xxlConfValueResolver = new StringValueResolver() {
			String placeholderPrefix = "${";
			String placeholderSuffix = "}";
			@Override
			public String resolveStringValue(String strVal) {
				StringBuffer buf = new StringBuffer(strVal);
				// loop replace by xxl-conf, if the value match '${***}'
				boolean start = strVal.startsWith(placeholderPrefix);
				boolean end = strVal.endsWith(placeholderSuffix);
				while (start && end) {
					// replace by xxl-conf
					String key = buf.substring(placeholderPrefix.length(), buf.length() - placeholderSuffix.length());
					String zkValue = XxlConfClient.get(key, "");
					buf = new StringBuffer(zkValue);
					logger.info(">>>>>>>>>>> xxl-conf resolved placeholder '" + key + "' to value [" + zkValue + "]");
					start = buf.toString().startsWith(placeholderPrefix);
					end = buf.toString().endsWith(placeholderSuffix);
				}
				return buf.toString();
			}
		};

		// init bean define visitor
		BeanDefinitionVisitor xxlConfVisitor = new BeanDefinitionVisitor(xxlConfValueResolver);
		return xxlConfVisitor;
	}

	@Override
	protected void processProperties(ConfigurableListableBeanFactory beanFactoryToProcess, ConfigurablePropertyResolver propertyResolver) throws BeansException {
		//super.processProperties(beanFactoryToProcess, propertyResolver);

		// xxlConf Visitor
		BeanDefinitionVisitor xxlConfVisitor = getXxlConfBeanDefinitionVisitor();

		// visit bean definition
		String[] beanNames = beanFactoryToProcess.getBeanDefinitionNames();
		if (beanNames != null && beanNames.length > 0) {
			for (String beanName : beanNames) {
				if (!(beanName.equals(this.beanName) && beanFactoryToProcess.equals(this.beanFactory))) {
					// XML：resolves ${...} placeholders within bean definition property values

					BeanDefinition bd = beanFactoryToProcess.getBeanDefinition(beanName);
					xxlConfVisitor.visitBeanDefinition(bd);

					// Annotation：resolves ${...} placeholders within bean definition annotations
					//Object object = beanFactoryToProcess.getBean(beanName);

				}
			}
		}

		logger.info(">>>>>>>>>>> xxl conf, processProperties success}");
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

	private String beanName;
	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}

	private BeanFactory beanFactory;
	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public void setIgnoreUnresolvablePlaceholders(boolean ignoreUnresolvablePlaceholders) {
		super.setIgnoreUnresolvablePlaceholders(true);
	}

}