/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.lasso_ranker.construction;

import static com.google.common.base.Preconditions.checkNotNull;
import static de.uni_freiburg.informatik.ultimate.lassoranker.variables.InequalityConverter.NlaHandling.EXCEPTION;
import static java.util.logging.Level.FINE;
import static java.util.stream.Collectors.toSet;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractStateByType;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import de.uni_freiburg.informatik.ultimate.lassoranker.Lasso;
import de.uni_freiburg.informatik.ultimate.lassoranker.LinearInequality;
import de.uni_freiburg.informatik.ultimate.lassoranker.LinearTransition;
import de.uni_freiburg.informatik.ultimate.lassoranker.exceptions.TermException;
import de.uni_freiburg.informatik.ultimate.lassoranker.variables.InequalityConverter;
import de.uni_freiburg.informatik.ultimate.lassoranker.variables.RankVar;
import de.uni_freiburg.informatik.ultimate.logic.Term;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.lasso_ranker.TermRankVar;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathIterator;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.termination.TerminationState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView.BooleanFormulaTransformationVisitor;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.SolverException;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.Formula;
import org.sosy_lab.solver.api.ProverEnvironment;
import org.sosy_lab.solver.basicimpl.AbstractFormulaManager;
import org.sosy_lab.solver.basicimpl.tactics.Tactic;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * Creates {@link Lasso}s from {@link CounterexampleInfo}.
 */
public class LassoBuilder {

  private final static Set<String> META_VARIABLES = ImmutableSet.of("__VERIFIER_nondet_int");

  final static String TERMINATION_AUX_VARS_PREFIX = "__TERMINATION-";

  final static String TERMINATION_REPLACE_VARS_PREFIX = "__TERMINATION_REPLACE-";

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;

  private final AbstractFormulaManager<Term, ?, ?, ?> formulaManager;
  private final Supplier<ProverEnvironment> proverEnvironmentSupplier;
  private final FormulaManagerView formulaManagerView;
  private final PathFormulaManager pathFormulaManager;

  private final IfThenElseElimination ifThenElseElimination;
  private final DivAndModElimination divAndModElimination;
  private final EqualElimination equalElimination;
  private final NotEqualAndNotInequalityElimination notEqualAndNotInequalityElimination;
  private final DnfTransformation dnfTransformation;

  public LassoBuilder(
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      AbstractFormulaManager<Term, ?, ?, ?> pFormulaManager,
      FormulaManagerView pFormulaManagerView,
      Supplier<ProverEnvironment> pProverEnvironmentSupplier,
      PathFormulaManager pPathFormulaManager) {
    logger = checkNotNull(pLogger);
    shutdownNotifier = checkNotNull(pShutdownNotifier);
    formulaManager = checkNotNull(pFormulaManager);
    proverEnvironmentSupplier = checkNotNull(pProverEnvironmentSupplier);
    formulaManagerView = checkNotNull(pFormulaManagerView);
    pathFormulaManager = checkNotNull(pPathFormulaManager);

    ifThenElseElimination = new IfThenElseElimination(formulaManagerView, formulaManager);
    divAndModElimination = new DivAndModElimination(formulaManagerView, formulaManager);
    equalElimination = new EqualElimination(formulaManagerView);
    notEqualAndNotInequalityElimination =
        new NotEqualAndNotInequalityElimination(formulaManagerView);
    dnfTransformation = new DnfTransformation(formulaManagerView);
  }

  public Collection<Lasso> buildLasso(
      CounterexampleInfo pCounterexampleInfo, Set<CVariableDeclaration> pRelevantVariables)
      throws CPATransferException, InterruptedException, TermException, SolverException {
    StemAndLoop stemAndLoop = createStemAndLoop(pCounterexampleInfo);
    shutdownNotifier.shutdownIfNecessary();

    Set<String> relevantVariables =
        pRelevantVariables.stream().map(AVariableDeclaration::getQualifiedName).collect(toSet());
    return createLassos(stemAndLoop, relevantVariables);
  }

