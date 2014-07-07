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

import java.io.Serializable;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Triple;
import org.sosy_lab.common.collect.Collections3;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.common.collect.PersistentSortedMaps;
import org.sosy_lab.common.collect.PersistentSortedMaps.MergeConflictHandler;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

import com.google.common.base.Equivalence;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

/**
 * Maps a variable name to its latest "SSA index", that should be used when
 * referring to that variable
 */
public class SSAMap implements Serializable {

  private static final long serialVersionUID = 7618801653203679876L;

  // Default value for the default value :p
  static final int DEFAULT_DEFAULT_IDX = -1;

  private final int defaultValue;

  private static MergeConflictHandler<String, CType> TYPE_CONFLICT_CHECKER = new MergeConflictHandler<String, CType>() {
    @Override
    public CType resolveConflict(String name, CType type1, CType type2) {
      Preconditions.checkArgument(
          type1 instanceof CFunctionType || type2 instanceof CFunctionType
          || (isEnumPointerType(type1) && isEnumPointerType(type2))
          || type1.equals(type2)
          , "Cannot change type of variable %s in SSAMap from %s to %s", name, type1, type2);

      return type1;
    }

    private boolean isEnumPointerType(CType type) {
      if (type instanceof CPointerType) {
        type = ((CPointerType) type).getType();
        return (type instanceof CComplexType) && ((CComplexType)type).getKind() == ComplexTypeKind.ENUM
            || (type instanceof CElaboratedType) && ((CElaboratedType)type).getKind() == ComplexTypeKind.ENUM;
      }
      return false;
    }
  };

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
      return ssa.getIndex(variable);
    }

    public CType getType(String name) {
      return varTypes.get(name);
    }

    public void setIndex(String name, CType type, int idx) {
      Preconditions.checkArgument(idx > 0, "Indices need to be positive for this SSAMap implementation!");
      int oldIdx = getIndex(name);
      Preconditions.checkArgument(idx >= oldIdx, "SSAMap updates need to be strictly monotone!");

      type = type.getCanonicalType();
      CType oldType = varTypes.get(name);
      if (oldType != null) {
        TYPE_CONFLICT_CHECKER.resolveConflict(name, type, oldType);
      } else {
        varTypes = varTypes.putAndCopy(name, type);
      }

      if (idx > oldIdx) {
        vars = vars.putAndCopy(name, idx);

        // wait wtf that's static.
        // WAT. Which map are we dealing with?
        if (oldIdx != ssa.defaultValue) {
          varsHashCode -= mapEntryHashCode(name, oldIdx);
        }
        varsHashCode += mapEntryHashCode(name, idx);
      }
    }

    public void deleteVariable(String variable) {
      int index = getIndex(variable);
      if (index != ssa.defaultValue) {
        vars = vars.removeAndCopy(variable);
        varsHashCode -= mapEntryHashCode(variable, index);

        varTypes = varTypes.removeAndCopy(variable);
      }
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
      if (vars == ssa.vars) {
        return ssa;
      }

      ssa = new SSAMap(vars, varsHashCode, varTypes);
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
      PathCopyingPersistentTreeMap.<String, CType>of());

  /**
   * Returns an empty immutable SSAMap.
   */
  public static SSAMap emptySSAMap() {
    return EMPTY_SSA_MAP;
  }

  public SSAMap withDefault(final int defaultValue) {
    return new SSAMap(this.vars, this.varsHashCode, this.varTypes, defaultValue);
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
      // both are absolutely identical
      return Pair.of(s1, differences);

    } else {
      differences = new ArrayList<>();
      vars = PersistentSortedMaps.merge(s1.vars, s2.vars, Equivalence.equals(),
          PersistentSortedMaps.<String, Integer>getMaximumMergeConflictHandler(), differences);
    }

    PersistentSortedMap<String, CType> varTypes = PersistentSortedMaps.merge(
        s1.varTypes, s2.varTypes,
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
        TYPE_CONFLICT_CHECKER,
        null);

    return Pair.of(new SSAMap(vars, 0, varTypes), differences);
  }

  private final PersistentSortedMap<String, Integer> vars;
  private final PersistentSortedMap<String, CType> varTypes;

  // Cache hashCode of potentially big map
  private final int varsHashCode;

  private SSAMap(PersistentSortedMap<String, Integer> vars,
                 int varsHashCode,
                 PersistentSortedMap<String, CType> varTypes,
                 int defaultSSAIdx) {
    this.vars = vars;
    this.varTypes = varTypes;

    if (varsHashCode == 0) {
      this.varsHashCode = vars.hashCode();
    } else {
      this.varsHashCode = varsHashCode;
      assert varsHashCode == vars.hashCode();
    }

    defaultValue = defaultSSAIdx;
  }

  private SSAMap(PersistentSortedMap<String, Integer> vars,
                 int varsHashCode,
                 PersistentSortedMap<String, CType> varTypes) {
    this(vars, varsHashCode, varTypes, DEFAULT_DEFAULT_IDX);
  }

  /**
   * Returns a SSAMapBuilder that is initialized with the current SSAMap.
   */
  public SSAMapBuilder builder() {
    return new SSAMapBuilder(this);
  }

  /**
   * @return index of the variable in the map,
   * or the [defaultValue].
   */
  public int getIndex(String variable) {
    return vars.getOrDefault(variable, defaultValue);
  }

  public boolean containsVariable(String variable) {
    // HM I think that is correct.
    return vars.containsKey(variable);
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

  private static final Joiner joiner = Joiner.on(" ");

  @Override
  public String toString() {
    return joiner.join(vars.entrySet());
  }

  @Override
  public int hashCode() {
    return varsHashCode;
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
          && vars.equals(other.vars);
    }
  }
}
