/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.logging.Level;

import org.sosy_lab.common.AbstractMBean;
import org.sosy_lab.common.Classes;
import org.sosy_lab.common.Classes.UnexpectedCheckedException;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.ClassOption;
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
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.util.AbstractElements;

import com.google.common.base.Preconditions;
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
    private boolean timedOut = false;

    @Override
    public String getName() {
      return "CEGAR algorithm";
    }

    @Override
    public void printStatistics(PrintStream out, Result pResult,
        ReachedSet pReached) {

      out.println("Timed out: " + timedOut);
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

  @Option(required = true,
      description = "Which refinement algorithm to use? "
      + "(give class name, required for CEGAR) If the package name starts with "
      + "'org.sosy_lab.cpachecker.', this prefix can be omitted.")
  @ClassOption(packagePrefix = "org.sosy_lab.cpachecker")
  private Class<? extends Refiner> refiner = null;

  @Option(description = "completely restart analysis on refinement "
      + "by removing everything from the reached set")
  private boolean restartOnRefinement = false;

  @Option(description = "threshold (in ms) after which the CEGAR algorithm gives up refining (spurious) counterexamples")
  private int stopRefiningThreshold = -1; //TODO maybe use ProgressObserver instead?

  @Option(description = "maximum count of attempted refinements")
  private int stopRefiningCount = -1;

  @Option(description="do not refine after a reset of the CEGAR algorithm")
  private boolean noRefinementInFirstRun = false;

  private long startTime = 0;
  private int refinementCount = 0;
  private final LogManager logger;
  private final Algorithm algorithm;
  private final Refiner mRefiner;
  private int resets = 0;


  // TODO Copied from CPABuilder, should be refactored into a generic implementation
  private Refiner createInstance(ConfigurableProgramAnalysis pCpa) throws CPAException, InvalidConfigurationException {

    // get factory method
    Method factoryMethod;
    try {
      factoryMethod = refiner.getMethod("create", ConfigurableProgramAnalysis.class);
    } catch (NoSuchMethodException e) {
      throw new CPAException("Each Refiner class has to offer a public static method \"create\" with one parameters, but " + refiner.getSimpleName() + " does not!");
    }

    // verify signature
    if (!Modifier.isStatic(factoryMethod.getModifiers())) {
      throw new CPAException("The factory method of the refiner " + refiner.getSimpleName() + " is not static!");
    }

    String exception = Classes.verifyDeclaredExceptions(factoryMethod, CPAException.class, InvalidConfigurationException.class);
    if (exception != null) {
      throw new CPAException("The factory method of the refiner " + refiner.getSimpleName() + " declares the unsupported checked exception: " + exception);
    }

    // invoke factory method
    Object refinerObj;
    try {
      refinerObj = factoryMethod.invoke(null, pCpa);

    } catch (IllegalAccessException e) {
      throw new CPAException("The factory method of the refiner " + refiner.getSimpleName() + " is not public!");

    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      Throwables.propagateIfPossible(cause, CPAException.class, InvalidConfigurationException.class);

      throw new UnexpectedCheckedException("instantiation of refiner " + refiner.getSimpleName(), cause);
    }

    if ((refinerObj == null) || !(refinerObj instanceof Refiner)) {
      throw new CPAException("The factory method of the refiner " + refiner.getSimpleName() + " didn't return a Refiner!");
    }

    return (Refiner)refinerObj;
  }

  public CEGARAlgorithm(Algorithm algorithm, ConfigurableProgramAnalysis pCpa, Configuration config, LogManager logger) throws InvalidConfigurationException, CPAException {
    config.inject(this);
    this.algorithm = algorithm;
    this.logger = logger;

    mRefiner = createInstance(pCpa);
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
    mRefiner = Preconditions.checkNotNull(pRefiner);
  }

  @Override
  public boolean run(ReachedSet reached) throws CPAException, InterruptedException {
    boolean sound = true;

    stats.totalTimer.start();

    if(startTime == 0) {
      startTime = System.currentTimeMillis();
    }

    boolean continueAnalysis;

    do {
      continueAnalysis = false;

      // run algorithm
      sound &= algorithm.run(reached);

      AbstractElement lastElement = reached.getLastElement();

      // if the element is a target element do refinement
      if (AbstractElements.isTargetElement(lastElement)) {
        if ((stopRefiningThreshold == -1 || System.currentTimeMillis() - startTime <= stopRefiningThreshold) &&
            (stopRefiningCount == -1 || stopRefiningCount > refinementCount) &&
            !(resets == 0 && noRefinementInFirstRun)) {
          refinementCount++;
          logger.log(Level.FINE, "Error found, performing CEGAR");
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

            logger.log(Level.FINE, "Refinement successful");
            stats.countSuccessfulRefinements++;

            if (restartOnRefinement) {
              // TODO
            }

            runGC();

            continueAnalysis = true;

          } else {
            // no refinement found, because the counterexample is not spurious
            logger.log(Level.FINE, "Refinement unsuccessful");
          }
        } else {
          stats.timedOut = true;
        }
      } // if lastElement is target element

    } while (continueAnalysis);

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
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (algorithm instanceof StatisticsProvider) {
      ((StatisticsProvider)algorithm).collectStatistics(pStatsCollection);
    }
    pStatsCollection.add(stats);
  }

  @Override
  public boolean reset() {
    startTime = 0;
    refinementCount = 0;
    resets++;
    return algorithm.reset();
  }
}