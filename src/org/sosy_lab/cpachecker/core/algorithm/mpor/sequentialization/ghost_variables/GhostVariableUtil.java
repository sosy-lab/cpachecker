// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables;

import static org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadFunctionType.PTHREAD_MUTEX_LOCK;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.AFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqDeclarationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqInitializers.SeqInitializer;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqTypes.SeqSimpleType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorEncoding;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.ScalarBitVectorVariable;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.function_statements.FunctionParameterAssignment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.function_statements.FunctionReturnValueAssignment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.function_statements.FunctionStatements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.thread_simulation.MutexLocked;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.thread_simulation.ThreadBeginsAtomic;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.thread_simulation.ThreadJoinsThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.thread_simulation.ThreadLocksMutex;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.thread_simulation.ThreadSimulationVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.MPORSubstitution;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;

public class GhostVariableUtil {

  public static BitVectorVariables buildBitVectorVariables(
      MPOROptions pOptions,
      ImmutableSet<MPORThread> pThreads,
      ImmutableMap<ThreadEdge, SubstituteEdge> pSubstituteEdges) {

    ImmutableMap.Builder<MPORThread, CIdExpression> rBitVectors = ImmutableMap.builder();
    if (pOptions.porBitVector) {
      for (MPORThread thread : pThreads) {
        String name = SeqNameUtil.buildBitVectorName(pOptions, thread.id);
        // this declaration is not actually used, we only need it for the CIdExpression
        CVariableDeclaration declaration =
            SeqDeclarationBuilder.buildVariableDeclaration(
                true, SeqSimpleType.INT, name, SeqInitializer.INT_MINUS_1);
        rBitVectors.put(thread, SeqExpressionBuilder.buildIdExpression(declaration));
      }
      // collect all global variables accessed in substitute edges, and assign unique ids
      ImmutableList<CVariableDeclaration> allGlobalVariables =
          SubstituteUtil.getAllGlobalVariables(pSubstituteEdges.values());
      ImmutableList<ScalarBitVectorVariable> bitVectorGlobalVariables =
          createBitVectorGlobalVariables(pOptions, pThreads, allGlobalVariables);
      return new BitVectorVariables(rBitVectors.buildOrThrow(), bitVectorGlobalVariables);
    }
    // TODO optionals?
    return new BitVectorVariables(ImmutableMap.of(), ImmutableList.of());
  }

  private static ImmutableList<ScalarBitVectorVariable> createBitVectorGlobalVariables(
      MPOROptions pOptions,
      ImmutableSet<MPORThread> pThreads,
      ImmutableList<CVariableDeclaration> pGlobalVariableDeclaration) {

    ImmutableList.Builder<ScalarBitVectorVariable> rVariables = ImmutableList.builder();
    int id = 0;
    for (CVariableDeclaration variableDeclaration : pGlobalVariableDeclaration) {
      assert variableDeclaration.isGlobal() : "variable declaration for bit vector must be global";
      ScalarBitVectorVariable globalVariable =
          new ScalarBitVectorVariable(
              id++,
              variableDeclaration,
              pOptions.porBitVectorEncoding.equals(BitVectorEncoding.SCALAR)
                  ? Optional.of(createAccessVariables(pOptions, pThreads, variableDeclaration))
                  : Optional.empty());
      rVariables.add(globalVariable);
    }
    return rVariables.build();
  }

  private static ImmutableMap<MPORThread, CIdExpression> createAccessVariables(
      MPOROptions pOptions,
      ImmutableSet<MPORThread> pThreads,
      CVariableDeclaration pVariableDeclaration) {

    ImmutableMap.Builder<MPORThread, CIdExpression> rAccessVariables = ImmutableMap.builder();
    for (MPORThread thread : pThreads) {
      rAccessVariables.put(
          thread, BitVectorUtil.createScalarAccessVariable(pOptions, thread, pVariableDeclaration));
    }
    return rAccessVariables.buildOrThrow();
  }

