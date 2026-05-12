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
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;

public class SeqPointerAliasingStartRoutineArgTest {

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

  private final class CParameterDeclarations {
    private final CParameterDeclaration START_ROUTINE_ARG_DECLARATION =
        new CParameterDeclaration(FileLocation.DUMMY, VOID_POINTER_TYPE, "start_routine_arg");

    CParameterDeclarations() {
      // qualified names are required, otherwise .asVariableDeclaration throws
      START_ROUTINE_ARG_DECLARATION.setQualifiedName("dummy");
    }
  }

  private final CParameterDeclarations PARAMETER_DECLARATIONS = new CParameterDeclarations();

  // CIdExpression

  private final CIdExpression START_ROUTINE_ARG_ID_EXPRESSION =
      new CIdExpression(
          FileLocation.DUMMY,
          PARAMETER_DECLARATIONS.START_ROUTINE_ARG_DECLARATION.asVariableDeclaration());

  private final CIdExpression LOCAL_L1_ID_EXPRESSION =
      new CIdExpression(FileLocation.DUMMY, LOCAL_L1_DECLARATION);

  // CUnaryExpression

  private final CUnaryExpression LOCAL_L1_UNARY_EXPRESSION =
      new CUnaryExpression(
          FileLocation.DUMMY,
          new CPointerType(CTypeQualifiers.NONE, LOCAL_L1_ID_EXPRESSION.getExpressionType()),
          LOCAL_L1_ID_EXPRESSION,
          UnaryOperator.AMPER);

  @Test
  public void test_local_start_routine_arg_implicit_global() throws UnsupportedCodeException {
    // arg = &local_l1; i.e. start_routine arg assignment
    Optional<SeqPointerAssignment> parameterAssignment =
        SeqPointerAliasingUtil.tryBuildPointerAssignment(
            START_ROUTINE_ARG_ID_EXPRESSION,
            LOCAL_L1_UNARY_EXPRESSION,
            Optional.empty(),
            Optional.empty(),
            ImmutableSortedMap.of(),
            SeqPointerAssignmentType.START_ROUTINE_ARG);

    assertThat(parameterAssignment).isPresent();

    SeqMemoryLocation argMemoryLocation =
        parameterAssignment.orElseThrow().leftHandSideMemoryLocation();
    SeqMemoryLocation l1MemoryLocation =
        parameterAssignment.orElseThrow().rightHandSideMemoryLocation();

    // local_L1 is now an implicit global memory location, due to start_routine_arg assignment
    assertThat(l1MemoryLocation.isGlobal()).isFalse();
    assertThat(
            SeqPointerAliasingMapBuilder.isImplicitGlobal(
                l1MemoryLocation,
                ImmutableSet.of(parameterAssignment.orElseThrow()),
                ImmutableSet.of()))
        .isTrue();
    // start_routine_arg is not explicit or implicit global
    assertThat(argMemoryLocation.isGlobal()).isFalse();
    assertThat(
            SeqPointerAliasingMapBuilder.isImplicitGlobal(
                argMemoryLocation,
                ImmutableSet.of(parameterAssignment.orElseThrow()),
                ImmutableSet.of()))
        .isFalse();
  }
}
