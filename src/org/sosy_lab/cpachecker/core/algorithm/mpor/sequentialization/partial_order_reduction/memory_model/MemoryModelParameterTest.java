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
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;

public class MemoryModelParameterTest {

  // Simple Types

  private final CSimpleType INT_TYPE =
      new CSimpleType(false, false, CBasicType.INT, false, false, true, false, false, false, false);

  private final CPointerType INT_POINTER_TYPE = new CPointerType(false, false, INT_TYPE);

  // Expressions

  private final CIntegerLiteralExpression INT_0 =
      new CIntegerLiteralExpression(FileLocation.DUMMY, INT_TYPE, BigInteger.valueOf(0));

  // Initializers

  private final CInitializer INT_0_INITIALIZER =
      new CInitializerExpression(FileLocation.DUMMY, INT_0);

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

  // CDeclaration

  private final CVariableDeclaration GLOBAL_POINTER_A_DECLARATION =
      new CVariableDeclaration(
          FileLocation.DUMMY,
          true,
          CStorageClass.AUTO,
          INT_POINTER_TYPE,
          "global_ptr_A",
          "global_ptr_A",
          "global_ptr_A",
          INT_0_INITIALIZER);

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

  private final CVariableDeclaration LOCAL_POINTER_C_DECLARATION =
      new CVariableDeclaration(
          FileLocation.DUMMY,
          false,
          CStorageClass.AUTO,
          INT_POINTER_TYPE,
          "local_ptr_C",
          "local_ptr_C",
          "local_ptr_C",
          INT_0_INITIALIZER);

  private final CVariableDeclaration LOCAL_Z_DECLARATION =
      new CVariableDeclaration(
          FileLocation.DUMMY,
          false,
          CStorageClass.AUTO,
          INT_TYPE,
          "local_Z",
          "local_Z",
          "local_Z",
          INT_0_INITIALIZER);

  private final CParameterDeclaration PARAMETER_DECLARATION_POINTER_P =
      new CParameterDeclaration(FileLocation.DUMMY, INT_POINTER_TYPE, "param_ptr_P");

  private final CParameterDeclaration PARAMETER_DECLARATION_Q =
      new CParameterDeclaration(FileLocation.DUMMY, INT_TYPE, "param_Q");

  private final CParameterDeclaration PARAMETER_DECLARATION_POINTER_R =
      new CParameterDeclaration(FileLocation.DUMMY, INT_POINTER_TYPE, "param_ptr_R");

  // Memory Locations (primitives)

  private final MemoryLocation GLOBAL_POINTER_A_MEMORY_LOCATION =
      MemoryLocation.of(
          MPOROptions.defaultTestInstance(), Optional.empty(), GLOBAL_POINTER_A_DECLARATION);

  private final MemoryLocation GLOBAL_X_MEMORY_LOCATION =
      MemoryLocation.of(MPOROptions.defaultTestInstance(), Optional.empty(), GLOBAL_X_DECLARATION);

  private final MemoryLocation LOCAL_POINTER_C_MEMORY_LOCATION =
      MemoryLocation.of(
          MPOROptions.defaultTestInstance(), Optional.empty(), LOCAL_POINTER_C_DECLARATION);

  private final MemoryLocation LOCAL_Z_MEMORY_LOCATION =
      MemoryLocation.of(MPOROptions.defaultTestInstance(), Optional.empty(), LOCAL_Z_DECLARATION);

  private final MemoryLocation PARAMETER_POINTER_P_MEMORY_LOCATION =
      MemoryLocation.of(
          MPOROptions.defaultTestInstance(),
          Optional.of(DUMMY_CALL_CONTEXT),
          PARAMETER_DECLARATION_POINTER_P);

  private final MemoryLocation PARAMETER_Q_MEMORY_LOCATION =
      MemoryLocation.of(
          MPOROptions.defaultTestInstance(),
          Optional.of(DUMMY_CALL_CONTEXT),
          PARAMETER_DECLARATION_Q);

  private final MemoryLocation PARAMETER_POINTER_R_MEMORY_LOCATION =
      MemoryLocation.of(
          MPOROptions.defaultTestInstance(),
          Optional.of(DUMMY_CALL_CONTEXT),
          PARAMETER_DECLARATION_POINTER_R);

  @Test
  public void test_pointer_parameter_dereference() {
    // param_ptr_P = &global_X; i.e. pointer parameter assignment
    ImmutableMap<MemoryLocation, MemoryLocation> parameterAssignments =
        ImmutableMap.<MemoryLocation, MemoryLocation>builder()
            .put(PARAMETER_POINTER_P_MEMORY_LOCATION, GLOBAL_X_MEMORY_LOCATION)
            .buildOrThrow();
    ImmutableMap<MemoryLocation, MemoryLocation> pointerParameterAssignments =
        MemoryModelBuilder.extractPointerParameters(parameterAssignments);

    // find the mem locations associated with deref of 'param_ptr_P' in the given call context
    ImmutableSet<MemoryLocation> memoryLocations =
        MemoryLocationFinder.findMemoryLocationsByPointerDereference(
            PARAMETER_POINTER_P_MEMORY_LOCATION,
            ImmutableSetMultimap.of(),
            ImmutableMap.of(),
            pointerParameterAssignments);

    // memory location of 'global_X' should be associated with dereference of 'param_ptr_P'
    assertThat(memoryLocations.size() == 1).isTrue();
    assertThat(memoryLocations.contains(GLOBAL_X_MEMORY_LOCATION)).isTrue();
  }

