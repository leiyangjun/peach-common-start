package org.peach.common.mvc.annotation.start;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Peach 微服务统一入口组合注解：等价于 {@link SpringBootApplication} + {@link EnableDiscoveryClient}。
 * <p>
 * 下游业务启动类仅需标注本注解即可启动 Spring Boot 并接入服务发现。
 * </p>
 * <p>
 * <b>Maven：</b>{@code peach-common-start} 已传递 {@code spring-cloud-starter-alibaba-nacos-discovery}
 * 与 {@code spring-cloud-starter-loadbalancer}，业务工程无需再写上述依赖。若使用 Nacos 配置中心，再引入
 * {@code spring-cloud-starter-alibaba-nacos-config}，并在 {@code application.yml} 中按需配置
 * {@code spring.config.import}（Boot 3.x 推荐方式）。
 * </p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SpringBootApplication
@EnableDiscoveryClient
public @interface PeachCloud {
}
