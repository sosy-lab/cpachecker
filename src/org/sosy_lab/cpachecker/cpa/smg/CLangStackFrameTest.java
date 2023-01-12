// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assert_;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMGTest;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGRegion;

public class CLangStackFrameTest {
  private static final MachineModel usedMachineModel = MachineModel.LINUX64;
  private CLangStackFrame sf;

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() {
    sf = new CLangStackFrame(CLangSMGTest.DUMMY_FUNCTION, usedMachineModel);
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

  // TODO: Test void functions
  @Test
  public void CLangFrameReturnValueTest() {
    SMGObject retval = sf.getReturnObject();
    assertThat(retval.getSize())
        .isEqualTo(usedMachineModel.getSizeofInBits(CNumericTypes.UNSIGNED_LONG_INT));
  }

  @Test(expected = IllegalArgumentException.class)
  public void CLangStackFrameAddVariableTwiceTest() {
    sf = sf.addStackVariable("fooVar", new SMGRegion(64, "fooVarObject"));
    sf = sf.addStackVariable("fooVar", new SMGRegion(128, "newFooVarObject"));
  }

  @Test(expected = NoSuchElementException.class)
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
