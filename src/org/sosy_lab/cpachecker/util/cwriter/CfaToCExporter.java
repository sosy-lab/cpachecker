// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.cwriter;

import java.io.IOException;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CParser;
import org.sosy_lab.cpachecker.cfa.CParser.ParserOptions;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.export.CWriter;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/** Exporter that exports CFAs to C programs while trying to stay close to the input program. */
public class CfaToCExporter {

  private final CWriter cWriter;

  public CfaToCExporter(
      final LogManager pLogger,
      final Configuration pConfig,
      final ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {

    final ParserOptions options = CParser.Factory.getOptions(pConfig);
    cWriter = CWriter.createInstance(pLogger, options, pShutdownNotifier);
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
      throws InvalidConfigurationException, IOException, CPAException, InterruptedException {

    if (pCfa.getLanguage() != Language.C) {
      throw new InvalidConfigurationException(
          "CFA can only be exported to C for C input programs, at the moment.");
    }

    return cWriter.exportCfa(pCfa);
  }
}
