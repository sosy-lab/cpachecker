/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.bdd;

import java.util.regex.Pattern;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.util.VariableClassification;

import com.google.common.base.Optional;

@Options(prefix = "cpa.bdd")
public class BDDPrecision implements Precision {

  @Option(description = "which vars should be tracked, default: track all. "
      + "the regex is not used for functionname, only for the variablename itself.")
  private String whiteListRegex = ".*";

  private final Pattern whiteListPattern;

  @Option(description = "track boolean variables from cfa only,"
      + " this option limits the whitelist")
  private boolean trackBooleanFromCFA = true;

  private final Optional<VariableClassification> varClass;

  public BDDPrecision(Configuration config, Optional<VariableClassification> vc)
      throws InvalidConfigurationException {
    config.inject(this);
    this.varClass = vc;
    whiteListPattern = Pattern.compile(whiteListRegex);
  }

  boolean isDisabled() {
    return whiteListPattern.pattern().isEmpty();
  }

  /**
   * This method tells if the precision demands the given variable to be tracked.
   *
   * @param variable the scoped name of the variable to check
   * @return true, if the variable has to be tracked, else false
   */
  boolean isTracking(String function, String var) {
    boolean isTracking = whiteListPattern.matcher(var).matches();
    boolean isTrackedBoolean = !trackBooleanFromCFA ||
        (varClass.isPresent() && varClass.get().getBooleanVars().containsEntry(function, var));
    return isTracking && isTrackedBoolean;
  }

}
