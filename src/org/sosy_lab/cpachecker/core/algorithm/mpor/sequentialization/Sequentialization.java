// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.AFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadFuncType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqDeclarations.SeqFunctionDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqDeclarations.SeqVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqInitializers;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqTypes.SeqSimpleType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom.function.SeqAssumeFunction;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom.function.SeqMainFunction;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.helper_vars.FunctionVars;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.helper_vars.PthreadVars;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.loop_case.SeqLoopCase;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.loop_case.SeqLoopCaseStmt;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqComment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqToken;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.CSimpleDeclarationSubstitution;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadNode;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

@SuppressWarnings("unused")
@SuppressFBWarnings({"UUF_UNUSED_FIELD", "URF_UNREAD_FIELD"})
public class Sequentialization {

  // TODO create LineOfCode class with multiple constructors where we define tab amount, semicolon
  //  curly left / right brackets, newlines, etc.
  public static final int TAB_SIZE = 2;

  protected final int threadCount;

  private final CBinaryExpressionBuilder binExprBuilder;

  public Sequentialization(int pThreadCount, CBinaryExpressionBuilder pBinExprBuilder) {
    threadCount = pThreadCount;
    binExprBuilder = pBinExprBuilder;
  }

  /** Generates and returns the entire sequentialized program. */
  public String generateProgram(
      ImmutableMap<MPORThread, CSimpleDeclarationSubstitution> pSubstitutions)
      throws UnrecognizedCodeException {

    StringBuilder rProgram = new StringBuilder();

    // add all original program declarations that are not substituted
    rProgram.append(SeqComment.createNonVarDeclarationComment());
    for (MPORThread thread : pSubstitutions.keySet()) {
      rProgram.append(createNonVarDecString(thread));
    }
    rProgram.append(SeqSyntax.NEWLINE);

    // add all var substitute declarations in the order global - local - params - return_pc
    MPORThread mainThread = MPORAlgorithm.getMainThread(pSubstitutions.keySet());
    rProgram.append(createGlobalVarString(pSubstitutions.get(mainThread)));
    for (var entry : pSubstitutions.entrySet()) {
      rProgram.append(createLocalVarString(entry.getKey().id, entry.getValue()));
    }
    for (var entry : pSubstitutions.entrySet()) {
      rProgram.append(createParamVarString(entry.getKey().id, entry.getValue()));
    }
    rProgram.append(SeqComment.createReturnPcVarsComment());
    ImmutableMap<MPORThread, ImmutableMap<ThreadEdge, CIdExpression>> returnPcVars =
        mapReturnPcVars(pSubstitutions.keySet());
    for (ImmutableMap<ThreadEdge, CIdExpression> map : returnPcVars.values()) {
      for (CIdExpression idExpr : map.values()) {
        rProgram.append(idExpr.getDeclaration().toASTString()).append(SeqSyntax.NEWLINE);
      }
    }
    rProgram.append(SeqSyntax.NEWLINE);

    // add pthread control vars
    rProgram.append(SeqComment.createPthreadVarsComment());
    PthreadVars pthreadVars = buildPthreadVars(pSubstitutions);
    for (CIdExpression idExpr : pthreadVars.getIdExpressions()) {
      assert idExpr.getDeclaration() instanceof CVariableDeclaration;
      CVariableDeclaration varDec = (CVariableDeclaration) idExpr.getDeclaration();
      rProgram.append(varDec.toASTString()).append(SeqSyntax.NEWLINE);
    }
    rProgram.append(SeqSyntax.NEWLINE);

    // add all custom function declarations
    rProgram.append(SeqComment.createFuncDeclarationComment());
    // abort and __VERIFIER_nondet_int may be duplicate depending on the input program
    rProgram.append(SeqFunctionDeclaration.ABORT.toASTString()).append(SeqSyntax.NEWLINE);
    rProgram
        .append(SeqFunctionDeclaration.VERIFIER_NONDET_INT.toASTString())
        .append(SeqSyntax.NEWLINE);
    rProgram.append(SeqFunctionDeclaration.ASSUME.toASTString()).append(SeqSyntax.NEWLINE);
    // main should always be duplicate
    rProgram.append(SeqFunctionDeclaration.MAIN.toASTString()).append(SeqSyntax.NEWLINE);
    rProgram.append(SeqSyntax.NEWLINE);

    // add non main() methods
    SeqAssumeFunction assume = new SeqAssumeFunction(binExprBuilder);
    rProgram.append(assume.toASTString()).append(SeqUtil.repeat(SeqSyntax.NEWLINE, 2));

    // TODO we also need to prune:
    //  - update targetPc to -1 if we reach a thread exit node
    //  - the rest of the state space pruning (e.g. two local accesses after another)
    //    will be done via assume statements
    //    (on this note: we should also remove the const CPAchecker TMP logic and replace it with
    //    assumes as they are basically atomic)
    // create pruned (i.e. only non-empty) cases statements
    ImmutableMap<MPORThread, ImmutableList<SeqLoopCase>> loopCases =
        mapLoopCases(pSubstitutions, returnPcVars, pthreadVars);
    ImmutableMap<MPORThread, ImmutableList<SeqLoopCase>> prunedCases = pruneLoopCases(loopCases);
    CIdExpression numThreads =
        SeqIdExpression.buildIdExpr(
            SeqVariableDeclaration.buildVarDec(
                false,
                // TODO need const int here
                SeqSimpleType.INT,
                SeqToken.NUM_THREADS,
                SeqInitializers.buildIntInitializer(
                    SeqIntegerLiteralExpression.buildIntLiteralExpr(threadCount))));
    SeqMainFunction mainMethod = new SeqMainFunction(binExprBuilder, prunedCases, numThreads);
    rProgram.append(mainMethod.toASTString());

    return rProgram.toString();
  }

