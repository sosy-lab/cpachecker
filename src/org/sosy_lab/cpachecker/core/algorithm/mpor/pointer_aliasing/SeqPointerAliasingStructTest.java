// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import java.math.BigInteger;
import java.util.Optional;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeQualifiers;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;

public class SeqPointerAliasingStructTest {

  // Simple Types

  private final CSimpleType INT_TYPE =
      new CSimpleType(
          CTypeQualifiers.NONE, CBasicType.INT, false, false, true, false, false, false, false);

  private final CPointerType INT_POINTER_TYPE = new CPointerType(CTypeQualifiers.NONE, INT_TYPE);

  // Complex Types (inner struct)

  private final CCompositeTypeMemberDeclaration INNER_STRUCT_MEMBER_DECLARATION =
      new CCompositeTypeMemberDeclaration(INT_TYPE, "inner_member");

  private final CCompositeTypeMemberDeclaration INNER_STRUCT_POINTER_MEMBER_DECLARATION =
      new CCompositeTypeMemberDeclaration(INT_POINTER_TYPE, "inner_member_ptr");

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

  // CCompositeTypeMemberDeclaration

  private final CCompositeTypeMemberDeclaration OUTER_STRUCT_MEMBER_DECLARATION =
      new CCompositeTypeMemberDeclaration(INT_TYPE, "outer_member");

  private final CCompositeTypeMemberDeclaration INNER_STRUCT_DECLARATION =
      new CCompositeTypeMemberDeclaration(INNER_STRUCT_TYPE, "inner_struct");

  private final CCompositeTypeMemberDeclaration OUTER_STRUCT_POINTER_MEMBER_DECLARATION =
      new CCompositeTypeMemberDeclaration(INT_POINTER_TYPE, "outer_member_ptr");

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

  // Expressions

  private final CIntegerLiteralExpression INT_0 =
      new CIntegerLiteralExpression(FileLocation.DUMMY, INT_TYPE, BigInteger.valueOf(0));

  // Initializers

  private final CInitializer INT_0_INITIALIZER =
      new CInitializerExpression(FileLocation.DUMMY, INT_0);

  private final CInitializerList EMPTY_INITIALIZER_LIST =
      new CInitializerList(FileLocation.DUMMY, ImmutableList.of());

  // Declarations

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

  private final CVariableDeclaration INNER_STRUCT_VARIABLE_DECLARATION =
      new CVariableDeclaration(
          FileLocation.DUMMY,
          false,
          CStorageClass.AUTO,
          INNER_STRUCT_TYPE,
          "inner_struct",
          "inner_struct",
          "inner_struct",
          EMPTY_INITIALIZER_LIST);

  private final class CParameterDeclarations {
    final CParameterDeclaration PARAMETER_DECLARATION_POINTER_OUTER_STRUCT =
        new CParameterDeclaration(
            FileLocation.DUMMY, OUTER_STRUCT_POINTER_TYPE, "param_ptr_outer_struct");

    final CParameterDeclaration PARAMETER_DECLARATION_POINTER_P1 =
        new CParameterDeclaration(FileLocation.DUMMY, INT_POINTER_TYPE, "param_ptr_P1");

    final CParameterDeclaration PARAMETER_DECLARATION_POINTER_P2 =
        new CParameterDeclaration(FileLocation.DUMMY, INT_POINTER_TYPE, "param_ptr_P2");

    CParameterDeclarations() {
      // qualified names are required, otherwise .asVariableDeclaration throws
      PARAMETER_DECLARATION_POINTER_OUTER_STRUCT.setQualifiedName("dummy");
      PARAMETER_DECLARATION_POINTER_P1.setQualifiedName("dummy");
      PARAMETER_DECLARATION_POINTER_P2.setQualifiedName("dummy");
    }
  }

  private final CParameterDeclarations PARAMETER_DECLARATIONS = new CParameterDeclarations();

  // CIdExpression

  private final CIdExpression GLOBAL_POINTER_A_ID_EXPRESSION =
      new CIdExpression(FileLocation.DUMMY, GLOBAL_POINTER_A_DECLARATION);

