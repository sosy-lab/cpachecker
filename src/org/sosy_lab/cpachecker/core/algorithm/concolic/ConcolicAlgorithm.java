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
import com.google.common.collect.ImmutableList;
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
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.concolic.ConcolicAlgorithmIsInitialized.AlgorithmType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.PartitionedReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist.TraversalMethod;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.constraints.ConstraintsCPA;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsSolver;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsSolver.SolverResult;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState;
import org.sosy_lab.cpachecker.cpa.location.LocationCPA;
import org.sosy_lab.cpachecker.cpa.value.NondeterministicValueProvider;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisConcreteCPA;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueFactory;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.testcase.TestCaseExporter;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;

@SuppressWarnings("Duplicates")
@Options(prefix = "concolic")
public class ConcolicAlgorithm implements Algorithm {

  @Option(
      secure = true,
      name = "coverageCriterion",
      description = "type of coverage criterion to use, branch or error")
  private String coverageCriterion = "not changed";

  private CoverageCriterion coverageCriterionEnum;

  //  private static boolean isInitialized = false;
  private final Algorithm algorithm;
  private final ConfigurableProgramAnalysis cpa;
  private final Configuration config;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final ConstraintsSolver constraintsSolver;
  //  public static MachineModel machineModel;
  private final CFA cfa;
  // visited blocks
  //   AssumeEdges for branches coverage
  private final Set<AbstractCFAEdge> visitedEdges;
//  private final Set<AbstractCFAEdge> allVisitedEdges;
  private final Set<List<CFAEdge>> visitedPaths;

  // TODO remove public -> Singleton in NondeterministicValueProvider?
  public static final NondeterministicValueProvider nonDetValueProvider =
      new NondeterministicValueProvider();
  public static int tmpcounter = 0;

  private final ConstraintsCPA constraintsCPA;
  TestCaseExporter exporter;
  CPAAlgorithm concreteAlgorithmCE; // for the concrete execution
  private final ConfigurableProgramAnalysis argCpaCE;

  private int testsWritten = 0;
  private final int maxTestCases = 99000;

  public ConcolicAlgorithm(
      Algorithm pAlgorithm,
      ConfigurableProgramAnalysis pCpa,
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      CFA pCfa)
      throws Exception {
    pConfig.inject(this, ConcolicAlgorithm.class);
    setIsInitialized(AlgorithmType.GENERATIONAL, pLogger);
    this.visitedEdges = new HashSet<>();
//    this.allVisitedEdges = new HashSet<>();
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
    this.shutdownNotifier = pShutdownNotifier;
    this.cfa = pCfa;
    //    machineModel = cfa.getMachineModel();

    this.constraintsCPA =
        CPAs.retrieveCPAOrFail(cpa, ConstraintsCPA.class, ConcolicAlgorithm.class);

    this.constraintsSolver = constraintsCPA.getSolver();

    Configuration configCE = null;
    if (coverageCriterionEnum == CoverageCriterion.BRANCH) {
      configCE =
          Configuration.builder().loadFromFile("cpachecker/config/concolic-only-concrete.properties").build();
    } else if (coverageCriterionEnum == CoverageCriterion.ERROR) {
      configCE =
          Configuration.builder()
              .loadFromFile("cpachecker/config/concolic-only-concrete-error-coverage.properties")
              .build();
    }

    ValueAnalysisConcreteCPA concreteCPACE =
        new ValueAnalysisConcreteCPA(configCE, logger, shutdownNotifier, cfa);

    Specification specificationCE =
        Specification.fromFiles(ImmutableList.of(), cfa, configCE, logger, shutdownNotifier);

    ConfigurableProgramAnalysis locationCpaCE =
        LocationCPA.factory().set(cfa, CFA.class).setConfiguration(configCE).createInstance();

    ConfigurableProgramAnalysis callstackCpaCE =
        CallstackCPA.factory()
            .set(cfa, CFA.class)
            .setConfiguration(configCE)
            .setLogger(logger)
            .createInstance();

    List<ConfigurableProgramAnalysis> analysesCE = new ArrayList<>();
    analysesCE.add(locationCpaCE);
    analysesCE.add(concreteCPACE);
    analysesCE.add(callstackCpaCE);
    ConfigurableProgramAnalysis concreteCPACompositeCE =
        CompositeCPA.factory()
            .setChildren(new ArrayList<>(analysesCE))
            .set(ImmutableList.of(new ArrayList<>(analysesCE)), ImmutableList.class)
            .set(cfa, CFA.class)
            .setConfiguration(configCE)
            .createInstance();

    this.argCpaCE =
        ARGCPA
            .factory()
            .set(concreteCPACompositeCE, ConfigurableProgramAnalysis.class)
            .setConfiguration(configCE)
            .setLogger(logger)
            .set(specificationCE, Specification.class)
            .set(cfa, CFA.class)
            .createInstance();

    this.concreteAlgorithmCE =
        new CPAAlgorithm(this.argCpaCE, logger, shutdownNotifier, null, false);

    this.exporter = new TestCaseExporter(cfa, logger, config);
  }

