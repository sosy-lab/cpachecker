// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * This package provides an API for CFA validation.
 *
 * <p>There are certain assumptions we have about CFAs. Only if these assumptions are met, we
 * consider a CFA valid. {@link org.sosy_lab.cpachecker.cfa.validation.CfaValidator CfaValidator}
 * implementations are used to determine whether those assumptions are met.
 */
package org.sosy_lab.cpachecker.cfa.validation;
