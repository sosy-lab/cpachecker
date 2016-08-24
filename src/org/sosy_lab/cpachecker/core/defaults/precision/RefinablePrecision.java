/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.defaults.precision;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public abstract class RefinablePrecision extends VariableTrackingPrecision {

  private final VariableTrackingPrecision baseline;

  protected RefinablePrecision(VariableTrackingPrecision pBaseline) {
    super();
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
    return other != null
        && other instanceof RefinablePrecision
        && baseline.equals(((RefinablePrecision) other).baseline);
  }

  @Override
  public int hashCode() {
    return baseline.hashCode();
  }
}
