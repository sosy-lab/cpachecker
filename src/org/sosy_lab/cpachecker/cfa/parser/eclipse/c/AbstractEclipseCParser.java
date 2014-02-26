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
package org.sosy_lab.cpachecker.cfa.parser.eclipse.c;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorIncludeStatement;
import org.eclipse.cdt.core.dom.ast.IASTProblem;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.gnu.c.GCCLanguage;
import org.eclipse.cdt.core.dom.parser.c.ANSICParserExtensionConfiguration;
import org.eclipse.cdt.core.dom.parser.c.ICParserExtensionConfiguration;
import org.eclipse.cdt.core.model.ILanguage;
import org.eclipse.cdt.core.parser.IParserLogService;
import org.eclipse.cdt.core.parser.IScannerInfo;
import org.eclipse.cdt.core.parser.ParserFactory;
import org.eclipse.core.runtime.CoreException;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CParser;
import org.sosy_lab.cpachecker.cfa.ParseResult;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.exceptions.CParserException;

import com.google.common.collect.ImmutableMap;

/**
 * Base implementation that should work with all CDT versions we support.
 *
 * @param <T> The type that the CDT version uses to encapsulate the source code access.
 */
abstract class AbstractEclipseCParser<T> implements CParser {

  protected final ILanguage language;

  protected final IParserLogService parserLog = ParserFactory.createDefaultLogService();

  private final MachineModel machine;

  private final LogManager logger;
  private final Configuration config;

  private final Timer parseTimer = new Timer();
  private final Timer cfaTimer = new Timer();

  protected AbstractEclipseCParser(Configuration pConfig, LogManager pLogger,
      Dialect dialect, MachineModel pMachine) {

    this.logger = pLogger;
    this.machine = pMachine;
    this.config = pConfig;

    switch (dialect) {
    case C99:
      language = new CLanguage(new ANSICParserExtensionConfiguration());
      break;
    case GNUC:
      language = GCCLanguage.getDefault();
      break;
    default:
      throw new IllegalArgumentException("Unknown C dialect");
    }
  }

  protected abstract T wrapCode(FileContentToParse pCode);

  protected abstract T wrapCode(String pFilename, String pCode);

  private final T wrapFile(String pFileName) throws IOException {
    String code = Paths.get(pFileName).asCharSource(Charset.defaultCharset()).read();
    return wrapCode(pFileName, code);
  }

  @Override
  public ParseResult parseFile(List<FileToParse> pFilenames) throws CParserException, IOException, InvalidConfigurationException {

    List<Pair<IASTTranslationUnit, String>> astUnits = new ArrayList<>();
    for(FileToParse f: pFilenames) {
      astUnits.add(Pair.of(parse(wrapFile(f.getFileName())), f.getStaticVariablePrefix()));
    }
    return buildCFA(astUnits);
  }

  @Override
  public ParseResult parseString(List<FileContentToParse> codeFragments) throws CParserException, InvalidConfigurationException {

    List<Pair<IASTTranslationUnit, String>> astUnits = new ArrayList<>();
    for(FileContentToParse f : codeFragments) {
      astUnits.add(Pair.of(parse(wrapCode(f)), f.getStaticVariablePrefix()));
    }
    return buildCFA(astUnits);
  }

  /**
   * This method parses a single file where no prefix for static variables is needed.
   */
  @Override
  public ParseResult parseFile(String pFilename) throws CParserException, IOException, InvalidConfigurationException {

    IASTTranslationUnit unit = parse(wrapFile(pFilename));
    String prefix = "";
    List<Pair<IASTTranslationUnit, String>> returnParam = new ArrayList<>();
    returnParam.add(Pair.of(unit, prefix));
    return buildCFA(returnParam);
  }

  /**
   * This method parses a single string, where no prefix for static variables is needed.
   */
  @Override
  public ParseResult parseString(String pFilename, String pCode) throws CParserException, InvalidConfigurationException {

    IASTTranslationUnit unit = parse(wrapCode(pFilename, pCode));
    String prefix = "";
    List<Pair<IASTTranslationUnit, String>> returnParam = new ArrayList<>();
    returnParam.add(Pair.of(unit, prefix));
    return buildCFA(returnParam);
  }

  @Override
  public CAstNode parseSingleStatement(String pCode) throws CParserException, InvalidConfigurationException {
    // parse
    IASTTranslationUnit ast = parse(wrapCode("", pCode));

    // strip wrapping function header
    IASTDeclaration[] declarations = ast.getDeclarations();
    if (   declarations == null
        || declarations.length != 1
        || !(declarations[0] instanceof IASTFunctionDefinition)) {
      throw new CParserException("Not a single function: " + ast.getRawSignature());
    }

    IASTFunctionDefinition func = (IASTFunctionDefinition)declarations[0];
    IASTStatement body = func.getBody();
    if (!(body instanceof IASTCompoundStatement)) {
      throw new CParserException("Function has an unexpected " + body.getClass().getSimpleName() + " as body: " + func.getRawSignature());
    }

    IASTStatement[] statements = ((IASTCompoundStatement)body).getStatements();
    if (!(statements.length == 2 && statements[1] == null || statements.length == 1)) {
      throw new CParserException("Not exactly one statement in function body: " + body);
    }

    Sideassignments sa = new Sideassignments();
    sa.enterBlock();
    return new ASTConverter(config, new FunctionScope(), logger, machine, "", false, sa).convert(statements[0]);
  }

