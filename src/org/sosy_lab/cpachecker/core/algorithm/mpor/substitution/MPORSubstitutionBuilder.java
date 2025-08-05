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
import com.google.common.collect.ImmutableSet;
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
      ImmutableSet<CVariableDeclaration> pGlobalVariableDeclarations,
      ImmutableList<MPORThread> pThreads,
      CBinaryExpressionBuilder pBinaryExpressionBuilder,
      LogManager pLogger) {

    // step 1: create global variable substitutes, their initializer cannot be local/param variables
    MPORThread mainThread = ThreadUtil.extractMainThread(pThreads);
    ImmutableMap<CVariableDeclaration, CIdExpression> globalVarSubstitutes =
        getGlobalVariableSubstitutes(
            pOptions, mainThread, pGlobalVariableDeclarations, pBinaryExpressionBuilder, pLogger);
    ImmutableMap<CParameterDeclaration, CIdExpression> mainFunctionArgSubstitutes =
        getMainFunctionArgSubstitutes(pOptions, mainThread);
    // use same start_routine arg substitutes across threads, so that all threads can access them
    ImmutableMap<ThreadEdge, ImmutableMap<CParameterDeclaration, CIdExpression>>
        startRoutineArgSubstitutes = getStartRoutineArgSubstitutes(pOptions, pThreads);

    ImmutableList.Builder<MPORSubstitution> rSubstitutions = ImmutableList.builder();

    // step 2: for each thread, create substitution
    for (MPORThread thread : pThreads) {
      ImmutableMap<ThreadEdge, ImmutableMap<CParameterDeclaration, CIdExpression>>
          parameterSubstitutes = getParameterSubstitutes(pOptions, thread);
      ImmutableMap<CVariableDeclaration, LocalVariableDeclarationSubstitute> localVarSubstitutes =
          buildVariableDeclarationSubstitutes(
              pOptions,
              thread,
              globalVarSubstitutes,
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
              globalVarSubstitutes,
              localVarSubstitutes,
              parameterSubstitutes,
              mainFunctionArgSubstitutes,
              startRoutineArgSubstitutes,
              pBinaryExpressionBuilder,
              pLogger));
    }
    return rSubstitutions.build();
  }

  private static ImmutableMap<CVariableDeclaration, CIdExpression> getGlobalVariableSubstitutes(
      MPOROptions pOptions,
      MPORThread pThread,
      ImmutableSet<CVariableDeclaration> pGlobalVariableDeclarations,
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
            ImmutableMap.of(),
            ImmutableMap.of(),
            ImmutableMap.of(),
            ImmutableMap.of(),
            pBinaryExpressionBuilder,
            pLogger);

    // step 2: replace initializers of CVariableDeclarations with substitutes
    ImmutableMap.Builder<CVariableDeclaration, CIdExpression> rFinalSubstitutes =
        ImmutableMap.builder();

    for (var entry : dummyGlobalSubstitutes.entrySet()) {
      CVariableDeclaration variableDeclaration = entry.getKey();
      CIdExpression idExpression = entry.getValue();
      CInitializer initializer = variableDeclaration.getInitializer();
      // TODO handle CInitializerList
      if (initializer instanceof CInitializerExpression initializerExpression) {
        CInitializerExpression initExprSub =
            substituteInitializerExpression(
                initializerExpression,
                dummySubstitution.substitute(
                    // no call context for global variables
                    initializerExpression.getExpression(),
                    Optional.empty(),
                    false,
                    false,
                    false,
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty()));
        CVariableDeclaration finalSub =
            substituteVariableDeclaration(variableDeclaration, initExprSub);
        rFinalSubstitutes.put(finalSub, idExpression);
      } else {
        rFinalSubstitutes.put(variableDeclaration, idExpression);
      }
    }
    return rFinalSubstitutes.buildOrThrow();
  }

  private static ImmutableMap<CVariableDeclaration, CIdExpression> initGlobalSubstitutes(
      MPOROptions pOptions, ImmutableSet<CVariableDeclaration> pGlobalVariableDeclarations) {

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
  private static ImmutableMap<ThreadEdge, ImmutableMap<CParameterDeclaration, CIdExpression>>
      getParameterSubstitutes(MPOROptions pOptions, MPORThread pThread) {

    ImmutableMap.Builder<ThreadEdge, ImmutableMap<CParameterDeclaration, CIdExpression>>
        rParameterSubstitutes = ImmutableMap.builder();
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
        rParameterSubstitutes.put(
            threadEdge,
            buildParameterSubstitutes(pOptions, pThread, functionDeclaration, callNumber));
      }
    }
    return rParameterSubstitutes.buildOrThrow();
  }

  private static ImmutableMap<CParameterDeclaration, CIdExpression> buildParameterSubstitutes(
      MPOROptions pOptions,
      MPORThread pThread,
      CFunctionDeclaration pFunctionDeclaration,
      int pCallNumber) {

    ImmutableMap.Builder<CParameterDeclaration, CIdExpression> substitutes = ImmutableMap.builder();
    for (CParameterDeclaration parameterDeclaration : pFunctionDeclaration.getParameters()) {
      String varName =
          SeqNameUtil.buildParameterName(
              pOptions,
              parameterDeclaration,
              pThread.id,
              pFunctionDeclaration.getOrigName(),
              pCallNumber);
      // we use variable declarations for parameters in the sequentialization
      CVariableDeclaration variableDeclaration =
          substituteVariableDeclaration(parameterDeclaration.asVariableDeclaration(), varName);
      substitutes.put(
          parameterDeclaration, SeqExpressionBuilder.buildIdExpression(variableDeclaration));
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

  private static ImmutableMap<ThreadEdge, ImmutableMap<CParameterDeclaration, CIdExpression>>
      getStartRoutineArgSubstitutes(MPOROptions pOptions, ImmutableList<MPORThread> pAllThreads) {

    ImmutableMap.Builder<ThreadEdge, ImmutableMap<CParameterDeclaration, CIdExpression>>
        rArgSubstitutes = ImmutableMap.builder();

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
            CFunctionType startRoutineType = PthreadUtil.extractStartRoutine(cfaEdge);
            CFunctionDeclaration startRoutineDeclaration =
                (CFunctionDeclaration) createdThread.cfa.entryNode.getFunction();
            // start_routine matches
            if (startRoutineDeclaration.getType().equals(startRoutineType)) {
              // start_routines only have one parameter
              CParameterDeclaration parameterDeclaration =
                  startRoutineDeclaration.getParameters().get(0);
              String varName =
                  SeqNameUtil.buildStartRoutineArgName(
                      pOptions,
                      parameterDeclaration,
                      createdThread.id,
                      startRoutineDeclaration.getOrigName());
              // we use variable declarations for start_routine args in the sequentialization
              CVariableDeclaration variableDeclaration =
                  substituteVariableDeclaration(
                      parameterDeclaration.asVariableDeclaration(), varName);
              CIdExpression substitute =
                  SeqExpressionBuilder.buildIdExpression(variableDeclaration);
              rArgSubstitutes.put(threadEdge, ImmutableMap.of(parameterDeclaration, substitute));
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
  private static ImmutableMap<CVariableDeclaration, LocalVariableDeclarationSubstitute>
      buildVariableDeclarationSubstitutes(
          MPOROptions pOptions,
          MPORThread pThread,
          ImmutableMap<CVariableDeclaration, CIdExpression> pGlobalSubstitutes,
          ImmutableMap<ThreadEdge, ImmutableMap<CParameterDeclaration, CIdExpression>>
              pParameterSubstitutes,
          ImmutableMap<CParameterDeclaration, CIdExpression> pMainFunctionArgSubstitutes,
          ImmutableMap<ThreadEdge, ImmutableMap<CParameterDeclaration, CIdExpression>>
              pStartRoutineArgSubstitutes,
          int pThreadId,
          ImmutableMultimap<CVariableDeclaration, Optional<ThreadEdge>> pLocalVariableDeclarations,
          CBinaryExpressionBuilder pBinaryExpressionBuilder,
          LogManager pLogger) {

    // step 1: create dummy CVariableDeclaration substitutes which may be adjusted in step 2
    ImmutableMap<CVariableDeclaration, LocalVariableDeclarationSubstitute> dummySubstitutes =
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
    ImmutableMap.Builder<CVariableDeclaration, LocalVariableDeclarationSubstitute>
        rFinalSubstitutes = ImmutableMap.builder();

    for (var dummySubstitute : dummySubstitutes.entrySet()) {
      CVariableDeclaration variableDeclaration = dummySubstitute.getKey();
      ImmutableMap.Builder<Optional<ThreadEdge>, CIdExpression> substitutes =
          ImmutableMap.builder();
      Set<CVariableDeclaration> accessedGlobalVariables = new HashSet<>();
      for (var substitute : dummySubstitute.getValue().substitutes.entrySet()) {
        CInitializer initializer = variableDeclaration.getInitializer();
        // TODO handle CInitializerList
        if (initializer instanceof CInitializerExpression initializerExpression) {
          CIdExpression substituteExpression = substitute.getValue();
          assert substituteExpression.getDeclaration() instanceof CVariableDeclaration
              : "substitute expression declaration must be variable declaration";
          CVariableDeclaration substituteDeclaration =
              (CVariableDeclaration) substituteExpression.getDeclaration();

          Optional<ThreadEdge> callContext =
              ThreadUtil.getCallContextOrStartRoutineCall(substitute.getKey(), pThread);
          CInitializerExpression initializerSubstitute =
              substituteInitializerExpression(
                  initializerExpression,
                  dummySubstitution.substitute(
                      initializerExpression.getExpression(),
                      callContext,
                      false,
                      false,
                      false,
                      Optional.empty(),
                      Optional.empty(),
                      Optional.empty(),
                      Optional.of(accessedGlobalVariables),
                      Optional.empty()));
          CVariableDeclaration finalSubstitute =
              substituteVariableDeclaration(substituteDeclaration, initializerSubstitute);
          substitutes.put(callContext, SeqExpressionBuilder.buildIdExpression(finalSubstitute));
        } else {
          substitutes.put(substitute);
        }
      }
      LocalVariableDeclarationSubstitute localSubstitute =
          new LocalVariableDeclarationSubstitute(
              substitutes.buildOrThrow(), ImmutableSet.copyOf(accessedGlobalVariables));
      rFinalSubstitutes.put(variableDeclaration, localSubstitute);
    }
    return rFinalSubstitutes.buildOrThrow();
  }

  private static ImmutableMap<CVariableDeclaration, LocalVariableDeclarationSubstitute>
      buildLocalVariableDummySubstitutes(
          MPOROptions pOptions,
          int pThreadId,
          ImmutableMultimap<CVariableDeclaration, Optional<ThreadEdge>> pVariableDeclarations) {

    ImmutableMap.Builder<CVariableDeclaration, LocalVariableDeclarationSubstitute>
        dummySubstitutes = ImmutableMap.builder();
    Set<CVariableDeclaration> visitedKeys = new HashSet<>();
    for (var entry : pVariableDeclarations.entries()) {
      CVariableDeclaration variableDeclaration = entry.getKey();
      if (visitedKeys.add(variableDeclaration)) {
        ImmutableMap.Builder<Optional<ThreadEdge>, CIdExpression> substitutes =
            ImmutableMap.builder();
        int call = 1;
        for (Optional<ThreadEdge> callContext : pVariableDeclarations.get(entry.getKey())) {
          CStorageClass storageClass = variableDeclaration.getCStorageClass();

          // if type declarations are not included, the storage class cannot be extern
          if (pOptions.inputTypeDeclarations || !storageClass.equals(CStorageClass.EXTERN)) {
            Optional<String> functionName = getFunctionNameByCallContext(callContext);
            String substituteName =
                SeqNameUtil.buildLocalVariableName(
                    pOptions, variableDeclaration, pThreadId, call++, functionName);
            CVariableDeclaration substitute =
                substituteVariableDeclaration(variableDeclaration, substituteName);
            CIdExpression substituteExpression = SeqExpressionBuilder.buildIdExpression(substitute);
            substitutes.put(callContext, substituteExpression);
          }
        }
        LocalVariableDeclarationSubstitute substitute =
            new LocalVariableDeclarationSubstitute(substitutes.buildOrThrow(), ImmutableSet.of());
        dummySubstitutes.put(variableDeclaration, substitute);
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
}
