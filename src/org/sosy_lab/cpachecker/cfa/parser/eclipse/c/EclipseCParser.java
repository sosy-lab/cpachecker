// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.eclipse.c;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sosy_lab.cpachecker.cfa.parser.eclipse.c.EclipseCdtWrapper.wrapCode;
import static org.sosy_lab.cpachecker.cfa.parser.eclipse.c.EclipseCdtWrapper.wrapFile;

import com.google.common.base.CharMatcher;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
import org.eclipse.cdt.core.parser.FileContent;
import org.eclipse.core.runtime.CoreException;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CParser;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.CSourceOriginMapping;
import org.sosy_lab.cpachecker.cfa.ParseResult;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.parser.Parsers.EclipseCParserOptions;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.exceptions.CParserException;

/** Parser based on Eclipse CDT */
class EclipseCParser implements CParser {

  private final EclipseCdtWrapper eclipseCdt;

  private final MachineModel machine;
  private final LogManager logger;
  private final EclipseCParserOptions options;
  private final ShutdownNotifier shutdownNotifier;

  private final Timer parseTimer = new Timer();
  private final Timer cfaTimer = new Timer();

  public EclipseCParser(
      LogManager pLogger,
      EclipseCParserOptions pOptions,
      MachineModel pMachine,
      ShutdownNotifier pShutdownNotifier) {

    logger = pLogger;
    machine = pMachine;
    options = pOptions;
    shutdownNotifier = pShutdownNotifier;

    eclipseCdt = new EclipseCdtWrapper(pOptions, pShutdownNotifier);
  }

  /**
   * Convert paths like "file.c" to "./file.c", and return all other patchs unchanged. The
   * pre-processor of Eclipse CDT needs this to resolve relative includes.
   */
  private static Path fixPath(Path path) {
    if (!path.toString().isEmpty() && !path.isAbsolute() && path.getParent() == null) {
      return Path.of(".").resolve(path);
    }
    return path;
  }

  private interface FileParseWrapper {
    FileContent wrap(Path pFileName, FileToParse pContent) throws IOException;
  }

  private ParseResult parseSomething(
      List<? extends FileToParse> pInput,
      CSourceOriginMapping pSourceOriginMapping,
      CProgramScope scope,
      FileParseWrapper pWrapperFunction)
      throws CParserException, InterruptedException {

    Preconditions.checkNotNull(pInput);
    Preconditions.checkNotNull(pSourceOriginMapping);
    Preconditions.checkNotNull(pWrapperFunction);

    Map<Path, Path> fileNameMapping = new HashMap<>();
    for (FileToParse f : pInput) {
      fileNameMapping.put(fixPath(f.getFileName()), f.getFileName());
    }
    FixedPathSourceOriginMapping sourceOriginMapping =
        new FixedPathSourceOriginMapping(pSourceOriginMapping, fileNameMapping);
    ParseContext parseContext =
        new ParseContext(createNiceFileNameFunction(fileNameMapping.keySet()), sourceOriginMapping);

    List<IASTTranslationUnit> astUnits = new ArrayList<>(pInput.size());

    for (FileToParse f : pInput) {
      final Path fileName = fixPath(f.getFileName());

      try {
        astUnits.add(parse(pWrapperFunction.wrap(fileName, f), parseContext));
      } catch (IOException e) {
        throw new CParserException("IO failed!", e);
      }
    }

    return buildCFA(astUnits, parseContext, scope);
  }

  @Override
  public ParseResult parseFiles(List<String> pFilenames)
      throws CParserException, InterruptedException {

    return parseSomething(
        Lists.transform(pFilenames, name -> new FileToParse(Path.of(name))),
        new CSourceOriginMapping(),
        CProgramScope.empty(),
        (pFileName, pContent) -> wrapFile(pFileName));
  }

  @Override
  public ParseResult parseString(
      List<FileContentToParse> pCodeFragments, CSourceOriginMapping sourceOriginMapping)
      throws CParserException, InterruptedException {

    return parseSomething(
        pCodeFragments,
        sourceOriginMapping,
        CProgramScope.empty(),
        (pFileName, pContent) -> {
          Preconditions.checkArgument(pContent instanceof FileContentToParse);
          return wrapCode(pFileName, ((FileContentToParse) pContent).getFileContent());
        });
  }

