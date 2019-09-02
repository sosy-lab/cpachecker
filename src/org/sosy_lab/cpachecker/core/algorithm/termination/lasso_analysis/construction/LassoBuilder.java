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
package org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.construction;

import static com.google.common.base.Preconditions.checkNotNull;
import static de.uni_freiburg.informatik.ultimate.lassoranker.variables.InequalityConverter.NlaHandling.EXCEPTION;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.FINER;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractStateByType;

import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import de.uni_freiburg.informatik.ultimate.lassoranker.Lasso;
import de.uni_freiburg.informatik.ultimate.lassoranker.LinearInequality;
import de.uni_freiburg.informatik.ultimate.lassoranker.LinearTransition;
import de.uni_freiburg.informatik.ultimate.lassoranker.exceptions.TermException;
import de.uni_freiburg.informatik.ultimate.lassoranker.variables.InequalityConverter;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.variables.IProgramVar;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.LassoAnalysisStatistics;
import org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.RankVar;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.PathIterator;
import org.sosy_lab.cpachecker.cpa.termination.TerminationState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView.BooleanFormulaTransformationVisitor;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverException;
import org.sosy_lab.java_smt.api.Tactic;
import org.sosy_lab.java_smt.basicimpl.AbstractFormulaManager;
import org.sosy_lab.java_smt.utils.SolverUtils;
import org.sosy_lab.java_smt.utils.UfElimination;
import org.sosy_lab.java_smt.utils.UfElimination.Result;

/**
 * Creates {@link Lasso}s from {@link CounterexampleInfo}.
 */
@Options(prefix = "termination.lassoBuilder")
public class LassoBuilder {

  protected static final ImmutableSet<String> META_VARIABLES_PREFIX =
      ImmutableSet.of("__VERIFIER_nondet_", "__ADDRESS_OF_");

  final static String TERMINATION_AUX_VARS_PREFIX = "__TERMINATION-";

  final static String TERMINATION_REPLACE_VARS_PREFIX = "__TERMINATION_REPLACE-";

  @Option(secure = true, description = "Simplifies loop and stem formulas.")
  private boolean simplify = false;

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;

  private final AbstractFormulaManager<Term, ?, ?, ?> fmgr;
  private final Supplier<ProverEnvironment> proverEnvironmentSupplier;
  private final FormulaManagerView fmgrView;
  private final BooleanFormulaManagerView bfmrView;
  private final PathFormulaManager pathFormulaManager;

  private final DivAndModElimination divAndModElimination;
  private final NonLinearMultiplicationElimination nonLinearMultiplicationElimination;
  private final UfElimination ufElimination;
  private final IfThenElseElimination ifThenElseElimination;
  private final EqualElimination equalElimination;
  private final NotEqualAndNotInequalityElimination notEqualAndNotInequalityElimination;

  private final LassoAnalysisStatistics stats;

  public LassoBuilder(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      AbstractFormulaManager<Term, ?, ?, ?> pFormulaManager,
      FormulaManagerView pFormulaManagerView,
      Supplier<ProverEnvironment> pProverEnvironmentSupplier,
      PathFormulaManager pPathFormulaManager,
      final LassoAnalysisStatistics pLassoAnalysisStats)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    logger = checkNotNull(pLogger);
    shutdownNotifier = checkNotNull(pShutdownNotifier);
    fmgr = checkNotNull(pFormulaManager);
    proverEnvironmentSupplier = checkNotNull(pProverEnvironmentSupplier);
    fmgrView = checkNotNull(pFormulaManagerView);
    bfmrView = fmgrView.getBooleanFormulaManager();
    pathFormulaManager = checkNotNull(pPathFormulaManager);

    divAndModElimination = new DivAndModElimination(fmgrView, fmgr);
    nonLinearMultiplicationElimination = new NonLinearMultiplicationElimination(fmgrView, fmgr);
    ufElimination = SolverUtils.ufElimination(pFormulaManager);
    ifThenElseElimination = new IfThenElseElimination(fmgrView, fmgr);
    equalElimination = new EqualElimination(fmgrView);
    notEqualAndNotInequalityElimination = new NotEqualAndNotInequalityElimination(fmgrView);

