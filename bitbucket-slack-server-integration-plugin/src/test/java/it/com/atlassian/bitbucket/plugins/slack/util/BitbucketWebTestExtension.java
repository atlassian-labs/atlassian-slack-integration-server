package it.com.atlassian.bitbucket.plugins.slack.util;

import com.atlassian.bitbucket.test.DefaultFuncTestData;
import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.ProductInstance;
import com.atlassian.pageobjects.TestedProductFactory;
import com.atlassian.webdriver.bitbucket.BitbucketTestedProduct;
import com.atlassian.webdriver.bitbucket.page.BitbucketLegacyLoginPage;
import com.atlassian.webdriver.bitbucket.page.BitbucketLoginPage;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.function.Supplier;

public class BitbucketWebTestExtension implements BeforeAllCallback {
    private final Supplier<ProductInstance> productInstanceSupplier;
    private BitbucketTestedProduct product;

    public BitbucketWebTestExtension(final Supplier<ProductInstance> productInstanceSupplier) {
        this.productInstanceSupplier = productInstanceSupplier;
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        if (product == null) {
            product = TestedProductFactory.create(BitbucketTestedProduct.class, productInstanceSupplier.get(), null);
        }
    }

    public BitbucketTestedProduct get() {
        return product;
    }

    public <P extends Page> P loginAsAdmin(Class<P> page, Object... args) {
        return loginAs(DefaultFuncTestData.getAdminUser(), DefaultFuncTestData.getAdminPassword(), page, args);
    }

    public <P extends Page> P loginAsRegularUser(Class<P> page, Object... args) {
        return loginAs(DefaultFuncTestData.getRegularUser(), DefaultFuncTestData.getRegularUserPassword(), page, args);
    }

    public <P extends Page> P anonymous(Class<P> page, Object... args) {
        return product.visit(page, args);
    }

    public <P extends Page> P loginAs(String username, String password, Class<P> page, Object... args) {
        return product.visit(BitbucketLegacyLoginPage.class).login(username, password, page, args);
    }
}
