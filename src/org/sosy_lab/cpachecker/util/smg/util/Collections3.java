// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.util;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Equivalence;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.google.common.collect.Ordering;
import com.google.common.collect.Streams;
import com.google.common.primitives.Chars;
import com.google.errorprone.annotations.Var;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Utility class similar to {@link Collections} and {@link Collections2}. */
public final class Collections3 {

  private Collections3() {}

  /**
   * Check whether all elements contained in the given iterable are equal to each other. For a
   * non-empty iterable, this is the same as {@code ImmutableSet.copyOf(iterable).size() == 1}, but
   * more efficient.
   *
   * @throws IllegalArgumentException if the iterable is empty
   */
  public static boolean allElementsEqual(Iterable<?> iterable) {
    return allElementsEqual(iterable.iterator());
  }
  /**
   * Check whether all elements contained in the given array are equal to each other. For a
   * non-empty array, this is the same as {@code ImmutableSet.copyOf(array).size() == 1}, but more
   * efficient.
   *
   * @throws IllegalArgumentException if the array is empty
   */
  public static boolean allElementsEqual(Object[] array) {
    return allElementsEqual(Iterators.forArray(array));
  }
  /**
   * Check whether all elements contained in the given stream are equal to each other. For a
   * non-empty stream, this is the same as {@code stream.distinct().count() == 1}, but more
   * efficient.
   *
   * @throws IllegalArgumentException if the stream is empty
   */
  public static boolean allElementsEqual(Stream<?> stream) {
    return allElementsEqual(stream.unordered().iterator());
  }

  private static boolean allElementsEqual(Iterator<?> it) {
    checkArgument(it.hasNext(), "Input was empty");
    Object o = it.next();
    while (it.hasNext()) {
      if (!Objects.equals(o, it.next())) {
        return false;
      }
    }
    return true;
  }

  /**
   * Apply a function to all elements in a collection and return an {@link ImmutableList} with the
   * results. This is an eager version of {@link Lists#transform(List, Function)} and {@link
   * Collections2#transform(Collection, Function)}.
   *
   * <p>This function is more efficient than code doing the same using {@link Stream} or {@link
   * com.google.common.collect.FluentIterable}.
   */
  public static <T1, T2> ImmutableList<T2> transformedImmutableListCopy(
      Collection<T1> input, Function<? super T1, T2> transformer) {
    return ImmutableList.copyOf(Collections2.transform(input, transformer));
  }

  /**
   * Apply a function to all elements in an array and return an {@link ImmutableList} with the
   * results.
   *
   * <p>This function is more efficient than code doing the same using {@link Stream} or {@link
   * com.google.common.collect.FluentIterable}.
   */
  public static <T1, T2> ImmutableList<T2> transformedImmutableListCopy(
      T1[] input, Function<? super T1, T2> transformer) {
    return ImmutableList.copyOf(Lists.transform(Arrays.asList(input), transformer));
  }

  /**
   * Apply a function to all elements in a collection and return an {@link ImmutableSet} with the
   * results. This is an eager version of {@link Collections2#transform(Collection, Function)}.
   */
  public static <T1, T2> ImmutableSet<T2> transformedImmutableSetCopy(
      Collection<T1> input, Function<? super T1, T2> transformer) {
    return ImmutableSet.copyOf(Collections2.transform(input, transformer));
  }

  /**
   * Provide a stream that consists of the result of applying the given function to each of a map's
   * entries, similarly to {@link com.google.common.collect.Streams#zip(Stream, Stream,
   * BiFunction)}.
   */
  public static <K, V, R> Stream<R> zipMapEntries(Map<K, V> map, BiFunction<K, V, R> func) {
    checkNotNull(func);
    return map.entrySet().stream().map(entry -> func.apply(entry.getKey(), entry.getValue()));
  }

  /**
   * Provide a stream that consists of the result of applying the given function to each of the map
   * entries, similarly to {@link com.google.common.collect.Streams#zip(Stream, Stream,
   * BiFunction)}.
   */
  public static <K, V, R> Stream<R> zipMapEntries(
      Iterable<Map.Entry<K, V>> entries, BiFunction<K, V, R> func) {
    checkNotNull(func);
    return Streams.stream(entries).map(entry -> func.apply(entry.getKey(), entry.getValue()));
  }

  /**
   * Provide a stream that results from filtering the given stream's element by their class.
   * Additionally, the resulting stream is typed accordingly. This is a stream equivalent of {@link
   * FluentIterable#filter(Class)} and {@link Iterables#filter(Iterable, Class)}.
   */
  @SuppressWarnings("unchecked")
  public static <T> Stream<T> filterByClass(Stream<? super T> stream, Class<T> cls) {
    // Unchecked cast is safe because of isInstance check.
    // Alternative way would be to add .map(cls::cast), but that does additional work.
    // cf. Iterables#filter(Iterable, Class)
    return (Stream<T>) stream.filter(cls::isInstance);
  }

