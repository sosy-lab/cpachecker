// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.substitution;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqToken;

public class SubstituteBuilder {

  private static int varId = 0;

  private static String createVarId() {
    return SeqSyntax.UNDERSCORE + varId++ + SeqSyntax.UNDERSCORE;
  }

  public static String substituteGlobalVarName(CVariableDeclaration pCVarDec) {
    return SeqToken.PREFIX_GLOBAL + createVarId() + pCVarDec.getName();
  }

  public static String substituteLocalVarName(CVariableDeclaration pCVarDec, int pThreadId) {
    return SeqToken.PREFIX_THREAD + pThreadId + createVarId() + pCVarDec.getName();
  }

  public static String substituteParamName(CParameterDeclaration pCParDec, int pThreadId) {
    return SeqToken.PREFIX_PARAMETER + pThreadId + createVarId() + pCParDec.getName();
  }

  // TODO createParameterVarSubstituteName

  /**
   * Creates a clone of the given CVariableDeclaration with substituted name(s).
   *
   * @param pOriginal the variable declaration to substitute
   */
  public static CVariableDeclaration substituteVarDec(
      CVariableDeclaration pOriginal, String pName) {
    return new CVariableDeclaration(
        pOriginal.getFileLocation(),
        pOriginal.isGlobal(),
        pOriginal.getCStorageClass(),
        pOriginal.getType(),
        pName,
        pName,
        pName, // TODO funcName::name but not relevant for seq
        pOriginal.getInitializer());
  }

  /**
   * Creates a clone of the given CVariableDeclaration with substituted name(s) and initializer.
   *
   * @param pOriginal the variable declaration to substitute
   */
  public static CVariableDeclaration substituteVarDec(
      CVariableDeclaration pOriginal, CInitializerExpression cInitExpr) {
    return new CVariableDeclaration(
        pOriginal.getFileLocation(),
        pOriginal.isGlobal(),
        pOriginal.getCStorageClass(),
        pOriginal.getType(),
        pOriginal.getName(),
        pOriginal.getOrigName(),
        pOriginal.getQualifiedName(), // TODO funcName::name but not relevant for seq
        cInitExpr);
  }

  public static CInitializerExpression substituteInitExpr(
      CInitializerExpression pOriginal, CExpression pExpression) {
    return new CInitializerExpression(pOriginal.getFileLocation(), pExpression);
  }

  public static CAssumeEdge substituteAssumeEdge(
      CAssumeEdge pOriginal, CExpression pSubstituteExpr) {
    return new CAssumeEdge(
        pOriginal.getRawStatement(),
        pOriginal.getFileLocation(),
        pOriginal.getPredecessor(),
        pOriginal.getSuccessor(),
        pSubstituteExpr,
        pOriginal.getTruthAssumption());
  }

  public static CDeclarationEdge substituteDeclarationEdge(
      CDeclarationEdge pOriginal, CVariableDeclaration pCVarDec) {
    return new CDeclarationEdge(
        pOriginal.getRawStatement(),
        pOriginal.getFileLocation(),
        pOriginal.getPredecessor(),
        pOriginal.getSuccessor(),
        pCVarDec);
  }

  public static CStatementEdge substituteStatementEdge(
      CStatementEdge pOriginal, CStatement pCStmt) {
    return new CStatementEdge(
        pOriginal.getRawStatement(),
        pCStmt,
        pOriginal.getFileLocation(),
        pOriginal.getPredecessor(),
        pOriginal.getSuccessor());
  }
}
