package com.atlassian.confluence.plugins.slack.util;

import net.sf.hibernate.Session;
import net.sf.hibernate.SessionFactory;
import net.sf.hibernate.dialect.Dialect;
import net.sf.hibernate.dialect.PostgreSQLDialect;
import net.sf.hibernate.engine.SessionFactoryImplementor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HibernateUtilTest {
    @Mock
    private Session session;
    @Mock
    private SessionFactoryImplementor sessionFactory;
    @Mock
    private SessionFactory unknownSessionFactory;
    @Mock
    private PostgreSQLDialect dialect;
    @Mock
    private Dialect unknownDialect;

    @Test
    public void getDialect_shouldReturnExpectedValue() {
        when(session.getSessionFactory()).thenReturn(sessionFactory);
        when(sessionFactory.getDialect()).thenReturn(dialect);

        HibernateUtil.SQLDialect result = HibernateUtil.getDialect(session);

        assertThat(result, is(HibernateUtil.SQLDialect.Postgres));
    }

    @Test
    public void getDialect_shouldReturnExpectedValueWhenDialectIsUnknown() {
        when(session.getSessionFactory()).thenReturn(sessionFactory);
        when(sessionFactory.getDialect()).thenReturn(unknownDialect);

        HibernateUtil.SQLDialect result = HibernateUtil.getDialect(session);

        assertThat(result, is(HibernateUtil.SQLDialect.Unknown));
    }

    @Test
    public void getDialect_shouldReturnExpectedValueWhenSessionIsNotSessionFactoryImplementor() {
        when(session.getSessionFactory()).thenReturn(unknownSessionFactory);

        HibernateUtil.SQLDialect result = HibernateUtil.getDialect(session);

        assertThat(result, is(HibernateUtil.SQLDialect.Unknown));
    }
}
