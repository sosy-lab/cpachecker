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
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
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
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadUtil;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class MPORSubstitutionBuilder {

  public static ImmutableList<MPORSubstitution> buildSubstitutions(
      MPOROptions pOptions,
      ImmutableList<CVariableDeclaration> pGlobalVariableDeclarations,
      ImmutableList<MPORThread> pThreads,
      CBinaryExpressionBuilder pBinaryExpressionBuilder,
      LogManager pLogger) {

    // step 1: create global variable substitutes, their initializer cannot be local/param variables
    MPORThread mainThread = ThreadUtil.extractMainThread(pThreads);
    ImmutableMap<CVariableDeclaration, CIdExpression> globalVariableSubstitutes =
        buildGlobalVariableSubstitutes(
            pOptions, mainThread, pGlobalVariableDeclarations, pBinaryExpressionBuilder, pLogger);
    ImmutableMap<CParameterDeclaration, CIdExpression> mainFunctionArgSubstitutes =
        getMainFunctionArgSubstitutes(pOptions, mainThread);
    // use same start_routine arg substitutes across threads, so that all threads can access them
    ImmutableTable<ThreadEdge, CParameterDeclaration, CIdExpression> startRoutineArgSubstitutes =
        buildStartRoutineArgSubstitutes(pOptions, pThreads);

    ImmutableList.Builder<MPORSubstitution> rSubstitutions = ImmutableList.builder();

    // step 2: for each thread, create substitution
    for (MPORThread thread : pThreads) {
      ImmutableTable<ThreadEdge, CParameterDeclaration, CIdExpression> parameterSubstitutes =
          buildParameterSubstitutes(pOptions, thread);
      ImmutableTable<Optional<ThreadEdge>, CVariableDeclaration, LocalVariableDeclarationSubstitute>
          localVariableSubstitutes =
              buildVariableDeclarationSubstitutes(
                  pOptions,
                  thread,
                  globalVariableSubstitutes,
                  parameterSubstitutes,
                  mainFunctionArgSubstitutes,
                  startRoutineArgSubstitutes,
                  thread.id,
                  thread.localVariables,
                  pBinaryExpressionBuilder,
                  pLogger);
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
              pBinaryExpressionBuilder,
              pLogger));
    }
    return rSubstitutions.build();
  }

  private static ImmutableMap<CVariableDeclaration, CIdExpression> buildGlobalVariableSubstitutes(
      MPOROptions pOptions,
      MPORThread pThread,
      ImmutableList<CVariableDeclaration> pGlobalVariableDeclarations,
      CBinaryExpressionBuilder pBinaryExpressionBuilder,
      LogManager pLogger) {

    checkArgument(pThread.isMain(), "thread must be main for global variable substitution");

    // step 1: create dummy CVariableDeclaration substitutes which may be adjusted in step 2
    ImmutableMap<CVariableDeclaration, CIdExpression> dummyGlobalSubstitutes =
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
            pBinaryExpressionBuilder,
            pLogger);

    // step 2: replace initializers of CVariableDeclarations with substitutes
    ImmutableMap.Builder<CVariableDeclaration, CIdExpression> rFinalSubstitutes =
        ImmutableMap.builder();

    for (var entry : dummyGlobalSubstitutes.entrySet()) {
      CVariableDeclaration variableDeclaration = entry.getKey();
      CIdExpression idExpression = entry.getValue();
      assert idExpression.getDeclaration() instanceof CVariableDeclaration;
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
                    false,
                    Optional.empty()));
        CVariableDeclaration newSubstituteDeclaration =
            substituteVariableDeclaration(
                (CVariableDeclaration) idExpression.getDeclaration(),
                substituteInitializerExpression);
        CIdExpression newIdExpression =
            SeqExpressionBuilder.buildIdExpression(newSubstituteDeclaration);
        rFinalSubstitutes.put(variableDeclaration, newIdExpression);
      } else {
        rFinalSubstitutes.put(variableDeclaration, idExpression);
      }
    }
    return rFinalSubstitutes.buildOrThrow();
  }

  private static ImmutableMap<CVariableDeclaration, CIdExpression> initGlobalSubstitutes(
      MPOROptions pOptions, ImmutableList<CVariableDeclaration> pGlobalVariableDeclarations) {

    ImmutableMap.Builder<CVariableDeclaration, CIdExpression> dummyGlobalSubstitutes =
        ImmutableMap.builder();
    for (CVariableDeclaration variableDeclaration : pGlobalVariableDeclarations) {
      CStorageClass storageClass = variableDeclaration.getCStorageClass();
      // if type declarations are not included, the storage class cannot be extern
      if (pOptions.inputTypeDeclarations || !storageClass.equals(CStorageClass.EXTERN)) {
        String substituteName = SeqNameUtil.buildGlobalVariableName(pOptions, variableDeclaration);
        CVariableDeclaration substitute =
            substituteVariableDeclaration(variableDeclaration, substituteName);
        CIdExpression substituteExpression = SeqExpressionBuilder.buildIdExpression(substitute);
        dummyGlobalSubstitutes.put(variableDeclaration, substituteExpression);
      }
    }
    return dummyGlobalSubstitutes.buildOrThrow();
  }

  // Parameter =====================================================================================

  /**
   * For each {@link CFunctionCallEdge} (i.e. calling context), we map the parameter declaration to
   * the created parameter variable.
   */
  private static ImmutableTable<ThreadEdge, CParameterDeclaration, CIdExpression>
      buildParameterSubstitutes(MPOROptions pOptions, MPORThread pThread) {

    ImmutableTable.Builder<ThreadEdge, CParameterDeclaration, CIdExpression> rParameterSubstitutes =
        ImmutableTable.builder();
    Map<CFunctionDeclaration, Integer> callCounts = new HashMap<>();

    for (ThreadEdge threadEdge : pThread.cfa.threadEdges) {
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

  private static ImmutableTable<ThreadEdge, CParameterDeclaration, CIdExpression>
      buildParameterSubstitutes(
          MPOROptions pOptions,
          MPORThread pThread,
          ThreadEdge pCallContext,
          CFunctionDeclaration pFunctionDeclaration,
          int pCallNumber) {

    ImmutableTable.Builder<ThreadEdge, CParameterDeclaration, CIdExpression> substitutes =
        ImmutableTable.builder();
    for (CParameterDeclaration parameterDeclaration : pFunctionDeclaration.getParameters()) {
      String varName =
          SeqNameUtil.buildParameterName(
              pOptions,
              parameterDeclaration,
              pThread.id,
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
    for (CParameterDeclaration parameterDeclaration : pMainThread.startRoutine.getParameters()) {
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

  private static ImmutableTable<ThreadEdge, CParameterDeclaration, CIdExpression>
      buildStartRoutineArgSubstitutes(MPOROptions pOptions, ImmutableList<MPORThread> pAllThreads) {

    ImmutableTable.Builder<ThreadEdge, CParameterDeclaration, CIdExpression> rArgSubstitutes =
        ImmutableTable.builder();

    for (MPORThread thread : pAllThreads) {
      for (ThreadEdge threadEdge : thread.cfa.threadEdges) {
        CFAEdge cfaEdge = threadEdge.cfaEdge;
        if (PthreadUtil.callsPthreadFunction(cfaEdge, PthreadFunctionType.PTHREAD_CREATE)) {
          // TODO if we support pthread return values, this may not hold
          assert cfaEdge instanceof CStatementEdge : "pthread_create must be CStatementEdge";
          CIdExpression pthreadT = PthreadUtil.extractPthreadT(cfaEdge);
          MPORThread createdThread =
              ThreadUtil.getThreadByObject(pAllThreads, Optional.of(pthreadT));
          // pthread_t matches
          if (pthreadT.equals(createdThread.threadObject.orElseThrow())) {
            CFunctionType startRoutineType = PthreadUtil.extractStartRoutineType(cfaEdge);
            CFunctionDeclaration startRoutineDeclaration =
                (CFunctionDeclaration) createdThread.cfa.entryNode.getFunction();
            // start_routine matches
            if (startRoutineDeclaration.getType().equals(startRoutineType)) {
              // start_routines only have one parameter
              CParameterDeclaration parameterDeclaration =
                  startRoutineDeclaration.getParameters().getFirst();
              String varName =
                  SeqNameUtil.buildStartRoutineArgName(
                      pOptions,
                      parameterDeclaration,
                      createdThread.id,
                      startRoutineDeclaration.getOrigName());
              CParameterDeclaration substituteParameterDeclaration =
                  substituteParameterDeclaration(parameterDeclaration, varName);
              CIdExpression substitute =
                  SeqExpressionBuilder.buildIdExpression(substituteParameterDeclaration);
              rArgSubstitutes.put(threadEdge, parameterDeclaration, substitute);
            }
          }
        }
      }
    }
    return rArgSubstitutes.buildOrThrow();
  }

  // Variable Declarations =========================================================================

  // TODO split into functions and improve overview
  /**
   * Creates substitutes for all variables in the program and maps them to their original. The
   * substitutes differ only in their name.
   */
  private static ImmutableTable<
          Optional<ThreadEdge>, CVariableDeclaration, LocalVariableDeclarationSubstitute>
      buildVariableDeclarationSubstitutes(
          MPOROptions pOptions,
          MPORThread pThread,
          ImmutableMap<CVariableDeclaration, CIdExpression> pGlobalSubstitutes,
          ImmutableTable<ThreadEdge, CParameterDeclaration, CIdExpression> pParameterSubstitutes,
          ImmutableMap<CParameterDeclaration, CIdExpression> pMainFunctionArgSubstitutes,
          ImmutableTable<ThreadEdge, CParameterDeclaration, CIdExpression>
              pStartRoutineArgSubstitutes,
          int pThreadId,
          ImmutableMultimap<CVariableDeclaration, Optional<ThreadEdge>> pLocalVariableDeclarations,
          CBinaryExpressionBuilder pBinaryExpressionBuilder,
          LogManager pLogger) {

    // step 1: create dummy CVariableDeclaration substitutes which may be adjusted in step 2
    ImmutableTable<Optional<ThreadEdge>, CVariableDeclaration, LocalVariableDeclarationSubstitute>
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
            pBinaryExpressionBuilder,
            pLogger);

    // step 2: replace initializers of CVariableDeclarations with substitutes
    ImmutableTable.Builder<
            Optional<ThreadEdge>, CVariableDeclaration, LocalVariableDeclarationSubstitute>
        rFinalSubstitutes = ImmutableTable.builder();

    for (var cell : dummySubstitutes.cellSet()) {
      CVariableDeclaration variableDeclaration = cell.getColumnKey();
      CInitializer initializer = variableDeclaration.getInitializer();
      // TODO handle CInitializerList
      if (initializer instanceof CInitializerExpression initializerExpression) {
        LocalVariableDeclarationSubstitute substituteDeclaration = cell.getValue();
        Optional<ThreadEdge> callContext = cell.getRowKey();
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
          Optional<ThreadEdge>, CVariableDeclaration, LocalVariableDeclarationSubstitute>
      buildLocalVariableDummySubstitutes(
          MPOROptions pOptions,
          int pThreadId,
          ImmutableMultimap<CVariableDeclaration, Optional<ThreadEdge>> pVariableDeclarations) {

    ImmutableTable.Builder<
            Optional<ThreadEdge>, CVariableDeclaration, LocalVariableDeclarationSubstitute>
        dummySubstitutes = ImmutableTable.builder();
    Set<CVariableDeclaration> visitedKeys = new HashSet<>();
    for (var entry : pVariableDeclarations.entries()) {
      CVariableDeclaration variableDeclaration = entry.getKey();
      if (visitedKeys.add(variableDeclaration)) {
        int call = 1;
        for (Optional<ThreadEdge> callContext : pVariableDeclarations.get(entry.getKey())) {
          CStorageClass storageClass = variableDeclaration.getCStorageClass();

          // if type declarations are not included, the storage class cannot be extern
          if (pOptions.inputTypeDeclarations || !storageClass.equals(CStorageClass.EXTERN)) {
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

  private static Optional<String> getFunctionNameByCallContext(Optional<ThreadEdge> pCallContext) {
    if (pCallContext.isPresent()) {
      CFAEdge callContext = pCallContext.orElseThrow().cfaEdge;
      if (callContext instanceof CFunctionCallEdge functionCallEdge) {
        CFunctionDeclaration functionDeclaration =
            functionCallEdge.getFunctionCallExpression().getDeclaration();
        return Optional.of(functionDeclaration.getOrigName());
      } else if (callContext instanceof CStatementEdge statementEdge) {
        CFunctionDeclaration functionDeclaration =
            CFAUtils.getFunctionDeclarationByStatementEdge(statementEdge);
        return Optional.of(functionDeclaration.getOrigName());
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
        pOriginal.getOrigName(), // TODO (not relevant for seq)
        pOriginal.getQualifiedName(), // TODO funcName::name but not relevant for seq
        pOriginal.getInitializer());
  }

  /**
   * Creates a clone of the given CVariableDeclaration with substituted name(s) and initializer.
   *
   * @param pOriginal the variable declaration to substitute
   */
  private static CVariableDeclaration substituteVariableDeclaration(
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

  private static CParameterDeclaration substituteParameterDeclaration(
      CParameterDeclaration pOriginal, String pName) {

    return new CParameterDeclaration(pOriginal.getFileLocation(), pOriginal.getType(), pName);
  }
}
