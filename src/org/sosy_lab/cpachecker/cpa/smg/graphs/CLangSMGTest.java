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
package org.sosy_lab.cpachecker.cpa.smg.graphs;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.Map;
import java.util.Set;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg.CLangStackFrame;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGRegion;

public class CLangSMGTest {
  static private final CFunctionType functionType = CFunctionType.functionTypeWithReturnType(CNumericTypes.UNSIGNED_LONG_INT);
  static private final CFunctionDeclaration functionDeclaration = new CFunctionDeclaration(FileLocation.DUMMY, functionType, "foo", ImmutableList.<CParameterDeclaration>of());
  private CLangStackFrame sf;

  static private final LogManager logger = LogManager.createTestLogManager();
  static private final CIdExpression id_expression = new CIdExpression(FileLocation.DUMMY, null, "label", null);

  private static CLangSMG getNewCLangSMG64() {
    return new CLangSMG(MachineModel.LINUX64);
  }

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() {
    sf = new CLangStackFrame(functionDeclaration, MachineModel.LINUX64);
    CLangSMG.setPerformChecks(true, logger);
  }

  @Test
  public void CLangSMGConstructorTest() {
    CLangSMG smg = getNewCLangSMG64();

    assertThat(smg.getStackFrames()).hasSize(0);
    assertThat(smg.getHeapObjects()).hasSize(1);
    assertThat(smg.getGlobalObjects()).hasSize(0);

    SMGRegion obj1 = new SMGRegion(64, "obj1");
    SMGRegion obj2 = new SMGRegion(64, "obj2");

    Integer val1 = Integer.valueOf(1);
    Integer val2 = Integer.valueOf(2);

    SMGEdgePointsTo pt = new SMGEdgePointsTo(val1, obj1, 0);
    SMGEdgeHasValue hv = new SMGEdgeHasValue(CNumericTypes.UNSIGNED_LONG_INT, 0, obj2, val2.intValue());

    smg.addValue(val1);
    smg.addValue(val2);
    smg.addHeapObject(obj1);
    smg.addGlobalObject(obj2);
    smg.addPointsToEdge(pt);
    smg.addHasValueEdge(hv);
    Assert.assertTrue(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg));

    // Copy constructor

