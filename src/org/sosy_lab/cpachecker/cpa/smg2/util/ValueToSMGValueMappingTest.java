// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.util;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.Assert.assertThrows;

import com.google.common.base.Equivalence.Wrapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.Test;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.ValueWrapper;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.floatingpoint.FloatValue;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;

public class ValueToSMGValueMappingTest {

  private static final ValueWrapper VALUE_WRAPPER = new ValueWrapper();

  private static final Value INT_ZERO = new NumericValue(0);
  private static final Value LONG_ZERO = new NumericValue(0L);
  private static final Value BIG_INTEGER_ZERO = new NumericValue(BigInteger.ZERO);
  // Not equivalent to INT_ZERO, but has the same hash code
  private static final Value FLOAT_ZERO = new NumericValue(0.0f);
  // Not equivalent to INT_ZERO, but has the same hash code (low and high 32 bits cancel out)
  private static final Value LONG_WITH_HASH_ZERO = new NumericValue((1L << 32) | 1L);

  private static ImmutableList<Map.Entry<Value, SMGValue>> entriesOf(
      ValueToSMGValueMapping pMapping) {
    return ImmutableList.copyOf(pMapping.entries());
  }

  /** Puts a mapping and checks the result and that the old version is unchanged. */
  private static ValueToSMGValueMapping put(
      ValueToSMGValueMapping pMapping, Value pValue, SMGValue pSMGValue) {
    ImmutableList<Map.Entry<Value, SMGValue>> oldEntries = entriesOf(pMapping);

    ValueToSMGValueMapping result = pMapping.putAndCopy(pValue, pSMGValue);
    result.checkInvariants();
    pMapping.checkInvariants();

    assertThat(entriesOf(pMapping)).isEqualTo(oldEntries);
    assertThat(result.size()).isEqualTo(pMapping.size() + 1);
    assertThat(result.get(pValue)).isEqualTo(pSMGValue);
    assertThat(result.getKeyForValue(pSMGValue)).isSameInstanceAs(pValue);
    assertThat(entriesOf(result))
        .isEqualTo(
            ImmutableList.builder()
                .addAll(oldEntries)
                .add(Maps.immutableEntry(pValue, pSMGValue))
                .build());
    return result;
  }

  /** Removes a mapping and checks the result and that the old version is unchanged. */
  private static ValueToSMGValueMapping remove(
      ValueToSMGValueMapping pMapping, SMGValue pSMGValue) {
    ImmutableList<Map.Entry<Value, SMGValue>> oldEntries = entriesOf(pMapping);

    ValueToSMGValueMapping result = pMapping.removeAndCopy(pSMGValue);
    result.checkInvariants();
    pMapping.checkInvariants();

    assertThat(entriesOf(pMapping)).isEqualTo(oldEntries);
    assertThat(result.containsValue(pSMGValue)).isFalse();
    assertThat(result.getKeyForValue(pSMGValue)).isNull();
    assertThat(entriesOf(result))
        .isEqualTo(
            oldEntries.stream()
                .filter(entry -> !entry.getValue().equals(pSMGValue))
                .collect(ImmutableList.toImmutableList()));
    return result;
  }

  @Test
  public void testEmpty() {
    ValueToSMGValueMapping mapping = ValueToSMGValueMapping.of();
    mapping.checkInvariants();

    assertThat(mapping.size()).isEqualTo(0);
    assertThat(mapping.entries()).isEmpty();
    assertThat(mapping.containsKey(INT_ZERO)).isFalse();
    assertThat(mapping.get(INT_ZERO)).isNull();
    SMGValue smgValue = SMGValue.of();
    assertThat(mapping.containsValue(smgValue)).isFalse();
    assertThat(mapping.getKeyForValue(smgValue)).isNull();
  }

  @Test
  public void testPutAndRemove() {
    Value value1 = new NumericValue(1);
    Value value2 = new NumericValue(2);
    SMGValue smgValue1 = SMGValue.of();
    SMGValue smgValue2 = SMGValue.of();

    ValueToSMGValueMapping mapping = ValueToSMGValueMapping.of();
    mapping = put(mapping, value1, smgValue1);
    mapping = put(mapping, value2, smgValue2);
    assertThat(mapping.containsKey(value1)).isTrue();
    assertThat(mapping.containsValue(smgValue2)).isTrue();

    mapping = remove(mapping, smgValue1);
    assertThat(mapping.get(value1)).isNull();
    assertThat(mapping.get(value2)).isEqualTo(smgValue2);
    mapping = remove(mapping, smgValue2);
    assertThat(mapping.size()).isEqualTo(0);
  }

