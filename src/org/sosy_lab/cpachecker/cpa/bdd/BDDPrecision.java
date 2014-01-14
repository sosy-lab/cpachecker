/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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

import javax.annotation.Nullable;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitState.MemoryLocation;
import org.sosy_lab.cpachecker.util.VariableClassification;

import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

@Options(prefix = "cpa.bdd")
public class BDDPrecision implements Precision {

  @Option(description = "track boolean variables from cfa")
  private boolean trackBoolean = true;

  @Option(description = "track variables from cfa, that are only compared " +
      "for equality, they are tracked as (small) bitvectors")
  private boolean trackIntEqual = true;

  @Option(description = "track variables, only used in simple calculations " +
      "(add, sub, gt, lt, eq,...) from cfa as bitvectors with (default) 32 bits")
  private boolean trackIntAdd = true;

  @Option(name = "forceTrackingPattern",
      description = "Pattern for variablenames that will always be tracked with BDDs." +
          "This pattern should only be used for known variables, i.e. for boolean vars.")
  private String forceTrackingPatternStr = "";

  private final Pattern forceTrackingPattern;
  private final CegarPrecision cegarPrecision;

  private final Optional<VariableClassification> varClass;

  /** initial constructor */
  public BDDPrecision(Configuration config, Optional<VariableClassification> vc)
      throws InvalidConfigurationException {
    config.inject(this);
    if (!forceTrackingPatternStr.isEmpty()) {
      this.forceTrackingPattern = Pattern.compile(forceTrackingPatternStr);
    } else {
      this.forceTrackingPattern = null;
    }
    this.cegarPrecision = new CegarPrecision(config);
    this.varClass = vc;
  }

  /** copy-constructor, that allows to add new variables to cegar-precision. */
  public BDDPrecision(BDDPrecision original, Multimap<CFANode, MemoryLocation> increment) {
    this.varClass = original.varClass;
    this.forceTrackingPattern = original.forceTrackingPattern;
    this.trackBoolean = original.trackBoolean;
    this.trackIntEqual = original.trackIntEqual;
    this.trackIntAdd = original.trackIntAdd;
    this.cegarPrecision = original.cegarPrecision.withAdditionalMappings(increment);
  }

  public boolean isDisabled() {
    if (forceTrackingPattern != null) { return false; }

	if (cegarPrecision.isEmpty()) { return true; }

    if (!varClass.isPresent()) { return true; }

    boolean trackSomeIntBools = trackBoolean &&
        !varClass.get().getIntBoolVars().isEmpty();
    boolean trackSomeIntEquals = trackIntEqual &&
        !varClass.get().getIntEqualVars().isEmpty();
    boolean trackSomeIntAdds = trackIntAdd &&
        !varClass.get().getIntAddVars().isEmpty();

    return !(trackSomeIntBools || trackSomeIntEquals || trackSomeIntAdds);
  }

  /**
   * This method tells if the precision demands the given variable to be tracked.
   *
   * @param variable the name of the variable to check
   * @param variable function of current scope or null, if variable is global
   * @return true, if the variable has to be tracked, else false
   */
  public boolean isTracking(@Nullable String function, String var) {

    // this pattern should only be used, if we know the class of the matching variables
    if (this.forceTrackingPattern != null &&
        this.forceTrackingPattern.matcher(var).matches()) {
      return true;
    }

    if (!cegarPrecision.allowsTrackingAt(function, var)) {
      return false;
    }

    return isInTrackedClass(function, var);
  }

  private boolean isInTrackedClass(@Nullable String function, String var) {
    if (!varClass.isPresent()) { return false; }

    final boolean isIntBool = varClass.get().getIntBoolVars().containsEntry(function, var);
    final boolean isIntEq = varClass.get().getIntEqualVars().containsEntry(function, var);
    final boolean isIntAdd = varClass.get().getIntAddVars().containsEntry(function, var);

    final boolean isTrackedBoolean = trackBoolean && isIntBool;
    final boolean isTrackedIntEqual = trackIntEqual && isIntEq;
    final boolean isTrackedIntAdd = trackIntAdd && isIntAdd;

    return isTrackedBoolean || isTrackedIntEqual || isTrackedIntAdd;
  }

  public CegarPrecision getCegarPrecision() {
    return cegarPrecision;
  }


  @Options(prefix = "cpa.bdd.precision.refinement")
  public class CegarPrecision {

    /** the collection that determines which variables are tracked at
     *  a specific location - if it is null, all variables are tracked */
    private final Multimap<CFANode, MemoryLocation> mapping;

    @Option(description = "whether or not to add newly-found variables " +
        "only to the exact program location or to the whole scope of the variable.")
    private boolean useScopedInterpolation = false;

    private CegarPrecision(Configuration config) throws InvalidConfigurationException {
      config.inject(this);

      if (Boolean.parseBoolean(config.getProperty("analysis.algorithm.CEGAR"))) {
        mapping = HashMultimap.create();
      } else {
        mapping = null;
      }
    }

    /** copy constructor */
    private CegarPrecision(Multimap<CFANode, MemoryLocation> pMapping,
        boolean pUseScopedInterpolation) {
      mapping = HashMultimap.create(pMapping);
      useScopedInterpolation = pUseScopedInterpolation;
    }

    /** returns, if nothing should be tracked. */
    public boolean isEmpty() {
      return mapping != null && mapping.isEmpty();
    }

    /**
     * This method determines if the given variable is being
     * tracked at the given location.
     *
     * @param location the location to check at
     * @param function the function of the variable or null, if global scope
     * @param var the variable to check for
     * @return if the given variable is being tracked at the given location
     */
    public boolean allowsTrackingAt(@Nullable String function, String var) {
      if (mapping == null) { return true; }

      final MemoryLocation variable = function == null ? MemoryLocation.valueOf(var, 0)
                                                       : MemoryLocation.valueOf(function, var, 0);

      // when using scoped interpolation, it suffices to have the (scoped) variable identifier in the precision
      if (useScopedInterpolation) {
        return mapping.containsValue(variable);
      }

      // when not using scoped interpolation, there must a pair of location -> variable identifier in the mapping
      else {
        return mapping.containsValue(variable);
        // TODO support for location-based tracking
        // return mapping.containsEntry(location, variable);
      }
    }

    /**
     * This method adds the additional mapping to the current mapping,
     * i.e., this precision can only grow in size, and never gets smaller.
     *
     * @param additionalMapping to be added to the current mapping
     */
    private CegarPrecision withAdditionalMappings(Multimap<CFANode, MemoryLocation> additionalMapping) {
      if (mapping == null) {
        // all variables are tracked anyway
        return this;
      }
      CegarPrecision result = new CegarPrecision(mapping, useScopedInterpolation);
      result.mapping.putAll(additionalMapping);
      return result;
    }
  }
}