  public static FunctionStatements buildFunctionVariables(
      MPORThread pThread,
      MPORSubstitution pSubstitution,
      ImmutableMap<ThreadEdge, SubstituteEdge> pSubstituteEdges) {

    return new FunctionStatements(
        buildParameterAssignments(pSubstitution),
        buildReturnValueAssignments(pThread, pSubstituteEdges));
  }

  public static ThreadSimulationVariables buildThreadSimulationVariables(
      MPOROptions pOptions,
      ImmutableSet<MPORThread> pThreads,
      ImmutableMap<ThreadEdge, SubstituteEdge> pSubstituteEdges) {

    return new ThreadSimulationVariables(
        pOptions,
        buildMutexLockedVariables(pOptions, pThreads, pSubstituteEdges),
        buildThreadAwaitsMutexVariables(pOptions, pThreads, pSubstituteEdges),
        buildThreadJoinsThreadVariables(pOptions, pThreads),
        buildThreadBeginsAtomicVariables(pOptions, pThreads));
  }

  private static ImmutableMap<CIdExpression, MutexLocked> buildMutexLockedVariables(
      MPOROptions pOptions,
      ImmutableSet<MPORThread> pThreads,
      ImmutableMap<ThreadEdge, SubstituteEdge> pSubstituteEdges) {

    ImmutableMap.Builder<CIdExpression, MutexLocked> rVars = ImmutableMap.builder();
    Set<CIdExpression> lockedVariables = new HashSet<>();
    for (MPORThread thread : pThreads) {
      for (ThreadEdge threadEdge : thread.cfa.threadEdges) {
        if (pSubstituteEdges.containsKey(threadEdge)) {
          SubstituteEdge substituteEdge = Objects.requireNonNull(pSubstituteEdges.get(threadEdge));
          CFAEdge cfaEdge = substituteEdge.cfaEdge;

          // extract pthread_mutex_t based on function calls to pthread_mutex_lock
          if (PthreadUtil.callsPthreadFunction(cfaEdge, PTHREAD_MUTEX_LOCK)) {
            CIdExpression pthreadMutexT = PthreadUtil.extractPthreadMutexT(threadEdge.cfaEdge);
            if (lockedVariables.add(pthreadMutexT)) { // add mutex only once
              String varName = SeqNameUtil.buildMutexLockedName(pOptions, pthreadMutexT.getName());
              CIdExpression mutexLocked =
                  SeqExpressionBuilder.buildIdExpressionWithIntegerInitializer(
                      varName, SeqInitializer.INT_0);
              rVars.put(pthreadMutexT, new MutexLocked(mutexLocked));
            }
          }
        }
      }
    }
    return rVars.buildOrThrow();
  }

  private static ImmutableMap<MPORThread, ImmutableMap<CIdExpression, ThreadLocksMutex>>
      buildThreadAwaitsMutexVariables(
          MPOROptions pOptions,
          ImmutableSet<MPORThread> pThreads,
          ImmutableMap<ThreadEdge, SubstituteEdge> pSubstituteEdges) {

    ImmutableMap.Builder<MPORThread, ImmutableMap<CIdExpression, ThreadLocksMutex>> rVars =
        ImmutableMap.builder();
    for (MPORThread thread : pThreads) {
      Map<CIdExpression, ThreadLocksMutex> locksVariables = new HashMap<>();
      for (ThreadEdge threadEdge : thread.cfa.threadEdges) {
        if (pSubstituteEdges.containsKey(threadEdge)) {
          SubstituteEdge substitute = Objects.requireNonNull(pSubstituteEdges.get(threadEdge));
          if (PthreadUtil.callsPthreadFunction(
              substitute.cfaEdge, PthreadFunctionType.PTHREAD_MUTEX_LOCK)) {
            CIdExpression pthreadMutexT = PthreadUtil.extractPthreadMutexT(threadEdge.cfaEdge);
            // multiple lock calls within one thread to the same mutex are possible -> only need one
            if (!locksVariables.containsKey(pthreadMutexT)) {
              String varName =
                  SeqNameUtil.buildThreadLocksMutexName(
                      pOptions, thread.id, pthreadMutexT.getName());
              CIdExpression awaits =
                  SeqExpressionBuilder.buildIdExpressionWithIntegerInitializer(
                      varName, SeqInitializer.INT_0);
              locksVariables.put(pthreadMutexT, new ThreadLocksMutex(awaits));
            }
          }
        }
      }
      rVars.put(thread, ImmutableMap.copyOf(locksVariables));
    }
    return rVars.buildOrThrow();
  }