  private StemAndLoop createStemAndLoop(CounterexampleInfo pCounterexampleInfo)
      throws CPATransferException, InterruptedException {
    PathIterator path = pCounterexampleInfo.getTargetPath().fullPathIterator();

    List<CFAEdge> stemEdges = Lists.newArrayList();
    List<CFAEdge> loopEdges = Lists.newArrayList();
    boolean loopStarted = false;
    path.advance(); // the first state has no incoming edge

    while (path.advanceIfPossible()) {
      if (!loopStarted) {
        ARGState nextState = path.getNextAbstractState();
        TerminationState nextTerminationState =
            extractStateByType(nextState, TerminationState.class);

        if (path.isPositionWithState()) {
          ARGState state = path.getAbstractState();
          TerminationState terminationState = extractStateByType(state, TerminationState.class);
          loopStarted = nextTerminationState.isPartOfLoop() && !terminationState.isPartOfStem();

        } else {
          loopStarted = nextTerminationState.isPartOfLoop();
        }
      }

      if (loopStarted) {
        loopEdges.add(path.getIncomingEdge());
      } else {
        stemEdges.add(path.getIncomingEdge());
      }
    }

    PathFormula stemPathFormula = pathFormulaManager.makeFormulaForPath(stemEdges);
    PathFormula loopPathFormula = pathFormulaManager.makeEmptyPathFormula(stemPathFormula);
    SSAMapBuilder loopInVars = stemPathFormula.getSsa().builder();
    for (CFAEdge edge : loopEdges) {
      loopPathFormula = pathFormulaManager.makeAnd(loopPathFormula, edge);

      // update SSA index of input variables
      SSAMap currentSsa = loopPathFormula.getSsa();
      currentSsa
          .allVariables()
          .stream()
          .filter(v -> !loopInVars.allVariables().contains(v))
          .forEach(v -> loopInVars.setIndex(v, currentSsa.getType(v), currentSsa.getIndex(v)));
    }

    logger.logf(Level.FINE, "Stem formula %s", stemPathFormula.getFormula());
    logger.logf(Level.FINE, "Loop formula %s", loopPathFormula.getFormula());

    StemAndLoop stemAndLoop = new StemAndLoop(stemPathFormula, loopPathFormula, loopInVars.build());
    return stemAndLoop;
  }

  private Collection<Lasso> createLassos(
      StemAndLoop pStemAndLoop,
      Set<String> pRelevantVariables)
      throws InterruptedException, TermException, SolverException {
    Collection<BooleanFormula> stemDnf = toDnf(pStemAndLoop.getStem());
    Collection<BooleanFormula> loopDnf = toDnf(pStemAndLoop.getLoop());
    InOutVariables stemRankVars =
        extractRankVars(
            pStemAndLoop.getStem(),
            pStemAndLoop.getStemInVars(),
            pStemAndLoop.getStemOutVars(),
            pRelevantVariables);
    InOutVariables loopRankVars =
        extractRankVars(
            pStemAndLoop.getLoop(),
            pStemAndLoop.getLoopInVars(),
            pStemAndLoop.getLoopOutVars(),
            pRelevantVariables);

    ImmutableList.Builder<Lasso> lassos = ImmutableList.builder();
    for (BooleanFormula stem : stemDnf) {
      for (BooleanFormula loop : loopDnf) {
        shutdownNotifier.shutdownIfNecessary();

        BooleanFormula path = formulaManagerView.makeAnd(stem, loop);
        if (!isUnsat(path)) {

          LinearTransition stemTransition =
              createLinearTransition(
                  stem,
                  stemRankVars);
          LinearTransition loopTransition =
              createLinearTransition(
                  loop,
                  loopRankVars);

          Lasso lasso = new Lasso(stemTransition, loopTransition);
          lassos.add(lasso);
        }
      }
    }

    return lassos.build();
  }

  private boolean isUnsat(BooleanFormula formula) throws SolverException, InterruptedException {
    try (ProverEnvironment proverEnvironment = proverEnvironmentSupplier.get()) {
      proverEnvironment.push(formula);
      return proverEnvironment.isUnsat();
    }
  }

  private LinearTransition createLinearTransition(BooleanFormula path, InOutVariables rankVars)
      throws TermException {
    List<List<LinearInequality>> polyhedra = extractPolyhedra(path);
    return new LinearTransition(polyhedra, rankVars.getInVars(), rankVars.getOutVars());
  }

  private List<List<LinearInequality>> extractPolyhedra(BooleanFormula pathInDnf)
      throws TermException {
    Set<BooleanFormula> clauses =
        formulaManagerView.getBooleanFormulaManager().toDisjunctionArgs(pathInDnf, true);

    List<List<LinearInequality>> polyhedra = Lists.newArrayListWithCapacity(clauses.size());
    for (BooleanFormula clause : clauses) {
      Term term = formulaManager.extractInfo(clause);
      polyhedra.add(InequalityConverter.convert(term, EXCEPTION));
    }
    return polyhedra;
  }

