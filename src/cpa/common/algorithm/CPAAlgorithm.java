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
package cpa.common.algorithm;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;

import logging.CustomLogLevel;
import logging.LazyLogger;

import common.LocationMappedReachedSet;
import common.Pair;

import cpa.common.ReachedElements;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.StopOperator;
import cpa.common.interfaces.TransferRelation;
import exceptions.CPAException;
import exceptions.CPATransferException;

public class CPAAlgorithm implements Algorithm {

  private static long chooseTime = 0;
  private final ConfigurableProgramAnalysis cpa;

  public CPAAlgorithm(ConfigurableProgramAnalysis cpa) {
    this.cpa = cpa;
  }

  @Override
  public void run(ReachedElements reachedElements, boolean stopAfterError) throws CPAException {
    TransferRelation transferRelation = cpa.getTransferRelation();
    MergeOperator mergeOperator = cpa.getMergeOperator();
    StopOperator stopOperator = cpa.getStopOperator();

    while (reachedElements.hasWaitingElement()) {
      
      // Pick next element using strategy
      // BFS, DFS or top sort according to the configuration
      long start = System.currentTimeMillis();
      Pair<AbstractElementWithLocation,Precision> e = reachedElements.popFromWaitlist();
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

          Collection<Pair<AbstractElementWithLocation,Precision>> reached = reachedElements.getReached();
          if (reached instanceof LocationMappedReachedSet) {
            AbstractElementWithLocation successorComp = successor;
            reached = ((LocationMappedReachedSet)reached).get(successorComp.getLocationNode());

            if(reached == null){
              reached = new HashSet<Pair<AbstractElementWithLocation,Precision>>();
            }
          }

          for (Pair<AbstractElementWithLocation, Precision> reachedEntry : reached) {
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

              toRemove.add(reachedEntry);
              toAdd.add(new Pair<AbstractElementWithLocation,Precision>(mergedElement, precision));
            }
          }
          reachedElements.removeAll(toRemove);
          reachedElements.addAll(toAdd);
        }

        Collection<AbstractElementWithLocation> simpleReached = new HashSet<AbstractElementWithLocation>();
        Collection<Pair<AbstractElementWithLocation,Precision>> reached = reachedElements.getReached();

        if (reached instanceof LocationMappedReachedSet) {
          AbstractElementWithLocation successorComp = successor;
          reached = ((LocationMappedReachedSet)reached).get(successorComp.getLocationNode());
        }

        if (reached != null) {
          for (Pair<AbstractElementWithLocation,Precision> p: reached) {
            AbstractElementWithLocation e2 = p.getFirst();
            simpleReached.add(e2);
          }
        }
        

        if (!stopOperator.stop(successor, simpleReached, precision)) {
          LazyLogger.log(CustomLogLevel.CentralCPAAlgorithmLevel,
              "No need to stop ", successor,
          " is added to queue");

          reachedElements.add(new Pair<AbstractElementWithLocation,Precision>(successor,precision));
          
          if(stopAfterError && successor.isError()) {
            return;
          }
        }
      }
    }

    return;
  }

  @Override
  public ConfigurableProgramAnalysis getCPA() {
    return cpa;
  }
}