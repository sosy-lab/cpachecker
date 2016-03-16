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
 * This package contains an additional Prover-wrapper,
 * which uses constraints to improve formulas with uninterpreted functions.
 *
 * We use UFs to model some operations and let the solver choose
 * arbitrary values (over-approximation!) for the result of the UF.
 * If a formula is UNSAT, we can ignore the UF. Otherwise we try to compute
 * a better result with the {@link org.sosy_lab.cpachecker.util.predicates.ufCheckingProver.FunctionApplicationManager} and
 * add an additional constraint for the UF.
 * This iteratively improves the solver's model.
 *
 * The {@link org.sosy_lab.cpachecker.util.predicates.ufCheckingProver.FunctionApplicationManager} depends on the program's analysis
 * and matches the precise operations that are over-approximated with UFs.
 */
package org.sosy_lab.cpachecker.util.predicates.ufCheckingProver;