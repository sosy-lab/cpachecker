package cpa.common;

import java.util.Collection;

import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.RefinableCPA;
import cpa.common.interfaces.RefinementManager;
import exceptions.CPAException;

public class CPAWithRefinement {

  public void CPAWithRefinementAlgorithm(ConfigurableProgramAnalysis cpa, 
      AbstractElementWithLocation initialElement,
      Precision initialPrecision) throws CPAException{
    Collection<AbstractElementWithLocation> reached = null;
    boolean stopAnalysis = false;
    while(!stopAnalysis){
      CPAAlgorithm algo = new CPAAlgorithm();
      try {
        reached = algo.CPA(cpa, initialElement, initialPrecision);
      } catch (CPAException e) {
        e.printStackTrace();
      }
      
      if(!(cpa instanceof RefinableCPA)) {
        throw new CPAException();
      }
      
      RefinableCPA refinableCpa = (RefinableCPA)cpa;
      RefinementManager refinementManager = refinableCpa.getRefinementManager();
      
      assert(reached != null);
      stopAnalysis = refinementManager.performRefinement(reached);
     
      
      
    }

  }

}
