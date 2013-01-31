/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.cpalien;

import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.NoSuchElementException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;

public class CLangSMGTest {

  private CFunctionType functionType = mock(CFunctionType.class);
  private CFunctionDeclaration functionDeclaration;
  private CLangStackFrame sf;
  private LogManager logger = mock(LogManager.class);

  @Before
  public void setUp(){
    functionDeclaration = new CFunctionDeclaration(null, functionType, "foo");
    sf = new CLangStackFrame(functionDeclaration);
  }

  @Test
  public void CLangStackFrameConstructorTest() {

    // Normal constructor
    HashMap<String, SMGObject> variables = sf.getVariables();
    Assert.assertEquals("CLangStackFrame contains no variables after creation",
                        variables.size(), 0);
    Assert.assertFalse(sf.containsVariable("foo"));

    // Copy constructor
    CLangStackFrame sf_copy = new CLangStackFrame(sf);
    variables = sf_copy.getVariables();
    Assert.assertEquals("Empty CLangStackFrame contains no variables after copying",
        variables.size(), 0);
    Assert.assertFalse(sf_copy.containsVariable("foo"));
  }

  @Test
  public void CLangStackFrameAddVariableTest(){
    sf.addStackVariable("fooVar", new SMGObject(8, "fooVarObject"));
    Assert.assertTrue("Added variable is present", sf.containsVariable("fooVar"));

    HashMap<String, SMGObject> variables = sf.getVariables();
    Assert.assertEquals("Variables set is nonempty after variable addition",
                        variables.size(), 1);
    SMGObject smg_object = variables.get("fooVar");
    Assert.assertEquals("Added variable present in variable map", smg_object.getLabel(), "fooVarObject");
    Assert.assertEquals("Added variable present in variable map", smg_object.getSizeInBytes(), 8);

    smg_object = null;
    smg_object = sf.getVariable("fooVar");
    Assert.assertEquals("Correct variable is returned: label", smg_object.getLabel(), "fooVarObject");
    Assert.assertEquals("Correct variable is returned: size", smg_object.getSizeInBytes(), 8);
  }

  @Test(expected=IllegalArgumentException.class)
  public void CLangStackFrameAddVariableTwiceTest(){
    sf.addStackVariable("fooVar", new SMGObject(8, "fooVarObject"));
    sf.addStackVariable("fooVar", new SMGObject(16, "newFooVarObject"));
  }

  @Test(expected=NoSuchElementException.class)
  public void CLangStackFrameMissingVariableTest(){
    Assert.assertFalse("Non-added variable is not present", sf.containsVariable("fooVaz"));

    @SuppressWarnings("unused")
    SMGObject smg_object = sf.getVariable("fooVaz");
  }

  @Test
  public void CLangStackFrameFunctionTest(){
    CFunctionDeclaration fd = sf.getFunctionDeclaration();
    Assert.assertNotNull(fd);
    Assert.assertEquals("Correct function is returned", "foo", fd.getName());
  }

  @Test
  public void ConsistencyViolationDisjunctnessTest() throws IllegalAccessException{
    CLangSMG smg = new CLangSMG();
    SMGObject obj = new SMGObject(8, "label");

    Assert.assertTrue(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg));
    smg.addHeapObject(obj);
    Assert.assertTrue(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg));
    smg.addGlobalObject(obj);
    Assert.assertFalse(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg));

    smg = new CLangSMG();
    smg.addStackFrame(sf.getFunctionDeclaration());

    Assert.assertTrue(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg));
    smg.addHeapObject(obj);
    Assert.assertTrue(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg));
    smg.addStackObject(obj);
    Assert.assertFalse(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg));

    smg = new CLangSMG();
    smg.addStackFrame(sf.getFunctionDeclaration());
    smg.addGlobalObject(obj);
    Assert.assertTrue(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg));
    smg.addStackObject(obj);
    Assert.assertFalse(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg));
  }
}
