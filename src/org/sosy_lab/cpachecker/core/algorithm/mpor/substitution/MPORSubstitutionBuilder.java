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
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;

public class MPORSubstitutionBuilder {

  public static ImmutableList<MPORSubstitution> buildSubstitutions(
      MPOROptions pOptions,
      ImmutableSet<CVariableDeclaration> pGlobalVariableDeclarations,
      ImmutableList<MPORThread> pThreads,
      CBinaryExpressionBuilder pBinaryExpressionBuilder) {

    // step 1: create global variable substitutes, their initializer cannot be local/param variables
    MPORThread mainThread = pThreads.stream().filter(t -> t.isMain()).findAny().orElseThrow();
    ImmutableMap<CVariableDeclaration, CIdExpression> globalVarSubstitutes =
        getGlobalVariableSubstitutes(
            pOptions, mainThread, pGlobalVariableDeclarations, pBinaryExpressionBuilder);

    ImmutableList.Builder<MPORSubstitution> rSubstitutions = ImmutableList.builder();

    // step 2: for each thread, create substitution
    for (MPORThread thread : pThreads) {
      ImmutableMap<ThreadEdge, ImmutableMap<CParameterDeclaration, CIdExpression>>
          parameterSubstitutes = getParameterSubstitutes(pOptions, thread);
      ImmutableMap<CVariableDeclaration, ImmutableMap<Optional<ThreadEdge>, CIdExpression>>
          localVarSubstitutes =
              buildVariableDeclarationSubstitutes(
                  pOptions,
                  thread,
                  globalVarSubstitutes,
                  parameterSubstitutes,
                  thread.id,
                  thread.localVars,
                  pBinaryExpressionBuilder);
      rSubstitutions.add(
          new MPORSubstitution(
              thread,
              globalVarSubstitutes,
              localVarSubstitutes,
              parameterSubstitutes,
              pBinaryExpressionBuilder));
    }
    return rSubstitutions.build();
  }

