// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.modifications;

import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
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
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;
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

@Options(prefix = "differential")
public class ModificationsCPA implements ConfigurableProgramAnalysis {

  @Option(
      secure = true,
      description = "Program to check against",
      name = "program",
      required = true)
  @FileOption(Type.REQUIRED_INPUT_FILE)
  private Path originalProgram = null;

  @Option(
      secure = true,
      description =
          "ignore declarations when detecting modifications, "
              + "be careful when variables are renamed (could be unsound)")
  private boolean ignoreDeclarations = false;

  private final Configuration config;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final CFA cfaForComparison;
  private final TransferRelation transfer;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ModificationsCPA.class);
  }

  // originalProgram != null checked through REQUIRED_INPUT_FILE annotation
  @SuppressFBWarnings("NP")
  public ModificationsCPA(
      CFA pCfa, Configuration pConfig, LogManager pLogger, ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    pConfig.inject(this);

    config = pConfig;
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;

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
            new ModificationsTransferRelation(
                true, origFunToDeclNames, varDeclCollect.getVisitedDeclarations());
      } else {
        transfer = new ModificationsTransferRelation();
      }

    } catch (ParserException pE) {
      throw new InvalidConfigurationException("Parser error for originalProgram", pE);
    } catch (InterruptedException | IOException pE) {
      throw new AssertionError(pE);
    }
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return new FlatLatticeDomain();
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
    return new ModificationsState(node, cfaForComparison.getMainFunction());
  }
}
