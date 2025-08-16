package it.com.atlassian.jira.plugins.slack.pageobjects;

import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.elements.PageElement;
import jakarta.inject.Inject;
import org.openqa.selenium.By;

import java.util.ArrayList;
import java.util.List;

public class MappingTable {
    @Inject
    private PageBinder pageBinder;
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
