/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package cpa.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import logging.CustomLogLevel;
import logging.LazyLogger;
import cmdline.CPAMain;

import common.LocationMappedReachedSet;
import common.Pair;

import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.PrecisionAdjustment;
import cpa.common.interfaces.StopOperator;
import cpa.common.interfaces.TransferRelation;
import exceptions.CPAException;
import exceptions.CPATransferException;

public class CPAAlgorithm
{
  private final int GC_PERIOD = 100;
  private int gcCounter = 0;
  private boolean useART = CPAMain.cpaConfig.getBooleanValue("cpa.useART");
  public static boolean errorFound;

  public ReachedElements CPA (ConfigurableProgramAnalysis cpa, AbstractElementWithLocation initialState,
      Precision initialPrecision) throws CPAException
      {
    errorFound = false;
    List<Pair<AbstractElementWithLocation,Precision>> waitlist = new ArrayList<Pair<AbstractElementWithLocation,Precision>>();
    ReachedElements reachedElements = new ReachedElements(cpa);

    LazyLogger.log(CustomLogLevel.CentralCPAAlgorithmLevel, initialState,
    " added as initial state to CPA");

    waitlist.add(new Pair<AbstractElementWithLocation,Precision>(initialState, initialPrecision));
    reachedElements.add(new Pair<AbstractElementWithLocation,Precision>(initialState, initialPrecision));

    TransferRelation transferRelation = cpa.getTransferRelation();
    MergeOperator mergeOperator = cpa.getMergeOperator();
    StopOperator stopOperator = cpa.getStopOperator();
    PrecisionAdjustment precisionAdjustment = cpa.getPrecisionAdjustment();

    while (!waitlist.isEmpty ())
    {
      // Pick next element using strategy
      // BFS, DFS or top sort according to the configuration
      Pair<AbstractElementWithLocation,Precision> e = choose(waitlist);
      // TODO enable this
      //e = precisionAdjustment.prec(e.getFirst(), e.getSecond(), reached);
      AbstractElementWithLocation element = e.getFirst();
      Precision precision = e.getSecond();

      LazyLogger.log(CustomLogLevel.CentralCPAAlgorithmLevel, element,
          " with precision ", precision, " is popped from queue");

      List<AbstractElementWithLocation> successors = null;
      try {
        successors = transferRelation.getAllAbstractSuccessors (element, precision);
//      } catch (ErrorReachedException err) {
//      System.out.println("Reached error state! Message is:");
//      System.out.println(err.toString());
//      return simpleReached;
//      } catch (RefinementNeededException re) {
//      doRefinement(reached, waitlist, re.getReachableToUndo(), re.getToWaitlist());
//      //doRefinementForSymbAbst(initialState, initialPrecision, reached, waitlist, re.getReachableToUndo());
//      continue;
      } catch (CPATransferException e1) {
        e1.printStackTrace();
        assert(false); // should not happen
      }

      for (AbstractElementWithLocation successor : successors)
      {
        LazyLogger.log(CustomLogLevel.CentralCPAAlgorithmLevel,
            "successor of ", element, " --> ", successor);

        // AG as an optimization, we allow the mergeOperator to be null,
        // as a synonym of a trivial operator that never merges

        if (mergeOperator != null) {
          List<Pair<AbstractElementWithLocation,Precision>> toRemove = new Vector<Pair<AbstractElementWithLocation,Precision>>();
          List<Pair<AbstractElementWithLocation,Precision>> toAdd = new Vector<Pair<AbstractElementWithLocation,Precision>>();

          Collection<Pair<AbstractElementWithLocation,Precision>> tempReached;
          Collection<Pair<AbstractElementWithLocation,Precision>> reached = reachedElements.getReached();
          if (reached instanceof LocationMappedReachedSet) {
            AbstractElementWithLocation successorComp = successor;
            tempReached = ((LocationMappedReachedSet)reached).get(successorComp.getLocationNode());

            if(tempReached == null){
              tempReached = new HashSet<Pair<AbstractElementWithLocation,Precision>>();
            }

          }
          else{
            tempReached = reached;
          }

          for (Pair<AbstractElementWithLocation, Precision> reachedEntry : tempReached) {
            AbstractElementWithLocation reachedElement = reachedEntry.getFirst();
            AbstractElementWithLocation mergedElement = mergeOperator.merge( successor, reachedElement, precision);
            LazyLogger.log(CustomLogLevel.CentralCPAAlgorithmLevel,
                " Merged ", successor, " and ", reachedElement, " --> ", mergedElement);
            if (!mergedElement.equals(reachedElement)) {
              LazyLogger.log(
                  CustomLogLevel.CentralCPAAlgorithmLevel,
                  "reached element ", reachedElement,
                  " is removed from queue and ", mergedElement,
                  " with precision ", precision, " is added to queue");
              waitlist.remove(new Pair<AbstractElementWithLocation,Precision>(reachedElement, reachedEntry.getSecond()));
              waitlist.add(new Pair<AbstractElementWithLocation,Precision>(mergedElement, precision));

              toRemove.add(new Pair<AbstractElementWithLocation,Precision>(reachedElement, reachedEntry.getSecond()));
              toAdd.add(new Pair<AbstractElementWithLocation,Precision>(mergedElement, precision));
            }
          }
          reached.removeAll(toRemove);
          reached.addAll(toAdd);

        }

        Collection<Pair<AbstractElementWithLocation,Precision>> tempReached;
        Collection<AbstractElementWithLocation> operatedReached = new HashSet<AbstractElementWithLocation>();
        Collection<Pair<AbstractElementWithLocation,Precision>> reached = reachedElements.getReached();

        if (reached instanceof LocationMappedReachedSet) {
          AbstractElementWithLocation successorComp = successor;
          tempReached = ((LocationMappedReachedSet)reached).get(successorComp.getLocationNode());

          if (tempReached == null) {

          }
          else{
            for (Pair<AbstractElementWithLocation,Precision> p: tempReached) {
              AbstractElementWithLocation e2 = p.getFirst();
              operatedReached.add(e2);
            }
          }
        }
        else{
          operatedReached = reachedElements.getReachedWithElements();
        }

        if (!stopOperator.stop (successor, operatedReached, precision))
        {
          LazyLogger.log(CustomLogLevel.CentralCPAAlgorithmLevel,
              "No need to stop ", successor,
          " is added to queue");

          waitlist.add(new Pair<AbstractElementWithLocation,Precision>(successor,precision));
          reachedElements.add(new Pair<AbstractElementWithLocation,Precision>(successor,precision));
          
          if(useART && errorFound){
            return reachedElements;
          }

        }
      }
      //CPACheckerStatistics.noOfReachedSet = reached.size();
    }

    return reachedElements;
      }

