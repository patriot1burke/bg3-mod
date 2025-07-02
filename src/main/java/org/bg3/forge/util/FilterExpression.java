package org.bg3.forge.util;

import java.util.Collection;
import java.util.UUID;

import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import dev.langchain4j.store.embedding.filter.logical.And;
import dev.langchain4j.store.embedding.filter.logical.Or;

public class FilterExpression {
    Filter filter;

    public void and(Filter other) {
        if (filter == null) {
            filter = other;
        } else {
            filter = filter.and(other);
        }
    }

    public void and(FilterExpression other) {
        if (filter == null) {
            filter = other.filter;
        } else {
            filter = filter.and(other.filter);
        }
    }

    public void or(Filter other) {
        if (filter == null) {
            filter = other;
        } else {
            filter = filter.or(other);
        }
    }

    public void or(FilterExpression other) {
        if (filter == null) {
            filter = other.filter;
        } else {
            filter = filter.or(other.filter);
        }
    }
    public Filter filter() {
        return filter;
    }
}
