// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package org.sosy_lab.cpachecker.core.algorithm.legion.selection;

import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

public interface Selector {
  /** Select a target state and return the path formula that leads to it. */
  PathFormula select(ReachedSet pReachedSet) throws InterruptedException;

  /**
   * Sometimes, others might want to influence which state(s) might be selected.
   *
   * <p>This feedback-mechanism is based on the following contract: Other phases might mark a state
   * with the given weight for the selector. A posivitive weight leads to this state beeing selected
   * with a higher percentage, a negative weight with a lower one. The actual mechanism is
   * implemented by the selector and does not guarantee anything (like this default) but might try
   * to respect weighted selection if possible.
   *
   * @param pPathFormula Which state to mark.
   * @param weight Weight to assign to the state.
   */
  default void feedback(PathFormula pPathFormula, int weight) {
    // The default is to do nothing
    return;
  }

  void writeStats(StatisticsWriter writer);
}
