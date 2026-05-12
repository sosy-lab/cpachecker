// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import java.math.BigInteger;
import java.util.Optional;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeQualifiers;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;

public class SeqPointerAliasingParameterTest {

  // Simple Types

  private static final CSimpleType INT_TYPE =
      new CSimpleType(
          CTypeQualifiers.NONE, CBasicType.INT, false, false, true, false, false, false, false);

  private final CPointerType INT_POINTER_TYPE = new CPointerType(CTypeQualifiers.NONE, INT_TYPE);

  // Expressions

  private final CIntegerLiteralExpression INT_0 =
      new CIntegerLiteralExpression(FileLocation.DUMMY, INT_TYPE, BigInteger.valueOf(0));

  // Initializers

  private final CInitializer INT_0_INITIALIZER =
      new CInitializerExpression(FileLocation.DUMMY, INT_0);

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

  private final class CParameterDeclarations {
    final CParameterDeclaration PARAMETER_DECLARATION_POINTER_P =
        new CParameterDeclaration(FileLocation.DUMMY, INT_POINTER_TYPE, "param_ptr_P");

    final CParameterDeclaration PARAMETER_DECLARATION_Q =
        new CParameterDeclaration(FileLocation.DUMMY, INT_TYPE, "param_Q");

    final CParameterDeclaration PARAMETER_DECLARATION_POINTER_R =
        new CParameterDeclaration(FileLocation.DUMMY, INT_POINTER_TYPE, "param_ptr_R");

    CParameterDeclarations() {
      // qualified names are required, otherwise .asVariableDeclaration throws
      PARAMETER_DECLARATION_POINTER_P.setQualifiedName("dummy");
      PARAMETER_DECLARATION_Q.setQualifiedName("dummy");
      PARAMETER_DECLARATION_POINTER_R.setQualifiedName("dummy");
    }
  }

  private final CParameterDeclarations PARAMETER_DECLARATIONS = new CParameterDeclarations();

  // CIdExpression

  private final CIdExpression GLOBAL_POINTER_A_ID_EXPRESSION =
      new CIdExpression(FileLocation.DUMMY, GLOBAL_POINTER_A_DECLARATION);

  private final CIdExpression GLOBAL_X_ID_EXPRESSION =
      new CIdExpression(FileLocation.DUMMY, GLOBAL_X_DECLARATION);

  private final CIdExpression LOCAL_POINTER_C_ID_EXPRESSION =
      new CIdExpression(FileLocation.DUMMY, LOCAL_POINTER_C_DECLARATION);

  private final CIdExpression LOCAL_Z_ID_EXPRESSION =
      new CIdExpression(FileLocation.DUMMY, LOCAL_Z_DECLARATION);

  private final CIdExpression PARAMETER_Q_ID_EXPRESSION =
      new CIdExpression(
          FileLocation.DUMMY,
          PARAMETER_DECLARATIONS.PARAMETER_DECLARATION_Q.asVariableDeclaration());

  private final CIdExpression PARAMETER_POINTER_P_ID_EXPRESSION =
      new CIdExpression(
          FileLocation.DUMMY,
          PARAMETER_DECLARATIONS.PARAMETER_DECLARATION_POINTER_P.asVariableDeclaration());

  private final CIdExpression PARAMETER_POINTER_R_ID_EXPRESSION =
      new CIdExpression(
          FileLocation.DUMMY,
          PARAMETER_DECLARATIONS.PARAMETER_DECLARATION_POINTER_R.asVariableDeclaration());

  // CUnaryExpression

  private final CUnaryExpression GLOBAL_X_UNARY_EXPRESSION =
      new CUnaryExpression(
          FileLocation.DUMMY,
          new CPointerType(CTypeQualifiers.NONE, GLOBAL_X_ID_EXPRESSION.getExpressionType()),
          GLOBAL_X_ID_EXPRESSION,
          UnaryOperator.AMPER);

  private final CUnaryExpression LOCAL_Z_UNARY_EXPRESSION =
      new CUnaryExpression(
          FileLocation.DUMMY,
          new CPointerType(CTypeQualifiers.NONE, LOCAL_Z_ID_EXPRESSION.getExpressionType()),
          LOCAL_Z_ID_EXPRESSION,
          UnaryOperator.AMPER);

