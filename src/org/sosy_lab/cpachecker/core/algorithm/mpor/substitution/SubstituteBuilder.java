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
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
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
          Optional<SubstituteEdge> substitute =
              trySubstituteEdge(pOptions, substitution, threadEdge);
          rSubstituteEdges.put(
              threadEdge,
              substitute.isPresent() ? substitute.orElseThrow() : new SubstituteEdge(cfaEdge));
        }
      }
    }
    return ImmutableMap.copyOf(rSubstituteEdges);
  }

  private static Optional<SubstituteEdge> trySubstituteEdge(
      MPOROptions pOptions, CSimpleDeclarationSubstitution pSubstitution, ThreadEdge pThreadEdge) {

    CFAEdge cfaEdge = pThreadEdge.cfaEdge;
    Optional<CFunctionCallEdge> callingContext = pThreadEdge.callingContext;
    if (cfaEdge instanceof CDeclarationEdge declarationEdge) {
      // TODO what about structs?
      if (SubstituteUtil.isExcludedDeclarationEdge(pOptions, declarationEdge)) {
        return Optional.empty();
      } else {
        CDeclaration declaration = declarationEdge.getDeclaration();
        // we only substitute variables, not functions or types
        if (declaration instanceof CVariableDeclaration) {
          CVariableDeclaration variableDeclaration =
              pSubstitution.getVariableDeclarationSubstitute(declaration, callingContext);
          return Optional.of(
              new SubstituteEdge(substituteDeclarationEdge(declarationEdge, variableDeclaration)));
        }
      }

    } else if (cfaEdge instanceof CAssumeEdge assume) {
      return Optional.of(
          new SubstituteEdge(
              substituteAssumeEdge(
                  assume, pSubstitution.substitute(assume.getExpression(), callingContext))));

    } else if (cfaEdge instanceof CStatementEdge statement) {
      return Optional.of(
          new SubstituteEdge(
              substituteStatementEdge(
                  statement, pSubstitution.substitute(statement.getStatement(), callingContext))));

    } else if (cfaEdge instanceof CFunctionSummaryEdge functionSummary) {
      // only substitute assignments (e.g. CPAchecker_TMP = func();)
      if (functionSummary.getExpression() instanceof CFunctionCallAssignmentStatement assignment) {
        return Optional.of(
            new SubstituteEdge(
                substituteFunctionSummaryEdge(
                    functionSummary, pSubstitution.substitute(assignment, callingContext))));
      }

    } else if (cfaEdge instanceof CFunctionCallEdge functionCall) {
      // CFunctionCallEdges also assign CPAchecker_TMPs -> handle assignment statements here
      // too
      return Optional.of(
          new SubstituteEdge(
              substituteFunctionCallEdge(
                  functionCall,
                  (CFunctionCall)
                      pSubstitution.substitute(functionCall.getFunctionCall(), callingContext))));

    } else if (cfaEdge instanceof CReturnStatementEdge returnStatement) {
      return Optional.of(
          new SubstituteEdge(
              substituteReturnStatementEdge(
                  returnStatement,
                  pSubstitution.substitute(returnStatement.getReturnStatement(), callingContext))));
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

    // step 1: create global variable substitutes, their initializer cannot contain local variables
    ImmutableMap<CVariableDeclaration, ImmutableMap<Optional<CFunctionCallEdge>, CIdExpression>>
        globalVarSubstitutes =
            buildVariableDeclarationSubstitutes(
                pOptions,
                ImmutableMap.of(),
                ImmutableMap.of(),
                0,
                // global variables have empty calling contexts, they are never inside functions
                mapKeysToOptionalEmpty(pGlobalVariableDeclarations),
                pBinaryExpressionBuilder);

    for (MPORThread thread : pThreads) {
      ImmutableMap<CFunctionCallEdge, ImmutableMap<CParameterDeclaration, CIdExpression>>
          parameterSubstitutes = getParameterSubstitutes(thread);
      ImmutableMap<CVariableDeclaration, ImmutableMap<Optional<CFunctionCallEdge>, CIdExpression>>
          localVarSubstitutes =
              buildVariableDeclarationSubstitutes(
                  pOptions,
                  mapKeysToSingleValue(globalVarSubstitutes),
                  parameterSubstitutes,
                  thread.id,
                  thread.localVars,
                  pBinaryExpressionBuilder);
      rDeclarationSubstitutions.put(
          thread,
          new CSimpleDeclarationSubstitution(
              mapKeysToSingleValue(globalVarSubstitutes),
              localVarSubstitutes,
              parameterSubstitutes,
              pBinaryExpressionBuilder));
    }
    return rDeclarationSubstitutions.buildOrThrow();
  }

  // TODO maybe place this in Util? though it is very specific.
  private static <K, V> ImmutableMultimap<K, Optional<V>> mapKeysToOptionalEmpty(
      ImmutableSet<K> pDeclarations) {

    ImmutableMultimap.Builder<K, Optional<V>> r = ImmutableMultimap.builder();
    pDeclarations.forEach(key -> r.put(key, Optional.empty()));
    return r.build();
  }

  // TODO refactor
  private static ImmutableMap<CVariableDeclaration, CIdExpression> mapKeysToSingleValue(
      ImmutableMap<CVariableDeclaration, ImmutableMap<Optional<CFunctionCallEdge>, CIdExpression>>
          pVariableSubstitutes) {

    ImmutableMap.Builder<CVariableDeclaration, CIdExpression> rExtract = ImmutableMap.builder();
    for (var entry : pVariableSubstitutes.entrySet()) {
      assert entry.getValue().size() == 1;
      assert entry.getValue().containsKey(Optional.empty());
      rExtract.put(entry.getKey(), entry.getValue().get(Optional.empty()));
    }
    return rExtract.buildOrThrow();
  }

  /**
   * For each {@link CFunctionCallEdge} (i.e. calling context), we map the parameter declaration to
   * the created parameter variable.
   */
  private static ImmutableMap<CFunctionCallEdge, ImmutableMap<CParameterDeclaration, CIdExpression>>
      getParameterSubstitutes(MPORThread pThread) {

    ImmutableMap.Builder<CFunctionCallEdge, ImmutableMap<CParameterDeclaration, CIdExpression>>
        rParameterSubstitutes = ImmutableMap.builder();
    Map<CFunctionDeclaration, Integer> callCounts = new HashMap<>();

    for (CFunctionCallEdge functionCallEdge : pThread.cfa.functionCallEdges) {
      CFunctionDeclaration functionDeclaration =
          functionCallEdge.getFunctionCallExpression().getDeclaration();
      if (!callCounts.containsKey(functionDeclaration)) {
        callCounts.put(functionDeclaration, 0);
      }
      callCounts.put(functionDeclaration, callCounts.get(functionDeclaration) + 1);
      int call = callCounts.get(functionDeclaration);

      ImmutableMap.Builder<CParameterDeclaration, CIdExpression> substitutes =
          ImmutableMap.builder();
      for (CParameterDeclaration parameterDeclaration : functionDeclaration.getParameters()) {
        String varName =
            SeqNameUtil.buildParameterName(
                parameterDeclaration, pThread.id, functionDeclaration.getOrigName(), call);
        // we use variable declarations for parameters in the sequentialization
        CVariableDeclaration variableDeclaration =
            substituteVarDeclaration(parameterDeclaration.asVariableDeclaration(), varName);
        substitutes.put(
            parameterDeclaration, SeqExpressionBuilder.buildIdExpression(variableDeclaration));
      }
      rParameterSubstitutes.put(functionCallEdge, substitutes.buildOrThrow());
    }
    return rParameterSubstitutes.buildOrThrow();
  }

  private static ImmutableMap<
          CVariableDeclaration, ImmutableMap<Optional<CFunctionCallEdge>, CIdExpression>>
      initSubstitutes(
          MPOROptions pOptions,
          int pThreadId,
          ImmutableMultimap<CVariableDeclaration, Optional<CFunctionCallEdge>>
              pVariableDeclarations) {

    ImmutableMap.Builder<
            CVariableDeclaration, ImmutableMap<Optional<CFunctionCallEdge>, CIdExpression>>
        dummySubstitutes = ImmutableMap.builder();
    Set<CVariableDeclaration> visitedKeys = new HashSet<>();
    for (var entry : pVariableDeclarations.entries()) {
      CVariableDeclaration variableDeclaration = entry.getKey();
      if (visitedKeys.add(variableDeclaration)) {
        ImmutableMap.Builder<Optional<CFunctionCallEdge>, CIdExpression> substitutes =
            ImmutableMap.builder();
        int call = 0;
        for (Optional<CFunctionCallEdge> callContext : pVariableDeclarations.get(entry.getKey())) {
          CStorageClass storageClass = variableDeclaration.getCStorageClass();
          // if type declarations are not included, the storage class cannot be extern
          if (pOptions.inputTypeDeclarations || !storageClass.equals(CStorageClass.EXTERN)) {
            String substituteName =
                SeqNameUtil.buildVariableName(
                    variableDeclaration,
                    pThreadId,
                    call++,
                    callContext.isEmpty()
                        ? Optional.empty()
                        : Optional.of(
                            callContext
                                .orElseThrow()
                                .getFunctionCallExpression()
                                .getDeclaration()
                                .getOrigName()));
            CVariableDeclaration substitute =
                substituteVarDeclaration(variableDeclaration, substituteName);
            CIdExpression substituteExpression = SeqExpressionBuilder.buildIdExpression(substitute);
            substitutes.put(callContext, substituteExpression);
          }
        }
        dummySubstitutes.put(variableDeclaration, substitutes.buildOrThrow());
      }
    }
    return dummySubstitutes.buildOrThrow();
  }

  // TODO split into functions and improve overview
  /**
   * Creates substitutes for all variables in the program and maps them to their original. The
   * substitutes differ only in their name.
   */
  private static ImmutableMap<
          CVariableDeclaration, ImmutableMap<Optional<CFunctionCallEdge>, CIdExpression>>
      buildVariableDeclarationSubstitutes(
          MPOROptions pOptions,
          ImmutableMap<CVariableDeclaration, CIdExpression> pGlobalSubstitutes,
          ImmutableMap<CFunctionCallEdge, ImmutableMap<CParameterDeclaration, CIdExpression>>
              pParameterSubstitutes,
          int pThreadId,
          ImmutableMultimap<CVariableDeclaration, Optional<CFunctionCallEdge>>
              pVariableDeclarations,
          CBinaryExpressionBuilder pBinaryExpressionBuilder) {

    // step 1: create dummy CVariableDeclaration substitutes which may be adjusted in step 2
    ImmutableMap<CVariableDeclaration, ImmutableMap<Optional<CFunctionCallEdge>, CIdExpression>>
        dummySubstitutes = initSubstitutes(pOptions, pThreadId, pVariableDeclarations);

    // create dummy substitution
    CSimpleDeclarationSubstitution dummySubstitution =
        new CSimpleDeclarationSubstitution(
            pGlobalSubstitutes, dummySubstitutes, pParameterSubstitutes, pBinaryExpressionBuilder);

    // step 2: replace initializers of CVariableDeclarations with substitutes
    ImmutableMap.Builder<
            CVariableDeclaration, ImmutableMap<Optional<CFunctionCallEdge>, CIdExpression>>
        rFinalSubstitutes = ImmutableMap.builder();

    for (var entryA : dummySubstitutes.entrySet()) {
      CVariableDeclaration variableDeclaration = entryA.getKey();
      ImmutableMap.Builder<Optional<CFunctionCallEdge>, CIdExpression> substitutes =
          ImmutableMap.builder();

      for (var entryB : entryA.getValue().entrySet()) {
        CIdExpression idExpression = entryB.getValue();
        assert idExpression.getDeclaration() instanceof CVariableDeclaration
            : "id expression declaration must be variable declaration";

        CInitializer initializer = variableDeclaration.getInitializer();
        // TODO handle CInitializerList
        if (initializer instanceof CInitializerExpression initializerExpression) {
          Optional<CFunctionCallEdge> callingContext = entryB.getKey();
          CInitializerExpression initExprSub =
              substituteInitializerExpression(
                  initializerExpression,
                  dummySubstitution.substitute(
                      initializerExpression.getExpression(), callingContext));
          CVariableDeclaration finalSub =
              substituteVarDeclaration(variableDeclaration, initExprSub);
          substitutes.put(callingContext, SeqExpressionBuilder.buildIdExpression(finalSub));
          continue;
        }
        substitutes.put(entryB);
      }

      rFinalSubstitutes.put(variableDeclaration, substitutes.buildOrThrow());
    }
    return rFinalSubstitutes.buildOrThrow();
  }
}
