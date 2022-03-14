// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * The Octagon CPA is based on the Octagon Abstract Domain library.
 *
 * <p>The octagon abstract domain is a weakly relational numerical domain. It is based on
 * Difference-Bound Matrices and therefore able to handle constraints of the form
 *
 * <pre> ±X ±Y ≤ c</pre>
 *
 * with
 *
 * <pre>X</pre>
 *
 * and
 *
 * <pre>Y</pre>
 *
 * being variables and
 *
 * <pre>c</pre>
 *
 * being a constant number in the range of the real numbers. Thus it can be seen as a more specific
 * version of the polyhedron domain, which allows the representation of linear constraints with an
 * arbitrary number of variables.
 *
 * <p>It provides options for both, analysing programs with integer and float variables, whereas the
 * second option drains much more performance.
 *
 * <p>More information on this abstract domain, the implementation, and the evaluation of this CPA
 * can be found at: <b>http://stieglmaier.me/uploads/thesis.pdf</b>
 */
package org.sosy_lab.cpachecker.cpa.octagon;
