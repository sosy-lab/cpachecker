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
package org.sosy_lab.cpachecker.util.predicates.pathformula;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.notNull;

import java.io.Serializable;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;

import javax.annotation.Nullable;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Triple;
import org.sosy_lab.common.collect.Collections3;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaList;

import com.google.common.base.Equivalence;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
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

  public static final int INDEX_NOT_CONTAINED = -1;

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
    private PersistentSortedMap<String, Integer> vars; // Do not update without updating varsHashCode!
    private PersistentSortedMap<String, CType> varTypes;
    private Multiset<Pair<String, FormulaList>> funcsBuilder = null;
    private Map<Pair<String, FormulaList>, CType> funcTypesBuilder = null;

    // Instead of computing vars.hashCode(),
    // we calculate the hashCode ourselves incrementally
    // (this is possible because a Map's hashCode is clearly defined).
    private int varsHashCode;

    private SSAMapBuilder(SSAMap ssa) {
      this.ssa = ssa;
      this.vars = ssa.vars;
      this.varTypes = ssa.varTypes;
      this.varsHashCode = ssa.varsHashCode;
    }

    public int getIndex(String variable) {
      return SSAMap.getIndex(variable, vars);
    }

    public int getIndex(String name, FormulaList args) {
      return SSAMap.getIndex(Pair.<String, FormulaList>of(name, args),
          Objects.firstNonNull(funcsBuilder, ssa.funcs));
    }

    public CType getType(String name) {
      return varTypes.get(name);
    }

    public void setIndex(String name, CType type, int idx) {
      Preconditions.checkArgument(idx > 0, "Indices need to be positive for this SSAMap implementation!");
      int oldIdx = getIndex(name);
      Preconditions.checkArgument(idx >= oldIdx, "SSAMap updates need to be strictly monotone!");

      checkNotNull(type);
      CType oldType = varTypes.get(name);
      if (oldType != null) {
        oldType = oldType.getCanonicalType();
        type = type.getCanonicalType();
        Preconditions.checkArgument(
            name.startsWith("__content__")
            || type instanceof CFunctionType || oldType instanceof CFunctionType
            || (isEnumPointerType(type) && isEnumPointerType(oldType))
            || oldType.equals(type)
            , "Cannot change type of variable %s in SSAMap from %s to %s", name, oldType, type);
      } else {
        varTypes = varTypes.putAndCopy(name, type);
      }

      if (idx > oldIdx) {
        vars = vars.putAndCopy(name, idx);

        if (oldIdx != INDEX_NOT_CONTAINED) {
          varsHashCode -= mapEntryHashCode(name, oldIdx);
        }
        varsHashCode += mapEntryHashCode(name, idx);
      }
    }

    public void setIndex(String name, FormulaList args, CType type, int idx) {
      Preconditions.checkArgument(idx > 0, "Indices need to be positive for this SSAMap implementation!");

      if (funcsBuilder == null) {
        funcsBuilder = LinkedHashMultiset.create(ssa.funcs);
      }
      Pair<String, FormulaList> key = Pair.of(name, args);
      Preconditions.checkArgument(idx >= funcsBuilder.count(key), "SSAMap updates need to be strictly monotone!");

      checkNotNull(type);
      CType oldType = Objects.firstNonNull(funcTypesBuilder, ssa.types).get(key);
      if (oldType != null) {
        oldType = oldType.getCanonicalType();
        type = type.getCanonicalType();
        Preconditions.checkArgument(
               type instanceof CFunctionType || oldType instanceof CFunctionType
            || (isEnumPointerType(type) && isEnumPointerType(oldType))
            || oldType.equals(type)
            , "Cannot change type of variable %s in SSAMap from %s to %s", key, oldType, type);
      } else {
        if (funcTypesBuilder == null) {
          funcTypesBuilder = new HashMap<>(ssa.types);
        }
        funcTypesBuilder.put(key, type);
      }

      funcsBuilder.setCount(key, idx);
    }

    private static boolean isEnumPointerType(CType type) {
      if (type instanceof CPointerType) {
        type = ((CPointerType) type).getType();
        return (type instanceof CComplexType) && ((CComplexType)type).getKind() == ComplexTypeKind.ENUM
            || (type instanceof CElaboratedType) && ((CElaboratedType)type).getKind() == ComplexTypeKind.ENUM;
      }
      return false;
    }

    public void deleteVariable(String variable) {
      int index = getIndex(variable);
      if (index != INDEX_NOT_CONTAINED) {
        vars = vars.removeAndCopy(variable);
        varsHashCode -= mapEntryHashCode(variable, index);

        varTypes = varTypes.removeAndCopy(variable);
      }
    }

    public Iterable<Pair<Variable, FormulaList>> allFunctions() {
      return SSAMap.allFunctions(Objects.firstNonNull(funcTypesBuilder, ssa.types));
    }

    public SortedSet<String> allVariables() {
      return varTypes.keySet();
    }

    public SortedSet<Map.Entry<String, CType>> allVariablesWithTypes() {
      return varTypes.entrySet();
    }

    public SortedMap<String, CType> allVariablesWithPrefix(String prefix) {
      return Collections3.subMapWithPrefix(varTypes, prefix);
    }

    /**
     * Returns an immutable SSAMap with all the changes made to the builder.
     */
    public SSAMap build() {
      if (vars == ssa.vars && funcsBuilder == null) {
        return ssa;
      }

      ssa = new SSAMap(vars, varsHashCode,
                       Objects.firstNonNull(funcsBuilder, ssa.funcs),
                       varTypes,
                       Objects.firstNonNull(funcTypesBuilder, ssa.types));
      funcsBuilder = null;
      return ssa;
    }

    /**
     * Not-null safe copy of {@link SimpleImmutableEntry#hashCode()}
     * for Object-to-int maps.
     */
    private static int mapEntryHashCode(Object key, int value) {
      return key.hashCode() ^ value;
    }
  }

  private static final SSAMap EMPTY_SSA_MAP = new SSAMap(
      PathCopyingPersistentTreeMap.<String, Integer>of(),
      0,
      ImmutableMultiset.<Pair<String, FormulaList>>of(),
      PathCopyingPersistentTreeMap.<String, CType>of(),
      ImmutableMap.<Pair<String, FormulaList>, CType>of());

  /**
   * Returns an empty immutable SSAMap.
   */
  public static SSAMap emptySSAMap() {
    return EMPTY_SSA_MAP;
  }

  public SSAMap withDefault(final int defaultValue) {
    return new SSAMap(this.vars, this.varsHashCode, this.funcs, this.varTypes, this.types) {

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
   * Creates an unmodifiable SSAMap that contains all indices from two SSAMaps.
   * If there are conflicting indices, the maximum of both is used.
   * Further returns a list with all variables for which different indices
   * were found, together with the two conflicting indices.
   */
  public static Pair<SSAMap, List<Triple<String, Integer, Integer>>> merge(SSAMap s1, SSAMap s2) {
    // This method uses some optimizations to avoid work when parts of both SSAMaps
    // are equal. These checks use == instead of equals() because it is much faster
    // and we create sets lazily (so when they are not identical, they are
    // probably not equal, too).
    // We don't bother checking the vars set for emptiness, because this will
    // probably never be the case on a merge.

    PersistentSortedMap<String, Integer> vars;
    List<Triple<String, Integer, Integer>> differences;
    if (s1.vars == s2.vars) {
      differences = ImmutableList.of();
      if (s1.funcs == s2.funcs && s1.types == s2.types) {
        // both are absolutely identical
        return Pair.of(s1, differences);
      }
      vars = s1.vars;

    } else {
      differences = new ArrayList<>();
      vars = merge(s1.vars, s2.vars, Equivalence.equals(),
          MAXIMUM_ON_CONFLICT, differences);
    }

    Multiset<Pair<String, FormulaList>> funcs;
    if ((s1.funcs == s2.funcs) || s2.funcs.isEmpty()) {
      funcs = s1.funcs;
    } else if (s1.funcs.isEmpty()) {
      funcs = s2.funcs;
    } else {
      funcs = merge(s1.funcs, s2.funcs);
    }

    PersistentSortedMap<String, CType> varTypes;
    if (s1.varTypes == s2.varTypes) {
      varTypes = s1.varTypes;

    } else {
      @SuppressWarnings("unchecked")
      ConflictHandler<Object, CType> exceptionOnConflict = (ConflictHandler<Object, CType>)EXCEPTION_ON_CONFLICT;
      varTypes = merge(s1.varTypes, s2.varTypes,
          new Equivalence<CType>() {
            @Override
            protected boolean doEquivalent(CType pA, CType pB) {
              return pA.getCanonicalType().equals(pB.getCanonicalType());
            }

            @Override
            protected int doHash(CType pT) {
              return pT.hashCode();
            }
          },
          exceptionOnConflict, null);
    }

    Map<Pair<String, FormulaList>, CType> funcTypes;
    if (s1.types == s2.types) {
      funcTypes = s1.types;
    } else {

      MapDifference<Pair<String, FormulaList>, CType> diff = Maps.difference(s1.types, s2.types);
      if (!diff.entriesDiffering().isEmpty()) {
        throw new IllegalArgumentException("Cannot merge SSAMaps that contain the same variable {0} with differing types: " + diff.entriesDiffering());
      }

      if (diff.entriesOnlyOnLeft().isEmpty()) {
        assert s2.types.size() >= s1.types.size();
        funcTypes = s2.types;

      } else if (diff.entriesOnlyOnRight().isEmpty()) {
        assert s1.types.size() >= s2.types.size();
        funcTypes = s1.types;

      } else {
        funcTypes = new HashMap<>(diff.entriesInCommon().size()
                            + diff.entriesOnlyOnLeft().size()
                            + diff.entriesOnlyOnRight().size());
        funcTypes.putAll(diff.entriesInCommon());
        funcTypes.putAll(diff.entriesOnlyOnLeft());
        funcTypes.putAll(diff.entriesOnlyOnRight());
      }
    }

    return Pair.of(new SSAMap(vars, 0, funcs, varTypes, funcTypes), differences);
  }

  /**
   * Merge two PersistentSortedMaps.
   * The result has all key-value pairs where the key is only in one of the map,
   * those which are identical in both map,
   * and for those keys that have a different value in both maps a handler is called,
   * and the result is put in the resulting map.
   * @param s1 The first map.
   * @param s2 The second map.
   * @param conflictHandler The handler that is called for a key with two different values.
   * @param collectDifferences Null or a modifiable list into which keys with different values are put.
   * @return
   */
  private static <K extends Comparable<? super K>, V> PersistentSortedMap<K, V> merge(
      PersistentSortedMap<K, V> s1, PersistentSortedMap<K, V> s2,
      Equivalence<? super V> valueEquals,
      ConflictHandler<? super K, V> conflictHandler,
      @Nullable List<Triple<K, V, V>> collectDifferences) {

    // s1 is the bigger one, so we use it as the base.
    PersistentSortedMap<K, V> result = s1;

    Iterator<Map.Entry<K, V>> it1 = s1.entrySet().iterator();
    Iterator<Map.Entry<K, V>> it2 = s2.entrySet().iterator();

    Map.Entry<K, V> e1 = null;
    Map.Entry<K, V> e2 = null;

    // This loop iterates synchronously through both sets
    // by trying to keep the keys equal.
    // If one iterator falls behind, the other is not forwarded until the first catches up.
    // The advantage of this is it is in O(n log(n))
    // (n iterations, log(n) per update).
    // Invariant: The elements e1 and e2, and all the elements in the iterator
    //            still need to be handled.
    while (((e1 != null) || it1.hasNext())
        && ((e2 != null) || it2.hasNext())) {

      if (e1 == null) {
        e1 = it1.next();
      }
      if (e2 == null) {
        e2 = it2.next();
      }

      int comp = e1.getKey().compareTo(e2.getKey());

      if (comp < 0) {
        // e1 < e2

        K key = e1.getKey();
        V value1 = e1.getValue();

        if (collectDifferences != null) {
          collectDifferences.add(Triple.<K,V,V>of(key, value1, null));
        }

        // forward e1 until e2 catches up
        e1 = null;

      } else if (comp > 0) {
        // e1 > e2

        K key = e2.getKey();
        V value2 = e2.getValue();

        // e2 is not in map
        assert !result.containsKey(key);
        result = result.putAndCopy(key, value2);

        if (collectDifferences != null) {
          collectDifferences.add(Triple.<K,V,V>of(key, null, value2));
        }

        // forward e2 until e1 catches up
        e2 = null;

      } else {
        // e1 == e2

        K key = e1.getKey();
        V value1 = e1.getValue();
        V value2 = e2.getValue();

        if (!valueEquals.equivalent(value1, value2)) {
          V newValue = conflictHandler.resolveConflict(key, value1, value2);
          result = result.putAndCopy(key, newValue);

          if (collectDifferences != null) {
            collectDifferences.add(Triple.of(key, value1, value2));
          }
        }

        // forward both iterators
        e1 = null;
        e2 = null;
      }
    }

    // Now copy the rest of the mappings from s2 (e2 and it2).
    // For s1 this is not necessary.
    if (e2 != null) {
      result = result.putAndCopy(e2.getKey(), e2.getValue());
    }
    while (it2.hasNext()) {
      e2 = it2.next();
      result = result.putAndCopy(e2.getKey(), e2.getValue());
    }

    assert result.size() >= Math.max(s1.size(), s2.size());

    return result;
  }

  private static interface ConflictHandler<K, V> {
    V resolveConflict(K key, V value1, V value2);
  }

  private static final ConflictHandler<Object, ?> EXCEPTION_ON_CONFLICT = new ConflictHandler<Object, Object>() {
    @Override
    public Void resolveConflict(Object key, Object value1, Object value2) {
      throw new IllegalArgumentException("Conflicting value when merging maps for key " + key + ": " + value1 + " and " + value2);
    }
  };

  private static final ConflictHandler<Object, Integer> MAXIMUM_ON_CONFLICT = new ConflictHandler<Object, Integer>() {
    @Override
    public Integer resolveConflict(Object key, Integer value1, Integer value2) {
      return Math.max(value1, value2);
    }
  };

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
  private final PersistentSortedMap<String, Integer> vars;
  private final Multiset<Pair<String, FormulaList>> funcs;

  private final PersistentSortedMap<String, CType> varTypes;
  private final Map<Pair<String, FormulaList>, CType> types;

  // Cache hashCode of potentiall big map
  private final int varsHashCode;

  private SSAMap(PersistentSortedMap<String, Integer> vars,
                 int varsHashCode,
                 Multiset<Pair<String, FormulaList>> funcs,
                 PersistentSortedMap<String, CType> varTypes,
                 Map<Pair<String, FormulaList>, CType> types) {
    this.vars = vars;
    this.funcs = funcs;
    this.varTypes = varTypes;
    this.types = types;

    if (varsHashCode == 0) {
      this.varsHashCode = vars.hashCode();
    } else {
      this.varsHashCode = varsHashCode;
      assert varsHashCode == vars.hashCode();
    }
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
      return INDEX_NOT_CONTAINED;
    }
  }

  private static <T> int getIndex(T key, PersistentMap<T, Integer> map) {
    Integer i = map.get(key);
    if (i != null) {
      return i;
    } else {
      // no index found, return -1
      return INDEX_NOT_CONTAINED;
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

  public CType getType(String name) {
    return varTypes.get(name);
  }

  public SortedSet<String> allVariables() {
    return vars.keySet();
  }

  public SortedSet<Map.Entry<String, CType>> allVariablesWithTypes() {
    return varTypes.entrySet();
  }

  public Iterable<Pair<Variable, FormulaList>> allFunctions() {
    return allFunctions(types);
  }

  static Iterable<Pair<Variable, FormulaList>> allFunctions(final Map<Pair<String, FormulaList>, CType> types) {
    return FluentIterable.from(types.entrySet())
        .transform(
            new Function<Map.Entry<Pair<String, FormulaList>, CType>, Pair<Variable, FormulaList>>() {
              @Override
              public Pair<Variable, FormulaList> apply(Map.Entry<Pair<String, FormulaList>, CType> pInput) {
                return Pair.of(Variable.create(pInput.getKey().getFirst(), pInput.getValue()), pInput.getKey().getSecond());
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
    return (31 + funcs.hashCode()) * 31 + varsHashCode;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    } else if (!(obj instanceof SSAMap)) {
      return false;
    } else {
      SSAMap other = (SSAMap)obj;
      // Do a few cheap checks before the expensive ones.
      return varsHashCode == other.varsHashCode
          && funcs.entrySet().size() == other.funcs.entrySet().size()
          && vars.equals(other.vars)
          && funcs.equals(other.funcs);
    }
  }
}
