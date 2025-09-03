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
import org.sosy_lab.cpachecker.cfa.types.c.CTypeQualifiers;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;

public class MemoryModelStructParameterTest {

  // Simple Types

  private final CSimpleType INT_TYPE =
      new CSimpleType(
          CTypeQualifiers.NONE, CBasicType.INT, false, false, true, false, false, false, false);

  private final CPointerType INT_POINTER_TYPE = new CPointerType(CTypeQualifiers.NONE, INT_TYPE);

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

  private final CCompositeTypeMemberDeclaration INNER_STRUCT_POINTER_MEMBER_DECLARATION =
      new CCompositeTypeMemberDeclaration(INT_POINTER_TYPE, "inner_member_ptr");

  // Complex Types (inner struct)

  private final CComplexType INNER_STRUCT_COMPLEX_TYPE =
      new CCompositeType(
          CTypeQualifiers.NONE,
          ComplexTypeKind.STRUCT,
          ImmutableList.of(
              INNER_STRUCT_MEMBER_DECLARATION, INNER_STRUCT_POINTER_MEMBER_DECLARATION),
          "inner_struct_complex",
          "inner_struct_complex");

  private final CElaboratedType INNER_STRUCT_ELABORATED_TYPE =
      new CElaboratedType(
          CTypeQualifiers.NONE,
          ComplexTypeKind.STRUCT,
          "inner_struct_elaborated",
          "inner_struct_elaborated",
          INNER_STRUCT_COMPLEX_TYPE);

  private final CTypedefType INNER_STRUCT_TYPE =
      new CTypedefType(CTypeQualifiers.NONE, "Inner", INNER_STRUCT_ELABORATED_TYPE);

  // Composite Type Member Declarations (outer struct)

  private final CCompositeTypeMemberDeclaration OUTER_STRUCT_MEMBER_DECLARATION =
      new CCompositeTypeMemberDeclaration(INT_TYPE, "outer_member");

  private final CCompositeTypeMemberDeclaration OUTER_STRUCT_POINTER_MEMBER_DECLARATION =
      new CCompositeTypeMemberDeclaration(INT_POINTER_TYPE, "outer_member_ptr");

  private final CCompositeTypeMemberDeclaration INNER_STRUCT_DECLARATION =
      new CCompositeTypeMemberDeclaration(INNER_STRUCT_TYPE, "inner_struct");

  // Complex Types (outer struct)

  private final CComplexType OUTER_STRUCT_COMPLEX_TYPE =
      new CCompositeType(
          CTypeQualifiers.NONE,
          ComplexTypeKind.STRUCT,
          ImmutableList.of(
              OUTER_STRUCT_MEMBER_DECLARATION,
              OUTER_STRUCT_POINTER_MEMBER_DECLARATION,
              INNER_STRUCT_DECLARATION),
          "outer_struct_complex",
          "outer_struct_complex");

  private final CElaboratedType OUTER_STRUCT_ELABORATED_TYPE =
      new CElaboratedType(
          CTypeQualifiers.NONE,
          ComplexTypeKind.STRUCT,
          "outer_struct_elaborated",
          "outer_struct_elaborated",
          OUTER_STRUCT_COMPLEX_TYPE);

  private final CTypedefType OUTER_STRUCT_TYPE =
      new CTypedefType(CTypeQualifiers.NONE, "Outer", OUTER_STRUCT_ELABORATED_TYPE);

  // Pointer Types to Complex Types

  private final CPointerType OUTER_STRUCT_POINTER_TYPE =
      new CPointerType(CTypeQualifiers.NONE, OUTER_STRUCT_COMPLEX_TYPE);

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

  private final CVariableDeclaration GLOBAL_G1_DECLARATION =
      new CVariableDeclaration(
          FileLocation.DUMMY,
          true,
          CStorageClass.AUTO,
          INT_TYPE,
          "global_G1",
          "global_G1",
          "global_G1",
          INT_0_INITIALIZER);

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

  private final CVariableDeclaration OUTER_STRUCT_DECLARATION =
      new CVariableDeclaration(
          FileLocation.DUMMY,
          false,
          CStorageClass.AUTO,
          OUTER_STRUCT_TYPE,
          "outer_struct",
          "outer_struct",
          "outer_struct",
          EMPTY_INITIALIZER_LIST);

  private final CParameterDeclaration PARAMETER_DECLARATION_POINTER_OUTER_STRUCT =
      new CParameterDeclaration(
          FileLocation.DUMMY, OUTER_STRUCT_POINTER_TYPE, "param_ptr_outer_struct");

  private final CParameterDeclaration PARAMETER_DECLARATION_POINTER_P1 =
      new CParameterDeclaration(FileLocation.DUMMY, INT_POINTER_TYPE, "param_ptr_P1");

