// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.eclipse.c;

import static com.google.common.base.Preconditions.checkArgument;

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
import java.nio.charset.Charset;
import java.nio.file.Files;
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
import org.eclipse.cdt.core.dom.ast.gnu.c.GCCLanguage;
import org.eclipse.cdt.core.dom.parser.c.ANSICParserExtensionConfiguration;
import org.eclipse.cdt.core.dom.parser.c.ICParserExtensionConfiguration;
import org.eclipse.cdt.core.index.IIndexFileLocation;
import org.eclipse.cdt.core.model.ILanguage;
import org.eclipse.cdt.core.parser.FileContent;
import org.eclipse.cdt.core.parser.IParserLogService;
import org.eclipse.cdt.core.parser.IScannerInfo;
import org.eclipse.cdt.internal.core.parser.IMacroDictionary;
import org.eclipse.cdt.internal.core.parser.InternalParserUtil;
import org.eclipse.cdt.internal.core.parser.scanner.InternalFileContent;
import org.eclipse.cdt.internal.core.parser.scanner.InternalFileContentProvider;
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

/** Wrapper for Eclipse CDT */
class EclipseCParser implements CParser {

  private final ILanguage language;

  private final IParserLogService parserLog;

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
    parserLog = new ShutdownNotifierLogAdapter(pShutdownNotifier);

    switch (pOptions.getDialect()) {
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

  private FileContent wrapCode(Path pFileName, String pCode) {
    return FileContent.create(pFileName.toString(), pCode.toCharArray());
  }

  private FileContent wrapFile(Path pFileName) throws IOException {
    String code = Files.readString(pFileName, Charset.defaultCharset());
    return wrapCode(pFileName, code);
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

    return statements;
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

  // we don't use IASTName#getImageLocation(), so the parser doesn't need to create them
  protected static final int PARSER_OPTIONS = ILanguage.OPTION_NO_IMAGE_LOCATIONS;

  private IASTTranslationUnit parse(FileContent codeReader, ParseContext parseContext)
      throws CParserException, InterruptedException {
    parseTimer.start();
    try {
      IASTTranslationUnit result = getASTTranslationUnit(codeReader);

      // Separate handling of include problems
      // so that we can give a better error message.
      for (IASTPreprocessorIncludeStatement include : result.getIncludeDirectives()) {
        if (!include.isResolved()) {
          if (include.isSystemInclude()) {
            throw new CFAGenerationRuntimeException("File includes system headers, either preprocess it manually or specify -preprocess.");
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

  private IASTTranslationUnit getASTTranslationUnit(FileContent pCode)
      throws CFAGenerationRuntimeException, CoreException, InterruptedException {
    try {
      return language.getASTTranslationUnit(
          pCode,
          StubScannerInfo.instance,
          FileContentProvider.instance,
          null,
          PARSER_OPTIONS,
          parserLog);
    } finally {
      shutdownNotifier.shutdownIfNecessary();
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
        commonPathPrefix = commonStringPrefix.substring(0, pos+1);
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
   * Private class extending the Eclipse CDT class that is the starting point
   * for using the parser.
   * Supports choise of parser dialect.
   */
  @SuppressWarnings("unchecked")
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
      // _Noreturn feature of C11
      macrosBuilder.put("_Noreturn", "");

      // These built-ins are defined as macros
      // in org.eclipse.cdt.core.dom.parser.GNUScannerExtensionConfiguration.
      // When the parser encounters their redefinition or
      // some non-trivial usage in the code, we get parsing errors.
      // So we redefine these macros to themselves in order to
      // parse them as functions.
      macrosBuilder.put("__builtin_constant_p", "__builtin_constant_p");
      macrosBuilder.put("__builtin_types_compatible_p(t1,t2)", "__builtin_types_compatible_p(({t1 arg1; arg1;}), ({t2 arg2; arg2;}))");
      macrosBuilder.put("__offsetof__", "__offsetof__");
      macrosBuilder.put("__builtin_offsetof(t,f)", "__builtin_offsetof(((t){}).f)");
      macrosBuilder.put("__func__", "\"__func__\"");
      macrosBuilder.put("__FUNCTION__", "\"__FUNCTION__\"");
      macrosBuilder.put("__PRETTY_FUNCTION__", "\"__PRETTY_FUNCTION__\"");

      // For vararg handling there are some interesting macros that we could use available at
      // https://web.archive.org/web/20160801170919/http://research.microsoft.com/en-us/um/redmond/projects/invisible/include/stdarg.h.htm
      // However, without proper support in the analysis, these just make things worse.
      // Cf. https://gitlab.com/sosy-lab/software/cpachecker/-/issues/711
      // We need size of smallest addressable unit:
      // macrosBuilder.put("_INTSIZEOF(n)", "((sizeof(n) + sizeof(int) - 1) & ~(sizeof(int) - 1))");
      // macrosBuilder.put("__builtin_va_start(ap,v)", "(ap = (va_list)&v + _INTSIZEOF(v))");
      // macrosBuilder.put("__builtin_va_arg(ap,t)", "*(t *)((ap += _INTSIZEOF(t)) -
      // _INTSIZEOF(t))");
      // macrosBuilder.put("__builtin_va_end(ap)", "(ap = (va_list)0)");

      // But for now we just make sure that code with varargs can be parsed
      macrosBuilder.put("__builtin_va_arg(ap,t)", "(t)__builtin_va_arg(ap)");

      // specifying a GCC version >= 4.7 enables handling of 128-bit types in
      // GCCScannerExtensionConfiguration
      macrosBuilder.put("__GNUC__", "4");
      macrosBuilder.put("__GNUC_MINOR__", "7");

      // Our version of CDT does not recognize _Float128 yet:
      // https://gitlab.com/sosy-lab/software/cpachecker/-/issues/471
      macrosBuilder.put("_Float128", "__float128");
      // https://gcc.gnu.org/onlinedocs/gcc/Floating-Types.html
      // https://code.woboq.org/userspace/glibc/bits/floatn-common.h.html
      macrosBuilder.put("_Float32", "float");
      macrosBuilder.put("_Float32x", "double");
      macrosBuilder.put("_Float64", "double");
      macrosBuilder.put("_Float64x", "long double");

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

  private static class FileContentProvider extends InternalFileContentProvider {

    static final InternalFileContentProvider instance = new FileContentProvider();

    @Override
    public InternalFileContent getContentForInclusion(String pFilePath,
        IMacroDictionary pMacroDictionary) {
      return InternalParserUtil.createExternalFileContent(pFilePath,
          InternalParserUtil.SYSTEM_DEFAULT_ENCODING);
    }

    @Override
    public InternalFileContent getContentForInclusion(IIndexFileLocation pIfl,
        String pAstPath) {
      return InternalParserUtil.createFileContent(pIfl);
    }
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