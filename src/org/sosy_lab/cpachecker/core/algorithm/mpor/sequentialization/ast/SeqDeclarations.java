// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration.FunctionAttribute;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqNameBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqInitializers.SeqInitializer;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqInitializers.SeqInitializerList;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqTypes.SeqArrayType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqTypes.SeqFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqTypes.SeqPointerType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqTypes.SeqSimpleType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqToken;

public class SeqDeclarations {

  public static class SeqVariableDeclaration {

    protected static final CVariableDeclaration DUMMY_PC =
        buildVarDec(false, SeqArrayType.INT_ARRAY, SeqToken.PC, SeqInitializerList.EMPTY_LIST);

    public static final CVariableDeclaration PREV_THREAD =
        buildVarDec(false, SeqSimpleType.INT, SeqToken.PREV_THREAD, SeqInitializer.INT_MINUS_1);

    public static final CVariableDeclaration NEXT_THREAD =
        buildVarDec(false, SeqSimpleType.INT, SeqToken.NEXT_THREAD, SeqInitializer.INT_MINUS_1);

    // TODO SubstituteBuilder.substituteVarDec also uses CVariableDeclaration constructor
    public static CVariableDeclaration buildVarDec(
        boolean pIsGlobal, CType pCType, String pName, CInitializer pInitializer) {

      return new CVariableDeclaration(
          FileLocation.DUMMY,
          pIsGlobal,
          CStorageClass.AUTO,
          pCType,
          pName,
          pName,
          SeqNameBuilder.createQualifiedName(pName),
          pInitializer);
    }

    /**
     * Creates a {@link CVariableDeclaration} of the form {@code int
     * __return_pc_t{pThreadId}_{pFuncName};}.
     */
    public static CVariableDeclaration buildReturnPcVarDec(int pThreadId, String pFuncName) {
      String varName = SeqNameBuilder.createReturnPcName(pThreadId, pFuncName);
      // TODO initialize with -2 and assert that it is not -2 when assigning in the
      //  sequentialization?
      return buildVarDec(true, SeqSimpleType.INT, varName, SeqInitializer.INT_0);
    }
  }

  public static class SeqParameterDeclaration {

    public static final CParameterDeclaration COND =
        new CParameterDeclaration(FileLocation.DUMMY, SeqSimpleType.CONST_INT, SeqToken.COND);

    public static final CParameterDeclaration ASSERTION =
        new CParameterDeclaration(
            FileLocation.DUMMY, SeqPointerType.POINTER_CONST_CHAR, SeqToken.__ASSERTION);

    public static final CParameterDeclaration FILE =
        new CParameterDeclaration(
            FileLocation.DUMMY, SeqPointerType.POINTER_CONST_CHAR, SeqToken.__FILE);

    public static final CParameterDeclaration LINE =
        new CParameterDeclaration(FileLocation.DUMMY, SeqSimpleType.UNSIGNED_INT, SeqToken.__LINE);

    public static final CParameterDeclaration FUNCTION =
        new CParameterDeclaration(
            FileLocation.DUMMY, SeqPointerType.POINTER_CONST_CHAR, SeqToken.__FUNCTION);
  }

  public static class SeqFunctionDeclaration {

    public static final CFunctionDeclaration ABORT =
        new CFunctionDeclaration(
            FileLocation.DUMMY,
            SeqFunctionType.ABORT,
            SeqToken.ABORT,
            ImmutableList.of(),
            ImmutableSet.of(FunctionAttribute.NO_RETURN));

    public static final CFunctionDeclaration VERIFIER_NONDET_INT =
        new CFunctionDeclaration(
            FileLocation.DUMMY,
            SeqFunctionType.VERIFIER_NONDET_INT,
            SeqToken.__VERIFIER_NONDET_INT,
            ImmutableList.of(),
            ImmutableSet.of());

    public static final CFunctionDeclaration ASSERT_FAIL =
        new CFunctionDeclaration(
            FileLocation.DUMMY,
            SeqFunctionType.ASSERT_FAIL,
            SeqToken.__ASSERT_FAIL,
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
            SeqNameBuilder.createFuncName(SeqToken.ASSUME),
            ImmutableList.of(SeqParameterDeclaration.COND),
            ImmutableSet.of());

    public static final CFunctionDeclaration MAIN =
        new CFunctionDeclaration(
            FileLocation.DUMMY,
            SeqFunctionType.MAIN,
            SeqToken.MAIN,
            ImmutableList.of(),
            ImmutableSet.of());
  }
}