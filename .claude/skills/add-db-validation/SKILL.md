---
name: add-db-validation
description: Query or validate a database (PostgreSQL/MySQL/MSSQL/Oracle/MongoDB) from a test using DBUtils/DatabaseClientFactory, or wire up a new DB vendor. Use when adding DB-backed assertions or a new database connection strategy.
---

# Adding a DB validation / new DB vendor

Location: `src/main/java/com/framework/db/`. Design: **Factory**
(`DatabaseClientFactory`) picks the implementation for a `DBType`; **Strategy**
(`strategy/IDBConnectionStrategy` + one impl per JDBC vendor) builds the vendor-specific
JDBC URL/driver; `DBUtils` is the **ThreadLocal facade** tests actually call.

There is no `db` test package or `testng.xml` block yet - if this is the first DB
test, create `src/test/java/com/framework/tests/db/` and add a `<test name="DB Tests">`
block (group `db`) to `testng.xml`, mirroring the `API Tests` block.

## Rules for a DB test

1. Extend `BaseTest`, tag `@Test(groups = {"db"})` so `BaseTest.isUiTest()` skips
   browser setup (same mechanism as `api`/`security`).

2. **Connect once per thread**, near the start of the test (or a `@BeforeMethod` in
   the test class):
   ```java
   DBUtils.connect(DBType.POSTGRESQL,
       ConfigReader.get("db.postgres.host"),
       ConfigReader.getInt("db.postgres.port", 5432),
       ConfigReader.get("db.postgres.database"),
       ConfigReader.get("db.postgres.user"),
       ConfigReader.get("db.postgres.password"));
   ```
   Never hardcode host/port/credentials - config keys already exist per vendor in
   `config.properties` (`db.postgres.*`, `db.mysql.*`, `db.mssql.*`, `db.oracle.*`,
   `db.mongodb.*`).

3. **Query / write**:
   - `DBUtils.getRecords(sql)` -> `List<Map<String,Object>>` (column name -> value,
     case-insensitive column lookup).
   - `DBUtils.update(sql)` -> affected row count.
   - MongoDB uses the same two methods but a different query string shape:
     `"collectionName|{filterJson}"` (empty filter = find/update/delete all) - see
     `IDatabaseClient` Javadoc.

4. **Assert**:
   - `DBUtils.assertColumnValue(record, columnName, expected)` (hard, or the
     `(records, rowIndex, columnName, expected)` overload) for a critical DB check.
   - `DBUtils.validateColumnValue(...)` (same overloads) returns a `boolean` when you
     want to branch or feed a `SoftAssertHelper` check instead.
   - `DBUtils.getColumnValue(...)` when you just need the raw value.

5. **Always `DBUtils.close()` when done** (e.g. in an `@AfterMethod` override or a
   `finally` block) - the client is `ThreadLocal`; unlike `DriverManager`, `BaseTest`
   does **not** close it for you automatically.

6. Never bypass `DBUtils`/`DatabaseClientFactory` by constructing a
   `JdbcDatabaseClient` or `MongoDatabaseClient` directly in a test.

## Template

```java
package com.framework.tests.db;

import com.framework.base.BaseTest;
import com.framework.config.ConfigReader;
import com.framework.db.DBType;
import com.framework.db.DBUtils;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

@Feature("User data integrity")
public class UserDbTest extends BaseTest {

    @Test(groups = {"db"})
    @Story("Newly created user is persisted with correct status")
    public void testUserStatusPersisted() {
        DBUtils.connect(DBType.POSTGRESQL,
                ConfigReader.get("db.postgres.host"),
                ConfigReader.getInt("db.postgres.port", 5432),
                ConfigReader.get("db.postgres.database"),
                ConfigReader.get("db.postgres.user"),
                ConfigReader.get("db.postgres.password"));

        List<Map<String, Object>> rows = DBUtils.getRecords("SELECT * FROM users WHERE email = 'demo@example.com'");
        DBUtils.assertColumnValue(rows, 0, "status", "active");
    }

    @AfterMethod(alwaysRun = true)
    public void closeDb() {
        DBUtils.close();
    }
}
```

## Adding a brand-new relational DB vendor

1. Add the JDBC driver dependency to `pom.xml` (most common ones are already there).
2. Add a `DBType` enum value.
3. Implement `IDBConnectionStrategy` (extend `AbstractJdbcConnectionStrategy`), e.g.:
   ```java
   public class MyVendorConnectionStrategy extends AbstractJdbcConnectionStrategy {
       @Override public String getDriverClassName() { return "com.myvendor.jdbc.Driver"; }
       @Override public String buildJdbcUrl(DBConfig config) {
           if (config.getConnectionString() != null && !config.getConnectionString().isBlank()) {
               return config.getConnectionString();
           }
           return "jdbc:myvendor://" + config.getHost() + ":" + config.getPort() + "/" + config.getDbName()
                   + buildExtraParamsSuffix(config, "?", "&");
       }
   }
   ```
4. Register the instance in `DatabaseClientFactory`'s static `JDBC_STRATEGIES` map.
5. Add `db.myvendor.*` config keys to `config.properties` (+ per-env overrides).

## Checklist

- [ ] `@Test(groups = {"db"}, ...)`; class registered in a `db` `testng.xml` block
- [ ] Connection params come from `ConfigReader`, not literals
- [ ] `DBUtils.close()` called (test-level `finally` or `@AfterMethod`)
- [ ] No direct instantiation of `JdbcDatabaseClient`/`MongoDatabaseClient` in test code
