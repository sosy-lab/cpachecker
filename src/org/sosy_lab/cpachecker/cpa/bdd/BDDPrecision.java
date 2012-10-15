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

  @Option(description = "track boolean variables from cfa")
  private boolean trackBooleans = true;

  @Option(description = "track simple numeral variables from cfa as bitvectors")
  private boolean trackDiscretes = true;

  @Option(description = "track variables, only used in simple calculations, from cfa as bitvectors")
  private boolean trackSimpleCalcs = true;

  @Option(name="forceTrackingPattern",
      description="Pattern for variablenames that will always be tracked with BDDs")
  private String forceTrackingPatternStr = "";

  private final Pattern forceTrackingPattern;

  private final Optional<VariableClassification> varClass;

  public BDDPrecision(Configuration config, Optional<VariableClassification> vc)
      throws InvalidConfigurationException {
    config.inject(this);
    if (forceTrackingPatternStr != "") {
      this.forceTrackingPattern = Pattern.compile(forceTrackingPatternStr);
    } else {
      this.forceTrackingPattern = null;
    }
    this.varClass = vc;
  }

  public boolean isDisabled() {
    if (forceTrackingPattern != null) return false;

    if (!varClass.isPresent()) { return true; }

    boolean trackSomeBooleans = trackBooleans &&
        !varClass.get().getBooleanVars().isEmpty();
    boolean trackSomeDiscretes = trackDiscretes &&
        !varClass.get().getDiscreteValueVars().isEmpty();
    boolean trackSomeSimpleCalcs = trackSimpleCalcs &&
        !varClass.get().getSimpleCalcVars().isEmpty();

    return !(trackSomeBooleans || trackSomeDiscretes || trackSomeSimpleCalcs);
  }

  /**
   * This method tells if the precision demands the given variable to be tracked.
   *
   * @param variable the name of the variable to check
   * @param variable function of current scope or null, if variable is global
   * @return true, if the variable has to be tracked, else false
   */
  public boolean isTracking(String function, String var) {
    if (this.forceTrackingPattern.matcher(var).matches()) {
      return true;
    }

    if (!varClass.isPresent()) { return false; }

    boolean isTrackedBoolean = trackBooleans &&
        varClass.get().getBooleanVars().containsEntry(function, var);

    // if a var is both boolean AND discrete, do NOT track it as discrete var!
    boolean isTrackedDiscrete = trackDiscretes &&
        !varClass.get().getBooleanVars().containsEntry(function, var) &&
        varClass.get().getDiscreteValueVars().containsEntry(function, var);

    // if a var is boolean, discrete and part of simple calcs, do NOT track it as simple-calc-var!
    boolean isTrackedSimpleNumber = trackSimpleCalcs &&
        !varClass.get().getBooleanVars().containsEntry(function, var) &&
        !varClass.get().getDiscreteValueVars().containsEntry(function, var) &&
        varClass.get().getSimpleCalcVars().containsEntry(function, var);

    return isTrackedBoolean || isTrackedDiscrete || isTrackedSimpleNumber;
  }
}