  private final CUnaryExpression PARAMETER_Q_UNARY_EXPRESSION =
      new CUnaryExpression(
          FileLocation.DUMMY,
          new CPointerType(CTypeQualifiers.NONE, PARAMETER_Q_ID_EXPRESSION.getExpressionType()),
          PARAMETER_Q_ID_EXPRESSION,
          UnaryOperator.AMPER);

  @Test
  public void test_pointer_parameter_dereference() throws UnsupportedCodeException {
    // param_ptr_P = &global_X; i.e. pointer parameter assignment
    Optional<SeqPointerAssignment> parameterAssignment =
        SeqPointerAliasingUtil.tryBuildPointerAssignment(
            PARAMETER_POINTER_P_ID_EXPRESSION,
            GLOBAL_X_UNARY_EXPRESSION,
            Optional.empty(),
            Optional.empty(),
            ImmutableSortedMap.of(),
            SeqPointerAssignmentType.PARAMETER);

    assertThat(parameterAssignment).isPresent();

    SeqMemoryLocation pMemoryLocation =
        parameterAssignment.orElseThrow().leftHandSideMemoryLocation();
    SeqMemoryLocation xMemoryLocation =
        parameterAssignment.orElseThrow().rightHandSideMemoryLocation();

    // find the mem locations associated with deref of 'param_ptr_P' in the given call context
    ImmutableSet<SeqMemoryLocation> memoryLocations =
        SeqMemoryLocationFinder.findMemoryLocationsByPointerDereference(
            pMemoryLocation, ImmutableSet.of(parameterAssignment.orElseThrow()));

    // memory location of 'global_X' should be associated with dereference of 'param_ptr_P'
    assertThat(memoryLocations).hasSize(1);
    assertThat(memoryLocations).contains(xMemoryLocation);
  }

  @Test
  public void test_transitive_pointer_parameter_dereference() throws UnsupportedCodeException {
    // param_ptr_P = local_ptr_C; i.e. transitive pointer parameter assignment
    Optional<SeqPointerAssignment> parameterAssignment =
        SeqPointerAliasingUtil.tryBuildPointerAssignment(
            PARAMETER_POINTER_P_ID_EXPRESSION,
            LOCAL_POINTER_C_ID_EXPRESSION,
            Optional.empty(),
            Optional.empty(),
            ImmutableSortedMap.of(),
            SeqPointerAssignmentType.PARAMETER);

    // local_ptr_C = &global_X; i.e. pointer assignment
    Optional<SeqPointerAssignment> pointerAssignment =
        SeqPointerAliasingUtil.tryBuildPointerAssignment(
            LOCAL_POINTER_C_ID_EXPRESSION,
            GLOBAL_X_UNARY_EXPRESSION,
            Optional.empty(),
            Optional.empty(),
            ImmutableSortedMap.of(),
            SeqPointerAssignmentType.EXPLICIT);

    assertThat(parameterAssignment).isPresent();
    assertThat(pointerAssignment).isPresent();

    SeqMemoryLocation pMemoryLocation =
        parameterAssignment.orElseThrow().leftHandSideMemoryLocation();
    SeqMemoryLocation xMemoryLocation =
        pointerAssignment.orElseThrow().rightHandSideMemoryLocation();

    // find the mem locations associated with deref of 'param_ptr_P' in the given call context
    ImmutableSet<SeqMemoryLocation> memoryLocations =
        SeqMemoryLocationFinder.findMemoryLocationsByPointerDereference(
            pMemoryLocation,
            ImmutableSet.of(parameterAssignment.orElseThrow(), pointerAssignment.orElseThrow()));

    // memory location of 'global_X' should be associated with dereference of 'param_ptr_P'
    assertThat(memoryLocations).hasSize(1);
    assertThat(memoryLocations).contains(xMemoryLocation);
  }

