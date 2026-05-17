package org.peach.common.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;


/**
 * Fastjson2 显式序列化工具；不参与 Spring MVC 默认 JSON 消息转换（Web 层仍用 Jackson）。
 */
public final class FastJson2Util {

	private FastJson2Util() {
	}

	/** 将对象序列化为 JSON 字符串。 */
	public static String toJson(Object value) {
		if (value == null) {
			return "null";
		}
		return JSON.toJSONString(value, JSONWriter.Feature.WriteMapNullValue);
	}

	/** 将 JSON 字符串反序列化为指定类型。 */
	public static <T> T fromJson(String json, Class<T> type) {
		return JSON.parseObject(json, type, JSONReader.Feature.SupportSmartMatch);
	}
}
