// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.Uninterruptibles;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.Concurrency;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AbstractSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.ACSLParser;
import org.sosy_lab.cpachecker.cfa.ast.acsl.util.SyntacticBlock;
import org.sosy_lab.cpachecker.cfa.ast.acsl.util.SyntacticBlockStructureBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3Declaration;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3ProcedureDeclaration;
import org.sosy_lab.cpachecker.cfa.export.CFAToPixelsWriter;
import org.sosy_lab.cpachecker.cfa.export.DOTBuilder;
import org.sosy_lab.cpachecker.cfa.export.DOTBuilder2;
import org.sosy_lab.cpachecker.cfa.export.FunctionCallDumper;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.k3.K3DeclarationEdge;
import org.sosy_lab.cpachecker.cfa.parser.Parsers;
import org.sosy_lab.cpachecker.cfa.parser.k3.K3ParserException;
import org.sosy_lab.cpachecker.cfa.postprocessing.function.AtExitTransformer;
import org.sosy_lab.cpachecker.cfa.postprocessing.function.CFADeclarationMover;
import org.sosy_lab.cpachecker.cfa.postprocessing.function.CFASimplifier;
import org.sosy_lab.cpachecker.cfa.postprocessing.function.CFunctionPointerResolver;
import org.sosy_lab.cpachecker.cfa.postprocessing.function.ExpandFunctionPointerArrayAssignments;
import org.sosy_lab.cpachecker.cfa.postprocessing.function.NullPointerChecks;
import org.sosy_lab.cpachecker.cfa.postprocessing.function.ThreadCreateTransformer;
import org.sosy_lab.cpachecker.cfa.postprocessing.global.CFACloner;
import org.sosy_lab.cpachecker.cfa.postprocessing.global.FunctionCallUnwinder;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CDefaults;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.threading.ThreadingTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CParserException;
import org.sosy_lab.cpachecker.exceptions.JParserException;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.LiveVariables;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.cwriter.CFAToCTranslator;
import org.sosy_lab.cpachecker.util.statistics.StatisticsUtils;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassificationBuilder;

/**
 * Class that encapsulates the whole CFA creation process.
 *
 * <p>It is not thread-safe, but it may be re-used.
 */
@Options
public class CFACreator {

  public static final String VALID_C_FUNCTION_NAME_PATTERN = "[_a-zA-Z][_a-zA-Z0-9]*";
  public static final String VALID_JAVA_FUNCTION_NAME_PATTERN = ".*"; // TODO

  @Option(
      secure = true,
      name = "parser.usePreprocessor",
      description =
          "For C files, run the preprocessor on them before parsing. Note that all line numbers"
              + " printed by CPAchecker will refer to the pre-processed file, not the original"
              + " input file.")
  private boolean usePreprocessor = false;

  @Option(
      secure = true,
      name = "parser.useClang",
      description =
          "For C files, convert to LLVM IR with clang first and then use the LLVM parser (currently"
              + " unsupported).")
  @Deprecated
  private boolean useClang = false;

  @Option(
      secure = true,
      name = "parser.readLineDirectives",
      description =
          "For C files, read #line preprocessor directives and use their information for"
              + " outputting line numbers. (Always enabled when pre-processing is used.)")
  private boolean readLineDirectives = false;

  @Option(secure = true, name = "analysis.entryFunction", description = "entry function")
  private String mainFunctionName = "main";

  @Option(
      secure = true,
      name = "analysis.machineModel",
      description =
          """
          the machine model, which determines the sizes of types like int:
          - LINUX32: ILP32 for Linux on 32-bit x86
          - LINUX64: LP64 for Linux on 64-bit x86
          - ARM: ILP32 for Linux on 32-bit ARM
          - ARM64: LP64 for Linux on 64-bit ARM\
          """)
  private MachineModel machineModel = MachineModel.LINUX32;

  @Option(
      secure = true,
      name = "analysis.interprocedural",
      description = "run interprocedural analysis")
  private boolean interprocedural = true;

  @Option(
      secure = true,
      name = "analysis.functionPointerCalls",
      description = "create all potential function pointer call edges")
  private boolean fptrCallEdges = true;

  @Option(
      secure = true,
      name = "analysis.threadOperationsTransform",
      description =
          "Replace thread creation operations with a special function calls"
              + "so, any analysis can go through the function")
  private boolean enableThreadOperationsInstrumentation = false;

  @Option(
      secure = true,
      name = "analysis.useGlobalVars",
      description = "add declarations for global variables before entry function")
  private boolean useGlobalVars = true;

  @Option(
      secure = true,
      name = "analysis.useLoopStructure",
      description = "add loop-structure information to CFA.")
  private boolean useLoopStructure = true;

  @Option(secure = true, name = "cfa.export", description = "export CFA as .dot file")
  private boolean exportCfa = true;

  @Option(
      secure = true,
      name = "cfa.exportPerFunction",
      description = "export individual CFAs for function as .dot files")
  private boolean exportCfaPerFunction = true;

  @Option(secure = true, name = "cfa.exportToC", description = "export CFA as C file")
  private boolean exportCfaToC = false;

