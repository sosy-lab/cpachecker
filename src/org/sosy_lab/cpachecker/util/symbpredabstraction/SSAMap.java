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

import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormulaList;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset.Entry;

/**
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 *
 * Maps a variable name to its latest "SSA index", that should be used when
 * referring to that variable
 */
public class SSAMap {

  /**
   * Builder for SSAMaps. Its state starts with an existing SSAMap, but may be
   * changed later. It supports read access, but changes made to the builder are
   * NOT visible until {@link #build()} has been called! Thus it is
   * NOT recommended to use instances of this class except for the short period
   * of time while creating a new SSAMap. 
   * 
   * This class is not thread-safe.
   */
  public static class SSAMapBuilder {
    
    private SSAMap ssa;
    private ImmutableMultiset.Builder<String> varsBuilder = null;
    private ImmutableMultiset.Builder<Pair<String, SymbolicFormulaList>> funcsBuilder = null;
    
    protected SSAMapBuilder(SSAMap ssa) {
      this.ssa = ssa;
    }
    
    public int getIndex(String variable) {
      return ssa.getIndex(variable);
    }
    
    public int getIndex(String func, SymbolicFormulaList args) {
      return ssa.getIndex(func, args);
    }
    
    public void setIndex(String var, int idx) {
      if (varsBuilder == null) {
        varsBuilder = ImmutableMultiset.builder();
        for (Entry<String> entry : ssa.vars.entrySet()) {
          varsBuilder.setCount(entry.getElement(), entry.getCount());
        }
      }
      varsBuilder.setCount(var, idx);
    }
    
    public void setIndex(String func, SymbolicFormulaList args, int idx) {
      if (funcsBuilder == null) {
        funcsBuilder = ImmutableMultiset.builder();
        for (Entry<Pair<String, SymbolicFormulaList>> entry : ssa.funcs.entrySet()) {
          funcsBuilder.setCount(entry.getElement(), entry.getCount());
        }
      }
      funcsBuilder.setCount(new Pair<String, SymbolicFormulaList>(func, args), idx);
    }
    
    /**
     * Returns an immutable SSAMap with all the changes made to the builder.
     */
    public SSAMap build() {
      if (varsBuilder == null && funcsBuilder == null) {
        return ssa;
      }
      
      ImmutableMultiset<String> newVars
                     = (varsBuilder  == null ? ssa.vars  : varsBuilder.build());
      ImmutableMultiset<Pair<String, SymbolicFormulaList>> newFuncs
                     = (funcsBuilder == null ? ssa.funcs : funcsBuilder.build());
      
      ssa = new SSAMap(newVars, newFuncs);
      varsBuilder  = null;
      funcsBuilder = null;
      return ssa;
    }
  }
  
  private static final SSAMap EMPTY_SSA_MAP = new SSAMap(
      ImmutableMultiset.<String>of(),
      ImmutableMultiset.<Pair<String, SymbolicFormulaList>>of());
  
  /**
   * Returns an empty immutable SSAMap.
   */
  public static SSAMap emptySSAMap() {
    return EMPTY_SSA_MAP;
  }

  /**
   * Returns an unmodifiable SSAMap that contains all indices from two SSAMaps.
   * If there are conflicting indices, the maximum of both is used.
   */
  public static SSAMap merge(SSAMap s1, SSAMap s2) {
    
    ImmutableMultiset.Builder<String> varsBuilder = ImmutableMultiset.builder();
    for (Entry<String> entry : s1.vars.entrySet()) {
      String name = entry.getElement();
      int i1 = entry.getCount();
      int i2 = s2.vars.count(name);
      varsBuilder.setCount(name, Math.max(i1, i2));
    }
    for (Entry<String> entry : s2.vars.entrySet()) {
      String name = entry.getElement();
      if (!s1.vars.contains(name)) {
        varsBuilder.setCount(name, entry.getCount());
      }
    }
    
    ImmutableMultiset.Builder<Pair<String, SymbolicFormulaList>> funcsBuilder = ImmutableMultiset.builder();
    for (Entry<Pair<String, SymbolicFormulaList>> entry : s1.funcs.entrySet()) {
      Pair<String, SymbolicFormulaList> key = entry.getElement();
      int i1 = entry.getCount();
      int i2 = s2.funcs.count(key);
      funcsBuilder.setCount(key, Math.max(i1, i2));
    }
    for (Entry<Pair<String, SymbolicFormulaList>> entry : s2.funcs.entrySet()) {
      Pair<String, SymbolicFormulaList> key = entry.getElement();
      if (!s1.vars.contains(key)) {
        funcsBuilder.setCount(key, entry.getCount());
      }
    }
    
    return new SSAMap(varsBuilder.build(), funcsBuilder.build());
  }
  
  /**
   * Use Multiset<String> instead of Map<String, Integer> because it is more
   * efficient. The integer value is stored as the number of instances of any
   * element in the Multiset. So instead of calling map.get(key) we just use
   * Multiset.count(key). This is better because the Multiset internally uses
   * modifiable integers instead of the immutable Integer class. Also, the
   * builder class for ImmutableMultiset is more flexible than the one for the
   * ImmutableMap, which makes using a SSAMapBuilder possible. 
   */
  private final ImmutableMultiset<String> vars;
  private final ImmutableMultiset<Pair<String, SymbolicFormulaList>> funcs;
  
  private SSAMap(ImmutableMultiset<String> vars,
                 ImmutableMultiset<Pair<String, SymbolicFormulaList>> funcs) {
    this.vars = vars;
    this.funcs = funcs;
  }
  
  /**
   * Returns a SSAMapBuilder that is initialized with the current SSAMap.
   */
  public SSAMapBuilder builder() {
    return new SSAMapBuilder(this);
  }
  
  private static <T> int getIndex(T key, ImmutableMultiset<T> map) {
    int i = map.count(key);
    if (i != 0) {
      return i;
    } else {
      // no index found, return -1
      return -1;
    }
  }
  
  /**
   * returns the index of the variable in the map
   */
  public int getIndex(String variable) {
    return getIndex(variable, vars);
  }

  public int getIndex(String name, SymbolicFormulaList args) {
    return getIndex(new Pair<String, SymbolicFormulaList>(name, args), funcs);
  }
  
  protected int getIndex(Pair<String, SymbolicFormulaList> key) {
    return getIndex(key, funcs);
  }

  protected Set<String> allVariables() {
    return vars.elementSet();
  }

  protected Set<Pair<String, SymbolicFormulaList>> allFunctions() {
    return funcs.elementSet();
  }

  private static final Joiner joiner = Joiner.on(" ");
  
  @Override
  public String toString() {
    return joiner.join(vars.entrySet()) + " " + joiner.join(funcs.entrySet());
  }
  
  @Override
  public int hashCode() {
    return (31 + funcs.hashCode()) * 31 + vars.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    } else if (!(obj instanceof SSAMap)) {
      return false;
    } else {
      SSAMap other = (SSAMap)obj;
      return vars.equals(other.vars) && funcs.equals(other.funcs);
    }
  }
}
