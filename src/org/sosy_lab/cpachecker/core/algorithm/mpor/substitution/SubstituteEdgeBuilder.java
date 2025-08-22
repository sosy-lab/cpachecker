// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.substitution;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadUtil;

public class SubstituteEdgeBuilder {

  public static ImmutableMap<ThreadEdge, SubstituteEdge> substituteEdges(
      MPOROptions pOptions, ImmutableList<MPORSubstitution> pSubstitutions) {

    // using map so that we can use .containsKey (+ linked hash map retains insertion order)
    Map<ThreadEdge, SubstituteEdge> rSubstituteEdges = new LinkedHashMap<>();
    for (MPORSubstitution substitution : pSubstitutions) {
      MPORThread thread = substitution.thread;

      for (ThreadEdge threadEdge : thread.cfa.threadEdges) {
        // prevent duplicate keys by excluding parallel edges
        if (!rSubstituteEdges.containsKey(threadEdge)) {
          CFAEdge cfaEdge = threadEdge.cfaEdge;
          Optional<SubstituteEdge> substitute =
              trySubstituteEdge(pOptions, substitution, threadEdge);
          // if edge is not substituted: just use original edge
          rSubstituteEdges.put(
              threadEdge,
              substitute.isPresent()
                  ? substitute.orElseThrow()
                  : SubstituteEdge.of(cfaEdge, threadEdge));
        }
      }
    }
    // copying here retains insertion order of linked hash map
    return ImmutableMap.copyOf(rSubstituteEdges);
  }

  // TODO create separate method for each edge type for better overview
  /**
   * Tries to substitute the given {@link ThreadEdge}. Not all edges are substituted, e.g. function
   * declarations from the input program are included if specified by {@link MPOROptions}.
   */
  private static Optional<SubstituteEdge> trySubstituteEdge(
      MPOROptions pOptions, MPORSubstitution pSubstitution, ThreadEdge pThreadEdge) {

    CFAEdge cfaEdge = pThreadEdge.cfaEdge;
    Optional<ThreadEdge> callContext =
        ThreadUtil.getCallContextOrStartRoutineCall(pThreadEdge.callContext, pSubstitution.thread);

    if (cfaEdge instanceof CDeclarationEdge declarationEdge) {
      // TODO what about structs?
      if (SubstituteUtil.isExcludedDeclarationEdge(pOptions, declarationEdge)) {
        return Optional.empty();
      } else {
        CDeclaration declaration = declarationEdge.getDeclaration();
        // we only substitute variables, not functions or types
        if (declaration instanceof CVariableDeclaration variableDeclaration) {
          MPORSubstitutionTracker tracker = MPORSubstitutionTracker.mutableInstance();
          CVariableDeclaration declarationSubstitute =
              pSubstitution.getVariableDeclarationSubstitute(
                  variableDeclaration, callContext, Optional.of(tracker));
          CDeclarationEdge substituteDeclarationEdge =
              substituteDeclarationEdge(declarationEdge, declarationSubstitute);
          return Optional.of(SubstituteEdge.of(substituteDeclarationEdge, pThreadEdge, tracker));
        }
      }

    } else if (cfaEdge instanceof CAssumeEdge assume) {
      MPORSubstitutionTracker tracker = MPORSubstitutionTracker.mutableInstance();
      CExpression substituteAssumption =
          pSubstitution.substitute(
              assume.getExpression(),
              callContext,
              false,
              false,
              false,
              false,
              Optional.of(tracker));
      CAssumeEdge substituteAssumeEdge = substituteAssumeEdge(assume, substituteAssumption);
      return Optional.of(SubstituteEdge.of(substituteAssumeEdge, pThreadEdge, tracker));

    } else if (cfaEdge instanceof CStatementEdge statement) {
      MPORSubstitutionTracker tracker = MPORSubstitutionTracker.mutableInstance();
      CStatement substituteStatement =
          pSubstitution.substitute(statement.getStatement(), callContext, Optional.of(tracker));
      CStatementEdge substituteStatementEdge =
          substituteStatementEdge(statement, substituteStatement);
      return Optional.of(SubstituteEdge.of(substituteStatementEdge, pThreadEdge, tracker));

    } else if (cfaEdge instanceof CFunctionSummaryEdge functionSummary) {
      // only substitute assignments (e.g. CPAchecker_TMP = func();)
      if (functionSummary.getExpression() instanceof CFunctionCallAssignmentStatement assignment) {
        MPORSubstitutionTracker tracker = MPORSubstitutionTracker.mutableInstance();
        CStatement substituteAssignment =
            pSubstitution.substitute(assignment, callContext, Optional.of(tracker));
        CFunctionSummaryEdge substituteFunctionSummaryEdge =
            substituteFunctionSummaryEdge(functionSummary, substituteAssignment);
        return Optional.of(SubstituteEdge.of(substituteFunctionSummaryEdge, pThreadEdge, tracker));
      }

    } else if (cfaEdge instanceof CFunctionCallEdge functionCall) {
      // CFunctionCallEdges also assign CPAchecker_TMPs -> handle assignment statements here too
      MPORSubstitutionTracker tracker = MPORSubstitutionTracker.mutableInstance();
      CStatement substituteFunctionCall =
          pSubstitution.substitute(
              functionCall.getFunctionCall(), callContext, Optional.of(tracker));
      assert substituteFunctionCall instanceof CFunctionCall
          : "substitute function call must be CFunctionCall";
      CFunctionCallEdge substituteFunctionCallEdge =
          substituteFunctionCallEdge(functionCall, (CFunctionCall) substituteFunctionCall);
      return Optional.of(SubstituteEdge.of(substituteFunctionCallEdge, pThreadEdge, tracker));

    } else if (cfaEdge instanceof CReturnStatementEdge returnStatement) {
      MPORSubstitutionTracker tracker = MPORSubstitutionTracker.mutableInstance();
      CReturnStatement substituteReturnStatement =
          pSubstitution.substitute(
              returnStatement.getReturnStatement(), callContext, Optional.of(tracker));
      CReturnStatementEdge substituteReturnStatementEdge =
          substituteReturnStatementEdge(returnStatement, substituteReturnStatement);
      return Optional.of(SubstituteEdge.of(substituteReturnStatementEdge, pThreadEdge, tracker));
    }
    return Optional.empty();
  }

