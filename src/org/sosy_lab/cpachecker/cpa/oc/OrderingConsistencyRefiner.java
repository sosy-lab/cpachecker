// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.oc;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class OrderingConsistencyRefiner implements Refiner {

  private final ConfigurableProgramAnalysis cpa;

  OrderingConsistencyRefiner(ConfigurableProgramAnalysis pCpa) {
    cpa = pCpa;
  }

  public static Refiner create(ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    return new OrderingConsistencyRefiner(pCpa);
  }

  @Override
  public boolean performRefinement(ReachedSet pReached) throws CPAException, InterruptedException {
    final Map<Integer, Set<AbstractState>> threads = new HashMap<>();
    for (AbstractState abstractState : pReached.asCollection()) {
      OrderingConsistencyState orderingConsistencyState =
          AbstractStates.extractStateByType(abstractState, OrderingConsistencyState.class);
      if(orderingConsistencyState != null) {
      orderingConsistencyState
              .pid()
              .ifPresent(
                  pid ->
                      threads
                          .computeIfAbsent(pid, k -> new HashSet<>())
                          .add(abstractState));
      }
      }
    return false;
  }
}
