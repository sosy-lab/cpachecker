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

import static com.google.common.base.Predicates.instanceOf;
import static org.sosy_lab.cpachecker.util.CFAUtils.enteringEdges;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.Concurrency;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.MoreFiles;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JDeclaration;
import org.sosy_lab.cpachecker.cfa.export.DOTBuilder;
import org.sosy_lab.cpachecker.cfa.export.DOTBuilder2;
import org.sosy_lab.cpachecker.cfa.export.FunctionCallDumper;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.EclipseParsers;
import org.sosy_lab.cpachecker.cfa.postprocessing.function.CFADeclarationMover;
import org.sosy_lab.cpachecker.cfa.postprocessing.function.CFASimplifier;
import org.sosy_lab.cpachecker.cfa.postprocessing.function.CFunctionPointerResolver;
import org.sosy_lab.cpachecker.cfa.postprocessing.function.ExpandFunctionPointerArrayAssignments;
import org.sosy_lab.cpachecker.cfa.postprocessing.function.NullPointerChecks;
import org.sosy_lab.cpachecker.cfa.postprocessing.global.CFACloner;
import org.sosy_lab.cpachecker.cfa.postprocessing.global.FunctionCallUnwinder;
import org.sosy_lab.cpachecker.cfa.postprocessing.global.singleloop.CFASingleLoopTransformation;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CDefaults;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.threading.ThreadingTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CParserException;
import org.sosy_lab.cpachecker.exceptions.JParserException;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.LiveVariables;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.VariableClassification;
import org.sosy_lab.cpachecker.util.VariableClassificationBuilder;

/**
 * Class that encapsulates the whole CFA creation process.
 *
 * It is not thread-safe, but it may be re-used.
 */
@Options
public class CFACreator {

  private static final String JAVA_MAIN_METHOD_CFA_SUFFIX = "_main_String[]";

  public static final String VALID_C_FUNCTION_NAME_PATTERN = "[_a-zA-Z][_a-zA-Z0-9]*";

  @Option(secure=true, name="parser.usePreprocessor",
      description="For C files, run the preprocessor on them before parsing. " +
                  "Note that all file numbers printed by CPAchecker will refer to the pre-processed file, not the original input file.")
  private boolean usePreprocessor = false;

  @Option(secure=true, name="parser.readLineDirectives",
      description="For C files, read #line preprocessor directives and use their information for outputting line numbers."
          + " (Always enabled when pre-processing is used.)")
  private boolean readLineDirectives = false;

  @Option(secure=true, name="analysis.entryFunction", regexp="^" + VALID_C_FUNCTION_NAME_PATTERN + "$",
      description="entry function")
  private String mainFunctionName = "main";

  @Option(secure=true, name="analysis.machineModel",
      description = "the machine model, which determines the sizes of types like int")
  private MachineModel machineModel = MachineModel.LINUX32;

  @Option(secure=true, name="analysis.interprocedural",
      description="run interprocedural analysis")
  private boolean interprocedural = true;

  @Option(secure=true, name="analysis.functionPointerCalls",
      description="create all potential function pointer call edges")
  private boolean fptrCallEdges = true;

  @Option(secure=true, name="analysis.useGlobalVars",
      description="add declarations for global variables before entry function")
  private boolean useGlobalVars = true;

  @Option(secure=true, name="cfa.export",
      description="export CFA as .dot file")
  private boolean exportCfa = true;

  @Option(secure=true, name="cfa.exportPerFunction",
      description="export individual CFAs for function as .dot files")
  private boolean exportCfaPerFunction = true;

  @Option(secure=true, name="cfa.callgraph.export",
      description="dump a simple call graph")
  private boolean exportFunctionCalls = true;

