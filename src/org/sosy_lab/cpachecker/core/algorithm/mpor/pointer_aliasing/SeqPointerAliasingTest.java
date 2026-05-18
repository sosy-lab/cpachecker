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
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeQualifiers;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;

public class SeqPointerAliasingTest {

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

  private final CVariableDeclaration GLOBAL_POINTER_B_DECLARATION =
      new CVariableDeclaration(
          FileLocation.DUMMY,
          true,
          CStorageClass.AUTO,
          INT_POINTER_TYPE,
          "global_ptr_B",
          "global_ptr_B",
          "global_ptr_B",
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

  private final CVariableDeclaration LOCAL_POINTER_D_DECLARATION =
      new CVariableDeclaration(
          FileLocation.DUMMY,
          false,
          CStorageClass.AUTO,
          INT_POINTER_TYPE,
          "local_ptr_D",
          "local_ptr_D",
          "local_ptr_D",
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

  private final CVariableDeclaration GLOBAL_Y_DECLARATION =
      new CVariableDeclaration(
          FileLocation.DUMMY,
          true,
          CStorageClass.AUTO,
          INT_TYPE,
          "global_Y",
          "global_Y",
          "global_Y",
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

  // CIdExpression

  private final CIdExpression GLOBAL_POINTER_A_ID_EXPRESSION =
      new CIdExpression(FileLocation.DUMMY, GLOBAL_POINTER_A_DECLARATION);

  private final CIdExpression GLOBAL_POINTER_B_ID_EXPRESSION =
      new CIdExpression(FileLocation.DUMMY, GLOBAL_POINTER_B_DECLARATION);

  private final CIdExpression GLOBAL_X_ID_EXPRESSION =
      new CIdExpression(FileLocation.DUMMY, GLOBAL_X_DECLARATION);

  private final CIdExpression GLOBAL_Y_ID_EXPRESSION =
      new CIdExpression(FileLocation.DUMMY, GLOBAL_Y_DECLARATION);

  private final CIdExpression LOCAL_C_ID_EXPRESSION =
      new CIdExpression(FileLocation.DUMMY, LOCAL_POINTER_C_DECLARATION);

  private final CIdExpression LOCAL_D_ID_EXPRESSION =
      new CIdExpression(FileLocation.DUMMY, LOCAL_POINTER_D_DECLARATION);

  private final CIdExpression LOCAL_Z_ID_EXPRESSION =
      new CIdExpression(FileLocation.DUMMY, LOCAL_Z_DECLARATION);

  // CUnaryExpression

  private final CUnaryExpression GLOBAL_X_UNARY_EXPRESSION =
      new CUnaryExpression(
          FileLocation.DUMMY,
          new CPointerType(CTypeQualifiers.NONE, GLOBAL_X_ID_EXPRESSION.getExpressionType()),
          GLOBAL_X_ID_EXPRESSION,
          UnaryOperator.AMPER);

  private final CUnaryExpression GLOBAL_Y_UNARY_EXPRESSION =
      new CUnaryExpression(
          FileLocation.DUMMY,
          new CPointerType(CTypeQualifiers.NONE, GLOBAL_Y_ID_EXPRESSION.getExpressionType()),
          GLOBAL_Y_ID_EXPRESSION,
          UnaryOperator.AMPER);

  private final CUnaryExpression LOCAL_Z_UNARY_EXPRESSION =
      new CUnaryExpression(
          FileLocation.DUMMY,
          new CPointerType(CTypeQualifiers.NONE, LOCAL_Z_ID_EXPRESSION.getExpressionType()),
          LOCAL_Z_ID_EXPRESSION,
          UnaryOperator.AMPER);

  @Test
  public void test_single_pointer_assignment() throws UnsupportedCodeException {
    // global_ptr_A = &global_X; i.e. pointer assignment
    Optional<SeqPointerAssignment> pointerAssignment =
        SeqPointerAliasingUtil.tryBuildPointerAssignment(
            GLOBAL_POINTER_A_ID_EXPRESSION,
            GLOBAL_X_UNARY_EXPRESSION,
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

    // only memory location of 'global_X' should be associated with dereference of 'global_ptr_A'
    assertThat(memoryLocations).hasSize(1);
    assertThat(memoryLocations)
        .contains(pointerAssignment.orElseThrow().rightHandSideMemoryLocation());
  }

  @Test
  public void test_multi_pointer_assignment() throws UnsupportedCodeException {
    // global_ptr_A = &global_X; and global_ptr_A = &global_Y; i.e. pointer assignments
    Optional<SeqPointerAssignment> pointerAssignment1 =
        SeqPointerAliasingUtil.tryBuildPointerAssignment(
            GLOBAL_POINTER_A_ID_EXPRESSION,
            GLOBAL_X_UNARY_EXPRESSION,
            Optional.empty(),
            Optional.empty(),
            ImmutableSortedMap.of(),
            SeqPointerAssignmentType.EXPLICIT);
    Optional<SeqPointerAssignment> pointerAssignment2 =
        SeqPointerAliasingUtil.tryBuildPointerAssignment(
            GLOBAL_POINTER_A_ID_EXPRESSION,
            GLOBAL_Y_UNARY_EXPRESSION,
            Optional.empty(),
            Optional.empty(),
            ImmutableSortedMap.of(),
            SeqPointerAssignmentType.EXPLICIT);

    assertThat(pointerAssignment1).isPresent();
    assertThat(pointerAssignment2).isPresent();

    // find the memory locations associated with dereference of 'global_ptr_A'
    ImmutableSet<SeqMemoryLocation> memoryLocations =
        SeqMemoryLocationFinder.findMemoryLocationsByPointerDereference(
            pointerAssignment1.orElseThrow().leftHandSideMemoryLocation(),
            ImmutableSet.of(pointerAssignment1.orElseThrow(), pointerAssignment2.orElseThrow()));

    // mem location of 'global_X' and 'global_Y' should be associated with deref of 'global_ptr_A'
    assertThat(memoryLocations).hasSize(2);
    assertThat(memoryLocations)
        .containsExactly(
            pointerAssignment1.orElseThrow().rightHandSideMemoryLocation(),
            pointerAssignment2.orElseThrow().rightHandSideMemoryLocation());
  }

  @Test
  public void test_transitive_pointer_assignment() throws UnsupportedCodeException {
    // global_ptr_A = &global_X; and global_ptr_B = global_ptr_A;
    // i.e. pointer assignment (transitive)
    Optional<SeqPointerAssignment> pointerAssignment1 =
        SeqPointerAliasingUtil.tryBuildPointerAssignment(
            GLOBAL_POINTER_A_ID_EXPRESSION,
            GLOBAL_X_UNARY_EXPRESSION,
            Optional.empty(),
            Optional.empty(),
            ImmutableSortedMap.of(),
            SeqPointerAssignmentType.EXPLICIT);
    Optional<SeqPointerAssignment> pointerAssignment2 =
        SeqPointerAliasingUtil.tryBuildPointerAssignment(
            GLOBAL_POINTER_B_ID_EXPRESSION,
            GLOBAL_POINTER_A_ID_EXPRESSION,
            Optional.empty(),
            Optional.empty(),
            ImmutableSortedMap.of(),
            SeqPointerAssignmentType.EXPLICIT);

    // find the memory locations associated with dereference of 'global_ptr_B'
    ImmutableSet<SeqMemoryLocation> memoryLocations =
        SeqMemoryLocationFinder.findMemoryLocationsByPointerDereference(
            pointerAssignment2.orElseThrow().leftHandSideMemoryLocation(),
            ImmutableSet.of(pointerAssignment1.orElseThrow(), pointerAssignment2.orElseThrow()));

    // memory location of 'global_X' should be associated with dereference of 'global_ptr_B'
    // even without direct assignment, due to transitive assignment of 'global_ptr_B = global_ptr_A'
    assertThat(memoryLocations).hasSize(1);
    assertThat(memoryLocations)
        .contains(pointerAssignment1.orElseThrow().rightHandSideMemoryLocation());
  }

  @Test
  public void test_implicit_global() throws UnsupportedCodeException {
    // global_ptr_A = &local_Z; i.e. pointer assignment
    Optional<SeqPointerAssignment> pointerAssignment =
        SeqPointerAliasingUtil.tryBuildPointerAssignment(
            GLOBAL_POINTER_A_ID_EXPRESSION,
            LOCAL_Z_UNARY_EXPRESSION,
            Optional.empty(),
            Optional.empty(),
            ImmutableSortedMap.of(),
            SeqPointerAssignmentType.EXPLICIT);

    assertThat(pointerAssignment).isPresent();

    // test that local_Z is now an implicit global memory location, because the global pointer
    // 'global_ptr_A' gets its address, and can be dereferenced by other threads
    assertThat(pointerAssignment.orElseThrow().leftHandSideMemoryLocation().isGlobal()).isTrue();
    assertThat(pointerAssignment.orElseThrow().rightHandSideMemoryLocation().isGlobal()).isFalse();
    assertThat(
            SeqPointerAliasingMapBuilder.isImplicitGlobal(
                pointerAssignment.orElseThrow().rightHandSideMemoryLocation(),
                ImmutableSet.of(pointerAssignment.orElseThrow()),
                ImmutableSet.of()))
        .isTrue();
  }

  @Test
  public void test_transitive_implicit_global() throws UnsupportedCodeException {
    // 'local_ptr_C = &local_Z;' and 'local_ptr_D = local_ptr_C;' and 'global_ptr_A = local_ptr_D'
    // i.e. transitive pointer assignments
    Optional<SeqPointerAssignment> pointerAssignment1 =
        SeqPointerAliasingUtil.tryBuildPointerAssignment(
            LOCAL_C_ID_EXPRESSION,
            LOCAL_Z_UNARY_EXPRESSION,
            Optional.empty(),
            Optional.empty(),
            ImmutableSortedMap.of(),
            SeqPointerAssignmentType.EXPLICIT);
    Optional<SeqPointerAssignment> pointerAssignment2 =
        SeqPointerAliasingUtil.tryBuildPointerAssignment(
            LOCAL_D_ID_EXPRESSION,
            LOCAL_C_ID_EXPRESSION,
            Optional.empty(),
            Optional.empty(),
            ImmutableSortedMap.of(),
            SeqPointerAssignmentType.EXPLICIT);
    Optional<SeqPointerAssignment> pointerAssignment3 =
        SeqPointerAliasingUtil.tryBuildPointerAssignment(
            GLOBAL_POINTER_A_ID_EXPRESSION,
            LOCAL_D_ID_EXPRESSION,
            Optional.empty(),
            Optional.empty(),
            ImmutableSortedMap.of(),
            SeqPointerAssignmentType.EXPLICIT);

    assertThat(pointerAssignment1).isPresent();
    assertThat(pointerAssignment2).isPresent();
    assertThat(pointerAssignment3).isPresent();

    // test that local_Z is now an implicit global memory location, because of transitivity:
    // 'global_ptr_A -> local_ptr_D -> local_ptr_C', and can then be dereferenced by other threads
    assertThat(pointerAssignment3.orElseThrow().leftHandSideMemoryLocation().isGlobal()).isTrue();
    assertThat(pointerAssignment1.orElseThrow().leftHandSideMemoryLocation().isGlobal()).isFalse();
    assertThat(pointerAssignment2.orElseThrow().leftHandSideMemoryLocation().isGlobal()).isFalse();
    assertThat(pointerAssignment1.orElseThrow().rightHandSideMemoryLocation().isGlobal()).isFalse();
    assertThat(
            SeqPointerAliasingMapBuilder.isImplicitGlobal(
                pointerAssignment1.orElseThrow().rightHandSideMemoryLocation(),
                ImmutableSet.of(
                    pointerAssignment1.orElseThrow(),
                    pointerAssignment2.orElseThrow(),
                    pointerAssignment3.orElseThrow()),
                ImmutableSet.of()))
        .isTrue();
  }
}
