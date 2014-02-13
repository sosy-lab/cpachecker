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
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.exceptions.CParserException;
import org.sosy_lab.cpachecker.exceptions.ParserException;

import com.google.common.collect.Lists;

/**
 * Encapsulates a {@link CParser} instance and tokenizes all files first.
 */
public class CParserWithLocationMapper implements CParser {

  private final CParser realParser;

  public CParserWithLocationMapper(CParser pRealParser) {
    this.realParser = pRealParser;
  }

//  public static void main(String[] args) throws CParserException {
//    String sourceFileName = args[0];
//    CParserWithLocationExtractor t = new CParserWithLocationExtractor(null);
//    StringBuilder tokenized = t.tokenizeSourcefile(sourceFileName);
//    System.out.append(tokenized.toString());
//  }

  @Override
  public ParseResult parseFile(String pFilename) throws ParserException, IOException, InvalidConfigurationException, InterruptedException {
    StringBuilder tokenizedCode = tokenizeSourcefile(pFilename);
    return realParser.parseString(pFilename, tokenizedCode.toString());
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

    return tokenizeCode(pFilename, code.toString().toCharArray());
  }

  private StringBuilder tokenizeCode(String fileName, char[] pCode) throws CParserException {
    StringBuilder tokenizedCode = new StringBuilder();

    CSourceOriginMapping.INSTANCE.setHasOneInputLinePerToken(true);

    LexerOptions options = new LexerOptions();
    ILexerLog log = ILexerLog.NULL;
    Object source = null;
    Lexer lx = new Lexer(pCode, options, log, source);

    try {
      int tokenNumber = 0;
      int lineNumber = 1;
      int adjustedLineNumber = lineNumber;

      String rangeLinesOriginFilename = fileName;
      int includeStartedIn = 0;
      int newLineStartedWithToken = tokenNumber;

      Token token;
      while ((token = lx.nextToken()).getType() != Token.tEND_OF_INPUT) {
        if (token.getType() == Lexer.tNEWLINE) {
          CSourceOriginMapping.INSTANCE.mapTokenRangeToInputLine(newLineStartedWithToken, tokenNumber, lineNumber);
          lineNumber += 1;
          adjustedLineNumber += 1;
          newLineStartedWithToken = tokenNumber;
        }

        if (token.getType() == Token.tPOUND) { // match #
          // Read the complete line containing the directive...
          ArrayList<Token> directiveTokens = Lists.newArrayList();
          token = lx.nextToken();
          while (token.getType() != Lexer.tNEWLINE && token.getType() != Token.tEND_OF_INPUT) {
            directiveTokens.add(token);
            token = lx.nextToken();
          }
          lineNumber += 1;
          adjustedLineNumber += 1;

          // Evaluate the preprocessor directive...
          if (directiveTokens.size() > 0) {
            String firstTokenImage = directiveTokens.get(0).getImage();
            if (firstTokenImage.equals("line")) {

            } else if (firstTokenImage.matches("[0-9]+")) {
              putRangeMapping(rangeLinesOriginFilename, includeStartedIn, lineNumber, adjustedLineNumber - lineNumber);

              includeStartedIn = lineNumber;
              adjustedLineNumber = Integer.parseInt(firstTokenImage);
              rangeLinesOriginFilename = directiveTokens.get(1).getImage();
            }
          }
        } else if (!token.getImage().trim().isEmpty()) {
          tokenNumber += 1;
          tokenizedCode.append(token);
          tokenizedCode.append(System.lineSeparator());
        }

      }

      putRangeMapping(rangeLinesOriginFilename, includeStartedIn + 1, lineNumber, adjustedLineNumber - lineNumber);
      CSourceOriginMapping.INSTANCE.mapTokenRangeToInputLine(newLineStartedWithToken, tokenNumber, lineNumber);
    } catch (OffsetLimitReachedException e) {
      throw new CParserException("Tokenizing failed", e);
    }

    return tokenizedCode;
  }

  private void putRangeMapping(String originFilename, int fromLine, int toLine, int deltaToOrigin) {
    CSourceOriginMapping.INSTANCE.mapInputLineRangeToDelta(originFilename, fromLine, toLine, deltaToOrigin);
  }

  @Override
  public ParseResult parseString(String pFilename, String pCode) throws ParserException, InvalidConfigurationException {
    StringBuilder tokenizedCode = tokenizeCode(pFilename, pCode.toCharArray());

    return realParser.parseString(pFilename, tokenizedCode.toString());
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
  public ParseResult parseFile(List<FileToParse> pFilenames) throws CParserException, IOException,
      InvalidConfigurationException, InterruptedException {

    List<FileContentToParse> programFragments = new ArrayList<>(pFilenames.size());
    for (FileToParse f : pFilenames) {
      String programCode = tokenizeSourcefile(f.fileName).toString();
      if (programCode.isEmpty()) {
        throw new CParserException("Tokenizer returned empty program");
      }
      programFragments.add(new FileContentToParse(f.fileName, programCode, f.staticVariablePrefix));
    }
    return realParser.parseString(programFragments);
  }

  @Override
  public ParseResult parseString(List<FileContentToParse> pCode) throws CParserException,
      InvalidConfigurationException {

    List<FileContentToParse> tokenizedFragments = new ArrayList<>(pCode.size());
    for (FileContentToParse f : pCode) {
      String programCode = tokenizeCode(f.fileName, f.fileContent.toCharArray()).toString();
      if (programCode.isEmpty()) {
        throw new CParserException("Tokenizer returned empty program");
      }
      tokenizedFragments.add(new FileContentToParse(f.fileName, programCode, f.staticVariablePrefix));
    }

    return realParser.parseString(tokenizedFragments);
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