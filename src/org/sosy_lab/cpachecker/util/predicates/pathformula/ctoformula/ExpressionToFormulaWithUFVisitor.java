/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula;

import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.PointerTargetSet.PointerTargetSetBuilder;

public class ExpressionToFormulaWithUFVisitor extends ExpressionToFormulaVisitor {

  public ExpressionToFormulaWithUFVisitor(final CToFormulaWithUFConverter cToFormulaConverter,
                                          final CFAEdge cfaEdge,
                                          final String function,
                                          final SSAMapBuilder ssa,
                                          final Constraints constraints,
                                          final PointerTargetSetBuilder pts) {

    super(cToFormulaConverter, cfaEdge, function, ssa, constraints);

    this.conv = cToFormulaConverter;
    this.pts = pts;

    this.baseVisitor = new BaseVisitor(cfaEdge, pts);
  }

  @Override
  protected Formula visitDefault(CExpression pExp) throws UnrecognizedCCodeException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Formula visit(final CArraySubscriptExpression e) throws UnrecognizedCCodeException {
    final Formula base = e.getArrayExpression().accept(this);
    final Formula index = e.getSubscriptExpression().accept(this);
    final int size = pts.getSize(e.getExpressionType());
    final Formula coeff = conv.fmgr.makeNumber(pts.getPointerType(), size);
    final Formula offset = conv.fmgr.makeMultiply(coeff, index);
    final Formula address = conv.fmgr.makePlus(base, offset);
    final CType resultType = e.getExpressionType();
    return conv.isCompositeType(resultType) ? address :
           conv.makeDereferece(resultType, address, ssa, pts);
  }

  @Override
  public Formula visit(final CFieldReference e) throws UnrecognizedCCodeException {
    final String baseName = e.getFieldOwner().accept(baseVisitor);
    final String fieldName = e.getFieldName();
    final CType resultType = e.getExpressionType();
    if (baseName != null) {
      if (!conv.isCompositeType(resultType)) {
        return conv.makeVariable(baseName + BaseVisitor.NAME_SEPARATOR + fieldName, resultType, ssa);
      } else {
        throw new UnrecognizedCCodeException("Unhandled reference to composite as a whole", edge, e);
      }
    } else {
      final CType fieldOwnerType = e.getFieldOwner().getExpressionType();
      if (fieldOwnerType instanceof CCompositeType) {
        final Formula base = e.getFieldOwner().accept(this);
        final Formula offset = conv.fmgr.makeNumber(pts.getPointerType(),
                                                    pts.getOffset((CCompositeType) fieldOwnerType, fieldName));
        final Formula address = conv.fmgr.makePlus(base, offset);
        return conv.isCompositeType(resultType) ? address :
               conv.makeDereferece(resultType, address, ssa, pts);
      } else {
        throw new UnrecognizedCCodeException("Field owner of a non-composite type", edge, e);
      }
    }
  }

  protected final CToFormulaWithUFConverter conv;
  protected final PointerTargetSetBuilder pts;

  protected final BaseVisitor baseVisitor;
}
