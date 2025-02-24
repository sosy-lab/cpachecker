// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.AFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqInitializers.SeqInitializer;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqStatements.SeqExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.function.FunctionParameterAssignment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.function.FunctionReturnPcRead;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.function.FunctionReturnPcWrite;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.function.FunctionReturnValueAssignment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.function.GhostFunctionVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.thread.GhostThreadVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.thread.MutexLocked;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.thread.ThreadBeginsAtomic;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.thread.ThreadJoinsThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.thread.ThreadLocksMutex;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqNameUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.CSimpleDeclarationSubstitution;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadNode;

public class GhostVariableUtil {

  public static GhostFunctionVariables buildFunctionVariables(
      MPORThread pThread,
      CSimpleDeclarationSubstitution pSubstitution,
      ImmutableMap<ThreadEdge, SubstituteEdge> pSubEdges,
      ImmutableMap<MPORThread, ImmutableMap<CFunctionDeclaration, CIdExpression>> pReturnPcVars) {

    ImmutableMap<ThreadEdge, FunctionReturnPcWrite> returnPcWrites =
        mapReturnPcWrites(pThread, pReturnPcVars.get(pThread));
    return new GhostFunctionVariables(
        mapParameterAssignments(pThread, pSubEdges, pSubstitution),
        mapReturnValueAssignments(pThread, pSubEdges, returnPcWrites),
        returnPcWrites,
        mapReturnPcReads(pThread, pReturnPcVars.get(pThread)));
  }

  public static GhostThreadVariables buildThreadVariables(
      ImmutableSet<MPORThread> pThreads, ImmutableMap<ThreadEdge, SubstituteEdge> pSubEdges) {

    return new GhostThreadVariables(
        mapMutexLockedVars(pThreads, pSubEdges),
        mapThreadAwaitsMutexVars(pThreads, pSubEdges),
        mapThreadJoinsThreadVars(pThreads),
        mapThreadBeginsAtomicVars(pThreads));
  }

  private static ImmutableMap<CIdExpression, MutexLocked> mapMutexLockedVars(
      ImmutableSet<MPORThread> pThreads, ImmutableMap<ThreadEdge, SubstituteEdge> pSubEdges) {

    ImmutableMap.Builder<CIdExpression, MutexLocked> rVars = ImmutableMap.builder();
    for (MPORThread thread : pThreads) {
      for (ThreadEdge threadEdge : thread.cfa.threadEdges) {
        assert pSubEdges.containsKey(threadEdge);
        SubstituteEdge sub = pSubEdges.get(threadEdge);
        assert sub != null;
        // TODO mutexes can also be init with pthread_mutex_t m = PTHREAD_MUTEX_INITIALIZER;
        if (PthreadFunctionType.callsPthreadFunc(
            sub.cfaEdge, PthreadFunctionType.PTHREAD_MUTEX_INIT)) {
          CIdExpression pthreadMutexT = PthreadUtil.extractPthreadMutexT(threadEdge.cfaEdge);
          CIdExpression subPthreadMutexT = PthreadUtil.extractPthreadMutexT(sub.cfaEdge);
          String varName = SeqNameUtil.buildMutexLockedName(subPthreadMutexT.getName());
          CIdExpression mutexLocked =
              SeqIdExpression.buildIdExpressionWithIntegerInitializer(
                  varName, SeqInitializer.INT_0);
          rVars.put(pthreadMutexT, new MutexLocked(mutexLocked));
        }
      }
    }
    // if the same mutex is init twice (i.e. undefined behavior), this throws an exception
    return rVars.buildOrThrow();
  }

  private static ImmutableMap<MPORThread, ImmutableMap<CIdExpression, ThreadLocksMutex>>
      mapThreadAwaitsMutexVars(
          ImmutableSet<MPORThread> pThreads, ImmutableMap<ThreadEdge, SubstituteEdge> pSubEdges) {

    ImmutableMap.Builder<MPORThread, ImmutableMap<CIdExpression, ThreadLocksMutex>> rVars =
        ImmutableMap.builder();
    for (MPORThread thread : pThreads) {
      Map<CIdExpression, ThreadLocksMutex> awaitVars = new HashMap<>();
      for (ThreadEdge threadEdge : thread.cfa.threadEdges) {
        assert pSubEdges.containsKey(threadEdge);
        SubstituteEdge sub = pSubEdges.get(threadEdge);
        assert sub != null;
        if (PthreadFunctionType.callsPthreadFunc(
            sub.cfaEdge, PthreadFunctionType.PTHREAD_MUTEX_LOCK)) {
          CIdExpression pthreadMutexT = PthreadUtil.extractPthreadMutexT(sub.cfaEdge);
          // multiple lock calls within one thread to the same mutex are possible -> only need one
          if (!awaitVars.containsKey(pthreadMutexT)) {
            String varName =
                SeqNameUtil.buildThreadLocksMutexName(thread.id, pthreadMutexT.getName());
            CIdExpression awaits =
                SeqIdExpression.buildIdExpressionWithIntegerInitializer(
                    varName, SeqInitializer.INT_0);
            awaitVars.put(pthreadMutexT, new ThreadLocksMutex(awaits));
          }
        }
      }
      rVars.put(thread, ImmutableMap.copyOf(awaitVars));
    }
    return rVars.buildOrThrow();
  }

