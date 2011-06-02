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

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
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

    public static <A, B> List<Pair<A, B>> zipList(Collection<? extends A> a, Collection<? extends B> b) {
      List<Pair<A, B>> result = new ArrayList<Pair<A, B>>(a.size());

      Iterator<? extends A> iteratorA = a.iterator();
      Iterator<? extends B> iteratorB = b.iterator();
      while (iteratorA.hasNext()) {
        checkArgument(iteratorB.hasNext(), "Second list is shorter");

        result.add(Pair.<A, B>of(iteratorA.next(), iteratorB.next()));
      }
      checkArgument(!iteratorB.hasNext(), "Second list is longer");

      return result;
    }

    public static <A, B> Iterable<Pair<A, B>> zipWithPadding(final Iterable<? extends A> a, final Iterable<? extends B> b) {
      return new Iterable<Pair<A, B>>() {
        @Override
        public Iterator<Pair<A, B>> iterator() {
          return new ZipIterator<A, B>(a.iterator(), b.iterator());
        }
      };
    }

    private static class ZipIterator<A, B> implements Iterator<Pair<A, B>> {

      private final Iterator<? extends A> a;
      private final Iterator<? extends B> b;

      public ZipIterator(Iterator<? extends A> pA, Iterator<? extends B> pB) {
        a = pA;
        b = pB;
      }

      @Override
      public boolean hasNext() {
        return a.hasNext() || b.hasNext();
      }

      @Override
      public Pair<A, B> next() {
        A nextA = null;
        if (a.hasNext()) {
          nextA = a.next();
        }
        B nextB = null;
        if (b.hasNext()) {
          nextB = b.next();
        }
        return Pair.of(nextA, nextB);
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    }
}
