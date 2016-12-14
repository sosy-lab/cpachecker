/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.util.predicates.pathformula;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.testing.ClassSanityTester;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentLinkedList;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;

import java.lang.reflect.Constructor;

/**
 * Testing the custom SSA implementation.
 */
@SuppressFBWarnings("NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
public class PathFormulaTest {

  private ClassSanityTester classSanityTester() throws Exception {
    // hacky way to get a non-empty PointerTargetSet
    // until that class gets mockable
    Constructor<?> ptsConstructor = PointerTargetSet.class.getDeclaredConstructors()[0];
    ptsConstructor.setAccessible(true);
    PointerTargetSet dummyPTS = (PointerTargetSet)ptsConstructor.newInstance(
        PathCopyingPersistentTreeMap.<String, CType>of().putAndCopy("foo", CVoidType.VOID),
        null,
        PathCopyingPersistentTreeMap.of(),
        PersistentLinkedList.of(),
        PathCopyingPersistentTreeMap.of()
        );

    return new ClassSanityTester()
        .setDistinctValues(SSAMap.class,
            SSAMap.emptySSAMap(),
            SSAMap.emptySSAMap().builder().setIndex("a", CVoidType.VOID, 1).build())
        .setDistinctValues(PointerTargetSet.class,
            PointerTargetSet.emptyPointerTargetSet(),
            dummyPTS);
  }

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

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
    FreshValueProvider bamfvp = new FreshValueProvider();
    bamfvp.put("c", 7);
    builder.mergeFreshValueProviderWith(bamfvp);

    assertThat(builder.getIndex("c")).isEqualTo(3);
    assertThat(builder.getFreshIndex("c")).isEqualTo(8);

    FreshValueProvider bamfvp2 = new FreshValueProvider();
    bamfvp2.put("c", 9);
    builder.mergeFreshValueProviderWith(bamfvp2);
    assertThat(builder.getFreshIndex("c")).isEqualTo(10);

    builder = builder.setIndex("c", CNumericTypes.INT, 15);

    assertThat(builder.getIndex("c")).isEqualTo(15);
    assertThat(builder.getFreshIndex("c")).isEqualTo(16);
  }

  @Test
  public void testSSAExceptionMonotone() {
    builder.setIndex("a", CNumericTypes.INT, 2);

    thrown.expect(IllegalArgumentException.class);
    builder.setIndex("a", CNumericTypes.INT, 1);
  }

  @Test
  public void testSSAExceptionNegative() {
    thrown.expect(IllegalArgumentException.class);
    builder.setIndex("a", CNumericTypes.INT, -5);
  }

  @Test
  public void testSSAExceptionMonotone2() {
    builder.setIndex("a", CNumericTypes.INT, 2);

    thrown.expect(IllegalArgumentException.class);
    builder.setIndex("a", CNumericTypes.INT, 1);
  }
}
