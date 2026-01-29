// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.expressions;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.core.counterexample.CExpressionToOriginalCodeVisitor;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.cwriter.export.CExportExpression;
import org.sosy_lab.cpachecker.util.cwriter.export.CExpressionWrapper;

abstract class AbstractExpressionTree<LeafType> implements ExpressionTree<LeafType> {

  @Override
  public String toString() {
    return accept(new ToCodeVisitor<>(this::formatLeafExpression));
  }

  private String formatLeafExpression(LeafType pLeafExpression) {
    if (pLeafExpression instanceof CExpression cExpression) {
      return cExpression.accept(CExpressionToOriginalCodeVisitor.BASIC_TRANSFORMER);
    }
    if (pLeafExpression instanceof CExportExpression cExportExpression) {
      // for wrapped CExpression, handle them like CExpression
      if (pLeafExpression instanceof CExpressionWrapper(CExpression cExpression)) {
        return cExpression.accept(CExpressionToOriginalCodeVisitor.BASIC_TRANSFORMER);
      } else {
        // for all other CExportExpression, just call toASTString
        try {
          return cExportExpression.toASTString();
        } catch (UnrecognizedCodeException e) {
          // TODO this is not ideal, likely we can get rid of UnrecognizedCodeException later in
          //  toASTString
          throw new AssertionError(e);
        }
      }
    }
    if (pLeafExpression == null) {
      return "null";
    }
    return pLeafExpression.toString();
  }
}