  private final CParameterDeclaration PARAMETER_DECLARATION_POINTER_P2 =
      new CParameterDeclaration(FileLocation.DUMMY, INT_POINTER_TYPE, "param_ptr_P2");

  // Memory Locations (structs)

  private final MemoryLocation OUTER_STRUCT_MEMORY_LOCATION =
      MemoryLocation.of(
          MPOROptions.defaultTestInstance(), Optional.empty(), OUTER_STRUCT_DECLARATION);

  private final MemoryLocation OUTER_STRUCT_MEMBER_MEMORY_LOCATION =
      MemoryLocation.of(
          MPOROptions.defaultTestInstance(),
          Optional.empty(),
          OUTER_STRUCT_DECLARATION,
          OUTER_STRUCT_MEMBER_DECLARATION);

  private final MemoryLocation OUTER_STRUCT_POINTER_MEMBER_MEMORY_LOCATION =
      MemoryLocation.of(
          MPOROptions.defaultTestInstance(),
          Optional.empty(),
          OUTER_STRUCT_DECLARATION,
          OUTER_STRUCT_POINTER_MEMBER_DECLARATION);

  private final MemoryLocation INNER_STRUCT_MEMBER_MEMORY_LOCATION =
      MemoryLocation.of(
          MPOROptions.defaultTestInstance(),
          Optional.empty(),
          OUTER_STRUCT_DECLARATION,
          INNER_STRUCT_MEMBER_DECLARATION);

  private final MemoryLocation INNER_STRUCT_POINTER_MEMBER_MEMORY_LOCATION =
      MemoryLocation.of(
          MPOROptions.defaultTestInstance(),
          Optional.empty(),
          OUTER_STRUCT_DECLARATION,
          INNER_STRUCT_POINTER_MEMBER_DECLARATION);

  // Memory Locations (primitives)

  private final MemoryLocation GLOBAL_G1_MEMORY_LOCATION =
      MemoryLocation.of(MPOROptions.defaultTestInstance(), Optional.empty(), GLOBAL_G1_DECLARATION);

  private final MemoryLocation LOCAL_L1_MEMORY_LOCATION =
      MemoryLocation.of(MPOROptions.defaultTestInstance(), Optional.empty(), LOCAL_L1_DECLARATION);

  // Memory Locations (parameters)

  private final MemoryLocation PARAMETER_POINTER_OUTER_STRUCT_MEMORY_LOCATION =
      MemoryLocation.of(
          MPOROptions.defaultTestInstance(),
          Optional.of(DUMMY_CALL_CONTEXT),
          PARAMETER_DECLARATION_POINTER_OUTER_STRUCT);

  private final MemoryLocation PARAMETER_POINTER_P1_MEMORY_LOCATION =
      MemoryLocation.of(
          MPOROptions.defaultTestInstance(),
          Optional.of(DUMMY_CALL_CONTEXT),
          PARAMETER_DECLARATION_POINTER_P1);

  private final MemoryLocation PARAMETER_POINTER_P2_MEMORY_LOCATION =
      MemoryLocation.of(
          MPOROptions.defaultTestInstance(),
          Optional.of(DUMMY_CALL_CONTEXT),
          PARAMETER_DECLARATION_POINTER_P2);

  @Test
  public void test_outer_struct_pointer_parameter_dereference() {
    // param_ptr_outer = &outer; i.e. pointer parameter assignment
    ImmutableMap<MemoryLocation, MemoryLocation> parameterAssignments =
        ImmutableMap.<MemoryLocation, MemoryLocation>builder()
            .put(PARAMETER_POINTER_OUTER_STRUCT_MEMORY_LOCATION, OUTER_STRUCT_MEMORY_LOCATION)
            .buildOrThrow();
    ImmutableMap<MemoryLocation, MemoryLocation> pointerParameterAssignments =
        MemoryModelBuilder.extractPointerParameters(parameterAssignments);

    // find the mem locations associated with deref of 'param_ptr_outer'
    ImmutableSet<MemoryLocation> memoryLocations =
        MemoryLocationFinder.findMemoryLocationsByPointerDereference(
            PARAMETER_POINTER_OUTER_STRUCT_MEMORY_LOCATION,
            ImmutableSetMultimap.of(),
            ImmutableMap.of(),
            pointerParameterAssignments);

    // memory location of 'outer' should be associated with deref of 'param_ptr_outer'
    assertThat(memoryLocations.size() == 1).isTrue();
    assertThat(memoryLocations.contains(OUTER_STRUCT_MEMORY_LOCATION)).isTrue();
  }

