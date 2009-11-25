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

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import cmdline.CPAMain;

import common.Pair;

import cpa.art.ARTElement;
import cpa.common.ReachedElements;
import cpa.common.RefinementOutcome;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.Refiner;
import exceptions.CPAException;

public class CEGARAlgorithm implements Algorithm {

  private static final int GC_PERIOD = 100;
  private int gcCounter = 0;

  private static long modifySetsTime=0;
  public static long totalfindArtTime =0;
  private static long refinementTime = 0;

  private final Algorithm algorithm;
  private final Refiner mRefiner;
  
  public CEGARAlgorithm(Algorithm algorithm) throws CPAException {
    this.algorithm = algorithm;
    
    String refManagerName = CPAMain.cpaConfig.getProperty("cegar.refiner");

    try {
      Class<?> cls = Class.forName(refManagerName);
      Class<?> parameterTypes[] = {ConfigurableProgramAnalysis.class};
      Constructor<?> ct = cls.getConstructor(parameterTypes);
      Object argumentlist[] = {algorithm.getCPA()};
      Object obj = ct.newInstance(argumentlist);
      mRefiner = (Refiner)obj;
    } catch (Exception e) {
      throw new CPAException("Could not instantiate " + refManagerName + ": " + e.getMessage());
    }
  }
  
  @Override
  public void run(ReachedElements reached, boolean stopAfterError) throws CPAException {

    boolean stopAnalysis = false;
    
    while (!stopAnalysis) {
      // run algorithm
      algorithm.run(reached, true);

      // if the element is an error element
      if (reached.getLastElement().isError()) {

        assert(reached != null);
        long startRef = System.currentTimeMillis();

        RefinementOutcome refout = mRefiner.performRefinement(reached);
        long endRef = System.currentTimeMillis();
        refinementTime = refinementTime + (endRef  - startRef);
        stopAnalysis = !refout.refinementPerformed();

        if (refout.refinementPerformed()) {
          // successful refinement
          long start = System.currentTimeMillis();
          
          if (CPAMain.cpaConfig.getBooleanValue("cegar.restartOnRefinement")) {
            // TODO
          
          } else {
            modifySets(algorithm, reached, refout.getToUnreach(), refout.getToWaitlist(), refout.getRoot());
          }
          
          long end = System.currentTimeMillis();
          modifySetsTime = modifySetsTime + (end - start);
        
          CPAMain.logManager.log(Level.FINEST, "Refinement done");
          
          runGC();
          
        } else {
          // no refinement found
          stopAnalysis = true;
          // TODO: if (stopAfterError == false), continue to look for next error
        }
        
      } else {
        // no error
        System.out.println("ERROR label NOT reached");
        stopAnalysis = true;
      }
    }
    return;
  }

  private void modifySets(Algorithm pAlgorithm,
      ReachedElements reached,
      Collection<ARTElement> reachableToUndo,
      Collection<ARTElement> toWaitlist, AbstractElementWithLocation pRoot) {

    // TODO if starting from nothing, do not bother calling this
    
    Map<AbstractElementWithLocation, Pair<AbstractElementWithLocation, Precision>> toWaitlistPrecision
      = new HashMap<AbstractElementWithLocation, Pair<AbstractElementWithLocation, Precision>>();

    // remove from reached all the elements in reachableToUndo
    List<Pair<AbstractElementWithLocation, Precision>> toRemove = new ArrayList<Pair<AbstractElementWithLocation, Precision>>();
    
    for (Pair<AbstractElementWithLocation, Precision> p : reached.getReached()) {
      AbstractElementWithLocation e = p.getFirst();
      
      if (reachableToUndo.contains(e)) {
        CPAMain.logManager.log(Level.FINEST, "Removing element:", e, "from reached");
        toRemove.add(p);
      }
      
      if (toWaitlist.contains(e)) {
        toWaitlistPrecision.put(e, p);
      }
    }
    
    reached.removeAll(toRemove);
    
    CPAMain.logManager.log(Level.FINEST, "Reached now is:", reached.getReached());
    
    CPAMain.logManager.log(Level.FINEST, "Adding elements:", toWaitlist, "to waitlist");
    
    for (AbstractElementWithLocation e : toWaitlist) {
      Pair<AbstractElementWithLocation, Precision> p;
      if (toWaitlistPrecision.containsKey(e)) {
        p = toWaitlistPrecision.get(e);
      } else {
        // TODO no precision information from toWaitlist available, setting to null
        p = new Pair<AbstractElementWithLocation, Precision>(e, null); 
      }
      reached.add(p);
    }
  }

  private void runGC() {
    if ((++gcCounter % GC_PERIOD) == 0) {
      System.gc();
      gcCounter = 0;
    }
  }
  
  @Override
  public ConfigurableProgramAnalysis getCPA() {
    return algorithm.getCPA();
  }
}