  private final CIdExpression GLOBAL_G1_ID_EXPRESSION =
      new CIdExpression(FileLocation.DUMMY, GLOBAL_G1_DECLARATION);

  private final CIdExpression LOCAL_L1_ID_EXPRESSION =
      new CIdExpression(FileLocation.DUMMY, GLOBAL_G1_DECLARATION);

  private final CIdExpression OUTER_STRUCT_ID_EXPRESSION =
      new CIdExpression(FileLocation.DUMMY, OUTER_STRUCT_DECLARATION);

  private final CIdExpression INNER_STRUCT_ID_EXPRESSION =
      new CIdExpression(FileLocation.DUMMY, INNER_STRUCT_VARIABLE_DECLARATION);

  private final CIdExpression PARAMETER_POINTER_OUTER_STRUCT_ID_EXPRESSION =
      new CIdExpression(
          FileLocation.DUMMY,
          PARAMETER_DECLARATIONS.PARAMETER_DECLARATION_POINTER_OUTER_STRUCT
              .asVariableDeclaration());

  private final CIdExpression PARAMETER_POINTER_P1_ID_EXPRESSION =
      new CIdExpression(
          FileLocation.DUMMY,
          PARAMETER_DECLARATIONS.PARAMETER_DECLARATION_POINTER_P1.asVariableDeclaration());

  private final CIdExpression PARAMETER_POINTER_P2_ID_EXPRESSION =
      new CIdExpression(
          FileLocation.DUMMY,
          PARAMETER_DECLARATIONS.PARAMETER_DECLARATION_POINTER_P2.asVariableDeclaration());

  // CFieldReference

  private final CFieldReference OUTER_STRUCT_MEMBER_FIELD_REFERENCE =
      new CFieldReference(
          FileLocation.DUMMY,
          OUTER_STRUCT_ID_EXPRESSION.getExpressionType(),
          OUTER_STRUCT_MEMBER_DECLARATION.getName(),
          OUTER_STRUCT_ID_EXPRESSION,
          false);

  private final CFieldReference OUTER_STRUCT_POINTER_MEMBER_FIELD_REFERENCE =
      new CFieldReference(
          FileLocation.DUMMY,
          OUTER_STRUCT_ID_EXPRESSION.getExpressionType(),
          OUTER_STRUCT_POINTER_MEMBER_DECLARATION.getName(),
          OUTER_STRUCT_ID_EXPRESSION,
          false);

  private final CFieldReference INNER_STRUCT_MEMBER_FIELD_REFERENCE =
      new CFieldReference(
          FileLocation.DUMMY,
          INNER_STRUCT_ID_EXPRESSION.getExpressionType(),
          INNER_STRUCT_MEMBER_DECLARATION.getName(),
          INNER_STRUCT_ID_EXPRESSION,
          false);

  private final CFieldReference OUTER_STRUCT_INNER_STRUCT_MEMBER_FIELD_REFERENCE =
      new CFieldReference(
          FileLocation.DUMMY,
          INNER_STRUCT_ID_EXPRESSION.getExpressionType(),
          INNER_STRUCT_MEMBER_DECLARATION.getName(),
          INNER_STRUCT_MEMBER_FIELD_REFERENCE,
          false);

  private final CFieldReference OUTER_STRUCT_INNER_STRUCT_POINTER_MEMBER_FIELD_REFERENCE =
      new CFieldReference(
          FileLocation.DUMMY,
          INNER_STRUCT_ID_EXPRESSION.getExpressionType(),
          INNER_STRUCT_POINTER_MEMBER_DECLARATION.getName(),
          INNER_STRUCT_MEMBER_FIELD_REFERENCE,
          false);

  // CUnaryExpression

  private final CUnaryExpression GLOBAL_G1_UNARY_EXPRESSION =
      new CUnaryExpression(
          FileLocation.DUMMY,
          new CPointerType(CTypeQualifiers.NONE, GLOBAL_G1_DECLARATION.getType()),
          GLOBAL_G1_ID_EXPRESSION,
          UnaryOperator.AMPER);