  @Test
  public void testEquivalentValues() {
    assertThat(VALUE_WRAPPER.equivalent(INT_ZERO, LONG_ZERO)).isTrue();
    assertThat(VALUE_WRAPPER.equivalent(INT_ZERO, BIG_INTEGER_ZERO)).isTrue();
    assertThat(INT_ZERO).isNotEqualTo(LONG_ZERO);

    SMGValue smgValue = SMGValue.of();
    ValueToSMGValueMapping mapping = put(ValueToSMGValueMapping.of(), INT_ZERO, smgValue);

    // Lookups use the equivalence, not equals()
    assertThat(mapping.containsKey(LONG_ZERO)).isTrue();
    assertThat(mapping.get(LONG_ZERO)).isEqualTo(smgValue);
    assertThat(mapping.get(BIG_INTEGER_ZERO)).isEqualTo(smgValue);
    // The originally added value is returned
    assertThat(mapping.getKeyForValue(smgValue)).isSameInstanceAs(INT_ZERO);
  }

  @Test
  public void testHashCollisions() {
    assertThat(VALUE_WRAPPER.hash(FLOAT_ZERO)).isEqualTo(VALUE_WRAPPER.hash(INT_ZERO));
    assertThat(VALUE_WRAPPER.hash(LONG_WITH_HASH_ZERO)).isEqualTo(VALUE_WRAPPER.hash(INT_ZERO));
    assertThat(VALUE_WRAPPER.equivalent(INT_ZERO, FLOAT_ZERO)).isFalse();
    assertThat(VALUE_WRAPPER.equivalent(INT_ZERO, LONG_WITH_HASH_ZERO)).isFalse();
    assertThat(VALUE_WRAPPER.equivalent(FLOAT_ZERO, LONG_WITH_HASH_ZERO)).isFalse();

    SMGValue smgValue1 = SMGValue.of();
    SMGValue smgValue2 = SMGValue.of();
    SMGValue smgValue3 = SMGValue.of();
    ValueToSMGValueMapping mapping = ValueToSMGValueMapping.of();
    mapping = put(mapping, INT_ZERO, smgValue1);
    mapping = put(mapping, FLOAT_ZERO, smgValue2);
    mapping = put(mapping, LONG_WITH_HASH_ZERO, smgValue3);
    assertThat(mapping.get(INT_ZERO)).isEqualTo(smgValue1);
    assertThat(mapping.get(FLOAT_ZERO)).isEqualTo(smgValue2);
    assertThat(mapping.get(LONG_WITH_HASH_ZERO)).isEqualTo(smgValue3);

    // Remove from the middle of a bucket, then the rest until the bucket is gone
    mapping = remove(mapping, smgValue2);
    assertThat(mapping.get(FLOAT_ZERO)).isNull();
    assertThat(mapping.get(INT_ZERO)).isEqualTo(smgValue1);
    assertThat(mapping.get(LONG_WITH_HASH_ZERO)).isEqualTo(smgValue3);
    mapping = remove(mapping, smgValue1);
    assertThat(mapping.get(LONG_WITH_HASH_ZERO)).isEqualTo(smgValue3);
    mapping = remove(mapping, smgValue3);
    assertThat(mapping.size()).isEqualTo(0);
    assertThat(mapping.containsKey(INT_ZERO)).isFalse();
  }

  @Test
  public void testPutEquivalentValueTwiceFails() {
    ValueToSMGValueMapping mapping = put(ValueToSMGValueMapping.of(), INT_ZERO, SMGValue.of());
    SMGValue otherSMGValue = SMGValue.of();

    assertThrows(IllegalArgumentException.class, () -> mapping.putAndCopy(INT_ZERO, otherSMGValue));
    assertThrows(
        IllegalArgumentException.class, () -> mapping.putAndCopy(LONG_ZERO, otherSMGValue));
    mapping.checkInvariants();
    assertThat(mapping.size()).isEqualTo(1);
  }

