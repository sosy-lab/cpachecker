// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * This package defines a CPA that executes a program instead of abstracting it. It wraps another
 * CPA (usually a {@link org.sosy_lab.cpachecker.cpa.composite.CompositeCPA} with a value analysis)
 * and enforces that every transition has exactly one or zero successors, i.e., that the analyzed
 * program does not make any nondeterministic choice. Under this restriction the analysis is a
 * concrete interpreter of the program and its result is exact: if it terminates without reaching a
 * target state, the program is safe (and terminating), and if it reaches a target state, the
 * program violates the specification.
 */
package org.sosy_lab.cpachecker.cpa.execution;
