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
}