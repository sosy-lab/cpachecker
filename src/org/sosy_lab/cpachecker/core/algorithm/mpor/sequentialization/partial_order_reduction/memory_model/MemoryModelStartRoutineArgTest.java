// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import java.math.BigInteger;
import java.util.Optional;
import org.junit.Test;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeQualifiers;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.CFAEdgeForThread;

public class MemoryModelStartRoutineArgTest {

  // Simple Types

  private final CSimpleType INT_TYPE =
      new CSimpleType(
          CTypeQualifiers.NONE, CBasicType.INT, false, false, true, false, false, false, false);

  private final CVoidType VOID_TYPE = CVoidType.VOID;

  private final CPointerType VOID_POINTER_TYPE = new CPointerType(CTypeQualifiers.NONE, VOID_TYPE);

  // Expressions

  private final CIntegerLiteralExpression INT_0 =
      new CIntegerLiteralExpression(FileLocation.DUMMY, INT_TYPE, BigInteger.valueOf(0));

  // Initializers

  private final CInitializer INT_0_INITIALIZER =
      new CInitializerExpression(FileLocation.DUMMY, INT_0);

  // CDeclaration

  private final CVariableDeclaration LOCAL_L1_DECLARATION =
      new CVariableDeclaration(
          FileLocation.DUMMY,
          false,
          CStorageClass.AUTO,
          INT_TYPE,
          "local_L1",
          "local_L1",
          "local_L1",
          INT_0_INITIALIZER);

  private final CParameterDeclaration START_ROUTINE_ARG_DECLARATION =
      new CParameterDeclaration(FileLocation.DUMMY, VOID_POINTER_TYPE, "start_routine_arg");

  // CFunctionType

  private final CFunctionType DUMMY_FUNCTION_TYPE =
      new CFunctionType(INT_TYPE, ImmutableList.of(), false);

  // CFunctionDeclarations

  private final CFunctionDeclaration DUMMY_FUNCTION_DECLARATION =
      new CFunctionDeclaration(
          FileLocation.DUMMY,
          DUMMY_FUNCTION_TYPE,
          "dummy_function",
          ImmutableList.of(),
          ImmutableSet.of());

  private final CIdExpression DUMMY_ID_EXPRESSION =
      new CIdExpression(FileLocation.DUMMY, DUMMY_FUNCTION_DECLARATION);

  // CFunctionCallExpression

  private final CFunctionCallExpression DUMMY_FUNCTION_CALL_EXPRESSION =
      new CFunctionCallExpression(
          FileLocation.DUMMY,
          DUMMY_FUNCTION_TYPE,
          DUMMY_ID_EXPRESSION,
          ImmutableList.of(),
          DUMMY_FUNCTION_DECLARATION);

  // CFunctionCallStatement

  private final CFunctionCallStatement DUMMY_FUNCTION_CALL_STATEMENT =
      new CFunctionCallStatement(FileLocation.DUMMY, DUMMY_FUNCTION_CALL_EXPRESSION);

  // CFA Nodes

  private final CFANode DUMMY_PREDECESSOR = CFANode.newDummyCFANode();

  private final CFANode DUMMY_SUCCESSOR = CFANode.newDummyCFANode();

  private final FunctionExitNode DUMMY_FUNCTION_EXIT_NODE =
      new FunctionExitNode(DUMMY_FUNCTION_DECLARATION);

  private final CFunctionEntryNode DUMMY_FUNCTION_ENTRY_NODE =
      new CFunctionEntryNode(
          FileLocation.DUMMY,
          DUMMY_FUNCTION_DECLARATION,
          DUMMY_FUNCTION_EXIT_NODE,
          Optional.empty());

  // CFA Edges

  private final CFunctionSummaryEdge DUMMY_FUNCTION_SUMMARY_EDGE =
      new CFunctionSummaryEdge(
          "",
          FileLocation.DUMMY,
          DUMMY_PREDECESSOR,
          DUMMY_SUCCESSOR,
          DUMMY_FUNCTION_CALL_STATEMENT,
          DUMMY_FUNCTION_ENTRY_NODE);

  private final CFunctionCallEdge DUMMY_FUNCTION_CALL_EDGE =
      new CFunctionCallEdge(
          "",
          FileLocation.DUMMY,
          DUMMY_PREDECESSOR,
          DUMMY_FUNCTION_ENTRY_NODE,
          DUMMY_FUNCTION_CALL_STATEMENT,
          DUMMY_FUNCTION_SUMMARY_EDGE);

  // CFAEdgeForThread

  private final CFAEdgeForThread DUMMY_CALL_CONTEXT =
      new CFAEdgeForThread(0, DUMMY_FUNCTION_CALL_EDGE, Optional.empty());

  // Memory Locations (primitives)

  private final SeqVariableMemoryLocation LOCAL_L1_MEMORY_LOCATION =
      SeqVariableMemoryLocation.of(
          MPOROptions.getDefaultTestInstance(), Optional.empty(), LOCAL_L1_DECLARATION);

  private final SeqParameterMemoryLocation START_ROUTINE_ARG_MEMORY_LOCATION =
      SeqParameterMemoryLocation.of(
          MPOROptions.getDefaultTestInstance(),
          DUMMY_CALL_CONTEXT,
          START_ROUTINE_ARG_DECLARATION,
          0);

  public MemoryModelStartRoutineArgTest() throws InvalidConfigurationException {}

  @Test
  public void test_local_start_routine_arg_implicit_global() {
    // param_ptr_P = &global_X; i.e. pointer parameter assignment
    ImmutableMap<SeqParameterMemoryLocation, SeqMemoryLocation> startRoutineArgAssignments =
        ImmutableMap.<SeqParameterMemoryLocation, SeqMemoryLocation>builder()
            .put(START_ROUTINE_ARG_MEMORY_LOCATION, LOCAL_L1_MEMORY_LOCATION)
            .buildOrThrow();
    ImmutableMap<SeqParameterMemoryLocation, SeqMemoryLocation> pointerParameterAssignments =
        MemoryModelBuilder.getPointerParameterAssignments(startRoutineArgAssignments);

    // check that start_routine_arg assignment is recognized as pointer parameter (void *)
    assertThat(pointerParameterAssignments).hasSize(1);

    // local_L1 is now an implicit global memory location, due to start_routine_arg assignment
    assertThat(LOCAL_L1_MEMORY_LOCATION.isExplicitGlobal()).isFalse();
    assertThat(
            MemoryModelBuilder.isImplicitGlobal(
                LOCAL_L1_MEMORY_LOCATION,
                ImmutableSetMultimap.of(),
                startRoutineArgAssignments,
                pointerParameterAssignments,
                ImmutableSet.of()))
        .isTrue();
    // start_routine_arg is not explicit or implicit global
    assertThat(START_ROUTINE_ARG_MEMORY_LOCATION.isExplicitGlobal()).isFalse();
    assertThat(
            MemoryModelBuilder.isImplicitGlobal(
                START_ROUTINE_ARG_MEMORY_LOCATION,
                ImmutableSetMultimap.of(),
                startRoutineArgAssignments,
                pointerParameterAssignments,
                ImmutableSet.of()))
        .isFalse();
  }
}