  /** Maps threads to their SeqLoopCases. */
  private static ImmutableMap<MPORThread, ImmutableList<SeqLoopCase>> mapLoopCases(
      ImmutableMap<MPORThread, CSimpleDeclarationSubstitution> pSubstitutions,
      ImmutableMap<MPORThread, ImmutableMap<ThreadEdge, CIdExpression>> pReturnPcVars,
      PthreadVars pPthreadVars) {

    ImmutableMap.Builder<MPORThread, ImmutableList<SeqLoopCase>> rLoopCases =
        ImmutableMap.builder();
    for (var entry : pSubstitutions.entrySet()) {
      ImmutableList.Builder<SeqLoopCase> loopCases = ImmutableList.builder();

      MPORThread thread = entry.getKey();
      CSimpleDeclarationSubstitution substitution = entry.getValue();

      ImmutableMap<ThreadEdge, CFAEdge> edgeSubs = substitution.substituteEdges(thread);

      // function handling
      ImmutableMap<ThreadEdge, ImmutableList<CExpressionAssignmentStatement>> paramAssigns =
          mapParamAssigns(thread, edgeSubs, substitution);
      ImmutableMap<ThreadEdge, ImmutableSet<CExpressionAssignmentStatement>> returnStmts =
          mapReturnStmts(thread, edgeSubs);
      ImmutableMap<ThreadEdge, CExpressionAssignmentStatement> returnPcToPcAssigns =
          mapReturnPcToPcAssigns(thread, pReturnPcVars.get(thread));
      ImmutableMap<ThreadNode, CExpressionAssignmentStatement> pcToReturnPcAssigns =
          mapPcToReturnPcAssigns(thread, pReturnPcVars.get(thread));
      FunctionVars funcVars =
          new FunctionVars(paramAssigns, returnStmts, returnPcToPcAssigns, pcToReturnPcAssigns);

      Set<ThreadNode> coveredNodes = new HashSet<>();

      for (ThreadNode threadNode : thread.cfa.threadNodes) {
        if (!coveredNodes.contains(threadNode)) {
          SeqLoopCase loopCase =
              SeqUtil.createCaseFromThreadNode(
                  thread, coveredNodes, threadNode, edgeSubs, funcVars, pPthreadVars);
          if (loopCase != null) {
            loopCases.add(loopCase);
          }
        }
      }
      rLoopCases.put(thread, loopCases.build());
    }
    return rLoopCases.buildOrThrow();
  }

