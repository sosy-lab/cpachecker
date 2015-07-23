package org.sosy_lab.cpachecker.cpa.formulaslicing;

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
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
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


@Options(prefix="cpa.slicing")
public class FormulaSlicingCPA extends SingleEdgeTransferRelation
  implements
    ConfigurableProgramAnalysis,
    AbstractDomain,
    PrecisionAdjustment,
    StatisticsProvider {

  @Option(secure=true,
      description="Cache formulas produced by path formula manager")
  private boolean useCachingPathFormulaManager = true;

  @Option(secure=true,
      description="Whether to join states on merge (leads to cycles in ARG)")
  private boolean joinOnMerge = true;

  private final StopOperator stopOperator;
  private final IFormulaSlicingManager manager;
  private final MergeOperator mergeOperator;
  private FormulaSlicingStatistics statistics;

  private FormulaSlicingCPA(
      Configuration pConfiguration,
      LogManager pLogger,
      ShutdownNotifier shutdownNotifier,
      CFA cfa
  ) throws InvalidConfigurationException {
    pConfiguration.inject(this);

    statistics = new FormulaSlicingStatistics();
    FormulaManagerFactory formulaManagerFactory = new FormulaManagerFactory(
        pConfiguration, pLogger, shutdownNotifier);

    FormulaManager realFormulaManager = formulaManagerFactory.getFormulaManager();
    FormulaManagerView formulaManager = new FormulaManagerView(
        formulaManagerFactory, pConfiguration, pLogger);
    Solver solver = new Solver(formulaManager, formulaManagerFactory,
        pConfiguration, pLogger);
    PathFormulaManager pathFormulaManager = new PathFormulaManagerImpl(
        formulaManager, pConfiguration, pLogger, shutdownNotifier, cfa,
        AnalysisDirection.FORWARD);

    if (useCachingPathFormulaManager) {
      pathFormulaManager = new CachingPathFormulaManager(pathFormulaManager);
    }

    LoopTransitionFinder ltf = new LoopTransitionFinder(
        pConfiguration, cfa, pathFormulaManager, formulaManager, pLogger);

    InductiveWeakeningManager pInductiveWeakeningManager =
        new InductiveWeakeningManager(pConfiguration,
        formulaManager, solver, realFormulaManager.getUnsafeFormulaManager(),
            pLogger);
    manager = new FormulaSlicingManager(
        pConfiguration,
        pathFormulaManager, formulaManager, pLogger, cfa, ltf,
        pInductiveWeakeningManager, solver,
        statistics);
    stopOperator = new StopSepOperator(this);
    mergeOperator = new SlicingMergeOperator(manager, joinOnMerge);
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
  public Collection<? extends AbstractState> strengthen(AbstractState state,
      List<AbstractState> otherStates, @Nullable CFAEdge cfaEdge,
      Precision precision) throws CPATransferException, InterruptedException {
    return manager.strengthen((SlicingState)state,
        otherStates, cfaEdge);
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
  public Precision getInitialPrecision(CFANode node,
      StateSpacePartition partition) {
    // At the moment, precision is not used for formula slicing.
    return new Precision() {};
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
    statsCollection.add(statistics);
  }
}
