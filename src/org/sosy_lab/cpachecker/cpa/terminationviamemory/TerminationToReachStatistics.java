// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.terminationviamemory;

import static com.google.common.base.Preconditions.checkState;
import static java.util.logging.Level.WARNING;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.termination.validation.well_foundedness.TransitionInvariantUtils;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.specification.Property.CommonVerificationProperty;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGStatistics;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.CounterexampleToWitness;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.TerminationYAMLWitnessExporter;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.YAMLWitnessExpressionType;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.AbstractInvariantEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantEntry.InvariantRecordType;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.LocationRecord;

@Options(prefix = "terminationtoreach")
public class TerminationToReachStatistics extends ARGStatistics implements Statistics {
  @Option(secure = true, name = "validation", description = "do not produce witness for validation")
  private boolean validation = false;

  @Option(
      secure = true,
      name = "yamlWitness",
      description =
          "The template from which the different "
              + "versions of the correctness witnesses will be exported. "
              + "Each version replaces the string '%s' "
              + "with its version number.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  protected PathTemplate yamlWitnessOutputFileTemplate = PathTemplate.ofFormatString("witness.yml");

  private final TerminationYAMLWitnessExporter terminationWitnessExporter;

  private ImmutableSet<Loop> nonterminatingLoops = null;
  private FormulaManagerView fmgr;
  private BooleanFormulaManagerView bfmgr;
  private CounterexampleToWitness nonterminationWitnessExporter;

  public TerminationToReachStatistics(
      Configuration pConfig, LogManager pLogger, CFA pCFA, ConfigurableProgramAnalysis pCPA)
      throws InvalidConfigurationException {
    super(
        pConfig,
        pLogger,
        pCPA,
        Specification.alwaysSatisfied()
            .withAdditionalProperties(ImmutableSet.of(CommonVerificationProperty.TERMINATION)),
        pCFA);
    pConfig.inject(this);
    terminationWitnessExporter =
        new TerminationYAMLWitnessExporter(
            pConfig,
            pCFA,
            Specification.alwaysSatisfied()
                .withAdditionalProperties(ImmutableSet.of(CommonVerificationProperty.TERMINATION)),
            pLogger);
    nonterminationWitnessExporter =
        new CounterexampleToWitness(
            pConfig,
            pCFA,
            Specification.alwaysSatisfied()
                .withAdditionalProperties(ImmutableSet.of(CommonVerificationProperty.TERMINATION)),
            pLogger);
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
    if (!validation && pResult == Result.FALSE) {
      int uniqueId = 0;
      for (CounterexampleInfo info : getAllCounterexamples(pReached).values()) {
        try {
          nonterminationWitnessExporter.export(info, yamlWitnessOutputFileTemplate, uniqueId);
        } catch (IOException e) {
          logger.logUserException(
              WARNING, e, "There is a problem when writing the witness into a file.");
        }
        uniqueId++;
      }
    }

    if (!validation && pResult == Result.TRUE) {
      exportTerminationWitness(pReached);
    }
  }

  public void setFormulaManager(FormulaManagerView pFmgr) {
    fmgr = pFmgr;
  }

  public void setBooleanFormulaManager(BooleanFormulaManagerView pBfmgr) {
    bfmgr = pBfmgr;
  }

  @Override
  public String getName() {
    return null;
  }

  void setNonterminatingLoop(ImmutableSet<Loop> pLoop) {
    checkState(nonterminatingLoops == null);
    checkState(pLoop != null);
    nonterminatingLoops = pLoop;
  }

  private void exportTerminationWitness(UnmodifiableReachedSet pReached) {
    try {
      Map<CFANode, AbstractInvariantEntry> transitionInvariants = new HashMap<>();
      for (AbstractState state :
          pReached.stream()
              .filter(
                  state ->
                      AbstractStates.extractStateByType(state, TerminationToReachState.class)
                          .getCandidateTransitionInvariant()
                          .isPresent())
              .collect(ImmutableSet.toImmutableSet())) {
        TerminationToReachState terminationState =
            AbstractStates.extractStateByType(state, TerminationToReachState.class);
        CFANode location =
            AbstractStates.extractStateByType(state, LocationState.class).getLocationNode();
        String transitionInvariantAsC;

        PartitionedRelationFormula formula =
            terminationState.getCandidateTransitionInvariant().orElseThrow();
        formula.extendPrevVarsWithPrefixSuffix("::at(", ", AnyPrev)");
        formula.extendCurrVarsWithPrefixSuffix("", "");
        // Transforming the candidate transition invariant from formula to C expression
        // and wrapping the previous variables into \\at(x, AnyPrev)
        try {
          transitionInvariantAsC =
              TransitionInvariantUtils.transformFormulaToStringWithTrivialReplacement(
                      formula.getFormula(), bfmgr, fmgr)
                  .replace("::at", "\\at");
          transitionInvariantAsC =
              TransitionInvariantUtils.removeFunctionFromVarsName(transitionInvariantAsC);
        } catch (CPAException e) {
          transitionInvariantAsC = "true";
        }

        if (transitionInvariants.containsKey(location)) {
          transitionInvariantAsC =
              transitionInvariantAsC + " || " + transitionInvariants.get(location);
          transitionInvariants.remove(location, transitionInvariants.get(location));
        }
        LocationRecord locationEntry =
            LocationRecord.createLocationRecordAtStart(
                location.getEnteringEdge(0).getFileLocation(),
                location.getFunction().getFileLocation().getFileName().toString(),
                location.getFunctionName());
        transitionInvariants.put(
            location,
            new InvariantEntry(
                transitionInvariantAsC,
                InvariantRecordType.TRANSITION_LOOP_INVARIANT.getKeyword(),
                YAMLWitnessExpressionType.EXT_C,
                locationEntry));
      }
      terminationWitnessExporter.export(
          transitionInvariants.values().stream().collect(ImmutableList.toImmutableList()),
          yamlWitnessOutputFileTemplate);
    } catch (IOException e) {
      logger.logUserException(
          WARNING, e, "There is a problem when writing the witness into a file.");
    }
  }
}
