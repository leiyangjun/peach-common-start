package org.peach.common.mybatis.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.peach.common.mybatis.annotation.ID;
import org.peach.common.mybatis.annotation.TableName;

/**
 * {@link CommonSqlProvider} 列名转义与主键生成策略单元测试。
 */
class CommonSqlProviderTest {

	@TableName("pk_long_demo")
	static class LongPkEntity {
		@ID
		private Long id;

		Long getId() {
			return id;
		}
	}

	@TableName("pk_str_demo")
	static class StringPkEntity {
		@ID
		private String id;

		String getId() {
			return id;
		}
	}

	@Test
	void sqlColumnName_quotesReservedDesc() {
		assertEquals("\"DESC\"", CommonSqlProvider.sqlColumnName("desc"));
	}

	@Test
	void sqlColumnName_quotesOtherReservedWords() {
		assertEquals("\"ORDER\"", CommonSqlProvider.sqlColumnName("order"));
		assertEquals("\"USER\"", CommonSqlProvider.sqlColumnName("user"));
		assertEquals("\"GROUP\"", CommonSqlProvider.sqlColumnName("group"));
	}

	@Test
	void sqlColumnName_leavesNonReservedUnquoted() {
		assertEquals("USER_NAME", CommonSqlProvider.sqlColumnName("userName"));
		assertEquals("ID", CommonSqlProvider.sqlColumnName("id"));
		assertEquals("EDIT_TIME", CommonSqlProvider.sqlColumnName("editTime"));
	}

	@Test
	void quoteIfReserved_onlyQuotesWhenNeeded() {
		assertEquals("\"DESC\"", CommonSqlProvider.quoteIfReserved("DESC"));
		assertEquals("NAME", CommonSqlProvider.quoteIfReserved("NAME"));
	}

	@Test
	void rename_unchangedWithoutQuoting() {
		assertEquals("DESC", CommonSqlProvider.rename("desc"));
	}

	@Test
	void ensureApplicationGeneratedPrimaryKey_assignsShortSnowIdForLongPk() {
		LongPkEntity entity = new LongPkEntity();
		CommonSqlProvider.ensureApplicationGeneratedPrimaryKey(entity);
		assertThat(entity.getId()).isNotNull().isPositive();
		assertThat(String.valueOf(entity.getId())).hasSizeBetween(12, 16);
	}

	@Test
	void ensureApplicationGeneratedPrimaryKey_assignsShortId22ForStringPk() {
		StringPkEntity entity = new StringPkEntity();
		CommonSqlProvider.ensureApplicationGeneratedPrimaryKey(entity);
		assertThat(entity.getId()).hasSize(22);
	}

	@Test
	void getTableKeyValue_usesShortSnowIdForLongPk() {
		String keyValue = CommonSqlProvider.getTableKeyValue(LongPkEntity.class, false);
		assertThat(keyValue).matches("\\d{12,16}");
	}
}
