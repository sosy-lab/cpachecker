// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.graphs;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg.CLangStackFrame;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValueFilter.SMGEdgeHasValueFilterByObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGRegion;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownExpValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;
import org.sosy_lab.cpachecker.cpa.smg.util.PersistentSet;

public class CLangSMGTest {
  private static final CFunctionType functionType =
      CFunctionType.functionTypeWithReturnType(CNumericTypes.UNSIGNED_LONG_INT);
  public static final CFunctionDeclaration DUMMY_FUNCTION =
      new CFunctionDeclaration(
          FileLocation.DUMMY, functionType, "foo", ImmutableList.of(), ImmutableSet.of());
  private CLangStackFrame sf;

  private static final LogManager logger = LogManager.createTestLogManager();
  private static final CIdExpression id_expression =
      new CIdExpression(FileLocation.DUMMY, null, "label", null);

  private static CLangSMG getNewCLangSMG64() {
    return new CLangSMG(MachineModel.LINUX64);
  }

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() {
    sf = new CLangStackFrame(DUMMY_FUNCTION, MachineModel.LINUX64);
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

    SMGValue val1 = SMGKnownExpValue.valueOf(1);
    SMGValue val2 = SMGKnownExpValue.valueOf(2);

    SMGEdgePointsTo pt = new SMGEdgePointsTo(val1, obj1, 0);
    SMGEdgeHasValue hv =
        new SMGEdgeHasValue(
            smg.getMachineModel().getSizeofInBits(CNumericTypes.UNSIGNED_LONG_INT), 0, obj2, val2);

    smg.addValue(val1);
    smg.addValue(val2);
    smg.addHeapObject(obj1);
    smg.addGlobalObject(obj2);
    smg.addPointsToEdge(pt);
    smg.addHasValueEdge(hv);
    assertThat(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg)).isTrue();

    // Copy constructor

