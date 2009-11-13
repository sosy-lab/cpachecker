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

import common.Pair;

import cpa.common.ReachedElements;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.StopOperator;
import cpa.common.interfaces.TransferRelation;
import exceptions.CPAException;

public class CPAAlgorithm implements Algorithm {

  private long chooseTime = 0;
  private long transferTime = 0;
  private long mergeTime = 0;
  private long stopTime = 0;
  
  private final ConfigurableProgramAnalysis cpa;

  public CPAAlgorithm(ConfigurableProgramAnalysis cpa) {
    this.cpa = cpa;
  }

  @Override
  public void run(final ReachedElements reachedElements, boolean stopAfterError) throws CPAException {
    final TransferRelation transferRelation = cpa.getTransferRelation();
    final MergeOperator mergeOperator = cpa.getMergeOperator();
    final StopOperator stopOperator = cpa.getStopOperator();

    while (reachedElements.hasWaitingElement()) {
      
      // Pick next element using strategy
      // BFS, DFS or top sort according to the configuration
      long start = System.currentTimeMillis();
      Pair<AbstractElementWithLocation,Precision> e = reachedElements.popFromWaitlist();
      long end = System.currentTimeMillis();
      chooseTime += (end - start); 
      // TODO enable this
      //e = precisionAdjustment.prec(e.getFirst(), e.getSecond(), reached);
      AbstractElementWithLocation element = e.getFirst();
      Precision precision = e.getSecond();

      LazyLogger.log(CustomLogLevel.CentralCPAAlgorithmLevel, element,
          " with precision ", precision, " is popped from queue");

      start = System.currentTimeMillis();
      List<AbstractElementWithLocation> successors = transferRelation.getAllAbstractSuccessors (element, precision);
      end = System.currentTimeMillis();
      transferTime += (end - start);
      // TODO When we have a nice way to mark the analysis result as incomplete, we could continue analysis on a CPATransferException with the next element from waitlist.
      
      for (AbstractElementWithLocation successor : successors) {
        LazyLogger.log(CustomLogLevel.CentralCPAAlgorithmLevel,
            "successor of ", element, " --> ", successor);
        
        Collection<Pair<AbstractElementWithLocation,Precision>> reached = reachedElements.getReached(successor.getLocationNode());

        // AG as an optimization, we allow the mergeOperator to be null,
        // as a synonym of a trivial operator that never merges
        if (mergeOperator != null && !reached.isEmpty()) {
          start = System.currentTimeMillis();

          List<Pair<AbstractElementWithLocation,Precision>> toRemove = new Vector<Pair<AbstractElementWithLocation,Precision>>();
          List<Pair<AbstractElementWithLocation,Precision>> toAdd = new Vector<Pair<AbstractElementWithLocation,Precision>>();
          
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
          
          end = System.currentTimeMillis();
          mergeTime += (end - start);
        }
        
        start = System.currentTimeMillis();
        Collection<AbstractElementWithLocation> simpleReached = new HashSet<AbstractElementWithLocation>();
        
        for (Pair<AbstractElementWithLocation,Precision> p: reached) {
          AbstractElementWithLocation e2 = p.getFirst();
          simpleReached.add(e2);
        }

        if (!stopOperator.stop(successor, simpleReached, precision)) {
          LazyLogger.log(CustomLogLevel.CentralCPAAlgorithmLevel,
              "No need to stop ", successor, " is added to queue");

          reachedElements.add(new Pair<AbstractElementWithLocation,Precision>(successor,precision));
          
          if(stopAfterError && successor.isError()) {
            end = System.currentTimeMillis();
            stopTime += (end - start);
            return;
          }
        }
        end = System.currentTimeMillis();
        stopTime += (end - start);
      }
    }
  }

  @Override
  public ConfigurableProgramAnalysis getCPA() {
    return cpa;
  }
}