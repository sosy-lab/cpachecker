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
import static org.sosy_lab.cpachecker.core.algorithm.termination.validation.well_foundedness.TransitionInvariantUtils.ANYPREV_SUFFIX;
import static org.sosy_lab.cpachecker.core.algorithm.termination.validation.well_foundedness.TransitionInvariantUtils.AT_PREFIX;
import static org.sosy_lab.cpachecker.core.algorithm.termination.validation.well_foundedness.TransitionInvariantUtils.AT_PREFIX_NON_C;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
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
import org.sosy_lab.cpachecker.util.ast.AstCfaRelation;
import org.sosy_lab.cpachecker.util.ast.IterationElement;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.CounterexampleToWitness;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.TerminationYAMLWitnessExporter;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.YAMLWitnessExpressionType;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantEntry.InvariantRecordType;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.LocationRecord;

public class TerminationToReachStatistics extends ARGStatistics implements Statistics {
  private ImmutableSet<Loop> nonterminatingLoops = null;
  private final TerminationYAMLWitnessExporter terminationWitnessExporter;
  private FormulaManagerView fmgr;
  private BooleanFormulaManagerView bfmgr;
  private CounterexampleToWitness nonterminationWitnessExporter;
  private Scope scope;

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

    scope = new CProgramScope(pCFA, pLogger);
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
    if (terminationWitnessExporter.isExportEnabled() && pResult == Result.FALSE) {
      int uniqueId = 0;
      for (CounterexampleInfo info : getAllCounterexamples(pReached).values()) {
        try {
          nonterminationWitnessExporter.export(
              info, terminationWitnessExporter.getOutputFileTemplate(), uniqueId);
        } catch (IOException e) {
          logger.logUserException(
              WARNING, e, "There is a problem when writing the witness into a file.");
        }
        uniqueId++;
      }
    }

    if (terminationWitnessExporter.isExportEnabled() && pResult == Result.TRUE) {
      exportTerminationWitness(pReached);
    }
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

  public void setFormulaManager(FormulaManagerView pFmgr) {
    fmgr = pFmgr;
  }

  public void setBooleanFormulaManager(BooleanFormulaManagerView pBfmgr) {
    bfmgr = pBfmgr;
  }

  private void exportTerminationWitness(UnmodifiableReachedSet pReached) {
    Map<FileLocation, InvariantEntry> transitionInvariants = new HashMap<>();
    AstCfaRelation astCfaRelation = cfa.getAstCfaRelation();
    for (AbstractState state :
        pReached.stream()
            .filter(
                state ->
                    !AbstractStates.extractStateByType(state, TerminationToReachState.class)
                        .getTransitionInvariants()
                        .isEmpty())
            .collect(ImmutableSet.toImmutableSet())) {
      TerminationToReachState terminationState =
          AbstractStates.extractStateByType(state, TerminationToReachState.class);
      CFANode location =
          AbstractStates.extractStateByType(state, LocationState.class).getLocationNode();
      String transitionInvariantAsC;

      for (PartitionedRelationFormula formula : terminationState.getTransitionInvariants()) {
        PartitionedRelationFormula wrappedFormula;
        wrappedFormula = formula.withPrevVarsWrapped(AT_PREFIX_NON_C, ANYPREV_SUFFIX);
        wrappedFormula = wrappedFormula.withCurrVarsWrapped("", "");
        // Transforming the candidate transition invariant from formula to C expression
        // and wrapping the previous variables into \\at(x, AnyPrev)
        try {
          transitionInvariantAsC =
              TransitionInvariantUtils.transformFormulaToStringWithTrivialReplacement(
                      wrappedFormula.getFormula(), bfmgr, fmgr, scope)
                  .replace(AT_PREFIX_NON_C, AT_PREFIX);
          transitionInvariantAsC =
              TransitionInvariantUtils.removeFunctionFromVarsName(transitionInvariantAsC);
        } catch (CPAException e) {
          transitionInvariantAsC = "true";
        }

        Optional<IterationElement> iterationElement =
            astCfaRelation.getTightestIterationStructureForNode(location);
        if (iterationElement.isPresent()) {
          FileLocation fileLocation =
              iterationElement.orElseThrow().getCompleteElement().location();
          if (transitionInvariants.containsKey(fileLocation)) {
            if (!transitionInvariants
                .get(fileLocation)
                .getValue()
                .contains(transitionInvariantAsC)) {
              transitionInvariantAsC =
                  transitionInvariantAsC
                      + " || "
                      + transitionInvariants.get(fileLocation).getValue();
              transitionInvariants.remove(fileLocation, transitionInvariants.get(fileLocation));
              LocationRecord locationEntry =
                  LocationRecord.createLocationRecordAtStart(
                      iterationElement.orElseThrow().getCompleteElement().location(),
                      location.getFunction().getFileLocation().getFileName().toString(),
                      location.getFunctionName());

              transitionInvariants.put(
                  fileLocation,
                  new InvariantEntry(
                      transitionInvariantAsC,
                      InvariantRecordType.TRANSITION_LOOP_INVARIANT.getKeyword(),
                      YAMLWitnessExpressionType.EXT_C,
                      locationEntry));
            }
          } else {
            LocationRecord locationEntry =
                LocationRecord.createLocationRecordAtStart(
                    iterationElement.orElseThrow().getCompleteElement().location(),
                    location.getFunction().getFileLocation().getFileName().toString(),
                    location.getFunctionName());

            transitionInvariants.put(
                fileLocation,
                new InvariantEntry(
                    transitionInvariantAsC,
                    InvariantRecordType.TRANSITION_LOOP_INVARIANT.getKeyword(),
                    YAMLWitnessExpressionType.EXT_C,
                    locationEntry));
          }
        }
      }
    }
    try {
      terminationWitnessExporter.export(
          transitionInvariants.values().stream().collect(ImmutableList.toImmutableList()));
    } catch (IOException e) {
      logger.logUserException(
          WARNING, e, "There is a problem when writing the witness into a file.");
    }
  }
}
