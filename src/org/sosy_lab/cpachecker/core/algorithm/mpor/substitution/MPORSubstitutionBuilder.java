// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.substitution;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableTable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadObjectType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationUtils;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.CFAEdgeForThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThreadUtil;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class MPORSubstitutionBuilder {

  public static ImmutableList<MPORSubstitution> buildSubstitutions(
      MPOROptions pOptions,
      ImmutableList<AVariableDeclaration> pGlobalVariableDeclarations,
      ImmutableList<MPORThread> pThreads,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    // step 1: create global variable substitutes, their initializer cannot be local/param variables
    MPORThread mainThread = MPORThreadUtil.extractMainThread(pThreads);
    ImmutableList<Entry<CVariableDeclaration, CIdExpression>> globalVariableSubstitutes =
        buildGlobalVariableSubstitutes(pOptions, mainThread, pGlobalVariableDeclarations, pUtils);
    ImmutableMap<CParameterDeclaration, CIdExpression> mainFunctionArgSubstitutes =
        getMainFunctionArgSubstitutes(pOptions, mainThread);
    // use same start_routine arg substitutes across threads, so that all threads can access them
    ImmutableTable<CFAEdgeForThread, CParameterDeclaration, CIdExpression>
        startRoutineArgSubstitutes = buildStartRoutineArgSubstitutes(pOptions, pThreads);

    ImmutableList.Builder<MPORSubstitution> rSubstitutions = ImmutableList.builder();

    // step 2: for each thread, create substitution
    for (MPORThread thread : pThreads) {
      ImmutableTable<CFAEdgeForThread, CParameterDeclaration, CIdExpression> parameterSubstitutes =
          buildParameterSubstitutes(pOptions, thread);
      ImmutableTable<
              Optional<CFAEdgeForThread>, CVariableDeclaration, LocalVariableDeclarationSubstitute>
          localVariableSubstitutes =
              buildVariableDeclarationSubstitutes(
                  pOptions,
                  thread,
                  globalVariableSubstitutes,
                  parameterSubstitutes,
                  mainFunctionArgSubstitutes,
                  startRoutineArgSubstitutes,
                  thread.id(),
                  thread.localVariables(),
                  pUtils);
      rSubstitutions.add(
          new MPORSubstitution(
              false,
              pOptions,
              thread,
              globalVariableSubstitutes,
              localVariableSubstitutes,
              parameterSubstitutes,
              mainFunctionArgSubstitutes,
              startRoutineArgSubstitutes,
              pUtils));
    }
    return rSubstitutions.build();
  }

  private static ImmutableList<Entry<CVariableDeclaration, CIdExpression>>
      buildGlobalVariableSubstitutes(
          MPOROptions pOptions,
          MPORThread pThread,
          ImmutableList<AVariableDeclaration> pGlobalVariableDeclarations,
          SequentializationUtils pUtils)
          throws UnrecognizedCodeException {

    checkArgument(pThread.isMain(), "thread must be main for global variable substitution");

    // step 1: create dummy CVariableDeclaration substitutes which may be adjusted in step 2
    ImmutableList<Entry<CVariableDeclaration, CIdExpression>> dummyGlobalSubstitutes =
        initGlobalSubstitutes(pOptions, pGlobalVariableDeclarations);

    // create dummy substitution. we can use empty maps for local and parameter substitutions
    // because initializers of global variables are always global variables, if present
    MPORSubstitution dummySubstitution =
        new MPORSubstitution(
            true,
            pOptions,
            pThread,
            dummyGlobalSubstitutes,
            ImmutableTable.of(),
            ImmutableTable.of(),
            ImmutableMap.of(),
            ImmutableTable.of(),
            pUtils);

    // step 2: replace initializers of CVariableDeclarations with substitutes
    ImmutableList.Builder<Entry<CVariableDeclaration, CIdExpression>> rFinalSubstitutes =
        ImmutableList.builder();

    for (Entry<CVariableDeclaration, CIdExpression> entry : dummyGlobalSubstitutes) {
      CVariableDeclaration variableDeclaration = entry.getKey();
      CIdExpression idExpression = entry.getValue();
      CInitializer initializer = variableDeclaration.getInitializer();
      // TODO handle CInitializerList
      if (initializer instanceof CInitializerExpression initializerExpression) {
        CInitializerExpression substituteInitializerExpression =
            substituteInitializerExpression(
                initializerExpression,
                dummySubstitution.substitute(
                    // no call context for global variables
                    initializerExpression.getExpression(),
                    Optional.empty(),
                    false,
                    false,
                    false,
                    false,
                    Optional.empty()));
        CVariableDeclaration newSubstituteDeclaration =
            substituteVariableDeclaration(
                (CVariableDeclaration) idExpression.getDeclaration(),
                substituteInitializerExpression);
        CIdExpression newIdExpression =
            SeqExpressionBuilder.buildIdExpression(newSubstituteDeclaration);
        rFinalSubstitutes.add(Map.entry(variableDeclaration, newIdExpression));
      } else {
        rFinalSubstitutes.add(Map.entry(variableDeclaration, idExpression));
      }
    }
    return rFinalSubstitutes.build();
  }

  private static ImmutableList<Entry<CVariableDeclaration, CIdExpression>> initGlobalSubstitutes(
      MPOROptions pOptions, ImmutableList<AVariableDeclaration> pGlobalVariableDeclarations) {

    ImmutableList.Builder<Entry<CVariableDeclaration, CIdExpression>> dummyGlobalSubstitutes =
        ImmutableList.builder();
    for (AVariableDeclaration aVariableDeclaration : pGlobalVariableDeclarations) {
      CVariableDeclaration variableDeclaration = (CVariableDeclaration) aVariableDeclaration;
      CStorageClass storageClass = variableDeclaration.getCStorageClass();
      // if type declarations are not included, the storage class cannot be extern
      if (pOptions.inputTypeDeclarations() || !storageClass.equals(CStorageClass.EXTERN)) {
        String substituteName = SeqNameUtil.buildGlobalVariableName(pOptions, variableDeclaration);
        CVariableDeclaration substitute =
            substituteVariableDeclaration(variableDeclaration, substituteName);
        CIdExpression substituteExpression = SeqExpressionBuilder.buildIdExpression(substitute);
        dummyGlobalSubstitutes.add(Map.entry(variableDeclaration, substituteExpression));
      }
    }
    return dummyGlobalSubstitutes.build();
  }

  // Parameter =====================================================================================

  /**
   * For each {@link CFunctionCallEdge} (i.e. calling context), we map the parameter declaration to
   * the created parameter variable.
   */
  private static ImmutableTable<CFAEdgeForThread, CParameterDeclaration, CIdExpression>
      buildParameterSubstitutes(MPOROptions pOptions, MPORThread pThread) {

    ImmutableTable.Builder<CFAEdgeForThread, CParameterDeclaration, CIdExpression>
        rParameterSubstitutes = ImmutableTable.builder();
    Map<CFunctionDeclaration, Integer> callCounts = new HashMap<>();

    for (CFAEdgeForThread threadEdge : pThread.cfa().threadEdges) {
      if (threadEdge.cfaEdge instanceof CFunctionCallEdge pFunctionCallEdge) {
        CFunctionDeclaration functionDeclaration =
            pFunctionCallEdge.getFunctionCallExpression().getDeclaration();
        if (!callCounts.containsKey(functionDeclaration)) {
          callCounts.put(functionDeclaration, 0);
        }
        callCounts.put(functionDeclaration, callCounts.get(functionDeclaration) + 1);
        int callNumber = callCounts.get(functionDeclaration);
        rParameterSubstitutes.putAll(
            buildParameterSubstitutes(
                pOptions, pThread, threadEdge, functionDeclaration, callNumber));
      }
    }
    return rParameterSubstitutes.buildOrThrow();
  }

  private static ImmutableTable<CFAEdgeForThread, CParameterDeclaration, CIdExpression>
      buildParameterSubstitutes(
          MPOROptions pOptions,
          MPORThread pThread,
          CFAEdgeForThread pCallContext,
          CFunctionDeclaration pFunctionDeclaration,
          int pCallNumber) {

    ImmutableTable.Builder<CFAEdgeForThread, CParameterDeclaration, CIdExpression> substitutes =
        ImmutableTable.builder();
    for (CParameterDeclaration parameterDeclaration : pFunctionDeclaration.getParameters()) {
      String varName =
          SeqNameUtil.buildParameterName(
              pOptions,
              parameterDeclaration,
              pThread.id(),
              pFunctionDeclaration.getOrigName(),
              pCallNumber);
      // we use variable declarations for parameters in the sequentialization
      CParameterDeclaration substituteParameterDeclaration =
          substituteParameterDeclaration(parameterDeclaration, varName);
      substitutes.put(
          pCallContext,
          parameterDeclaration,
          SeqExpressionBuilder.buildIdExpression(substituteParameterDeclaration));
    }
    return substitutes.buildOrThrow();
  }

  // Main Function Args ============================================================================

  private static ImmutableMap<CParameterDeclaration, CIdExpression> getMainFunctionArgSubstitutes(
      MPOROptions pOptions, MPORThread pMainThread) {

    ImmutableMap.Builder<CParameterDeclaration, CIdExpression> rArgs = ImmutableMap.builder();
    for (CParameterDeclaration parameterDeclaration : pMainThread.startRoutine().getParameters()) {
      String varName = SeqNameUtil.buildMainFunctionArgName(pOptions, parameterDeclaration);
      // we use variable declarations for main function args in the sequentialization
      CVariableDeclaration variableDeclaration =
          substituteVariableDeclaration(parameterDeclaration.asVariableDeclaration(), varName);
      CIdExpression substitute = SeqExpressionBuilder.buildIdExpression(variableDeclaration);
      rArgs.put(parameterDeclaration, substitute);
    }
    return rArgs.buildOrThrow();
  }

  // Start Routine Args ============================================================================

  private static ImmutableTable<CFAEdgeForThread, CParameterDeclaration, CIdExpression>
      buildStartRoutineArgSubstitutes(MPOROptions pOptions, ImmutableList<MPORThread> pAllThreads) {

    ImmutableTable.Builder<CFAEdgeForThread, CParameterDeclaration, CIdExpression> rArgSubstitutes =
        ImmutableTable.builder();

    for (MPORThread thread : pAllThreads) {
      for (CFAEdgeForThread threadEdge : thread.cfa().threadEdges) {
        CFAEdge cfaEdge = threadEdge.cfaEdge;
        Optional<CFunctionCall> optionalFunctionCall =
            PthreadUtil.tryGetFunctionCallFromCfaEdge(cfaEdge);
        if (optionalFunctionCall.isPresent()) {
          CFunctionCall functionCall = optionalFunctionCall.orElseThrow();
          if (PthreadUtil.isCallToPthreadFunction(
              functionCall, PthreadFunctionType.PTHREAD_CREATE)) {
            CIdExpression pthreadT =
                PthreadUtil.extractPthreadObject(functionCall, PthreadObjectType.PTHREAD_T);
            MPORThread createdThread =
                MPORThreadUtil.getThreadByObject(pAllThreads, Optional.of(pthreadT));
            tryBuildStartRoutineArgSubstitute(pOptions, functionCall, pthreadT, createdThread)
                .ifPresent(
                    entry -> rArgSubstitutes.put(threadEdge, entry.getKey(), entry.getValue()));
          }
        }
      }
    }
    return rArgSubstitutes.buildOrThrow();
  }

  private static Optional<Entry<CParameterDeclaration, CIdExpression>>
      tryBuildStartRoutineArgSubstitute(
          MPOROptions pOptions,
          CFunctionCall pFunctionCall,
          CIdExpression pPthreadT,
          MPORThread pCreatedThread) {

    // pthread_t matches
    if (pPthreadT.equals(pCreatedThread.threadObject().orElseThrow())) {
      CFunctionType startRoutineType = PthreadUtil.extractStartRoutineType(pFunctionCall);
      CFunctionDeclaration startRoutineDeclaration =
          (CFunctionDeclaration) pCreatedThread.cfa().entryNode.getFunction();
      // start_routine matches
      if (startRoutineDeclaration.getType().equals(startRoutineType)) {
        if (!startRoutineDeclaration.getParameters().isEmpty()) {
          assert startRoutineDeclaration.getParameters().size() == 1
              : "start_routines can have either 0 or 1 arguments";
          CParameterDeclaration parameterDeclaration =
              startRoutineDeclaration.getParameters().getFirst();
          String varName =
              SeqNameUtil.buildStartRoutineArgName(
                  pOptions,
                  parameterDeclaration,
                  pCreatedThread.id(),
                  startRoutineDeclaration.getOrigName());
          CParameterDeclaration substituteParameterDeclaration =
              substituteParameterDeclaration(parameterDeclaration, varName);
          CIdExpression substitute =
              SeqExpressionBuilder.buildIdExpression(substituteParameterDeclaration);
          return Optional.of(Map.entry(parameterDeclaration, substitute));
        }
      }
    }
    return Optional.empty();
  }

  // Variable Declarations =========================================================================

  // TODO split into functions and improve overview
  /**
   * Creates substitutes for all variables in the program and maps them to their original. The
   * substitutes differ only in their name.
   */
  private static ImmutableTable<
          Optional<CFAEdgeForThread>, CVariableDeclaration, LocalVariableDeclarationSubstitute>
      buildVariableDeclarationSubstitutes(
          MPOROptions pOptions,
          MPORThread pThread,
          ImmutableList<Entry<CVariableDeclaration, CIdExpression>> pGlobalSubstitutes,
          ImmutableTable<CFAEdgeForThread, CParameterDeclaration, CIdExpression>
              pParameterSubstitutes,
          ImmutableMap<CParameterDeclaration, CIdExpression> pMainFunctionArgSubstitutes,
          ImmutableTable<CFAEdgeForThread, CParameterDeclaration, CIdExpression>
              pStartRoutineArgSubstitutes,
          int pThreadId,
          ImmutableMultimap<CVariableDeclaration, Optional<CFAEdgeForThread>>
              pLocalVariableDeclarations,
          SequentializationUtils pUtils)
          throws UnrecognizedCodeException {

    // step 1: create dummy CVariableDeclaration substitutes which may be adjusted in step 2
    ImmutableTable<
            Optional<CFAEdgeForThread>, CVariableDeclaration, LocalVariableDeclarationSubstitute>
        dummySubstitutes =
            buildLocalVariableDummySubstitutes(pOptions, pThreadId, pLocalVariableDeclarations);

    // create dummy substitution
    MPORSubstitution dummySubstitution =
        new MPORSubstitution(
            true,
            pOptions,
            pThread,
            pGlobalSubstitutes,
            dummySubstitutes,
            pParameterSubstitutes,
            pMainFunctionArgSubstitutes,
            pStartRoutineArgSubstitutes,
            pUtils);

    // step 2: replace initializers of CVariableDeclarations with substitutes
    ImmutableTable.Builder<
            Optional<CFAEdgeForThread>, CVariableDeclaration, LocalVariableDeclarationSubstitute>
        rFinalSubstitutes = ImmutableTable.builder();

    for (var cell : dummySubstitutes.cellSet()) {
      CVariableDeclaration variableDeclaration = cell.getColumnKey();
      CInitializer initializer = variableDeclaration.getInitializer();
      // TODO handle CInitializerList
      if (initializer instanceof CInitializerExpression initializerExpression) {
        LocalVariableDeclarationSubstitute substituteDeclaration = cell.getValue();
        Optional<CFAEdgeForThread> callContext = cell.getRowKey();
        MPORSubstitutionTracker tracker = new MPORSubstitutionTracker();
        CInitializerExpression initializerSubstitute =
            substituteInitializerExpression(
                initializerExpression,
                dummySubstitution.substitute(
                    initializerExpression.getExpression(),
                    callContext,
                    true,
                    false,
                    false,
                    false,
                    Optional.of(tracker)));
        CVariableDeclaration newDeclarationSubstitute =
            substituteVariableDeclaration(
                substituteDeclaration.getSubstituteVariableDeclaration(), initializerSubstitute);
        CIdExpression newIdExpression =
            SeqExpressionBuilder.buildIdExpression(newDeclarationSubstitute);
        LocalVariableDeclarationSubstitute newSubstituteDeclaration =
            new LocalVariableDeclarationSubstitute(newIdExpression, Optional.of(tracker));
        rFinalSubstitutes.put(callContext, variableDeclaration, newSubstituteDeclaration);
      } else {
        rFinalSubstitutes.put(cell);
      }
    }
    return rFinalSubstitutes.build();
  }

  private static ImmutableTable<
          Optional<CFAEdgeForThread>, CVariableDeclaration, LocalVariableDeclarationSubstitute>
      buildLocalVariableDummySubstitutes(
          MPOROptions pOptions,
          int pThreadId,
          ImmutableMultimap<CVariableDeclaration, Optional<CFAEdgeForThread>>
              pVariableDeclarations) {

    ImmutableTable.Builder<
            Optional<CFAEdgeForThread>, CVariableDeclaration, LocalVariableDeclarationSubstitute>
        dummySubstitutes = ImmutableTable.builder();
    Set<CVariableDeclaration> visitedKeys = new HashSet<>();
    for (var entry : pVariableDeclarations.entries()) {
      CVariableDeclaration variableDeclaration = entry.getKey();
      if (visitedKeys.add(variableDeclaration)) {
        int call = 1;
        for (Optional<CFAEdgeForThread> callContext : pVariableDeclarations.get(entry.getKey())) {
          CStorageClass storageClass = variableDeclaration.getCStorageClass();

          // if type declarations are not included, the storage class cannot be extern
          if (pOptions.inputTypeDeclarations() || !storageClass.equals(CStorageClass.EXTERN)) {
            Optional<String> functionName = getFunctionNameByCallContext(callContext);
            String substituteName =
                SeqNameUtil.buildLocalVariableName(
                    pOptions, variableDeclaration, pThreadId, call++, functionName);
            CVariableDeclaration substituteDeclaration =
                substituteVariableDeclaration(variableDeclaration, substituteName);
            CIdExpression substituteExpression =
                SeqExpressionBuilder.buildIdExpression(substituteDeclaration);
            dummySubstitutes.put(
                callContext,
                variableDeclaration,
                new LocalVariableDeclarationSubstitute(substituteExpression, Optional.empty()));
          }
        }
      }
    }
    return dummySubstitutes.buildOrThrow();
  }

  // Helpers =======================================================================================

  private static Optional<String> getFunctionNameByCallContext(
      Optional<CFAEdgeForThread> pCallContext) {

    if (pCallContext.isPresent()) {
      CFAEdge callContext = pCallContext.orElseThrow().cfaEdge;

      if (callContext instanceof CFunctionCallEdge functionCallEdge) {
        CFunctionDeclaration functionDeclaration =
            functionCallEdge.getFunctionCallExpression().getDeclaration();
        return Optional.of(functionDeclaration.getOrigName());

      } else if (callContext instanceof CStatementEdge statementEdge) {
        if (statementEdge.getStatement() instanceof CFunctionCallStatement functionCallStatement) {
          CFunctionDeclaration functionDeclaration =
              functionCallStatement.getFunctionCallExpression().getDeclaration();
          return Optional.of(functionDeclaration.getOrigName());
        }
      }
      throw new IllegalArgumentException(
          "call context must be CFunctionCallEdge or CStatementEdge");
    }
    return Optional.empty();
  }

  private static CInitializerExpression substituteInitializerExpression(
      CInitializerExpression pOriginal, CExpression pExpression) {

    return new CInitializerExpression(pOriginal.getFileLocation(), pExpression);
  }

  /**
   * Creates a clone of the given CVariableDeclaration with substituted name(s).
   *
   * @param pOriginal the variable declaration to substitute
   */
  private static CVariableDeclaration substituteVariableDeclaration(
      CVariableDeclaration pOriginal, String pName) {

    return new CVariableDeclaration(
        pOriginal.getFileLocation(),
        pOriginal.isGlobal(),
        pOriginal.getCStorageClass(),
        pOriginal.getType(),
        pName,
        pOriginal.getOrigName(),
        pOriginal.getQualifiedName(),
        pOriginal.getInitializer());
  }

  /**
   * Creates a clone of the given CVariableDeclaration with substituted name(s) and initializer.
   *
   * @param pOriginal the variable declaration to substitute
   */
  private static CVariableDeclaration substituteVariableDeclaration(
      CVariableDeclaration pOriginal, CInitializerExpression pInitializerExpression) {

    return new CVariableDeclaration(
        pOriginal.getFileLocation(),
        pOriginal.isGlobal(),
        pOriginal.getCStorageClass(),
        pOriginal.getType(),
        pOriginal.getName(),
        pOriginal.getOrigName(),
        pOriginal.getQualifiedName(),
        pInitializerExpression);
  }

  private static CParameterDeclaration substituteParameterDeclaration(
      CParameterDeclaration pOriginal, String pName) {

    return new CParameterDeclaration(pOriginal.getFileLocation(), pOriginal.getType(), pName);
  }
}
