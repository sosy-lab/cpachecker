// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.java;

import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;

/**
 * This interface represents all sorts of top-level declarations (i.e., declarations
 * not nested inside another type declaration).
 * This excludes for examples methods parameter declarations.
 * It includes variables, as well as methods.
 */
public interface JDeclaration extends ADeclaration, JSimpleDeclaration {

}
