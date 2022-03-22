// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.ucageneration;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageCPA;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;

public class UCAGenerator {

  @Options
  public static class UCAGeneratorOptions {
    @Option(
        secure = true,
        name = "assumptions.automatonIgnoreAssumptions",
        description =
            "If it is enabled, automaton does not add assumption which is considered to continue path with corresponding this edge.")
    boolean automatonIgnoreAssumptions = false;

    @Option(
        secure = true,
        name = "assumptions.genUCA4Witness",
        description = "Generate uca for violation or correctness witness")
    private boolean genUCA4Witness = false;

    @Option(
        secure = true,
        name = "assumptions.genUCA4Testcase",
        description = "Generate uca for testcomp testcase")
    private boolean genUCA4Testcase = false;

    @Option(
        secure = true,
        name = "assumptions.genUCA4Refinement",
        description = "Generate uca for refinement (usable in C-CEGAR for craig interpolation)")
    private boolean genUCA4Refinement = false;

    public UCAGeneratorOptions(Configuration pConfig) throws InvalidConfigurationException {
      pConfig.inject(this);
    }

    public boolean isAutomatonIgnoreAssumptions() {
      return automatonIgnoreAssumptions;
    }

    public boolean isGenUCA4Witness() {
      return genUCA4Witness;
    }

    public boolean isGenUCA4Testcase() {
      return genUCA4Testcase;
    }

    public boolean isGenUCA4Refinement() {
      return genUCA4Refinement;
    }
  }

  public static final String NAME_OF_WITNESS_AUTOMATON = "WitnessAutomaton";
  public static final String NAME_OF_TEMP_STATE = "__qTEMP";
  public static final String NAME_OF_ERROR_STATE = "__qERROR";

  @SuppressWarnings("unused")
  public static final String NAME_OF_FINAL_STATE = "__qFINAL";

  public static final String NAME_OF_NEWTESTINPUT_STATE = "__qNEWTEST";

  final UCAGeneratorOptions optinons;

  @SuppressWarnings("unused")
  private final ShutdownNotifier shutdownNotifier;

  private final UCATestcaseGenerator testcaseGenerator;
  private final UCAInterpolantGenerator interpolantGenerator;
  private final UCAWitnessGenerator witnessGenerator;

  final LogManager logger;

  @SuppressWarnings("unused")
  private final Algorithm innerAlgorithm;

  @SuppressWarnings("unused")
  final FormulaManagerView formulaManager;

  @SuppressWarnings("unused")
  private final CFA cfa;

  final ConfigurableProgramAnalysis cpa;

  @SuppressWarnings("unused")
  private final Configuration config;

  int universalConditionAutomaton = 0;

  public UCAGenerator(
      Algorithm algo,
      ConfigurableProgramAnalysis pCpa,
      Configuration pConfig,
      LogManager pLogger,
      CFA pCfa,
      ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    this.optinons = new UCAGeneratorOptions(pConfig);
    this.logger = pLogger;
    this.innerAlgorithm = algo;
    shutdownNotifier = pShutdownNotifier;
    AssumptionStorageCPA asCpa =
        CPAs.retrieveCPAOrFail(pCpa, AssumptionStorageCPA.class, AssumptionStorageCPA.class);

    this.formulaManager = asCpa.getFormulaManager();

    this.cpa = pCpa;
    this.cfa = pCfa;
    this.config = pConfig;

    this.testcaseGenerator = new UCATestcaseGenerator(cpa);
    this.interpolantGenerator = new UCAInterpolantGenerator(cpa, formulaManager);

    this.witnessGenerator =
        new UCAWitnessGenerator(
            new UCACorWitGenerator(logger, optinons),
            new UCAVioWitGenerator(logger, optinons, cpa));
  }

  public int produceUniversalConditionAutomaton(
      Appendable output, UnmodifiableReachedSet reached, Set<Integer> pExceptionStates)
      throws CPAException, IOException {

    if (optinons.isGenUCA4Witness()) {
      universalConditionAutomaton += witnessGenerator.produceUCA4Witness(output, reached);
    } else if (optinons.isGenUCA4Testcase()) {
      universalConditionAutomaton +=
          testcaseGenerator.produceUCA4Testcase(output, reached, pExceptionStates);
    } else if (optinons.isGenUCA4Refinement()) {
      universalConditionAutomaton += interpolantGenerator.produceUCA4Interpolant(output, reached);
    } else {
      logger.log(Level.INFO, "Do not generate any UCA!");
    }
    return this.universalConditionAutomaton;
  }

  /**
   * Checks, if the Node pS in the path has no successors in pRelevantStates, and hence is an
   * assumption state without successors.
   *
   * @param pS the state to check
   * @param pRelevantStates the other states in the graph
   * @return true, if pS has no child in pRelevantStates.
   */
  static boolean hasNoSuccessor(ARGState pS, Set<ARGState> pRelevantStates) {
    return pS.getChildren().stream().noneMatch(child -> pRelevantStates.contains(child));
  }

  static Optional<AutomatonState> getWitnessAutomatonState(AbstractState s) {
    Optional<AbstractState> target =
        AbstractStates.asIterable(s)
            .filter(
                cs -> {
                  if (cs instanceof AutomatonState) {
                    return ((AutomatonState) cs)
                        .getOwningAutomatonName()
                        .equals(NAME_OF_WITNESS_AUTOMATON);
                  }
                  return false;
                })
            .stream()
            .findFirst();
    if (target.isPresent() && target.orElseThrow() instanceof AutomatonState) {
      return Optional.of((AutomatonState) target.orElseThrow());
    }
    return Optional.empty();
  }

  static CFAEdge getEdge(Pair<ARGState, AutomatonState> parentPair, ARGState argState) {
    return ARGUtils.getOnePathFromTo(
            (x) ->
                Objects.nonNull(x)
                    && Objects.nonNull(parentPair.getFirst())
                    && x.equals(parentPair.getFirst()),
            argState)
        .getFullPath()
        .get(0);
  }

  static void storeInitialNode(Appendable sb, boolean pEmpty, String pName) throws IOException {
    String initialStateName;
    if (pEmpty) {
      initialStateName = "__TRUE";
    } else {
      initialStateName = pName;
    }

    sb.append("INITIAL STATE ").append(initialStateName).append(";\n\n");
    sb.append("STATE __TRUE :\n");
    sb.append("    TRUE -> GOTO __TRUE;\n\n");

    sb.append(String.format("STATE %s :\n", NAME_OF_TEMP_STATE));
  }

  static String getName(AutomatonState s) {
    return s.isTarget() ? NAME_OF_ERROR_STATE : s.getInternalStateName();
  }

  static String getName(ARGState pSource) {
    return pSource.isTarget() ? NAME_OF_ERROR_STATE :  String.format("ARG%d", +pSource.getStateId());
  }

  static String getEdgeString(CFAEdge pEdge) {
    if (pEdge instanceof BlankEdge && pEdge.getDescription().equals("Function start dummy edge")) {
      final String funcName = pEdge.getSuccessor().getFunction().toString();
      int indexSemicolon = funcName.indexOf(";");
      if (indexSemicolon > 0) {
        return funcName.substring(0, indexSemicolon);
      }
      return funcName;
    }
    return pEdge.getRawStatement();
  }
}
