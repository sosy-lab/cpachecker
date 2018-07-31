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
package org.sosy_lab.cpachecker.core.algorithm.mpv.property;

import com.google.common.collect.Sets;
import java.util.Set;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;

/*
 * Abstract representation of a single property inside multi-property verification.
 */
public abstract class AbstractSingleProperty {

  private final String name;

  private TimeSpan cpuTime; // CPU time, which was spent on checking this property
  protected boolean relevant; // whether this property was used during the analysis or not
  private int violations; // number of found violations
  private boolean allViolationsFound;
  private Result result;
  private final Set<String> description;

  protected AbstractSingleProperty(String pName) {
    name = pName;
    cpuTime = TimeSpan.ofSeconds(0);
    relevant = false;
    violations = 0;
    allViolationsFound = false;
    result = Result.NOT_YET_STARTED;
    description = Sets.newHashSet();
  }

  /*
   * Ignore this property during the analysis.
   */
  public abstract void disableProperty();

  /*
   * Check this property during the analysis.
   */
  public abstract void enableProperty();

  /*
   * Check if the property is violated in the automaton state.
   */
  public abstract boolean isTarget(AutomatonState state);

  /*
   * Check if this property was used during the analysis.
   */
  public abstract void checkIfRelevant();

  /*
   * Return true, if this property got final verdict (all violations have been found in case FALSE
   * result).
   */
  public boolean isChecked() {
    return !result.equals(Result.NOT_YET_STARTED);
  }

  /*
   * Set final result for the property. Note, that FALSE result cannot be change to UNKNOWN if not
   * all property violations were found.
   */
  public void updateResult(Result newResult) {
    if (!(newResult.equals(Result.UNKNOWN) && result.equals(Result.FALSE))) {
      result = newResult;
    }
    if (newResult.equals(Result.FALSE)) {
      violations++;
    }
  }

  public boolean isRelevant() {
    return relevant;
  }

  public Result getResult() {
    return result;
  }

  public void allViolationsFound() {
    allViolationsFound = true;
  }

  public boolean isAllViolationsFound() {
    return allViolationsFound;
  }

  public String getName() {
    return name;
  }

  public void addDescription(String pDescription) {
    description.add(pDescription);
  }

  public String getDescription() {
    return description.toString();
  }

  public TimeSpan getCpuTime() {
    return cpuTime;
  }

  public void addCpuTime(TimeSpan pCpuTime) {
    cpuTime = TimeSpan.sum(cpuTime, pCpuTime);
  }

  public int getViolations() {
    return violations;
  }

  @Override
  public String toString() {
    String res = "Property '" + name + "': " + result;
    if (!relevant) {
      res += ", irrelevant";
    } else {
      res += ", relevant";
    }
    if (!allViolationsFound && result.equals(Result.FALSE)) {
      res += ", incomplete";
    }
    if (description.size() > 0) {
      res += " " + getDescription();
    }
    return res;
  }
}
