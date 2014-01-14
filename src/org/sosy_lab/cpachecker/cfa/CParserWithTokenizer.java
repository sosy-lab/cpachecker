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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.parser.OffsetLimitReachedException;
import org.eclipse.cdt.internal.core.parser.scanner.ILexerLog;
import org.eclipse.cdt.internal.core.parser.scanner.Lexer;
import org.eclipse.cdt.internal.core.parser.scanner.Lexer.LexerOptions;
import org.eclipse.cdt.internal.core.parser.scanner.Token;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.exceptions.CParserException;
import org.sosy_lab.cpachecker.exceptions.ParserException;

/**
 * Encapsulates a {@link CParser} instance and tokenizes all files first.
 */
public class CParserWithTokenizer implements CParser {

  private final CParser realParser;

  public CParserWithTokenizer(CParser pRealParser) {
    realParser = pRealParser;
  }

  public static void main(String[] args) throws CParserException {
    String sourceFileName = args[0];
    CParserWithTokenizer t = new CParserWithTokenizer(null);
    StringBuilder tokenized = t.tokenizeSourcefile(sourceFileName);
    System.out.append(tokenized.toString());
  }

  @Override
  public ParseResult parseFile(String pFilename) throws ParserException, IOException, InvalidConfigurationException, InterruptedException {
    StringBuilder tokenizedCode = tokenizeSourcefile(pFilename);
    return realParser.parseString(tokenizedCode.toString());
  }

  private StringBuilder tokenizeSourcefile(String pFilename) throws CParserException {
    StringBuffer code = new StringBuffer();
    try (BufferedReader br = new BufferedReader(new FileReader(pFilename))) {
      String line;
      while ((line = br.readLine()) != null) {
        code.append(line);
        code.append(System.lineSeparator());
      }
    } catch (IOException e) {
      throw new CParserException("Error reading input program file", e);
    }

    return tokenizeCode(code.toString().toCharArray());
  }

  private StringBuilder tokenizeCode(char[] pCode) throws CParserException {
    StringBuilder tokenizedCode = new StringBuilder();

    LexerOptions options = new LexerOptions();
    ILexerLog log = ILexerLog.NULL;
    Object source = null;
    Lexer lx = new Lexer(pCode, options, log, source);

    try {
      boolean skipAllOnLine = false;
      Token token = lx.nextToken();

      while (token.getType() != Token.tEND_OF_INPUT) {
        if (!token.getImage().trim().isEmpty()) {
          if (token.getImage().equals("#")) {
            Token ppDirect = lx.nextToken();
            if (ppDirect.getImage().equals("pragma")) {
              skipAllOnLine = true;
            } else {
              throw new CParserException("Tokenizing does not work on all preprocessor directives! Run a preprocessor first!");
            }
          }

          if (!skipAllOnLine) {
            tokenizedCode.append(token);
            tokenizedCode.append(System.lineSeparator());
          }
        }

        token = lx.nextToken();
        if (lx.currentTokenIsFirstOnLine()) {
          skipAllOnLine = false;
        }
      }
    } catch (OffsetLimitReachedException e) {
      throw new CParserException("Tokenizing failed", e);
    }

    return tokenizedCode;
  }

  @Override
  public ParseResult parseString(String pCode) throws ParserException, InvalidConfigurationException {
    StringBuilder tokenizedCode = tokenizeCode(pCode.toCharArray());

    return realParser.parseString(tokenizedCode.toString());
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

    List<Pair<String, String>> programFragments = new ArrayList<>(pFilenames.size());
    for (Pair<String, String> p : pFilenames) {
      String programCode = tokenizeSourcefile(p.getFirst()).toString();
      if (programCode.isEmpty()) {
        throw new CParserException("Tokenizer returned empty program");
      }
      programFragments.add(Pair.of(programCode, p.getSecond()));
    }
    return realParser.parseString(programFragments);
  }

  @Override
  public ParseResult parseString(List<Pair<String, String>> pCode) throws CParserException,
      InvalidConfigurationException {

    List<Pair<String, String>> tokenizedFragments = new ArrayList<>(pCode.size());
    for (Pair<String, String> p : pCode) {
      String programCode = tokenizeCode(p.getFirst().toCharArray()).toString();
      if (programCode.isEmpty()) {
        throw new CParserException("Tokenizer returned empty program");
      }
      tokenizedFragments.add(Pair.of(programCode, p.getSecond()));
    }

    return realParser.parseString(tokenizedFragments);
  }

  @Override
  public CAstNode parseSingleStatement(String pCode) throws CParserException, InvalidConfigurationException {
    return realParser.parseSingleStatement(pCode);
  }
}