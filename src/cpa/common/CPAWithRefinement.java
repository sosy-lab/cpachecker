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

  public ReachedElements CPAWithRefinementAlgorithm(CFAMap pCfas, ConfigurableProgramAnalysis cpa, 
      AbstractElementWithLocation initialElement,
      Precision initialPrecision) throws CPAException{
    ReachedElements reached = null;
    CPAAlgorithm algo = new CPAAlgorithm(cpa, initialElement, initialPrecision);
    boolean stopAnalysis = false;
    while(!stopAnalysis){
      // TODO if we want to restart
//    CPAAlgorithm algo = new CPAAlgorithm(cpa, initialElement, initialPrecision);
//    ((ARTElement)initialElement).clearChildren();
      try {
        reached = algo.CPA();
      } catch (CPAException e) {
        e.printStackTrace();
      }
      if(!(cpa instanceof RefinableCPA)) {
        throw new CPAException();
      }

      // if the element is an error element
      if (reached.getLastElement().isError()) {
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
          if (CPAMain.cpaConfig.getBooleanValue("analysis.useCBMC")) {
            List<CFAEdge> errorPath = buildErrorPath(reached);
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
        } else {
          long start = System.currentTimeMillis();
          modifySets(algo, refout.getToUnreach(), refout.getToWaitlist(), refout.getRoot());
          long end = System.currentTimeMillis();
          modifySetsTime = modifySetsTime + (end - start);
        }
        
      } else {
        // TODO safe -- print reached elements
        System.out.println("ERROR label NOT reached");
        stopAnalysis = true;
      }
    }
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

  @SuppressWarnings("unused")
  // TODO for what is this method?
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

    // TODO if starting from nothing, do not bother calling this
    Collection<Pair<AbstractElementWithLocation, Precision>> reachedSet = 
      pAlgorithm.getReachedElements().getReached();
    Collection<Pair<AbstractElementWithLocation, Precision>> waitList = 
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
        newreached.add(lNewWaitToPrecision.get(w));
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

    List<Pair<AbstractElementWithLocation, Precision>> removeFromWaitlist = new LinkedList<Pair<AbstractElementWithLocation,Precision>>();

    for(Pair<AbstractElementWithLocation, Precision> p: waitList){
      AbstractElementWithLocation elem = p.getFirst();
      if(reachableToUndo.contains(elem)){
        removeFromWaitlist.add(p);
      }
    }

    pAlgorithm.removeAllFromWaitlist(removeFromWaitlist);

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
    // we can get rid of children of root because we're clearing them from the
    // reached set
    ((ARTElement)pRoot).clearChildren();
  }
}
