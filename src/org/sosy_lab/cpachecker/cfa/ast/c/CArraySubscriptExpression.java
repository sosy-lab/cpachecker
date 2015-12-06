/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.ast.c;

import org.sosy_lab.cpachecker.cfa.ast.AArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public final class CArraySubscriptExpression extends AArraySubscriptExpression implements CLeftHandSide {



  public CArraySubscriptExpression(final FileLocation pFileLocation,
                                      final CType pType,
                                      final CExpression pArrayExpression,
                                      final CExpression pSubscriptExpression) {
    super(pFileLocation, pType, pArrayExpression, pSubscriptExpression);

    assert checkTypeConsistency(pType, pArrayExpression) : "subscript return type doens't match the array type";
  }
  
  private boolean checkTypeConsistency(final CType pType, final CExpression pArrayExpression) {
    CType declaredType = pArrayExpression.getExpressionType();
    CType checkType;
    if(declaredType instanceof CArrayType) {
      checkType = ((CArrayType) declaredType).getType();
    } else if(declaredType instanceof CPointerType) {
      // it is legal to use array names as constant pointers.
      /*
       * double *p;
       * double array[10];
       * p = array;
       */ 
       // Such that *(p + 3) is a legitimate way to access data of array[3]
      checkType = ((CPointerType) declaredType).getType();
    } else {
      checkType = null;
      assert false;
    }
    
    return checkType.getCanonicalType().equals(pType.getCanonicalType());
  }

  @Override
  public CType getExpressionType() {
    return (CType) super.getExpressionType();
  }

  @Override
  public CExpression getArrayExpression() {
    return (CExpression) super.getArrayExpression();
  }

  @Override
  public CExpression getSubscriptExpression() {
    return (CExpression) super.getSubscriptExpression();
  }

  @Override
  public <R, X extends Exception> R accept(CExpressionVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(CRightHandSideVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(CLeftHandSideVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(CAstNodeVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    return result * prime + super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof CArraySubscriptExpression)) {
      return false;
    }

    return super.equals(obj);
  }
}
