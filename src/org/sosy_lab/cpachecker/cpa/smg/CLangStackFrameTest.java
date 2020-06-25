/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.smg;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assert_;

import com.google.common.collect.ImmutableList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGRegion;

public class CLangStackFrameTest {
  static private final CFunctionType functionType = CFunctionType.functionTypeWithReturnType(CNumericTypes.UNSIGNED_LONG_INT);
  private static final CFunctionDeclaration functionDeclaration =
      new CFunctionDeclaration(FileLocation.DUMMY, functionType, "foo", ImmutableList.of());
  static private final MachineModel usedMachineModel = MachineModel.LINUX64;
  private CLangStackFrame sf;

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() {

    sf = new CLangStackFrame(functionDeclaration, usedMachineModel);
  }

  @Test
  public void CLangStackFrameConstructorTest() {

    // Normal constructor
    Map<String, SMGRegion> variables = sf.getVariables();
    assert_()
        .withMessage("CLangStackFrame contains no variables after creation")
        .that(variables)
        .isEmpty();
    assertThat(sf.containsVariable("foo")).isFalse();
  }

  @Test
  public void CLangStackFrameAddVariableTest() {
    sf = sf.addStackVariable("fooVar", new SMGRegion(64, "fooVarObject"));
    assert_().withMessage("Added variable is present").that(sf.containsVariable("fooVar")).isTrue();

    Map<String, SMGRegion> variables = sf.getVariables();
    assert_()
        .withMessage("Variables set is nonempty after variable addition")
        .that(variables)
        .hasSize(1);
    SMGObject smg_object = variables.get("fooVar");
    assert_()
        .withMessage("Added variable present in variable map")
        .that(smg_object.getLabel())
        .isEqualTo("fooVarObject");
    assert_()
        .withMessage("Added variable present in variable map")
        .that(smg_object.getSize())
        .isEqualTo(64);

    smg_object = sf.getVariable("fooVar");
    assert_()
        .withMessage("Correct variable is returned: label")
        .that(smg_object.getLabel())
        .isEqualTo("fooVarObject");
    assert_()
        .withMessage("Correct variable is returned: size")
        .that(smg_object.getSize())
        .isEqualTo(64);
  }

  @Test
  public void CLangFrameGetObjectsTest() {
    Set<SMGObject> objects = sf.getAllObjects();
    // Test that there is an return value object at
    assertThat(objects).hasSize(1);

    sf = sf.addStackVariable("fooVar", new SMGRegion(64, "fooVarObject"));
    objects = sf.getAllObjects();
    assertThat(objects).hasSize(2);
  }

  //TODO: Test void functions
  @Test
  public void CLangFrameReturnValueTest() {
    SMGObject retval = sf.getReturnObject();
    assertThat(retval.getSize())
        .isEqualTo(usedMachineModel.getSizeofInBits(CNumericTypes.UNSIGNED_LONG_INT));
  }

  @Test(expected=IllegalArgumentException.class)
  public void CLangStackFrameAddVariableTwiceTest() {
    sf = sf.addStackVariable("fooVar", new SMGRegion(64, "fooVarObject"));
    sf = sf.addStackVariable("fooVar", new SMGRegion(128, "newFooVarObject"));
  }

  @Test(expected=NoSuchElementException.class)
  public void CLangStackFrameMissingVariableTest() {
    assert_()
        .withMessage("Non-added variable is not present")
        .that(sf.containsVariable("fooVaz"))
        .isFalse();

    sf.getVariable("fooVaz");
  }

  @Test
  public void CLangStackFrameFunctionTest() {
    CFunctionDeclaration fd = sf.getFunctionDeclaration();
    assert_().withMessage("Correct function is returned").that(fd.getName()).isEqualTo("foo");
  }
}