  @Option(secure=true, name="cfa.callgraph.file",
      description="file name for call graph as .dot file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path exportFunctionCallsFile = Paths.get("functionCalls.dot");

  @Option(secure=true, name="cfa.file",
      description="export CFA as .dot file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path exportCfaFile = Paths.get("cfa.dot");

  @Option(secure=true, name="cfa.checkNullPointers",
      description="while this option is activated, before each use of a "
          + "PointerExpression, or a dereferenced field access the expression is "
          + "checked if it is 0")
  private boolean checkNullPointers = false;

  @Option(secure=true, name="cfa.expandFunctionPointerArrayAssignments",
      description="When a function pointer array element is written with a variable as index, "
          + "create a series of if-else edges with explicit indizes instead.")
  private boolean expandFunctionPointerArrayAssignments = false;

  @Option(secure=true, name="cfa.transformIntoSingleLoop",
      description="This option causes the control flow automaton to be "
        + "transformed into the automaton of an equivalent program with one "
        + "single loop and an artificial program counter.")
  private boolean transformIntoSingleLoop = false;

  @Option(secure=true, name="cfa.simplifyCfa",
        description="Remove all edges which don't have any effect on the program")
  private boolean simplifyCfa = true;

  @Option(secure=true, name="cfa.moveDeclarationsToFunctionStart",
      description="With this option, all declarations in each function will be moved"
          + "to the beginning of each function. Do only use this option if you are"
          + "not able to handle initializer lists and designated initializers (like"
          + " they can be used for arrays and structs) in your analysis anyway. this"
          + " option will otherwise create c code which is not the same as the original"
          + " one")
  private boolean moveDeclarationsToFunctionStart = false;

  @Option(secure=true, name="cfa.useFunctionCallUnwinding",
      description="unwind recursive functioncalls (bounded to max call stack size)")
  private boolean useFunctionCallUnwinding = false;

  @Option(secure=true, name="cfa.useCFACloningForMultiThreadedPrograms",
      description="clone functions of the CFA, such that there are several "
          + "identical CFAs for each function, only with different names.")
  private boolean useCFACloningForMultiThreadedPrograms = false;

  @Option(secure=true, name="cfa.findLiveVariables",
          description="By enabling this option the variables that are live are"
              + " computed for each edge of the cfa. Live means that their value"
              + " is read later on.")
  private boolean findLiveVariables = false;

  @Option(secure=true, name="cfa.classifyNodes",
      description="This option enables the computation of a classification of CFA nodes.")
private boolean classifyNodes = false;

  @Option(secure=true, description="C or Java?")
  private Language language = Language.C;

  private final LogManager logger;
  private final Parser parser;
  private final ShutdownNotifier shutdownNotifier;

  private static class CFACreatorStatistics implements Statistics {

    private final Timer parserInstantiationTime = new Timer();
    private final Timer totalTime = new Timer();
    private Timer parsingTime;
    private Timer conversionTime;
    private final Timer checkTime = new Timer();
    private final Timer processingTime = new Timer();
    private final Timer variableClassificationTime = new Timer();
    private final Timer exportTime = new Timer();

    @Override
    public String getName() {
      return "";
    }

    @Override
    public void printStatistics(PrintStream out, Result pResult, ReachedSet pReached) {
      out.println("  Time for loading parser:    " + parserInstantiationTime);
      out.println("  Time for CFA construction:  " + totalTime);
      out.println("    Time for parsing file(s): " + parsingTime);
      out.println("    Time for AST to CFA:      " + conversionTime);
      out.println("    Time for CFA sanity check:" + checkTime);
      out.println("    Time for post-processing: " + processingTime);
      if (variableClassificationTime.getNumberOfIntervals() > 0) {
        out.println("      Time for var class.:    " + variableClassificationTime);
      }
      if (exportTime.getNumberOfIntervals() > 0) {
        out.println("    Time for CFA export:      " + exportTime);
      }
    }
  }

  private final CFACreatorStatistics stats = new CFACreatorStatistics();
  private final Configuration config;

  public CFACreator(Configuration config, LogManager logger, ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {

    config.inject(this);

    this.config = config;
    this.logger = logger;
    this.shutdownNotifier = pShutdownNotifier;

    stats.parserInstantiationTime.start();

    switch (language) {
    case JAVA:
      parser = EclipseParsers.getJavaParser(logger, config);
      break;
    case C:
      CParser outerParser = CParser.Factory.getParser(config, logger, CParser.Factory.getOptions(config), machineModel);

      outerParser = new CParserWithLocationMapper(config, logger, outerParser,
          readLineDirectives || usePreprocessor);

      if (usePreprocessor) {
        CPreprocessor preprocessor = new CPreprocessor(config, logger);
        outerParser = new CParserWithPreprocessor(outerParser, preprocessor);
      }

      parser = outerParser;

      break;
    default:
      throw new AssertionError();
    }

    stats.parsingTime = parser.getParseTime();
    stats.conversionTime = parser.getCFAConstructionTime();

    stats.parserInstantiationTime.stop();
  }

  /**
   * Parse a program given as String and create a CFA, including all post-processing etc.
   *
   * @param program  The program represented as String to parse.
   * @return A representation of the CFA.
   * @throws InvalidConfigurationException If the main function that was specified in the configuration is not found.
   * @throws IOException If an I/O error occurs.
   * @throws ParserException If the parser or the CFA builder cannot handle the C code.
   */
  public CFA parseSourceAndCreateCFA(String program)
      throws InvalidConfigurationException, IOException, ParserException, InterruptedException {

    stats.totalTime.start();
    try {
      ParseResult parseResult = parseToCFAs(program);
      FunctionEntryNode mainFunction = parseResult.getFunctions().get(mainFunctionName);
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
   * @param sourceFiles  The files to parse.
   * @return A representation of the CFA.
   * @throws InvalidConfigurationException If the main function that was specified in the configuration is not found.
   * @throws IOException If an I/O error occurs.
   * @throws ParserException If the parser or the CFA builder cannot handle the C code.
   */
  public CFA parseFileAndCreateCFA(List<String> sourceFiles)
          throws InvalidConfigurationException, IOException, ParserException, InterruptedException {

    Preconditions.checkArgument(!sourceFiles.isEmpty(), "At least one source file must be provided!");

    stats.totalTime.start();
    try {
      // FIRST, parse file(s) and create CFAs for each function
      logger.log(Level.FINE, "Starting parsing of file(s)");

      final ParseResult c = parseToCFAs(sourceFiles);

      logger.log(Level.FINE, "Parser Finished");

      FunctionEntryNode mainFunction;

      switch (language) {
      case JAVA:
        mainFunction = getJavaMainMethod(sourceFiles, c.getFunctions());
        break;
      case C:
        mainFunction = getCMainFunction(sourceFiles, c.getFunctions());
        break;
      default:
        throw new AssertionError();
      }

      return createCFA(c, mainFunction);

    } finally {
      stats.totalTime.stop();
    }
  }

  private CFA createCFA(ParseResult pParseResult, FunctionEntryNode pMainFunction) throws InvalidConfigurationException, InterruptedException, ParserException {

    FunctionEntryNode mainFunction = pMainFunction;

    assert mainFunction != null;

    MutableCFA cfa = new MutableCFA(machineModel, pParseResult.getFunctions(), pParseResult.getCFANodes(), mainFunction, language);

    stats.checkTime.start();

    // check the CFA of each function
    for (String functionName : cfa.getAllFunctionNames()) {
      assert CFACheck.check(cfa.getFunctionHead(functionName), cfa.getFunctionNodes(functionName));
    }
    stats.checkTime.stop();

    // SECOND, do those post-processings that change the CFA by adding/removing nodes/edges
    stats.processingTime.start();

    cfa = postProcessingOnMutableCFAs(cfa, pParseResult.getGlobalDeclarations());

    // Check CFA again after post-processings
    stats.checkTime.start();
    for (String functionName : cfa.getAllFunctionNames()) {
      assert CFACheck.check(cfa.getFunctionHead(functionName), cfa.getFunctionNodes(functionName));
    }
    stats.checkTime.stop();

    // THIRD, do read-only post-processings on each single function CFA

    // Annotate CFA nodes with reverse postorder information for later use.
    for (FunctionEntryNode function : cfa.getAllFunctionHeads()) {
      CFAReversePostorder sorter = new CFAReversePostorder();
      sorter.assignSorting(function);
    }

    // get loop information
    // (needs post-order information)
    addLoopStructure(cfa);

    // FOURTH, insert call and return edges and build the supergraph
    if (interprocedural) {
      logger.log(Level.FINE, "Analysis is interprocedural, adding super edges.");
      CFASecondPassBuilder spbuilder = new CFASecondPassBuilder(cfa, language, logger, config);
      spbuilder.insertCallEdgesRecursively();
    }

    // FIFTH, do post-processings on the supergraph
    // Mutating post-processings should be checked carefully for their effect
    // on the information collected above (such as loops and post-order ids).

    // optionally transform CFA so that there is only one single loop
    if (transformIntoSingleLoop) {
      cfa = CFASingleLoopTransformation.getSingleLoopTransformation(logger, config, shutdownNotifier).apply(cfa);
      mainFunction = cfa.getMainFunction();
    }

    // SIXTH, get information about the CFA,
    // the cfa should not be modified after this line.

    // Get information about variables, needed for some analysis.
    final Optional<VariableClassification> varClassification;
    if (language == Language.C) {
      try {
        stats.variableClassificationTime.start();
        varClassification = Optional.of(new VariableClassificationBuilder(config, logger).build(cfa));
      } catch (UnrecognizedCCodeException e) {
        throw new CParserException(e);
      } finally {
        stats.variableClassificationTime.stop();
      }
    } else {
      varClassification = Optional.empty();
    }

    // create the live variables if the variable classification is present
    if (findLiveVariables &&
        (varClassification.isPresent() || cfa.getLanguage() != Language.C)) {
      cfa.setLiveVariables(LiveVariables.create(varClassification,
                                                pParseResult.getGlobalDeclarations(),
                                                cfa, logger, shutdownNotifier,
                                                config));
    }

    stats.processingTime.stop();

    final ImmutableCFA immutableCFA = cfa.makeImmutableCFA(varClassification);

    // check the super CFA starting at the main function
    stats.checkTime.start();
    assert CFACheck.check(mainFunction, null);
    stats.checkTime.stop();

    if (((exportCfaFile != null) && (exportCfa || exportCfaPerFunction))
        || ((exportFunctionCallsFile != null) && exportFunctionCalls)) {
      exportCFAAsync(immutableCFA);
    }

    logger.log(Level.FINE, "DONE, CFA for", immutableCFA.getNumberOfFunctions(), "functions created.");

    return immutableCFA;
  }

  /** This method parses the program from the String and builds a CFA for each function.
   * The ParseResult is only a Wrapper for the CFAs of the functions and global declarations. */
  private ParseResult parseToCFAs(final String program)
      throws InvalidConfigurationException, ParserException {
    final ParseResult parseResult;

    parseResult = parser.parseString("test", program);

    if (parseResult.isEmpty()) {
      switch (language) {
      case JAVA:
        throw new JParserException("No methods found in program");
      case C:
        throw new CParserException("No functions found in program");
      default:
        throw new AssertionError();
      }
    }

    return parseResult;
  }

  /** This method parses the sourceFiles and builds a CFA for each function.
   * The ParseResult is only a Wrapper for the CFAs of the functions and global declarations. */
  private ParseResult parseToCFAs(final List<String> sourceFiles)
          throws InvalidConfigurationException, IOException, ParserException, InterruptedException {
    final ParseResult parseResult;

    if (language == Language.C) {
      checkIfValidFiles(sourceFiles);
    }

    if (sourceFiles.size() == 1) {
      parseResult = parser.parseFile(sourceFiles.get(0));
    } else {
      // when there is more than one file which should be evaluated, the
      // programdenotations are separated from each other and a prefix for
      // static variables is generated
      if (language != Language.C) {
        throw new InvalidConfigurationException("Multiple program files not supported for languages other than C.");
      }

      parseResult = ((CParser) parser).parseFile(sourceFiles);
    }

    if (parseResult.isEmpty()) {
      switch (language) {
        case JAVA:
          throw new JParserException("No methods found in program");
        case C:
          throw new CParserException("No functions found in program");
        default:
          throw new AssertionError();
      }
    }

    return parseResult;
  }

  /** This method changes the CFAs of the functions with adding, removing, replacing or moving CFAEdges.
   * The CFAs are independent, i.e. there are no super-edges (functioncall- and return-edges) between them.
   *
   * @return either a modified old CFA or a complete new CFA
   */
  private MutableCFA postProcessingOnMutableCFAs(MutableCFA cfa, final List<Pair<ADeclaration, String>> globalDeclarations)
          throws InvalidConfigurationException, CParserException, InterruptedException {

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
      ExpandFunctionPointerArrayAssignments transformer = new ExpandFunctionPointerArrayAssignments(logger);
      transformer.replaceFunctionPointerArrayAssignments(cfa);
    }

    // add function pointer edges
    if (language == Language.C && fptrCallEdges) {
      CFunctionPointerResolver fptrResolver = new CFunctionPointerResolver(cfa, globalDeclarations, config, logger);
      fptrResolver.resolveFunctionPointers();
    }

    // Transform dummy loops into edges to termination nodes
    transformDummyLoopsToEdges(cfa);

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

  /** Transform dummy loops into edges to termination nodes */
  private void transformDummyLoopsToEdges(MutableCFA cfa) throws InterruptedException {
    List<CFANode> toAdd = new ArrayList<>(1);
    Predicate<Object> isBlankEdge = instanceOf(BlankEdge.class);

    for (CFANode node : cfa.getAllNodes()) {
      this.shutdownNotifier.shutdownIfNecessary();

      // only potential loop heads are interesting for us, they also need to have
      // at least one BlankEdge as successor and predecessor
      if (node.getNumEnteringEdges() < 2
          || !(node.getNumLeavingEdges() == 1 && node.getLeavingEdge(0) instanceof BlankEdge)
          || enteringEdges(node).anyMatch(isBlankEdge)) {
        continue;
      }

      Set<CFANode> visited = new HashSet<>();
      CFANode current = node;
      while (current.getNumLeavingEdges() == 1
          && current.getLeavingEdge(0) instanceof BlankEdge
          && visited.add(current)) {

        CFAEdge leavingBlankEdge = current.getLeavingEdge(0);
        CFANode succ = leavingBlankEdge.getSuccessor();

        // Found empty loop
        if (succ.equals(node)) {
          leavingBlankEdge.getPredecessor().removeLeavingEdge(leavingBlankEdge);
          leavingBlankEdge.getSuccessor().removeEnteringEdge(leavingBlankEdge);
          CFANode terminationNode = new CFATerminationNode(node.getFunctionName());
          BlankEdge terminationEdge =
              new BlankEdge(
                  leavingBlankEdge.getRawStatement(),
                  leavingBlankEdge.getFileLocation(),
                  leavingBlankEdge.getPredecessor(),
                  terminationNode,
                  leavingBlankEdge.getDescription());
          terminationEdge.getPredecessor().addLeavingEdge(terminationEdge);
          terminationEdge.getSuccessor().addEnteringEdge(terminationEdge);
          toAdd.add(terminationNode);
        }

        current = succ;
      }
    }

    for (CFANode nodeToAdd : toAdd) {
      cfa.addNode(nodeToAdd);
    }
  }

  /** check, whether the program contains function calls to crate a new thread. */
  private boolean isMultiThreadedProgram(MutableCFA pCfa) {
    // for all possible edges
    for (CFANode node : pCfa.getAllNodes()) {
      for (CFAEdge edge : CFAUtils.allLeavingEdges(node)) {
        // check for creation of new thread
        if (edge instanceof AStatementEdge) {
          final AStatement statement = ((AStatementEdge)edge).getStatement();
          if (statement instanceof AFunctionCall) {
            final AExpression functionNameExp = ((AFunctionCall)statement).getFunctionCallExpression().getFunctionNameExpression();
            if (functionNameExp instanceof AIdExpression) {
              if (ThreadingTransferRelation.THREAD_START.equals(((AIdExpression)functionNameExp).getName())){
                return true;
              }
            }
          }
        }
      }
    }
    return false;
  }

  private FunctionEntryNode getJavaMainMethod(List<String> sourceFiles, Map<String, FunctionEntryNode> cfas)
      throws InvalidConfigurationException {

    Preconditions.checkArgument(sourceFiles.size() == 1, "Multiple input files not supported by 'getJavaMainMethod'");
    String mainClassName = sourceFiles.get(0);

    // try specified function
    FunctionEntryNode mainFunction = cfas.get(mainFunctionName);

    if (mainFunction != null) {
      return mainFunction;
    }

    if (!mainFunctionName.equals("main")) {
      // function explicitly given by user, but not found
      throw new InvalidConfigurationException("Method " + mainFunctionName + " not found.\n" +
          "Please note that a method has to be given in the following notation:\n <ClassName>_" +
          "<MethodName>_<ParameterTypes>.\nExample: pack1.Car_drive_int_pack1.Car\n" +
          "for the method drive(int speed, Car car) in the class Car.");
    }

    mainFunction = cfas.get(mainClassName + JAVA_MAIN_METHOD_CFA_SUFFIX);

    if (mainFunction == null) {
      throw new InvalidConfigurationException("No main method in given main class found, please specify one.");
    }

    return mainFunction;
  }

  private void checkIfValidFiles(List<String> sourceFiles) throws InvalidConfigurationException {
    for (String file : sourceFiles) {
      checkIfValidFile(file);
    }
  }

  private void checkIfValidFile(String fileDenotation) throws InvalidConfigurationException {
    Path file = Paths.get(fileDenotation);

    try {
      MoreFiles.checkReadableFile(file);
    } catch (FileNotFoundException e) {
      throw new InvalidConfigurationException(e.getMessage());
    }
  }

  private FunctionEntryNode getCMainFunction(List<String> sourceFiles,
      final Map<String, FunctionEntryNode> cfas)
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
      Path path = Paths.get(sourceFiles.get(0)).getFileName();
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
      // don't abort here, because if the analysis doesn't need the loop information, we can continue
      logger.logUserException(Level.WARNING, e, "Could not analyze loop structure of program.");

    } catch (OutOfMemoryError e) {
      logger.logUserException(Level.WARNING, e,
          "Could not analyze loop structure of program due to memory problems");
    }
  }

  /**
   * Insert nodes for global declarations after first node of the CFA of the main-function.
   */
  private void insertGlobalDeclarations(final MutableCFA cfa, final List<Pair<ADeclaration, String>> globalVars) {
    if (globalVars.isEmpty()) {
      return;
    }

    if (cfa.getLanguage() == Language.C) {
      addDefaultInitializers(globalVars);
    } else {
      //TODO addDefaultInitializerForJava
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
    CFANode cur = new CFANode(firstNode.getFunctionName());
    cfa.addNode(cur);
    final CFAEdge newFirstEdge = new BlankEdge("", FileLocation.DUMMY, firstNode, cur, "INIT GLOBAL VARS");
    CFACreationUtils.addEdgeUnconditionallyToCFA(newFirstEdge);

    // create a series of GlobalDeclarationEdges, one for each declaration
    for (Pair<? extends ADeclaration, String> p : globalVars) {
      ADeclaration d = p.getFirst();
      String rawSignature = p.getSecond();
      assert d.isGlobal();

      CFANode n = new CFANode(cur.getFunctionName());
      cfa.addNode(n);

      final CFAEdge newEdge;
      switch (cfa.getLanguage()) {
        case C:
          newEdge = new CDeclarationEdge(rawSignature, d.getFileLocation(), cur, n, (CDeclaration) d);
          break;
        case JAVA :
          newEdge = new JDeclarationEdge(rawSignature, d.getFileLocation(), cur, n, (JDeclaration) d);
          break;
        default:
          throw new AssertionError("unknown language");
      }

      CFACreationUtils.addEdgeUnconditionallyToCFA(newEdge);
      cur = n;
    }

    // add a blank edge connecting the declarations with the (old) second node of CFA
    final CFAEdge newLastEdge = new BlankEdge(firstEdge.getRawStatement(), firstEdge.getFileLocation(),
            cur, secondNode, firstEdge.getDescription());
    CFACreationUtils.addEdgeUnconditionallyToCFA(newLastEdge);
  }

  /**
   * This method adds an initializer to all global variables which do not have
   * an explicit initial value (global variables are initialized to zero by default in C).
   * @param globalVars a list with all global declarations
   */
  private static void addDefaultInitializers(List<Pair<ADeclaration, String>> globalVars) {
    // first, collect all variables which do have an explicit initializer
    Set<String> initializedVariables = new HashSet<>();
    for (Pair<ADeclaration, String> p : globalVars) {
      if (p.getFirst() instanceof AVariableDeclaration) {
        AVariableDeclaration v = (AVariableDeclaration)p.getFirst();
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
        CVariableDeclaration v = (CVariableDeclaration)p.getFirst();
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
          if (!(type instanceof CElaboratedType)
              || (((CElaboratedType)type).getKind() == ComplexTypeKind.ENUM)) {
            CInitializer initializer = CDefaults.forType(type, v.getFileLocation());
v.addInitializer(initializer);
            v = new CVariableDeclaration(v.getFileLocation(),
                                         v.isGlobal(),
                                         v.getCStorageClass(),
                                         v.getType(),
                                         v.getName(),
                                         v.getOrigName(),
                                         v.getQualifiedName(),
                                         initializer);

            previouslyInitializedVariables.add(name);
            iterator.set(Pair.<ADeclaration, String>of(v, p.getSecond())); // replace declaration
          }
        }
      }
    }
  }

  private void exportCFAAsync(final CFA cfa) {
    // Execute asynchronously, this may take several seconds for large programs on slow disks.
    // This is safe because we don't modify the CFA from this point on.
    Concurrency.newThread("CFA export thread", () -> exportCFA(cfa)).start();
  }

  private void exportCFA(final CFA cfa) {
    stats.exportTime.start();

    // write CFA to file
    if (exportCfa && exportCfaFile != null) {
      try (Writer w = MoreFiles.openOutputFile(exportCfaFile, Charset.defaultCharset())) {
        DOTBuilder.generateDOT(w, cfa);
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e,
          "Could not write CFA to dot file");
        // continue with analysis
      }
    }

    // write the CFA to files (one file per function)
    if (exportCfaPerFunction && exportCfaFile != null) {
      try {
        Path outdir = exportCfaFile.getParent();
        new DOTBuilder2(cfa).writeGraphs(outdir);
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e,
          "Could not write CFA to dot files");
        // continue with analysis
      }
    }

    if (exportFunctionCalls && exportFunctionCallsFile != null) {
      try (Writer w = MoreFiles.openOutputFile(exportFunctionCallsFile, Charset.defaultCharset())) {
        FunctionCallDumper.dump(w, cfa);
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e,
            "Could not write functionCalls to dot file");
        // continue with analysis
      }
    }

    stats.exportTime.stop();
  }

  public CFACreatorStatistics getStatistics() {
    return stats;
  }
}
