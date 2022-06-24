// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategyDependencies;

import java.util.List;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategiesEnum;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.Strategy;

public interface StrategyDependency {

  /** Returns true if the CFA postprocessing should stop or not */
  public boolean stopPostProcessing(Integer iteration, boolean changesInCFA);

  /** Returns true if the Strategy can be applied in this iteration, else false */
  public boolean apply(Strategy pStrategy, Integer iteration);

  /**
   * Given a Set of Strategies which can be used to get the successor of the current strategy This
   * solves the Problem of Strategies depending on one another to be applied
   */
  public List<StrategiesEnum> filter(List<StrategiesEnum> availableStrategies);
}
