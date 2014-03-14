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
import java.util.ArrayList;
import java.util.List;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.time.Timer;
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
  public ParseResult parseFile(String pFilename, CSourceOriginMapping sourceOriginMapping) throws ParserException, IOException, InvalidConfigurationException, InterruptedException {
    String programCode = preprocessor.preprocess(pFilename);
    if (programCode.isEmpty()) {
      throw new CParserException("Preprocessor returned empty program");
    }
    return realParser.parseString(pFilename, programCode, sourceOriginMapping);
  }

  @Override
  public ParseResult parseString(String pFilename, String pCode, CSourceOriginMapping sourceOriginMapping) throws ParserException, InvalidConfigurationException {
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
  public ParseResult parseFile(List<FileToParse> pFilenames, CSourceOriginMapping sourceOriginMapping) throws CParserException, IOException,
      InvalidConfigurationException, InterruptedException {

    List<FileContentToParse> programs = new ArrayList<>(pFilenames.size());
    for (FileToParse p : pFilenames) {
      String programCode = preprocessor.preprocess(p.getFileName());
      if (programCode.isEmpty()) {
        throw new CParserException("Preprocessor returned empty program");
      }
      programs.add(new FileContentToParse(p.getFileName(), programCode, p.getStaticVariablePrefix()));
    }
    return realParser.parseString(programs, sourceOriginMapping);
  }

  @Override
  public ParseResult parseString(List<FileContentToParse> pCode, CSourceOriginMapping sourceOriginMapping)
      throws CParserException, InvalidConfigurationException {
    // TODO
    throw new UnsupportedOperationException();
  }

  @Override
  public CAstNode parseSingleStatement(String pCode) throws CParserException, InvalidConfigurationException {
    return realParser.parseSingleStatement(pCode);
  }

  @Override
  public List<CAstNode> parseStatements(String pCode) throws CParserException, InvalidConfigurationException {
    return realParser.parseStatements(pCode);
  }
}