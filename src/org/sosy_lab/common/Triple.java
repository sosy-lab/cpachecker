/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.common;


/**
 * A generic Triple class based on Pair.java
 * @author Michael Tautschnig <tautschnig@forsyte.de>
 *
 * @param <A>
 * @param <B>
 * @param <C>
 */
public class Triple<A, B, C> {
    private final A first;
    private final B second;
    private final C third;

    public Triple(A first, B second, C third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    public A getFirst() { return first; }
    public B getSecond() { return second; }
    public C getThird() { return third; }

    @Override
    public String toString() {
        return "(" + first + ", " + second + ", " + third + ")";
    }

    private static boolean equals(Object x, Object y) {
        return (x == null && y == null) || (x != null && x.equals(y));
    }

    @Override
    public boolean equals(Object other) {
    return (other instanceof Triple<?,?,?>)
        && equals(first,  ((Triple<?,?,?>)other).first)
        && equals(second, ((Triple<?,?,?>)other).second)
        && equals(third,  ((Triple<?,?,?>)other).third);
    }

    @Override
    public int hashCode() {
        if (first == null && second == null) return (third == null) ? 0 : third.hashCode() + 1;
        else if (first == null && third == null) return second.hashCode() + 2;
        else if (first == null) return second.hashCode() * 7 + third.hashCode();
        else if (second == null && third == null) return first.hashCode() + 3;
        else if (second == null) return first.hashCode() * 11 + third.hashCode();
        else if (third == null) return first.hashCode() * 13 + second.hashCode();
        else return first.hashCode() * 17 + second.hashCode() * 5 + third.hashCode();
    }
}
