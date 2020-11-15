// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.symbolic.util;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;

/**
 * This alias creator only allows the identity as a valid alias.
 */
public class IdentityAliasCreator implements AliasCreator {

  @Override
  public Set<Environment> getPossibleAliases(
      final Collection<? extends SymbolicValue> pFirstValues,
      final Collection<? extends SymbolicValue> pSecondValues
  ) {

    Collection<? extends SymbolicValue> biggerState;
    Collection<? extends SymbolicValue> smallerState;

    if (pFirstValues.size() > pSecondValues.size()) {
      biggerState = pFirstValues;
      smallerState = pSecondValues;
    } else {
      biggerState = pSecondValues;
      smallerState = pFirstValues;
    }

    for (SymbolicValue v : smallerState) {
      if (!biggerState.contains(v)) {
        return ImmutableSet.of();
      }
    }

    return Collections.singleton(buildEnvironment(smallerState));
  }

  private Environment buildEnvironment(Collection<? extends SymbolicValue> pValues) {
    Environment e = new Environment();

    Collection<SymbolicIdentifier> allIds = SymbolicValues.getContainedSymbolicIdentifiers(pValues);

    for (SymbolicIdentifier i : allIds) {
      e.addAlias(i, i);
    }

    for (SymbolicValue v : pValues) {
      e.addCounterpart(v, v);
    }

    return e;
  }
}
