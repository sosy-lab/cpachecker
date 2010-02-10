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

import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

import cpa.common.CPAchecker;
import cpa.common.ReachedElements;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.Refiner;
import cpa.common.interfaces.Statistics;
import cpa.common.interfaces.StatisticsProvider;
import exceptions.CPAException;
import exceptions.TransferTimeOutException;

public class CEGARAlgorithm implements Algorithm, StatisticsProvider {

  private static class CEGARStatistics implements Statistics {
    
    private long totalTime = 0;
    private long refinementTime = 0;
    private long gcTime = 0;
    
    private int countRefinements = 0;
    private int countSuccessfulRefinements = 0;
    
    @Override
    public String getName() {
      return "CEGAR algorithm";
    }
    
    @Override
    public void printStatistics(PrintWriter out, Result pResult,
        ReachedElements pReached) {
      
      out.println("Number of refinements:          " + countRefinements + " (" + countSuccessfulRefinements + " successful)");
      
      if (countRefinements > 0) {
        out.println("");
        out.println("Total time for CEGAR algorithm: " + toTime(totalTime));
        out.println("Time for refinements:           " + toTime(refinementTime));
        out.println("Average time for refinement:    " + toTime(refinementTime/countRefinements));
        out.println("Time for garbage collection:    " + toTime(gcTime));
      }
    }
    
    private String toTime(long timeMillis) {
      return String.format("% 5d.%03ds", timeMillis/1000, timeMillis%1000);
    }
  }
  
  private final CEGARStatistics stats = new CEGARStatistics();
  
  private static final int GC_PERIOD = 100;
  private int gcCounter = 0;

  private final Algorithm algorithm;
  private final Refiner mRefiner;

  public static final ExecutorService executor = Executors.newCachedThreadPool();

  /**
   * Creates an instance of class className, passing the objects from argumentList
   * to the constructor and casting the object to class type. Throws a CPAException
   * if anything goes wrong.
   * 
   * TODO This method could be used in other places, too. Perhaps move it to a central location. 
   */
  @SuppressWarnings("unchecked")
  private static <T> T createInstance(String className, Object[] argumentList, Class<T> type)
  throws CPAException {
    try {
      Class<?> cls = Class.forName(className);
      Class<?> parameterTypes[] = {ConfigurableProgramAnalysis.class};
      Constructor<?> ct = cls.getConstructor(parameterTypes);
      Object obj = ct.newInstance(argumentList);
      if (type.isAssignableFrom(obj.getClass())) {
        return (T)obj;
      } else {
        throw new ClassCastException(obj.getClass() + " cannot be cast to " + type);
      }
    } catch (Exception e) {
      throw new CPAException("Could not instantiate " + className + ": " + e.getMessage());
    }

  }

  public CEGARAlgorithm(Algorithm algorithm) throws CPAException {
    this.algorithm = algorithm;

    String refinerName = CPAchecker.config.getProperty("cegar.refiner");
    Object[] refinerArguments = {algorithm.getCPA()};

    mRefiner = createInstance(refinerName, refinerArguments, Refiner.class);
  }

  @Override
  public void run(ReachedElements reached, boolean stopAfterError) throws CPAException {
    long start = System.currentTimeMillis();
    
    boolean stopAnalysis = false;
    while (!stopAnalysis) {
      // run algorithm
      try{
        algorithm.run(reached, true);
      } catch (TransferTimeOutException toE) {
        // TODO this is temp. to terminate
        // this exception should be handled in InvariantCOllectionAlgorithm 
        System.out.println("Timed out @ " + toE.getCfaEdge());
      }

      AbstractElement lastElement = reached.getLastElement();
      
      // if the element is an error element
      if (lastElement != null && lastElement.isError()) {

        CPAchecker.logger.log(Level.FINER, "Error found, performing CEGAR");
        stats.countRefinements++;
        
        long startRefinement = System.currentTimeMillis();
        boolean refinementResult = mRefiner.performRefinement(reached);
        stats.refinementTime += (System.currentTimeMillis() - startRefinement);

        if (refinementResult) {
          // successful refinement

          CPAchecker.logger.log(Level.FINER, "Refinement successful");
          stats.countSuccessfulRefinements++;

          if (CPAchecker.config.getBooleanValue("cegar.restartOnRefinement")) {
            // TODO
          }

          runGC();
          
          stopAnalysis = false;

        } else {
          // no refinement found, because the counterexample is not spurious
          CPAchecker.logger.log(Level.FINER, "Refinement unsuccessful");

          stopAnalysis = true;

          // TODO: if (stopAfterError == false), continue to look for next error
        }

      } else {
        // no error
        System.out.println("ERROR label NOT reached");
        stopAnalysis = true;
      }
    }
    executor.shutdownNow();
    
    stats.totalTime += (System.currentTimeMillis() - start);
  }

  private void runGC() {
    if ((++gcCounter % GC_PERIOD) == 0) {
      long start = System.currentTimeMillis();
      System.gc();
      gcCounter = 0;
      stats.gcTime += (System.currentTimeMillis() - start);
    }
  }

  @Override
  public ConfigurableProgramAnalysis getCPA() {
    return algorithm.getCPA();
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
    if (algorithm instanceof StatisticsProvider) {
      ((StatisticsProvider)algorithm).collectStatistics(pStatsCollection);
    }
  }

}