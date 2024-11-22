// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.concolic;

import static org.sosy_lab.cpachecker.core.algorithm.concolic.ConcolicAlgorithmIsInitialized.setIsInitialized;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.AbstractCFAEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.concolic.ConcolicAlgorithmIsInitialized.AlgorithmType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.PartitionedReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist.TraversalMethod;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.constraints.ConstraintsCPA;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsSolver;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsSolver.SolverResult;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState;
import org.sosy_lab.cpachecker.cpa.value.NondeterministicValueProvider;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueFactory;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.testcase.TestCaseExporter;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;

@SuppressWarnings("Duplicates")
@Options(prefix = "concolic")
public class ConcolicAlgorithmRandom implements Algorithm {

  @Option(
      secure = true,
      name = "coverageCriterion",
      description = "type of coverage criterion to use, branch or error")
  private String coverageCriterion = "changed";

  private CoverageCriterion coverageCriterionEnum;

  //  private ConcolicAlgorithm.CoverageCriterion coverageCriterionEnum;

  //  private static boolean isInitialized = false;
  private final Algorithm algorithm;
  private final ConfigurableProgramAnalysis cpa;
  private final Configuration config;
  private final LogManager logger;
  private final ConstraintsSolver constraintsSolver;
  //  public static MachineModel machineModel;
  private final CFA cfa;
  // visited blocks
  //   AssumeEdges for branch coverage
  //   blocks after the AssumeEdge for block coverage
  private final Set<AbstractCFAEdge> visitedBlocks;
  private final Set<List<Constraint>> checkedConstraints;
  private final Set<List<CFAEdge>> visitedPaths;
  private int testsWritten = 0;
  private final int maxTestCases = 99000;

  // TODO remove public -> Singleton in NondeterministicValueProvider?
  public static final NondeterministicValueProvider nonDetValueProvider =
      new NondeterministicValueProvider();
  public static int tmpcounter = 0;

  private final ConstraintsCPA constraintsCPA;
  TestCaseExporter exporter;
  //  CPAAlgorithm concreteAlgorithmCE; // for the concrete execution
  //  private final ConfigurableProgramAnalysis argCpaCE;
  private Random rnd;
  private SearchAlgorithm searchAlgorithm;

  // deterministic comparator
  //  public class DeterministicConcolicInputComparator implements Comparator<ConcolicInput> {
  //    @Override
  //    public int compare(ConcolicInput ci0, ConcolicInput ci1) {
  //      int result = ci0.score - ci1.score;
  //      if (result == 0) return ci0.hashCode() - ci1.hashCode();
  //      return result;
  //    }
  //  }

  public ConcolicAlgorithmRandom(
      Algorithm pAlgorithm,
      ConfigurableProgramAnalysis pCpa,
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      CFA pCfa,
      //      String concolicCoverageCriterion,
      String pSearchAlgorithm)
      throws Exception {
    pConfig.inject(this, ConcolicAlgorithmRandom.class);
    if (pSearchAlgorithm.equals("random")) {
      this.searchAlgorithm = SearchAlgorithm.RANDOM;
    } else if (pSearchAlgorithm.equals("DFS")) {
      this.searchAlgorithm = SearchAlgorithm.DFS;
    } else {
      throw new Error("SearchAlgorithm not known");
    }
    //    this.searchAlgorithm = pSearchAlgorithm;
    //    isInitialized = true;
    setIsInitialized(AlgorithmType.RANDOM_OR_DFS);
    this.visitedBlocks = new HashSet<>();
    this.checkedConstraints = new HashSet<>();
    this.visitedPaths = new HashSet<>();
    if (Objects.equals(coverageCriterion, "branch")) {
      this.coverageCriterionEnum = CoverageCriterion.BRANCH;
    } else if (Objects.equals(coverageCriterion, "error")) {
      this.coverageCriterionEnum = CoverageCriterion.ERROR;
    } else {
      throw new Error("coverageCriterion unknown");
    }

    this.algorithm = pAlgorithm;
    this.cpa = pCpa;
    this.config = pConfig;
    this.logger = pLogger;
    this.cfa = pCfa;
    //    machineModel = cfa.getMachineModel();
    this.constraintsCPA =
        CPAs.retrieveCPAOrFail(cpa, ConstraintsCPA.class, ConcolicAlgorithmRandom.class);
    this.constraintsSolver = constraintsCPA.getSolver();
    this.exporter = new TestCaseExporter(cfa, logger, config);
    this.rnd = new Random(1636672210L);
  }

