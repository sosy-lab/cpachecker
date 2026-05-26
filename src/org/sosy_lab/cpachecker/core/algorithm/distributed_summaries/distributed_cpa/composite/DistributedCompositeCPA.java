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
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssBlockAnalysisStatistics;
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
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositePrecision;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;

public class DistributedCompositeCPA
    implements ForwardingDistributedConfigurableProgramAnalysis, WrapperCPA {

  private final LogManager logger;
  private final CompositeCPA compositeCPA;
  private final SerializeOperator serialize;
  private final DeserializeOperator deserialize;
  private final ProceedCompositeStateOperator proceed;

  private final DssBlockAnalysisStatistics statistics;

  private final DeserializeCompositePrecisionOperator deserializePrecisionOperator;
  private final SerializeCompositePrecisionOperator serializePrecisionOperator;
  private final ViolationConditionOperator verificationConditionOperator;
  private final CoverageOperator coverageOperator;
  private final CombineOperator combineOperator;
  private final CombinePrecisionOperator combinePrecisionOperator;

  private final ImmutableList<ConfigurableProgramAnalysis> wrappedCpas;

  public DistributedCompositeCPA(
      LogManager pLogger,
      CompositeCPA pCompositeCPA,
      BlockNode pNode,
      ImmutableMap<
              Class<? extends ConfigurableProgramAnalysis>, DistributedConfigurableProgramAnalysis>
          registered) {
    ImmutableList.Builder<ConfigurableProgramAnalysis> wrappedCpasBuilder = ImmutableList.builder();
    for (ConfigurableProgramAnalysis wrapped : pCompositeCPA.getWrappedCPAs()) {
      if (registered.containsKey(wrapped.getClass())) {
        wrappedCpasBuilder.add(Objects.requireNonNull(registered.get(wrapped.getClass())));
      } else {
        wrappedCpasBuilder.add(wrapped);
      }
    }
    wrappedCpas = wrappedCpasBuilder.build();

    logger = pLogger;
    statistics = new DssBlockAnalysisStatistics("DCPA-" + pNode.getId());
    compositeCPA = pCompositeCPA;
    serialize = new SerializeCompositeStateOperator(wrappedCpas, statistics);
    deserialize = new DeserializeCompositeStateOperator(wrappedCpas, pNode, statistics);
    proceed = new ProceedCompositeStateOperator(wrappedCpas, statistics);
    serializePrecisionOperator = new SerializeCompositePrecisionOperator(wrappedCpas);
    deserializePrecisionOperator = new DeserializeCompositePrecisionOperator(wrappedCpas, pNode);
    verificationConditionOperator = new CompositeViolationConditionOperator(wrappedCpas);
    coverageOperator = new CompositeStateCoverageOperator(wrappedCpas);
    combineOperator = new CombineCompositeStateOperator(wrappedCpas);
    combinePrecisionOperator = new CombineCompositePrecisionOperator(wrappedCpas);
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
  public boolean isMostGeneralBlockEntryState(AbstractState pAbstractState) {
    CompositeState composite = (CompositeState) pAbstractState;
    Preconditions.checkArgument(composite.getWrappedStates().size() == wrappedCpas.size());
    for (CpaAndState cpaAndState : zip(wrappedCpas, composite)) {
      if (cpaAndState.cpa() instanceof DistributedConfigurableProgramAnalysis dcpa) {
        Preconditions.checkState(dcpa.doesOperateOn(cpaAndState.state().getClass()));
        if (!dcpa.isMostGeneralBlockEntryState(cpaAndState.state())) {
          return false;
        }
      }
    }
    return true;
  }

  @Override
  public AbstractState reset(AbstractState pAbstractState) {
    ImmutableList.Builder<AbstractState> wrappedStates = ImmutableList.builder();
    for (CpaAndState cpaAndState : zip(wrappedCpas, (CompositeState) pAbstractState)) {
      if (cpaAndState.cpa() instanceof DistributedConfigurableProgramAnalysis dcpa) {
        Preconditions.checkState(dcpa.doesOperateOn(cpaAndState.state().getClass()));
        wrappedStates.add(dcpa.reset(cpaAndState.state()));
      } else {
        wrappedStates.add(cpaAndState.state());
      }
    }
    return new CompositeState(wrappedStates.build());
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
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    Preconditions.checkNotNull(node);
    ImmutableList.Builder<AbstractState> initialStates = ImmutableList.builder();
    for (ConfigurableProgramAnalysis cpa : wrappedCpas) {
      initialStates.add(cpa.getInitialState(node, partition));
    }
    return new CompositeState(initialStates.build());
  }

  public DssBlockAnalysisStatistics getStatistics() {
    return statistics;
  }

  record CpaAndState(ConfigurableProgramAnalysis cpa, AbstractState state) {}

  record CpaAndPrecision(ConfigurableProgramAnalysis cpa, Precision precision) {}

  static List<CpaAndPrecision> zip(
      List<ConfigurableProgramAnalysis> pCpas, CompositePrecision pPrecision) {
    Preconditions.checkArgument(pPrecision.getWrappedPrecisions().size() == pCpas.size());
    ImmutableList.Builder<CpaAndPrecision> cpasAndPrecisions =
        ImmutableList.builderWithExpectedSize(pCpas.size());
    for (int i = 0; i < pPrecision.getWrappedPrecisions().size(); i++) {
      Precision wrappedPrecision = pPrecision.getWrappedPrecisions().get(i);
      ConfigurableProgramAnalysis cpa = pCpas.get(i);
      cpasAndPrecisions.add(new CpaAndPrecision(cpa, wrappedPrecision));
    }
    return cpasAndPrecisions.build();
  }

  static List<CpaAndState> zip(List<ConfigurableProgramAnalysis> pCpas, CompositeState pState) {
    Preconditions.checkArgument(
        pState.getWrappedStates().size() == pCpas.size(),
        "Expected %s wrapped states, but got %s",
        pCpas.size(),
        pState.getWrappedStates().size());
    ImmutableList.Builder<CpaAndState> cpasAndStates =
        ImmutableList.builderWithExpectedSize(pCpas.size());
    for (int i = 0; i < pState.getWrappedStates().size(); i++) {
      AbstractState wrappedState = pState.getWrappedStates().get(i);
      ConfigurableProgramAnalysis cpa = pCpas.get(i);
      if (cpa instanceof DistributedConfigurableProgramAnalysis dcpa) {
        Preconditions.checkState(
            dcpa.doesOperateOn(wrappedState.getClass()),
            "CPA %s does not operate on state of class %s",
            cpa.getClass().getName(),
            wrappedState.getClass().getName());
        cpasAndStates.add(new CpaAndState(cpa, wrappedState));
      } else {
        cpasAndStates.add(new CpaAndState(cpa, wrappedState));
      }
    }
    return cpasAndStates.build();
  }

  @Override
  public @Nullable <T extends ConfigurableProgramAnalysis> T retrieveWrappedCpa(Class<T> type) {
    if (type.isAssignableFrom(getClass())) {
      return type.cast(this);
    } else {
      for (ConfigurableProgramAnalysis cpa : wrappedCpas) {
        if (type.isAssignableFrom(cpa.getClass())) {
          return type.cast(cpa);
        } else if (cpa instanceof WrapperCPA wrapperCPA) {
          T result = wrapperCPA.retrieveWrappedCpa(type);
          if (result != null) {
            return result;
          }
        }
      }
    }
    if (compositeCPA.retrieveWrappedCpa(type) != null) {
      logger.log(
          Level.FINE,
          "Requested to retrieve CPA type "
              + type
              + " from "
              + this.getClass()
              + " but does not exist in distributed versions of CPAs.");
    }
    return null;
  }

  @Override
  public Iterable<ConfigurableProgramAnalysis> getWrappedCPAs() {
    // The 'wrappedCpas' may contain some CPAs that were locally adjusted for DSS by wrapping them
    // in their corresponding Distributed*CPA version. For example, if compositeCPA contains a
    // PredicateCPA, then wrappedCpas will contain a DistributedPredicateCPA that wraps that
    // instance of the PredicateCPA. These Distributed*CPA versions may manage additional resources
    // that must be closed. So we can not delegate to compositeCPA to get the wrapped CPAs; instead,
    // we need to make sure to return the Distributed*CPA versions.
    return wrappedCpas;
  }
}
