/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
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
              + "(non-negative number, 0 means never, if positive should be smaller than blocksize)")
  private int satCheckBlockSize = 0;

  @Option(
      secure = true,
      description =
          "Enables sat checks at abstraction location.\n"
              + "Infeasible paths are already excluded by transfer relation and not later by precision adjustment. This property is required in proof checking.")
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