  /** Prunes empty loop cases i.e. loop cases without any statements other than pc adjustments. */
  private static ImmutableMap<MPORThread, ImmutableList<SeqLoopCase>> pruneLoopCases(
      ImmutableMap<MPORThread, ImmutableList<SeqLoopCase>> pLoopCases) {

    ImmutableMap.Builder<MPORThread, ImmutableList<SeqLoopCase>> rPrunedCases =
        ImmutableMap.builder();

    for (var entry : pLoopCases.entrySet()) {
      ImmutableList<SeqLoopCase> loopCases = entry.getValue();
      ImmutableMap<Integer, SeqLoopCase> originPc = mapOriginPcToCases(loopCases);
      ImmutableList.Builder<SeqLoopCase> prunedCases = ImmutableList.builder();

      Set<SeqLoopCase> skippedCases = new HashSet<>();
      Set<Long> newCaseIds = new HashSet<>();

      for (SeqLoopCase loopCase : loopCases) {
        if (!skippedCases.contains(loopCase)) {
          if (loopCase.allStatementsEmpty()) {
            SeqLoopCase prunedCase = handleCasePrune(originPc, loopCase, skippedCases, loopCase);
            prunedCases.add(prunedCase);
            newCaseIds.add(prunedCase.id);
          } else if (!newCaseIds.contains(loopCase.id)) {
            prunedCases.add(loopCase);
            newCaseIds.add(loopCase.id);
          }
        }
      }
      rPrunedCases.put(entry.getKey(), prunedCases.build());
    }
    return rPrunedCases.buildOrThrow();
  }

  private static ImmutableMap<Integer, SeqLoopCase> mapOriginPcToCases(
      ImmutableList<SeqLoopCase> pCases) {
    ImmutableMap.Builder<Integer, SeqLoopCase> rOriginPcs = ImmutableMap.builder();
    for (SeqLoopCase loopCase : pCases) {
      rOriginPcs.put(loopCase.originPc, loopCase);
    }
    return rOriginPcs.buildOrThrow();
  }

  private static SeqLoopCase handleCasePrune(
      final ImmutableMap<Integer, SeqLoopCase> pOriginPc,
      final SeqLoopCase pInitCase,
      Set<SeqLoopCase> pSkipped,
      SeqLoopCase pCurrentCase) {

    for (SeqLoopCaseStmt caseStmt : pCurrentCase.statements) {
      // if all statements in pCurrentCase are empty -> check if it should be pruned
      if (pCurrentCase.allStatementsEmpty()) {
        pSkipped.add(pCurrentCase);
        SeqLoopCase nextCase = pOriginPc.get(caseStmt.targetPc.orElseThrow());
        // TODO nextCase null -> targetPc is EXIT_PC, needs to be handled separately
        if (nextCase == null) {
          return pInitCase;
        }
        // do not visit exit nodes of the threads cfa
        if (!nextCase.statements.isEmpty()) {
          return handleCasePrune(pOriginPc, pInitCase, pSkipped, nextCase);
        }
      }
      // otherwise break recursion -> non-empty case found
      return pCurrentCase.cloneWithOriginPc(pInitCase.originPc);
    }
    throw new IllegalArgumentException("pCurrentCase statements are empty");
  }

