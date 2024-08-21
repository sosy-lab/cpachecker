// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * This package defines a CPA that determines the level of (non-)determism of a program, by
 * inspecting how many assume edges can be evaluated to a concrete result, and how many not This
 * should be a good indicator for whether using refinement for the value analysis or not (e.g.
 * product lines are quite deterministic, and work very good without refinement, while it is the
 * other way round for most ldv* tasks)
 */
package org.sosy_lab.cpachecker.cpa.deterministic;