  /**
   * Provide a stream that results from filtering the given collection's element by their class.
   * Additionally, the resulting stream is typed accordingly. This is a stream equivalent of {@link
   * FluentIterable#filter(Class)} and {@link Iterables#filter(Iterable, Class)}.
   */
  public static <T> Stream<T> filterByClass(Collection<? super T> collection, Class<T> cls) {
    return filterByClass(collection.stream(), cls);
  }

  /**
   * Provide a stream that results from filtering the given iterable's element by their class.
   * Additionally, the resulting stream is typed accordingly. This is a stream equivalent of {@link
   * FluentIterable#filter(Class)} and {@link Iterables#filter(Iterable, Class)}.
   */
  public static <T> Stream<T> filterByClass(Iterable<? super T> iterable, Class<T> cls) {
    return filterByClass(Streams.stream(iterable), cls);
  }

  /**
   * Given a {@link NavigableMap} with {@link String}s as key, return a partial map (similar to
   * {@link NavigableMap#subMap(Object, Object)}) of all keys that have a given prefix.
   *
   * @param map The map to filter.
   * @param prefix The prefix that all keys in the result need to have.
   * @return A partial map of the input.
   */
  public static <V> NavigableMap<String, V> subMapWithPrefix(
      NavigableMap<String, V> map, String prefix) {
    checkNotNull(map);
    checkArgument(!prefix.isEmpty());

    // As the end marker of the set, create the string that is
    // the next bigger string than all possible strings with the given prefix.
    String end = incrementStringByOne(prefix);
    return map.subMap(prefix, true, end, false);
  }

  /**
   * Given a {@link SortedMap} with {@link String}s as key, return a partial map (similar to {@link
   * SortedMap#subMap(Object, Object)}) of all keys that have a given prefix.
   *
   * @param map The map to filter.
   * @param prefix The prefix that all keys in the result need to have.
   * @return A partial map of the input.
   */
  @SuppressWarnings("JdkObsolete") // handling SortedSet is more general
  public static <V> SortedMap<String, V> subMapWithPrefix(SortedMap<String, V> map, String prefix) {
    checkNotNull(map);
    checkArgument(!prefix.isEmpty());

    // As the end marker of the set, create the string that is
    // the next bigger string than all possible strings with the given prefix.
    String end = incrementStringByOne(prefix);
    return map.subMap(prefix, end);
  }

  /**
   * Given a {@link NavigableSet} of {@link String}, return a set (similar to {@link
   * NavigableSet#subSet(Object, Object)}) of all entries that have a given prefix.
   *
   * @param set The set to filter.
   * @param prefix The prefix that all keys in the result need to have.
   * @return A subset of the input.
   */
  public static NavigableSet<String> subSetWithPrefix(NavigableSet<String> set, String prefix) {
    checkNotNull(set);
    checkArgument(!prefix.isEmpty());

    // As the end marker of the set, create the string that is
    // the next bigger string than all possible strings with the given prefix.
    String end = incrementStringByOne(prefix);
    return set.subSet(prefix, true, end, false);
  }

  /**
   * Given a {@link SortedSet} of {@link String}, return a set (similar to {@link
   * SortedSet#subSet(Object, Object)}) of all entries that have a given prefix.
   *
   * @param set The set to filter.
   * @param prefix The prefix that all keys in the result need to have.
   * @return A subset of the input.
   */
  @SuppressWarnings("JdkObsolete") // handling SortedSet is more general
  public static SortedSet<String> subSetWithPrefix(SortedSet<String> set, String prefix) {
    checkNotNull(set);
    checkArgument(!prefix.isEmpty());

    // As the end marker of the set, create the string that is
    // the next bigger string than all possible strings with the given prefix.
    String end = incrementStringByOne(prefix);
    return set.subSet(prefix, end);
  }

  private static String incrementStringByOne(String prefix) {
    // To create the next bigger string than all strings with the same prefix,
    // take the prefix and increment the value of the last character by one.
    StringBuilder end = new StringBuilder(prefix);

    int lastPos = end.length() - 1;
    // This is basically end[lastPos] += 1
    end.setCharAt(lastPos, Chars.checkedCast(end.charAt(lastPos) + 1));

    return end.toString();
  }

  private static boolean guaranteesNaturalOrder(Comparator<?> comp) {
    return comp == null
        || comp.equals(Comparator.naturalOrder())
        || comp.equals(Ordering.natural());
  }

