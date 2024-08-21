// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * This package contains all components necessary for performing a CEGAR refinement based on the
 * concept of 'Trace Abstraction' as described in the following two papers:
 *
 * <ul>
 *   <li>"Refinement of Trace Abstraction" [Heizmann, Hoenicke, Podelski]<br>
 *       (DOI: 10.1007/978-3-642-03237-0_7), and
 *   <li>"Software Model Checking for People Who Love Automata" [Heizmann, Hoenicke, Podelski]<br>
 *       (DOI: 10.1007/978-3-642-39799-8_2).
 * </ul>
 *
 * <p>The papers describe how each CEGAR iteration introduces a new finite automaton that recognizes
 * a set of infeasible traces. These automata are composed out of Craig interpolants, which in term
 * are the result of counterexample traces which were already proven infeasible before.
 *
 * <p>In context of CPAchecker the interpolation automata are not eagerly computed during a
 * refinement, but instead a new CPA is added (the TraceAbstractionCPA) in which the successor
 * states are only computed on the fly. The computation is likewise based on the interpolants and
 * and in that way mimics the behavior of an interpolation-automaton.
 */
@javax.annotation.ParametersAreNonnullByDefault
@org.sosy_lab.common.annotations.FieldsAreNonnullByDefault
@org.sosy_lab.common.annotations.ReturnValuesAreNonnullByDefault
package org.sosy_lab.cpachecker.cpa.traceabstraction;
