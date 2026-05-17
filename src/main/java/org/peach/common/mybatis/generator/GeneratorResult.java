package org.peach.common.mybatis.generator;

import java.nio.file.Path;
import java.util.List;

/**
 * {@link GeneratorUtil#generateAll} 的一次性生成结果：MBG 警告与已写入的 MVC 源码路径。
 */
public record GeneratorResult(

		/** MyBatis Generator 产出的非致命警告，可能为空列表 */
		List<String> mybatisGeneratorWarnings,

		/** 本次由 MVC 层生成器写入的 Java 文件（VO / Service / Controller 等） */
		List<Path> mvcGeneratedJavaFiles) {

	public GeneratorResult {
		mybatisGeneratorWarnings = List.copyOf(mybatisGeneratorWarnings);
		mvcGeneratedJavaFiles = List.copyOf(mvcGeneratedJavaFiles);
	}
}
