// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.giacombiner;

import com.google.common.base.Verify;
import com.google.common.collect.MoreCollectors;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.DummyScope;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.DelegateAbstractDomain;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker.ProofCheckerCPA;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonInternalState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonParser;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonTargetInformation;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonTransition;
import org.sosy_lab.cpachecker.cpa.automaton.GIAAutomatonParser;

@Options(prefix = "cpa.giacombiner")
public class GIACombinerCPA extends AbstractCPA implements ProofCheckerCPA {

  private final LogManager logger;
  private final CFA cfa;
  private final ShutdownNotifier shutdownNotifier;
  private final Configuration config;
  private final Automaton automaton1;
  private final Automaton automaton2;
  private Optional<Path> pathToOnlyAutomaton = Optional.empty();

  @Option(secure = true, description = "Filename to the first GIA")
  @FileOption(Type.OPTIONAL_INPUT_FILE)
  private Path pathToGIA1 = null;

  @Option(secure = true, description = "Filename to the first GIA")
  @FileOption(Type.OPTIONAL_INPUT_FILE)
  private Path pathToGIA2 = null;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(GIACombinerCPA.class);
  }

  private GIACombinerCPA(
      LogManager pLogger, Configuration pConfig, CFA pCFA, ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    super(DelegateAbstractDomain.getInstance(), new GIACombinerTransferRelation(pLogger, pCFA));
    pConfig.inject(this);
    this.logger = pLogger;
    this.cfa = pCFA;
    this.shutdownNotifier = pShutdownNotifier;
    this.config = pConfig;
    if (pathToGIA1 != null) automaton1 = getAutomaton(pathToGIA1);
    else {
      automaton1 = null;
      this.pathToOnlyAutomaton = Optional.of(pathToGIA2);
    }
    if (pathToGIA2 != null) {
      automaton2 = getAutomaton(pathToGIA2);
    } else {
      if (pathToOnlyAutomaton.isPresent()) {
        throw new InvalidConfigurationException("At least one path to a GIA needs to be present");
      } else {
        automaton2 = null;
        this.pathToOnlyAutomaton = Optional.of(pathToGIA1);
      }
    }
  }

  private Automaton getAutomaton(Path pPath) throws InvalidConfigurationException {
    Scope scope;
    if (cfa.getLanguage() == Language.C) {
      scope = new CProgramScope(cfa, logger);
    } else {
      scope = DummyScope.getInstance();
    }
    List<Automaton> automata =
        AutomatonParser.parseAutomatonFile(
            pPath,
            config,
            logger,
            cfa.getMachineModel(),
            scope,
            cfa.getLanguage(),
            shutdownNotifier);

    if (automata.isEmpty()) {
      throw new InvalidConfigurationException("Specification file contains no automata: " + pPath);
    } else if (GIAAutomatonParser.isGIA(automata)) {
      GIAAutomatonParser giaParsre = new GIAAutomatonParser(logger);
      automata = giaParsre.postProcessGIA(automata);
      return automata.get(0);
    }
    throw new InvalidConfigurationException("Specification file contains no GIA: " + pPath);
  }

  @Override
  public MergeOperator getMergeOperator() {
    return new MergeSepOperator();
  }

  @Override
  public StopOperator getStopOperator() {
    return new StopSepOperator(getAbstractDomain());
  }

  public Optional<Path> getPathToOnlyAutomaton() {
    return pathToOnlyAutomaton;
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    if (this.pathToOnlyAutomaton.isPresent()){
      return new GIACombinerState(new NotPresentGIAState(), new NotPresentGIAState());
    }else{
    return new GIACombinerState(
        new GIAInternalState(buildInitStateForAutomaton(automaton1)),
        new GIAInternalState(buildInitStateForAutomaton(automaton2)));}
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
        true);
  }
}
