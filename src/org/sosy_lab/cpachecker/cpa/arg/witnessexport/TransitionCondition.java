// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg.witnessexport;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.KeyDef;

/** an immutable map of facts about the transition. */
public class TransitionCondition implements Comparable<TransitionCondition> {

  private static final TransitionCondition EMPTY = new TransitionCondition();

  private final EnumMap<KeyDef, String> keyValues;

  private final Scope scope;

  private int hashCode = 0;

  private TransitionCondition() {
    keyValues = new EnumMap<>(KeyDef.class);
    scope = new Scope(Optional.empty(), ImmutableMap.of());
  }

  private TransitionCondition(EnumMap<KeyDef, String> pKeyValues, Scope pScope) {
    keyValues = pKeyValues;
    scope = pScope;
  }

  public TransitionCondition putAndCopy(final KeyDef pKey, final String pValue) {
    if (pValue.equals(keyValues.get(pKey))) {
      return this;
    }
    EnumMap<KeyDef, String> newMap = keyValues.clone();
    newMap.put(pKey, pValue);
    return new TransitionCondition(newMap, scope);
  }

  public TransitionCondition putAllAndCopy(TransitionCondition tc) {
    Optional<Scope> newScope = scope.mergeWith(tc.scope);
    checkArgument(newScope.isPresent(), "Cannot merge transitions due to conflicting scopes.");
    EnumMap<KeyDef, String> newMap = null;
    for (Entry<KeyDef, String> e : keyValues.entrySet()) {
      if (!tc.keyValues.containsKey(e.getKey())) {
        if (newMap == null) {
          newMap = tc.keyValues.clone();
        }
        newMap.put(e.getKey(), e.getValue());
      }
    }
    return newMap == null ? tc : new TransitionCondition(newMap, newScope.orElseThrow());
  }

  public TransitionCondition removeAndCopy(final KeyDef pKey) {
    if (keyValues.containsKey(pKey)) {
      EnumMap<KeyDef, String> newMap = keyValues.clone();
      newMap.remove(pKey);
      return new TransitionCondition(newMap, scope);
    } else {
      return this;
    }
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    return pOther instanceof TransitionCondition
        && keyValues.equals(((TransitionCondition) pOther).keyValues)
        && scope.equals(((TransitionCondition) pOther).scope);
  }

  public Map<KeyDef, String> getMapping() {
    return keyValues;
  }

  public Scope getScope() {
    return scope;
  }

  public boolean hasTransitionRestrictions() {
    return !keyValues.isEmpty();
  }

  @Override
  public int hashCode() {
    if (hashCode == 0) {
      hashCode = keyValues.hashCode() * 31 + scope.hashCode();
    }
    return hashCode;
  }

  @Override
  public String toString() {
    return keyValues.toString();
  }

  public boolean summarizes(TransitionCondition pLabel) {
    if (keyValues.containsKey(KeyDef.FUNCTIONENTRY)
        || keyValues.containsKey(KeyDef.FUNCTIONEXIT)
        || pLabel.keyValues.containsKey(KeyDef.FUNCTIONENTRY)
        || pLabel.keyValues.containsKey(KeyDef.FUNCTIONEXIT)) {
      return false;
    }
    if (equals(pLabel)) {
      return true;
    }
    boolean ignoreAssumptionScope =
        !keyValues.containsKey(KeyDef.ASSUMPTION)
            || !pLabel.keyValues.containsKey(KeyDef.ASSUMPTION);
    boolean ignoreInvariantScope =
        !keyValues.containsKey(KeyDef.INVARIANT) || !pLabel.keyValues.containsKey(KeyDef.INVARIANT);

    final EnumSet<KeyDef> keyDefs;
    if (!keyValues.isEmpty()) {
      keyDefs = EnumSet.copyOf(keyValues.keySet());
      keyDefs.addAll(pLabel.keyValues.keySet());
    } else {
      keyDefs = EnumSet.copyOf(pLabel.keyValues.keySet());
    }
    keyDefs.remove(KeyDef.ASSUMPTION);
    keyDefs.remove(KeyDef.INVARIANT);
    if (ignoreAssumptionScope) {
      keyDefs.remove(KeyDef.ASSUMPTIONSCOPE);
      keyDefs.remove(KeyDef.ASSUMPTIONRESULTFUNCTION);
    }
    if (ignoreInvariantScope) {
      keyDefs.remove(KeyDef.INVARIANTSCOPE);
    }

    for (KeyDef keyDef : keyDefs) {
      if (!Objects.equals(keyValues.get(keyDef), pLabel.keyValues.get(keyDef))) {
        return false;
      }
    }

    if (!scope.mergeWith(pLabel.scope).isPresent()) {
      return false;
    }

    return true;
  }

  @Override
  public int compareTo(TransitionCondition pO) {
    if (this == pO) {
      return 0;
    }
    Iterator<Map.Entry<KeyDef, String>> entryIterator = keyValues.entrySet().iterator();
    Iterator<Map.Entry<KeyDef, String>> otherEntryIterator = pO.keyValues.entrySet().iterator();
    while (entryIterator.hasNext() && otherEntryIterator.hasNext()) {
      Map.Entry<KeyDef, String> entry = entryIterator.next();
      Map.Entry<KeyDef, String> otherEntry = otherEntryIterator.next();
      int compKey = entry.getKey().compareTo(otherEntry.getKey());
      if (compKey != 0) {
        return compKey;
      }
      int compVal = entry.getValue().compareTo(otherEntry.getValue());
      if (compVal != 0) {
        return compVal;
      }
    }
    if (!entryIterator.hasNext()) {
      return -1;
    }
    if (!otherEntryIterator.hasNext()) {
      return 1;
    }
    return scope.compareTo(pO.scope);
  }

