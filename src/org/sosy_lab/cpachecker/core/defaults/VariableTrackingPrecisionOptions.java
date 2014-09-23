/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.core.defaults;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;

@Options(prefix="precision")
public final class VariableTrackingPrecisionOptions {

  enum Sharing {
    SCOPE, LOCATION;
  }

  public VariableTrackingPrecisionOptions(Configuration config) throws InvalidConfigurationException {
    config.inject(this);
  }

  @Option(description = "the threshold which controls whether or not variable"
      + "valuations ought to be abstracted once the specified number of valuations"
      + "per variable is reached in the set of reached states")
  private int reachedSetThreshold = -1;

  @Option(values={"LOCATION", "SCOPE"},
      description = "whether to track relevant variables only at the exact program"
          + "location (sharing=location), or within their respective"
          + "(function-/global-) scope (sharing=scoped).")
  private Sharing sharing = Sharing.SCOPE;

  @Option(description = "ignore boolean variables. if this option is used, "
      + "booleans from the cfa should tracked with another CPA, "
      + "i.e. with BDDCPA.")
  private boolean ignoreBoolean = false;

  @Option(description = "ignore variables, that are only compared for equality. "
      + "if this option is used, these variables from the cfa should "
      + "tracked with another CPA, i.e. with BDDCPA.")
  private boolean ignoreIntEqual = false;

  @Option(description = "ignore variables, that are only used in simple " +
      "calculations (add, sub, lt, gt, eq). "
      + "if this option is used, these variables from the cfa should "
      + "tracked with another CPA, i.e. with BDDCPA.")
  private boolean ignoreIntAdd = false;

  @Option(description ="Ignore variables that have type double or float.")
  private boolean ignoreFloats = true;

  public final int getReachedSetThreshold() {
    return reachedSetThreshold;
  }

  public final Sharing getSharingStrategy() {
    return sharing;
  }

  public final boolean ignoreBooleanVariables() {
    return ignoreBoolean;
  }

  public final boolean ignoreIntEqualVariables() {
    return ignoreIntEqual;
  }

  public final boolean ignoreIntAddVariables() {
    return ignoreIntAdd;
  }

  public final boolean ignoreFloatVariables() {
    return ignoreFloats;
  }

  public static VariableTrackingPrecisionOptions getDefaultOptions() {
    try {
      return new VariableTrackingPrecisionOptions(Configuration.defaultConfiguration());
    } catch (InvalidConfigurationException e) {
      // as we just use the default configuration this will never happen
      throw new RuntimeException();
    }
  }
}
