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
package org.sosy_lab.cpachecker.cpa.explicit;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.util.VariableClassification;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

@Options(prefix="cpa.explicit.precision")
public class ExplicitPrecision implements Precision {

  /**
   * the pattern describing variable names that are not being tracked - if it is null, no variables are black-listed
   */
  private final Pattern blackListPattern;

  /**
   * the current location, given by the ExplicitTransferRelation, needed for checking the white-list
   */
  private CFANode currentLocation                   = null;

  /**
   * the component responsible for thresholds concerning the reached set
   */
  private ReachedSetThresholds reachedSetThresholds = null;

  /**
   * the component responsible for thresholds concerning paths
   */
  private PathThresholds pathThresholds             = null;

  /**
   * the component responsible for variables that need to be tracked, according to refinement
   */
  private CegarPrecision cegarPrecision             = null;

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

  private final Optional<VariableClassification> varClass;

  public ExplicitPrecision(String variableBlacklist, Configuration config,
      Optional<VariableClassification> vc,
      Multimap<CFANode, String> mapping) throws InvalidConfigurationException {
    config.inject(this);

    blackListPattern = Pattern.compile(variableBlacklist);
    this.varClass = vc;

    cegarPrecision        = new CegarPrecision(config, mapping);
    reachedSetThresholds  = new ReachedSetThresholds(config);
    pathThresholds        = new PathThresholds(config);
  }

  /**
   * copy constructor
   *
   * @param original the ExplicitPrecision to copy
   */
  public ExplicitPrecision(ExplicitPrecision original) {

    blackListPattern = original.blackListPattern;
    varClass = original.varClass;
    cegarPrecision        = new CegarPrecision(original.cegarPrecision);
    reachedSetThresholds  = new ReachedSetThresholds(original.reachedSetThresholds);
    pathThresholds        = new PathThresholds(original.pathThresholds);
  }

  public CegarPrecision getCegarPrecision() {
    return cegarPrecision;
  }

  public ReachedSetThresholds getReachedSetThresholds() {
    return reachedSetThresholds;
  }

  public PathThresholds getPathThresholds() {
    return pathThresholds;
  }

  public void setLocation(CFANode node) {
    currentLocation = node;
  }

  boolean isOnBlacklist(String variable) {
    return this.blackListPattern.matcher(variable).matches();
  }

  /**
   * This method tells if the precision demands the given variable to be tracked.
   *
   * A variable is demanded to be tracked if it does not exceed a threshold (when given),
   * it is on the white-list (when not null), and is not on the black-list.
   *
   * @param variable the scoped name of the variable to check
   * @return true, if the variable has to be tracked, else false
   */
  public boolean isTracking(String variable) {
    boolean result = reachedSetThresholds.allowsTrackingOf(variable)
            && pathThresholds.allowsTrackingOf(variable)
            && cegarPrecision.allowsTrackingOf(variable)
            && !isOnBlacklist(variable)
            && !isInIgnoredVarClass(variable);

    return result;
  }

  /** returns true, iff the variable is in an varClass, that should be ignored. */
  private boolean isInIgnoredVarClass(String variable) {
    if (varClass==null || !varClass.isPresent()) { return false; }

    Pair<String, String> var = splitVar(variable);

    final boolean isBoolean = varClass.get().getBooleanVars().containsEntry(var.getFirst(), var.getSecond());
    final boolean isIntEqual = varClass.get().getIntEqualVars().containsEntry(var.getFirst(), var.getSecond());
    final boolean isIntAdd = varClass.get().getIntAddVars().containsEntry(var.getFirst(), var.getSecond());

    final boolean isIgnoredBoolean = ignoreBoolean && isBoolean;

    // if a var is boolean and intEqual, it is not handled as intEqual.
    final boolean isIgnoredIntEqual = ignoreIntEqual && !isBoolean && isIntEqual;

    // if a var is (boolean or intEqual) and intAdd, it is not handled as intAdd.
    final boolean isIgnoredIntAdd = ignoreIntAdd && !isBoolean && !isIntEqual && isIntAdd;

    return isIgnoredBoolean || isIgnoredIntEqual || isIgnoredIntAdd;
  }

  /** split var into function and varName */
  private static Pair<String, String> splitVar(String variable) {
    int i = variable.indexOf("::");
    String function;
    String varName;
    if (i == -1) { // global variable, no splitting
      function = null;
      varName = variable;
    } else { // split function::varName
      function = variable.substring(0, i);
      varName = variable.substring(i + 2);
    }
    return Pair.of(function, varName);
  }

  @Options(prefix="cpa.explicit.precision.refinement")
  public class CegarPrecision {
    /**
     * the collection that determines which variables are tracked at a specific location - if it is null, all variables are tracked
     */
    private HashMultimap<CFANode, String> mapping = null;

    public static final String DELIMITER = ", ";

    @Option(description = "whether or not to add newly-found variables only to the exact program location or to the whole scope of the variable.")
    private boolean useScopedInterpolation = false;

    private CegarPrecision(Configuration config) throws InvalidConfigurationException {
      config.inject(this);

      if (Boolean.parseBoolean(config.getProperty("analysis.useRefinement"))) {
        mapping = HashMultimap.create();
      }
    }

    private CegarPrecision(Configuration config, Multimap<CFANode, String> mapping) throws InvalidConfigurationException {
      config.inject(this);

      if (Boolean.parseBoolean(config.getProperty("analysis.useRefinement"))) {
        this.mapping = HashMultimap.create(mapping);
      }
    }

