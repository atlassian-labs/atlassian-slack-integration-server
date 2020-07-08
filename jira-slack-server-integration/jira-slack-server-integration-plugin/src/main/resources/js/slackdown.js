/*! https://github.com/blockmar/slackdown by @blockmar | MIT license */
;(function (root, factory) {
    if (typeof define === 'function' && define.amd) {
        define([], factory);
    } else if (typeof exports === 'object') {
        module.exports = factory();
    } else {
        root.slackdown = factory();
    }
}(this, function () {

    var RE_ALPHANUMERIC = new RegExp('^\\w?$'),
        RE_TAG = new RegExp('<(.+?)>', 'g'),
        RE_BOLD = new RegExp('\\*([^\\*]+?)\\*', 'g'),
        RE_ITALIC = new RegExp('_([^_]+?)_', 'g'),
        RE_FIXED = new RegExp('`([^`]+?)`', 'g');

    var payloads = function(tag, start) {
        if(!start) {
            start = 0;
        }
        var length = tag.length;
        return pipeSplit(tag.substr(start, length - start));
    };

    var pipeSplit = function(payload) {
        return payload.split('|');
    };

    var tag = function(tag, attributes, payload) {
        if(!payload) {
            payload = attributes;
            attributes = {};
        }

        var html = "<".concat(tag);
        for (var attribute in attributes) {
            if (attributes.hasOwnProperty(attribute)) {
                html = html.concat(' ', attribute, '="', attributes[attribute], '"');
            }
        }
        return html.concat('>', payload, '</', tag, '>');
    };

    var matchTag = function(match, teamId) {
        var action = match[1].substr(0,1),
            p;

        switch(action) {
            case "!":
                return tag("span", { "class": "slack-cmd" }, payloads(match[1], 1)[0]);
            case "#":
                p = payloads(match[1], Math.max(match[1].indexOf("#") + 1, 0));
                if (teamId) {
                    return tag(
                        "a",
                        {
                            "class": "slack-channel slack-channel-link-url",
                            "data-team-id": teamId,
                            "data-channel-id": p[0]
                        },
                        (p.length === 1 ? p[0] : p[1]));
                }
                return tag("span", { "class": "slack-channel" }, (p.length === 1 ? p[0] : p[1]));
            case "@":
                p = payloads(match[1], Math.max(match[1].indexOf("@") + 1, 0));
                if (teamId) {
                    return tag(
                        "a",
                        {
                            "class": "slack-user slack-user-link-url",
                            "data-team-id": teamId,
                            "data-user-id": p[0]
                        },
                        (p.length === 1 ? p[0] : p[1]));
                }
                return tag("span", { "class": "slack-user slack-user-link-url" }, (p.length === 1 ? p[0] : p[1]));
            default:
                p = payloads(match[1]);
                return tag("a", { "href": p[0] }, (p.length === 1 ? p[0] : p[1]));
        }
    };

    var matchBold = function(match) {
        return safeMatch(match, tag("strong", payloads(match[1])), "*");
    };

    var matchItalic = function(match) {
        return safeMatch(match, tag("em", payloads(match[1])), "_");
    };

    var matchFixed = function(match) {
        return safeMatch(match, tag("code", payloads(match[1])));
    };

    var notAlphanumeric = function(input) {
        return !RE_ALPHANUMERIC.test(input);
    };

    var notRepeatedChar = function(trigger, input) {
        return !trigger || trigger !== input;
    };

    var safeMatch = function(match, tag, trigger) {
        var prefix_ok = match.index === 0;
        var postfix_ok = match.index === match.input.length - match[0].length;

        if(!prefix_ok) {
            var charAtLeft = match.input.substr(match.index - 1, 1);
            prefix_ok = notAlphanumeric(charAtLeft) && notRepeatedChar(trigger, charAtLeft);
        }

        if(!postfix_ok) {
            var charAtRight = match.input.substr(match.index + match[0].length, 1);
            postfix_ok = notAlphanumeric(charAtRight) && notRepeatedChar(trigger, charAtRight);
        }

        if(prefix_ok && postfix_ok) {
            return tag;
        }
        return false;
    };

    var publicParse = function (text, teamId) {

        if(typeof text === 'string') {
            var patterns = [
                {p: RE_TAG, cb: matchTag},
                {p: RE_BOLD, cb: matchBold},
                {p: RE_ITALIC, cb: matchItalic},
                {p: RE_FIXED, cb: matchFixed}
            ];

            for (var p = 0; p < patterns.length; p++) {

                var pattern = patterns[p],
                    original = text,
                    result;

                while ((result = pattern.p.exec(original)) !== null) {
                    var replace = pattern.cb(result, teamId);

                    if (replace) {
                        text = text.replace(result[0], replace);
                    }
                }
            }
        }

        return text;
    };

    return {
        parse: publicParse
    };

}));
