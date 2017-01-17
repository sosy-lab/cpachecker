/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.policyiteration;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.summary.SummaryManager;
import org.sosy_lab.cpachecker.cpa.summary.blocks.Block;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.VariableClassificationBuilder.VariablesCollectingVisitor;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;

/**
 * Summaries for LPI
 *
 * todo: aliasing support.
 */
public class PolicySummaryManager implements SummaryManager {
  private final PathFormulaManager pfmgr;
  private final StateFormulaConversionManager stateFormulaConversionManager;
  private final BooleanFormulaManager bfmgr;

  private static final int SSA_NAMESPACING_CONST = 1000;

  public PolicySummaryManager(
      PathFormulaManager pPfmgr,
      StateFormulaConversionManager pStateFormulaConversionManager,
      FormulaManagerView pFmgr) {
    pfmgr = pPfmgr;
    stateFormulaConversionManager = pStateFormulaConversionManager;
    bfmgr = pFmgr.getBooleanFormulaManager();
  }

  @Override
  public List<? extends AbstractState> getEntryStates(
      AbstractState callSite, CFANode callNode, Block calledBlock)
      throws CPAException, InterruptedException {
    return getEntryStates0((PolicyAbstractedState) callSite, callNode);
  }

  private List<? extends AbstractState> getEntryStates0(
      PolicyAbstractedState aCallState, CFANode callNode)
      throws CPAException, InterruptedException {

    PolicyIntermediateState iCallState =
        stateFormulaConversionManager.abstractStateToIntermediate(aCallState);
    PathFormula context = iCallState.getPathFormula();

    assert callNode.getNumLeavingEdges() == 1;

    CFunctionCallEdge callEdge = (CFunctionCallEdge) callNode.getLeavingEdge(0);
    List<CExpression> arguments = callEdge.getArguments();
    List<CParameterDeclaration> parameters = callEdge.getSuccessor().getFunctionParameters();
    Preconditions.checkState(arguments.size() == parameters.size());

    PathFormula entryState = pfmgr.makeAnd(
        context, callEdge
    );

    // todo: fix the precision inside the function, only consider the vars
    // *read* inside the func.
    return Collections.singletonList(PolicyIntermediateState.of(
        callEdge.getSuccessor(),
        entryState,
        aCallState
    ));
  }

  @Override
  public List<? extends AbstractState> applyFunctionSummary(
      AbstractState callSite, AbstractState exitState, CFANode callNode, Block calledBlock)
      throws CPATransferException, InterruptedException {
    PolicyAbstractedState aCallState = (PolicyAbstractedState) callSite;
    PolicyAbstractedState aExitState = (PolicyAbstractedState) exitState;

    assert callNode.getNumLeavingEdges() == 1;
    CFunctionCallEdge callEdge = (CFunctionCallEdge) callNode.getLeavingEdge(0);
    CFANode returnNode = callNode.getLeavingSummaryEdge().getSuccessor();
    assert returnNode.getNumEnteringEdges() == 1;
    return Collections.singletonList(
        applySummary(
            aCallState, aExitState,
            returnNode,
            callEdge,
            calledBlock
        )
    );
  }

  private PolicyIntermediateState applySummary(
    PolicyAbstractedState callState,
    PolicyAbstractedState exitState,
    CFANode returnNode,
    CFunctionCallEdge callEdge,
    Block calledBlock
  ) throws CPATransferException, InterruptedException {

    Map<String, Integer> ssaUpdatesToExit = new HashMap<>();

    SSAMap outMap = getOutSSAMap(
        callEdge, callState, exitState, calledBlock, ssaUpdatesToExit);

    SSAMap exitSsa = exitState.getSSA();
    SSAMap callSsa = callState.getSSA();

    SSAMapBuilder newExitSsaBuilder = SSAMap.emptySSAMap().builder();
    exitSsa.allVariables().forEach(
        varName -> {
          int newIdx = ssaUpdatesToExit.getOrDefault(varName, exitSsa.getIndex(varName));
          newExitSsaBuilder.setIndex(varName, exitSsa.getType(varName), newIdx);
        }
    );
    SSAMap newExitSsa = newExitSsaBuilder.build();

    PolicyAbstractedState weakenedExitState = rebaseExitState(exitState, newExitSsa);

    BooleanFormula paramRenamingConstraint = getParamRenamingConstraint(
        callEdge, callSsa, newExitSsa
    ).getFormula();

    BooleanFormula returnRenamingConstraint = getReturnRenamingConstraint(
        callEdge.getSummaryEdge(), callSsa, newExitSsa
    ).getFormula();

    PathFormula outConstraint = new PathFormula(
        bfmgr.and(paramRenamingConstraint, returnRenamingConstraint),
        outMap,
        callState.getPointerTargetSet(),
        1
    );

    return PolicyIntermediateState.of(
        returnNode, outConstraint, callState, weakenedExitState
    );
  }

