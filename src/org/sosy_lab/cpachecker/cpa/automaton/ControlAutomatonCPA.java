// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.automaton;

import com.google.common.base.Verify;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MoreCollectors;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.DummyScope;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory.OptionalAnnotation;
import org.sosy_lab.cpachecker.core.defaults.BreakOnTargetsPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithBAM;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker.ProofCheckerCPA;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.globalinfo.AutomatonInfo;

/** This class implements an AutomatonAnalysis as described in the related Documentation. */
@Options(prefix = "cpa.automaton")
public class ControlAutomatonCPA
    implements StatisticsProvider, ConfigurableProgramAnalysisWithBAM, ProofCheckerCPA {

  @Option(secure = true, name = "dotExport", description = "export automaton to file")
  private boolean export = false;

  @Option(
      secure = true,
      name = "dotExportFile",
      description =
          "file for saving the automaton in DOT format (%s will be replaced with automaton name)")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate dotExportFile = PathTemplate.ofFormatString("%s.dot");

  @Option(
      secure = true,
      name = "spcExportFile",
      description =
          "file for saving the automaton in spc format (%s will be replaced with automaton name)")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate spcExportFile = PathTemplate.ofFormatString("%s.spc");

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ControlAutomatonCPA.class);
  }

  @Option(
      secure = true,
      required = false,
      description =
          "file with automaton specification for ObserverAutomatonCPA and ControlAutomatonCPA")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private Path inputFile = null;

  @Option(
      secure = true,
      description =
          "signal the analysis to break in case the given number of "
              + "error state is reached. Use -1 to disable this limit.")
  private int breakOnTargetState = 1;

  @Option(
      secure = true,
      description =
          "the maximum number of iterations performed after the "
              + "initial error is found, despite the limit given as "
              + "cpa.automaton.breakOnTargetState is not yet reached. "
              + "Use -1 to disable this limit.")
  private int extraIterationsLimit = -1;

  @Option(
      secure = true,
      description =
          "Whether to treat automaton states with an internal error "
              + "state as targets. This should be the standard use case.")
  private boolean treatErrorsAsTargets = true;

  @Option(secure = true, description = "Merge two automata states if one of them is TOP.")
  private boolean mergeOnTop = false;

  @Option(
      secure = true,
      name = "prec.topOnFinalSelfLoopingState",
      description =
          "An implicit precision: consider states with a self-loop and no other outgoing edges as"
              + " TOP.")
  private boolean topOnFinalSelfLoopingState = false;

  private final Automaton automaton;
  private final AutomatonState topState;
  private final AutomatonState bottomState;

  private final AbstractDomain automatonDomain;
  private final AutomatonStatistics stats;
  private final CFA cfa;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;

  protected ControlAutomatonCPA(
      @OptionalAnnotation Automaton pAutomaton,
      Configuration pConfig,
      LogManager pLogger,
      CFA pCFA,
      ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {

    pConfig.inject(this, ControlAutomatonCPA.class);

    cfa = pCFA;
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    if (pAutomaton != null) {
      automaton = pAutomaton;

    } else if (inputFile == null) {
      throw new InvalidConfigurationException(
          "Explicitly specified automaton CPA needs option cpa.automaton.inputFile!");

    } else {
      automaton = constructAutomataFromFile(pConfig, inputFile);
    }

    pLogger.log(Level.FINEST, "Automaton", automaton.getName(), "loaded.");

    topState = new AutomatonState.TOP(getAutomaton(), isTreatingErrorsAsTargets());
    bottomState = new AutomatonState.BOTTOM(getAutomaton(), isTreatingErrorsAsTargets());

    automatonDomain = new FlatLatticeDomain(topState);
    stats = new AutomatonStatistics(automaton);

    if (export) {
      if (dotExportFile != null) {
        try (Writer w =
            IO.openOutputFile(
                dotExportFile.getPath(automaton.getName()), Charset.defaultCharset())) {
          automaton.writeDotFile(w);
        } catch (IOException e) {
          pLogger.logUserException(Level.WARNING, e, "Could not write the automaton to DOT file");
        }
      }
      if (spcExportFile != null) {
        try {
          IO.writeFile(
              spcExportFile.getPath(automaton.getName()), Charset.defaultCharset(), automaton);
        } catch (IOException e) {
          pLogger.logUserException(Level.WARNING, e, "Could not write the automaton to SPC file");
        }
      }
    }
  }

  private Automaton constructAutomataFromFile(Configuration pConfig, Path pFile)
      throws InvalidConfigurationException {

    Scope scope =
        cfa.getLanguage() == Language.C ? new CProgramScope(cfa, logger) : DummyScope.getInstance();

    List<Automaton> lst =
        AutomatonParser.parseAutomatonFile(
            pFile,
            pConfig,
            logger,
            cfa.getMachineModel(),
            scope,
            cfa.getLanguage(),
            shutdownNotifier);

    if (lst.isEmpty()) {
      throw new InvalidConfigurationException(
          "Could not find automata in the file " + inputFile.toAbsolutePath());
    } else if (lst.size() > 1) {
      throw new InvalidConfigurationException(
          "Found "
              + lst.size()
              + " automata in the File "
              + inputFile.toAbsolutePath()
              + " The CPA can only handle ONE Automaton!");
    }

    return lst.get(0);
  }

  Automaton getAutomaton() {
    return automaton;
  }

  public void registerInAutomatonInfo(AutomatonInfo info) {
    info.register(automaton, this);
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return automatonDomain;
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    return buildInitStateForAutomaton(automaton);
  }

  public AutomatonState buildInitStateForAutomaton(Automaton pAutomaton) {
    AutomatonInternalState initState = pAutomaton.getInitialState();
    AutomatonTargetInformation safetyProp = null;
    if (initState.isTarget()) {
      for (AutomatonTransition t : initState.getTransitions()) {
        if (t.getFollowState().isTarget()) {
          Optional<AExpression> assumptionOpt =
              t.getAssumptions(null, logger, cfa.getMachineModel()).stream()
                  .collect(MoreCollectors.toOptional());
          safetyProp =
              assumptionOpt.isPresent()
                  ? new AutomatonTargetInformation(
                      pAutomaton, t, assumptionOpt.orElseThrow().toASTString())
                  : new AutomatonTargetInformation(pAutomaton, t);
          break;
        }
      }
      Verify.verifyNotNull(safetyProp);
    }
    return AutomatonState.automatonStateFactory(
        pAutomaton.getInitialVariables(),
        pAutomaton.getInitialState(),
        pAutomaton,
        0,
        0,
        safetyProp,
        isTreatingErrorsAsTargets());
  }

  @Override
  public MergeOperator getMergeOperator() {
    if (mergeOnTop) {
      return new AutomatonTopMergeOperator(automatonDomain, topState);
    } else {
      return MergeSepOperator.getInstance();
    }
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    final PrecisionAdjustment lPrecisionAdjustment;

    if (breakOnTargetState > 0) {
      lPrecisionAdjustment =
          new BreakOnTargetsPrecisionAdjustment(breakOnTargetState, extraIterationsLimit);
    } else {
      lPrecisionAdjustment = StaticPrecisionAdjustment.getInstance();
    }

    return new ControlAutomatonPrecisionAdjustment(
        topState, lPrecisionAdjustment, topOnFinalSelfLoopingState);
  }

  @Override
  public StopOperator getStopOperator() {
    return new StopSepOperator(getAbstractDomain());
  }

  @Override
  public AutomatonTransferRelation getTransferRelation() {
    return new AutomatonTransferRelation(this, logger, cfa.getMachineModel(), stats);
  }

  public AutomatonState getBottomState() {
    return bottomState;
  }

  public AutomatonState getTopState() {
    return topState;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }

  @Override
  public boolean areAbstractSuccessors(
      AbstractState pElement, CFAEdge pCfaEdge, Collection<? extends AbstractState> pSuccessors)
      throws CPATransferException, InterruptedException {
    ImmutableSet<? extends AbstractState> successors = ImmutableSet.copyOf(pSuccessors);
    ImmutableSet<? extends AbstractState> actualSuccessors =
        ImmutableSet.copyOf(
            getTransferRelation()
                .getAbstractSuccessorsForEdge(
                    pElement, SingletonPrecision.getInstance(), pCfaEdge));
    return successors.equals(actualSuccessors);
  }

  boolean isTreatingErrorsAsTargets() {
    return treatErrorsAsTargets;
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition pPartition)
      throws InterruptedException {
    return new AutomatonPrecision(automaton);
  }
}
