package org.sosy_lab.cpachecker.cpa.stator.policy;

import java.util.Collection;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;

/**
 * Configurable-Program-Analysis implementation for policy iteration.
 */
@Options(prefix="cpa.policy")
public class PolicyCPA implements ConfigurableProgramAnalysis, StatisticsProvider {
  private final PolicyAbstractDomain abstractDomain;
  private final TransferRelation transferRelation;
  private final PolicyMergeOperator mergeOperator;
  private final StopOperator stopOperator;
  private final PrecisionAdjustment precisionAdjustment;
  private final PolicyIterationStatistics statistics;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(PolicyCPA.class);
  }

  @SuppressWarnings("unused")
  private PolicyCPA(
        Configuration config,
        LogManager logger,
        ShutdownNotifier shutdownNotifier,
        CFA cfa
        ) throws InvalidConfigurationException {
    config.inject(this);

    FormulaManagerFactory formulaManagerFactory = new FormulaManagerFactory(config, logger, shutdownNotifier);

    FormulaManager realFormulaManager = formulaManagerFactory.getFormulaManager();
    FormulaManagerView formulaManager = new FormulaManagerView(realFormulaManager, config, logger);
    PathFormulaManager pathFormulaManager = new PathFormulaManagerImpl(
        formulaManager, config, logger, shutdownNotifier, cfa, AnalysisDirection.FORWARD);
    FreshVariableManager freshVariableManager = new FreshVariableManager(
        formulaManager.getRationalFormulaManager(),
        formulaManager.getBooleanFormulaManager());
    LinearConstraintManager lcmgr = new LinearConstraintManager(formulaManager, logger, freshVariableManager);
    ValueDeterminationFormulaManager valueDeterminationFormulaManager =
        new ValueDeterminationFormulaManager(
            pathFormulaManager, formulaManager, config, logger,
        cfa,
        realFormulaManager,
        lcmgr
    );

    statistics = new PolicyIterationStatistics(config);
    abstractDomain = new PolicyAbstractDomain(
        config,
        valueDeterminationFormulaManager,
        formulaManager,
        formulaManagerFactory,
        logger,
        lcmgr,
        shutdownNotifier,
        statistics
    );

    mergeOperator = new PolicyMergeOperator(abstractDomain, statistics);
    stopOperator = new StopSepOperator(abstractDomain);
    precisionAdjustment = new PolicyPrecisionAdjustment();
    transferRelation = new PolicyTransferRelation(
        config, formulaManager, formulaManagerFactory, pathFormulaManager,
        logger, lcmgr, new TemplateManager(config), statistics);
  }

  @Override
  public AbstractState getInitialState(CFANode node) {
    return PolicyAbstractState.withEmptyState(node);
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
  public Precision getInitialPrecision(CFANode node) {
    return SingletonPrecision.getInstance();
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(statistics);
  }
}