    stats = pLassoAnalysisStats;
  }

  protected static boolean isMetaVariable(String variableName) {
    return META_VARIABLES_PREFIX.stream().anyMatch(variableName::startsWith);
  }

  public Collection<Lasso> buildLasso(
      CounterexampleInfo pCounterexampleInfo, Set<CVariableDeclaration> pRelevantVariables)
      throws CPATransferException, InterruptedException, TermException, SolverException {

    stats.stemAndLoopConstructionStarted();
    StemAndLoop stemAndLoop = createStemAndLoop(pCounterexampleInfo);
    shutdownNotifier.shutdownIfNecessary();
    stats.stemAndLoopConstructionFinished();

    ImmutableMap<String, CVariableDeclaration> relevantVariables =
        Maps.uniqueIndex(pRelevantVariables, AVariableDeclaration::getQualifiedName);
    try {
      stats.lassosCreationStarted();
    return createLassos(stemAndLoop, relevantVariables);
    } finally {
      stats.lassosCreationFinished();
    }
  }

  private StemAndLoop createStemAndLoop(CounterexampleInfo pCounterexampleInfo)
      throws CPATransferException, InterruptedException {
    PathIterator path = pCounterexampleInfo.getTargetPath().fullPathIterator();

    List<CFAEdge> stemEdges = new ArrayList<>();
    List<CFAEdge> loopEdges = new ArrayList<>();
    boolean loopStarted = false;
    path.advance(); // the first state has no incoming edge

    while (path.advanceIfPossible()) {
      if (!loopStarted) {
        if (path.hasNext()) {
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

        } else { // last state of the lasso has to be a loop state
          TerminationState state =
              extractStateByType(path.getAbstractState(), TerminationState.class);
          Verify.verify(state.isPartOfLoop());
          loopStarted = true;
        }
      }

      if (loopStarted) {
        loopEdges.add(path.getIncomingEdge());
      } else {
        stemEdges.add(path.getIncomingEdge());
      }
    }

    return createStemAndLoop(stemEdges, loopEdges);
  }

  public StemAndLoop createStemAndLoop(List<CFAEdge> stemEdges, List<CFAEdge> loopEdges)
      throws CPATransferException, InterruptedException {
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

    return new StemAndLoop(stemPathFormula, loopPathFormula, loopInVars.build());
  }

  private Collection<Lasso> createLassos(
      StemAndLoop pStemAndLoop, ImmutableMap<String, CVariableDeclaration> pRelevantVariables)
      throws InterruptedException, TermException, SolverException {
    Dnf stemDnf = toDnf(pStemAndLoop.getStem(), Result.empty(fmgr));
    Dnf loopDnf = toDnf(pStemAndLoop.getLoop(), stemDnf.getUfEliminationResult());
    return createLassos(pStemAndLoop, stemDnf, loopDnf, pRelevantVariables, true);
  }

  public Collection<Lasso> createLassos(
      StemAndLoop pStemAndLoop,
      Dnf stemDnf,
      Dnf loopDnf,
      ImmutableMap<String, CVariableDeclaration> pRelevantVariables,
      boolean checkSat)
      throws InterruptedException, TermException, SolverException {
    InOutVariables stemRankVars =
        extractRankVars(
            stemDnf,
            pStemAndLoop.getStemInVars(),
            pStemAndLoop.getStemOutVars(),
            pRelevantVariables);
    InOutVariables loopRankVars =
        extractRankVars(
            loopDnf,
            pStemAndLoop.getLoopInVars(),
            pStemAndLoop.getLoopOutVars(),
            pRelevantVariables);

    ImmutableList.Builder<Lasso> lassos = ImmutableList.builder();
    for (BooleanFormula stem : stemDnf.getClauses()) {
      for (BooleanFormula loop : loopDnf.getClauses()) {

        shutdownNotifier.shutdownIfNecessary();
        if (checkSat && isUnsat(bfmrView.and(stem, loop))) {
          continue;
        }

        LinearTransition stemTransition = createLinearTransition(stem, stemRankVars);
        LinearTransition loopTransition = createLinearTransition(loop, loopRankVars);

        Lasso lasso = new Lasso(stemTransition, loopTransition);
        lassos.add(lasso);
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
    Set<BooleanFormula> clauses = bfmrView.toDisjunctionArgs(pathInDnf, true);

    List<List<LinearInequality>> polyhedra = new ArrayList<>(clauses.size());
    for (BooleanFormula clause : clauses) {
      Term term = fmgr.extractInfo(clause);
      polyhedra.add(InequalityConverter.convert(term, EXCEPTION));
    }
    return polyhedra;
  }

  public Dnf toDnf(BooleanFormula pFormula) throws InterruptedException, SolverException {
    return toDnf(pFormula, Result.empty(fmgr));
  }

  public Dnf toDnf(BooleanFormula pFormula, UfElimination.Result eliminatedUfs)
      throws InterruptedException, SolverException {

    BooleanFormula simplified;
    if (simplify) {
      simplified = fmgrView.simplify(pFormula);
    } else {
      simplified = pFormula;
    }

    BooleanFormula withoutDivAndMod = transformRecursively(divAndModElimination, simplified);
    BooleanFormula withoutNonLinearMult =
        transformRecursively(nonLinearMultiplicationElimination, withoutDivAndMod);
    Result ufEliminationResult = ufElimination.eliminateUfs(withoutNonLinearMult, eliminatedUfs);
    BooleanFormula withoutUfs =
        bfmrView.and(ufEliminationResult.getFormula(), ufEliminationResult.getConstraints());
    Map<Formula, Formula> ufSubstitution = ufEliminationResult.getSubstitution();
    logger.logf(FINER, "Substitution of Ufs in lasso formula: %s", ufSubstitution);

    BooleanFormula withoutIfThenElse = transformRecursively(ifThenElseElimination, withoutUfs);
    BooleanFormula nnf = fmgrView.applyTactic(withoutIfThenElse, Tactic.NNF);
    BooleanFormula notEqualEliminated =
        transformRecursively(notEqualAndNotInequalityElimination, nnf);
    BooleanFormula equalEliminated = transformRecursively(equalElimination, notEqualEliminated);
    BooleanFormula dnf =
        DnfTransformation.transformToDnf(
            equalEliminated, fmgrView, shutdownNotifier, proverEnvironmentSupplier);
    ImmutableSet<BooleanFormula> clauses =
        ImmutableSet.copyOf(bfmrView.toDisjunctionArgs(dnf, true));

    return new Dnf(clauses, ufEliminationResult);
  }

  private BooleanFormula transformRecursively(
      BooleanFormulaTransformationVisitor visitor, BooleanFormula formula)
      throws InterruptedException {
    shutdownNotifier.shutdownIfNecessary();
    return fmgrView.getBooleanFormulaManager().transformRecursively(formula, visitor);
  }

  private InOutVariables extractRankVars(
      Dnf pDnf,
      SSAMap inSsa,
      SSAMap outSsa,
      ImmutableMap<String, CVariableDeclaration> pRelevantVariables) {
    ImmutableMap<Formula, Formula> substitution =
        ImmutableMap.copyOf(pDnf.getUfEliminationResult().getSubstitution());
    InOutVariablesCollector variablesCollector =
        new InOutVariablesCollector(
            fmgrView, inSsa, outSsa, pRelevantVariables.keySet(), substitution);
    fmgrView.visitRecursively(pDnf.getUfEliminationResult().getFormula(), variablesCollector);

    ImmutableMap<RankVar, Term> inRankVars =
        createRankVars(variablesCollector.getInVariables(), pRelevantVariables, substitution);
    ImmutableMap<RankVar, Term> outRankVars =
        createRankVars(variablesCollector.getOutVariables(), pRelevantVariables, substitution);

    return new InOutVariables(inRankVars, outRankVars);
  }

  private ImmutableMap<RankVar, Term> createRankVars(
      Set<Formula> variables,
      Map<String, CVariableDeclaration> pRelevantVariables,
      Map<Formula, Formula> substitution) {
    ImmutableMap.Builder<RankVar, Term> rankVars = ImmutableMap.builder();
    for (Formula variable : variables) {
      Term term = fmgr.extractInfo(variable);
      Formula uninstantiatedVariable = fmgrView.uninstantiate(variable);
      Set<String> variableNames = fmgrView.extractVariableNames(uninstantiatedVariable);
      String variableName = Iterables.getOnlyElement(variableNames);

      if (pRelevantVariables.get(variableName) != null) {
        rankVars.put(
            new RankVar(
                variableName,
                pRelevantVariables.get(variableName).isGlobal(),
                fmgr.extractInfo(uninstantiatedVariable)),
            term);

      } else if (substitution.containsValue(variable)) {
        Formula originalFormula =
            substitution
                .entrySet()
                .stream()
                .filter(e -> e.getValue().equals(variable))
                .map(Entry::getKey)
                .findAny()
                .get();
        Formula uninstantiatedOriginalFormula = fmgrView.uninstantiate(originalFormula);
        Term originalTerm = fmgr.extractInfo(uninstantiatedOriginalFormula);
        rankVars.put(new RankVar(originalTerm.toString(), true, originalTerm), term);

      } else if (!isMetaVariable(variableName)
          && !variableName.startsWith(TERMINATION_AUX_VARS_PREFIX)) {
        logger.logf(FINE, "Ignoring variable %s during construction of lasso.", variableName);
      }
    }
    return rankVars.build();
  }

  private static class InOutVariables {

    private final ImmutableMap<RankVar, Term> inVars;
    private final ImmutableMap<RankVar, Term> outVars;

    public InOutVariables(
        ImmutableMap<RankVar, Term> pInVars, ImmutableMap<RankVar, Term> pOutVars) {
      inVars = checkNotNull(pInVars);
      outVars = checkNotNull(pOutVars);
    }

    public ImmutableMap<IProgramVar, Term> getInVars() {
      return ImmutableMap.copyOf(inVars);
    }

    public ImmutableMap<IProgramVar, Term> getOutVars() {
      return ImmutableMap.copyOf(outVars);
    }
  }

  public static class StemAndLoop {

    private final PathFormula stem;
    private final PathFormula loop;
    private final SSAMap loopInVars;

    public StemAndLoop(PathFormula pStem, PathFormula pLoop, SSAMap pLoopInVars) {
      stem = checkNotNull(pStem);
      loop = checkNotNull(pLoop);
      loopInVars = checkNotNull(pLoopInVars);
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

  public static class Dnf {

    private final ImmutableSet<BooleanFormula> clauses;
    private final Result ufEliminationResult;

    Dnf(ImmutableSet<BooleanFormula> pClauses, Result p) {
      clauses = checkNotNull(pClauses);
      ufEliminationResult = checkNotNull(p);
    }

    public ImmutableSet<BooleanFormula> getClauses() {
      return clauses;
    }

    public Result getUfEliminationResult() {
      return ufEliminationResult;
    }
  }
}
