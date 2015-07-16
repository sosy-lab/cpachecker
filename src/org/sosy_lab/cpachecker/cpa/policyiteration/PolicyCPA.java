package org.sosy_lab.cpachecker.cpa.policyiteration;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.conditions.AdjustableConditionCPA;
import org.sosy_lab.cpachecker.core.interfaces.conditions.ReachedSetAdjustingCPA;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.policyiteration.congruence.CongruenceManager;
import org.sosy_lab.cpachecker.cpa.policyiteration.polyhedra.PolyhedraWideningManager;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.CachingPathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;

import com.google.common.base.Function;
import com.google.common.base.Optional;

/**
 * New version of policy iteration, now with path focusing.
 */
@Options(prefix="cpa.stator.policy")
public class PolicyCPA extends SingleEdgeTransferRelation
    implements ConfigurableProgramAnalysis,
               StatisticsProvider,
               AbstractDomain,
               PrecisionAdjustment,
               AdjustableConditionCPA,
               ReachedSetAdjustingCPA {

  @Option(secure=true,
      description="Cache formulas produced by path formula manager")
  private boolean useCachingPathFormulaManager = true;

  @Option(secure=true,
    description="Whether to join states on merge (leads to cycles in ARG)")
  private boolean joinOnMerge = true;

  private final MergeOperator mergeOperator;
  private final StopOperator stopOperator;
  private final PolicyIterationStatistics statistics;
  private final IPolicyIterationManager policyIterationManager;
  private final LogManager logger;
  private final Configuration config;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(PolicyCPA.class);
  }

  @SuppressWarnings("unused")
  private PolicyCPA(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier shutdownNotifier,
      CFA cfa
  ) throws InvalidConfigurationException {
    pConfig.inject(this);

    logger = pLogger;
    config = pConfig;

    FormulaManagerFactory formulaManagerFactory = new FormulaManagerFactory(
        pConfig, pLogger, shutdownNotifier);

    FormulaManager realFormulaManager = formulaManagerFactory.getFormulaManager();
    FormulaManagerView formulaManager = new FormulaManagerView(
        formulaManagerFactory, pConfig, pLogger);
    Solver solver = new Solver(formulaManager, formulaManagerFactory, pConfig, pLogger);
    PathFormulaManager pathFormulaManager = new PathFormulaManagerImpl(
        formulaManager, pConfig, pLogger, shutdownNotifier, cfa,
        AnalysisDirection.FORWARD);

    if (useCachingPathFormulaManager) {
      pathFormulaManager = new CachingPathFormulaManager(
          pathFormulaManager
      );
    }

    statistics = new PolicyIterationStatistics(pConfig);
    TemplateManager templateManager = new TemplateManager(pLogger, pConfig, cfa,
        statistics);
    ValueDeterminationManager valueDeterminationFormulaManager =
        new ValueDeterminationManager(
            formulaManager, pLogger, templateManager, pathFormulaManager);
    FormulaLinearizationManager formulaLinearizationManager = new
        FormulaLinearizationManager(
          realFormulaManager.getUnsafeFormulaManager(),
          formulaManager.getBooleanFormulaManager(),
          formulaManager,
        formulaManager.getIntegerFormulaManager());
    CongruenceManager pCongruenceManager = new CongruenceManager(
        pConfig,
        solver, templateManager,
        formulaManager,
        statistics, pathFormulaManager);
    PolyhedraWideningManager pPwm = new PolyhedraWideningManager(
        statistics, logger);
    policyIterationManager = new PolicyIterationManager(
        pConfig,
        formulaManager,
        cfa, pathFormulaManager,
        solver, pLogger, shutdownNotifier,
        templateManager, valueDeterminationFormulaManager,
        statistics,
        formulaLinearizationManager, pCongruenceManager, joinOnMerge, pPwm);
    mergeOperator = new PolicyMergeOperator(policyIterationManager, joinOnMerge);
    stopOperator = new StopSepOperator(this);
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition pPartition) {
    return policyIterationManager.getInitialState(node);
  }

  @Override
  public AbstractState join(AbstractState state1, AbstractState state2)
      throws CPAException, InterruptedException {
    throw new UnsupportedOperationException("PolicyCPA should be used with its"
        + " own merge operator");
  }

  /**
   * We only keep one abstract state per node.
   * {@code #isLessOrEqual} is called after the merge, but as our
   * merge is always joining two states {@code #isLessOrEqual} should
   * always return {@code true}.
   */
  @Override
  public boolean isLessOrEqual(AbstractState state1, AbstractState state2)
      throws CPAException, InterruptedException {
    return policyIterationManager.isLessOrEqual(
        (PolicyState) state1, (PolicyState) state2
    );
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState,
      Precision pPrecision,
      CFAEdge pEdge
  ) throws CPATransferException, InterruptedException {
  return policyIterationManager.getAbstractSuccessors(
      (PolicyState) pState, pEdge
  );
}

  @Override
  public Collection<? extends AbstractState> strengthen(AbstractState state,
      List<AbstractState> otherStates, @Nullable CFAEdge cfaEdge,
      Precision precision) throws CPATransferException, InterruptedException {
    return policyIterationManager.strengthen(
        (PolicyState)state, otherStates, cfaEdge);
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return this;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return this;
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
    return this;
  }

  @Override
  public PolicyPrecision getInitialPrecision(CFANode node,
      StateSpacePartition pPartition) {
    return PolicyPrecision.empty();
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(statistics);
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(
      AbstractState state,
      Precision precision,
      UnmodifiableReachedSet states,
      Function<AbstractState, AbstractState> projection,
      AbstractState fullState) throws CPAException, InterruptedException {

    return policyIterationManager.prec((PolicyState)state,
        (PolicyPrecision)precision, states,
        fullState);
  }

  @Override
  public boolean adjustPrecision() {
    return policyIterationManager.adjustPrecision();
  }

  @Override
  public void adjustReachedSet(ReachedSet pReachedSet) {
    policyIterationManager.adjustReachedSet(pReachedSet);
  }

  public LogManager getLogger() {
    return logger;
  }

  public Configuration getConfig() {
    return config;
  }
}

