// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.export;

import java.io.IOException;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CParser.ParserOptions;
import org.sosy_lab.cpachecker.cfa.parser.Parsers;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Abstraction of a C writer that generates C code from CFAs.
 *
 * <p>A C writer should be state-less and therefore thread-safe as well as reusable.
 */
public interface CWriter {

  String exportCfa(final CFA pCfa) throws IOException, CPAException, InterruptedException;

  /** Factory that tries to create a writer based on available libraries (e.g. Eclipse CDT). */
  class Factory {

    public static CWriter getWriter(
        final LogManager pLogger,
        final ParserOptions pOptions,
        final ShutdownNotifier pShutdownNotifier) {
      return Parsers.getCWriter(pLogger, pOptions, pShutdownNotifier);
    }
  }
}
