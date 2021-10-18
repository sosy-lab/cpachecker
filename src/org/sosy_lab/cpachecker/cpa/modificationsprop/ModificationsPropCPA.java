// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.modificationsprop;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.DelegateAbstractDomain;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CToFormulaConverterWithPointerAliasing;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.FormulaEncodingWithPointerAliasingOptions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.TypeHandlerWithPointerAliasing;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;

@Options(prefix = "differential")
public class ModificationsPropCPA implements ConfigurableProgramAnalysis, AutoCloseable {

  @Option(
      secure = true,
      description = "Program to check against",
      name = "program",
      required = true)
  @FileOption(Type.REQUIRED_INPUT_FILE)
  private Path originalProgram = null;

  @Option(
      secure = true,
      // this description is not 100% accurate (no mod detection) but matches the other modification
      // CPAs leading to only one documentation entry
      description =
          "ignore declarations when detecting modifications, "
              + "be careful when variables are renamed (could be unsound)")
  private boolean ignoreDeclarations = false;

  // TODO: document and implement
  @Option(secure = true, description = "perform assumption implication check")
  private boolean implicationCheck = true;

  private final Configuration config;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final CFA cfaForComparison;
  private final TransferRelation transfer;
  private final DelegateAbstractDomain<ModificationsPropState> domain;
  private final Solver solver;
  private final CtoFormulaConverter converter;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ModificationsPropCPA.class);
  }

  // originalProgram != null checked through REQUIRED_INPUT_FILE annotation
  @SuppressFBWarnings("NP")
  public ModificationsPropCPA(
      CFA pCfa, Configuration pConfig, LogManager pLogger, ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    pConfig.inject(this);

    domain = DelegateAbstractDomain.getInstance();
    config = pConfig;
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    solver = Solver.create(pConfig, pLogger, pShutdownNotifier);
    converter =
        initializeCToFormulaConverter(
            solver.getFormulaManager(),
            pLogger,
            pConfig,
            pShutdownNotifier,
            pCfa.getMachineModel());

    // create CFA here to avoid handling of checked exceptions in #getInitialState
    CFACreator cfaCreator = new CFACreator(config, logger, shutdownNotifier);
    try {
      cfaForComparison =
          cfaCreator.parseFileAndCreateCFA(ImmutableList.of(originalProgram.toString()));

      if (ignoreDeclarations) {
        CFATraversal.DeclarationCollectingCFAVisitor varDeclCollect =
            new CFATraversal.DeclarationCollectingCFAVisitor();
        CFATraversal.dfs().traverse(cfaForComparison.getMainFunction(), varDeclCollect);
        Map<String, Set<String>> origFunToDeclNames = varDeclCollect.getVisitedDeclarations();

        varDeclCollect = new CFATraversal.DeclarationCollectingCFAVisitor();
        CFATraversal.dfs().traverse(pCfa.getMainFunction(), varDeclCollect);
        transfer =
            new ModificationsPropTransferRelation(
                true,
                implicationCheck,
                origFunToDeclNames,
                varDeclCollect.getVisitedDeclarations(),
                solver,
                converter,
                logger);
      } else {
        transfer =
            new ModificationsPropTransferRelation(implicationCheck, solver, converter, logger);
      }

    } catch (ParserException pE) {
      throw new InvalidConfigurationException("Parser error for originalProgram", pE);
    } catch (InterruptedException | IOException pE) {
      throw new AssertionError(pE);
    }
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return domain;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transfer;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return MergeSepOperator.getInstance();
  }

  @Override
  public StopOperator getStopOperator() {
    return new StopSepOperator(getAbstractDomain());
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    return new ModificationsPropState(node, cfaForComparison.getMainFunction(), ImmutableSet.of());
  }

  // TODO: think over
  // Can only be called after machineModel and formulaManager are set
  private CtoFormulaConverter initializeCToFormulaConverter(
      FormulaManagerView pFormulaManager,
      LogManager pLogger,
      Configuration pConfig,
      ShutdownNotifier pShutdownNotifier,
      MachineModel pMachineModel)
      throws InvalidConfigurationException {

    FormulaEncodingWithPointerAliasingOptions options =
        new FormulaEncodingWithPointerAliasingOptions(pConfig);
    TypeHandlerWithPointerAliasing typeHandler =
        new TypeHandlerWithPointerAliasing(logger, pMachineModel, options);

    return new CToFormulaConverterWithPointerAliasing(
        options,
        pFormulaManager,
        pMachineModel,
        Optional.empty(),
        pLogger,
        pShutdownNotifier,
        typeHandler,
        AnalysisDirection.FORWARD);
  }

  @Override
  public void close() {
    solver.close();
  }
}
