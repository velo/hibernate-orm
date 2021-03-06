/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.util.Locale;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertEquals;

/**
 * Unit test of the behavior of the SQLServerDialect utility methods
 *
 * @author Valotasion Yoryos
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class SQLServer2005DialectTestCase extends BaseUnitTestCase {
	private SQLServer2005Dialect dialect;

	@Before
	public void setup() {
		dialect = new SQLServer2005Dialect();
	}

	@After
	public void tearDown() {
		dialect = null;
	}

	@Test
	public void testGetLimitString() {
		String input = "select distinct f1 as f53245 from table849752 order by f234, f67 desc";

		assertEquals(
				"with query as (select inner_query.*, row_number() over (order by current_timestamp) as __hibernate_row_nr__ from ( " +
						"select distinct top(?) f1 as f53245 from table849752 order by f234, f67 desc ) inner_query )" +
						" select f53245 from query where __hibernate_row_nr__ >= ? and __hibernate_row_nr__ < ?",
				dialect.getLimitHandler().processSql( input, toRowSelection( 10, 15 ) ).toLowerCase(Locale.ROOT) );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10736")
	public void testGetLimitStringWithNewlineAfterSelect() {
		final String query = "select" + System.lineSeparator() + "* FROM Employee E WHERE E.firstName = :firstName";
		assertEquals(
				"WITH query AS (SELECT inner_query.*, ROW_NUMBER() OVER (ORDER BY CURRENT_TIMESTAMP) as __hibernate_row_nr__ FROM ( " +
						query + " ) inner_query ) SELECT * FROM query WHERE __hibernate_row_nr__ >= ? AND __hibernate_row_nr__ < ?",
				dialect.getLimitHandler().processSql( query, toRowSelection( 1, 25 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10736")
	public void testGetLimitStringWithNewlineAfterSelectWithMultipleSpaces() {
		final String query = "select    " + System.lineSeparator() + "* FROM Employee E WHERE E.firstName = :firstName";
		assertEquals(
				"WITH query AS (SELECT inner_query.*, ROW_NUMBER() OVER (ORDER BY CURRENT_TIMESTAMP) as __hibernate_row_nr__ FROM ( " +
						query + " ) inner_query ) SELECT * FROM query WHERE __hibernate_row_nr__ >= ? AND __hibernate_row_nr__ < ?",
				dialect.getLimitHandler().processSql( query, toRowSelection( 1, 25 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8507")
	public void testGetLimitStringWithNewlineAfterColumnList() {
		final String query = "select E.fieldA,E.fieldB" + System.lineSeparator() + "FROM Employee E WHERE E.firstName = :firstName";
		assertEquals(
				"WITH query AS (SELECT inner_query.*, ROW_NUMBER() OVER (ORDER BY CURRENT_TIMESTAMP) as __hibernate_row_nr__ FROM ( " +
						"select E.fieldA as page0_,E.fieldB as page1_" + System.lineSeparator() +
						"FROM Employee E WHERE E.firstName = :firstName ) inner_query ) SELECT page0_, page1_ FROM query " +
						"WHERE __hibernate_row_nr__ >= ? AND __hibernate_row_nr__ < ?",
				dialect.getLimitHandler().processSql( query, toRowSelection( 1, 25 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-6950")
	public void testGetLimitStringWithFromColumnName() {
		final String fromColumnNameSQL = "select persistent0_.rid as rid1688_, " +
				"persistent0_.deviationfromtarget as deviati16_1688_, " + // "from" character sequence as a part of the column name
				"persistent0_.sortindex as sortindex1688_ " +
				"from m_evalstate persistent0_ " +
				"where persistent0_.customerid=?";

		assertEquals(
				"WITH query AS (SELECT inner_query.*, ROW_NUMBER() OVER (ORDER BY CURRENT_TIMESTAMP) as __hibernate_row_nr__ FROM ( " +
						fromColumnNameSQL + " ) inner_query ) " +
						"SELECT rid1688_, deviati16_1688_, sortindex1688_ FROM query WHERE __hibernate_row_nr__ >= ? AND __hibernate_row_nr__ < ?",
				dialect.getLimitHandler().processSql( fromColumnNameSQL, toRowSelection( 1, 10 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8301")
	public void testGetLimitStringAliasGeneration() {
		final String notAliasedSQL = "select column1, column2, column3, column4 from table1";

		assertEquals(
				"WITH query AS (SELECT inner_query.*, ROW_NUMBER() OVER (ORDER BY CURRENT_TIMESTAMP) as __hibernate_row_nr__ FROM ( " +
						"select column1 as page0_, column2 as page1_, column3 as page2_, column4 as page3_ from table1 ) inner_query ) " +
						"SELECT page0_, page1_, page2_, page3_ FROM query WHERE __hibernate_row_nr__ >= ? AND __hibernate_row_nr__ < ?",
				dialect.getLimitHandler().processSql( notAliasedSQL, toRowSelection( 3, 5 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-7019")
	public void testGetLimitStringWithSubselect() {
		final String subselectInSelectClauseSQL = "select persistent0_.id as col_0_0_, " +
				"(select max(persistent1_.acceptancedate) " +
				"from av_advisoryvariant persistent1_ " +
				"where persistent1_.clientid=persistent0_.id) as col_1_0_ " +
				"from c_customer persistent0_ " +
				"where persistent0_.type='v'";

		assertEquals(
				"WITH query AS (SELECT inner_query.*, ROW_NUMBER() OVER (ORDER BY CURRENT_TIMESTAMP) as __hibernate_row_nr__ FROM ( " +
						subselectInSelectClauseSQL + " ) inner_query ) " +
						"SELECT col_0_0_, col_1_0_ FROM query WHERE __hibernate_row_nr__ >= ? AND __hibernate_row_nr__ < ?",
				dialect.getLimitHandler().processSql( subselectInSelectClauseSQL, toRowSelection( 2, 5 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-6728")
	public void testGetLimitStringCaseSensitive() {
		final String caseSensitiveSQL = "select persistent0_.id, persistent0_.uid AS tmp1, " +
				"(select case when persistent0_.name = 'Smith' then 'Neo' else persistent0_.id end) " +
				"from C_Customer persistent0_ " +
				"where persistent0_.type='Va' " +
				"order by persistent0_.Order";

		assertEquals(
				"WITH query AS (SELECT inner_query.*, ROW_NUMBER() OVER (ORDER BY CURRENT_TIMESTAMP) as __hibernate_row_nr__ FROM ( " +
						"select TOP(?) persistent0_.id as page0_, persistent0_.uid AS tmp1, " +
						"(select case when persistent0_.name = 'Smith' then 'Neo' else persistent0_.id end) as page1_ " +
						"from C_Customer persistent0_ where persistent0_.type='Va' order by persistent0_.Order ) " +
						"inner_query ) SELECT page0_, tmp1, page1_ FROM query WHERE __hibernate_row_nr__ >= ? AND __hibernate_row_nr__ < ?",
				dialect.getLimitHandler().processSql( caseSensitiveSQL, toRowSelection( 1, 2 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-6310")
	public void testGetLimitStringDistinctWithinAggregation() {
		final String distinctInAggregateSQL = "select aggregate_function(distinct p.n) as f1 from table849752 p order by f1";

		assertEquals(
				"WITH query AS (SELECT inner_query.*, ROW_NUMBER() OVER (ORDER BY CURRENT_TIMESTAMP) as __hibernate_row_nr__ FROM ( " +
						"select TOP(?) aggregate_function(distinct p.n) as f1 from table849752 p order by f1 ) inner_query ) " +
						"SELECT f1 FROM query WHERE __hibernate_row_nr__ >= ? AND __hibernate_row_nr__ < ?",
				dialect.getLimitHandler().processSql( distinctInAggregateSQL, toRowSelection( 2, 5 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-7370")
	public void testGetLimitStringWithMaxOnly() {
		final String query = "select product2x0_.id as id0_, product2x0_.description as descript2_0_ " +
				"from Product2 product2x0_ order by product2x0_.id";

		assertEquals(
				"select TOP(?) product2x0_.id as id0_, product2x0_.description as descript2_0_ " +
						"from Product2 product2x0_ order by product2x0_.id",
				dialect.getLimitHandler().processSql( query, toRowSelection( 0, 1 ) )
		);

		final String distinctQuery = "select distinct product2x0_.id as id0_, product2x0_.description as descript2_0_ " +
				"from Product2 product2x0_ order by product2x0_.id";

		assertEquals(
				"select distinct TOP(?) product2x0_.id as id0_, product2x0_.description as descript2_0_ " +
						"from Product2 product2x0_ order by product2x0_.id",
				dialect.getLimitHandler().processSql( distinctQuery, toRowSelection( 0, 5 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-7781")
	public void testGetLimitStringWithCastOperator() {
		final String query = "select cast(lc302_doku6_.redniBrojStavke as varchar(255)) as col_0_0_, lc302_doku6_.dokumentiID as col_1_0_ " +
				"from LC302_Dokumenti lc302_doku6_ order by lc302_doku6_.dokumentiID DESC";

		assertEquals(
				"WITH query AS (SELECT inner_query.*, ROW_NUMBER() OVER (ORDER BY CURRENT_TIMESTAMP) as __hibernate_row_nr__ FROM ( " +
						"select TOP(?) cast(lc302_doku6_.redniBrojStavke as varchar(255)) as col_0_0_, lc302_doku6_.dokumentiID as col_1_0_ " +
						"from LC302_Dokumenti lc302_doku6_ order by lc302_doku6_.dokumentiID DESC ) inner_query ) " +
						"SELECT col_0_0_, col_1_0_ FROM query WHERE __hibernate_row_nr__ >= ? AND __hibernate_row_nr__ < ?",
				dialect.getLimitHandler().processSql( query, toRowSelection( 1, 3 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8007")
	public void testGetLimitStringSelectingMultipleColumnsFromSeveralTables() {
		final String query = "select t1.*, t2.* from tab1 t1, tab2 t2 where t1.ref = t2.ref order by t1.id desc";

		assertEquals(
				"WITH query AS (SELECT inner_query.*, ROW_NUMBER() OVER (ORDER BY CURRENT_TIMESTAMP) as __hibernate_row_nr__ FROM ( " +
						"select TOP(?) t1.*, t2.* from tab1 t1, tab2 t2 where t1.ref = t2.ref order by t1.id desc ) inner_query ) " +
						"SELECT * FROM query WHERE __hibernate_row_nr__ >= ? AND __hibernate_row_nr__ < ?",
				dialect.getLimitHandler().processSql( query, toRowSelection( 1, 3 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8007")
	public void testGetLimitStringSelectingAllColumns() {
		final String query = "select * from tab1 t1, tab2 t2 where t1.ref = t2.ref order by t1.id desc";

		assertEquals(
				"WITH query AS (SELECT inner_query.*, ROW_NUMBER() OVER (ORDER BY CURRENT_TIMESTAMP) as __hibernate_row_nr__ FROM ( " +
						"select TOP(?) * from tab1 t1, tab2 t2 where t1.ref = t2.ref order by t1.id desc ) inner_query ) " +
						"SELECT * FROM query WHERE __hibernate_row_nr__ >= ? AND __hibernate_row_nr__ < ?",
				dialect.getLimitHandler().processSql( query, toRowSelection( 1, 3 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9635")
	public void testAppendLockHintReadPastLocking() {
		final String expectedLockHint = "tab1 with (updlock, rowlock, readpast)";

		LockOptions lockOptions = new LockOptions( LockMode.UPGRADE_SKIPLOCKED );
		String lockHint = dialect.appendLockHint( lockOptions, "tab1" );

		assertEquals( expectedLockHint, lockHint );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9635")
	public void testAppendLockHintReadPastLockingNoTimeOut() {
		final String expectedLockHint = "tab1 with (updlock, rowlock, readpast, nowait)";

		LockOptions lockOptions = new LockOptions( LockMode.UPGRADE_SKIPLOCKED );
		lockOptions.setTimeOut( LockOptions.NO_WAIT );
		String lockHint = dialect.appendLockHint( lockOptions, "tab1" );

		assertEquals( expectedLockHint, lockHint );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9635")
	public void testAppendLockHintPessimisticRead() {
		final String expectedLockHint = "tab1 with (holdlock, rowlock)";

		LockOptions lockOptions = new LockOptions( LockMode.PESSIMISTIC_READ );
		String lockHint = dialect.appendLockHint( lockOptions, "tab1" );

		assertEquals( expectedLockHint, lockHint );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9635")
	public void testAppendLockHintPessimisticReadNoTimeOut() {
		final String expectedLockHint = "tab1 with (holdlock, rowlock, nowait)";

		LockOptions lockOptions = new LockOptions( LockMode.PESSIMISTIC_READ );
		lockOptions.setTimeOut( LockOptions.NO_WAIT );
		String lockHint = dialect.appendLockHint( lockOptions, "tab1" );

		assertEquals( expectedLockHint, lockHint );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9635")
	public void testAppendLockHintWrite() {
		final String expectedLockHint = "tab1 with (updlock, rowlock)";

		LockOptions lockOptions = new LockOptions( LockMode.WRITE );
		String lockHint = dialect.appendLockHint( lockOptions, "tab1" );

		assertEquals( expectedLockHint, lockHint );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9635")
	public void testAppendLockHintWriteWithNoTimeOut() {
		final String expectedLockHint = "tab1 with (updlock, rowlock, nowait)";

		LockOptions lockOptions = new LockOptions( LockMode.WRITE );
		lockOptions.setTimeOut( LockOptions.NO_WAIT );

		String lockHint = dialect.appendLockHint( lockOptions, "tab1" );

		assertEquals( expectedLockHint, lockHint );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9635")
	public void testAppendLockHintUpgradeNoWait() {
		final String expectedLockHint = "tab1 with (updlock, rowlock, nowait)";

		LockOptions lockOptions = new LockOptions( LockMode.UPGRADE_NOWAIT );
		String lockHint = dialect.appendLockHint( lockOptions, "tab1" );

		assertEquals( expectedLockHint, lockHint );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9635")
	public void testAppendLockHintUpgradeNoWaitNoTimeout() {
		final String expectedLockHint = "tab1 with (updlock, rowlock, nowait)";

		LockOptions lockOptions = new LockOptions( LockMode.UPGRADE_NOWAIT );
		lockOptions.setTimeOut( LockOptions.NO_WAIT );
		String lockHint = dialect.appendLockHint( lockOptions, "tab1" );

		assertEquals( expectedLockHint, lockHint );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9635")
	public void testAppendLockHintUpgrade() {
		final String expectedLockHint = "tab1 with (updlock, rowlock)";

		LockOptions lockOptions = new LockOptions( LockMode.UPGRADE );
		String lockHint = dialect.appendLockHint( lockOptions, "tab1" );

		assertEquals( expectedLockHint, lockHint );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9635")
	public void testAppendLockHintUpgradeNoTimeout() {
		final String expectedLockHint = "tab1 with (updlock, rowlock, nowait)";

		LockOptions lockOptions = new LockOptions( LockMode.UPGRADE );
		lockOptions.setTimeOut( LockOptions.NO_WAIT );
		String lockHint = dialect.appendLockHint( lockOptions, "tab1" );

		assertEquals( expectedLockHint, lockHint );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9635")
	public void testAppendLockHintPessimisticWrite() {
		final String expectedLockHint = "tab1 with (updlock, rowlock)";

		LockOptions lockOptions = new LockOptions( LockMode.UPGRADE );
		String lockHint = dialect.appendLockHint( lockOptions, "tab1" );

		assertEquals( expectedLockHint, lockHint );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9635")
	public void testAppendLockHintPessimisticWriteNoTimeOut() {
		final String expectedLockHint = "tab1 with (updlock, rowlock, nowait)";

		LockOptions lockOptions = new LockOptions( LockMode.UPGRADE );
		lockOptions.setTimeOut( LockOptions.NO_WAIT );
		String lockHint = dialect.appendLockHint( lockOptions, "tab1" );

		assertEquals( expectedLockHint, lockHint );
	}

	private RowSelection toRowSelection(int firstRow, int maxRows) {
		RowSelection selection = new RowSelection();
		selection.setFirstRow( firstRow );
		selection.setMaxRows( maxRows );
		return selection;
	}
}
