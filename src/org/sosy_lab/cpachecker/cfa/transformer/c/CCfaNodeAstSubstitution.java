// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.transformer.c;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.SubstitutingCAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.TransformingCAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;

/**
 * A substitution for AST nodes that appear in CFA nodes.
 *
 * <p>{@link TransformingCAstNodeVisitor} and {@link SubstitutingCAstNodeVisitor} can be useful for
 * implementing such a substitution.
 */
public interface CCfaNodeAstSubstitution {

  /**
   * Returns the substitute function declaration for the specified function declaration that is
   * contained in the specified CFA node.
   *
   * @param pNode the CFA node that contains the specified function declaration
   * @param pFunction the function declaration to get the substitute for
   * @return the substitute function declaration for the specified function declaration that is
   *     contained in the specified CFA node (must not return {@code null})
   * @throws NullPointerException if any parameter is {@code null}
   */
  CFunctionDeclaration apply(CFANode pNode, CFunctionDeclaration pFunction);

  /**
   * Returns the substitute return variable for the specified return variable that is contained in
   * the specified function entry node.
   *
   * <p>A return variable may not exist (no variable declaration) for functions returning {@code
   * void}.
   *
   * @param pFunctionEntryNode the function entry node that contains the specified return variable
   * @param pReturnVariable the return variable to get the substitute for
   * @return the substitute return variable for the specified return variable that is contained in
   *     the specified function entry node (must not return {@code null})
   * @throws NullPointerException if any parameter is {@code null}
   */
  Optional<CVariableDeclaration> apply(
      CFunctionEntryNode pFunctionEntryNode, Optional<CVariableDeclaration> pReturnVariable);
}
