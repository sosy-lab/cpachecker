// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssBlockAnalysisStatistics;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.ForwardingDistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializePrecisionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed.ProceedOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializePrecisionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.verification_condition.ViolationConditionOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;

public class DistributedCompositeCPA implements ForwardingDistributedConfigurableProgramAnalysis {

  private final CompositeCPA compositeCPA;
  private final SerializeOperator serialize;
  private final DeserializeOperator deserialize;
  private final ProceedCompositeStateOperator proceed;

  private final DssBlockAnalysisStatistics statistics;

  private final DeserializeCompositePrecisionOperator deserializePrecisionOperator;
  private final SerializeCompositePrecisionOperator serializePrecisionOperator;
  private final ViolationConditionOperator verificationConditionOperator;

  private final ImmutableMap<
          Class<? extends ConfigurableProgramAnalysis>, DistributedConfigurableProgramAnalysis>
      analyses;

  public DistributedCompositeCPA(
      CompositeCPA pCompositeCPA,
      BlockNode pNode,
      ImmutableMap<Integer, CFANode> pIntegerCFANodeMap,
      ImmutableMap<
              Class<? extends ConfigurableProgramAnalysis>, DistributedConfigurableProgramAnalysis>
          registered) {
    statistics = new DssBlockAnalysisStatistics("DCPA-" + pNode.getId());
    compositeCPA = pCompositeCPA;
    serialize = new SerializeCompositeStateOperator(registered, statistics);
    deserialize =
        new DeserializeCompositeStateOperator(
            compositeCPA, registered, pIntegerCFANodeMap, statistics);
    proceed = new ProceedCompositeStateOperator(registered, statistics);
    serializePrecisionOperator = new SerializeCompositePrecisionOperator(registered);
    deserializePrecisionOperator =
        new DeserializeCompositePrecisionOperator(registered, compositeCPA, pIntegerCFANodeMap);
    analyses = registered;
    verificationConditionOperator = new CompositeViolationConditionOperator(compositeCPA, analyses);
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
  public ProceedOperator getProceedOperator() {
    return proceed;
  }

  @Override
  public Class<? extends AbstractState> getAbstractStateClass() {
    return CompositeState.class;
  }

  @Override
  public ConfigurableProgramAnalysis getCPA() {
    return compositeCPA;
  }

  @Override
  public boolean isTop(AbstractState pAbstractState) {
    CompositeState co = (CompositeState) pAbstractState;
    for (AbstractState wrappedState : co.getWrappedStates()) {
      for (DistributedConfigurableProgramAnalysis value : analyses.values()) {
        if (value.doesOperateOn(wrappedState.getClass())) {
          if (!value.isTop(wrappedState)) {
            return false;
          }
          break;
        }
      }
    }
    return true;
  }

  @Override
  public ViolationConditionOperator getViolationConditionOperator() {
    return verificationConditionOperator;
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
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    Preconditions.checkNotNull(node);
    ImmutableList.Builder<AbstractState> initialStates = ImmutableList.builder();
    for (ConfigurableProgramAnalysis cpa : compositeCPA.getWrappedCPAs()) {
      if (analyses.containsKey(cpa.getClass())) {
        initialStates.add(
            Objects.requireNonNull(analyses.get(cpa.getClass())).getInitialState(node, partition));
      } else {
        initialStates.add(cpa.getInitialState(node, partition));
      }
    }
    return new CompositeState(initialStates.build());
  }

  public DssBlockAnalysisStatistics getStatistics() {
    return statistics;
  }
}
