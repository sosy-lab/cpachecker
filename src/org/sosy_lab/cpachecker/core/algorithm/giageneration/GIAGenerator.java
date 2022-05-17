// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.giageneration;

import com.google.common.base.Throwables;
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

public class GIAGenerator {

  public static final String ASSUMPTION_AUTOMATON_NAME = "AssumptionAutomaton";
  public static final String AUTOMATON_HEADER =
      "OBSERVER AUTOMATON " + ASSUMPTION_AUTOMATON_NAME + "\n\n";

  public static final String DESC_OF_DUMMY_FUNC_START_EDGE = "Function start dummy edge";

  public static String getDefStringForState(
      ARGState pCurrentState,
      Set<ARGState> pTargetStates,
      Set<ARGState> pNonTargetStates,
      Set<ARGState> pUnknownStates) {
    if (pTargetStates.contains(pCurrentState)) {
      return String.format("TARGET STATE USEFIRST %s :\n", GIAGenerator.getName(pCurrentState));
    } else if (pNonTargetStates.contains(pCurrentState)) {
      return String.format("NON_TARGET STATE USEFIRST %s :\n", GIAGenerator.getName(pCurrentState));
    } else if (pUnknownStates.contains(pCurrentState)) {
      return String.format("UNKNOWN STATE USEFIRST %s :\n", GIAGenerator.getName(pCurrentState));
    }
    return String.format("STATE USEFIRST %s :\n", GIAGenerator.getName(pCurrentState));
  }

  @Options
  public static class GIAGeneratorOptions {
    @Option(
        secure = true,
        name = "assumptions.automatonIgnoreAssumptions",
        description =
            "If it is enabled, automaton does not add assumption which is considered to continue"
                + " path with corresponding this edge.")
    boolean automatonIgnoreAssumptions = false;

    @Option(
        secure = true,
        name = "assumptions.genGIA4Witness",
        description = "Generate GIA for violation or correctness witness")
    private  boolean genGIA4Witness = false;

    @Option(
        secure = true,
        name = "assumptions.genGIA4Testcase",
        description = "Generate GIA for testcomp testcase")
    private  boolean genGIA4Testcase = false;

    @Option(
        secure = true,
        name = "assumptions.genGIA4Refinement",
        description = "Generate GIA for refinement (usable in C-CEGAR for craig interpolation)")
    private  boolean genGIA4Refinement = false;

    @Option(
        secure = true,
        name = "assumptions.isOverApproxAnalysis",
        description =
            "Indicates whether this analysis is over-approximate (and hence can extend F_NT)")
    private  boolean isOverApproxAnalysis = false;

    @Option(
        secure = true,
        name = "assumptions.genGIA4Refinement",
        description =
            "Indicates whether this analysis is under-approximate (and hence can extend F_T)")
    private  boolean isUnderApproxAnalysis = false;

    @Option(
        secure = true,
        name = "assumptions.storeInterpolantsInGIA",
        description = "Store the discovered interpolants in the gia")
    private  boolean storeInterpolantsInGIA = false;

    public GIAGeneratorOptions(Configuration pConfig) throws InvalidConfigurationException {
      pConfig.inject(this);
    }

    public boolean isAutomatonIgnoreAssumptions() {
      return automatonIgnoreAssumptions;
    }

    public boolean isGenGIA4Witness() {
      return genGIA4Witness;
    }

    public boolean isGenGIA4Testcase() {
      return genGIA4Testcase;
    }

    public boolean isGenGIA4Refinement() {
      return genGIA4Refinement;
    }

    public boolean isOverApproxAnalysis() {
      return isOverApproxAnalysis;
    }

    public boolean isUnderApproxAnalysis() {
      return isUnderApproxAnalysis;
    }

    public boolean isStoreInterpolantsInGIA() {
      return storeInterpolantsInGIA;
    }
  }

  public static final String NAME_OF_WITNESS_AUTOMATON = "WitnessAutomaton";
  public static final String NAME_OF_TEMP_STATE = "__qTEMP";
  public static final String NAME_OF_ERROR_STATE = "__qERROR";
  public static final String NAME_OF_UNKNOWN_STATE = "__qUNKNOWN";
  public static final String NAME_OF_FINAL_STATE = "__qFINAL";

  public static final String NAME_OF_NEWTESTINPUT_STATE = "__qNEWTEST";

  final GIAGeneratorOptions optinons;

  @SuppressWarnings("unused")
  private final ShutdownNotifier shutdownNotifier;

  @SuppressWarnings("unused")
  private final GIATestcaseGenerator testcaseGenerator;

  @SuppressWarnings("unused")
  private final GIAInterpolantGenerator interpolantGenerator;

  @SuppressWarnings("unused")
  private final GIAWitnessGenerator witnessGenerator;

  private final GIAARGGenerator argGeneator;
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

  public GIAGenerator(
      Algorithm algo,
      ConfigurableProgramAnalysis pCpa,
      Configuration pConfig,
      LogManager pLogger,
      CFA pCfa,
      ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    this.optinons = new GIAGeneratorOptions(pConfig);
    this.logger = pLogger;
    this.innerAlgorithm = algo;
    shutdownNotifier = pShutdownNotifier;
    AssumptionStorageCPA asCpa =
        CPAs.retrieveCPAOrFail(pCpa, AssumptionStorageCPA.class, AssumptionStorageCPA.class);

    this.formulaManager = asCpa.getFormulaManager();

    this.cpa = pCpa;
    this.cfa = pCfa;
    this.config = pConfig;

    this.testcaseGenerator = new GIATestcaseGenerator(cpa);
    this.interpolantGenerator = new GIAInterpolantGenerator(cpa, formulaManager);

    this.witnessGenerator =
        new GIAWitnessGenerator(
            new GIACorWitGenerator(logger, optinons), new GIAVioWitGenerator(logger, optinons));
    this.argGeneator =
        new GIAARGGenerator(logger, optinons, cfa.getMachineModel(), cpa, config, formulaManager);
  }

  public int produceGeneralizedInformationExchangeAutomaton(
      Appendable output, UnmodifiableReachedSet reached) throws CPAException, IOException {

    try {
      universalConditionAutomaton += argGeneator.produceGIA4ARG(output, reached, optinons);
    } catch (InterruptedException pE) {
      logger.log(
          Level.WARNING,
          String.format(
              "Generation of GIA failed due to %s", Throwables.getStackTraceAsString(pE)));
    }

    //     if (optinons.isGenGIA4Testcase()) {
    //      universalConditionAutomaton +=
    //          testcaseGenerator.produceGIA4Testcase(output, reached, pExceptionStates);
    //    } else if (optinons.isGenGIA4Refinement()) {
    //       //FIXME: Unify with case below!
    //      universalConditionAutomaton += interpolantGenerator.produceGIA4Interpolant(output,
    // reached);
    //    } else {
    //      universalConditionAutomaton += witnessGenerator.produceGIA4Witness(output, reached,
    // optinons);
    //    }
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

  static String getNameOrError(AutomatonState s) {
    return s.isTarget() ? NAME_OF_ERROR_STATE : s.getInternalStateName();
  }

  static String getName(ARGState pSource) {
    return String.format("ARG%d", +pSource.getStateId());
  }

  static String getNameOrError(ARGState pSource) {
    return pSource.isTarget() ? NAME_OF_ERROR_STATE : getName(pSource);
  }

  static String getEdgeString(CFAEdge pEdge) {
    if (pEdge instanceof BlankEdge
        && pEdge.getDescription().equals(DESC_OF_DUMMY_FUNC_START_EDGE)) {
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
