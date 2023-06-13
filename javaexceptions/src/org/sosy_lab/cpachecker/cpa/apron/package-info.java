// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * The Apron CPA is based on the APRON library. It can use octagons, intervals or polyhedra
 * (octagons were used during the implementation of this CPA).
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
 * <p>Please note that the used APRON library is the official successor of the Octagon Abstract
 * Domain Library (also added in CPAchecker c.f. OctagonCPA), so you might want to take a look at
 * this CPA, too, especially as its results where much more reliable than the ones of the ApronCPA.
 *
 * <p>More information on this abstract domain, the implementation, and the evaluation of this CPA
 * can be found at: <b>http://stieglmaier.me/uploads/thesis.pdf</b>
 */
@org.sosy_lab.common.annotations.Unmaintained
package org.sosy_lab.cpachecker.cpa.apron;
