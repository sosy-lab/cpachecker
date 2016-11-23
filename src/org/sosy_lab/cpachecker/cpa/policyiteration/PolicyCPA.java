package org.sosy_lab.cpachecker.cpa.policyiteration;

import com.google.common.base.Function;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithBAM;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.conditions.AdjustableConditionCPA;
import org.sosy_lab.cpachecker.core.interfaces.conditions.ReachedSetAdjustingCPA;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.policyiteration.polyhedra.PolyhedraWideningManager;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.CachingPathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.templates.TemplatePrecision;
import org.sosy_lab.cpachecker.util.templates.TemplateToFormulaConversionManager;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Policy iteration CPA.
 */
@Options(prefix="cpa.lpi")
public class PolicyCPA extends SingleEdgeTransferRelation
    implements ConfigurableProgramAnalysisWithBAM,
               AdjustableConditionCPA,
               ReachedSetAdjustingCPA,
               StatisticsProvider,
               AbstractDomain,
               PrecisionAdjustment,
               MergeOperator,
               AutoCloseable {

  @Option(secure=true,
      description="Cache formulas produced by path formula manager")
  private boolean useCachingPathFormulaManager = true;

  private final Configuration config;
  private final PolicyIterationManager policyIterationManager;
  private final LogManager logger;
  private final PolicyIterationStatistics statistics;
  private final StopOperator stopOperator;
  private final Solver solver;
  private final FormulaManagerView fmgr;
  private final PathFormulaManager pfmgr;
  private final StateFormulaConversionManager stateFormulaConversionManager;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(PolicyCPA.class);
  }

  @SuppressWarnings("unused")
  private PolicyCPA(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier shutdownNotifier,
      CFA cfa)
      throws InvalidConfigurationException, CPAException {
    pConfig.inject(this);

    logger = pLogger;
    config = pConfig;

    solver = Solver.create(config, pLogger, shutdownNotifier);
    fmgr = solver.getFormulaManager();
    PathFormulaManager pathFormulaManager = new PathFormulaManagerImpl(
        fmgr, pConfig, pLogger, shutdownNotifier, cfa,
        AnalysisDirection.FORWARD);
    if (useCachingPathFormulaManager) {
      pathFormulaManager = new CachingPathFormulaManager(pathFormulaManager);
    }
    pfmgr = pathFormulaManager;

    statistics = new PolicyIterationStatistics(cfa);
    TemplateToFormulaConversionManager pTemplateToFormulaConversionManager =
        new TemplateToFormulaConversionManager(cfa, pLogger);
    stateFormulaConversionManager = new StateFormulaConversionManager(
            fmgr,
            pTemplateToFormulaConversionManager, pConfig, cfa,
            logger, shutdownNotifier, pfmgr, solver);
    ValueDeterminationManager valueDeterminationFormulaManager =
        new ValueDeterminationManager(
            config, fmgr, pLogger, pfmgr,
            stateFormulaConversionManager,
            pTemplateToFormulaConversionManager);
    FormulaLinearizationManager formulaLinearizationManager = new
        FormulaLinearizationManager(fmgr, statistics);
    PolyhedraWideningManager pPwm = new PolyhedraWideningManager(
        statistics, logger);

    policyIterationManager = new PolicyIterationManager(
        pConfig,
        fmgr,
        cfa, pfmgr,
        solver, pLogger, shutdownNotifier,
        valueDeterminationFormulaManager,
        statistics,
        formulaLinearizationManager,
        pPwm,
        stateFormulaConversionManager, pTemplateToFormulaConversionManager);
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
  public AbstractDomain getAbstractDomain() {
    return this;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return this;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return this;
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
  public Precision getInitialPrecision(CFANode node,
                                                StateSpacePartition pPartition) {
    return policyIterationManager.getInitialPrecision();
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

    return policyIterationManager.precisionAdjustment(
        (PolicyState)state,
        (TemplatePrecision) precision, states,
        fullState);
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState state,
      List<AbstractState> otherStates,
      CFAEdge cfaEdge,
      Precision precision)
      throws CPATransferException, InterruptedException {
    return policyIterationManager.strengthen(
        ((PolicyState) state).asIntermediate(),
        otherStates
    );
  }

  @Override
  public Optional<AbstractState> strengthen(
      AbstractState pState, Precision pPrecision,
      List<AbstractState> otherStates)
      throws CPAException, InterruptedException {
    return policyIterationManager.strengthen(
        (PolicyState) pState, (TemplatePrecision) pPrecision, otherStates);
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

  @Override
  public AbstractState merge(AbstractState state1, AbstractState state2,
      Precision precision) throws CPAException, InterruptedException {
    return policyIterationManager.merge(
        (PolicyState) state1, (PolicyState) state2
    );
  }

  @Override
  public Reducer getReducer() {
    return new PolicyReducer(policyIterationManager, fmgr, stateFormulaConversionManager, pfmgr);
  }

  @Override
  public void setPartitioning(BlockPartitioning pPartitioning) {
    policyIterationManager.setPartitioning(pPartitioning);
  }

  @Override
  public void close() {
    solver.close();
  }
}

