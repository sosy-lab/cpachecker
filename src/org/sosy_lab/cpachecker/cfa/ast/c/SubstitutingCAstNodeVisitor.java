// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.function.Function;
import java.util.function.Supplier;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.exceptions.NoException;

/**
 * A visitor for creating modified copies of abstract syntax tree (AST) nodes by applying a user
 * defined node substitution.
 *
 * <p>A substitution is a {@link Function} that maps original nodes to substitute nodes. If the
 * substitution function returns an instance of {@link CAstNode}, the returned instance is used as a
 * substitute. Otherwise, if {@code null} is returned, the node is not substituted and its children
 * are recursively visited and check for substitution.
 *
 * <p>Modified AST nodes are created by calling {@link CAstNode#accept(CAstNodeVisitor)}:
 *
 * <pre>{@code
 * CAstNode modifiedAstNode = originalAstNode.accept(substitutingVisitor);
 * }</pre>
 */
public final class SubstitutingCAstNodeVisitor
    extends AbstractTransformingCAstNodeVisitor<NoException> {

  private final Function<CAstNode, CAstNode> substitution;

  /**
   * Creates a {@code SubstitutingCAstNodeVisitor} instance for a specified substitution.
   *
   * @param pSubstitution The substitution function that maps original nodes to substitute nodes. If
   *     the substitution function returns an instance of {@link CAstNode}, the returned instance is
   *     used as a substitute. Otherwise, if {@code null} is returned, the node is not substituted
   *     and its children are recursively visited and check for substitution.
   * @throws NullPointerException if {@code pSubstitution == null}
   */
  public SubstitutingCAstNodeVisitor(Function<CAstNode, CAstNode> pSubstitution) {
    substitution = checkNotNull(pSubstitution);
  }

  /**
   * Returns the substitute for a specified {@link CAstNode}, or, if the substitution function
   * returns {@code null}, the value returned by the specified supplier.
   */
  private CAstNode substitute(CAstNode pCAstNode, Supplier<CAstNode> pDefaultSupplier) {

    CAstNode result = substitution.apply(pCAstNode);

    return result != null ? result : pDefaultSupplier.get();
  }

  @Override
  public CAstNode visit(CArrayDesignator pNode) {
    return substitute(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CArrayRangeDesignator pNode) {
    return substitute(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CFieldDesignator pNode) {
    return substitute(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CInitializerExpression pNode) {
    return substitute(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CInitializerList pNode) {
    return substitute(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CDesignatedInitializer pNode) {
    return substitute(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CFunctionCallExpression pNode) {
    return substitute(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CBinaryExpression pNode) {
    return substitute(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CCastExpression pNode) {
    return substitute(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CCharLiteralExpression pNode) {
    return substitute(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CFloatLiteralExpression pNode) {
    return substitute(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CIntegerLiteralExpression pNode) {
    return substitute(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CStringLiteralExpression pNode) {
    return substitute(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CTypeIdExpression pNode) {
    return substitute(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CUnaryExpression pNode) {
    return substitute(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CImaginaryLiteralExpression pNode) {
    return substitute(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CAddressOfLabelExpression pNode) {
    return substitute(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CArraySubscriptExpression pNode) {
    return substitute(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CFieldReference pNode) {
    return substitute(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CIdExpression pNode) {
    return substitute(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CPointerExpression pNode) {
    return substitute(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CComplexCastExpression pNode) {
    return substitute(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CFunctionDeclaration pNode) {
    return substitute(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CComplexTypeDeclaration pNode) {
    return substitute(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CTypeDefDeclaration pNode) {
    return substitute(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CVariableDeclaration pNode) {
    return substitute(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CParameterDeclaration pNode) {
    return substitute(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CEnumerator pNode) {
    return substitute(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CExpressionStatement pNode) {
    return substitute(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CExpressionAssignmentStatement pNode) {
    return substitute(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CFunctionCallAssignmentStatement pNode) {
    return substitute(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CFunctionCallStatement pNode) {
    return substitute(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CReturnStatement pNode) {
    return substitute(pNode, () -> super.visit(pNode));
  }
}
