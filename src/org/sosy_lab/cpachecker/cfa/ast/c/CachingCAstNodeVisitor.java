// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.exceptions.NoException;

/**
 * A visitor for creating copies of abstract syntax tree (AST) nodes where a cache is used to reduce
 * the number of duplicate AST nodes (i.e., multiple AST node instances that are equal).
 *
 * <p>A custom {@link Cache} implementation can be specified using {@link
 * #CachingCAstNodeVisitor(Cache)}. A default cache implementation that doesn't discard any entries
 * is used when no custom cache is specified using {@link #CachingCAstNodeVisitor()}.
 *
 * <p>Copied AST nodes are created by calling {@link CAstNode#accept(CAstNodeVisitor)}:
 *
 * <pre>{@code CAstNode copiedAstNode = originalAstNode.accept(cachingVisitor);}</pre>
 */
public final class CachingCAstNodeVisitor extends AbstractTransformingCAstNodeVisitor<NoException> {

  private final Cache cache;

  /**
   * Creates a {@code CachingCAstNodeVisitor} instance using the specified cache.
   *
   * @param pCache the cache to use for AST node duplicate reduction
   * @throws NullPointerException if {@code pCache == null}
   */
  public CachingCAstNodeVisitor(Cache pCache) {
    cache = checkNotNull(pCache);
  }

  /**
   * Creates a {@code CachingCAstNodeVisitor} instance which uses a default cache implementation
   * that doesn't discard any entries.
   */
  public CachingCAstNodeVisitor() {
    this(new HashMapCache());
  }

  @Override
  public CAstNode visit(CVariableDeclaration pNode) {
    return cache.retrieve(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CArrayDesignator pNode) {
    return cache.retrieve(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CArrayRangeDesignator pNode) {
    return cache.retrieve(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CFieldDesignator pNode) {
    return cache.retrieve(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CInitializerExpression pNode) {
    return cache.retrieve(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CInitializerList pNode) {
    return cache.retrieve(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CDesignatedInitializer pNode) {
    return cache.retrieve(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CFunctionCallExpression pNode) {
    return cache.retrieve(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CBinaryExpression pNode) {
    return cache.retrieve(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CCastExpression pNode) {
    return cache.retrieve(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CCharLiteralExpression pNode) {
    return cache.retrieve(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CFloatLiteralExpression pNode) {
    return cache.retrieve(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CIntegerLiteralExpression pNode) {
    return cache.retrieve(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CStringLiteralExpression pNode) {
    return cache.retrieve(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CTypeIdExpression pNode) {
    return cache.retrieve(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CUnaryExpression pNode) {
    return cache.retrieve(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CImaginaryLiteralExpression pNode) {
    return cache.retrieve(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CAddressOfLabelExpression pNode) {
    return cache.retrieve(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CArraySubscriptExpression pNode) {
    return cache.retrieve(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CFieldReference pNode) {
    return cache.retrieve(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CIdExpression pNode) {
    return cache.retrieve(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CPointerExpression pNode) {
    return cache.retrieve(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CComplexCastExpression pNode) {
    return cache.retrieve(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CFunctionDeclaration pNode) {
    return cache.retrieve(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CComplexTypeDeclaration pNode) {
    return cache.retrieve(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CTypeDefDeclaration pNode) {
    return cache.retrieve(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CParameterDeclaration pNode) {
    return cache.retrieve(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CEnumerator pNode) {
    return cache.retrieve(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CExpressionStatement pNode) {
    return cache.retrieve(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CExpressionAssignmentStatement pNode) {
    return cache.retrieve(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CFunctionCallAssignmentStatement pNode) {
    return cache.retrieve(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CFunctionCallStatement pNode) {
    return cache.retrieve(pNode, () -> super.visit(pNode));
  }

  @Override
  public CAstNode visit(CReturnStatement pNode) {
    return cache.retrieve(pNode, () -> super.visit(pNode));
  }

  /** Represents a cache for storing and retrieving {@link CAstNode} instances. */
  public interface Cache {

    /**
     * Returns the value for the specified key or, if no such associated value exists, the value
     * returned by the specified supplier.
     *
     * <p>This cache may store values returned by the specified supplier. Additionally, there is no
     * guarantee that a retrieved value will be permanently stored in this cache, because a cache
     * may discard stored values at any time.
     *
     * @param pKey the AST node to get the cached value for
     * @param pValueSupplier the value supplier (must not return {@code null}) used if there is no
     *     value stored for the specified key
     * @return If this cache contains a value for the specified key, the value is returned.
     *     Otherwise, if this cache contains no such associated value, the value returned by the
     *     specified supplier is returned.
     * @throws NullPointerException if any parameter is {@code null} or the specified supplier is
     *     used and returns {@code null}
     */
    CAstNode retrieve(CAstNode pKey, Supplier<CAstNode> pValueSupplier);
  }

  /** {@link Cache} implementation based on {@link HashMap}. */
  private static final class HashMapCache implements Cache {

    private final Map<CAstNode, CAstNode> cache = new HashMap<>();

    @Override
    public CAstNode retrieve(CAstNode pKey, Supplier<CAstNode> pValueSupplier) {

      checkNotNull(pKey);
      checkNotNull(pValueSupplier);

      return cache.computeIfAbsent(pKey, key -> checkNotNull(pValueSupplier.get()));
    }
  }
}
