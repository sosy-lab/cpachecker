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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqNameUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;

public class SubstituteBuilder {

  // Edge Substitutes ============================================================================

  public static ImmutableMap<ThreadEdge, SubstituteEdge> substituteEdges(
      ImmutableMap<MPORThread, CSimpleDeclarationSubstitution> pSubstitutions) {

    Map<ThreadEdge, SubstituteEdge> rSubstitutes = new HashMap<>();
    for (var entry : pSubstitutions.entrySet()) {
      MPORThread thread = entry.getKey();
      CSimpleDeclarationSubstitution substitution = entry.getValue();

      for (ThreadEdge threadEdge : thread.cfa.threadEdges) {
        // prevent duplicate keys by excluding parallel edges
        if (!rSubstitutes.containsKey(threadEdge)) {
          CFAEdge cfaEdge = threadEdge.cfaEdge;
          // if edge is not substituted: just use original edge
          SubstituteEdge substitute = new SubstituteEdge(cfaEdge);

          if (cfaEdge instanceof CDeclarationEdge decl) {
            // TODO what about structs?
            CDeclaration dec = decl.getDeclaration();
            if (dec instanceof CVariableDeclaration) {
              CVariableDeclaration varDec = substitution.getVarDeclarationSubstitute(dec);
              substitute = new SubstituteEdge(substituteDeclarationEdge(decl, varDec));
            }

          } else if (cfaEdge instanceof CAssumeEdge assume) {
            substitute =
                new SubstituteEdge(
                    substituteAssumeEdge(assume, substitution.substitute(assume.getExpression())));

          } else if (cfaEdge instanceof CStatementEdge stmt) {
            substitute =
                new SubstituteEdge(
                    substituteStatementEdge(stmt, substitution.substitute(stmt.getStatement())));

          } else if (cfaEdge instanceof CFunctionSummaryEdge funcSumm) {
            // only substitute assignments (e.g. CPAchecker_TMP = func();)
            if (funcSumm.getExpression() instanceof CFunctionCallAssignmentStatement assignStmt) {
              substitute =
                  new SubstituteEdge(
                      substituteFunctionSummaryEdge(funcSumm, substitution.substitute(assignStmt)));
            }

          } else if (cfaEdge instanceof CFunctionCallEdge funcCall) {
            // CFunctionCallEdges also assign CPAchecker_TMPs -> handle assignment statements here
            // too
            substitute =
                new SubstituteEdge(
                    substituteFunctionCallEdge(
                        funcCall,
                        (CFunctionCall) substitution.substitute(funcCall.getFunctionCall())));

          } else if (cfaEdge instanceof CReturnStatementEdge retStmt) {
            substitute =
                new SubstituteEdge(
                    substituteReturnStatementEdge(
                        retStmt, substitution.substitute(retStmt.getReturnStatement())));
          }

          rSubstitutes.put(threadEdge, substitute);
        }
      }
    }
    return ImmutableMap.copyOf(rSubstitutes);
  }

