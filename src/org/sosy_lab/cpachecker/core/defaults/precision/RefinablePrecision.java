// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.defaults.precision;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public abstract class RefinablePrecision extends VariableTrackingPrecision {

  private static final long serialVersionUID = 1L;

  private final VariableTrackingPrecision baseline;

  protected RefinablePrecision(VariableTrackingPrecision pBaseline) {
    baseline = pBaseline;
  }

  @Override
  public final boolean allowsAbstraction() {
    return true;
  }

  @Override
  public boolean isTracking(MemoryLocation pVariable, Type pType, CFANode pLocation) {
    checkNotNull(pVariable);
    checkNotNull(pType);
    checkNotNull(pLocation);
    return baseline.isTracking(pVariable, pType, pLocation);
  }

  protected VariableTrackingPrecision getBaseline() {
    return baseline;
  }

  @Override
  @SuppressWarnings("ForOverride")
  protected final Class<? extends ConfigurableProgramAnalysis> getCPAClass() {
    return baseline.getCPAClass();
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof RefinablePrecision
        && baseline.equals(((RefinablePrecision) other).baseline);
  }

  @Override
  public int hashCode() {
    return baseline.hashCode();
  }
}
