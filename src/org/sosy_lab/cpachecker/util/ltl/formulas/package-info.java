// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * Contains objects for the following ltl-formulas:
 *
 * <p>\phi := "P" | \phi || \phi | \phi && \phi | !\phi | G \phi | F \phi | \phi U \phi | \phi W
 * \phi | \phi R \phi *
 *
 * <p>where \phi is a temporal formula and "P" is an atomic proposition. .
 */
package org.sosy_lab.cpachecker.util.ltl.formulas;
