// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpv.property;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.nio.file.Path;
import java.util.Set;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractWrapperState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;

/** Representation of multiple properties for multi-property verification algorithm. */
public final class MultipleProperties {

  public enum PropertySeparator {
    FILE,
    AUTOMATON
  }

  private final boolean findAllViolations;
  private final ImmutableList<AbstractSingleProperty> properties;

  /**
   * Create a list of properties based on specified separator: FILE - each "*.spc" file represent a
   * single property; AUTOMATON - each automaton represent a single property.
   */
  public MultipleProperties(
      ImmutableListMultimap<Path, Automaton> specification,
      PropertySeparator propertySeparator,
      boolean findAllViolations) {
    this.findAllViolations = findAllViolations;
    ImmutableList.Builder<AbstractSingleProperty> propertyBuilder = ImmutableList.builder();
    for (Path path : specification.keySet()) {
      switch (propertySeparator) {
        case FILE:
          Path propertyFileName = path.getFileName();
          String propertyName;
          if (propertyFileName != null) {
            propertyName = propertyFileName.toString();
          } else {
            propertyName = path.toString();
          }
          propertyName = propertyName.replace(".spc", "");
          propertyBuilder.add(new AutomataSingleProperty(propertyName, specification.get(path)));
          break;
        case AUTOMATON:
          for (Automaton automaton : specification.get(path)) {
            propertyName = automaton.getName();
            propertyBuilder.add(
                new AutomataSingleProperty(propertyName, Lists.newArrayList(automaton)));
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

  /** Create a joint partition with currently irrelevant properties. */
  public MultipleProperties createIrrelevantProperties() {
    ImmutableList.Builder<AbstractSingleProperty> propertyBuilder = ImmutableList.builder();
    for (AbstractSingleProperty property : properties) {
      if (!property.isRelevant()) {
        propertyBuilder.add(property);
      }
    }
    return new MultipleProperties(propertyBuilder.build(), findAllViolations);
  }

  /** Stop analysis and make all currently checked properties UNKNOWN. */
  public void stopAnalysisOnFailure(String reason) {
    for (AbstractSingleProperty property : properties) {
      if (property.isNotChecked()) {
        property.updateResult(Result.UNKNOWN);
        property.setReasonOfUnknown(reason);
      }
    }
  }

  /** Stop analysis and make all currently checked properties TRUE. */
  public void stopAnalysisOnSuccess() {
    for (AbstractSingleProperty property : properties) {
      if (property.isNotDetermined()) {
        property.updateResult(Result.TRUE);
      }
      if (property.getResult().equals(Result.FALSE)) {
        property.allViolationsFound();
      }
    }
  }

  /** Process reached set with violated properties. */
  public void processPropertyViolation(ReachedSet reached) {
    Set<AbstractSingleProperty> violatedProperties =
        determineViolatedProperties(reached.getLastState());
    for (AbstractSingleProperty property : violatedProperties) {
      if (!findAllViolations) {
        disablePropertyDuringAnalysis(reached, property);
        property.allViolationsFound();
      }
    }
  }

  /*
   * Update automaton precision to disable corresponding property.
   */
  private void disablePropertyDuringAnalysis(ReachedSet reached, AbstractSingleProperty property) {
    for (AbstractState state : reached.getWaitlist()) {
      Precision precision = reached.getPrecision(state);
      property.disable(precision);
    }
  }

  /** Determine, which properties were violated in the given target state. */
  private Set<AbstractSingleProperty> determineViolatedProperties(AbstractState targetState) {
    ImmutableSet.Builder<AbstractSingleProperty> builder = ImmutableSet.builder();
    if (targetState instanceof AbstractWrapperState) {
      for (AbstractState state : ((AbstractWrapperState) targetState).getWrappedStates()) {
        builder.addAll(determineViolatedProperties(state));
      }
    } else if (targetState instanceof AutomatonState) {
      AutomatonState automatonState = (AutomatonState) targetState;
      if (automatonState.isTarget()) {
        for (AbstractSingleProperty property : properties) {
          if (property.isTarget(automatonState)) {
            property.updateResult(Result.FALSE);
            property.setRelevant();
            property.addViolatedPropertyDescription(automatonState.getViolatedProperties());
            builder.add(property);
          }
        }
      }
    }
    return builder.build();
  }

  /** Based on the given reached set determine relevant properties. */
  public void determineRelevance(CFA cfa) {
    for (AbstractSingleProperty property : properties) {
      property.determineRelevancy(cfa);
    }
  }

  /**
   * Check only specified properties and ignore everything else. Here we assume, that reached set
   * contains only one initial state, so it is sufficient to change precision there.
   */
  public void setTargetProperties(MultipleProperties targetProperties, ReachedSet reached) {
    Precision precision = reached.getPrecision(reached.getFirstState());
    for (AbstractSingleProperty property : properties) {
      if (!targetProperties.properties.contains(property)) {
        property.disable(precision);
      } else if (property.isNotChecked()) {
        property.enable(precision);
      }
    }
  }

  /** Return true, if all properties are checked. */
  public boolean isChecked() {
    for (AbstractSingleProperty property : properties) {
      if (property.isNotDetermined()) {
        return false;
      }
    }
    return true;
  }

  /**
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

  /** Adjust overall verification result based on result for each property. */
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
