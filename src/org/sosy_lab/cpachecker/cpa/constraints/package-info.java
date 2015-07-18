/*
 * CPAchecker is a tool for configurable software verification.
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
 * Contains classes for Constraints CPA.
 * Constraints CPA tracks constraints such as conditions in if- or while-statements.
 * The ConstraintsCPA is only useful in combination with a CPA creating symbolic values,
 * for example {@link org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA ValueAnalysisCPA} with
 * property <code>cpa.value.symbolic.useSymbolicValues</code> set to true.
 * Without symbolic execution, it's transfer relation will always return a state containing
 * no information.
 */
package org.sosy_lab.cpachecker.cpa.constraints;
