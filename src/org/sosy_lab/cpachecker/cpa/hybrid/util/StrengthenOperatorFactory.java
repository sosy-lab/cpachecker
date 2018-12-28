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
package org.sosy_lab.cpachecker.cpa.hybrid.util;

import java.util.HashMap;
import java.util.Map;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.hybrid.AssumptionGenerator;
import org.sosy_lab.cpachecker.cpa.hybrid.ValueAnalysisHybridStrengthenOperator;
import org.sosy_lab.cpachecker.cpa.hybrid.abstraction.HybridStrengthenOperator;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;

public final class StrengthenOperatorFactory {

  // cache
  private Map<String, HybridStrengthenOperator> operatorMap;

  private final AssumptionGenerator assumptionGenerator;
  private final LogManager logger;

  public StrengthenOperatorFactory(
      AssumptionGenerator pAssumptionGenerator,
      LogManager pLogger) {
    operatorMap = new HashMap<>();
    assumptionGenerator = pAssumptionGenerator;
    logger = pLogger;
  }

  /**
   * Factory method for strengthening operators
   *
   * @param state The state for which to provide the operator
   * @return A instance of an object implementing HybridStrengthenOperator
   */
  @SuppressWarnings("unchecked")
  public HybridStrengthenOperator provideStrengthenOperator(AbstractState state) {
    // first check if the
    String stateClassName = state.getClass().getName();
    if(operatorMap.containsKey(stateClassName)) {
      return operatorMap.get(stateClassName);
    }

    // check for new domain states

    if(state instanceof ValueAnalysisState) {
      return pushAndReturn(
          new ValueAnalysisHybridStrengthenOperator(
              assumptionGenerator,
              logger),
          stateClassName);
    }

    // fallback
    return new DefaultStrengthenOperator();
    }

    // assume existence in map is already checked
    private HybridStrengthenOperator pushAndReturn(
          HybridStrengthenOperator operator,
          String key) {
      operatorMap.put(key, operator);
      return operator;
    }
}