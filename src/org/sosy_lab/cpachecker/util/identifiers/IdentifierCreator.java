// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.identifiers;

import com.google.common.base.Preconditions;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.NoException;

public class IdentifierCreator extends DefaultCExpressionVisitor<AbstractIdentifier, NoException> {
  protected int dereference;
  protected String function;

  public IdentifierCreator(String func) {
    function = func;
  }

  public static AbstractIdentifier createIdentifier(
      CSimpleDeclaration decl, String function, int dereference) {
    Preconditions.checkNotNull(decl);
    String name = decl.getName();
    CType type = decl.getType();

    if (decl instanceof CDeclaration) {
      if (((CDeclaration) decl).isGlobal()) {
        return new GlobalVariableIdentifier(name, type, dereference);
      } else {
        return new LocalVariableIdentifier(name, type, function, dereference);
      }
    } else if (decl instanceof CParameterDeclaration) {
      return new LocalVariableIdentifier(name, type, function, dereference);
    } else if (decl instanceof CEnumerator) {
      return new ConstantIdentifier(name, dereference);
    } else {
      // Composite type
      return null;
    }
  }

  public AbstractIdentifier createIdentifier(CExpression expression, int pDereference) {
    Preconditions.checkNotNull(expression);

    dereference = pDereference;
    return expression.accept(this);
  }

  @Override
  public AbstractIdentifier visit(CArraySubscriptExpression expression) {
    dereference++;
    return expression.getArrayExpression().accept(this);
  }

  @Override
  public AbstractIdentifier visit(CBinaryExpression expression) {
    AbstractIdentifier resultId1, resultId2, result;
    int oldDereference = dereference;
    dereference = 0;
    resultId1 = expression.getOperand1().accept(this);
    dereference = 0;
    resultId2 = expression.getOperand2().accept(this);
    result = new BinaryIdentifier(resultId1, resultId2, oldDereference);
    dereference = oldDereference;
    return result;
  }

  @Override
  public AbstractIdentifier visit(CCastExpression expression) {
    return expression.getOperand().accept(this);
  }

  @Override
  public AbstractIdentifier visit(CFieldReference expression) {
    CExpression owner = expression.getFieldOwner();
    int oldDeref = dereference;
    dereference = (expression.isPointerDereference() ? 1 : 0);
    AbstractIdentifier ownerId = owner.accept(this);
    dereference = oldDeref;
    StructureIdentifier fullId =
        new StructureIdentifier(
            expression.getFieldName(), expression.getExpressionType(), dereference, ownerId);
    return fullId;
  }

  @Override
  public AbstractIdentifier visit(CIdExpression expression) {
    CSimpleDeclaration decl = expression.getDeclaration();

    if (decl == null) {
      // In our cil-file it means, that we have function pointer
      // This data can't be shared (we wouldn't write)
      return new LocalVariableIdentifier(
          expression.getName(), expression.getExpressionType(), function, dereference);
    } else {
      return createIdentifier(decl, function, dereference);
    }
  }

  @Override
  public AbstractIdentifier visit(CUnaryExpression expression) {
    if (expression.getOperator() == CUnaryExpression.UnaryOperator.AMPER) {
      --dereference;
    }
    AbstractIdentifier result = expression.getOperand().accept(this);
    if (result instanceof BinaryIdentifier) {
      return getMainPart((BinaryIdentifier) result);
    }
    return result;
  }

  @Override
  public AbstractIdentifier visit(CPointerExpression pPointerExpression) {
    ++dereference;
    AbstractIdentifier result = pPointerExpression.getOperand().accept(this);
    if (result instanceof BinaryIdentifier) {
      return getMainPart((BinaryIdentifier) result);
    }
    return result;
  }

  private AbstractIdentifier getMainPart(BinaryIdentifier id) {
    /* It is very strange, but CIL sometimes replace 'a[i]' to '*(a + i)'
     * So, if we see it, create other identifier: '*a'
     */
    AbstractIdentifier id1 = id.getIdentifier1();
    AbstractIdentifier id2 = id.getIdentifier2();
    AbstractIdentifier main = null;
    if (id1 instanceof SingleIdentifier && id2 instanceof ConstantIdentifier) {
      main = id1;
    } else if (id2 instanceof SingleIdentifier && id1 instanceof ConstantIdentifier) {
      main = id2;
    } else if (id1 instanceof SingleIdentifier && id2 instanceof SingleIdentifier) {
      SingleIdentifier s1 = (SingleIdentifier) id1;
      SingleIdentifier s2 = (SingleIdentifier) id2;
      if (s1.isPointer() && !s2.isPointer()) {
        main = s1;
      } else if (s1.isPointer() && !s2.isPointer()) {
        main = s2;
      } else if (s1.getType().getClass() == CSimpleType.class
          && s2.getType().getClass() != CSimpleType.class) {
        main = s2;
      } else if (s2.getType().getClass() == CSimpleType.class
          && s1.getType().getClass() != CSimpleType.class) {
        main = s1;
      }
    }
    if (main != null) {
      return main.cloneWithDereference(main.getDereference() + id.getDereference());
    } else {
      return id;
    }
  }

  @Override
  public AbstractIdentifier visit(CComplexCastExpression pComplexCastExpression) {
    return pComplexCastExpression.getOperand().accept(this);
  }

  @Override
  protected AbstractIdentifier visitDefault(CExpression pExp) {
    return new ConstantIdentifier(pExp.toASTString(), dereference);
  }
}
