package it.com.atlassian.bitbucket.plugins.slack.pageobjects;

import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.webdriver.bitbucket.element.AbstractElementPageObject;
import org.openqa.selenium.By;

import java.util.ArrayList;
import java.util.List;

public class MappingTable extends AbstractElementPageObject {
    public MappingTable(PageElement container) {
        super(container);
    }

    public List<MappingRow> getTableRows() {
        Iterable<PageElement> tbodys = findAll(By.tagName("tbody"));
        List<MappingRow> rows = new ArrayList<>();

        for (PageElement tbody : tbodys) {
            rows.add(pageBinder.bind(MappingRow.class, tbody));
        }

        return rows;
    }

    public MappingRow getExpandedRow() {
        return pageBinder.bind(MappingRow.class, find(By.cssSelector("tbody.expanded")));
    }
}
