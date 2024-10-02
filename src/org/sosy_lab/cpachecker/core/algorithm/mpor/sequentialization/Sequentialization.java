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
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
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
import org.sosy_lab.cpachecker.core.algorithm.mpor.PthreadFuncType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.data_entity.Value;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.data_entity.Variable;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.ASTStringExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.AssignExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.DeclareExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.SeqExprBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.VariableExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.function.AnyUnsigned;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.function.Assume;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.function.MainMethod;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.function.VerifierNonDetInt;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.loop_case.SeqLoopCase;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.loop_case.SeqLoopCaseStmt;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.loop_case.statements.SeqExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqComment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.CSimpleDeclarationSubstitution;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadNode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadUtil;
import org.sosy_lab.cpachecker.util.CFAUtils;

@SuppressWarnings("unused")
@SuppressFBWarnings({"UUF_UNUSED_FIELD", "URF_UNREAD_FIELD"})
public class Sequentialization {

  // TODO create LineOfCode class with multiple constructors where we define tab amount, semicolon
  //  curly left / right brackets, newlines, etc.
  public static final int TAB_SIZE = 2;

  protected final int numThreads;

  public Sequentialization(int pNumThreads) {
    numThreads = pNumThreads;
  }

  /** Generates and returns the entire sequentialized program. */
  public String generateProgram(
      ImmutableMap<MPORThread, CSimpleDeclarationSubstitution> pSubstitutions) {

    StringBuilder rProgram = new StringBuilder();

    // prepend all original program declarations that are not substituted
    rProgram.append(SeqComment.createNonVarDeclarationComment());
    for (MPORThread thread : pSubstitutions.keySet()) {
      rProgram.append(createNonVarDecString(thread));
    }
    rProgram.append(SeqSyntax.NEWLINE);

    // prepend all var substitute declarations in the order global - local - params - return_pc
    MPORThread mainThread = MPORAlgorithm.getMainThread(pSubstitutions.keySet());
    rProgram.append(createGlobalVarString(pSubstitutions.get(mainThread)));
    for (var entry : pSubstitutions.entrySet()) {
      rProgram.append(createLocalVarString(entry.getKey().id, entry.getValue()));
    }
    for (var entry : pSubstitutions.entrySet()) {
      rProgram.append(createParamVarString(entry.getKey().id, entry.getValue()));
    }
    rProgram.append(SeqComment.createReturnPcVarsComment());
    for (MPORThread thread : pSubstitutions.keySet()) {
      for (DeclareExpr dec : mapReturnPcDecs(thread).values()) {
        rProgram.append(dec.toString()).append(SeqSyntax.NEWLINE);
      }
    }
    rProgram.append(SeqSyntax.NEWLINE);

    // prepend pthread control vars
    rProgram.append(SeqComment.createPthreadReplacementVarsComment());
    // TODO the functions are called here separately -> separate maps
    //  make sure to use the same maps
    // thread active vars
    for (CIdExpression idExpr : mapThreadActiveVars(pSubstitutions.keySet()).values()) {
      assert idExpr.getDeclaration() instanceof CVariableDeclaration;
      CVariableDeclaration varDec = (CVariableDeclaration) idExpr.getDeclaration();
      rProgram.append(varDec.toASTString()).append(SeqSyntax.NEWLINE);
    }
    // mutex locked vars
    for (CIdExpression idExpr : mapMutexLockedVars(pSubstitutions).values()) {
      assert idExpr.getDeclaration() instanceof CVariableDeclaration;
      CVariableDeclaration varDec = (CVariableDeclaration) idExpr.getDeclaration();
      rProgram.append(varDec.toASTString()).append(SeqSyntax.NEWLINE);
    }
    rProgram.append(SeqSyntax.NEWLINE);

    // prepend all custom function declarations
    VerifierNonDetInt verifierNonDetInt = new VerifierNonDetInt();
    Assume assume = new Assume();
    AnyUnsigned anyUnsigned = new AnyUnsigned();
    rProgram.append(SeqComment.createFuncDeclarationComment());
    // this declaration may be duplicate depending on the input program, but that's fine in C
    rProgram.append(verifierNonDetInt.getDeclaration()).append(SeqSyntax.NEWLINE);
    rProgram.append(assume.getDeclaration()).append(SeqSyntax.NEWLINE);
    rProgram.append(anyUnsigned.getDeclaration()).append(SeqSyntax.NEWLINE);
    rProgram.append(SeqSyntax.NEWLINE);

    // prepend non main() methods
    rProgram.append(assume).append(SeqUtil.repeat(SeqSyntax.NEWLINE, 2));
    rProgram.append(anyUnsigned).append(SeqUtil.repeat(SeqSyntax.NEWLINE, 2));

    // TODO prune empty loop cases
    ImmutableMap<MPORThread, ImmutableList<SeqLoopCase>> loopCases = mapLoopCases(pSubstitutions);
    MainMethod mainMethod = new MainMethod(loopCases);
    rProgram.append(mainMethod);

    /*rProgram.append("===== pruned main thread =====");
    for (SeqLoopCase loopCase : pruneLoopCases(loopCases.get(mainThread))) {
      rProgram.append(loopCase.createString());
    }*/

    return rProgram.toString();
  }

