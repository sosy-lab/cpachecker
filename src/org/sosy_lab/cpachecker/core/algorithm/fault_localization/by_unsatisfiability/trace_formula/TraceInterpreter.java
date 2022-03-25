// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula;

import org.sosy_lab.java_smt.api.BooleanFormula;

/** Transform object of {@link Trace} to a {@link BooleanFormula} in a given way. */
@FunctionalInterface
public interface TraceInterpreter {

  BooleanFormula interpret(Trace pTrace);
}
