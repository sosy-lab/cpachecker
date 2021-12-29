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

public class LoopStrategyMostGeneralOrderingDependency extends LoopStrategyDependency {

  @Override
  public List<StrategiesEnum> filter(List<StrategiesEnum> pAvailableStrategies) {
    List<StrategiesEnum> preferredStrategies = new ArrayList<>();

    if (pAvailableStrategies.contains(StrategiesEnum.HavocStrategy)) {
      preferredStrategies.add(StrategiesEnum.HavocStrategy);
    } else if (pAvailableStrategies.contains(StrategiesEnum.NaiveLoopAcceleration)) {
      preferredStrategies.add(StrategiesEnum.NaiveLoopAcceleration);
    } else if (pAvailableStrategies.contains(StrategiesEnum.LoopAcceleration)) {
      preferredStrategies.add(StrategiesEnum.LoopAcceleration);
    } else if (pAvailableStrategies.contains(StrategiesEnum.NonDetBoundConstantExtrapolation)) {
      preferredStrategies.add(StrategiesEnum.NonDetBoundConstantExtrapolation);
    } else if (pAvailableStrategies.contains(StrategiesEnum.LoopConstantExtrapolation)) {
      preferredStrategies.add(StrategiesEnum.LoopConstantExtrapolation);
    } else if (pAvailableStrategies.contains(StrategiesEnum.LoopLinearExtrapolation)) {
      preferredStrategies.add(StrategiesEnum.LoopLinearExtrapolation);
    } else if (pAvailableStrategies.contains(StrategiesEnum.Base)) {
      preferredStrategies.add(StrategiesEnum.Base);
    }
    return preferredStrategies;
  }
}
