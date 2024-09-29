// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.substitution;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqNameBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public class SubstituteBuilder {

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
        pOriginal.getOrigName(), // TODO (not relevant for seq)
        pOriginal.getQualifiedName(), // TODO funcName::name but not relevant for seq
        pOriginal.getInitializer());
  }

  /**
   * Creates a clone of the given CVariableDeclaration with substituted name(s) and initializer.
   *
   * @param pOriginal the variable declaration to substitute
   */
  public static CVariableDeclaration substituteVarDec(
      CVariableDeclaration pOriginal, CInitializerExpression pInitExpr) {
    return new CVariableDeclaration(
        pOriginal.getFileLocation(),
        pOriginal.isGlobal(),
        pOriginal.getCStorageClass(),
        pOriginal.getType(),
        pOriginal.getName(),
        pOriginal.getOrigName(),
        pOriginal.getQualifiedName(), // TODO funcName::name but not relevant for seq
        pInitExpr);
  }

  public static CInitializerExpression substituteInitExpr(
      CInitializerExpression pOriginal, CExpression pExpression) {
    return new CInitializerExpression(pOriginal.getFileLocation(), pExpression);
  }

  public static CAssumeEdge substituteAssumeEdge(CAssumeEdge pOriginal, CExpression pExpr) {
    return new CAssumeEdge(
        pOriginal.getRawStatement(),
        pOriginal.getFileLocation(),
        pOriginal.getPredecessor(),
        pOriginal.getSuccessor(),
        pExpr,
        pOriginal.getTruthAssumption());
  }

  public static CDeclarationEdge substituteDeclarationEdge(
      CDeclarationEdge pOriginal, CVariableDeclaration pVarDec) {
    return new CDeclarationEdge(
        pOriginal.getRawStatement(),
        pOriginal.getFileLocation(),
        pOriginal.getPredecessor(),
        pOriginal.getSuccessor(),
        pVarDec);
  }

  public static CStatementEdge substituteStatementEdge(CStatementEdge pOriginal, CStatement pStmt) {
    return new CStatementEdge(
        pOriginal.getRawStatement(),
        pStmt,
        pOriginal.getFileLocation(),
        pOriginal.getPredecessor(),
        pOriginal.getSuccessor());
  }

  public static CFunctionSummaryEdge substituteFunctionSummaryEdge(
      CFunctionSummaryEdge pOriginal, CStatement pFuncCall) {
    return new CFunctionSummaryEdge(
        pOriginal.getRawStatement(),
        pOriginal.getFileLocation(),
        pOriginal.getPredecessor(),
        pOriginal.getSuccessor(),
        (CFunctionCall) pFuncCall,
        pOriginal.getFunctionEntry());
  }

  public static CFunctionCallEdge substituteFunctionCallEdge(
      CFunctionCallEdge pOriginal, CFunctionCall pFuncCall) {
    return new CFunctionCallEdge(
        pOriginal.getRawStatement(),
        pOriginal.getFileLocation(),
        pOriginal.getPredecessor(),
        pOriginal.getSuccessor(),
        pFuncCall,
        pOriginal.getSummaryEdge());
  }

  public static CFunctionCallExpression substituteFunctionCallExpr(
      CFunctionCallExpression pOriginal, List<CExpression> pParams) {
    return new CFunctionCallExpression(
        pOriginal.getFileLocation(),
        pOriginal.getExpressionType(),
        pOriginal.getFunctionNameExpression(),
        pParams,
        pOriginal.getDeclaration());
  }

  public static ImmutableMap<MPORThread, CSimpleDeclarationSubstitution> getDecSubstitutions(
      ImmutableSet<CVariableDeclaration> pGlobalVars,
      ImmutableSet<MPORThread> pThreads,
      CBinaryExpressionBuilder pBinExprBuilder) {
    ImmutableMap.Builder<MPORThread, CSimpleDeclarationSubstitution> rDecSubstitutions =
        ImmutableMap.builder();
    // create global vars up front, their initializer cannot contain local variables
    ImmutableMap<CVariableDeclaration, CVariableDeclaration> globalVarSubs =
        getVarSubs(null, null, 0, pGlobalVars, pBinExprBuilder);
    for (MPORThread thread : pThreads) {
      ImmutableMap<CParameterDeclaration, CVariableDeclaration> paramSubs = getParamSubs(thread);
      ImmutableMap<CVariableDeclaration, CVariableDeclaration> localVarSubs =
          getVarSubs(globalVarSubs, paramSubs, thread.id, thread.localVars, pBinExprBuilder);
      rDecSubstitutions.put(
          thread,
          new CSimpleDeclarationSubstitution(
              globalVarSubs, localVarSubs, paramSubs, pBinExprBuilder));
    }
    return rDecSubstitutions.buildOrThrow();
  }

  private static ImmutableMap<CParameterDeclaration, CVariableDeclaration> getParamSubs(
      MPORThread pThread) {
    ImmutableMap.Builder<CParameterDeclaration, CVariableDeclaration> rThreadSubs =
        ImmutableMap.builder();
    for (CFunctionDeclaration funcDec : pThread.cfa.calledFuncs) {
      for (CParameterDeclaration paramDec : funcDec.getParameters()) {
        String varName = SeqNameBuilder.createParamName(paramDec, pThread.id);
        rThreadSubs.put(
            paramDec,
            SubstituteBuilder.substituteVarDec(paramDec.asVariableDeclaration(), varName));
      }
    }
    return rThreadSubs.buildOrThrow();
  }

  /**
   * Creates substitutes for all variables in the program and maps them to their original. The
   * substitutes differ only in their name.
   */
  private static ImmutableMap<CVariableDeclaration, CVariableDeclaration> getVarSubs(
      @Nullable ImmutableMap<CVariableDeclaration, CVariableDeclaration> pGlobalVarSubs,
      @Nullable ImmutableMap<CParameterDeclaration, CVariableDeclaration> pParamSubs,
      int pThreadId,
      ImmutableSet<CVariableDeclaration> pVarDecs,
      CBinaryExpressionBuilder pBinExprBuilder) {

    // step 1: create dummy CVariableDeclaration substitutes which may be adjusted in step 2
    ImmutableMap.Builder<CVariableDeclaration, CVariableDeclaration> dummyVarSubsB =
        ImmutableMap.builder();
    for (CVariableDeclaration varDec : pVarDecs) {
      String substituteName = SeqNameBuilder.createVarName(varDec, pThreadId);
      CVariableDeclaration substitute = SubstituteBuilder.substituteVarDec(varDec, substituteName);
      dummyVarSubsB.put(varDec, substitute);
    }
    ImmutableMap<CVariableDeclaration, CVariableDeclaration> dummyLocalVarSubs =
        dummyVarSubsB.buildOrThrow();

    // create dummy substitution
    CSimpleDeclarationSubstitution dummySubstitution =
        new CSimpleDeclarationSubstitution(
            pGlobalVarSubs, dummyLocalVarSubs, pParamSubs, pBinExprBuilder);

    // step 2: replace initializers of CVariableDeclarations with substitutes
    ImmutableMap.Builder<CVariableDeclaration, CVariableDeclaration> rFinalSubs =
        ImmutableMap.builder();
    // TODO handle CInitializerList?
    for (var entry : dummyLocalVarSubs.entrySet()) {
      CInitializer initializer = entry.getValue().getInitializer();
      if (initializer != null) {
        if (initializer instanceof CInitializerExpression initExpr) {
          CInitializerExpression initExprSub =
              SubstituteBuilder.substituteInitExpr(
                  initExpr, dummySubstitution.substitute(initExpr.getExpression()));
          CVariableDeclaration finalSub =
              SubstituteBuilder.substituteVarDec(entry.getValue(), initExprSub);
          rFinalSubs.put(entry.getKey(), finalSub);
          continue;
        }
      }
      rFinalSubs.put(entry);
    }
    return rFinalSubs.buildOrThrow();
  }
}
