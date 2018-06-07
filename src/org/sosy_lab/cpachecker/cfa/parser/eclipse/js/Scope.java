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
package org.sosy_lab.cpachecker.cfa.parser.eclipse.js;

import java.nio.file.Path;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.js.JSSimpleDeclaration;

/**
 * The scope is responsible for renaming and resolving identifiers. It provides methods to rename
 * identifiers of the source code to unique names (to avoid shadowing). It stores which CFA
 * declarations belong to the identifiers of the source code to resolve them.
 */
interface Scope {
  Scope getParentScope();

  /**
   * Get the (unqualified) name of this scope, which is unique in its parent scope. It may only
   * contain characters that are valid in a file name, because it is part of the qualified name of
   * the scope, which is used in function identifiers for which a file <code>
   * cfa__qualifiedFunctionName.dot</code> is created (in the output directory) whose name contains
   * the qualified name of the scope (see {@link
   * org.sosy_lab.cpachecker.cfa.export.DOTBuilder2.DOTViewBuilder#writeFunctionFile(String,
   * Path)}).
   *
   * @return Name of this scope.
   */
  @Nonnull
  String getNameOfScope();

  /**
   * Returns the separator which is used to separate the qualified name of this scope and the
   * qualified name of the parent scope. The separator may only contain characters that are valid in
   * a file name, because the qualified name of the scope is used in function identifiers for which
   * a file <code>cfa__qualifiedFunctionName.dot</code> is created (in the output directory) whose
   * name contains the qualified name of the scope (see {@link
   * org.sosy_lab.cpachecker.cfa.export.DOTBuilder2.DOTViewBuilder#writeFunctionFile(String,
   * Path)}).
   *
   * @return The qualified scope name separator.
   * @see Scope#qualifiedNameOfScope()
   */
  @Nonnull
  default String getQualifiedScopeNameSeparator() {
    return ".";
  }

  /**
   * Determine the qualified name of this scope, which is based on the qualified name of the parent
   * scope. The qualified name may only contain characters that are valid in a file name, because
   * the qualified name of the scope is used in function identifiers for which a file <code>
   * cfa__qualifiedFunctionName.dot</code> is created (in the output directory) whose name contains
   * the qualified name of the scope (see {@link
   * org.sosy_lab.cpachecker.cfa.export.DOTBuilder2.DOTViewBuilder#writeFunctionFile(String,
   * Path)}).
   *
   * @return The qualified name of this scope.
   */
  @Nonnull
  default String qualifiedNameOfScope() {
    return !hasParentScope()
        ? getNameOfScope()
        : separateNames(
            getParentScope().qualifiedNameOfScope(),
            getQualifiedScopeNameSeparator(),
            getNameOfScope());
  }

  /**
   * Add declaration to this scope.
   *
   * @param pDeclaration Declaration that is declared in this scope.
   */
  void addDeclaration(final JSSimpleDeclaration pDeclaration);

  /**
   * Search for the declaration of an identifier.
   *
   * @param pIdentifier The original name as in the source code to analyze.
   * @return Only present if declaration is found.
   */
  Optional<? extends JSSimpleDeclaration> findDeclaration(final String pIdentifier);

  default FileScope getFileScope() {
    return getScope(FileScope.class);
  }

  /**
   * Get scope of specific type <code>S</code>, which might be this scope or a parent scope.
   *
   * @param scopeType The non-nullable class object that represents the type of the scope to find.
   * @param <S> The type of the scope to find.
   * @return If <code>null</code> is returned, there is no scope of specific type <code>S</code>.
   */
  @SuppressWarnings({"unchecked"})
  default <S extends Scope> S getScope(final Class<S> scopeType) {
    assert scopeType != null : "The scope-type has to be defined";
    return scopeType.isInstance(this) ? (S) this : getParentScope(scopeType);
  }

  /**
   * Get parent scope of specific type <code>S</code>.
   *
   * @param parentScopeType The non-nullable class object that represents the type of the parent
   *     scope to find.
   * @param <S> The type of the parent scope to find.
   * @return If <code>null</code> is returned, there is no parent scope of specific type <code>S
   *     </code>.
   */
  @SuppressWarnings({"unchecked"})
  default <S extends Scope> S getParentScope(final Class<S> parentScopeType) {
    assert parentScopeType != null : "The scope-type has to be defined";
    if (!hasParentScope()) {
      return null;
    }
    for (Scope current = this; current.hasParentScope(); current = current.getParentScope()) {
      final Scope currentParentScope = current.getParentScope();
      if (parentScopeType.isInstance(currentParentScope)) {
        return (S) currentParentScope;
      }
    }
    return null;
  }

  default boolean hasParentScope() {
    return getParentScope() != null;
  }

  /**
   * Get qualified name of a member (e.g. variable or parameter) declared in this scope. Usually,
   * what you get, when you call {@link ASimpleDeclaration#getQualifiedName()}.
   *
   * @param pName The name of the member declaration (usually what you get, when you call {@link
   *     ASimpleDeclaration#getName()}).
   * @return The qualified name of the member declaration.
   */
  @Nonnull
  default String qualifiedVariableNameOf(final @Nonnull String pName) {
    return separateNames(qualifiedNameOfScope(), "::", pName);
  }

  /**
   * Get qualified name of a member (e.g. function) declared in this scope. Usually, what you get,
   * when you call {@link ASimpleDeclaration#getQualifiedName()}.
   *
   * @param pName The name of the member declaration (usually what you get, when you call {@link
   *     ASimpleDeclaration#getName()}).
   * @return The qualified name of the member declaration.
   */
  @Nonnull
  default String qualifiedFunctionNameOf(final @Nonnull String pName) {
    return separateNames(qualifiedNameOfScope(), ".", pName);
  }

  /**
   * Create unique name of <code>pName</code> in this scope.
   *
   * @param pName (Original) Name of a declaration.
   * @return <code>pName</code> if it is already unique in this scope. Otherwise, a numbered prefix
   *     is appended to <code>pName</code>.
   */
  @Nonnull
  default String uniquifyName(final @Nonnull String pName) {
    String uniqueName = pName;
    for (int counter = 0; findDeclaration(uniqueName).isPresent(); counter++) {
      uniqueName = pName + "__" + counter;
    }
    return uniqueName;
  }

  /**
   * Separate scope or identifier names using a separator.
   *
   * @param pLeftName The left name is usually the name of a scope (parent or current), which might
   *     be the empty string.
   * @param pSeparator The separator depends on the kind of names (scope, function, variable, etc.)
   *     that are appended. For variables it is usually <code>"::"</code>.
   * @param pRightName The right name is usually an identifier (of a variable or function) or the
   *     name of the child scope.
   * @return If the left name is empty, the right name is returned without the separator. Otherwise,
   *     left name, separator and right name are concatenated in order.
   */
  @Nonnull
  default String separateNames(
      final @Nonnull String pLeftName,
      final @Nonnull String pSeparator,
      final @Nonnull String pRightName) {
    return pLeftName.isEmpty() ? pRightName : pLeftName + pSeparator + pRightName;
  }
}