  private final CUnaryExpression LOCAL_L1_UNARY_EXPRESSION =
      new CUnaryExpression(
          FileLocation.DUMMY,
          new CPointerType(CTypeQualifiers.NONE, LOCAL_L1_DECLARATION.getType()),
          LOCAL_L1_ID_EXPRESSION,
          UnaryOperator.AMPER);

  private final CUnaryExpression OUTER_STRUCT_UNARY_EXPRESSION =
      new CUnaryExpression(
          FileLocation.DUMMY,
          new CPointerType(CTypeQualifiers.NONE, OUTER_STRUCT_ID_EXPRESSION.getExpressionType()),
          OUTER_STRUCT_ID_EXPRESSION,
          UnaryOperator.AMPER);

  private final CUnaryExpression OUTER_STRUCT_MEMBER_UNARY_EXPRESSION =
      new CUnaryExpression(
          FileLocation.DUMMY,
          new CPointerType(
              CTypeQualifiers.NONE, OUTER_STRUCT_MEMBER_FIELD_REFERENCE.getExpressionType()),
          OUTER_STRUCT_MEMBER_FIELD_REFERENCE,
          UnaryOperator.AMPER);

  private final CUnaryExpression OUTER_STRUCT_INNER_STRUCT_MEMBER_UNARY_EXPRESSION =
      new CUnaryExpression(
          FileLocation.DUMMY,
          new CPointerType(
              CTypeQualifiers.NONE,
              OUTER_STRUCT_INNER_STRUCT_MEMBER_FIELD_REFERENCE.getExpressionType()),
          OUTER_STRUCT_INNER_STRUCT_MEMBER_FIELD_REFERENCE,
          UnaryOperator.AMPER);

  @Test
  public void test_field_owner_field_member() throws UnsupportedCodeException {
    // global_ptr_A = &outer_struct.outer_member; i.e. pointer assignment
    Optional<SeqPointerAssignment> pointerAssignment =
        SeqPointerAliasingUtil.tryBuildPointerAssignment(
            GLOBAL_POINTER_A_ID_EXPRESSION,
            OUTER_STRUCT_MEMBER_UNARY_EXPRESSION,
            Optional.empty(),
            Optional.empty(),
            ImmutableSortedMap.of(),
            SeqPointerAssignmentType.EXPLICIT);

    assertThat(pointerAssignment).isPresent();

    // find the memory locations associated with dereference of 'global_ptr_A'
    ImmutableSet<SeqMemoryLocation> memoryLocations =
        SeqMemoryLocationFinder.findMemoryLocationsByPointerDereference(
            pointerAssignment.orElseThrow().leftHandSideMemoryLocation(),
            ImmutableSet.of(pointerAssignment.orElseThrow()));

    // mem location 'outer_struct.outer_member' should be associated with dereference of
    // 'global_ptr_A'
    assertThat(memoryLocations).hasSize(1);
    assertThat(memoryLocations)
        .contains(pointerAssignment.orElseThrow().rightHandSideMemoryLocation());
  }

  @Test
  public void test_outer_inner_struct() throws UnsupportedCodeException {
    // global_ptr_A = &outer_struct.outer_member
    Optional<SeqPointerAssignment> pointerAssignment1 =
        SeqPointerAliasingUtil.tryBuildPointerAssignment(
            GLOBAL_POINTER_A_ID_EXPRESSION,
            OUTER_STRUCT_MEMBER_UNARY_EXPRESSION,
            Optional.empty(),
            Optional.empty(),
            ImmutableSortedMap.of(),
            SeqPointerAssignmentType.EXPLICIT);
    // global_ptr_A = &outer_struct.inner_struct.inner_member
    Optional<SeqPointerAssignment> pointerAssignment2 =
        SeqPointerAliasingUtil.tryBuildPointerAssignment(
            GLOBAL_POINTER_A_ID_EXPRESSION,
            OUTER_STRUCT_INNER_STRUCT_MEMBER_FIELD_REFERENCE,
            Optional.empty(),
            Optional.empty(),
            ImmutableSortedMap.of(),
            SeqPointerAssignmentType.EXPLICIT);

    assertThat(pointerAssignment2).isPresent();

    // find the memory locations associated with dereference of 'global_ptr_A'
    ImmutableSet<SeqMemoryLocation> memoryLocationsB =
        SeqMemoryLocationFinder.findMemoryLocationsByPointerDereference(
            pointerAssignment2.orElseThrow().leftHandSideMemoryLocation(),
            ImmutableSet.of(pointerAssignment1.orElseThrow(), pointerAssignment2.orElseThrow()));
    assertThat(memoryLocationsB).hasSize(2);
    assertThat(memoryLocationsB)
        .contains(pointerAssignment1.orElseThrow().rightHandSideMemoryLocation());
    assertThat(memoryLocationsB)
        .contains(pointerAssignment2.orElseThrow().rightHandSideMemoryLocation());
  }

