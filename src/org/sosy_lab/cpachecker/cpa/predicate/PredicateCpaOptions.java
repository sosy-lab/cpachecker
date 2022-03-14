// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;

@Options(prefix = "cpa.predicate")
class PredicateCpaOptions {

  @Option(
      secure = true,
      name = "satCheck",
      description =
          "maximum blocksize before a satisfiability check is done\n"
              + "(non-negative number, 0 means never, if positive should be smaller than"
              + " blocksize)")
  private int satCheckBlockSize = 0;

  @Option(
      secure = true,
      description =
          "Enables sat checks at abstraction location.\n"
              + "Infeasible paths are already excluded by transfer relation and not later by"
              + " precision adjustment. This property is required in proof checking.")
  private boolean satCheckAtAbstraction = false;

  @Option(secure = true, description = "check satisfiability when a target state has been found")
  private boolean targetStateSatCheck = false;

  @Option(
      secure = true,
      description = "do not include assumptions of states into path formula during strengthening")
  private boolean ignoreStateAssumptions = false;

  @Option(secure = true, description = "Use formula reporting states for strengthening.")
  private boolean strengthenWithFormulaReportingStates = false;

  @Option(
      secure = true,
      description = "Check satisfiability for plain conjunction of edge and assumptions.")
  private boolean assumptionStrengtheningSatCheck = false;

  PredicateCpaOptions(Configuration config) throws InvalidConfigurationException {
    config.inject(this);
  }

  int getSatCheckBlockSize() {
    return satCheckBlockSize;
  }

  boolean satCheckAtAbstraction() {
    return satCheckAtAbstraction;
  }

  boolean targetStateSatCheck() {
    return targetStateSatCheck;
  }

  boolean ignoreStateAssumptions() {
    return ignoreStateAssumptions;
  }

  boolean strengthenWithFormulaReportingStates() {
    return strengthenWithFormulaReportingStates;
  }

  public boolean assumptionStrengtheningSatCheck() {
    return assumptionStrengtheningSatCheck;
  }
}
