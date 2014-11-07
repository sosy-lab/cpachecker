package org.sosy_lab.cpachecker.cpa.stator.memory;

import java.util.Collection;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.defaults.*;
import org.sosy_lab.cpachecker.core.interfaces.*;

import com.google.common.base.Preconditions;

/**
 * Explicit-value memory analysis.
 * Explicitly calculates the set each memory block can point to.
 */
@Options(prefix="cpa.memory_explicit")
public class ExplicitMemoryCPA implements ConfigurableProgramAnalysis, StatisticsProvider{

  private final AbstractDomain abstractDomain;
  private final ExplicitMemoryTransferRelation transferRelation;
  private final MergeOperator mergeOperator;
  private final StopOperator stopOperator;
  private final PrecisionAdjustment precisionAdjustment;
  private final ExplicitMemoryStatistics statistics;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ExplicitMemoryCPA.class);
  }

  @SuppressWarnings("unused")
  private ExplicitMemoryCPA(
      Configuration config,
      LogManager logger,
      ShutdownNotifier shutdownNotifier,
      CFA cfa
  ) throws InvalidConfigurationException {
    config.inject(this);

    Preconditions.checkState(cfa.getLanguage().equals(Language.C));

    statistics = new ExplicitMemoryStatistics();
    abstractDomain = DelegateAbstractDomain.<AliasState> getInstance();
    transferRelation = new ExplicitMemoryTransferRelation(
        cfa.getMachineModel(), logger, statistics);
    mergeOperator = new MergeJoinOperator(abstractDomain);
    stopOperator = new StopJoinOperator(abstractDomain);
    precisionAdjustment = StaticPrecisionAdjustment.getInstance();
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return abstractDomain;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transferRelation;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return mergeOperator;
  }

  @Override
  public StopOperator getStopOperator() {
    return stopOperator;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return precisionAdjustment;
  }

  @Override
  public AliasState getInitialState(CFANode node) {
    return AliasState.TOP;
  }

  @Override
  public Precision getInitialPrecision(CFANode node) {
    return SingletonPrecision.getInstance();
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(statistics);
  }
}
