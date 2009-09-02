package cpa.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import symbpredabstraction.BDDMathsatSymbPredAbstractionAbstractManager;

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
import cpa.symbpredabs.SSAMap;
import cpa.symbpredabsCPA.SymbPredAbsMergeOperator;
import cpa.symbpredabsCPA.SymbPredAbsRefinementManager;
import cpa.symbpredabsCPA.SymbPredAbsTransferRelation;
import exceptions.CPAException;

public class CPAWithRefinement {
  
  private final int GC_PERIOD = 100;
  private int gcCounter = 0;
private static long modifySetsTime=0;
public static long totalfindArtTime =0;
private static long refinementTime = 0;
private static long part1 = 0;
private static long part2 = 0;
private static long part3 = 0;
private static long part4 = 0;
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
        long startRef = System.currentTimeMillis();
        RefinementOutcome refout = refinementManager.performRefinement(reached, null);
        long endRef = System.currentTimeMillis();
        refinementTime = refinementTime + (endRef  - startRef);
        stopAnalysis = !refout.refinementPerformed();

        if(stopAnalysis){
          System.out.println("ERROR FOUND");
          System.out.println("_______________________");
        }
        else{
          long start = System.currentTimeMillis();
          modifySets(algo, refout.getToUnreach(), refout.getToWaitlist());
          long end = System.currentTimeMillis();
          modifySetsTime = modifySetsTime + (end - start);
        }
      }

      else {
        // TODO safe -- print reached elements
        System.out.println("ERROR label NOT reached");
        System.out.println("_______________________");
        stopAnalysis = true;
      }

    }
    System.out.println("total art find time .. " + totalfindArtTime);
    System.out.println("modify sets .. " + modifySetsTime);
    System.out.println("art element equals .. " + ARTElement.artElementEqualsTime);
    System.out.println("ssamap equals .. " + SSAMap.ssaMapEqualsTime );
    System.out.println("ssamap hash .. " + SSAMap.ssaMapHashTime);
    System.out.println("ssamap get index .. " + SSAMap.ssaGetIndexTime);
    System.out.println("findArtElement .. " + totalfindArtTime);
    System.out.println("choose .. " + CPAAlgorithm.chooseTime);
    System.out.println();
    System.out.println("modify sets");
    System.out.println("part 1 .. " + part1);
    System.out.println("part 2 .. " + part2);
    System.out.println("part 3 .. " + part3);
    System.out.println("part 4 .. " + part4);
    System.out.println("replacing .. " + BDDMathsatSymbPredAbstractionAbstractManager.replacing);
    System.out.println();
    System.out.println("abstraction time .. " + SymbPredAbsTransferRelation.abstractionTime);
    System.out.println("abst time 1: .. " + SymbPredAbsTransferRelation.abstTime1);
    System.out.println("abst time 2: .. " + SymbPredAbsTransferRelation.abstTime2);
    System.out.println("abst time 3: .. " + SymbPredAbsTransferRelation.abstTime3);
    System.out.println();
    System.out.println("non abstract time .. " + SymbPredAbsTransferRelation.nonAbstractionTime);
    System.out.println("time for pf .." + SymbPredAbsTransferRelation.totalTimeForPFCopmutation);
    System.out.println("time for actual pf .. " + SymbPredAbsTransferRelation.totalTimeForActualPfComputation );
    System.out.println("time spent for updating ssamap .. " + SymbPredAbsTransferRelation.updateSSATime);
    System.out.println("time spent for creating new elements .. " + SymbPredAbsTransferRelation.newElementCreationTime);
    System.out.println("refinement time .. " + refinementTime);
    System.out.println("total merge time .. " + SymbPredAbsMergeOperator.totalMergeTime);
    System.out.println();
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
      long start1 = System.currentTimeMillis();
      if (!reachableToUndo.contains(e.getFirst())) {
        long start2 = System.currentTimeMillis();
        part1 = part1 +(start2 - start1);
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
