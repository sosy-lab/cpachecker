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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageCPA;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.cpa.giacombiner.GIACombinerState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;

public class GIAGenerator {

  public static final String ASSUMPTION_AUTOMATON_NAME = "AssumptionAutomaton";
  public static final String AUTOMATON_HEADER =
      "OBSERVER AUTOMATON " + ASSUMPTION_AUTOMATON_NAME + "\n\n";

  public static final String DESC_OF_DUMMY_FUNC_START_EDGE = "Function start dummy edge";
  private final GIACombinerGenerator combinerGenerator;

  public static String getDefStringForState(
      AbstractState pCurrentState,
      Set<? extends AbstractState> pTargetStates,
      Set<? extends AbstractState> pNonTargetStates,
      Set<? extends AbstractState> pUnknownStates) {
    String prefix = "";
    if (pTargetStates.contains(pCurrentState)) {
      prefix = "TARGET ";
    } else if (pNonTargetStates.contains(pCurrentState)) {
      prefix = "NON_TARGET ";
    } else if (pUnknownStates.contains(pCurrentState)) {
      prefix = "UNKNOWN ";
    }
    return String.format("%SSTATE USEALL %s :\n", prefix, GIAGenerator.getName(pCurrentState));
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
    private boolean genGIA4Witness = false;

    @Option(
        secure = true,
        name = "assumptions.genGIA4Testcase",
        description = "Generate GIA for testcomp testcase")
    private boolean genGIA4Testcase = false;

    @Option(
        secure = true,
        name = "assumptions.genGIA4Refinement",
        description = "Generate GIA for refinement (usable in C-CEGAR for craig interpolation)")
    private boolean genGIA4Refinement = false;

    @Option(
        secure = true,
        name = "assumptions.isOverApproxAnalysis",
        description =
            "Indicates whether this analysis is over-approximate (and hence can extend F_NT)")
    private boolean isOverApproxAnalysis = false;

    @Option(
        secure = true,
        name = "assumptions.isUnderApproxAnalysis",
        description =
            "Indicates whether this analysis is under-approximate (and hence can extend F_T)")
    private boolean isUnderApproxAnalysis = false;

    @Option(
        secure = true,
        name = "assumptions.storeInterpolantsInGIA",
        description = "Store the discovered interpolants in the gia")
    private boolean storeInterpolantsInGIA = false;

    @Option(
        secure = true,
        name = "assumptions.combineGIAs",
        description = "Merge two GIAs, ignore all other options")
    private boolean combine = false;

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

    public boolean isCombine() {
      return combine;
    }
  }

  public static final String NAME_OF_WITNESS_AUTOMATON = "WitnessAutomaton";
  public static final String NAME_OF_TEMP_STATE = "__qTEMP";
  public static final String NAME_OF_ERROR_STATE = "__qERROR";
  public static final String NAME_OF_UNKNOWN_STATE = "__qUNKNOWN";
  public static final String NAME_OF_FINAL_STATE = "__qFINAL";

  public static final String NAME_OF_NEWTESTINPUT_STATE = "__qNEWTEST";

  final GIAGeneratorOptions options;

  @SuppressWarnings("unused")
  private final ShutdownNotifier shutdownNotifier;

  @SuppressWarnings("unused")
  private final GIATestcaseGenerator testcaseGenerator;

  //  @SuppressWarnings("unused")
  //  private final GIAInterpolantGenerator interpolantGenerator;

  //  @SuppressWarnings("unused")
  //  private final GIAWitnessGenerator witnessGenerator;

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
    this.options = new GIAGeneratorOptions(pConfig);
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
    //    this.interpolantGenerator = new GIAInterpolantGenerator(cpa, formulaManager);

