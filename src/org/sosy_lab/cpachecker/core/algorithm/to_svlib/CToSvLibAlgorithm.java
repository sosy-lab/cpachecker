// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.to_svlib;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.PrintStream;
import java.io.Serial;
import java.nio.file.Path;
import java.util.Collection;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.ImmutableCFA;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SmtLibLogic;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.SvLibCurrentScope;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibProcedureDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibScript;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibProceduresRecDefinitionCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibSetInfoCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibSetLogicCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibVerifyCallCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibStatement;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CFormulaEncodingOptions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaTypeHandler;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.svlibwitnessexport.FormulaToSvLibVisitor;

@Options(prefix = "analysis.algorithm.toSvLib")
public class CToSvLibAlgorithm implements Algorithm, StatisticsProvider, AutoCloseable {

  @Option(
      secure = true,
      description =
          "If the transformation to SV-LIB should be performed as a preprocessing step"
              + " before the main analysis of the generated SV-LIB script begins, set to true."
              + "If the transformed SV-LIB script should just be exported and analyzed externally,"
              + " set to false.")
  private boolean runAnalysis = false;

  private final CFA cfa;

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final Configuration config;
  private final Specification specification;
  private final Solver solver;
  private final FormulaManagerView formulaManager;
  private final PathFormulaManager pathFormulaManager;
  private final CtoFormulaConverter converter;

  private final SvLibCurrentScope scope;
  private final FormulaToSvLibVisitor formulaToSvLibVisitor;

  private final TransformationStatistics transformationStatistics;

  private final String INPUT_DUMMY_VAR_PREFIX = "__originalInput_@";

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
    logger = pLogManager;
    shutdownNotifier = pShutdownNotifier;
    config = pConfiguration;
    specification = pSpecification;

    cfa = pCfa;
    if (cfa.getLanguage() != Language.C) {
      throw new InvalidConfigurationException(
          "Currently only C programs can be transformed to SV-LIB");
    }

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

    scope = new SvLibCurrentScope();
    formulaToSvLibVisitor = new FormulaToSvLibVisitor(solver.getFormulaManager(), scope);
    transformationStatistics = new TransformationStatistics();
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
        new SvLibSetInfoCommand(":format-version", "1.0", FileLocation.DUMMY));

    // 1. Step: Initialize CurrentScope with declarations of procedures and global variables,
    // global variables are added to scope +  declaration commands are added to commandsCollector
    transformationStatistics.initializationTime.start();
    try {
      CToSvLibInitializer initializer =
          new CToSvLibInitializer(cfa, scope, formulaManager, converter, INPUT_DUMMY_VAR_PREFIX);
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
            formulaManager,
            pathFormulaManager,
            formulaToSvLibVisitor,
            scope,
            INPUT_DUMMY_VAR_PREFIX);

    try {
      for (FunctionEntryNode functionEntryNode : cfa.entryNodes()) {
        SvLibStatement procedureBody =
            transformation.transformFunction((CFunctionEntryNode) functionEntryNode);

        procedureDeclarationCollector.add(
            scope.getProcedureDeclaration(functionEntryNode.getFunctionName()));
        procedureBodiesCollector.add(procedureBody);
      }
    } finally {
      transformationStatistics.transformationTime.stop();
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

    if (!runAnalysis) {
      // handleExport(transformationResultScript.toASTString());
      return AlgorithmStatus.NO_PROPERTY_CHECKED;
    }

    ImmutableCFA newSvLibCfa;
    final CoreComponentsFactory coreComponents;
    final ConfigurableProgramAnalysis cpa;
    Algorithm innerAlgorithm;
    try {
      // update config, set transformToSvLib to false and language to svlib
      Configuration newConfig =
          Configuration.builder()
              .copyFrom(config)
              .setOptions(
                  ImmutableMap.of(
                      "analysis.algorithm.transformToSvLib", "false", "language", "svlib"))
              .build();

      CFACreator cfaCreator = new CFACreator(newConfig, logger, shutdownNotifier);
      newSvLibCfa = cfaCreator.parseSourceAndCreateCFA(transformationResultScript.toASTString());

      coreComponents =
          new CoreComponentsFactory(
              newConfig, logger, shutdownNotifier, AggregatedReachedSets.empty(), newSvLibCfa);

      Specification svLibSpecification =
          Specification.fromFiles(
              ImmutableList.of(
                  Path.of("config", "specification", "correct-tags.spc").toAbsolutePath()),
              newSvLibCfa,
              newConfig,
              logger,
              shutdownNotifier);

      cpa = coreComponents.createCPA(svLibSpecification);
      /* if (cpa instanceof StatisticsProvider statisticsProvider) {
        // statisticsProvider.collectStatistics(inner stats);
      }*/

      innerAlgorithm = coreComponents.createAlgorithm(cpa, svLibSpecification);
      /* if (innerAlgorithm instanceof StatisticsProvider statisticsProvider) {
        // statisticsProvider.collectStatistics(inner stats);
      }*/
    } catch (InvalidConfigurationException e) {
      throw new UnsupportedTransformationException(
          "Building the algorithm which should be run on the transformed SV-LIB script failed.", e);
    } catch (ParserException e) {
      throw new UnsupportedOperationException(
          "Failed to create a CFA for the transformed SV-LIB script.", e);
    }

    // Prepare new reached set
    pReachedSet.clear();
    coreComponents.initializeReachedSet(pReachedSet, newSvLibCfa.getMainFunction(), cpa);

    return innerAlgorithm.run(pReachedSet);
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {}

  @Override
  public void close() {
    solver.close();
  }

  private static class TransformationStatistics implements Statistics {

    private final StatTimer totalTransformationTime = new StatTimer("Total Transformation Time");
    private final StatTimer initializationTime = new StatTimer("Time to initialize scope");
    private final StatTimer transformationTime = new StatTimer("Transformation Time");
    private int numberOfCommands;
    private int numberOfLabelsCreated;

    private TransformationStatistics() {
      numberOfCommands = 0;
      numberOfLabelsCreated = 0;
    }

    @Override
    public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {
      out.println("Total time for transformation:     " + totalTransformationTime);
      out.println("Time to initialize scope:          " + initializationTime);
      out.println("Time for transformation:           " + transformationTime);
      out.println("Number of commands:                " + numberOfCommands);
      out.println("Number of labels created:          " + numberOfLabelsCreated);
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
