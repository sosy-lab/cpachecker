// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.cwriter;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Verify;
import java.io.IOException;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.core.runtime.CoreException;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CParser.Factory;
import org.sosy_lab.cpachecker.cfa.CParser.ParserOptions;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.c.EclipseCdtWrapper;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class CfaToCExporter {

  private final EclipseCdtWrapper nativeCdtParser;

  public CfaToCExporter(final Configuration pConfig, final ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    final ParserOptions options = Factory.getOptions(pConfig);
    nativeCdtParser = new EclipseCdtWrapper(options, pShutdownNotifier);
  }

  /**
   * Exports the given {@link CFA} to a C program.
   *
   * @param pCfa the CFA to export
   * @return C representation of the given CFA
   * @throws InvalidConfigurationException if the given CFA is not the CFA of a C program
   * @throws CPAException if the given CFA could not be exported
   */
  public String exportCfa(final CFA pCfa)
      throws InvalidConfigurationException, IOException, CPAException {

    if (pCfa.getLanguage() != Language.C) {
      throw new InvalidConfigurationException(
          "CFA can only be exported to C for C input programs, at the moment.");
    }
    checkArgument(
        pCfa.getFileNames().size() <= 1,
        "CFA can only be exported for a single input program, at the moment.");

    try {
      final IASTTranslationUnit astUnit =
          nativeCdtParser.getASTTranslationUnit(
              EclipseCdtWrapper.wrapFile(pCfa.getFileNames().get(0)));

      Verify.verify(
          astUnit.getPreprocessorProblemsCount() == 0,
          "Problems should have been caught during CFA generation.");

      return astUnit.getRawSignature();

    } catch (CoreException | InterruptedException pE) {
      throw new CPAException(
          "Failed to export CFA to C program because AST parsing failed/ was interrupted.");
    }
  }
}
