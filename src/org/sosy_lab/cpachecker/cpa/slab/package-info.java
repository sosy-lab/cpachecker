/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
 * The SLAB CPA is based on the Software Model Checker SLAB from the paper Slicing Abstractions
 *
 * <p>SLAB works by constructing an initial abstraction containing a state for each combination of
 * the special predicates init and error. This model is then refined in a CEGAR loop where the
 * states are split according to the interpolants found for the infeasible counterexample trace.The
 * locations are not tracked by a Location CPA, so the information about the program counter is
 * encoded symbolically into the path/state formulas. This is the reason why we need a special CPA
 * and cannot simply use the Predicate CPA with a special refinement strategy (as is the case for
 * Kojak)
 */
@javax.annotation.ParametersAreNonnullByDefault
@org.sosy_lab.common.annotations.FieldsAreNonnullByDefault
@org.sosy_lab.common.annotations.ReturnValuesAreNonnullByDefault
package org.sosy_lab.cpachecker.cpa.slab;
