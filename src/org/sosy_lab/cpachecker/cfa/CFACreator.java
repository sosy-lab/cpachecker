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

import static org.sosy_lab.cpachecker.util.CFAUtils.findLoops;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.logging.Level;

import org.sosy_lab.common.Files;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Path;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.EclipseParsers;
import org.sosy_lab.cpachecker.cfa.simplification.ExpressionSimplifier;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CDefaults;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CParserException;
import org.sosy_lab.cpachecker.exceptions.JParserException;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFAUtils.Loop;
import org.sosy_lab.cpachecker.util.VariableClassification;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Iterables;

/**
 * Class that encapsulates the whole CFA creation process.
 *
 * It is not thread-safe, but it may be re-used.
 */
@Options
public class CFACreator {

  private static final String JAVA_MAIN_METHOD_CFA_SUFFIX = "_main_String[]";

  public static final String VALID_C_FUNCTION_NAME_PATTERN = "[_a-zA-Z][_a-zA-Z0-9]*";

  @Option(name="parser.usePreprocessor",
      description="For C files, run the preprocessor on them before parsing. " +
                  "Note that all file numbers printed by CPAchecker will refer to the pre-processed file, not the original input file.")
  private boolean usePreprocessor = false;

  @Option(name="parser.transformTokensToLines",
      description="Preprocess the given C files before parsing: Put every single token onto a new line. "
      + "Then the line number corresponds to the token number.")
  private boolean transformTokensToLines = true;

  @Option(name="analysis.entryFunction", regexp="^" + VALID_C_FUNCTION_NAME_PATTERN + "$",
      description="entry function")
  private String mainFunctionName = "main";

  @Option(name="analysis.machineModel",
      description = "the machine model, which determines the sizes of types like int")
  private MachineModel machineModel = MachineModel.LINUX32;

  @Option(name="analysis.interprocedural",
      description="run interprocedural analysis")
  private boolean interprocedural = true;

  @Option(name="analysis.functionPointerCalls",
      description="create all potential function pointer call edges")
  private boolean fptrCallEdges = true;

  @Option(name="analysis.useGlobalVars",
      description="add declarations for global variables before entry function")
  private boolean useGlobalVars = true;

  @Option(name="analysis.simplifyExpressions",
      description="simplify pure numeral expressions like '1+2' to '3'")
  private boolean simplifyExpressions = false;

  @Option(name="cfa.useMultiEdges",
      description="combine sequences of simple edges into a single edge")
  private boolean useMultiEdges = false;

  @Option(name="cfa.removeIrrelevantForSpecification",
      description="remove paths from CFA that cannot lead to a specification violation")
  private boolean removeIrrelevantForSpecification = false;

  @Option(name="cfa.export",
      description="export CFA as .dot file")
  private boolean exportCfa = true;

  @Option(name="cfa.exportPerFunction",
      description="export individual CFAs for function as .dot files")
  private boolean exportCfaPerFunction = true;

  @Option(name="cfa.callgraph.export",
      description="dump a simple call graph")
  private boolean exportFunctionCalls = true;

