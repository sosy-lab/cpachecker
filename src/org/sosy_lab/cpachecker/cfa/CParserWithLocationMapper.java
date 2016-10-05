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

import com.google.common.collect.Lists;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.eclipse.cdt.core.parser.OffsetLimitReachedException;
import org.eclipse.cdt.internal.core.parser.scanner.ILexerLog;
import org.eclipse.cdt.internal.core.parser.scanner.Lexer;
import org.eclipse.cdt.internal.core.parser.scanner.Lexer.LexerOptions;
import org.eclipse.cdt.internal.core.parser.scanner.Token;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.MoreFiles;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.exceptions.CParserException;
import org.sosy_lab.cpachecker.exceptions.ParserException;

/**
 * Encapsulates a {@link CParser} instance and tokenizes all files first.
 */

@Options
public class CParserWithLocationMapper implements CParser {

  private final CParser realParser;

  private final LogManager logger;

  private final boolean readLineDirectives;

  @Option(secure=true, name="locmapper.dumpTokenizedProgramToFile",
      description="Write the tokenized version of the input program to this file.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path dumpTokenizedProgramToFile = null;

  @Option(secure=true, name="parser.transformTokensToLines",
      description="Preprocess the given C files before parsing: Put every single token onto a new line. "
      + "Then the line number corresponds to the token number.")
  private boolean tokenizeCode = false;

  public CParserWithLocationMapper(
      Configuration pConfig,
      LogManager pLogger,
      CParser pRealParser,
      boolean pReadLineDirectives) throws InvalidConfigurationException {

    pConfig.inject(this);

    this.logger = pLogger;
    this.realParser = pRealParser;
    readLineDirectives = pReadLineDirectives;
  }

  @Override
  public ParseResult parseFile(String pFilename)
      throws ParserException, IOException, InvalidConfigurationException, InterruptedException {
    CSourceOriginMapping sourceOriginMapping = new CSourceOriginMapping();
    String tokenizedCode = tokenizeSourcefile(pFilename, sourceOriginMapping);
    return realParser.parseString(
        pFilename, tokenizedCode, sourceOriginMapping, CProgramScope.empty());
  }

  private String tokenizeSourcefile(String pFilename,
      CSourceOriginMapping sourceOriginMapping) throws CParserException, IOException {
    String code = MoreFiles.toString(Paths.get(pFilename), Charset.defaultCharset());
    return processCode(pFilename, code, sourceOriginMapping);
  }

  private String processCode(final String fileName, String pCode,
      CSourceOriginMapping sourceOriginMapping) throws CParserException {
    StringBuilder tokenizedCode = new StringBuilder();

    LexerOptions options = new LexerOptions();
    ILexerLog log = ILexerLog.NULL;
    Object source = null;
    Lexer lx = new Lexer(pCode.toCharArray(), options, log, source);

    try {
      int absoluteLineNumber = 1;
      int relativeLineNumber = absoluteLineNumber;

      String rangeLinesOriginFilename = fileName;
      int includeStartedWithAbsoluteLine = 0;

      Token token;
      while ((token = lx.nextToken()).getType() != Token.tEND_OF_INPUT) {
        if (token.getType() == Lexer.tNEWLINE) {
          absoluteLineNumber += 1;
          relativeLineNumber += 1;
        }

        if (token.getType() == Token.tPOUND) { // match #
          // Read the complete line containing the directive...
          ArrayList<Token> directiveTokens = Lists.newArrayList();
          token = lx.nextToken();
          while (token.getType() != Lexer.tNEWLINE && token.getType() != Token.tEND_OF_INPUT) {
            directiveTokens.add(token);
            token = lx.nextToken();
          }
          absoluteLineNumber += 1;
          relativeLineNumber += 1;

          // Evaluate the preprocessor directive...
          if (readLineDirectives && directiveTokens.size() > 0) {
            String firstTokenImage = directiveTokens.get(0).getImage().trim();

            final int lineNumberTokenIndex;

            if (directiveTokens.size() > 1
                && firstTokenImage.equals("line")
                && directiveTokens.get(1).getImage().matches("[0-9]+")) {
              lineNumberTokenIndex = 1;
            } else if (firstTokenImage.matches("[0-9]+")) {
              lineNumberTokenIndex = 0;
            } else {
              lineNumberTokenIndex = -1;
            }
            if (lineNumberTokenIndex >= 0) {

              sourceOriginMapping.mapInputLineRangeToDelta(fileName, rangeLinesOriginFilename, includeStartedWithAbsoluteLine, absoluteLineNumber, relativeLineNumber - absoluteLineNumber);

              final String lineNumberToken = directiveTokens.get(lineNumberTokenIndex).getImage().trim();
              includeStartedWithAbsoluteLine = absoluteLineNumber;
              relativeLineNumber = Integer.parseInt(lineNumberToken);
              if (directiveTokens.size() > lineNumberTokenIndex + 1) {
                String file = directiveTokens.get(lineNumberTokenIndex + 1).getImage().trim();
                if (file.charAt(0) == '"' && file.charAt(file.length()-1) == '"') {
                  file = file.substring(1, file.length()-1);
                }
                rangeLinesOriginFilename = file;
              }
            }
          }
        } else if (!token.getImage().trim().isEmpty()) {
          if (tokenizeCode) {
            tokenizedCode.append(token.toString());
            tokenizedCode.append(System.lineSeparator());
          }
        }
      }

      if (readLineDirectives) {
        sourceOriginMapping.mapInputLineRangeToDelta(fileName, rangeLinesOriginFilename, includeStartedWithAbsoluteLine + 1, absoluteLineNumber, relativeLineNumber - absoluteLineNumber);
      }
    } catch (OffsetLimitReachedException e) {
      throw new CParserException("Tokenizing failed", e);
    }

    String code = tokenizeCode ? tokenizedCode.toString() : pCode;
    if (tokenizeCode && dumpTokenizedProgramToFile != null) {
      try (Writer out =
          MoreFiles.openOutputFile(dumpTokenizedProgramToFile, StandardCharsets.US_ASCII)) {
        out.append(code);
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not write tokenized program to file");
      }
    }
    return code;
  }

  @Override
  public ParseResult parseString(
      String pFilename, String pCode, CSourceOriginMapping pSourceOriginMapping, Scope pScope)
      throws CParserException, InvalidConfigurationException {
    String tokenizedCode = processCode(pFilename, pCode, pSourceOriginMapping);

    return realParser.parseString(pFilename, tokenizedCode, pSourceOriginMapping, pScope);
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
  public ParseResult parseFile(List<String> pFilenames)
      throws CParserException, IOException, InvalidConfigurationException, InterruptedException {
    CSourceOriginMapping sourceOriginMapping = new CSourceOriginMapping();

    List<FileContentToParse> programFragments = new ArrayList<>(pFilenames.size());
    for (String f : pFilenames) {
      String programCode = tokenizeSourcefile(f, sourceOriginMapping);
      if (programCode.isEmpty()) {
        throw new CParserException("Tokenizer returned empty program");
      }
      programFragments.add(new FileContentToParse(f, programCode));
    }
    return realParser.parseString(programFragments, sourceOriginMapping);
  }

  @Override
  public ParseResult parseString(List<FileContentToParse> pCode, CSourceOriginMapping sourceOriginMapping) throws CParserException,
      InvalidConfigurationException {

    List<FileContentToParse> tokenizedFragments = new ArrayList<>(pCode.size());
    for (FileContentToParse f : pCode) {
      String programCode = processCode(f.getFileName(), f.getFileContent(), sourceOriginMapping);
      if (programCode.isEmpty()) {
        throw new CParserException("Tokenizer returned empty program");
      }
      tokenizedFragments.add(new FileContentToParse(f.getFileName(), programCode));
    }

    return realParser.parseString(tokenizedFragments, sourceOriginMapping);
  }

  @Override
  public CAstNode parseSingleStatement(String pCode, Scope pScope) throws CParserException, InvalidConfigurationException {
    return realParser.parseSingleStatement(pCode, pScope);
  }

  @Override
  public List<CAstNode> parseStatements(String pCode, Scope pScope) throws CParserException, InvalidConfigurationException {
    return realParser.parseStatements(pCode, pScope);
  }
}