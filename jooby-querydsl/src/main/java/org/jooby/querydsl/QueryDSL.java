/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby.querydsl;

import static java.util.Objects.requireNonNull;
import static javaslang.API.Case;
import static javaslang.API.Match;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.inject.Provider;
import javax.sql.DataSource;

import org.jooby.Env;
import org.jooby.jdbc.Jdbc;

import com.google.inject.Binder;
import com.querydsl.sql.Configuration;
import com.querydsl.sql.DB2Templates;
import com.querydsl.sql.FirebirdTemplates;
import com.querydsl.sql.H2Templates;
import com.querydsl.sql.HSQLDBTemplates;
import com.querydsl.sql.MySQLTemplates;
import com.querydsl.sql.OracleTemplates;
import com.querydsl.sql.PostgreSQLTemplates;
import com.querydsl.sql.SQLQueryFactory;
import com.querydsl.sql.SQLTemplates;
import com.querydsl.sql.SQLiteTemplates;
import com.typesafe.config.Config;

/**
 * <h1>queryDSL</h1>
 *
 * <p>
 * SQL abstraction provided by <a href="http://www.querydsl.com">QueryDSL</a> using plain JDBC
 * underneath.
 * </p>
 *
 * <p>
 * This module depends on {@link Jdbc} module, make sure you read the doc of the {@link Jdbc}
 * module before using this module.
 * </p>
 *
 * <h2>usage</h2>
 *
 * <pre>
 * import org.jooby.querydsl.QueryDSL;
 *
 * {
 *   use(new QueryDSL());
 *
 *   get("/my-api", req {@literal ->} {
 *     SQLQueryFactory queryFactory = req.require(SQLQueryFactory.class);
 *     // Do something with the database
 *     ...
 *   });
 * }
 * </pre>
 *
 * <h2>dialects</h2>
 * <p>
 * Dialect is detected automatically and usually you don't need to do anything. But if the default
 * dialect detector doesn't work and/or you have a custom dialect:
 * </p>
 *
 * <pre>
 * {
 *   use(new QueryDSL().with(new MyCustomTemplates());
 * }
 * </pre>
 *
 * <h2>multiple databases</h2>
 *
 * <pre>
 * import org.jooby.querydsl.QueryDSL;
 *
 * {
 *   use(new QueryDSL("db.main"));
 *   use(new QueryDSL("db.aux"));
 *
 *   get("/my-api", req {@literal ->} {
 *     SQLQueryFactory queryFactory = req.require("db.main", SQLQueryFactory.class);
 *     // Do something with the database
 *   });
 * }
 * </pre>
 *
 * <h2>advanced configuration</h2>
 * <p>
 * This module builds QueryDSL SQLQueryFactories on top of a default {@link Configuration} object, a
 * {@link SQLTemplates} instance and a {@link javax.sql.DataSource} from the {@link Jdbc} module.
 * </p>
 * <p>
 * Advanced configuration can be added by invoking the {@link #doWith(BiConsumer)} method, adding
 * your own settings to the configuration.
 * </p>
 *
 * <pre>
 * {
 *   use(new QueryDSL().doWith(conf {@literal ->} {
 *     conf.set(...);
 *   });
 * }
 * </pre>
 *
 * <h2>code generation</h2>
 * <p>
 * This module does not provide code generation for DB mapping classes. To learn how to create
 * QueryDSL mapping classes using Maven, please consult the
 * <a href="http://www.querydsl.com/static/querydsl/latest/reference/html_single/#d0e725">QueryDSL
 * documentation.</a>
 * </p>
 *
 * @since 1.0.0.CR
 * @author sjackel
 */
public class QueryDSL extends Jdbc {

  private BiConsumer<Configuration, Config> callback;

  private Function<String, SQLTemplates> templates = QueryDSL::toSQLTemplates;

  /**
   * Creates a new {@link QueryDSL} module
   *
   * @param name Database name
   */
  public QueryDSL(final String name) {
    super(name);
  }

  /**
   * Creates a new {@link QueryDSL} module
   */
  public QueryDSL() {
    this(DEFAULT_DB);
  }

  @Override
  public void configure(final Env env, final Config conf, final Binder binder) {
    super.configure(env, conf, binder);
    SQLTemplates templates = this.templates.apply(dbtype.orElse("unknown"));

    Configuration querydslconf = new Configuration(templates);

    if (callback != null) {
      callback.accept(querydslconf, conf);
    }
    DataSource ds = dataSource().get();
    Provider<SQLQueryFactory> sqfp = () -> new SQLQueryFactory(querydslconf, ds);

    keys(SQLTemplates.class, k -> binder.bind(k).toInstance(templates));
    keys(Configuration.class, k -> binder.bind(k).toInstance(querydslconf));
    keys(SQLQueryFactory.class, k -> binder.bind(k).toProvider(sqfp));
  }

  /**
   * Apply advanced configuration
   *
   * @param callback a configuration callback.
   * @return This module.
   */
  public QueryDSL doWith(final Consumer<Configuration> callback) {
    requireNonNull(callback, "Callback needs to be defined");
    return doWith((configuration, conf) -> callback.accept(configuration));
  }

  /**
   * Apply advanced configuration
   *
   * @param callback a configuration callback.
   * @return This module.
   */
  public QueryDSL doWith(final BiConsumer<Configuration, Config> callback) {
    this.callback = requireNonNull(callback, "Callback needs to be defined");
    return this;
  }

  public QueryDSL with(final SQLTemplates templates) {
    requireNonNull(templates, "SQL templates needs to be defined");
    this.templates = t -> templates;
    return this;
  }

  static SQLTemplates toSQLTemplates(final String type) {
    return Match(type).option(
        Case("db2", DB2Templates::new),
        Case("mysql", MySQLTemplates::new),
        Case("mariadb", MySQLTemplates::new),
        Case("h2", H2Templates::new),
        Case("hsqldb", HSQLDBTemplates::new),
        Case("pgsql", PostgreSQLTemplates::new),
        Case("postgresql", PostgreSQLTemplates::new),
        Case("sqlite", SQLiteTemplates::new),
        Case("oracle", OracleTemplates::new),
        Case("firebirdsql", FirebirdTemplates::new))
        .getOrElseThrow(() -> new IllegalStateException("Unsupported database: " + type));
  }
}