  /** This method parses a single string, where no prefix for static variables is needed. */
  @Override
  public ParseResult parseString(
      Path pFileName, String pCode, CSourceOriginMapping sourceOriginMapping, Scope pScope)
      throws CParserException, InterruptedException {

    return parseSomething(
        ImmutableList.of(new FileContentToParse(pFileName, pCode)),
        sourceOriginMapping,
        pScope instanceof CProgramScope ? ((CProgramScope) pScope) : CProgramScope.empty(),
        (fileName, content) -> {
          Preconditions.checkArgument(content instanceof FileContentToParse);
          return wrapCode(fileName, ((FileContentToParse) content).getFileContent());
        });
  }

  private IASTStatement[] parseCodeFragmentReturnBody(String pCode)
      throws CParserException, InterruptedException {
    // parse
    IASTTranslationUnit ast = parse(wrapCode(Path.of("fragment"), pCode), ParseContext.dummy());

    // strip wrapping function header
    IASTDeclaration[] declarations = ast.getDeclarations();
    if (declarations == null
        || declarations.length != 1
        || !(declarations[0] instanceof IASTFunctionDefinition)) {
      throw new CParserException("Not a single function: " + ast.getRawSignature());
    }

    IASTFunctionDefinition func = (IASTFunctionDefinition) declarations[0];
    IASTStatement body = func.getBody();
    if (!(body instanceof IASTCompoundStatement)) {
      throw new CParserException(
          "Function has an unexpected "
              + body.getClass().getSimpleName()
              + " as body: "
              + func.getRawSignature());
    }

    return ((IASTCompoundStatement) body).getStatements();
  }

  private ASTConverter prepareTemporaryConverter(Scope scope) {
    Sideassignments sa = new Sideassignments();
    sa.enterBlock();

    return new ASTConverter(
        options,
        scope,
        new LogManagerWithoutDuplicates(logger),
        ParseContext.dummy(),
        machine,
        "",
        sa);
  }

  @Override
  public CAstNode parseSingleStatement(String pCode, Scope scope)
      throws CParserException, InterruptedException {

    IASTStatement[] statements = parseCodeFragmentReturnBody(pCode);
    ASTConverter converter = prepareTemporaryConverter(scope);

    if (!((statements.length == 2 && statements[1] == null) || statements.length == 1)) {
      throw new CParserException("Not exactly one statement in function body: " + pCode);
    }

    try {
      return converter.convert(statements[0]);
    } catch (CFAGenerationRuntimeException e) {
      throw new CParserException(e);
    }
  }

  @Override
  public List<CAstNode> parseStatements(String pCode, Scope scope)
      throws CParserException, InterruptedException {

    IASTStatement[] statements = parseCodeFragmentReturnBody(pCode);
    ASTConverter converter = prepareTemporaryConverter(scope);

    List<CAstNode> nodeList = new ArrayList<>(statements.length);

    for (IASTStatement statement : statements) {
      if (statement != null) {
        try {
          nodeList.add(converter.convert(statement));
        } catch (CFAGenerationRuntimeException e) {
          throw new CParserException(e);
        }
      }
    }

    if (nodeList.size() < 1) {
      throw new CParserException("No statement found in function body: " + pCode);
    }

    return nodeList;
  }

  private IASTTranslationUnit parse(FileContent codeReader, ParseContext parseContext)
      throws CParserException, InterruptedException {
    parseTimer.start();
    try {
      IASTTranslationUnit result = eclipseCdt.getASTTranslationUnit(codeReader);

      // Separate handling of include problems
      // so that we can give a better error message.
      for (IASTPreprocessorIncludeStatement include : result.getIncludeDirectives()) {
        if (!include.isResolved()) {
          if (include.isSystemInclude()) {
            throw new CFAGenerationRuntimeException(
                "File includes system headers, either preprocess it manually or specify"
                    + " -preprocess.");
          } else {
            throw parseContext.parseError(
                "Included file " + include.getName() + " is missing", include);
          }
        }
      }

      // Report the preprocessor problems.
      // TODO this shows only the first problem
      for (IASTProblem problem : result.getPreprocessorProblems()) {
        throw parseContext.parseError(problem);
      }

      return result;

    } catch (CFAGenerationRuntimeException | CoreException e) {
      throw new CParserException(e);
    } finally {
      parseTimer.stop();
    }
  }

  private static final CharMatcher LEGAL_VAR_NAME_CHARACTERS =
      // Taken from § 6.4.2.1 of C11
      CharMatcher.is('_')
          .or(CharMatcher.inRange('A', 'Z'))
          .or(CharMatcher.inRange('a', 'z'))
          .or(CharMatcher.inRange('0', '9'))
          .precomputed();