  /**
   * Maps {@link ThreadEdge}s whose {@link CFAEdge}s are {@link CFunctionCallEdge}s to a list of
   * {@link CExpressionAssignmentStatement}s.
   *
   * <p>E.g. {@code func(&paramA, paramB);} in thread 0 is linked to {@code __t0_0_paramA = &paramA
   * ;} and {@code __t0_1_paramB = paramB ;}. Both substitution vars are declared in {@link
   * CSimpleDeclarationSubstitution#paramSubs}.
   */
  private static ImmutableMap<ThreadEdge, ImmutableList<CExpressionAssignmentStatement>>
      mapParamAssigns(
          MPORThread pThread,
          ImmutableMap<ThreadEdge, CFAEdge> pEdgeSubs,
          CSimpleDeclarationSubstitution pSub) {

    ImmutableMap.Builder<ThreadEdge, ImmutableList<CExpressionAssignmentStatement>> rAssigns =
        ImmutableMap.builder();

    for (ThreadEdge threadEdge : pThread.cfa.threadEdges) {
      CFAEdge subEdge = pEdgeSubs.get(threadEdge);
      if (subEdge instanceof CFunctionCallEdge funcCall) {

        ImmutableList.Builder<CExpressionAssignmentStatement> assigns = ImmutableList.builder();
        List<CParameterDeclaration> paramDecs =
            funcCall.getSuccessor().getFunctionDefinition().getParameters();

        // for each parameter, assign the param substitute to the param expression in funcCall
        for (int i = 0; i < paramDecs.size(); i++) {
          CParameterDeclaration paramDec = paramDecs.get(i);
          assert pSub.paramSubs != null;
          CExpression paramExpr =
              funcCall.getFunctionCallExpression().getParameterExpressions().get(i);
          CIdExpression sub = pSub.paramSubs.get(paramDec);
          assert sub != null;
          CExpressionAssignmentStatement assign =
              SeqExpressions.buildExprAssignStmt(sub, paramExpr);
          assigns.add(assign);
        }
        rAssigns.put(threadEdge, assigns.build());
      }
    }
    return rAssigns.buildOrThrow();
  }

  /**
   * Maps {@link ThreadEdge}s whose {@link CFAEdge}s are {@link CReturnStatementEdge}s to {@link
   * CExpressionAssignmentStatement} where the CPAchecker_TMP vars are assigned the return value.
   *
   * <p>Note that {@code main} functions and start routines of threads oftentimes do not have
   * corresponding {@link CFunctionSummaryEdge}s.
   */
  private static ImmutableMap<ThreadEdge, ImmutableSet<CExpressionAssignmentStatement>>
      mapReturnStmts(MPORThread pThread, ImmutableMap<ThreadEdge, CFAEdge> pEdgeSubs) {

    ImmutableMap.Builder<ThreadEdge, ImmutableSet<CExpressionAssignmentStatement>> rRetStmts =
        ImmutableMap.builder();
    for (ThreadEdge aThreadEdge : pThread.cfa.threadEdges) {
      CFAEdge aSub = pEdgeSubs.get(aThreadEdge);

      if (aSub instanceof CReturnStatementEdge retStmt) {
        AFunctionType aFunc = retStmt.getSuccessor().getFunction().getType();
        ImmutableSet.Builder<CExpressionAssignmentStatement> assigns = ImmutableSet.builder();
        for (ThreadEdge bThreadEdge : pThread.cfa.threadEdges) {
          CFAEdge bSub = pEdgeSubs.get(bThreadEdge);

          if (bSub instanceof CFunctionSummaryEdge funcSumm) {
            // if the summary edge is of the form CPAchecker_TMP = func(); (i.e. an assignment)
            if (funcSumm.getExpression() instanceof CFunctionCallAssignmentStatement assignStmt) {
              AFunctionType bFunc = funcSumm.getFunctionEntry().getFunction().getType();
              if (aFunc.equals(bFunc)) {
                assigns.add(
                    SeqExpressions.buildExprAssignStmt(
                        assignStmt.getLeftHandSide(), retStmt.getExpression().orElseThrow()));
              }
            }
          }
        }
        rRetStmts.put(aThreadEdge, assigns.build());
      }
    }
    return rRetStmts.buildOrThrow();
  }

  /**
   * Maps {@link ThreadEdge}s whose {@link CFAEdge}s are {@link CFunctionSummaryEdge}s to {@code
   * return_pc} {@link CIdExpression}s.
   *
   * <p>E.g. a {@link CFunctionSummaryEdge} for the function {@code fib} in thread 0 is mapped to
   * the expression of {@code int __return_pc_t0_fib}.
   */
  private static ImmutableMap<MPORThread, ImmutableMap<ThreadEdge, CIdExpression>> mapReturnPcVars(
      ImmutableSet<MPORThread> pThreads) {

    ImmutableMap.Builder<MPORThread, ImmutableMap<ThreadEdge, CIdExpression>> rDecs =
        ImmutableMap.builder();
    for (MPORThread thread : pThreads) {
      ImmutableMap.Builder<ThreadEdge, CIdExpression> returnPc = ImmutableMap.builder();
      for (ThreadEdge threadEdge : thread.cfa.threadEdges) {
        if (threadEdge.cfaEdge instanceof CFunctionSummaryEdge funcSummary) {
          String funcName = funcSummary.getFunctionEntry().getFunctionName();
          CVariableDeclaration varDec =
              SeqVariableDeclaration.buildReturnPcVarDec(thread.id, funcName);
          returnPc.put(threadEdge, SeqIdExpression.buildIdExpr(varDec));
        }
      }
      rDecs.put(thread, returnPc.buildOrThrow());
    }
    return rDecs.buildOrThrow();
  }

