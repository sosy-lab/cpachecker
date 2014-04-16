/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.pcc.strategy;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.sosy_lab.common.Classes;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.pcc.PCCStrategy;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker;
import org.sosy_lab.cpachecker.cpa.PropertyChecker.PropertyCheckerCPA;


public class PCCStrategyBuilder {

  private static final String STRATEGY_CLASS_PREFIX = "org.sosy_lab.cpachecker.pcc.strategy";
  private static final String PARALLEL_STRATEGY_CLASS_PREFIX = "org.sosy_lab.cpachecker.pcc.strategy.parallel";

  public static PCCStrategy buildStrategy(String pPccStrategy, Configuration pConfig, LogManager pLogger,
      ShutdownNotifier pShutdownNotifier, ConfigurableProgramAnalysis pCpa) throws InvalidConfigurationException {
    if (pPccStrategy == null) { throw new InvalidConfigurationException(
        "No PCC strategy defined."); }

    Class<?> pccStrategyClass;
    try {
      pccStrategyClass = Classes.forName(pPccStrategy, STRATEGY_CLASS_PREFIX);
    } catch (ClassNotFoundException e) {
      try {
        pccStrategyClass = Classes.forName(pPccStrategy, PARALLEL_STRATEGY_CLASS_PREFIX);
      } catch (ClassNotFoundException e1) {
        throw new InvalidConfigurationException(
            "Class for pcc checker  " + pPccStrategy + " is unknown.", e1);
      }
    }

    if (!PCCStrategy.class.isAssignableFrom(pccStrategyClass)) { throw new InvalidConfigurationException(
        "Specified class " + pPccStrategy + "does not implement the pPccStrategy interface!"); }

    // construct property checker instance
    try {
      Constructor<?>[] cons = pccStrategyClass.getConstructors();

      Class<?>[] paramTypes;
      for (Constructor<?> con : cons) {
        paramTypes = con.getParameterTypes();
        if (paramTypes.length != 4) {
          continue;
        } else {
          if (paramTypes[0] == Configuration.class
              && paramTypes[1] == LogManager.class
              && paramTypes[2] == ShutdownNotifier.class) {
            if (pCpa == null) {
              return (PCCStrategy) con.newInstance(pConfig, pLogger, pShutdownNotifier, pCpa);
            }
            if (paramTypes[3] == ProofChecker.class) {
              if (!(pCpa instanceof ProofChecker)) {
                continue;
              }
              return (PCCStrategy) con.newInstance(pConfig, pLogger, pShutdownNotifier, pCpa);
            }
            if (paramTypes[3] == PropertyCheckerCPA.class) {
              if (!(pCpa instanceof PropertyCheckerCPA)) {
                continue;
              }
              return (PCCStrategy) con.newInstance(pConfig,
                  pLogger, pShutdownNotifier, pCpa);
            }
          }
        }
      }

      throw new UnsupportedOperationException(
          "Cannot create PCC Strategy "
              + pPccStrategy
              +
              " if it does not provide a constructor (Configuration, LogManager, ShutdownNotifier, (PropertyCheckerCPA|ProofChecker)");
    } catch (SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException
        | InvocationTargetException e) {
      throw new UnsupportedOperationException(
          "Creation of specified PropertyChecker instance failed.", e);
    }
  }
}
