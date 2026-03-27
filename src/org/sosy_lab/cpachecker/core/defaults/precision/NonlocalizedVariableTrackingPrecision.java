// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.defaults.precision;

import com.google.common.collect.Multimap;
import java.util.Collection;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public abstract class NonlocalizedVariableTrackingPrecision extends VariableTrackingPrecision {

  public abstract boolean isTracking(MemoryLocation variable, Type type);

  @Override
  public boolean isTracking(MemoryLocation variable, Type type, CFANode location) {
    return isTracking(variable, type);
  }

  public abstract NonlocalizedVariableTrackingPrecision withIncrement(Collection<MemoryLocation> increment);

  @Override
  public VariableTrackingPrecision withIncrement(Multimap<CFANode, MemoryLocation> increment) {
    return withIncrement(increment.values());
  }
}
