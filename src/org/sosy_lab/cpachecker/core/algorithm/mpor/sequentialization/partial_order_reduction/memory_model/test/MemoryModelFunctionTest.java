// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.test;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableTable;
import java.math.BigInteger;
import java.util.Optional;
import org.junit.Test;
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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryLocation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryLocationFinder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;

public class MemoryModelFunctionTest {

  // Simple Types

  private final CSimpleType INT_TYPE =
      new CSimpleType(false, false, CBasicType.INT, false, false, true, false, false, false, false);

  private final CPointerType INT_POINTER_TYPE = new CPointerType(false, false, INT_TYPE);

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

  // ThreadEdge

  private final ThreadEdge DUMMY_CALL_CONTEXT =
      new ThreadEdge(0, DUMMY_FUNCTION_CALL_EDGE, Optional.empty());

  // Expressions

  private final CIntegerLiteralExpression INT_0 =
      new CIntegerLiteralExpression(FileLocation.DUMMY, INT_TYPE, BigInteger.valueOf(0));

  // Initializers

  private final CInitializer INT_0_INITIALIZER =
      new CInitializerExpression(FileLocation.DUMMY, INT_0);

  // CDeclaration

  private final CVariableDeclaration GLOBAL_X_DECLARATION =
      new CVariableDeclaration(
          FileLocation.DUMMY,
          true,
          CStorageClass.AUTO,
          INT_TYPE,
          "global_X",
          "global_X",
          "global_X",
          INT_0_INITIALIZER);

  private final CParameterDeclaration PARAMETER_DECLARATION_POINTER_P =
      new CParameterDeclaration(FileLocation.DUMMY, INT_POINTER_TYPE, "param_ptr_P");

  private final CParameterDeclaration PARAMETER_DECLARATION_Q =
      new CParameterDeclaration(FileLocation.DUMMY, INT_TYPE, "param_Q");

  // Memory Locations (primitives)

  private final MemoryLocation GLOBAL_X_MEMORY_LOCATION =
      MemoryLocation.of(Optional.empty(), GLOBAL_X_DECLARATION);

  private final MemoryLocation PARAMETER_P_MEMORY_LOCATION =
      MemoryLocation.of(Optional.empty(), PARAMETER_DECLARATION_POINTER_P);

  private final MemoryLocation PARAMETER_Q_MEMORY_LOCATION =
      MemoryLocation.of(Optional.empty(), PARAMETER_DECLARATION_Q);

  // Memory Location IDs

  private final ImmutableMap<MemoryLocation, Integer> memory_location_ids =
      ImmutableMap.<MemoryLocation, Integer>builder()
          .put(GLOBAL_X_MEMORY_LOCATION, 0)
          .put(PARAMETER_P_MEMORY_LOCATION, 1)
          .put(PARAMETER_Q_MEMORY_LOCATION, 2)
          .buildOrThrow();

  @Test
  public void test_pointer_parameter_dereference() {
    // param_ptr_P = &global_X; i.e. pointer parameter assignment
    ImmutableTable<ThreadEdge, CParameterDeclaration, MemoryLocation> pointerParameterAssignments =
        ImmutableTable.<ThreadEdge, CParameterDeclaration, MemoryLocation>builder()
            .put(DUMMY_CALL_CONTEXT, PARAMETER_DECLARATION_POINTER_P, GLOBAL_X_MEMORY_LOCATION)
            .build();
    // *param_ptr_P i.e. pointer parameter dereference
    ImmutableSet<MemoryLocation> pointerDereferences =
        ImmutableSet.<MemoryLocation>builder().add(PARAMETER_P_MEMORY_LOCATION).build();

    // create memory model
    MemoryModel testMemoryModel =
        new MemoryModel(
            memory_location_ids,
            ImmutableSetMultimap.of(),
            pointerParameterAssignments,
            pointerDereferences);

    // find the mem locations associated with deref of 'param_ptr_P' in the given call context
    ImmutableSet<MemoryLocation> memoryLocations =
        MemoryLocationFinder.findMemoryLocationsByPointerDereference(
            PARAMETER_P_MEMORY_LOCATION, Optional.of(DUMMY_CALL_CONTEXT), testMemoryModel);

    // memory location of 'global_X' should be associated with dereference of 'param_ptr_P'
    assertThat(memoryLocations.size() == 1).isTrue();
    assertThat(memoryLocations.contains(GLOBAL_X_MEMORY_LOCATION)).isTrue();
  }
}
