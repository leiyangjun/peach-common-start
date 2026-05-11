package org.peach.common.mvc.jackson.sensitive;

import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * 注册脱敏 {@link com.fasterxml.jackson.databind.ser.BeanSerializerModifier} 的 Jackson 模块。
 *
 * @author leiyangjun
 */
public class SensitiveJacksonModule extends SimpleModule {

	private static final long serialVersionUID = 1L;

	public static final String MODULE_NAME = "peach-sensitive-json";

	public SensitiveJacksonModule() {
		super(MODULE_NAME);
		setSerializerModifier(new SensitiveBeanSerializerModifier());
	}
}
