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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormulaList;

import com.google.common.base.Joiner;
import com.google.common.base.Joiner.MapJoiner;

/**
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 *
 * Maps a variable name to its latest "SSA index", that should be used when
 * referring to that variable
 * 
 * TODO: think about memory efficient copying
 */
public class SSAMap {

  private static class UnmodifiableSSAMap extends SSAMap {
    
    private UnmodifiableSSAMap(SSAMap ssa) {
      super(ssa.vars, ssa.funcs);
    }
    
    @Override
    public void setIndex(String pName, SymbolicFormulaList pArgs, int pIdx) {
      throw new UnsupportedOperationException();
    }
    
    @Override
    public void setIndex(String pVariable, int pIdx) {
      throw new UnsupportedOperationException();
    }
  }
  
  public static SSAMap unmodifiableSSAMap(SSAMap ssa) {
    if (ssa instanceof UnmodifiableSSAMap) {
      return ssa;
    } else {
      return new UnmodifiableSSAMap(ssa);
    }
  }
  
  private static final SSAMap EMPTY_SSA_MAP = new UnmodifiableSSAMap(new SSAMap());
  
  public static SSAMap emptySSAMap() {
    return EMPTY_SSA_MAP;
  }

  private final Map<String, Integer> vars;
  private final Map<Pair<String, SymbolicFormulaList>, Integer> funcs;

  public SSAMap() {
    vars = new HashMap<String, Integer>(0);
    funcs = new HashMap<Pair<String, SymbolicFormulaList>, Integer>(0);
  }
  
  public SSAMap(SSAMap old) {
    vars = new HashMap<String, Integer>(old.vars.size() + 1);
    funcs = new HashMap<Pair<String, SymbolicFormulaList>, Integer>(old.funcs.size() + 1);
    vars.putAll(old.vars);
    funcs.putAll(old.funcs);
  }
  
  private SSAMap(Map<String, Integer> vars, Map<Pair<String, SymbolicFormulaList>, Integer> funcs) {
    this.vars = vars;
    this.funcs = funcs;
  }
  
  /**
   * returns the index of the variable in the map
   */
  public int getIndex(String variable) {
    Integer i = vars.get(variable);
    if (i != null) {
      return i;
    } else {
      // no index found, return -1
      return -1;
    }
  }

  public void setIndex(String variable, int idx) {
    vars.put(variable, idx);
  }

  public int getIndex(String name, SymbolicFormulaList args) {
    Integer i = funcs.get(new Pair<String, SymbolicFormulaList>(name, args));
    if (i != null) {
      return i;
    } else {
      // no index found, return -1
      return -1;
    }
  }

  public void setIndex(String name, SymbolicFormulaList args, int idx) {
    funcs.put(new Pair<String, SymbolicFormulaList>(name, args), idx);
  }

  public Collection<String> allVariables() {
    return Collections.unmodifiableSet(vars.keySet());
  }

  public Collection<Pair<String, SymbolicFormulaList>> allFunctions() {
    return Collections.unmodifiableSet(funcs.keySet());
  }

  private static final MapJoiner joiner = Joiner.on(" ").withKeyValueSeparator("@");
  
  @Override
  public String toString() {
    return joiner.join(vars) + " " + joiner.join(funcs);
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
