// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package org.sosy_lab.cpachecker.core.algorithm.legion.selection;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

/** Selects a random state from the reached set which has a nondet-mark. */
public class RandomSelectionStrategy implements Selector {

  private final LogManager logger;
  private final Random random = new Random(1200709844L);
  private final Set<PathFormula> blacklisted = new HashSet<>();
  private final StatTimer iterationTimer = new StatTimer(StatKind.SUM, "Selection time");

  public RandomSelectionStrategy(LogManager logger) {
    this.logger = logger;
  }

  @Override
  public PathFormula select(ReachedSet reachedSet) {
    this.iterationTimer.start();
    List<ARGState> nonDetStates = getNondetStates(reachedSet);
    PathFormula target;
    while (true) {
      ARGState state = nonDetStates.remove(this.random.nextInt(nonDetStates.size()));
      PredicateAbstractState ps =
          AbstractStates.extractStateByType(state, PredicateAbstractState.class);
      target = ps.getPathFormula();

      // If the just selected target is the last one,
      // No choice but to return it.
      if (nonDetStates.isEmpty()) {
        break;
      }

      // Otherwhise, check if it's blacklisted
      if (!this.blacklisted.contains(target)) {
        break;
      }
    }
    this.iterationTimer.stop();
    return target;
  }

  /** Find all nondet-marked states from the reachedSet */
  List<ARGState> getNondetStates(ReachedSet reachedSet) {
    List<ARGState> nonDetStates = new ArrayList<>();
    for (AbstractState state : reachedSet.asCollection()) {
      ValueAnalysisState vs = AbstractStates.extractStateByType(state, ValueAnalysisState.class);
      if (vs.isMarkedNonDeterministic()) {
        for (AbstractState parent : ((ARGState) state).getParents()) {
          ValueAnalysisState parentVAState =
              AbstractStates.extractStateByType(parent, ValueAnalysisState.class);
          logger.log(Level.FINE, "NonDeterministic state found.", parentVAState.getConstants());
          nonDetStates.add((ARGState) parent);
        }
      }
    }
    return nonDetStates;
  }

  /**
   * The random selector just blacklists states with a weight below zero. Selection will then try to
   * select a state not blacklisted, only when there is no other choice it will return one.
   */
  @Override
  public void feedback(PathFormula pState, int pWeight) {
    if (pWeight < 0) {
      this.blacklisted.add(pState);
    }
  }

  @Override
  public void printStatistics(StatisticsWriter writer) {
    writer.put(this.iterationTimer);
    writer.put("Selection Iterations", this.iterationTimer.getUpdateCount());
    writer.put("Blacklisted States", this.blacklisted.size());
  }
}
