// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.eclipse.c.export;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Verify;
import java.io.IOException;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.core.runtime.CoreException;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CParser.ParserOptions;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.export.CWriter;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.c.EclipseCdtWrapper;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/** Writer based on Eclipse CDT. */
class EclipseCWriter implements CWriter {

  private final EclipseCdtWrapper eclipseCdt;

  public EclipseCWriter(final ParserOptions pOptions, final ShutdownNotifier pShutdownNotifier) {
    eclipseCdt = new EclipseCdtWrapper(pOptions, pShutdownNotifier);
  }

  @Override
  public String exportCfa(final CFA pCfa) throws IOException, CPAException, InterruptedException {

    checkArgument(
        pCfa.getLanguage() == Language.C,
        "CFA can only be exported to C for C input programs, at the moment.");
    checkArgument(
        pCfa.getFileNames().size() == 1,
        "CFA can only be exported for a single input program, at the moment.");

    try {
      final IASTTranslationUnit astUnit =
          eclipseCdt.getASTTranslationUnit(EclipseCdtWrapper.wrapFile(pCfa.getFileNames().get(0)));

      Verify.verify(
          astUnit.getPreprocessorProblemsCount() == 0,
          "Problems should have been caught during CFA generation.");

      return astUnit.getRawSignature();

    } catch (final CoreException pE) {
      throw new CPAException("Failed to export CFA to C program because AST parsing failed.");
    }
  }
}
