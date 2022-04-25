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
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.loops.ConstantExtrapolationStrategy;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.loops.LinearExtrapolationStrategy;

public class LoopStrategyDependency implements StrategyDependency {

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
    if (pAvailableStrategies.contains(StrategiesEnum.LOOPCONSTANTEXTRAPOLATION)) {
      preferredStrategies.add(StrategiesEnum.LOOPCONSTANTEXTRAPOLATION);
    } else if (pAvailableStrategies.contains(StrategiesEnum.LOOPLINEAREXTRAPOLATION)) {
      preferredStrategies.add(StrategiesEnum.LOOPLINEAREXTRAPOLATION);
    } else if (pAvailableStrategies.contains(StrategiesEnum.NAIVELOOPACCELERATION)) {
      preferredStrategies.add(StrategiesEnum.NAIVELOOPACCELERATION);
    } else if (pAvailableStrategies.contains(StrategiesEnum.BASE)) {
      preferredStrategies.add(StrategiesEnum.BASE);
    } else if (pAvailableStrategies.contains(StrategiesEnum.HAVOCSTRATEGY)) {
      preferredStrategies.add(StrategiesEnum.HAVOCSTRATEGY);
    } else if (pAvailableStrategies.contains(StrategiesEnum.NONDETBOUNDCONSTANTEXTRAPOLATION)) {
      preferredStrategies.add(StrategiesEnum.NONDETBOUNDCONSTANTEXTRAPOLATION);
    }
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
