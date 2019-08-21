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
 * Utilities for refinement and refinement selection.
 *
 * <p>Contains Generic* classes which can be used for composing a simple refinement based on
 * refinement for abstract variable assignments.
 * Most of these are only dependent on interfaces
 * {@link org.sosy_lab.cpachecker.util.refinement.StrongestPostOperator StrongestPostOperator},
 * {@link org.sosy_lab.cpachecker.util.refinement.Interpolant Interpolant}
 * , {@link org.sosy_lab.cpachecker.util.refinement.InterpolantManager InterpolantManager} and
 * {@link org.sosy_lab.cpachecker.util.refinement.ForgetfulState ForgetfulState}.
 * By defining implementations for these four interfaces, one can define a complete refinement
 * using the Generic* classes.</p>
 */
package org.sosy_lab.cpachecker.util.refinement;