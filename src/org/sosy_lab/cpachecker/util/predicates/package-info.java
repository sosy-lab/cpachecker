// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/** Dealing with formulas: solvers interfaces, creating formulas from code, etc. */
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
    value = {"SE_BAD_FIELD", "SE_TRANSIENT_FIELD_NOT_RESTORED"},
    justification = "serialization of formulas is currently unsupported")
package org.sosy_lab.cpachecker.util.predicates;