  private static ImmutableMap<MPORThread, ImmutableMap<MPORThread, ThreadJoinsThread>>
      buildThreadJoinsThreadVariables(MPOROptions pOptions, ImmutableSet<MPORThread> pThreads) {

    ImmutableMap.Builder<MPORThread, ImmutableMap<MPORThread, ThreadJoinsThread>> rVars =
        ImmutableMap.builder();
    for (MPORThread thread : pThreads) {
      Map<MPORThread, ThreadJoinsThread> targetThreads = new HashMap<>();
      for (ThreadEdge threadEdge : thread.cfa.threadEdges) {
        CFAEdge cfaEdge = threadEdge.cfaEdge;
        if (PthreadUtil.callsPthreadFunction(cfaEdge, PthreadFunctionType.PTHREAD_JOIN)) {
          MPORThread targetThread = PthreadUtil.extractThread(pThreads, cfaEdge);
          // multiple join calls within one thread to the same thread are possible -> only need one
          if (!targetThreads.containsKey(targetThread)) {
            String varName =
                SeqNameUtil.buildThreadJoinsThreadName(pOptions, thread.id, targetThread.id);
            CIdExpression joins =
                SeqExpressionBuilder.buildIdExpressionWithIntegerInitializer(
                    varName, SeqInitializer.INT_0);
            targetThreads.put(targetThread, new ThreadJoinsThread(joins));
          }
        }
      }
      rVars.put(thread, ImmutableMap.copyOf(targetThreads));
    }
    return rVars.buildOrThrow();
  }

  private static ImmutableMap<MPORThread, ThreadBeginsAtomic> buildThreadBeginsAtomicVariables(
      MPOROptions pOptions, ImmutableSet<MPORThread> pThreads) {

    ImmutableMap.Builder<MPORThread, ThreadBeginsAtomic> rVars = ImmutableMap.builder();
    for (MPORThread thread : pThreads) {
      for (ThreadEdge threadEdge : thread.cfa.threadEdges) {
        CFAEdge cfaEdge = threadEdge.cfaEdge;
        if (PthreadUtil.callsPthreadFunction(
            cfaEdge, PthreadFunctionType.__VERIFIER_ATOMIC_BEGIN)) {
          String varName = SeqNameUtil.buildThreadBeginsAtomicName(pOptions, thread.id);
          CIdExpression begin =
              SeqExpressionBuilder.buildIdExpressionWithIntegerInitializer(
                  varName, SeqInitializer.INT_0);
          rVars.put(thread, new ThreadBeginsAtomic(begin));
          break; // only need one call to atomic_begin -> break inner loop
        }
      }
    }
    return rVars.buildOrThrow();
  }

