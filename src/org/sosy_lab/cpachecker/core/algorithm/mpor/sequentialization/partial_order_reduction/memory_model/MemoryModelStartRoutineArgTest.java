// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import java.math.BigInteger;
import java.util.Optional;
import org.junit.Test;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeQualifiers;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;

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

  private final class CParameterDeclarations {
    private final CParameterDeclaration START_ROUTINE_ARG_DECLARATION =
        new CParameterDeclaration(FileLocation.DUMMY, VOID_POINTER_TYPE, "start_routine_arg");

    CParameterDeclarations() {
      // qualified names are required, otherwise .asVariableDeclaration throws
      START_ROUTINE_ARG_DECLARATION.setQualifiedName("dummy");
    }
  }

  // Memory Locations (primitives)

  private final SeqMemoryLocation LOCAL_L1_MEMORY_LOCATION =
      SeqMemoryLocation.of(
          MPOROptions.getDefaultTestInstance(), Optional.empty(), LOCAL_L1_DECLARATION);

  private final CParameterDeclarations PARAMETER_DECLARATIONS = new CParameterDeclarations();

  private final SeqMemoryLocation START_ROUTINE_ARG_MEMORY_LOCATION =
      SeqMemoryLocation.of(
          MPOROptions.getDefaultTestInstance(),
          Optional.of(MemoryModelParameterTest.DUMMY_CALL_CONTEXT),
          PARAMETER_DECLARATIONS.START_ROUTINE_ARG_DECLARATION.asVariableDeclaration());

  public MemoryModelStartRoutineArgTest() throws InvalidConfigurationException {}

  @Test
  public void test_local_start_routine_arg_implicit_global() {
    // param_ptr_P = &global_X; i.e. pointer parameter assignment
    ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> startRoutineArgAssignments =
        ImmutableMap.<SeqMemoryLocation, SeqMemoryLocation>builder()
            .put(START_ROUTINE_ARG_MEMORY_LOCATION, LOCAL_L1_MEMORY_LOCATION)
            .buildOrThrow();
    ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> pointerParameterAssignments =
        MemoryModelBuilder.getPointerParameterAssignments(startRoutineArgAssignments);

    // check that start_routine_arg assignment is recognized as pointer parameter (void *)
    assertThat(pointerParameterAssignments).hasSize(1);

    // local_L1 is now an implicit global memory location, due to start_routine_arg assignment
    assertThat(LOCAL_L1_MEMORY_LOCATION.declaration().isGlobal()).isFalse();
    assertThat(
            MemoryModelBuilder.isImplicitGlobal(
                LOCAL_L1_MEMORY_LOCATION,
                ImmutableSetMultimap.of(),
                startRoutineArgAssignments,
                pointerParameterAssignments,
                ImmutableSet.of()))
        .isTrue();
    // start_routine_arg is not explicit or implicit global
    assertThat(START_ROUTINE_ARG_MEMORY_LOCATION.declaration().isGlobal()).isFalse();
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