  /**
   * Rebase exit state on top of the new SSA.
   */
  private PolicyAbstractedState rebaseExitState(
      PolicyAbstractedState exitState,
      SSAMap newSsa
  ) {
    return exitState.withNewSSA(newSsa);
  }

  /**
   * Generate {@link SSAMap} associated with the linked state.
   *
   * @param ssaUpdatesToIndex Write-into parameters for specifying updates which should
   *                          be performed on the exit state.
   */
  private SSAMap getOutSSAMap(
      CFunctionCallEdge pCallEdge,
      PolicyAbstractedState pCallState,
      PolicyAbstractedState pExitState,
      Block pBlock,
      Map<String, Integer> ssaUpdatesToIndex
  ) {
    SSAMapBuilder outSSABuilder = SSAMap.emptySSAMap().builder();
    SSAMap exitSsa = pExitState.getSSA();
    SSAMap callSsa = pCallState.getSSA();

    CFunctionSummaryEdge summaryEdge = pCallEdge.getSummaryEdge();

    Set<String> modifiedVars = pBlock.getModifiedVariableNames();
    Set<String> readVars = pBlock.getReadVariableNames();

    // todo: get rid of this variable.
    Set<String> processed = new HashSet<>();

    // For modified globals:
    // the SSA index should be larger than that of
    // the one currently in {@code callSsa} and should agree with
    // {@code exitSsa}.
    modifiedVars.stream()
        .filter(varName -> isGlobal(varName))
        .forEach(varName -> {
          int callIdx = callSsa.getIndex(varName);
          int newIdx = callIdx + 1;
          ssaUpdatesToIndex.put(varName, newIdx);
          processed.add(varName);
          outSSABuilder.setIndex(varName, callSsa.getType(varName), newIdx);
        }
    );

    // For variables written into, the index should be one bigger
    // than that of a callsite.
    getWrittenIntoVars(summaryEdge).forEach(varName -> {
      int callIdx = callSsa.getIndex(varName);
      int newIdx = callIdx + 1;
      ssaUpdatesToIndex.put(varName, newIdx);
      processed.add(varName);
      outSSABuilder.setIndex(varName, callSsa.getType(varName), newIdx);
    });

    // For read globals which are NOT modified:
    // the SSA index should match on call and exit site.
    readVars.stream()
        .filter(s -> !modifiedVars.contains(s) && isGlobal(s))
        .forEach(varName -> {
          int callIdx = callSsa.getIndex(varName);
          ssaUpdatesToIndex.put(varName, callIdx);
          processed.add(varName);
          outSSABuilder.setIndex(varName, callSsa.getType(varName), callIdx);
        });

    // todo: for modified & read globals: do the renaming trick.

    // For all variables from calling site which weren't processed yet:
    // the output SSA index should be the same.
    callSsa.allVariables().stream().filter(
        varName -> !processed.contains(varName)
    ).forEach(
        varName -> outSSABuilder.setIndex(
            varName, callSsa.getType(varName), callSsa.getIndex(varName)));

    // For all other variables: namespace them away in order to avoid the
    // collision.
    exitSsa.allVariables().stream().filter(
        varName -> !processed.contains(varName)
    ).forEach(
        varName -> ssaUpdatesToIndex.put(varName, namespaceSsaIdx(callSsa.getIndex(varName))));

    return outSSABuilder.build();
  }

  /**
   * Get function parameters. E.g. {@code k, t} for {@code int f(int k, int t)}.
   */
  private Set<String> getParamVarNames(CFunctionEntryNode entryNode) {
    return entryNode.getFunctionParameters().stream()
        .map(s -> s.getQualifiedName()).collect(Collectors.toSet());
  }