  public record ConcolicInput(
      Map<NondetLocation, List<Value>> values,
      Optional<List<ValueAssignment>> model,
      Optional<ReachedSet> reachedSet,
      Optional<ARGPath> argPath,
      int bound,
      int score,
      boolean isNewPath,
      boolean isInitial) {}

  enum CoverageCriterion {
    BRANCH,
    ERROR,
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
            0,
            0,
            true,
            true));

//    int numAllAssumeEdges =
//        cfa.edges().stream().filter(e -> e instanceof CAssumeEdge).toList().size();

    while (!workList.isEmpty()) {

      if (coverageCriterionEnum == CoverageCriterion.ERROR && !visitedEdges.isEmpty()) {
        // for error coverage, stop when one error location is found
        reachedSet.clearWaitlist();
        return AlgorithmStatus.SOUND_AND_PRECISE;
      }

      // optimization: re-calculate score
      if (coverageCriterionEnum == CoverageCriterion.BRANCH) {
        List<ConcolicAlgorithm.ConcolicInput> inputsToRemove = new ArrayList<>();
        List<ConcolicAlgorithm.ConcolicInput> inputsToAdd = new ArrayList<>();
        for (ConcolicInput ci : workList) {
          if (ci.argPath().isPresent()) {
            // circumvent ConcurrentModificationException
            ARGPath argPath = ci.argPath().orElseThrow();
            inputsToRemove.add(ci);
            inputsToAdd.add(
                new ConcolicInput(
                    ci.values(),
                    ci.model(),
                    ci.reachedSet(),
                    ci.argPath(),
                    ci.bound(),
                    calculateScoreBranchCoverage(argPath),
                    !visitedPaths.contains(argPath.getFullPath()),
                    ci.isInitial));
          }
        }
        workList.removeAll(inputsToRemove);
        workList.addAll(inputsToAdd);
      }
      if (workList.stream().noneMatch(ci -> ci.score() > 0)) {
        // we are probably finished
        logger.log(Level.INFO, "ConcolicAlgorithm: no input with score > 0 in the worklist");
      }

      // Get the element with the highest score and remove it
      ConcolicInput highestScoreSeed =
          workList.stream()
              .max(
                  Comparator.comparing(ci -> ((ConcolicInput) ci).score())
                      .thenComparing(ci -> ((ConcolicInput) ci).bound(), Comparator.reverseOrder()))
              .get();
      workList.remove(highestScoreSeed);
      // check the path again
      if (highestScoreSeed.argPath().isPresent()
          && visitedPaths.contains(highestScoreSeed.argPath().orElseThrow().getFullPath())) {
        continue;
      }
      // run concolic execution
      List<ConcolicInput> childInputs = expandExecution(highestScoreSeed, reachedSet);
      // own optimization: filter out inputs with score 0
      // if (!Objects.equals(coverageCriterion, "error"))
      //            childInputs.removeIf(ci -> ci.score == 0);
      childInputs.removeIf(ci -> !ci.isNewPath);
      //      List<ConcolicInput> tmp =
      //          childInputs.stream().filter(ci -> !ci.isNewPath && ci.score() == 0).toList();
      workList.addAll(childInputs);
      // TODO
      //      if (visitedPaths.size() > 1000) {
      // if (visitedPaths.size() > 100) -> remove inputs that will probably not lead to a new path
      // to prevent endless loop
      //        logger.log(Level.WARNING, "ConcolicAlgorithm: visitedPaths.size() > 1000");
      //        workList.removeIf(ci -> ci.score == 0);
      //      }

      // print statistics
//      if (coverageCriterionEnum == CoverageCriterion.BRANCH) {
//        logger.log(Level.FINE, "Size CFA Edges: " + numAllAssumeEdges);
//        logger.log(Level.FINE, "Size All Visited Edges: " + allVisitedEdges.size());
//        //        logger.log(
//        //            Level.FINE,
//        //            "Size Missing Edges: "
//        //                + cfa.edges().stream()
//        //                    .filter(e -> e instanceof CAssumeEdge && !allVisitedEdges.contains(e))
//        //                    .toList()
//        //                    .size());
//        logger.log(
//            Level.FINE,
//            "Estimated Coverage: " + (float) allVisitedEdges.size() / numAllAssumeEdges);
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
          visitedEdges.add((AssumeEdge) ce);
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
          visitedEdges.add((FunctionCallEdge) ce);
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
          visitedEdges.add((AssumeEdge) ce);
        }
      }
    } catch (NoSuchElementException e) {
      throw new Error(e);
    }
  }

