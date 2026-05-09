package org.peach.common.mybatis.autoconfigure;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.alibaba.druid.pool.DruidDataSource;

/**
 * 读写分离数据源自动配置：在开启 {@code spring.datasource.rw.enabled=true} 时注册写库/读库两个 Druid
 * {@link DataSource}，并以 {@link ReadWriteRoutingDataSource} 作为 {@code @Primary} 的 {@code dataSource}，
 * 根据当前事务是否只读在读写库间路由。
 *
 * @author leiyangjun
 */
@AutoConfiguration(beforeName = "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration")
@ConditionalOnClass({ DataSource.class, DruidDataSource.class })
@ConditionalOnProperty(prefix = "spring.datasource.rw", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(ReadWriteDataSourceProperties.class)
public class ReadWriteDataSourceAutoConfiguration {

	private static final Logger log = LoggerFactory.getLogger(ReadWriteDataSourceAutoConfiguration.class);

	@Bean("writeDataSource")
	DataSource writeDataSource(ReadWriteDataSourceProperties properties) {
		ReadWriteDataSourceProperties.Node write = properties.getWrite();
		if (StringUtils.isBlank(write.getUrl())) {
			throw new IllegalArgumentException("开启读写分离时，spring.datasource.rw.write.url 不能为空");
		}
		return createDruidDataSource(write.getUrl(), write.getUsername(), write.getPassword(), write.getDriverClassName());
	}

	@Bean("readDataSource")
	DataSource readDataSource(ReadWriteDataSourceProperties properties) {
		return createDruidDataSource(properties.resolveReadUrl(), properties.resolveReadUsername(),
				properties.resolveReadPassword(), properties.resolveReadDriverClassName());
	}

	@Bean("dataSource")
	@Primary
	DataSource routingDataSource(DataSource writeDataSource, DataSource readDataSource) {
		Map<Object, Object> targets = new HashMap<>(2);
		targets.put(ReadWriteRoutingDataSource.WRITE_KEY, writeDataSource);
		targets.put(ReadWriteRoutingDataSource.READ_KEY, readDataSource);
		ReadWriteRoutingDataSource routingDataSource = new ReadWriteRoutingDataSource();
		routingDataSource.setDefaultTargetDataSource(writeDataSource);
		routingDataSource.setTargetDataSources(targets);
		routingDataSource.afterPropertiesSet();
		log.info("peach-common-start 自动配置已激活: ReadWriteDataSourceAutoConfiguration（事务只读路由启用）");
		return routingDataSource;
	}

	private static DruidDataSource createDruidDataSource(String url, String username, String password,
			String driverClassName) {
		DruidDataSource ds = new DruidDataSource();
		ds.setUrl(url);
		ds.setUsername(username);
		ds.setPassword(password);
		if (StringUtils.isNotBlank(driverClassName)) {
			ds.setDriverClassName(driverClassName);
		}
		return ds;
	}
}