  /**
   * @return constraint for renaming returned parameters.
   *
   * @param exitSSA {@link SSAMap} used for returned parameter.
   * @param callSSA {@link SSAMap} used for parameter overriden by the function call.
   */
  private PathFormula getReturnRenamingConstraint(
      CFunctionSummaryEdge pEdge,
      SSAMap callSSA,
      SSAMap exitSSA
  ) throws CPATransferException, InterruptedException {

    SSAMapBuilder usedSSABuilder = SSAMap.emptySSAMap().builder();
    Set<String> visited = new HashSet<>();

    for (String var : getWrittenIntoVars(pEdge)) {
      usedSSABuilder.setIndex(var, callSSA.getType(var), callSSA.getIndex(var));
      visited.add(var);
    }

    for (String var : exitSSA.allVariables()) {
      if (!visited.contains(var)) {
        usedSSABuilder.setIndex(var, exitSSA.getType(var), exitSSA.getIndex(var));
      }
    }


    CFAEdge returnEdge = pEdge.getSuccessor().getEnteringEdge(0);
    PathFormula context = new PathFormula(
        bfmgr.makeTrue(),
        usedSSABuilder.build(),
        PointerTargetSet.emptyPointerTargetSet(),
        1);

    return pfmgr.makeAnd(context, returnEdge);
  }

  /**
   * @return set of variables participating in the {@code return} expression,
   * e.g. {@code a, b, c} in {@code return a + b + c;}
   */
  private Set<String> getVarsInReturnArgument(CFunctionSummaryEdge pEdge) {
    CFANode exitNode = pEdge.getSuccessor().getEnteringEdge(0).getPredecessor();
    VariablesCollectingVisitor varsCollector = new VariablesCollectingVisitor(exitNode);
    List<CExpression> params =
        pEdge.getExpression().getFunctionCallExpression().getParameterExpressions();
    return params.stream()
        .map(p -> p.accept(varsCollector)).filter(s -> s != null)
        .reduce(ImmutableSet.of(), Sets::union);
  }

  /**
   * @return vars written-into by the function call,
   * e.g. {@code a} in {@code a = f(42);}
   */
  private Set<String> getWrittenIntoVars(CFunctionSummaryEdge pEdge) {
    CFANode callNode = pEdge.getPredecessor();
    VariablesCollectingVisitor varsCollector = new VariablesCollectingVisitor(callNode);
    if (pEdge.getExpression() instanceof CFunctionCallAssignmentStatement) {
      CLeftHandSide lhs = ((CFunctionCallAssignmentStatement) pEdge.getExpression()).getLeftHandSide();
      Set<String> collected = lhs.accept(varsCollector);
      if (collected != null) {
        return collected;
      }
    }
    return ImmutableSet.of();
  }

  /**
   * Get constraint for parameter renaming.
   */
  private PathFormula getParamRenamingConstraint(
      CFunctionCallEdge pCallEdge,
      SSAMap pCallSSA,
      SSAMap pExitSSA
  ) throws CPATransferException, InterruptedException {
    return pfmgr.makeAnd(
        new PathFormula(
            bfmgr.makeTrue(),
            getSsaForParamRenaming(pCallEdge, pCallSSA, pExitSSA),
            PointerTargetSet.emptyPointerTargetSet(),
            1),
        pCallEdge
    );
  }

  /**
   * Generate {@link SSAMap} for parameter/argument renaming.
   *
   * Indexes for the parameters should be taken from the {@code pCallSSA}.
   * Indexes for the arguments should be taken from the {@code pExitSSA}.
   */
  private SSAMap getSsaForParamRenaming(
      CFunctionCallEdge pCallEdge, SSAMap pCallSSA, SSAMap pExitSSA) {

    CFunctionEntryNode entryNode = pCallEdge.getSuccessor();
    SSAMapBuilder builder = SSAMap.emptySSAMap().builder();
    List<CExpression> args = pCallEdge.getArguments();

    VariablesCollectingVisitor variablesCollector =
        new VariablesCollectingVisitor(pCallEdge.getPredecessor());

    Set<String> argVars = args.stream().map(
        expr -> expr.accept(variablesCollector)
    ).filter(c -> c != null).reduce(ImmutableSet.of(), Sets::union);

    for (String var : argVars) {
      builder = builder.setIndex(var, pCallSSA.getType(var), pCallSSA.getIndex(var));
    }

    for (CParameterDeclaration param : entryNode.getFunctionParameters()) {
      String varName = param.getQualifiedName();
      builder = builder.setIndex(varName, pExitSSA.getType(varName), pExitSSA.getIndex(varName));
    }

    return builder.build();
  }

  /**
   * "Namespace" the SSA index in order to guarantee no collisions between exit and call state.
   */
  private int namespaceSsaIdx(int idx) {
    return SSA_NAMESPACING_CONST + idx;
  }

  /**
   * @return whether the variable name is global.
   */
  private boolean isGlobal(String varName) {
    return !varName.contains("::");
  }
}
