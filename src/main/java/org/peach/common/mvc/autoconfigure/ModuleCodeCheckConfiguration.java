package org.peach.common.mvc.autoconfigure;

import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.peach.common.mvc.result.ApiResult;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.env.Environment;

import lombok.extern.slf4j.Slf4j;

/**
 * 校验 {@code spring.application.module-code}：去空白后恰好四位，由字母或数字组成，首位须为字母，
 * 规范化后须全大写（配置可大小写混写，写入缓存前统一 {@link String#toUpperCase(Locale)}）。 通过后调用
 * {@link ModuleCodeCache#setCachedModule(String)}，使
 * {@link ModuleCodeCache#getModule()} 与
 * {@link ApiResult}、{@link org.peach.common.mvc.exception.ErrorResult} 拼接业务码一致。
 * <p>
 * 若校验失败会抛出运行时异常，直接中断 Spring Boot 启动流程（不会进入 Web 层统一异常处理）。
 * </p>
 *
 * @author leiyangjun
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(SpringApplicationModuleProperties.class)
public class ModuleCodeCheckConfiguration {

	public ModuleCodeCheckConfiguration(SpringApplicationModuleProperties props, Environment environment) {
		// 恰好 4 位：首位 A–Z，后 3 位 A–Z 或 0–9；与 trim + 大写后的串匹配
		String regex = "^[A-Z][A-Z0-9]{3}$";
		String rawModule = props.getModuleCode();
		String active = resolveActiveProfile(environment);
		String moduleUpper = rawModule == null ? "" : rawModule.trim().toUpperCase(Locale.ROOT);
		if (StringUtils.isBlank(active) || moduleUpper.length() != 4 || !moduleUpper.matches(regex)) {
			String reason = StringUtils.isBlank(active)
					? "spring.profiles.active 未配置或为空（须显式指定 dev/test 等环境标识）"
					: "spring.application.module-code 无效（当前值=" + rawModule
							+ "；须为四位字母数字且首位为字母，如 COMM）";
			log.error("启动失败：{}。请检查 application.yml 或 SPRING_PROFILES_ACTIVE / spring.application.module-code", reason);
			throw new IllegalStateException("启动失败：" + reason);
		}
		ModuleCodeCache.setCachedModule(moduleUpper);
		ModuleCodeCache.setActive(active);
		log.info("检测到模块编码配置 spring.application.module-code={}", moduleUpper);
	}

	/**
	 * 解析当前环境标识：AutoConfiguration 构造阶段 {@link Environment#getActiveProfiles()} 可能仍为空，
	 * 此时回退读取 {@code spring.profiles.active}；二者皆无则视为未配置，由上层校验触发启动失败。
	 */
	private static String resolveActiveProfile(Environment environment) {
		String[] activeProfiles = environment.getActiveProfiles();
		if (activeProfiles.length > 0 && StringUtils.isNotBlank(activeProfiles[0])) {
			return activeProfiles[0].trim();
		}
		String fromProperty = environment.getProperty("spring.profiles.active");
		return fromProperty == null ? "" : fromProperty.trim();
	}

}
