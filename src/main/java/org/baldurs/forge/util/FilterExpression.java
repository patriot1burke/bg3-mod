package org.baldurs.forge.util;

import dev.langchain4j.store.embedding.filter.Filter;

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
