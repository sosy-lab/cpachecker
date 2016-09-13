/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
/**
 * The Apron CPA is based on the APRON library. It can use octagons, intervals
 * or polyhedra (octagons were used during the implementation of this CPA).
 *
 * The octagon abstract domain is a weakly relational numerical domain. It is
 * based on Difference-Bound Matrices and therefore able to handle
 * constraints of the form <pre> ±X ±Y ≤ c</pre> with <pre>X</pre> and <pre>Y</pre> being variables
 * and <pre>c</pre> being a constant number in the range of the real numbers.  Thus it can be
 * seen as a more specific version of the polyhedron domain, which allows
 * the representation of linear constraints with an arbitrary number of variables.
 *
 * Please note that the used APRON library is the official successor of the Octagon Abstract Domain
 * Library (also added in CPAchecker c.f. OctagonCPA), so you might want to take a look at this CPA,
 * too, especially as its results where much more reliable than the ones of the ApronCPA.
 *
 * More information on this abstract domain, the implementation, and the evaluation of this CPA
 * can be found at: <b>http://stieglmaier.me/uploads/thesis.pdf</b>
 */
package org.sosy_lab.cpachecker.cpa.apron;