package org.peach.common.utils;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;


/**
 * JSON 序列化与反序列化工具（Jackson 3），供异常消息体、日志等统一使用。
 */
public final class JSONUtil {

	private static final ObjectMapper MAPPER = JsonMapper.builder().build();

	private JSONUtil() {
	}

	/** 序列化为 JSON 字符串；失败时返回固定错误占位 JSON。 */
	public static String toJson(Object value) {
		if (value == null) {
			return "null";
		}
		try {
			return MAPPER.writeValueAsString(value);
		}
		catch (JacksonException e) {
			return "{\"error\":\"JSONUtil.toJson 序列化失败\"}";
		}
	}

	/** 将 JSON 字符串反序列化为指定类型。 */
	public static <T> T fromJson(String json, Class<T> type) throws JacksonException {
		return MAPPER.readValue(json, type);
	}

	/** 将 JSON 字符串反序列化为泛型结构（如 {@code Map<String,Object>}）。 */
	public static <T> T fromJson(String json, TypeReference<T> typeRef) throws JacksonException {
		return MAPPER.readValue(json, typeRef);
	}
}
