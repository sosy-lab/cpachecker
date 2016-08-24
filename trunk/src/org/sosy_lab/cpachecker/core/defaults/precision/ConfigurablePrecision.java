/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.defaults.precision;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Multimap;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.util.VariableClassification;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.io.IOException;
import java.io.Writer;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

@Options(prefix = "precision")
public class ConfigurablePrecision extends VariableTrackingPrecision {

  @Option(
    secure = true,
    name = "variableBlacklist",
    description =
        "blacklist regex for variables that won't be tracked by the CPA using this precision"
  )
  private Pattern variableBlacklist = Pattern.compile("");

  @Option(
    secure = true,
    name = "variableWhitelist",
    description =
        "whitelist regex for variables that will always be tracked by the CPA using this precision"
  )
  private Pattern variableWhitelist = Pattern.compile("");

  @Option(secure = true, description = "If this option is used, booleans from the cfa are tracked.")
  private boolean trackBooleanVariables = true;

  @Option(
    secure = true,
    description =
        "If this option is used, variables that are only compared" + " for equality are tracked."
  )
  private boolean trackIntEqualVariables = true;

  @Option(
    secure = true,
    description =
        "If this option is used, variables, that are only used in"
            + " simple calculations (add, sub, lt, gt, eq) are tracked."
  )
  private boolean trackIntAddVariables = true;

  @Option(
    secure = true,
    description =
        "If this option is used, variables that have type double" + " or float are tracked."
  )
  private boolean trackFloatVariables = true;

  @Option(
    secure = true,
    description =
        "If this option is used, variables that are addressed"
            + " may get tracked depending on the rest of the precision. When this option"
            + " is disabled, a variable that is addressed is definitely not tracked."
  )
  private boolean trackAddressedVariables = true;

  @Option(
    secure = true,
    description =
        "If this option is used, all variables that are"
            + " of a different classification than IntAdd, IntEq and Boolean get tracked"
            + " by the precision."
  )
  private boolean trackVariablesBesidesEqAddBool = true;

  private final Optional<VariableClassification> vc;
  private final Class<? extends ConfigurableProgramAnalysis> cpaClass;

  ConfigurablePrecision(
      Configuration config,
      Optional<VariableClassification> pVc,
      Class<? extends ConfigurableProgramAnalysis> cpaClass)
      throws InvalidConfigurationException {
    super();
    config.inject(this);
    this.cpaClass = cpaClass;
    this.vc = pVc;
  }

  @Override
  public boolean allowsAbstraction() {
    return !trackBooleanVariables
        || !trackIntEqualVariables
        || !trackIntAddVariables
        || !trackAddressedVariables
        || !trackVariablesBesidesEqAddBool
        || !variableBlacklist.toString().isEmpty();
  }

  @Override
  public boolean isTracking(MemoryLocation pVariable, Type pType, CFANode location) {
    if (trackFloatVariables) {
      return isTracking(pVariable);
    } else {
      return !((pType instanceof CSimpleType
                  && (((CSimpleType) pType).getType().isFloatingPointType()))
              || (pType instanceof JSimpleType
                  && (((JSimpleType) pType).getType().isFloatingPointType())))
          && isTracking(pVariable);
    }
  }

  private boolean isTracking(MemoryLocation pVariable) {
    return isOnWhitelist(pVariable.getIdentifier())
        || (!isOnBlacklist(pVariable.getIdentifier())
            && isInTrackedVarClass(pVariable.getAsSimpleString()));
  }

  private boolean isOnBlacklist(String variable) {
    return !variableBlacklist.toString().isEmpty() && variableBlacklist.matcher(variable).matches();
  }

  private boolean isOnWhitelist(String variable) {
    return !variableWhitelist.toString().isEmpty() && variableWhitelist.matcher(variable).matches();
  }

