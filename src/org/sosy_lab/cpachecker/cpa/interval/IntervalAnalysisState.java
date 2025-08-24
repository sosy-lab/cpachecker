// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.interval;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sosy_lab.cpachecker.cpa.interval.ExpressionUtility.normalizeExpression;

import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.collect.ComparisonChain;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.core.interfaces.PseudoPartitionable;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.IntegerFormulaManager;
import org.sosy_lab.java_smt.api.NumeralFormula;

public final class IntervalAnalysisState
    implements Serializable,
               LatticeAbstractState<IntervalAnalysisState>,
               AbstractQueryableState,
               Graphable,
               FormulaReportingState,
               PseudoPartitionable {

  @Serial
  private static final long serialVersionUID = -2030700797958100666L;

  private static final Splitter propertySplitter = Splitter.on("<=").trimResults();
  private final PersistentMap<String, Interval> intervals;
  private final PersistentMap<String, Integer> referenceCounts;
  private final PersistentMap<String, FunArray> arrays;
  private final CFANode location;
  private final PersistentMap<CFANode, Integer> visitCounts;

  private IntervalAnalysisState(
      PersistentMap<String, Interval> pIntervals,
      PersistentMap<String, Integer> pReferenceCounts,
      PersistentMap<String, FunArray> pArrays,
      CFANode pLocation,
      PersistentMap<CFANode, Integer> pVisitCounts
  ) {
    this.intervals = pIntervals;
    this.referenceCounts = pReferenceCounts;
    this.arrays = pArrays;
    this.location = pLocation;
    this.visitCounts = pVisitCounts;
  }


  /**
   * This method acts as the default constructor, which initializes the intervals and reference
   * counts to empty maps and the previous element to null.
   */
  public IntervalAnalysisState(CFANode pLocation) {
    this(
        PathCopyingPersistentTreeMap.of(),
        PathCopyingPersistentTreeMap.of(),
        PathCopyingPersistentTreeMap.of(),
        pLocation,
        PathCopyingPersistentTreeMap.of()
    );
  }

  public IntervalAnalysisState withIntervals(PersistentMap<String, Interval> pIntervals) {
    return new IntervalAnalysisState(
        pIntervals,
        referenceCounts,
        arrays,
        location,
        visitCounts
    );
  }

  public IntervalAnalysisState withReferenceCounts(PersistentMap<String, Integer> pReferenceCounts) {
    return new IntervalAnalysisState(
        intervals,
        pReferenceCounts,
        arrays,
        location,
        visitCounts
    );
  }

  public IntervalAnalysisState withArrays(PersistentMap<String, FunArray> pArrays) {
    return new IntervalAnalysisState(
        intervals,
        referenceCounts,
        pArrays,
        location,
        visitCounts
    );
  }

  public IntervalAnalysisState withLocation(CFANode pLocation) {
    return new IntervalAnalysisState(
        intervals,
        referenceCounts,
        arrays,
        pLocation,
        visitCounts.putAndCopy(pLocation,
            visitCounts.containsKey(pLocation)
            ? visitCounts.get(pLocation) + 1
            : 1
        )
    );
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

  public boolean containsArray(String variableName) {
    return arrays.containsKey(variableName);
  }

  public Interval arrayAccess(
      String variableName, CExpression index, ExpressionValueVisitor visitor)
      throws UnrecognizedCodeException {
    return arrays.get(variableName)
        .get(normalizeExpression(index, visitor).stream().findAny().orElseThrow(),
            visitor); //TODO: Don't just pick any random normalization, but rather the one that results in the least abstract result
  }

  /**
   * This method assigns an interval to a variable and puts it in the map.
   *
   * @param variableName name of the variable
   * @param interval     the interval to be assigned
   * @param pThreshold   threshold from property valueAnalysis.threshold
   * @return this
   */
  // see ExplicitState::assignConstant
  public IntervalAnalysisState addInterval(
      String variableName,
      Interval interval,
      int pThreshold,
      CFANode pLocation) {
    if (interval.isUnbound()) {
      return removeInterval(variableName, pLocation);
    }
    // only add the interval if it is not already present
    if (!intervals.containsKey(variableName) || !intervals.get(variableName).equals(interval)) {
      int referenceCount = getReferenceCount(variableName);

      if (pThreshold == -1 || referenceCount < pThreshold) {
        return this.withIntervals(intervals.putAndCopy(variableName, interval))
            .withReferenceCounts(referenceCounts.putAndCopy(variableName, referenceCount + 1));
      } else {
        return removeInterval(variableName, pLocation);
      }
    }
    return this;
  }

  public IntervalAnalysisState addArray(String variableName, FunArray funArray, CFANode pLocation) {
    return this.withArrays(arrays.putAndCopy(variableName, funArray))
        .withLocation(pLocation);
  }

  public IntervalAnalysisState assignArrayElement(
      String arrayName,
      NormalFormExpression index,
      Interval interval,
      ExpressionValueVisitor visitor,
      CFANode pLocation) {
    if (arrays.containsKey(arrayName)) {
      return this.withArrays(arrays.putAndCopy(arrayName, arrays.get(arrayName).insert(index, interval, visitor)))
          .withLocation(pLocation);
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
  public IntervalAnalysisState removeInterval(String variableName, CFANode pLocation) {
    if (intervals.containsKey(variableName)) {
      return this.withIntervals(intervals.removeAndCopy(variableName))
          .withLocation(pLocation);
    }
    return this;
  }

  public IntervalAnalysisState dropFrame(String pCalledFunctionName, CFANode pLocation) {
    IntervalAnalysisState tmp = this;
    for (String variableName : intervals.keySet()) {
      if (variableName.startsWith(pCalledFunctionName + "::")) {
        tmp = tmp.removeInterval(variableName, pLocation);
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
    // TODO: Join arrays as well
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
      CFANode newLocation = null;
      if (this.location.equals(reachedState.location)) {
        newLocation = reachedState.location;
      }
      return this
          .withIntervals(newIntervals)
          .withReferenceCounts(newReferences)
          .withLocation(newLocation);
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
   * imposed by the lattice
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

    for (String arrayName : reachedState.arrays.keySet()) {
      if (!arrays.containsKey(arrayName) || !arrays.get(arrayName)
          .isLessOrEqual(reachedState.arrays.get(arrayName))) {
        return false;
      }
    }

    // else, this element < reached state on the lattice
    return true;
  }

  /**
   * Returns the set of tracked variables by this state.
   */
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
        rebuildState = rebuildState.removeInterval(trackedVar, functionExit);
      }
    }

    // second: learn new information
    for (final String trackedVar : intervals.keySet()) {

      if (!trackedVar.contains("::")) { // global -> override deleted value
        rebuildState =
            rebuildState.addInterval(trackedVar, getInterval(trackedVar), -1, functionExit);

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
          rebuildState =
              rebuildState.addInterval(trackedVar, getInterval(trackedVar), -1, functionExit);
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

    return other instanceof IntervalAnalysisState otherElement
        && intervals.equals(otherElement.intervals);
  }

  @Override
  public int hashCode() {
    return intervals.hashCode();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("[\n");

    for (Entry<String, Interval> entry : intervals.entrySet()) {
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

  private static boolean isLong(String s) {
    return Pattern.matches("-?\\d+", s);
  }

  @Override
  public boolean checkProperty(String pProperty) throws InvalidQueryException {
    List<String> parts = propertySplitter.splitToList(pProperty);

    if (parts.size() == 2) {

      if (isLong(parts.get(0))) {
        // pProperty = value <= varName
        long value = Long.parseLong(parts.get(0));
        Interval iv = getInterval(parts.get(1));
        return (value <= iv.getLow());

      } else if (isLong(parts.get(1))) {
        // pProperty = varName <= value
        long value = Long.parseLong(parts.get(1));
        Interval iv = getInterval(parts.get(0));
        return (iv.getHigh() <= value);

      } else {
        // pProperty = varName1 <= varName2
        Interval iv1 = getInterval(parts.get(0));
        Interval iv2 = getInterval(parts.get(1));
        return iv1.contains(iv2);
      }

      // pProperty = value1 <= varName <= value2
    } else if (parts.size() == 3) {
      if (isLong(parts.get(0)) && isLong(parts.get(2))) {
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
              "%s = %s (%s),",
              entry.getKey(), entry.getValue(), getReferenceCount(entry.getKey())));
    }

    for (Entry<String, FunArray> entry : arrays.entrySet()) {
      sb.append(String.format("%s = %s,", entry.getKey(), entry.getValue()));
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
    return getFormulaApproximationWithSpecifiedVars(pMgr, Predicates.alwaysTrue(), true);
  }

  @Override
  public BooleanFormula getScopedFormulaApproximation(
      final FormulaManagerView pManager, final FunctionEntryNode pFunctionScope) {
    return getFormulaApproximationWithSpecifiedVars(
        pManager,
        name ->
            !name.startsWith("__CPAchecker_TMP_")
                && !name.contains("::__CPAchecker_TMP_")
                && (name.startsWith(pFunctionScope.getFunctionName() + "::")
                || !name.contains("::")),
        false);
  }

  private BooleanFormula getFormulaApproximationWithSpecifiedVars(
      final FormulaManagerView pMgr,
      final Predicate<String> considerVar,
      final boolean useQualifiedVarNames) {
    IntegerFormulaManager nfmgr = pMgr.getIntegerFormulaManager();
    List<BooleanFormula> result = new ArrayList<>();
    for (Entry<String, Interval> entry : intervals.entrySet()) {
      if (considerVar.test(entry.getKey())) {
        Interval interval = entry.getValue();
        if (interval.isEmpty()) {
          // one invalid interval disqualifies the whole state
          return pMgr.getBooleanFormulaManager().makeFalse();
        }

        // we assume that everything is an SIGNED INTEGER
        // and build "LOW <= X" and "X <= HIGH"
        NumeralFormula var =
            nfmgr.makeVariable(
                useQualifiedVarNames
                ? entry.getKey()
                : (entry.getKey().contains("::")
                   ? entry.getKey().substring(entry.getKey().indexOf("::") + 2)
                   : entry.getKey()));
        Long low = interval.getLow();
        Long high = interval.getHigh();
        if (low != null && low != Long.MIN_VALUE) { // check for unbound interval
          result.add(pMgr.makeLessOrEqual(nfmgr.makeNumber(low), var, true));
        }
        if (high != null && high != Long.MIN_VALUE) { // check for unbound interval
          result.add(pMgr.makeGreaterOrEqual(nfmgr.makeNumber(high), var, true));
        }
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

  /**
   * Just a pair of values, can be compared alphabetically.
   */
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

      return pObj instanceof IntervalPseudoPartitionKey other
          && size == other.size
          && absoluteDistance.equals(other.absoluteDistance);
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

  public IntervalAnalysisState adaptToVariableAssignment(
      CIdExpression changedVariable,
      Set<NormalFormExpression> expressions,
      CFANode pLocation) { //TODO replace with forAllArrays() method

    PersistentMap<String, FunArray> adaptedArrays = arrays.empty();

    for (Entry<String, FunArray> entry : arrays.entrySet()) {
      adaptedArrays = adaptedArrays.putAndCopy(
          entry.getKey(),
          entry.getValue().adaptToVariableAssignment(changedVariable, expressions)
      );
    }
    return this.withArrays(adaptedArrays).withLocation(pLocation);
  }

  public IntervalAnalysisState satisfyStrictLessThan(
      Set<NormalFormExpression> lesserSet,
      Set<NormalFormExpression> greaterSet,
      CFANode pLocation) {

    IntervalAnalysisState modifiedState = this;

    for (NormalFormExpression lesser : lesserSet) {
      for (NormalFormExpression greater : greaterSet) {
        modifiedState =
            modifiedState.forAllArrays(e -> e.satisfyStrictLessThan(lesser, greater), pLocation);
      }
    }

    return modifiedState;
  }

  public IntervalAnalysisState satisfyLessEqual(
      Set<NormalFormExpression> lesserSet,
      Set<NormalFormExpression> greaterSet,
      CFANode pLocation) {
    IntervalAnalysisState modifiedState = this;

    for (NormalFormExpression lesser : lesserSet) {
      for (NormalFormExpression greater : greaterSet) {
        modifiedState =
            modifiedState.forAllArrays(e -> e.satisfyLessEqual(lesser, greater), pLocation);
      }
    }

    return modifiedState;
  }

  public IntervalAnalysisState forAllArrays(
      Function<FunArray, FunArray> function,
      CFANode pLocation) {
    PersistentMap<String, FunArray> modifiedArrays = arrays.empty();
    for (Entry<String, FunArray> entry : arrays.entrySet()) {
      modifiedArrays = modifiedArrays.putAndCopy(
          entry.getKey(),
          function.apply(entry.getValue())
      );
    }

    return this.withArrays(modifiedArrays).withLocation(pLocation);
  }

  public IntervalAnalysisState widen(IntervalAnalysisState other, CFANode pLocation) {

    var modifiedFunArrays = other.arrays.entrySet().stream()
        .collect(Collectors.toMap(
            Entry::getKey,
            e -> {

              FunArray leftSide = arrays.get(e.getKey());
              FunArray rightSide = e.getValue();

              if (leftSide == null) {
                return rightSide;
              }

              return leftSide.widen(rightSide);
            }
        ));

    var modifiedVariables = other.intervals.entrySet().stream()
        .collect(Collectors.toMap(
            Entry::getKey,
            e -> {

              Interval leftSide = intervals.get(e.getKey());
              Interval rightSide = e.getValue();

              if (leftSide == null) {
                return rightSide;
              }
              return leftSide.widen(rightSide);
            }
        ));

    return this.withIntervals(PathCopyingPersistentTreeMap.copyOf(modifiedVariables))
        .withArrays(PathCopyingPersistentTreeMap.copyOf(modifiedFunArrays))
        .withLocation(pLocation); //TODO: Widening der reference counts überlegen
  }

  public PersistentMap<String, Interval> intervals() {
    return intervals;
  }

  public PersistentMap<String, Integer> referenceCounts() {
    return referenceCounts;
  }

  public PersistentMap<String, FunArray> arrays() {
    return arrays;
  }

  public CFANode location() {
    return location;
  }


}
