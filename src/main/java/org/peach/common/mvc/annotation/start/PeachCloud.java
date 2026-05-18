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
 * <b>Maven：</b>{@code peach-common-start} 已传递 Nacos 注册发现、配置中心与 LoadBalancer，业务工程无需重复声明依赖。
 * <b>配置：</b>{@code spring.profiles.active} 须由下游显式配置（Starter 不设默认）；连接地址与账号（{@code spring.cloud.nacos.server-addr/username/password}）由下游填写。
 * Nacos 的 namespace 等占位解析依赖上述 profile 或 {@code NACOS_NAMESPACE}；其余约定（group、DataId 等）见 Starter 默认配置。
 * 关闭 Nacos 可使用 {@code spring.cloud.nacos.discovery.enable=false} / {@code spring.cloud.nacos.config.enable=false}。
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
