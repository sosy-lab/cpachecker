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

import com.google.common.annotations.VisibleForTesting;

import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.common.collect.PersistentSortedMaps;

import java.io.Serializable;

/**
 * Generator of new fresh to-be-returned values for {@link SSAMap}.
 **/
public class FreshValueProvider implements Serializable {

  private static final long serialVersionUID = 12359384095345L;

  // Default difference for two SSA-indexes of the same name.
  @VisibleForTesting
  static final int DEFAULT_INCREMENT = 1;

  private PersistentSortedMap<String, Integer> vars;

  public FreshValueProvider() {
    vars = PathCopyingPersistentTreeMap.of();
  }

  private FreshValueProvider(PersistentSortedMap<String, Integer> diffVars) {
    this.vars = diffVars;
  }

  /**
   * Get a new unused value based on the given one.
   **/
  int getFreshValue(String variable, int value) {
    Integer currentValue = vars.get(variable);
    if (currentValue != null && value < currentValue) {
      value = currentValue;
    }
    return value + DEFAULT_INCREMENT;
  }

  /**
   * Get a new provider, that is based on the current one and the given one.
   *
   * <p>Keeps the maximum to-be-generated index for each variable.
   **/
  public FreshValueProvider merge(FreshValueProvider other) {
    if (vars.isEmpty() && other.vars.isEmpty()) {
      return this;
    }
    PersistentSortedMap<String, Integer> vars =
        PersistentSortedMaps.merge(
            this.vars, other.vars,
            PersistentSortedMaps.getMaximumMergeConflictHandler());
    return new FreshValueProvider(vars);
  }

  public void put(String variable, int index) {
    vars = vars.putAndCopy(variable, index);
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof FreshValueProvider
        && ((other == this) || vars.equals(((FreshValueProvider) other).vars));
  }

  @Override
  public int hashCode() {
    return vars.hashCode();
  }
}
