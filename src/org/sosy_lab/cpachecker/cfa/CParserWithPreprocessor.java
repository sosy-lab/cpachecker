/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
import java.util.ArrayList;
import java.util.List;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.exceptions.CParserException;
import org.sosy_lab.cpachecker.exceptions.ParserException;

/**
 * Encapsulates a {@link CParser} instance and processes all files first
 * with a {@link CPreprocessor}.
 */
class CParserWithPreprocessor implements CParser {

  private final CParser realParser;
  private final CPreprocessor preprocessor;

  public CParserWithPreprocessor(CParser pRealParser, CPreprocessor pPreprocessor) {
    realParser = pRealParser;
    preprocessor = pPreprocessor;
  }

  @Override
  public ParseResult parseFile(String pFilename) throws ParserException, IOException, InvalidConfigurationException, InterruptedException {
    String programCode = preprocessor.preprocess(pFilename);
    if (programCode.isEmpty()) {
      throw new CParserException("Preprocessor returned empty program");
    }
    return realParser.parseString(programCode);
  }

  @Override
  public ParseResult parseString(String pCode) throws ParserException, InvalidConfigurationException {
    // TODO
    throw new UnsupportedOperationException();
  }

  @Override
  public Timer getParseTime() {
    return realParser.getParseTime();
  }

  @Override
  public Timer getCFAConstructionTime() {
    return realParser.getCFAConstructionTime();
  }

  @Override
  public ParseResult parseFile(List<Pair<String, String>> pFilenames) throws CParserException, IOException,
      InvalidConfigurationException, InterruptedException {

    List<Pair<String, String>> programs = new ArrayList<>(pFilenames.size());
    for (Pair<String, String> p : pFilenames) {
      String programCode = preprocessor.preprocess(p.getFirst());
      if (programCode.isEmpty()) {
        throw new CParserException("Preprocessor returned empty program");
      }
      programs.add(Pair.of(programCode, p.getSecond()));
    }
    return realParser.parseString(programs);
  }

  @Override
  public ParseResult parseString(List<Pair<String, String>> pCode) throws CParserException,
      InvalidConfigurationException {
    // TODO
    throw new UnsupportedOperationException();
  }

  @Override
  public CAstNode parseSingleStatement(String pCode) throws CParserException, InvalidConfigurationException {
    return realParser.parseSingleStatement(pCode);
  }
}