  /** returns true, iff the variable is in an varClass, that should be ignored. */
  private boolean isInTrackedVarClass(final String variableName) {
    // when there is no variable classification we cannot make any assumptions
    // about the tracking of variables and say that all variables are tracked
    if (!vc.isPresent()) {
      return true;
    }
    VariableClassification varClass = vc.get();

    final boolean varIsAddressed = varClass.getAddressedVariables().contains(variableName);

    // addressed variables do not belong to a specific type, so they have to
    // be handled extra. We want the precision to be as strict as possible,
    // therefore, when a variable is addressed but addressed variables should
    // not be tracked, we do not consider the other parts of the variable classification
    if (varIsAddressed && !trackAddressedVariables) {
      return false;

      // in this case addressed variables can at most be included in the
      // tracking variables and the rest of the variable classification is
      // the limiting factor
    } else {
      final boolean varIsBoolean = varClass.getIntBoolVars().contains(variableName);
      final boolean varIsIntEqual = varClass.getIntEqualVars().contains(variableName);
      final boolean varIsIntAdd = varClass.getIntAddVars().contains(variableName);

      // if the variable is not in a matching classification we have to check
      // if other variables should be tracked
      if (!(varIsBoolean || varIsIntAdd || varIsIntEqual)) {
        return trackVariablesBesidesEqAddBool;
      }

      final boolean isTrackedBoolean = trackBooleanVariables && varIsBoolean;
      final boolean isTrackedIntEqual = trackIntEqualVariables && varIsIntEqual;
      final boolean isTrackedIntAdd = trackIntAddVariables && varIsIntAdd;

      return isTrackedBoolean || isTrackedIntAdd || isTrackedIntEqual;
    }
  }

  @Override
  public VariableTrackingPrecision withIncrement(Multimap<CFANode, MemoryLocation> pIncrement) {
    return this;
  }

  @Override
  public void serialize(Writer writer) throws IOException {
    writer.write("# configured precision used - nothing to show here");
  }

  @Override
  public VariableTrackingPrecision join(VariableTrackingPrecision consolidatedPrecision) {
    Preconditions.checkArgument((getClass().equals(consolidatedPrecision.getClass())));
    return this;
  }

  @Override
  public int getSize() {
    return -1;
  }

  @Override
  public boolean isEmpty() {
    if (!variableWhitelist.toString().isEmpty()) {
      return false;
    }
    if (!vc.isPresent()) {
      return true;
    }
    VariableClassification varClass = vc.get();

    boolean trackSomeIntBools = trackBooleanVariables && !varClass.getIntBoolVars().isEmpty();
    boolean trackSomeIntEquals = trackIntEqualVariables && !varClass.getIntEqualVars().isEmpty();
    boolean trackSomeIntAdds = trackIntAddVariables && !varClass.getIntAddVars().isEmpty();

    return !(trackSomeIntBools
        || trackSomeIntEquals
        || trackSomeIntAdds
        || trackVariablesBesidesEqAddBool);
  }

  @Override
  protected Class<? extends ConfigurableProgramAnalysis> getCPAClass() {
    return cpaClass;
  }

  @Override
  public boolean tracksTheSameVariablesAs(VariableTrackingPrecision pOtherPrecision) {
    if (pOtherPrecision.getClass().equals(getClass())) {
      ConfigurablePrecision precisionCompare = (ConfigurablePrecision) pOtherPrecision;
      if (variableBlacklist.equals(precisionCompare.variableBlacklist)
          && variableWhitelist.equals(precisionCompare.variableWhitelist)
          && trackBooleanVariables == precisionCompare.trackBooleanVariables
          && trackIntEqualVariables == precisionCompare.trackIntEqualVariables
          && trackIntAddVariables == precisionCompare.trackIntAddVariables
          && trackFloatVariables == precisionCompare.trackFloatVariables
          && trackAddressedVariables == precisionCompare.trackAddressedVariables
          && vc.isPresent() == precisionCompare.vc.isPresent()
          && vc.isPresent()
          && vc.get().equals(precisionCompare.vc.get())
          && cpaClass.equals(precisionCompare.cpaClass)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof ConfigurablePrecision
        && tracksTheSameVariablesAs((ConfigurablePrecision) other);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        variableBlacklist,
        variableWhitelist,
        trackBooleanVariables,
        trackIntEqualVariables,
        trackIntAddVariables,
        trackFloatVariables,
        trackAddressedVariables);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(ConfigurablePrecision.class)
        .add("CPA", cpaClass.getSimpleName())
        .add("blacklist", variableBlacklist)
        .add("whitelist", variableWhitelist)
        .add("trackBooleanVariables", trackBooleanVariables)
        .add("trackIntEqualVariables", trackIntEqualVariables)
        .add("trackIntAddVariables", trackIntAddVariables)
        .add("trackFloatVariables", trackFloatVariables)
        .add("trackAddressedVariables", trackAddressedVariables)
        .toString();
  }
}