  @Option(name="cfa.callgraph.file",
      description="file name for call graph as .dot file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path exportFunctionCallsFile = new Path("functionCalls.dot");

  @Option(name="cfa.file",
      description="export CFA as .dot file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path exportCfaFile = new Path("cfa.dot");

  @Option(name="cfa.checkNullPointers",
      description="while this option is activated, before each use of a "
          + "PointerExpression, or a dereferenced field access the expression is "
          + "checked if it is 0")
  private boolean checkNullPointers = false;

  @Option(name="cfa.expandFunctionPointerArrayAssignments",
      description="When a function pointer array element is written with a variable as index, "
          + "create a series of if-else edges with explicit indizes instead.")
  private boolean expandFunctionPointerArrayAssignments = false;

  @Option(name="cfa.transformIntoSingleLoop",
      description="This option causes the control flow automaton to be "
        + "transformed into the automaton of an equivalent program with one "
        + "single loop and an artificial program counter.")
  private boolean transformIntoSingleLoop = false;

  @Option(description="C or Java?")
  private Language language = Language.C;

  private final LogManager logger;
  private final Parser parser;
  private final CFAReduction cfaReduction;

  private static class CFACreatorStatistics implements Statistics {

    private final Timer parserInstantiationTime = new Timer();
    private final Timer totalTime = new Timer();
    private Timer parsingTime;
    private Timer conversionTime;
    private final Timer checkTime = new Timer();
    private final Timer processingTime = new Timer();
    private final Timer pruningTime = new Timer();
    private final Timer exportTime = new Timer();

    @Override
    public String getName() {
      return "";
    }

    @Override
    public void printStatistics(PrintStream out, Result pResult, ReachedSet pReached) {
      out.println("  Time for loading C parser:  " + parserInstantiationTime);
      out.println("  Time for CFA construction:  " + totalTime);
      out.println("    Time for parsing C file:  " + parsingTime);
      out.println("    Time for AST to CFA:      " + conversionTime);
      out.println("    Time for CFA sanity check:" + checkTime);
      out.println("    Time for post-processing: " + processingTime);
      if (pruningTime.getNumberOfIntervals() > 0) {
        out.println("    Time for CFA pruning:     " + pruningTime);
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

    stats.parserInstantiationTime.start();

    switch (language) {
    case JAVA:
      parser = EclipseParsers.getJavaParser(logger, config);
      break;
    case C:
      CParser realParser = CParser.Factory.getParser(config, logger, CParser.Factory.getOptions(config), machineModel);
      if (usePreprocessor || transformTokensToLines) {
        CPreprocessor preprocessor = new CPreprocessor(config, logger);
        if (transformTokensToLines) {
          realParser = new CParserWithTokenizer(realParser);
        }
        parser = new CParserWithPreprocessor(realParser, preprocessor);
      } else {
        parser = realParser;
      }
      break;
    default:
      throw new AssertionError();
    }

    stats.parsingTime = parser.getParseTime();
    stats.conversionTime = parser.getCFAConstructionTime();

    if (removeIrrelevantForSpecification) {
      cfaReduction = new CFAReduction(config, logger, pShutdownNotifier);
    } else {
      cfaReduction = null;
    }

    stats.parserInstantiationTime.stop();
  }

  /**
   * Parse a file and create a CFA, including all post-processing etc.
   *
   * @param sourceFiles  The files to parse.
   * @return A representation of the CFA.
   * @throws InvalidConfigurationException If the main function that was specified in the configuration is not found.
   * @throws IOException If an I/O error occurs.
   * @throws ParserException If the parser or the CFA builder cannot handle the C code.
   * @throws InterruptedException
   */
  public CFA parseFileAndCreateCFA(List<String> sourceFiles)
          throws InvalidConfigurationException, IOException, ParserException, InterruptedException {

    Preconditions.checkArgument(!sourceFiles.isEmpty(), "At least one source file must be provided!");

    stats.totalTime.start();
    try {

      // FIRST, parse file(s) and create CFAs for each function
      logger.log(Level.FINE, "Starting parsing of file");
      ParseResult c;

      if (language == Language.C) {
        checkIfValidFiles(sourceFiles);
      }

      if (sourceFiles.size() == 1) {
        c = parser.parseFile(sourceFiles.get(0));

      } else {
        // when there is more than one file which should be evaluated, the
        // programdenotations are separated from each other and a prefix for
        // static variables is generated
        if (language != Language.C) {
          throw new InvalidConfigurationException("Multiple program files not supported for languages other than C.");
        }

        List<Pair<String, String>> programFragments = new ArrayList<>();
        int counter = 0;
        String staticVarPrefix;
        for (String fileName : sourceFiles) {
          String[] tmp = fileName.split("/");
          staticVarPrefix = tmp[tmp.length-1].replaceAll("\\W", "_") + "__" + counter + "__";

          programFragments.add(Pair.of(fileName, staticVarPrefix));
        }

        c = ((CParser)parser).parseFile(programFragments);
      }

      logger.log(Level.FINE, "Parser Finished");

      if (c.isEmpty()) {
        switch (language) {
        case JAVA:
          throw new JParserException("No methods found in program");
        case C:
          throw new CParserException("No functions found in program");
        default:
          throw new AssertionError();
        }
      }

      final FunctionEntryNode mainFunction;

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


      MutableCFA cfa = new MutableCFA(machineModel, c.getFunctions(), c.getCFANodes(), mainFunction, language);

      stats.checkTime.start();

      // check the CFA of each function
      for (String functionName : cfa.getAllFunctionNames()) {
        assert CFACheck.check(cfa.getFunctionHead(functionName), cfa.getFunctionNodes(functionName));
      }
      stats.checkTime.stop();

      // SECOND, do those post-processings that change the CFA by adding/removing nodes/edges
      stats.processingTime.start();

      // remove all edges which don't have any effect on the program
      CFASimplifier.simplifyCFA(cfa);

      if (checkNullPointers) {
        CFATransformations transformations = new CFATransformations(logger, config);
        transformations.detectNullPointers(cfa);
      }

      if (expandFunctionPointerArrayAssignments) {
        ExpandFunctionPointerArrayAssignments transformer = new ExpandFunctionPointerArrayAssignments(logger, config);
        transformer.replaceFunctionPointerArrayAssignments(cfa);
      }

      // add function pointer edges
      if (language == Language.C && fptrCallEdges) {
        CFunctionPointerResolver fptrResolver = new CFunctionPointerResolver(cfa, c.getGlobalDeclarations(), config, logger);
        fptrResolver.resolveFunctionPointers();
      }

      // THIRD, do read-only post-processings on each single function CFA

      // Annotate CFA nodes with reverse postorder information for later use.
      for (FunctionEntryNode function : cfa.getAllFunctionHeads()) {
        CFAReversePostorder sorter = new CFAReversePostorder();
        sorter.assignSorting(function);
      }

      // get loop information
      // (needs post-order information)
      Optional<ImmutableMultimap<String, Loop>> loopStructure = getLoopStructure(cfa);

      // FOURTH, insert call and return edges and build the supergraph
      if (interprocedural) {
        logger.log(Level.FINE, "Analysis is interprocedural, adding super edges.");
        CFASecondPassBuilder spbuilder = new CFASecondPassBuilder(cfa, language, logger, config);
        spbuilder.insertCallEdgesRecursively();
      }

      if (useGlobalVars) {
        // add global variables at the beginning of main
        insertGlobalDeclarations(cfa, c.getGlobalDeclarations());
      }

      // FIFTH, do post-processings on the supergraph
      // Mutating post-processings should be checked carefully for their effect
      // on the information collected above (such as loops and post-order ids).

      if (simplifyExpressions) {
        // this replaces some edges in the CFA with new edges.
        // all expressions, that can be evaluated, will be replaced with their result.
        // example: a=1+2; --> a=3;
        // TODO support for constant propagation like "define MAGIC_NUMBER 1234".
        ExpressionSimplifier es = new ExpressionSimplifier(machineModel, logger);
        CFATraversal.dfs().ignoreSummaryEdges().traverseOnce(mainFunction, es);
        es.replaceEdges();
      }

      if (useMultiEdges) {
        MultiEdgeCreator.createMultiEdges(cfa);
      }

      // remove irrelevant locations
      if (cfaReduction != null) {
        stats.pruningTime.start();
        cfaReduction.removeIrrelevantForSpecification(cfa);
        stats.pruningTime.stop();

        if (cfa.isEmpty()) {
          logger.log(Level.INFO, "No states which violate the specification are syntactically reachable from the function " + mainFunction.getFunctionName()
                + ", analysis not necessary. "
                + "If you want to run the analysis anyway, set the option cfa.removeIrrelevantForSpecification to false.");

          return ImmutableCFA.empty(machineModel, language);
        }
      }

      // Get information about variables, needed for some analysis.
      final Optional<VariableClassification> varClassification
          = loopStructure.isPresent()
          ? Optional.of(new VariableClassification(cfa, config, logger, loopStructure.get()))
          : Optional.<VariableClassification>absent();

      stats.processingTime.stop();

      final ImmutableCFA immutableCFA;

      if (transformIntoSingleLoop) {
        stats.processingTime.start();
        immutableCFA = new CFASingleLoopTransformation(logger, config).apply(cfa);
        stats.processingTime.stop();
      } else {
        immutableCFA = cfa.makeImmutableCFA(loopStructure, varClassification);
      }

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

    } finally {
      stats.totalTime.stop();
    }
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
    File file = new File(fileDenotation);

    try {
      org.sosy_lab.common.Files.checkReadableFile(file);
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
      String filename = sourceFiles.get(0);

      // get the AAA part out of a filename like test/program/AAA.cil.c
      filename = (new File(filename)).getName(); // remove directory

      int indexOfDot = filename.indexOf('.');
      String baseFilename = indexOfDot >= 1 ? filename.substring(0, indexOfDot) : filename;

      // try function with same name as file
      mainFunction = cfas.get(baseFilename);
    }

    if (mainFunction == null) {
      throw new InvalidConfigurationException("No entry function found, please specify one.");
    }
    return mainFunction;
  }

  private Optional<ImmutableMultimap<String, Loop>> getLoopStructure(MutableCFA cfa) {
    try {
      ImmutableMultimap.Builder<String, Loop> loops = ImmutableMultimap.builder();
      for (String functionName : cfa.getAllFunctionNames()) {
        SortedSet<CFANode> nodes = cfa.getFunctionNodes(functionName);
        loops.putAll(functionName, findLoops(nodes, cfa.getLanguage()));
      }
      return Optional.of(loops.build());

    } catch (ParserException e) {
      // don't abort here, because if the analysis doesn't need the loop information, we can continue
      logger.logUserException(Level.WARNING, e, "Could not analyze loop structure of program.");

    } catch (OutOfMemoryError e) {
      logger.logUserException(Level.WARNING, e,
          "Could not analyze loop structure of program due to memory problems");
    }
    return Optional.absent();
  }

  /**
   * Insert nodes for global declarations after first node of CFA.
   */
  public static void insertGlobalDeclarations(final MutableCFA cfa, List<Pair<IADeclaration, String>> globalVars) {
    if (globalVars.isEmpty()) {
      return;
    }

    if (cfa.getLanguage() == Language.C) {
      addDefaultInitializers(globalVars);
    } else {
      //TODO addDefaultInitializerForJava
    }

    // split off first node of CFA
    FunctionEntryNode firstNode = cfa.getMainFunction();
    assert firstNode.getNumLeavingEdges() == 1;
    CFAEdge firstEdge = firstNode.getLeavingEdge(0);
    assert firstEdge instanceof BlankEdge;
    CFANode secondNode = firstEdge.getSuccessor();

    CFACreationUtils.removeEdgeFromNodes(firstEdge);

    // insert one node to start the series of declarations
    CFANode cur = new CFANode(0, firstNode.getFunctionName());
    cfa.addNode(cur);
    BlankEdge be = new BlankEdge("", 0, firstNode, cur, "INIT GLOBAL VARS");
    addToCFA(be);

    // create a series of GlobalDeclarationEdges, one for each declaration
    for (Pair<? extends IADeclaration, String> p : globalVars) {
      IADeclaration d = p.getFirst();
      String rawSignature = p.getSecond();
      assert d.isGlobal();

      CFANode n = new CFANode(d.getFileLocation().getStartingLineNumber(), cur.getFunctionName());
      cfa.addNode(n);

      if (cfa.getLanguage() == Language.C) {
        CDeclarationEdge e = new CDeclarationEdge(rawSignature,
            d.getFileLocation().getStartingLineNumber(), cur, n, (CDeclaration) d);
        addToCFA(e);
        cur = n;
      } else if (cfa.getLanguage() == Language.JAVA) {
        JDeclarationEdge e = new JDeclarationEdge(rawSignature,
            d.getFileLocation().getStartingLineNumber(), cur, n, (JDeclaration) d);
        addToCFA(e);
        cur = n;
      }
    }

    // and a blank edge connecting the declarations with the second node of CFA
    be = new BlankEdge(firstEdge.getRawStatement(), firstEdge.getLineNumber(), cur, secondNode, firstEdge.getDescription());
    addToCFA(be);
  }

  /**
   * This method adds an initializer to all global variables which do not have
   * an explicit initial value (global variables are initialized to zero by default in C).
   * @param globalVars a list with all global declarations
   */
  private static void addDefaultInitializers(List<Pair<IADeclaration, String>> globalVars) {
    // first, collect all variables which do have an explicit initializer
    Set<String> initializedVariables = new HashSet<>();
    for (Pair<IADeclaration, String> p : globalVars) {
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
    ListIterator<Pair<IADeclaration, String>> iterator = globalVars.listIterator();
    while (iterator.hasNext()) {
      Pair<IADeclaration, String> p = iterator.next();

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
          CInitializer initializer = CDefaults.forType(v.getType(), v.getFileLocation());
          v = new CVariableDeclaration(v.getFileLocation(),
                                       v.isGlobal(),
                                       v.getCStorageClass(),
                                       v.getType(),
                                       v.getName(),
                                       v.getOrigName(),
                                       v.getQualifiedName(),
                                       initializer);

          previouslyInitializedVariables.add(name);
          p = Pair.<IADeclaration, String>of(v, p.getSecond());
          iterator.set(p); // replace declaration
        }
      }
    }
  }

  private void exportCFAAsync(final CFA cfa) {
    // execute asynchronously, this may take several seconds for large programs on slow disks
    new Thread(new Runnable() {
      @Override
      public void run() {
        // running the following in parallel is thread-safe
        // because we don't modify the CFA from this point on
        exportCFA(cfa);
      }
    }, "CFA export thread").start();
  }

  private void exportCFA(final CFA cfa) {
    stats.exportTime.start();

    // write CFA to file
    if (exportCfa && exportCfaFile != null) {
      try (Writer w = Files.openOutputFile(exportCfaFile)) {
        DOTBuilder.generateDOT(w, cfa);
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e,
          "Could not write CFA to dot file");
        // continue with analysis
      }
    }

    // write the CFA to files (one file per function + some metainfo)
    if (exportCfaPerFunction && exportCfaFile != null) {
      try {
        Path outdir = exportCfaFile.getParent();
        DOTBuilder2.writeReport(cfa, outdir);
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e,
          "Could not write CFA to dot and json file");
        // continue with analysis
      }
    }

    if (exportFunctionCalls && exportFunctionCallsFile != null) {
      try (Writer w = Files.openOutputFile(exportFunctionCallsFile)) {
        FunctionCallDumper.dump(w, cfa);
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e,
            "Could not write functionCalls to dot file");
        // continue with analysis
      }
    }

    stats.exportTime.stop();
  }

  private static void addToCFA(CFAEdge edge) {
    edge.getPredecessor().addLeavingEdge(edge);
    edge.getSuccessor().addEnteringEdge(edge);
  }

  public CFACreatorStatistics getStatistics() {
    return stats;
  }
}
