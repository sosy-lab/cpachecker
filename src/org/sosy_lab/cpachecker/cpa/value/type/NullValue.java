/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.value.type;

import java.io.Serializable;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.interval.NumberInterface;

/**
 * Singleton class for the special value <code>null</code>.
 */
public class NullValue implements NumberInterface, Serializable {

    private static final long serialVersionUID = 49593725475613735L;

    private static final NullValue SINGLETON = new NullValue();

    private NullValue() {
        // private constructor for singleton pattern
    }

    /**
     * Returns an instance of a <code>NullValue</code> object.
     *
     * @return an instance of this object
     */
    public static NumberInterface getInstance() {
        return SINGLETON;
    }

    /**
     * Always returns <code>false</code> since <code>null</code> is no numeric
     * value.
     *
     * @return always returns <code>false</code>
     */
    @Override
    public boolean isNumericValue() {
        return false;
    }

    /**
     * Always returns <code>false</code> since <code>null</code> is a specific
     * value.
     *
     * @return always returns <code>false</code>
     */
    @Override
    public boolean isUnknown() {
        return false;
    }

    /**
     * Always returns <code>true</code> since <code>null</code> is a specific value.
     */
    @Override
    public boolean isExplicitlyKnown() {
        return true;
    }

    /**
     * This method is not implemented and will lead to an
     * <code>AssertionError</code>. <code>Null</code> can't be represented by a
     * specific number.
     */
    @Override
    public Long asLong(CType pType) {
        throw new AssertionError("Null cannot be represented as Long");
    }

    @Override
    public boolean equals(Object other) {

        // all NullValue objects are equal
        return other instanceof NullValue;
    }

    @Override
    public int hashCode() {
        return 1; // singleton without any values
    }

    @Override
    public String toString() {
        return "NULL";
    }

    @Override
    public NumberInterface plus(NumberInterface pRight) {
        throw new AssertionError("trying to perform default function");

    }

    @Override
    public NumberInterface minus(NumberInterface pRight) {
        throw new AssertionError("trying to perform default function");

    }

    @Override
    public NumberInterface times(NumberInterface pRight) {
        throw new AssertionError("trying to perform default function");

    }

    @Override
    public NumberInterface divide(NumberInterface pRight) {
        throw new AssertionError("trying to perform default function");

    }

    @Override
    public NumberInterface modulo(NumberInterface pRight) {
        throw new AssertionError("trying to perform default function");

    }

    @Override
    public NumberInterface binaryAnd(NumberInterface pRight) {
        throw new AssertionError("trying to perform default function");

    }

    @Override
    public NumberInterface evaluateLessOperator(NumberInterface pRight) {
        throw new AssertionError("trying to perform default function");

    }

    @Override
    public NumberInterface evaluateLessEqualOperator(NumberInterface pRight) {
        throw new AssertionError("trying to perform default function");

    }

    @Override
    public NumberInterface evaluateEqualOperator(NumberInterface pRight) {
        throw new AssertionError("trying to perform default function");

    }
}
