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
package org.sosy_lab.cpachecker.cfa;

import java.io.IOException;
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
   * @param filename The file to parse.
   * @return The CFA.
   * @throws IOException If file cannot be read.
   * @throws ParserException If parser or CFA builder cannot handle the code.
   */
  ParseResult parseFile(String filename)
      throws ParserException, IOException, InvalidConfigurationException, InterruptedException;

  /**
   * Parse the content of a String into a CFA.
   *
   * @param code The code to parse.
   * @return The CFA.
   * @throws ParserException If parser or CFA builder cannot handle the code.
   */
  ParseResult parseString(String filename, String code)
      throws ParserException, InvalidConfigurationException;

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
