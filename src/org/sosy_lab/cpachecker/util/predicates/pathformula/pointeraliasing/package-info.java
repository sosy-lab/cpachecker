// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * Encoding of (possibly-aliased) C pointers into formulas, including conditional updates for
 * maybe-aliased pointers. This package assumes that pointers of different types are never aliased.
 */
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
    value = "SE_BAD_FIELD",
    justification = "serialization of formulas is currently unsupported")
@javax.annotation.ParametersAreNonnullByDefault
@org.sosy_lab.common.annotations.FieldsAreNonnullByDefault
@org.sosy_lab.common.annotations.ReturnValuesAreNonnullByDefault
package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;
