package it.com.atlassian.confluence.plugins.slack.pageobjects;

import com.atlassian.confluence.webdriver.pageobjects.component.ConfluenceAbstractPageComponent;
import com.atlassian.pageobjects.elements.PageElement;
import org.openqa.selenium.By;

import java.util.ArrayList;
import java.util.List;

public class MappingTable extends ConfluenceAbstractPageComponent {
    private final PageElement container;

    public MappingTable(PageElement container) {
        this.container = container;
    }

    public List<MappingRow> getTableRows() {
        Iterable<PageElement> tbodys = container.findAll(By.tagName("tbody"));
        List<MappingRow> rows = new ArrayList<>();

        for (PageElement tbody : tbodys) {
            rows.add(pageBinder.bind(MappingRow.class, tbody));
        }

        return rows;
    }

    public MappingRow getExpandedRow() {
        return pageBinder.bind(MappingRow.class, container.find(By.cssSelector("tbody.edit-active")));
    }
}
