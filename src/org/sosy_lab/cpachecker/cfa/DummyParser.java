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
import java.util.List;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonParser;
import org.sosy_lab.cpachecker.exceptions.CParserException;
import org.sosy_lab.cpachecker.exceptions.ParserException;

/**
 * For all languages, where parsing of single or blocks of statements is not yet implemented,
 * use this dummy scope when parsing an automaton {@link AutomatonParser}.
 */
public class DummyParser implements CParser {

  private static final DummyParser DUMMYPARSER = new DummyParser();

  private DummyParser() {} // Private constructor to insure one instance.

  public static DummyParser getInstance() {
    return DUMMYPARSER;
  }

  @Override
  public ParseResult parseFile(String pFilename)
      throws ParserException, IOException, InterruptedException {
    throw new UnsupportedOperationException();
  }

  @Override
  public ParseResult parseString(
      String pFileName, String pCode, CSourceOriginMapping pSourceOriginMapping, Scope pScope)
      throws CParserException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Timer getParseTime() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Timer getCFAConstructionTime() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ParseResult parseFile(List<String> pFilenames)
      throws CParserException, IOException, InterruptedException {
    throw new UnsupportedOperationException();
  }

  @Override
  public ParseResult parseString(
      List<FileContentToParse> pCode, CSourceOriginMapping pSourceOriginMapping)
      throws CParserException {
    throw new UnsupportedOperationException();
  }

  @Override
  public CAstNode parseSingleStatement(String pCode, Scope pScope) throws CParserException {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<CAstNode> parseStatements(String pCode, Scope pScope) throws CParserException {
    throw new UnsupportedOperationException();
  }
}