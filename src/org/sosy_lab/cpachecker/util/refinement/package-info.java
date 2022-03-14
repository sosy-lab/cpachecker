// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * Utilities for refinement and refinement selection.
 *
 * <p>Contains Generic* classes which can be used for composing a simple refinement based on
 * refinement for abstract variable assignments. Most of these are only dependent on interfaces
 * {@link org.sosy_lab.cpachecker.util.refinement.StrongestPostOperator StrongestPostOperator},
 * {@link org.sosy_lab.cpachecker.util.refinement.Interpolant Interpolant} , {@link
 * org.sosy_lab.cpachecker.util.refinement.InterpolantManager InterpolantManager} and {@link
 * org.sosy_lab.cpachecker.util.refinement.ForgetfulState ForgetfulState}. By defining
 * implementations for these four interfaces, one can define a complete refinement using the
 * Generic* classes.
 */
package org.sosy_lab.cpachecker.util.refinement;