  /**
   * Maps {@link ThreadEdge}s whose {@link CFAEdge}s are {@link CFunctionSummaryEdge}s to {@code
   * return_pc} assignments.
   *
   * <p>E.g. a {@link CFunctionSummaryEdge} going from pc 5 to 10 for the function {@code fib} in
   * thread 0 is mapped to the assignment {@code __return_pc_t0_fib = 10;}.
   */
  private static ImmutableMap<ThreadEdge, CExpressionAssignmentStatement> mapReturnPcToPcAssigns(
      MPORThread pThread, ImmutableMap<ThreadEdge, CIdExpression> pReturnPcVars) {

    ImmutableMap.Builder<ThreadEdge, CExpressionAssignmentStatement> rAssigns =
        ImmutableMap.builder();
    for (ThreadEdge threadEdge : pThread.cfa.threadEdges) {
      if (threadEdge.cfaEdge instanceof CFunctionSummaryEdge) {
        CIdExpression idExpr = pReturnPcVars.get(threadEdge);
        assert idExpr != null;
        CIntegerLiteralExpression pc =
            SeqIntegerLiteralExpression.buildIntLiteralExpr(threadEdge.getSuccessor().pc);
        CExpressionAssignmentStatement assign = SeqExpressions.buildExprAssignStmt(idExpr, pc);
        rAssigns.put(threadEdge, assign);
      }
    }
    return rAssigns.buildOrThrow();
  }

  /**
   * Maps {@link ThreadNode}s whose {@link CFANode}s are {@link FunctionExitNode}s to {@code
   * return_pc} variable assignments.
   *
   * <p>E.g. a {@link FunctionExitNode} for the function {@code fib} in thread 0 is mapped to the
   * assignment {@code pc[0] = __return_pc_t0_fib;}.
   */
  private static ImmutableMap<ThreadNode, CExpressionAssignmentStatement> mapPcToReturnPcAssigns(
      MPORThread pThread, ImmutableMap<ThreadEdge, CIdExpression> pReturnPcVars) {

    Map<ThreadNode, CExpressionAssignmentStatement> rAssigns = new HashMap<>();
    for (ThreadEdge threadEdge : pThread.cfa.threadEdges) {
      if (threadEdge.cfaEdge instanceof CFunctionSummaryEdge funcSummaryEdge) {
        Optional<FunctionExitNode> funcExitNode = funcSummaryEdge.getFunctionEntry().getExitNode();
        if (funcExitNode.isPresent()) {
          CIdExpression returnPc = pReturnPcVars.get(threadEdge);
          ThreadNode threadNode = pThread.cfa.getThreadNodeByCfaNode(funcExitNode.orElseThrow());
          if (!rAssigns.containsKey(threadNode)) {
            CIntegerLiteralExpression threadId =
                SeqIntegerLiteralExpression.buildIntLiteralExpr(pThread.id);
            CArraySubscriptExpression pcArray = SeqExpressions.buildPcSubscriptExpr(threadId);
            CExpressionAssignmentStatement assign =
                SeqExpressions.buildExprAssignStmt(pcArray, returnPc);
            rAssigns.put(threadNode, assign);
          }
        }
      }
    }
    return ImmutableMap.copyOf(rAssigns);
  }

