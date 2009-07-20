package cpa.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import cmdline.CPAMain;

import logging.CustomLogLevel;
import logging.LazyLogger;

import common.Pair;

import cpa.art.ARTElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.RefinableCPA;
import cpa.common.interfaces.RefinementManager;
import exceptions.CPAException;

public class CPAWithRefinement {
  
  private final int GC_PERIOD = 100;
  private int gcCounter = 0;

  public ReachedElements CPAWithRefinementAlgorithm(ConfigurableProgramAnalysis cpa, 
      AbstractElementWithLocation initialElement,
      Precision initialPrecision) throws CPAException{
    ReachedElements reached = null;
    boolean stopAnalysis = false;
    while(!stopAnalysis){
      CPAAlgorithm algo = new CPAAlgorithm(cpa, initialElement, initialPrecision);
      try {
        reached = algo.CPA();
      } catch (CPAException e) {
        e.printStackTrace();
      }

      if(!(cpa instanceof RefinableCPA)) {
        throw new CPAException();
      }

      // if the element is an error element
      if(CPAAlgorithm.errorFound){
        RefinableCPA refinableCpa = (RefinableCPA)cpa;
        RefinementManager refinementManager = refinableCpa.getRefinementManager();

        assert(reached != null);
        RefinementOutcome refout = refinementManager.performRefinement(reached, null);
        stopAnalysis = !refout.refinementPerformed();

        if(stopAnalysis){
          System.out.println("ERROR FOUND");
        }
        else{
          modifySets(algo, refout.getToUnreach(), refout.getToWaitlist());
        }
      }

      else {
        // TODO safe -- print reached elements
        System.out.println("ERROR label NOT reached");
        System.out.println("_______________________");
        stopAnalysis = true;
      }

    }
    return reached;
  }
  
  private void modifySets(CPAAlgorithm pAlgorithm,
      Collection<ARTElement> reachableToUndo,
      Collection<ARTElement> toWaitlist) {
    
    Collection<Pair<AbstractElementWithLocation, Precision>> reachedSet = 
      pAlgorithm.getReachedElements().getReached();
    
    List<Pair<AbstractElementWithLocation, Precision>> waitlist = 
      pAlgorithm.getWaitlist();
    
    List<Pair<AbstractElementWithLocation, Precision>> lToWaitlist = new ArrayList<Pair<AbstractElementWithLocation, Precision>>(toWaitlist.size());
    Map<AbstractElementWithLocation, Pair<AbstractElementWithLocation, Precision>> lNewWaitToPrecision = new HashMap<AbstractElementWithLocation, Pair<AbstractElementWithLocation, Precision>>();

    LazyLogger.log(CustomLogLevel.SpecificCPALevel,
    "Performing refinement");
    // remove from reached all the elements in reachableToUndo
    Collection<Pair<AbstractElementWithLocation, Precision>> newreached =
      new LinkedList<Pair<AbstractElementWithLocation, Precision>>();
    for (Pair<AbstractElementWithLocation, Precision> e : reachedSet) {

      if (toWaitlist.contains(e.getFirst())) {
        lNewWaitToPrecision.put(e.getFirst(), e);
      }

      if (!reachableToUndo.contains(e.getFirst())) {
        newreached.add(e);
      } else {
        LazyLogger.log(CustomLogLevel.SpecificCPALevel,
            "Removing element: ", e.getFirst(), " from reached");
        if (waitlist.remove(e)) {
          LazyLogger.log(CustomLogLevel.SpecificCPALevel,
              "Removing element: ", e.getFirst(),
          " also from waitlist");
        }
      }
    }

    for (AbstractElementWithLocation w : toWaitlist) {
      if (lNewWaitToPrecision.containsKey(w)) {
        lToWaitlist.add(lNewWaitToPrecision.get(w));
      } else {
        // TODO no precision information from toWaitlist available, setting to null
        Pair<AbstractElementWithLocation, Precision> e = new Pair<AbstractElementWithLocation, Precision>(w, null);
        lToWaitlist.add(e);
        newreached.add(e);
      }
    }   

    reachedSet.clear();
    reachedSet.addAll(newreached);
    LazyLogger.log(CustomLogLevel.SpecificCPALevel,
        "Reached now is: ", newreached);
    // and add to the wait list all the elements in toWaitlist
    boolean useBfs = CPAMain.cpaConfig.getBooleanValue("analysis.bfs");

    LazyLogger.log(CustomLogLevel.SpecificCPALevel, "Adding elements: ", lToWaitlist, " to waitlist");

    if (useBfs) {
      waitlist.addAll(lToWaitlist);
    }
    else {
      waitlist.addAll(0, lToWaitlist);
    }

    LazyLogger.log(CustomLogLevel.SpecificCPALevel,
        "Waitlist now is: ", waitlist);
    LazyLogger.log(CustomLogLevel.SpecificCPALevel,
    "Refinement done");

    if ((++gcCounter % GC_PERIOD) == 0) {
      System.gc();
      gcCounter = 0;
    }
  }

}
