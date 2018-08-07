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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractWrapperState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonInternalState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonTransition;

/*
 * Representation of multiple properties for multi-property verification.
 */
public final class MultipleProperties {

  public enum PropertySeparator {
    FILE,
    AUTOMATON,
    TRANSITION
  }

  private final boolean findAllViolations;
  private final ImmutableList<AbstractSingleProperty> properties;

  /*
   * Create a list of properties based on specified separator:
   * FILE - each "*.spc" file represent a single property;
   * AUTOMATON - each automaton represent a single property;
   * TRANSITION - each transition to the target state represent a single property.
   */
  public MultipleProperties(
      ImmutableMap<Path, List<Automaton>> specification,
      PropertySeparator propertySeparator,
      boolean findAllViolations) {
    this.findAllViolations = findAllViolations;
    ImmutableList.Builder<AbstractSingleProperty> propertyBuilder = ImmutableList.builder();
    for (Entry<Path, List<Automaton>> entry : specification.entrySet()) {
      switch (propertySeparator) {
        case FILE:
          Path propertyFileName = entry.getKey().getFileName();
          String propertyName;
          if (propertyFileName != null) {
            propertyName = propertyFileName.toString();
          } else {
            propertyName = entry.getKey().toString();
          }
          propertyName = propertyName.replace(".spc", "");
          propertyBuilder.add(new AutomataSingleProperty(propertyName, entry.getValue()));
          break;
        case AUTOMATON:
          for (Automaton automaton : entry.getValue()) {
            propertyName = automaton.getName();
            propertyBuilder.add(
                new AutomataSingleProperty(propertyName, Lists.newArrayList(automaton)));
          }
          break;
        case TRANSITION:
          Map<String, Set<AutomatonTransition>> targetTransitions = Maps.newHashMap();
          for (Automaton automaton : entry.getValue()) {
            for (AutomatonInternalState state : automaton.getStates()) {
              for (AutomatonTransition transition : state.getTransitions()) {
                if (transition.getFollowState().isTarget()) {
                  propertyName = transition.getTransitionDescription();
                  if (targetTransitions.containsKey(propertyName)) {
                    targetTransitions.get(propertyName).add(transition);
                  } else {
                    targetTransitions.put(propertyName, Sets.newHashSet(transition));
                  }
                }
              }
            }
          }
          for (Entry<String, Set<AutomatonTransition>> entryTransitions :
              targetTransitions.entrySet()) {
            propertyBuilder.add(
                new TransitionSingleProperty(
                    entryTransitions.getKey(), entryTransitions.getValue()));
          }
          break;
        default:
          assert false;
      }
    }
    properties = propertyBuilder.build();
  }

  public MultipleProperties(
      ImmutableList<AbstractSingleProperty> properties, boolean findAllViolations) {
    this.properties = properties;
    this.findAllViolations = findAllViolations;
  }

  /*
   * Create a joint partition with currently irrelevant properties.
   */
  public MultipleProperties createIrrelevantProperties() {
    checkIfRelevant();
    ImmutableList.Builder<AbstractSingleProperty> propertyBuilder = ImmutableList.builder();
    for (AbstractSingleProperty property : properties) {
      if (!property.isRelevant()) {
        propertyBuilder.add(property);
      }
    }
    return new MultipleProperties(propertyBuilder.build(), findAllViolations);
  }

  /*
   * Stop analysis and make all currently checked properties UNKNOWN.
   */
  public void stopAnalysisOnFailure(String reason) {
    for (AbstractSingleProperty property : properties) {
      if (property.isNotChecked()) {
        property.updateResult(Result.UNKNOWN);
        property.setReasonOfUnknown(reason);
      }
    }
    checkIfRelevant();
  }

  /*
   * Stop analysis and make all currently checked properties TRUE.
   */
  public void stopAnalysisOnSuccess() {
    for (AbstractSingleProperty property : properties) {
      if (property.isNotDetermined()) {
        property.updateResult(Result.TRUE);
      }
      if (property.getResult().equals(Result.FALSE)) {
        property.allViolationsFound();
      }
    }
    checkIfRelevant();
  }

  /*
   * Determine, which property was violated based on target state, and stop checking it if
   * necessary.
   */
  public void processPropertyViolation(AbstractState targetState) {
    if (targetState instanceof AbstractWrapperState) {
      for (AbstractState state : ((AbstractWrapperState) targetState).getWrappedStates()) {
        processPropertyViolation(state);
      }
    } else if (targetState instanceof AutomatonState) {
      AutomatonState automatonState = (AutomatonState) targetState;
      if (automatonState.isTarget()) {
        for (AbstractSingleProperty property : properties) {
          if (property.isTarget(automatonState)) {
            if (!findAllViolations) {
              property.disableProperty();
              property.allViolationsFound();
            }
            property.checkIfRelevant();
            property.updateResult(Result.FALSE);
            property.addViolatedPropertyDescription(automatonState.getViolatedProperties());
            return; // shortcut
          }
        }
      }
    }
  }

  /*
   * Check all properties, if they are relevant.
   */
  private void checkIfRelevant() {
    for (AbstractSingleProperty property : properties) {
      if (!property.isRelevant()) {
        property.checkIfRelevant();
      }
    }
  }

  /*
   * Check only specified properties and ignore everything else.
   */
  public void setTargetProperties(MultipleProperties targetProperties) {
    for (AbstractSingleProperty property : properties) {
      if (!targetProperties.properties.contains(property)) {
        property.disableProperty();
      } else if (property.isNotChecked()) {
        property.enableProperty();
      }
    }
  }

  /*
   * Return true, if all properties are checked.
   */
  public boolean isChecked() {
    for (AbstractSingleProperty property : properties) {
      if (property.isNotDetermined()) {
        return false;
      }
    }
    return true;
  }

  /*
   * Divide resources, which were spent on verification of multiple properties, between each checked
   * property.
   */
  public void divideSpentResources(
      TimeSpan spentCPUTime, @SuppressWarnings("unused") ReachedSet reached) {
    // TODO: add operator for dividing CPU time, which will take reached as input.
    for (AbstractSingleProperty property : properties) {
      property.addCpuTime(spentCPUTime.divide(getNumberOfProperties()));
    }
  }

  /*
   * Adjust final result.
   */
  public Result getOverallResult() {
    boolean isUnknown = false;
    for (AbstractSingleProperty property : properties) {
      if (property.getResult().equals(Result.FALSE)) {
        return Result.FALSE;
      } else if (property.getResult().equals(Result.UNKNOWN)) {
        isUnknown = true;
      } else if (property.getResult().equals(Result.NOT_YET_STARTED)) {
        return Result.NOT_YET_STARTED;
      }
    }
    if (isUnknown) {
      return Result.UNKNOWN;
    } else {
      return Result.TRUE;
    }
  }

  public int getNumberOfProperties() {
    return properties.size();
  }

  public boolean isFindAllViolations() {
    return findAllViolations;
  }

  public ImmutableList<AbstractSingleProperty> getProperties() {
    return properties;
  }

  @Override
  public String toString() {
    return properties.toString();
  }
}
