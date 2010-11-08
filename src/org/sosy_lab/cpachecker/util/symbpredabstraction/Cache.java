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
package org.sosy_lab.cpachecker.util.symbpredabstraction;

import java.util.Arrays;
import java.util.HashMap;

import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormula;

/**
 * Helper class for various caching things, which are used by several other
 * classes (mostly AbstractFormulaManagers from various CPAs).
 */
public class Cache {

  public static abstract class KeyWithTimeStamp implements Comparable<KeyWithTimeStamp> {
    private long timeStamp;

    public KeyWithTimeStamp() {
      updateTimeStamp();
    }

    public void updateTimeStamp() {
      timeStamp = System.currentTimeMillis();
    }

    @Override
    public int compareTo(KeyWithTimeStamp p1) {
      long r = this.timeStamp - p1.timeStamp;
      return r < 0 ? -1 : (r > 0 ? 1 : 0);
    }

    @Override
    public abstract boolean equals(Object pObj);

    @Override
    public abstract int hashCode();
  }

  public static class CartesianAbstractionCacheKey extends KeyWithTimeStamp {
    private final SymbolicFormula formula;
    private final Predicate pred;

    public CartesianAbstractionCacheKey(SymbolicFormula f, Predicate p) {
      super();
      formula = f;
      pred = p;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o instanceof CartesianAbstractionCacheKey) {
        CartesianAbstractionCacheKey c =
          (CartesianAbstractionCacheKey)o;
        return formula.equals(c.formula) && pred.equals(c.pred);
      } else {
        return false;
      }
    }

    @Override
    public int hashCode() {
      return formula.hashCode() ^ pred.hashCode();
    }
  }

  public static class FeasibilityCacheKey extends KeyWithTimeStamp {

    private final SymbolicFormula f;

    public FeasibilityCacheKey(SymbolicFormula fm) {
      super();
      f = fm;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o instanceof FeasibilityCacheKey) {
        return f.equals(((FeasibilityCacheKey)o).f);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return f.hashCode();
    }
  }

  public static class TimeStampCache<Key extends KeyWithTimeStamp, Value> extends HashMap<Key, Value> {

    private static final long serialVersionUID = 1L;
    private final int maxSize;

    public TimeStampCache(int maxSize) {
      super();
      this.maxSize = maxSize;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Value get(Object o) {
      Key key = (Key)o;
      key.updateTimeStamp();
      return super.get(key);
    }

    @Override
    public Value put(Key key, Value value) {
      key.updateTimeStamp();
      compact();
      return super.put(key, value);
    }

    private void compact() {
      if (size() > maxSize) {
        // find the half oldest entries, and get rid of them...
        KeyWithTimeStamp[] keys = keySet().toArray(new KeyWithTimeStamp[size()]);
        Arrays.sort(keys);
        for (int i = 0; i < keys.length/2; ++i) {
          remove(keys[i]);
        }
      }
    }
  }
}
