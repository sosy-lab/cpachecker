// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.taint;

import com.google.common.base.MoreObjects;
import java.util.Objects;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

class TaintState implements AbstractState, AbstractQueryableState {

  public static final TaintState INITIAL_STATE = new TaintState(false);

  private final boolean taintError;

  private final PersistentSortedMap<CFAEdge, Void> taintedEdges;
  private final PersistentSortedMap<MemoryLocation, Void> taintedMemoryLocations;

  private TaintState(boolean pTaintError) {
    taintError = pTaintError;
    taintedEdges = PathCopyingPersistentTreeMap.<CFAEdge, Void>of();
    taintedMemoryLocations = PathCopyingPersistentTreeMap.<MemoryLocation, Void>of();
  }

  private TaintState(
      boolean pTaintError,
      PersistentSortedMap<CFAEdge, Void> pTaintedEdges,
      PersistentSortedMap<MemoryLocation, Void> pTaintedMemoryLocations) {
    taintError = pTaintError;
    taintedEdges = pTaintedEdges;
    taintedMemoryLocations = pTaintedMemoryLocations;
  }

  private static <K> PersistentSortedMap<K, Void> union(
      PersistentSortedMap<K, Void> pSome, PersistentSortedMap<K, Void> pOther) {
    PersistentSortedMap<K, Void> current = pSome.size() >= pOther.size() ? pSome : pOther;
    PersistentSortedMap<K, Void> toInsert = pSome.size() >= pOther.size() ? pOther : pSome;
    for (K key : toInsert.keySet()) {
      current = current.putAndCopy(key, null);
    }
    return current;
  }

  @Override
  public String getCPAName() {
    return TaintAnalysisCPA.class.getSimpleName();
  }

  @Override
  public boolean checkProperty(String pProperty) throws InvalidQueryException {
    if (pProperty.equals("taint-error")) {
      return taintError;
    } else {
      return AbstractQueryableState.super.checkProperty(pProperty);
    }
  }

  public boolean isTainted(CFAEdge pEdge) {
    return taintedEdges.containsKey(pEdge);
  }

  public boolean isTainted(MemoryLocation pMemoryLocation) {
    return taintedMemoryLocations.containsKey(pMemoryLocation);
  }

  public TaintState taintError() {
    return new TaintState(true, taintedEdges, taintedMemoryLocations);
  }

  public boolean containsAll(TaintState pOther) {
    if (taintError && !pOther.taintError) {
      return false;
    }
    for (CFAEdge edge : taintedEdges.keySet()) {
      if (!pOther.taintedEdges.containsKey(edge)) {
        return false;
      }
    }
    for (MemoryLocation memoryLocation : taintedMemoryLocations.keySet()) {
      if (!pOther.taintedMemoryLocations.containsKey(memoryLocation)) {
        return false;
      }
    }
    return true;
  }

  public TaintState taint(CFAEdge pEdge) {
    PersistentSortedMap<CFAEdge, Void> newTaintedEdges = taintedEdges.putAndCopy(pEdge, null);
    return new TaintState(taintError, newTaintedEdges, taintedMemoryLocations);
  }

  public TaintState taint(MemoryLocation pMemoryLocation) {
    PersistentSortedMap<MemoryLocation, Void> newTaintedMemoryLocations =
        taintedMemoryLocations.putAndCopy(pMemoryLocation, null);
    return new TaintState(taintError, taintedEdges, newTaintedMemoryLocations);
  }

  public TaintState untaint(CFAEdge pEdge) {
    PersistentSortedMap<CFAEdge, Void> newTaintedEdges = taintedEdges.removeAndCopy(pEdge);
    return new TaintState(taintError, newTaintedEdges, taintedMemoryLocations);
  }

  public TaintState untaint(MemoryLocation pMemoryLocation) {
    PersistentSortedMap<MemoryLocation, Void> newTaintedMemoryLocations =
        taintedMemoryLocations.removeAndCopy(pMemoryLocation);
    return new TaintState(taintError, taintedEdges, newTaintedMemoryLocations);
  }

  public TaintState union(TaintState pOther) {
    boolean newTaintError = taintError || pOther.taintError;
    PersistentSortedMap<CFAEdge, Void> newTaintedEdges = union(taintedEdges, pOther.taintedEdges);
    PersistentSortedMap<MemoryLocation, Void> newTaintedMemoryLocations =
        union(taintedMemoryLocations, pOther.taintedMemoryLocations);
    return new TaintState(newTaintError, newTaintedEdges, newTaintedMemoryLocations);
  }

  @Override
  public int hashCode() {
    return Objects.hash(taintError, taintedEdges, taintedMemoryLocations);
  }

  @Override
  public boolean equals(Object pOther) {

    if (this == pOther) {
      return true;
    }

    if (!(pOther instanceof TaintState)) {
      return false;
    }

    TaintState other = (TaintState) pOther;
    return taintedEdges == other.taintedEdges
        && Objects.equals(taintedEdges, other.taintedEdges)
        && Objects.equals(taintedMemoryLocations, other.taintedMemoryLocations);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("taintError", taintError)
        .add("taintedEdges", taintedEdges)
        .add("taintedMemoryLocations", taintedMemoryLocations)
        .toString();
  }
}