  /**
   * Creates a clone of the given CVariableDeclaration with substituted name(s).
   *
   * @param pOriginal the variable declaration to substitute
   */
  private static CVariableDeclaration substituteVarDeclaration(
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
  private static CVariableDeclaration substituteVarDeclaration(
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

  private static CInitializerExpression substituteInitExpr(
      CInitializerExpression pOriginal, CExpression pExpression) {
    return new CInitializerExpression(pOriginal.getFileLocation(), pExpression);
  }

  private static CAssumeEdge substituteAssumeEdge(CAssumeEdge pOriginal, CExpression pExpr) {
    return new CAssumeEdge(
        pOriginal.getRawStatement(),
        pOriginal.getFileLocation(),
        pOriginal.getPredecessor(),
        pOriginal.getSuccessor(),
        pExpr,
        pOriginal.getTruthAssumption());
  }

  private static CDeclarationEdge substituteDeclarationEdge(
      CDeclarationEdge pOriginal, CVariableDeclaration pVarDec) {
    return new CDeclarationEdge(
        pOriginal.getRawStatement(),
        pOriginal.getFileLocation(),
        pOriginal.getPredecessor(),
        pOriginal.getSuccessor(),
        pVarDec);
  }

  private static CStatementEdge substituteStatementEdge(
      CStatementEdge pOriginal, CStatement pStmt) {
    return new CStatementEdge(
        pOriginal.getRawStatement(),
        pStmt,
        pOriginal.getFileLocation(),
        pOriginal.getPredecessor(),
        pOriginal.getSuccessor());
  }

  private static CFunctionSummaryEdge substituteFunctionSummaryEdge(
      CFunctionSummaryEdge pOriginal, CStatement pFuncCall) {
    return new CFunctionSummaryEdge(
        pOriginal.getRawStatement(),
        pOriginal.getFileLocation(),
        pOriginal.getPredecessor(),
        pOriginal.getSuccessor(),
        (CFunctionCall) pFuncCall,
        pOriginal.getFunctionEntry());
  }

  private static CFunctionCallEdge substituteFunctionCallEdge(
      CFunctionCallEdge pOriginal, CFunctionCall pFuncCall) {
    return new CFunctionCallEdge(
        pOriginal.getRawStatement(),
        pOriginal.getFileLocation(),
        pOriginal.getPredecessor(),
        pOriginal.getSuccessor(),
        pFuncCall,
        pOriginal.getSummaryEdge());
  }

  private static CReturnStatementEdge substituteReturnStatementEdge(
      CReturnStatementEdge pOriginal, CReturnStatement pRetStmt) {
    return new CReturnStatementEdge(
        pOriginal.getRawStatement(),
        pRetStmt,
        pOriginal.getFileLocation(),
        pOriginal.getPredecessor(),
        pOriginal.getSuccessor());
  }

  // Thread Substitutions ========================================================================

  public static ImmutableMap<MPORThread, CSimpleDeclarationSubstitution> buildSubstitutions(
      ImmutableSet<CVariableDeclaration> pGlobalVars, ImmutableSet<MPORThread> pThreads) {

    ImmutableMap.Builder<MPORThread, CSimpleDeclarationSubstitution> rDeclarationSubstitutions =
        ImmutableMap.builder();
    // create global vars up front, their initializer cannot contain local variables
    ImmutableMap<CVariableDeclaration, CIdExpression> globalVarSubstitutes =
        getVarSubstitutes(Optional.empty(), Optional.empty(), 0, pGlobalVars);
    for (MPORThread thread : pThreads) {
      ImmutableMap<CParameterDeclaration, CIdExpression> parameterSubstitutes =
          getParameterSubstitutes(thread);
      ImmutableMap<CVariableDeclaration, CIdExpression> localVarSubstitutes =
          getVarSubstitutes(
              Optional.of(globalVarSubstitutes),
              Optional.of(parameterSubstitutes),
              thread.id,
              thread.localVars);
      rDeclarationSubstitutions.put(
          thread,
          new CSimpleDeclarationSubstitution(
              Optional.of(globalVarSubstitutes),
              localVarSubstitutes,
              Optional.of(parameterSubstitutes)));
    }
    return rDeclarationSubstitutions.buildOrThrow();
  }

  private static ImmutableMap<CParameterDeclaration, CIdExpression> getParameterSubstitutes(
      MPORThread pThread) {

    ImmutableMap.Builder<CParameterDeclaration, CIdExpression> rThreadSubs = ImmutableMap.builder();
    for (CFunctionDeclaration funcDec : pThread.cfa.calledFuncs) {
      for (CParameterDeclaration paramDec : funcDec.getParameters()) {
        String varName = SeqNameUtil.buildParameterName(paramDec, pThread.id);
        CVariableDeclaration varDec =
            substituteVarDeclaration(paramDec.asVariableDeclaration(), varName);
        rThreadSubs.put(paramDec, SeqIdExpression.buildIdExpr(varDec));
      }
    }
    return rThreadSubs.buildOrThrow();
  }

  /**
   * Creates substitutes for all variables in the program and maps them to their original. The
   * substitutes differ only in their name.
   */
  private static ImmutableMap<CVariableDeclaration, CIdExpression> getVarSubstitutes(
      Optional<ImmutableMap<CVariableDeclaration, CIdExpression>> pGlobalVarSubstitutes,
      Optional<ImmutableMap<CParameterDeclaration, CIdExpression>> pParameterSubstitutes,
      int pThreadId,
      ImmutableSet<CVariableDeclaration> pVarDeclarations) {

    // step 1: create dummy CVariableDeclaration substitutes which may be adjusted in step 2
    ImmutableMap.Builder<CVariableDeclaration, CIdExpression> dummyVarSubsB =
        ImmutableMap.builder();
    for (CVariableDeclaration varDec : pVarDeclarations) {
      String substituteName = SeqNameUtil.buildVarName(varDec, pThreadId);
      CVariableDeclaration substitute = substituteVarDeclaration(varDec, substituteName);
      dummyVarSubsB.put(varDec, SeqIdExpression.buildIdExpr(substitute));
    }
    ImmutableMap<CVariableDeclaration, CIdExpression> dummyLocalVarSubs =
        dummyVarSubsB.buildOrThrow();

    // create dummy substitution
    CSimpleDeclarationSubstitution dummySubstitution =
        new CSimpleDeclarationSubstitution(
            pGlobalVarSubstitutes, dummyLocalVarSubs, pParameterSubstitutes);

    // step 2: replace initializers of CVariableDeclarations with substitutes
    ImmutableMap.Builder<CVariableDeclaration, CIdExpression> rFinalSubs = ImmutableMap.builder();
    // TODO handle CInitializerList?
    for (var entry : dummyLocalVarSubs.entrySet()) {
      CIdExpression idExpression = entry.getValue();

      assert idExpression.getDeclaration() instanceof CVariableDeclaration;
      CVariableDeclaration varDeclaration = (CVariableDeclaration) idExpression.getDeclaration();
      CInitializer initializer = varDeclaration.getInitializer();

      if (initializer instanceof CInitializerExpression initExpr) {
        CInitializerExpression initExprSub =
            substituteInitExpr(initExpr, dummySubstitution.substitute(initExpr.getExpression()));
        CVariableDeclaration finalSub = substituteVarDeclaration(varDeclaration, initExprSub);
        rFinalSubs.put(entry.getKey(), SeqIdExpression.buildIdExpr(finalSub));
        continue;
      }
      rFinalSubs.put(entry);
    }
    return rFinalSubs.buildOrThrow();
  }
}
