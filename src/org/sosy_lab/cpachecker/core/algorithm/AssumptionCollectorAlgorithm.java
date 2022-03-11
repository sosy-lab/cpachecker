// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.cpa.arg.ARGUtils.getUncoveredChildrenView;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import org.sosy_lab.common.Appender;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.IntegerOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.DummyScope;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageCPA;
import org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageState;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonParser;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.assumptions.AssumptionWithLocation;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;

/**
 * Outer algorithm to collect all invariants generated during the analysis, and report them to the
 * user
 */
@Options(prefix = "assumptions")
public class AssumptionCollectorAlgorithm implements Algorithm, StatisticsProvider {

  @Option(secure = true, name = "export", description = "write collected assumptions to file")
  private boolean exportAssumptions = true;

  @Option(
      secure = true,
      name = "export.location",
      description = "export assumptions collected per location")
  private boolean exportLocationAssumptions = true;

  @Option(secure = true, name = "file", description = "write collected assumptions to file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path assumptionsFile = Path.of("assumptions.txt");

  @Option(
      secure = true,
      name = "automatonFile",
      description = "write collected assumptions as automaton to file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path assumptionAutomatonFile = Path.of("AssumptionAutomaton.txt");

  @Option(
      secure = true,
      name = "dotExport",
      description = "export assumptions as automaton to dot file")
  private boolean dotExport = false;

  @Option(
      secure = true,
      name = "dotFile",
      description = "write collected assumptions as automaton to dot file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path assumptionAutomatonDotFile = Path.of("AssumptionAutomaton.dot");

  @Option(
      secure = true,
      description = "compress the produced assumption automaton using GZIP compression.")
  private boolean compressAutomaton = false;

  @Option(
      secure = true,
      description =
          "Add a threshold to the automaton, after so many branches on a path the automaton will be"
              + " ignored (0 to disable)")
  @IntegerOption(min = 0)
  private int automatonBranchingThreshold = 0;

  @Option(
      secure = true,
      description =
          "If it is enabled, automaton does not add assumption which is considered to continue path"
              + " with corresponding this edge.")
  private boolean automatonIgnoreAssumptions = false;

  @Option(
      secure = true,
      description =
          "If it is enabled, check if a state that should lead to false state indeed has"
              + " successors.")
  private boolean removeNonExploredWithoutSuccessors = false;

  private final LogManager logger;
  private final Algorithm innerAlgorithm;
  private final FormulaManagerView formulaManager;
  private final AssumptionWithLocation exceptionAssumptions;
  private final BooleanFormulaManager bfmgr;
  private final CFA cfa;
  private final Configuration config;

  // store only the ids, not the states in order to prevent memory leaks
  private final Set<Integer> exceptionStates = new HashSet<>();

  // statistics
  private int automatonStates = 0;

  private final ConfigurableProgramAnalysis cpa;

  private final ShutdownNotifier shutdownNotifier;

  public AssumptionCollectorAlgorithm(
      Algorithm algo,
      ConfigurableProgramAnalysis pCpa,
      Configuration config,
      LogManager logger,
      CFA cfa,
      ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    config.inject(this);

    this.logger = logger;
    innerAlgorithm = algo;
    shutdownNotifier = pShutdownNotifier;
    AssumptionStorageCPA asCpa =
        CPAs.retrieveCPAOrFail(pCpa, AssumptionStorageCPA.class, AssumptionStorageCPA.class);
    if (exportAssumptions && assumptionAutomatonFile != null && !(pCpa instanceof ARGCPA)) {
      throw new InvalidConfigurationException(
          "ARGCPA needed for for export of assumption automaton in AssumptionCollectionAlgorithm");
    }
    formulaManager = asCpa.getFormulaManager();
    bfmgr = formulaManager.getBooleanFormulaManager();
    exceptionAssumptions = new AssumptionWithLocation(formulaManager);
    cpa = pCpa;
    this.cfa = cfa;
    this.config = config;
  }

  @Override
  public AlgorithmStatus run(ReachedSet reached) throws CPAException, InterruptedException {
    AlgorithmStatus status = AlgorithmStatus.SOUND_AND_PRECISE;
    boolean restartCPA;

    // loop if restartCPA is set to false
    do {
      restartCPA = false;
      try {
        // run the inner algorithm to fill the reached set
        status = status.update(innerAlgorithm.run(reached));

      } catch (RefinementFailedException failedRefinement) {
        logger.logUserException(
            Level.INFO, failedRefinement, "Will generate assumption for incomplete analysis");

        ARGPath path = failedRefinement.getErrorPath();
        ARGState errorState = path.getLastState();
        assert errorState == reached.getLastState();

        // old code, perhaps we can use the information from getFailurePoint()
        //        int pos = failedRefinement.getFailurePoint();
        //
        //        if (pos == -1)
        //          pos = path.size() - 2; // the node before the error node
        //
        //        ARGState state = path.get(pos).getFirst();
        //        addAvoidingAssumptions(exceptionAssumptions, state);
        //        exceptionStates.add(state.getStatetId());

        // remove state
        // remove it's parents from waitlist (CPAAlgorithm re-added them)
        // and create assumptions for the parents

        // we have to do this for the parents and not for the errorState itself,
        // because the parents might have other potential successors that were
        // ignored by CPAAlgorithm due to the signaled break

        ARGState parent = Iterables.getOnlyElement(errorState.getParents());
        reached.removeOnlyFromWaitlist(parent);
        exceptionStates.add(parent.getStateId());
        addAvoidingAssumptions(exceptionAssumptions, parent);

        reached.remove(errorState);
        errorState.removeFromARG();

        restartCPA = true;
        status = status.withSound(false);

        // TODO: handle CounterexampleAnalysisFailed similar to RefinementFailedException
        // TODO: handle other kinds of CPAException?

        //      } catch (CPAException e) {
        //        // TODO is it really wise to swallow exceptions here?
        //        logger.log(Level.FINER, "Dumping assumptions due to: " + e.toString());
      }
    } while (restartCPA);

    return status;
  }

  private AssumptionWithLocation collectLocationAssumptions(
      UnmodifiableReachedSet reached, AssumptionWithLocation pExceptionAssumptions) {
    AssumptionWithLocation result = AssumptionWithLocation.copyOf(pExceptionAssumptions);

    // collect and dump all assumptions stored in abstract states
    logger.log(Level.FINER, "Dumping assumptions resulting from tool assumptions");
    for (AbstractState state : reached) {

      if (AbstractStates.isTargetState(state)) {
        // create assumptions for target state
        addAvoidingAssumptions(result, state);

      } else {
        // get stored assumption

        AssumptionStorageState e =
            AbstractStates.extractStateByType(state, AssumptionStorageState.class);
        BooleanFormula assumption = bfmgr.and(e.getAssumption(), e.getStopFormula());

        if (!bfmgr.isTrue(assumption)) {
          addAssumption(result, assumption, state);
        }
      }
    }

    // dump invariants to prevent going further with nodes in the waitlist
    logger.log(Level.FINER, "Dumping assumptions resulting from waitlist states");
    for (AbstractState state : reached.getWaitlist()) {
      addAvoidingAssumptions(result, state);
    }

    return result;
  }

  /** Add a given assumption for the location and state of a state. */
  private void addAssumption(
      AssumptionWithLocation invariant, BooleanFormula assumption, AbstractState state) {
    BooleanFormula dataRegion = AbstractStates.extractReportedFormulas(formulaManager, state);

    CFANode loc = extractLocation(state);
    assert loc != null;
    invariant.add(loc, bfmgr.or(assumption, bfmgr.not(dataRegion)));
  }

  /** Create an assumption that is sufficient to exclude an abstract state */
  private void addAvoidingAssumptions(AssumptionWithLocation invariant, AbstractState state) {
    addAssumption(invariant, bfmgr.makeFalse(), state);
  }

  private void produceAssumptionAutomaton(Appendable output, UnmodifiableReachedSet reached)
      throws IOException {
    final AbstractState firstState = reached.getFirstState();
    if (!(firstState instanceof ARGState)) {
      output.append("Cannot dump assumption as automaton if ARGCPA is not used.");
    }

    Set<AbstractState> falseAssumptionStates = getFalseAssumptionStates(reached);

    // scan reached set for all relevant states with an assumption
    // Invariant: relevantStates does not contain any covered state.
    // A covered state is always replaced by its covering state.
    Set<ARGState> relevantStates = new TreeSet<>();
    for (AbstractState state : reached) {
      ARGState e = (ARGState) state;
      AssumptionStorageState asmptState =
          AbstractStates.extractStateByType(e, AssumptionStorageState.class);

      boolean hasFalseAssumption =
          e.isTarget() || asmptState.isStop() || exceptionStates.contains(e.getStateId());

      boolean isRelevant = !asmptState.isAssumptionTrue();

      if (e.isCovered()) {
        e = e.getCoveringState(); // replace with covering state
        assert !e.isCovered();
        asmptState = null; // just to prevent accidental misuse
      }

      if (hasFalseAssumption) {
        falseAssumptionStates.add(e);
      }

      if (relevantStates.contains(e)) {
        continue;
      }

      if (isRelevant || falseAssumptionStates.contains(e)) {
        // now add e and all its transitive parents to the relevantStates set
        findAllParents(e, relevantStates);
      }
    }

    automatonStates +=
        writeAutomaton(
            output,
            (ARGState) firstState,
            relevantStates,
            falseAssumptionStates,
            automatonBranchingThreshold,
            automatonIgnoreAssumptions);
  }

  private Automaton constructAutomatonFromFile() throws InvalidConfigurationException {

    Scope scope =
        cfa.getLanguage() == Language.C ? new CProgramScope(cfa, logger) : DummyScope.getInstance();

    List<Automaton> lst =
        AutomatonParser.parseAutomatonFile(
            assumptionAutomatonFile,
            config,
            logger,
            cfa.getMachineModel(),
            scope,
            cfa.getLanguage(),
            shutdownNotifier);

    if (lst.isEmpty()) {
      throw new InvalidConfigurationException(
          "Could not find automata in the file " + assumptionAutomatonFile.toAbsolutePath());
    } else if (lst.size() > 1) {
      throw new InvalidConfigurationException(
          "Found "
              + lst.size()
              + " automata in the File "
              + assumptionAutomatonFile.toAbsolutePath()
              + " The CPA can only handle ONE Automaton!");
    }

    return lst.get(0);
  }

  private void writeAutomatonToDot(Automaton automaton) {
    try (Writer w = IO.openOutputFile(assumptionAutomatonDotFile, Charset.defaultCharset())) {
      automaton.writeDotFile(w);
    } catch (IOException e) {
      logger.logUserException(Level.WARNING, e, "Could not write the automaton to DOT file");
    }
  }

  private Set<AbstractState> getFalseAssumptionStates(UnmodifiableReachedSet pReached) {
    Set<AbstractState> falseAssumptionStates;
    if (removeNonExploredWithoutSuccessors) {
      falseAssumptionStates = Sets.newHashSetWithExpectedSize(pReached.getWaitlist().size());
      for (AbstractState state : pReached.getWaitlist()) {
        try {
          if (!cpa.getTransferRelation()
              .getAbstractSuccessors(state, pReached.getPrecision(state))
              .isEmpty()) {
            falseAssumptionStates.add(state);
            if (state instanceof ARGState) {
              ARGState argState = (ARGState) state;
              while (!argState.getChildren().isEmpty()) {
                argState.getChildren().iterator().next().removeFromARG();
              }
            }
          }
        } catch (CPATransferException | UnsupportedOperationException | InterruptedException e) {
          falseAssumptionStates.add(state);
        }
      }
      return falseAssumptionStates;
    } else {
      falseAssumptionStates = new HashSet<>(pReached.getWaitlist());
    }
    return falseAssumptionStates;
  }

  /**
   * Create a String containing the assumption automaton.
   *
   * @param sb Where to write the String into.
   * @param pInitialState The initial state of the automaton.
   * @param relevantStates A set with all states with non-trivial assumptions (all others will have
   *     assumption TRUE).
   * @param falseAssumptionStates A set with all states with the assumption FALSE
   * @param branchingThreshold After branchingThreshold many branches on a path the automaton will
   *     be ignored (0 to disable)")
   * @param ignoreAssumptions if set to true, the automaton does not add assumption which is
   *     considered to continue path with corresponding this edge.
   * @return the number of states contained in the written automaton
   */
  public static int writeAutomaton(
      Appendable sb,
      ARGState pInitialState,
      Set<ARGState> relevantStates,
      Set<AbstractState> falseAssumptionStates,
      int branchingThreshold,
      boolean ignoreAssumptions)
      throws IOException {
    int numProducedStates = 0;
    sb.append("OBSERVER AUTOMATON AssumptionAutomaton\n\n");

    String actionOnFinalEdges = "";
    if (branchingThreshold > 0) {
      sb.append("LOCAL int branchingThreshold = " + branchingThreshold + ";\n");
      sb.append("LOCAL int branchingCount = 0;\n\n");

      // Reset automaton variable on all edges like "GOTO __FALSE"
      // to allow merging of states.
      actionOnFinalEdges = "DO branchingCount = 0 ";
    }

    String initialStateName;
    if (relevantStates.isEmpty()) {
      initialStateName = "__TRUE";
    } else {
      initialStateName = "ARG" + pInitialState.getStateId();
    }

    sb.append("INITIAL STATE ").append(initialStateName).append(";\n\n");
    sb.append("STATE __TRUE :\n");
    sb.append("    TRUE -> GOTO __TRUE;\n\n");

    if (!falseAssumptionStates.isEmpty()) {
      sb.append("STATE __FALSE :\n");
      if (ignoreAssumptions) {
        sb.append("    TRUE -> GOTO __FALSE;\n\n");
      } else {
        sb.append("    TRUE -> ASSUME {false} GOTO __FALSE;\n\n");
      }
    }

    for (final ARGState s : relevantStates) {
      assert !s.isCovered();

      if (falseAssumptionStates.contains(s)) {
        continue;
      }

      sb.append("STATE USEFIRST ARG" + s.getStateId() + " :\n");
      numProducedStates++;

      boolean branching = false;
      if ((branchingThreshold > 0) && (s.getChildren().size() > 1)) {
        branching = true;
        sb.append(
            "    branchingCount == branchingThreshold -> "
                + actionOnFinalEdges
                + "GOTO __FALSE;\n");
      }

      final StringBuilder descriptionForInnerMultiEdges = new StringBuilder();
      int multiEdgeID = 0;

      for (final ARGState child : getUncoveredChildrenView(s)) {
        assert !child.isCovered();

        List<CFAEdge> edges = s.getEdgesToChild(child);

        if (edges.size() > 1) {
          sb.append("    MATCH \"");
          escape(edges.get(0).getRawStatement(), sb);
          sb.append("\" -> ");
          sb.append("GOTO ARG" + s.getStateId() + "M" + multiEdgeID);

          boolean first = true;
          for (CFAEdge innerEdge : from(edges).skip(1)) {

            if (!first) {
              multiEdgeID++;
              descriptionForInnerMultiEdges.append(
                  "GOTO ARG" + s.getStateId() + "M" + multiEdgeID + ";\n");
              descriptionForInnerMultiEdges.append(
                  "    TRUE -> " + actionOnFinalEdges + "GOTO __TRUE;\n\n");
            } else {
              first = false;
            }

            descriptionForInnerMultiEdges.append(
                "STATE USEFIRST ARG" + s.getStateId() + "M" + multiEdgeID + " :\n");
            numProducedStates++;
            descriptionForInnerMultiEdges.append("    MATCH \"");
            escape(innerEdge.getRawStatement(), descriptionForInnerMultiEdges);
            descriptionForInnerMultiEdges.append("\" -> ");
          }

          AssumptionStorageState assumptionChild =
              AbstractStates.extractStateByType(child, AssumptionStorageState.class);
          addAssumption(
              descriptionForInnerMultiEdges,
              assumptionChild,
              ignoreAssumptions,
              AbstractStates.extractLocation(child));
          finishTransition(
              descriptionForInnerMultiEdges,
              child,
              relevantStates,
              falseAssumptionStates,
              actionOnFinalEdges,
              branching);
          descriptionForInnerMultiEdges.append(";\n");
          descriptionForInnerMultiEdges.append(
              "    TRUE -> " + actionOnFinalEdges + "GOTO __TRUE;\n\n");

        } else {

          sb.append("    MATCH \"");
          escape(Iterables.getOnlyElement(edges).getRawStatement(), sb);
          sb.append("\" -> ");

          AssumptionStorageState assumptionChild =
              AbstractStates.extractStateByType(child, AssumptionStorageState.class);
          addAssumption(
              sb, assumptionChild, ignoreAssumptions, AbstractStates.extractLocation(child));
          finishTransition(
              sb, child, relevantStates, falseAssumptionStates, actionOnFinalEdges, branching);
        }

        sb.append(";\n");
      }
      sb.append("    TRUE -> " + actionOnFinalEdges + "GOTO __TRUE;\n\n");
      sb.append(descriptionForInnerMultiEdges);
    }
    sb.append("END AUTOMATON\n");

    return numProducedStates;
  }

  private static void addAssumption(
      final Appendable writer,
      final AssumptionStorageState assumptionState,
      boolean ignoreAssumptions,
      CFANode pCFANode)
      throws IOException {
    if (!ignoreAssumptions) {
      FormulaManagerView fmgr = assumptionState.getFormulaManager();
      final BooleanFormulaManagerView bmgr =
          assumptionState.getFormulaManager().getBooleanFormulaManager();
      BooleanFormula assumption =
          bmgr.and(assumptionState.getAssumption(), assumptionState.getStopFormula());
      if (!bmgr.isTrue(assumption)) {
        try {
          ExpressionTree<Object> assumptionTree =
              ExpressionTrees.fromFormula(assumption, fmgr, pCFANode);
          // At this point, we know that the InterruptedException is not thrown,
          // hence, we can continue
          writer.append("ASSUME {");
          escape(assumptionTree.toString(), writer);
          writer.append("} ");
        } catch (InterruptedException e) {
          // Nothing to do here, as we simply ignore this assumption if it is not parsable
        }
      }
    }
  }

  private static void finishTransition(
      final Appendable writer,
      final ARGState child,
      final Set<ARGState> relevantStates,
      final Set<AbstractState> falseAssumptionStates,
      final String actionOnFinalEdges,
      final boolean branching)
      throws IOException {
    if (falseAssumptionStates.contains(child)) {
      writer.append(actionOnFinalEdges + "GOTO __FALSE");

    } else if (relevantStates.contains(child)) {
      if (branching) {
        writer.append("DO branchingCount = branchingCount+1 ");
      }
      writer.append("GOTO ARG" + child.getStateId());

    } else {
      writer.append(actionOnFinalEdges + "GOTO __TRUE");
    }
  }

  /**
   * This method transitively finds all parents of a given state and adds them to a given set.
   * Covering nodes are considered to be parents of the covered nodes.
   *
   * @param s the ARGSTate whose parents should be found
   * @param parentSet the set of ARGStates the parents should be added to
   */
  private static void findAllParents(ARGState s, Set<ARGState> parentSet) {
    Deque<ARGState> toAdd = new ArrayDeque<>();
    toAdd.add(s);
    while (!toAdd.isEmpty()) {
      ARGState current = toAdd.pop();
      assert !current.isCovered();

      if (parentSet.add(current)) {
        // current was not yet contained in parentSet,
        // so we need to handle its parents

        toAdd.addAll(current.getParents());

        for (ARGState coveredByCurrent : current.getCoveredByThis()) {
          toAdd.addAll(coveredByCurrent.getParents());
        }
      }
    }
  }

  private static void escape(String s, Appendable appendTo) throws IOException {
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      switch (c) {
        case '\r':
          appendTo.append("\\r");
          break;
        case '\n':
          appendTo.append("\\n");
          break;
        case '\"':
          appendTo.append("\\\"");
          break;
        case '\\':
          appendTo.append("\\\\");
          break;
        case '`':
          break;
        default:
          appendTo.append(c);
          break;
      }
    }
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (innerAlgorithm instanceof StatisticsProvider) {
      ((StatisticsProvider) innerAlgorithm).collectStatistics(pStatsCollection);
    }
    pStatsCollection.add(new AssumptionCollectionStatistics());
  }

  private class AssumptionCollectionStatistics implements Statistics {
    @Override
    public String getName() {
      return "Assumption Collection algorithm";
    }

    @Override
    public void printStatistics(PrintStream out, Result pResult, UnmodifiableReachedSet pReached) {
      AssumptionWithLocation resultAssumption = null;

      if (exportLocationAssumptions) {
        resultAssumption = collectLocationAssumptions(pReached, exceptionAssumptions);
        put(out, "Number of locations with assumptions", resultAssumption.getNumberOfLocations());
      }

      if (exportAssumptions) {
        if (exportLocationAssumptions && assumptionsFile != null) {
          try {
            IO.writeFile(assumptionsFile, Charset.defaultCharset(), resultAssumption);
          } catch (IOException e) {
            logger.logUserException(Level.WARNING, e, "Could not write assumptions to file");
          }
        }

        if (assumptionAutomatonFile != null) {

          if (!compressAutomaton) {
            try (Writer w = IO.openOutputFile(assumptionAutomatonFile, Charset.defaultCharset())) {
              produceAssumptionAutomaton(w, pReached);
            } catch (IOException e) {
              logger.logUserException(Level.WARNING, e, "Could not write assumptions to file");
            }
          } else {
            assumptionAutomatonFile =
                assumptionAutomatonFile.resolveSibling(
                    assumptionAutomatonFile.getFileName() + ".gz");
            try {
              IO.writeGZIPFile(
                  assumptionAutomatonFile,
                  Charset.defaultCharset(),
                  (Appender) appendable -> produceAssumptionAutomaton(appendable, pReached));
            } catch (IOException e) {
              logger.logUserException(Level.WARNING, e, "Could not write assumptions to file");
            }
          }

          put(out, "Number of states in automaton", automatonStates);

          // After calling writeAutomaton the assumptionAutomatonTxt now contains the automaton
          // description and the correspond dot file can be created by creating the Automaton object
          if (dotExport) {
            try {
              writeAutomatonToDot(constructAutomatonFromFile());
            } catch (InvalidConfigurationException e) {
              logger.logfUserException(Level.WARNING, e, "Could not write to DOT File");
            }
          }
        }
      }
    }
  }
}
