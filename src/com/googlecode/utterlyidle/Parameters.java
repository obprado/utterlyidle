package com.googlecode.utterlyidle;

import com.googlecode.totallylazy.functions.Function1;
import com.googlecode.totallylazy.functions.Function2;
import com.googlecode.totallylazy.functions.Callables;
import com.googlecode.totallylazy.First;
import com.googlecode.totallylazy.Maps;
import com.googlecode.totallylazy.Option;
import com.googlecode.totallylazy.Pair;
import com.googlecode.totallylazy.predicates.Predicate;
import com.googlecode.totallylazy.Sequence;
import com.googlecode.totallylazy.collections.PersistentList;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.googlecode.totallylazy.Callers.call;
import static com.googlecode.totallylazy.Pair.pair;
import static com.googlecode.totallylazy.predicates.Predicates.by;
import static com.googlecode.totallylazy.Sequences.sequence;

public abstract class Parameters<K, V, Self extends Parameters<K, V, Self>> implements Iterable<Pair<K, V>> {
    private final Function1<K, Predicate<K>> predicate;
    protected final PersistentList<Pair<K, V>> values;

    protected Parameters(Function1<K, Predicate<K>> predicate, PersistentList<Pair<K, V>> values) {
        this.predicate = predicate;
        this.values = values;
    }

    protected abstract Self self(PersistentList<Pair<K, V>> values);

    public Self add(K name, V value) {
        return self(values.append(pair(name, value)));
    }

    public Self joinTo(Self self) {
        return self(values.joinTo(self.values));
    }

    public Self remove(K name) {
        return self(values.deleteAll(filterByKey(name)));
    }

    public Self replace(K name, V value) {
        return remove(name).add(name, value);
    }

    public int size() {
        return values.size();
    }

    public V getValue(K key) {
        return valueOption(key).getOrNull();
    }

    public Option<V> valueOption(K key) {
        return filterByKey(key).headOption().map(Callables.<V>second());
    }

    public Sequence<V> getValues(K key) {
        return filterByKey(key).map(Callables.<V>second());
    }

    public boolean contains(K key) {
        return !filterByKey(key).headOption().isEmpty();
    }

    public Iterator<Pair<K, V>> iterator() {
        return values.iterator();
    }

    private Sequence<Pair<K, V>> filterByKey(K key) {
        Predicate<First<K>> predicate = by(Callables.<K>first(), call(this.predicate, key));
        return filter(predicate).realise();
    }

    public Sequence<Pair<K, V>> filter(final Predicate<First<K>> predicate) {
        return sequence(values).filter(predicate);
    }

    public static <K, V, Self extends Parameters<K, V, Self>> Function2<Self, Pair<K, V>, Self> pairIntoParameters() {
        return (result, pair) -> result.add(pair.first(), pair.second());
    }

    @Override
    public int hashCode() {
        return size();
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(Object other) {
        if (other instanceof Parameters) {
            final Self parameters = (Self) other;

            if (size() != parameters.size()) return false;

            return sequence(this).zip(parameters).forAll(pairsMatch());
        }
        return false;
    }

    private Predicate<Pair<Pair<K, V>, Pair<K, V>>> pairsMatch() {
        return pair -> {
            Pair<K, V> first = pair.first();
            Pair<K, V> second = pair.second();

            Predicate<K> predicate1 = call(Parameters.this.predicate, first.first());
            return predicate1.matches(second.first()) && first.second().equals(second.second());
        };
    }

    @Override
    public String toString() {
        return sequence(values).toString();
    }

    public Map<K, List<V>> toMap() {
        return Maps.multiMap(this);
    }
}
