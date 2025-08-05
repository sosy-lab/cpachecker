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
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
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
                  : new SubstituteEdge(
                      cfaEdge,
                      threadEdge,
                      ImmutableMap.of(),
                      ImmutableSet.of(),
                      ImmutableSet.of(),
                      ImmutableSet.of(),
                      ImmutableSet.of(),
                      ImmutableSet.of()));
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
          if (declaration.isGlobal()) {
            CVariableDeclaration declarationSubstitute =
                pSubstitution.getLocalVariableDeclarationSubstitute(
                    variableDeclaration, callContext);
            return Optional.of(
                new SubstituteEdge(
                    substituteDeclarationEdge(declarationEdge, declarationSubstitute),
                    pThreadEdge,
                    // TODO this requires handling, e.g. 'int * ptr = &x;'
                    ImmutableMap.of(),
                    ImmutableSet.of(),
                    ImmutableSet.of(),
                    // no global accesses needed, global declarations are outside main()
                    ImmutableSet.of(),
                    ImmutableSet.of(),
                    ImmutableSet.of()));
          } else {
            CVariableDeclaration declarationSubstitute =
                pSubstitution.getLocalVariableDeclarationSubstitute(
                    variableDeclaration, callContext);
            ImmutableSet<CVariableDeclaration> accessedGlobalVariables =
                pSubstitution.getGlobalVariablesUsedInLocalVariableDeclaration(variableDeclaration);
            return Optional.of(
                new SubstituteEdge(
                    substituteDeclarationEdge(declarationEdge, declarationSubstitute),
                    pThreadEdge,
                    // TODO this requires handling, e.g. 'int * ptr = &x;'
                    ImmutableMap.of(),
                    ImmutableSet.of(),
                    ImmutableSet.of(),
                    ImmutableSet.of(),
                    accessedGlobalVariables,
                    ImmutableSet.of()));
          }
        }
      }

      // TODO try to create a single method here for all these edge types
    } else if (cfaEdge instanceof CAssumeEdge assume) {
      Map<CVariableDeclaration, CVariableDeclaration> pointerAssignments = new HashMap<>();
      Set<CVariableDeclaration> writtenPointerDereferences = new HashSet<>();
      Set<CVariableDeclaration> accessedPointerDereferences = new HashSet<>();
      Set<CVariableDeclaration> writtenGlobalVariables = new HashSet<>();
      Set<CVariableDeclaration> accessedGlobalVariables = new HashSet<>();
      Set<CFunctionDeclaration> accessedFunctionPointers = new HashSet<>();
      CExpression substituteAssumption =
          pSubstitution.substitute(
              assume.getExpression(),
              callContext,
              false,
              false,
              Optional.of(writtenPointerDereferences),
              Optional.of(accessedPointerDereferences),
              Optional.of(writtenGlobalVariables),
              Optional.of(accessedGlobalVariables),
              Optional.of(accessedFunctionPointers));
      return Optional.of(
          new SubstituteEdge(
              substituteAssumeEdge(assume, substituteAssumption),
              pThreadEdge,
              ImmutableMap.copyOf(pointerAssignments),
              ImmutableSet.copyOf(writtenPointerDereferences),
              ImmutableSet.copyOf(accessedPointerDereferences),
              ImmutableSet.copyOf(writtenGlobalVariables),
              ImmutableSet.copyOf(accessedGlobalVariables),
              ImmutableSet.copyOf(accessedFunctionPointers)));

    } else if (cfaEdge instanceof CStatementEdge statement) {
      Map<CVariableDeclaration, CVariableDeclaration> pointerAssignments = new HashMap<>();
      Set<CVariableDeclaration> writtenPointerDereferences = new HashSet<>();
      Set<CVariableDeclaration> accessedPointerDereferences = new HashSet<>();
      Set<CVariableDeclaration> writtenGlobalVariables = new HashSet<>();
      Set<CVariableDeclaration> accessedGlobalVariables = new HashSet<>();
      Set<CFunctionDeclaration> accessedFunctionPointers = new HashSet<>();
      CStatement substituteStatement =
          pSubstitution.substitute(
              statement.getStatement(),
              callContext,
              Optional.of(pointerAssignments),
              Optional.of(writtenPointerDereferences),
              Optional.of(accessedPointerDereferences),
              Optional.of(writtenGlobalVariables),
              Optional.of(accessedGlobalVariables),
              Optional.of(accessedFunctionPointers));
      return Optional.of(
          new SubstituteEdge(
              substituteStatementEdge(statement, substituteStatement),
              pThreadEdge,
              ImmutableMap.copyOf(pointerAssignments),
              ImmutableSet.copyOf(writtenPointerDereferences),
              ImmutableSet.copyOf(accessedPointerDereferences),
              ImmutableSet.copyOf(writtenGlobalVariables),
              ImmutableSet.copyOf(accessedGlobalVariables),
              ImmutableSet.copyOf(accessedFunctionPointers)));

    } else if (cfaEdge instanceof CFunctionSummaryEdge functionSummary) {
      // only substitute assignments (e.g. CPAchecker_TMP = func();)
      if (functionSummary.getExpression() instanceof CFunctionCallAssignmentStatement assignment) {
        Map<CVariableDeclaration, CVariableDeclaration> pointerAssignments = new HashMap<>();
        Set<CVariableDeclaration> writtenPointerDereferences = new HashSet<>();
        Set<CVariableDeclaration> accessedPointerDereferences = new HashSet<>();
        Set<CVariableDeclaration> writtenGlobalVariables = new HashSet<>();
        Set<CVariableDeclaration> accessedGlobalVariables = new HashSet<>();
        Set<CFunctionDeclaration> accessedFunctionPointers = new HashSet<>();
        CStatement substituteAssignment =
            pSubstitution.substitute(
                assignment,
                callContext,
                Optional.of(pointerAssignments),
                Optional.of(writtenPointerDereferences),
                Optional.of(accessedPointerDereferences),
                Optional.of(writtenGlobalVariables),
                Optional.of(accessedGlobalVariables),
                Optional.of(accessedFunctionPointers));
        return Optional.of(
            new SubstituteEdge(
                substituteFunctionSummaryEdge(functionSummary, substituteAssignment),
                pThreadEdge,
                ImmutableMap.copyOf(pointerAssignments),
                ImmutableSet.copyOf(writtenPointerDereferences),
                ImmutableSet.copyOf(accessedPointerDereferences),
                ImmutableSet.copyOf(writtenGlobalVariables),
                ImmutableSet.copyOf(accessedGlobalVariables),
                ImmutableSet.copyOf(accessedFunctionPointers)));
      }

    } else if (cfaEdge instanceof CFunctionCallEdge functionCall) {
      // CFunctionCallEdges also assign CPAchecker_TMPs -> handle assignment statements here too
      Map<CVariableDeclaration, CVariableDeclaration> pointerAssignments = new HashMap<>();
      Set<CVariableDeclaration> writtenPointerDereferences = new HashSet<>();
      Set<CVariableDeclaration> accessedPointerDereferences = new HashSet<>();
      Set<CVariableDeclaration> writtenGlobalVariables = new HashSet<>();
      Set<CVariableDeclaration> accessedGlobalVariables = new HashSet<>();
      Set<CFunctionDeclaration> accessedFunctionPointers = new HashSet<>();
      CStatement substituteFunctionCall =
          pSubstitution.substitute(
              functionCall.getFunctionCall(),
              callContext,
              Optional.of(pointerAssignments),
              Optional.of(writtenPointerDereferences),
              Optional.of(accessedPointerDereferences),
              Optional.of(writtenGlobalVariables),
              Optional.of(accessedGlobalVariables),
              Optional.of(accessedFunctionPointers));
      return Optional.of(
          new SubstituteEdge(
              substituteFunctionCallEdge(functionCall, (CFunctionCall) substituteFunctionCall),
              pThreadEdge,
              ImmutableMap.copyOf(pointerAssignments),
              ImmutableSet.copyOf(writtenPointerDereferences),
              ImmutableSet.copyOf(accessedPointerDereferences),
              ImmutableSet.copyOf(writtenGlobalVariables),
              ImmutableSet.copyOf(accessedGlobalVariables),
              ImmutableSet.copyOf(accessedFunctionPointers)));

    } else if (cfaEdge instanceof CReturnStatementEdge returnStatement) {
      Set<CVariableDeclaration> writtenPointerDereferences = new HashSet<>();
      Set<CVariableDeclaration> accessedPointerDereferences = new HashSet<>();
      Set<CVariableDeclaration> writtenGlobalVariables = new HashSet<>();
      Set<CVariableDeclaration> accessedGlobalVariables = new HashSet<>();
      Set<CFunctionDeclaration> accessedFunctionPointers = new HashSet<>();
      CReturnStatement substituteReturnStatement =
          pSubstitution.substitute(
              returnStatement.getReturnStatement(),
              callContext,
              Optional.of(writtenPointerDereferences),
              Optional.of(accessedPointerDereferences),
              Optional.of(writtenGlobalVariables),
              Optional.of(accessedGlobalVariables),
              Optional.of(accessedFunctionPointers));
      return Optional.of(
          new SubstituteEdge(
              substituteReturnStatementEdge(returnStatement, substituteReturnStatement),
              pThreadEdge,
              ImmutableMap.of(),
              ImmutableSet.copyOf(writtenPointerDereferences),
              ImmutableSet.copyOf(accessedPointerDereferences),
              ImmutableSet.copyOf(writtenGlobalVariables),
              ImmutableSet.copyOf(accessedGlobalVariables),
              ImmutableSet.copyOf(accessedFunctionPointers)));
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
