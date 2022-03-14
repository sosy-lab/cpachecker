// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.assumptions.genericassumptions;

import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/** Abstraction of a class that generates generic assumption invariants from CFA edges */
public interface GenericAssumptionBuilder {

  /**
   * Return a set of assumption predicate that the system assumes when it encounters the given edge.
   * The assumptions are evaluated in the pre-state of the edge.
   *
   * @return A non-null, possibly empty list of predicates representing the assumptions
   */
  Set<CExpression> assumptionsForEdge(CFAEdge edge) throws UnrecognizedCodeException;
}
