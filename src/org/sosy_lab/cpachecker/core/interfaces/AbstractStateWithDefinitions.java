// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.interfaces;

import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslLogicDefinition;

/**
 * Sub-interface for {@link AbstractState}s that marks states with logic definitions. This is
 * intended for CPAs to use in their strengthen operator, e.g. to retrieve definitions from
 * witnesses used in invariants.
 */
public interface AbstractStateWithDefinitions extends AbstractState {

  /**
   * Get the set of logic definitions represented as {@link AcslLogicDefinition}s. This is used to
   * retrieve definitions from witnesses used in invariants for example.
   *
   * <p>Implementors should make sure that only definitions are returned which do not occur in the
   * CFA or invariants.
   *
   * @return A (possibly empty set) of logic definitions.
   */
  Set<? extends AcslLogicDefinition> getLogicDefinitions();
}
