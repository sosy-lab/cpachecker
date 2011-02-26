/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.core.algorithm;

import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

import org.sosy_lab.common.AbstractMBean;
import org.sosy_lab.common.Classes;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.Classes.ClassInstantiationException;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;

import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;

import com.google.common.base.Throwables;

@Options(prefix="cegar")
public class CEGARAlgorithm implements Algorithm, StatisticsProvider {

  private static class CEGARStatistics implements Statistics {

    private final Timer totalTimer = new Timer();
    private final Timer refinementTimer = new Timer();
    private final Timer gcTimer = new Timer();
    
    private volatile int countRefinements = 0;
    private int countSuccessfulRefinements = 0;
    private int countFailedRefinements = 0;

    @Override
    public String getName() {
      return "CEGAR algorithm";
    }

    @Override
    public void printStatistics(PrintStream out, Result pResult,
        ReachedSet pReached) {

      out.println("Number of refinements:            " + countRefinements);

      if (countRefinements > 0) {
        out.println("Number of successful refinements: " + countSuccessfulRefinements);
        out.println("Number of failed refinements:     " + countFailedRefinements);
        out.println("");
        out.println("Total time for CEGAR algorithm:   " + totalTimer);
        out.println("Time for refinements:             " + refinementTimer);
        out.println("Average time for refinement:      " + refinementTimer.printAvgTime());
        out.println("Max time for refinement:          " + refinementTimer.printMaxTime());
        out.println("Time for garbage collection:      " + gcTimer);
      }
    }
  }

  private final CEGARStatistics stats = new CEGARStatistics();

  public static interface CEGARMXBean {
    int getNumberOfRefinements();
    int getSizeOfReachedSetBeforeLastRefinement();
    boolean isRefinementActive();
  }
  
  private class CEGARMBean extends AbstractMBean implements CEGARMXBean {
    public CEGARMBean() {
      super("org.sosy_lab.cpachecker:type=CEGAR", logger);
      register();
    }
    
    @Override
    public int getNumberOfRefinements() {
      return stats.countRefinements;
    }
    
    @Override
    public int getSizeOfReachedSetBeforeLastRefinement() {
      return sizeOfReachedSetBeforeRefinement;
    }
    
    @Override
    public boolean isRefinementActive() {
      return stats.refinementTimer.isRunning();
    }
  }
  
  private static final int GC_PERIOD = 100;
  private int gcCounter = 0;
  
  private volatile int sizeOfReachedSetBeforeRefinement = 0;

  private static final String PACKAGE_NAME_PREFIX = "org.sosy_lab.cpachecker";

  @Option(required=true)
  private String refiner = "";

  @Option
  private boolean restartOnRefinement = false;

  private final LogManager logger;
  private final Algorithm algorithm;
  private final Refiner mRefiner;

  public static final ExecutorService executor = Executors.newCachedThreadPool();

  private <T> T createInstance(String className, Object[] argumentList, Class<T> type) throws CPAException {
    Class<?> argumentTypes[] = {ConfigurableProgramAnalysis.class};
    
    try {
      return Classes.createInstance(className, PACKAGE_NAME_PREFIX, argumentTypes, argumentList, type);
    
    } catch (ClassInstantiationException e) {
      throw new CPAException("Invalid refiner specified (" + e.getMessage() + ")!");

    } catch (InvocationTargetException e) {
      Throwable t = e.getCause();
      Throwables.propagateIfPossible(t, CPAException.class);
      
      logger.logException(Level.FINE, t, "Unexpected exception during refiner instantiation!");
      throw new CPAException("Unexpected exception " + t.getClass().getSimpleName() + " during refiner instantiation (" + t.getMessage() + ")!");
    }
  }

  public CEGARAlgorithm(Algorithm algorithm, Configuration config, LogManager logger) throws InvalidConfigurationException, CPAException {
    config.inject(this);
    this.algorithm = algorithm;
    this.logger = logger;

    Object[] refinerArguments = {algorithm.getCPA()};

    mRefiner = createInstance(refiner, refinerArguments, Refiner.class);
    new CEGARMBean(); // don't store it because we wouldn't know when to unregister anyway
  }
  
  /**
   * This constructor gets a Refiner object instead of generating it
   * from the refiner parameter.
   * 
   * @param algorithm
   * @param pRefiner
   * @param config
   * @param logger
   * @throws InvalidConfigurationException
   * @throws CPAException
   */
  public CEGARAlgorithm(Algorithm algorithm, Refiner pRefiner, Configuration config, LogManager logger) throws InvalidConfigurationException, CPAException {
    config.inject(this);
    this.algorithm = algorithm;
    this.logger = logger;

    if (pRefiner == null) {
      throw new IllegalArgumentException("Given Refiner object is null!");
    }
    
    mRefiner = pRefiner;
  }

  @Override
  public boolean run(ReachedSet reached) throws CPAException {
    boolean sound = true;
    
    stats.totalTimer.start();

    boolean stopAnalysis = false;
    while (!stopAnalysis) {
      // run algorithm
      sound &= algorithm.run(reached);

      AbstractElement lastElement = reached.getLastElement();

      // if the element is an error element
      if ((lastElement instanceof Targetable) && ((Targetable)lastElement).isTarget()) {

        logger.log(Level.FINER, "Error found, performing CEGAR");
        stats.countRefinements++;
        sizeOfReachedSetBeforeRefinement = reached.size();

        stats.refinementTimer.start();
        boolean refinementResult;
        try {
          refinementResult = mRefiner.performRefinement(reached);
          
        } catch (RefinementFailedException e) {
          stats.countFailedRefinements++;
          throw e;
        } finally {
          stats.refinementTimer.stop();
        }
        
        if (refinementResult) {
          // successful refinement

          logger.log(Level.FINER, "Refinement successful");
          stats.countSuccessfulRefinements++;

          if (restartOnRefinement) {
            // TODO
          }

          runGC();

          stopAnalysis = false;

        } else {
          // no refinement found, because the counterexample is not spurious
          logger.log(Level.FINER, "Refinement unsuccessful");

          stopAnalysis = true;

          // TODO: if (stopAfterError == false), continue to look for next error
        }

      } else {
        // no error
        stopAnalysis = true;
      }
    }
    executor.shutdownNow();

    stats.totalTimer.stop();
    return sound;
  }

  private void runGC() {
    if ((++gcCounter % GC_PERIOD) == 0) {
      stats.gcTimer.start();
      System.gc();
      gcCounter = 0;
      stats.gcTimer.stop();
    }
  }

  @Override
  public ConfigurableProgramAnalysis getCPA() {
    return algorithm.getCPA();
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (algorithm instanceof StatisticsProvider) {
      ((StatisticsProvider)algorithm).collectStatistics(pStatsCollection);
    }
    pStatsCollection.add(stats);
  }

}