  @Test
  public void test_outer_struct_pointer_parameter_dereference() throws UnsupportedCodeException {
    // param_ptr_outer = &outer; i.e. pointer parameter assignment
    Optional<SeqPointerAssignment> parameterAssignment =
        SeqPointerAliasingUtil.tryBuildPointerAssignment(
            PARAMETER_POINTER_OUTER_STRUCT_ID_EXPRESSION,
            OUTER_STRUCT_UNARY_EXPRESSION,
            Optional.empty(),
            Optional.empty(),
            ImmutableSortedMap.of(),
            SeqPointerAssignmentType.PARAMETER);

    assertThat(parameterAssignment).isPresent();

    // find the mem locations associated with deref of 'param_ptr_outer'
    ImmutableSet<SeqMemoryLocation> memoryLocations =
        SeqMemoryLocationFinder.findMemoryLocationsByPointerDereference(
            parameterAssignment.orElseThrow().leftHandSideMemoryLocation(),
            ImmutableSet.of(parameterAssignment.orElseThrow()));

    // memory location of 'outer' should be associated with deref of 'param_ptr_outer'
    assertThat(memoryLocations).hasSize(1);
    assertThat(memoryLocations)
        .contains(parameterAssignment.orElseThrow().rightHandSideMemoryLocation());
  }

  @Test
  public void test_struct_members_pointer_parameter_dereference() throws UnsupportedCodeException {
    // param_ptr_P1 = &outer.member; and param_ptr_P2 = &outer.inner.member
    // i.e. pointer parameter assignments
    Optional<SeqPointerAssignment> parameterAssignment1 =
        SeqPointerAliasingUtil.tryBuildPointerAssignment(
            PARAMETER_POINTER_P1_ID_EXPRESSION,
            OUTER_STRUCT_MEMBER_UNARY_EXPRESSION,
            Optional.empty(),
            Optional.empty(),
            ImmutableSortedMap.of(),
            SeqPointerAssignmentType.EXPLICIT);
    Optional<SeqPointerAssignment> parameterAssignment2 =
        SeqPointerAliasingUtil.tryBuildPointerAssignment(
            PARAMETER_POINTER_P2_ID_EXPRESSION,
            OUTER_STRUCT_INNER_STRUCT_MEMBER_UNARY_EXPRESSION,
            Optional.empty(),
            Optional.empty(),
            ImmutableSortedMap.of(),
            SeqPointerAssignmentType.EXPLICIT);

    assertThat(parameterAssignment1).isPresent();
    assertThat(parameterAssignment2).isPresent();

    // find the mem locations associated with deref of 'param_ptr_P1'
    ImmutableSet<SeqMemoryLocation> memoryLocationsP1 =
        SeqMemoryLocationFinder.findMemoryLocationsByPointerDereference(
            parameterAssignment1.orElseThrow().leftHandSideMemoryLocation(),
            ImmutableSet.of(
                parameterAssignment1.orElseThrow(), parameterAssignment2.orElseThrow()));
    // find the mem locations associated with deref of 'param_ptr_P2'
    ImmutableSet<SeqMemoryLocation> memoryLocationsP2 =
        SeqMemoryLocationFinder.findMemoryLocationsByPointerDereference(
            parameterAssignment2.orElseThrow().leftHandSideMemoryLocation(),
            ImmutableSet.of(
                parameterAssignment1.orElseThrow(), parameterAssignment2.orElseThrow()));

    // memory location of 'outer.member' should be associated with deref of 'param_ptr_P1'
    assertThat(memoryLocationsP1).hasSize(1);
    assertThat(memoryLocationsP1)
        .contains(parameterAssignment1.orElseThrow().rightHandSideMemoryLocation());
    // memory location of 'outer.inner.member' should be associated with deref of 'param_ptr_P2'
    assertThat(memoryLocationsP2).hasSize(1);
    assertThat(memoryLocationsP2)
        .contains(parameterAssignment2.orElseThrow().rightHandSideMemoryLocation());
  }

