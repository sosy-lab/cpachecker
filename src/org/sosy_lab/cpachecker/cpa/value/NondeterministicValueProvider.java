// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value;

import java.util.ArrayList;
import java.util.List;

import org.sosy_lab.cpachecker.cpa.value.type.Value;

/**
 * This provider is instantiated by {@link ValueAnalysisTransferRelation} and
 * is then provided to the {@link ExpressionValueVisitor}. It can contain a List
 * of values to be used by the same to assign to an already known Value instead
 * of performing unknown value handling (e.g. to assign a value to a
 * VERIFIER_nondet_* function call)
 */
public class NondeterministicValueProvider {

    private List<Value> knownValues;

    public NondeterministicValueProvider(){
        knownValues = new ArrayList<>();
    }

    public void setKnownValues(List<Value> pKnownValues){
        knownValues = new ArrayList<>(pKnownValues);
    }
    
    public void clearKnownValues() {
        if (knownValues != null) {
            knownValues.clear();
        }
    }

    /**
     * Retrieve a value to check some of it's properties but don't remove it for
     * further use.
     */
    public Value peek() {
        return knownValues.get(0);
    }

    /**
     * Actually use up the value and remove it from the provider.
     */
    public Value remove() {
        return knownValues.remove(0);
    }

    public boolean isEmpty() {
        return knownValues.isEmpty();
    }
}
