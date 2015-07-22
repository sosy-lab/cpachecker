/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.value.symbolic.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;

/**
 * Classes implementing this interface allow the creation of aliases for two sets of symbolic values,
 * so that the first set of symbolic values equals the second set of symbolic values, if each
 * symbolic identifier is replaced with its alias.
 */
public interface AliasCreator {

  Set<Environment> getPossibleAliases(
      Collection<? extends SymbolicValue> pFirstValues,
      Collection<? extends SymbolicValue> pSecondValues
  );

  /**
   * Environment for comparison of sets of symbolic values. An environment contains
   * aliases for {@link SymbolicIdentifier}s and counterparts of {@link SymbolicValue}s.
   */
  class Environment {
    private Map<SymbolicIdentifier, SymbolicIdentifier> aliases = new HashMap<>();
    private Map<SymbolicValue, SymbolicValue> counterparts = new HashMap<>();

    Environment() { }

    Environment(final Environment pEnvironmentToClone) {
      aliases = new HashMap<>(pEnvironmentToClone.aliases);
      counterparts = new HashMap<>(pEnvironmentToClone.counterparts);
    }

    /**
     * Joins this environment with the given one.
     * The resulting environment will contain all aliases and counterparts of this and the given
     * environment.
     * If two different aliases/counterparts exist for one identifier/expression, the join of the
     * environments is impossible and <code>null</code> is returned.
     *
     * @param pOther the environment to join with this one
     * @return the join of this environment and the given one
     */
    Environment join(final Environment pOther) {
      Environment newEnv = new Environment(this);

      // add the other environment's aliases
      for (Map.Entry<SymbolicIdentifier, SymbolicIdentifier> entry : pOther.aliases.entrySet()) {
        SymbolicIdentifier key = entry.getKey();

        if (newEnv.aliases.containsKey(key)
            && !SymbolicValues.representSameSymbolicMeaning(newEnv.aliases.get(key), entry.getValue())) {
          return null;
        }

        newEnv.aliases.put(key, entry.getValue());
      }

      // add the other environment's counterparts
      for (Map.Entry<SymbolicValue, SymbolicValue> entry : pOther.counterparts.entrySet()) {
        SymbolicValue key = entry.getKey();

        if (newEnv.counterparts.containsKey(key)
            && !SymbolicValues.representSameSymbolicMeaning(newEnv.counterparts.get(key), entry.getValue())) {
          return null;
        }

        newEnv.counterparts.put(key, entry.getValue());
      }

      return newEnv;
    }

    public SymbolicValue getCounterpart(final SymbolicValue pExp) {
      return counterparts.get(pExp);
    }

    public SymbolicIdentifier getAlias(final SymbolicIdentifier pIdentifier) {
      return aliases.get(pIdentifier);
    }

    /**
     * Returns whether the given symbolic value has a counterpart in this environment.
     * A counterpart can be the equal expression or any expression of the same form, but with
     * different symbolic identifiers (which have to be aliases in this environment).
     *
     * @param pExpressionOfFirstGraph the expression to check for a counterpart
     * @return <code>true</code> if a counterpart exists in this environment
     */
    public boolean hasCounterpart(final SymbolicValue pExpressionOfFirstGraph) {
      return counterparts.containsKey(pExpressionOfFirstGraph);
    }

    public boolean isCounterpart(final SymbolicValue pExpression) {
      return counterparts.containsValue(pExpression);
    }

    public boolean hasAlias(final SymbolicIdentifier pIdentifier) {
      return aliases.containsKey(pIdentifier);
    }

    public void addAlias(final SymbolicIdentifier pIdentifier, final SymbolicIdentifier pAlias) {
      aliases.put(pIdentifier, pAlias);
    }

    public void addCounterpart(final SymbolicValue pValue, final SymbolicValue pCounterpart) {
      counterparts.put(pValue, pCounterpart);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      Environment that = (Environment) o;

      if (!aliases.equals(that.aliases)) {
        return false;
      }
      if (!counterparts.equals(that.counterparts)) {
        return false;
      }

      return true;
    }

    @Override
    public int hashCode() {
      int result = aliases.hashCode();
      result = 31 * result + counterparts.hashCode();
      return result;
    }
  }
}
