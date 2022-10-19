package com.atlassian.plugins.slack.api.client.jslack;

import com.github.seratch.jslack.api.model.block.LayoutBlock;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Some BlockKit blocks aren't supported by jslack 1.5.6. This class is a generic representation for those blocks.
 * At the moment aren't interested in fields of that blocks, so the same class may be used for all the blocks.
 */
@Data
@NoArgsConstructor
public class UnsupportedLayoutBlock implements LayoutBlock {
    public static final String TYPE = "unsupported_block";

    private String type = TYPE;
}