  private static ImmutableMap<CIdExpression, CIdExpression> mapThreadActiveVars(
      ImmutableSet<MPORThread> pThreads) {

    ImmutableMap.Builder<CIdExpression, CIdExpression> rVars = ImmutableMap.builder();
    for (MPORThread thread : pThreads) {
      for (ThreadEdge threadEdge : thread.cfa.threadEdges) {
        CFAEdge cfaEdge = threadEdge.cfaEdge;
        if (PthreadFuncType.callsPthreadFunc(cfaEdge, PthreadFuncType.PTHREAD_CREATE)) {
          MPORThread createdThread = PthreadUtil.extractThread(pThreads, cfaEdge);
          String varName = SeqNameBuilder.createThreadActiveName(createdThread.id);
          CIdExpression pthreadT = PthreadUtil.extractPthreadT(cfaEdge);
          rVars.put(pthreadT, SeqIdExpression.buildIdExprInt(varName));
        }
      }
    }
    return rVars.buildOrThrow();
  }

  private static ImmutableMap<CIdExpression, CIdExpression> mapMutexLockedVars(
      ImmutableMap<MPORThread, CSimpleDeclarationSubstitution> pSubstitutions) {

    ImmutableMap.Builder<CIdExpression, CIdExpression> rVars = ImmutableMap.builder();
    for (var entry : pSubstitutions.entrySet()) {
      MPORThread thread = entry.getKey();
      ImmutableMap<ThreadEdge, CFAEdge> edgeSubs = entry.getValue().substituteEdges(thread);
      for (ThreadEdge threadEdge : thread.cfa.threadEdges) {
        assert edgeSubs.containsKey(threadEdge);
        CFAEdge sub = edgeSubs.get(threadEdge);
        // TODO mutexes can also be init with pthread_mutex_t m = PTHREAD_MUTEX_INITIALIZER;
        if (PthreadFuncType.callsPthreadFunc(sub, PthreadFuncType.PTHREAD_MUTEX_INIT)) {
          // TODO use CIdExpressions for pthreadMutexT too
          CIdExpression pthreadMutexT = PthreadUtil.extractPthreadMutexT(sub);
          String varName = SeqNameBuilder.createMutexLockedName(pthreadMutexT.getName());
          // TODO it should be wiser to use the original idExpr as the key and not the substitute...
          rVars.put(pthreadMutexT, SeqIdExpression.buildIdExprInt(varName));
        }
      }
    }
    // if the same mutex is init twice (i.e. undefined behavior), this throws an exception
    return rVars.buildOrThrow();
  }

  private static ImmutableMap<MPORThread, ImmutableMap<CIdExpression, CIdExpression>>
      mapMutexAwaitsVars(ImmutableMap<MPORThread, CSimpleDeclarationSubstitution> pSubstitutions) {

    ImmutableMap.Builder<MPORThread, ImmutableMap<CIdExpression, CIdExpression>> rVars =
        ImmutableMap.builder();
    for (var aEntry : pSubstitutions.entrySet()) {
      MPORThread thread = aEntry.getKey();
      ImmutableMap<ThreadEdge, CFAEdge> edgeSubs = aEntry.getValue().substituteEdges(thread);
      Map<CIdExpression, CIdExpression> awaitVars = new HashMap<>();
      for (ThreadEdge threadEdge : thread.cfa.threadEdges) {
        assert edgeSubs.containsKey(threadEdge);
        CFAEdge sub = edgeSubs.get(threadEdge);
        if (PthreadFuncType.callsPthreadFunc(sub, PthreadFuncType.PTHREAD_MUTEX_LOCK)) {
          CIdExpression pthreadMutexT = PthreadUtil.extractPthreadMutexT(sub);
          // multiple lock calls within one thread to the same mutex are possible -> only need one
          if (!awaitVars.containsKey(pthreadMutexT)) {
            String varName =
                SeqNameBuilder.createMutexLockedName(thread.id, pthreadMutexT.getName());
            awaitVars.put(pthreadMutexT, SeqIdExpression.buildIdExprInt(varName));
          }
        }
      }
      rVars.put(thread, ImmutableMap.copyOf(awaitVars));
    }
    return rVars.buildOrThrow();
  }