  @Test
  public void testPutSMGValueTwiceFails() {
    SMGValue smgValue = SMGValue.of();
    ValueToSMGValueMapping mapping = put(ValueToSMGValueMapping.of(), INT_ZERO, smgValue);
    Value otherValue = new NumericValue(1);

    assertThrows(IllegalArgumentException.class, () -> mapping.putAndCopy(otherValue, smgValue));
    mapping.checkInvariants();
    assertThat(mapping.size()).isEqualTo(1);
  }

  @Test
  public void testRemoveMissingSMGValue() {
    ValueToSMGValueMapping mapping = put(ValueToSMGValueMapping.of(), INT_ZERO, SMGValue.of());

    assertThat(mapping.removeAndCopy(SMGValue.of())).isSameInstanceAs(mapping);
    assertThat(mapping.removeAllAndCopy(ImmutableList.of())).isSameInstanceAs(mapping);
    assertThat(mapping.removeAllAndCopy(ImmutableList.of(SMGValue.of()))).isSameInstanceAs(mapping);
  }

  @Test
  public void testRemoveAll() {
    SMGValue smgValue1 = SMGValue.of();
    SMGValue smgValue2 = SMGValue.of();
    SMGValue smgValue3 = SMGValue.of();
    ValueToSMGValueMapping mapping = ValueToSMGValueMapping.of();
    mapping = put(mapping, new NumericValue(1), smgValue1);
    mapping = put(mapping, new NumericValue(2), smgValue2);
    mapping = put(mapping, new NumericValue(3), smgValue3);

    // Duplicates and missing SMGValues are allowed
    ValueToSMGValueMapping result =
        mapping.removeAllAndCopy(ImmutableList.of(smgValue1, SMGValue.of(), smgValue3, smgValue1));
    result.checkInvariants();
    assertThat(entriesOf(result))
        .containsExactly(Maps.immutableEntry(new NumericValue(2), smgValue2));
    assertThat(mapping.size()).isEqualTo(3);
  }

  @Test
  public void testInsertionOrder() {
    Value value1 = new NumericValue(1);
    Value value2 = new NumericValue(2);
    Value value3 = new NumericValue(3);
    SMGValue smgValue1 = SMGValue.of();
    SMGValue smgValue2 = SMGValue.of();
    SMGValue smgValue3 = SMGValue.of();

    ValueToSMGValueMapping mapping = ValueToSMGValueMapping.of();
    // Insert in an order that differs from the order of hash codes and SMGValue ids
    mapping = put(mapping, value3, smgValue2);
    mapping = put(mapping, value1, smgValue3);
    mapping = put(mapping, value2, smgValue1);
    assertThat(entriesOf(mapping))
        .containsExactly(
            Maps.immutableEntry(value3, smgValue2),
            Maps.immutableEntry(value1, smgValue3),
            Maps.immutableEntry(value2, smgValue1))
        .inOrder();

    // A removed and re-added mapping is moved to the end
    mapping = remove(mapping, smgValue2);
    mapping = put(mapping, value3, smgValue2);
    assertThat(entriesOf(mapping))
        .containsExactly(
            Maps.immutableEntry(value1, smgValue3),
            Maps.immutableEntry(value2, smgValue1),
            Maps.immutableEntry(value3, smgValue2))
        .inOrder();
  }

