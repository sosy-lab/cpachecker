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
        return Collections.emptySet();
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
