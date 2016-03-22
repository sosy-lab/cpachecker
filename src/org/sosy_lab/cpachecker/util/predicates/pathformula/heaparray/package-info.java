/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
 * This packages provides the encoding of (possibly-aliased) C pointers into SMT formulae, including
 * conditional updates for maybe-aliased pointers.
 * <p/>
 * The packages assumes that pointers of different types are never aliased.
 * <p/>
 * The package uses the SMT theory of arrays to model the heap memory with SMT
 * arrays, hence we need a SMT solver capable of the SMT theory of arrays. In
 * current CPAchecker, the following bundled solvers can be used:
 * <ul>
 *   <li><emph>SMTInterpol</emph>&mdash;the default solver (configuration option:
 *       <code>solver.solver=SMTINTERPOL</code>)</li>
 *   <li><emph>MathSAT5</emph>&mdash;available with configuration option:
 *       <code>solver.solver=MATHSAT5</code></li>
 *   <li><emph>Princess</emph>&mdash;available with configuration option:
 *       <code>solver.solver=PRINCESS</code>)</li>
 *   <li><emph>Z3</emph>&mdash;available with configuration option:
 *       <code>solver.solver=Z3</code></li>
 * </ul>
 */
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "SE_BAD_FIELD",
    justification = "serialization of formulas is currently unsupported")
package org.sosy_lab.cpachecker.util.predicates.pathformula.heaparray;
