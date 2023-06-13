// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.weakening;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.util.predicates.weakening.CEXWeakeningManager.SELECTION_STRATEGY;
import org.sosy_lab.cpachecker.util.predicates.weakening.InductiveWeakeningManager.WEAKENING_STRATEGY;

@Options(prefix = "cpa.slicing")
public class WeakeningOptions {

  @Option(description = "Inductive weakening strategy", secure = true)
  private WEAKENING_STRATEGY weakeningStrategy = WEAKENING_STRATEGY.CEX;

  @Option(secure = true, description = "Pre-run syntactic weakening")
  private boolean preRunSyntacticWeakening = true;

  @Option(description = "Strategy for abstracting children during CEX weakening", secure = true)
  private SELECTION_STRATEGY removalSelectionStrategy = SELECTION_STRATEGY.ALL;

  @Option(description = "Depth limit for the 'LEAST_REMOVALS' strategy.")
  private int leastRemovalsDepthLimit = 2;

  public WeakeningOptions(Configuration config) throws InvalidConfigurationException {
    config.inject(this);
  }

  WEAKENING_STRATEGY getWeakeningStrategy() {
    return weakeningStrategy;
  }

  boolean doPreRunSyntacticWeakening() {
    return preRunSyntacticWeakening;
  }

  SELECTION_STRATEGY getRemovalSelectionStrategy() {
    return removalSelectionStrategy;
  }

  int getLeastRemovalsDepthLimit() {
    return leastRemovalsDepthLimit;
  }
}