  /**
   * Maps {@link ThreadEdge}s whose {@link CFAEdge} is a {@link CFunctionCallEdge} to a list of
   * {@link FunctionParameterAssignment}s.
   *
   * <p>E.g. {@code func(&paramA, paramB);} in thread 0 is linked to {@code __t0_0_paramA = &paramA
   * ;} and {@code __t0_1_paramB = paramB ;}. Both substitution variables are declared in {@link
   * MPORSubstitution#parameterSubstitutes}.
   */
  private static ImmutableMap<ThreadEdge, ImmutableList<FunctionParameterAssignment>>
      buildParameterAssignments(MPORSubstitution pSubstitution) {

    ImmutableMap.Builder<ThreadEdge, ImmutableList<FunctionParameterAssignment>> rAssignments =
        ImmutableMap.builder();

    // for each function call edge (= calling context)
    for (var entryA : pSubstitution.parameterSubstitutes.entrySet()) {
      ThreadEdge threadEdge = entryA.getKey();
      assert threadEdge.cfaEdge instanceof CFunctionCallEdge;
      CFunctionCallEdge functionCallEdge = (CFunctionCallEdge) entryA.getKey().cfaEdge;
      ImmutableMap<CParameterDeclaration, CIdExpression> parameterSubstitutes = entryA.getValue();

      ImmutableList.Builder<FunctionParameterAssignment> assignments = ImmutableList.builder();
      List<CParameterDeclaration> parameterDeclarations =
          functionCallEdge.getSuccessor().getFunctionDefinition().getParameters();

      // for each parameter, assign the param substitute to the param expression in funcCall
      for (int i = 0; i < parameterDeclarations.size(); i++) {
        CParameterDeclaration parameterDeclaration = parameterDeclarations.get(i);
        CExpression rightHandSide =
            functionCallEdge.getFunctionCallExpression().getParameterExpressions().get(i);
        CIdExpression parameterSubstitute =
            Objects.requireNonNull(parameterSubstitutes.get(parameterDeclaration));
        FunctionParameterAssignment parameterAssignment =
            new FunctionParameterAssignment(
                SeqStatementBuilder.buildExpressionAssignmentStatement(
                    parameterSubstitute,
                    pSubstitution.substitute(
                        rightHandSide, threadEdge.callContext, Optional.empty())));
        assignments.add(parameterAssignment);
      }
      rAssignments.put(threadEdge, assignments.build());
    }
    return rAssignments.buildOrThrow();
  }

  /**
   * Maps {@link ThreadEdge}s whose {@link CFAEdge} is a {@link CReturnStatementEdge} to {@link
   * FunctionReturnValueAssignment}s where the CPAchecker_TMP vars are assigned the return value.
   *
   * <p>Note that {@code main} functions and start routines of threads oftentimes do not have
   * corresponding {@link CFunctionSummaryEdge}s.
   */
  private static ImmutableMap<ThreadEdge, ImmutableSet<FunctionReturnValueAssignment>>
      buildReturnValueAssignments(
          MPORThread pThread, ImmutableMap<ThreadEdge, SubstituteEdge> pSubstituteEdges) {

    ImmutableMap.Builder<ThreadEdge, ImmutableSet<FunctionReturnValueAssignment>>
        rReturnStatements = ImmutableMap.builder();
    for (ThreadEdge threadEdgeA : pThread.cfa.threadEdges) {
      if (pSubstituteEdges.containsKey(threadEdgeA)) {
        SubstituteEdge substituteEdgeA = Objects.requireNonNull(pSubstituteEdges.get(threadEdgeA));

        if (substituteEdgeA.cfaEdge instanceof CReturnStatementEdge returnStatementEdge) {
          ImmutableSet.Builder<FunctionReturnValueAssignment> assigns = ImmutableSet.builder();
          for (ThreadEdge threadEdgeB : pThread.cfa.threadEdges) {
            if (pSubstituteEdges.containsKey(threadEdgeB)) {
              SubstituteEdge substituteEdgeB =
                  Objects.requireNonNull(pSubstituteEdges.get(threadEdgeB));

              if (substituteEdgeB.cfaEdge instanceof CFunctionSummaryEdge functionSummary) {
                // if the summary edge is of the form value = func(); (i.e. an assignment)
                if (functionSummary.getExpression()
                    instanceof CFunctionCallAssignmentStatement assignmentStatement) {
                  AFunctionDeclaration functionDeclarationA =
                      returnStatementEdge.getSuccessor().getFunction();
                  AFunctionType functionTypeA = functionDeclarationA.getType();
                  AFunctionType functionTypeB =
                      functionSummary.getFunctionEntry().getFunction().getType();
                  if (functionTypeA.equals(functionTypeB)) {
                    assert functionDeclarationA instanceof CFunctionDeclaration;
                    FunctionReturnValueAssignment assign =
                        new FunctionReturnValueAssignment(
                            assignmentStatement.getLeftHandSide(),
                            returnStatementEdge.getExpression().orElseThrow());
                    assigns.add(assign);
                  }
                }
              }
            }
          }
          rReturnStatements.put(threadEdgeA, assigns.build());
        }
      }
    }
    return rReturnStatements.buildOrThrow();
  }
}
