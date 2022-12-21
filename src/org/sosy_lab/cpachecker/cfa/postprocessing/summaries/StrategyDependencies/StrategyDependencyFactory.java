// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategyDependencies;

public class StrategyDependencyFactory {

  public StrategyDependency createStrategy(StrategyDependencyEnum pStrategy) {

    switch (pStrategy) {
      case BASESTRATEGYDEPENDENCY:
        return new BaseStrategyDependency();
      case LOOPSTRATEGYDEPENDENCY:
        return new LoopStrategyDependency();
      case LOOPSTRATEGYMOSTGENERALORDERINGDEPENDENCY:
        return new LoopStrategyMostGeneralOrderingDependency();
      case LOOPSTRATEGYOVERFLOWDEPENDENCY:
        return new LoopStrategyOverflowDependency();
      case NOSTRATEGYDEPENDENCY:
        return new NoStrategyDependency();
      case UNDERAPPROXIMATINGDEPENDENCY:
        return new UnderapproximatingStrategyDependency();
      default:
        return null;
    }
  }
}
