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
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;

/*
 * Abstract representation of a single property inside multi-property verification.
 */
public abstract class AbstractSingleProperty {

  private final String name;

  private TimeSpan cpuTime; // CPU time, which was spent on checking this property
  private boolean relevant; // whether this property was used during the analysis or not
  private int violations; // number of found violations
  private boolean allViolationsFound;
  private Result result;
  private final Set<Property> violatedPropertyDescription;
  private String reasonOfUnknown;

  protected AbstractSingleProperty(String pName) {
    name = pName;
    cpuTime = TimeSpan.ofSeconds(0);
    relevant = false;
    violations = 0;
    allViolationsFound = false;
    result = Result.NOT_YET_STARTED;
    violatedPropertyDescription = Sets.newHashSet();
    reasonOfUnknown = "";
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
   * Return true, if this property did not get final result (TRUE, FALSE or UNKNOWN).
   */
  public boolean isNotDetermined() {
    return result.equals(Result.NOT_YET_STARTED);
  }

  /*
   * Return true, if this property is still checking (it did not get final result or some property
   * violations were not found).
   */
  public boolean isNotChecked() {
    return isNotDetermined() || (result.equals(Result.FALSE) && !allViolationsFound);
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

  protected void setRelevant() {
    relevant = true;
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

  public void addViolatedPropertyDescription(Set<Property> pDescription) {
    violatedPropertyDescription.addAll(pDescription);
  }

  public String getReasonOfUnknown() {
    return reasonOfUnknown;
  }

  public void setReasonOfUnknown(String pReasonOfUnknown) {
    assert !result.equals(Result.TRUE);
    reasonOfUnknown = pReasonOfUnknown;
  }

  public Set<Property> getViolatedPropertyDescription() {
    return violatedPropertyDescription;
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

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return name;
  }
}
