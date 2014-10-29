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

import java.lang.reflect.Constructor;

import org.junit.Assert;
import org.junit.Test;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;

import com.google.common.testing.ClassSanityTester;

/**
 * Testing the custom SSA implementation.
 */
public class PathFormulaTest {

  private ClassSanityTester classSanityTester() throws Exception {
    // hacky way to get a non-empty PointerTargetSet
    // until that class gets mockable
    Constructor<?> ptsConstructor = PointerTargetSet.class.getDeclaredConstructors()[0];
    ptsConstructor.setAccessible(true);
    PointerTargetSet dummyPTS = (PointerTargetSet)ptsConstructor.newInstance(
        PathCopyingPersistentTreeMap.<String, CType>of().putAndCopy("foo", CNumericTypes.VOID),
        null,
        PathCopyingPersistentTreeMap.of(),
        PathCopyingPersistentTreeMap.of(),
        PathCopyingPersistentTreeMap.of()
        );

    return new ClassSanityTester()
        .setDistinctValues(SSAMap.class,
            SSAMap.emptySSAMap(),
            SSAMap.emptySSAMap().builder().setIndex("a", CNumericTypes.VOID, 1).build())
        .setDistinctValues(PointerTargetSet.class,
            PointerTargetSet.emptyPointerTargetSet(),
            dummyPTS);
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
    SSAMap.SSAMapBuilder builder = SSAMap.emptySSAMap().builder()
        .setIndex("a", CNumericTypes.INT, 1)
        .setIndex("b", CNumericTypes.INT, 2)
        .setIndex("c", CNumericTypes.INT, 3);

    Assert.assertTrue(builder.getIndex("a") == 1);
    Assert.assertTrue(builder.getIndex("b") == 2);
    Assert.assertTrue(builder.getIndex("c") == 3);

    Assert.assertTrue(builder.getFreshIndex("a") == 2);
    Assert.assertTrue(builder.getFreshIndex("b") == 3);
    Assert.assertTrue(builder.getFreshIndex("c") == 4);

    // simple var
    builder = builder.setIndex("b", CNumericTypes.INT, 5);

    Assert.assertTrue(builder.getIndex("b") == 5);
    Assert.assertTrue(builder.getFreshIndex("b") == 6);


    // latest used var
    builder = builder.setFreshValueBasis("c", CNumericTypes.INT, 7);

    Assert.assertTrue(builder.getIndex("c") == 3);
    Assert.assertTrue(builder.getFreshIndex("c") == 8);

    builder = builder.setFreshValueBasis("c", CNumericTypes.INT, 9);

    Assert.assertTrue(builder.getIndex("c") == 3);
    Assert.assertTrue(builder.getFreshIndex("c") == 10);

    builder = builder.setIndex("c", CNumericTypes.INT, 15);

    Assert.assertTrue(builder.getIndex("c") == 15);
    Assert.assertTrue(builder.getFreshIndex("c") == 16);
  }

  @Test(expected=IllegalArgumentException.class)
  public void testSSAExceptionMonotone() {
    SSAMap.emptySSAMap().builder()
        .setIndex("a", CNumericTypes.INT, 2)
        .setIndex("a", CNumericTypes.INT, 1);
  }

  @Test(expected=IllegalArgumentException.class)
  public void testSSAExceptionNegative() {
    SSAMap.emptySSAMap().builder()
        .setIndex("a", CNumericTypes.INT, -5);
  }

  @Test(expected=IllegalArgumentException.class)
  public void testSSAExceptionMonotone2() {
    SSAMap.emptySSAMap().builder()
        .setIndex("a", CNumericTypes.INT, 2)
        .setFreshValueBasis("a", CNumericTypes.INT, 1);
  }
}