/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.predicate;

import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.common.collect.PersistentSortedMaps;
import org.sosy_lab.cpachecker.util.predicates.pathformula.FreshValueProvider;

import com.google.common.annotations.VisibleForTesting;

public class BAMFreshValueProvider implements FreshValueProvider {

  private PersistentSortedMap<String, Integer> vars;

  @VisibleForTesting
  public BAMFreshValueProvider() {
    vars = PathCopyingPersistentTreeMap.of();
  }

  private BAMFreshValueProvider(final PersistentSortedMap<String, Integer> diffVars) {
    this.vars = diffVars;
  }

  @Override
  public int getFreshValue(String variable, int value) {
    Integer currentValue = vars.get(variable);
    if (currentValue != null && value < currentValue) {
      value = currentValue;
    }
    return value + DefaultFreshValueProvider.DEFAULT_INCREMENT; // increment for a new index
  }

  @Override
  public FreshValueProvider merge(final FreshValueProvider other) {
    if (other instanceof DefaultFreshValueProvider) {
      return this;
    } else if (other instanceof BAMFreshValueProvider) {
      PersistentSortedMap<String, Integer> vars =
          PersistentSortedMaps.merge(
              this.vars,
              ((BAMFreshValueProvider) other).vars,
              PersistentSortedMaps.<String, Integer>getMaximumMergeConflictHandler());
      return new BAMFreshValueProvider(vars);
    } else {
      throw new AssertionError("unhandled case for FreshValueProvider: " + other.getClass());
    }
  }

  public void put(String variable, int index) {
    vars = vars.putAndCopy(variable, index);
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof BAMFreshValueProvider && this.vars.equals(((BAMFreshValueProvider)other).vars);
  }

  @Override
  public int hashCode() {
    return vars.hashCode();
  }
}
