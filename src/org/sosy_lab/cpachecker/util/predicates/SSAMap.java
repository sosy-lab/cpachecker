/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.notNull;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaList;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;

/**
 * Maps a variable name to its latest "SSA index", that should be used when
 * referring to that variable
 */
public class SSAMap implements Serializable {

  private static final long serialVersionUID = 7618801653203679876L;

  /**
   * Builder for SSAMaps. Its state starts with an existing SSAMap, but may be
   * changed later. It supports read access, but it is not recommended to use
   * instances of this class except for the short period of time
   * while creating a new SSAMap.
   *
   * This class is not thread-safe.
   */
  public static class SSAMapBuilder {

    private SSAMap ssa;
    private Map<Object, CType> typesBuilder = null;
    private Multiset<String> varsBuilder = null;
    private Multiset<Pair<String, FormulaList>> funcsBuilder = null;

    protected SSAMapBuilder(SSAMap ssa) {
      this.ssa = ssa;
    }

    public int getIndex(String variable) {
      return SSAMap.getIndex(variable, Objects.firstNonNull(varsBuilder, ssa.vars));
    }

    public int getIndex(String name, FormulaList args) {
      return SSAMap.getIndex(Pair.<String, FormulaList>of(name, args),
          Objects.firstNonNull(funcsBuilder, ssa.funcs));
    }

    public CType getType(String name) {
      return Objects.firstNonNull(typesBuilder, ssa.types).get(name);
    }

    public void setIndex(String name, CType type, int idx) {
      Preconditions.checkArgument(idx > 0, "Indices need to be positive for this SSAMap implementation!");

      if (varsBuilder == null) {
        varsBuilder = LinkedHashMultiset.create(ssa.vars);
      }
      Preconditions.checkArgument(idx >= varsBuilder.count(name), "SSAMap updates need to be strictly monotone!");

      setType(name, type);
      varsBuilder.setCount(name, idx);
    }

    private void setType(Object key, CType type) {
      checkNotNull(type);
      CType oldType = Objects.firstNonNull(typesBuilder, ssa.types).get(key);
      if (oldType != null) {
        Preconditions.checkArgument(oldType.equals(type)
            || type instanceof CFunctionType || oldType instanceof CFunctionType
            , "Cannot change type of variable %s in SSAMap from %s to %s", key, oldType, type);
      } else {
        if (typesBuilder == null) {
          typesBuilder = new HashMap<>(ssa.types);
        }
        typesBuilder.put(key, type);
      }
    }

    public void setIndex(String name, FormulaList args, CType type, int idx) {
      Preconditions.checkArgument(idx > 0, "Indices need to be positive for this SSAMap implementation!");

      if (funcsBuilder == null) {
        funcsBuilder = LinkedHashMultiset.create(ssa.funcs);
      }
      Pair<String, FormulaList> key = Pair.of(name, args);
      Preconditions.checkArgument(idx >= funcsBuilder.count(key), "SSAMap updates need to be strictly monotone!");

      setType(key, type);
      funcsBuilder.setCount(key, idx);
    }

    public void deleteVariable(String variable) {
      int index = getIndex(variable);
      if (index != -1) {

        if (varsBuilder == null) {
          varsBuilder = LinkedHashMultiset.create(ssa.vars);
        }
        if (typesBuilder == null) {
          typesBuilder = new HashMap<>(ssa.types);
        }

        varsBuilder.remove(variable, index);
        typesBuilder.remove(variable);
      }
    }

    public Iterable<Pair<Variable, FormulaList>> allFunctions() {
      return SSAMap.allFunctions(Objects.firstNonNull(typesBuilder, ssa.types));
    }

    public Iterable<Variable> allVariables() {
      return SSAMap.allVariables(Objects.firstNonNull(typesBuilder, ssa.types));
    }

    /**
     * Returns an immutable SSAMap with all the changes made to the builder.
     */
    public SSAMap build() {
      if (varsBuilder == null && funcsBuilder == null) {
        return ssa;
      }

      ssa = new SSAMap(Objects.firstNonNull(varsBuilder, ssa.vars),
                       Objects.firstNonNull(funcsBuilder, ssa.funcs),
                       Objects.firstNonNull(typesBuilder, ssa.types));
      varsBuilder  = null;
      funcsBuilder = null;
      return ssa;
    }
  }

  private static final SSAMap EMPTY_SSA_MAP = new SSAMap(
      ImmutableMultiset.<String>of(),
      ImmutableMultiset.<Pair<String, FormulaList>>of(),
      ImmutableMap.<Object, CType>of());

  /**
   * Returns an empty immutable SSAMap.
   */
  public static SSAMap emptySSAMap() {
    return EMPTY_SSA_MAP;
  }

  public SSAMap withDefault(final int defaultValue) {
    return new SSAMap(this.vars, this.funcs, this.types) {

      private static final long serialVersionUID = -5638018887478723717L;

      @Override
      public int getIndex(String pVariable) {
        int result = super.getIndex(pVariable);

        return (result < 0) ? defaultValue : result;
      }

      @Override
      public int getIndex(String pName, FormulaList pArgs) {
        int result = super.getIndex(pName, pArgs);

        return (result < 0) ? defaultValue : result;
      }
    };
  }

