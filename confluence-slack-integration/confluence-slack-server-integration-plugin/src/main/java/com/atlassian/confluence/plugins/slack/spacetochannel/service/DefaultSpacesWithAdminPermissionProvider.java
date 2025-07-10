package com.atlassian.confluence.plugins.slack.spacetochannel.service;

import com.atlassian.confluence.persistence.JpaQueryFactory;
import com.atlassian.confluence.plugins.slack.spacetochannel.model.SpaceResult;
import com.atlassian.confluence.security.SpacePermission;
import com.atlassian.confluence.spaces.SpaceStatus;
import com.atlassian.confluence.status.service.SystemInformationService;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.sal.api.user.UserManager;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import io.atlassian.fugue.Either;
import jakarta.persistence.Query;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

import static org.hibernate.jpa.QueryHints.HINT_CACHEABLE;

/**
 * A class which is able to provide a list of spaces for which the a user is a space admin. This class performs its
 * query on the database, and thus is very quick.
 */
@Component
public class DefaultSpacesWithAdminPermissionProvider implements SpacesWithAdminPermissionProvider {

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

    private final JpaQueryFactory jpaQueryFactory;
    private final UserAccessor userAccessor;
    private final UserManager userManager;
    private final SystemInformationService systemInformationService;

    @Autowired
    public DefaultSpacesWithAdminPermissionProvider(
            final JpaQueryFactory jpaQueryFactory,
            final SystemInformationService systemInformationService,
            final UserAccessor userAccessor,
            @Qualifier("salUserManager") final UserManager userManager) {
        this.userManager = userManager;
        this.jpaQueryFactory = jpaQueryFactory;
        this.systemInformationService = systemInformationService;
        this.userAccessor = userAccessor;
    }

    @Override
    public Either<Exception, List<SpaceResult>> findSpacesMatchingName(String name, ConfluenceUser user, int maxResults) {
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(user);
        Preconditions.checkArgument(maxResults > 0);

        Query query;

        if (userManager.isAdmin(user.getKey())) {
            query = jpaQueryFactory.createQuery(QUERY_ADMIN + queryOrderBy());
        } else {
            List<String> userGroups = userAccessor.getGroupNamesForUserName(user.getName());

            if (userGroups.isEmpty()) {
                query = jpaQueryFactory.createQuery(QUERY_WITH_NO_GROUPS + queryOrderBy());
            } else {
                query = jpaQueryFactory.createQuery(QUERY_WITH_GROUPS + queryOrderBy());
                query.setParameter("groups", filterBlanks(userGroups));
            }

            query.setParameter("permission", SpacePermission.ADMINISTER_SPACE_PERMISSION);
            query.setParameter("user", user);
        }

        query.setParameter("name", name + "%");
        query.setParameter("status", SpaceStatus.CURRENT.toString());
        query.setHint(HINT_CACHEABLE, false);
        query.setMaxResults(maxResults);

        //noinspection unchecked
        return Either.right(((List<Object[]>) query.getResultList()).stream()
                .map(input -> {
                    Preconditions.checkNotNull(input);
                    Preconditions.checkElementIndex(1, input.length);

                    return new SpaceResult((String) input[0], (String) input[1]);
                })
                .collect(Collectors.toList())
        );
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

    /**
     * Reference for dialect names :
     * https://docs.jboss.org/hibernate/stable/annotations/api/org/hibernate/dialect/package-summary.html
     */
    private String queryOrderBy() {
        String dialect = systemInformationService.getDatabaseInfo().getDialect();
        if (dialect != null && dialect.toLowerCase().contains("sqlserver")) {
            return QUERY_ORDER_LENGTH_MSSQL;
        } else {
            return QUERY_ORDER_LENGTH;
        }
    }
}
