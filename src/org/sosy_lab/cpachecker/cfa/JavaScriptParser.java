/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
import java.util.List;
import org.sosy_lab.cpachecker.exceptions.JSParserException;

public interface JavaScriptParser extends Parser {
  /**
   * Parse the content of files into a single CFA.
   *
   * @param filenames The List of files to parse. The first part of the pair should be the filename,
   *     the second part should be the prefix which will be appended to static variables
   * @return The CFA.
   * @throws IOException If file cannot be read.
   * @throws JSParserException If parser or CFA builder cannot handle the C code.
   */
  ParseResult parseFiles(List<String> filenames)
      throws JSParserException, IOException, InterruptedException;
}
