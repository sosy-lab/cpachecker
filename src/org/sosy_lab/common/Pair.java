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

import java.util.Map.Entry;

import com.google.common.base.Function;


/**
 * A generic Pair class. Code borrowed from here:
 * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6229146
 * @author alb
 *
 * @param <A>
 * @param <B>
 */
public class Pair<A, B> {
    private final A first;
    private final B second;

    private Pair(A first, B second) {
        this.first = first;
        this.second = second;
    }
    
    public static <A, B> Pair<A, B> of(A first, B second) {
      return new Pair<A, B>(first, second);
    }

    public A getFirst() { return first; }
    public B getSecond() { return second; }

    @Override
    public String toString() {
        return "(" + first + ", " + second + ")";
    }

    private static boolean equals(Object x, Object y) {
        return (x == null && y == null) || (x != null && x.equals(y));
    }

    @Override
    public boolean equals(Object other) {
    return (other instanceof Pair<?,?>)
        && equals(first,  ((Pair<?,?>)other).first)
        && equals(second, ((Pair<?,?>)other).second);
    }

    @Override
    public int hashCode() {
        if (first == null) return (second == null) ? 0 : second.hashCode() + 1;
        else if (second == null) return first.hashCode() + 2;
        else return first.hashCode() * 17 + second.hashCode();
    }
    
    public static <T> Function<Pair<? extends T, ?>, T> getProjectionToFirst() {
      return Holder.<T, Void>getInstance().PROJECTION_TO_FIRST;
    }
    
    public static <T> Function<Pair<?, ? extends T>, T> getProjectionToSecond() {
      return Holder.<T, Void>getInstance().PROJECTION_TO_SECOND;
    }
    
    public static <K,V> Function<Entry<? extends K, ? extends V>, Pair<K, V>> getPairFomMapEntry() {
      return Holder.<K, V>getInstance().PAIR_FROM_MAP_ENTRY;
    }
    
    /*
     * Static holder class for several function objects because if these fields
     * were static fields of the Pair class, they couldn't be generic.
     */
    private static final class Holder<T, T2> {
      
      private static final Holder<?, ?> INSTANCE = new Holder<Void, Void>();
      
      // Cast is safe because class has no state
      @SuppressWarnings("unchecked")
      public static <T, T2> Holder<T, T2> getInstance() {
        return (Holder<T, T2>) INSTANCE;
      }
      
      private final Function<Pair<? extends T, ?>, T> PROJECTION_TO_FIRST = new Function<Pair<? extends T, ?>, T>() {
        @Override
        public T apply(Pair<? extends T, ?> pArg0) {
          return pArg0.getFirst();
        }
      };
      
      private final Function<Pair<?, ? extends T>, T> PROJECTION_TO_SECOND = new Function<Pair<?, ? extends T>, T>() {
        @Override
        public T apply(Pair<?, ? extends T> pArg0) {
          return pArg0.getSecond();
        }
      };
      
      private final Function<Entry<? extends T, ? extends T2>, Pair<T, T2>> PAIR_FROM_MAP_ENTRY = new Function<Entry<? extends T, ? extends T2>, Pair<T, T2>>() {
        @Override
        public Pair<T, T2> apply(
            Entry<? extends T, ? extends T2> pArg0) {
          return Pair.<T, T2>of(pArg0.getKey(), pArg0.getValue());
        }
      };
    }
}
