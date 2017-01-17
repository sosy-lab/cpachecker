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

import com.google.common.base.Equivalence.Wrapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
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
import org.sosy_lab.cpachecker.util.templates.Template;
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
    CFAEdge returnEdge = returnNode.getEnteringEdge(0);
    return Collections.singletonList(
        applySummary(
            aCallState, aExitState,
            returnNode,
            callEdge, returnEdge,
            calledBlock
        )
    );
  }

  private PolicyIntermediateState applySummary(
    PolicyAbstractedState callState,
    PolicyAbstractedState exitState,
    CFANode returnNode,
    CFunctionCallEdge callEdge,
    CFAEdge returnEdge,
    Block calledBlock
  ) throws CPATransferException, InterruptedException {

    Map<String, Integer> ssaUpdatesToExit = new HashMap<>();

    SSAMap outMap = getOutSSAMap(
        callEdge, returnEdge, callState, exitState, calledBlock, ssaUpdatesToExit);

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
    Set<String> paramVarNames = getParamVarNames(callEdge.getSuccessor());

    PolicyAbstractedState weakenedExitState
        = weakenExitState(exitState, paramVarNames, newExitSsa);

    BooleanFormula paramRenamingConstraint = getParamRenamingConstraint(
        callEdge, callSsa, newExitSsa
    ).getFormula();

    BooleanFormula returnRenamingConstraint = getReturnRenamingConstraint(
        returnEdge, weakenedExitState
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
   * Weaken the exit state and rebase it on top of the new SSA.
   */
  private PolicyAbstractedState weakenExitState(
      PolicyAbstractedState exitState,
      Set<String> paramVarNames,
      SSAMap newSsa
  ) {

    // Remove all vars which are not global OR parameters.
    Map<Template, PolicyBound> weakenedAbstraction =
        Maps.filterKeys(
            exitState.getAbstraction(),
            t -> t.getVarNames().stream().allMatch(
                varName -> isGlobal(varName) || paramVarNames.contains(varName)
            ));

    return exitState.withNewAbstractionAndSSA(weakenedAbstraction, newSsa);
  }

  /**
   * Generate {@link SSAMap} associated with the linked state.
   *
   * @param ssaUpdatesToIndex Write-into parameters for specifying updates which should
   *                          be performed on the exit state.
   */
  private SSAMap getOutSSAMap(
      CFunctionCallEdge pCallEdge,
      CFAEdge pReturnEdge,
      PolicyAbstractedState pCallState,
      PolicyAbstractedState pExitState,
      Block pBlock,
      Map<String, Integer> ssaUpdatesToIndex
  ) {
    SSAMapBuilder outSSABuilder = SSAMap.emptySSAMap().builder();
    SSAMap exitSsa = pExitState.getSSA();
    SSAMap callSsa = pCallState.getSSA();

    CFunctionEntryNode entryNode = pCallEdge.getSuccessor();

    Set<Wrapper<ASimpleDeclaration>>
        modifiedVars = pBlock.getModifiedVarsForReturnEdge(pReturnEdge);
    Set<Wrapper<ASimpleDeclaration>> readVars =
        pBlock.getReadVariablesForCallEdge(pCallEdge);

    Set<String> processed = new HashSet<>();

    // For all vars modified inside the block:
    //    take index from {@code pExitState}.
    modifiedVars.stream().map(s -> s.get().getQualifiedName()).forEach(
        varName -> {
          if (!exitSsa.containsVariable(varName) &&
              !callSsa.containsVariable(varName)) {
            return;
          }
          int exitIdx = exitSsa.getIndex(varName);
          int callIdx = callSsa.getIndex(varName);
          int newIdx;
          CType type;

          if (exitIdx >= callIdx) {
            newIdx = exitIdx;
            type = exitSsa.getType(varName);
          } else {
            newIdx = callIdx + 1;
            ssaUpdatesToIndex.put(varName, newIdx);
            type = callSsa.getType(varName);
          }
          processed.add(varName);
          outSSABuilder.setIndex(varName, type, newIdx);
        }
    );

    Set<String> paramVarNames = getParamVarNames(entryNode);

    // For all parameter vars and GLOBAL vars read inside the block:
    //    indexes should match.
    readVars.stream().map(s -> s.get().getQualifiedName())
        .filter(s -> isGlobal(s) || paramVarNames.contains(s))
        .forEach(varName -> {
          if (!callSsa.containsVariable(varName) || processed.contains(varName)) {
            return;
          }
          int callIdx = callSsa.getIndex(varName);
          int exitIdx = exitSsa.getIndex(varName);
          outSSABuilder.setIndex(
              varName, callSsa.getType(varName), callIdx
          );
          processed.add(varName);
          if (callIdx != exitIdx) {
            ssaUpdatesToIndex.put(varName, callIdx);
          }
        }
    );

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

  private Set<String> getParamVarNames(CFunctionEntryNode entryNode) {
    return entryNode.getFunctionParameters().stream()
        .map(s -> s.getQualifiedName()).collect(Collectors.toSet());
  }


  /**
   * "Namespace" the SSA index in order to guarantee no collisions between exit and call state.
   */
  private int namespaceSsaIdx(int idx) {
    return SSA_NAMESPACING_CONST + idx;
  }

  private boolean isGlobal(String varName) {
    // todo: avoid hacks.
    return !varName.contains("::");
  }

  /**
   * @return constraint for renaming returned parameters.
   */
  private PathFormula getReturnRenamingConstraint(
      CFAEdge pReturnEdge,
      PolicyAbstractedState pExitState
  ) throws CPATransferException, InterruptedException {
    PathFormula context = new PathFormula(
        bfmgr.makeTrue(), pExitState.getSSA(), pExitState.getPointerTargetSet(), 1);

    return pfmgr.makeAnd(context, pReturnEdge);
  }

  /**
   * Get constraint for parameter renaming.
   */
  private PathFormula getParamRenamingConstraint(
      CFunctionCallEdge pCallEdge, SSAMap pCallSSA, SSAMap pExitSSA
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
}
