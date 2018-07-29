/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.ast.js;

import javax.annotation.Nonnull;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.types.js.JSType;

/**
 * This class represents the core components that occur in each declaration: a type and an
 * (optional) name.
 *
 * <p>It is part of the declaration of types and variables (see {@link
 * org.sosy_lab.cpachecker.cfa.ast.js.JSDeclaration}) and functions (see {@link
 * org.sosy_lab.cpachecker.cfa.ast.js.JSFunctionDeclaration}). It is also used stand-alone for the
 * declaration of members of composite types (e.g. structs) and for the declaration of function
 * parameters.
 */
public interface JSSimpleDeclaration extends ASimpleDeclaration, JSAstNode {

  @Nonnull
  Scope getScope();

  @Override
  public JSType getType();

  public <R, X extends Exception> R accept(JSSimpleDeclarationVisitor<R, X> v) throws X;
}
