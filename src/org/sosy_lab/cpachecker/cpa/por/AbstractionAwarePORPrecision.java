// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.por;

import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

class AbstractionAwarePORPrecision extends PORPrecision {

  AbstractionAwarePORPrecision(Precision pWrappedPrecision) {
    super(pWrappedPrecision);
  }

  @Override
  protected PORPrecision withWrappedPrecision(Precision newWrappedPrecision) {
    return new AbstractionAwarePORPrecision(newWrappedPrecision);
  }

  @Override
  public boolean canIgnoreVariable(MemoryLocation memoryLocation) {
    if (getWrappedPrecision() instanceof PredicatePrecision predicatePrecision) {
      if (!predicatePrecision.getLocalPredicates().isEmpty() ||
          !predicatePrecision.getFunctionPredicates().isEmpty() ||
          !predicatePrecision.getLocationInstancePredicates().isEmpty()) {
        return false;
      }

      var globalPredicates = predicatePrecision.getGlobalPredicates();
      for (var predicate : globalPredicates) {
        // TODO
      }

      return true;
    }
    return false;
  }
}
