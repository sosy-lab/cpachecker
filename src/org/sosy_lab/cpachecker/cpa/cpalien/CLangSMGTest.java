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
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

import com.google.common.collect.ImmutableList;

public class CLangSMGTest {

  private CFunctionType functionType = mock(CFunctionType.class);
  private CFunctionDeclaration functionDeclaration = new CFunctionDeclaration(null, functionType, "foo", ImmutableList.<CParameterDeclaration>of());
  private CLangStackFrame sf =  new CLangStackFrame(functionDeclaration);
  private LogManager logger = mock(LogManager.class);
  private CIdExpression id_expression = new CIdExpression(null, null, "label", null);

  // TODO: Test for 32bit model too

  private static CLangSMG getNewCLangSMG64(){
    return new CLangSMG(MachineModel.LINUX64);
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
  public void CLangSMGConstructorTest(){
    CLangSMG smg = getNewCLangSMG64();

    Assert.assertEquals(0, smg.getStackFrames().size());
    Assert.assertEquals(1, smg.getHeapObjects().size());
    Assert.assertEquals(0, smg.getGlobalObjects().size());

    SMGObject obj1 = new SMGObject(8, "obj1");
    SMGObject obj2 = new SMGObject(8, "obj2");

    Integer val1 = Integer.valueOf(1);
    Integer val2 = Integer.valueOf(2);

    SMGEdgePointsTo pt = new SMGEdgePointsTo(val1, obj1, 0);
    SMGEdgeHasValue hv = new SMGEdgeHasValue(mock(CType.class), 0, obj2, val2.intValue());

    smg.addValue(val1.intValue());
    smg.addValue(val2.intValue());
    smg.addHeapObject(obj1);
    smg.addGlobalObject(obj2);
    smg.addPointsToEdge(pt);
    smg.addHasValueEdge(hv);
    Assert.assertTrue(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg));

    // Copy constructor

    CLangSMG smg_copy = new CLangSMG(smg);
    Assert.assertTrue(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg_copy));

    Assert.assertEquals(0, smg_copy.getStackFrames().size());
    Assert.assertEquals(2, smg_copy.getHeapObjects().size());
    Assert.assertEquals(1, smg_copy.getGlobalObjects().size());

