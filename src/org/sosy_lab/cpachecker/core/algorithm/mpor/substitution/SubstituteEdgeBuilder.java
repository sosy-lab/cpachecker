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
                  : new SubstituteEdge(cfaEdge, ImmutableList.of(), ImmutableList.of()));
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
        if (declaration instanceof CVariableDeclaration) {
          CVariableDeclaration variableDeclaration =
              pSubstitution.getVariableDeclarationSubstitute(declaration, callContext);
          // TODO which global variables to include for the set?
          return Optional.of(
              new SubstituteEdge(
                  substituteDeclarationEdge(declarationEdge, variableDeclaration),
                  ImmutableList.of(),
                  ImmutableList.of()));
        }
      }

    } else if (cfaEdge instanceof CAssumeEdge assume) {
      ImmutableList.Builder<CVariableDeclaration> writtenGlobalVariables = ImmutableList.builder();
      ImmutableList.Builder<CVariableDeclaration> globalVariables = ImmutableList.builder();
      CExpression substituteAssumption =
          pSubstitution.substitute(
              assume.getExpression(),
              callContext,
              false,
              Optional.of(writtenGlobalVariables),
              Optional.of(globalVariables));
      return Optional.of(
          new SubstituteEdge(
              substituteAssumeEdge(assume, substituteAssumption),
              writtenGlobalVariables.build(),
              globalVariables.build()));

    } else if (cfaEdge instanceof CStatementEdge statement) {
      ImmutableList.Builder<CVariableDeclaration> writtenGlobalVariables = ImmutableList.builder();
      ImmutableList.Builder<CVariableDeclaration> globalVariables = ImmutableList.builder();
      CStatement substituteStatement =
          pSubstitution.substitute(
              statement.getStatement(),
              callContext,
              Optional.of(writtenGlobalVariables),
              Optional.of(globalVariables));
      return Optional.of(
          new SubstituteEdge(
              substituteStatementEdge(statement, substituteStatement),
              writtenGlobalVariables.build(),
              globalVariables.build()));

    } else if (cfaEdge instanceof CFunctionSummaryEdge functionSummary) {
      // only substitute assignments (e.g. CPAchecker_TMP = func();)
      if (functionSummary.getExpression() instanceof CFunctionCallAssignmentStatement assignment) {
        ImmutableList.Builder<CVariableDeclaration> writtenGlobalVariables =
            ImmutableList.builder();
        ImmutableList.Builder<CVariableDeclaration> globalVariables = ImmutableList.builder();
        CStatement substituteAssignment =
            pSubstitution.substitute(
                assignment,
                callContext,
                Optional.of(writtenGlobalVariables),
                Optional.of(globalVariables));
        return Optional.of(
            new SubstituteEdge(
                substituteFunctionSummaryEdge(functionSummary, substituteAssignment),
                writtenGlobalVariables.build(),
                globalVariables.build()));
      }

    } else if (cfaEdge instanceof CFunctionCallEdge functionCall) {
      // CFunctionCallEdges also assign CPAchecker_TMPs -> handle assignment statements here too
      ImmutableList.Builder<CVariableDeclaration> writtenGlobalVariables = ImmutableList.builder();
      ImmutableList.Builder<CVariableDeclaration> globalVariables = ImmutableList.builder();
      CStatement substituteFunctionCall =
          pSubstitution.substitute(
              functionCall.getFunctionCall(),
              callContext,
              Optional.of(writtenGlobalVariables),
              Optional.of(globalVariables));
      return Optional.of(
          new SubstituteEdge(
              substituteFunctionCallEdge(functionCall, (CFunctionCall) substituteFunctionCall),
              writtenGlobalVariables.build(),
              globalVariables.build()));

    } else if (cfaEdge instanceof CReturnStatementEdge returnStatement) {
      ImmutableList.Builder<CVariableDeclaration> writtenGlobalVariables = ImmutableList.builder();
      ImmutableList.Builder<CVariableDeclaration> globalVariables = ImmutableList.builder();
      CReturnStatement substituteReturnStatement =
          pSubstitution.substitute(
              returnStatement.getReturnStatement(),
              callContext,
              Optional.of(writtenGlobalVariables),
              Optional.of(globalVariables));
      return Optional.of(
          new SubstituteEdge(
              substituteReturnStatementEdge(returnStatement, substituteReturnStatement),
              writtenGlobalVariables.build(),
              globalVariables.build()));
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
