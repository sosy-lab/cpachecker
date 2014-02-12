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
package org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula;

import org.junit.Assert;
import org.junit.Test;
import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;

public class CtoFormulaConverterTest {

  static SSAMapBuilder emptyMap = SSAMap.emptySSAMap().builder();

  private String rawFieldTest(String name, Pair<Integer, Integer> msb_lsb) {

    String fieldName = CtoFormulaConverter.makeFieldVariableName(name, msb_lsb, emptyMap);

    Assert.assertTrue("Expected field as output", CtoFormulaConverter.IS_FIELD_VARIABLE.apply(fieldName));

    Pair<String, Pair<Integer, Integer>> data = CtoFormulaConverter.removeFieldVariable(fieldName);

    Assert.assertTrue("Output name should match the input name", data.getFirst().equals(name));
    Assert.assertTrue("Output data should match the input data", data.getSecond().equals(msb_lsb));

    return fieldName;
  }

  /**
   * This method tests if the creation of variablenames for fields works propery
   * @throws Exception
   */
  @Test
  public void testSimpleField() throws Exception {
    String name = "varName";
    Pair<Integer, Integer> msb_lsb = Pair.of(3, 7);
    rawFieldTest(name, msb_lsb);
  }
}
