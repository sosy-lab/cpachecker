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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqToken;

public class SeqVariableDeclarations {

  static final CVariableDeclaration PC_ARRAY_DUMMY =
      buildVariableDeclaration(
          false,
          SeqArrayTypes.UNSIGNED_INT_ARRAY,
          SeqToken.PROGRAM_COUNTER_VARIABLE,
          SeqInitializers.EMPTY_LIST);

  // last_thread

  public static final CVariableDeclaration LAST_THREAD_DUMMY =
      buildVariableDeclaration(
          true,
          CNumericTypes.UNSIGNED_INT,
          SeqToken.LAST_KEYWORD + SeqSyntax.UNDERSCORE + SeqToken.THREAD,
          SeqInitializers.INT_0);

  // next_thread

  public static final CVariableDeclaration NEXT_THREAD_DUMMY =
      buildVariableDeclaration(
          true, CNumericTypes.INT, SeqToken.NEXT_THREAD_VARIABLE, SeqInitializers.INT_0);

  // cnt (thread count)

  public static final CVariableDeclaration CNT =
      buildVariableDeclaration(
          false, CNumericTypes.UNSIGNED_INT, SeqToken.THREAD_COUNT_VARIABLE, SeqInitializers.INT_1);

  // round_max

  public static final CVariableDeclaration ROUND_MAX_DUMMY =
      buildVariableDeclaration(
          false, CNumericTypes.INT, SeqToken.ROUND_MAX_VARIABLE, SeqInitializers.INT_0);

  // round

  public static final CVariableDeclaration ROUND =
      buildVariableDeclaration(
          false, CNumericTypes.UNSIGNED_INT, SeqToken.ROUND_VARIABLE, SeqInitializers.INT_0);

  // iteration

  public static final CVariableDeclaration ITERATION =
      buildVariableDeclaration(
          false, CNumericTypes.INT, SeqToken.ITERATION_VARIABLE, SeqInitializers.INT_0);
}
