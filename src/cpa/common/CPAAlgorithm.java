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
import java.util.HashSet;
import java.util.List;
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
  private boolean useART = CPAMain.cpaConfig.getBooleanValue("cpa.useART");
  public static boolean errorFound;
public static long chooseTime = 0;
  private List<Pair<AbstractElementWithLocation,Precision>> waitlist;
  private ReachedElements reachedElements;
  private AbstractElementWithLocation initialState;
  private Precision initialPrecision;

  private ConfigurableProgramAnalysis cpa;

  public CPAAlgorithm(ConfigurableProgramAnalysis pCpa, AbstractElementWithLocation pInitialState,
      Precision pInitialPrecision) {
    cpa = pCpa;
    waitlist = new ArrayList<Pair<AbstractElementWithLocation,Precision>>();
    reachedElements = new ReachedElements(cpa);
    initialState = pInitialState;
    initialPrecision = pInitialPrecision;
    waitlist.add(new Pair<AbstractElementWithLocation,Precision>(initialState, initialPrecision));
    reachedElements.add(new Pair<AbstractElementWithLocation,Precision>(initialState, initialPrecision));
  }

  public ReachedElements CPA () throws CPAException
  {
    errorFound = false;
    TransferRelation transferRelation = cpa.getTransferRelation();
    MergeOperator mergeOperator = cpa.getMergeOperator();
    StopOperator stopOperator = cpa.getStopOperator();
    PrecisionAdjustment precisionAdjustment = cpa.getPrecisionAdjustment();

    while (!waitlist.isEmpty ())
    {
      // Pick next element using strategy
      // BFS, DFS or top sort according to the configuration
      long start = System.currentTimeMillis();
      Pair<AbstractElementWithLocation,Precision> e = choose(waitlist);
      long end = System.currentTimeMillis();
      chooseTime = chooseTime + (end - start); 
      // TODO enable this
      //e = precisionAdjustment.prec(e.getFirst(), e.getSecond(), reached);
      AbstractElementWithLocation element = e.getFirst();
      Precision precision = e.getSecond();

      LazyLogger.log(CustomLogLevel.CentralCPAAlgorithmLevel, element,
          " with precision ", precision, " is popped from queue");

      List<AbstractElementWithLocation> successors = null;
      try {
        successors = transferRelation.getAllAbstractSuccessors (element, precision);
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
          reachedElements.removeAll(toRemove);
          reachedElements.addAll(toAdd);
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

  public List<Pair<AbstractElementWithLocation, Precision>> getWaitlist() {
    return waitlist;
  }

  public ReachedElements getReachedElements() {
    return reachedElements;
  }

  public void buildNewReachedSet(
      Collection<Pair<AbstractElementWithLocation, Precision>> pNewreached) {
    reachedElements.buildNewReachedSet(pNewreached);
  }
  
  public boolean removeFromWaitlist(Pair<AbstractElementWithLocation, Precision> pElement){
    return waitlist.remove(pElement);
  }
  
  public boolean removeAllFromWaitlist(Collection<Pair<AbstractElementWithLocation, Precision>> pElements){
    return waitlist.removeAll(pElements);
  }
  
  public void addAllToWaitlist(List<Pair<AbstractElementWithLocation, Precision>> pToWaitlist){
    waitlist.addAll(pToWaitlist);
  }

  public void addAllToWaitlistAt(int pIndex,
      List<Pair<AbstractElementWithLocation, Precision>> pToWaitlist) {
    waitlist.addAll(pIndex, pToWaitlist);
  }

  public void clearWaitlist() {
    waitlist.clear();
  }
}