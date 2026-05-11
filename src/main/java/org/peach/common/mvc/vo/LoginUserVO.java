package org.peach.common.mvc.vo;

import java.io.Serial;
import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 登录用户视图：与认证服务签发 JWT 标准 {@code sub}（JSON 对象）、网关解析后向下游追加的查询参数一一对应。
 * <p>
 * <strong>JSON（{@code sub}）</strong>：属性名为驼峰（与下表「JSON 属性」列一致），序列化时 {@code null} 字段可省略（{@link JsonInclude.Include#NON_NULL}）。<br>
 * <strong>网关查询参数</strong>：见 {@link org.peach.common.utils.LoginUserUtil} 中以 {@code peach_} 为前缀的常量（含 {@code peach_user_type}）；微服务通过 {@link org.peach.common.utils.LoginUserUtil#getLoginUser()} 读取。
 * </p>
 *
 * @author leiyangjun
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "当前登录用户快照：JWT sub 载荷 / 网关注入身份；下游请按字段语义展示或二次查询库表补齐")
public class LoginUserVO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "用户主键，雪花 Long；与 JWT sub.id、查询参数 peach_user_id 一致", example = "1970000000000000001")
	private Long id;

	@Schema(description = "用户类别：system=系统侧（后台/运营），app=应用端（C 端等）；与库 user_type、JWT sub.userType、查询参数 peach_user_type 一致；取值域见 {@link UserType}",
			example = "system", allowableValues = { "system", "app" })
	private String userType;

	@Schema(description = "登录名；系统侧账号通常非空，C 端可能为空（依赖手机等登录）", example = "admin")
	private String username;

	@Schema(description = "用户昵称，展示用", example = "系统管理员")
	private String nickname;

	@Schema(description = "真实姓名或对内实名", example = "张三")
	private String realName;

	@Schema(description = "手机号，国内建议 11 位", example = "13800138000")
	private String mobile;

	@Schema(description = "电子邮箱", example = "user@example.com")
	private String email;

	@Schema(description = "头像：完整 URL 或对象存储对象键，由业务约定", example = "https://cdn.example.com/avatar/u1.png")
	private String avatar;

	@Schema(description = "性别：0 未知，1 男，2 女", example = "1", allowableValues = { "0", "1", "2" })
	private Short gender;
}
