// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.exceptions.ParserException;

/**
 * Abstraction of a  parser that creates CFAs from  code.
 *
 * A parser should be state-less and therefore thread-safe as well as reusable.
 *
 * It may offer timing of it's operations. If present, this is not expected to
 * be thread-safe.
 */
public interface Parser {

  /**
   * Parse the content of a file into a CFA.
   *
   * @param filenames The files to parse.
   * @return The CFA.
   * @throws IOException If file cannot be read.
   * @throws ParserException If parser or CFA builder cannot handle the code.
   */
  ParseResult parseFiles(List<String> filenames)
      throws ParserException, IOException, InterruptedException, InvalidConfigurationException;

  /**
   * Parse the content of a String into a CFA.
   *
   * @param filename A filename that is the supposed source of this code (for relative lookups).
   * @param code The code to parse.
   * @return The CFA.
   * @throws ParserException If parser or CFA builder cannot handle the code.
   */
  ParseResult parseString(Path filename, String code) throws ParserException, InterruptedException;

  /**
   * Return a timer that measured the time needed for parsing.
   * Optional method: may return null.
   */
  Timer getParseTime();

  /**
   * Return a timer that measured the time need for CFA construction.
   * Optional method: may return null.
   */
  Timer getCFAConstructionTime();

}
