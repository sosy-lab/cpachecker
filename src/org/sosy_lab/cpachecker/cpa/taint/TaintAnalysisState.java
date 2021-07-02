// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.taint;

import com.google.common.base.Joiner;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.defaults.NamedProperty;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public final class TaintAnalysisState implements AbstractState, Graphable, AbstractQueryableState {

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (isTarget ? 1231 : 1237);
    result = prime * result + ((pointerMap == null) ? 0 : pointerMap.hashCode());
    result = prime * result + ((taintedMap == null) ? 0 : taintedMap.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    TaintAnalysisState other = (TaintAnalysisState) obj;
    if (isTarget != other.isTarget) {
      return false;
    }
    if (pointerMap == null) {
      if (other.pointerMap != null) {
        return false;
      }
    } else if (!pointerMap.equals(other.pointerMap)) {
      return false;
    }
    if (taintedMap == null) {
      if (other.taintedMap != null) {
        return false;
      }
    } else if (!taintedMap.equals(other.taintedMap)) {
      return false;
    }
    return true;
  }

  private final boolean isTarget;

  private Map<MemoryLocation, Boolean> taintedMap = new HashMap<>();
  private Map<MemoryLocation, MemoryLocation> pointerMap = new HashMap<>();

  private final Set<Property> violations;

  private final LogManager logger;

  public TaintAnalysisState(LogManager plogger) {
    logger = plogger;
    taintedMap = new HashMap<>();
    pointerMap = new HashMap<>();
    isTarget = false;
    violations = Collections.emptySet();
  }

  private TaintAnalysisState(TaintAnalysisState state, LogManager plogger) {
    logger = plogger;
    taintedMap = new HashMap<>(state.taintedMap);
    pointerMap = new HashMap<>(state.pointerMap);
    isTarget = false;
    violations = Collections.emptySet();
  }

  private TaintAnalysisState(
      TaintAnalysisState state, LogManager plogger, Boolean target, String violation) {
    logger = plogger;
    taintedMap = new HashMap<>(state.taintedMap);
    pointerMap = new HashMap<>(state.pointerMap);
    isTarget = target;
    violations = NamedProperty.singleton(violation);
  }

  public static TaintAnalysisState copyOf(TaintAnalysisState state) {
    return new TaintAnalysisState(state, state.logger);
  }

  public static TaintAnalysisState copyOf(
      TaintAnalysisState state, Boolean target, String violation) {
    return new TaintAnalysisState(state, state.logger, target, violation);
  }

  public void assignTaint(MemoryLocation var, Boolean tainted) {
    if(tainted == null) {
      tainted = false;
    }
    addToMap(var, tainted);
  }

  private void addToMap(final MemoryLocation value, final Boolean tainted) {
    taintedMap.put(value, tainted);
  }

  public void change(MemoryLocation var, Boolean tainted) {
    if(tainted == null) {
      tainted = false;
    }
    changeMap(var, tainted);
  }

  private void changeMap(final MemoryLocation value, final Boolean tainted) {
    taintedMap.replace(value, tainted);
    logger.log(Level.FINEST, "Changed: " + value + " => " + tainted);
  }

  public Boolean getStatus(MemoryLocation var) {
    return getFromMap(var);
  }

  private Boolean getFromMap(final MemoryLocation value) {
    if(getPointerTo(value) != null) {
      return taintedMap.get(getPointerTo(value));
    }
    return taintedMap.get(value);
  }

  public void remove(MemoryLocation var) {
    removeFromMap(var);
  }

  private void removeFromMap(final MemoryLocation value) {
    taintedMap.remove(value);
  }

  public Map<MemoryLocation, Boolean> getTaintedMap() {
    return Collections.unmodifiableMap(this.taintedMap);
  }

  public TaintAnalysisState addTaintToInformation(MemoryLocation value, Boolean tainted) {
    if (getFromMap(value) == null) {
      TaintAnalysisState result = copyOf(this);
      result.addToMap(value, tainted);
      return result;
    } else if (getFromMap(value).equals(tainted)) {
      return this;
    } else if (!getFromMap(value).equals(tainted) && tainted) {
      TaintAnalysisState result = copyOf(this);
      result.changeMap(value, tainted);
      return result;
    } else {
      return this;
    }
  }


  // Pointer Logic
  public void addPointerTo(MemoryLocation from, MemoryLocation to) {
    addToPointerMap(from, to);
  }
  private void addToPointerMap(final MemoryLocation from, final MemoryLocation to) {
    pointerMap.put(from, to);
  }
  public void removePointerTo(MemoryLocation from) {
    removeFromPointerMap(from);
  }
  private void removeFromPointerMap(final MemoryLocation from) {
    pointerMap.remove(from);
  }
  public MemoryLocation getPointerTo(MemoryLocation from) {
    return getPointerMapTo(from);
  }
  private MemoryLocation getPointerMapTo(final MemoryLocation from) {
    return pointerMap.get(from);
  }
  // public Map<MemoryLocation, LocationSet> getPointsToMap() {
  //   return Collections.unmodifiableMap(this.pointerMap);
  // }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Tainted: [");
    for (Entry<MemoryLocation, Boolean> entry : taintedMap.entrySet()) {
      String key = entry.getKey().toString();
      sb.append(" <");
      sb.append(key);
      sb.append(" = ");
      sb.append(entry.getValue());
      sb.append(">\n");
    }
    sb.append("] size->  ").append(taintedMap.size());
    if (isTarget) {
      sb.append("Violations:\n").append(Joiner.on("\n").join(violations));
    }
    return sb.toString();
  }

  /**
   * This method returns a more compact string representation of the state, compared to toString().
   *
   * @return a more compact string representation of the state
   */
  @Override
  public String toDOTLabel() {
    StringBuilder sb = new StringBuilder();
    if(taintedMap != null) {
      sb.append("[");
      Joiner.on(", ").withKeyValueSeparator("=").appendTo(sb, taintedMap);
      sb.append("]");
    }

    return sb.toString();
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }


  @Override
  public String getCPAName() {
    return "TaintAnalysisCPA";
  }

  @Override
  public boolean checkProperty(String property) throws InvalidQueryException {
    if (!"taint-error".equals(property)) {
      throw new InvalidQueryException(
          String.format("%s cannot check the property %s!", getCPAName(), property));
    }
    return isTarget;
  }
}
