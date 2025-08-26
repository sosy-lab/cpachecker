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
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryLocation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryLocationFinder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryModelBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;

public class MemoryModelStructParameterTest {

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

  private final CInitializerList EMPTY_INITIALIZER_LIST =
      new CInitializerList(FileLocation.DUMMY, ImmutableList.of());

  // Composite Type Member Declarations (inner struct)

  private final CCompositeTypeMemberDeclaration INNER_STRUCT_MEMBER_DECLARATION =
      new CCompositeTypeMemberDeclaration(INT_TYPE, "inner_member");

  // Complex Types (inner struct)

  private final CComplexType INNER_STRUCT_COMPLEX_TYPE =
      new CCompositeType(
          false,
          false,
          ComplexTypeKind.STRUCT,
          ImmutableList.of(INNER_STRUCT_MEMBER_DECLARATION),
          "inner_struct_complex",
          "inner_struct_complex");

  private final CElaboratedType INNER_STRUCT_ELABORATED_TYPE =
      new CElaboratedType(
          false,
          false,
          ComplexTypeKind.STRUCT,
          "inner_struct_elaborated",
          "inner_struct_elaborated",
          INNER_STRUCT_COMPLEX_TYPE);

  private final CTypedefType INNER_STRUCT_TYPE =
      new CTypedefType(false, false, "Inner", INNER_STRUCT_ELABORATED_TYPE);

  // Composite Type Member Declarations (outer struct)

  private final CCompositeTypeMemberDeclaration OUTER_STRUCT_MEMBER_DECLARATION =
      new CCompositeTypeMemberDeclaration(INT_TYPE, "outer_member");

  private final CCompositeTypeMemberDeclaration INNER_STRUCT_DECLARATION =
      new CCompositeTypeMemberDeclaration(INNER_STRUCT_TYPE, "inner_struct");

  // Complex Types (outer struct)

  private final CComplexType OUTER_STRUCT_COMPLEX_TYPE =
      new CCompositeType(
          false,
          false,
          ComplexTypeKind.STRUCT,
          ImmutableList.of(OUTER_STRUCT_MEMBER_DECLARATION, INNER_STRUCT_DECLARATION),
          "outer_struct_complex",
          "outer_struct_complex");

  private final CElaboratedType OUTER_STRUCT_ELABORATED_TYPE =
      new CElaboratedType(
          false,
          false,
          ComplexTypeKind.STRUCT,
          "outer_struct_elaborated",
          "outer_struct_elaborated",
          OUTER_STRUCT_COMPLEX_TYPE);

  private final CTypedefType OUTER_STRUCT_TYPE =
      new CTypedefType(false, false, "Outer", OUTER_STRUCT_ELABORATED_TYPE);

  // Pointer Types to Complex Types

  private final CPointerType OUTER_STRUCT_POINTER_TYPE =
      new CPointerType(false, false, OUTER_STRUCT_COMPLEX_TYPE);

  private final CPointerType INNER_STRUCT_POINTER_TYPE =
      new CPointerType(false, false, INNER_STRUCT_COMPLEX_TYPE);

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

  private final CVariableDeclaration OUTER_STRUCT_DECLARATION =
      new CVariableDeclaration(
          FileLocation.DUMMY,
          true,
          CStorageClass.AUTO,
          OUTER_STRUCT_TYPE,
          "outer_struct",
          "outer_struct",
          "outer_struct",
          EMPTY_INITIALIZER_LIST);

  private final CParameterDeclaration PARAMETER_DECLARATION_POINTER_OUTER_STRUCT =
      new CParameterDeclaration(
          FileLocation.DUMMY, OUTER_STRUCT_POINTER_TYPE, "param_ptr_outer_struct");

  private final CParameterDeclaration PARAMETER_DECLARATION_POINTER_OUTER_STRUCT_MEMBER =
      new CParameterDeclaration(
          FileLocation.DUMMY, INT_POINTER_TYPE, "param_ptr_outer_struct_member");

  private final CParameterDeclaration PARAMETER_DECLARATION_POINTER_INNER_STRUCT =
      new CParameterDeclaration(
          FileLocation.DUMMY, INNER_STRUCT_POINTER_TYPE, "param_ptr_inner_struct");

  private final CParameterDeclaration PARAMETER_DECLARATION_POINTER_INNER_STRUCT_MEMBER =
      new CParameterDeclaration(
          FileLocation.DUMMY, INT_POINTER_TYPE, "param_ptr_inner_struct_member");

  // Memory Locations (structs)

  private final MemoryLocation OUTER_STRUCT_MEMORY_LOCATION =
      MemoryLocation.of(Optional.empty(), OUTER_STRUCT_DECLARATION);