  private static ImmutableMap<CVariableDeclaration, CIdExpression> getGlobalVariableSubstitutes(
      MPOROptions pOptions,
      MPORThread pThread,
      ImmutableSet<CVariableDeclaration> pGlobalVariableDeclarations,
      CBinaryExpressionBuilder pBinaryExpressionBuilder) {

    checkArgument(pThread.isMain(), "thread must be main for global variable substitution");

    // step 1: create dummy CVariableDeclaration substitutes which may be adjusted in step 2
    ImmutableMap<CVariableDeclaration, CIdExpression> dummyGlobalSubstitutes =
        initGlobalSubstitutes(pOptions, pGlobalVariableDeclarations);

    // create dummy substitution. we can use empty maps for local and parameter substitutions
    // because initializers of global variables are always global variables, if present
    MPORSubstitution dummySubstitution =
        new MPORSubstitution(
            pThread,
            dummyGlobalSubstitutes,
            ImmutableMap.of(),
            ImmutableMap.of(),
            pBinaryExpressionBuilder);

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
                    initializerExpression.getExpression(), Optional.empty(), Optional.empty()));
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
      if (threadEdge.cfaEdge instanceof CFunctionCallEdge functionCallEdge) {
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
                  pOptions,
                  parameterDeclaration,
                  pThread.id,
                  functionDeclaration.getOrigName(),
                  call);
          // we use variable declarations for parameters in the sequentialization
          CVariableDeclaration variableDeclaration =
              substituteVariableDeclaration(parameterDeclaration.asVariableDeclaration(), varName);
          substitutes.put(
              parameterDeclaration, SeqExpressionBuilder.buildIdExpression(variableDeclaration));
        }
        rParameterSubstitutes.put(threadEdge, substitutes.buildOrThrow());
      }
    }
    return rParameterSubstitutes.buildOrThrow();
  }

  // TODO split into functions and improve overview
  /**
   * Creates substitutes for all variables in the program and maps them to their original. The
   * substitutes differ only in their name.
   */
  private static ImmutableMap<
          CVariableDeclaration, ImmutableMap<Optional<ThreadEdge>, CIdExpression>>
      buildVariableDeclarationSubstitutes(
          MPOROptions pOptions,
          MPORThread pThread,
          ImmutableMap<CVariableDeclaration, CIdExpression> pGlobalSubstitutes,
          ImmutableMap<ThreadEdge, ImmutableMap<CParameterDeclaration, CIdExpression>>
              pParameterSubstitutes,
          int pThreadId,
          ImmutableMultimap<CVariableDeclaration, Optional<ThreadEdge>> pVariableDeclarations,
          CBinaryExpressionBuilder pBinaryExpressionBuilder) {

    // step 1: create dummy CVariableDeclaration substitutes which may be adjusted in step 2
    ImmutableMap<CVariableDeclaration, ImmutableMap<Optional<ThreadEdge>, CIdExpression>>
        dummySubstitutes = initSubstitutes(pOptions, pThreadId, pVariableDeclarations);

    // create dummy substitution
    MPORSubstitution dummySubstitution =
        new MPORSubstitution(
            pThread,
            pGlobalSubstitutes,
            dummySubstitutes,
            pParameterSubstitutes,
            pBinaryExpressionBuilder);

    // step 2: replace initializers of CVariableDeclarations with substitutes
    ImmutableMap.Builder<CVariableDeclaration, ImmutableMap<Optional<ThreadEdge>, CIdExpression>>
        rFinalSubstitutes = ImmutableMap.builder();

    for (var variableToSubstitutesEntry : dummySubstitutes.entrySet()) {
      CVariableDeclaration variableDeclaration = variableToSubstitutesEntry.getKey();
      ImmutableMap.Builder<Optional<ThreadEdge>, CIdExpression> substitutes =
          ImmutableMap.builder();

      for (var callContextToSubstituteEntry : variableToSubstitutesEntry.getValue().entrySet()) {
        CInitializer initializer = variableDeclaration.getInitializer();
        // TODO handle CInitializerList
        if (initializer instanceof CInitializerExpression initializerExpression) {
          CIdExpression substituteExpression = callContextToSubstituteEntry.getValue();
          assert substituteExpression.getDeclaration() instanceof CVariableDeclaration
              : "substitute expression declaration must be variable declaration";
          CVariableDeclaration substituteDeclaration =
              (CVariableDeclaration) substituteExpression.getDeclaration();

          Optional<ThreadEdge> callContext = callContextToSubstituteEntry.getKey();
          // TODO not sure if global var set required here?
          CInitializerExpression initializerSubstitute =
              substituteInitializerExpression(
                  initializerExpression,
                  dummySubstitution.substitute(
                      initializerExpression.getExpression(), callContext, Optional.empty()));
          CVariableDeclaration finalSubstitute =
              substituteVariableDeclaration(substituteDeclaration, initializerSubstitute);

          substitutes.put(callContext, SeqExpressionBuilder.buildIdExpression(finalSubstitute));
        } else {
          substitutes.put(callContextToSubstituteEntry);
        }
      }
      rFinalSubstitutes.put(variableDeclaration, substitutes.buildOrThrow());
    }
    return rFinalSubstitutes.buildOrThrow();
  }

  private static ImmutableMap<
          CVariableDeclaration, ImmutableMap<Optional<ThreadEdge>, CIdExpression>>
      initSubstitutes(
          MPOROptions pOptions,
          int pThreadId,
          ImmutableMultimap<CVariableDeclaration, Optional<ThreadEdge>> pVariableDeclarations) {

    ImmutableMap.Builder<CVariableDeclaration, ImmutableMap<Optional<ThreadEdge>, CIdExpression>>
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
        dummySubstitutes.put(variableDeclaration, substitutes.buildOrThrow());
      }
    }
    return dummySubstitutes.buildOrThrow();
  }

  private static Optional<String> getFunctionNameByCallContext(Optional<ThreadEdge> pCallContext) {
    if (pCallContext.isPresent()) {
      assert pCallContext.orElseThrow().cfaEdge instanceof CFunctionCallEdge;
      CFunctionCallEdge functionCallEdge = (CFunctionCallEdge) pCallContext.orElseThrow().cfaEdge;
      return Optional.of(
          functionCallEdge.getFunctionCallExpression().getDeclaration().getOrigName());
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
