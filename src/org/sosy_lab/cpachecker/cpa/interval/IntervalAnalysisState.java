// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.interval;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Splitter;
import com.google.common.collect.ComparisonChain;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.core.interfaces.PseudoPartitionable;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;
import org.sosy_lab.cpachecker.util.CheckTypesOfStringsUtil;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.IntegerFormulaManager;
import org.sosy_lab.java_smt.api.NumeralFormula;

public class IntervalAnalysisState
    implements Serializable,
        LatticeAbstractState<IntervalAnalysisState>,
        AbstractQueryableState,
        Graphable,
        FormulaReportingState,
        PseudoPartitionable {

  private static final long serialVersionUID = -2030700797958100666L;

  private static final Splitter propertySplitter = Splitter.on("<=").trimResults();

  /** the intervals of the element */
  private final PersistentMap<String, Interval> intervals;

  /** the reference counts of the element */
  private final PersistentMap<String, Integer> referenceCounts;

  /**
   * This method acts as the default constructor, which initializes the intervals and reference
   * counts to empty maps and the previous element to null.
   */
  public IntervalAnalysisState() {
    intervals = PathCopyingPersistentTreeMap.of();
    referenceCounts = PathCopyingPersistentTreeMap.of();
  }

  /**
   * This method acts as constructor, which initializes the intervals, the reference counts and the
   * previous element to the respective objects.
   *
   * @param intervals the intervals
   * @param referencesMap the reference counts
   */
  public IntervalAnalysisState(
      PersistentMap<String, Interval> intervals, PersistentMap<String, Integer> referencesMap) {
    this.intervals = intervals;
    referenceCounts = referencesMap;
  }

  /**
   * This method returns the intervals of a given variable.
   *
   * @param variableName the name of the variable
   * @return the intervals of the variable
   */
  // see ExplicitState::getValueFor
  public Interval getInterval(String variableName) {
    return intervals.getOrDefault(variableName, Interval.UNBOUND);
  }

  /**
   * This method returns the reference count for a given variable.
   *
   * @param variableName of the variable to query the reference count on
   * @return the reference count of the variable, or 0 if the the variable is not yet referenced
   */
  private Integer getReferenceCount(String variableName) {
    return referenceCounts.getOrDefault(variableName, 0);
  }

  /**
   * This method determines if this element contains an interval for a variable.
   *
   * @param variableName the name of the variable
   * @return true, if this element contains an interval for the given variable
   */
  public boolean contains(String variableName) {
    return intervals.containsKey(variableName);
  }

  /**
   * This method assigns an interval to a variable and puts it in the map.
   *
   * @param variableName name of the variable
   * @param interval the interval to be assigned
   * @param pThreshold threshold from property valueAnalysis.threshold
   * @return this
   */
  // see ExplicitState::assignConstant
  public IntervalAnalysisState addInterval(String variableName, Interval interval, int pThreshold) {
    if (interval.isUnbound()) {
      return removeInterval(variableName);
    }
    // only add the interval if it is not already present
    if (!intervals.containsKey(variableName) || !intervals.get(variableName).equals(interval)) {
      int referenceCount = getReferenceCount(variableName);

      if (pThreshold == -1 || referenceCount < pThreshold) {
        return new IntervalAnalysisState(
            intervals.putAndCopy(variableName, interval),
            referenceCounts.putAndCopy(variableName, referenceCount + 1));
      } else {
        return removeInterval(variableName);
      }
    }
    return this;
  }

  /**
   * This method removes the interval for a given variable.
   *
   * @param variableName the name of the variable whose interval should be removed
   * @return this
   */
  // see ExplicitState::forget
  public IntervalAnalysisState removeInterval(String variableName) {
    if (intervals.containsKey(variableName)) {
      return new IntervalAnalysisState(intervals.removeAndCopy(variableName), referenceCounts);
    }

    return this;
  }

  public IntervalAnalysisState dropFrame(String pCalledFunctionName) {
    IntervalAnalysisState tmp = this;
    for (String variableName : intervals.keySet()) {
      if (variableName.startsWith(pCalledFunctionName + "::")) {
        tmp = tmp.removeInterval(variableName);
      }
    }
    return tmp;
  }

  /**
   * This element joins this element with a reached state.
   *
   * @param reachedState the reached state to join this element with
   * @return a new state representing the join of this element and the reached state
   */
  @Override
  public IntervalAnalysisState join(IntervalAnalysisState reachedState) {
    boolean changed = false;
    PersistentMap<String, Interval> newIntervals = PathCopyingPersistentTreeMap.of();
    PersistentMap<String, Integer> newReferences = referenceCounts;

    for (String variableName : reachedState.intervals.keySet()) {
      Integer otherRefCount = reachedState.getReferenceCount(variableName);
      Interval otherInterval = reachedState.getInterval(variableName);
      if (intervals.containsKey(variableName)) {
        // update the interval
        Interval mergedInterval = getInterval(variableName).union(otherInterval);
        if (mergedInterval != otherInterval) {
          changed = true;
        }

        if (!mergedInterval.isUnbound()) {
          newIntervals = newIntervals.putAndCopy(variableName, mergedInterval);
        }

        // update the references
        Integer thisRefCount = getReferenceCount(variableName);
        if (mergedInterval != otherInterval && thisRefCount > otherRefCount) {
          changed = true;
          newReferences = newReferences.putAndCopy(variableName, thisRefCount);
        } else {
          newReferences = newReferences.putAndCopy(variableName, otherRefCount);
        }

      } else {
        newReferences = newReferences.putAndCopy(variableName, otherRefCount);
        changed = true;
      }
    }

    if (changed) {
      return new IntervalAnalysisState(newIntervals, newReferences);
    } else {
      return reachedState;
    }
  }

  /**
   * This method decides if this element is less or equal than the reached state, based on the order
   * imposed by the lattice.
   *
   * @param reachedState the reached state
   * @return true, if this element is less or equal than the reached state, based on the order
   *     imposed by the lattice
   */
  @Override
  public boolean isLessOrEqual(IntervalAnalysisState reachedState) {
    if (intervals.equals(reachedState.intervals)) {
      return true;
    }
    // this element is not less or equal than the reached state, if it contains less intervals
    if (intervals.size() < reachedState.intervals.size()) {
      return false;
    }

    // also, this element is not less or equal than the reached state, if any one interval of the
    // reached state is not contained in this element,
    // or if the interval of the reached state is not wider than the respective interval of this
    // element
    for (String variableName : reachedState.intervals.keySet()) {
      if (!intervals.containsKey(variableName)
          || !reachedState.getInterval(variableName).contains(getInterval(variableName))) {
        return false;
      }
    }

    // else, this element < reached state on the lattice
    return true;
  }

  /** Returns the set of tracked variables by this state. */
  public Map<String, Interval> getIntervalMap() {
    return intervals;
  }

  /**
   * If there was a recursive function, we have wrong intervals for scoped variables in the
   * returnState. This function rebuilds a new state with the correct intervals from the previous
   * callState. We delete the wrong intervals and insert new intervals, if necessary.
   */
  public IntervalAnalysisState rebuildStateAfterFunctionCall(
      final IntervalAnalysisState callState, final FunctionExitNode functionExit) {

    // we build a new state from:
    // - local variables from callState,
    // - global variables from THIS,
    // - the local return variable from THIS.
    // we copy callState and override all global values and the return variable.

    IntervalAnalysisState rebuildState = callState;

    // first forget all global information
    for (final String trackedVar : callState.intervals.keySet()) {
      if (!trackedVar.contains("::")) { // global -> delete
        rebuildState = rebuildState.removeInterval(trackedVar);
      }
    }

    // second: learn new information
    for (final String trackedVar : intervals.keySet()) {

      if (!trackedVar.contains("::")) { // global -> override deleted value
        rebuildState = rebuildState.addInterval(trackedVar, getInterval(trackedVar), -1);

      } else if (functionExit.getEntryNode().getReturnVariable().isPresent()
          && functionExit
              .getEntryNode()
              .getReturnVariable()
              .get()
              .getQualifiedName()
              .equals(trackedVar)) {
        assert !rebuildState.contains(trackedVar)
            : "calling function should not contain return-variable of called function: "
                + trackedVar;
        if (contains(trackedVar)) {
          rebuildState = rebuildState.addInterval(trackedVar, getInterval(trackedVar), -1);
        }
      }
    }

    return rebuildState;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }

    if (other instanceof IntervalAnalysisState) {
      IntervalAnalysisState otherElement = (IntervalAnalysisState) other;
      return intervals.equals(otherElement.intervals);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return intervals.hashCode();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("[\n");

    for (Map.Entry<String, Interval> entry : intervals.entrySet()) {
      sb.append(
          String.format(
              "  < %s = %s :: %s >%n",
              entry.getKey(), entry.getValue(), getReferenceCount(entry.getKey())));
    }

    return sb.append("] size -> ").append(intervals.size()).toString();
  }

  @Override
  public String getCPAName() {
    return "IntervalAnalysis";
  }

  @Override
  public boolean checkProperty(String pProperty) throws InvalidQueryException {
    List<String> parts = propertySplitter.splitToList(pProperty);

    if (parts.size() == 2) {

      // pProperty = value <= varName
      if (CheckTypesOfStringsUtil.isLong(parts.get(0))) {
        long value = Long.parseLong(parts.get(0));
        Interval iv = getInterval(parts.get(1));
        return (value <= iv.getLow());
      }

      // pProperty = varName <= value
      else if (CheckTypesOfStringsUtil.isLong(parts.get(1))) {
        long value = Long.parseLong(parts.get(1));
        Interval iv = getInterval(parts.get(0));
        return (iv.getHigh() <= value);
      }

      // pProperty = varName1 <= varName2
      else {
        Interval iv1 = getInterval(parts.get(0));
        Interval iv2 = getInterval(parts.get(1));
        return iv1.contains(iv2);
      }

      // pProperty = value1 <= varName <= value2
    } else if (parts.size() == 3) {
      if (CheckTypesOfStringsUtil.isLong(parts.get(0))
          && CheckTypesOfStringsUtil.isLong(parts.get(2))) {
        long value1 = Long.parseLong(parts.get(0));
        long value2 = Long.parseLong(parts.get(2));
        Interval iv = getInterval(parts.get(1));
        return (value1 <= iv.getLow() && iv.getHigh() <= value2);
      }
    }

    return false;
  }

  @Override
  public String toDOTLabel() {
    StringBuilder sb = new StringBuilder();

    sb.append("{");
    // create a string like: x =  [low; high] (refCount)
    for (Entry<String, Interval> entry : intervals.entrySet()) {
      sb.append(
          String.format(
              "%s = %s (%s), ",
              entry.getKey(), entry.getValue(), getReferenceCount(entry.getKey())));
    }
    sb.append("}");

    return sb.toString();
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  @Override
  public BooleanFormula getFormulaApproximation(FormulaManagerView pMgr) {
    IntegerFormulaManager nfmgr = pMgr.getIntegerFormulaManager();
    List<BooleanFormula> result = new ArrayList<>();
    for (Entry<String, Interval> entry : intervals.entrySet()) {
      Interval interval = entry.getValue();
      if (interval.isEmpty()) {
        // one invalid interval disqualifies the whole state
        return pMgr.getBooleanFormulaManager().makeFalse();
      }

      // we assume that everything is an SIGNED INTEGER
      // and build "LOW <= X" and "X <= HIGH"
      NumeralFormula var = nfmgr.makeVariable(entry.getKey());
      Long low = interval.getLow();
      Long high = interval.getHigh();
      if (low != null && low != Long.MIN_VALUE) { // check for unbound interval
        result.add(pMgr.makeLessOrEqual(nfmgr.makeNumber(low), var, true));
      }
      if (high != null && high != Long.MIN_VALUE) { // check for unbound interval
        result.add(pMgr.makeGreaterOrEqual(nfmgr.makeNumber(high), var, true));
      }
    }
    return pMgr.getBooleanFormulaManager().and(result);
  }

  @Override
  public Comparable<?> getPseudoPartitionKey() {
    // The size alone is not sufficient for pseudo-partitioning, if we want to use object-identity
    // as hashcode. Thus we need a second measurement: the absolute distance of all intervals.
    // -> if the distance is "smaller" than the other state, we know nothing and have to compare the
    // states.
    // -> if the distance is "equal", we can compare by "identity".
    // -> if the distance is "greater", we are "greater" than the other state.
    // We negate the absolute distance to match the "lessEquals"-specifiction.
    // Be aware of overflows! -> we use BigInteger, and zero should be a sound value.
    BigInteger absDistance = BigInteger.ZERO;
    for (Interval i : intervals.values()) {
      long high = i.getHigh() == null ? 0 : i.getHigh();
      long low = i.getLow() == null ? 0 : i.getLow();
      checkArgument(low <= high, "LOW greater than HIGH: %s", i);
      absDistance = absDistance.add(BigInteger.valueOf(high).subtract(BigInteger.valueOf(low)));
    }
    return new IntervalPseudoPartitionKey(intervals.size(), absDistance.negate());
  }

  @Override
  public Object getPseudoHashCode() {
    return this;
  }

  /** Just a pair of values, can be compared alphabetically. */
  private static final class IntervalPseudoPartitionKey
      implements Comparable<IntervalPseudoPartitionKey> {

    private final int size;
    private final BigInteger absoluteDistance;

    public IntervalPseudoPartitionKey(int pSize, BigInteger pAbsoluteDistance) {
      size = pSize;
      absoluteDistance = pAbsoluteDistance;
    }

    @Override
    public boolean equals(Object pObj) {
      if (this == pObj) {
        return true;
      }

      if (!(pObj instanceof IntervalPseudoPartitionKey)) {
        return false;
      }

      IntervalPseudoPartitionKey other = (IntervalPseudoPartitionKey) pObj;
      return size == other.size && absoluteDistance.equals(other.absoluteDistance);
    }

    @Override
    public int hashCode() {
      return 137 * size + absoluteDistance.hashCode();
    }

    @Override
    public String toString() {
      return "[" + size + ", " + absoluteDistance + "]";
    }

    @Override
    public int compareTo(IntervalPseudoPartitionKey other) {
      return ComparisonChain.start()
          .compare(size, other.size)
          .compare(absoluteDistance, other.absoluteDistance)
          .result();
    }
  }
}