  @Test
  public void test_struct_pointer_members_pointer_parameter_dereference()
      throws UnsupportedCodeException {
    // outer.member_ptr = &local_L1; and outer.inner.member_ptr = &global_L1
    Optional<SeqPointerAssignment> pointerAssignment1 =
        SeqPointerAliasingUtil.tryBuildPointerAssignment(
            OUTER_STRUCT_POINTER_MEMBER_FIELD_REFERENCE,
            LOCAL_L1_UNARY_EXPRESSION,
            Optional.empty(),
            Optional.empty(),
            ImmutableSortedMap.of(),
            SeqPointerAssignmentType.EXPLICIT);
    Optional<SeqPointerAssignment> pointerAssignment2 =
        SeqPointerAliasingUtil.tryBuildPointerAssignment(
            OUTER_STRUCT_INNER_STRUCT_POINTER_MEMBER_FIELD_REFERENCE,
            GLOBAL_G1_UNARY_EXPRESSION,
            Optional.empty(),
            Optional.empty(),
            ImmutableSortedMap.of(),
            SeqPointerAssignmentType.EXPLICIT);

    assertThat(pointerAssignment1).isPresent();
    assertThat(pointerAssignment2).isPresent();

    // param_ptr_P1 = outer.member_ptr; and param_ptr_P2 = outer.inner.member_ptr
    // i.e. pointer parameter assignment
    Optional<SeqPointerAssignment> parameterAssignment1 =
        SeqPointerAliasingUtil.tryBuildPointerAssignment(
            PARAMETER_POINTER_P1_ID_EXPRESSION,
            OUTER_STRUCT_POINTER_MEMBER_FIELD_REFERENCE,
            Optional.empty(),
            Optional.empty(),
            ImmutableSortedMap.of(),
            SeqPointerAssignmentType.PARAMETER);
    Optional<SeqPointerAssignment> parameterAssignment2 =
        SeqPointerAliasingUtil.tryBuildPointerAssignment(
            PARAMETER_POINTER_P2_ID_EXPRESSION,
            OUTER_STRUCT_INNER_STRUCT_POINTER_MEMBER_FIELD_REFERENCE,
            Optional.empty(),
            Optional.empty(),
            ImmutableSortedMap.of(),
            SeqPointerAssignmentType.PARAMETER);

    assertThat(parameterAssignment1).isPresent();
    assertThat(parameterAssignment2).isPresent();

    ImmutableSet<SeqPointerAssignment> allPointerAssignments =
        ImmutableSet.of(
            pointerAssignment1.orElseThrow(),
            pointerAssignment2.orElseThrow(),
            parameterAssignment1.orElseThrow(),
            parameterAssignment2.orElseThrow());

    ImmutableSet<SeqMemoryLocation> memoryLocationsP1 =
        SeqMemoryLocationFinder.findMemoryLocationsByPointerDereference(
            parameterAssignment1.orElseThrow().leftHandSideMemoryLocation(), allPointerAssignments);
    ImmutableSet<SeqMemoryLocation> memoryLocationsP2 =
        SeqMemoryLocationFinder.findMemoryLocationsByPointerDereference(
            parameterAssignment2.orElseThrow().leftHandSideMemoryLocation(), allPointerAssignments);

    // check that param_ptr_P1 is associated with local_l1 and param_ptr_P2 with global_G1
    assertThat(memoryLocationsP1).hasSize(1);
    assertThat(memoryLocationsP1)
        .contains(pointerAssignment1.orElseThrow().rightHandSideMemoryLocation());
    assertThat(memoryLocationsP2).hasSize(1);
    assertThat(memoryLocationsP2)
        .contains(pointerAssignment2.orElseThrow().rightHandSideMemoryLocation());
  }
}
