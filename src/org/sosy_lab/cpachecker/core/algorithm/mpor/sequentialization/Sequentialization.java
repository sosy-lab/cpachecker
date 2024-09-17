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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.data_entity.Variable;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.ASTStringExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.AssignExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.DeclareExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.SeqExprBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.VariableExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqComment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqToken;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.CSimpleDeclarationSubstitution;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadNode;

@SuppressWarnings("unused")
@SuppressFBWarnings({"UUF_UNUSED_FIELD", "URF_UNREAD_FIELD"})
public class Sequentialization {

  // TODO create LineOfCode class with multiple constructors where we define tab amount, semicolon
  //  curly left / right brackets, newlines, etc.
  public static final int TAB_SIZE = 3;

  protected final int numThreads;

  public Sequentialization(int pNumThreads) {
    numThreads = pNumThreads;
  }

  /**
   * Maps {@link ThreadEdge}s whose {@link CFAEdge}s are {@link FunctionSummaryEdge}s to {@code
   * return_pc} declarations.
   *
   * <p>E.g. a {@link FunctionSummaryEdge} for the function {@code fib} in thread 0 is mapped to the
   * declaration {@code int t0_{fib}_return_pc;}.
   */
  private static ImmutableMap<ThreadEdge, DeclareExpr> mapReturnPcDecs(MPORThread pThread) {
    ImmutableMap.Builder<ThreadEdge, DeclareExpr> rDecs = ImmutableMap.builder();
    for (ThreadEdge threadEdge : pThread.cfa.threadEdges) {
      if (threadEdge.cfaEdge instanceof FunctionSummaryEdge funcSummary) {
        rDecs.put(
            threadEdge,
            SeqExprBuilder.createReturnPcDec(
                pThread.id, funcSummary.getFunctionEntry().getFunctionName()));
      }
    }
    return rDecs.buildOrThrow();
  }

  // TODO handle return_pc = funcSummaryEdge target pc (assignExpr)

  /**
   * Maps {@link ThreadNode}s whose {@link CFANode}s are {@link FunctionExitNode}s to {@code
   * return_pc} variable assignments.
   *
   * <p>E.g. a {@link FunctionExitNode} for the function {@code fib} in thread 0 is mapped to the
   * assignment {@code pcs[next_thread] = t0__fib__return_pc;}.
   */
  private static ImmutableMap<ThreadNode, AssignExpr> mapReturnPcAssigns(MPORThread pThread) {
    Map<ThreadNode, AssignExpr> rAssigns = new HashMap<>();

    ImmutableMap<ThreadEdge, DeclareExpr> returnPcDecs = mapReturnPcDecs(pThread);
    for (ThreadEdge threadEdge : pThread.cfa.threadEdges) {
      if (threadEdge.cfaEdge instanceof FunctionSummaryEdge funcSummaryEdge) {
        Optional<FunctionExitNode> funcExitNode = funcSummaryEdge.getFunctionEntry().getExitNode();
        if (funcExitNode.isPresent()) {
          DeclareExpr dec = returnPcDecs.get(threadEdge);
          ThreadNode threadNode = pThread.cfa.getThreadNodeByCfaNode(funcExitNode.orElseThrow());
          if (!rAssigns.containsKey(threadNode)) {
            assert dec != null;
            AssignExpr assign =
                new AssignExpr(SeqExprBuilder.pcsNextThread, dec.variableExpr.variable);
            rAssigns.put(threadNode, assign);
          }
        }
      }
    }
    return ImmutableMap.copyOf(rAssigns);
  }

