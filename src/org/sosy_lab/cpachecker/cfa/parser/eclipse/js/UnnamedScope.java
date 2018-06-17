/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.parser.eclipse.js;

import javax.annotation.Nonnull;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;

/**
 * Scope that does not influence the naming of declarations.
 * The scope might be used to provide other context information.
 */
interface UnnamedScope extends Scope {
  /**
   * The value analysis expects that the qualified scope name equals the (qualified) function scope
   * name. This scope is not named till this (invalid) assumption (see {@link
   * ASimpleDeclaration#getQualifiedName()}: "Client code should not rely on a specific format of
   * the returned name") is discarded to stay compatible with the value analysis.
   *
   * @return Empty string.
   */
  @Nonnull
  @Override
  default String getNameOfScope() {
    return "";
  }

  /**
   * The value analysis expects that the qualified scope name equals the (qualified) function scope
   * name. This scope is not named till this (invalid) assumption (see {@link
   * ASimpleDeclaration#getQualifiedName()}: "Client code should not rely on a specific format of
   * the returned name") is discarded to stay compatible with the value analysis.
   *
   * @return Qualified name of the parent scope.
   */
  @Nonnull
  @Override
  default String qualifiedNameOfScope() {
    return getParentScope().qualifiedNameOfScope();
  }
}