  private Collection<BooleanFormula> toDnf(BooleanFormula path) throws InterruptedException {
    BooleanFormula simplified = formulaManagerView.simplify(path);
    BooleanFormula withoutIfThenElse = transformRecursively(ifThenElseElimination, simplified);
    BooleanFormula withoutDivAndMod = transformRecursively(divAndModElimination, withoutIfThenElse);
    BooleanFormula nnf = formulaManagerView.applyTactic(withoutDivAndMod, Tactic.NNF);
    BooleanFormula notEqualEliminated =
        transformRecursively(notEqualAndNotInequalityElimination, nnf);
    BooleanFormula equalEliminated = transformRecursively(equalElimination, notEqualEliminated);
    BooleanFormula dnf = transformRecursively(dnfTransformation, equalEliminated);
    Set<BooleanFormula> clauses =
        formulaManagerView.getBooleanFormulaManager().toDisjunctionArgs(dnf, true);

    return clauses;
  }

  private BooleanFormula transformRecursively(
      BooleanFormulaTransformationVisitor visitor, BooleanFormula formula)
      throws InterruptedException {
    shutdownNotifier.shutdownIfNecessary();
    return formulaManagerView.getBooleanFormulaManager().transformRecursively(formula, visitor);
  }

  private InOutVariables extractRankVars(
      BooleanFormula path, SSAMap inSsa, SSAMap outSsa, Set<String> pRelevantVariables) {
    InOutVariablesCollector veriablesCollector =
        new InOutVariablesCollector(formulaManagerView, inSsa, outSsa);
    formulaManagerView.visitRecursively(path, veriablesCollector);
    Map<RankVar, Term> inRankVars =
        createRankVars(veriablesCollector.getInVariables(), pRelevantVariables);
    Map<RankVar, Term> outRankVars =
        createRankVars(veriablesCollector.getOutVariables(), pRelevantVariables);
    return new InOutVariables(inRankVars, outRankVars);
  }

  private Map<RankVar, Term> createRankVars(
      Set<Formula> variables, Set<String> pRelevantVariables) {
    ImmutableMap.Builder<RankVar, Term> rankVars = ImmutableMap.builder();
    for (Formula variable : variables) {
      Term term = formulaManager.extractInfo(variable);
      Formula uninstantiatedVariable = formulaManagerView.uninstantiate(variable);
      Set<String> variableNames = formulaManagerView.extractVariableNames(uninstantiatedVariable);
      String variableName = Iterables.getOnlyElement(variableNames);

      if (pRelevantVariables.contains(variableName)) {
        rankVars.put(new TermRankVar(variableName, term), term);

      } else if (!META_VARIABLES.contains(variableName)
          && !variableName.startsWith(TERMINATION_AUX_VARS_PREFIX)) {
        logger.logf(FINE, "Ignoring variable %s during construction of lasso.", variableName);
      }
    }
    return rankVars.build();
  }

  private static class InOutVariables {

    private final Map<RankVar, Term> inVars;
    private final Map<RankVar, Term> outVars;

    public InOutVariables(Map<RankVar, Term> pInVars, Map<RankVar, Term> pOutVars) {
      inVars = checkNotNull(pInVars);
      outVars = checkNotNull(pOutVars);
    }

    public Map<RankVar, Term> getInVars() {
      return inVars;
    }

    public Map<RankVar, Term> getOutVars() {
      return outVars;
    }
  }

  private static class StemAndLoop {

    private final PathFormula stem;
    private final PathFormula loop;
    private final SSAMap loopInVars;

    public StemAndLoop(PathFormula pStem, PathFormula pLoop, SSAMap pLoopInVars) {
      stem =checkNotNull(pStem);
      loop =checkNotNull(pLoop);
      loopInVars =checkNotNull(pLoopInVars);
    }

    public BooleanFormula getStem() {
      return stem.getFormula();
    }

    public SSAMap getStemInVars() {
      return SSAMap.emptySSAMap();
    }

    public SSAMap getStemOutVars() {
      return stem.getSsa();
    }

    public BooleanFormula getLoop() {
      return loop.getFormula();
    }

    public SSAMap getLoopInVars() {
      return loopInVars;
    }

    public SSAMap getLoopOutVars() {
      return loop.getSsa();
    }
  }
}
