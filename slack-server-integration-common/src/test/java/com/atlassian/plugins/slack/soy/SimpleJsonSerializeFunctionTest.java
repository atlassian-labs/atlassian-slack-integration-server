package com.atlassian.plugins.slack.soy;

import com.atlassian.sal.api.ApplicationProperties;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyString;

public class SimpleJsonSerializeFunctionTest {
    @Mock
    private ApplicationProperties applicationProperties;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private SimpleJsonSerializeFunction target;

    @Test
    public void apply_shouldReturnExpectedValue() {
        String result = target.apply("a");
        assertThat(result, is("\"a\""));
    }

    @Test
    public void apply_shouldReturnEmptyOnError() {
        String result = target.apply(new Object());
        assertThat(result, isEmptyString());
    }

    @Test
    public void getters() {
        assertThat(target.getName(), is("simpleJsonSerialize"));
        assertThat(target.validArgSizes(), contains(1));
    }
}