  @Test
  public void test_struct_members_pointer_parameter_dereference() {
    // param_ptr_P1 = &outer.member; and param_ptr_P2 = &outer.inner.member
    // i.e. pointer parameter assignment
    ImmutableMap<MemoryLocation, MemoryLocation> parameterAssignments =
        ImmutableMap.<MemoryLocation, MemoryLocation>builder()
            .put(PARAMETER_POINTER_P1_MEMORY_LOCATION, OUTER_STRUCT_MEMBER_MEMORY_LOCATION)
            .put(PARAMETER_POINTER_P2_MEMORY_LOCATION, INNER_STRUCT_MEMBER_MEMORY_LOCATION)
            .buildOrThrow();
    ImmutableMap<MemoryLocation, MemoryLocation> pointerParameterAssignments =
        MemoryModelBuilder.extractPointerParameters(parameterAssignments);

    // find the mem locations associated with deref of 'param_ptr_P1'
    ImmutableSet<MemoryLocation> memoryLocationsP1 =
        MemoryLocationFinder.findMemoryLocationsByPointerDereference(
            PARAMETER_POINTER_P1_MEMORY_LOCATION,
            ImmutableSetMultimap.of(),
            ImmutableMap.of(),
            pointerParameterAssignments);
    // find the mem locations associated with deref of 'param_ptr_P2'
    ImmutableSet<MemoryLocation> memoryLocationsP2 =
        MemoryLocationFinder.findMemoryLocationsByPointerDereference(
            PARAMETER_POINTER_P2_MEMORY_LOCATION,
            ImmutableSetMultimap.of(),
            ImmutableMap.of(),
            pointerParameterAssignments);

    // memory location of 'outer.member' should be associated with deref of 'param_ptr_P1'
    assertThat(memoryLocationsP1.size() == 1).isTrue();
    assertThat(memoryLocationsP1.contains(OUTER_STRUCT_MEMBER_MEMORY_LOCATION)).isTrue();
    // memory location of 'outer.inner.member' should be associated with deref of 'param_ptr_P2'
    assertThat(memoryLocationsP2.size() == 1).isTrue();
    assertThat(memoryLocationsP2.contains(INNER_STRUCT_MEMBER_MEMORY_LOCATION)).isTrue();
  }

  @Test
  public void test_struct_pointer_members_pointer_parameter_dereference() {
    // outer.member_ptr = &local_L1; and outer.inner.member_ptr = &global_L1
    ImmutableSetMultimap<MemoryLocation, MemoryLocation> pointerAssignments =
        ImmutableSetMultimap.<MemoryLocation, MemoryLocation>builder()
            .put(OUTER_STRUCT_POINTER_MEMBER_MEMORY_LOCATION, LOCAL_L1_MEMORY_LOCATION)
            .put(INNER_STRUCT_POINTER_MEMBER_MEMORY_LOCATION, GLOBAL_G1_MEMORY_LOCATION)
            .build();

    // param_ptr_P1 = outer.member_ptr; and param_ptr_P2 = outer.inner.member_ptr
    // i.e. pointer parameter assignment
    ImmutableMap<MemoryLocation, MemoryLocation> parameterAssignments =
        ImmutableMap.<MemoryLocation, MemoryLocation>builder()
            .put(PARAMETER_POINTER_P1_MEMORY_LOCATION, OUTER_STRUCT_POINTER_MEMBER_MEMORY_LOCATION)
            .put(PARAMETER_POINTER_P2_MEMORY_LOCATION, INNER_STRUCT_POINTER_MEMBER_MEMORY_LOCATION)
            .buildOrThrow();
    ImmutableMap<MemoryLocation, MemoryLocation> pointerParameterAssignments =
        MemoryModelBuilder.extractPointerParameters(parameterAssignments);

    ImmutableSet<MemoryLocation> memoryLocationsP1 =
        MemoryLocationFinder.findMemoryLocationsByPointerDereference(
            PARAMETER_POINTER_P1_MEMORY_LOCATION,
            pointerAssignments,
            ImmutableMap.of(),
            pointerParameterAssignments);
    ImmutableSet<MemoryLocation> memoryLocationsP2 =
        MemoryLocationFinder.findMemoryLocationsByPointerDereference(
            PARAMETER_POINTER_P2_MEMORY_LOCATION,
            pointerAssignments,
            ImmutableMap.of(),
            pointerParameterAssignments);

    // assert that param_ptr_P1 is associated with local_l1 and param_ptr_P2 with global_G1
    assertThat(memoryLocationsP1.size() == 1).isTrue();
    assertThat(memoryLocationsP1.contains(LOCAL_L1_MEMORY_LOCATION)).isTrue();
    assertThat(memoryLocationsP2.size() == 1).isTrue();
    assertThat(memoryLocationsP2.contains(GLOBAL_G1_MEMORY_LOCATION)).isTrue();
  }
}