  /** Maps threads to their SeqLoopCases. */
  private static ImmutableMap<MPORThread, ImmutableList<SeqLoopCase>> mapLoopCases(
      ImmutableMap<MPORThread, CSimpleDeclarationSubstitution> pSubstitutions) {
    ImmutableMap.Builder<MPORThread, ImmutableList<SeqLoopCase>> rLoopCases =
        ImmutableMap.builder();
    for (var entry : pSubstitutions.entrySet()) {
      ImmutableList.Builder<SeqLoopCase> loopCases = ImmutableList.builder();
      Set<ThreadNode> coveredNodes = new HashSet<>();

      MPORThread thread = entry.getKey();
      CSimpleDeclarationSubstitution substitution = entry.getValue();

      // TODO with so many vars used as params, its best to create a separate class for better
      //  overview
      ImmutableMap<ThreadEdge, CFAEdge> edgeSubs = substitution.substituteEdges(thread);
      ImmutableMap<ThreadEdge, ImmutableList<AssignExpr>> paramAssigns =
          mapParamAssigns(thread, edgeSubs, substitution);
      ImmutableMap<ThreadEdge, ImmutableSet<AssignExpr>> returnStmts =
          mapReturnStmts(thread, edgeSubs);
      ImmutableMap<ThreadEdge, AssignExpr> returnPcAssigns = mapReturnPcAssigns(thread);
      ImmutableMap<ThreadNode, AssignExpr> pcToReturnPcAssigns = mapPcToReturnPcAssigns(thread);
      // TODO we could (and should) map C(Id)Expressions i.e. threadObjects to CIdExpressions here
      ImmutableMap<MPORThread, CIdExpression> threadActiveVars =
          mapThreadActiveVars(pSubstitutions.keySet());
      ImmutableMap<CIdExpression, CIdExpression> mutexLockedVars =
          mapMutexLockedVars(pSubstitutions);

      for (ThreadNode threadNode : thread.cfa.threadNodes) {
        if (!coveredNodes.contains(threadNode)) {
          SeqLoopCase loopCase =
              SeqUtil.createCaseFromThreadNode(
                  thread,
                  coveredNodes,
                  threadNode,
                  edgeSubs,
                  paramAssigns,
                  returnStmts,
                  returnPcAssigns,
                  pcToReturnPcAssigns,
                  threadActiveVars,
                  mutexLockedVars);
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
  private static ImmutableList<SeqLoopCase> pruneLoopCases(ImmutableList<SeqLoopCase> pCases) {
    ImmutableMap<Integer, SeqLoopCase> originPcs = mapOriginPcToCases(pCases);

    Set<SeqLoopCase> skippedCases = new HashSet<>();
    ImmutableList.Builder<SeqLoopCase> rNewCases = ImmutableList.builder();
    for (SeqLoopCase loopCase : pCases) {
      if (!skippedCases.contains(loopCase)) {
        for (SeqLoopCaseStmt caseStmt : loopCase.statements) {
          if (caseStmt.statement.isEmpty()) {
            rNewCases.add(handleCasePrune(originPcs, loopCase, skippedCases, loopCase));
          }
        }
      }
    }
    return rNewCases.build();
  }

  private static ImmutableMap<Integer, SeqLoopCase> mapOriginPcToCases(
      ImmutableList<SeqLoopCase> pCases) {
    ImmutableMap.Builder<Integer, SeqLoopCase> rOriginPcs = ImmutableMap.builder();
    for (SeqLoopCase loopCase : pCases) {
      rOriginPcs.put(loopCase.originPc, loopCase);
    }
    return rOriginPcs.buildOrThrow();
  }

  // TODO refactor this
  private static SeqLoopCase handleCasePrune(
      final ImmutableMap<Integer, SeqLoopCase> pOriginPcs,
      final SeqLoopCase pInitCase,
      Set<SeqLoopCase> pSkipped,
      SeqLoopCase pCurrentCase) {

    assert !pCurrentCase.statements.isEmpty();

    for (SeqLoopCaseStmt caseStmt : pCurrentCase.statements) {
      if (caseStmt.statement.isEmpty()) {
        assert pCurrentCase.statements.size() == 1;
        pSkipped.add(pCurrentCase);
        SeqLoopCase nextCase = pOriginPcs.get(caseStmt.targetPc.orElseThrow());
        assert nextCase != null;
        // do not visit exit nodes of the threads cfa
        if (!nextCase.statements.isEmpty()) {
          handleCasePrune(pOriginPcs, pInitCase, pSkipped, nextCase);
        }
      }
      return pInitCase.cloneWithTargetPc(pCurrentCase.originPc);
    }
    throw new IllegalStateException();
  }

  /**
   * Maps {@link ThreadEdge}s whose {@link CFAEdge}s are {@link CFunctionCallEdge}s to a list of
   * parameter assignment expressions.
   *
   * <p>E.g. {@code func(&paramA, paramB);} in thread 0 is linked to {@code __t0_0_paramA = &paramA
   * ;} and {@code __t0_1_paramB = paramB ;}. Both substitution vars are declared in {@link
   * CSimpleDeclarationSubstitution#paramSubs}.
   */
  private static ImmutableMap<ThreadEdge, ImmutableList<AssignExpr>> mapParamAssigns(
      MPORThread pThread,
      ImmutableMap<ThreadEdge, CFAEdge> pEdgeSubs,
      CSimpleDeclarationSubstitution pSub) {

    ImmutableMap.Builder<ThreadEdge, ImmutableList<AssignExpr>> rAssigns = ImmutableMap.builder();

    for (ThreadEdge threadEdge : pThread.cfa.threadEdges) {
      CFAEdge subEdge = pEdgeSubs.get(threadEdge);
      if (subEdge instanceof CFunctionCallEdge funcCall) {

        ImmutableList.Builder<AssignExpr> assigns = ImmutableList.builder();
        List<CParameterDeclaration> paramDecs =
            funcCall.getSuccessor().getFunctionDefinition().getParameters();

        // for each parameter, assign the param substitute to the param expression in funcCall
        for (int i = 0; i < paramDecs.size(); i++) {
          CParameterDeclaration paramDec = paramDecs.get(i);
          assert pSub.paramSubs != null;
          CExpression paramExpr =
              funcCall.getFunctionCallExpression().getParameterExpressions().get(i);
          CVariableDeclaration sub = pSub.paramSubs.get(paramDec);
          assert sub != null;
          AssignExpr assign =
              new AssignExpr(
                  new VariableExpr(Optional.empty(), new Variable(sub.getName())),
                  new ASTStringExpr(paramExpr.toASTString()));
          assigns.add(assign);
        }
        rAssigns.put(threadEdge, assigns.build());
      }
    }
    return rAssigns.buildOrThrow();
  }

  /**
   * Maps {@link ThreadEdge}s whose {@link CFAEdge}s are {@link CReturnStatementEdge}s to {@link
   * AssignExpr} where the CPAchecker_TMP vars are assigned the return value.
   *
   * <p>Note that {@code main} functions and start routines of threads oftentimes do not have
   * corresponding {@link CFunctionSummaryEdge}s.
   */
  private static ImmutableMap<ThreadEdge, ImmutableSet<AssignExpr>> mapReturnStmts(
      MPORThread pThread, ImmutableMap<ThreadEdge, CFAEdge> pEdgeSubs) {

    ImmutableMap.Builder<ThreadEdge, ImmutableSet<AssignExpr>> rRetStmts = ImmutableMap.builder();
    for (ThreadEdge aThreadEdge : pThread.cfa.threadEdges) {
      CFAEdge aSub = pEdgeSubs.get(aThreadEdge);
      if (aSub instanceof CReturnStatementEdge retStmt) {
        AFunctionType aFunc = retStmt.getSuccessor().getFunction().getType();
        ImmutableSet.Builder<AssignExpr> assigns = ImmutableSet.builder();
        for (ThreadEdge bThreadEdge : pThread.cfa.threadEdges) {
          CFAEdge bSub = pEdgeSubs.get(bThreadEdge);
          if (bSub instanceof CFunctionSummaryEdge funcSumm) {
            // if the summary edge is of the form CPAchecker_TMP = func(); (i.e. an assignment)
            if (funcSumm.getExpression() instanceof CFunctionCallAssignmentStatement assignStmt) {
              AFunctionType bFunc = funcSumm.getFunctionEntry().getFunction().getType();
              if (aFunc.equals(bFunc)) {
                assigns.add(
                    new AssignExpr(
                        new Variable(assignStmt.getLeftHandSide().toASTString()),
                        new Variable(retStmt.getExpression().orElseThrow().toASTString())));
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
   * return_pc} declarations.
   *
   * <p>E.g. a {@link CFunctionSummaryEdge} for the function {@code fib} in thread 0 is mapped to
   * the declaration {@code int t0_fib_return_pc;}.
   */
  private static ImmutableMap<ThreadEdge, DeclareExpr> mapReturnPcDecs(MPORThread pThread) {
    ImmutableMap.Builder<ThreadEdge, DeclareExpr> rDecs = ImmutableMap.builder();
    for (ThreadEdge threadEdge : pThread.cfa.threadEdges) {
      if (threadEdge.cfaEdge instanceof CFunctionSummaryEdge funcSummary) {
        rDecs.put(
            threadEdge,
            SeqExprBuilder.createReturnPcDec(
                pThread.id, funcSummary.getFunctionEntry().getFunctionName()));
      }
    }
    return rDecs.buildOrThrow();
  }

  /**
   * Maps {@link ThreadEdge}s whose {@link CFAEdge}s are {@link CFunctionSummaryEdge}s to {@code
   * return_pc} assignments.
   *
   * <p>E.g. a {@link CFunctionSummaryEdge} going from pc 5 to 10 for the function {@code fib} in
   * thread 0 is mapped to the assignment {@code t0_fib_return_pc = 10;}.
   */
  private static ImmutableMap<ThreadEdge, AssignExpr> mapReturnPcAssigns(MPORThread pThread) {
    ImmutableMap.Builder<ThreadEdge, AssignExpr> rAssigns = ImmutableMap.builder();

    ImmutableMap<ThreadEdge, DeclareExpr> returnPcDecs = mapReturnPcDecs(pThread);
    for (ThreadEdge threadEdge : pThread.cfa.threadEdges) {
      if (threadEdge.cfaEdge instanceof CFunctionSummaryEdge) {
        DeclareExpr dec = returnPcDecs.get(threadEdge);
        assert dec != null;
        AssignExpr assign =
            new AssignExpr(
                dec.variableExpr.variable,
                new Value(Integer.toString(threadEdge.getSuccessor().pc)));
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
   * assignment {@code pc[next_thread] = t0__fib__return_pc;}.
   */
  private static ImmutableMap<ThreadNode, AssignExpr> mapPcToReturnPcAssigns(MPORThread pThread) {
    Map<ThreadNode, AssignExpr> rAssigns = new HashMap<>();

    ImmutableMap<ThreadEdge, DeclareExpr> returnPcDecs = mapReturnPcDecs(pThread);
    for (ThreadEdge threadEdge : pThread.cfa.threadEdges) {
      if (threadEdge.cfaEdge instanceof CFunctionSummaryEdge funcSummaryEdge) {
        Optional<FunctionExitNode> funcExitNode = funcSummaryEdge.getFunctionEntry().getExitNode();
        if (funcExitNode.isPresent()) {
          DeclareExpr dec = returnPcDecs.get(threadEdge);
          ThreadNode threadNode = pThread.cfa.getThreadNodeByCfaNode(funcExitNode.orElseThrow());
          if (!rAssigns.containsKey(threadNode)) {
            assert dec != null;
            AssignExpr assign =
                new AssignExpr(
                    SeqExprBuilder.createPcUpdate(pThread.id), dec.variableExpr.variable);
            rAssigns.put(threadNode, assign);
          }
        }
      }
    }
    return ImmutableMap.copyOf(rAssigns);
  }

  private static ImmutableMap<MPORThread, CIdExpression> mapThreadActiveVars(
      ImmutableSet<MPORThread> pThreads) {
    ImmutableMap.Builder<MPORThread, CIdExpression> rVars = ImmutableMap.builder();
    for (MPORThread thread : pThreads) {
      for (ThreadEdge threadEdge : thread.cfa.threadEdges) {
        CFAEdge cfaEdge = threadEdge.cfaEdge;
        if (PthreadFuncType.isCallToPthreadFunc(cfaEdge, PthreadFuncType.PTHREAD_CREATE)) {
          MPORThread createdThread = ThreadUtil.extractThreadFromPthreadCreate(pThreads, cfaEdge);
          String varName = SeqNameBuilder.createThreadActiveName(createdThread.id);
          rVars.put(createdThread, SeqExpressions.buildThreadActiveVar(varName));
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
        if (PthreadFuncType.isCallToPthreadFunc(sub, PthreadFuncType.PTHREAD_MUTEX_INIT)) {
          CExpression expr = CFAUtils.getValueFromPointer(CFAUtils.getParameterAtIndex(sub, 0));
          assert expr instanceof CIdExpression;
          CIdExpression idExpr = (CIdExpression) expr;
          String varName = SeqNameBuilder.createMutexLockedName(idExpr.getName());
          rVars.put(idExpr, SeqExpressions.buildMutexLockedVar(varName));
        }
      }
    }
    // if the same mutex is init twice (i.e. undefined behavior), this throws an exception
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
    for (ASTStringExpr expr : SeqUtil.createGlobalDeclarations(pSubstitution)) {
      rDecs.append(expr.toString()).append(SeqSyntax.NEWLINE);
    }
    rDecs.append(SeqSyntax.NEWLINE);
    return rDecs.toString();
  }

  private String createLocalVarString(int pThreadId, CSimpleDeclarationSubstitution pSubstitution) {
    StringBuilder rDecs = new StringBuilder();
    rDecs.append(SeqComment.createLocalVarsComment(pThreadId));
    for (ASTStringExpr expr : SeqUtil.createLocalDeclarations(pSubstitution)) {
      rDecs.append(expr.toString()).append(SeqSyntax.NEWLINE);
    }
    rDecs.append(SeqSyntax.NEWLINE);
    return rDecs.toString();
  }

  private String createParamVarString(int pThreadId, CSimpleDeclarationSubstitution pSubstitution) {
    StringBuilder rDecs = new StringBuilder();
    rDecs.append(SeqComment.createParamVarsComment(pThreadId));
    for (ASTStringExpr expr : SeqUtil.createParamDeclarations(pSubstitution)) {
      rDecs.append(expr.toString()).append(SeqSyntax.NEWLINE);
    }
    rDecs.append(SeqSyntax.NEWLINE);
    return rDecs.toString();
  }
}
