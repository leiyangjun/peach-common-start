package org.peach.common.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * JSON 序列化与反序列化工具，供 MVC 异常消息体、日志等统一使用。
 */
public final class JSONUtil {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private JSONUtil() {
	}

	/** 将对象序列化为 JSON 字符串；失败时返回固定占位串，避免调用方再包一层 try-catch。 */
	public static String toJson(Object value) {
		if (value == null) {
			return "null";
		}
		try {
			return MAPPER.writeValueAsString(value);
		} catch (JsonProcessingException e) {
			return "{\"error\":\"JSONUtil.toJson 序列化失败\"}";
		}
	}

	/** 将 JSON 字符串反序列化为指定类型。 */
	public static <T> T fromJson(String json, Class<T> type) throws JsonProcessingException {
		return MAPPER.readValue(json, type);
	}

	/** 将 JSON 字符串反序列化为泛型结构（如 {@code Map<String,Object>}）。 */
	public static <T> T fromJson(String json, TypeReference<T> typeRef) throws JsonProcessingException {
		return MAPPER.readValue(json, typeRef);
	}
}