  private Pair<AbstractElementWithLocation,Precision> choose(List<Pair<AbstractElementWithLocation,Precision>> waitlist) {

    if(waitlist.size() == 1){
      return waitlist.remove(0);
    } 
    else if(CPAMain.cpaConfig.getBooleanValue("analysis.topSort")) {
      Pair<AbstractElementWithLocation,Precision> currentElement = waitlist.get(0);
      for(int i=1; i<waitlist.size(); i++){
        Pair<AbstractElementWithLocation,Precision> currentTempElement = waitlist.get(i);
        if(currentTempElement.getFirst().getLocationNode().getTopologicalSortId() >
        currentElement.getFirst().getLocationNode().getTopologicalSortId()){
          currentElement = currentTempElement;
        }
      }

      waitlist.remove(currentElement);
      return currentElement;
    }
    else if(CPAMain.cpaConfig.getBooleanValue("analysis.bfs")){
      return waitlist.remove(0);
    }
    else {
      return waitlist.remove(waitlist.size()-1);
    }
  }

  private void doRefinement(Collection<Pair<AbstractElementWithLocation, Precision>> reached,
      List<Pair<AbstractElementWithLocation, Precision>> waitlist,
      Collection<AbstractElementWithLocation> reachableToUndo,
      Collection<AbstractElementWithLocation> toWaitlist) {
    List<Pair<AbstractElementWithLocation, Precision>> lToWaitlist = new ArrayList<Pair<AbstractElementWithLocation, Precision>>(toWaitlist.size());
    Map<AbstractElementWithLocation, Pair<AbstractElementWithLocation, Precision>> lNewWaitToPrecision = new HashMap<AbstractElementWithLocation, Pair<AbstractElementWithLocation, Precision>>();

    LazyLogger.log(CustomLogLevel.SpecificCPALevel,
    "Performing refinement");
    // remove from reached all the elements in reachableToUndo
    Collection<Pair<AbstractElementWithLocation, Precision>> newreached =
      new LinkedList<Pair<AbstractElementWithLocation, Precision>>();
    for (Pair<AbstractElementWithLocation, Precision> e : reached) {

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

    reached.clear();
    reached.addAll(newreached);
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

  private void doRefinementForSymbAbst(AbstractElementWithLocation initialElement, Precision initialPrecision, 
      Collection<Pair<AbstractElementWithLocation, Precision>> reached,
      List<Pair<AbstractElementWithLocation, Precision>> waitlist,
      Collection<AbstractElementWithLocation> reachableToUndo) {
    LazyLogger.log(CustomLogLevel.SpecificCPALevel,
    "Performing refinement");
    // remove from reached all the elements in reachableToUndo
//  Collection<AbstractElement> newreached =
//  new LinkedList<AbstractElement>();
//  for (AbstractElement e : reached) {
//  CompositeElement compElem = (CompositeElement)e;
//  if (!reachableToUndo.contains(compElem)) {
//  newreached.add(compElem);
//  } else {
//  LazyLogger.log(CustomLogLevel.SpecificCPALevel,
//  "Removing element: ", compElem, " from reached");
//  if (waitlist.remove(compElem)) {
//  LazyLogger.log(CustomLogLevel.SpecificCPALevel,
//  "Removing element: ", compElem,
//  " also from waitlist");
//  }
//  }
//  }
    reached.clear();
//  reached.addAll(newreached);
//  LazyLogger.log(CustomLogLevel.SpecificCPALevel,
//  "Reached now is: ", newreached);
    // and add to the wait list all the elements in toWaitlist
//  boolean useBfs = CPAMain.cpaConfig.getBooleanValue("analysis.bfs");
    //for (AbstractElement e : toWaitlist) {
//  LazyLogger.log(CustomLogLevel.SpecificCPALevel,
//  "Adding element: ", e, " to waitlist");
//  if (useBfs) {
//  // end to the end
//  waitlist.add(e);
//  } else {
    // at to the first index
    waitlist.clear();
    waitlist.add(0, new Pair<AbstractElementWithLocation, Precision>(initialElement, initialPrecision));
//  }
    //}
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
