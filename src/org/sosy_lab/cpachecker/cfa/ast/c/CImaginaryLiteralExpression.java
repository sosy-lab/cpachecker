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

import java.util.Objects;

import org.sosy_lab.cpachecker.cfa.ast.ALiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public final class CImaginaryLiteralExpression extends ALiteralExpression implements CLiteralExpression {

  private final CLiteralExpression value;
  private final String imaginary;

  public CImaginaryLiteralExpression(FileLocation pFileLocation,
                                    CType pType,
                                    CLiteralExpression pValue,
                                    String imaginary) {
    super(pFileLocation, pType);
    value = pValue;
    this.imaginary = imaginary;
  }

  @Override
  public CType getExpressionType() {
    return (CType) super.getExpressionType();
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
  public <R, X extends Exception> R accept(CAstNodeVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }

  @Override
  public String toASTString() {
    return getValue().toString() + imaginary;
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(value);
    result = prime * result + Objects.hashCode(imaginary);
    return prime * result + super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof CImaginaryLiteralExpression) || !super.equals(obj)) {
      return false;
    }

    CImaginaryLiteralExpression other = (CImaginaryLiteralExpression) obj;

    return Objects.equals(other.value, value) && Objects.equals(other.imaginary, imaginary);
  }

  @Override
  public CLiteralExpression getValue() {
    return value;
  }

  public String getImaginaryString() {
    return imaginary;
  }
}
