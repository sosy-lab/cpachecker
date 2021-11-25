// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.expressions;

/**
 * Casts an ExpressionTree to a subclass of the generic LeafType. This visitor checks compatibility
 * of the corresponding types and performes a static cast.
 */
public class DownwardCastingVisitor<ActualType, DesiredType extends ActualType>
    extends DefaultExpressionTreeVisitor<
        ActualType,
        ExpressionTree<DesiredType>,
        DownwardCastingVisitor.IncompatibleLeafTypesException> {
  /**
   * This exception indicates that the cast attempted by this visitor is ineed invalid. It is a
   * checked exception, because the client of this visitor shall not accidentally ignore this case,
   * but might be able to recover.
   */
  public static class IncompatibleLeafTypesException extends Exception {
    private static final long serialVersionUID = -2173628713139897329L;
    private final Class<?> actual;
    private final Class<?> desired;

    private IncompatibleLeafTypesException(Class<?> pActual, Class<?> pDesired) {
      super("Cannot cast from " + pActual + "to " + pDesired.getName());
      actual = pActual;
      desired = pDesired;
    }

    public Class<?> getActualClass() {
      return actual;
    }

    public Class<?> getDesiredClass() {
      return desired;
    }
  }

  private final Class<DesiredType> desiredClass;

  public DownwardCastingVisitor(Class<DesiredType> pDesiredClass) {
    desiredClass = pDesiredClass;
  }

  @Override
  @SuppressWarnings("unchecked")
  protected ExpressionTree<DesiredType> visitDefault(ExpressionTree<ActualType> pExpressionTree)
      throws IncompatibleLeafTypesException {
    Object expressionTreeAny = pExpressionTree;

    // This is safe, because we a) checked the actual LeafType, b) the tree is immutable.
    return (ExpressionTree<DesiredType>) expressionTreeAny;
  }

  @Override
  public ExpressionTree<DesiredType> visit(LeafExpression<ActualType> pLeafExpression)
      throws IncompatibleLeafTypesException {
    // This is where we ensure that the attempted cast is valid!
    if (!desiredClass.isInstance(pLeafExpression.getExpression())) {
      throw new IncompatibleLeafTypesException(
          pLeafExpression.getExpression().getClass(), desiredClass);
    }
    return visitDefault(pLeafExpression);
  }
}
