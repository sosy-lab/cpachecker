// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.substitution;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
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
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;

public class SubstituteBuilder {

  // Edge Substitutes ============================================================================

  public static ImmutableMap<ThreadEdge, SubstituteEdge> substituteEdges(
      MPOROptions pOptions,
      ImmutableMap<MPORThread, CSimpleDeclarationSubstitution> pSubstitutions) {

    Map<ThreadEdge, SubstituteEdge> rSubstituteEdges = new HashMap<>();
    for (var entry : pSubstitutions.entrySet()) {
      MPORThread thread = entry.getKey();
      CSimpleDeclarationSubstitution substitution = entry.getValue();

      for (ThreadEdge threadEdge : thread.cfa.threadEdges) {
        // prevent duplicate keys by excluding parallel edges
        if (!rSubstituteEdges.containsKey(threadEdge)) {
          CFAEdge cfaEdge = threadEdge.cfaEdge;
          // if edge is not substituted: just use original edge
          Optional<SubstituteEdge> substitute = trySubstituteEdge(pOptions, substitution, cfaEdge);
          rSubstituteEdges.put(
              threadEdge,
              substitute.isPresent() ? substitute.orElseThrow() : new SubstituteEdge(cfaEdge));
        }
      }
    }
    return ImmutableMap.copyOf(rSubstituteEdges);
  }

  private static Optional<SubstituteEdge> trySubstituteEdge(
      MPOROptions pOptions, CSimpleDeclarationSubstitution pSubstitution, CFAEdge pCfaEdge) {

    if (pCfaEdge instanceof CDeclarationEdge declarationEdge) {
      // TODO what about structs?
      if (SubstituteUtil.isExcludedDeclarationEdge(pOptions, declarationEdge)) {
        return Optional.empty();
      } else {
        CDeclaration declaration = declarationEdge.getDeclaration();
        // we only substitute variables, not functions or types
        if (declaration instanceof CVariableDeclaration) {
          CVariableDeclaration variableDeclaration =
              pSubstitution.getVariableDeclarationSubstitute(declaration);
          return Optional.of(
              new SubstituteEdge(substituteDeclarationEdge(declarationEdge, variableDeclaration)));
        }
      }

    } else if (pCfaEdge instanceof CAssumeEdge assume) {
      return Optional.of(
          new SubstituteEdge(
              substituteAssumeEdge(assume, pSubstitution.substitute(assume.getExpression()))));

    } else if (pCfaEdge instanceof CStatementEdge statement) {
      return Optional.of(
          new SubstituteEdge(
              substituteStatementEdge(
                  statement, pSubstitution.substitute(statement.getStatement()))));

    } else if (pCfaEdge instanceof CFunctionSummaryEdge functionSummary) {
      // only substitute assignments (e.g. CPAchecker_TMP = func();)
      if (functionSummary.getExpression() instanceof CFunctionCallAssignmentStatement assignment) {
        return Optional.of(
            new SubstituteEdge(
                substituteFunctionSummaryEdge(
                    functionSummary, pSubstitution.substitute(assignment))));
      }

    } else if (pCfaEdge instanceof CFunctionCallEdge functionCall) {
      // CFunctionCallEdges also assign CPAchecker_TMPs -> handle assignment statements here
      // too
      return Optional.of(
          new SubstituteEdge(
              substituteFunctionCallEdge(
                  functionCall,
                  (CFunctionCall) pSubstitution.substitute(functionCall.getFunctionCall()))));

    } else if (pCfaEdge instanceof CReturnStatementEdge returnStatement) {
      return Optional.of(
          new SubstituteEdge(
              substituteReturnStatementEdge(
                  returnStatement,
                  pSubstitution.substitute(returnStatement.getReturnStatement()))));
    }
    return Optional.empty();
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
      CVariableDeclaration pOriginal, CInitializerExpression pInitExpression) {

    return new CVariableDeclaration(
        pOriginal.getFileLocation(),
        pOriginal.isGlobal(),
        pOriginal.getCStorageClass(),
        pOriginal.getType(),
        pOriginal.getName(),
        pOriginal.getOrigName(),
        pOriginal.getQualifiedName(), // TODO funcName::name but not relevant for seq
        pInitExpression);
  }

