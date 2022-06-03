// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

/**
 * This class represents the core components that occur in each declaration: a type and an
 * (optional) name.
 *
 * <p>It is part of the declaration of types and variables (see {@link CDeclaration}) and functions
 * (see {@link CFunctionDeclaration}). It is also used stand-alone for the declaration of members of
 * composite types (e.g. structs) and for the declaration of function parameters.
 */
public interface CSimpleDeclaration extends ASimpleDeclaration, CAstNode {

  @Override
  CType getType();

  <R, X extends Exception> R accept(CSimpleDeclarationVisitor<R, X> v) throws X;
}
