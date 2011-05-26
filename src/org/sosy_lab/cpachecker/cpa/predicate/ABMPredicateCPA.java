package org.sosy_lab.cpachecker.cpa.predicate;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithABM;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.cpa.predicate.relevantpredicates.AuxiliaryComputer;
import org.sosy_lab.cpachecker.cpa.predicate.relevantpredicates.OccurrenceComputer;
import org.sosy_lab.cpachecker.cpa.predicate.relevantpredicates.RelevantPredicatesComputer;


/**
 * Implements an ABM-based predicate CPA.
 * @author dwonisch
 *
 */
@Options(prefix="cpa.predicate.abm")
public class ABMPredicateCPA extends PredicateCPA implements ConfigurableProgramAnalysisWithABM {
  
  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ABMPredicateCPA.class);
  }
  
  private final RelevantPredicatesComputer relevantPredicatesComputer;
  private final ABMPredicateReducer reducer;
  private final ABMPredicateTransferRelation transfer;
  private final ABMPredicatePrecisionAdjustment prec;

  @Option
  private boolean auxiliaryPredicateComputer = true;
   
  private ABMPredicateCPA(Configuration config, LogManager logger) throws InvalidConfigurationException {
    super(config, logger);
    
    config.inject(this, ABMPredicateCPA.class);
    
    if (auxiliaryPredicateComputer) {
      relevantPredicatesComputer = new AuxiliaryComputer();
    } else {
      relevantPredicatesComputer = new OccurrenceComputer(); 
    }
    
    reducer = new ABMPredicateReducer(this);
    transfer = new ABMPredicateTransferRelation(this);
    prec = new ABMPredicatePrecisionAdjustment(this);
  }
  
  @Override
  protected PredicateCPAStatistics createStatistics() throws InvalidConfigurationException {
    return new ABMPredicateCPAStatistics(this);
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
  public Reducer getReducer() {
    return reducer;
  }

  @Override
  public ABMPredicatePrecisionAdjustment getPrecisionAdjustment() {
    return prec;
  }
  
  public RelevantPredicatesComputer getRelevantPredicatesComputer() {
    return relevantPredicatesComputer;
  }

  @Override
  public ABMPredicateTransferRelation getTransferRelation() {
    return transfer;
  }
}
