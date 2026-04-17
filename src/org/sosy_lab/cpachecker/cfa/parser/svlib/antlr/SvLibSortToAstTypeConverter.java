// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib.antlr;

import com.google.common.base.Verify;
import java.math.BigInteger;
import java.nio.file.Path;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.IdentifierSymbolContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.IdentifierUnderscoreContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.ParametricSortContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.SimpleSortContext;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibArrayType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibBitVectorType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibType;

public class SvLibSortToAstTypeConverter extends AbstractAntlrToAstConverter<SvLibType> {

  public SvLibSortToAstTypeConverter(SvLibScope pScope, Path pFilePath) {
    super(pScope, pFilePath);
  }

  public SvLibSortToAstTypeConverter(SvLibScope pScope) {
    super(pScope);
  }

  @Override
  public SvLibType visitIdentifierSymbol(IdentifierSymbolContext pContext) {
    return scope.getTypeForName(pContext.getText());
  }

  @Override
  public SvLibType visitIdentifierUnderscore(IdentifierUnderscoreContext pContext) {
    if (pContext.symbol().getText().equals("BitVec")) {
      Verify.verify(pContext.index().size() == 1, "BitVec should have exactly one index");
      BigInteger bitvectorSize = new BigInteger(pContext.index(0).getText());
      // In case the parsed int does not fit into an integer we want to crash explicitly
      return new SvLibSmtLibBitVectorType(bitvectorSize.intValueExact());
    }

    throw new UnsupportedOperationException(
        "Underscore sorts apart from bitvectors are not yet implemented");
  }

  @Override
  public SvLibType visitSimpleSort(SimpleSortContext pContext) {
    return visit(pContext.identifier());
  }

  @Override
  public SvLibType visitParametricSort(ParametricSortContext pContext) {
    if (pContext.sort().size() == 2 && pContext.identifier().getText().equals("Array")) {
      SvLibSmtLibType indexType = (SvLibSmtLibType) visit(pContext.sort(0));
      SvLibSmtLibType elementType = (SvLibSmtLibType) visit(pContext.sort(1));
      return new SvLibSmtLibArrayType(indexType, elementType);
    }

    throw new UnsupportedOperationException("Parametric sorts are not supported yet.");
  }
}
