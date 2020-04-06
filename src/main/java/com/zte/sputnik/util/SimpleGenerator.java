package com.zte.sputnik.util;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author zscgrhg
 */
public class SimpleGenerator<T> implements Iterator<Collection<T>> {

    Collection<T> seed;
    Function<T, Collection<T>> next;
    boolean start;

    public SimpleGenerator(Collection<T> seed, Function<T, Collection<T>> next) {
        this(seed, next, true);
    }

    public SimpleGenerator(Collection<T> seed, Function<T, Collection<T>> next, boolean start) {
        this.seed = seed;
        this.next = next;
        this.start = start;
    }

    @Override
    public boolean hasNext() {
        return !seed.isEmpty();
    }

    @Override
    public Collection<T> next() {
        if (!hasNext()) {
            throw new NoSuchElementException("No more elements!");
        }
        if (start) {
            return seed;
        }
        seed = seed.stream()
                .flatMap(t -> Optional.ofNullable(next.apply(t))
                        .map(Collection::stream)
                        .filter(Objects::nonNull)
                        .orElse(Stream.empty()))
                .collect(Collectors.toList());
        return seed;
    }
}
