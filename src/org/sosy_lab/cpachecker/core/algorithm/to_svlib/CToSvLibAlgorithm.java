// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.to_svlib;

import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serial;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.ImmutableCFA;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SmtLibLogic;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.SvLibCurrentScope;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibParsingVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibProcedureDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibScript;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibSmtFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibDeclareFunCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibProceduresRecDefinitionCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibSetInfoCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibSetLogicCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibVariableDeclarationCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibVerifyCallCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibSequenceStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibStatement;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ForwardingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.threading.ThreadingTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CFormulaEncodingOptions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaTypeHandler;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CFormulaEncodingWithPointerAliasingOptions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.TypeHandlerWithPointerAliasing;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.svlibwitnessexport.FormulaToSvLibVisitor;
import org.sosy_lab.java_smt.SolverContextFactory.Solvers;

@Options(prefix = "analysis.algorithm.toSvLib")
public class CToSvLibAlgorithm implements Algorithm, StatisticsProvider, AutoCloseable {

  @Option(
      secure = true,
      description = "Path to configuration file to be used for analysis of the transformed script.")
  @FileOption(Type.OPTIONAL_INPUT_FILE)
  private @Nullable Path svLibAnalysisConfiguration = null;

  @Option(
      secure = true,
      description = "Path to specification file to be used for analysis of the transformed script.")
  @FileOption(Type.OPTIONAL_INPUT_FILE)
  private @Nullable Path svLibAnalysisSpecification = null;

  @Option(
      secure = true,
      description =
          "If the transformation to SV-LIB should be performed as a preprocessing step"
              + " before the main analysis of the generated SV-LIB script begins, set to true."
              + "If the transformed SV-LIB script should just be exported and analyzed externally,"
              + " set to false.")
  private boolean runAnalysis = false;

  @Option(
      secure = true,
      description =
          "Merge every block of the generated program that only one jump reaches into the block"
              + " that ends with that jump, negating the condition of a conditional jump where"
              + " that is what makes the merge possible. The analysis of the generated program"
              + " then has larger blocks, but the program has fewer labels, so the counterexamples"
              + " and the witnesses of that analysis refer to fewer locations of the input"
              + " program.")
  private boolean useLargeBlockEncoding = false;

  @Option(secure = true, description = "Export SV-LIB script generated by transformation to file.")
  @FileOption(Type.OUTPUT_FILE)
  private @Nullable Path scriptPath = Path.of("transformedScript.svlib");

  /** The algorithm that analyses the transformed script, which its statistics are taken from. */
  private @Nullable Algorithm algorithmOfAnalysisOfScript = null;

  private final CFA cfa;

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final Configuration config;
  private final Specification specification;
  private final Solver solver;
  private final FormulaManagerView formulaManager;
  private final PathFormulaManager pathFormulaManager;

  /** The sizes of the types that the formulas of the analysis assume. */
  private final TypeHandlerWithPointerAliasing typeHandler;

  private final CtoFormulaConverter converter;

  private final SvLibCurrentScope scope;
  private final FormulaToSvLibVisitor formulaToSvLibVisitor;

  private final TransformationStatistics transformationStatistics;

  /** The configuration for the analysis of the transformed script, if that analysis is run. */
  private final @Nullable Configuration svLibAnalysisConfig;

  /**
   * Reads the SMT solver that a configuration selects, in order to be able to compare the choices
   * of two configurations.
   */
  @Options(prefix = "solver")
  private static class SolverChoice {

    // The default has to be the same as the one of the option of the same name in Solver.
    @Option(secure = true, name = "solver", description = "Which SMT solver to use.")
    private Solvers solver = Solvers.MATHSAT5;

    SolverChoice(Configuration pConfiguration) throws InvalidConfigurationException {
      pConfiguration.inject(this);
    }
  }

