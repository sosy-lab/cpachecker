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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqInitializers.SeqInitializer;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqInitializers.SeqInitializerList;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqTypes.SeqArrayType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqTypes.SeqFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqTypes.SeqPointerType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqTypes.SeqSimpleType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqToken;

public class SeqDeclarations {

  public static class SeqVariableDeclaration {

    protected static final CVariableDeclaration DUMMY_PC =
        buildVariableDeclaration(
            false, SeqArrayType.INT_ARRAY, SeqToken.pc, SeqInitializerList.EMPTY_LIST);

    public static final CVariableDeclaration NEXT_THREAD_SIGNED =
        buildVariableDeclaration(
            false, SeqSimpleType.INT, SeqToken.next_thread, SeqInitializer.INT_MINUS_1);

    public static final CVariableDeclaration NEXT_THREAD_UNSIGNED =
        buildVariableDeclaration(
            false, SeqSimpleType.UNSIGNED_INT, SeqToken.next_thread, SeqInitializer.INT_0);
  }

  public static class SeqParameterDeclaration {

    public static final CParameterDeclaration COND =
        new CParameterDeclaration(FileLocation.DUMMY, SeqSimpleType.CONST_INT, SeqToken.cond);

    public static final CParameterDeclaration ASSERTION =
        new CParameterDeclaration(
            FileLocation.DUMMY, SeqPointerType.POINTER_CONST_CHAR, SeqToken.__assertion);

    public static final CParameterDeclaration FILE =
        new CParameterDeclaration(
            FileLocation.DUMMY, SeqPointerType.POINTER_CONST_CHAR, SeqToken.__file);

    public static final CParameterDeclaration LINE =
        new CParameterDeclaration(FileLocation.DUMMY, SeqSimpleType.UNSIGNED_INT, SeqToken.__line);

    public static final CParameterDeclaration FUNCTION =
        new CParameterDeclaration(
            FileLocation.DUMMY, SeqPointerType.POINTER_CONST_CHAR, SeqToken.__function);
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
            SeqToken.__VERIFIER_nondet_int,
            ImmutableList.of(),
            ImmutableSet.of());

    public static final CFunctionDeclaration VERIFIER_NONDET_UINT =
        new CFunctionDeclaration(
            FileLocation.DUMMY,
            SeqFunctionType.VERIFIER_NONDET_UINT,
            SeqToken.__VERIFIER_nondet_uint,
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
            SeqToken.__assert_fail,
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
            SeqNameUtil.buildFuncName(SeqToken.assume),
            ImmutableList.of(SeqParameterDeclaration.COND),
            ImmutableSet.of());

    public static final CFunctionDeclaration MAIN =
        new CFunctionDeclaration(
            FileLocation.DUMMY,
            SeqFunctionType.MAIN,
            SeqToken.main,
            ImmutableList.of(),
            ImmutableSet.of());
  }
}
