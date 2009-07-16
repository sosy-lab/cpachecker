package cpa.common;

import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.RefinableCPA;
import cpa.common.interfaces.RefinementManager;
import exceptions.CPAException;

public class CPAWithRefinement {

  public ReachedElements CPAWithRefinementAlgorithm(ConfigurableProgramAnalysis cpa, 
      AbstractElementWithLocation initialElement,
      Precision initialPrecision) throws CPAException{
    ReachedElements reached = null;
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

      // if the element is an error element
      if(CPAAlgorithm.errorFound){
        System.out.println("here");
        RefinableCPA refinableCpa = (RefinableCPA)cpa;
        RefinementManager refinementManager = refinableCpa.getRefinementManager();

        assert(reached != null);
        stopAnalysis = !refinementManager.performRefinement(reached, null);
        if(stopAnalysis){
          System.out.println("ERROR FOUND");
        }
      }

      else {
        // TODO safe -- print reached elements
        stopAnalysis = true;
      }

    }
    return reached;
  }

}
