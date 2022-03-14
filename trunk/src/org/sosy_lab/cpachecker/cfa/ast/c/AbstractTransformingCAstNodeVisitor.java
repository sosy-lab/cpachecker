// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

import java.util.IdentityHashMap;

/**
 * {@link TransformingCAstNodeVisitor} that prevents infinite recursive visit-calls due to cyclic
 * references between variable declarations and their initializer expressions.
 *
 * @param <X> the type of exception thrown by the visitor
 */
public abstract class AbstractTransformingCAstNodeVisitor<X extends Exception>
    implements TransformingCAstNodeVisitor<X> {

  // original CVariableDeclaration --> transformed CVariableDeclaration
  private final IdentityHashMap<CVariableDeclaration, CVariableDeclaration> variableDeclarations =
      new IdentityHashMap<>();

  @Override
  public CAstNode visit(CVariableDeclaration pCVariableDeclaration) throws X {

    boolean firstVisitedVariableDeclaration = variableDeclarations.isEmpty();
    CVariableDeclaration newVariableDeclaration = variableDeclarations.get(pCVariableDeclaration);

    if (newVariableDeclaration == null) {

      newVariableDeclaration =
          new CVariableDeclaration(
              pCVariableDeclaration.getFileLocation(),
              pCVariableDeclaration.isGlobal(),
              pCVariableDeclaration.getCStorageClass(),
              pCVariableDeclaration.getType(),
              pCVariableDeclaration.getName(),
              pCVariableDeclaration.getOrigName(),
              pCVariableDeclaration.getQualifiedName(),
              null);

      variableDeclarations.put(pCVariableDeclaration, newVariableDeclaration);

      CInitializer oldInitializer = pCVariableDeclaration.getInitializer();
      if (oldInitializer != null) {
        CInitializer newInitializer = (CInitializer) oldInitializer.accept(this);
        if (newInitializer != null) {
          newVariableDeclaration.addInitializer(newInitializer);
        }
      }
    }

    if (firstVisitedVariableDeclaration) {
      variableDeclarations.clear();
    }

    return newVariableDeclaration;
  }
}
