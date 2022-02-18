// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.exceptions.CParserException;

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
  public ParseResult parseString(
      Path pFilename, String pCode, CSourceOriginMapping pSourceOriginMapping, Scope pScope) {
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
  public ParseResult parseFiles(List<String> pFilenames)
      throws CParserException, InterruptedException {

    List<FileContentToParse> programs = new ArrayList<>(pFilenames.size());
    for (String f : pFilenames) {
      Path path = Path.of(f);
      String programCode = preprocessor.preprocess(path);
      if (programCode.isEmpty()) {
        throw new CParserException("Preprocessor returned empty program");
      }
      programs.add(new FileContentToParse(path, programCode));
    }
    return realParser.parseString(programs, new CSourceOriginMapping());
  }

  @Override
  public ParseResult parseString(
      List<FileContentToParse> pCode, CSourceOriginMapping sourceOriginMapping) {
    // TODO
    throw new UnsupportedOperationException();
  }

  @Override
  public CAstNode parseSingleStatement(String pCode, Scope pScope)
      throws CParserException, InterruptedException {
    return realParser.parseSingleStatement(pCode, pScope);
  }

  @Override
  public List<CAstNode> parseStatements(String pCode, Scope pScope)
      throws CParserException, InterruptedException {
    return realParser.parseStatements(pCode, pScope);
  }
}