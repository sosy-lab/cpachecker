// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.location;

import com.google.common.base.Preconditions;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.ForwardingDistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombineOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombinePrecisionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombineSingletonPrecisionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.EqualityCombineOperator;
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
import org.sosy_lab.cpachecker.cpa.location.LocationCPA;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.location.LocationTransferRelationBackwards;

public class DistributedLocationCPA implements ForwardingDistributedConfigurableProgramAnalysis {

  private final SerializePrecisionOperator serializePrecisionOperator;
  private final DeserializePrecisionOperator deserializePrecisionOperator;
  private final SerializeOperator serializeOperator;
  private final DeserializeOperator deserializeOperator;
  private final CoverageOperator coverageOperator;
  private final CombineOperator combineOperator;
  private final ProceedOperator proceedOperator;
  private final ViolationConditionOperator violationConditionOperator;
  private final CombinePrecisionOperator combinePrecisionOperator;

  private final LocationCPA locationCPA;
  private final BlockNode node;

  public DistributedLocationCPA(
      LocationCPA pLocationCPA, BlockNode pNode, Map<Integer, CFANode> pNodes) {
    locationCPA = pLocationCPA;
    serializePrecisionOperator = new NoPrecisionSerializeOperator();
    deserializePrecisionOperator = new NoPrecisionDeserializeOperator();
    proceedOperator = ProceedOperator.always();
    coverageOperator = new LocationStateCoverageOperator();
    combineOperator = new EqualityCombineOperator(coverageOperator, getAbstractStateClass());
    serializeOperator = new SerializeLocationStateOperator();
    deserializeOperator = new DeserializeLocationState(locationCPA.getStateFactory(), pNodes);
    violationConditionOperator =
        new BackwardTransferViolationConditionOperator(
            new LocationTransferRelationBackwards(locationCPA.getStateFactory()), locationCPA);
    combinePrecisionOperator = new CombineSingletonPrecisionOperator();
    node = pNode;
  }

  @Override
  public SerializeOperator getSerializeOperator() {
    return serializeOperator;
  }

  @Override
  public DeserializeOperator getDeserializeOperator() {
    return deserializeOperator;
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
  public CombinePrecisionOperator getCombinePrecisionOperator() {
    return combinePrecisionOperator;
  }

  @Override
  public ProceedOperator getProceedOperator() {
    return proceedOperator;
  }

  @Override
  public ViolationConditionOperator getViolationConditionOperator() {
    return violationConditionOperator;
  }

  @Override
  public CoverageOperator getCoverageOperator() {
    return coverageOperator;
  }

  @Override
  public CombineOperator getCombineOperator() {
    return combineOperator;
  }

  @Override
  public Class<? extends AbstractState> getAbstractStateClass() {
    return LocationState.class;
  }

  @Override
  public ConfigurableProgramAnalysis getCPA() {
    return locationCPA;
  }

  @Override
  public boolean isMostGeneralBlockEntryState(AbstractState pAbstractState) {
    Preconditions.checkArgument(
        pAbstractState instanceof LocationState,
        "Expected LocationState, but got %s",
        pAbstractState.getClass().getSimpleName());
    CFANode location = ((LocationState) pAbstractState).getLocationNode();
    return location.equals(node.getInitialLocation()) || location.equals(node.getFinalLocation());
  }

  @Override
  public AbstractState reset(AbstractState pAbstractState) {
    Preconditions.checkArgument(
        pAbstractState instanceof LocationState,
        "Expected LocationState, but got %s",
        pAbstractState.getClass().getSimpleName());
    return pAbstractState;
  }
}
