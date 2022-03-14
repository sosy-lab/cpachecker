// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * The SLAB CPA is based on the Software Model Checker SLAB from the paper Slicing Abstractions
 *
 * <p>SLAB works by constructing an initial abstraction containing a state for each combination of
 * the special predicates init and error. This model is then refined in a CEGAR loop where the
 * states are split according to the interpolants found for the infeasible counterexample trace.The
 * locations are not tracked by a Location CPA, so the information about the program counter is
 * encoded symbolically into the path/state formulas. This is the reason why we need a special CPA
 * and cannot simply use the Predicate CPA with a special refinement strategy (as is the case for
 * Kojak)
 */
@javax.annotation.ParametersAreNonnullByDefault
@org.sosy_lab.common.annotations.FieldsAreNonnullByDefault
@org.sosy_lab.common.annotations.ReturnValuesAreNonnullByDefault
package org.sosy_lab.cpachecker.cpa.slab;
