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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.core.interfaces.PseudoPartitionable;
import org.sosy_lab.cpachecker.cpa.sign.CreatorSIGN;
import org.sosy_lab.cpachecker.cpa.sign.SIGN;
import org.sosy_lab.cpachecker.cpa.sign.SignTransferRelation;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisInformation;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisInterpolant;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ConstantSymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;
import org.sosy_lab.cpachecker.util.CheckTypesOfStringsUtil;
import org.sosy_lab.cpachecker.util.predicates.smt.BitvectorFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FloatingPointFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.refinement.ForgetfulState;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.FloatingPointFormula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.FormulaType.FloatingPointType;
import org.sosy_lab.java_smt.api.IntegerFormulaManager;
import org.sosy_lab.java_smt.api.NumeralFormula;

public class UnifyAnalysisState
        implements Serializable, LatticeAbstractState<UnifyAnalysisState>, AbstractQueryableState, Graphable,
        FormulaReportingState, PseudoPartitionable, ForgetfulState<ValueAnalysisInformation> {
    private static final long serialVersionUID = -3057330425781141121L;

    private final NumericalType numericalType;
    private static final boolean DEBUG = false;

    private static final Splitter propertySplitter = Splitter.on("<=").trimResults();

    private static final Set<MemoryLocation> blacklist = new HashSet<>();

    public static void addToBlacklist(MemoryLocation var) {
        blacklist.add(checkNotNull(var));
    }

    /**
     * the map that keeps the name of variables and their constant values (concrete
     * and symbolic ones)
     */
    private PersistentMap<MemoryLocation, NumberInterface> unifyElements;

    private final @Nullable MachineModel machineModel;

    private transient PersistentMap<MemoryLocation, Type> memLocToType = PathCopyingPersistentTreeMap.of();

    public final static UnifyAnalysisState TOP = new UnifyAnalysisState(NumericalType.SIGN);
    private final static SerialProxySign proxy = new SerialProxySign();

    /**
     * Constructor for sign/interval
     */
    public UnifyAnalysisState(PersistentMap<MemoryLocation, NumberInterface> pSignMap, NumericalType sign) {
        unifyElements = pSignMap;
        machineModel = null;
        numericalType = sign;
    }

    /**
     * Constructor for sign/interval
     */
    public UnifyAnalysisState(NumericalType sign) {
        unifyElements = PathCopyingPersistentTreeMap.of();
        machineModel = null;
        numericalType = sign;
    }

    /**
     * Constructor for value
     */
    public UnifyAnalysisState(MachineModel pMachineModel) {
        this(checkNotNull(pMachineModel), PathCopyingPersistentTreeMap.of(), PathCopyingPersistentTreeMap.of());
    }

    /**
     * Constructor for value
     */
    public UnifyAnalysisState(Optional<MachineModel> pMachineModel,
            PersistentMap<MemoryLocation, NumberInterface> pConstantsMap,
            PersistentMap<MemoryLocation, Type> pLocToTypeMap) {
        this(pMachineModel.orElse(null), pConstantsMap, pLocToTypeMap);
    }

    /**
     * Constructor for value
     */
    private UnifyAnalysisState(@Nullable MachineModel pMachineModel,
            PersistentMap<MemoryLocation, NumberInterface> pConstantsMap,
            PersistentMap<MemoryLocation, Type> pLocToTypeMap) {
        unifyElements = null;
        // referenceCounts = null;
        machineModel = pMachineModel;
        unifyElements = checkNotNull(pConstantsMap);
        memLocToType = checkNotNull(pLocToTypeMap);
        numericalType = NumericalType.VALUE;
    }

    /**
     * This method assigns a element to the variable and puts it in the map.
     *
     * @param variableName
     *            the location in the memory.
     * @param value
     *            value to be assigned.
     * @param type
     *            the type of <code>value</code>. (can be null)
     *
     */
    public UnifyAnalysisState assignElement(MemoryLocation variableName, NumberInterface value, Type type) {
        switch (numericalType) {
        case SIGN:
        case INTERVAL:
            // check if sigh is ALL
            if (value.isUnbound()) {
                return forgetElement(variableName);
            }
            // only add the element if it is not already present
            return !unifyElements.containsKey(variableName) || !unifyElements.get(variableName).equals(value)
                    ? new UnifyAnalysisState(unifyElements.putAndCopy(variableName, value), numericalType)
                    : this;
        case VALUE:
            memLocToType = memLocToType.putAndCopy(variableName, type);
            NumberInterface valueToAdd = value;
            if (valueToAdd instanceof SymbolicValue) {
                valueToAdd = ((SymbolicValue) valueToAdd).copyForLocation(variableName);
            }
            unifyElements = unifyElements.putAndCopy(variableName, checkNotNull(valueToAdd));
            return this;
        default:
            return this;
        }
    }

    /**
     * This method assigns a concrete value to the given {@link SymbolicIdentifier}.
     *
     * @param pSymbolicIdentifier
     *            the <code>SymbolicIdentifier</code> to assign the concrete value
     *            to.
     * @param pValue
     *            value to be assigned.
     */
    public void assignConstant(SymbolicIdentifier pSymbolicIdentifier, NumberInterface pValue) {
        for (Map.Entry<MemoryLocation, NumberInterface> entry : unifyElements.entrySet()) {
            MemoryLocation currMemloc = entry.getKey();
            NumberInterface currVal = entry.getValue();

            if (currVal instanceof ConstantSymbolicExpression) {
                currVal = ((ConstantSymbolicExpression) currVal).getValue();
            }
            if (currVal instanceof SymbolicIdentifier
                    && ((SymbolicIdentifier) currVal).getId() == pSymbolicIdentifier.getId()) {
                assignElement(currMemloc, pValue, getTypeForMemoryLocation(currMemloc));
            }
        }
    }

    /**
     * This method removes a memory location from the underlying map and returns the
     * removed value.
     *
     * @param pMemoryLocation
     *            the name of the memory location to remove
     * @return the value of the removed memory location
     */
    @Override
    public ValueAnalysisInformation forget(MemoryLocation pMemoryLocation) {

        if (!unifyElements.containsKey(pMemoryLocation)) {
            return ValueAnalysisInformation.EMPTY;
        }

        NumberInterface value = unifyElements.get(pMemoryLocation);
        Type type = memLocToType.get(pMemoryLocation);
        unifyElements = unifyElements.removeAndCopy(pMemoryLocation);
        memLocToType = memLocToType.removeAndCopy(pMemoryLocation);

        PersistentMap<MemoryLocation, Type> typeAssignment = PathCopyingPersistentTreeMap.of();
        if (type != null) {
            typeAssignment = typeAssignment.putAndCopy(pMemoryLocation, type);
        }
        PersistentMap<MemoryLocation, NumberInterface> valueAssignment = PathCopyingPersistentTreeMap.of();
        valueAssignment = valueAssignment.putAndCopy(pMemoryLocation, value);

        return new ValueAnalysisInformation(valueAssignment, typeAssignment);
    }

    /**
     * This method removes the interval or sign (later value) for a given variable.
     *
     * @param variableName
     *            the name of the variable whose interval should be removed
     * @return this
     */
    // see ExplicitState::forget
    public UnifyAnalysisState forgetElement(MemoryLocation variableName) {
        return unifyElements.containsKey(variableName)
                ? new UnifyAnalysisState(unifyElements.removeAndCopy(variableName), numericalType)
                : this;
    }

    /**
     * This method drops all entries belonging to the stack frame of a function.
     * This method should be called right before leaving a function.
     *
     * @param functionName
     *            the name of the function that is about to be left
     */
    public UnifyAnalysisState dropFrame(String functionName) {
        UnifyAnalysisState temp = this;
        for (MemoryLocation variableName : unifyElements.keySet()) {
            if (variableName.isOnFunctionStack(functionName)) {
                switch (numericalType) {
                case INTERVAL:
                case SIGN:
                    temp  = temp.forgetElement(variableName);
                    break;
                default:
                    temp.forget(variableName);
                    break;
                }
            }
        }
        return temp;
    }

    /**
     * This method returns the element for the given variable.
     *
     * @param variableName
     *            the name of the variable for which to get the value
     * @throws NullPointerException
     *             - if no value is present in this state for the given variable
     * @return the value associated with the given variable
     */
    public NumberInterface getElement(MemoryLocation variableName) {
        NumberInterface value = unifyElements.get(variableName);
        switch (numericalType) {
        case SIGN:
            return value == null ? new CreatorSIGN().factoryMethod(7) : value;
        case INTERVAL:
            return value == null ? new CreatorIntegerInterval().factoryMethod(null) : value;
        default:
            return checkNotNull(value);
        }
    }

    @Override
    public void remember(final MemoryLocation pLocation, final ValueAnalysisInformation pValueAndType) {
        final NumberInterface value = pValueAndType.getAssignments().get(pLocation);
        final Type valueType = pValueAndType.getLocationTypes().get(pLocation);

        assignElement(pLocation, value, valueType);
    }
    @Override
    public BooleanFormula getFormulaApproximation(FormulaManagerView pManager) {
        List<BooleanFormula> result = new ArrayList<>();
        switch (numericalType) {
        case INTERVAL:
            IntegerFormulaManager nfmgr = pManager.getIntegerFormulaManager();
            for (Entry<MemoryLocation, NumberInterface> entry : unifyElements.entrySet()) {
                NumberInterface interval = entry.getValue();
                if (interval.isEmpty()) {
                    // one invalid interval disqualifies the whole state
                    return pManager.getBooleanFormulaManager().makeFalse();
                }
                // we assume that everything is an SIGNED INTEGER
                // and build "LOW <= X" and "X <= HIGH"
                // TODO instanceof ...
                NumeralFormula var = nfmgr.makeVariable(entry.getKey().getAsSimpleString());
                if (interval.getLow() instanceof Long) {
                    Long low = interval.getLow().longValue();
                    Long high = interval.getHigh().longValue();
                    // if (low != null && low != Long.MIN_VALUE) { // check for
                    // unbound
                    if (low != Long.MIN_VALUE) {
                        // interval
                        result.add(pManager.makeLessOrEqual(nfmgr.makeNumber(low), var, true));
                    }
                    // if (high != null && high != Long.MIN_VALUE) { // check for
                    // unbound
                    if (high != Long.MIN_VALUE) {
                        // interval
                        result.add(pManager.makeGreaterOrEqual(nfmgr.makeNumber(high), var, true));
                    }
                } else {
                    Double low = interval.getLow().doubleValue();
                    Double high = interval.getHigh().doubleValue();
                    // if (low != null && low != Double.MIN_VALUE) { // check for
                    // unbound
                    // interval
                    if (low != Double.MIN_VALUE) {
                        result.add(pManager.makeLessOrEqual(nfmgr.makeNumber(low), var, true));
                    }
                    // if (high != null && high != Double.MIN_VALUE) { // check for
                    // unbound interval
                    if (high != Double.MIN_VALUE) {
                        result.add(pManager.makeGreaterOrEqual(nfmgr.makeNumber(high), var, true));
                    }
                }
            }
            return pManager.getBooleanFormulaManager().and(result);
        case VALUE:
            BooleanFormulaManager bfmgr = pManager.getBooleanFormulaManager();
            if (machineModel == null) {
                return bfmgr.makeTrue();
            }
            BitvectorFormulaManagerView bitvectorFMGR = pManager.getBitvectorFormulaManager();
            FloatingPointFormulaManagerView floatFMGR = pManager.getFloatingPointFormulaManager();

            for (Map.Entry<MemoryLocation, NumberInterface> entry : unifyElements.entrySet()) {
                NumericValue num = entry.getValue().asNumericValue();

                if (num != null) {
                    MemoryLocation memoryLocation = entry.getKey();
                    Type type = getTypeForMemoryLocation(memoryLocation);
                    if (!memoryLocation.isReference() && type instanceof CSimpleType) {
                        CSimpleType simpleType = (CSimpleType) type;
                        if (simpleType.getType().isIntegerType()) {
                            int bitSize = machineModel.getSizeof(simpleType) * machineModel.getSizeofCharInBits();
                            BitvectorFormula var = bitvectorFMGR.makeVariable(bitSize,
                                    entry.getKey().getAsSimpleString());

                            Number value = num.getNumber();
                            final BitvectorFormula val;
                            if (value instanceof BigInteger) {
                                val = bitvectorFMGR.makeBitvector(bitSize, (BigInteger) value);
                            } else {
                                val = bitvectorFMGR.makeBitvector(bitSize, num.longValue());
                            }
                            result.add(bitvectorFMGR.equal(var, val));
                        } else if (simpleType.getType().isFloatingPointType()) {
                            final FloatingPointType fpType;
                            switch (simpleType.getType()) {
                            case FLOAT:
                                fpType = FormulaType.getSinglePrecisionFloatingPointType();
                                break;
                            case DOUBLE:
                                fpType = FormulaType.getDoublePrecisionFloatingPointType();
                                break;
                            default:
                                throw new AssertionError("Unsupported floating point type: " + simpleType);
                            }
                            FloatingPointFormula var = floatFMGR.makeVariable(entry.getKey().getAsSimpleString(),
                                    fpType);
                            FloatingPointFormula val = floatFMGR.makeNumber(num.doubleValue(), fpType);
                            result.add(floatFMGR.equalWithFPSemantics(var, val));
                        } else {
                            // ignore in formula-approximation
                        }
                    } else {
                        // ignore in formula-approximation
                    }
                } else {
                    // ignore in formula-approximation
                }
            }
            return bfmgr.and(result);
        default:
            return null;
        }
    }
    @Override
    @Nullable
    public Comparable<?> getPseudoPartitionKey() {
        switch (numericalType) {
        case INTERVAL:
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
                long high = i.getHigh() == null ? 0 : i.getHigh().longValue();
                long low = i.getLow() == null ? 0 : i.getLow().longValue();
                Preconditions.checkArgument(low <= high, "LOW greater than HIGH:" + i);
                absDistance = absDistance.add(BigInteger.valueOf(high).subtract(BigInteger.valueOf(low)));
            }
            return new IntervalPseudoPartitionKey(unifyElements.size(), absDistance.negate());
        case VALUE:
            return getSize();
        default:
            return null;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        String delim = ", ";
        String loopDelim = "";
        if (numericalType.equals(NumericalType.INTERVAL)) {
            sb.append("[\n");
        } else {
            sb.append("[");
        }
        for (Map.Entry<MemoryLocation, NumberInterface> entry : unifyElements.entrySet()) {
            MemoryLocation key = entry.getKey();
            // this if statement for sigh only
            if (numericalType.equals(NumericalType.SIGN)) {
                if (!DEBUG && (key.getAsSimpleString().matches("\\w*::__CPAchecker_TMP_\\w*")
                        || key.getAsSimpleString().endsWith(SignTransferRelation.FUNC_RET_VAR))) {
                    continue;
                }
                sb.append(loopDelim);
                sb.append(key.getAsSimpleString() + "->" + getElement(key));
                loopDelim = delim;
            }
            sb.append(" <");
            sb.append(key.getAsSimpleString());
            sb.append(" = ");
            sb.append(entry.getValue());
            sb.append(">\n");
        }
        if (numericalType.equals(NumericalType.SIGN)) {
            sb.append("]");
            return sb.toString();
        }
        return sb.append("] size -> ").append(unifyElements.size()).toString();

    }
    /**
     * This method checks whether or not the given Memory Location is contained in
     * this state.
     *
     * @param pMemoryLocation
     *            the location in the Memory to check for
     * @return true, if the variable is contained, else false
     */
    public boolean contains(MemoryLocation pMemoryLocation) {
        return unifyElements.containsKey(pMemoryLocation);
    }
    @Override
    public Set<MemoryLocation> getTrackedMemoryLocations() {
        // no copy necessary, set is immutable
        return unifyElements.keySet();
    }
    /**
     * This method determines the total number of variables contained in this state.
     *
     * @return the total number of variables contained in this state
     */
    @Override
    public int getSize() {
        return unifyElements.size();
    }
    @Override
    public String toDOTLabel() {
        StringBuilder sb = new StringBuilder();

        sb.append("{");
        Joiner.on(", ").withKeyValueSeparator(" = ").appendTo(sb, unifyElements);
        sb.append("}");
        return sb.toString();
    }
    @Override
    public String getCPAName() {
        switch (numericalType) {
        case SIGN:
            return "SignAnalysis";
        case INTERVAL:
            return "IntervalAnalysis";
        case VALUE:
            return "ValueAnalysis";
        default:
            return null;
        }
    }
    @Override
    public boolean shouldBeHighlighted() {
        return false;
    }

    /**
     * This element joins this element with another element.
     *
     * @param reachedState
     *            the other element to join with this element
     * @return a new state representing the join of this element and the other
     *         element
     */
    @Override
    public UnifyAnalysisState join(UnifyAnalysisState pOther) throws CPAException, InterruptedException {
        switch (numericalType) {
        case SIGN:
            return stateJoinSign(pOther);
        case INTERVAL:
            return stateJoinInterval(pOther);
        case VALUE:
            return stateJoinValue(pOther);
        default:
            return null;
        }
    }
    private UnifyAnalysisState stateJoinSign(UnifyAnalysisState pToJoin) {
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
        PersistentMap<MemoryLocation, NumberInterface> newMap = PathCopyingPersistentTreeMap.of();
        NumberInterface combined;
        for (MemoryLocation varIdent : pToJoin.unifyElements.keySet()) {
            // only add those variables that are contained in both states (otherwise one has
            // value ALL (not saved))
            if (unifyElements.containsKey(varIdent)) {
                combined = getElement(varIdent).combineWith(pToJoin.getElement(varIdent));
                if (!combined.isUnbound()) {
                    newMap = newMap.putAndCopy(varIdent, combined);
                }
            }
        }
        return newMap.size() > 0 ? new UnifyAnalysisState(newMap, NumericalType.SIGN) : result;
    }

    private UnifyAnalysisState stateJoinInterval(UnifyAnalysisState reachedState) {
        boolean changed = false;
        PersistentMap<MemoryLocation, NumberInterface> newIntervals = PathCopyingPersistentTreeMap.of();

        for (MemoryLocation variableName : reachedState.unifyElements.keySet()) {
            // Integer otherRefCount = reachedState.getReferenceCount(variableName);
            NumberInterface otherInterval = reachedState.getElement(variableName);
            if (unifyElements.containsKey(variableName)) {
                // update the interval
                NumberInterface mergedInterval = getElement(variableName).union(otherInterval);
                if (mergedInterval != otherInterval) {
                    changed = true;
                }
                if (!mergedInterval.isUnbound()) {
                    newIntervals = newIntervals.putAndCopy(variableName, mergedInterval);
                }
            }
        }

        if (changed) {
            return new UnifyAnalysisState(newIntervals, NumericalType.INTERVAL);
        } else {
            return reachedState;
        }
    }

    public UnifyAnalysisState stateJoinValue(UnifyAnalysisState reachedState) {
        PersistentMap<MemoryLocation, NumberInterface> newConstantsMap = PathCopyingPersistentTreeMap.of();
        PersistentMap<MemoryLocation, Type> newlocToTypeMap = PathCopyingPersistentTreeMap.of();

        for (Map.Entry<MemoryLocation, NumberInterface> otherEntry : reachedState.unifyElements.entrySet()) {
            MemoryLocation key = otherEntry.getKey();

            if (Objects.equals(otherEntry.getValue(), unifyElements.get(key))) {
                newConstantsMap = newConstantsMap.putAndCopy(key, otherEntry.getValue());
                newlocToTypeMap = newlocToTypeMap.putAndCopy(key, memLocToType.get(key));
            }
        }

        // return the reached state if both maps are equal
        if (newConstantsMap.size() == reachedState.unifyElements.size()) {
            return reachedState;
        } else {
            return new UnifyAnalysisState(machineModel, newConstantsMap, newlocToTypeMap);
        }
    }













    @Override
    @Nullable
    public Object getPseudoHashCode() {
        return this;
    }



    @Override
    public int hashCode() {
        switch (numericalType) {
        case SIGN:
        case INTERVAL:
            return unifyElements.hashCode();
        case VALUE:
            return unifyElements.hashCode();
        default:
            return -1;
        }

    }

    @Override
    public boolean isLessOrEqual(UnifyAnalysisState pOther) {// throws CPAException, InterruptedException {
        switch (numericalType) {
        case SIGN:
            return IsLessOrEqualSign(pOther);
        case INTERVAL:
            return intervalIsLessOrEqual(pOther);
        case VALUE:
            return valueIsLessOrEqual(pOther);
        default:
            return false;
        }
    }

    @Override
    public boolean equals(Object pObj) {
        switch (numericalType) {
        case SIGN:
            return equalsSing(pObj);
        case INTERVAL:
            return equalsInterval(pObj);
        case VALUE:
            return equalsValue(pObj);
        default:
            return false;
        }
    }

    @Override
    public boolean checkProperty(String pProperty) throws InvalidQueryException {
        switch (numericalType) {
        case SIGN:
            return checkPropertySign(pProperty);
        case INTERVAL:
            return checkPropertyInterval(pProperty);
        case VALUE:
            return checkPropertyValue(pProperty);
        default:
            return false;
        }
    }



    private boolean IsLessOrEqualSign(UnifyAnalysisState pSuperset) {
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
        for (MemoryLocation varIdent : pSuperset.unifyElements.keySet()) {
            if (!getElement(varIdent).isSubsetOf(pSuperset.getElement(varIdent))) {
                return false;
            }
        }
        return true;
    }



    private static class SerialProxySign implements Serializable {

        private static final long serialVersionUID = 2843708585446089623L;

        public SerialProxySign() {
        }

        private Object readResolve() {
            return TOP;
        }
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
    public UnifyAnalysisState enterFunction(ImmutableMap<MemoryLocation, NumberInterface> pArguments) {
        PersistentMap<MemoryLocation, NumberInterface> newMap = unifyElements;
        for (MemoryLocation var : pArguments.keySet()) {
            if (!pArguments.get(var).equals(SIGN.ALL)) {
                newMap = newMap.putAndCopy(var, pArguments.get(var));
            }
        }
        return unifyElements == newMap ? this : new UnifyAnalysisState(newMap, NumericalType.SIGN);
    }

    private boolean checkPropertySign(String pProperty) {
        List<String> parts = propertySplitter.splitToList(pProperty);

        if (parts.size() == 2) {

            // pProperty = value <= varName
            if (CheckTypesOfStringsUtil.isSIGN(parts.get(0))) {
                NumberInterface value = SIGN.valueOf(parts.get(0));
                NumberInterface varName = getElement(MemoryLocation.valueOf(parts.get(1)));
                return (varName.covers(value));
            }

            // pProperty = varName <= value
            else if (CheckTypesOfStringsUtil.isSIGN(parts.get(1))) {
                NumberInterface varName = getElement(MemoryLocation.valueOf(parts.get(0)));
                NumberInterface value = SIGN.valueOf(parts.get(1));
                return (value.covers(varName));
            }

            // pProperty = varName1 <= varName2
            else {
                NumberInterface varName1 = getElement(MemoryLocation.valueOf(parts.get(0)));
                NumberInterface varName2 = getElement(MemoryLocation.valueOf(parts.get(1)));
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
    public Map<MemoryLocation, NumberInterface> getSignMapView() {
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
     * This method decides if this element is less or equal than the reached state,
     * based on the order imposed by the lattice.
     *
     * @param reachedState
     *            the reached state
     * @return true, if this element is less or equal than the reached state, based
     *         on the order imposed by the lattice
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
        for (MemoryLocation key : reachedState.unifyElements.keySet()) {
            if (!unifyElements.containsKey(key) || !reachedState.getElement(key).contains(getElement(key))) {
                return false;
            }
        }
        // else, this element < reached state on the lattice
        return true;
    }

    /**
     * @return the set of tracked variables by this state
     */
    public Map<MemoryLocation, NumberInterface> getIntervalMap() {
        return unifyElements;
    }

    /**
     * @return the set of tracked variables by this state
     */
    public List<String> getVariables() {
        List<String> temp = new ArrayList<>();
        for (MemoryLocation ml : getIntervalMap().keySet()) {
            temp.add(ml.getAsSimpleString());
        }
        return temp;
    }

    /**
     * If there was a recursive function, we have wrong intervals/values for scoped
     * variables in the returnState. This function rebuilds a new state with the
     * correct intervals from the previous callState. We delete the wrong intervals
     * and insert new intervals, if necessary.
     */
    public UnifyAnalysisState rebuildStateAfterFunctionCall(final UnifyAnalysisState callState,
            final FunctionExitNode functionExit) {
        switch (numericalType) {
        case INTERVAL:
            return rebuildStateAfterFunctionCallInterval(callState, functionExit);
        case VALUE:
            return rebuildStateAfterFunctionCallValue(callState, functionExit);
        default:
            return null;
        }
    }

    public UnifyAnalysisState rebuildStateAfterFunctionCallInterval(final UnifyAnalysisState callState,
            final FunctionExitNode functionExit) {

        // we build a new state from:
        // - local variables from callState,
        // - global variables from THIS,
        // - the local return variable from THIS.
        // we copy callState and override all global values and the return
        // variable.

        UnifyAnalysisState rebuildState = callState;

        // first forget all global information
        for (final MemoryLocation key : callState.unifyElements.keySet()) {
            if (!key.getAsSimpleString().contains("::")) { // global -> delete
                rebuildState = rebuildState.forgetElement(key);
            }
        }
        // second: learn new information
        for (final MemoryLocation trackedVar : this.unifyElements.keySet()) {

            if (!trackedVar.getAsSimpleString().contains("::")) { // global -> override deleted
                // value
                rebuildState = rebuildState.assignElement(trackedVar, this.getElement(trackedVar), null);
                rebuildState = rebuildState.assignElement(trackedVar, this.getElement(trackedVar), null);
            } else if (functionExit.getEntryNode().getReturnVariable().isPresent() && functionExit.getEntryNode()
                    .getReturnVariable().get().getQualifiedName().equals(trackedVar.getAsSimpleString())) {
                assert (!rebuildState.contains(
                        trackedVar)) : "calling function should not contain return-variable of called function: "
                                + trackedVar;
                if (this.contains(trackedVar)) {
                    rebuildState = rebuildState.assignElement(trackedVar, this.getElement(trackedVar), null);
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
        for (Map.Entry<MemoryLocation, NumberInterface> entry : unifyElements.entrySet()) {
            sb.append(String.format("  < %s = %s >%n", entry.getKey().getAsSimpleString(), entry.getValue()));
        }

        return sb.append("] size -> ").append(unifyElements.size()).toString();
    }

    private boolean checkPropertyInterval(String pProperty) {// throws InvalidQueryException {
        List<String> parts = propertySplitter.splitToList(pProperty);

        if (parts.size() == 2) {

            // pProperty = value <= varName
            if (CheckTypesOfStringsUtil.isLong(parts.get(0))) {
                long value = Long.parseLong(parts.get(0));
                NumberInterface iv = getElement(MemoryLocation.valueOf(parts.get(1)));
                // return (value <= iv.getLow());
                return (value <= iv.getLow().longValue());
            }

            // pProperty = varName <= value
            else if (CheckTypesOfStringsUtil.isLong(parts.get(1))) {
                long value = Long.parseLong(parts.get(1));
                NumberInterface iv = getElement(MemoryLocation.valueOf(parts.get(0)));
                // return (iv.getHigh() <= value);
                return (iv.getHigh().longValue() <= value);
            }

            // pProperty = varName1 <= varName2
            else {
                NumberInterface iv1 = getElement(MemoryLocation.valueOf(parts.get(0)));
                NumberInterface iv2 = getElement(MemoryLocation.valueOf(parts.get(1)));
                return (iv1.contains(iv2));
            }

            // pProperty = value1 <= varName <= value2
        } else if (parts.size() == 3) {
            if (CheckTypesOfStringsUtil.isLong(parts.get(0)) && CheckTypesOfStringsUtil.isLong(parts.get(2))) {
                long value1 = Long.parseLong(parts.get(0));
                long value2 = Long.parseLong(parts.get(2));
                NumberInterface iv = getElement(MemoryLocation.valueOf(parts.get(1)));
                // return (value1 <= iv.getLow() && iv.getHigh() <= value2);
                return (value1 <= iv.getLow().longValue() && iv.getHigh().longValue() <= value2);
            }
        }

        return false;
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

    public static UnifyAnalysisState copyOf(UnifyAnalysisState state) {
        return new UnifyAnalysisState(state.machineModel, state.unifyElements, state.memLocToType);
    }

    /**
     * This method retains all variables and their respective values in the
     * underlying map, while removing all others.
     *
     * @param toRetain
     *            the names of the variables to retain
     */
    public void retainAll(Set<MemoryLocation> toRetain) {
        Set<MemoryLocation> toRemove = new HashSet<>();
        for (MemoryLocation memoryLocation : unifyElements.keySet()) {
            if (!toRetain.contains(memoryLocation)) {
                toRemove.add(memoryLocation);
            }
        }

        for (MemoryLocation memoryLocation : toRemove) {
            forget(memoryLocation);
        }
    }

    /**
     * This method returns the type for the given memory location.
     *
     * @param loc
     *            the memory location for which to get the type
     * @throws NullPointerException
     *             - if no type is present in this state for the given memory
     *             location
     * @return the type associated with the given memory location
     */
    public Type getTypeForMemoryLocation(MemoryLocation loc) {
        return memLocToType.get(loc);
    }



    /**
     * This method determines the number of global variables contained in this
     * state.
     *
     * @return the number of global variables contained in this state
     */
    public int getNumberOfGlobalVariables() {
        int numberOfGlobalVariables = 0;

        for (MemoryLocation variableName : unifyElements.keySet()) {
            if (!variableName.isOnFunctionStack()) {
                numberOfGlobalVariables++;
            }
        }

        return numberOfGlobalVariables;
    }



    /**
     * This method decides if this element is less or equal than the other element,
     * based on the order imposed by the lattice.
     *
     * @param other
     *            the other element
     * @return true, if this element is less or equal than the other element, based
     *         on the order imposed by the lattice
     */
    public boolean valueIsLessOrEqual(UnifyAnalysisState other) {

        // also, this element is not less or equal than the other element, if it
        // contains less elements
        if (unifyElements.size() < other.unifyElements.size()) {
            return false;
        }

        // also, this element is not less or equal than the other element,
        // if any one constant's value of the other element differs from the constant's
        // value in this
        // element
        for (Map.Entry<MemoryLocation, NumberInterface> otherEntry : other.unifyElements.entrySet()) {
            MemoryLocation key = otherEntry.getKey();
            NumberInterface otherValue = otherEntry.getValue();
            NumberInterface thisValue = unifyElements.get(key);

            if (!otherValue.equals(thisValue)) {
                return false;
            }
        }
        return true;
    }

    public boolean equalsValue(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }
        if (!getClass().equals(other.getClass())) {
            return false;
        }
        UnifyAnalysisState otherElement = (UnifyAnalysisState) other;
        return otherElement.unifyElements.equals(unifyElements)
                && Objects.equals(memLocToType, otherElement.memLocToType);
    }



    @Override
    public Object evaluateProperty(String pProperty) throws InvalidQueryException {
        pProperty = pProperty.trim();

        if (pProperty.startsWith("contains(")) {
            String varName = pProperty.substring("contains(".length(), pProperty.length() - 1);
            return this.unifyElements.containsKey(MemoryLocation.valueOf(varName));
        } else {
            String[] parts = pProperty.split("==");
            if (parts.length != 2) {
                NumberInterface value = this.unifyElements.get(MemoryLocation.valueOf(pProperty));
                if (value != null && value.isExplicitlyKnown()) {
                    return value;
                } else {
                    throw new InvalidQueryException("The Query \"" + pProperty
                            + "\" is invalid. Could not find the variable \"" + pProperty + "\"");
                }
            } else {
                return checkProperty(pProperty);
            }
        }
    }

    public boolean checkPropertyValue(String pProperty) throws InvalidQueryException {
        // e.g. "x==5" where x is a variable. Returns if 5 is the associated constant
        String[] parts = pProperty.split("==");

        if (parts.length != 2) {
            throw new InvalidQueryException(
                    "The Query \"" + pProperty + "\" is invalid. Could not split the property string correctly.");
        } else {
            // The following is a hack
            NumberInterface val = this.unifyElements.get(MemoryLocation.valueOf(parts[0]));
            if (val == null) {
                return false;
            }
            Long value = val.asLong(CNumericTypes.INT);

            if (value == null) {
                return false;
            } else {
                try {
                    return value == Long.parseLong(parts[1]);
                } catch (NumberFormatException e) {
                    // The command might contains something like "main::p==cmd" where the user wants
                    // to compare the variable p to the variable cmd (nearest in scope)
                    // perhaps we should omit the "main::" and find the variable via static scoping
                    // ("main::p" is also not intuitive for a user)
                    // TODO: implement Variable finding via static scoping
                    throw new InvalidQueryException("The Query \"" + pProperty
                            + "\" is invalid. Could not parse the long \"" + parts[1] + "\"");
                }
            }
        }
    }

    private static boolean startsWithIgnoreCase(String s, String prefix) {
        s = s.substring(0, prefix.length());
        return s.equalsIgnoreCase(prefix);
    }

    @Override
    public void modifyProperty(String pModification) throws InvalidQueryException {
        Preconditions.checkNotNull(pModification);

        // either "deletevalues(methodname::varname)" or
        // "setvalue(methodname::varname:=1929)"
        String[] statements = pModification.split(";");
        for (String statement : statements) {
            statement = statement.trim();
            if (startsWithIgnoreCase(statement, "deletevalues(")) {
                if (!statement.endsWith(")")) {
                    throw new InvalidQueryException(statement + " should end with \")\"");
                }

                MemoryLocation varName = MemoryLocation
                        .valueOf(statement.substring("deletevalues(".length(), statement.length() - 1));

                if (contains(varName)) {
                    forget(varName);
                } else {
                    // varname was not present in one of the maps
                    // i would like to log an error here, but no logger is available
                }

            } else if (startsWithIgnoreCase(statement, "setvalue(")) {
                if (!statement.endsWith(")")) {
                    throw new InvalidQueryException(statement + " should end with \")\"");
                }

                String assignment = statement.substring("setvalue(".length(), statement.length() - 1);
                String[] assignmentParts = assignment.split(":=");

                if (assignmentParts.length != 2) {
                    throw new InvalidQueryException("The Query \"" + pModification
                            + "\" is invalid. Could not split the property string correctly.");
                } else {
                    String varName = assignmentParts[0].trim();
                    try {
                        NumberInterface newValue = new NumericValue(Long.parseLong(assignmentParts[1].trim()));
                        this.assignElement(MemoryLocation.valueOf(varName), newValue, null);
                    } catch (NumberFormatException e) {
                        throw new InvalidQueryException("The Query \"" + pModification
                                + "\" is invalid. Could not parse the long \"" + assignmentParts[1].trim() + "\"");
                    }
                }
            }
        }
    }

    /**
     * This method determines the set of variable names that are in the other state
     * but not in this, or that are in both, but differ in their value.
     *
     * @param other
     *            the other state for which to get the difference
     * @return the set of variable names that differ
     */
    public Set<MemoryLocation> getDifference(UnifyAnalysisState other) {
        Set<MemoryLocation> difference = new HashSet<>();

        for (MemoryLocation variableName : other.unifyElements.keySet()) {
            if (!contains(variableName)) {
                difference.add(variableName);

            } else if (!getElement(variableName).equals(other.getElement(variableName))) {
                difference.add(variableName);
            }
        }

        return difference;
    }

    /**
     * This method adds the key-value-pairs of this state to the given value mapping
     * and returns the new mapping.
     *
     * @param valueMapping
     *            the mapping from variable name to the set of values of this
     *            variable
     * @return the new mapping
     */
    public Multimap<String, NumberInterface> addToValueMapping(Multimap<String, NumberInterface> valueMapping) {
        for (Map.Entry<MemoryLocation, NumberInterface> entry : unifyElements.entrySet()) {
            valueMapping.put(entry.getKey().getAsSimpleString(), entry.getValue());
        }

        return valueMapping;
    }

    /**
     * This method returns the set of tracked variables by this state.
     *
     * @return the set of tracked variables by this state
     */
    public Set<String> getTrackedVariableNames() {
        Set<String> result = new HashSet<>();

        for (MemoryLocation loc : unifyElements.keySet()) {
            result.add(loc.getAsSimpleString());
        }

        // no copy necessary, fresh instance of set
        return Collections.unmodifiableSet(result);
    }

    public Map<MemoryLocation, NumberInterface> getConstantsMapView() {
        return Collections.unmodifiableMap(unifyElements);
    }

    /**
     * This method acts as factory to create a value-analysis interpolant from this
     * value-analysis state.
     *
     * @return the value-analysis interpolant reflecting the value assignment of
     *         this state
     */
    public ValueAnalysisInterpolant createInterpolant() {
        return new ValueAnalysisInterpolant(new HashMap<>(unifyElements), new HashMap<>(memLocToType));
    }

    public ValueAnalysisInformation getInformation() {
        return new ValueAnalysisInformation(unifyElements, memLocToType);
    }

    public Set<MemoryLocation> getMemoryLocationsOnStack(String pFunctionName) {
        Set<MemoryLocation> result = new HashSet<>();

        Set<MemoryLocation> memoryLocations = unifyElements.keySet();

        for (MemoryLocation memoryLocation : memoryLocations) {
            if (memoryLocation.isOnFunctionStack() && memoryLocation.getFunctionName().equals(pFunctionName)) {
                result.add(memoryLocation);
            }
        }

        // Doesn't need a copy, Memory Location is Immutable
        return Collections.unmodifiableSet(result);
    }

    public Set<MemoryLocation> getGlobalMemoryLocations() {
        Set<MemoryLocation> result = new HashSet<>();

        Set<MemoryLocation> memoryLocations = unifyElements.keySet();

        for (MemoryLocation memoryLocation : memoryLocations) {
            if (!memoryLocation.isOnFunctionStack()) {
                result.add(memoryLocation);
            }
        }

        // Doesn't need a copy, Memory Location is Immutable
        return Collections.unmodifiableSet(result);
    }

    public void forgetValuesWithIdentifier(String pIdentifier) {
        for (MemoryLocation memoryLocation : unifyElements.keySet()) {
            if (memoryLocation.getAsSimpleString().equals(pIdentifier)) {
                unifyElements = unifyElements.removeAndCopy(memoryLocation);
                memLocToType = memLocToType.removeAndCopy(memoryLocation);
            }
        }
    }

    public UnifyAnalysisState rebuildStateAfterFunctionCallValue(final UnifyAnalysisState callState,
            final FunctionExitNode functionExit) {

        // we build a new state from:
        // - local variables from callState,
        // - global variables from THIS,
        // - the local return variable from THIS.
        // we copy callState and override all global values and the return variable.

        final UnifyAnalysisState rebuildState = UnifyAnalysisState.copyOf(callState);

        // first forget all global information
        for (final MemoryLocation trackedVar : callState.getTrackedMemoryLocations()) {
            if (!trackedVar.isOnFunctionStack()) { // global -> delete
                rebuildState.forget(trackedVar);
            }
        }

        // second: learn new information
        for (final MemoryLocation trackedVar : this.getTrackedMemoryLocations()) {

            if (!trackedVar.isOnFunctionStack()) { // global -> override deleted value
                rebuildState.assignElement(trackedVar, this.getElement(trackedVar),
                        this.getTypeForMemoryLocation(trackedVar));

            } else if (functionExit.getEntryNode().getReturnVariable().isPresent() && functionExit.getEntryNode()
                    .getReturnVariable().get().getQualifiedName().equals(trackedVar.getAsSimpleString())) {
                /*
                 * assert (!rebuildState.contains(trackedVar)) :
                 * "calling function should not contain return-variable of called function: " +
                 * trackedVar;
                 */
                if (this.contains(trackedVar)) {
                    rebuildState.assignElement(trackedVar, this.getElement(trackedVar),
                            this.getTypeForMemoryLocation(trackedVar));
                }
            }
        }

        return rebuildState;
    }

    private void readObject(ObjectInputStream in) throws IOException {
        try {
            in.defaultReadObject();
        } catch (ClassNotFoundException e) {
            throw new IOException("", e);
        }
        memLocToType = PathCopyingPersistentTreeMap.of();
    }
}