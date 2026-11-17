// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.util;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Equivalence.Wrapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.Map;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;

/**
 * Persistent bijective mapping between (wrapped) {@link Value}s and {@link SMGValue}s. Adding or
 * removing a mapping costs O(log n), unlike copying an immutable map. Iteration follows the
 * insertion order.
 */
public final class ValueToSMGValueMapping {

  /*
   * Implementation: each mapping is stored once as a Mapping object, which is referenced from three
   * persistent tree maps (one per kind of access). All three maps always contain the same Mapping
   * objects. Adding or removing a mapping updates each of them in O(log n) by path copying, so a
   * new instance shares nearly all of its structure with the old one.
   */

  /**
   * A single mapping. The position is the insertion counter at the time the mapping was added and
   * is used as key in {@link #byPosition}.
   */
  private record Mapping(Wrapper<Value> value, SMGValue smgValue, long position) {}

  /**
   * Forward direction (lookup by {@link Value}). {@link Value} is not {@link Comparable}, and
   * lookups must use the equivalence of the {@link Wrapper} (not {@link Value#equals(Object)}), so
   * the values cannot be keys of a tree map directly. Instead, the tree map is keyed by the hash
   * code of the wrapped value, and maps it to a bucket with all mappings of wrapped values with
   * this hash code. A bucket is never empty and nearly always holds only one mapping; it holds more
   * than one only for hash collisions of non-equivalent values (e.g., integer 0 and float 0.0).
   */
  private final PersistentMap<Integer, ImmutableMap<Wrapper<Value>, Mapping>> byValue;

  /** Reverse direction (lookup by {@link SMGValue}), which is {@link Comparable}. */
  private final PersistentMap<SMGValue, Mapping> bySMGValue;

  /**
   * The mappings ordered by their insertion counter. This map exists only to iterate in insertion
   * order (like the {@link com.google.common.collect.ImmutableBiMap} this class replaced), because
   * the order of the other two maps depends on hash codes and {@link SMGValue} ids.
   */
  private final PersistentMap<Long, Mapping> byPosition;

  /**
   * The position of the next added mapping. It is only ever incremented, so positions of removed
   * mappings are not reused and positions are not contiguous.
   */
  private final long nextPosition;

  private ValueToSMGValueMapping(
      PersistentMap<Integer, ImmutableMap<Wrapper<Value>, Mapping>> pByValue,
      PersistentMap<SMGValue, Mapping> pBySMGValue,
      PersistentMap<Long, Mapping> pByPosition,
      long pNextPosition) {
    byValue = pByValue;
    bySMGValue = pBySMGValue;
    byPosition = pByPosition;
    nextPosition = pNextPosition;
  }

  public static ValueToSMGValueMapping of() {
    return new ValueToSMGValueMapping(
        PathCopyingPersistentTreeMap.of(),
        PathCopyingPersistentTreeMap.of(),
        PathCopyingPersistentTreeMap.of(),
        0);
  }

  public int size() {
    return bySMGValue.size();
  }

  public boolean containsKey(Wrapper<Value> pValue) {
    return get(pValue) != null;
  }

  public SMGValue get(Wrapper<Value> pValue) {
    ImmutableMap<Wrapper<Value>, Mapping> bucket = byValue.get(pValue.hashCode());
    if (bucket == null) {
      return null;
    }
    Mapping mapping = bucket.get(pValue);
    return mapping == null ? null : mapping.smgValue();
  }

  public boolean containsValue(SMGValue pSMGValue) {
    return bySMGValue.containsKey(pSMGValue);
  }

  /** Returns the wrapped value mapped to the given {@link SMGValue}, or null. */
  public Wrapper<Value> getKeyForValue(SMGValue pSMGValue) {
    Mapping mapping = bySMGValue.get(pSMGValue);
    return mapping == null ? null : mapping.value();
  }

  /** The mappings in insertion order. */
  public Iterable<Map.Entry<Wrapper<Value>, SMGValue>> entries() {
    return () ->
        byPosition.values().stream()
            .map(m -> Maps.immutableEntry(m.value(), m.smgValue()))
            .iterator();
  }

  /** Adds a mapping. Both sides must not be mapped yet (like an ImmutableBiMap). */
  public ValueToSMGValueMapping putAndCopy(Wrapper<Value> pValue, SMGValue pSMGValue) {
    checkArgument(!containsKey(pValue), "Multiple entries with same key: %s", pValue);
    checkArgument(!containsValue(pSMGValue), "Multiple entries with same value: %s", pSMGValue);
    Mapping mapping = new Mapping(pValue, pSMGValue, nextPosition);
    int hash = pValue.hashCode();
    ImmutableMap<Wrapper<Value>, Mapping> bucket = byValue.get(hash);
    ImmutableMap<Wrapper<Value>, Mapping> newBucket =
        bucket == null
            ? ImmutableMap.of(pValue, mapping)
            : ImmutableMap.<Wrapper<Value>, Mapping>builder()
                .putAll(bucket)
                .put(pValue, mapping)
                .buildOrThrow();
    return new ValueToSMGValueMapping(
        byValue.putAndCopy(hash, newBucket),
        bySMGValue.putAndCopy(pSMGValue, mapping),
        byPosition.putAndCopy(nextPosition, mapping),
        nextPosition + 1);
  }

  /** Removes the mappings of the given {@link SMGValue}s. */
  public ValueToSMGValueMapping removeAllAndCopy(Collection<SMGValue> pSMGValues) {
    ValueToSMGValueMapping result = this;
    for (SMGValue smgValue : pSMGValues) {
      result = result.removeAndCopy(smgValue);
    }
    return result;
  }

  /** Removes the mapping of the given {@link SMGValue}, if it exists. */
  public ValueToSMGValueMapping removeAndCopy(SMGValue pSMGValue) {
    Mapping mapping = bySMGValue.get(pSMGValue);
    if (mapping == null) {
      return this;
    }
    int hash = mapping.value().hashCode();
    ImmutableMap<Wrapper<Value>, Mapping> bucket = byValue.get(hash);
    PersistentMap<Integer, ImmutableMap<Wrapper<Value>, Mapping>> newByValue;
    if (bucket.size() == 1) {
      newByValue = byValue.removeAndCopy(hash);
    } else {
      newByValue =
          byValue.putAndCopy(
              hash, ImmutableMap.copyOf(Maps.filterKeys(bucket, k -> !k.equals(mapping.value()))));
    }
    return new ValueToSMGValueMapping(
        newByValue,
        bySMGValue.removeAndCopy(pSMGValue),
        byPosition.removeAndCopy(mapping.position()),
        nextPosition);
  }
}
