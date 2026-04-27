package org.peach.common.mybatis.autoconfigure;

import java.util.List;

import javax.sql.DataSource;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

/**
 * MyBatis 基础设施自动配置：在存在 {@link DataSource} 时补齐
 * {@link SqlSessionFactory}/{@link SqlSessionTemplate}，并按业务启动包自动扫描
 * {@link Mapper} 标注接口，减少下游显式配置负担。
 *
 * @author leiyangjun
 */
@AutoConfiguration(afterName = "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration")
@ConditionalOnClass({ SqlSessionFactory.class, SqlSessionFactoryBean.class, MapperScannerConfigurer.class })
public class BaseMybatisAutoConfiguration {

	private static final Logger log = LoggerFactory.getLogger(BaseMybatisAutoConfiguration.class);

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnSingleCandidate(DataSource.class)
	SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
		SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
		factory.setDataSource(dataSource);
		SqlSessionFactory sqlSessionFactory = factory.getObject();
		if (sqlSessionFactory == null) {
			throw new IllegalStateException("SqlSessionFactory 创建失败：返回值为 null");
		}
		log.info("peach-common-start 自动配置已激活: BaseMybatisAutoConfiguration（SqlSessionFactory）");
		return sqlSessionFactory;
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnBean(SqlSessionFactory.class)
	SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
		return new SqlSessionTemplate(sqlSessionFactory);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnBean(SqlSessionFactory.class)
	MapperScannerConfigurer mapperScannerConfigurer(org.springframework.beans.factory.BeanFactory beanFactory) {
		List<String> packages = AutoConfigurationPackages.has(beanFactory)
				? AutoConfigurationPackages.get(beanFactory)
				: List.of();
		MapperScannerConfigurer configurer = new MapperScannerConfigurer();
		if (!packages.isEmpty()) {
			configurer.setBasePackage(StringUtils.collectionToCommaDelimitedString(packages));
		}
		configurer.setAnnotationClass(Mapper.class);
		configurer.setSqlSessionFactoryBeanName("sqlSessionFactory");
		log.info("peach-common-start 自动配置已激活: BaseMybatisAutoConfiguration（Mapper 扫描包={}）", packages);
		return configurer;
	}
}
