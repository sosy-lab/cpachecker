// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.loop_case.statements;

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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqToken;

public class SeqDeclarations {

  // CVariableDeclaration ========================================================================

  public static final CVariableDeclaration PC =
      buildVarDec(false, SeqTypes.PC, SeqToken.PC, SeqInitializers.INT_0);

  // CParameterDeclaration =======================================================================

  public static final CParameterDeclaration COND =
      new CParameterDeclaration(FileLocation.DUMMY, SeqTypes.CONST_INT, SeqToken.COND);

  public static final CParameterDeclaration ARRAY =
      new CParameterDeclaration(
          FileLocation.DUMMY, SeqTypes.CONST_POINTER_CONST_INT, SeqToken.ARRAY);

  public static final CParameterDeclaration SIZE =
      new CParameterDeclaration(FileLocation.DUMMY, SeqTypes.CONST_INT, SeqToken.SIZE);

  // CFunctionDeclarations =======================================================================

  public static final CFunctionDeclaration VERIFIER_NONDET_INT =
      new CFunctionDeclaration(
          FileLocation.DUMMY,
          SeqTypes.VERIFIER_NONDET_INT,
          SeqToken.VERIFIER_NONDET_INT,
          ImmutableList.of(),
          ImmutableSet.of());

  public static final CFunctionDeclaration ASSUME =
      new CFunctionDeclaration(
          FileLocation.DUMMY,
          SeqTypes.ASSUME,
          SeqNameBuilder.createFuncName(SeqToken.ASSUME),
          ImmutableList.of(COND),
          ImmutableSet.of(FunctionAttribute.NO_RETURN));

  public static final CFunctionDeclaration ANY_UNSIGNED =
      new CFunctionDeclaration(
          FileLocation.DUMMY,
          SeqTypes.ANY_UNSIGNED,
          SeqNameBuilder.createFuncName(SeqToken.ANY_UNSIGNED),
          ImmutableList.of(ARRAY, SIZE),
          ImmutableSet.of());

  public static final CFunctionDeclaration MAIN =
      new CFunctionDeclaration(
          FileLocation.DUMMY, SeqTypes.MAIN, SeqToken.MAIN, ImmutableList.of(), ImmutableSet.of());

  // Build Functions =============================================================================

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
    // TODO initialize with -2 and assert that it is not -2 when assigning in the sequentialization?
    return buildVarDec(true, SeqTypes.INT, varName, SeqInitializers.INT_0);
  }
}
