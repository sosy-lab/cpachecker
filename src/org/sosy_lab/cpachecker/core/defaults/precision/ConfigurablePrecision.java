// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.defaults.precision;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.IOException;
import java.io.Writer;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;

@Options(prefix = "precision")
public class ConfigurablePrecision extends VariableTrackingPrecision {

  @Option(
      secure = true,
      name = "variableBlacklist",
      description =
          "blacklist regex for variables that won't be tracked by the CPA using this precision")
  private Pattern variableBlacklist = Pattern.compile("");

  @Option(
      secure = true,
      name = "variableWhitelist",
      description =
          "whitelist regex for variables that will always be tracked by the CPA using this"
              + " precision")
  private Pattern variableWhitelist = Pattern.compile("");

  @Option(secure = true, description = "If this option is used, booleans from the CFA are tracked.")
  private boolean trackBooleanVariables = true;

  @Option(
      secure = true,
      description =
          "If this option is used, variables that are only compared for equality are tracked.")
  private boolean trackIntEqualVariables = true;

  @Option(
      secure = true,
      description =
          "If this option is used, variables, that are only used in"
              + " simple calculations (add, sub, lt, gt, eq) are tracked.")
  private boolean trackIntAddVariables = true;

  @Option(
      secure = true,
      description = "If this option is used, variables that have type double or float are tracked.")
  private boolean trackFloatVariables = true;

  @Option(
      secure = true,
      description =
          "If this option is used, variables that are addressed"
              + " may get tracked depending on the rest of the precision. When this option"
              + " is disabled, a variable that is addressed is definitely not tracked.")
  private boolean trackAddressedVariables = true;

  @Option(
      secure = true,
      description =
          "If this option is used, all variables that are"
              + " of a different classification than IntAdd, IntEq and Boolean get tracked"
              + " by the precision.")
  private boolean trackVariablesBesidesEqAddBool = true;

  @Option(
      secure = true,
      description = "If this option is used, variables that are irrelevantare also tracked.")
  private boolean trackIrrelevantVariables = true;

  private final Optional<VariableClassification> vc;
  private final Class<? extends ConfigurableProgramAnalysis> cpaClass;

  // When isTracking() only returns a single static value for all variables, it is found here
  private Optional<Boolean> staticTrackingResult = Optional.empty();

  ConfigurablePrecision(
      Configuration config,
      Optional<VariableClassification> pVc,
      Class<? extends ConfigurableProgramAnalysis> pCpaClass)
      throws InvalidConfigurationException {
    config.inject(this);
    this.cpaClass = pCpaClass;
    vc = pVc;
    preCalculateStaticTracking();
  }

  /**
   * Tries to determine whether the tracking result of all variables in the used
   * VariableClassification is equal (i.e. all return true or false) and caches that result to
   * reduce lookups later. Should only ever be used on static precisions. Needs to be run only once
   * for a static precision.
   */
  private void preCalculateStaticTracking() {
    if (vc.isEmpty()) {
      return;
    }

    VariableClassification varClass = vc.orElseThrow();

    // TODO: explore usage of this "not tracking" set, so that we may return false fast if the
    //  difference in size to the tracking sets is large enough
    ImmutableSet.Builder<String> isNotTrackingSetBuilder = ImmutableSet.builder();
    try {
      for (String variable : varClass.getAllVariables()) {
        if (!isTracking(MemoryLocation.fromQualifiedName(variable), null)) {
          isNotTrackingSetBuilder.add(variable);
        }
      }
    } catch (NullPointerException irrelevant) {
      // Type information is needed to determine tracking
      return;
    }

    Set<String> isNotTrackingSet = isNotTrackingSetBuilder.build();
    if (isNotTrackingSet.size() == varClass.getAllVariables().size()) {
      staticTrackingResult = Optional.of(false);
    } else if (isNotTrackingSet.isEmpty()) {
      staticTrackingResult = Optional.of(true);
    }
  }

