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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

import cmdline.CPAMain;
import cpa.common.ReachedElements;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.Refiner;
import exceptions.CPAException;
import exceptions.TransferTimeOutException;

public class CEGARAlgorithm implements Algorithm {

  private static final int GC_PERIOD = 100;
  private int gcCounter = 0;

  private static long modifySetsTime=0;
  private static long refinementTime = 0;

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

    String refinerName = CPAMain.cpaConfig.getProperty("cegar.refiner");
    Object[] refinerArguments = {algorithm.getCPA()};

    mRefiner = createInstance(refinerName, refinerArguments, Refiner.class);
  }

  @Override
  public void run(ReachedElements reached, boolean stopAfterError) throws CPAException {

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

      // if the element is an error element
      if (reached.getLastElement().isError()) {

        CPAMain.logManager.log(Level.FINER, "Error found, performing CEGAR");

        long startRef = System.currentTimeMillis();

        boolean refinementResult = mRefiner.performRefinement(reached);
        long endRef = System.currentTimeMillis();
        refinementTime = refinementTime + (endRef  - startRef);

        if (refinementResult) {
          // successful refinement

          CPAMain.logManager.log(Level.FINER, "Refinement successful");

          long start = System.currentTimeMillis();

          if (CPAMain.cpaConfig.getBooleanValue("cegar.restartOnRefinement")) {
            // TODO
          }

          long end = System.currentTimeMillis();
          modifySetsTime = modifySetsTime + (end - start);

          runGC();
          
          stopAnalysis = false;

        } else {
          // no refinement found, because the counterexample is not spurious
          CPAMain.logManager.log(Level.FINER, "Refinement unsuccessful");

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
    return;
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