  @Test
  public void test_transitive_pointer_parameter_dereference() {
    // param_ptr_P = local_ptr_C; i.e. transitive pointer parameter assignment
    ImmutableMap<MemoryLocation, MemoryLocation> parameterAssignments =
        ImmutableMap.<MemoryLocation, MemoryLocation>builder()
            .put(PARAMETER_POINTER_P_MEMORY_LOCATION, LOCAL_POINTER_C_MEMORY_LOCATION)
            .buildOrThrow();
    ImmutableMap<MemoryLocation, MemoryLocation> pointerParameterAssignments =
        MemoryModelBuilder.extractPointerParameters(parameterAssignments);

    // local_ptr_C = &global_X; i.e. pointer assignment
    ImmutableSetMultimap<MemoryLocation, MemoryLocation> pointerAssignments =
        ImmutableSetMultimap.<MemoryLocation, MemoryLocation>builder()
            .put(LOCAL_POINTER_C_MEMORY_LOCATION, GLOBAL_X_MEMORY_LOCATION)
            .build();

    // find the mem locations associated with deref of 'param_ptr_P' in the given call context
    ImmutableSet<MemoryLocation> memoryLocations =
        MemoryLocationFinder.findMemoryLocationsByPointerDereference(
            PARAMETER_POINTER_P_MEMORY_LOCATION,
            pointerAssignments,
            ImmutableMap.of(),
            pointerParameterAssignments);

    // memory location of 'global_X' should be associated with dereference of 'param_ptr_P'
    assertThat(memoryLocations.size() == 1).isTrue();
    assertThat(memoryLocations.contains(GLOBAL_X_MEMORY_LOCATION)).isTrue();
  }

  @Test
  public void test_parameter_implicit_global() {
    // param_Q = local_Z; i.e. non-pointer parameter assignment with local variable
    ImmutableMap<MemoryLocation, MemoryLocation> parameterAssignments =
        ImmutableMap.<MemoryLocation, MemoryLocation>builder()
            .put(PARAMETER_Q_MEMORY_LOCATION, LOCAL_Z_MEMORY_LOCATION)
            .buildOrThrow();
    ImmutableMap<MemoryLocation, MemoryLocation> pointerParameterAssignments =
        MemoryModelBuilder.extractPointerParameters(parameterAssignments);

    // global_ptr_A = &param_Q; i.e. pointer assignment
    ImmutableSetMultimap<MemoryLocation, MemoryLocation> pointerAssignments =
        ImmutableSetMultimap.<MemoryLocation, MemoryLocation>builder()
            .put(GLOBAL_POINTER_A_MEMORY_LOCATION, PARAMETER_Q_MEMORY_LOCATION)
            .build();

    // assert that param_Q is now an implicit global memory location, but local_Z is not
    assertThat(PARAMETER_Q_MEMORY_LOCATION.isExplicitGlobal()).isFalse();
    assertThat(LOCAL_Z_MEMORY_LOCATION.isExplicitGlobal()).isFalse();
    assertThat(GLOBAL_POINTER_A_MEMORY_LOCATION.isExplicitGlobal()).isTrue();
    assertThat(
            MemoryModelBuilder.isImplicitGlobal(
                LOCAL_Z_MEMORY_LOCATION,
                pointerAssignments,
                ImmutableMap.of(),
                pointerParameterAssignments,
                ImmutableSet.of()))
        .isFalse();
    assertThat(
            MemoryModelBuilder.isImplicitGlobal(
                PARAMETER_Q_MEMORY_LOCATION,
                pointerAssignments,
                ImmutableMap.of(),
                parameterAssignments,
                ImmutableSet.of()))
        .isTrue();
  }

  @Test
  public void test_transitive_pointer_parameter_assignments() {
    // param_ptr_R = &local_Z; and param_ptr_P = param_ptr_R;
    // i.e. transitive pointer parameter assignments
    ImmutableMap<MemoryLocation, MemoryLocation> parameterAssignments =
        ImmutableMap.<MemoryLocation, MemoryLocation>builder()
            .put(PARAMETER_POINTER_R_MEMORY_LOCATION, LOCAL_Z_MEMORY_LOCATION)
            .put(PARAMETER_POINTER_P_MEMORY_LOCATION, PARAMETER_POINTER_R_MEMORY_LOCATION)
            .buildOrThrow();
    ImmutableMap<MemoryLocation, MemoryLocation> pointerParameterAssignments =
        MemoryModelBuilder.extractPointerParameters(parameterAssignments);

    // all are not explicit global memory locations
    assertThat(PARAMETER_POINTER_R_MEMORY_LOCATION.isExplicitGlobal()).isFalse();
    assertThat(PARAMETER_POINTER_R_MEMORY_LOCATION.isExplicitGlobal()).isFalse();
    assertThat(LOCAL_Z_MEMORY_LOCATION.isExplicitGlobal()).isFalse();

    // find the mem locations associated with deref of 'param_ptr_P' in the given call context
    ImmutableSet<MemoryLocation> memoryLocations =
        MemoryLocationFinder.findMemoryLocationsByPointerDereference(
            PARAMETER_POINTER_P_MEMORY_LOCATION,
            ImmutableSetMultimap.of(),
            ImmutableMap.of(),
            pointerParameterAssignments);

    assertThat(memoryLocations.size() == 1).isTrue();
    assertThat(memoryLocations.contains(LOCAL_Z_MEMORY_LOCATION)).isTrue();
  }
}
