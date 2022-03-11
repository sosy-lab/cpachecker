// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * Converting code to formulas, and representation of program paths as formulas. This includes the
 * handling of re-assigned variables, which get converted into SSA form.
 */
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
    value = {"SE_BAD_FIELD", "SE_TRANSIENT_FIELD_NOT_RESTORED"},
    justification = "serialization of formulas is currently unsupported")
@javax.annotation.ParametersAreNonnullByDefault
@org.sosy_lab.common.annotations.FieldsAreNonnullByDefault
@org.sosy_lab.common.annotations.ReturnValuesAreNonnullByDefault
package org.sosy_lab.cpachecker.util.predicates.pathformula;
