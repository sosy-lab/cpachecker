// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.constraints.refiner.precision;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;

/** Refinable {@link ConstraintsPrecision}. */
@Options(prefix = "cpa.constraints.refinement")
public class RefinableConstraintsPrecision implements ConstraintsPrecision {

  public enum PrecisionType {
    CONSTRAINTS,
    LOCATION
  }

  @Option(
      description =
          "Type of precision to use. Has to be LOCATION if"
              + " PredicateExtractionRefiner is used.",
      toUppercase = true)
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