//  private void fillAllVisitedBlocksBranchCoverage(ARGPath pARGPath) {
//    List<CFAEdge> edges = pARGPath.getInnerEdges();
//    try {
//      for (CFAEdge ce : edges) {
//        if (ce instanceof AssumeEdge) {
//          allVisitedEdges.add((AssumeEdge) ce);
//        }
//      }
//    } catch (NoSuchElementException e) {
//      throw new Error(e);
//    }
//  }

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
    initialReachedSet.stream()
        .forEach(as -> ((ARGState) as).deleteChildren()); // contains only one state
    ReachedSet reachedSetConcolic = new PartitionedReachedSet(cpa, TraversalMethod.BFS);
    reachedSetConcolic.add(
        initialReachedSet.getFirstState(),
        initialReachedSet.getPrecision(initialReachedSet.getFirstState()));
    ARGPath argPath = getARGPath(reachedSetConcolic, concolicInput.values, false);
    visitedPaths.add(argPath.getFullPath());
    if (coverageCriterionEnum == CoverageCriterion.BRANCH) fillVisitedBlocksBranchCoverage(argPath);
    //    else if (Objects.equals(coverageCriterion, "block"))
    // fillVisitedBlocksBlockCoverage(argPath);
    else if (coverageCriterionEnum == CoverageCriterion.ERROR)
      fillVisitedBlocksErrorCoverage(argPath);
    else throw new Error("coverageCriterion unknown");

    // also fill allVisitedBlocks for branch coverage
