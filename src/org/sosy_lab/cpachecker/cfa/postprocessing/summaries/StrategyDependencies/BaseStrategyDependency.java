// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategyDependencies;

import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategiesEnum;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.Strategy;

public class BaseStrategyDependency implements StrategyDependency {

  @Override
  public boolean apply(Strategy pStrategy, Integer pIteration) {
    return false;
  }

  @Override
  public List<StrategiesEnum> filter(List<StrategiesEnum> pAvailableStrategies) {
    List<StrategiesEnum> preferredStrategies = new ArrayList<>();
    if (pAvailableStrategies.contains(StrategiesEnum.BASE)) {
      preferredStrategies.add(StrategiesEnum.BASE);
    }
    return preferredStrategies;
  }

  @Override
  public boolean stopPostProcessing(Integer pIteration, boolean pChangesInCFA) {
    return true;
  }
}
