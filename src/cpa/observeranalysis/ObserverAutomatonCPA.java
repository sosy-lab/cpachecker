package cpa.observeranalysis;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.logging.Level;

import java_cup.runtime.ComplexSymbolFactory;
import java_cup.runtime.Symbol;
import java_cup.runtime.SymbolFactory;
import cfa.objectmodel.CFAFunctionDefinitionNode;

import common.configuration.Configuration;
import common.configuration.Option;
import common.configuration.Options;

import cpa.common.LogManager;
import cpa.common.defaults.AbstractCPAFactory;
import cpa.common.defaults.EqualityPartialOrder;
import cpa.common.defaults.MergeSepOperator;
import cpa.common.defaults.StaticPrecisionAdjustment;
import cpa.common.defaults.StopSepOperator;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.CPAFactory;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.JoinOperator;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.PartialOrder;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.PrecisionAdjustment;
import cpa.common.interfaces.StopOperator;
import cpa.common.interfaces.TransferRelation;
import exceptions.CPAException;
import exceptions.InvalidConfigurationException;

/**
 * This class implements an ObserverAutomatonAnalysis as described in the related Documentation. 
 * @author rhein
 */
@Options(prefix="observerAnalysis")
public class ObserverAutomatonCPA implements ConfigurableProgramAnalysis {
  
  private static class ObserverAutomatonCPAFactory extends AbstractCPAFactory {
    
    @Override
    public ConfigurableProgramAnalysis createInstance() throws CPAException {
      return new ObserverAutomatonCPA(getConfiguration(), getLogger());
    }
  }
  
  public static CPAFactory factory() {
    return new ObserverAutomatonCPAFactory();
  }
  
  @Option(required=true)
  private String inputFile = "";
  
  private final ObserverAutomaton automaton;
  private final TransferRelation transferRelation;
  
  private static final ObserverDomain observerDomain = new ObserverDomain();
  private static final PartialOrder partialOrder = new EqualityPartialOrder(observerDomain);
  private static final StopOperator stopOperator = new StopSepOperator(partialOrder);
  private static final JoinOperator joinOperator = new JoinOperator() {
    @Override
    public AbstractElement join(AbstractElement pElement1,
                                AbstractElement pElement2) throws CPAException {
      if (pElement1 == pElement2) {
        return pElement1;
      } else {
        return ObserverState.TOP;
      }
    }
  };
  
  private static class ObserverDomain implements AbstractDomain {
    @Override
    public AbstractElement getTopElement() {
      return ObserverState.TOP;
    }
    
    @Override
    public PartialOrder getPartialOrder() {
      return partialOrder;
    }
    
    @Override
    public JoinOperator getJoinOperator() {
      return joinOperator; 
    }
    
    @Override
    public AbstractElement getBottomElement() {
      return ObserverState.BOTTOM;
    }
  };
  
  /**
   * Loads a ObserverAutomaton from the argument DefinitionFile.
   * The argument mergeType is ignored.
   * @param mergeType
   * @param pStopType
   * @throws FileNotFoundException
   * @throws InvalidConfigurationException 
   */
  private ObserverAutomatonCPA(Configuration config, LogManager logger) throws InvalidConfigurationException {
    config.inject(this);
    automaton = parseObserverFile(logger);
    logger.log(Level.FINEST, "Automaton", automaton.getName(), "loaded.");
    transferRelation = new ObserverTransferRelation(automaton, logger);
    logger.log(Level.FINER, "loaded the ObserverAutomaton \"" + automaton.getName() +"\"" );
  }
  
  private ObserverAutomaton parseObserverFile(LogManager pLogger) throws InvalidConfigurationException {
    SymbolFactory sf = new ComplexSymbolFactory();   
    try {
      FileInputStream input = new FileInputStream(inputFile);
      try {
        Symbol symbol = new ObserverParser(new ObserverScanner(input, sf),sf,pLogger).parse();
        return (ObserverAutomaton)symbol.value;
      } finally {
        input.close();
      }
    } catch (Exception e) {
      pLogger.logException(Level.FINER, e, "Could not load automaton from file " + inputFile);
      throw new InvalidConfigurationException("Could not load automaton from file " + inputFile
          + " (" + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()) + ")");
    } 
  }
  
  @Override
  public AbstractDomain getAbstractDomain() {
    return observerDomain;
  }

  @Override
  public AbstractElement getInitialElement(CFAFunctionDefinitionNode pNode) {
    return new ObserverState(automaton.getInitialVariables(), automaton.getInitialState());
  }

  @Override
  public Precision getInitialPrecision(CFAFunctionDefinitionNode pNode) {
    return null;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return MergeSepOperator.getInstance();
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return StaticPrecisionAdjustment.getInstance();
  }

  @Override
  public StopOperator getStopOperator() {
    return stopOperator;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transferRelation ;
  }

}
