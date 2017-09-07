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

import com.google.common.base.Splitter;
import java.io.Serializable;
import java.util.Set;
import javax.annotation.Nullable;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.core.interfaces.PseudoPartitionable;
import org.sosy_lab.cpachecker.cpa.sign.SIGN;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.refinement.ForgetfulState;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class UnifyAnalysisState
        implements Serializable, LatticeAbstractState<UnifyAnalysisState>, AbstractQueryableState, Graphable,
        FormulaReportingState, PseudoPartitionable, ForgetfulState<UnifyAnalysisState> {
    private static final long serialVersionUID = -3057330425781141121L;

    private final NumericalType numericalType;
    private static final boolean DEBUG = false;

    private static final Splitter propertySplitter = Splitter.on("<=").trimResults();

    // TODO change to elementsMap
    private PersistentMap<String, NumberInterface> signMap;

    public final static UnifyAnalysisState TOP = new UnifyAnalysisState();
    private final static SerialProxySign proxy = new SerialProxySign();

    private UnifyAnalysisState(PersistentMap<String, NumberInterface> pSignMap) {
        numericalType = NumericalType.SIGN;
        signMap = pSignMap;
    }

    private UnifyAnalysisState() {
        signMap = PathCopyingPersistentTreeMap.of();
        numericalType = NumericalType.SIGN;
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
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String toDOTLabel() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean shouldBeHighlighted() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getCPAName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UnifyAnalysisState join(UnifyAnalysisState pOther) throws CPAException, InterruptedException {
        switch (numericalType) {
        case SIGN:
            return signStateJoin(pOther);
        default:
            break;
        }
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isLessOrEqual(UnifyAnalysisState pOther) {// throws CPAException, InterruptedException {
        switch (numericalType) {
        case SIGN:
            return signIsLessOrEqual(pOther);
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
        if (signMap.size() < pSuperset.signMap.size()) {
            return false;
        }
        // is subset if for every variable all sign assumptions are considered in
        // pSuperset
        // check that all variables in superset with SIGN != ALL have no bigger
        // assumptions in subset
        for (String varIdent : pSuperset.signMap.keySet()) {
            if (!getSignForVariable(varIdent).isSubsetOf(pSuperset.getSignForVariable(varIdent))) {
                return false;
            }
        }
        return true;
    }

    public NumberInterface getSignForVariable(String pVarIdent) {
        return signMap.containsKey(pVarIdent) ? signMap.get(pVarIdent) : SIGN.ALL;
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
        for (String varIdent : pToJoin.signMap.keySet()) {
            // only add those variables that are contained in both states (otherwise one has
            // value ALL (not saved))
            if (signMap.containsKey(varIdent)) {
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

}