// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * This package contains an additional Prover-wrapper, which uses constraints to improve formulas
 * with uninterpreted functions.
 *
 * <p>We use UFs to model some operations and let the solver choose arbitrary values
 * (over-approximation!) for the result of the UF. If a formula is UNSAT, we can ignore the UF.
 * Otherwise we try to compute a better result with the {@link
 * org.sosy_lab.cpachecker.util.predicates.ufCheckingProver.FunctionApplicationManager} and add an
 * additional constraint for the UF. This iteratively improves the solver's model.
 *
 * <p>The {@link
 * org.sosy_lab.cpachecker.util.predicates.ufCheckingProver.FunctionApplicationManager} depends on
 * the program's analysis and matches the precise operations that are over-approximated with UFs.
 */
package org.sosy_lab.cpachecker.util.predicates.ufCheckingProver;
