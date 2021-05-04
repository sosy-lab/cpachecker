// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.taint;

import com.google.common.base.Joiner;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.defaults.NamedProperty;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public final class TaintAnalysisState implements AbstractState, Graphable, Targetable {

  private final boolean isTarget;

  private HashMap<MemoryLocation, Boolean> map = new HashMap<>();
  private HashMap<MemoryLocation, MemoryLocation> pointerMap = new HashMap<>();

  private final Set<Property> violations;

  private final LogManager logger;

  public TaintAnalysisState(LogManager plogger) {
    logger = plogger;
    map = new HashMap<>();
    pointerMap = new HashMap<>();
    isTarget = false;
    violations = Collections.emptySet();
  }

  private TaintAnalysisState(TaintAnalysisState state, LogManager plogger) {
    logger = plogger;
    map = new HashMap<>(state.map);
    pointerMap = new HashMap<>(state.pointerMap);
    // constantsMap = state.constantsMap;
    isTarget = false;
    violations = Collections.emptySet();
  }

  private TaintAnalysisState(
      TaintAnalysisState state, LogManager plogger, Boolean target, String violation) {
    logger = plogger;
    map = new HashMap<>(state.map);
    pointerMap = new HashMap<>(state.pointerMap);
    isTarget = target;
    violations = NamedProperty.singleton(violation);
    // logger.log(Level.INFO, "isTarget: " + isTarget + " | violations: " + violations);
  }

  public static TaintAnalysisState copyOf(TaintAnalysisState state) {
    return new TaintAnalysisState(state, state.logger);
  }

  public static TaintAnalysisState copyOf(
      TaintAnalysisState state, Boolean target, String violation) {
    return new TaintAnalysisState(state, state.logger, target, violation);
  }

  public void assignTaint(MemoryLocation var, Boolean tainted) {
    addToMap(var, tainted);
  }

  private void addToMap(final MemoryLocation value, final Boolean tainted) {
    map.put(value, tainted);
    // MemoryLocation mem = MemoryLocation.valueOf(value);
    // memoryMap.put(mem, tainted);
    // logger.log(Level.INFO, "Hinzugefügt: "+value+": "+tainted+ " || "+mem);
  }

  public void change(MemoryLocation var, Boolean tainted) {
    changeMap(var, tainted);
  }

  private void changeMap(final MemoryLocation value, final Boolean tainted) {
    map.replace(value, tainted);
    logger.log(Level.FINEST, "Changed: " + value + " => " + tainted);
  }

  public Boolean getStatus(MemoryLocation var) {
    return getMap(var);
  }

  private Boolean getMap(final MemoryLocation value) {
    if(getPointerTo(value) != null)
      return map.get(getPointerTo(value));
    return map.get(value);
    // logger.log(Level.INFO, "Hinzugefügt: "+value+": "+tainted);
    // hashCode += (pMemLoc.hashCode() ^ valueAndType.hashCode());
  }

  public void remove(MemoryLocation var) {
    removeFromMap(var);
  }

  private void removeFromMap(final MemoryLocation value) {
    map.remove(value);
    // logger.log(Level.INFO, "Hinzugefügt: "+value+": "+tainted);
    // hashCode += (pMemLoc.hashCode() ^ valueAndType.hashCode());
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
    return getPointerMap(from);
  }
  private MemoryLocation getPointerMap(final MemoryLocation from) {
    return pointerMap.get(from);
    // logger.log(Level.INFO, "Hinzugefügt: "+value+": "+tainted);
    // hashCode += (pMemLoc.hashCode() ^ valueAndType.hashCode());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Tainted: [");
    for (Entry<MemoryLocation, Boolean> entry : map.entrySet()) {
      String key = entry.getKey().toString();
      sb.append(" <");
      sb.append(key);
      sb.append(" = ");
      sb.append(entry.getValue());
      sb.append(">\n");
    }

    return sb.append("] size->  ").append(map.size()).toString();
  }

  /**
   * This method returns a more compact string representation of the state, compared to toString().
   *
   * @return a more compact string representation of the state
   */
  @Override
  public String toDOTLabel() {
    StringBuilder sb = new StringBuilder();

    sb.append("[");
    Joiner.on(", ").withKeyValueSeparator("=").appendTo(sb, map);
    sb.append("]");

    return sb.toString();
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
    return violations;
  }
}