  private static CInitializerExpression substituteInitializerExpression(
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
      CStatementEdge pOriginal, CStatement pStatement) {

    return new CStatementEdge(
        pOriginal.getRawStatement(),
        pStatement,
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
      CReturnStatementEdge pOriginal, CReturnStatement pReturnStatement) {

    return new CReturnStatementEdge(
        pOriginal.getRawStatement(),
        pReturnStatement,
        pOriginal.getFileLocation(),
        pOriginal.getPredecessor(),
        pOriginal.getSuccessor());
  }

  // Thread Substitutions ========================================================================

  public static ImmutableMap<MPORThread, CSimpleDeclarationSubstitution> buildSubstitutions(
      MPOROptions pOptions,
      ImmutableSet<CVariableDeclaration> pGlobalVariableDeclarations,
      ImmutableList<MPORThread> pThreads,
      CBinaryExpressionBuilder pBinaryExpressionBuilder) {

    ImmutableMap.Builder<MPORThread, CSimpleDeclarationSubstitution> rDeclarationSubstitutions =
        ImmutableMap.builder();
    // create global vars up front, their initializer cannot contain local variables
    ImmutableMap<CVariableDeclaration, CIdExpression> globalVarSubstitutes =
        buildVariableDeclarationSubstitutes(
            pOptions,
            Optional.empty(),
            Optional.empty(),
            0,
            pGlobalVariableDeclarations,
            pBinaryExpressionBuilder);
    for (MPORThread thread : pThreads) {
      ImmutableMap<CParameterDeclaration, CIdExpression> parameterSubstitutes =
          getParameterSubstitutes(thread);
      ImmutableMap<CVariableDeclaration, CIdExpression> localVarSubstitutes =
          buildVariableDeclarationSubstitutes(
              pOptions,
              Optional.of(globalVarSubstitutes),
              Optional.of(parameterSubstitutes),
              thread.id,
              thread.localVars,
              pBinaryExpressionBuilder);
      rDeclarationSubstitutions.put(
          thread,
          new CSimpleDeclarationSubstitution(
              Optional.of(globalVarSubstitutes),
              localVarSubstitutes,
              Optional.of(parameterSubstitutes),
              pBinaryExpressionBuilder));
    }
    return rDeclarationSubstitutions.buildOrThrow();
  }

  private static ImmutableMap<CParameterDeclaration, CIdExpression> getParameterSubstitutes(
      MPORThread pThread) {

    ImmutableMap.Builder<CParameterDeclaration, CIdExpression> rParameterSubstitutes =
        ImmutableMap.builder();
    for (CFunctionDeclaration functionDeclaration : pThread.cfa.functionCalls.keySet()) {
      for (CParameterDeclaration parameterDeclaration : functionDeclaration.getParameters()) {
        String varName = SeqNameUtil.buildParameterName(parameterDeclaration, pThread.id);
        // we use variable declarations for parameters in the sequentialization
        CVariableDeclaration varDec =
            substituteVarDeclaration(parameterDeclaration.asVariableDeclaration(), varName);
        rParameterSubstitutes.put(parameterDeclaration, SeqIdExpression.buildIdExpression(varDec));
      }
    }
    return rParameterSubstitutes.buildOrThrow();
  }

  /**
   * Creates substitutes for all variables in the program and maps them to their original. The
   * substitutes differ only in their name.
   */
  private static ImmutableMap<CVariableDeclaration, CIdExpression>
      buildVariableDeclarationSubstitutes(
          MPOROptions pOptions,
          Optional<ImmutableMap<CVariableDeclaration, CIdExpression>> pGlobalSubstitutes,
          Optional<ImmutableMap<CParameterDeclaration, CIdExpression>> pParameterSubstitutes,
          int pThreadId,
          ImmutableSet<CVariableDeclaration> pVariableDeclarations,
          CBinaryExpressionBuilder pBinaryExpressionBuilder) {

    // step 1: create dummy CVariableDeclaration substitutes which may be adjusted in step 2
    ImmutableMap.Builder<CVariableDeclaration, CIdExpression> dummyVarSubsB =
        ImmutableMap.builder();
    for (CVariableDeclaration variableDeclaration : pVariableDeclarations) {
      CStorageClass storageClass = variableDeclaration.getCStorageClass();
      // if type declarations are not included, the storage class cannot be extern
      if (pOptions.inputTypeDeclarations || !storageClass.equals(CStorageClass.EXTERN)) {
        String substituteName = SeqNameUtil.buildVariableName(variableDeclaration, pThreadId);
        CVariableDeclaration substitute =
            substituteVarDeclaration(variableDeclaration, substituteName);
        dummyVarSubsB.put(variableDeclaration, SeqIdExpression.buildIdExpression(substitute));
      }
    }
    ImmutableMap<CVariableDeclaration, CIdExpression> dummyLocalVarSubs =
        dummyVarSubsB.buildOrThrow();

    // create dummy substitution
    CSimpleDeclarationSubstitution dummySubstitution =
        new CSimpleDeclarationSubstitution(
            pGlobalSubstitutes, dummyLocalVarSubs, pParameterSubstitutes, pBinaryExpressionBuilder);

    // step 2: replace initializers of CVariableDeclarations with substitutes
    ImmutableMap.Builder<CVariableDeclaration, CIdExpression> rFinalSubs = ImmutableMap.builder();
    // TODO handle CInitializerList?
    for (var entry : dummyLocalVarSubs.entrySet()) {
      CIdExpression idExpression = entry.getValue();

      assert idExpression.getDeclaration() instanceof CVariableDeclaration;
      CVariableDeclaration varDeclaration = (CVariableDeclaration) idExpression.getDeclaration();
      CInitializer initializer = varDeclaration.getInitializer();

      if (initializer instanceof CInitializerExpression initializerExpression) {
        CInitializerExpression initExprSub =
            substituteInitializerExpression(
                initializerExpression,
                dummySubstitution.substitute(initializerExpression.getExpression()));
        CVariableDeclaration finalSub = substituteVarDeclaration(varDeclaration, initExprSub);
        rFinalSubs.put(entry.getKey(), SeqIdExpression.buildIdExpression(finalSub));
        continue;
      }
      rFinalSubs.put(entry);
    }
    return rFinalSubs.buildOrThrow();
  }
}
