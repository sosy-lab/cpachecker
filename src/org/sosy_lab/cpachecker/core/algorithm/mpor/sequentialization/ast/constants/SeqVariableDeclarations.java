// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants;

import static org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqDeclarationBuilder.buildVariableDeclaration;

import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;

public class SeqVariableDeclarations {

  // last_thread

  public static final CVariableDeclaration LAST_THREAD_DUMMY =
      buildVariableDeclaration(
          true, CNumericTypes.UNSIGNED_INT, "LAST_THREAD", SeqInitializers.INT_0);

  // next_thread

  public static final CVariableDeclaration NEXT_THREAD_DUMMY =
      buildVariableDeclaration(true, CNumericTypes.INT, "next_thread", SeqInitializers.INT_0);

  // cnt (thread count)

  public static final CVariableDeclaration THREAD_COUNT =
      buildVariableDeclaration(
          false, CNumericTypes.UNSIGNED_INT, "thread_count", SeqInitializers.INT_1);

  // round_max

  public static final CVariableDeclaration ROUND_MAX_DUMMY =
      buildVariableDeclaration(false, CNumericTypes.INT, "round_max", SeqInitializers.INT_0);

  // round

  public static final CVariableDeclaration ROUND =
      buildVariableDeclaration(false, CNumericTypes.UNSIGNED_INT, "round", SeqInitializers.INT_0);

  // iteration

  public static final CVariableDeclaration ITERATION =
      buildVariableDeclaration(false, CNumericTypes.INT, "iteration", SeqInitializers.INT_0);
}
