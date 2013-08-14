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
package org.sosy_lab.cpachecker.cfa.parser.eclipse.c;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CParser;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.ParseResult;
import org.sosy_lab.cpachecker.cfa.ast.IADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.exceptions.CParserException;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;

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

  private final Timer parseTimer = new Timer();
  private final Timer cfaTimer = new Timer();

  private final Configuration config;

  protected AbstractEclipseCParser(Configuration config, LogManager pLogger, Dialect dialect, MachineModel pMachine) {
    logger = pLogger;
    machine = pMachine;

    this.config = config;

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

  protected abstract T wrapCode(String pCode);

  protected abstract T wrapFile(String pFilename) throws IOException;

  @Override
  public ParseResult parseFile(String[] pFilenames, String[] staticVariablePrefixes) throws CParserException, IOException, InvalidConfigurationException {

    IASTTranslationUnit[] astUnits = new IASTTranslationUnit[pFilenames.length];
    for(int i = 0; i < pFilenames.length; i++) {
      astUnits[i] = parse(wrapFile(pFilenames[i]));
    }
    return buildCFA(astUnits, staticVariablePrefixes);
  }

  @Override
  public ParseResult parseString(String[] pCode, String[] staticVariablePrefixes) throws CParserException, InvalidConfigurationException {

    IASTTranslationUnit[] astUnits = new IASTTranslationUnit[pCode.length];
    for(int i = 0; i < pCode.length; i++) {
      astUnits[i] = parse(wrapCode(pCode[i]));
    }
    return buildCFA(astUnits, staticVariablePrefixes);
  }

  @Override
  public ParseResult parseFile(String pFilename) throws CParserException, IOException, InvalidConfigurationException {

    IASTTranslationUnit[] unit = {parse(wrapFile(pFilename))};
    String[] prefix = {""};
    return buildCFA(unit, prefix);
  }

  @Override
  public ParseResult parseString(String pCode) throws CParserException, InvalidConfigurationException {

    IASTTranslationUnit[] unit = {parse(wrapCode(pCode))};
    String[] prefix = {""};
    return buildCFA(unit, prefix);
  }

  @Override
  public CAstNode parseSingleStatement(String pCode) throws CParserException, InvalidConfigurationException {
    // parse
    IASTTranslationUnit ast = parse(wrapCode(pCode));

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
//TODO
    return new ASTConverter(config, new FunctionScope(), logger, machine, "").convert(statements[0]);
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

  private ParseResult buildCFA(IASTTranslationUnit[] ast, String[] staticVariablePrefix) throws CParserException, InvalidConfigurationException {
    cfaTimer.start();
    try {
      CFABuilder[] builder = new CFABuilder[ast.length];
      for(int i = 0; i < ast.length; i++) {

        builder[i] = new CFABuilder(config, logger, machine, staticVariablePrefix[i]);
        try {
          ast[i].accept(builder[i]);
        } catch (CFAGenerationRuntimeException e) {
          e.printStackTrace();
          throw new CParserException(e);
        }
      }

      Map<String, FunctionEntryNode> cfas = new HashMap<>();
      SortedSetMultimap<String, CFANode> cfaNodes = TreeMultimap.create();
      List<Pair<IADeclaration, String>> globalDeclarations = Lists.newArrayList();
      for(CFABuilder b : builder) {
        cfas.putAll(b.getCFAs());
        cfaNodes.putAll(b.getCFANodes());
        globalDeclarations.addAll(b.getGlobalDeclarations());
      }

      // remove global elements which are declarated in several files
      Iterator<Pair<IADeclaration, String>> it = globalDeclarations.iterator();
      HashSet<IADeclaration> globals = new HashSet<>();
      while(it.hasNext()) {
        Pair<IADeclaration, String> p = it.next();
        if(globals.contains(p.getFirst())) {
          it.remove();
        } else {
          globals.add(p.getFirst());
        }
      }

      return new ParseResult(cfas, cfaNodes, globalDeclarations, Language.C);
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
      macrosBuilder.put("__builtin_types_compatible_p", "__builtin_types_compatible_p");
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