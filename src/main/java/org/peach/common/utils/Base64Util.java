package org.peach.common.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Objects;

/**
 * Base64 编解码与文件/流辅助（JDK {@link java.util.Base64}）。
 */
public final class Base64Util {

	private Base64Util() {
	}

	/**
	 * Base64 字符串 -> 字节数组。
	 */
	public static byte[] base64StrToBytes(String base64Str) {
		Objects.requireNonNull(base64Str, "base64Str");
		return Base64.getDecoder().decode(base64Str);
	}

	/**
	 * 字节数组 -> Base64 字符串（标准编码，含填充）。
	 */
	public static String bytesToBase64Str(byte[] bytes) {
		Objects.requireNonNull(bytes, "bytes");
		return Base64.getEncoder().encodeToString(bytes);
	}

	/**
	 * Base64 字符串解码后写入文件（覆盖已存在文件；父目录不存在则创建）。
	 */
	public static void base64StrToFile(String base64Str, Path targetPath) throws IOException {
		Objects.requireNonNull(targetPath, "targetPath");
		byte[] data = base64StrToBytes(base64Str);
		Path parent = targetPath.getParent();
		if (parent != null) {
			Files.createDirectories(parent);
		}
		Files.write(targetPath, data);
	}

	/**
	 * Base64 字符串解码后写入文件（覆盖已存在文件）。
	 */
	public static void base64StrToFile(String base64Str, java.io.File file) throws IOException {
		Objects.requireNonNull(file, "file");
		base64StrToFile(base64Str, file.toPath());
	}

	/**
	 * 读取输入流全部字节并转为 Base64 字符串。不关闭 {@code inputStream}，由调用方负责。
	 */
	public static String inputStreamToBase64Str(InputStream inputStream) throws IOException {
		Objects.requireNonNull(inputStream, "inputStream");
		return bytesToBase64Str(inputStream.readAllBytes());
	}
}

