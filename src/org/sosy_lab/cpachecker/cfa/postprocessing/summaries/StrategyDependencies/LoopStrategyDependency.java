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
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategyInterface;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.loops.ConstantExtrapolationStrategy;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.loops.LinearExtrapolationStrategy;

public class LoopStrategyDependency implements StrategyDependencyInterface {

  @Override
  public boolean apply(StrategyInterface pStrategy, Integer pIteration) {
    if (pStrategy.getClass().equals(ConstantExtrapolationStrategy.class) && pIteration >= 0) {
      return true;
    } else if (pStrategy instanceof LinearExtrapolationStrategy && pIteration >= 1) {
      return true;
    } else if (pIteration >= 2) {
      return true;
    }
    return false;
  }

  @Override
  public List<StrategiesEnum> filter(List<StrategiesEnum> pAvailableStrategies) {
    List<StrategiesEnum> preferredStrategies = new ArrayList<>();
    if (pAvailableStrategies.contains(StrategiesEnum.LoopConstantExtrapolation)) {
      preferredStrategies.add(StrategiesEnum.LoopConstantExtrapolation);
    } else if (pAvailableStrategies.contains(StrategiesEnum.LoopLinearExtrapolation)) {
      preferredStrategies.add(StrategiesEnum.LoopLinearExtrapolation);
    } else if (pAvailableStrategies.contains(StrategiesEnum.LoopAcceleration)) {
      preferredStrategies.add(StrategiesEnum.LoopAcceleration);
    } else if (pAvailableStrategies.contains(StrategiesEnum.NaiveLoopAcceleration)) {
      preferredStrategies.add(StrategiesEnum.NaiveLoopAcceleration);
    } else {
      preferredStrategies = pAvailableStrategies;
    }
    return preferredStrategies;
  }
}
