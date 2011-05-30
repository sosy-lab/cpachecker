package org.sosy_lab.cpachecker.cpa.abm;

import static com.google.common.base.Preconditions.checkState;

import java.util.Collection;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.cfa.blocks.builder.DelayedFunctionAndLoopPartitioning;
import org.sosy_lab.cpachecker.cfa.blocks.builder.FunctionAndLoopPartitioning;
import org.sosy_lab.cpachecker.cfa.blocks.builder.PartitioningHeuristic;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithABM;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.predicate.ABMPredicateCPA;


@Options(prefix="cpa.abm")
public class ABMCPA extends AbstractSingleWrapperCPA implements StatisticsProvider {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ABMCPA.class);
  }
  
  private BlockPartitioning blockPartitioning;

  private final LogManager logger;
  private final TimedReducer reducer;
  private final ABMTransferRelation transfer;
  private final ABMCPAStatistics stats;
  
  @Option
  private boolean delayDeclarations = false;
  
  public ABMCPA(ConfigurableProgramAnalysis pCpa, Configuration config, LogManager pLogger, ReachedSetFactory pReachedSetFactory) throws InvalidConfigurationException {
    super(pCpa);
    config.inject(this);

    logger = pLogger;
    
    if (!(pCpa instanceof ConfigurableProgramAnalysisWithABM)) {
      throw new InvalidConfigurationException("ABM needs CPAs that are capable for ABM");
    }
    Reducer wrappedReducer = ((ConfigurableProgramAnalysisWithABM)pCpa).getReducer();
    if (wrappedReducer == null) {
      throw new InvalidConfigurationException("ABM needs CPAs that are capable for ABM");
    }
    reducer = new TimedReducer(wrappedReducer);
    transfer = new ABMTransferRelation(config, logger, this, pReachedSetFactory);
    
    stats = new ABMCPAStatistics(this);
  }
  
  @Override
  public AbstractElement getInitialElement(CFANode node) {
    if (blockPartitioning == null) {
      blockPartitioning = getPartitioningHeuristic().buildPartitioning(node);
      transfer.setBlockPartitioning(blockPartitioning);
      ((AbstractSingleWrapperCPA) getWrappedCpa()).retrieveWrappedCpa(ABMPredicateCPA.class).getTransferRelation().setPartitioning(blockPartitioning);
    } else {
      assert blockPartitioning.getBlockForNode(node) != null : "CPA re-used for other CFA, this is currently not supported.";
    }
    return getWrappedCpa().getInitialElement(node);
  }
  
  @Override
  public Precision getInitialPrecision(CFANode pNode) {
    return getWrappedCpa().getInitialPrecision(pNode);
  }
  
  private PartitioningHeuristic getPartitioningHeuristic() {
    if(delayDeclarations) {
      return new DelayedFunctionAndLoopPartitioning(logger);
    } else {
      return new FunctionAndLoopPartitioning(logger);
    }
  }
  
  @Override
  public AbstractDomain getAbstractDomain() {
    return getWrappedCpa().getAbstractDomain();
  }
  
  @Override
  public MergeOperator getMergeOperator() {
    return getWrappedCpa().getMergeOperator();
  }
  
  @Override
  public StopOperator getStopOperator() {
    return getWrappedCpa().getStopOperator();
  }
  
  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return getWrappedCpa().getPrecisionAdjustment();
  }
  
  @Override
  public ABMTransferRelation getTransferRelation() {
    return transfer;
  }

  TimedReducer getReducer() {
    return reducer;
  }
  
  public BlockPartitioning getBlockPartitioning() {
    checkState(blockPartitioning != null);
    return blockPartitioning;
  }
  
  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
    super.collectStatistics(pStatsCollection);
  }
  
  ABMCPAStatistics getStatistics() {
    return stats;
  }
}