  /**
   * Transforms the CFA of a C program to a SvLibScript. At the moment in development and works
   * currently only for a limited subset of the C language.
   *
   * @throws InvalidConfigurationException If the program to be transformed is not a C program
   */
  public CToSvLibAlgorithm(
      Configuration pConfiguration,
      Specification pSpecification,
      LogManager pLogManager,
      ShutdownNotifier pShutdownNotifier,
      CFA pCfa)
      throws InvalidConfigurationException {
    pConfiguration.inject(this);

    if (pCfa.getLanguage() != Language.C) {
      throw new InvalidConfigurationException(
          "Currently only C programs can be transformed to SV-LIB");
    }
    if (runAnalysis && (svLibAnalysisConfiguration == null || svLibAnalysisSpecification == null)) {
      throw new InvalidConfigurationException(
          "If runAnalysis is enabled, then a configuration for the analysis of the transformed"
              + " script has to be provided.");
    }
    if (pCfa.getAllFunctionNames().contains(ThreadingTransferRelation.THREAD_START)
        || FluentIterable.from(pCfa.edges())
            .anyMatch(edge -> callsFunction(edge, ThreadingTransferRelation.THREAD_START))) {
      // The generated program has one execution and no threads, so proving it correct would say
      // nothing about a program that creates threads.
      throw new InvalidConfigurationException(
          "Transformation of a program that creates threads with "
              + ThreadingTransferRelation.THREAD_START
              + " to SV-LIB is not supported, because the generated program has no threads.");
    }

    config = pConfiguration;
    specification = pSpecification;
    svLibAnalysisConfig = runAnalysis ? loadSvLibAnalysisConfiguration(pConfiguration) : null;
    logger = pLogManager;
    shutdownNotifier = pShutdownNotifier;
    cfa = pCfa;

    solver = Solver.create(config, logger, shutdownNotifier);
    formulaManager = solver.getFormulaManager();
    pathFormulaManager =
        new PathFormulaManagerImpl(
            solver.getFormulaManager(),
            config,
            logger,
            shutdownNotifier,
            cfa,
            AnalysisDirection.FORWARD);
    converter =
        new CtoFormulaConverter(
            new CFormulaEncodingOptions(config),
            solver.getFormulaManager(),
            cfa.getMachineModel(),
            cfa.getVarClassification(),
            logger,
            shutdownNotifier,
            new CtoFormulaTypeHandler(logger, cfa.getMachineModel()),
            AnalysisDirection.FORWARD);
    typeHandler =
        new TypeHandlerWithPointerAliasing(
            logger, cfa.getMachineModel(), new CFormulaEncodingWithPointerAliasingOptions(config));

    scope = new SvLibCurrentScope();
    formulaToSvLibVisitor = new FormulaToSvLibVisitor(solver.getFormulaManager(), scope);
    transformationStatistics = new TransformationStatistics();
  }

  /**
   * Load the configuration for the analysis of the transformed script and check that it is
   * compatible with the configuration of the transformation.
   *
   * <p>Both run in the same JVM, and two different native SMT solver libraries interfere with each
   * other there: Z3 and MathSAT5 for example both bundle GMP, so the library that is loaded second
   * silently stops being able to create numerals (and MathSAT5 followed by Z3 even crashes the
   * JVM). Both therefore have to use the same solver.
   */
  private Configuration loadSvLibAnalysisConfiguration(Configuration pConfiguration)
      throws InvalidConfigurationException {
    assert svLibAnalysisConfiguration != null;
    Configuration analysisConfig;
    try {
      analysisConfig = Configuration.builder().loadFromFile(svLibAnalysisConfiguration).build();
    } catch (IOException e) {
      throw new InvalidConfigurationException(
          "Failed to load the configuration "
              + svLibAnalysisConfiguration
              + " for the analysis of the transformed SV-LIB script.",
          e);
    }

    Solvers transformationSolver = new SolverChoice(pConfiguration).solver;
    Solvers analysisSolver = new SolverChoice(analysisConfig).solver;
    if (transformationSolver != analysisSolver) {
      throw new InvalidConfigurationException(
          "The transformation to SV-LIB uses the SMT solver "
              + transformationSolver
              + ", but the configuration "
              + svLibAnalysisConfiguration
              + " for the analysis of the transformed script uses "
              + analysisSolver
              + ". Only a single native SMT solver library can be used per JVM, so both have to"
              + " use the same solver.");
    }
    return analysisConfig;
  }