  /**
   * Returns an unmodifiable SSAMap that contains all indices from two SSAMaps.
   * If there are conflicting indices, the maximum of both is used.
   */
  public static SSAMap merge(SSAMap s1, SSAMap s2) {
    // This method uses some optimizations to avoid work when parts of both SSAMaps
    // are equal. These checks use == instead of equals() because it is much faster
    // and we create sets lazily (so when they are not identical, they are
    // probably not equal, too).
    // We don't bother checking the vars set for emptiness, because this will
    // probably never be the case on a merge.

    Multiset<String> vars;
    if (s1.vars == s2.vars) {
      if (s1.funcs == s2.funcs && s1.types == s2.types) {
        // both are absolutely identical
        return s1;
      }
      vars = s1.vars;

    } else {
      vars = merge(s1.vars, s2.vars);
    }

    Multiset<Pair<String, FormulaList>> funcs;
    if ((s1.funcs == s2.funcs) || s2.funcs.isEmpty()) {
      funcs = s1.funcs;
    } else if (s1.funcs.isEmpty()) {
      funcs = s2.funcs;
    } else {
      funcs = merge(s1.funcs, s2.funcs);
    }

    Map<Object, CType> types;
    if (s1.types == s2.types) {
      types = s1.types;
    } else {

      MapDifference<Object, CType> diff = Maps.difference(s1.types, s2.types);
      if (!diff.entriesDiffering().isEmpty()) {
        throw new IllegalArgumentException("Cannot merge SSAMaps that contain the same variable {0} with differing types: " + diff.entriesDiffering());
      }

      if (diff.entriesOnlyOnLeft().isEmpty()) {
        assert s2.types.size() >= s1.types.size();
        types = s2.types;

      } else if (diff.entriesOnlyOnRight().isEmpty()) {
        assert s1.types.size() >= s2.types.size();
        types = s1.types;

      } else {
        types = new HashMap<>(diff.entriesInCommon().size()
                            + diff.entriesOnlyOnLeft().size()
                            + diff.entriesOnlyOnRight().size());
        types.putAll(diff.entriesInCommon());
        types.putAll(diff.entriesOnlyOnLeft());
        types.putAll(diff.entriesOnlyOnRight());
      }
    }

    return new SSAMap(vars, funcs, types);
  }

  private static <T> Multiset<T> merge(Multiset<T> s1, Multiset<T> s2) {
    Multiset<T> result = LinkedHashMultiset.create(Math.max(s1.elementSet().size(), s2.elementSet().size()));
    for (Entry<T> entry : s1.entrySet()) {
      T key = entry.getElement();
      int i1 = entry.getCount();
      int i2 = s2.count(key);
      result.setCount(key, Math.max(i1, i2));
    }
    for (Entry<T> entry : s2.entrySet()) {
      T key = entry.getElement();
      if (!s1.contains(key)) {
        result.setCount(key, entry.getCount());
      }
    }

    return result;
  }

  /*
   * Use Multiset<String> instead of Map<String, Integer> because it is more
   * efficient. The integer value is stored as the number of instances of any
   * element in the Multiset. So instead of calling map.get(key) we just use
   * Multiset.count(key). This is better because the Multiset internally uses
   * modifiable integers instead of the immutable Integer class.
   */
  private final Multiset<String> vars;
  private final Multiset<Pair<String, FormulaList>> funcs;

  private final Map<Object, CType> types;

  private SSAMap(Multiset<String> vars,
                 Multiset<Pair<String, FormulaList>> funcs,
                 Map<Object, CType> types) {
    this.vars = vars;
    this.funcs = funcs;
    this.types = types;
  }

  /**
   * Returns a SSAMapBuilder that is initialized with the current SSAMap.
   */
  public SSAMapBuilder builder() {
    return new SSAMapBuilder(this);
  }

  private static <T> int getIndex(T key, Multiset<T> map) {
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

  public int getIndex(String name, FormulaList args) {
    return getIndex(Pair.<String, FormulaList>of(name, args), funcs);
  }

  public Set<String> allVariables() {
    return Collections.unmodifiableSet(vars.elementSet());
  }

  public Iterable<Variable> allVariablesWithTypes() {
    return allVariables(types);
  }

  static Iterable<Variable> allVariables(final Map<Object, CType> types) {
    return FluentIterable.from(types.entrySet())
        .transform(
            new Function<Map.Entry<Object, CType>, Variable>() {
              @Override
              public Variable apply(Map.Entry<Object, CType> pInput) {
                if (pInput.getKey() instanceof String) {
                  return Variable.create((String)pInput.getKey(), pInput.getValue());
                } else {
                  return null;
                }
              }
            })
        .filter(notNull());
  }

  public Iterable<Pair<Variable, FormulaList>> allFunctions() {
    return allFunctions(types);
  }

  static Iterable<Pair<Variable, FormulaList>> allFunctions(final Map<Object, CType> types) {
    return FluentIterable.from(types.entrySet())
        .transform(
            new Function<Map.Entry<Object, CType>, Pair<Variable, FormulaList>>() {
              @Override
              public Pair<Variable, FormulaList> apply(Map.Entry<Object, CType> pInput) {
                if (pInput.getKey() instanceof Pair<?, ?>) {
                  @SuppressWarnings("unchecked")
                  Pair<String, FormulaList> input = (Pair<String, FormulaList>)pInput.getKey();
                  return Pair.of(Variable.create(input.getFirst(), pInput.getValue()), input.getSecond());
                } else {
                  return null;
                }
              }
            })
        .filter(notNull());
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
