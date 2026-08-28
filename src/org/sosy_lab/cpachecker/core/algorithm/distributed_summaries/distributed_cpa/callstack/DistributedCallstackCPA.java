// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.callstack;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.ForwardingDistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombinePrecisionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombinePreconditionsOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombineSingletonPrecisionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombineViolationConditionsOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.EqualityCombinePreconditionsOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.coverage.CoverageOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializePrecisionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.NoPrecisionDeserializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed.ProceedOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.NoPrecisionSerializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializePrecisionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.verification_condition.BackwardTransferViolationConditionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.verification_condition.ViolationConditionOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.callstack.DssCallstackCPA;
import org.sosy_lab.cpachecker.cpa.callstack.DssCallstackState;

public class DistributedCallstackCPA implements ForwardingDistributedConfigurableProgramAnalysis {

  static final String DELIMITER = ",  ";

  /** Key under which the serialized callstack state stores {@code canBeTopState}. */
  static final String ALLOW_ALL_TRANSFERS_KEY = "canBeTopState ";

  private final SerializeOperator serialize;
  private final DeserializeOperator deserialize;
  private final CoverageOperator coverageOperator;
  private final ViolationConditionOperator verificationConditionOperator;
  private final CombinePreconditionsOperator combinePreconditionsOperator;
  private final SerializePrecisionOperator serializePrecisionOperator;
  private final DeserializePrecisionOperator deserializePrecisionOperator;
  private final CombinePrecisionOperator combinePrecisionOperator;
  private final CombineViolationConditionsOperator combineViolationConditionsOperator;

  private final DssCallstackCPA callstackCPA;
  private final CFA cfa;
  private final BlockNode block;
  private final boolean requiresStateResets;

  private boolean ignoreCallstack;

  public DistributedCallstackCPA(
      DssCallstackCPA pCallstackCPA,
      BlockNode pBlockNode,
      CFA pCFA,
      boolean pRequiresStateResets,
      BiMap<Integer, CFANode> pIdToNodeMap) {
    requiresStateResets = pRequiresStateResets;
    callstackCPA = pCallstackCPA;
    cfa = pCFA;
    block = pBlockNode;
    serialize = new SerializeCallstackStateOperator(pIdToNodeMap.inverse());
    deserialize =
        new DeserializeCallstackStateOperator(pCallstackCPA, pBlockNode, pIdToNodeMap::get);
    verificationConditionOperator =
        new BackwardTransferViolationConditionOperator(
            callstackCPA.getTransferRelation().copyBackwards(), pCallstackCPA);
    coverageOperator = new CallstackStateCoverageOperator();
    combinePreconditionsOperator =
        new EqualityCombinePreconditionsOperator(coverageOperator, getAbstractStateClass());
    serializePrecisionOperator = new NoPrecisionSerializeOperator();
    deserializePrecisionOperator = new NoPrecisionDeserializeOperator();
    combinePrecisionOperator = new CombineSingletonPrecisionOperator();
    combineViolationConditionsOperator = new CallstackStateCombineViolationConditionOperator();
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    // if the callstack of this block analysis is unknown,
    // the callstack must not restrict any transfer
    return callstackCPA.createState(null, node.getFunctionName(), node, ignoreCallstack);
  }

  @Override
  public Precision getInitialPrecision(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    return getCPA()
        .getInitialPrecision(cfa.getAllFunctions().get(node.getFunctionName()), partition);
  }

  @Override
  public SerializeOperator getSerializeOperator() {
    return serialize;
  }

  @Override
  public DeserializeOperator getDeserializeOperator() {
    return deserialize;
  }

  @Override
  public SerializePrecisionOperator getSerializePrecisionOperator() {
    return serializePrecisionOperator;
  }

  @Override
  public DeserializePrecisionOperator getDeserializePrecisionOperator() {
    return deserializePrecisionOperator;
  }

  @Override
  public CombineViolationConditionsOperator getCombineViolationConditionsOperator() {
    return combineViolationConditionsOperator;
  }

  @Override
  public CombinePrecisionOperator getCombinePrecisionOperator() {
    return combinePrecisionOperator;
  }

  @Override
  public ProceedOperator getProceedOperator() {
    return ProceedOperator.always();
  }

  @Override
  public Class<? extends AbstractState> getAbstractStateClass() {
    return CallstackState.class;
  }

  @Override
  public ConfigurableProgramAnalysis getCPA() {
    return callstackCPA;
  }

  @Override
  public boolean isMostGeneralBlockEntryState(AbstractState pAbstractState) {
    return true;
  }

  @Override
  public AbstractState reset(AbstractState pAbstractState) {
    Preconditions.checkArgument(pAbstractState instanceof CallstackState);
    if (requiresStateResets) {
      return callstackCPA.createState(
          null,
          block.getInitialLocation().getFunctionName(),
          block.getInitialLocation(),
          canBeTopState(pAbstractState));
    }
    return pAbstractState;
  }

  /** Whether the given state stems from a block analysis that does not know its callstack. */
  public static boolean canBeTopState(@Nullable AbstractState pState) {
    return pState instanceof DssCallstackState dssState && dssState.canBeTopState();
  }

  @Override
  public ViolationConditionOperator getViolationConditionOperator() {
    return verificationConditionOperator;
  }

  @Override
  public CoverageOperator getCoverageOperator() {
    return coverageOperator;
  }

  @Override
  public CombinePreconditionsOperator getCombineOperator() {
    return combinePreconditionsOperator;
  }

  @Override
  public Object computeProgramPointId(AbstractState pAbstractState) {
    return proofCheckingProgramPoint((CallstackState) pAbstractState);
  }

  private static CallstackProgramPoint proofCheckingProgramPoint(CallstackState pState) {
    return new CallstackProgramPoint(
        pState.getCallNode().getNodeNumber(),
        pState.getDepth(),
        pState.getCurrentFunction(),
        pState.getPreviousState() == null
            ? null
            : proofCheckingProgramPoint(pState.getPreviousState()));
  }

  private record CallstackProgramPoint(
      int callNode, int depth, String currentFunction, @Nullable CallstackProgramPoint previous) {}

  public void setIgnoreTransfer(boolean pIgnoreCallstack) {
    ignoreCallstack = pIgnoreCallstack;
  }
}
