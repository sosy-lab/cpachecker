// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * Contains classes for Constraints CPA. Constraints CPA tracks constraints such as conditions in
 * if- or while-statements. The ConstraintsCPA is only useful in combination with a CPA creating
 * symbolic values, for example {@link org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA
 * ValueAnalysisCPA} with property <code>cpa.value.unknownValueHandling=INTRODUCE_SYMBOLIC</code>.
 * Without symbolic execution, it's transfer relation will always return a state containing no
 * information.
 */
package org.sosy_lab.cpachecker.cpa.constraints;
