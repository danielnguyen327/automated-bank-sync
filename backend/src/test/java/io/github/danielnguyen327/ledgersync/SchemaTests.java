package io.github.danielnguyen327.ledgersync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SchemaTests {
  private static final String INSERT_TRANSACTION = """
      insert into bank_transaction
        (user_id, account_id, plaid_transaction_id, amount_cents, transaction_date, name)
      values (?, ?, 'txn-1', 433, date '2026-09-20', 'Starbucks')
      """;

  @Autowired
  private JdbcTemplate jdbc;

  private UUID userId;
  private UUID itemId;
  private UUID accountId;

  @BeforeEach
  void seedUserWithCard() {
    jdbc.update("delete from app_user");
    userId = jdbc.queryForObject(
      "insert into app_user (email, name) values ('test@example.com', 'Test User') returning id",
      UUID.class);
    itemId = jdbc.queryForObject("""
      insert into plaid_item (user_id, plaid_item_id, institution_name, access_token_encrypted)
      values (?, 'item-1', 'Test Bank', 'ciphertext') returning id
      """, UUID.class, userId);
    accountId = jdbc.queryForObject("""
      insert into financial_account (user_id, item_id, plaid_account_id, name, type)
      values (?, ?, 'account-1', 'Plaid Credit Card', 'credit')returning id
      """, UUID.class, userId, itemId);
  }

  @Test
  void migrationCreatesEveryTable() {
    List<String> tables = jdbc.queryForList("""
        select table_name from information_schema.tables
        where table_schema = 'public' and table_name <> 'flyway_schema_history'
        order by table_name
        """, String.class);
    assertThat(tables).containsExactly(
      "app_user", "balance_snapshot", "bank_transaction", "financial_account", "insight_set", "plaid_item", "sync_run");
  }

   @Test
    void deletingAUserDeletesAllOfTheirData() {
        jdbc.update(INSERT_TRANSACTION, userId, accountId);
        jdbc.update("""
            insert into balance_snapshot (user_id, account_id, snapshot_date, current_cents)
            values (?, ?, date '2026-09-20', 114620)
            """, userId, accountId);
        jdbc.update("insert into sync_run (user_id, item_id) values (?, ?)", userId, itemId);
        jdbc.update("""
            insert into insight_set (user_id, period_start, period_end, facts)
            values (?, date '2026-09-01', date '2026-09-30', '[]'::jsonb)
            """, userId);
        jdbc.update("delete from app_user where id = ?", userId);

        for (String table : List.of("plaid_item", "financial_account", "bank_transaction", "balance_snapshot", "insight_set", "sync_run")) {
          assertThat(jdbc.queryForObject("select count(*) from " + table, Integer.class))
          .as(table)
          .isZero();
        }
    }

    @Test
    void aPlaidTransactionIsStoredOnlyOnce() {
      jdbc.update(INSERT_TRANSACTION, userId, accountId);
      assertThatThrownBy(() -> jdbc.update(INSERT_TRANSACTION, userId, accountId))
      .isInstanceOf(DuplicateKeyException.class);
    }
}