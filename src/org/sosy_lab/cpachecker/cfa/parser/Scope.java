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
package org.sosy_lab.cpachecker.cfa.parser;

import javax.annotation.Nullable;

import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

/**
 * Provides a symbol table that maps variable and functions to their declaration.
 * Is needed if single or blocks of statements are supposed to be parsed.
 */
public interface Scope {

  /**
   * Checks if the scope is al global scope.
   * Global scope allow to register functions, types and global variables.
   * @return true, if the scope is a global scope, false otherwise.
   */
  public boolean isGlobalScope();

  /**
   *
   * @param name
   * @param origName
   * @return
   */
  public boolean variableNameInUse(String name, String origName);

  /**
   * Returns the declaration of the variable with the given name, if the
   * variable is visible.
   *
   * @param name the declaration for the variable with this name is returned.
   * @return returns the declaration for the variable with the given name,
   * or null, if the variable is not visible.
   */
  @Nullable
  public CSimpleDeclaration lookupVariable(String name);

  /**
   * Returns the declaration of the function with the given name.
   *
   * @param name the declaration for the function with this name is returned.
   * @return returns the declaration for the function with the given name,
   * or null, if the declaration can't be found.
   */
  public CFunctionDeclaration lookupFunction(String name);

  /**
   * Look up {@link CComplexType}s by their name.
   * @param name The fully qualified name (e.g., "struct s").
   * @return The CComplexType instance or null.
   */
  public CComplexType lookupType(String name);

  /**
   * Look up {@link CType}s by the names of their typedefs.
   * This is basically needed to correctly search for anonymous complex types e.g.
   * <pre>
   * typedef struct { // The struct gets the tag __anon_type_0
   *    ...
   * } s_type;
   * </pre>
   * @param name typedef type name e.g. s_type
   * @return the type declared in typedef e.g. struct __anon_type_0
   */
  public CType lookupTypedef(String name);

  /**
   * Adds the given declaration to the scope.
   *
   * @param declaration Adds this declaration to the scope.
   */
  public void registerDeclaration(CSimpleDeclaration declaration);

  /**
   * Register a type, e.g., a new struct type.
   *
   * @return True if the type actually needs to be declared, False if the declaration can be omitted because the type is already known.
   */
  public boolean registerTypeDeclaration(CComplexTypeDeclaration declaration);

  /**
   * Take a name and return a name qualified with the current function
   * (if we are in a function).
   */
  public String createScopedNameOf(String name);

  /**
   * Returns the name for the type as it would be if it is renamed.
   */
  public String getRenamedTypeName(String type);
}