package org.sosy_lab.cpachecker.cpa.abm;

import static com.google.common.base.Preconditions.checkState;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
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
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.cpa.predicate.ABMPredicateCPA;

import de.upb.agw.cpachecker.cpa.abm.heuristics.CachedSubtreeHeuristic;
import de.upb.agw.cpachecker.cpa.abm.heuristics.DelayedFunctionAndLoopCacher;
import de.upb.agw.cpachecker.cpa.abm.heuristics.FunctionAndLoopCacher;
import de.upb.agw.cpachecker.cpa.abm.util.CachedSubtreeManager;

@Options(prefix="cpa.abm")
public class ABMCPA extends AbstractSingleWrapperCPA {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ABMCPA.class);
  }
  
  private CachedSubtreeManager manager;

  private final LogManager logger;
  private final TimedReducer reducer;
  private final ABMTransferRelation transfer;
  
  @Option
  private boolean delayDeclarations = false;
  
  public ABMCPA(ConfigurableProgramAnalysis pCpa, Configuration config, LogManager pLogger) throws InvalidConfigurationException {
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
    transfer = new ABMTransferRelation(config, logger, this);
    
    ((AbstractSingleWrapperCPA) getWrappedCpa()).retrieveWrappedCpa(ABMPredicateCPA.class).getPrecisionAdjustment().setTransferRelation(transfer);
  }
  
  @Override
  public AbstractElement getInitialElement(CFANode node) {
    if(manager == null && node.getFunctionName().equalsIgnoreCase("main")) {
      manager = getCachedSubtreeHeuristic().buildMananger(node);
      transfer.setCachedSubtreeManager(manager);
      ((AbstractSingleWrapperCPA) getWrappedCpa()).retrieveWrappedCpa(ABMPredicateCPA.class).getTransferRelation().setCsmgr(manager);
    }
    return getWrappedCpa().getInitialElement(node);
  }
  
  @Override
  public Precision getInitialPrecision(CFANode pNode) {
    return getWrappedCpa().getInitialPrecision(pNode);
  }
  
  private CachedSubtreeHeuristic getCachedSubtreeHeuristic() {
    if(delayDeclarations) {
      return new DelayedFunctionAndLoopCacher(logger);
    } else {
      return new FunctionAndLoopCacher(logger);
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
  
  public CachedSubtreeManager getCachedSubtreeManager() {
    checkState(manager != null);
    return manager;
  }
}
