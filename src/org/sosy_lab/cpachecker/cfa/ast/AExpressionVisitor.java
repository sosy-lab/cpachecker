// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast;

import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.java.JFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JUnaryExpression;

public abstract class AExpressionVisitor<R, X extends Exception>
    implements CExpressionVisitor<R, X>, JExpressionVisitor<R, X> {

  /*
   * only language common expressions here, all other have to be implemented by
   * the concrete visitor
   */

  @Override
  public R visit(CArraySubscriptExpression exp) throws X {
    return visit((AArraySubscriptExpression) exp);
  }

  @Override
  public R visit(JArraySubscriptExpression exp) throws X {
    return visit((AArraySubscriptExpression) exp);
  }

  public abstract R visit(AArraySubscriptExpression exp) throws X;

  @Override
  public R visit(CIdExpression exp) throws X {
    return visit((AIdExpression) exp);
  }

  @Override
  public R visit(JIdExpression exp) throws X {
    return visit((AIdExpression) exp);
  }

  public abstract R visit(AIdExpression exp) throws X;

  @Override
  public R visit(CBinaryExpression exp) throws X {
    return visit((ABinaryExpression) exp);
  }

  @Override
  public R visit(JBinaryExpression exp) throws X {
    return visit((ABinaryExpression) exp);
  }

  public abstract R visit(ABinaryExpression exp) throws X;

  @Override
  public R visit(CCastExpression exp) throws X {
    return visit((ACastExpression) exp);
  }

  @Override
  public R visit(JCastExpression exp) throws X {
    return visit((ACastExpression) exp);
  }

  public abstract R visit(ACastExpression exp) throws X;

  @Override
  public R visit(CCharLiteralExpression exp) throws X {
    return visit((ACharLiteralExpression) exp);
  }

  @Override
  public R visit(JCharLiteralExpression exp) throws X {
    return visit((ACharLiteralExpression) exp);
  }

  public abstract R visit(ACharLiteralExpression exp) throws X;

  @Override
  public R visit(CFloatLiteralExpression exp) throws X {
    return visit((AFloatLiteralExpression) exp);
  }

  @Override
  public R visit(JFloatLiteralExpression exp) throws X {
    return visit((AFloatLiteralExpression) exp);
  }

  public abstract R visit(AFloatLiteralExpression exp) throws X;

  @Override
  public R visit(CIntegerLiteralExpression exp) throws X {
    return visit((AIntegerLiteralExpression) exp);
  }

  @Override
  public R visit(JIntegerLiteralExpression exp) throws X {
    return visit((AIntegerLiteralExpression) exp);
  }

  public abstract R visit(AIntegerLiteralExpression exp) throws X;

  @Override
  public R visit(CStringLiteralExpression exp) throws X {
    return visit((AStringLiteralExpression) exp);
  }

  @Override
  public R visit(JStringLiteralExpression exp) throws X {
    return visit((AStringLiteralExpression) exp);
  }

  public abstract R visit(AStringLiteralExpression exp) throws X;

  @Override
  public R visit(CUnaryExpression exp) throws X {
    return visit((AUnaryExpression) exp);
  }

  @Override
  public R visit(JUnaryExpression exp) throws X {
    return visit((AUnaryExpression) exp);
  }

  public abstract R visit(AUnaryExpression exp) throws X;
}