  @Override
  public boolean allowsAbstraction() {
    return !trackIrrelevantVariables
        || !trackBooleanVariables
        || !trackIntEqualVariables
        || !trackIntAddVariables
        || !trackAddressedVariables
        || !trackVariablesBesidesEqAddBool
        || !variableBlacklist.toString().isEmpty();
  }

  @Override
  public boolean isTracking(MemoryLocation pVariable, Type pType, CFANode location) {
    if (staticTrackingResult.isPresent()) {
      return staticTrackingResult.orElseThrow();
    }

    return isTracking(pVariable, pType);
  }

  /**
   * Checks whether the {@link MemoryLocation} argument pVariable is being tracked in this
   * precision. The {@link Type} argument pType is only needed if not all types are tracked. Throws
   * {@link NullPointerException} if argument pType is needed, but null.
   */
  private boolean isTracking(MemoryLocation pVariable, Type pType) {
    if (trackFloatVariables) {
      return isTracking(pVariable);
    } else {
      checkNotNull(pType);
      return !((pType instanceof CSimpleType cSimpleType
                  && cSimpleType.getType().isFloatingPointType())
              || (pType instanceof JSimpleType jSimpleType && jSimpleType.isFloatingPointType()))
          && isTracking(pVariable);
    }
  }

  private boolean isTracking(MemoryLocation pVariable) {
    if (isOnWhitelist(pVariable.getIdentifier())) {
      return true;
    }

    if (isOnBlacklist(pVariable.getIdentifier())) {
      return false;
    }

    if (pVariable.isReference()) {
      MemoryLocation owner = pVariable.getReferenceStart();
      return isInTrackedVarClass(owner.getExtendedQualifiedName());
    } else {
      return isInTrackedVarClass(pVariable.getExtendedQualifiedName());
    }
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
    if (vc.isEmpty()) {
      return true;
    }

    VariableClassification varClass = vc.orElseThrow();

    final boolean varIsAddressed = varClass.getAddressedVariables().contains(variableName);

    // addressed variables do not belong to a specific type, so they have to
    // be handled extra. We want the precision to be as strict as possible,
    // therefore, when a variable is addressed but addressed variables should
    // not be tracked, we do not consider the other parts of the variable classification
    if (varIsAddressed && !trackAddressedVariables) {
      return false;

      // If we don't track irrelevant variables, check whether this is the case
    } else if (!trackIrrelevantVariables
        && !varClass.getRelevantVariables().contains(variableName)) {
      return false;

      // in this case addressed variables can at most be included in the
      // tracking variables and the rest of the variable classification is
      // the limiting factor
    } else {

      final boolean varIsBoolean = varClass.getIntBoolVars().contains(variableName);
      if (trackBooleanVariables && varIsBoolean) {
        return true;
      }

      final boolean varIsIntEqual = varClass.getIntEqualVars().contains(variableName);
      if (trackIntEqualVariables && varIsIntEqual) {
        return true;
      }

      final boolean varIsIntAdd = varClass.getIntAddVars().contains(variableName);
      if (trackIntAddVariables && varIsIntAdd) {
        return true;
      }

      // if the variable is not in a matching classification we have to check
      // if other variables should be tracked
      if (!(varIsBoolean || varIsIntAdd || varIsIntEqual)) {
        return trackVariablesBesidesEqAddBool;
      }

      return false;
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

  @CanIgnoreReturnValue
  @Override
  public VariableTrackingPrecision join(VariableTrackingPrecision consolidatedPrecision) {
    Preconditions.checkArgument(getClass().equals(consolidatedPrecision.getClass()));
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
    if (vc.isEmpty()) {
      return true;
    }

    VariableClassification varClass = vc.orElseThrow();
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
          && vc.orElseThrow().equals(precisionCompare.vc.orElseThrow())
          && cpaClass.equals(precisionCompare.cpaClass)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean equals(Object pObj) {
    return pObj instanceof ConfigurablePrecision other && tracksTheSameVariablesAs(other);
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
