// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula;

import com.google.common.annotations.VisibleForTesting;
import com.google.errorprone.annotations.Immutable;
import java.io.Serializable;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.common.collect.PersistentSortedMaps;

/** Generator of new fresh to-be-returned values for {@link SSAMap}. */
@Immutable
public final class FreshValueProvider implements Serializable {

  private static final long serialVersionUID = 12359384095345L;

  // Default difference for two SSA-indexes of the same name.
  @VisibleForTesting static final int DEFAULT_INCREMENT = 1;

  private final PersistentSortedMap<String, Integer> vars;

  public FreshValueProvider() {
    vars = PathCopyingPersistentTreeMap.of();
  }

  public FreshValueProvider(PersistentSortedMap<String, Integer> diffVars) {
    vars = diffVars;
  }

  /** Get a new unused value based on the given one. */
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
   */
  public FreshValueProvider merge(FreshValueProvider other) {
    if (vars.isEmpty() && other.vars.isEmpty()) {
      return this;
    }
    return new FreshValueProvider(
        PersistentSortedMaps.merge(
            vars, other.vars, PersistentSortedMaps.getMaximumMergeConflictHandler()));
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
