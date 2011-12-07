/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.interfaces.conditions;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;

/**
 * Interface to implement in order for an abstract element to be able to
 * make the system generate an assumption to avoid re-considering
 * this node.
 */
public interface AvoidanceReportingElement extends AbstractElement {

  /**
   * Returns true if an invariant must be added so as to avoid
   * the given state in the future.
   */
  public boolean mustDumpAssumptionForAvoidance();

  /**
   * If {@link #mustDumpAssumptionForAvoidance()} returned true, this method
   * returns a formula that provides an explanation. This formula may not be TRUE.
   * If the element cannot provide such a formula, it SHOULD return FALSE.
   * If {@link #mustDumpAssumptionForAvoidance()} returned false, this method
   * SHOULD return TRUE.
   */
  public Formula getReasonFormula(FormulaManager mgr);

}
