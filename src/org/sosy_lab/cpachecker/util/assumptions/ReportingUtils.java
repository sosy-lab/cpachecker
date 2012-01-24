/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.assumptions;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingElement;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;

/**
 * Static methods used as helpers to manipulate elements implementing
 * FormulaReportingElement.
 */
public class ReportingUtils {

  /** Cannot have instances */
  private ReportingUtils() {}

  /**
   * Returns a predicate representing states represented by
   * the given abstract element, according to reported
   * formulas
   */
  public static Formula extractReportedFormulas(FormulaManager manager, AbstractElement element) {
    Formula result = manager.makeTrue();

    // traverse through all the sub-elements contained in this element
    for (AbstractElement e : AbstractElements.asIterable(element)) {

      // If the element can be approximated by a formula, conjunct its approximation
      if (e instanceof FormulaReportingElement) {
        FormulaReportingElement repel = (FormulaReportingElement) e;
        result = manager.makeAnd(result, repel.getFormulaApproximation(manager));
      }
    }

    return result;
  }
}
