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
package org.sosy_lab.cpachecker.cpa.multigoal;

import java.util.Collections;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;

public class MultiGoalState implements AbstractState, Targetable, Graphable {

  private boolean isTarget;
  // TODO handle regions
  private Region region;
  private CFAEdgesGoal coveredGoal;

  public static MultiGoalState NonTargetState() {
    return new MultiGoalState(false, null);
  }

  public static MultiGoalState TargetState(CFAEdgesGoal pCoveredGoal) {
    return new MultiGoalState(true, pCoveredGoal);
  }


  private MultiGoalState(boolean pIsTarget, CFAEdgesGoal pCoveredGoal) {
    coveredGoal = pCoveredGoal;
    isTarget = pIsTarget;
  }

  @Override
  public String toString() {
      if (isTarget) {
        return "TARGET";
      }
      return "NO_TARGET";
  }

  @Override
  public String toDOTLabel() {
    return toString();
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  @Override
  public boolean isTarget() {
    return isTarget;
  }

  @Override
  public @NonNull Set<Property> getViolatedProperties() throws IllegalStateException {
    return Collections.emptySet();
  }

  public CFAEdgesGoal getCoveredGoal() {
    return coveredGoal;
  }

  public void setCoveredGoal(CFAEdgesGoal pGoal) {
    coveredGoal = pGoal;
  }

}