  public record ConcolicInput(
      Map<NondetLocation, List<Value>> values,
      Optional<List<ValueAssignment>> model,
      Optional<ReachedSet> reachedSet,
      Optional<ARGPath> argPath,
      Optional<ConstraintsState> csState,
      int bound,
      int score,
      boolean isNewPath,
      boolean isInitial) {}

  enum CoverageCriterion {
    BRANCH,
    ERROR,
  }

  enum SearchAlgorithm {
    RANDOM,
    DFS,
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {
    List<ConcolicInput> workList = new ArrayList<>();
    workList.add(
        new ConcolicInput(
            new HashMap<>(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            0,
            0,
            true,
            true));

    while (!workList.isEmpty()) {
      // random testing: workList should only contain 1 element
      if (workList.size() > 1) {
        throw new Error("workList.size() > 1");
      }

      if (coverageCriterionEnum == CoverageCriterion.ERROR && !visitedBlocks.isEmpty()) {
        // for error coverage, stop when one error location is found
        reachedSet.clearWaitlist();
        return AlgorithmStatus.SOUND_AND_PRECISE;
      }

      // Get the element with the highest score and remove it
      ConcolicInput highestScoreSeed =
          workList.stream().max(Comparator.comparing(ci -> ci.score())).get();
      // check the path again
      //      if (highestScoreSeed.argPath().isPresent()
      //          && visitedPaths.contains(highestScoreSeed.argPath().orElseThrow().getFullPath()))
      // {
      //        workList.remove(highestScoreSeed);
      //        continue;
      //      }
      workList.remove(highestScoreSeed);
      // run concolic execution
      List<ConcolicInput> childInputs = expandExecution(highestScoreSeed, reachedSet);
      // own optimization: filter out inputs with score 0
      // if (!Objects.equals(coverageCriterion, "error"))
      //            childInputs.removeIf(ci -> ci.score == 0);
      //      childInputs.removeIf(ci -> !ci.isNewPath);
      //      List<ConcolicInput> tmp =
      //          childInputs.stream().filter(ci -> !ci.isNewPath && ci.score() == 0).toList();
      workList.addAll(childInputs);
      // TODO
      //      if (visitedPaths.size() > 1000) {
      //        // if (visitedPaths.size() > 100) -> remove inputs that will probably not lead to a
      // new path
      //        // to prevent endless loop
      //
      //    logger.log(Level.FINE,"ConcolicAlgorithm: visitedPaths.size() > 1000");
      //        workList.removeIf(ci -> ci.score == 0);
      //      }
    }
    // clear Waitlist for nice end result in the console
    reachedSet.clearWaitlist();
    return AlgorithmStatus.SOUND_AND_PRECISE;
  }

  private void fillVisitedBlocksBlockCoverage(ARGPath pARGPath) {
    List<CFAEdge> edges = pARGPath.getInnerEdges();
    try {
      for (int i = 0; i < edges.size() - 1; i++) {
        CFAEdge ce = edges.get(i);
        CFAEdge ceChild = edges.get(i + 1);
        if (ce instanceof AssumeEdge && !(ceChild instanceof AssumeEdge)) {
          // TODO letzte Assume Edge wird vielleicht ignoriert, auch bei calculateScore -> prüfen
          // if so, check if visitedBlocks contains the edge -> if not, add 1 to the score
          visitedBlocks.add((AssumeEdge) ce);
        }
      }
    } catch (NoSuchElementException e) {
      throw new Error(e);
    }
  }

  private void fillVisitedBlocksErrorCoverage(ARGPath pARGPath) {
    List<CFAEdge> edges = pARGPath.getInnerEdges();
    try {
      for (CFAEdge ce : edges) {
        if (ce instanceof FunctionCallEdge && ce.getRawStatement().equals("reach_error();")) {
          visitedBlocks.add((FunctionCallEdge) ce);
        }
      }
    } catch (NoSuchElementException e) {
      throw new Error(e);
    }
  }

  private void fillVisitedBlocksBranchCoverage(ARGPath pARGPath) {
    List<CFAEdge> edges = pARGPath.getInnerEdges();
    try {
      for (CFAEdge ce : edges) {
        if (ce instanceof AssumeEdge) {
          visitedBlocks.add((AssumeEdge) ce);
        }
      }
    } catch (NoSuchElementException e) {
      throw new Error(e);
    }
  }

  private void writeTestCaseFromValues(List<Object> model) {
    testsWritten++;
    if (testsWritten > maxTestCases) {
      throw new Error(String.format("Maximum number of test cases (%d) reached", maxTestCases));
    }
    try {
      List<String> ls = new ArrayList<>();
      for (Object v : model) {
        ls.add(String.valueOf(v));
      }
      exporter.writeTestCaseFiles(ls, Optional.empty());
    } catch (Exception e) {
      throw new Error(e);
    }
  }

  /*
   * ! uses  nonDetValueProvider.getReturnedValueHistory() ! */
  private ConcolicInput getConcolicInputAndWriteTestCase(
      ConstraintsState state,
      ARGPath argPath,
      ReachedSet reachedSet,
      int bound,
      Map<NondetLocation, List<Value>> values,
      int score,
      boolean isInitial,
      Optional<List<ValueAssignment>> optModel) {
    try {
      List<ValueAssignment> model;
      // TODO get function of all constraints not possible? -> calling function gemeint?
      if (optModel.isEmpty()) {
        SolverResult result =
            constraintsSolver.checkUnsat(state, "foofunction"); // TODO function name
        model = result.model().orElseThrow();
      } else {
        model = optModel.orElseThrow();
      }
      List<Object> valueHistory = nonDetValueProvider.getReturnedValueHistory();
//      if (coverageCriterionEnum == CoverageCriterion.ERROR) {
//        // for error, just write the test case that led to the error
//        if (score > 0 || isInitial) {
//          writeTestCaseFromValues(valueHistory);
//        }
//      } else {
        writeTestCaseFromValues(valueHistory);
//      }
      return new ConcolicInput(
          getValues(values, model, argPath),
          Optional.of(model),
          Optional.of(reachedSet),
          Optional.of(argPath),
          Optional.of(state),
          bound,
          score,
          !visitedPaths.contains(argPath.getFullPath()),
          isInitial);
    } catch (Exception e) {
      throw new Error(e);
    }
  }

  private List<ConcolicInput> expandExecution(
      ConcolicInput pConcolicInput, ReachedSet initialReachedSet)
      throws CPAException, InterruptedException {
    ConcolicInput concolicInput = pConcolicInput;
    List<ConcolicInput> childInputs = new ArrayList<>();
    ConstraintsState state;
    ARGPath argPath;
    ReachedSet reachedSetFirstState;
    if (concolicInput.csState.isPresent()
        // is the last edge (unflipped) has not been checked
        && checkedConstraints.contains(concolicInput.csState.orElseThrow().constraints)) {
      state = concolicInput.csState.orElseThrow();
      argPath = concolicInput.argPath.orElseThrow();
      //      reachedSetFirstState = concolicInput.reachedSet.orElseThrow();
    } else {
      initialReachedSet.stream()
          .forEach(as -> ((ARGState) as).deleteChildren()); // contains only one state
      reachedSetFirstState = new PartitionedReachedSet(cpa, TraversalMethod.BFS);
      reachedSetFirstState.add(
          initialReachedSet.getFirstState(),
          initialReachedSet.getPrecision(initialReachedSet.getFirstState()));
      argPath = getARGPath(reachedSetFirstState, concolicInput.values);
      visitedPaths.add(argPath.getFullPath());
      if (coverageCriterionEnum == CoverageCriterion.BRANCH)
        fillVisitedBlocksBranchCoverage(argPath);
      //      else if (Objects.equals(coverageCriterion, "block"))
      // fillVisitedBlocksBlockCoverage(argPath);
      else if (coverageCriterionEnum == CoverageCriterion.ERROR)
        fillVisitedBlocksErrorCoverage(argPath);
      else throw new Error("coverageCriterion unknown");

      ConstraintsState csfromlastatate =
          (ConstraintsState)
              ((CompositeState) argPath.getLastState().getWrappedState()).getWrappedStates().get(3);

      state = csfromlastatate;

      if (searchAlgorithm == SearchAlgorithm.DFS) {
        checkedConstraints.add(state.constraints);
      }

      Map<NondetLocation, List<Value>> previousValues =
          nonDetValueProvider.getReturnedValueHistoryWithLocation();

      if (concolicInput.isInitial) {
        // always replace objects from concrete with ones from symbolic, and replace model
        // generate test case for the first input and add to concolicInput
        concolicInput =
            getConcolicInputAndWriteTestCase(
                state,
                argPath,
                reachedSetFirstState,
                concolicInput.bound,
                concolicInput.values,
                concolicInput.score,
                true,
                Optional.empty());
      }
    }

    //    int lastCheckedConstraint = -1;
    // use while loop for the case that the result is unsat
    while (true) {
      if (state.constraints.isEmpty()) {
        logger.log(Level.FINE, "state.constraints.isEmpty()");
        return childInputs;
      }

      int i = -1;
      // get constraint to flip
      if (searchAlgorithm == SearchAlgorithm.RANDOM) {
        i = this.rnd.nextInt(0, state.constraints.size());
      } else if (searchAlgorithm == SearchAlgorithm.DFS) {
        // use the last constraint that has not been flipped yet in DFS
        for (int j = state.constraints.size() - 1; j >= 0; j--) {
          // list with the constraints up to the current one minus one
          List<Constraint> constraintsWithFlippedCheck =
              new ArrayList<>(state.constraints.subList(0, j));

          // get the last constraint
          Constraint lastConstraintCheck = state.constraints.get(j);

          // flip the last constraint
          SymbolicValueFactory svfCheck = SymbolicValueFactory.getInstance();
          SymbolicExpression flippedCheck =
              svfCheck.logicalNot(
                  (SymbolicExpression) lastConstraintCheck, lastConstraintCheck.getType());
          constraintsWithFlippedCheck.add((Constraint) flippedCheck);

          if (!isSublistOfAnyInCheckedConstraints(constraintsWithFlippedCheck)) {
            i = j;
            break;
          }
        }
      }
      if (i == -1) {
        logger.log(Level.FINE, "i == -1");
        return childInputs;
      }
      //    for (int i = concolicInput.bound; i < state.constraints.size(); i++) {

      try {
        // list with the constraints up to the current one minus one
        List<Constraint> constraintsWithFlipped = new ArrayList<>(state.constraints.subList(0, i));

        // get the last constraint
        Constraint lastConstraint = state.constraints.get(i);

        // flip the last constraint
        SymbolicValueFactory svf = SymbolicValueFactory.getInstance();
        SymbolicExpression flipped =
            svf.logicalNot((SymbolicExpression) lastConstraint, lastConstraint.getType());
        constraintsWithFlipped.add((Constraint) flipped);

        ConstraintsState stateWithFlipped = state.cloneWithNewConstraints(constraintsWithFlipped);
        SolverResult result =
            constraintsSolver.checkUnsat(stateWithFlipped, "foofunction"); // TODO function name
        // don't add unsat results to childInputs
        if (result.isUNSAT()) {
          if (searchAlgorithm == SearchAlgorithm.DFS) {
            checkedConstraints.add(constraintsWithFlipped);
          }
          continue;
        }
        List<ValueAssignment> model = result.model().orElseThrow();

        Map<NondetLocation, List<Value>> values = getValues(concolicInput.values, model, argPath);
        initialReachedSet.stream()
            .forEach(as -> ((ARGState) as).deleteChildren()); // contains only one state
        reachedSetFirstState = new PartitionedReachedSet(cpa, TraversalMethod.BFS);
        reachedSetFirstState.add(
            initialReachedSet.getFirstState(),
            initialReachedSet.getPrecision(initialReachedSet.getFirstState()));
        argPath =
            getARGPath(reachedSetFirstState, concolicInput.values); // ! Needed for state to fill

        state =
            (ConstraintsState)
                ((CompositeState) argPath.getLastState().getWrappedState())
                    .getWrappedStates()
                    .get(3);
        if (searchAlgorithm == SearchAlgorithm.DFS) {
          checkedConstraints.add(state.constraints);
        }

        int score;
        if (coverageCriterionEnum == CoverageCriterion.BRANCH)
          score = calculateScoreBranchCoverage(argPath);
        //        else if (Objects.equals(coverageCriterion, "block"))
        //          score = calculateScoreBlockCoverage(argPath);
        else if (coverageCriterionEnum == CoverageCriterion.ERROR)
          score = calculateScoreErrorCoverage(argPath);
        else throw new Error("coverageCriterion unknown");
        childInputs.add(
            getConcolicInputAndWriteTestCase(
                state,
                argPath,
                reachedSetFirstState,
                i, // TODO i+1
                values,
                score,
                false,
                Optional.of(model)));
      } catch (Exception e) {
        e.printStackTrace();
        throw new Error(e);
      }
      //    }
      return childInputs;
    }
  }

  private boolean isSublistOfAnyInCheckedConstraints(List<Constraint> newList) {
    for (List<Constraint> list : checkedConstraints) {
      // Check if the targetList is a prefix of the current list
      if (isPrefix(list, newList)) {
        return true;
      }
    }
    return false;
  }

  // Helper method to check if newList is a prefix of the given list
  private static <T> boolean isPrefix(List<T> list, List<T> newList) {
    if (newList.size() > list.size()) {
      return false;
    }

    for (int i = 0; i < newList.size(); i++) {
      if (!list.get(i).equals(newList.get(i))) {
        return false;
      }
    }
    return true;
  }

  private Map<NondetLocation, List<Value>> getValues(
      Map<NondetLocation, List<Value>> previousValues,
      List<ValueAssignment> model,
      ARGPath argPath) {
    Map<NondetLocation, List<Value>> values = new HashMap<>();

    // Iterate over each ValueAssignment and extract the Value
    // TODO effizienter, wenn man über den ARGPath iteriert und dann immer das model checked?
    for (ValueAssignment assignment : model) {
      Object valueObject = assignment.getValue();
      String valueFormula = assignment.getValueAsFormula().toString();
      // Workaround for int overflows
      if (valueObject instanceof BigInteger && valueFormula.endsWith("_32")) {
        int intValue = ((BigInteger) valueObject).intValue();
        valueObject = BigInteger.valueOf(intValue);
      }

      Value value = Value.of(valueObject); // Extract the value

      FileLocation fl = null;
      try {
        fl = convertToFileLocation(assignment.getKey().toString(), argPath).orElseThrow();
      } catch (NoSuchElementException e) {
        logger.log(
            Level.WARNING, "No result for convertToFileLocation, variable: " + assignment.getKey());
        // sometimes, new SymbolicIdentifiers are intruduced (when a function is called with a
        // parameter), so there is no nondet call and location for the SymbolicIdentifier
        // TODO -> ignore for now -> actually works without changes, test again
        continue;
      }
      NondetLocation location =
          new NondetLocation(
              fl.getFileName().toString(),
              fl.getStartingLineNumber(),
              fl.getStartColumnInLine(),
              fl.getEndColumnInLine());
      if (values.containsKey(location)) {
        values.get(location).add(value);
      } else {
        List<Value> valueList = new ArrayList<>();
        valueList.add(value);
        values.put(location, valueList);
      }
    }
    for (Map.Entry<NondetLocation, List<Value>> entry : previousValues.entrySet()) {
      if (!values.containsKey(entry.getKey())) {
        values.put(entry.getKey(), entry.getValue());
      }
    }
    return values;
  }

  public Optional<FileLocation> convertToFileLocation(String pLocation, ARGPath pARGPath) {
    String variableNameWithoutFunctionname = Iterables.get(Splitter.on("::").split(pLocation), 1);
    String variableName = Iterables.get(Splitter.on('#').split(variableNameWithoutFunctionname), 0);

    String identifier = Iterables.get(Splitter.on('#').split(pLocation), 0);
    for (int i = 0; i < pARGPath.getInnerEdges().size(); i++) {
      CFAEdge edge = pARGPath.getInnerEdges().get(i);
      //       new code
      if (edge instanceof CDeclarationEdge
          && edge.getRawStatement().contains("__VERIFIER_nondet")
          && ((CDeclarationEdge) edge).getDeclaration().getQualifiedName() != null
          && ((CDeclarationEdge) edge).getDeclaration().getQualifiedName().contains(identifier)) {
        return Optional.of(edge.getFileLocation());
      } else if (edge instanceof CStatementEdge
          && edge.getRawStatement().contains("__VERIFIER_nondet")
          && edge.getRawStatement().contains(variableName)) {
        // check if a previous edge is a declaration edge with the identifier
        for (int j = i - 1; j >= 0; j--) {
          CFAEdge previousEdge = pARGPath.getInnerEdges().get(j);
          if (previousEdge instanceof CDeclarationEdge
              && ((CDeclarationEdge) previousEdge).getDeclaration().getQualifiedName() != null
              && ((CDeclarationEdge) previousEdge)
                  .getDeclaration()
                  .getQualifiedName()
                  .contains(identifier)) {
            return Optional.of(edge.getFileLocation());
          }
        }
      }
    }
    return Optional.empty();
  }

  public int calculateScoreBlockCoverage(ARGPath pARGPath) {
    List<CFAEdge> edges = pARGPath.getInnerEdges();
    int score = 0;
    for (int i = 0; i < edges.size() - 1; i++) {
      CFAEdge ce = edges.get(i);
      CFAEdge ceChild = edges.get(i + 1);
      // check if the current edge is an AssumeEdge,
      // and the next edge isn't to check if there is a block of code
      // ceChild can also be null here
      if (ce instanceof AssumeEdge && !(ceChild instanceof AssumeEdge)) {
        // if so, check if visitedBlocks contains the edge -> if not, add 1 to the score
        if (!visitedBlocks.contains((AssumeEdge) ce)) {
          score++;
        }
      }
    }
    return score;
  }

  public int calculateScoreBranchCoverage(ARGPath pARGPath) {
    List<CFAEdge> edges = pARGPath.getInnerEdges();
    List<CFAEdge> scoreEdges = new ArrayList<>();

    int score = 0;
    for (int i = 0; i < edges.size() - 1; i++) {
      CFAEdge ce = edges.get(i);
      if (ce instanceof AssumeEdge) {
        // check if visitedBlocks contains the edge -> if not, add 1 to the score
        if (!visitedBlocks.contains((AssumeEdge) ce) && !scoreEdges.contains(ce)) {
          score++;
          scoreEdges.add(ce);
        }
      }
    }
    return score;
  }

  public int calculateScoreErrorCoverage(ARGPath pARGPath) {
    List<CFAEdge> edges = pARGPath.getInnerEdges();
    int score = 0;
    for (int i = 0; i < edges.size() - 1; i++) {
      CFAEdge ce = edges.get(i);
      if (ce instanceof FunctionCallEdge && ce.getRawStatement().equals("reach_error();")) {
        // check if visitedBlocks contains the edge -> if not, add 1 to the score
        if (!visitedBlocks.contains(ce)) {
          score++;
        }
      }
    }
    return score;
  }

  public ARGPath getARGPath(ReachedSet pReachedSet, Map<NondetLocation, List<Value>> values)
      throws CPAException {
    logger.log(Level.FINE, "ConcolicAlgorithmRandom: executing getARGPath concrete");
    nonDetValueProvider.clearKnownValues();
    nonDetValueProvider.setKnownValues(values);
    AlgorithmStatus algorithmStatus = null;
    boolean isInterrupted = false;
    try {
      algorithmStatus = algorithm.run(pReachedSet);

    } catch (InterruptedException e) {
      if (e.toString().contains("The CPU-time limit")) {
        logger.log(Level.SEVERE, "CPU-time limit reached, writing last test case...");
        isInterrupted = true;
      } else {
        throw new Error(e);
      }
    }
    // create ARGPath
    List<ARGState> argStateSet = new ArrayList<>();

    Set<AbstractState> abstractStates =
        pReachedSet.asCollection(); // set is unordered, use getChildren later
    for (AbstractState state : abstractStates) {

      if (state instanceof ARGState argState) {
        argStateSet.add(argState);
      } else {
        throw new Error("State is not an ARGState");
      }
    }
    ARGPath ap;
    if (!isInterrupted) ap = new ARGPath(argStateSet, true); // true
    else ap = new ARGPath(argStateSet);
    return ap;
  }
}