  /**
   * Check whether two comparators define the same order. If this method returns {@code false}
   * nothing is known, but if it returns {@code true}, the comparators can be treated as equal. This
   * method accepts {@code null} and interprets this as natural order, like for example {@link
   * SortedSet#comparator()} does.
   */
  static boolean guaranteedSameOrder(@Nullable Comparator<?> comp1, @Nullable Comparator<?> comp2) {
    return Objects.equals(comp1, comp2)
        || (guaranteesNaturalOrder(comp1) && guaranteesNaturalOrder(comp2));
  }

  /** An implementation of {@link SortedSet#equals(Object)}. */
  @SuppressWarnings("JdkObsolete") // handling SortedSet is more general
  static boolean sortedSetEquals(SortedSet<?> coll1, @Nullable Object pColl2) {
    checkNotNull(coll1);
    if (coll1 == pColl2) {
      return true;
    }
    if (!(pColl2 instanceof Set)) {
      return false;
    }
    Set<?> coll2 = (Set<?>) pColl2;
    if (coll1.size() != coll2.size()) {
      return false;
    }

    if (pColl2 instanceof SortedSet<?>
        && Collections3.guaranteedSameOrder(
            coll1.comparator(), ((SortedSet<?>) coll2).comparator())) {
      @SuppressWarnings("unchecked")
      Comparator<Object> comp =
          (Comparator<Object>)
              Objects.requireNonNullElse(coll1.comparator(), Comparator.naturalOrder());
      Iterator<?> it1 = coll1.iterator();
      Iterator<?> it2 = coll2.iterator();
      try {
        while (it1.hasNext()) {
          Object element = it1.next();
          Object otherElement = it2.next();
          if (otherElement == null || comp.compare(element, otherElement) != 0) {
            return false;
          }
        }
        return true;
      } catch (ClassCastException e) {
        return false;
      } catch (NoSuchElementException e) {
        return false; // concurrent change to other set
      }
    }

    try {
      return coll1.containsAll(coll2);
      // The catch should catch a NullPointerException! But spotbugs complained. This class is only
      // temporarly here anyway, so no problem
    } catch (ClassCastException e) {
      return false;
    }
  }

  /* This method implements {@link SortedSet#containsAll} */
  @SuppressWarnings("JdkObsolete") // handling SortedSet is more general
  static boolean sortedSetContainsAll(
      SortedSet<?> coll1, Collection<?> pColl2, @Nullable Equivalence<Object> pAdditionalEquality) {
    checkNotNull(coll1);
    if (pColl2.isEmpty()) {
      return true;
    }
    if (coll1.isEmpty()) {
      return false;
    }
    Collection<?> coll2;
    if (pColl2 instanceof Multiset<?>) {
      // Multiset is irrelevant for containsAll
      coll2 = ((Multiset<?>) pColl2).elementSet();
    } else {
      coll2 = pColl2;
    }

    if (coll2 instanceof SortedSet<?>
        && guaranteedSameOrder(coll1.comparator(), ((SortedSet<?>) coll2).comparator())) {

      if (coll2.size() > coll1.size()) {
        return false;
      }

      // There are two strategies for containsAll of two sorted sets with the same order:
      // 1) iterate through both sets simultaneously
      // 2) iterate through the other set and check for containment each time
      // Assuming this set has n elements and the other has k, the time is as follows:
      // 1) O(n)              (because k < n)
      // 2) O(k * log(n))     (lookup is logarithmic)
      // Here we implement method 1)

      @SuppressWarnings("unchecked")
      Comparator<Object> comparator =
          (Comparator<Object>)
              Objects.requireNonNullElse(coll1.comparator(), Comparator.naturalOrder());

      Iterator<?> it1 = coll1.iterator();
      Iterator<?> it2 = coll2.iterator();

      // val2 is always the next value we have to find in this set.
      // If its not there, we can return false.
      @Var Object val2 = it2.next();

      while (true) {
        Object val1 = it1.next();

        int comp;
        try {
          comp = comparator.compare(val1, val2);
          // The catch should catch a NullPointerException! But spotbugs complained. This class is
          // only temporarly here anyway, so no problem
        } catch (ClassCastException e) {
          return false;
        }
        if (comp < 0) {
          // val1 < val2
          if (!it1.hasNext()) {
            // There is no matching entry of val2 in coll1.
            return false;
          }

        } else if (comp > 0) {
          // val1 > val2
          // There is no matching entry of val2 in coll1.
          return false;

        } else {
          // val1 = val2
          if (pAdditionalEquality != null && !pAdditionalEquality.equivalent(val1, val2)) {
            return false;
          }
          if (!it2.hasNext()) {
            return true; // coll2 finished, all elements were in coll1.
          }
          if (!it1.hasNext()) {
            return false; // No matching entry of it2.next in coll1.
          }
          val2 = it2.next();
        }
      }
    }

    for (Object val2 : coll2) {
      if (!coll1.contains(val2)) {
        return false;
      }
    }
    return true;
  }
}
