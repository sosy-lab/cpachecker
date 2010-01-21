/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package symbpredabstraction;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import symbpredabstraction.interfaces.SymbolicFormula;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import common.Pair;

/**
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 *
 * Maps a variable name to its latest "SSA index", that should be used when
 * referring to that variable
 */
public class SSAMap {
  
  private static interface Key {}
  
  private static class VarKey implements Key {
    private final String name;

    public VarKey(String str) { name = str; }
    public String getName() { return name; }

    @Override
    public int hashCode() {
      return name.hashCode();
    }
    
    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      } else if (o instanceof VarKey) {
        return name.equals(((VarKey)o).name);
      } else {
        return false;
      }
    }

    @Override
    public String toString() { return name; }
  }
    
  private static class FuncKey implements Key {
    private final String name;
    private final SymbolicFormula[] args;

    public FuncKey(String n, SymbolicFormula[] a) {
        name = n;
        args = a;
    }
    
    public String getName() { return name; }
    public SymbolicFormula[] getArgs() { return args; }

    @Override
    public int hashCode() {
        return 31 * name.hashCode() + Arrays.hashCode(args);
    }
    
    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      } else if (o instanceof FuncKey) {
          FuncKey f = (FuncKey)o;
          return name.equals(f.name) && Arrays.deepEquals(args, f.args);
      } else {
        return false;
      }
    }

    @Override
    public String toString() {
      return name + "(" + Joiner.on(",").join(args) + ")";
    }
  }
  
  private final Map<Key, Integer> repr = new HashMap<Key, Integer>();
  
    /**
     * returns the index of the variable in the map
     */
    public int getIndex(String variable) {
      int i;
      VarKey k = new VarKey(variable);
      if (repr.containsKey(k)) {
          i = repr.get(k);
      } else {
          // no index found, return -1
          i = -1;
      }
      return i;
    }

    public void setIndex(String variable, int idx) {
        repr.put(new VarKey(variable), idx);
    }

    public int getIndex(String name, SymbolicFormula[] args) {
        FuncKey k = new FuncKey(name, args);
        if (repr.containsKey(k)) {
            return repr.get(k);
        } else {
            return -1;
        }
    }

    public void setIndex(String name, SymbolicFormula[] args, int idx) {
        repr.put(new FuncKey(name, args), idx);
    }

    public Collection<String> allVariables() {
      List<String> ret = Lists.newArrayList();

      for (Key k : repr.keySet()) {
        if (k instanceof VarKey) {
          ret.add(((VarKey)k).getName());
        }
      }
      return ret;
    }
    
    public Collection<Pair<String, SymbolicFormula[]>> allFunctions() {
      List<Pair<String, SymbolicFormula[]>> ret = Lists.newArrayList();

      for (Key k : repr.keySet()) {
        if (k instanceof FuncKey) {
          FuncKey kk = (FuncKey)k;
          ret.add(new Pair<String, SymbolicFormula[]>(kk.getName(), kk.getArgs()));
        }
      }
      return ret;
    }

    @Override
    public String toString() {
      return Joiner.on(" ").withKeyValueSeparator("@").join(repr);
    }

    /**
     * Explicit "copy constructor". I am not experienced enough with Java to
     * dare implementing a proper clone() :-)
     */
    public void copyFrom(SSAMap other) {
        for (Key k : other.repr.keySet()) {
            repr.put(k, other.repr.get(k));
        }
    }

    /**
     * updates this map with the contents of other. That is, adds to this map
     * all the variables present in other but not in this
     */
    public void update(SSAMap other) {
        for (Key k : other.repr.keySet()) {
            if (!repr.containsKey(k)) {
                repr.put(k, other.repr.get(k));
            }
        }
    }
}
