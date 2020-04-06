package com.zte.sputnik.util;

import java.util.Collection;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class StreamBuilder {
    public static <T> Stream<T> build(Collection<T> seed, Function<T, Collection<T>> next) {
        return build(seed, next, true);
    }

    public static <T> Stream<T> build(Collection<T> seed, Function<T, Collection<T>> next, boolean includeSeed) {
        SimpleGenerator<T> generator = new SimpleGenerator<>(seed, next, includeSeed);
        int characteristics = Spliterator.ORDERED | Spliterator.IMMUTABLE;
        Spliterator<Collection<T>> spliterator = Spliterators.spliteratorUnknownSize(generator, characteristics);
        Stream<Collection<T>> stream = StreamSupport.stream(spliterator, false);
        return stream.flatMap(Collection::stream);
    }
}
