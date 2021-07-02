// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg;

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.common.collect.PersistentSortedMaps;
import org.sosy_lab.cpachecker.cpa.smg.util.PersistentSet;

/**
 * Utility class for collecting streams to persistent sorted map.
 */
public class PersistentMapCollector<E, K extends Comparable<K>, V>
    implements Collector<E, PersistentSortedMap<K, V>, PersistentSortedMap<K, V>> {

  private final Function<E, K> keyMapper;
  private final Function<E, V> valueMapper;

  public static <E, K extends Comparable<K>, V> PersistentMapCollector<E, K, V>
      toPersistentMap(Function<E, K> pKeyMapper, Function<E, V> pValueMapper) {
    return new PersistentMapCollector<>(pKeyMapper, pValueMapper);
  }

  private PersistentMapCollector(Function<E, K> pKeyMapper, Function<E, V> pValueMapper) {
    keyMapper = pKeyMapper;
    valueMapper = pValueMapper;
  }

  @Override
  public BiConsumer<PersistentSortedMap<K, V>, E> accumulator() {
    return (map, elem) -> {
      map = map.putAndCopy(keyMapper.apply(elem), valueMapper.apply(elem));
    };
  }

  @Override
  public Set<Characteristics> characteristics() {
    return PersistentSet.of();
  }

  @Override
  public BinaryOperator<PersistentSortedMap<K, V>> combiner() {
    return (map1, map2) -> PersistentSortedMaps
        .merge(map1, map2, PersistentSortedMaps.getExceptionMergeConflictHandler());
  }

  @Override
  public Function<PersistentSortedMap<K, V>, PersistentSortedMap<K, V>> finisher() {
    return map -> map;
  }

  @Override
  public Supplier<PersistentSortedMap<K, V>> supplier() {
    return PathCopyingPersistentTreeMap::of;
  }

}
