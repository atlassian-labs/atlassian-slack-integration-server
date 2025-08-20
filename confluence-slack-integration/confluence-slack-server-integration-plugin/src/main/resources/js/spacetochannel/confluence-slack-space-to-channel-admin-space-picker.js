define('confluence/slack/space-to-channel/internal/admin-space-picker', [
    'jquery',
    'underscore'
],
/**
 *
 * Return a module that can be used to build a space selector that will return spaces for which the
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
            url: AJS.Confluence.getContextPath() + '/rest/slack/latest/spaces',
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
                    var spaces = _.map(data, function (match) {
                        return {
                            id: match.key,
                            text: match.name
                        }
                    });

                    return {
                        results: [
                            {
                                text: AJS.I18n.getText("confluence-ui-components.space-picker.search-results"),
                                children: spaces
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

        return function (query) {
            search(query);
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
                placeholder: AJS.I18n.getText("confluence-ui-components.space-picker.placeholder"),

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

                minimumInputLength: 0,
                minimumResultsForSearch: 0,
                multiple: opts.multiple === true,
                query: makeQueryFn(opts),
                // Show suggestions until you typed at least 2 characters, then search.
                minimumInputLength: 2,
                // Show search box even if there are no results
                minimumResultsForSearch: 0,
                allowClear: true,
            }, opts.select2Options);
        }
    };
});
