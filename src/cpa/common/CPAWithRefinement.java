package cpa.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import logging.CustomLogLevel;
import logging.LazyLogger;
import cfa.CFAMap;
import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFANode;
import cfa.objectmodel.c.FunctionDefinitionNode;
import cmdline.CPAMain;

import common.Pair;

import cpa.art.ARTElement;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.RefinableCPA;
import cpa.common.interfaces.RefinementManager;
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
  public ReachedElements CPAWithRefinementAlgorithm(CFAMap pCfas, ConfigurableProgramAnalysis cpa, 
      AbstractElementWithLocation initialElement,
      Precision initialPrecision) throws CPAException{
    if(!(cpa instanceof RefinableCPA)) {
      throw new CPAException("Need refinable CPA for refinement algorithm");
    }

    ReachedElements reached = null;
//  CPAAlgorithm algo = new CPAAlgorithm(cpa, initialElement, initialPrecision);
    boolean stopAnalysis = false;
    while(!stopAnalysis){
      // TODO if we want to restart
      CPAAlgorithm algo = new CPAAlgorithm(cpa, initialElement, initialPrecision);
      ((ARTElement)initialElement).clearChildren();
//      System.out.println("initial element has " + ((ARTElement)initialElement).getChildren().size());
      try {
        reached = algo.CPA();
      } catch (CPAException e) {
        e.printStackTrace();
      }
      
      // if the element is an error element
      if (reached.getLastElement().isError()) {
        RefinableCPA refinableCpa = (RefinableCPA)cpa;
        RefinementManager refinementManager = refinableCpa.getRefinementManager();

        assert(reached != null);
        long startRef = System.currentTimeMillis();
//        System.out.println(" =========================== REFINEMENT =============================== ");
//        dumpErrorPathToDotFile(reached, "/localhome/erkan/refpath.dot");
        RefinementOutcome refout = refinementManager.performRefinement(reached, null);
        long endRef = System.currentTimeMillis();
        refinementTime = refinementTime + (endRef  - startRef);
        stopAnalysis = !refout.refinementPerformed();

        if(stopAnalysis){
          System.out.println("ERROR FOUND");
          List<CFAEdge> errorPath = buildErrorPath(reached);
          // TODO make this optional
          if (CPAMain.cpaConfig.getBooleanValue("analysis.useCBMC")) {
            System.out.println("________ ERROR PATH ____________");
            int cbmcRes = CProver.checkSat(AbstractPathToCTranslator.translatePaths(pCfas, errorPath));
            if(cbmcRes == 10){
              System.out.println("CBMC comfirms the bug");
            }
            else if(cbmcRes == 0){
              System.out.println("CBMC thinks this path contains no bug");
  //          reached.setLastElementToFalse();
  //          CPAAlgorithm.errorFound = false;
  //          stopAnalysis = false;
            }
            System.out.println("________________________________");
          }
        }
        else{
          long start = System.currentTimeMillis();
          modifySets(algo, refout.getToUnreach(), refout.getToWaitlist(), refout.getRoot());
          long end = System.currentTimeMillis();
          modifySetsTime = modifySetsTime + (end - start);
        }
      }

      else {
        // TODO safe -- print reached elements
        System.out.println("ERROR label NOT reached");
        stopAnalysis = true;
      }

    }
//  System.out.println("total art find time .. " + totalfindArtTime);
//  System.out.println("modify sets .. " + modifySetsTime);
//  System.out.println("art element equals .. " + ARTElement.artElementEqualsTime);
//  System.out.println("ssamap equals .. " + SSAMap.ssaMapEqualsTime );
//  System.out.println("ssamap hash .. " + SSAMap.ssaMapHashTime);
//  System.out.println("ssamap get index .. " + SSAMap.ssaGetIndexTime);
//  System.out.println("findArtElement .. " + totalfindArtTime);
//  System.out.println("choose .. " + CPAAlgorithm.chooseTime);
//  System.out.println();
//  System.out.println("modify sets");
//  System.out.println("part 1 .. " + part1);
//  System.out.println("part 2 .. " + part2);
//  System.out.println("part 3 .. " + part3);
//  System.out.println("part 4 .. " + part4);
//  System.out.println("replacing .. " + BDDMathsatSymbPredAbstractionAbstractManager.replacing);
//  System.out.println();
//  System.out.println("abstraction time .. " + SymbPredAbsTransferRelation.abstractionTime);
//  System.out.println("abst time 1: .. " + SymbPredAbsTransferRelation.abstTime1);
//  System.out.println("abst time 2: .. " + SymbPredAbsTransferRelation.abstTime2);
//  System.out.println("abst time 3: .. " + SymbPredAbsTransferRelation.abstTime3);
//  System.out.println();
//  System.out.println("non abstract time .. " + SymbPredAbsTransferRelation.nonAbstractionTime);
//  System.out.println("time for pf .." + SymbPredAbsTransferRelation.totalTimeForPFCopmutation);
//  System.out.println("time for actual pf .. " + SymbPredAbsTransferRelation.totalTimeForActualPfComputation );
//  System.out.println("time spent for updating ssamap .. " + SymbPredAbsTransferRelation.updateSSATime);
//  System.out.println("time spent for creating new elements .. " + SymbPredAbsTransferRelation.newElementCreationTime);
//  System.out.println("refinement time .. " + refinementTime);
//  System.out.println("total merge time .. " + SymbPredAbsMergeOperator.totalMergeTime);
    System.out.println();
    return reached;
  }

  private List<CFAEdge> buildErrorPath(ReachedElements pReached) {
    AbstractElement lastElement = pReached.getLastElement();
    ARTElement lastArtElement = (ARTElement)lastElement;

    List<CFAEdge> path = new ArrayList<CFAEdge>();
    ARTElement currentArtElement = lastArtElement;

    while(currentArtElement.getParent() != null){
      ARTElement parentElement = currentArtElement.getParent();
      CFANode currentNode = currentArtElement.getLocationNode();
      for(int i=0; i<currentNode.getNumEnteringEdges(); i++){
        CFAEdge edge = currentNode.getEnteringEdge(i);
        if(parentElement.getLocationNode().getNodeNumber() == edge.getPredecessor().getNodeNumber()){
          path.add(0, edge);
        }
      }
      currentArtElement = parentElement;
    }
    return path;
  }

  private List<String> buildFunctionCallsToError(ReachedElements pReached) {
    AbstractElement lastElement = pReached.getLastElement();
    ARTElement lastArtElement = (ARTElement)lastElement;

    List<String> path = new ArrayList<String>();
    ARTElement currentArtElement = lastArtElement;

    while(currentArtElement.getParent() != null){
      ARTElement parentElement = currentArtElement.getParent();
      CFANode currentNode = currentArtElement.getLocationNode();
      if(currentNode instanceof FunctionDefinitionNode){
        path.add(0, currentNode.getFunctionName());
      }
      currentArtElement = parentElement;
    }
    return path;
  }

  private void modifySets(CPAAlgorithm pAlgorithm,
      Collection<ARTElement> reachableToUndo,
      Collection<ARTElement> toWaitlist, AbstractElementWithLocation pRoot) {

    // TODO if starting from nothing, do not bother
    Collection<Pair<AbstractElementWithLocation, Precision>> reachedSet = 
      pAlgorithm.getReachedElements().getReached();
//  List<Pair<AbstractElementWithLocation, Precision>> waitlist = 
//  pAlgorithm.getWaitlist();

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
        if (pAlgorithm.removeFromWaitlist(e)) {
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

    pAlgorithm.buildNewReachedSet(newreached);
    LazyLogger.log(CustomLogLevel.SpecificCPALevel,
        "Reached now is: ", newreached);
    // and add to the wait list all the elements in toWaitlist
    boolean useBfs = CPAMain.cpaConfig.getBooleanValue("analysis.bfs");

    LazyLogger.log(CustomLogLevel.SpecificCPALevel, "Adding elements: ", lToWaitlist, " to waitlist");

    if (useBfs) {
      pAlgorithm.addAllToWaitlist(lToWaitlist);
    }
    else {
      pAlgorithm.addAllToWaitlistAt(0, lToWaitlist);
    }

    LazyLogger.log(CustomLogLevel.SpecificCPALevel,
        "Waitlist now is: ", pAlgorithm.getWaitlist());
    LazyLogger.log(CustomLogLevel.SpecificCPALevel,
    "Refinement done");
    if ((++gcCounter % GC_PERIOD) == 0) {
      System.gc();
      gcCounter = 0;
    }
    ((ARTElement)pRoot).clearChildren();
  }
}
