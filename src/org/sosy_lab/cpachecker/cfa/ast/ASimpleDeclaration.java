/*
 *  CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cfa.ast;

import org.sosy_lab.cpachecker.cfa.types.Type;

/**
 * This interface represents the core components that occur in each declaration:
 * a type and an (optional) name.
 *
 * It is part of the declaration of types and variables (see {@link ADeclaration})
 * and functions (see {@link AFunctionDeclaration}).
 * It is also used stand-alone for the declaration of members of composite types
 * (e.g. structs) and for the declaration of function parameters.
 */
public interface ASimpleDeclaration extends AAstNode {

  /**
   * Get the name of this declaration. The name is unique within its context (e.g., function), but
   * not globally. Use {@link #getQualifiedName()} to get a globally unique name.
   *
   * <p>This name is not necessarily identical to the name that appeared in the source code, use
   * {@link #getOrigName()} to retrieve the latter.
   */
  String getName();

  /**
   * Return the name of this declaration as it appeared in the source code. This name should not be
   * used to identify the declaration inside CPAchecker, but is useful for output to the user or
   * output files that relate to the source code.
   */
  String getOrigName();

  Type getType();

  /**
   * Get a globally unique name of this declaration, i.e., names of local declarations are qualified
   * with the name of the context (function, class, etc.).
   *
   * <p>Client code should not rely on a specific format of the returned name.
   */
  String getQualifiedName();
}
