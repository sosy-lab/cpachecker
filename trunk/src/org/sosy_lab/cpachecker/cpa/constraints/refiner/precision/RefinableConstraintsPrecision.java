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
package org.sosy_lab.cpachecker.cpa.constraints.refiner.precision;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;

/**
 * Refinable {@link ConstraintsPrecision}.
 */
@Options(prefix = "cpa.constraints.refinement")
public class RefinableConstraintsPrecision implements ConstraintsPrecision {

  public enum PrecisionType { CONSTRAINTS, LOCATION }

  @Option(description = "Type of precision to use. Has to be LOCATION if"
      + " PredicateExtractionRefiner is used.", toUppercase = true)
  private PrecisionType precisionType = PrecisionType.CONSTRAINTS;

  private final ConstraintsPrecision delegate;

  public RefinableConstraintsPrecision(final Configuration pConfig)
      throws InvalidConfigurationException {
    pConfig.inject(this);

    switch (precisionType) {
      case CONSTRAINTS:
        delegate = ConstraintBasedConstraintsPrecision.getEmptyPrecision();
        break;
      case LOCATION:
        delegate = LocationBasedConstraintsPrecision.getEmptyPrecision();
        break;
      default:
        throw new AssertionError("Unhandled precision type " + precisionType);
    }
  }

  private RefinableConstraintsPrecision(final ConstraintsPrecision pDelegate) {
    delegate = pDelegate;
  }

  @Override
  public boolean isTracked(Constraint pConstraint, CFANode pLocation) {
    return delegate.isTracked(pConstraint, pLocation);
  }

  @Override
  public ConstraintsPrecision join(ConstraintsPrecision pOther) {
    assert pOther instanceof RefinableConstraintsPrecision;
    final ConstraintsPrecision otherDelegate = ((RefinableConstraintsPrecision) pOther).delegate;

    return new RefinableConstraintsPrecision(delegate.join(otherDelegate));
  }

  @Override
  public ConstraintsPrecision withIncrement(Increment pIncrement) {
    return new RefinableConstraintsPrecision(delegate.withIncrement(pIncrement));
  }

  @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
  @Override
  public boolean equals(Object o) {
    return delegate.equals(o);
  }

  @Override
  public int hashCode() {
    int result = precisionType.hashCode();
    result = 31 * result + delegate.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return delegate.toString();
  }
}
