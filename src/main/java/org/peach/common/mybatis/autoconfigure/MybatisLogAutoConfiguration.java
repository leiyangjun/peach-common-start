package org.peach.common.mybatis.autoconfigure;

import java.util.List;
import java.util.Arrays;

import javax.sql.DataSource;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.slf4j.Slf4jImpl;
import org.apache.ibatis.logging.stdout.StdOutImpl;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.peach.common.mybatis.interceptor.MybatisLogInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * MyBatis 基础设施与 SQL 日志相关自动配置：在存在 {@link DataSource} 时补齐
 * {@link SqlSessionFactory}/{@link SqlSessionTemplate}，按业务启动包自动扫描 {@link Mapper} 接口；
 * 非严格生产环境注册 {@link MybatisLogInterceptor}，生产 profile 不注册该拦截器。
 *
 * @author leiyangjun
 */
@AutoConfiguration(afterName = "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration")
@ConditionalOnClass({ SqlSessionFactory.class, SqlSessionFactoryBean.class, MapperScannerConfigurer.class })
public class MybatisLogAutoConfiguration {

	private static final Logger log = LoggerFactory.getLogger(MybatisLogAutoConfiguration.class);

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnSingleCandidate(DataSource.class)
	SqlSessionFactory sqlSessionFactory(DataSource dataSource, Environment environment) throws Exception {
		SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
		factory.setDataSource(dataSource);
		Configuration configuration = new Configuration();
		// PostgreSQL 等返回 user_type 风格列名时，须映射到 userType；默认 false 会导致属性全为 null，误判业务字段
		configuration.setMapUnderscoreToCamelCase(true);
		Class<? extends Log> logImpl = resolveMybatisLogImpl(environment);
		configuration.setLogImpl(logImpl);
		factory.setConfiguration(configuration);
		if (!isStrictProduction(environment)) {
			factory.setPlugins(new MybatisLogInterceptor());
		}
		SqlSessionFactory sqlSessionFactory = factory.getObject();
		if (sqlSessionFactory == null) {
			throw new IllegalStateException("SqlSessionFactory 创建失败：返回值为 null");
		}
		log.info("peach-common-start 自动配置已激活: MybatisLogAutoConfiguration（SqlSessionFactory, logImpl={}）",
				logImpl.getSimpleName());
		return sqlSessionFactory;
	}

	/**
	 * 非严格生产环境使用 {@link StdOutImpl}，SQL 直接打到标准输出，不依赖 {@code logging.level}；
	 * 仅 {@code pro}/{@code produce}/{@code product} 使用 {@link Slf4jImpl} 纳入 Logback。
	 * <p>
	 * 注意：{@code test} profile 若仍用 Slf4j，且 root 为 WARN、未单独放开 {@code org.apache.ibatis}，会导致 MyBatis
	 * SQL 看不见；因此测试与本地统一走 StdOutImpl。
	 * </p>
	 */
	private static Class<? extends Log> resolveMybatisLogImpl(Environment environment) {
		return isStrictProduction(environment) ? Slf4jImpl.class : StdOutImpl.class;
	}

	private static boolean isStrictProduction(Environment environment) {
		return Arrays.stream(environment.getActiveProfiles())
				.anyMatch(MybatisLogAutoConfiguration::isStrictProductionProfile);
	}

	/** 仅线上发布类 profile，不含 {@code test}（测试环境仍需便于看见 SQL）。 */
	private static boolean isStrictProductionProfile(String profile) {
		if (!StringUtils.hasText(profile)) {
			return false;
		}
		String p = profile.trim();
		return "pro".equalsIgnoreCase(p) || "produce".equalsIgnoreCase(p) || "product".equalsIgnoreCase(p);
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
		log.info("peach-common-start 自动配置已激活: MybatisLogAutoConfiguration（Mapper 扫描包={}）", packages);
		return configurer;
	}
}
