// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.testing.ClassSanityTester;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Constructor;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.annotations.SuppressForbidden;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentLinkedList;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;

/** Testing the custom SSA implementation. */
@SuppressFBWarnings("NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
public class SSAMapTest {

  @SuppressForbidden("reflection only in test")
  private ClassSanityTester classSanityTester() throws Exception {
    // hacky way to get a non-empty PointerTargetSet
    // until that class gets mockable
    Constructor<?> ptsConstructor = PointerTargetSet.class.getDeclaredConstructors()[0];
    ptsConstructor.setAccessible(true);
    PointerTargetSet dummyPTS =
        (PointerTargetSet)
            ptsConstructor.newInstance(
                PathCopyingPersistentTreeMap.<String, CType>of().putAndCopy("foo", CVoidType.VOID),
                PathCopyingPersistentTreeMap.of(),
                PersistentLinkedList.of(),
                PathCopyingPersistentTreeMap.of(),
                PersistentLinkedList.of(),
                0);

    return new ClassSanityTester()
        .setDistinctValues(
            SSAMap.class,
            SSAMap.emptySSAMap(),
            SSAMap.emptySSAMap().builder().setIndex("a", CVoidType.VOID, 1).build())
        .setDistinctValues(
            PointerTargetSet.class, PointerTargetSet.emptyPointerTargetSet(), dummyPTS);
  }

  private SSAMapBuilder builder;

  @Before
  public void createBuilder() {
    builder = SSAMap.emptySSAMap().builder();
  }

  @Test
  public void testEquals() throws Exception {
    classSanityTester().testEquals(PathFormula.class);
  }

  @Test
  public void testNulls() throws Exception {
    classSanityTester().testNulls(PathFormula.class);
  }

  @Test
  public void testSSA() {
    builder
        .setIndex("a", CNumericTypes.INT, 1)
        .setIndex("b", CNumericTypes.INT, 2)
        .setIndex("c", CNumericTypes.INT, 3);

    assertThat(builder.getIndex("a")).isEqualTo(1);
    assertThat(builder.getIndex("b")).isEqualTo(2);
    assertThat(builder.getIndex("c")).isEqualTo(3);

    assertThat(builder.getFreshIndex("a")).isEqualTo(2);
    assertThat(builder.getFreshIndex("b")).isEqualTo(3);
    assertThat(builder.getFreshIndex("c")).isEqualTo(4);

    // simple var
    builder = builder.setIndex("b", CNumericTypes.INT, 5);

    assertThat(builder.getIndex("b")).isEqualTo(5);
    assertThat(builder.getFreshIndex("b")).isEqualTo(6);
  }

  private static FreshValueProvider createFreshNewValueProviderWith(String name, int index) {
    PersistentSortedMap<String, Integer> mapping =
        PathCopyingPersistentTreeMap.<String, Integer>of().putAndCopy(name, index);
    return new FreshValueProvider(mapping);
  }

  @Test
  public void testSSAbam() {
    builder
        .setIndex("a", CNumericTypes.INT, 1)
        .setIndex("b", CNumericTypes.INT, 2)
        .setIndex("c", CNumericTypes.INT, 3);

    assertThat(builder.getIndex("a")).isEqualTo(1);
    assertThat(builder.getIndex("b")).isEqualTo(2);
    assertThat(builder.getIndex("c")).isEqualTo(3);

    assertThat(builder.getFreshIndex("a")).isEqualTo(2);
    assertThat(builder.getFreshIndex("b")).isEqualTo(3);
    assertThat(builder.getFreshIndex("c")).isEqualTo(4);

    // simple var
    builder = builder.setIndex("b", CNumericTypes.INT, 5);

    assertThat(builder.getIndex("b")).isEqualTo(5);
    assertThat(builder.getFreshIndex("b")).isEqualTo(6);

    // latest used var
    FreshValueProvider bamfvp = createFreshNewValueProviderWith("c", 7);
    builder.mergeFreshValueProviderWith(bamfvp);

    assertThat(builder.getIndex("c")).isEqualTo(3);
    assertThat(builder.getFreshIndex("c")).isEqualTo(8);

    FreshValueProvider bamfvp2 = createFreshNewValueProviderWith("c", 9);
    builder.mergeFreshValueProviderWith(bamfvp2);
    assertThat(builder.getFreshIndex("c")).isEqualTo(10);

    builder = builder.setIndex("c", CNumericTypes.INT, 15);

    assertThat(builder.getIndex("c")).isEqualTo(15);
    assertThat(builder.getFreshIndex("c")).isEqualTo(16);
  }

  @Test
  public void testSSAExceptionMonotone() {
    builder.setIndex("a", CNumericTypes.INT, 2);

    assertThrows(IllegalArgumentException.class, () -> builder.setIndex("a", CNumericTypes.INT, 1));
  }

  @Test
  public void testSSAExceptionNegative() {
    assertThrows(
        IllegalArgumentException.class, () -> builder.setIndex("a", CNumericTypes.INT, -5));
  }

  @Test
  public void testSSAExceptionMonotone2() {
    builder.setIndex("a", CNumericTypes.INT, 2);

    assertThrows(IllegalArgumentException.class, () -> builder.setIndex("a", CNumericTypes.INT, 1));
  }
}
