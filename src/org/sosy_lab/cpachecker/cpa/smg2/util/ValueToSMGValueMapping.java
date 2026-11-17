// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.util;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Equivalence.Wrapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.ValueWrapper;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;

/**
 * Persistent bijective mapping between {@link Value}s and {@link SMGValue}s. {@link Value}s are
 * compared w.r.t. the equivalence {@link ValueWrapper}, not with {@code Value.equals()}. Adding or
 * removing a mapping costs O(log n), unlike copying an immutable map. Iteration follows the
 * insertion order.
 */
public final class ValueToSMGValueMapping {

  private static final ValueWrapper VALUE_EQUIVALENCE = new ValueWrapper();

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
   * lookups must use the equivalence of the {@link Wrapper} (not {@code Value.equals()}), so the
   * values cannot be keys of a tree map directly. Instead, the tree map is keyed by the hash code
   * of the wrapped value, and maps it to a bucket with all mappings of wrapped values with this
   * hash code. A bucket is never empty and nearly always holds only one mapping; it holds more than
   * one only for hash collisions of non-equivalent values (e.g., integer 0 and float 0.0).
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

  /**
   * Returns whether a {@link Value} equivalent to the given one (w.r.t. {@link ValueWrapper}) is
   * mapped.
   */
  public boolean containsKey(Value pValue) {
    return getMapping(VALUE_EQUIVALENCE.wrap(pValue)) != null;
  }

  /**
   * Returns the {@link SMGValue} mapped to a {@link Value} equivalent to the given one (w.r.t.
   * {@link ValueWrapper}), or null.
   */
  public @Nullable SMGValue get(Value pValue) {
    Mapping mapping = getMapping(VALUE_EQUIVALENCE.wrap(pValue));
    return mapping == null ? null : mapping.smgValue();
  }

  private @Nullable Mapping getMapping(Wrapper<Value> pValue) {
    ImmutableMap<Wrapper<Value>, Mapping> bucket = byValue.get(pValue.hashCode());
    return bucket == null ? null : bucket.get(pValue);
  }

  public boolean containsValue(SMGValue pSMGValue) {
    return bySMGValue.containsKey(pSMGValue);
  }

  /**
   * Returns the {@link Value} mapped to the given {@link SMGValue} (as it was added with {@link
   * #putAndCopy(Value, SMGValue)}), or null.
   */
  public @Nullable Value getKeyForValue(SMGValue pSMGValue) {
    Mapping mapping = bySMGValue.get(pSMGValue);
    return mapping == null ? null : mapping.value().get();
  }

  /** The mappings in insertion order. */
  public Iterable<Map.Entry<Value, SMGValue>> entries() {
    return () ->
        byPosition.values().stream()
            .map(m -> Maps.immutableEntry(m.value().get(), m.smgValue()))
            .iterator();
  }

  /**
   * Adds a mapping. Neither a {@link Value} equivalent to the given one (w.r.t. {@link
   * ValueWrapper}) nor the given {@link SMGValue} may be mapped yet (like for an ImmutableBiMap).
   */
  public ValueToSMGValueMapping putAndCopy(Value pValue, SMGValue pSMGValue) {
    Wrapper<Value> wrappedValue = VALUE_EQUIVALENCE.wrap(pValue);
    checkArgument(getMapping(wrappedValue) == null, "Multiple entries with same key: %s", pValue);
    checkArgument(!containsValue(pSMGValue), "Multiple entries with same value: %s", pSMGValue);
    Mapping mapping = new Mapping(wrappedValue, pSMGValue, nextPosition);
    int hash = wrappedValue.hashCode();
    ImmutableMap<Wrapper<Value>, Mapping> bucket = byValue.get(hash);
    ImmutableMap<Wrapper<Value>, Mapping> newBucket =
        bucket == null
            ? ImmutableMap.of(wrappedValue, mapping)
            : ImmutableMap.<Wrapper<Value>, Mapping>builder()
                .putAll(bucket)
                .put(wrappedValue, mapping)
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

  /**
   * Checks that the three internal maps are consistent, i.e., contain the same mappings under the
   * correct keys, and that no bucket is empty.
   */
  @VisibleForTesting
  void checkInvariants() {
    checkState(byPosition.size() == bySMGValue.size(), "Maps have different sizes");
    int numberOfMappingsInBuckets = 0;
    for (Map.Entry<Integer, ImmutableMap<Wrapper<Value>, Mapping>> bucket : byValue.entrySet()) {
      checkState(!bucket.getValue().isEmpty(), "Empty bucket for hash %s", bucket.getKey());
      for (Map.Entry<Wrapper<Value>, Mapping> entry : bucket.getValue().entrySet()) {
        Mapping mapping = entry.getValue();
        checkState(entry.getKey() == mapping.value(), "Wrong key for %s", mapping);
        checkState(entry.getKey().hashCode() == bucket.getKey(), "Wrong bucket for %s", mapping);
        checkState(bySMGValue.get(mapping.smgValue()) == mapping, "Not in bySMGValue: %s", mapping);
        checkState(byPosition.get(mapping.position()) == mapping, "Not in byPosition: %s", mapping);
        checkState(mapping.position() < nextPosition, "Position too large: %s", mapping);
        numberOfMappingsInBuckets++;
      }
    }
    checkState(numberOfMappingsInBuckets == bySMGValue.size(), "Maps have different sizes");
  }
}
