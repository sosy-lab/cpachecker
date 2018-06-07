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

interface Scope {
  Scope getParentScope();

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
    for (Scope current = getParentScope();
        current.hasParentScope();
        current = current.getParentScope()) {
      if (parentScopeType.isInstance(current)) {
        return (S) current;
      }
    }
    return null;
  }

  default boolean hasParentScope() {
    return getParentScope() != null;
  }
}