  private static ImmutableMap<MPORThread, ImmutableMap<MPORThread, ThreadJoinsThread>>
      mapThreadJoinsThreadVars(ImmutableSet<MPORThread> pThreads) {

    ImmutableMap.Builder<MPORThread, ImmutableMap<MPORThread, ThreadJoinsThread>> rVars =
        ImmutableMap.builder();
    for (MPORThread thread : pThreads) {
      Map<MPORThread, ThreadJoinsThread> targetThreads = new HashMap<>();
      for (ThreadEdge threadEdge : thread.cfa.threadEdges) {
        CFAEdge cfaEdge = threadEdge.cfaEdge;
        if (PthreadFunctionType.callsPthreadFunc(cfaEdge, PthreadFunctionType.PTHREAD_JOIN)) {
          MPORThread targetThread = PthreadUtil.extractThread(pThreads, cfaEdge);
          // multiple join calls within one thread to the same thread are possible -> only need one
          if (!targetThreads.containsKey(targetThread)) {
            String varName = SeqNameUtil.buildThreadJoinsThreadName(thread.id, targetThread.id);
            CIdExpression joins =
                SeqIdExpression.buildIdExpressionWithIntegerInitializer(
                    varName, SeqInitializer.INT_0);
            targetThreads.put(targetThread, new ThreadJoinsThread(joins));
          }
        }
      }
      rVars.put(thread, ImmutableMap.copyOf(targetThreads));
    }
    return rVars.buildOrThrow();
  }