  /**
   * Builds the cfa out of a list of pairs of translation units and their appropriate prefixes for
   * static variables
   *
   * @param asts a List of Pairs of translation units and the appropriate prefix for static
   *     variables
   */
  private ParseResult buildCFA(
      List<IASTTranslationUnit> asts, ParseContext parseContext, Scope pScope)
      throws CParserException, InterruptedException {

    checkArgument(!asts.isEmpty());
    cfaTimer.start();

    try {
      CFABuilder builder = new CFABuilder(options, logger, shutdownNotifier, parseContext, machine);

      // we don't need any file prefix if we only have one file
      if (asts.size() == 1) {
        builder.analyzeTranslationUnit(asts.get(0), "", pScope);

        // in case of several files we need to add a file prefix to global variables
        // as there could be several equally named files in different directories
        // we consider not only the file name but also the path for creating
        // the prefix
      } else {
        for (IASTTranslationUnit ast : asts) {
          String staticVariablePrefix =
              LEGAL_VAR_NAME_CHARACTERS
                  .negate()
                  .replaceFrom(parseContext.mapFileNameToNameForHumans(ast.getFilePath()), "_");
          builder.analyzeTranslationUnit(ast, staticVariablePrefix, pScope);
        }
      }

      return builder.createCFA();

    } catch (CFAGenerationRuntimeException e) {
      throw new CParserException(e);
    } finally {
      cfaTimer.stop();
    }
  }

  /**
   * Given a file name, this function returns a "nice" representation of it. This should be used for
   * situations where the name is going to be presented to the user. The result may be the empty
   * string, if for example CPAchecker only uses one file (we expect the user to know its name in
   * this case).
   */
  private Function<String, String> createNiceFileNameFunction(Collection<Path> pFileNames) {
    Iterator<String> fileNames = Iterators.transform(pFileNames.iterator(), Path::toString);

    if (pFileNames.size() == 1) {
      final String mainFileName = fileNames.next();
      return pInput ->
          (mainFileName.equals(pInput)
              ? "" // no file name necessary for main file if there is only one
              : pInput);

    } else {
      String commonStringPrefix = fileNames.next();
      while (fileNames.hasNext()) {
        commonStringPrefix = Strings.commonPrefix(commonStringPrefix, fileNames.next());
      }

      final String commonPathPrefix;
      int pos = commonStringPrefix.lastIndexOf(File.separator);
      if (pos < 0) {
        commonPathPrefix = commonStringPrefix;
      } else {
        commonPathPrefix = commonStringPrefix.substring(0, pos + 1);
      }

      return pInput -> {
        if (pInput.isEmpty()) {
          return pInput;
        }
        if (pInput.charAt(0) == '"' && pInput.charAt(pInput.length() - 1) == '"') {
          pInput = pInput.substring(1, pInput.length() - 1);
        }
        if (pInput.startsWith(commonPathPrefix)) {
          return pInput.substring(commonPathPrefix.length()).intern();
        } else {
          return pInput.intern();
        }
      };
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
   * Wrapper for {@link CSourceOriginMapping} that does the reverse file-name mapping of {@link
   * EclipseCParser#fixPath(Path)}, otherwise file-name lookup fails and origin-source mapping does
   * not work for files in the current directory that are not specified as "./foo.c" but only as
   * "foo.c".
   */
  private static class FixedPathSourceOriginMapping extends CSourceOriginMapping {

    private final CSourceOriginMapping delegate;
    private final ImmutableMap<Path, Path> fileNameMapping;

    FixedPathSourceOriginMapping(CSourceOriginMapping pDelegate, Map<Path, Path> pFileNameMapping) {
      delegate = pDelegate;
      fileNameMapping = ImmutableMap.copyOf(pFileNameMapping);
    }

    @Override
    public CodePosition getOriginLineFromAnalysisCodeLine(
        final Path pAnalysisFile, final int pAnalysisCodeLine) {
      final Path analysisFile = fileNameMapping.getOrDefault(pAnalysisFile, pAnalysisFile);

      CodePosition result =
          delegate.getOriginLineFromAnalysisCodeLine(analysisFile, pAnalysisCodeLine);

      if (result.getFileName().equals(analysisFile)) {
        // reverse mapping
        result = result.withFileName(pAnalysisFile);
      }
      return result;
    }

    @Override
    public boolean isMappingToIdenticalLineNumbers() {
      return delegate.isMappingToIdenticalLineNumbers();
    }
  }
}
