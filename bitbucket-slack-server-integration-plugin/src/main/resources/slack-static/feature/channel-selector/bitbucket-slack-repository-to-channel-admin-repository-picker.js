define('bitbucket/slack/repository-to-channel/internal/admin-repository-picker', [
    'jquery',
    'underscore'
],
/**
 * Return a module that can be used to build a repository selector that will return repositories for which the
 *  user is an administrator
 *
 * @param $ {jQuery}
 * @param _
 * @param AJS
 * @returns {{build:function()}}
 * @tainted window.Select2
 */
function(
     $,
     _
) {
    /**
     * Query our rest API for spaces
     * @returns {*}
     */
    var makeSearchFn = function () {
        return window.Select2.query.ajax({
            url: AJS.contextPath() + '/rest/slack/latest/repositories',
            data: function(term, page) {
                return {
                    name: term
                }
            },
            quietMillis: 250,
            results: function (data, page) {
                if (data.length < 1) {
                    // no search results
                    return { results: [] };
                } else {
                    // format results
                    var repositories = _.map(data, function (match) {
                        return {
                            id: match.id,
                            slug: match.slug,
                            projectKey: match.projectKey,
                            name: match.name,
                            projectName: match.projectName,
                            text: match.projectName + ' / ' + match.name
                        }
                    });

                    return {
                        results: [
                            {
                                text: AJS.I18n.getText("bitbucket-ui-components.repository-picker.search-results"),
                                children: repositories
                            }
                        ]
                    };
                }
            }
        });
    };

    /**
     * Make a query with an empty result set
     * @returns {*}
     */
    function makeEmptyQuery() {
        return window.Select2.query.local({
            results: []
        });
    }

    /**
     * Builds the query function that is passed to select2.
     *
     * @param opts
     * @returns {Function}
     */
    var makeQueryFn = function (opts) {
        var search = makeSearchFn();
        var emptyQuery = makeEmptyQuery();

        // Show suggestions until you typed at least 2 characters, then search.
        return function (query) {
            if (query.term.length >= 1) {
                search(query);
            } else {
                emptyQuery(query);
            }
        }
    };

    return {
        /**
         * Build a configuration object for a select2
         * @param opts
         * @returns {void|*}
         */
        build: function (opts) {
            return _.extend({
                placeholder: AJS.I18n.getText("bitbucket-ui-components.repository-picker.placeholder"),

                formatResult: function (result, label, query) {
                    // Add a tooltip to disclose full result text (as we truncate it to fit the drop down)
                    label.attr("title", result.text);
                    return $.fn.select2.defaults.formatResult.apply(this, arguments);
                },

                formatNoMatches: function (term) {
                    if (!term) {
                        return '';
                    }
                    return $.fn.select2.defaults.formatNoMatches.apply(this, arguments)
                },

                multiple: opts.multiple === true,
                query: makeQueryFn(opts)
            }, opts.select2Options);
        }
    };
});
