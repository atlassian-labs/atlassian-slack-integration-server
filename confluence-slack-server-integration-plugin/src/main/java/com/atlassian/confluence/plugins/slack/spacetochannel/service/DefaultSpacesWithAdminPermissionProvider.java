package com.atlassian.confluence.plugins.slack.spacetochannel.service;

import com.atlassian.confluence.plugins.slack.spacetochannel.model.SpaceResult;
import com.atlassian.confluence.plugins.slack.util.HibernateUtil;
import com.atlassian.confluence.security.SpacePermission;
import com.atlassian.confluence.spaces.SpaceStatus;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.sal.api.user.UserManager;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import io.atlassian.fugue.Either;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Query;
import net.sf.hibernate.Session;
import net.sf.hibernate.SessionFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A class which is able to provide a list of spaces for which the a user is a space admin. This class performs its
 * query on the database, and thus is very quick.
 */
@Component
public class DefaultSpacesWithAdminPermissionProvider implements SpacesWithAdminPermissionProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSpacesWithAdminPermissionProvider.class);

    @VisibleForTesting
    static final String QUERY_WITH_GROUPS =
            "select space.name, space.key"
                    + " from SpacePermission as perm"
                    + " inner join perm.space as space"
                    + " where (perm.userSubject = :user or perm.group in (:groups) or (perm.userSubject is null and perm.group is null))"
                    + " and (perm.type = :permission)"
                    + " and space.spaceStatus = :status"
                    + " and lower(space.name) like lower(:name)";

    @VisibleForTesting
    static final String QUERY_WITH_NO_GROUPS =
            "select space.name, space.key"
                    + " from SpacePermission as perm"
                    + " inner join perm.space as space"
                    + " where perm.type = :permission and perm.group is null and (perm.userSubject = :user or perm.userSubject is null)"
                    + " and space.spaceStatus = :status"
                    + " and lower(space.name) like lower(:name)";

    @VisibleForTesting
    static final String QUERY_ADMIN =
            "select space.name, space.key"
                    + " from Space as space"
                    + " where space.spaceStatus = :status"
                    + " and lower(space.name) like lower(:name)";

    @VisibleForTesting
    static final String QUERY_ORDER_LENGTH = " order by length(space.name) asc";

    private static final String QUERY_ORDER_LENGTH_MSSQL = " order by len(space.name) asc";

    private final SessionFactory sessionFactory;
    private final UserAccessor userAccessor;
    private final UserManager userManager;

    @Autowired
    public DefaultSpacesWithAdminPermissionProvider(
            final SessionFactory sessionFactory,
            final UserAccessor userAccessor,
            @Qualifier("salUserManager") final UserManager userManager) {
        this.userManager = userManager;
        this.sessionFactory = sessionFactory;
        this.userAccessor = userAccessor;
    }

    @Override
    public Either<Exception, List<SpaceResult>> findSpacesMatchingName(String name, ConfluenceUser user, int maxResults) {
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(user);
        Preconditions.checkArgument(maxResults > 0);

        try {
            final Session session = sessionFactory.openSession();
            Query query;

            if (userManager.isAdmin(user.getKey())) {
                query = session.createQuery(QUERY_ADMIN + queryOrderBy(session));
            } else {
                List<String> userGroups = userAccessor.getGroupNamesForUserName(user.getName());

                if (userGroups.isEmpty()) {
                    query = session.createQuery(QUERY_WITH_NO_GROUPS + queryOrderBy(session));
                } else {
                    query = session.createQuery(QUERY_WITH_GROUPS + queryOrderBy(session));
                    query.setParameterList("groups", filterBlanks(userGroups));
                }

                query.setParameter("permission", SpacePermission.ADMINISTER_SPACE_PERMISSION);
                query.setParameter("user", user);
            }

            query.setParameter("name", name + "%");
            query.setParameter("status", SpaceStatus.CURRENT.toString());
            query.setCacheable(false);
            query.setMaxResults(maxResults);

            //noinspection unchecked
            return Either.right(((List<Object[]>) query.list()).stream()
                    .map(input -> {
                        Preconditions.checkNotNull(input);
                        Preconditions.checkElementIndex(1, input.length);

                        return new SpaceResult((String) input[0], (String) input[1]);
                    })
                    .collect(Collectors.toList())
            );


        } catch (HibernateException hibernateException) {
            LOGGER.warn("Error encountered querying space list", hibernateException);
            return Either.left(hibernateException);
        }
    }

    private List<String> filterBlanks(List<String> userGroups) {
        final ImmutableList.Builder<String> builder = ImmutableList.builder();

        for (String userGroup : userGroups) {
            if (!StringUtils.isBlank(userGroup)) {
                builder.add(userGroup);
            }
        }

        return builder.build();
    }

    private String queryOrderBy(Session session) {
        final HibernateUtil.SQLDialect dialect = HibernateUtil.getDialect(session);
        LOGGER.debug("Using {} as dialect for space query");

        switch (dialect) {
            case MSSQL:
                return QUERY_ORDER_LENGTH_MSSQL;
            default:
                return QUERY_ORDER_LENGTH;
        }
    }
}