    UnmodifiableCLangSMG smg_copy = smg.copyOf();
    assertThat(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg_copy)).isTrue();

    assertThat(smg_copy.getStackFrames()).hasSize(0);
    assertThat(smg_copy.getHeapObjects()).hasSize(2);
    assertThat(smg_copy.getGlobalObjects()).hasSize(1);

    assertThat(smg_copy.getObjectPointedBy(val1)).isEqualTo(obj1);

    SMGEdgeHasValueFilterByObject filter = SMGEdgeHasValueFilter.objectFilter(obj2);
    assertThat(smg_copy.getHVEdges(filter)).containsExactly(hv);
  }

  @Test
  public void CLangSMGaddHeapObjectTest() {
    CLangSMG smg = getNewCLangSMG64();
    SMGRegion obj1 = new SMGRegion(64, "label");
    SMGRegion obj2 = new SMGRegion(64, "label");

    smg.addHeapObject(obj1);
    assertThat(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg)).isTrue();
    PersistentSet<SMGObject> heap_objs = smg.getHeapObjects();

    assertThat(heap_objs).contains(obj1);
    assertThat(heap_objs).doesNotContain(obj2);
    assertThat(heap_objs).hasSize(2);

    smg.addHeapObject(obj2);
    assertThat(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg)).isTrue();
    heap_objs = smg.getHeapObjects();

    assertThat(heap_objs).contains(obj1);
    assertThat(heap_objs).contains(obj2);
    assertThat(heap_objs).hasSize(3);
  }

  @Test(expected = IllegalArgumentException.class)
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
    // test just checks that no exception occurs
  }

  @Test
  public void CLangSMGaddGlobalObjectTest() {
    CLangSMG smg = getNewCLangSMG64();
    SMGRegion obj1 = new SMGRegion(64, "label");
    SMGRegion obj2 = new SMGRegion(64, "another_label");

    smg.addGlobalObject(obj1);
    Map<String, SMGRegion> global_objects = smg.getGlobalObjects();

    assertThat(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg)).isTrue();
    assertThat(global_objects).hasSize(1);
    assertThat(global_objects.values()).contains(obj1);

    smg.addGlobalObject(obj2);
    global_objects = smg.getGlobalObjects();

    assertThat(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg)).isTrue();
    assertThat(global_objects).hasSize(2);
    assertThat(global_objects.values()).contains(obj1);
    assertThat(global_objects.values()).contains(obj2);
  }

  @Test(expected = IllegalArgumentException.class)
  public void CLangSMGaddGlobalObjectTwiceTest() {
    CLangSMG smg = getNewCLangSMG64();
    SMGRegion obj = new SMGRegion(64, "label");

    smg.addGlobalObject(obj);
    smg.addGlobalObject(obj);
  }

  @Test(expected = IllegalArgumentException.class)
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

    assertThat(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg)).isTrue();
    assertThat(current_frame.getVariable("label")).isEqualTo(obj1);
    assertThat(current_frame.getVariables()).hasSize(1);

    smg.addStackObject(diffobj1);
    current_frame = Iterables.get(smg.getStackFrames(), 0);

    assertThat(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg)).isTrue();
    assertThat(current_frame.getVariable("label")).isEqualTo(obj1);
    assertThat(current_frame.getVariable("difflabel")).isEqualTo(diffobj1);
    assertThat(current_frame.getVariables()).hasSize(2);
  }

  @Test(expected = IllegalArgumentException.class)
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

    assertThat(smg.getObjectForVisibleVariable(id_expression.getName())).isNull();
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
  public void consistencyViolationDisjunctnessTest() {
    CLangSMG smg = getNewCLangSMG64();
    SMGRegion obj = new SMGRegion(64, "label");

    assertThat(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg)).isTrue();
    smg.addHeapObject(obj);
    assertThat(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg)).isTrue();
    smg.addGlobalObject(obj);
    assertThat(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg)).isFalse();

    smg = getNewCLangSMG64();
    smg.addStackFrame(sf.getFunctionDeclaration());

    assertThat(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg)).isTrue();
    smg.addHeapObject(obj);
    assertThat(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg)).isTrue();
    smg.addStackObject(obj);
    assertThat(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg)).isFalse();

    smg = getNewCLangSMG64();
    smg.addStackFrame(sf.getFunctionDeclaration());
    smg.addGlobalObject(obj);
    assertThat(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg)).isTrue();
    smg.addStackObject(obj);
    assertThat(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg)).isFalse();
  }

  @Test
  public void consistencyViolationUnionTest() {
    CLangSMG smg = getNewCLangSMG64();
    assertThat(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg)).isTrue();
    SMGRegion stack_obj = new SMGRegion(64, "stack_variable");
    SMGRegion heap_obj = new SMGRegion(64, "heap_object");
    SMGRegion global_obj = new SMGRegion(64, "global_variable");
    SMGRegion dummy_obj = new SMGRegion(64, "dummy_object");

    smg.addStackFrame(sf.getFunctionDeclaration());
    assertThat(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg)).isTrue();
    smg.addStackObject(stack_obj);
    assertThat(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg)).isTrue();
    smg.addGlobalObject(global_obj);
    assertThat(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg)).isTrue();
    smg.addHeapObject(heap_obj);
    assertThat(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg)).isTrue();
    smg.addObject(dummy_obj);
    assertThat(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg)).isFalse();
  }

  @Test
  public void consistencyViolationNullTest() {

    CLangSMG smg = getNewCLangSMG64();
    SMGObject null_object = smg.getHeapObjects().iterator().next();
    SMGValue some_value = SMGKnownExpValue.valueOf(5);
    CType type = mock(CType.class);
    when(type.getCanonicalType()).thenReturn(type);
    SMGEdgeHasValue edge = new SMGEdgeHasValue(32, 0, null_object, some_value);
    smg.addValue(some_value);
    smg.addHasValueEdge(edge);
    assertThat(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg)).isFalse();
  }

  /** Identical object in different frames is inconsistent */
  @Test
  public void consistencyViolationStackNamespaceTest1() {
    CLangSMG smg = getNewCLangSMG64();
    SMGRegion obj1 = new SMGRegion(64, "label");

    smg.addStackFrame(sf.getFunctionDeclaration());
    smg.addStackObject(obj1);
    assertThat(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg)).isTrue();
    smg.addStackFrame(sf.getFunctionDeclaration());
    smg.addStackObject(obj1);
    assertThat(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg)).isFalse();
  }

  /** Two objects with same label (variable name) in different frames are not inconsistent */
  @Test
  public void consistencyViolationStackNamespaceTest2() {
    CLangSMG smg = getNewCLangSMG64();
    SMGRegion obj1 = new SMGRegion(64, "label");
    SMGRegion obj2 = new SMGRegion(128, "label");

    smg.addStackFrame(sf.getFunctionDeclaration());
    smg.addStackObject(obj1);
    smg.addStackFrame(sf.getFunctionDeclaration());
    smg.addStackObject(obj2);
    assertThat(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg)).isTrue();
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
    assertThat(CLangSMGConsistencyVerifier.verifyCLangSMG(logger, smg)).isTrue();
  }
}
