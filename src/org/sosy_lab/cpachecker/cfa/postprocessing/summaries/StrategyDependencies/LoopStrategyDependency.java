// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategyDependencies;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategiesEnum;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.Strategy;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.loops.ConstantExtrapolationStrategy;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.loops.LinearExtrapolationStrategy;

public class LoopStrategyDependency implements StrategyDependency {

  private final List<StrategiesEnum> strategyOrder =
      Arrays.asList(
          StrategiesEnum.LOOPCONSTANTEXTRAPOLATION,
          StrategiesEnum.LOOPLINEAREXTRAPOLATION,
          StrategiesEnum.OUTPUTLOOPACCELERATION,
          StrategiesEnum.NAIVELOOPACCELERATION,
          StrategiesEnum.NONDETBOUNDCONSTANTEXTRAPOLATION,
          StrategiesEnum.HAVOCSTRATEGY,
          StrategiesEnum.BASE);

  @Override
  public boolean apply(Strategy pStrategy, Integer pIteration) {
    if (pStrategy instanceof ConstantExtrapolationStrategy && pIteration >= 0) {
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
    strategyOrder.stream()
        .filter(pAvailableStrategies::contains)
        .findFirst()
        .ifPresent(preferredStrategies::add);
    return preferredStrategies;
  }

  @Override
  public boolean stopPostProcessing(Integer pIteration, boolean pChangesInCFA) {
    if (pIteration < 3) {
      return false;
    } else {
      return !pChangesInCFA;
    }
  }
}
