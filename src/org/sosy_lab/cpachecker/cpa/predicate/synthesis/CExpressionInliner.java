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
package org.sosy_lab.cpachecker.cpa.predicate.synthesis;

import javax.annotation.Nonnull;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;


/**
 * The boolean-part of the result pair states wether there was performed inlining in the sub-tree or not.
 */
public class CExpressionInliner extends DefaultCExpressionVisitor<Pair<Boolean, CExpression>, RuntimeException> {

  public interface SubstitutionProvider {
    public Optional<CExpression> getSubstitutionFor(final CIdExpression pLhs);
  }

  private final SubstitutionProvider substProvider;

  public CExpressionInliner(@Nonnull SubstitutionProvider pSubstProvider) {
    Preconditions.checkNotNull(pSubstProvider);
    substProvider = pSubstProvider;
  }

  // TODO: Add a cache

  @Override
  protected Pair<Boolean, CExpression> visitDefault(CExpression exp) {
    return Pair.of(false, exp);
  }

  @Override
  public Pair<Boolean, CExpression> visit(CBinaryExpression exp) {
    Pair<Boolean, CExpression> substOp1 = exp.getOperand1().accept(this);
    Pair<Boolean, CExpression> substOp2 = exp.getOperand2().accept(this);

    if (substOp1.getFirst() || substOp2.getFirst()) {
      return Pair.of(true,
          (CExpression) new CBinaryExpression(
          exp.getFileLocation(),
          exp.getExpressionType(),
          exp.getCalculationType(),
          substOp1.getSecond(),
          substOp2.getSecond(),
          exp.getOperator()));
    }

    return Pair.of(false, (CExpression) exp);
  }

  @Override
  public Pair<Boolean, CExpression> visit(CCastExpression exp) {
    Pair<Boolean, CExpression> substOp1 = exp.getOperand().accept(this);

    if (substOp1.getFirst()) {
      return Pair.of(true,
          (CExpression) new CCastExpression(
          exp.getFileLocation(),
          exp.getExpressionType(),
          substOp1.getSecond()));
    }

    return Pair.of(false, (CExpression) exp);
  }

  public Pair<Boolean, CInitializer> visitInit(CInitializer exp) {
    return Pair.of(false, exp);
    // TODO
  }

  @Override
  public Pair<Boolean, CExpression> visit(CUnaryExpression exp) {
    Pair<Boolean, CExpression> substOp1 = exp.getOperand().accept(this);

    if (substOp1.getFirst()) {
      return Pair.of(true,
          (CExpression) new CUnaryExpression(
          exp.getFileLocation(),
          exp.getExpressionType(),
          substOp1.getSecond(),
          exp.getOperator()));
    }

    return Pair.of(false, (CExpression) exp);
  }

  @Override
  public Pair<Boolean, CExpression> visit(CArraySubscriptExpression exp) {
    Pair<Boolean, CExpression> substOp1 = exp.getArrayExpression().accept(this);
    Pair<Boolean, CExpression> substOp2 = exp.getSubscriptExpression().accept(this);

    if (substOp1.getFirst()) {
      return Pair.of(true,
          (CExpression) new CArraySubscriptExpression(
          exp.getFileLocation(),
          exp.getExpressionType(),
          substOp1.getSecond(),
          substOp2.getSecond()));
    }

    return Pair.of(false, (CExpression) exp);
  }

  @Override
  public Pair<Boolean, CExpression> visit(CFieldReference exp) {
    Pair<Boolean, CExpression> substOp1 = exp.getFieldOwner().accept(this);

    if (substOp1.getFirst()) {
      return Pair.of(true,
          (CExpression) new CFieldReference(
          exp.getFileLocation(),
          exp.getExpressionType(),
          exp.getFieldName(),
          substOp1.getSecond(),
          exp.isPointerDereference()));
    }

    return Pair.of(false, (CExpression) exp);
  }

  public Pair<Boolean, CExpression> trySubstitute(CIdExpression pLhs) {
    Optional<CExpression> substitution = substProvider.getSubstitutionFor(pLhs);
    if (substitution.isPresent()) {
      return Pair.of(true, substitution.get());
    } else {
      return Pair.of(false, (CExpression) pLhs);
    }
  }

  @Override
  public Pair<Boolean, CExpression> visit(CIdExpression exp) {
    return trySubstitute(exp);
  }

  @Override
  public Pair<Boolean, CExpression> visit(CPointerExpression exp) {
    Pair<Boolean, CExpression> substOp1 = exp.getOperand().accept(this);

    if (substOp1.getFirst()) {
      return Pair.of(true,
          (CExpression) new CPointerExpression(
          exp.getFileLocation(),
          exp.getExpressionType(),
          substOp1.getSecond()));
    }

    return Pair.of(false, (CExpression) exp);
  }

  @Override
  public Pair<Boolean, CExpression> visit(CComplexCastExpression exp) {
    Pair<Boolean, CExpression> substOp1 = exp.getOperand().accept(this);

    if (substOp1.getFirst()) {
      return Pair.of(true,
          (CExpression) new CComplexCastExpression(
          exp.getFileLocation(),
          exp.getExpressionType(),
          substOp1.getSecond(),
          exp.getType(),
          exp.isRealCast()));
    }

    return Pair.of(false, (CExpression) exp);
  }

}