  @Override
  public List<CAstNode> parseStatements(String pCode) throws CParserException, InvalidConfigurationException {
    // parse
    IASTTranslationUnit ast = parse(wrapCode("", pCode));

    // strip wrapping function header
    IASTDeclaration[] declarations = ast.getDeclarations();
    if (   declarations == null
        || declarations.length != 1
        || !(declarations[0] instanceof IASTFunctionDefinition)) {
      throw new CParserException("Not a single function: " + ast.getRawSignature());
    }

    IASTFunctionDefinition func = (IASTFunctionDefinition)declarations[0];
    IASTStatement body = func.getBody();
    if (!(body instanceof IASTCompoundStatement)) {
      throw new CParserException("Function has an unexpected " + body.getClass().getSimpleName() + " as body: " + func.getRawSignature());
    }

    IASTStatement[] statements = ((IASTCompoundStatement)body).getStatements();
    if (statements.length == 1 && statements[0] == null || statements.length == 0) {
      throw new CParserException("No statement found in function body: " + body);
    }

    Sideassignments sa = new Sideassignments();
    sa.enterBlock();

    ASTConverter converter = new ASTConverter(config, new FunctionScope(), logger, machine, "", false, sa);

    List<CAstNode> nodeList = new ArrayList<>(statements.length);

    for(IASTStatement statement : statements) {
      if(statement != null) {
        nodeList.add(converter.convert(statement));
      }
    }

    if (nodeList.size() < 1) {
      throw new CParserException("No statement found in function body: " + body);
    }

    return nodeList;
  }

  protected static final int PARSER_OPTIONS =
            ILanguage.OPTION_IS_SOURCE_UNIT     // our code files are always source files, not header files
          | ILanguage.OPTION_NO_IMAGE_LOCATIONS // we don't use IASTName#getImageLocation(), so the parse doesn't need to create them
          ;

  private IASTTranslationUnit parse(T codeReader) throws CParserException {
    parseTimer.start();
    try {
      IASTTranslationUnit result = getASTTranslationUnit(codeReader);

      IASTPreprocessorIncludeStatement[] includes = result.getIncludeDirectives();
      if (includes.length > 0) {
        throw new CParserException("File has #include directives and needs to be pre-processed.");
      }

      // Report the preprocessor problems.
      // TODO this shows only the first problem
      for (IASTProblem problem : result.getPreprocessorProblems()) {
        throw new CFAGenerationRuntimeException(problem);
      }

      return result;

    } catch (CFAGenerationRuntimeException e) {
      // thrown by StubCodeReaderFactory
      throw new CParserException(e);
    } catch (CoreException e) {
      throw new CParserException(e);
    } finally {
      parseTimer.stop();
    }
  }

  protected abstract IASTTranslationUnit getASTTranslationUnit(T code) throws CParserException, CFAGenerationRuntimeException, CoreException;

  /**
   * Builds the cfa out of a list of pairs of translation units and their appropriate prefixes for static variables
   *
   * @param asts a List of Pairs of translation units and the appropriate prefix for static variables
   * @return
   * @throws CParserException
   * @throws InvalidConfigurationException
   */
  private ParseResult buildCFA(List<Pair<IASTTranslationUnit, String>> asts) throws CParserException, InvalidConfigurationException {
    cfaTimer.start();
    try {
      CFABuilder builder = new CFABuilder(config, logger, machine);
      for(Pair<IASTTranslationUnit, String> ast : asts) {
        builder.analyzeTranslationUnit(ast.getFirst(), ast.getSecond());
      }

      return builder.createCFA();

    } catch (CFAGenerationRuntimeException e) {
      throw new CParserException(e);
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

    private static final ImmutableMap<String, String> MACROS;

    static {
      ImmutableMap.Builder<String, String> macrosBuilder = ImmutableMap.builder();

      // _Static_assert(cond, msg) feature of C11
      macrosBuilder.put("_Static_assert(c, m)", "");

      // These built-ins are defined as macros
      // in org.eclipse.cdt.core.dom.parser.GNUScannerExtensionConfiguration.
      // When the parser encounters their redefinition or
      // some non-trivial usage in the code, we get parsing errors.
      // So we redefine these macros to themselves in order to
      // parse them as functions.
      macrosBuilder.put("__builtin_va_arg", "__builtin_va_arg");
      macrosBuilder.put("__builtin_constant_p", "__builtin_constant_p");
      macrosBuilder.put("__builtin_types_compatible_p(t1,t2)", "__builtin_types_compatible_p(({t1 arg1; arg1;}), ({t2 arg2; arg2;}))");
      macrosBuilder.put("__offsetof__", "__offsetof__");

      macrosBuilder.put("__func__", "\"__func__\"");
      macrosBuilder.put("__FUNCTION__", "\"__FUNCTION__\"");
      macrosBuilder.put("__PRETTY_FUNCTION__", "\"__PRETTY_FUNCTION__\"");

      // Eclipse CDT 8.1.1 has problems with more complex attributes
      macrosBuilder.put("__attribute__(a)", "");

      MACROS = macrosBuilder.build();
    }

    protected final static IScannerInfo instance = new StubScannerInfo();

    @Override
    public Map<String, String> getDefinedSymbols() {
      // the externally defined pre-processor macros
      return MACROS;
    }

    @Override
    public String[] getIncludePaths() {
      return new String[0];
    }
  }
}