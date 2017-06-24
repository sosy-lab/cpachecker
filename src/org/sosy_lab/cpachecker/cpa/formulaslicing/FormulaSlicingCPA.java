package org.sosy_lab.cpachecker.cpa.formulaslicing;

import com.google.common.base.Function;
import java.util.Collection;
import java.util.Optional;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
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
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.RCNFManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.CachingPathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.predicates.weakening.InductiveWeakeningManager;


public class FormulaSlicingCPA extends SingleEdgeTransferRelation
  implements
    ConfigurableProgramAnalysis,
    AbstractDomain,
    PrecisionAdjustment,
    StatisticsProvider,
    MergeOperator,
    AutoCloseable {

  private final StopOperator stopOperator;
  private final IFormulaSlicingManager manager;
  private final MergeOperator mergeOperator;
  private final InductiveWeakeningManager inductiveWeakeningManager;
  private final RCNFManager RCNFManager;
  private final Solver solver;

  private FormulaSlicingCPA(
      Configuration pConfiguration,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      CFA cfa
  ) throws InvalidConfigurationException {
    solver = Solver.create(pConfiguration, pLogger, pShutdownNotifier);
    FormulaManagerView formulaManager = solver.getFormulaManager();
    PathFormulaManager origPathFormulaManager = new PathFormulaManagerImpl(
        formulaManager, pConfiguration, pLogger, pShutdownNotifier, cfa,
        AnalysisDirection.FORWARD);

    CachingPathFormulaManager pathFormulaManager = new CachingPathFormulaManager
        (origPathFormulaManager);

    inductiveWeakeningManager = new InductiveWeakeningManager(pConfiguration, solver, pLogger,
        pShutdownNotifier);
    RCNFManager = new RCNFManager(pConfiguration);
    manager = new FormulaSlicingManager(
        pConfiguration,
        pathFormulaManager,
        formulaManager,
        cfa,
        inductiveWeakeningManager,
        RCNFManager,
        solver,
        pLogger);
    stopOperator = new StopSepOperator(this);
    mergeOperator = this;
  }

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(FormulaSlicingCPA.class);
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
  public AbstractState getInitialState(CFANode node,
      StateSpacePartition partition) {
    return manager.getInitialState(node);
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState state, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {
    return manager.getAbstractSuccessors((SlicingState)state, cfaEdge);
  }

  @Override
  public AbstractState join(AbstractState state1, AbstractState state2)
      throws CPAException, InterruptedException {
    throw new UnsupportedOperationException("FormulaSlicingCPA should be used" +
     " with its own merge operator");
  }

  @Override
  public boolean isLessOrEqual(AbstractState state1, AbstractState state2)
      throws CPAException, InterruptedException {
    return manager.isLessOrEqual((SlicingState)state1,
        (SlicingState)state2);
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(AbstractState state,
      Precision precision, UnmodifiableReachedSet states,
      Function<AbstractState, AbstractState> stateProjection,
      AbstractState fullState) throws CPAException, InterruptedException {
    return manager.prec((SlicingState) state,  states, fullState);
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    manager.collectStatistics(statsCollection);
    inductiveWeakeningManager.collectStatistics(statsCollection);
    RCNFManager.collectStatistics(statsCollection);
  }

  @Override
  public AbstractState merge(AbstractState state1, AbstractState state2,
      Precision precision) throws CPAException, InterruptedException {
    return manager.merge((SlicingState) state1, (SlicingState) state2);
  }

  @Override
  public void close() {
    solver.close();
  }
}
