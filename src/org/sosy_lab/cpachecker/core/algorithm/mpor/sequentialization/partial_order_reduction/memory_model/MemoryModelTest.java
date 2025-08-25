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
import com.google.common.collect.ImmutableTable;
import java.math.BigInteger;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.nondeterminism.NondeterminismSource;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.multi_control.MultiControlStatementEncoding;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorEncoding;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.ReductionMode;

public class MemoryModelTest {

  private final MPOROptions testOptions =
      MPOROptions.testInstance(
          true,
          BitVectorEncoding.NONE,
          false,
          false,
          false,
          false,
          MultiControlStatementEncoding.NONE,
          MultiControlStatementEncoding.SWITCH_CASE,
          false,
          false,
          false,
          false,
          false,
          // enable link reduction, otherwise MemoryModel is not created
          true,
          false,
          0,
          false,
          NondeterminismSource.NUM_STATEMENTS,
          false,
          false,
          ReductionMode.NONE,
          true,
          false,
          false);

  private final CSimpleType intType =
      new CSimpleType(false, false, CBasicType.INT, false, false, true, false, false, false, false);

  private final CIntegerLiteralExpression int_0 =
      new CIntegerLiteralExpression(FileLocation.DUMMY, intType, BigInteger.valueOf(0));

  private final CPointerType intPointerType = new CPointerType(false, false, intType);

  private final CInitializer pointerInitializer =
      new CInitializerExpression(FileLocation.DUMMY, int_0);

  private final CVariableDeclaration pointerDeclaration =
      new CVariableDeclaration(
          FileLocation.DUMMY,
          true,
          CStorageClass.AUTO,
          intPointerType,
          "int_ptr",
          "int_ptr",
          "int_ptr",
          pointerInitializer);

  private final CIdExpression pointer = new CIdExpression(FileLocation.DUMMY, pointerDeclaration);

  // Memory Locations

  @Test
  public void testPointerAssignment() {
    MemoryModel testMemoryModel =
        new MemoryModel(
            ImmutableMap.of(), ImmutableSetMultimap.of(), ImmutableTable.of(), ImmutableSet.of());
    assertThat(testMemoryModel).isNotNull();
  }
}