//    if (coverageCriterionEnum == CoverageCriterion.BRANCH)
//      fillAllVisitedBlocksBranchCoverage(argPath);

    ConstraintsState state =
        (ConstraintsState)
            ((CompositeState) argPath.getLastState().getWrappedState()).getWrappedStates().get(3);

    if (concolicInput.isInitial) {
      // always replace objects from concrete with ones from symbolic, and replace model
      // generate test case for the first input and add to concolicInput
      concolicInput =
          getConcolicInputAndWriteTestCase(
              state,
              argPath,
              reachedSetConcolic,
              concolicInput.bound,
              concolicInput.values,
              concolicInput.score,
              true,
              Optional.empty());
    }

    for (int i = concolicInput.bound; i < state.constraints.size(); i++) {

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
        if (result.isUNSAT()) {
          // don't add unsat results to childInputs
          continue;
        }
        List<ValueAssignment> model = result.model().orElseThrow();

        Map<NondetLocation, List<Value>> values = getValues(concolicInput.values, model, argPath);

        // reset reached Set to set with one state
        initialReachedSet.stream()
            .forEach(as -> ((ARGState) as).deleteChildren()); // note: contains only one state
        ReachedSet reachedSetConcrete = new PartitionedReachedSet(argCpaCE, TraversalMethod.BFS);
        CompositeState seedState =
            (CompositeState) ((ARGState) initialReachedSet.getFirstState()).getWrappedState(); //
        // 0. state: LocationState, 1. State ValueAnalysis
        //        concreteAbstractState
        List<AbstractState> editableStates = new ArrayList<>();
        assert seedState != null;
        editableStates.add(seedState.get(0)); // location
        editableStates.add(seedState.get(2)); // concrete
        editableStates.add(seedState.get(4)); // callstack
        CompositeState concreteAbstractState = new CompositeState(editableStates);
        ARGState concreteAbstractStateARG = new ARGState(concreteAbstractState, null);
        reachedSetConcrete.add(
            concreteAbstractStateARG,
            initialReachedSet.getPrecision(initialReachedSet.getFirstState()));

        ARGPath argPathConcrete = getARGPath(reachedSetConcrete, values, true);

        // also fill allVisitedBlocks for branch coverage
//        if (coverageCriterionEnum == CoverageCriterion.BRANCH)
//          fillAllVisitedBlocksBranchCoverage(argPathConcrete);

        int score;
        if (coverageCriterionEnum == CoverageCriterion.BRANCH)
          score = calculateScoreBranchCoverage(argPathConcrete);
        //        else if (Objects.equals(coverageCriterion, "block"))
        //          score = calculateScoreBlockCoverage(argPathConcrete);
        else if (coverageCriterionEnum == CoverageCriterion.ERROR)
          score = calculateScoreErrorCoverage(argPathConcrete);
        else throw new Error("coverageCriterion unknown");
        childInputs.add(
            getConcolicInputAndWriteTestCase(
                state,
                argPathConcrete,
                reachedSetConcolic,
                i, // TODO i+1
                values,
                score,
                false,
                Optional.of(model)));
      } catch (Exception e) {
        e.printStackTrace();
        throw new Error(e);
      }
    }
    return childInputs;
  }

  private Map<NondetLocation, List<Value>> getValues(
      Map<NondetLocation, List<Value>> previousValues,
      List<ValueAssignment> model,
      ARGPath argPath) {
    Map<NondetLocation, List<Value>> values = new HashMap<>();

    // Iterate over each ValueAssignment and extract the Value
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
    String variableNameWithoutFunctionname;
    String variableName;
    String identifier;
    try {
      Iterable<String> locationSplit = Splitter.on("::").split(pLocation);

      // some pLocations do not contain a function name, so we need to check if the split is
      // possible, otherwise, use the whole pLocation
      if (Iterables.size(locationSplit) == 2) {
        variableNameWithoutFunctionname = Iterables.get(locationSplit, 1);
      } else {
        variableNameWithoutFunctionname = pLocation;
      }

      variableName = Iterables.get(Splitter.on('#').split(variableNameWithoutFunctionname), 0);

      identifier = Iterables.get(Splitter.on('#').split(pLocation), 0);
    } catch (Exception e) {
      logger.log(Level.WARNING, "Error in convertToFileLocation: " + e);
      return Optional.empty();
    }
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
        if (!visitedEdges.contains((AssumeEdge) ce)) {
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
        if (!visitedEdges.contains((AssumeEdge) ce) && !scoreEdges.contains(ce)) {
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
        if (!visitedEdges.contains(ce)) {
          score++;
        }
      }
    }
    return score;
  }

  public ARGPath getARGPath(
      ReachedSet pReachedSet, Map<NondetLocation, List<Value>> values, boolean isConcrete)
      throws CPAException {
    logger.log(
        Level.FINE,
        "ConcolicAlgorithm: executing getARGPath, isConcrete: " + (isConcrete ? "true" : "false"));
    nonDetValueProvider.clearKnownValues();
    nonDetValueProvider.setKnownValues(values);
    AlgorithmStatus algorithmStatus = null;
    boolean isInterrupted = false;
    boolean isStopped = false;
    try {
      //    boolean isStopped = runAlgorithmWithTimeout(pReachedSet, isConcrete); // !! TODO

      //      ResourceLimitChecker limits = null;
      //      WalltimeLimit l = WalltimeLimit.fromNowOn(TimeSpan.of(10, TimeUnit.MILLISECONDS));
      //      ShutdownManager loopGenerationShutdown =
      // ShutdownManager.createWithParent(shutdownNotifier);
      //      limits = new ResourceLimitChecker(loopGenerationShutdown,
      // Collections.singletonList(l));
      //      limits.start();
      //
      if (isConcrete) {

        long start = System.currentTimeMillis();
        algorithmStatus = concreteAlgorithmCE.run(pReachedSet);
        logger.log(
            Level.FINE,
            "Time in milliseconds for concrete execution: " + (System.currentTimeMillis() - start));
      } else {
        long start = System.currentTimeMillis();
        algorithmStatus = algorithm.run(pReachedSet);
        logger.log(
            Level.FINE,
            "Time in milliseconds for concolic execution: " + (System.currentTimeMillis() - start));
      }

      if (algorithmStatus == AlgorithmStatus.NO_PROPERTY_CHECKED) {
        isStopped = true;
      }
      //
      //      limits.cancel();

    } catch (InterruptedException e) {
      if (e.toString().contains("The CPU-time limit")) {
        logger.log(Level.SEVERE, "CPU-time limit reached, writing last test case...");
        isInterrupted = true;
      } else {
        throw new Error(e);
      }
    } catch (Exception pE) {
      logger.log(Level.SEVERE, "Exception in getARGPath: " + pE);
      logger.log(Level.SEVERE, "Continuing...");
    }

    // create ARGPath
    List<ARGState> argStateSet = new ArrayList<>();

    Set<AbstractState> abstractStates =
        pReachedSet.asCollection(); // set is unordered, use getChildren later
    for (AbstractState state : abstractStates) {

      if (state instanceof ARGState argState) {
        argStateSet.add(argState);
      } else throw new Error("State is not an ARGState");
    }
    ARGPath ap;
    if (!isInterrupted && !isStopped) ap = new ARGPath(argStateSet, true); // true
    else ap = new ARGPath(argStateSet);
    return ap;
  }
}