    CLangSMG smg_copy = new CLangSMG(smg);
    Assert.assertTrue(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg_copy));

    assertThat(smg_copy.getStackFrames()).hasSize(0);
    assertThat(smg_copy.getHeapObjects()).hasSize(2);
    assertThat(smg_copy.getGlobalObjects()).hasSize(1);

    assertThat(smg_copy.getObjectPointedBy(val1)).isEqualTo(obj1);

    SMGEdgeHasValueFilter filter = SMGEdgeHasValueFilter.objectFilter(obj2);
    assertThat(smg_copy.getHVEdges(filter)).containsExactly(hv);
  }

  @Test
  public void CLangSMGaddHeapObjectTest() {
    CLangSMG smg = getNewCLangSMG64();
    SMGRegion obj1 = new SMGRegion(64, "label");
    SMGRegion obj2 = new SMGRegion(64, "label");

    smg.addHeapObject(obj1);
    Assert.assertTrue(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg));
    Set<SMGObject> heap_objs = smg.getHeapObjects();

    assertThat(heap_objs).contains(obj1);
    assertThat(heap_objs).doesNotContain(obj2);
    assertThat(heap_objs).hasSize(2);

    smg.addHeapObject(obj2);
    Assert.assertTrue(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg));
    heap_objs = smg.getHeapObjects();

    assertThat(heap_objs).contains(obj1);
    assertThat(heap_objs).contains(obj2);
    assertThat(heap_objs).hasSize(3);
  }

  @Test(expected=IllegalArgumentException.class)
  public void CLangSMGaddHeapObjectTwiceTest() {
    CLangSMG smg = getNewCLangSMG64();
    SMGRegion obj = new SMGRegion(64, "label");

    smg.addHeapObject(obj);
    smg.addHeapObject(obj);
  }

  @Test
  public void CLangSMGaddHeapObjectTwiceWithoutChecksTest() {
    CLangSMG.setPerformChecks(false, logger);
    CLangSMG smg = getNewCLangSMG64();
    SMGRegion obj = new SMGRegion(64, "label");

    smg.addHeapObject(obj);
    smg.addHeapObject(obj);
    Assert.assertTrue("Asserting the test finished without exception", true);
  }

  @Test
  public void CLangSMGaddGlobalObjectTest() {
    CLangSMG smg = getNewCLangSMG64();
    SMGRegion obj1 = new SMGRegion(64, "label");
    SMGRegion obj2 = new SMGRegion(64, "another_label");

    smg.addGlobalObject(obj1);
    Map<String, SMGRegion> global_objects = smg.getGlobalObjects();

    Assert.assertTrue(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg));
    assertThat(global_objects).hasSize(1);
    assertThat(global_objects.values()).contains(obj1);

    smg.addGlobalObject(obj2);
    global_objects = smg.getGlobalObjects();

    Assert.assertTrue(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg));
    assertThat(global_objects).hasSize(2);
    assertThat(global_objects.values()).contains(obj1);
    assertThat(global_objects.values()).contains(obj2);
  }

  @Test(expected=IllegalArgumentException.class)
  public void CLangSMGaddGlobalObjectTwiceTest() {
    CLangSMG smg = getNewCLangSMG64();
    SMGRegion obj = new SMGRegion(64, "label");

    smg.addGlobalObject(obj);
    smg.addGlobalObject(obj);
  }

  @Test(expected=IllegalArgumentException.class)
  public void CLangSMGaddGlobalObjectWithSameLabelTest() {
    CLangSMG smg = getNewCLangSMG64();
    SMGRegion obj1 = new SMGRegion(64, "label");
    SMGRegion obj2 = new SMGRegion(128, "label");

    smg.addGlobalObject(obj1);
    smg.addGlobalObject(obj2);
  }

  @Test
  public void CLangSMGaddStackObjectTest() {
    CLangSMG smg = getNewCLangSMG64();
    SMGRegion obj1 = new SMGRegion(64, "label");
    SMGRegion diffobj1 = new SMGRegion(64, "difflabel");

    smg.addStackFrame(sf.getFunctionDeclaration());

    smg.addStackObject(obj1);
    CLangStackFrame current_frame = Iterables.get(smg.getStackFrames(), 0);

    Assert.assertTrue(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg));
    assertThat(current_frame.getVariable("label")).isEqualTo(obj1);
    assertThat(current_frame.getVariables()).hasSize(1);

    smg.addStackObject(diffobj1);
    current_frame = Iterables.get(smg.getStackFrames(), 0);

    Assert.assertTrue(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg));
    assertThat(current_frame.getVariable("label")).isEqualTo(obj1);
    assertThat(current_frame.getVariable("difflabel")).isEqualTo(diffobj1);
    assertThat(current_frame.getVariables()).hasSize(2);
  }

  @Test(expected=IllegalArgumentException.class)
  public void CLangSMGaddStackObjectTwiceTest() {
    CLangSMG smg = getNewCLangSMG64();
    SMGRegion obj1 = new SMGRegion(64, "label");

    smg.addStackFrame(sf.getFunctionDeclaration());

    smg.addStackObject(obj1);
    smg.addStackObject(obj1);
  }

  @Test
  public void CLangSMGgetObjectForVisibleVariableTest() {
    CLangSMG smg = getNewCLangSMG64();
    SMGRegion obj1 = new SMGRegion(64, "label");
    SMGRegion obj2 = new SMGRegion(128, "label");
    SMGRegion obj3 = new SMGRegion(256, "label");

    Assert.assertNull(smg.getObjectForVisibleVariable(id_expression.getName()));
    smg.addGlobalObject(obj3);
    assertThat(smg.getObjectForVisibleVariable(id_expression.getName())).isEqualTo(obj3);

    smg.addStackFrame(sf.getFunctionDeclaration());
    smg.addStackObject(obj1);
    assertThat(smg.getObjectForVisibleVariable(id_expression.getName())).isEqualTo(obj1);

    smg.addStackFrame(sf.getFunctionDeclaration());
    smg.addStackObject(obj2);
    assertThat(smg.getObjectForVisibleVariable(id_expression.getName())).isEqualTo(obj2);
    assertThat(smg.getObjectForVisibleVariable(id_expression.getName())).isNotEqualTo(obj1);

    smg.addStackFrame(sf.getFunctionDeclaration());
    assertThat(smg.getObjectForVisibleVariable(id_expression.getName())).isEqualTo(obj3);
    assertThat(smg.getObjectForVisibleVariable(id_expression.getName())).isNotEqualTo(obj2);
  }

  @Test
  public void CLangSMGgetStackFramesTest() {
    CLangSMG smg = getNewCLangSMG64();
    assertThat(smg.getStackFrames()).hasSize(0);

    smg.addStackFrame(sf.getFunctionDeclaration());
    smg.addStackObject(new SMGRegion(64, "frame1_1"));
    smg.addStackObject(new SMGRegion(64, "frame1_2"));
    smg.addStackObject(new SMGRegion(64, "frame1_3"));
    assertThat(smg.getStackFrames()).hasSize(1);
    assertThat(Iterables.get(smg.getStackFrames(), 0).getVariables()).hasSize(3);

    smg.addStackFrame(sf.getFunctionDeclaration());
    smg.addStackObject(new SMGRegion(64, "frame2_1"));
    smg.addStackObject(new SMGRegion(64, "frame2_2"));
    assertThat(smg.getStackFrames()).hasSize(2);
    assertThat(Iterables.get(smg.getStackFrames(), 1).getVariables()).hasSize(2);

    smg.addStackFrame(sf.getFunctionDeclaration());
    smg.addStackObject(new SMGRegion(64, "frame3_1"));
    assertThat(smg.getStackFrames()).hasSize(3);
    assertThat(Iterables.get(smg.getStackFrames(), 2).getVariables()).hasSize(1);

    smg.addStackFrame(sf.getFunctionDeclaration());
    assertThat(smg.getStackFrames()).hasSize(4);
    assertThat(Iterables.get(smg.getStackFrames(), 3).getVariables()).hasSize(0);
  }

  @Test
  public void CLangSMGgetHeapObjectsTest() {
    CLangSMG smg = getNewCLangSMG64();
    assertThat(smg.getHeapObjects()).hasSize(1);

    smg.addHeapObject(new SMGRegion(64, "heap1"));
    assertThat(smg.getHeapObjects()).hasSize(2);

    smg.addHeapObject(new SMGRegion(64, "heap2"));
    smg.addHeapObject(new SMGRegion(64, "heap3"));
    assertThat(smg.getHeapObjects()).hasSize(4);
  }

  @Test
  public void CLangSMGgetGlobalObjectsTest() {
    CLangSMG smg = getNewCLangSMG64();
    assertThat(smg.getGlobalObjects()).hasSize(0);

    smg.addGlobalObject(new SMGRegion(64, "heap1"));
    assertThat(smg.getGlobalObjects()).hasSize(1);

    smg.addGlobalObject(new SMGRegion(64, "heap2"));
    smg.addGlobalObject(new SMGRegion(64, "heap3"));
    assertThat(smg.getGlobalObjects()).hasSize(3);
  }

  @Test
  public void CLangSMGmemoryLeaksTest() {
    CLangSMG smg = getNewCLangSMG64();

    Assert.assertFalse(smg.hasMemoryLeaks());
    smg.setMemoryLeak();
    Assert.assertTrue(smg.hasMemoryLeaks());
  }

  @Test
  public void consistencyViolationDisjunctnessTest() {
    CLangSMG smg = getNewCLangSMG64();
    SMGRegion obj = new SMGRegion(64, "label");

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
  public void consistencyViolationUnionTest() {
    CLangSMG smg = getNewCLangSMG64();
    Assert.assertTrue(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg));
    SMGRegion stack_obj = new SMGRegion(64, "stack_variable");
    SMGRegion heap_obj = new SMGRegion(64, "heap_object");
    SMGRegion global_obj = new SMGRegion(64, "global_variable");
    SMGRegion dummy_obj = new SMGRegion(64, "dummy_object");

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
  public void consistencyViolationNullTest() {

    CLangSMG smg = getNewCLangSMG64();

    smg = getNewCLangSMG64();
    SMGObject null_object = smg.getHeapObjects().iterator().next();
    Integer some_value = Integer.valueOf(5);
    CType type = mock(CType.class);
    when(type.getCanonicalType()).thenReturn(type);
    SMGEdgeHasValue edge = new SMGEdgeHasValue(type, 0, null_object, some_value);

    smg.addValue(some_value);
    smg.addHasValueEdge(edge);
    Assert.assertFalse(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg));
  }

  /**
   * Identical object in different frames is inconsistent
   */
  @Test
  public void consistencyViolationStackNamespaceTest1() {
    CLangSMG smg = getNewCLangSMG64();
    SMGRegion obj1 = new SMGRegion(64, "label");

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
  public void consistencyViolationStackNamespaceTest2() {
    CLangSMG smg = getNewCLangSMG64();
    SMGRegion obj1 = new SMGRegion(64, "label");
    SMGRegion obj2 = new SMGRegion(128, "label");

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
  public void consistencyViolationStackNamespaceTest3() {
    CLangSMG smg = getNewCLangSMG64();
    SMGRegion obj1 = new SMGRegion(64, "label");
    SMGRegion obj2 = new SMGRegion(128, "label");

    smg.addGlobalObject(obj1);
    smg.addStackFrame(sf.getFunctionDeclaration());
    smg.addStackObject(obj2);
    Assert.assertTrue(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg));
  }
}
