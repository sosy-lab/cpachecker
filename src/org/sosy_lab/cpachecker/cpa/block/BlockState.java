// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.block;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage.MessageType;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.infer.InferErrorConditionTargetInformation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;

// cannot be an AbstractStateWithLocation as initialization corrupts analysis
public class BlockState
    implements AbstractQueryableState, Partitionable, Targetable, FormulaReportingState {

  public enum BlockStateType {
    INITIAL,
    MID,
    FINAL,
    ABSTRACTION
  }

  public record StrengtheningInfo(Map<String, Formula> params, String strengtheningFunction) {}

  private Set<StrengtheningInfo> strengtheningInfos;

  private final CFANode targetCFANode;
  private final CFANode node;
  private final AnalysisDirection direction;
  private final BlockStateType type;
  private final BlockNode blockNode;
  private Map<String, Set<AbstractState>> errorConditions;
  private Map<String, MessageType> strengthenTypes;

  public BlockState(
      CFANode pNode,
      BlockNode pTargetNode,
      AnalysisDirection pDirection,
      BlockStateType pType,
      Map<String, Set<AbstractState>> pErrorConditions,
      Set<StrengtheningInfo> pStrengtheningInfos,
      Map<String, MessageType> pStrengthenTypes) {
    node = pNode;
    direction = pDirection;
    type = pType;
    if (pTargetNode == null) {
      targetCFANode = CFANode.newDummyCFANode();
    } else {
      targetCFANode =
          direction == AnalysisDirection.FORWARD
              ? pTargetNode.getAbstractionLocation()
              : pTargetNode.getFirst();
    }
    blockNode = pTargetNode;
    errorConditions = new HashMap<>(pErrorConditions);
    strengthenTypes = pStrengthenTypes;
    strengtheningInfos = pStrengtheningInfos;
  }

  public void setErrorConditionsForFunction(
      String pFunctionName, Set<AbstractState> pErrorConditions) {
    errorConditions.put(pFunctionName, pErrorConditions);
  }

  public void setErrorCondition(AbstractState pErrorCondition) {
    errorConditions.clear();
    errorConditions.put("single_element_map", ImmutableSet.of(pErrorCondition));
  }

  public void addErrorConditionToFunction(String pFunctionName, AbstractState pErrorCondition) {
    ImmutableSet.Builder<AbstractState> builder = ImmutableSet.builder();
    ImmutableSet<AbstractState> newSet =
        builder.addAll(errorConditions.get(pFunctionName)).add(pErrorCondition).build();
    errorConditions.put(pFunctionName, newSet);
  }

  public void setStrengthenTypes(Map<String, MessageType> pStrengthenTypes) {
    strengthenTypes = pStrengthenTypes;
  }

  public Map<String, MessageType> getStrengthenTypes() {
    return strengthenTypes;
  }

  public void setStrengthenType(String pFunctionName, MessageType pFunctionType) {
    strengthenTypes.put(pFunctionName, pFunctionType);
  }

  public BlockNode getBlockNode() {
    return blockNode;
  }

  public CFANode getLocationNode() {
    return node;
  }

  public BlockStateType getType() {
    return type;
  }

  @Override
  public String getCPAName() {
    return BlockCPA.class.getSimpleName();
  }

  @Override
  public @Nullable Object getPartitionKey() {
    return this;
  }

  @Override
  public String toString() {
    return "BlockState{" + "node=" + node + ", type=" + type + '}';
  }

  @Override
  public @NonNull Set<TargetInformation> getTargetInformation() throws IllegalStateException {
    ImmutableSet.Builder<TargetInformation> targetInformation = ImmutableSet.builder();
    if (!isTarget()) {
      return ImmutableSet.of();
    } else {
      if (isSatisfiesErrorConditionState()) {
        targetInformation.add(new InferErrorConditionTargetInformation());
      }
      if (isBlockEntryReached()) {
        targetInformation.add(
            new BlockEntryReachedTargetInformation(
                targetCFANode, type == BlockStateType.ABSTRACTION));
      }
    }
    return targetInformation.build();
  }

  public Map<String, Set<AbstractState>> getErrorConditions() {
    return errorConditions;
  }

  @Override
  public BooleanFormula getFormulaApproximation(FormulaManagerView manager) {
    if (isTarget()
        && !errorConditions.isEmpty()
        && direction == AnalysisDirection.FORWARD
        && !blockNode.getLast().equals(blockNode.getAbstractionLocation())
        && !blockNode.getLast().equals(node)
        && !isStartNodeOfBlock()) {
      ImmutableList<BooleanFormula> approximations =
          flattenedConditions().stream()
              .filter(FormulaReportingState.class::isInstance)
              .map(FormulaReportingState.class::cast)
              .map(s -> s.getFormulaApproximation(manager))
              .collect(ImmutableList.toImmutableList());
      return manager.getBooleanFormulaManager().and(approximations);
    }

    // TODO we could cache this so that it only gets updated when a new errorCondition is added
    ImmutableList.Builder<BooleanFormula> approximations = new ImmutableList.Builder<>();
    for (AbstractState state : flattenedConditions()) {
      approximations.add(AbstractStates.extractReportedFormulas(manager, state));
    }
    return manager.getBooleanFormulaManager().and(approximations.build());
  }

  // error condition intentionally left out as it is mutable
  @Override
  public boolean equals(Object pO) {
    if (pO instanceof BlockState that) {
      return direction == that.direction
          && Objects.equals(targetCFANode, that.targetCFANode)
          && Objects.equals(node, that.node)
          && type == that.type;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(targetCFANode, node, direction, type);
  }

  private boolean isLocatedOnTargetNode() {
    // equal to isLastNodeOfBlock iff there is no artificial block end,
    // else true iff node is equal to artificial block end
    return targetCFANode.equals(node);
  }

  private boolean isLastNodeOfBlock() {
    // abstract at the real block end (last node, not artificial block end)
    return blockNode.getLast().equals(node);
  }

  protected boolean isStartNodeOfBlock() {
    return blockNode.getFirst().equals(node);
  }

  @Override
  public boolean isTarget() {
    return isSatisfiesErrorConditionState() || isBlockEntryReached();
  }

  private boolean isBlockEntryReached() {
    return isLocatedOnTargetNode()
        || (direction == AnalysisDirection.FORWARD ? isLastNodeOfBlock() : isStartNodeOfBlock());
  }

  public boolean isSatisfiesErrorConditionState() {
    return CFAUtils.allEnteringEdges(node)
        .filter(FunctionSummaryEdge.class)
        .anyMatch(
            fse ->
                errorConditions.containsKey(fse.getFunctionEntry().getFunctionName())
                    && strengthenTypes.containsKey(fse.getFunctionEntry().getFunctionName())
                    && strengthenTypes
                        .get(fse.getFunctionEntry().getFunctionName())
                        .equals(MessageType.ERROR_CONDITION));
  }

  public void addStrengtheningInfo(
      PathFormulaManager pPathFormulaManager, PathFormula pPathFormula, CFunctionSummaryEdge cfse)
      throws CPATransferException {
    Map<String, Formula> paramMappings = new HashMap<>();
    List<CExpression> paramExps =
        cfse.getExpression().getFunctionCallExpression().getParameterExpressions();
    ImmutableList<String> paramNames =
        cfse.getFunctionEntry().getFunctionParameters().stream()
            .map(CParameterDeclaration::getQualifiedName)
            .collect(ImmutableList.toImmutableList());
    if (paramExps.size() != paramNames.size()) {
      throw new CPATransferException("Number of parameters does not match number of arguments");
    }
    for (int i = 0; i < paramNames.size(); i++) {
      Formula paramExpFormula =
          pPathFormulaManager.expressionToFormula(pPathFormula, paramExps.get(i), cfse);
      paramMappings.put(paramNames.get(i), paramExpFormula);
    }
    StrengtheningInfo newStrengtheningInfo =
        new StrengtheningInfo(paramMappings, cfse.getFunctionEntry().getFunctionName());
    ImmutableSet.Builder<StrengtheningInfo> builder = ImmutableSet.builder();
    strengtheningInfos = builder.addAll(strengtheningInfos).add(newStrengtheningInfo).build();
  }

  public Set<StrengtheningInfo> getStrengtheningInfo() {
    return strengtheningInfos;
  }

  Set<AbstractState> flattenedConditions() {
    return errorConditions.values().stream()
        .flatMap(s -> s.stream())
        .collect(ImmutableSet.toImmutableSet());
  }
}
