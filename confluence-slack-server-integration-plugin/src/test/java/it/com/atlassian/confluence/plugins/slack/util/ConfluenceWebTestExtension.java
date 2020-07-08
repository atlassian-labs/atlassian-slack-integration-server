package it.com.atlassian.confluence.plugins.slack.util;

import com.atlassian.confluence.test.api.model.person.UserWithDetails;
import com.atlassian.confluence.webdriver.pageobjects.ConfluenceTestedProduct;
import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.ProductInstance;
import com.atlassian.pageobjects.TestedProductFactory;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.function.Supplier;

public class ConfluenceWebTestExtension implements BeforeAllCallback {
    private final Supplier<ProductInstance> productInstanceSupplier;
    private ConfluenceTestedProduct product;

    public ConfluenceWebTestExtension(final Supplier<ProductInstance> productInstanceSupplier) {
        this.productInstanceSupplier = productInstanceSupplier;
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        if (product == null) {
            product = TestedProductFactory.create(ConfluenceTestedProduct.class, productInstanceSupplier.get(), null);
        }
    }

    public ConfluenceTestedProduct get() {
        return product;
    }

    public <P extends Page> P loginAs(UserWithDetails user, Class<P> page, Object... args) {
        return product.login(user, page, args);
    }
}
