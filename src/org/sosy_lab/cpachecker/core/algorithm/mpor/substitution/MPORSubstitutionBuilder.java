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
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
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
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;

public record MPORSubstitutionBuilder(
    MPOROptions options,
    ImmutableList<AVariableDeclaration> globalVariableDeclarations,
    ImmutableList<MPORThread> threads,
    SequentializationUtils utils) {

  public ImmutableList<MPORSubstitution> buildSubstitutions() throws UnrecognizedCodeException {
    // step 1: create global variable substitutes, their initializer cannot be local/param variables
    MPORThread mainThread = MPORThreadUtil.extractMainThread(threads);
    ImmutableList<Entry<CVariableDeclaration, CIdExpression>> globalVariableSubstitutes =
        buildGlobalVariableSubstitutes(mainThread);
    ImmutableMap<CParameterDeclaration, CIdExpression> mainFunctionArgSubstitutes =
        getMainFunctionArgSubstitutes(mainThread);
    // use same start_routine arg substitutes across threads, so that all threads can access them
    ImmutableTable<CFAEdgeForThread, CParameterDeclaration, CIdExpression>
        startRoutineArgSubstitutes = buildStartRoutineArgSubstitutes();

    ImmutableList.Builder<MPORSubstitution> rSubstitutions = ImmutableList.builder();

    // step 2: for each thread, create substitution
    for (MPORThread thread : threads) {
      ImmutableTable<CFAEdgeForThread, CParameterDeclaration, ImmutableList<CIdExpression>>
          parameterSubstitutes = buildParameterSubstitutes(thread);
      ImmutableTable<
              Optional<CFAEdgeForThread>, CVariableDeclaration, LocalVariableDeclarationSubstitute>
          localVariableSubstitutes =
              buildVariableDeclarationSubstitutes(
                  thread,
                  globalVariableSubstitutes,
                  parameterSubstitutes,
                  mainFunctionArgSubstitutes,
                  startRoutineArgSubstitutes,
                  thread.id(),
                  thread.localVariables());
      rSubstitutions.add(
          new MPORSubstitution(
              false,
              options,
              thread,
              globalVariableSubstitutes,
              localVariableSubstitutes,
              parameterSubstitutes,
              mainFunctionArgSubstitutes,
              startRoutineArgSubstitutes,
              utils));
    }
    return rSubstitutions.build();
  }

  private ImmutableList<Entry<CVariableDeclaration, CIdExpression>> buildGlobalVariableSubstitutes(
      MPORThread pThread) throws UnrecognizedCodeException {

    checkArgument(pThread.isMain(), "thread must be main for global variable substitution");

    // step 1: create dummy CVariableDeclaration substitutes which may be adjusted in step 2
    ImmutableList<Entry<CVariableDeclaration, CIdExpression>> dummyGlobalSubstitutes =
        initGlobalSubstitutes();

    // create dummy substitution. we can use empty maps for local and parameter substitutions
    // because initializers of global variables are always global variables, if present
    MPORSubstitution dummySubstitution =
        new MPORSubstitution(
            true,
            options,
            pThread,
            dummyGlobalSubstitutes,
            ImmutableTable.of(),
            ImmutableTable.of(),
            ImmutableMap.of(),
            ImmutableTable.of(),
            utils);

    // step 2: replace initializers of CVariableDeclarations with substitutes
    ImmutableList.Builder<Entry<CVariableDeclaration, CIdExpression>> rFinalSubstitutes =
        ImmutableList.builder();

    for (Entry<CVariableDeclaration, CIdExpression> entry : dummyGlobalSubstitutes) {
      CVariableDeclaration variableDeclaration = entry.getKey();
      CIdExpression idExpression = entry.getValue();
      CInitializer initializer = variableDeclaration.getInitializer();
      // TODO handle CInitializerList
      if (initializer instanceof CInitializerExpression initializerExpression) {
        MPORSubstitutionTracker dummyTracker = new MPORSubstitutionTracker();
        // no call context is used for global variables
        CExpression substituteExpression =
            dummySubstitution.substitute(
                initializerExpression.getExpression(),
                Optional.empty(),
                false,
                false,
                false,
                false,
                dummyTracker);
        CInitializerExpression substituteInitializerExpression =
            substituteInitializerExpression(initializerExpression, substituteExpression);
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

  private ImmutableList<Entry<CVariableDeclaration, CIdExpression>> initGlobalSubstitutes() {
    ImmutableList.Builder<Entry<CVariableDeclaration, CIdExpression>> dummyGlobalSubstitutes =
        ImmutableList.builder();
    for (AVariableDeclaration aVariableDeclaration : globalVariableDeclarations) {
      CVariableDeclaration variableDeclaration = (CVariableDeclaration) aVariableDeclaration;
      CStorageClass storageClass = variableDeclaration.getCStorageClass();
      // if type declarations are not included, the storage class cannot be extern
      if (options.inputTypeDeclarations() || !storageClass.equals(CStorageClass.EXTERN)) {
        String substituteName = SeqNameUtil.buildGlobalVariableName(options, variableDeclaration);
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
   * For each {@link CFunctionCallEdge} (i.e. calling context), we map the {@link
   * CParameterDeclaration} of the functions to the created parameter variables.
   */
  private ImmutableTable<CFAEdgeForThread, CParameterDeclaration, ImmutableList<CIdExpression>>
      buildParameterSubstitutes(MPORThread pThread) {

    ImmutableTable.Builder<CFAEdgeForThread, CParameterDeclaration, ImmutableList<CIdExpression>>
        rParameterSubstitutes = ImmutableTable.builder();
    Map<CFunctionDeclaration, Integer> callCounts = new HashMap<>();

    for (CFAEdgeForThread threadEdge : pThread.cfa().threadEdges) {
      if (threadEdge.cfaEdge instanceof CFunctionCallEdge functionCallEdge) {
        CFunctionDeclaration functionDeclaration =
            functionCallEdge.getFunctionCallExpression().getDeclaration();
        // put functionDeclaration if not present, and increase by 1 for each call
        callCounts.merge(functionDeclaration, 1, Integer::sum);
        int callNumber = callCounts.get(functionDeclaration);
        rParameterSubstitutes.putAll(
            buildParameterSubstitutes(pThread, threadEdge, functionCallEdge, callNumber));
      }
    }
    return rParameterSubstitutes.buildOrThrow();
  }

  private ImmutableTable<CFAEdgeForThread, CParameterDeclaration, ImmutableList<CIdExpression>>
      buildParameterSubstitutes(
          MPORThread pThread,
          CFAEdgeForThread pCallContext,
          CFunctionCallEdge pFunctionCallEdge,
          int pCallNumber) {

    ImmutableTable.Builder<CFAEdgeForThread, CParameterDeclaration, ImmutableList<CIdExpression>>
        rSubstitutes = ImmutableTable.builder();
    for (CParameterDeclaration parameterDeclaration :
        pFunctionCallEdge.getFunctionCallExpression().getDeclaration().getParameters()) {
      rSubstitutes.put(
          pCallContext,
          parameterDeclaration,
          getSubstituteParametersByDeclaration(
              pThread, pFunctionCallEdge, parameterDeclaration, pCallNumber));
    }
    return rSubstitutes.buildOrThrow();
  }

  private ImmutableList<CIdExpression> getSubstituteParametersByDeclaration(
      MPORThread pThread,
      CFunctionCallEdge pFunctionCallEdge,
      CParameterDeclaration pParameterDeclaration,
      int pCallNumber) {

    ImmutableList.Builder<CIdExpression> rSubstitutes = ImmutableList.builder();

    CFunctionDeclaration functionDeclaration =
        pFunctionCallEdge.getFunctionCallExpression().getDeclaration();
    for (int i = 0; i < pFunctionCallEdge.getArguments().size(); i++) {
      CParameterDeclaration parameterDeclaration =
          MPORUtil.getParameterDeclarationByIndex(i, functionDeclaration);
      if (parameterDeclaration.equals(pParameterDeclaration)) {
        String substituteName =
            SeqNameUtil.buildSubstituteParameterDeclarationName(
                options,
                parameterDeclaration,
                pThread.id(),
                functionDeclaration.getOrigName(),
                pCallNumber,
                i);
        CInitializerExpression initializerExpression =
            new CInitializerExpression(
                pFunctionCallEdge.getFileLocation(), pFunctionCallEdge.getArguments().get(i));
        // we use variable declarations for parameters in the sequentialization
        rSubstitutes.add(
            SeqExpressionBuilder.buildIdExpression(
                substituteParameterDeclaration(
                    parameterDeclaration, initializerExpression, substituteName)));
      }
    }
    return rSubstitutes.build();
  }

  // Main Function Args ============================================================================

  private ImmutableMap<CParameterDeclaration, CIdExpression> getMainFunctionArgSubstitutes(
      MPORThread pMainThread) {

    ImmutableMap.Builder<CParameterDeclaration, CIdExpression> rArgs = ImmutableMap.builder();
    for (CParameterDeclaration parameterDeclaration : pMainThread.startRoutine().getParameters()) {
      String varName = SeqNameUtil.buildMainFunctionArgName(options, parameterDeclaration);
      // we use variable declarations for main function args in the sequentialization
      CVariableDeclaration variableDeclaration =
          substituteVariableDeclaration(parameterDeclaration.asVariableDeclaration(), varName);
      CIdExpression substitute = SeqExpressionBuilder.buildIdExpression(variableDeclaration);
      rArgs.put(parameterDeclaration, substitute);
    }
    return rArgs.buildOrThrow();
  }

  // Start Routine Args ============================================================================

  private ImmutableTable<CFAEdgeForThread, CParameterDeclaration, CIdExpression>
      buildStartRoutineArgSubstitutes() throws UnsupportedCodeException {

    ImmutableTable.Builder<CFAEdgeForThread, CParameterDeclaration, CIdExpression> rArgSubstitutes =
        ImmutableTable.builder();

    for (MPORThread thread : threads) {
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
                MPORThreadUtil.getThreadByObject(threads, Optional.of(pthreadT));
            tryBuildStartRoutineArgSubstitute(functionCall, pthreadT, createdThread)
                .ifPresent(
                    entry -> rArgSubstitutes.put(threadEdge, entry.getKey(), entry.getValue()));
          }
        }
      }
    }
    return rArgSubstitutes.buildOrThrow();
  }

  private Optional<Entry<CParameterDeclaration, CIdExpression>> tryBuildStartRoutineArgSubstitute(
      CFunctionCall pFunctionCall, CIdExpression pPthreadT, MPORThread pCreatedThread) {

    // pthread_t matches
    if (pPthreadT.equals(pCreatedThread.threadObject().orElseThrow())) {
      CFunctionDeclaration functionCallDeclaration =
          PthreadUtil.extractStartRoutineDeclaration(pFunctionCall);
      CFunctionDeclaration startRoutineDeclaration =
          (CFunctionDeclaration) pCreatedThread.cfa().entryNode.getFunctionDefinition();
      // start_routine matches
      if (startRoutineDeclaration.equals(functionCallDeclaration)) {
        if (!startRoutineDeclaration.getParameters().isEmpty()) {
          assert startRoutineDeclaration.getParameters().size() == 1
              : "start_routines can have either 0 or 1 arguments";
          CParameterDeclaration parameterDeclaration =
              startRoutineDeclaration.getParameters().getFirst();
          String substituteName =
              SeqNameUtil.buildStartRoutineArgName(
                  options,
                  parameterDeclaration,
                  pCreatedThread.id(),
                  startRoutineDeclaration.getOrigName());
          CVariableDeclaration substituteParameterDeclaration =
              substituteParameterDeclaration(parameterDeclaration, null, substituteName);
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
  private ImmutableTable<
          Optional<CFAEdgeForThread>, CVariableDeclaration, LocalVariableDeclarationSubstitute>
      buildVariableDeclarationSubstitutes(
          MPORThread pThread,
          ImmutableList<Entry<CVariableDeclaration, CIdExpression>> pGlobalSubstitutes,
          ImmutableTable<CFAEdgeForThread, CParameterDeclaration, ImmutableList<CIdExpression>>
              pParameterSubstitutes,
          ImmutableMap<CParameterDeclaration, CIdExpression> pMainFunctionArgSubstitutes,
          ImmutableTable<CFAEdgeForThread, CParameterDeclaration, CIdExpression>
              pStartRoutineArgSubstitutes,
          int pThreadId,
          ImmutableMultimap<CVariableDeclaration, Optional<CFAEdgeForThread>>
              pLocalVariableDeclarations)
          throws UnrecognizedCodeException {

    // step 1: create dummy CVariableDeclaration substitutes which may be adjusted in step 2
    ImmutableTable<
            Optional<CFAEdgeForThread>, CVariableDeclaration, LocalVariableDeclarationSubstitute>
        dummySubstitutes =
            buildLocalVariableDummySubstitutes(pThreadId, pLocalVariableDeclarations);

    // create dummy substitution
    MPORSubstitution dummySubstitution =
        new MPORSubstitution(
            true,
            options,
            pThread,
            pGlobalSubstitutes,
            dummySubstitutes,
            pParameterSubstitutes,
            pMainFunctionArgSubstitutes,
            pStartRoutineArgSubstitutes,
            utils);

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
                    tracker));
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

  private ImmutableTable<
          Optional<CFAEdgeForThread>, CVariableDeclaration, LocalVariableDeclarationSubstitute>
      buildLocalVariableDummySubstitutes(
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
          if (options.inputTypeDeclarations() || !storageClass.equals(CStorageClass.EXTERN)) {
            Optional<String> functionName = getFunctionNameByCallContext(callContext);
            String substituteName =
                SeqNameUtil.buildLocalVariableName(
                    options, variableDeclaration, pThreadId, call++, functionName);
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

  private Optional<String> getFunctionNameByCallContext(Optional<CFAEdgeForThread> pCallContext) {
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

  private CInitializerExpression substituteInitializerExpression(
      CInitializerExpression pOriginal, CExpression pExpression) {

    return new CInitializerExpression(pOriginal.getFileLocation(), pExpression);
  }

  /**
   * Creates a clone of the given CVariableDeclaration with substituted name(s).
   *
   * @param pOriginal the variable declaration to substitute
   */
  private CVariableDeclaration substituteVariableDeclaration(
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
  private CVariableDeclaration substituteVariableDeclaration(
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

  /**
   * We use {@link CVariableDeclaration}s when substituting {@link CParameterDeclaration}s in the
   * sequentialization because all functions are inlined.
   */
  private static CVariableDeclaration substituteParameterDeclaration(
      CParameterDeclaration pOriginal,
      CInitializerExpression pInitializerExpression,
      String pName) {

    return new CVariableDeclaration(
        pOriginal.getFileLocation(),
        false,
        CStorageClass.AUTO,
        pOriginal.getType(),
        pName,
        pOriginal.getOrigName(),
        pOriginal.getQualifiedName(),
        pInitializerExpression);
  }
}
