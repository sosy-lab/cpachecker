/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.parser.eclipse;

import java.io.IOException;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.gnu.c.GCCLanguage;
import org.eclipse.cdt.core.dom.parser.c.ANSICParserExtensionConfiguration;
import org.eclipse.cdt.core.dom.parser.c.GCCParserExtensionConfiguration;
import org.eclipse.cdt.core.dom.parser.c.ICParserExtensionConfiguration;
import org.eclipse.cdt.core.model.ILanguage;
import org.eclipse.cdt.core.parser.IParserLogService;
import org.eclipse.cdt.core.parser.IScannerInfo;
import org.eclipse.cdt.core.parser.ParserFactory;
import org.eclipse.core.runtime.CoreException;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Timer;
import org.sosy_lab.cpachecker.cfa.CParser;
import org.sosy_lab.cpachecker.cfa.ParseResult;
import org.sosy_lab.cpachecker.exceptions.ParserException;

/**
 * Base implementation that should work with all CDT versions we support.
 *
 * @param <T> The type that the CDT version uses to encapsulate the source code access.
 */
public abstract class AbstractEclipseCParser<T> implements CParser {

  private boolean ignoreCasts;

  protected final ILanguage language;

  protected final IParserLogService parserLog = ParserFactory.createDefaultLogService();

  private final LogManager logger;

  private final Timer parseTimer = new Timer();
  private final Timer cfaTimer = new Timer();

  protected AbstractEclipseCParser(LogManager pLogger, Dialect dialect) {
    logger = pLogger;

    switch (dialect) {
    case C99:
      language = new CLanguage(new ANSICParserExtensionConfiguration());
      break;
    case GNUC:
      language = new CLanguage(new GCCParserExtensionConfiguration());
      break;
    default:
      throw new IllegalArgumentException("Unknown C dialect");
    }
  }

  protected abstract T wrapCode(String pCode);

  protected abstract T wrapFile(String pFilename) throws IOException;

  @Override
  public ParseResult parseFile(String pFilename) throws ParserException, IOException {

    return buildCFA(parse(wrapFile(pFilename)));
  }

  @Override
  public ParseResult parseString(String pCode) throws ParserException {

    return buildCFA(parse(wrapCode(pCode)));
  }

  @Override
  public org.sosy_lab.cpachecker.cfa.ast.c.CAstNode parseSingleStatement(String pCode) throws ParserException {

    // parse
    IASTTranslationUnit ast = parse(wrapCode(pCode));

    // strip wrapping function header
    IASTDeclaration[] declarations = ast.getDeclarations();
    if (   declarations == null
        || declarations.length != 1
        || !(declarations[0] instanceof IASTFunctionDefinition)) {
      throw new ParserException("Not a single function: " + ast.getRawSignature());
    }

    IASTFunctionDefinition func = (IASTFunctionDefinition)declarations[0];
    org.eclipse.cdt.core.dom.ast.IASTStatement body = func.getBody();
    if (!(body instanceof IASTCompoundStatement)) {
      throw new ParserException("Function has an unexpected " + body.getClass().getSimpleName() + " as body: " + func.getRawSignature());
    }

    org.eclipse.cdt.core.dom.ast.IASTStatement[] statements = ((IASTCompoundStatement)body).getStatements();
    if (!(statements.length == 2 && statements[1] == null || statements.length == 1)) {
      throw new ParserException("Not exactly one statement in function body: " + body);
    }

    return new ASTConverter(new Scope(), ignoreCasts, logger).convert(statements[0]);
  }

  protected static final int PARSER_OPTIONS =
            ILanguage.OPTION_IS_SOURCE_UNIT     // our code files are always source files, not header files
          | ILanguage.OPTION_NO_IMAGE_LOCATIONS // we don't use IASTName#getImageLocation(), so the parse doesn't need to create them
          ;

  private IASTTranslationUnit parse(T codeReader) throws ParserException {
    parseTimer.start();
    try {
      return getASTTranslationUnit(codeReader);
    } catch (CFAGenerationRuntimeException e) {
      // thrown by StubCodeReaderFactory
      throw new ParserException(e);
    } catch (CoreException e) {
      throw new ParserException(e);
    } finally {
      parseTimer.stop();
    }
  }

  protected abstract IASTTranslationUnit getASTTranslationUnit(T code) throws ParserException, CFAGenerationRuntimeException, CoreException;

  private ParseResult buildCFA(IASTTranslationUnit ast) throws ParserException {
    cfaTimer.start();
    try {
      CFABuilder builder = new CFABuilder(logger, ignoreCasts);
      try {
        ast.accept(builder);
      } catch (CFAGenerationRuntimeException e) {
        throw new ParserException(e);
      }

      return new ParseResult(builder.getCFAs(), builder.getCFANodes(), builder.getGlobalDeclarations());
    } finally {
      cfaTimer.stop();
    }
  }


  @Override
  public Timer getParseTime() {
    return parseTimer;
  }

  @Override
  public Timer getCFAConstructionTime() {
    return cfaTimer;
  }


  /**
   * Private class extending the Eclipse CDT class that is the starting point
   * for using the parser.
   * Supports choise of parser dialect.
   */
  private static class CLanguage extends GCCLanguage {

    private final ICParserExtensionConfiguration parserConfig;

    public CLanguage(ICParserExtensionConfiguration parserConfig) {
      this.parserConfig = parserConfig;
    }

    @Override
    protected ICParserExtensionConfiguration getParserExtensionConfiguration() {
      return parserConfig;
    }
  }

  /**
   * Private class that tells the Eclipse CDT scanner that no macros and include
   * paths have been defined externally.
   */
  protected static class StubScannerInfo implements IScannerInfo {

    protected final static IScannerInfo instance = new StubScannerInfo();

    @Override
    public Map<String, String> getDefinedSymbols() {
      // the externally defined pre-processor macros
      return null;
    }

    @Override
    public String[] getIncludePaths() {
      return new String[0];
    }
  }

  public void setIgnoreCasts(boolean pIgnoreCasts) {
    ignoreCasts = pIgnoreCasts;
  }
}