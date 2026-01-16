// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.arg;

import java.util.Collection;
import org.jspecify.annotations.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.ForwardingDistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombineOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombinePrecisionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.coverage.CoverageOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializePrecisionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed.ProceedOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializePrecisionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.verification_condition.ViolationConditionOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

public class DistributedARGCPA
    implements ForwardingDistributedConfigurableProgramAnalysis, StatisticsProvider, WrapperCPA {

  private final ARGCPA argcpa;
  private final DistributedConfigurableProgramAnalysis wrappedCPA;
  private final ProceedOperator proceedOperator;
  private final DeserializeARGStateOperator deserializeARGStateOperator;
  private final SerializeARGStateOperator serializeARGStateOperator;
  private final SerializePrecisionOperator serializePrecisionOperator;
  private final DeserializePrecisionOperator deserializePrecisionOperator;
  private final ViolationConditionOperator verificationConditionOperator;
  private final CoverageOperator coverageOperator;
  private final CombineOperator combineOperator;

  public DistributedARGCPA(ARGCPA pARGCPA, DistributedConfigurableProgramAnalysis pWrapped) {
    argcpa = pARGCPA;
    wrappedCPA = pWrapped;
    proceedOperator = new ProceedARGCPAOperator(wrappedCPA);
    serializeARGStateOperator = new SerializeARGStateOperator(wrappedCPA);
    deserializeARGStateOperator = new DeserializeARGStateOperator(wrappedCPA);
    serializePrecisionOperator = new SerializeARGPrecisionOperator(wrappedCPA);
    deserializePrecisionOperator = new DeserializeARGPrecisionOperator(wrappedCPA);
    verificationConditionOperator = new ARGViolationConditionOperator(wrappedCPA);
    coverageOperator = new ARGStateCoverageOperator(wrappedCPA);
    combineOperator = new ARGStateCombineOperator(wrappedCPA);
  }

  @Override
  public SerializeOperator getSerializeOperator() {
    return serializeARGStateOperator;
  }

  @Override
  public DeserializeOperator getDeserializeOperator() {
    return deserializeARGStateOperator;
  }

  @Override
  public ProceedOperator getProceedOperator() {
    return proceedOperator;
  }

  @Override
  public DeserializePrecisionOperator getDeserializePrecisionOperator() {
    return deserializePrecisionOperator;
  }

  @Override
  public CombinePrecisionOperator getCombinePrecisionOperator() {
    return wrappedCPA.getCombinePrecisionOperator();
  }

  @Override
  public SerializePrecisionOperator getSerializePrecisionOperator() {
    return serializePrecisionOperator;
  }

  @Override
  public Class<? extends AbstractState> getAbstractStateClass() {
    return ARGState.class;
  }

  @Override
  public ConfigurableProgramAnalysis getCPA() {
    return argcpa;
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    return new ARGState(wrappedCPA.getInitialState(node, partition), null);
  }

  @Override
  public boolean isMostGeneralBlockEntryState(AbstractState pAbstractState) {
    if (pAbstractState instanceof ARGState argstate) {
      return wrappedCPA.isMostGeneralBlockEntryState(argstate.getWrappedState());
    }
    throw new IllegalArgumentException(
        "DistributedARGCPA can only work on " + getAbstractStateClass());
  }

  @Override
  public AbstractState reset(AbstractState pAbstractState) {
    ARGState argState = (ARGState) pAbstractState;
    return new ARGState(wrappedCPA.reset(argState.getWrappedState()), null);
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
  public CombineOperator getCombineOperator() {
    return combineOperator;
  }

  public DistributedConfigurableProgramAnalysis getWrappedCPA() {
    return wrappedCPA;
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    if (wrappedCPA instanceof StatisticsProvider statisticsProvider) {
      statisticsProvider.collectStatistics(statsCollection);
    }
  }

  @Override
  public @Nullable <T extends ConfigurableProgramAnalysis> T retrieveWrappedCpa(Class<T> type) {
    return argcpa.retrieveWrappedCpa(type);
  }

  @Override
  public Iterable<ConfigurableProgramAnalysis> getWrappedCPAs() {
    return argcpa.getWrappedCPAs();
  }
}