    /**
     * copy constructor
     *
     * @param original the CegarPrecison to copy
     */
    private CegarPrecision(CegarPrecision original) {
      if (original.mapping != null) {
        mapping                = HashMultimap.create(original.mapping);
        useScopedInterpolation = original.useScopedInterpolation;
      }
    }

    /**
     * This method decides whether or not a variable is being tracked by this precision.
     *
     * @param variable the scoped name of the variable for which to make the decision
     * @return true, when the variable is allowed to be tracked, else false
     */
    boolean allowsTrackingOf(String variable) {
      return allowsTrackingAt(currentLocation, variable);
    }

    /**
     * This method determines if the given variable is being tracked at the given location.
     *
     * @param location the location to check at
     * @param variable the variable to check for
     * @return true, if the given variable is being tracked at the given location, else false
     */
    public boolean allowsTrackingAt(CFANode location, String variable) {
      if (mapping == null) {
        return true;
      }

      // when using scoped interpolation, it suffices to have the (scoped) variable identifier in the precision
      if (useScopedInterpolation) {
        return mapping.containsValue(variable);
      }

      // when not using scoped interpolation, there must a pair of location -> variable identifier in the mapping
      else {
        return mapping.containsEntry(location, variable);
      }
    }

    /**
     * This method adds the additional mapping to the current mapping, i.e., this precision can only grow in size, and never gets smaller.
     *
     * @param additionalMapping the additional mapping to be added to the current mapping
     */
    public void addToMapping(Multimap<CFANode, String> additionalMapping) {
      mapping.putAll(additionalMapping);
    }

    @Override
    public String toString() {
      return Joiner.on(DELIMITER).join(mapping.entries());
    }

    /**
     * This method joins this precision into a generic map, i.e. already joined precisions.
     *
     * @param consolidatedPrecision the generic map, i.e. the already joined precisions
     */
    public void consolidate(Map<CFANode, Collection<String>> consolidatedPrecision) {
      consolidatedPrecision.putAll(mapping.asMap());
    }
  }

  abstract class Thresholds {
    /**
     * the mapping of variable names to the threshold of the respective variable
     *
     * a value of null means, that the variable has reached its threshold and is no longer tracked
     */
    protected HashMap<String, Integer> thresholds = new HashMap<>();

    /**
     * This method decides whether or not a variable is being tracked by this precision.
     *
     * @param variable the scoped name of the variable for which to make the decision
     * @return true, when the variable is allowed to be tracked, else false
     */
    boolean allowsTrackingOf(String variable) {
      return !thresholds.containsKey(variable) || thresholds.get(variable) != null;
    }

    /**
     * This method declares the given variable to have exceeded its threshold.
     *
     * @param variable the name of the variable
     */
    void setExceeded(String variable) {
      thresholds.put(variable, null);
    }
  }

  @Options(prefix="cpa.explicit.precision.reachedSet")
  class ReachedSetThresholds extends Thresholds {

    /**
     * the default threshold
     */
    @Option(description="threshold for amount of different values that "
        + "are tracked for one variable within the reached set (-1 means infinitely)")
    protected Integer defaultThreshold = -1;

    private ReachedSetThresholds(Configuration config) throws InvalidConfigurationException {
      config.inject(this);
    }

    /**
     * copy constructor
     *
     * @param original the ReachedSetThresholds to copy
     */
    private ReachedSetThresholds(ReachedSetThresholds original) {
      defaultThreshold  = original.defaultThreshold;
      thresholds        = new HashMap<>(original.thresholds);
    }

    /**
     * This method decides if the given variable with the given count exceeds the threshold.
     *
     * @param variable the scoped name of the variable to check
     * @param count the value count to compare to the threshold
     * @return true, if the variable with the given count exceeds the threshold, else false
     */
    boolean exceeds(String variable, Integer count) {
      if (defaultThreshold == -1) {
        return false;
      }

      else if ((thresholds.containsKey(variable) && thresholds.get(variable) == null)
          || (thresholds.containsKey(variable) && thresholds.get(variable) < count)
          || (!thresholds.containsKey(variable) && defaultThreshold < count)) {
        return true;
      }

      return false;
    }
  }

  @Options(prefix="cpa.explicit.precision.path")
  class PathThresholds extends Thresholds {
    /**
     * the default threshold
     */
    @Option(description="threshold for amount of different values that "
        + "are tracked for one variable per path (-1 means infinitely)")
    protected Integer defaultThreshold = -1;

    private PathThresholds(Configuration config) throws InvalidConfigurationException {
      config.inject(this);
    }

    /**
     * copy constructor
     *
     * @param original the PathThresholds to copy
     */
    private PathThresholds(PathThresholds original) {
      defaultThreshold  = original.defaultThreshold;
      thresholds        = new HashMap<>(original.thresholds);
    }

    /**
     * This method decides if the given variable with the given count exceeds the threshold.
     *
     * @param variable the scoped name of the variable to check
     * @param count the value count to compare to the threshold
     * @return true, if the variable with the given count exceeds the threshold, else false
     */
    boolean exceeds(String variable, Integer count) {
      if (defaultThreshold == -1) {
        return false;
      }

      else if ((thresholds.containsKey(variable) && thresholds.get(variable) == null)
          || (thresholds.containsKey(variable) && thresholds.get(variable) < count)
          || (!thresholds.containsKey(variable) && defaultThreshold < count)) {
        return true;
      }

      return false;
    }
  }
}
