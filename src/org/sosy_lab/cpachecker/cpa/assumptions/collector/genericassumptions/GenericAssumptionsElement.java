/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.assumptions.collector.genericassumptions;

import org.sosy_lab.cpachecker.util.assumptions.AssumptionReportingElement;
import org.sosy_lab.cpachecker.util.assumptions.AssumptionWithLocation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;

/**
 * Abstract element for the generic assumption generator CPA;
 * encapsulate a symbolic formula that represents the
 * assumption.
 *
 * @author g.theoduloz
 */
public class GenericAssumptionsElement implements AbstractElement, AssumptionReportingElement {

  // The inner representation is a formula.
  private final AssumptionWithLocation assumption;

  public GenericAssumptionsElement(AssumptionWithLocation anAssumption)
  {
    assumption = anAssumption;
  }

  @Override
  public AssumptionWithLocation getAssumptionWithLocation()
  {
    if (assumption == null)
      return AssumptionWithLocation.TRUE;
    else
      return assumption;
  }

  /**
   * @param other an other abstract element <b>with the same manager</b>
   * @return an abstract element representing the conjunction of
   *         the formula of this element with the other element
   */
  public GenericAssumptionsElement makeAnd(GenericAssumptionsElement other)
  {
    // Special cases
    if (this == TOP) return other;
    if (other == TOP) return this;
    if ((this == BOTTOM) || (other == BOTTOM)) return BOTTOM;

    return new GenericAssumptionsElement(assumption.and(other.assumption));
  }

  @Override
  public boolean isError() {
    return false;
  }

  @Override
  public boolean equals(Object pObj) {
    if (pObj instanceof GenericAssumptionsElement)
      return assumption.equals(((GenericAssumptionsElement)pObj).assumption);
    else
      return false;
  }

  @Override
  public String toString() {
    return assumption.toString();
  }

  public static final GenericAssumptionsElement TOP = new GenericAssumptionsElement(AssumptionWithLocation.TRUE);
  public static final GenericAssumptionsElement BOTTOM = new GenericAssumptionsElement(AssumptionWithLocation.FALSE);
}
