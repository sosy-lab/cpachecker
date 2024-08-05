// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.eclipse.cdt.core.parser.IToken;
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
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.c.BOMParser;
import org.sosy_lab.cpachecker.exceptions.CParserException;

/** Encapsulates a {@link CParser} instance and tokenizes all files first. */
@Options
public class CParserWithLocationMapper implements CParser {

  private final CParser realParser;

  private final LogManager logger;

  private final boolean readLineDirectives;

  @Option(
      secure = true,
      name = "locmapper.dumpTokenizedProgramToFile",
      description = "Write the tokenized version of the input program to this file.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path dumpTokenizedProgramToFile = null;

  @Option(
      secure = true,
      name = "parser.transformTokensToLines",
      description =
          "Preprocess the given C files before parsing: Put every single token onto a new line. "
              + "Then the line number corresponds to the token number.")
  private boolean tokenizeCode = false;

  public CParserWithLocationMapper(
      Configuration pConfig, LogManager pLogger, CParser pRealParser, boolean pReadLineDirectives)
      throws InvalidConfigurationException {

    pConfig.inject(this);

    logger = pLogger;
    realParser = pRealParser;
    readLineDirectives = pReadLineDirectives;
  }

  private String tokenizeSourcefile(Path pFilename, CSourceOriginMapping sourceOriginMapping)
      throws CParserException, IOException {
    String code = BOMParser.filterAndDecode(pFilename);
    return processCode(pFilename, code, sourceOriginMapping);
  }

  private String processCode(
      final Path fileName, String pCode, CSourceOriginMapping sourceOriginMapping)
      throws CParserException {
    StringBuilder tokenizedCode = new StringBuilder();

    LexerOptions options = new LexerOptions();
    ILexerLog log = ILexerLog.NULL;
    Object source = null;
    Lexer lx = new Lexer(pCode.toCharArray(), options, log, source);

    try {
      int absoluteLineNumber = 1;
      int relativeLineNumber = absoluteLineNumber;

      Path rangeLinesOriginFilename = fileName;
      int includeStartedWithAbsoluteLine = 1;

      Token token;
      while ((token = lx.nextToken()).getType() != IToken.tEND_OF_INPUT) {
        if (token.getType() == Lexer.tNEWLINE) {
          absoluteLineNumber += 1;
          relativeLineNumber += 1;
        }

        if (token.getType() == IToken.tPOUND) { // match #
          // Read the complete line containing the directive...
          List<Token> directiveTokens = new ArrayList<>();
          token = lx.nextToken();
          while (token.getType() != Lexer.tNEWLINE && token.getType() != IToken.tEND_OF_INPUT) {
            directiveTokens.add(token);
            token = lx.nextToken();
          }
          absoluteLineNumber += 1;
          relativeLineNumber += 1;

          // Evaluate the preprocessor directive...
          if (readLineDirectives && !directiveTokens.isEmpty()) {
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

              sourceOriginMapping.mapInputLineRangeToDelta(
                  fileName,
                  rangeLinesOriginFilename,
                  includeStartedWithAbsoluteLine,
                  absoluteLineNumber,
                  relativeLineNumber - absoluteLineNumber);

              final String lineNumberToken =
                  directiveTokens.get(lineNumberTokenIndex).getImage().trim();
              includeStartedWithAbsoluteLine = absoluteLineNumber;
              relativeLineNumber = Integer.parseInt(lineNumberToken);
              if (directiveTokens.size() > lineNumberTokenIndex + 1) {
                String file = directiveTokens.get(lineNumberTokenIndex + 1).getImage().trim();
                if (file.charAt(0) == '"' && file.charAt(file.length() - 1) == '"') {
                  file = file.substring(1, file.length() - 1);
                }
                rangeLinesOriginFilename = Path.of(file);
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
        sourceOriginMapping.mapInputLineRangeToDelta(
            fileName,
            rangeLinesOriginFilename,
            includeStartedWithAbsoluteLine,
            absoluteLineNumber + 1,
            relativeLineNumber - absoluteLineNumber);
      }
    } catch (OffsetLimitReachedException e) {
      throw new CParserException("Tokenizing failed", e);
    }

    String code = tokenizeCode ? tokenizedCode.toString() : pCode;
    if (tokenizeCode && dumpTokenizedProgramToFile != null) {
      try (Writer out = IO.openOutputFile(dumpTokenizedProgramToFile, StandardCharsets.US_ASCII)) {
        out.append(code);
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not write tokenized program to file");
      }
    }
    return code;
  }

  @Override
  public ParseResult parseString(
      Path pFilename, String pCode, CSourceOriginMapping pSourceOriginMapping, Scope pScope)
      throws CParserException, InterruptedException {
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
  public ParseResult parseFiles(List<String> pFilenames)
      throws CParserException, IOException, InterruptedException {
    CSourceOriginMapping sourceOriginMapping = new CSourceOriginMapping();

    List<FileContentToParse> programFragments = new ArrayList<>(pFilenames.size());
    for (String f : pFilenames) {
      Path path = Path.of(f);
      String programCode = tokenizeSourcefile(path, sourceOriginMapping);
      if (programCode.isEmpty()) {
        throw new CParserException("Tokenizer returned empty program");
      }
      programFragments.add(new FileContentToParse(path, programCode));
    }
    return realParser.parseString(programFragments, sourceOriginMapping);
  }

  @Override
  public ParseResult parseString(
      List<FileContentToParse> pCode, CSourceOriginMapping sourceOriginMapping)
      throws CParserException, InterruptedException {

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