  private final MemoryLocation OUTER_STRUCT_MEMBER_MEMORY_LOCATION =
      MemoryLocation.of(
          Optional.empty(), OUTER_STRUCT_DECLARATION, OUTER_STRUCT_MEMBER_DECLARATION);

  private final MemoryLocation INNER_STRUCT_MEMORY_LOCATION =
      MemoryLocation.of(Optional.empty(), OUTER_STRUCT_DECLARATION, INNER_STRUCT_DECLARATION);

  private final MemoryLocation INNER_STRUCT_MEMBER_MEMORY_LOCATION =
      MemoryLocation.of(
          Optional.empty(), OUTER_STRUCT_DECLARATION, INNER_STRUCT_MEMBER_DECLARATION);

  private final MemoryLocation PARAMETER_POINTER_OUTER_STRUCT_MEMORY_LOCATION =
      MemoryLocation.of(Optional.empty(), PARAMETER_DECLARATION_POINTER_OUTER_STRUCT);

  private final MemoryLocation PARAMETER_POINTER_OUTER_STRUCT_MEMBER_MEMORY_LOCATION =
      MemoryLocation.of(Optional.empty(), PARAMETER_DECLARATION_POINTER_OUTER_STRUCT_MEMBER);

  private final MemoryLocation PARAMETER_POINTER_INNER_STRUCT_MEMORY_LOCATION =
      MemoryLocation.of(Optional.empty(), PARAMETER_DECLARATION_POINTER_INNER_STRUCT);

  private final MemoryLocation PARAMETER_POINTER_INNER_STRUCT_MEMBER_MEMORY_LOCATION =
      MemoryLocation.of(Optional.empty(), PARAMETER_DECLARATION_POINTER_INNER_STRUCT_MEMBER);

  // Memory Location IDs

  private final ImmutableMap<MemoryLocation, Integer> MEMORY_LOCATION_IDS =
      ImmutableMap.<MemoryLocation, Integer>builder()
          .put(OUTER_STRUCT_MEMORY_LOCATION, 0)
          .put(OUTER_STRUCT_MEMBER_MEMORY_LOCATION, 1)
          .put(INNER_STRUCT_MEMORY_LOCATION, 2)
          .put(INNER_STRUCT_MEMBER_MEMORY_LOCATION, 3)
          .put(PARAMETER_POINTER_OUTER_STRUCT_MEMORY_LOCATION, 4)
          .put(PARAMETER_POINTER_OUTER_STRUCT_MEMBER_MEMORY_LOCATION, 5)
          .put(PARAMETER_POINTER_INNER_STRUCT_MEMORY_LOCATION, 6)
          .put(PARAMETER_POINTER_INNER_STRUCT_MEMBER_MEMORY_LOCATION, 7)
          .buildOrThrow();

  @Test
  public void test_outer_struct_pointer_parameter_dereference() {
    // param_ptr_outer_struct = &outer_struct; i.e. pointer parameter assignment
    ImmutableTable<ThreadEdge, CParameterDeclaration, MemoryLocation> parameterAssignments =
        ImmutableTable.<ThreadEdge, CParameterDeclaration, MemoryLocation>builder()
            .put(
                DUMMY_CALL_CONTEXT,
                PARAMETER_DECLARATION_POINTER_OUTER_STRUCT,
                OUTER_STRUCT_MEMORY_LOCATION)
            .build();
    ImmutableTable<ThreadEdge, CParameterDeclaration, MemoryLocation> pointerParameterAssignments =
        MemoryModelBuilder.extractPointerParameters(parameterAssignments);
    // *param_ptr_outer_struct i.e. pointer parameter dereference
    ImmutableSet<MemoryLocation> pointerDereferences =
        ImmutableSet.<MemoryLocation>builder()
            .add(PARAMETER_POINTER_OUTER_STRUCT_MEMORY_LOCATION)
            .build();

    // create memory model
    MemoryModel testMemoryModel =
        new MemoryModel(
            MEMORY_LOCATION_IDS,
            ImmutableSetMultimap.of(),
            parameterAssignments,
            pointerParameterAssignments,
            pointerDereferences);

    // find the mem locations associated with deref of 'param_ptr_outer_struct'
    ImmutableSet<MemoryLocation> memoryLocations =
        MemoryLocationFinder.findMemoryLocationsByPointerDereference(
            PARAMETER_POINTER_OUTER_STRUCT_MEMORY_LOCATION,
            Optional.of(DUMMY_CALL_CONTEXT),
            testMemoryModel);

    // memory location of 'outer_struct' should be associated with deref of 'param_ptr_outer_struct'
    assertThat(memoryLocations.size() == 1).isTrue();
    assertThat(memoryLocations.contains(OUTER_STRUCT_MEMORY_LOCATION)).isTrue();
  }
}