  @Option(secure = true, name = "cfa.exportToC.file", description = "export CFA as C file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path exportCfaToCFile = Path.of("cfa.c");

  @Option(secure = true, name = "cfa.callgraph.export", description = "dump a simple call graph")
  private boolean exportFunctionCalls = true;

  @Option(
      secure = true,
      name = "cfa.callgraph.file",
      description = "file name for call graph as .dot file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path exportFunctionCallsFile = Path.of("functionCalls.dot");

  @Option(
      secure = true,
      name = "cfa.callgraph.fileUsed",
      description = "file name for call graph as .dot file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path exportFunctionCallsUsedFile = Path.of("functionCallsUsed.dot");

  @Option(secure = true, name = "cfa.file", description = "export CFA as .dot file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path exportCfaFile = Path.of("cfa.dot");

  @Option(
      secure = true,
      name = "cfa.pathForExportingVariablesInScopeWithTheirType",
      description =
          "the path to export a json mapping which for each"
              + " location contains the variables"
              + " in scope and their type. Please be aware that this "
              + "is **not** a stable interface and the output format of "
              + "the file may change in future versions.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path pathForExportingVariablesInScopeWithTheirType = null;

  @Option(
      secure = true,
      name = "cfa.pixelGraphicFile",
      description =
          "Export CFA as pixel graphic to the given file name. The suffix is added"
              + " corresponding"
              + " to the value of option pixelgraphic.export.format"
              + "If set to 'null', no pixel graphic is exported.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path exportCfaPixelFile = null;

  @Option(
      secure = true,
      name = "cfa.checkNullPointers",
      description =
          "while this option is activated, before each use of a "
              + "PointerExpression, or a dereferenced field access the expression is "
              + "checked if it is 0")
  private boolean checkNullPointers = false;

  @Option(
      secure = true,
      name = "cfa.expandFunctionPointerArrayAssignments",
      description =
          "When a function pointer array element is written with a variable as index, "
              + "create a series of if-else edges with explicit indizes instead.")
  private boolean expandFunctionPointerArrayAssignments = false;

  @Option(
      secure = true,
      name = "cfa.simplifyCfa",
      description = "Remove all edges which don't have any effect on the program")
  private boolean simplifyCfa = true;

  @Option(
      secure = true,
      name = "cfa.moveDeclarationsToFunctionStart",
      description =
          "With this option, all declarations in each function will be moved"
              + "to the beginning of each function. Do only use this option if you are"
              + "not able to handle initializer lists and designated initializers (like"
              + " they can be used for arrays and structs) in your analysis anyway. this"
              + " option will otherwise create c code which is not the same as the original"
              + " one")
  private boolean moveDeclarationsToFunctionStart = false;

  @Option(
      secure = true,
      name = "cfa.useFunctionCallUnwinding",
      description = "unwind recursive functioncalls (bounded to max call stack size)")
  private boolean useFunctionCallUnwinding = false;

  @Option(
      secure = true,
      name = "cfa.useCFACloningForMultiThreadedPrograms",
      description =
          "clone functions of the CFA, such that there are several "
              + "identical CFAs for each function, only with different names.")
  private boolean useCFACloningForMultiThreadedPrograms = false;

  @Option(
      secure = true,
      name = "cfa.findLiveVariables",
      description =
          "By enabling this option the variables that are live are"
              + " computed for each edge of the CFA. Live means that their value"
              + " is read later on.")
  private boolean findLiveVariables = false;

  @Option(
      secure = true,
      description =
          "Programming language of the input program. If not given explicitly, auto-detection"
              + " will occur. LLVM IR is currently unsupported as input (cf."
              + " https://gitlab.com/sosy-lab/software/cpachecker/-/issues/1356).")
  // keep option name in sync with {@link CPAMain#language} and {@link
  // ConfigurationFileChecks.OptionsWithSpecialHandlingInTest#language}, value might differ
  private Language language = Language.C;

  private Language inputLanguage = Language.C;

  // data structures for parsing ACSL annotations
  private final List<FileLocation> commentPositions = new ArrayList<>();
  private final List<SyntacticBlock> blocks = new ArrayList<>();

  private final LogManager logger;
  private final Parser parser;
  private final ShutdownNotifier shutdownNotifier;
  private static final String EXAMPLE_JAVA_METHOD_NAME =
      """
      Please note that a method has to be given in the following notation:
      <ClassName>_<MethodName>_<ParameterTypes>.
      Example: pack1.Car_drive_int_Car
      for the method drive(int speed, Car car) in the class Car.
      """;

  private static class CFACreatorStatistics implements Statistics {

    private final Timer parserInstantiationTime = new Timer();
    private final Timer totalTime = new Timer();
    private Timer parsingTime;
    private Timer conversionTime;
    private final Timer checkTime = new Timer();
    private final Timer processingTime = new Timer();
    private final Timer exportTime = new Timer();
    private final Timer loopStructureTime = new Timer();
    private final Timer astStructureTime = new Timer();
    private final List<Statistics> statisticsCollection;
    private final LogManager logger;

    private @Nullable Thread exportThread;

    private CFACreatorStatistics(LogManager pLogger) {
      logger = pLogger;
      statisticsCollection = new ArrayList<>();
    }

    @Override
    public String getName() {
      return "";
    }

    @Override
    public void printStatistics(PrintStream out, Result pResult, UnmodifiableReachedSet pReached) {
      out.println("  Time for loading parser:    " + parserInstantiationTime);
      out.println("  Time for CFA construction:  " + totalTime);
      out.println("    Time for parsing file(s): " + parsingTime);
      out.println("    Time for AST to CFA:      " + conversionTime);
      out.println("    Time for CFA sanity check:" + checkTime);
      out.println("    Time for post-processing: " + processingTime);
      out.println("    Time for loop structure:  " + loopStructureTime);
      out.println("    Time for AST structure:   " + astStructureTime);

      if (exportThread != null) {
        // If export is still running we should wait such that statistics are correct
        // and we don't kill the export once CPAchecker terminates.
        Uninterruptibles.joinUninterruptibly(exportThread);
      }
      if (exportTime.getNumberOfIntervals() > 0) {
        out.println("    Time for CFA export:      " + exportTime);
      }

      for (Statistics st : statisticsCollection) {
        StatisticsUtils.printStatistics(st, out, logger, pResult, pReached);
      }
    }

    @Override
    public void writeOutputFiles(Result pResult, UnmodifiableReachedSet pReached) {
      for (Statistics st : statisticsCollection) {
        StatisticsUtils.writeOutputFiles(st, logger, pResult, pReached);
      }
    }
  }

  private final CFACreatorStatistics stats;
  private final Configuration config;

  public CFACreator(Configuration config, LogManager logger, ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {

    config.inject(this);

    this.config = config;
    this.logger = logger;
    shutdownNotifier = pShutdownNotifier;
    stats = new CFACreatorStatistics(logger);

    stats.parserInstantiationTime.start();
    String regExPattern;
    inputLanguage = language;
    switch (language) {
      case JAVA -> {
        regExPattern = "^" + VALID_JAVA_FUNCTION_NAME_PATTERN + "$";
        if (!mainFunctionName.matches(regExPattern)) {
          throw new InvalidConfigurationException(
              "Entry function for java programs must match pattern " + regExPattern);
        }
        parser = Parsers.getJavaParser(logger, config, mainFunctionName);
      }
      case C -> {
        regExPattern = "^" + VALID_C_FUNCTION_NAME_PATTERN + "$";
        if (!mainFunctionName.matches(regExPattern)) {
          throw new InvalidConfigurationException(
              "Entry function for c programs must match pattern " + regExPattern);
        }
        CParser outerParser =
            CParser.Factory.getParser(
                logger, CParser.Factory.getOptions(config), machineModel, shutdownNotifier);

        outerParser =
            new CParserWithLocationMapper(
                config, logger, outerParser, readLineDirectives || usePreprocessor || useClang);

        if (usePreprocessor) {
          CPreprocessor preprocessor = new CPreprocessor(config, logger);
          outerParser = new CParserWithPreprocessor(outerParser, preprocessor);
        }

        if (useClang) {
          if (usePreprocessor) {
            logger.log(
                Level.WARNING, "Option --preprocess is ignored when used with option -clang");
          }
          parser = Parsers.getLlvmClangParser(config, logger, machineModel);
        } else {
          parser = outerParser;
        }
      }
      case LLVM -> {
        parser = Parsers.getLlvmParser(logger, machineModel);
        language = Language.C;
        // After parsing, we will have a CFA representing C code
      }
      case K3 -> {
        parser = Parsers.getK3Parser(logger, config, machineModel, shutdownNotifier);
      }
      default -> throw new AssertionError();
    }

    stats.parsingTime = parser.getParseTime();
    stats.conversionTime = parser.getCFAConstructionTime();

    stats.parserInstantiationTime.stop();
  }

  /**
   * Parse a program given as String and create a CFA, including all post-processing etc.
   *
   * @param program The program represented as String to parse.
   * @return A representation of the CFA.
   * @throws InvalidConfigurationException If the main function that was specified in the
   *     configuration is not found.
   * @throws ParserException If the parser or the CFA builder cannot handle the C code.
   */
  public CFA parseSourceAndCreateCFA(String program)
      throws InvalidConfigurationException, ParserException, InterruptedException {

    stats.totalTime.start();
    try {
      ParseResult parseResult = parseToCFAs(program);
      FunctionEntryNode mainFunction = parseResult.functions().get(mainFunctionName);
      assert mainFunction != null : "program lacks main function.";

      CFA cfa = createCFA(parseResult, mainFunction);

      return cfa;
    } finally {
      stats.totalTime.stop();
    }
  }

  /**
   * Parse some files and create a CFA, including all post-processing etc.
   *
   * @param sourceFiles The files to parse.
   * @return A representation of the CFA.
   * @throws InvalidConfigurationException If the main function that was specified in the
   *     configuration is not found.
   * @throws IOException If an I/O error occurs.
   * @throws ParserException If the parser or the CFA builder cannot handle the C code.
   */
  public CFA parseFileAndCreateCFA(List<String> sourceFiles)
      throws InvalidConfigurationException, IOException, ParserException, InterruptedException {

    Preconditions.checkArgument(
        !sourceFiles.isEmpty(), "At least one source file must be provided!");

    stats.totalTime.start();
    try {
      // FIRST, parse file(s) and create CFAs for each function
      logger.log(Level.FINE, "Starting parsing of file(s)");

      final ParseResult c = parseToCFAs(sourceFiles);

      logger.log(Level.FINE, "Parser Finished");

      FunctionEntryNode mainFunction;

      switch (language) {
        case JAVA -> {
          mainFunction = getJavaMainMethod(sourceFiles, mainFunctionName, c.functions());
          checkForAmbiguousMethod(mainFunction, mainFunctionName, c.functions());
        }
        case C -> mainFunction = getCMainFunction(sourceFiles, c.functions());
        case K3 -> mainFunction = getK3MainFunction(c.functions());
        default -> throw new AssertionError();
      }

      CFA cfa = createCFA(c, mainFunction);

      if (!commentPositions.isEmpty()) {
        SyntacticBlockStructureBuilder blockStructureBuilder =
            new SyntacticBlockStructureBuilder(cfa);
        blockStructureBuilder.addAll(blocks);
        cfa =
            ACSLParser.parseACSLAnnotations(
                sourceFiles, cfa, logger, commentPositions, blockStructureBuilder.build());
      }

      return cfa;

    } finally {
      stats.totalTime.stop();
    }
  }

  private FunctionEntryNode getK3MainFunction(NavigableMap<String, FunctionEntryNode> pFunctions) {
    String mainFunctioName = K3ProcedureDeclaration.mainFunctionDeclaration().getOrigName();
    return pFunctions.get(mainFunctioName);
  }

  @VisibleForTesting
  static FunctionEntryNode getJavaMainMethod(
      List<String> sourceFiles, String mainFunction, Map<String, FunctionEntryNode> cfas)
      throws InvalidConfigurationException {
    Optional<FunctionEntryNode> mainMethodKey = Optional.empty();

    for (String sourceFile : sourceFiles) {
      // Try classPath given in sourceFiles and plain method name in mainFunction
      String classPath = sourceFile.replace("\\/", ".");

      mainMethodKey = findJavaFunctionInCfa(cfas, classPath, mainFunction).stream().findFirst();

      // Try classPath given in sourceFiles and relative Path with main function name in
      // mainFunctionName
      if (mainMethodKey.isEmpty()) {
        int indexOfLastSlash = mainFunction.lastIndexOf('.');
        if (indexOfLastSlash >= 0) {
          classPath = mainFunction.substring(0, indexOfLastSlash);
          String mainFunctionExtracted = mainFunction.substring(indexOfLastSlash + 1);
          mainMethodKey =
              findJavaFunctionInCfa(cfas, classPath, mainFunctionExtracted).stream().findFirst();
        }
      }

      // Try classPath given in sourceFiles and relative Path without main function name in
      // mainFunctionName
      if (mainMethodKey.isEmpty()) {
        classPath = mainFunction;
        mainMethodKey = findJavaFunctionInCfa(cfas, classPath, "main").stream().findFirst();
      }
      if (mainMethodKey.isPresent()) {
        break;
      }
    }
    return mainMethodKey.orElseThrow(
        () ->
            new InvalidConfigurationException(
                "Method " + mainFunction + " not found.\n" + EXAMPLE_JAVA_METHOD_NAME));
  }

  private CFA createCFA(ParseResult pParseResult, FunctionEntryNode pMainFunction)
      throws InvalidConfigurationException, InterruptedException, ParserException {

    FunctionEntryNode mainFunction = pMainFunction;

    assert mainFunction != null;

    CfaMetadata cfaMetadata =
        CfaMetadata.forMandatoryAttributes(
            machineModel,
            language,
            inputLanguage,
            pParseResult.fileNames(),
            mainFunction,
            CfaConnectedness.UNCONNECTED_FUNCTIONS);
    MutableCFA cfa = new MutableCFA(pParseResult.functions(), pParseResult.cfaNodes(), cfaMetadata);

    stats.checkTime.start();

    // check the CFA of each function
    for (String functionName : cfa.getAllFunctionNames()) {
      assert CFACheck.check(
          cfa.getFunctionHead(functionName), cfa.getFunctionNodes(functionName), machineModel);
    }
    stats.checkTime.stop();

    // SECOND, do those post-processings that change the CFA by adding/removing nodes/edges
    stats.processingTime.start();

    cfa = postProcessingOnMutableCFAs(cfa, pParseResult.globalDeclarations());

    // Check CFA again after post-processings
    stats.checkTime.start();
    for (String functionName : cfa.getAllFunctionNames()) {
      assert CFACheck.check(
          cfa.getFunctionHead(functionName), cfa.getFunctionNodes(functionName), machineModel);
    }
    stats.checkTime.stop();

    // THIRD, do read-only post-processings on each single function CFA

    // Annotate CFA nodes with reverse postorder information for later use.
    cfa.entryNodes().forEach(CFAReversePostorder::assignIds);

    // get loop information
    // (needs post-order information)
    if (useLoopStructure) {
      stats.loopStructureTime.start();
      addLoopStructure(cfa);
      stats.loopStructureTime.stop();
    }

    // FOURTH, insert call and return edges and build the supergraph
    if (interprocedural) {
      logger.log(Level.FINE, "Analysis is interprocedural, adding super edges.");
      CFASecondPassBuilder spbuilder = new CFASecondPassBuilder(cfa, language, logger, config);
      spbuilder.insertCallEdgesRecursively();
      cfa.setMetadata(cfa.getMetadata().withConnectedness(CfaConnectedness.SUPERGRAPH));
    }

    // FIFTH, do post-processings on the supergraph
    // Mutating post-processings should be checked carefully for their effect
    // on the information collected above (such as loops and post-order ids).

    // (currently no such post-processings exist)

    // SIXTH, get information about the CFA,
    // the CFA should not be modified after this line.

    // Get information about variables, needed for some analysis.
    if (language == Language.C) {
      try {
        VariableClassificationBuilder builder = new VariableClassificationBuilder(config, logger);
        cfa.setVariableClassification(builder.build(cfa));
        builder.collectStatistics(stats.statisticsCollection);
      } catch (UnrecognizedCodeException e) {
        throw new CParserException(e);
      }
    }

    // create the live variables if the variable classification is present
    if (findLiveVariables
        && (cfa.getVarClassification().isPresent() || cfa.getLanguage() != Language.C)) {
      cfa.setLiveVariables(
          LiveVariables.create(
              pParseResult.globalDeclarations(), cfa, logger, shutdownNotifier, config));
    }

    stats.processingTime.stop();

    if (pParseResult.astStructure().isPresent()) {
      cfa.setAstCfaRelation(pParseResult.astStructure().orElseThrow());
    }

    final ImmutableCFA immutableCFA = cfa.immutableCopy();

    if (pParseResult.blocks().isPresent() && pParseResult.commentLocations().isPresent()) {
      commentPositions.addAll(pParseResult.commentLocations().orElseThrow());
      blocks.addAll(pParseResult.blocks().orElseThrow());
    }

    // check the super CFA starting at the main function
    stats.checkTime.start();
    assert CFACheck.check(mainFunction, null, machineModel);
    stats.checkTime.stop();

    if (((exportCfaFile != null) && (exportCfa || exportCfaPerFunction))
        || ((exportFunctionCallsFile != null) && exportFunctionCalls)
        || ((exportFunctionCallsUsedFile != null) && exportFunctionCalls)
        || (exportCfaPixelFile != null)
        || (exportCfaToCFile != null && exportCfaToC)
        || (pathForExportingVariablesInScopeWithTheirType != null)) {
      exportCFAAsync(immutableCFA);
    }

    logger.log(
        Level.FINE, "DONE, CFA for", immutableCFA.getNumberOfFunctions(), "functions created.");

    return immutableCFA;
  }

  /**
   * This method parses the program from the String and builds a CFA for each function. The
   * ParseResult is only a Wrapper for the CFAs of the functions and global declarations.
   */
  private ParseResult parseToCFAs(final String program)
      throws ParserException, InterruptedException {
    final ParseResult parseResult = parser.parseString(Path.of("test"), program);
    if (parseResult.isEmpty()) {
      switch (language) {
        case JAVA -> throw new JParserException("No methods found in program");
        case C -> throw new CParserException("No functions found in program");
        default -> throw new AssertionError();
      }
    }

    return parseResult;
  }

  /**
   * This method parses the sourceFiles and builds a CFA for each function. The ParseResult is only
   * a Wrapper for the CFAs of the functions and global declarations.
   */
  private ParseResult parseToCFAs(final List<String> sourceFiles)
      throws InvalidConfigurationException, IOException, ParserException, InterruptedException {
    final ParseResult parseResult;

    if (language == Language.C) {
      checkIfValidFiles(sourceFiles);
    } else if (language == Language.JAVA) {
      // TODO Handling is different for java as files are extracted in EclipseJavaParser
      // TODO Thus verification is different
    }

    parseResult = parser.parseFiles(sourceFiles);

    if (parseResult.isEmpty()) {
      switch (language) {
        case JAVA -> throw new JParserException("No methods found in program");
        case C -> throw new CParserException("No functions found in program");
        case K3 ->
            throw new K3ParserException(
                "No verification call found in the K3 program. Please check the syntax of your K3"
                    + " program.");
        default -> throw new AssertionError();
      }
    }

    return parseResult;
  }

  /**
   * This method changes the CFAs of the functions with adding, removing, replacing or moving
   * CFAEdges. The CFAs are independent, i.e. there are no super-edges (functioncall- and
   * return-edges) between them.
   *
   * @return either a modified old CFA or a complete new CFA
   */
  private MutableCFA postProcessingOnMutableCFAs(
      MutableCFA cfa, final List<Pair<ADeclaration, String>> globalDeclarations)
      throws InvalidConfigurationException, CParserException {
    // remove all edges which don't have any effect on the program
    if (simplifyCfa) {
      CFASimplifier.simplifyCFA(cfa);
    }

    if (moveDeclarationsToFunctionStart) {
      CFADeclarationMover declarationMover = new CFADeclarationMover(logger);
      declarationMover.moveDeclarationsToFunctionStart(cfa);
    }

    if (checkNullPointers) {
      NullPointerChecks nullPointerCheck = new NullPointerChecks(logger, config);
      nullPointerCheck.addNullPointerChecks(cfa);
    }

    if (expandFunctionPointerArrayAssignments) {
      ExpandFunctionPointerArrayAssignments transformer =
          new ExpandFunctionPointerArrayAssignments(logger);
      transformer.replaceFunctionPointerArrayAssignments(cfa);
    }

    // add atexit handlers
    if (language == Language.C) {
      AtExitTransformer atExitTransformer = new AtExitTransformer(cfa, logger, config);
      atExitTransformer.transformIfNeeded();
    }

    // add function pointer edges
    if (language == Language.C && fptrCallEdges) {
      CFunctionPointerResolver fptrResolver =
          new CFunctionPointerResolver(cfa, globalDeclarations, config, logger);
      fptrResolver.resolveFunctionPointers();
      fptrResolver.collectStatistics(stats.statisticsCollection);
    }

    // Transform pthread_create(.., &func) -> func()
    if (enableThreadOperationsInstrumentation) {
      ThreadCreateTransformer TCtransformer = new ThreadCreateTransformer(logger, config);
      TCtransformer.transform(cfa);
    }

    if (useFunctionCallUnwinding) {
      // must be done before adding global vars
      final FunctionCallUnwinder fca = new FunctionCallUnwinder(cfa, config);
      cfa = fca.unwindRecursion();
    }

    if (useCFACloningForMultiThreadedPrograms && isMultiThreadedProgram(cfa)) {
      // cloning must be done before adding global vars,
      // current use case is ThreadingCPA, thus we check for the creation of new threads first.
      logger.log(Level.INFO, "program contains concurrency, cloning functions...");
      final CFACloner cloner = new CFACloner(cfa, config);
      cfa = cloner.execute();
    }

    if (useGlobalVars) {
      // add global variables at the beginning of main
      insertGlobalDeclarations(cfa, globalDeclarations);
    }

    return cfa;
  }

  /** check, whether the program contains function calls to crate a new thread. */
  private boolean isMultiThreadedProgram(MutableCFA pCfa) {
    // for all possible edges
    for (CFAEdge edge : CFAUtils.allEdges(pCfa)) {
      // check for creation of new thread
      if (edge instanceof AStatementEdge aStatementEdge) {
        final AStatement statement = aStatementEdge.getStatement();
        if (statement instanceof AFunctionCall aFunctionCall) {
          final AExpression functionNameExp =
              aFunctionCall.getFunctionCallExpression().getFunctionNameExpression();
          if (functionNameExp instanceof AIdExpression aIdExpression) {
            if (ThreadingTransferRelation.THREAD_START.equals(aIdExpression.getName())) {
              return true;
            }
          }
        }
      }
    }
    return false;
  }

  private static Set<FunctionEntryNode> findJavaFunctionInCfa(
      Map<String, FunctionEntryNode> cfas, final String classPath, final String mainMethodName)
      throws InvalidConfigurationException {

    Set<FunctionEntryNode> nodesWithCorrectClassPath = getCfaNodesOfClass(cfas, classPath);

    // Try method name has parameters declared (No parameter is also a declared parameter)
    String fullName = classPath + "_" + mainMethodName;
    Set<FunctionEntryNode> mainMethodValues =
        nodesWithCorrectClassPath.stream()
            .filter(v -> v.getFunctionDefinition().getName().equals(fullName))
            .collect(ImmutableSet.toImmutableSet());

    // Try method name has no parameters declared
    if (mainMethodValues.isEmpty()) {

      mainMethodValues =
          nodesWithCorrectClassPath.stream()
              .filter(
                  v ->
                      ((JMethodDeclaration) v.getFunctionDefinition())
                          .getSimpleName()
                          .equals(mainMethodName))
              .collect(ImmutableSet.toImmutableSet());
    }

    if (mainMethodValues.size() >= 2) {
      StringBuilder exceptionMessage = new StringBuilder();
      mainMethodValues.forEach(
          k ->
              exceptionMessage
                  .append(((JMethodDeclaration) k.getFunctionDefinition()).getSimpleName())
                  .append("\n"));

      throw new InvalidConfigurationException(
          "Two or more matching functions for \""
              + mainMethodName
              + "\" found:\n"
              + exceptionMessage
              + EXAMPLE_JAVA_METHOD_NAME);
    }

    return mainMethodValues;
  }

  private static Set<FunctionEntryNode> getCfaNodesOfClass(
      Map<String, FunctionEntryNode> cfas, String classPath) {
    return cfas.values().stream()
        .filter(
            v ->
                ((JMethodDeclaration) v.getFunctionDefinition())
                    .getDeclaringClass()
                    .getName()
                    .equals(classPath))
        .collect(ImmutableSet.toImmutableSet());
  }

  private void checkForAmbiguousMethod(
      FunctionEntryNode mainFunction, String mainMethodName, Map<String, FunctionEntryNode> cfas) {
    if (!mainFunction.getFunctionDefinition().getName().equals(mainFunctionName)) {
      Set<FunctionEntryNode> pNodesWithCorrectClassPath =
          getCfaNodesOfClass(
              cfas,
              ((JMethodDeclaration) mainFunction.getFunctionDefinition())
                  .getDeclaringClass()
                  .getName());
      Set<FunctionEntryNode> methodsWithSameName =
          pNodesWithCorrectClassPath.stream()
              .filter(v -> hasMethodName(v, mainMethodName))
              .collect(ImmutableSet.toImmutableSet());

      if (methodsWithSameName.size() > 1) {
        String foundMethods =
            methodsWithSameName.stream()
                .map(m -> m.getFunctionDefinition().getName())
                .collect(Collectors.joining("\n"));

        logger.log(
            Level.WARNING,
            "Multiple methods with same name but different parameters found. Make sure you picked"
                + " the right one.\n"
                + "Methods found:\n\n"
                + foundMethods
                + "\n\n"
                + EXAMPLE_JAVA_METHOD_NAME);
      }
    }
  }

  private static boolean hasMethodName(FunctionEntryNode entryNode, String methodName) {
    final JMethodDeclaration functionDefinition =
        (JMethodDeclaration) entryNode.getFunctionDefinition();
    return (functionDefinition.getDeclaringClass().getName()
                + "."
                + functionDefinition.getSimpleName())
            .equals(methodName)
        || functionDefinition.getSimpleName().equals(methodName);
  }

  private void checkIfValidFiles(List<String> sourceFiles) throws InvalidConfigurationException {
    for (String file : sourceFiles) {
      checkIfValidFile(Path.of(file));
    }
  }

  private void checkIfValidFile(Path file) throws InvalidConfigurationException {

    try {
      IO.checkReadableFile(file);
    } catch (FileNotFoundException e) {
      throw new InvalidConfigurationException(e.getMessage());
    }
  }

  private FunctionEntryNode getCMainFunction(
      List<String> sourceFiles, final Map<String, FunctionEntryNode> cfas)
      throws InvalidConfigurationException {

    // try specified function
    FunctionEntryNode mainFunction = cfas.get(mainFunctionName);

    if (mainFunction != null) {
      return mainFunction;
    }

    if (!mainFunctionName.equals("main")) {
      // function explicitly given by user, but not found
      throw new InvalidConfigurationException("Function " + mainFunctionName + " not found.");
    }

    if (cfas.size() == 1) {
      // only one function available, take this one
      return Iterables.getOnlyElement(cfas.values());

    } else if (sourceFiles.size() == 1) {
      // get the AAA part out of a filename like test/program/AAA.cil.c
      Path path = Path.of(sourceFiles.getFirst()).getFileName();
      if (path != null) {
        String filename = path.toString(); // remove directory

        int indexOfDot = filename.indexOf('.');
        String baseFilename = indexOfDot >= 1 ? filename.substring(0, indexOfDot) : filename;

        // try function with same name as file
        mainFunction = cfas.get(baseFilename);
      }
    }

    if (mainFunction == null) {
      throw new InvalidConfigurationException("No entry function found, please specify one.");
    }
    return mainFunction;
  }

  private void addLoopStructure(MutableCFA cfa) {
    try {
      cfa.setLoopStructure(LoopStructure.getLoopStructure(cfa));

    } catch (ParserException e) {
      // don't abort here, because if the analysis doesn't need the loop information, we can
      // continue
      logger.logUserException(Level.WARNING, e, "Could not analyze loop structure of program.");

    } catch (OutOfMemoryError e) {
      logger.logUserException(
          Level.WARNING, e, "Could not analyze loop structure of program due to memory problems");
    }
  }

  /** Insert nodes for global declarations after first node of the CFA of the main-function. */
  private void insertGlobalDeclarations(
      final MutableCFA cfa, final List<Pair<ADeclaration, String>> globalVars) {
    if (globalVars.isEmpty()) {
      return;
    }

    if (cfa.getLanguage() == Language.C) {
      addDefaultInitializers(cfa.getMachineModel(), globalVars);
    } else {
      // TODO addDefaultInitializerForJava
    }

    // split off first node of CFA
    final FunctionEntryNode firstNode = cfa.getMainFunction();
    assert firstNode.getNumLeavingEdges() == 1;
    final CFAEdge firstEdge = firstNode.getLeavingEdge(0);
    assert firstEdge.getEdgeType() == CFAEdgeType.BlankEdge;
    final CFANode secondNode = firstEdge.getSuccessor();

    CFACreationUtils.removeEdgeFromNodes(firstEdge);

    // now the first node is not connected to the second node,
    // we can add new edges between them and then reconnect the nodes

    // insert one node to start the series of declarations
    CFANode cur = new CFANode(firstNode.getFunction());
    cfa.addNode(cur);
    final CFAEdge newFirstEdge =
        new BlankEdge("", FileLocation.DUMMY, firstNode, cur, "INIT GLOBAL VARS");
    CFACreationUtils.addEdgeUnconditionallyToCFA(newFirstEdge);

    // create a series of GlobalDeclarationEdges, one for each declaration
    for (Pair<? extends ADeclaration, String> p : globalVars) {
      ADeclaration d = p.getFirst();
      String rawSignature = p.getSecond();
      assert d.isGlobal();

      CFANode n = new CFANode(cur.getFunction());
      cfa.addNode(n);

      final CFAEdge newEdge =
          switch (cfa.getLanguage()) {
            case C ->
                new CDeclarationEdge(rawSignature, d.getFileLocation(), cur, n, (CDeclaration) d);
            case JAVA ->
                new JDeclarationEdge(rawSignature, d.getFileLocation(), cur, n, (JDeclaration) d);
            case K3 ->
                new K3DeclarationEdge(
                    Objects.requireNonNull(rawSignature),
                    d.getFileLocation(),
                    cur,
                    n,
                    (K3Declaration) d);
            default -> throw new AssertionError("unknown language");
          };
      CFACreationUtils.addEdgeUnconditionallyToCFA(newEdge);
      cur = n;
    }

    // add a blank edge connecting the declarations with the (old) second node of CFA
    final CFAEdge newLastEdge =
        new BlankEdge(
            firstEdge.getRawStatement(),
            firstEdge.getFileLocation(),
            cur,
            secondNode,
            firstEdge.getDescription());
    CFACreationUtils.addEdgeUnconditionallyToCFA(newLastEdge);
  }

  /**
   * This method adds an initializer to all global variables which do not have an explicit initial
   * value (global variables are initialized to zero by default in C).
   *
   * @param globalVars a list with all global declarations
   */
  private static void addDefaultInitializers(
      MachineModel pMachineModel, List<Pair<ADeclaration, String>> globalVars) {
    // first, collect all variables which do have an explicit initializer
    Set<String> initializedVariables = new HashSet<>();
    for (Pair<ADeclaration, String> p : globalVars) {
      if (p.getFirst() instanceof AVariableDeclaration v) {
        if (v.getInitializer() != null) {
          initializedVariables.add(v.getName());
        }
      }
    }

    // Now iterate through all declarations,
    // adding a default initializer to the first  of a variable.
    // All subsequent declarations of a variable after the one with the initializer
    // will be removed.
    Set<String> previouslyInitializedVariables = new HashSet<>();
    ListIterator<Pair<ADeclaration, String>> iterator = globalVars.listIterator();
    while (iterator.hasNext()) {
      final Pair<ADeclaration, String> p = iterator.next();

      if (p.getFirst() instanceof AVariableDeclaration) {
        CVariableDeclaration v = (CVariableDeclaration) p.getFirst();
        assert v.isGlobal();
        String name = v.getName();

        if (previouslyInitializedVariables.contains(name)) {
          // there was a full declaration before this one, we can just omit this one
          // TODO check for equality of initializers and give error message
          // if there are conflicting initializers.
          iterator.remove();

        } else if (v.getInitializer() != null) {
          previouslyInitializedVariables.add(name);

        } else if (!initializedVariables.contains(name)
            && v.getCStorageClass() == CStorageClass.AUTO) {

          // Add default variable initializer, because the storage class is AUTO
          // and there is no initializer later in the file.
          // In the case we have an incompletely defined struct
          // (e.g., "struct s;"), we cannot produce an initializer.
          // (Although there shouldn't be any variables of this type anyway.)
          CType type = v.getType().getCanonicalType();
          if (!(type instanceof CElaboratedType cElaboratedType)
              || (cElaboratedType.getKind() == ComplexTypeKind.ENUM)) {
            CInitializer initializer = CDefaults.forType(pMachineModel, type, v.getFileLocation());
            v.addInitializer(initializer);
            v =
                new CVariableDeclaration(
                    v.getFileLocation(),
                    v.isGlobal(),
                    v.getCStorageClass(),
                    v.getType(),
                    v.getName(),
                    v.getOrigName(),
                    v.getQualifiedName(),
                    initializer);

            previouslyInitializedVariables.add(name);
            iterator.set(Pair.of(v, p.getSecond())); // replace declaration
          }
        }
      }
    }
  }

  private void exportCFAAsync(final CFA cfa) {
    // Execute asynchronously, this may take several seconds for large programs on slow disks.
    // This is safe because we don't modify the CFA from this point on.
    stats.exportThread = Concurrency.newThread("CFA export thread", () -> exportCFA(cfa));
    stats.exportThread.start();
  }

  /**
   * A helper class to have some information about the type of a variable at a certain point in the
   * scope
   */
  private record AVariableDeclarationExchange(
      @JsonProperty("name") @NonNull String name,
      @JsonProperty("simpleType") @NonNull CBasicType simpleType) {

    AVariableDeclarationExchange {
      checkNotNull(name);
      checkNotNull(simpleType);
    }
  }

  /**
   * Export a json file containing information about what the types of variables are at a certain
   * location in the program.
   *
   * @param pCFA the CFA to export the information from
   */
  private void exportTypeInformationForEachVariable(CFA pCFA) {
    // This is a map from a filename, line and column to a set of variable names with their types
    // at that location.
    // To be able to export this to json we need to separate this mapping from a triple to a set
    // into multiple maps each one with a single key.
    Map<String, Map<Integer, Map<Integer, Set<AVariableDeclarationExchange>>>>
        locationToVariablesInScope = new HashMap<>();

    for (CFANode node : pCFA.nodes()) {
      Optional<FileLocation> statementContainingNode =
          pCFA.getAstCfaRelation().getStatementFileLocationForNode(node);
      if (statementContainingNode.isEmpty()) {
        continue;
      }

      Optional<FluentIterable<AbstractSimpleDeclaration>> declarationAtNode =
          pCFA.getAstCfaRelation().getVariablesAndParametersInScope(node);

      if (declarationAtNode.isEmpty()) {
        continue;
      }

      Set<AVariableDeclarationExchange> variables =
          declarationAtNode
              .orElseThrow()
              .filter(Predicates.notNull())
              .transform(
                  declaration ->
                      declaration.getType() instanceof CSimpleType pCSimpleType
                          ? new AVariableDeclarationExchange(
                              declaration.getOrigName(), pCSimpleType.getType())
                          : null)
              .toSet();

      // create a new map if it does not exist
      FileLocation statementFileLocation = statementContainingNode.orElseThrow();
      String filename = statementFileLocation.getFileName().toString();
      Integer lineNumber = statementFileLocation.getStartingLineNumber();
      Integer columnNumber = statementFileLocation.getStartColumnInLine();
      locationToVariablesInScope.putIfAbsent(filename, new HashMap<>());
      locationToVariablesInScope.get(filename).putIfAbsent(lineNumber, new HashMap<>());
      locationToVariablesInScope
          .get(filename)
          .get(lineNumber)
          .putIfAbsent(columnNumber, new HashSet<>());
      locationToVariablesInScope.get(filename).get(lineNumber).get(columnNumber).addAll(variables);
    }

    ObjectMapper mapper = new ObjectMapper(JsonFactory.builder().build());
    mapper.setSerializationInclusion(Include.NON_NULL);

    try (Writer writer =
        IO.openOutputFile(
            pathForExportingVariablesInScopeWithTheirType, Charset.defaultCharset())) {
      mapper.writeValue(writer, locationToVariablesInScope);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Could not serialize the variables in scope to json.", e);
    } catch (IOException e) {
      logger.logfUserException(
          Level.WARNING,
          e,
          "exporting information about what variables are in scope at each statement in the CFA"
              + " to %s  failed due to not being able to write to the output file.",
          pathForExportingVariablesInScopeWithTheirType);
    }
  }

  private void exportCFA(final CFA cfa) {
    stats.exportTime.start();

    // write CFA to file
    if (exportCfa && exportCfaFile != null) {
      try (Writer w = IO.openOutputFile(exportCfaFile, Charset.defaultCharset())) {
        DOTBuilder.generateDOT(w, cfa);
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not write CFA to dot file");
        // continue with analysis
      }
    }

    // write the CFA to files (one file per function)
    if (exportCfaPerFunction && exportCfaFile != null) {
      try {
        Path outdir = exportCfaFile.getParent().resolve("cfa");
        new DOTBuilder2(cfa).writeGraphs(outdir);
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not write CFA to dot files");
        // continue with analysis
      }
    }

    if (exportFunctionCalls && exportFunctionCallsFile != null) {
      try (Writer w = IO.openOutputFile(exportFunctionCallsFile, Charset.defaultCharset())) {
        FunctionCallDumper.dump(w, cfa, false);
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not write functionCalls to dot file");
        // continue with analysis
      }
    }

    if (exportFunctionCalls && exportFunctionCallsUsedFile != null) {
      try (Writer w = IO.openOutputFile(exportFunctionCallsUsedFile, Charset.defaultCharset())) {
        FunctionCallDumper.dump(w, cfa, true);
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not write functionCalls to dot file");
        // continue with analysis
      }
    }

    if (exportCfaPixelFile != null) {
      try {
        new CFAToPixelsWriter(config).write(cfa.getMainFunction(), exportCfaPixelFile);
      } catch (IOException | InvalidConfigurationException e) {
        logger.logUserException(Level.WARNING, e, "Could not write CFA as pixel graphic.");
      }
    }

    if (exportCfaToC && exportCfaToCFile != null) {
      try {
        String code = new CFAToCTranslator(config).translateCfa(cfa);
        try (Writer writer = IO.openOutputFile(exportCfaToCFile, Charset.defaultCharset())) {
          writer.write(code);
        }
      } catch (CPAException | IOException | InvalidConfigurationException e) {
        logger.logUserException(Level.WARNING, e, "Could not write CFA to C file.");
      }
    }

    if (pathForExportingVariablesInScopeWithTheirType != null) {
      exportTypeInformationForEachVariable(cfa);
    }

    stats.exportTime.stop();
  }

  public CFACreatorStatistics getStatistics() {
    return stats;
  }
}
