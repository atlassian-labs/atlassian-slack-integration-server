package com.atlassian.confluence.plugins.slack.spacetochannel.service;

import com.atlassian.confluence.plugins.slack.spacetochannel.model.SpaceResult;
import com.atlassian.confluence.security.SpacePermission;
import com.atlassian.confluence.spaces.SpaceStatus;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.sal.api.user.UserKey;
import com.atlassian.sal.api.user.UserManager;
import io.atlassian.fugue.Either;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Query;
import net.sf.hibernate.Session;
import net.sf.hibernate.dialect.PostgreSQLDialect;
import net.sf.hibernate.engine.SessionFactoryImplementor;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DefaultSpacesWithAdminPermissionProviderTest {
    private static final String USER = "USR";
    private static final UserKey userKey = new UserKey(USER);

    @Mock
    private UserAccessor userAccessor;
    @Mock
    private UserManager userManager;
    @Mock
    private ConfluenceUser confluenceUser;
    @Mock
    private Session session;
    @Mock
    private Query query;
    @Mock
    private SessionFactoryImplementor sessionFactory;
    @Mock
    private PostgreSQLDialect dialect;

    @InjectMocks
    private DefaultSpacesWithAdminPermissionProvider target;

    @Test
    public void findSpacesMatchingName_shouldReturnExpectedValueForAdminQuery() throws HibernateException {
        when(sessionFactory.openSession()).thenReturn(session);
        when(confluenceUser.getKey()).thenReturn(userKey);
        when(userManager.isAdmin(userKey)).thenReturn(true);
        when(session.getSessionFactory()).thenReturn(sessionFactory);
        when(sessionFactory.getDialect()).thenReturn(dialect);
        when(session.createQuery(DefaultSpacesWithAdminPermissionProvider.QUERY_ADMIN
                + DefaultSpacesWithAdminPermissionProvider.QUERY_ORDER_LENGTH)).thenReturn(query);
        when(query.list()).thenReturn(Collections.singletonList(new Object[]{"name", "key"}));

        Either<Exception, List<SpaceResult>> result = target
                .findSpacesMatchingName("n", confluenceUser, 10);

        verify(query).setParameter("name", "n%");
        verify(query).setParameter("status", SpaceStatus.CURRENT.toString());
        verify(query).setMaxResults(10);

        assertThat(result.isRight(), is(true));
        assertThat(result.right().get(), hasSize(1));
        assertThat(result.right().get().get(0).getKey(), is("key"));
        assertThat(result.right().get().get(0).getName(), is("name"));
    }

    @Test
    public void findSpacesMatchingName_shouldReturnExpectedValueForNonAdminQueryWithGroups() throws HibernateException {
        when(sessionFactory.openSession()).thenReturn(session);
        when(confluenceUser.getKey()).thenReturn(userKey);
        when(confluenceUser.getName()).thenReturn(USER);
        when(userManager.isAdmin(userKey)).thenReturn(false);
        when(userAccessor.getGroupNamesForUserName(USER)).thenReturn(Arrays.asList("PERM", " "));
        when(session.getSessionFactory()).thenReturn(sessionFactory);
        when(sessionFactory.getDialect()).thenReturn(dialect);
        when(session.createQuery(DefaultSpacesWithAdminPermissionProvider.QUERY_WITH_GROUPS
                + DefaultSpacesWithAdminPermissionProvider.QUERY_ORDER_LENGTH)).thenReturn(query);
        when(query.list()).thenReturn(Collections.singletonList(new Object[]{"name", "key"}));

        Either<Exception, List<SpaceResult>> result = target
                .findSpacesMatchingName("n", confluenceUser, 10);

        verify(query).setParameterList(eq("groups"), (Collection) argThat(o -> Matchers.contains("PERM").matches(o)));
        verify(query).setParameter("permission", SpacePermission.ADMINISTER_SPACE_PERMISSION);
        verify(query).setParameter("user", confluenceUser);
        verify(query).setParameter("name", "n%");
        verify(query).setParameter("status", SpaceStatus.CURRENT.toString());
        verify(query).setMaxResults(10);

        assertThat(result.isRight(), is(true));
        assertThat(result.right().get(), hasSize(1));
        assertThat(result.right().get().get(0).getKey(), is("key"));
        assertThat(result.right().get().get(0).getName(), is("name"));
    }

    @Test
    public void findSpacesMatchingName_shouldReturnExpectedValueForNonAdminQueryWithoutGroups() throws HibernateException {
        when(sessionFactory.openSession()).thenReturn(session);
        when(confluenceUser.getKey()).thenReturn(userKey);
        when(confluenceUser.getName()).thenReturn(USER);
        when(userManager.isAdmin(userKey)).thenReturn(false);
        when(userAccessor.getGroupNamesForUserName(USER)).thenReturn(Collections.emptyList());
        when(session.getSessionFactory()).thenReturn(sessionFactory);
        when(sessionFactory.getDialect()).thenReturn(dialect);
        when(session.createQuery(DefaultSpacesWithAdminPermissionProvider.QUERY_WITH_NO_GROUPS
                + DefaultSpacesWithAdminPermissionProvider.QUERY_ORDER_LENGTH)).thenReturn(query);
        when(query.list()).thenReturn(Collections.singletonList(new Object[]{"name", "key"}));

        Either<Exception, List<SpaceResult>> result = target
                .findSpacesMatchingName("n", confluenceUser, 10);

        verify(query).setParameter("permission", SpacePermission.ADMINISTER_SPACE_PERMISSION);
        verify(query).setParameter("user", confluenceUser);
        verify(query).setParameter("name", "n%");
        verify(query).setParameter("status", SpaceStatus.CURRENT.toString());
        verify(query).setMaxResults(10);

        assertThat(result.isRight(), is(true));
        assertThat(result.right().get(), hasSize(1));
        assertThat(result.right().get().get(0).getKey(), is("key"));
        assertThat(result.right().get().get(0).getName(), is("name"));
    }
}