  private static CAssumeEdge substituteAssumeEdge(CAssumeEdge pOriginal, CExpression pExpr) {
    return new CAssumeEdge(
        pOriginal.getRawStatement(),
        pOriginal.getFileLocation(),
        pOriginal.getPredecessor(),
        pOriginal.getSuccessor(),
        pExpr,
        pOriginal.getTruthAssumption());
  }

  private static CDeclarationEdge substituteDeclarationEdge(
      CDeclarationEdge pOriginal, CVariableDeclaration pVarDec) {

    return new CDeclarationEdge(
        pOriginal.getRawStatement(),
        pOriginal.getFileLocation(),
        pOriginal.getPredecessor(),
        pOriginal.getSuccessor(),
        pVarDec);
  }

  private static CStatementEdge substituteStatementEdge(
      CStatementEdge pOriginal, CStatement pStatement) {

    return new CStatementEdge(
        pOriginal.getRawStatement(),
        pStatement,
        pOriginal.getFileLocation(),
        pOriginal.getPredecessor(),
        pOriginal.getSuccessor());
  }

  private static CFunctionSummaryEdge substituteFunctionSummaryEdge(
      CFunctionSummaryEdge pOriginal, CStatement pFuncCall) {

    return new CFunctionSummaryEdge(
        pOriginal.getRawStatement(),
        pOriginal.getFileLocation(),
        pOriginal.getPredecessor(),
        pOriginal.getSuccessor(),
        (CFunctionCall) pFuncCall,
        pOriginal.getFunctionEntry());
  }

  private static CFunctionCallEdge substituteFunctionCallEdge(
      CFunctionCallEdge pOriginal, CFunctionCall pFuncCall) {

    return new CFunctionCallEdge(
        pOriginal.getRawStatement(),
        pOriginal.getFileLocation(),
        pOriginal.getPredecessor(),
        pOriginal.getSuccessor(),
        pFuncCall,
        pOriginal.getSummaryEdge());
  }

  private static CReturnStatementEdge substituteReturnStatementEdge(
      CReturnStatementEdge pOriginal, CReturnStatement pReturnStatement) {

    return new CReturnStatementEdge(
        pOriginal.getRawStatement(),
        pReturnStatement,
        pOriginal.getFileLocation(),
        pOriginal.getPredecessor(),
        pOriginal.getSuccessor());
  }
}
