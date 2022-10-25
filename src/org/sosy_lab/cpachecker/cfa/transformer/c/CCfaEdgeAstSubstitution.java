// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.transformer.c;

import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.SubstitutingCAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.TransformingCAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

/**
 * A substitution for AST nodes that appear in CFA edges.
 *
 * <p>{@link TransformingCAstNodeVisitor} and {@link SubstitutingCAstNodeVisitor} can be useful for
 * implementing such a substitution.
 */
@FunctionalInterface
public interface CCfaEdgeAstSubstitution {

  /**
   * Returns the substitute AST node for the specified AST node that is contained in the specified
   * CFA edge.
   *
   * @param pEdge the CFA edge that contains the specified AST node
   * @param pAstNode the AST node to get the substitute for
   * @return the substitute AST node for the specified AST node that is contained in the specified
   *     CFA edge (must not return {@code null})
   * @throws NullPointerException if any parameter is {@code null}
   */
  CAstNode apply(CFAEdge pEdge, CAstNode pAstNode);
}