    Assert.assertEquals(obj1, smg_copy.getObjectPointedBy(val1));
    Assert.assertEquals(hv, smg_copy.getValuesForObject(obj2).iterator().next());
  }

  @Test
  public void CLangSMGaddHeapObjectTest(){
    CLangSMG smg = getNewCLangSMG64();
    SMGObject obj1 = new SMGObject(8, "label");
    SMGObject obj2 = new SMGObject(8, "label");

    smg.addHeapObject(obj1);
    Assert.assertTrue(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg));
    Set<SMGObject> heap_objs = smg.getHeapObjects();

    Assert.assertTrue(heap_objs.contains(obj1));
    Assert.assertFalse(heap_objs.contains(obj2));
    Assert.assertTrue(heap_objs.size() == 2);

    smg.addHeapObject(obj2);
    Assert.assertTrue(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg));
    heap_objs = smg.getHeapObjects();

    Assert.assertTrue(heap_objs.contains(obj1));
    Assert.assertTrue(heap_objs.contains(obj2));
    Assert.assertEquals(heap_objs.size(), 3);
  }

  @Test(expected=IllegalArgumentException.class)
  public void CLangSMGaddHeapObjectTwiceTest(){
    CLangSMG smg = getNewCLangSMG64();
    SMGObject obj = new SMGObject(8, "label");

    smg.addHeapObject(obj);
    smg.addHeapObject(obj);
  }

  @Test
  public void CLangSMGaddGlobalObjectTest(){
    CLangSMG smg = getNewCLangSMG64();
    SMGObject obj1 = new SMGObject(8, "label");
    SMGObject obj2 = new SMGObject(8, "another_label");

    smg.addGlobalObject(obj1);
    Map<String, SMGObject> global_objects =smg.getGlobalObjects();

    Assert.assertTrue(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg));
    Assert.assertEquals(global_objects.size(), 1);
    Assert.assertTrue(global_objects.values().contains(obj1));

    smg.addGlobalObject(obj2);
    global_objects =smg.getGlobalObjects();

    Assert.assertTrue(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg));
    Assert.assertEquals(global_objects.size(), 2);
    Assert.assertTrue(global_objects.values().contains(obj1));
    Assert.assertTrue(global_objects.values().contains(obj2));
  }

  @Test(expected=IllegalArgumentException.class)
  public void CLangSMGaddGlobalObjectTwiceTest(){
    CLangSMG smg = getNewCLangSMG64();
    SMGObject obj = new SMGObject(8, "label");

    smg.addGlobalObject(obj);
    smg.addGlobalObject(obj);
  }

  @Test(expected=IllegalArgumentException.class)
  public void CLangSMGaddGlobalObjectWithSameLabelTest(){
    CLangSMG smg = getNewCLangSMG64();
    SMGObject obj1 = new SMGObject(8, "label");
    SMGObject obj2 = new SMGObject(16, "label");

    smg.addGlobalObject(obj1);
    smg.addGlobalObject(obj2);
  }

  @Test
  public void CLangSMGaddStackObjectTest(){
    CLangSMG smg = getNewCLangSMG64();
    SMGObject obj1 = new SMGObject(8, "label");
    SMGObject diffobj1 = new SMGObject(8, "difflabel");

    smg.addStackFrame(sf.getFunctionDeclaration());

    smg.addStackObject(obj1);
    CLangStackFrame current_frame = smg.getStackFrames().peek();

    Assert.assertTrue(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg));
    Assert.assertEquals(current_frame.getVariable("label"), obj1);
    Assert.assertEquals(current_frame.getVariables().size(), 1);

    smg.addStackObject(diffobj1);
    current_frame = smg.getStackFrames().peek();

    Assert.assertTrue(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg));
    Assert.assertEquals(current_frame.getVariable("label"), obj1);
    Assert.assertEquals(current_frame.getVariable("difflabel"), diffobj1);
    Assert.assertEquals(current_frame.getVariables().size(), 2);
  }

  @Test(expected=IllegalArgumentException.class)
  public void CLangSMGaddStackObjectTwiceTest(){
    CLangSMG smg = getNewCLangSMG64();
    SMGObject obj1 = new SMGObject(8, "label");

    smg.addStackFrame(sf.getFunctionDeclaration());

    smg.addStackObject(obj1);
    smg.addStackObject(obj1);
  }

  @Test
  public void CLangSMGgetObjectForVisibleVariableTest(){
    CLangSMG smg = getNewCLangSMG64();
    SMGObject obj1 = new SMGObject(8, "label");
    SMGObject obj2 = new SMGObject(16, "label");
    SMGObject obj3 = new SMGObject(32, "label");

    Assert.assertNull(smg.getObjectForVisibleVariable(id_expression));
    smg.addGlobalObject(obj3);
    Assert.assertEquals(smg.getObjectForVisibleVariable(id_expression), obj3);

    smg.addStackFrame(sf.getFunctionDeclaration());
    smg.addStackObject(obj1);
    Assert.assertEquals(smg.getObjectForVisibleVariable(id_expression), obj1);

    smg.addStackFrame(sf.getFunctionDeclaration());
    smg.addStackObject(obj2);
    Assert.assertEquals(smg.getObjectForVisibleVariable(id_expression), obj2);
    Assert.assertNotEquals(smg.getObjectForVisibleVariable(id_expression), obj1);

    smg.addStackFrame(sf.getFunctionDeclaration());
    Assert.assertEquals(smg.getObjectForVisibleVariable(id_expression), obj3);
    Assert.assertNotEquals(smg.getObjectForVisibleVariable(id_expression), obj2);
  }

  @Test
  public void CLangSMGgetStackFramesTest(){
    CLangSMG smg = getNewCLangSMG64();
    Assert.assertEquals(smg.getStackFrames().size(), 0);

    smg.addStackFrame(sf.getFunctionDeclaration());
    smg.addStackObject(new SMGObject(8, "frame1_1"));
    smg.addStackObject(new SMGObject(8, "frame1_2"));
    smg.addStackObject(new SMGObject(8, "frame1_3"));
    Assert.assertEquals(smg.getStackFrames().size(), 1);
    Assert.assertEquals(smg.getStackFrames().peek().getVariables().size(), 3);

    smg.addStackFrame(sf.getFunctionDeclaration());
    smg.addStackObject(new SMGObject(8, "frame2_1"));
    smg.addStackObject(new SMGObject(8, "frame2_2"));
    Assert.assertEquals(smg.getStackFrames().size(), 2);
    Assert.assertEquals(smg.getStackFrames().peek().getVariables().size(), 2);

    smg.addStackFrame(sf.getFunctionDeclaration());
    smg.addStackObject(new SMGObject(8, "frame3_1"));
    Assert.assertEquals(smg.getStackFrames().size(), 3);
    Assert.assertEquals(smg.getStackFrames().peek().getVariables().size(), 1);

    smg.addStackFrame(sf.getFunctionDeclaration());
    Assert.assertEquals(smg.getStackFrames().size(), 4);
    Assert.assertEquals(smg.getStackFrames().peek().getVariables().size(), 0);
  }

  @Test
  public void CLangSMGgetHeapObjectsTest(){
    CLangSMG smg = getNewCLangSMG64();
    Assert.assertEquals(smg.getHeapObjects().size(), 1);

    smg.addHeapObject(new SMGObject(8, "heap1"));
    Assert.assertEquals(smg.getHeapObjects().size(), 2);

    smg.addHeapObject(new SMGObject(8, "heap2"));
    smg.addHeapObject(new SMGObject(8, "heap3"));
    Assert.assertEquals(smg.getHeapObjects().size(), 4);
  }

  @Test
  public void CLangSMGgetGlobalObjectsTest(){
    CLangSMG smg = getNewCLangSMG64();
    Assert.assertEquals(smg.getGlobalObjects().size(), 0);

    smg.addGlobalObject(new SMGObject(8, "heap1"));
    Assert.assertEquals(smg.getGlobalObjects().size(), 1);

    smg.addGlobalObject(new SMGObject(8, "heap2"));
    smg.addGlobalObject(new SMGObject(8, "heap3"));
    Assert.assertEquals(smg.getGlobalObjects().size(), 3);
  }

  @Test
  public void CLangSMGmemoryLeaksTest(){
    CLangSMG smg = getNewCLangSMG64();

    Assert.assertFalse(smg.hasMemoryLeaks());
    smg.setMemoryLeak();
    Assert.assertTrue(smg.hasMemoryLeaks());
  }

  @Test
  public void ConsistencyViolationDisjunctnessTest(){
    CLangSMG smg = getNewCLangSMG64();
    SMGObject obj = new SMGObject(8, "label");

    Assert.assertTrue(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg));
    smg.addHeapObject(obj);
    Assert.assertTrue(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg));
    smg.addGlobalObject(obj);
    Assert.assertFalse(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg));

    smg = getNewCLangSMG64();
    smg.addStackFrame(sf.getFunctionDeclaration());

    Assert.assertTrue(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg));
    smg.addHeapObject(obj);
    Assert.assertTrue(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg));
    smg.addStackObject(obj);
    Assert.assertFalse(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg));

    smg = getNewCLangSMG64();
    smg.addStackFrame(sf.getFunctionDeclaration());
    smg.addGlobalObject(obj);
    Assert.assertTrue(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg));
    smg.addStackObject(obj);
    Assert.assertFalse(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg));
  }

  @Test
  public void ConsistencyViolationUnionTest(){
    CLangSMG smg = getNewCLangSMG64();
    Assert.assertTrue(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg));
    SMGObject stack_obj = new SMGObject(8, "stack_variable");
    SMGObject heap_obj = new SMGObject(8, "heap_object");
    SMGObject global_obj = new SMGObject(8, "global_variable");
    SMGObject dummy_obj = new SMGObject(8, "dummy_object");

    smg.addStackFrame(sf.getFunctionDeclaration());
    Assert.assertTrue(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg));
    smg.addStackObject(stack_obj);
    Assert.assertTrue(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg));
    smg.addGlobalObject(global_obj);
    Assert.assertTrue(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg));
    smg.addHeapObject(heap_obj);
    Assert.assertTrue(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg));
    smg.addObject(dummy_obj);
    Assert.assertFalse(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg));
  }

  @Test
  public void ConsistencyViolationNullTest(){
    SMGObject fake_null = new SMGObject();

    CLangSMG smg = getNewCLangSMG64();
    smg.addGlobalObject(fake_null);
    Assert.assertFalse(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg));

    smg = getNewCLangSMG64();
    smg.addHeapObject(fake_null);
    Assert.assertFalse(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg));

    smg = getNewCLangSMG64();
    smg.addStackFrame(sf.getFunctionDeclaration());
    smg.addStackObject(fake_null);
    Assert.assertFalse(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg));

    smg = getNewCLangSMG64();
    SMGObject null_object = smg.getHeapObjects().iterator().next();
    Integer some_value = Integer.valueOf(5);
    SMGEdgeHasValue edge = new SMGEdgeHasValue(mock(CType.class), 0, null_object, some_value);

    smg.addValue(some_value);
    smg.addHasValueEdge(edge);
    Assert.assertFalse(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg));
  }

  /**
   * Identical object in different frames is inconsistent
   */
  @Test
  public void ConsistencyViolationStackNamespaceTest1(){
    CLangSMG smg = getNewCLangSMG64();
    SMGObject obj1 = new SMGObject(8, "label");

    smg.addStackFrame(sf.getFunctionDeclaration());
    smg.addStackObject(obj1);
    Assert.assertTrue(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg));
    smg.addStackFrame(sf.getFunctionDeclaration());
    smg.addStackObject(obj1);
    Assert.assertFalse(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg));
  }

  /**
   * Two objects with same label (variable name) in different frames are not inconsistent
   */
  @Test
  public void ConsistencyViolationStackNamespaceTest2(){
    CLangSMG smg = getNewCLangSMG64();
    SMGObject obj1 = new SMGObject(8, "label");
    SMGObject obj2 = new SMGObject(16, "label");

    smg.addStackFrame(sf.getFunctionDeclaration());
    smg.addStackObject(obj1);
    smg.addStackFrame(sf.getFunctionDeclaration());
    smg.addStackObject(obj2);
    Assert.assertTrue(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg));
  }

  /**
   * Two objects with same label (variable name) on stack and global namespace are not inconsistent
   */
  @Test
  public void ConsistencyViolationStackNamespaceTest3(){
    CLangSMG smg = getNewCLangSMG64();
    SMGObject obj1 = new SMGObject(8, "label");
    SMGObject obj2 = new SMGObject(16, "label");

    smg.addGlobalObject(obj1);
    smg.addStackFrame(sf.getFunctionDeclaration());
    smg.addStackObject(obj2);
    Assert.assertTrue(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg));
  }
}
