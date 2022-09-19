// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

/**
 * Provides a symbol table that maps variable and functions to their declaration. Is needed if
 * single or blocks of statements are supposed to be parsed.
 */
public interface Scope {

  /**
   * Checks if the scope is al global scope. Global scope allow to register functions, types and
   * global variables.
   *
   * @return true, if the scope is a global scope, false otherwise.
   */
  boolean isGlobalScope();

  /**
   * Checks if the given name is already used
   *
   * @param name the name to check
   */
  boolean variableNameInUse(String name);

  /**
   * Returns the declaration of the variable with the given name, if the variable is visible.
   *
   * @param name the declaration for the variable with this name is returned.
   * @return returns the declaration for the variable with the given name, or null, if the variable
   *     is not visible.
   */
  @Nullable CSimpleDeclaration lookupVariable(String name);

  /**
   * Returns the declaration of the function with the given name.
   *
   * @param name the declaration for the function with this name is returned.
   * @return returns the declaration for the function with the given name, or null, if the
   *     declaration can't be found.
   */
  @Nullable CFunctionDeclaration lookupFunction(String name);

  /**
   * Look up {@link CComplexType}s by their name.
   *
   * @param name The fully qualified name (e.g., "struct s").
   * @return The CComplexType instance or null.
   */
  @Nullable CComplexType lookupType(String name);

  /**
   * Look up {@link CType}s by the names of their typedefs. This is basically needed to correctly
   * search for anonymous complex types e.g.
   *
   * <pre>
   * typedef struct { // The struct gets the tag __anon_type_0
   *    ...
   * } s_type;
   * </pre>
   *
   * @param name typedef type name e.g. s_type
   * @return the type declared in typedef e.g. struct __anon_type_0
   */
  CType lookupTypedef(String name);

  /**
   * Adds the given declaration to the scope.
   *
   * @param declaration Adds this declaration to the scope.
   */
  void registerDeclaration(CSimpleDeclaration declaration);

  /**
   * Register a type, e.g., a new struct type.
   *
   * @return True if the type actually needs to be declared, False if the declaration can be omitted
   *     because the type is already known.
   */
  boolean registerTypeDeclaration(CComplexTypeDeclaration declaration);

  /**
   * Take a name and return a name qualified with the current function (if we are in a function).
   */
  String createScopedNameOf(String name);

  /** Returns the name for the type as it would be if it is renamed to a file specific version. */
  String getFileSpecificTypeName(String type);

  /** Checks if a given type is the file specific version or not. */
  boolean isFileSpecificTypeName(String type);
}
