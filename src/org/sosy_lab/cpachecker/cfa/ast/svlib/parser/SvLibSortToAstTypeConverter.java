// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib.parser;

import java.nio.file.Path;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibType;
import org.sosy_lab.cpachecker.cfa.ast.svlib.parser.generated.SvLibParser.ParametricSortContext;
import org.sosy_lab.cpachecker.cfa.ast.svlib.parser.generated.SvLibParser.SimpleSortContext;

public class SvLibSortToAstTypeConverter extends AbstractAntlrToAstConverter<SvLibType> {

  public SvLibSortToAstTypeConverter(SvLibScope pScope, Path pFilePath) {
    super(pScope, pFilePath);
  }

  public SvLibSortToAstTypeConverter(SvLibScope pScope) {
    super(pScope);
  }

  @Override
  public SvLibType visitSimpleSort(SimpleSortContext pContext) {
    return scope.getTypeForName(pContext.identifier().getText());
  }

  @Override
  public SvLibType visitParametricSort(ParametricSortContext pContext) {
    throw new UnsupportedOperationException("Parametric sorts are not supported yet.");
  }
}
