package org.sosy_lab.cpachecker.cpa.formulaslicing;

import com.google.common.base.Function;
import com.google.common.base.Optional;

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
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.CachingPathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;


@Options(prefix="cpa.slicing")
public class FormulaSlicingCPA extends SingleEdgeTransferRelation
  implements
    ConfigurableProgramAnalysis,
    AbstractDomain,
    PrecisionAdjustment,
    StatisticsProvider, MergeOperator {

  @Option(secure=true,
      description="Cache formulas produced by path formula manager")
  private boolean useCachingPathFormulaManager = true;

  private final StopOperator stopOperator;
  private final IFormulaSlicingManager manager;
  private final MergeOperator mergeOperator;
  private final InductiveWeakeningManager inductiveWeakeningManager;
  private final RCNFManager rcnfManager;

  private FormulaSlicingCPA(
      Configuration pConfiguration,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      CFA cfa
  ) throws InvalidConfigurationException {
    pConfiguration.inject(this);

    Solver solver = Solver.create(pConfiguration, pLogger, pShutdownNotifier);
    FormulaManagerView formulaManager = solver.getFormulaManager();
    PathFormulaManager pathFormulaManager = new PathFormulaManagerImpl(
        formulaManager, pConfiguration, pLogger, pShutdownNotifier, cfa,
        AnalysisDirection.FORWARD);

    if (useCachingPathFormulaManager) {
      pathFormulaManager = new CachingPathFormulaManager(pathFormulaManager);
    }

    inductiveWeakeningManager = new InductiveWeakeningManager(pConfiguration, solver, pLogger,
        pShutdownNotifier);
    rcnfManager = new RCNFManager(formulaManager, pConfiguration);
    manager = new FormulaSlicingManager(
        pConfiguration,
        pathFormulaManager,
        formulaManager,
        cfa,
        inductiveWeakeningManager,
        rcnfManager,
        solver,
        pLogger);
    stopOperator = new StopSepOperator(this);
    mergeOperator = this;
  }

  @Override
  public Optional<PrecisionAdjustmentResult> postAdjustmentStrengthen(
      AbstractState result,
      Precision precision,
      Iterable<AbstractState> otherStates,
      Iterable<Precision> otherPrecisions,
      UnmodifiableReachedSet states,
      Function<AbstractState, AbstractState> stateProjection,
      AbstractState resultFullState) throws CPAException, InterruptedException {
    return Optional.of(PrecisionAdjustmentResult.create(result, precision, Action.CONTINUE));
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
    return null;
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
    return SingletonPrecision.getInstance();
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
    rcnfManager.collectStatistics(statsCollection);
  }

  @Override
  public AbstractState merge(AbstractState state1, AbstractState state2,
      Precision precision) throws CPAException, InterruptedException {
    return manager.merge((SlicingState) state1, (SlicingState) state2);
  }
}
