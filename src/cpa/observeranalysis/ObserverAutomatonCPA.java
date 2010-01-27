package cpa.observeranalysis;

import java.io.File;
import java.io.FileNotFoundException;

import cmdline.CPAMain;

import java_cup.runtime.ComplexSymbolFactory;
import java_cup.runtime.Symbol;
import java_cup.runtime.SymbolFactory;
import cfa.objectmodel.CFAFunctionDefinitionNode;
import cpa.common.defaults.EqualityPartialOrder;
import cpa.common.defaults.MergeSepOperator;
import cpa.common.defaults.StaticPrecisionAdjustment;
import cpa.common.defaults.StopSepOperator;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.JoinOperator;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.PartialOrder;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.PrecisionAdjustment;
import cpa.common.interfaces.StopOperator;
import cpa.common.interfaces.TransferRelation;
import exceptions.CPAException;

/**
 * This class implements an ObserverAutomatonAnalysis as described in the related Documentation. 
 * @author rhein
 */
public class ObserverAutomatonCPA implements ConfigurableProgramAnalysis {
  private ObserverAutomaton automaton;
  private TransferRelation transferRelation;
  
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
   */
  public ObserverAutomatonCPA(String pMergeType, String pStopType) throws FileNotFoundException { 
    String file = CPAMain.cpaConfig.getProperty("observerAnalysis.inputFile");
    parseObserverFile(file);
  }
  
  private void parseObserverFile(String pFilename) throws FileNotFoundException {
    File f = new File(pFilename);
    try {
      SymbolFactory sf = new ComplexSymbolFactory();   
      Symbol symbol = new ObserverParser(new ObserverScanner(new java.io.FileInputStream(f), sf),sf).parse();
      automaton = (ObserverAutomaton)symbol.value;
      transferRelation = new ObserverTransferRelation(automaton);
      
      System.out.println("ObserverAutomatonAnalysis: Loaded the " + automaton.getName());
      
    } catch (Exception e) {
      System.err.println("ObserverAnalysis.ObserverParser: General Exception during Parsing of the inputfile " + pFilename);
      e.printStackTrace();
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