  /** Does the given edge call the function with the given name? */
  private static boolean callsFunction(CFAEdge pEdge, String pFunctionName) {
    return pEdge instanceof AStatementEdge statementEdge
        && statementEdge.getStatement() instanceof AFunctionCall functionCall
        && functionCall
            .getFunctionCallExpression()
            .getFunctionNameExpression()
            .toASTString()
            .equals(pFunctionName);
  }

  /**
   * The name of the file of the transformed program, as the value of the attribute {@code :source}.
   *
   * <p>A name that is not a simple symbol of SMT-LIB, for example one that starts with a digit, has
   * to be quoted so that the generated script can be parsed again.
   */
  private String getNameOfSourceFile() {
    return CToSvLibTransformationConstants.asSymbol(
        cfa.getFileNames().getFirst().getFileName().toString());
  }

  /**
   * Transforms the {@link CFA} of a C program to a {@link SvLibScript}.
   *
   * @return The SvLibScript generated from the CFA
   */
  SvLibScript transformCfaToSvLibScript() throws CPATransferException, InterruptedException {
    ImmutableList.Builder<SvLibCommand> commandsCollector = ImmutableList.builder();
    commandsCollector.add(
        new SvLibSetLogicCommand(SmtLibLogic.ALL, FileLocation.DUMMY),
        new SvLibSetInfoCommand(":format-version", "1.0", FileLocation.DUMMY),
        new SvLibSetInfoCommand(":source", getNameOfSourceFile(), FileLocation.DUMMY),
        new SvLibSetInfoCommand(":producer", "CPAchecker", FileLocation.DUMMY));

    // 1. Step: Initialize CurrentScope with declarations of procedures and global variables,
    // global variables are added to scope +  declaration commands are added to commandsCollector
    transformationStatistics.initializationTime.start();
    try {
      CToSvLibInitializer initializer =
          new CToSvLibInitializer(
              logger, cfa, scope, formulaManager, pathFormulaManager, converter);
      initializer.initialize(commandsCollector);
    } finally {
      transformationStatistics.initializationTime.stop();
    }

    // 2. Step: transform each function to a procedure body
    transformationStatistics.transformationTime.start();
    ImmutableList.Builder<SvLibProcedureDeclaration> procedureDeclarationCollector =
        ImmutableList.builder();
    ImmutableList.Builder<SvLibStatement> procedureBodiesCollector = ImmutableList.builder();

    CToSvLibTransformation transformation =
        new CToSvLibTransformation(
            cfa,
            formulaManager,
            pathFormulaManager,
            formulaToSvLibVisitor,
            scope,
            typeHandler,
            useLargeBlockEncoding);

    try {
      List<SvLibStatement> procedureBodies = new ArrayList<>();
      List<FunctionEntryNode> transformedFunctions = new ArrayList<>();
      for (FunctionEntryNode functionEntryNode : cfa.entryNodes()) {
        SvLibStatement procedureBody =
            transformation.transformFunction((CFunctionEntryNode) functionEntryNode);

        procedureDeclarationCollector.add(
            scope.getProcedureDeclaration(functionEntryNode.getFunctionName()));
        transformedFunctions.add(functionEntryNode);
        procedureBodies.add(procedureBody);
      }

      // Every procedure assumes what holds for the addresses of its own objects, and the procedure
      // that the execution begins with also does so for the global ones. Which objects a procedure
      // has is only used here, because the variable that the assumptions need is declared by the
      // first allocation, which can be in any procedure.
      for (int index = 0; index < procedureBodies.size(); index++) {
        FunctionEntryNode function = transformedFunctions.get(index);
        SvLibStatement assumptions =
            transformation.getSeparationOfAllocationsFromObjectsOf(
                scope.getProcedureDeclaration(function.getFunctionName()).getProcedureName(),
                function.equals(cfa.getMainFunction()));
        if (assumptions instanceof SvLibSequenceStatement sequence
            && sequence.getStatements().isEmpty()) {
          continue;
        }
        if (procedureBodies.get(index) instanceof SvLibSequenceStatement body) {
          procedureBodies.set(
              index,
              new SvLibSequenceStatement(
                  ImmutableList.<SvLibStatement>builder()
                      .add(assumptions)
                      .addAll(body.getStatements())
                      .build(),
                  body.getFileLocation(),
                  body.getTagAttributes(),
                  body.getTagReferences()));
        }
      }
      procedureBodiesCollector.addAll(procedureBodies);
    } finally {
      transformationStatistics.transformationTime.stop();
    }

    // The variables that the transformation and the transformed formulas introduced have to be
    // declared before the procedures that use them.
    for (SvLibParsingVariableDeclaration variableOfTransformation :
        Iterables.concat(
            transformation.getVariablesOfTransformation(),
            formulaToSvLibVisitor.getVariablesOfFormulas())) {
      commandsCollector.add(
          new SvLibVariableDeclarationCommand(variableOfTransformation, FileLocation.DUMMY));
    }

    // The uninterpreted functions that the transformation of the procedures encountered have to be
    // declared before the procedures that use them.
    for (SvLibSmtFunctionDeclaration functionDeclaration : scope.getFunctionDeclarations()) {
      commandsCollector.add(new SvLibDeclareFunCommand(functionDeclaration, FileLocation.DUMMY));
    }

    SvLibProceduresRecDefinitionCommand proceduresRecDefinitionCommand =
        new SvLibProceduresRecDefinitionCommand(
            FileLocation.DUMMY,
            procedureDeclarationCollector.build(),
            procedureBodiesCollector.build());
    commandsCollector.add(proceduresRecDefinitionCommand);

    // 3. Step: encode property
    CToSvLibPropertyEncoder propertyEncoder = new CToSvLibPropertyEncoder(specification);
    propertyEncoder.encodeProperty(commandsCollector);
    if (!propertyEncoder.hasEncodedProperty()) {
      // Without an annotation nothing of the generated program can be violated, so the analysis of
      // it would report that the program is correct whatever it does.
      logger.log(
          Level.WARNING,
          "The generated program has no property to check, because the specification does not"
              + " apply to any of its procedures. Its analysis cannot find a violation.");
    }

    commandsCollector.add(
        new SvLibVerifyCallCommand(
            scope.getProcedureDeclaration(cfa.getMainFunction().getFunctionName()),
            ImmutableList.of(),
            FileLocation.DUMMY));

    ImmutableList<SvLibCommand> commandsCollectorBuilt = commandsCollector.build();

    transformationStatistics.numberOfCommands = commandsCollectorBuilt.size();
    return new SvLibScript(commandsCollectorBuilt, FileLocation.DUMMY);
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {

    SvLibScript transformationResultScript;

    logger.log(Level.INFO, "Starting transformation of the input C program to SV-LIB.");
    transformationStatistics.totalTransformationTime.start();
    try {
      transformationResultScript = transformCfaToSvLibScript();
    } finally {
      transformationStatistics.totalTransformationTime.stop();
    }
    logger.log(Level.INFO, "Finished transformation of the input C program to SV-LIB.");

    // The generated script is exported in both cases, because it is the artifact that is needed to
    // understand the result of the analysis of it.
    String generatedScript = transformationResultScript.toASTString();
    handleExport(generatedScript);
    if (!runAnalysis) {
      return AlgorithmStatus.NO_PROPERTY_CHECKED;
    }

    ImmutableCFA newSvLibCfa;
    final CoreComponentsFactory coreComponents;
    final ConfigurableProgramAnalysis cpa;
    try {
      // The configuration is already loaded and checked in the constructor
      assert svLibAnalysisConfiguration != null && svLibAnalysisConfig != null;
      Configuration innerConfig = svLibAnalysisConfig;

      CFACreator cfaCreator = new CFACreator(innerConfig, logger, shutdownNotifier);
      newSvLibCfa = cfaCreator.parseSourceAndCreateCFA(generatedScript);

      coreComponents =
          new CoreComponentsFactory(
              innerConfig, logger, shutdownNotifier, AggregatedReachedSets.empty(), newSvLibCfa);

      assert svLibAnalysisSpecification != null;
      Specification svLibSpecification =
          Specification.fromFiles(
              ImmutableList.of(svLibAnalysisSpecification),
              newSvLibCfa,
              innerConfig,
              logger,
              shutdownNotifier);

      cpa = coreComponents.createCPA(svLibSpecification);
      if (cpa instanceof StatisticsProvider statisticsProvider) {
        statisticsProvider.collectStatistics(transformationStatistics.innerStatistics);
      }

      algorithmOfAnalysisOfScript = coreComponents.createAlgorithm(cpa, svLibSpecification);
      if (algorithmOfAnalysisOfScript instanceof StatisticsProvider statisticsProvider) {
        statisticsProvider.collectStatistics(transformationStatistics.innerStatistics);
      }
    } catch (InvalidConfigurationException e) {
      throw new UnsupportedTransformationException(
          "Building the algorithm which should be run on the transformed SV-LIB script failed: "
              + e.getMessage(),
          e);
    } catch (ParserException e) {
      throw new UnsupportedTransformationException(
          "Failed to create a CFA for the transformed SV-LIB script.", e);
    }

    // The analysis of the transformed script gets its own reached set, because the one of this
    // algorithm was created with this configuration, whose waitlist is not the one that the
    // configuration for that analysis asks for. Exploring an SV-LIB program in the wrong order
    // costs an order of magnitude, since the states of a merging analysis have to meet.
    Preconditions.checkArgument(
        pReachedSet instanceof ForwardingReachedSet,
        "The analysis of the transformed SV-LIB script needs a ForwardingReachedSet");
    ReachedSet reachedSetOfAnalysis = coreComponents.createReachedSet(cpa);
    coreComponents.initializeReachedSet(reachedSetOfAnalysis, newSvLibCfa.getMainFunction(), cpa);
    ((ForwardingReachedSet) pReachedSet).setDelegate(reachedSetOfAnalysis);

    AlgorithmStatus algorithmStatus;
    transformationStatistics.innerAnalysisTimer.start();
    try {
      algorithmStatus = algorithmOfAnalysisOfScript.run(reachedSetOfAnalysis);
    } finally {
      transformationStatistics.innerAnalysisTimer.stop();
    }
    return algorithmStatus;
  }

  private void handleExport(String pOutputScript) {
    // write script, if the path is successfully determined
    if (scriptPath != null) {
      try {
        try (Writer writer = IO.openOutputFile(scriptPath, Charset.defaultCharset())) {
          writer.write(pOutputScript);
          logger.log(Level.INFO, "Transformed script exported to: " + scriptPath);
        }
      } catch (IOException e) {
        logger.logUserException(
            Level.WARNING,
            e,
            "An IO error occurred while writing the output script. The transformed script was not"
                + " exported.");
      }
    } else {
      logger.log(Level.WARNING, "Could not determine path for transformed script.");
    }
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(transformationStatistics);
  }

  @Override
  public void close() {
    // The analysis of the transformed script is not closed, although it has a solver of its own:
    // both use the same native library, and closing one of the two contexts of it makes that
    // library abort the process with std::bad_alloc while the other one is still in use.
    solver.close();
  }

  private static class TransformationStatistics implements Statistics {

    private final List<Statistics> innerStatistics = new ArrayList<>();

    private final StatTimer totalTransformationTime = new StatTimer("Total transformation time");
    private final StatTimer initializationTime = new StatTimer("time to initialize scope");
    private final StatTimer transformationTime = new StatTimer("Transformation time");
    private int numberOfCommands;
    private final StatTimer innerAnalysisTimer = new StatTimer("Time for inner analysis");

    private TransformationStatistics() {
      numberOfCommands = 0;
    }

    @Override
    public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {
      out.println("Total time for transformation:     " + totalTransformationTime);
      out.println("Time to initialize scope:          " + initializationTime);
      out.println("Time for transformation:           " + transformationTime);
      out.println("Number of commands:                " + numberOfCommands);
      out.println("Time for inner analysis            " + innerAnalysisTimer);
      out.println("\n-- Inner statistics --\n");

      for (Statistics statistics : innerStatistics) {
        statistics.printStatistics(out, result, reached);
      }
    }

    @Override
    public String getName() {
      return "C to SV-LIB Transformation Statistics";
    }
  }

  private static class UnsupportedTransformationException extends CPAException {
    @Serial private static final long serialVersionUID = 7983804183988784111L;

    UnsupportedTransformationException(String msg, Throwable cause) {
      super(msg, cause);
    }
  }
}