  private static ImmutableMap<MPORThread, ThreadBeginsAtomic> mapThreadBeginsAtomicVars(
      ImmutableSet<MPORThread> pThreads) {

    ImmutableMap.Builder<MPORThread, ThreadBeginsAtomic> rVars = ImmutableMap.builder();
    for (MPORThread thread : pThreads) {
      for (ThreadEdge threadEdge : thread.cfa.threadEdges) {
        CFAEdge cfaEdge = threadEdge.cfaEdge;
        if (PthreadFunctionType.callsPthreadFunc(
            cfaEdge, PthreadFunctionType.__VERIFIER_ATOMIC_BEGIN)) {
          String varName = SeqNameUtil.buildThreadBeginsAtomicName(thread.id);
          CIdExpression begin =
              SeqIdExpression.buildIdExpressionWithIntegerInitializer(
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
   * ;} and {@code __t0_1_paramB = paramB ;}. Both substitution vars are declared in {@link
   * CSimpleDeclarationSubstitution#parameterSubstitutes}.
   */
  private static ImmutableMap<ThreadEdge, ImmutableList<FunctionParameterAssignment>>
      mapParameterAssignments(
          MPORThread pThread,
          ImmutableMap<ThreadEdge, SubstituteEdge> pSubEdges,
          CSimpleDeclarationSubstitution pSub) {

    ImmutableMap.Builder<ThreadEdge, ImmutableList<FunctionParameterAssignment>> rAssigns =
        ImmutableMap.builder();

    for (ThreadEdge threadEdge : pThread.cfa.threadEdges) {
      SubstituteEdge sub = pSubEdges.get(threadEdge);
      assert sub != null;
      if (sub.cfaEdge instanceof CFunctionCallEdge funcCall) {

        ImmutableList.Builder<FunctionParameterAssignment> assigns = ImmutableList.builder();
        List<CParameterDeclaration> paramDecs =
            funcCall.getSuccessor().getFunctionDefinition().getParameters();

        // for each parameter, assign the param substitute to the param expression in funcCall
        for (int i = 0; i < paramDecs.size(); i++) {
          CParameterDeclaration paramDec = paramDecs.get(i);
          CExpression paramExpr =
              funcCall.getFunctionCallExpression().getParameterExpressions().get(i);
          CIdExpression paramSub = pSub.parameterSubstitutes.orElseThrow().get(paramDec);
          assert paramSub != null;
          FunctionParameterAssignment parameterAssignment =
              new FunctionParameterAssignment(
                  SeqExpressionAssignmentStatement.build(paramSub, paramExpr));
          assigns.add(parameterAssignment);
        }
        rAssigns.put(threadEdge, assigns.build());
      }
    }
    return rAssigns.buildOrThrow();
  }

  /**
   * Maps {@link ThreadEdge}s whose {@link CFAEdge} is a {@link CReturnStatementEdge} to {@link
   * FunctionReturnValueAssignment}s where the CPAchecker_TMP vars are assigned the return value.
   *
   * <p>Note that {@code main} functions and start routines of threads oftentimes do not have
   * corresponding {@link CFunctionSummaryEdge}s.
   */
  private static ImmutableMap<ThreadEdge, ImmutableSet<FunctionReturnValueAssignment>>
      mapReturnValueAssignments(
          MPORThread pThread,
          ImmutableMap<ThreadEdge, SubstituteEdge> pSubEdges,
          ImmutableMap<ThreadEdge, FunctionReturnPcWrite> pReturnPcWrites) {

    ImmutableMap.Builder<ThreadEdge, ImmutableSet<FunctionReturnValueAssignment>>
        rReturnStatements = ImmutableMap.builder();
    for (ThreadEdge threadEdgeA : pThread.cfa.threadEdges) {
      SubstituteEdge substituteEdgeA = pSubEdges.get(threadEdgeA);
      assert substituteEdgeA != null;

      if (substituteEdgeA.cfaEdge instanceof CReturnStatementEdge returnStatementEdge) {
        ImmutableSet.Builder<FunctionReturnValueAssignment> assigns = ImmutableSet.builder();
        for (ThreadEdge threadEdgeB : pThread.cfa.threadEdges) {
          SubstituteEdge substituteEdgeB = pSubEdges.get(threadEdgeB);
          assert substituteEdgeB != null;

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
                        pReturnPcWrites.get(threadEdgeB),
                        assignmentStatement.getLeftHandSide(),
                        returnStatementEdge.getExpression().orElseThrow());
                assigns.add(assign);
              }
            }
          }
        }
        rReturnStatements.put(threadEdgeA, assigns.build());
      }
    }
    return rReturnStatements.buildOrThrow();
  }

  // TODO the major problem here is that if we assign a pc that is pruned later, the assignment
  //  results the case not being matched because the origin pc is pruned --> the program stops.
  //  before pruning, we should assert that the pc assigned does not point to a blank statement
  /**
   * Maps {@link ThreadEdge}s whose {@link CFAEdge} is a {@link CFunctionSummaryEdge}s to {@link
   * FunctionReturnPcWrite}s.
   *
   * <p>E.g. a {@link CFunctionSummaryEdge} going from pc 5 to 10 for the function {@code fib} in
   * thread 0 is mapped to the return pc write with the assignment {@code __return_pc_t0_fib = 10;}.
   */
  private static ImmutableMap<ThreadEdge, FunctionReturnPcWrite> mapReturnPcWrites(
      MPORThread pThread, ImmutableMap<CFunctionDeclaration, CIdExpression> pReturnPcVars) {

    ImmutableMap.Builder<ThreadEdge, FunctionReturnPcWrite> rAssigns = ImmutableMap.builder();
    for (ThreadEdge threadEdge : pThread.cfa.threadEdges) {
      if (threadEdge.cfaEdge instanceof CFunctionSummaryEdge funcSummary) {
        if (!MPORUtil.isReachErrorCall(funcSummary)) {
          CFunctionDeclaration function = funcSummary.getFunctionEntry().getFunctionDefinition();
          CIdExpression returnPc = pReturnPcVars.get(function);
          assert returnPc != null;
          FunctionReturnPcWrite returnPcWrite =
              new FunctionReturnPcWrite(returnPc, threadEdge.getSuccessor().pc);
          rAssigns.put(threadEdge, returnPcWrite);
        }
      }
    }
    return rAssigns.buildOrThrow();
  }

  /**
   * Maps {@link ThreadNode}s whose {@link CFANode} is a {@link FunctionExitNode} to {@link
   * FunctionReturnPcRead}s.
   *
   * <p>E.g. a {@link FunctionExitNode} for the function {@code fib} in thread 0 is mapped to the
   * assignment {@code pc[0] = __return_pc_t0_fib;}.
   */
  private static ImmutableMap<ThreadNode, FunctionReturnPcRead> mapReturnPcReads(
      MPORThread pThread, ImmutableMap<CFunctionDeclaration, CIdExpression> pReturnPcVars) {

    Map<ThreadNode, FunctionReturnPcRead> rAssigns = new HashMap<>();
    for (ThreadEdge threadEdge : pThread.cfa.threadEdges) {
      if (threadEdge.cfaEdge instanceof CFunctionSummaryEdge funcSummary) {
        if (!MPORUtil.isReachErrorCall(funcSummary)) {
          Optional<FunctionExitNode> funcExitNode = funcSummary.getFunctionEntry().getExitNode();
          if (funcExitNode.isPresent()) {
            CFunctionDeclaration function = funcSummary.getFunctionEntry().getFunctionDefinition();
            CIdExpression returnPc = pReturnPcVars.get(function);
            ThreadNode threadNode = pThread.cfa.getThreadNodeByCfaNode(funcExitNode.orElseThrow());
            if (!rAssigns.containsKey(threadNode)) {
              FunctionReturnPcRead returnPcRead = new FunctionReturnPcRead(pThread.id, returnPc);
              rAssigns.put(threadNode, returnPcRead);
            }
          }
        }
      }
    }
    return ImmutableMap.copyOf(rAssigns);
  }
}