  public static TransitionCondition empty() {
    return EMPTY;
  }

  static class Scope implements Comparable<Scope> {

    private final Optional<String> functionName;

    private final ImmutableMap<String, ASimpleDeclaration> usedDeclarations;

    private Scope(
        Optional<String> pFunctionName, Map<String, ASimpleDeclaration> pUsedDeclarations) {
      functionName = pFunctionName;
      usedDeclarations = ImmutableMap.copyOf(pUsedDeclarations);
      for (ASimpleDeclaration decl : pUsedDeclarations.values()) {
        if (decl instanceof AVariableDeclaration) {
          AVariableDeclaration varDecl = (AVariableDeclaration) decl;
          checkArgument(
              varDecl.isGlobal() || functionName.isPresent(),
              "Cannot create a global scope with non-global variable declarations.");
        }
      }
    }

    public boolean isGlobal() {
      return !functionName.isPresent();
    }

    public String getFunctionName() {
      return functionName.orElseThrow();
    }

    public Collection<ASimpleDeclaration> getUsedDeclarations() {
      return usedDeclarations.values();
    }

    @Override
    public int hashCode() {
      return usedDeclarations.hashCode() * 31 + functionName.hashCode();
    }

    @Override
    public boolean equals(Object pOther) {
      if (this == pOther) {
        return true;
      }
      if (pOther instanceof Scope) {
        Scope other = (Scope) pOther;
        return functionName.equals(other.functionName)
            && usedDeclarations.equals(other.usedDeclarations);
      }
      return false;
    }

    @Override
    public String toString() {
      final String prefix;
      if (isGlobal()) {
        prefix = "";
      } else {
        prefix = getFunctionName() + ": ";
      }
      return prefix + usedDeclarations;
    }

    @Override
    public int compareTo(Scope pOther) {
      return ComparisonChain.start()
          .compareTrueFirst(isGlobal(), pOther.isGlobal())
          .compare(functionName.orElseThrow(), pOther.functionName.orElseThrow())
          .compare(
              usedDeclarations,
              pOther.usedDeclarations,
              (a, b) -> {
                Iterator<Map.Entry<String, ASimpleDeclaration>> aIt = a.entrySet().iterator();
                Iterator<Map.Entry<String, ASimpleDeclaration>> bIt = b.entrySet().iterator();
                while (aIt.hasNext() && bIt.hasNext()) {
                  Map.Entry<String, ASimpleDeclaration> aEntry = aIt.next();
                  Map.Entry<String, ASimpleDeclaration> bEntry = bIt.next();
                  int entryComp =
                      ComparisonChain.start()
                          .compare(aEntry.getKey(), bEntry.getKey())
                          .compare(
                              aEntry.getValue().getQualifiedName(),
                              bEntry.getValue().getQualifiedName())
                          .result();
                  if (entryComp != 0) {
                    return entryComp;
                  }
                }
                if (aIt.hasNext()) {
                  return -1;
                }
                if (bIt.hasNext()) {
                  return 1;
                }
                return 0;
              })
          .result();
    }

    public Optional<Scope> extendBy(
        Optional<String> pNewFunctionName, Collection<ASimpleDeclaration> pUsedDeclarations) {
      final Optional<String> newFunctionName;
      if (pNewFunctionName.isPresent()) {
        if (functionName.isPresent()
            && !functionName.orElseThrow().equals(pNewFunctionName.orElseThrow())) {
          return Optional.empty();
        }
        if (!functionName.isPresent() && !usedDeclarations.isEmpty()) {
          return Optional.empty();
        }
        newFunctionName = pNewFunctionName;
      } else {
        if (pUsedDeclarations.isEmpty()) {
          return Optional.of(this);
        }
        newFunctionName = functionName;
      }
      Map<String, ASimpleDeclaration> newUsed = new TreeMap<>(usedDeclarations);
      for (ASimpleDeclaration decl : pUsedDeclarations) {
        boolean isGlobalVarDecl = isGlobalVarDecl(decl);
        if (!isGlobalVarDecl && !newFunctionName.isPresent()) {
          return Optional.empty();
        }
        String potentiallyAmbiguousName = decl.getOrigName();
        ASimpleDeclaration conflictingDecl = newUsed.get(potentiallyAmbiguousName);
        if (conflictingDecl == null) {
          newUsed.put(potentiallyAmbiguousName, decl);
        } else if (!decl.equals(conflictingDecl)) {
          return Optional.empty();
        }
      }
      return Optional.of(new Scope(newFunctionName, newUsed));
    }

    public Optional<Scope> mergeWith(Scope pOther) {
      return extendBy(pOther.functionName, pOther.getUsedDeclarations());
    }
  }

  public TransitionCondition withScope(Scope pScope) {
    if (pScope == scope) {
      return this;
    }
    return new TransitionCondition(keyValues, pScope);
  }

  private static boolean isGlobalVarDecl(@Nullable ASimpleDeclaration pDecl) {
    if (pDecl instanceof AVariableDeclaration) {
      AVariableDeclaration varDecl = (AVariableDeclaration) pDecl;
      return varDecl.isGlobal();
    }
    return false;
  }
}
