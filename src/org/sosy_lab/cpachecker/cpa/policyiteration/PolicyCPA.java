package org.sosy_lab.cpachecker.cpa.policyiteration;

import java.util.Collection;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithBAM;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.conditions.AdjustableConditionCPA;
import org.sosy_lab.cpachecker.core.interfaces.conditions.ReachedSetAdjustingCPA;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.policyiteration.polyhedra.PolyhedraWideningManager;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.CachingPathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.templates.TemplatePrecision;
import org.sosy_lab.cpachecker.util.templates.TemplateToFormulaConversionManager;

/**
 * Policy iteration CPA.
 */
@Options(prefix="cpa.lpi")
public class PolicyCPA
    implements ConfigurableProgramAnalysisWithBAM,
               AdjustableConditionCPA,
               ReachedSetAdjustingCPA,
               StatisticsProvider,
               AutoCloseable {

  @Option(secure=true,
      description="Cache formulas produced by path formula manager")
  private boolean useCachingPathFormulaManager = true;

  private final Configuration config;
  private final PolicyIterationManager policyIterationManager;
  private final LogManager logger;
  private final PolicyIterationStatistics statistics;
  private final Solver solver;
  private final FormulaManagerView fmgr;
  private final PathFormulaManager pfmgr;
  private final StateFormulaConversionManager stateFormulaConversionManager;
  private final TemplatePrecision templatePrecision;

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
    templatePrecision = new TemplatePrecision(
        logger, config, cfa, pTemplateToFormulaConversionManager
    );

    policyIterationManager = new PolicyIterationManager(
        pConfig,
        fmgr,
        cfa,
        pfmgr,
        solver,
        pLogger,
        shutdownNotifier,
        valueDeterminationFormulaManager,
        statistics,
        formulaLinearizationManager,
        pPwm,
        stateFormulaConversionManager,
        pTemplateToFormulaConversionManager);
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition pPartition) {
    return PolicyAbstractedState.empty(
        node,
        fmgr.getBooleanFormulaManager().makeTrue(), stateFormulaConversionManager);
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return new PolicyDomain();
  }

  @Override
  public TransferRelation getTransferRelation() {
    return new PolicyTransferRelation(pfmgr, stateFormulaConversionManager);
  }

  @Override
  public MergeOperator getMergeOperator() {
    return new PolicyMergeOperator(pfmgr);
  }

  @Override
  public StopOperator getStopOperator() {
    return new StopSepOperator(getAbstractDomain());
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return policyIterationManager;
  }

  @Override
  public Precision getInitialPrecision(CFANode node, StateSpacePartition pPartition) {
    return templatePrecision;
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(statistics);
  }

  @Override
  public boolean adjustPrecision() {
    return templatePrecision.adjustPrecision();
  }

  boolean injectPrecisionFromInterpolant(CFANode pNode, Set<String> vars) {
    return templatePrecision.injectPrecisionFromInterpolant(pNode, vars);
  }

  @Override
  public void adjustReachedSet(ReachedSet pReachedSet) {
    pReachedSet.clear();
  }

  LogManager getLogger() {
    return logger;
  }

  Configuration getConfig() {
    return config;
  }

  Solver getSolver() {
    return solver;
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

