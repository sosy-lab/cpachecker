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

    if (pAvailableStrategies.contains(StrategiesEnum.HAVOCSTRATEGY)) {
      preferredStrategies.add(StrategiesEnum.HAVOCSTRATEGY);
    } else if (pAvailableStrategies.contains(StrategiesEnum.NAIVELOOPACCELERATION)) {
      preferredStrategies.add(StrategiesEnum.NAIVELOOPACCELERATION);
    } else if (pAvailableStrategies.contains(StrategiesEnum.OUTPUTLOOPACCELERATION)) {
      preferredStrategies.add(StrategiesEnum.OUTPUTLOOPACCELERATION);
    } else if (pAvailableStrategies.contains(StrategiesEnum.NONDETBOUNDCONSTANTEXTRAPOLATION)) {
      preferredStrategies.add(StrategiesEnum.NONDETBOUNDCONSTANTEXTRAPOLATION);
    } else if (pAvailableStrategies.contains(StrategiesEnum.LOOPCONSTANTEXTRAPOLATION)) {
      preferredStrategies.add(StrategiesEnum.LOOPCONSTANTEXTRAPOLATION);
    } else if (pAvailableStrategies.contains(StrategiesEnum.LOOPLINEAREXTRAPOLATION)) {
      preferredStrategies.add(StrategiesEnum.LOOPLINEAREXTRAPOLATION);
    } else if (pAvailableStrategies.contains(StrategiesEnum.LOOPUNROLLING)) {
      preferredStrategies.add(StrategiesEnum.LOOPUNROLLING);
    } else if (pAvailableStrategies.contains(StrategiesEnum.BASE)) {
      preferredStrategies.add(StrategiesEnum.BASE);
    }
    return preferredStrategies;
  }
}