  private static ImmutableMap<MPORThread, ImmutableMap<MPORThread, CIdExpression>>
      mapThreadJoinsVars(ImmutableSet<MPORThread> pThreads) {

    ImmutableMap.Builder<MPORThread, ImmutableMap<MPORThread, CIdExpression>> rVars =
        ImmutableMap.builder();
    for (MPORThread thread : pThreads) {
      Map<MPORThread, CIdExpression> targetThreads = new HashMap<>();
      for (ThreadEdge threadEdge : thread.cfa.threadEdges) {
        CFAEdge cfaEdge = threadEdge.cfaEdge;
        if (PthreadFuncType.callsPthreadFunc(cfaEdge, PthreadFuncType.PTHREAD_JOIN)) {
          CIdExpression pthreadT = PthreadUtil.extractPthreadT(cfaEdge);
          MPORThread targetThread = PthreadUtil.extractThread(pThreads, cfaEdge);

          // multiple join calls within one thread to the same thread are possible -> only need one
          if (!targetThreads.containsKey(targetThread)) {
            String varName = SeqNameBuilder.createThreadJoinsName(thread.id, targetThread.id);
            targetThreads.put(targetThread, SeqIdExpression.buildIdExprInt(varName));
          }
        }
      }
      rVars.put(thread, ImmutableMap.copyOf(targetThreads));
    }
    return rVars.buildOrThrow();
  }

  // String Creators =============================================================================

  private String createNonVarDecString(MPORThread pThread) {
    StringBuilder rDecs = new StringBuilder();
    for (ThreadEdge threadEdge : pThread.cfa.threadEdges) {
      if (threadEdge.cfaEdge instanceof CDeclarationEdge decEdge) {
        CDeclaration dec = decEdge.getDeclaration();
        if (!(dec instanceof CVariableDeclaration)) {
          assert pThread.isMain(); // TODO testing if only the main thread declares non vars
          rDecs.append(threadEdge.cfaEdge.getCode()).append(SeqSyntax.NEWLINE);
        }
      }
    }
    return rDecs.toString();
  }

  private String createGlobalVarString(CSimpleDeclarationSubstitution pSubstitution) {
    StringBuilder rDecs = new StringBuilder();
    rDecs.append(SeqComment.createGlobalVarsComment());
    assert pSubstitution.globalVarSubs != null;
    for (CIdExpression idExpr : pSubstitution.globalVarSubs.values()) {
      rDecs.append(idExpr.getDeclaration().toASTString()).append(SeqSyntax.NEWLINE);
    }
    rDecs.append(SeqSyntax.NEWLINE);
    return rDecs.toString();
  }

  private String createLocalVarString(int pThreadId, CSimpleDeclarationSubstitution pSubstitution) {
    StringBuilder rDecs = new StringBuilder();
    rDecs.append(SeqComment.createLocalVarsComment(pThreadId));
    for (CIdExpression idExpr : pSubstitution.localVarSubs.values()) {
      CVariableDeclaration varDec = pSubstitution.castIdExprDec(idExpr.getDeclaration());
      // TODO handle const CPAchecker TMP vars
      if (!SeqUtil.isConstCPAcheckerTMP(varDec)) {
        rDecs.append(varDec.toASTString()).append(SeqSyntax.NEWLINE);
      }
    }
    rDecs.append(SeqSyntax.NEWLINE);
    return rDecs.toString();
  }

  private String createParamVarString(int pThreadId, CSimpleDeclarationSubstitution pSubstitution) {
    StringBuilder rDecs = new StringBuilder();
    rDecs.append(SeqComment.createParamVarsComment(pThreadId));
    assert pSubstitution.paramSubs != null;
    for (CIdExpression idExpr : pSubstitution.paramSubs.values()) {
      rDecs.append(idExpr.getDeclaration().toASTString()).append(SeqSyntax.NEWLINE);
    }
    rDecs.append(SeqSyntax.NEWLINE);
    return rDecs.toString();
  }

  // Helpers for better Overview =================================================================

  private PthreadVars buildPthreadVars(
      ImmutableMap<MPORThread, CSimpleDeclarationSubstitution> pSubstitutions) {
    return new PthreadVars(
        mapThreadActiveVars(pSubstitutions.keySet()),
        mapMutexLockedVars(pSubstitutions),
        mapMutexAwaitsVars(pSubstitutions),
        mapThreadJoinsVars(pSubstitutions.keySet()));
  }
}
