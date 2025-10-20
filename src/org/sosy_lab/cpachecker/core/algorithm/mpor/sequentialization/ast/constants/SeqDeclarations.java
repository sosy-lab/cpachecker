// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants;

import static org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqDeclarationBuilder.buildVariableDeclaration;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration.FunctionAttribute;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqInitializers.SeqInitializer;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqInitializers.SeqInitializerList;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqTypes.SeqArrayType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqTypes.SeqFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions.VerifierNondetFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqToken;

public class SeqDeclarations {

  public static class SeqVariableDeclaration {

    protected static final CVariableDeclaration DUMMY_PC =
        buildVariableDeclaration(
            false, SeqArrayType.UNSIGNED_INT_ARRAY, SeqToken.pc, SeqInitializerList.EMPTY_LIST);

    // last_thread, initialized with 0, but updated later in main()

    public static final CVariableDeclaration LAST_THREAD_UNSIGNED =
        buildVariableDeclaration(
            true, CNumericTypes.UNSIGNED_INT, SeqToken.last_thread, SeqInitializer.INT_0);

    // next_thread

    public static final CVariableDeclaration NEXT_THREAD_SIGNED =
        buildVariableDeclaration(
            true, CNumericTypes.INT, SeqToken.next_thread, SeqInitializer.INT_0);

    public static final CVariableDeclaration NEXT_THREAD_UNSIGNED =
        buildVariableDeclaration(
            true, CNumericTypes.UNSIGNED_INT, SeqToken.next_thread, SeqInitializer.INT_0);

    // cnt (thread count)

    public static final CVariableDeclaration CNT =
        buildVariableDeclaration(
            false, CNumericTypes.UNSIGNED_INT, SeqToken.cnt, SeqInitializer.INT_1);

    // round_max

    public static final CVariableDeclaration ROUND_MAX_SIGNED =
        buildVariableDeclaration(
            false, CNumericTypes.INT, SeqToken.round_max, SeqInitializer.INT_0);

    public static final CVariableDeclaration ROUND_MAX_UNSIGNED =
        buildVariableDeclaration(
            false, CNumericTypes.UNSIGNED_INT, SeqToken.round_max, SeqInitializer.INT_0);

    // round

    public static final CVariableDeclaration ROUND =
        buildVariableDeclaration(
            false, CNumericTypes.UNSIGNED_INT, SeqToken.round, SeqInitializer.INT_0);

    // iteration

    public static final CVariableDeclaration ITERATION =
        buildVariableDeclaration(
            false, CNumericTypes.INT, SeqToken.iteration, SeqInitializer.INT_0);
  }

  public static class SeqParameterDeclaration {

    public static final CParameterDeclaration ASSERTION =
        new CParameterDeclaration(
            FileLocation.DUMMY,
            CPointerType.POINTER_TO_CONST_CHAR,
            SeqToken.ASSERTION_KEYWORD_ASSERT_FAIL);

    public static final CParameterDeclaration COND =
        new CParameterDeclaration(FileLocation.DUMMY, CNumericTypes.CONST_INT, SeqToken.cond);

    public static final CParameterDeclaration FUNCTION =
        new CParameterDeclaration(
            FileLocation.DUMMY,
            CPointerType.POINTER_TO_CONST_CHAR,
            SeqToken.FUNCTION_KEYWORD_ASSERT_FAIL);

    public static final CParameterDeclaration FILE =
        new CParameterDeclaration(
            FileLocation.DUMMY,
            CPointerType.POINTER_TO_CONST_CHAR,
            SeqToken.FILE_KEYWORD_ASSERT_FAIL);

    public static final CParameterDeclaration LINE =
        new CParameterDeclaration(
            FileLocation.DUMMY, CNumericTypes.UNSIGNED_INT, SeqToken.LINE_KEYWORD_ASSERT_FAIL);

    public static final CParameterDeclaration SIZE =
        new CParameterDeclaration(FileLocation.DUMMY, CNumericTypes.UNSIGNED_INT, SeqToken.size);
  }

  public static class SeqFunctionDeclaration {

    public static final CFunctionDeclaration ABORT =
        new CFunctionDeclaration(
            FileLocation.DUMMY,
            SeqFunctionType.ABORT,
            SeqToken.abort,
            ImmutableList.of(),
            ImmutableSet.of(FunctionAttribute.NO_RETURN));

    public static final CFunctionDeclaration VERIFIER_NONDET_INT =
        new CFunctionDeclaration(
            FileLocation.DUMMY,
            SeqFunctionType.VERIFIER_NONDET_INT,
            VerifierNondetFunctionType.INT.getName(),
            ImmutableList.of(),
            ImmutableSet.of());

    public static final CFunctionDeclaration VERIFIER_NONDET_UINT =
        new CFunctionDeclaration(
            FileLocation.DUMMY,
            SeqFunctionType.VERIFIER_NONDET_UINT,
            VerifierNondetFunctionType.UINT.getName(),
            ImmutableList.of(),
            ImmutableSet.of());

    public static final CFunctionDeclaration REACH_ERROR =
        new CFunctionDeclaration(
            FileLocation.DUMMY,
            SeqFunctionType.REACH_ERROR,
            SeqToken.reach_error,
            ImmutableList.of(
                SeqParameterDeclaration.FILE,
                SeqParameterDeclaration.LINE,
                SeqParameterDeclaration.FUNCTION),
            ImmutableSet.of(FunctionAttribute.NO_RETURN));

    public static final CFunctionDeclaration ASSERT_FAIL =
        new CFunctionDeclaration(
            FileLocation.DUMMY,
            SeqFunctionType.ASSERT_FAIL,
            SeqToken.ASSERT_FAIL_KEYWORD,
            ImmutableList.of(
                SeqParameterDeclaration.ASSERTION,
                SeqParameterDeclaration.FILE,
                SeqParameterDeclaration.LINE,
                SeqParameterDeclaration.FUNCTION),
            ImmutableSet.of(FunctionAttribute.NO_RETURN));

    public static final CFunctionDeclaration ASSUME =
        new CFunctionDeclaration(
            FileLocation.DUMMY,
            SeqFunctionType.ASSUME,
            SeqNameUtil.buildFunctionName(SeqToken.assume),
            ImmutableList.of(SeqParameterDeclaration.COND),
            ImmutableSet.of());

    public static final CFunctionDeclaration MAIN =
        new CFunctionDeclaration(
            FileLocation.DUMMY,
            SeqFunctionType.MAIN,
            SeqToken.main,
            ImmutableList.of(),
            ImmutableSet.of());

    public static final CFunctionDeclaration MALLOC =
        new CFunctionDeclaration(
            FileLocation.DUMMY,
            SeqFunctionType.MALLOC,
            SeqToken.malloc,
            ImmutableList.of(SeqParameterDeclaration.SIZE),
            ImmutableSet.of());
  }
}
