package de.upb.agw.cpachecker.cpa.abm.predicate;

import java.util.Collection;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractDomain;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractElement;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecisionAdjustment;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateRefinementManager;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;

import de.upb.agw.cpachecker.cpa.abm.heuristics.CachedSubtreeHeuristic;
import de.upb.agw.cpachecker.cpa.abm.heuristics.DelayedFunctionAndLoopCacher;
import de.upb.agw.cpachecker.cpa.abm.heuristics.FunctionAndLoopCacher;
import de.upb.agw.cpachecker.cpa.abm.util.CachedSubtreeManager;
import de.upb.agw.cpachecker.cpa.abm.util.RelevantPredicatesComputer;
import de.upb.agw.cpachecker.cpa.abm.util.impl.AuxiliaryComputer;
import de.upb.agw.cpachecker.cpa.abm.util.impl.OccurrenceComputer;

/**
 * Implements an ABM-based predicate CPA.
 * @author dwonisch
 *
 */
@Options(prefix="cpa.predicate.abm")
public class ABMPredicateCPA extends PredicateCPA {
  
  private CachedSubtreeManager manager;
  private ABMPCPAStatistics stats;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ABMPredicateCPA.class);
  }
  
  private ABMPTransferRelation transfer;  
  private ABMPPrecisionAdjustment prec;
  private ABMPAbstractDomain domain;
  private StopSepOperator stop;
  
  @Option
  private boolean delayDeclarations = false;
  @Option
  private boolean auxiliaryPredicateComputer = true;
   
  private ABMPredicateCPA(Configuration config, LogManager logger) throws InvalidConfigurationException {
    super(config, logger);
    
    config.inject(this, ABMPredicateCPA.class);
    
    transfer = new ABMPTransferRelation(this); 
    prec = new ABMPPrecisionAdjustment(this);
    stats = new ABMPCPAStatistics(this);
    domain = new ABMPAbstractDomain(this);    
    stop = new StopSepOperator(domain);
    manager = null;
  }
  
  public void setAlgorithm(Algorithm algorithm) {
    transfer.setAlgorithm(algorithm);
  }
  
  @Override
  public ABMPTransferRelation getTransferRelation() {
    return transfer;
  }  
  
  @Override
  public PredicatePrecisionAdjustment getPrecisionAdjustment() {
    return prec;
  }
  
  @Override
  public PredicateAbstractDomain getAbstractDomain() {
    return domain;
  }  

  @Override
  public StopOperator getStopOperator() {
    return stop;
  }
  
  @Override
  protected Configuration getConfiguration() {
    return super.getConfiguration();
  }

  @Override
  public LogManager getLogger() {
    return super.getLogger();
  }
  
  @Override
  public AbstractElement getInitialElement(CFANode node) {
    if(manager == null && node.getFunctionName().equalsIgnoreCase("main")) {
      manager = getCachedSubtreeHeuristic().buildMananger(node);
      transfer.setCachedSubtreeManager(manager);
      prec.setPredicateTransferRelation(transfer);
    }
    PredicateAbstractElement result = (PredicateAbstractElement)super.getInitialElement(node); 
    return new PredicateAbstractElement(result.getPathFormula(), new AbstractionFormula(result.getAbstractionFormula().asRegion(), result.getAbstractionFormula().asFormula(), result.getAbstractionFormula().getBlockFormula()));
  }
  
  @SuppressWarnings("unchecked")
  @Override
  public PredicateRefinementManager<Integer, Integer> getPredicateManager() {
    return (PredicateRefinementManager<Integer, Integer>)super.getPredicateManager();
  }
  
  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    super.collectStatistics(pStatsCollection);
    pStatsCollection.add(stats);
  }
  
  ABMPCPAStatistics getFCCPStats() {
    return stats;
  }
  
  private CachedSubtreeHeuristic getCachedSubtreeHeuristic() {
    if(delayDeclarations) {
      return new DelayedFunctionAndLoopCacher(getLogger());
    } else {
      return new FunctionAndLoopCacher(getLogger());
    }
  }

  public RelevantPredicatesComputer getRelevantPredicatesComputer() {
    if(auxiliaryPredicateComputer ) {
      return new AuxiliaryComputer();
    } else {
     return new OccurrenceComputer(); 
    }
  }
}