    //    this.witnessGenerator =
    //        new GIAWitnessGenerator(
    //            new GIACorWitGenerator(logger, options), new GIAVioWitGenerator(logger, options));
    this.argGeneator =
        new GIAARGGenerator(logger, options, cfa.getMachineModel(), cpa, config, formulaManager);
    this.combinerGenerator = new GIACombinerGenerator(cpa);
  }

  public int produceGeneralizedInformationExchangeAutomaton(
      Appendable output, UnmodifiableReachedSet reached) throws IOException {

    try {
      if (options.isCombine()) {
        universalConditionAutomaton += combinerGenerator.produceGIA4ARG(output, reached);
      } else {
        universalConditionAutomaton += argGeneator.produceGIA4ARG(output, reached);
      }
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

  static String getName(AbstractState pSource) {
    if (pSource instanceof ARGState)
      return String.format("ARG%d", +((ARGState) pSource).getStateId());
    else if (pSource instanceof GIACombinerState) {
      return ((GIACombinerState) pSource).toDOTLabel();
    }
    return "STATE_" + pSource.hashCode();
  }

  static String getEdgeString(CFAEdge pEdge) {
    if (pEdge == null) return String.format("\"%s\"", "");
    if (pEdge instanceof BlankEdge
        && pEdge.getDescription().equals(DESC_OF_DUMMY_FUNC_START_EDGE)) {
      if (AutomatonGraphmlCommon.isMainFunctionEntry(pEdge)) {
        final String funcName = pEdge.getSuccessor().getFunction().getOrigName();
        return String.format("FUNCTIONCALL \"%s\"", funcName);
      } else {
        // FIXME: Find a workaround for this
      }
    } else if (pEdge instanceof FunctionCallEdge) {
      final String funcName = pEdge.getSuccessor().getFunction().getOrigName();
      return String.format("FUNCTIONCALL \"%s\"", funcName);
    }

    return String.format("\"%s\"", pEdge.getRawStatement());
  }

  //  /**
  //   * Create an GIA for the given set of edges Beneth printing the edges, each node gets a
  // self-loop
  //   * and a node to the temp-location
  //   *
  //   * @param sb the appendable to print to
  //   * @param rootState the root state of the automaton
  //   * @param edgesToAdd the edges between states to add
  //   * @param pTargetStates the target states
  //   * @param pNonTargetStates the non target states
  //   * @param pUnknownStates the unknwon states
  //   * @throws IOException if the file cannot be accessed or does not exist
  //   */
  //  private static int writeGIA(
  //      Appendable sb,
  //      ARGState rootState,
  //      Set<GIAARGStateEdge<ARGState>> edgesToAdd,
  //      boolean ignoreAssumptions,
  //      Set<ARGState> pTargetStates,
  //      Set<ARGState> pNonTargetStates,
  //      Set<ARGState> pUnknownStates)
  //      throws IOException, InterruptedException {
  //    // TODO Refactor this method and the copied to one
  //    int numProducedStates = 0;
  //    sb.append(GIAGenerator.AUTOMATON_HEADER);
  //
  //    String actionOnFinalEdges = "";
  //
  //    GIAGenerator.storeInitialNode(sb, edgesToAdd.isEmpty(),
  // GIAGenerator.getNameOrError(rootState));
  //    if (ignoreAssumptions) {
  //      sb.append(String.format("    TRUE -> GOTO %s;\n\n", GIAGenerator.NAME_OF_TEMP_STATE));
  //    } else {
  //      sb.append(
  //          String.format("    TRUE -> ASSUME {true} GOTO %s;\n\n",
  // GIAGenerator.NAME_OF_TEMP_STATE));
  //    }
  //
  //    if (setIsReached(pTargetStates, edgesToAdd)) {
  //      sb.append(String.format("TARGET STATE %s :\n", GIAGenerator.NAME_OF_ERROR_STATE));
  //      if (ignoreAssumptions) {
  //        sb.append(String.format("    TRUE -> GOTO %s;\n\n", GIAGenerator.NAME_OF_ERROR_STATE));
  //      } else {
  //        sb.append(
  //            String.format(
  //                "    TRUE -> ASSUME {true} GOTO %s;\n\n", GIAGenerator.NAME_OF_ERROR_STATE));
  //      }
  //    }
  //
  //    if (setIsReached(pNonTargetStates, edgesToAdd)) {
  //      sb.append(String.format("NON_TARGET STATE %s :\n", GIAGenerator.NAME_OF_FINAL_STATE));
  //      if (ignoreAssumptions) {
  //        sb.append(String.format("    TRUE -> GOTO %s;\n\n", GIAGenerator.NAME_OF_FINAL_STATE));
  //      } else {
  //        sb.append(
  //            String.format(
  //                "    TRUE -> ASSUME {true} GOTO %s;\n\n", GIAGenerator.NAME_OF_FINAL_STATE));
  //      }
  //    }
  //
  //    if (setIsReached(pUnknownStates, edgesToAdd)) {
  //      sb.append(String.format("UNKNOWN STATE %s :\n", GIAGenerator.NAME_OF_UNKNOWN_STATE));
  //      if (ignoreAssumptions) {
  //        sb.append(String.format("    TRUE -> GOTO %s;\n\n",
  // GIAGenerator.NAME_OF_UNKNOWN_STATE));
  //      } else {
  //        sb.append(
  //            String.format(
  //                "    TRUE -> ASSUME {true} GOTO %s;\n\n", GIAGenerator.NAME_OF_UNKNOWN_STATE));
  //      }
  //    }
  //
  //    // Fill the map to be able to iterate over the nodes
  //    Map<ARGState, Set<GIAARGStateEdge<ARGState>>>
  //        nodesToEdges = groupEdgesByNodes(edgesToAdd);
  //
  //    for (final ARGState currentState :
  //        nodesToEdges.keySet().stream()
  //            .sorted(Comparator.comparing(GIAGenerator::getNameOrError))
  //            .collect(ImmutableList.toImmutableList())) {
  //
  //      sb.append(
  //          GIAGenerator.getDefStringForState(
  //              currentState, pTargetStates, pNonTargetStates, pUnknownStates));
  //      numProducedStates++;
  //
  //      // Only add a node if it is neither in F_N nor F_NT
  //      if (!pTargetStates.contains(currentState) && !pNonTargetStates.contains(currentState)) {
  //
  //        for (GIAARGStateEdge<ARGState> edge : nodesToEdges.get(currentState)) {
  //
  //          sb.append("    MATCH \"");
  //          AssumptionCollectorAlgorithm.escape(GIAGenerator.getEdgeString(edge.getEdge()), sb);
  //          sb.append("\" -> ");
  //          sb.append(edge.getStringOfAssumption(edge.getTarget()));
  //          sb.append(
  //              String.format(
  //                  "GOTO %s", edge.getTargetName(pTargetStates, pNonTargetStates,
  // pUnknownStates)));
  //          sb.append(";\n");
  //        }
  //      }
  //      //     if (pTargetStates.contains(currentState) || pNonTargetStates.contains(currentState)
  //      //      || pUnknownStates.contains(currentState)){
  //      sb.append(
  //          String.format(
  //              "    MATCH OTHERWISE -> " + actionOnFinalEdges + "GOTO %s;\n",
  //              GIAGenerator.getNameOrError(currentState)));
  //      //        sb.append("    TRUE -> " + actionOnFinalEdges + "GOTO __TRUE;\n\n");
  //      //      }
  //      sb.append("\n");
  //    }
  //    sb.append("END AUTOMATON\n");
  //
  //    return numProducedStates;
  //  }

  @Nonnull
  public static Map<ARGState, Set<GIAARGStateEdge<ARGState>>> groupEdgesByNodes(
      Set<GIAARGStateEdge<ARGState>> edgesToAdd) {
    Map<ARGState, Set<GIAARGStateEdge<ARGState>>> nodesToEdges = new HashMap<>();
    edgesToAdd.forEach(
        e -> {
          if (nodesToEdges.containsKey(e.getSource())) {
            nodesToEdges.get(e.getSource()).add(e);
          } else {
            HashSet<GIAARGStateEdge<ARGState>> set = new HashSet<>();
            set.add(e);
            nodesToEdges.put(e.getSource(), set);
          }
        });
    return nodesToEdges;
  }

  //  private static boolean setIsReached(
  //      Set<ARGState> pSet, Set<GIAARGStateEdge<ARGState>> pEdgesToAdd) {
  //    return pEdgesToAdd.stream()
  //        .anyMatch(e -> e.getTarget().isPresent() && pSet.contains(e.getTarget().orElseThrow()));
  //  }
}
