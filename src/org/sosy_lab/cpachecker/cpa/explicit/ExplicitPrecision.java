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

import java.util.HashMap;
import java.util.regex.Pattern;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.Precision;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;

@Options(prefix="cpa.explicit.precision")
public class ExplicitPrecision implements Precision {

  /**
   * the pattern describing variable names that are not being tracked - if it is null, no variables are black-listed
   */
  private final Pattern blackListPattern;

  /**
   * the white-list which determines which variables are tracked at which location - if it is null, all variables are tracked
   */
  private SetMultimap<CFANode, String> whiteList = null;

  /**
   * the current location, given by the ExplicitTransferRelation, needed for checking the white-list
   */
  private CFANode currentLocation = null;

  private ReachedSetThresholds reachedSetThresholds = null;

  private PathThresholds pathThresholds = null;

  public ExplicitPrecision(String variableBlacklist, SetMultimap<CFANode, String> whiteList, Configuration config) throws InvalidConfigurationException {
    config.inject(this);

    blackListPattern = Pattern.compile(variableBlacklist);

    if (whiteList != null) {
      this.whiteList = HashMultimap.create(whiteList);
    }

    reachedSetThresholds  = new ReachedSetThresholds(config);
    pathThresholds        = new PathThresholds(config);
  }

  public ExplicitPrecision(ExplicitPrecision precision, Multimap<CFANode, String> predicateInfo,
      Multimap<CFANode, String> pathInfo) {

    blackListPattern = precision.blackListPattern;

    this.whiteList = HashMultimap.create(precision.whiteList);

    this.whiteList.putAll(predicateInfo);
    this.whiteList.putAll(pathInfo);

    reachedSetThresholds  = new ReachedSetThresholds(precision.reachedSetThresholds);
    pathThresholds        = new PathThresholds(precision.pathThresholds);
  }

  public ExplicitPrecision(ExplicitPrecision original) {

    blackListPattern = original.blackListPattern;

    this.whiteList = HashMultimap.create(original.whiteList);

    reachedSetThresholds  = new ReachedSetThresholds(original.reachedSetThresholds);
    pathThresholds        = new PathThresholds(original.pathThresholds);
  }

  public ReachedSetThresholds getReachedSetThresholds() {
    return reachedSetThresholds;
  }

  public PathThresholds getPathThresholds() {
    return pathThresholds;
  }

  @Override
  public String toString() {
    return whiteList != null ? whiteList.toString() : "whitelist disabled";
  }

  public void setLocation(CFANode node) {
    currentLocation = node;
  }

  boolean isOnBlacklist(String variable) {
    return this.blackListPattern.matcher(variable).matches();
  }

  /**
   * This method determines if the given variable is on the white-list, i.e. if it is being tracked.
   *
   * @param variable the scoped name of the variable to check
   * @return true, if the white-list is null or if variable is on the white-list, else false
   */
  boolean isOnWhitelist(String variable) {
    return whiteList == null
        || whiteList.containsEntry(currentLocation, variable);
  }

  /**
   * This method tells if the precision demands the given variable to be tracked.
   *
   * A variable is demanded to be tracked if it does not exceed a threshold (when given), it is on the white-list (when not null), and is not on the black-list.
   *
   * @param variable the scoped name of the variable to check
   * @return true, if the variable has to be tracked, else false
   */
  public boolean isTracking(String variable) {
    //System.out.println("thresholdPrecision.allowsTrackingOf(" + variable + ") = " + reachedSetThresholds.allowsTrackingOf(variable));
    return reachedSetThresholds.allowsTrackingOf(variable) && isOnWhitelist(variable) && !blackListPattern.matcher(variable).matches();
  }

  public boolean isNotTracking(String variable) {
    return !isTracking(variable);
  }

  public String getBlackListPattern() {
    return blackListPattern.pattern();
  }

  private class CegarPrecision {

  }

  private class BlacklistPrecision {

  }


  abstract class Thresholds {
    /**
     * the mapping of variable names to the threshold of the respective variable
     *
     * a value of null means, that the variable has reached its threshold and is no longer tracked
     */
    protected HashMap<String, Integer> thresholds = new HashMap<String, Integer>();

    /**
     * This method decides whether or not a variable is being tracked by this precision.
     *
     * @param variable the name of the variable for which to make the decision
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

    private ReachedSetThresholds(ReachedSetThresholds original) {
      defaultThreshold  = original.defaultThreshold;
      thresholds        = new HashMap<String, Integer>(original.thresholds);
    }

    /**
     * This method decides if the given variable with the given count exceeds the threshold.
     *
     * @param variable the name of the variable to check
     * @param count the value count to compare to the threshold
     * @return true, if the  variable with the given count exceeds the threshold, else false
     */
    boolean exceeds(String variable, Integer count) {
      if(defaultThreshold == -1) {
        return false;
      }

      else if((thresholds.containsKey(variable) && thresholds.get(variable) == null)
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

    private PathThresholds(PathThresholds original) {
      defaultThreshold  = original.defaultThreshold;
      thresholds        = new HashMap<String, Integer>(original.thresholds);
    }

    /**
     * This method decides if the given variable with the given count exceeds the threshold.
     *
     * @param variable the name of the variable to check
     * @param count the value count to compare to the threshold
     * @return true, if the  variable with the given count exceeds the threshold, else false
     */
    boolean exceeds(String variable, Integer count) {
      if(defaultThreshold == -1) {
        return false;
      }

      else if((thresholds.containsKey(variable) && thresholds.get(variable) == null)
          || (thresholds.containsKey(variable) && thresholds.get(variable) < count)
          || (!thresholds.containsKey(variable) && defaultThreshold < count)) {
        return true;
      }

      return false;
    }
  }
}
