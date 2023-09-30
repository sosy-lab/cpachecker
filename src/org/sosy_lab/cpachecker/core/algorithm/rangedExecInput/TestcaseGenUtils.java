// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.rangedExecInput;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.algorithm.rangedExecInputSequences.SequenceGenUtils;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.path.PathIterator;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.randomWalk.RandomWalkState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.Model;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;

public class TestcaseGenUtils {
  private static final long DEFAULT_LONG = 0;
  private final ImmutableSet<String> namesOfRandomFunctions;
  private final Solver solver;
  private final LogManager logger;
  private final  PathFormulaManager pfManager;
  private final FormulaManagerView fmgr;
  private final CFA cfa;

  public TestcaseGenUtils(
      ImmutableSet<String> pNamesOfRandomFunctions,
      Solver pSolver,
      LogManager pLogger,
      PathFormulaManager pPfManager,
      FormulaManagerView pFmgr,
      CFA pCfa) {
    namesOfRandomFunctions = pNamesOfRandomFunctions;
    solver = pSolver;
    logger = pLogger;
    pfManager = pPfManager;
    fmgr = pFmgr;
    cfa = pCfa;
  }

  public List<Pair<CIdExpression, Long>> computeInputForRandomWalkPathNonIterative(
      RandomWalkState pState, ARGPath path)
      throws InterruptedException, SolverException, CPAException {
    {
      // Check, if the given path is sat by conjoining the path formulae of the abstraction
      // locations.
      // If not, cut off the last part and recursively continue.

      try (ProverEnvironment prover = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {

        try {
          prover.push();
          final BooleanFormula formula = pState.getCurrentPathFormula().getFormula();
          prover.addConstraint(formula);
          boolean unsat = prover.isUnsat();
          if (unsat) {
            logger.log(
                Level.INFO,
                String.format("The formul'%s' is unsat, continuing with a shorter one", formula));
            if (pState.getLastBranchingPoint() != null) {
              return computeInputForRandomWalkPathNonIterative(
                  pState.getLastBranchingPoint(), path);
            } else {
              throw new CPAException(
                  String.format(
                      "Failed to compute a path formula for %s, as no predecessor is given ",
                      pState));
            }
          }
        } catch (InterruptedException | SolverException e) {
          // In case of an error, we assume that the formula is sat and should be exported
          return ImmutableList.of();
        }

        // we now that the model is sat at this point
        logger.log(Level.FINEST, prover.isUnsat());
        Model m = prover.getModel();
        List<Pair<CIdExpression, Long>> inputs = matchInputsOnPath(path, m);
        logger.log(Level.INFO, inputs);
        return inputs;
      }
    }
  }

  public List<Pair<CIdExpression, Long>> computeInputForLoopbound(ARGPath pARGPath)
      throws InterruptedException, SolverException, CPAException {

    // Check, if the given path is sat by conjoining the path formulae of the abstraction locations.
    // If not, cut off the last part and recursively continue.

    // Build the path formula
    PathFormula pf = pfManager.makeEmptyPathFormula();

    try (ProverEnvironment prover = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      for (ARGState state : pARGPath.asStatesList()) {
        @Nullable PredicateAbstractState predState =
            AbstractStates.extractStateByType(state, PredicateAbstractState.class);
        if (predState != null && predState.isAbstractionState()) {
          final PathFormula blockFormula = predState.getAbstractionFormula().getBlockFormula();
          pf = pfManager.makeConjunction(Lists.newArrayList(pf, blockFormula));
          try {
            prover.addConstraint(blockFormula.getFormula());
            boolean unsat = prover.isUnsat();
            if (unsat) {

              while (unsat) {
                logger.log(
                    Level.INFO,
                    String.format(
                        "The formul'%s' is unsat, continuing with a shorter one", pf.getFormula()));
                prover.pop();
                unsat = prover.isUnsat();
              }
              break;
            }
            prover.push();
          } catch (InterruptedException | SolverException e) {
            // In case of an error, we assume that the formula is sat and should be exported
            return ImmutableList.of();
          }
        }
      }
      // we now that the model is sat at this point
      logger.log(Level.FINEST, prover.isUnsat());
      logger.log(Level.INFO, pf.getFormula());
      Model m = prover.getModel();

      List<Pair<CIdExpression, Long>> inputs = matchInputsOnPath(pARGPath, m);
      logger.log(Level.INFO, inputs);
      return inputs;
    }
  }

  public List<Pair<Boolean, Integer>> computeSequenceForLoopbound(
      ARGPath pARGPath, Set<String> blacklist)
      throws InterruptedException, SolverException, CPAException {

    // Check, if the given path is sat by conjoining the path formulae of the abstraction locations.
    // If not, cut off the last part and recursively continue.

    // Build the path formula
    PathFormula pf = pfManager.makeEmptyPathFormula();
    List<ARGState> statesOnSatPath = new ArrayList<>();
    try (ProverEnvironment prover = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      for (ARGState state : pARGPath.asStatesList()) {
        @Nullable PredicateAbstractState predState =
            AbstractStates.extractStateByType(state, PredicateAbstractState.class);
        if (predState != null && predState.isAbstractionState()) {
          statesOnSatPath.add(state);
          final PathFormula blockFormula = predState.getAbstractionFormula().getBlockFormula();
          logger.log(
              Level.FINE,
              String.format(
                  "Block Formula for State with id %d is %s", state.getStateId(), blockFormula));
          pf = pfManager.makeConjunction(Lists.newArrayList(pf, blockFormula));
          logger.log(Level.FINE, blockFormula.getFormula());
          try {
            prover.addConstraint(blockFormula.getFormula());
            boolean unsat = prover.isUnsat();
            if (unsat) {

              while (unsat) {
                logger.log(
                    Level.INFO,
                    String.format(
                        "The formul'%s' is unsat, continuing with a shorter one", pf.getFormula()));
                prover.pop();
                statesOnSatPath.remove(statesOnSatPath.size() - 1);
                unsat = prover.isUnsat();
              }
              break;
            }
            prover.push();
          } catch (InterruptedException | SolverException e) {
            // In case of an error, we assume that the formula is sat and should be exported
            return ImmutableList.of();
          }
        }
      }
      // we now that the model is sat at this point
      logger.log(Level.FINEST, prover.isUnsat());

      SequenceGenUtils sequenceGenUtils = new SequenceGenUtils(logger, cfa);

      List<Pair<Boolean, Integer>> inputs =
          sequenceGenUtils.computeSequenceForLoopbound(
              pARGPath,
              blacklist,
              Optional.ofNullable(statesOnSatPath.get(statesOnSatPath.size() - 1)));
      logger.log(Level.INFO, inputs);
      return inputs;
    }
  }

  public void printFileToPutput(List<Pair<CIdExpression, Long>> pInputs, Path testcaseName)
      throws IOException {

    logger.logf(Level.INFO, "Storing the testcase at %s", testcaseName.toAbsolutePath().toString());
    List<String> content = new ArrayList<>();
    content.add("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>");
    //    content.add(
    //        "<!DOCTYPE testcase PUBLIC \"+//IDN sosy-lab.org//DTD test-format testcase 1.1//EN\""
    //            + " \"https://sosy-lab.org/test-format/testcase-1.1.dtd\">");
    content.add("<testcase>");
    for (Pair<CIdExpression, Long> pair : pInputs) {

      CIdExpression var = pair.getFirst();

      content.add(
          String.format(
              " <input variable=\"%s\" type=\"%s\">%s</input>",
              var.getDeclaration().getName(), var.getDeclaration().getType(), pair.getSecond()));
    }

    content.add("</testcase>");
    IO.writeFile(testcaseName, Charset.defaultCharset(), Joiner.on("\n").join(content));
  }

  private List<Pair<CIdExpression, Long>> matchInputsOnPath(ARGPath pARGPath, Model pM)
      throws CPAException, InterruptedException {
    PathIterator pathIterator = pARGPath.fullPathIterator();
    List<Pair<CIdExpression, Long>> results = new ArrayList<>();
    do {
      if (pathIterator.isPositionWithState()) {
        final ARGState abstractState = pathIterator.getAbstractState();
        if (pathIterator.hasNext()) {
          @Nullable CFAEdge edge = pathIterator.getOutgoingEdge();
          if (edge != null) {
            // Check  if the edge is an assignemtnt with random function at rhs
            if (edge instanceof CStatementEdge) {
              CStatement stmt = ((CStatementEdge) edge).getStatement();
              if (stmt instanceof CFunctionCallAssignmentStatement ass) {
                if (leftIsVar(ass.getLeftHandSide()) && isRandomFctCall(ass.getRightHandSide())) {
                  Optional<Long> value =
                      getIntegerValueFromModelForEdge(pM, abstractState, edge, ass);
                  if (value.isPresent()) {
                    results.add(Pair.of((CIdExpression) ass.getLeftHandSide(), value.get()));
                  } else {
                    throw new CPAException(
                        String.format(
                            "Unable to compute the value of variable from  the expression %s in the"
                                + " model %s",
                            edge, pM));
                  }
                }
              }
            } else if (edge instanceof CDeclarationEdge) {
              // TODO: Handle this case
            }
          }
        }
      }
    } while (pathIterator.advanceIfPossible());
    return results;
  }

  private Optional<Long> getIntegerValueFromModelForEdge(
      Model pM, ARGState abstractState, @NonNull CFAEdge edge, CFunctionCallAssignmentStatement ass)
      throws InterruptedException, CPAException {
    @Nullable PredicateAbstractState predState =
        AbstractStates.extractStateByType(abstractState, PredicateAbstractState.class);
    assert predState != null;
    PathFormula pathFormula = predState.getPathFormula();
    PathFormula emptyPF =
        pfManager.makeEmptyPathFormulaWithContext(
            pathFormula.getSsa(), pathFormula.getPointerTargetSet());
    PathFormula pfCurrent = pfManager.makeAnd(emptyPF, edge);

    // As a temp variable is created representing the function call return value, we
    // need to clean the map
    Map<String, Formula> varMap = new HashMap<>(fmgr.extractVariables(pfCurrent.getFormula()));
    List<String> toRemove =
        varMap.keySet().stream()
            .filter(
                pFormula -> pFormula.contains(ass.getRightHandSide().getDeclaration().getName()))
            .collect(ImmutableList.toImmutableList());
    toRemove.forEach(e -> varMap.remove(e));
    if (varMap.size() != 1) {
      if (varMap.isEmpty()) {
        logger.log(
            Level.WARNING,
            String.format("Unable to compute the variable at LHS for the expression %s", edge));
        return Optional.empty();
      }
      throw new CPAException(
          String.format("Unable to compute the variable at LHS for the expression %s", edge));
    }

    Formula varAsFormula = varMap.entrySet().stream().findFirst().get().getValue();
    Object evalRes = pM.evaluate(varAsFormula);
    if (evalRes instanceof Number) {
      return Optional.of(((Number) evalRes).longValue());
    } else if (evalRes == null) {
      return Optional.of(DEFAULT_LONG);
    }
    return Optional.empty();
  }

  private boolean isRandomFctCall(CFunctionCallExpression pRightHandSide) {
    String name = pRightHandSide.getDeclaration().getName();
    return this.namesOfRandomFunctions.stream().anyMatch(s -> name.contains(s));
  }

  private boolean leftIsVar(CLeftHandSide pLeftHandSide) {
    return pLeftHandSide instanceof CIdExpression;
  }
}
