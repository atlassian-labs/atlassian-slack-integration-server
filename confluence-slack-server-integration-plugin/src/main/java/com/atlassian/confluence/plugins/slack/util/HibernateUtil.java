package com.atlassian.confluence.plugins.slack.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import net.sf.hibernate.Session;
import net.sf.hibernate.SessionFactory;
import net.sf.hibernate.dialect.Dialect;
import net.sf.hibernate.dialect.HSQLDialect;
import net.sf.hibernate.dialect.MySQLDialect;
import net.sf.hibernate.dialect.Oracle9Dialect;
import net.sf.hibernate.dialect.OracleDialect;
import net.sf.hibernate.dialect.OracleIntlDialect;
import net.sf.hibernate.dialect.PostgreSQLDialect;
import net.sf.hibernate.dialect.SQLServerDialect;
import net.sf.hibernate.engine.SessionFactoryImplementor;

import java.util.Map;

public class HibernateUtil {
    private final static Map<Class<? extends Dialect>, SQLDialect> HIBERNATE_TO_SQL_TEMPLATE_MAPPING =
            ImmutableMap.<Class<? extends Dialect>, SQLDialect>builder()
                    .put(HSQLDialect.class, SQLDialect.HSQL)
                    .put(MySQLDialect.class, SQLDialect.MySQl)
                    .put(PostgreSQLDialect.class, SQLDialect.Postgres)
                    .put(SQLServerDialect.class, SQLDialect.MSSQL)
                    .put(OracleDialect.class, SQLDialect.Oracle)
                    .put(Oracle9Dialect.class, SQLDialect.Oracle)
                    .put(OracleIntlDialect.class, SQLDialect.Oracle)
                    .build();

    public enum SQLDialect {
        HSQL,
        MSSQL,
        Postgres,
        MySQl,
        Oracle,
        Unknown
    }

    public static SQLDialect getDialect(final Session session) {
        Preconditions.checkNotNull(session);

        final Class<? extends Dialect> dialect = getHibernateDialect(session);
        if (dialect != null) {
            for (Map.Entry<Class<? extends Dialect>, SQLDialect> entry : HIBERNATE_TO_SQL_TEMPLATE_MAPPING.entrySet()) {
                if (entry.getKey().isAssignableFrom(dialect)) {
                    return entry.getValue();
                }
            }

        }

        return SQLDialect.Unknown;
    }

    private static Class<? extends Dialect> getHibernateDialect(final Session session) {
        final SessionFactory hibernateSessionFactory = session.getSessionFactory();
        if (hibernateSessionFactory instanceof SessionFactoryImplementor) {
            return ((SessionFactoryImplementor) hibernateSessionFactory).getDialect().getClass();
        } else {
            return null;
        }
    }
}