  /**
   * Applies random operations and compares the results with a reference model, consisting of an
   * insertion-ordered map with wrapped values as keys and an inverse map. The pools of values are
   * small, so that duplicates, hash collisions, and removals of missing mappings are frequent.
   * Finally, checks that all intermediate versions are unchanged.
   */
  @Test
  public void testRandomOperations() {
    ImmutableList<Value> valuePool =
        ImmutableList.of(
            INT_ZERO,
            LONG_ZERO,
            BIG_INTEGER_ZERO,
            FLOAT_ZERO,
            new NumericValue(0.0),
            new NumericValue(-0.0),
            new NumericValue(FloatValue.zero(FloatValue.Format.Float32)),
            new NumericValue(FloatValue.zero(FloatValue.Format.Float64)),
            new NumericValue(1),
            new NumericValue(1L), // same hash code as integer 1, but not equivalent
            new NumericValue(2),
            new NumericValue(3),
            LONG_WITH_HASH_ZERO,
            new NumericValue((2L << 32) | 2L),
            new NumericValue((3L << 32) | 3L));
    List<SMGValue> smgValuePool = new ArrayList<>();
    for (int i = 0; i < 12; i++) {
      smgValuePool.add(SMGValue.of());
    }

    Random random = new Random(4815162342L);
    ValueToSMGValueMapping mapping = ValueToSMGValueMapping.of();
    Map<Wrapper<Value>, SMGValue> expected = new LinkedHashMap<>();
    Map<SMGValue, Value> expectedInverse = new HashMap<>();
    List<ValueToSMGValueMapping> versions = new ArrayList<>();
    List<ImmutableList<Map.Entry<Value, SMGValue>>> versionEntries = new ArrayList<>();

    for (int step = 0; step < 3000; step++) {
      int operation = random.nextInt(10);
      if (operation < 6) {
        Value value = valuePool.get(random.nextInt(valuePool.size()));
        SMGValue smgValue = smgValuePool.get(random.nextInt(smgValuePool.size()));
        Wrapper<Value> wrappedValue = VALUE_WRAPPER.wrap(value);
        if (expected.containsKey(wrappedValue) || expectedInverse.containsKey(smgValue)) {
          ValueToSMGValueMapping current = mapping;
          assertThrows(IllegalArgumentException.class, () -> current.putAndCopy(value, smgValue));
        } else {
          mapping = mapping.putAndCopy(value, smgValue);
          expected.put(wrappedValue, smgValue);
          expectedInverse.put(smgValue, value);
        }
      } else if (operation < 9) {
        SMGValue smgValue = smgValuePool.get(random.nextInt(smgValuePool.size()));
        mapping = mapping.removeAndCopy(smgValue);
        Value removed = expectedInverse.remove(smgValue);
        if (removed != null) {
          expected.remove(VALUE_WRAPPER.wrap(removed));
        }
      } else {
        List<SMGValue> toRemove = new ArrayList<>();
        int count = random.nextInt(4);
        for (int i = 0; i < count; i++) {
          toRemove.add(smgValuePool.get(random.nextInt(smgValuePool.size())));
        }
        mapping = mapping.removeAllAndCopy(toRemove);
        for (SMGValue smgValue : toRemove) {
          Value removed = expectedInverse.remove(smgValue);
          if (removed != null) {
            expected.remove(VALUE_WRAPPER.wrap(removed));
          }
        }
      }

      mapping.checkInvariants();
      ImmutableList<Map.Entry<Value, SMGValue>> expectedEntries =
          expected.entrySet().stream()
              .map(entry -> Maps.immutableEntry(entry.getKey().get(), entry.getValue()))
              .collect(ImmutableList.toImmutableList());
      assertWithMessage("entries after step %s", step)
          .that(entriesOf(mapping))
          .isEqualTo(expectedEntries);
      assertThat(mapping.size()).isEqualTo(expected.size());
      for (Value value : valuePool) {
        SMGValue expectedSMGValue = expected.get(VALUE_WRAPPER.wrap(value));
        assertWithMessage("get(%s) after step %s", value, step)
            .that(mapping.get(value))
            .isEqualTo(expectedSMGValue);
        assertThat(mapping.containsKey(value)).isEqualTo(expectedSMGValue != null);
      }
      for (SMGValue smgValue : smgValuePool) {
        Value expectedValue = expectedInverse.get(smgValue);
        assertWithMessage("getKeyForValue(%s) after step %s", smgValue, step)
            .that(mapping.getKeyForValue(smgValue))
            .isSameInstanceAs(expectedValue);
        assertThat(mapping.containsValue(smgValue)).isEqualTo(expectedValue != null);
      }
      versions.add(mapping);
      versionEntries.add(expectedEntries);
    }

    for (int i = 0; i < versions.size(); i++) {
      versions.get(i).checkInvariants();
      assertWithMessage("entries of version %s", i)
          .that(entriesOf(versions.get(i)))
          .isEqualTo(versionEntries.get(i));
    }
  }
}
