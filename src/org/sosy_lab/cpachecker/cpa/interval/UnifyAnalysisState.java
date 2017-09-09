/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.interval;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.annotation.Nullable;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.core.interfaces.PseudoPartitionable;
import org.sosy_lab.cpachecker.cpa.sign.SIGN;
import org.sosy_lab.cpachecker.cpa.sign.SignTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;
import org.sosy_lab.cpachecker.util.CheckTypesOfStringsUtil;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.refinement.ForgetfulState;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.IntegerFormulaManager;
import org.sosy_lab.java_smt.api.NumeralFormula;

public class UnifyAnalysisState
        implements Serializable, LatticeAbstractState<UnifyAnalysisState>, AbstractQueryableState, Graphable,
        FormulaReportingState, PseudoPartitionable, ForgetfulState<UnifyAnalysisState> {
    private static final long serialVersionUID = -3057330425781141121L;

    private final NumericalType numericalType;
    private static final boolean DEBUG = false;

    private static final Splitter propertySplitter = Splitter.on("<=").trimResults();

    /**
     * the intervals/signs of the element
     */
    private final PersistentMap<String, NumberInterface> unifyElements;

    /**
     * the reference counts of the element
     */
    private final PersistentMap<String, Integer> referenceCounts;


    public final static UnifyAnalysisState TOP = new UnifyAnalysisState(NumericalType.SIGN);
    private final static SerialProxySign proxy = new SerialProxySign();

    private UnifyAnalysisState(PersistentMap<String, NumberInterface> pSignMap) {
        unifyElements = pSignMap;
        referenceCounts = PathCopyingPersistentTreeMap.of();
        numericalType = NumericalType.SIGN;
    }

    public UnifyAnalysisState() {
        unifyElements = PathCopyingPersistentTreeMap.of();
        referenceCounts = PathCopyingPersistentTreeMap.of();
        numericalType = NumericalType.INTERVAL;
    }
    private UnifyAnalysisState(NumericalType nt) {
        unifyElements = PathCopyingPersistentTreeMap.of();
        referenceCounts = PathCopyingPersistentTreeMap.of();
        numericalType = nt;
    }
    public UnifyAnalysisState(PersistentMap<String, NumberInterface> intervals,
            PersistentMap<String, Integer> referencesMap) {
        this.unifyElements = intervals;
        this.referenceCounts = referencesMap;
        numericalType = NumericalType.INTERVAL;
    }

    @Override
    public UnifyAnalysisState forget(MemoryLocation pLocation) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void remember(MemoryLocation pLocation, UnifyAnalysisState pForgottenInformation) {
        // TODO Auto-generated method stub

    }

    @Override
    public Set<MemoryLocation> getTrackedMemoryLocations() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getSize() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    @Nullable
    public Comparable<?> getPseudoPartitionKey() {
        switch (numericalType) {
        case INTERVAL:
            return getPseudoPartitionKeyInterval();
        case VALUE:
            return null;
        default:
            break;
        }
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    @Nullable
    public Object getPseudoHashCode() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BooleanFormula getFormulaApproximation(FormulaManagerView pManager) {
        switch (numericalType) {
        case INTERVAL:
            return getFormulaApproximationInterval(pManager);
        case VALUE:
            return null;
        default:
            break;
        }
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String toDOTLabel() {
        switch (numericalType) {
        case SIGN:
            return toDOTLabelSing();
        case INTERVAL:
            return toDOTLabelInterval();
        default:
            break;
        }
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean shouldBeHighlighted() {
        switch (numericalType) {
        case SIGN:
        case INTERVAL:
            return false;
        default:
            break;
        }
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getCPAName() {
        switch (numericalType) {
        case SIGN:
            return "SignAnalysis";
        case INTERVAL:
            return "IntervalAnalysis";
        default:
            break;
        }
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UnifyAnalysisState join(UnifyAnalysisState pOther) throws CPAException, InterruptedException {
        switch (numericalType) {
        case SIGN:
            return signStateJoin(pOther);
        case INTERVAL:
            return intervalStateJoin(pOther);
        default:
            break;
        }
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int hashCode() {
        return unifyElements.hashCode();
    }

    @Override
    public boolean isLessOrEqual(UnifyAnalysisState pOther) {// throws CPAException, InterruptedException {
        switch (numericalType) {
        case SIGN:
            return signIsLessOrEqual(pOther);
        case INTERVAL:
            return intervalIsLessOrEqual(pOther);
        default:
            break;
        }
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean equals(Object pObj) {
        switch (numericalType) {
        case SIGN:
            return equalsSing(pObj);
        case INTERVAL:
            return equalsInterval(pObj);
        default:
            break;
        }
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean checkProperty(String pProperty) throws InvalidQueryException {
        switch (numericalType) {
        case SIGN:
            return checkPropertySign(pProperty);
        case INTERVAL:
            return checkPropertyInterval(pProperty);
        default:
            break;
        }
        // TODO Auto-generated method stub
        return false;
    }

    private boolean signIsLessOrEqual(UnifyAnalysisState pSuperset) {
        if (pSuperset.equals(this) || pSuperset.equals(TOP)) {
            return true;
        }
        if (unifyElements.size() < pSuperset.unifyElements.size()) {
            return false;
        }
        // is subset if for every variable all sign assumptions are considered in
        // pSuperset
        // check that all variables in superset with SIGN != ALL have no bigger
        // assumptions in subset
        for (String varIdent : pSuperset.unifyElements.keySet()) {
            if (!getSignForVariable(varIdent).isSubsetOf(pSuperset.getSignForVariable(varIdent))) {
                return false;
            }
        }
        return true;
    }

    private UnifyAnalysisState signStateJoin(UnifyAnalysisState pToJoin) {
        if (pToJoin.equals(this)) {
            return pToJoin;
        }
        if (this.equals(TOP) || pToJoin.equals(TOP)) {
            return TOP;
        }

        // assure termination of loops do not merge if pToJoin covers this but return
        // pToJoin
        if (isLessOrEqual(pToJoin)) {
            return pToJoin;
        }

        UnifyAnalysisState result = UnifyAnalysisState.TOP;
        PersistentMap<String, NumberInterface> newMap = PathCopyingPersistentTreeMap.of();
        NumberInterface combined;
        for (String varIdent : pToJoin.unifyElements.keySet()) {
            // only add those variables that are contained in both states (otherwise one has
            // value ALL (not saved))
            if (unifyElements.containsKey(varIdent)) {
                combined = getSignForVariable(varIdent).combineWith(pToJoin.getSignForVariable(varIdent));
                if (!((SIGN) combined).isAll()) {
                    newMap = newMap.putAndCopy(varIdent, combined);
                }
            }
        }
        return newMap.size() > 0 ? new UnifyAnalysisState(newMap) : result;
    }

    private static class SerialProxySign implements Serializable {

        private static final long serialVersionUID = 2843708585446089623L;

        public SerialProxySign() {
        }

        private Object readResolve() {
            return TOP;
        }
    }

    @Override
    public String toString() {
        switch (numericalType) {
        case SIGN:
            return toStringSIGN();
        case INTERVAL:
            return toStringInterval();
        default:
            break;
        }
        // TODO Auto-generated method stub
        return null;
    }

    /**
     *
     * The Method is for sign
     *
     */
    private String toStringSIGN() {
        StringBuilder builder = new StringBuilder();
        String delim = ", ";
        builder.append("[");
        String loopDelim = "";
        for (String key : unifyElements.keySet()) {
            if (!DEBUG && (key.matches("\\w*::__CPAchecker_TMP_\\w*")
                    || key.endsWith(SignTransferRelation.FUNC_RET_VAR))) {
                continue;
            }
            builder.append(loopDelim);
            builder.append(key + "->" + getSignForVariable(key));
            loopDelim = delim;
        }
        builder.append("]");
        return builder.toString();
    }

    private boolean equalsSing(Object pObj) {
        if (!(pObj instanceof UnifyAnalysisState)) {
            return false;
        }
        return ((UnifyAnalysisState) pObj).unifyElements.equals(this.unifyElements);
    }

    /**
     *
     * The Method is for sign
     *
     */
    public UnifyAnalysisState enterFunction(ImmutableMap<String, NumberInterface> pArguments) {
        PersistentMap<String, NumberInterface> newMap = unifyElements;

        for (String var : pArguments.keySet()) {
            if (!pArguments.get(var).equals(SIGN.ALL)) {
                newMap = newMap.putAndCopy(var, pArguments.get(var));
            }
        }
        return unifyElements == newMap ? this : new UnifyAnalysisState(newMap);
    }

    /**
     *
     * The Method is for sign
     *
     */
    public UnifyAnalysisState leaveFunction(String pFunctionName) {
        PersistentMap<String, NumberInterface> newMap = unifyElements;

        for (String var : unifyElements.keySet()) {
            if (var.startsWith(pFunctionName + "::")) {
                newMap = newMap.removeAndCopy(var);
            }
        }

        return newMap == unifyElements ? this : new UnifyAnalysisState(newMap);
    }

    /**
     *
     * The Method is for sign
     *
     */
    public UnifyAnalysisState assignSignToVariable(String pVarIdent, NumberInterface sign) {
        if (((SIGN) sign).isAll()) {
            return unifyElements.containsKey(pVarIdent) ? new UnifyAnalysisState(unifyElements.removeAndCopy(pVarIdent)) : this;
        }
        return unifyElements.containsKey(pVarIdent) && getSignForVariable(pVarIdent).equals(sign) ? this
                : new UnifyAnalysisState(unifyElements.putAndCopy(pVarIdent, sign));
    }

    /**
     *
     * The Method is for sign
     *
     */
    public UnifyAnalysisState removeSignAssumptionOfVariable(String pVarIdent) {
        return assignSignToVariable(pVarIdent, SIGN.ALL);
    }

    /**
     *
     * The Method is for sign
     *
     */
    public NumberInterface getSignForVariable(String pVarIdent) {
        return unifyElements.containsKey(pVarIdent) ? unifyElements.get(pVarIdent) : SIGN.ALL;
    }

    private boolean checkPropertySign(String pProperty) {
        List<String> parts = propertySplitter.splitToList(pProperty);

        if (parts.size() == 2) {

            // pProperty = value <= varName
            if (CheckTypesOfStringsUtil.isSIGN(parts.get(0))) {
                NumberInterface value = SIGN.valueOf(parts.get(0));
                NumberInterface varName = getSignForVariable(parts.get(1));
                return (varName.covers(value));
            }

            // pProperty = varName <= value
            else if (CheckTypesOfStringsUtil.isSIGN(parts.get(1))) {
                NumberInterface varName = getSignForVariable(parts.get(0));
                NumberInterface value = SIGN.valueOf(parts.get(1));
                return (value.covers(varName));
            }

            // pProperty = varName1 <= varName2
            else {
                NumberInterface varName1 = getSignForVariable(parts.get(0));
                NumberInterface varName2 = getSignForVariable(parts.get(1));
                return (varName2.covers(varName1));
            }
        }

        return false;
    }

    /**
     *
     * The Method is for sign
     *
     */
    private String toDOTLabelSing() {
        StringBuilder sb = new StringBuilder();

        sb.append("{");
        Joiner.on(", ").withKeyValueSeparator("=").appendTo(sb, unifyElements);
        sb.append("}");

        return sb.toString();
    }

    /**
     *
     * The Method is for sign
     *
     */
    public Map<String, NumberInterface> getSignMapView() {
        return Collections.unmodifiableMap(unifyElements);
    }

    /**
     *
     * The Method is for sign Do we need this?
     *
     */
    private Object writeReplace() {
        if (this == TOP) {
            return proxy;
        } else {
            return this;
        }
    }

    /**
     *
     * The Method is for sign Do we need this?
     *
     */
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    /**
     *
     * The Method is for sign Do we need this?
     *
     */
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
    }
    /**
     * This method returns the intervals of a given variable.
     *
     * @param variableName
     *            the name of the variable
     * @return the intervals of the variable
     */
    // see ExplicitState::getValueFor
    public NumberInterface getInterval(String variableName) {
        return unifyElements.getOrDefault(variableName, new CreatorIntegerInterval().factoryMethod(null).UNBOUND());
    }
    /**
     * This method returns the reference count for a given variable.
     *
     * @param variableName
     *            of the variable to query the reference count on
     * @return the reference count of the variable, or 0 if the the variable is
     *         not yet referenced
     */
    private Integer getReferenceCount(String variableName) {
        return referenceCounts.getOrDefault(variableName, 0);
    }
    /**
     * This method determines if this element contains an interval for a
     * variable.
     *
     * @param variableName
     *            the name of the variable
     * @return true, if this element contains an interval for the given variable
     */
    public boolean contains(String variableName) {
        return unifyElements.containsKey(variableName);
    }
    /**
     * This method assigns an interval to a variable and puts it in the map.
     *
     * @param variableName
     *            name of the variable
     * @param interval
     *            the interval to be assigned
     * @param pThreshold
     *            threshold from property valueAnalysis.threshold
     * @return this
     */
    // see ExplicitState::assignConstant
    public UnifyAnalysisState addInterval(String variableName, NumberInterface interval, int pThreshold) {
        if (interval.isUnbound()) {
            return removeInterval(variableName);
        }
        // only add the interval if it is not already present
        if (!unifyElements.containsKey(variableName) || !unifyElements.get(variableName).equals(interval)) {
            int referenceCount = getReferenceCount(variableName);

            if (pThreshold == -1 || referenceCount < pThreshold) {
                return new UnifyAnalysisState(unifyElements.putAndCopy(variableName, interval),
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
     * @param variableName
     *            the name of the variable whose interval should be removed
     * @return this
     */
    // see ExplicitState::forget
    public UnifyAnalysisState removeInterval(String variableName) {
        if (unifyElements.containsKey(variableName)) {
            return new UnifyAnalysisState(unifyElements.removeAndCopy(variableName), referenceCounts);
        }

        return this;
    }
    public UnifyAnalysisState dropFrame(String pCalledFunctionName) {
        UnifyAnalysisState tmp = this;
        for (String variableName : unifyElements.keySet()) {
            if (variableName.startsWith(pCalledFunctionName + "::")) {
                tmp = tmp.removeInterval(variableName);
            }
        }
        return tmp;
    }
    /**
     * This element joins this element with a reached state.
     *
     * @param reachedState
     *            the reached state to join this element with
     * @return a new state representing the join of this element and the reached
     *         state
     */
    private UnifyAnalysisState intervalStateJoin(UnifyAnalysisState reachedState) {
        boolean changed = false;
        PersistentMap<String, NumberInterface> newIntervals = PathCopyingPersistentTreeMap.of();
        PersistentMap<String, Integer> newReferences = referenceCounts;

        for (String variableName : reachedState.unifyElements.keySet()) {
            Integer otherRefCount = reachedState.getReferenceCount(variableName);
            NumberInterface otherInterval = reachedState.getInterval(variableName);
            if (unifyElements.containsKey(variableName)) {
                // update the interval
                NumberInterface mergedInterval = getInterval(variableName).union(otherInterval);
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
            return new UnifyAnalysisState(newIntervals, newReferences);
        } else {
            return reachedState;
        }
    }
    /**
     * This method decides if this element is less or equal than the reached
     * state, based on the order imposed by the lattice.
     *
     * @param reachedState
     *            the reached state
     * @return true, if this element is less or equal than the reached state,
     *         based on the order imposed by the lattice
     */
    private boolean intervalIsLessOrEqual(UnifyAnalysisState reachedState) {
        if (unifyElements.equals(reachedState.unifyElements)) {
            return true;
        }
        // this element is not less or equal than the reached state, if it
        // contains less intervals
        if (unifyElements.size() < reachedState.unifyElements.size()) {
            return false;
        }

        // also, this element is not less or equal than the reached state, if
        // any one interval of the reached state is not contained in this
        // element,
        // or if the interval of the reached state is not wider than the
        // respective interval of this element
        for (String variableName : reachedState.unifyElements.keySet()) {
            if (!unifyElements.containsKey(variableName)
                    || !reachedState.getInterval(variableName).contains(getInterval(variableName))) {
                return false;
            }
        }

        // else, this element < reached state on the lattice
        return true;
    }
    /**
     * @return the set of tracked variables by this state
     */
    public Map<String, NumberInterface> getIntervalMap() {
        return unifyElements;
    }
    /**
     * If there was a recursive function, we have wrong intervals for scoped
     * variables in the returnState. This function rebuilds a new state with the
     * correct intervals from the previous callState. We delete the wrong
     * intervals and insert new intervals, if necessary.
     */
    public UnifyAnalysisState rebuildStateAfterFunctionCall(final UnifyAnalysisState callState,
            final FunctionExitNode functionExit) {

        // we build a new state from:
        // - local variables from callState,
        // - global variables from THIS,
        // - the local return variable from THIS.
        // we copy callState and override all global values and the return
        // variable.

        UnifyAnalysisState rebuildState = callState;

        // first forget all global information
        for (final String trackedVar : callState.unifyElements.keySet()) {
            if (!trackedVar.contains("::")) { // global -> delete
                rebuildState = rebuildState.removeInterval(trackedVar);
            }
        }

        // second: learn new information
        for (final String trackedVar : this.unifyElements.keySet()) {

            if (!trackedVar.contains("::")) { // global -> override deleted
                                              // value
                rebuildState = rebuildState.addInterval(trackedVar, this.getInterval(trackedVar), -1);

            } else if (functionExit.getEntryNode().getReturnVariable().isPresent()
                    && functionExit.getEntryNode().getReturnVariable().get().getQualifiedName().equals(trackedVar)) {
                assert (!rebuildState.contains(
                        trackedVar)) : "calling function should not contain return-variable of called function: "
                                + trackedVar;
                if (this.contains(trackedVar)) {
                    rebuildState = rebuildState.addInterval(trackedVar, this.getInterval(trackedVar), -1);
                }
            }
        }

        return rebuildState;
    }
    private boolean equalsInterval(Object other) {
        if (this == other) {
            return true;
        }

        if (other instanceof UnifyAnalysisState) {
            UnifyAnalysisState otherElement = (UnifyAnalysisState) other;
            return unifyElements.equals(otherElement.unifyElements);
        }
        return false;
    }
    public String toStringInterval() {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");

        for (Map.Entry<String, NumberInterface> entry : unifyElements.entrySet()) {
            sb.append(String.format("  < %s = %s :: %s >%n", entry.getKey(), entry.getValue(),
                    getReferenceCount(entry.getKey())));
        }

        return sb.append("] size -> ").append(unifyElements.size()).toString();
    }
    private boolean checkPropertyInterval(String pProperty) {// throws InvalidQueryException {
        List<String> parts = propertySplitter.splitToList(pProperty);

        if (parts.size() == 2) {

            // pProperty = value <= varName
            if (CheckTypesOfStringsUtil.isLong(parts.get(0))) {
                long value = Long.parseLong(parts.get(0));
                NumberInterface iv = getInterval(parts.get(1));
                // return (value <= iv.getLow());
                return (value <= iv.getLow().longValue());
            }

            // pProperty = varName <= value
            else if (CheckTypesOfStringsUtil.isLong(parts.get(1))) {
                long value = Long.parseLong(parts.get(1));
                NumberInterface iv = getInterval(parts.get(0));
                // return (iv.getHigh() <= value);
                return (iv.getHigh().longValue() <= value);
            }

            // pProperty = varName1 <= varName2
            else {
                NumberInterface iv1 = getInterval(parts.get(0));
                NumberInterface iv2 = getInterval(parts.get(1));
                return (iv1.contains(iv2));
            }

            // pProperty = value1 <= varName <= value2
        } else if (parts.size() == 3) {
            if (CheckTypesOfStringsUtil.isLong(parts.get(0)) && CheckTypesOfStringsUtil.isLong(parts.get(2))) {
                long value1 = Long.parseLong(parts.get(0));
                long value2 = Long.parseLong(parts.get(2));
                NumberInterface iv = getInterval(parts.get(1));
                // return (value1 <= iv.getLow() && iv.getHigh() <= value2);
                return (value1 <= iv.getLow().longValue() && iv.getHigh().longValue() <= value2);
            }
        }

        return false;
    }
    private String toDOTLabelInterval() {
        StringBuilder sb = new StringBuilder();

        sb.append("{");
        // create a string like: x = [low; high] (refCount)
        for (Entry<String, NumberInterface> entry : unifyElements.entrySet()) {
            sb.append(String.format("%s = %s (%s), ", entry.getKey(), entry.getValue(),
                    getReferenceCount(entry.getKey())));
        }
        sb.append("}");

        return sb.toString();
    }
    private BooleanFormula getFormulaApproximationInterval(FormulaManagerView pMgr) {
        IntegerFormulaManager nfmgr = pMgr.getIntegerFormulaManager();
        List<BooleanFormula> result = new ArrayList<>();
        for (Entry<String, NumberInterface> entry : unifyElements.entrySet()) {
            NumberInterface interval = entry.getValue();
            if (interval.isEmpty()) {
                // one invalid interval disqualifies the whole state
                return pMgr.getBooleanFormulaManager().makeFalse();
            }

            // we assume that everything is an SIGNED INTEGER
            // and build "LOW <= X" and "X <= HIGH"
            // TODO instanceof ...
            NumeralFormula var = nfmgr.makeVariable(entry.getKey());

            if (interval.getLow() instanceof Long) {
                Long low = interval.getLow().longValue();
                Long high = interval.getHigh().longValue();
                // if (low != null && low != Long.MIN_VALUE) { // check for
                // unbound
                if (low != Long.MIN_VALUE) {
                    // interval
                    result.add(pMgr.makeLessOrEqual(nfmgr.makeNumber(low), var, true));
                }
                // if (high != null && high != Long.MIN_VALUE) { // check for
                // unbound
                if (high != Long.MIN_VALUE) {
                    // interval
                    result.add(pMgr.makeGreaterOrEqual(nfmgr.makeNumber(high), var, true));
                }
            } else {
                Double low = interval.getLow().doubleValue();
                Double high = interval.getHigh().doubleValue();
                // if (low != null && low != Double.MIN_VALUE) { // check for
                // unbound
                // interval
                if (low != Double.MIN_VALUE) {
                    result.add(pMgr.makeLessOrEqual(nfmgr.makeNumber(low), var, true));
                }
                // if (high != null && high != Double.MIN_VALUE) { // check for
                // unbound interval
                if (high != Double.MIN_VALUE) {
                    result.add(pMgr.makeGreaterOrEqual(nfmgr.makeNumber(high), var, true));
                }
            }

        }
        return pMgr.getBooleanFormulaManager().and(result);
    }
    public Comparable<?> getPseudoPartitionKeyInterval() {
        // The size alone is not sufficient for pseudo-partitioning, if we want
        // to use object-identity
        // as hashcode. Thus we need a second measurement: the absolute distance
        // of all intervals.
        // -> if the distance is "smaller" than the other state, we know nothing
        // and have to compare the states.
        // -> if the distance is "equal", we can compare by "identity".
        // -> if the distance is "greater", we are "greater" than the other
        // state.
        // We negate the absolute distance to match the
        // "lessEquals"-specifiction.
        // Be aware of overflows! -> we use BigInteger, and zero should be a
        // sound value.
        BigInteger absDistance = BigInteger.ZERO;
        for (NumberInterface i : unifyElements.values()) {
            // TODO doubles???
            long high = i.getHigh() == null ? 0 : i.getHigh().longValue();
            long low = i.getLow() == null ? 0 : i.getLow().longValue();
            Preconditions.checkArgument(low <= high, "LOW greater than HIGH:" + i);
            absDistance = absDistance.add(BigInteger.valueOf(high).subtract(BigInteger.valueOf(low)));
        }
        return new IntervalPseudoPartitionKey(unifyElements.size(), absDistance.negate());
    }
    /** Just a pair of values, can be compared alphabetically. */
    private static final class IntervalPseudoPartitionKey implements Comparable<IntervalPseudoPartitionKey> {

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
            return ComparisonChain.start().compare(size, other.size).compare(absoluteDistance, other.absoluteDistance)
                    .result();
        }
    }

}