  @Test
  public void test_non_pointer_parameter() throws UnsupportedCodeException {
    // param_Q = local_Z; i.e. non-pointer parameter assignment with local variable
    Optional<SeqPointerAssignment> parameterAssignment =
        SeqPointerAliasingUtil.tryBuildPointerAssignment(
            PARAMETER_Q_ID_EXPRESSION,
            LOCAL_Z_ID_EXPRESSION,
            Optional.empty(),
            Optional.empty(),
            ImmutableSortedMap.of(),
            SeqPointerAssignmentType.PARAMETER);

    assertThat(parameterAssignment).isEmpty();
  }

  @Test
  public void test_parameter_implicit_global() throws UnsupportedCodeException {
    // global_ptr_A = &param_Q; i.e. pointer assignment
    Optional<SeqPointerAssignment> pointerAssignment =
        SeqPointerAliasingUtil.tryBuildPointerAssignment(
            GLOBAL_POINTER_A_ID_EXPRESSION,
            PARAMETER_Q_UNARY_EXPRESSION,
            Optional.empty(),
            Optional.empty(),
            ImmutableSortedMap.of(),
            SeqPointerAssignmentType.EXPLICIT);

    assertThat(pointerAssignment).isPresent();

    ImmutableSet<SeqPointerAssignment> allPointerAssignments =
        ImmutableSet.of(pointerAssignment.orElseThrow());

    SeqMemoryLocation aMemoryLocation =
        pointerAssignment.orElseThrow().leftHandSideMemoryLocation();
    SeqMemoryLocation qMemoryLocation =
        pointerAssignment.orElseThrow().rightHandSideMemoryLocation();

    // check that param_Q is now an implicit global memory location
    assertThat(qMemoryLocation.isGlobal()).isFalse();
    assertThat(aMemoryLocation.isGlobal()).isTrue();
    assertThat(
            SeqPointerAliasingMapBuilder.isImplicitGlobal(
                qMemoryLocation, allPointerAssignments, ImmutableSet.of()))
        .isTrue();
  }

  @Test
  public void test_transitive_pointer_parameter_assignments() throws UnsupportedCodeException {
    // param_ptr_R = &local_Z; and param_ptr_P = param_ptr_R;
    // i.e. transitive pointer parameter assignments
    Optional<SeqPointerAssignment> pointerAssignment1 =
        SeqPointerAliasingUtil.tryBuildPointerAssignment(
            PARAMETER_POINTER_R_ID_EXPRESSION,
            LOCAL_Z_UNARY_EXPRESSION,
            Optional.empty(),
            Optional.empty(),
            ImmutableSortedMap.of(),
            SeqPointerAssignmentType.PARAMETER);
    Optional<SeqPointerAssignment> pointerAssignment2 =
        SeqPointerAliasingUtil.tryBuildPointerAssignment(
            PARAMETER_POINTER_P_ID_EXPRESSION,
            PARAMETER_POINTER_R_ID_EXPRESSION,
            Optional.empty(),
            Optional.empty(),
            ImmutableSortedMap.of(),
            SeqPointerAssignmentType.PARAMETER);

    SeqMemoryLocation rMemoryLocation =
        pointerAssignment1.orElseThrow().leftHandSideMemoryLocation();
    SeqMemoryLocation zMemoryLocation =
        pointerAssignment1.orElseThrow().rightHandSideMemoryLocation();
    SeqMemoryLocation pMemoryLocation =
        pointerAssignment2.orElseThrow().leftHandSideMemoryLocation();

    // all are not explicit global memory locations
    assertThat(rMemoryLocation.isGlobal()).isFalse();
    assertThat(zMemoryLocation.isGlobal()).isFalse();

    // find the mem locations associated with deref of 'param_ptr_P' in the given call context
    ImmutableSet<SeqMemoryLocation> memoryLocations =
        SeqMemoryLocationFinder.findMemoryLocationsByPointerDereference(
            pMemoryLocation,
            ImmutableSet.of(pointerAssignment1.orElseThrow(), pointerAssignment2.orElseThrow()));

    assertThat(memoryLocations).hasSize(1);
    assertThat(memoryLocations).contains(zMemoryLocation);
  }
}