  /**
   * Maps {@link ThreadEdge}s whose {@link CFAEdge}s are {@link FunctionCallEdge}s to a list of
   * parameter assignment expressions.
   *
   * <p>E.g. {@code func(&paramA, paramB);} in thread 0 is linked to {@code __t0_0_paramA = &paramA
   * ;} and {@code __t0_1_paramB = paramB ;}. Both substitution vars are declared in {@link
   * CSimpleDeclarationSubstitution#paramSubs}.
   */
  private static ImmutableMap<ThreadEdge, ImmutableList<AssignExpr>> mapParamAssigns(
      MPORThread pThread, CSimpleDeclarationSubstitution pSub) {

    ImmutableMap.Builder<ThreadEdge, ImmutableList<AssignExpr>> rAssigns = ImmutableMap.builder();

    for (ThreadEdge threadEdge : pThread.cfa.threadEdges) {
      if (threadEdge.cfaEdge instanceof CFunctionCallEdge funcCall) {

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

  /** Generates and returns the entire sequentialized program. */
  public String generateProgram(
      ImmutableMap<MPORThread, CSimpleDeclarationSubstitution> pSubstitutions) {

    StringBuilder rProgram = new StringBuilder();

    // prepend all function, complex type, typedef declarations (i.e. non var declarations)
    rProgram.append(SeqComment.createNonVarDeclarationComment());
    for (MPORThread thread : pSubstitutions.keySet()) {
      rProgram.append(createNonVarDecString(thread));
    }
    rProgram.append(SeqSyntax.NEWLINE);

    // prepend all var declarations in the order global - local - params - return_pcs
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
        rProgram.append(dec.createString());
      }
    }

    // create while loop with all switch cases
    for (var entry : pSubstitutions.entrySet()) {
      MPORThread thread = entry.getKey();
      CSimpleDeclarationSubstitution substitution = entry.getValue();
      ImmutableMap<ThreadEdge, CFAEdge> edgeSubs = substitution.substituteEdges(thread);
      ImmutableMap<ThreadNode, AssignExpr> returnPcAssigns = mapReturnPcAssigns(thread);
      ImmutableMap<ThreadEdge, ImmutableList<AssignExpr>> paramAssigns =
          mapParamAssigns(thread, substitution);

      rProgram.append(SeqSyntax.NEWLINE);
      rProgram.append("=============== thread ").append(thread.id).append(" ===============");
      rProgram.append(SeqSyntax.NEWLINE).append(SeqSyntax.NEWLINE);
      for (ThreadNode threadNode : thread.cfa.threadNodes) {
        rProgram.append(SeqToken.CASE).append(SeqSyntax.SPACE);
        rProgram.append(threadNode.pc).append(SeqSyntax.COLON).append(SeqSyntax.SPACE);
        rProgram.append(
            SeqUtil.createCodeFromThreadNode(threadNode, edgeSubs, paramAssigns, returnPcAssigns));
        rProgram.append(SeqSyntax.NEWLINE);
      }
    }

    return rProgram.toString();
  }

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
      rDecs.append(expr.createString()).append(SeqSyntax.NEWLINE);
    }
    rDecs.append(SeqSyntax.NEWLINE);
    return rDecs.toString();
  }

  private String createLocalVarString(int pThreadId, CSimpleDeclarationSubstitution pSubstitution) {
    StringBuilder rDecs = new StringBuilder();
    rDecs.append(SeqComment.createLocalVarsComment(pThreadId));
    for (ASTStringExpr expr : SeqUtil.createLocalDeclarations(pSubstitution)) {
      rDecs.append(expr.createString()).append(SeqSyntax.NEWLINE);
    }
    rDecs.append(SeqSyntax.NEWLINE);
    return rDecs.toString();
  }

  private String createParamVarString(int pThreadId, CSimpleDeclarationSubstitution pSubstitution) {
    StringBuilder rDecs = new StringBuilder();
    rDecs.append(SeqComment.createParamVarsComment(pThreadId));
    for (ASTStringExpr expr : SeqUtil.createParamDeclarations(pSubstitution)) {
      rDecs.append(expr.createString()).append(SeqSyntax.NEWLINE);
    }
    rDecs.append(SeqSyntax.NEWLINE);
    return rDecs.toString();
  }
}
