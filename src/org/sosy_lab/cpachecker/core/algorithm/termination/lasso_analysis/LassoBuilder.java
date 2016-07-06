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
package org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis;

import static com.google.common.base.Preconditions.checkNotNull;
import static de.uni_freiburg.informatik.ultimate.lassoranker.variables.InequalityConverter.NlaHandling.EXCEPTION;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractStateByType;
import static org.sosy_lab.solver.api.FunctionDeclarationKind.EQ;
import static org.sosy_lab.solver.api.FunctionDeclarationKind.GT;
import static org.sosy_lab.solver.api.FunctionDeclarationKind.GTE;
import static org.sosy_lab.solver.api.FunctionDeclarationKind.LT;
import static org.sosy_lab.solver.api.FunctionDeclarationKind.LTE;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathIterator;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.termination.TerminationState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView.BooleanFormulaTransformationVisitor;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.solver.SolverException;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.BooleanFormulaManager;
import org.sosy_lab.solver.api.Formula;
import org.sosy_lab.solver.api.FunctionDeclaration;
import org.sosy_lab.solver.api.FunctionDeclarationKind;
import org.sosy_lab.solver.basicimpl.AbstractFormulaManager;
import org.sosy_lab.solver.basicimpl.tactics.Tactic;
import org.sosy_lab.solver.visitors.DefaultFormulaVisitor;
import org.sosy_lab.solver.visitors.TraversalProcess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.stream.Collectors;

import de.uni_freiburg.informatik.ultimate.lassoranker.Lasso;
import de.uni_freiburg.informatik.ultimate.lassoranker.LinearInequality;
import de.uni_freiburg.informatik.ultimate.lassoranker.LinearTransition;
import de.uni_freiburg.informatik.ultimate.lassoranker.exceptions.TermException;
import de.uni_freiburg.informatik.ultimate.lassoranker.variables.InequalityConverter;
import de.uni_freiburg.informatik.ultimate.lassoranker.variables.RankVar;
import de.uni_freiburg.informatik.ultimate.logic.Term;

/**
 * Creates {@link Lasso}s from {@link CounterexampleInfo}.
 */
class LassoBuilder {

  private final static Set<String> META_VARIABLES = ImmutableSet.of("__VERIFIER_nondet_int");

  private final static Set<FunctionDeclarationKind> IF_THEN_ELSE_FUNCTIONS =
      ImmutableSet.of(EQ, GT, GTE, LT, LTE);

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;

  private final AbstractFormulaManager<Term, ?, ?, ?> formulaManager;
  private final Solver solver;
  private final FormulaManagerView formulaManagerView;
  private final PathFormulaManager pathFormulaManager;

  private final IfThenElseElimination ifThenElseElimination;
  private final EqualElimination equalElimination;
  private final NotEqualElimination notEqualElimination;
  private final DnfTransformation dnfTransformation;

  LassoBuilder(
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      AbstractFormulaManager<Term, ?, ?, ?> pFormulaManager,
      Solver pSolver,
      PathFormulaManager pPathFormulaManager) {
    logger = checkNotNull(pLogger);
    shutdownNotifier = checkNotNull(pShutdownNotifier);
    formulaManager = checkNotNull(pFormulaManager);
    solver = checkNotNull(pSolver);
    formulaManagerView = pSolver.getFormulaManager();
    pathFormulaManager = checkNotNull(pPathFormulaManager);

    ifThenElseElimination = new IfThenElseElimination(formulaManagerView);
    equalElimination = new EqualElimination(formulaManagerView);
    notEqualElimination = new NotEqualElimination(formulaManagerView);
    dnfTransformation = new DnfTransformation(formulaManagerView);
  }

  public Collection<Lasso> buildLasso(CounterexampleInfo pCounterexampleInfo)
      throws CPATransferException, InterruptedException, TermException, SolverException {
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
    shutdownNotifier.shutdownIfNecessary();

    return createLassos(stemPathFormula, loopPathFormula, loopInVars.build());
  }

  private Collection<Lasso> createLassos(
      PathFormula stemPathFormula, PathFormula loopPathFormula, SSAMap pLoopInVars)
      throws InterruptedException, TermException, SolverException {
    Collection<BooleanFormula> stemDnf = toDnf(stemPathFormula);
    Collection<BooleanFormula> loopDnf = toDnf(loopPathFormula);

    Collection<Lasso> lassos = Lists.newArrayListWithCapacity(stemDnf.size() * loopDnf.size());
    for (BooleanFormula stem : stemDnf) {
      for (BooleanFormula loop : loopDnf) {
        shutdownNotifier.shutdownIfNecessary();

        BooleanFormula path = formulaManagerView.makeAnd(stem, loop);
        if (!solver.isUnsat(path)) {

          LinearTransition stemTransition =
              createLinearTransition(stem, SSAMap.emptySSAMap(), stemPathFormula.getSsa());
          LinearTransition loopTransition =
              createLinearTransition(loop, pLoopInVars, loopPathFormula.getSsa());

          Lasso lasso = new Lasso(stemTransition, loopTransition);
          lassos.add(lasso);
        }
      }
    }

    return lassos;
  }

  private LinearTransition createLinearTransition(
      BooleanFormula path, SSAMap inSsa, SSAMap outSSa)
      throws TermException {
    List<List<LinearInequality>> polyhedra = extractPolyhedra(path);
    InOutVariables rankVars = extractRankVars(path, inSsa, outSSa);
    return new LinearTransition(polyhedra, rankVars.getInVars(), rankVars.getOutVars());
  }

  private Collection<BooleanFormula> toDnf(PathFormula path) throws InterruptedException {
    BooleanFormula simplified = formulaManagerView.simplify(path.getFormula());
    BooleanFormula withoutIfThenElse = transformRecursively(ifThenElseElimination, simplified);
    BooleanFormula nnf = formulaManagerView.applyTactic(withoutIfThenElse, Tactic.NNF);
    BooleanFormula notEqualEliminated = transformRecursively(notEqualElimination, nnf);
    BooleanFormula equalEliminated = transformRecursively(equalElimination, notEqualEliminated);
    BooleanFormula dnf = transformRecursively(dnfTransformation, equalEliminated);
    Set<BooleanFormula> clauses =
        formulaManagerView.getBooleanFormulaManager().toDisjunctionArgs(dnf, true);

    return clauses;
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

  private BooleanFormula transformRecursively(
      BooleanFormulaTransformationVisitor visitor, BooleanFormula formula) {
    return formulaManagerView.getBooleanFormulaManager().transformRecursively(visitor, formula);
  }

  private InOutVariables extractRankVars(BooleanFormula path, SSAMap inSsa, SSAMap outSsa) {
    InOutVariablesCollector veriablesCollector =
        new InOutVariablesCollector(formulaManagerView, inSsa, outSsa);
    formulaManagerView.visitRecursively(veriablesCollector, path);
    Map<RankVar, Term> inRankVars = createRankVars(veriablesCollector.getInVariables());
    Map<RankVar, Term> outRankVars = createRankVars(veriablesCollector.getOutVariables());
    return new InOutVariables(inRankVars, outRankVars);
  }

  private Map<RankVar, Term> createRankVars(Set<Formula> variables) {
    ImmutableMap.Builder<RankVar, Term> rankVars = ImmutableMap.builder();
    for (Formula variable : variables) {
      Term term = formulaManager.extractInfo(variable);
      Formula uninstantiatedVariable = formulaManagerView.uninstantiate(variable);
      Set<String> variableNames = formulaManagerView.extractVariableNames(uninstantiatedVariable);
      String variableName = Iterables.getOnlyElement(variableNames);

      if (!META_VARIABLES.contains(variableName)) {
        rankVars.put(new TermRankVar(variableName, term), term);
      }
    }
    return rankVars.build();
  }

  private static class IfThenElseElimination extends BooleanFormulaTransformationVisitor {

    private final FormulaManagerView fmgr;

    private final IfThenElseTransformation IfThenElseTransformation;

    private IfThenElseElimination(FormulaManagerView pFmgr) {
      super(pFmgr);
      fmgr = pFmgr;
      IfThenElseTransformation = new IfThenElseTransformation(pFmgr);
    }

    @Override
    public BooleanFormula visitAtom(
        BooleanFormula pAtom, FunctionDeclaration<BooleanFormula> pDecl) {
      if (IF_THEN_ELSE_FUNCTIONS.contains(pDecl.getKind())) {
        return fmgr.visit(IfThenElseTransformation, pAtom);
      } else {
        return pAtom;
      }
    }

    private static class IfThenElseTransformation extends DefaultFormulaVisitor<BooleanFormula> {

      private final FormulaManagerView fmgr;

      private IfThenElseTransformation(FormulaManagerView pFmgr) {
        fmgr = pFmgr;
      }

      @Override
      protected BooleanFormula visitDefault(Formula pF) {
        return (BooleanFormula) pF;
      }

      @Override
      public BooleanFormula visitFunction(
          Formula pF, List<Formula> pArgs, FunctionDeclaration<?> pFunctionDeclaration) {

        FunctionDeclarationKind kind = pFunctionDeclaration.getKind();
        if (IF_THEN_ELSE_FUNCTIONS.contains(kind)) {

          Optional<Triple<BooleanFormula, Formula, Formula>> ifThenElse =
              fmgr.splitIfThenElse(pArgs.get(1));

          // right hand side is if-then-else
          if (ifThenElse.isPresent()) {
            return transformIfThenElse(getFunction(kind, false), pArgs.get(0), ifThenElse);

          } else { // check left hand side
            ifThenElse = fmgr.splitIfThenElse(pArgs.get(0));

            // left hand side is if-then-else
            if (ifThenElse.isPresent()) {
              return transformIfThenElse(getFunction(kind, false), pArgs.get(1), ifThenElse);
            }
          }
        }

        return (BooleanFormula) pF;
      }

      private BooleanFormula transformIfThenElse(
          BiFunction<Formula, Formula, BooleanFormula> function,
          Formula otherArg,
          Optional<Triple<BooleanFormula, Formula, Formula>> ifThenElse) {

        BooleanFormula condition = ifThenElse.get().getFirst();
        Formula thenFomula = ifThenElse.get().getSecond();
        Formula elseFomula = ifThenElse.get().getThird();
        return fmgr.makeOr(
            fmgr.makeAnd(function.apply(otherArg, thenFomula), condition),
            fmgr.makeAnd(function.apply(otherArg, elseFomula), fmgr.makeNot(condition)));
      }

      private BiFunction<Formula, Formula, BooleanFormula> getFunction(
          FunctionDeclarationKind functionKind, boolean swapArguments) {
        BiFunction<Formula, Formula, BooleanFormula> baseFunction;
        switch (functionKind) {
          case EQ:
            baseFunction = fmgr::makeEqual;
            break;
          case GT:
            baseFunction = (f1, f2) -> fmgr.makeLessOrEqual(f1, f2, true);
            break;
          case GTE:
            baseFunction = (f1, f2) -> fmgr.makeLessOrEqual(f1, f2, true);
            break;
          case LT:
            baseFunction = (f1, f2) -> fmgr.makeLessOrEqual(f1, f2, true);
            break;
          case LTE:
            baseFunction = (f1, f2) -> fmgr.makeGreaterThan(f1, f2, true);
            break;

          default:
            throw new AssertionError();
        }

        BiFunction<Formula, Formula, BooleanFormula> function;
        if (swapArguments) {
          function = (f1, f2) -> baseFunction.apply(f2, f1);
        } else {
          function = baseFunction;
        }
        return function;
      }
    }
  }

  private static class NotEqualElimination extends BooleanFormulaTransformationVisitor {

    private final FormulaManagerView fmgr;

    private final StrictInequalityTransformation strictInequalityTransformation;
    private final InvertInequalityTransformation invertInequalityTransformation;

    private NotEqualElimination(FormulaManagerView pFmgr) {
      super(pFmgr);
      fmgr = pFmgr;
      strictInequalityTransformation = new StrictInequalityTransformation(pFmgr);
      invertInequalityTransformation = new InvertInequalityTransformation(pFmgr);
    }

    @Override
    public BooleanFormula visitNot(BooleanFormula pOperand) {
      List<BooleanFormula> split = fmgr.splitNumeralEqualityIfPossible(pOperand);

      // Pattern matching on (NOT (= A B)).
      if (split.size() == 2) {
        return fmgr.makeOr(
            fmgr.visit(strictInequalityTransformation, split.get(0)),
            fmgr.visit(strictInequalityTransformation, split.get(1)));

        // handle <,<=, >, >=
      } else {
        return fmgr.visit(invertInequalityTransformation, pOperand);
      }
    }

    private static class StrictInequalityTransformation
        extends DefaultFormulaVisitor<BooleanFormula> {

      private final FormulaManagerView fmgr;

      private StrictInequalityTransformation(FormulaManagerView pFmgr) {
        fmgr = pFmgr;
      }

      @Override
      protected BooleanFormula visitDefault(Formula pF) {
        return (BooleanFormula) pF;
      }

      @Override
      public BooleanFormula visitFunction(
          Formula pF, List<Formula> pNewArgs, FunctionDeclaration<?> pFunctionDeclaration) {

        if (pFunctionDeclaration.getKind().equals(FunctionDeclarationKind.GTE)
            || pFunctionDeclaration.getName().equals(">=")) {
          assert pNewArgs.size() == 2;
          return fmgr.makeGreaterThan(pNewArgs.get(0), pNewArgs.get(1), true);

        } else if (pFunctionDeclaration.getKind().equals(FunctionDeclarationKind.LTE)
            || pFunctionDeclaration.getName().equals("<=")) {
          assert pNewArgs.size() == 2;
          return fmgr.makeLessThan(pNewArgs.get(0), pNewArgs.get(1), true);

        } else {
          return super.visitFunction(pF, pNewArgs, pFunctionDeclaration);
        }
      }
    }

    private static class InvertInequalityTransformation
        extends DefaultFormulaVisitor<BooleanFormula> {

      private final FormulaManagerView fmgr;

      private InvertInequalityTransformation(FormulaManagerView pFmgr) {
        fmgr = pFmgr;
      }

      @Override
      protected BooleanFormula visitDefault(Formula pF) {
        return (BooleanFormula) pF;
      }

      @Override
      public BooleanFormula visitFunction(
          Formula pF, List<Formula> pNewArgs, FunctionDeclaration<?> pFunctionDeclaration) {

        if (pFunctionDeclaration.getKind().equals(FunctionDeclarationKind.GTE)
            || pFunctionDeclaration.getName().equals(">=")) {
          assert pNewArgs.size() == 2;
          return fmgr.makeLessThan(pNewArgs.get(0), pNewArgs.get(1), true);

        } else if (pFunctionDeclaration.getKind().equals(FunctionDeclarationKind.LTE)
            || pFunctionDeclaration.getName().equals("<=")) {
          assert pNewArgs.size() == 2;
          return fmgr.makeGreaterThan(pNewArgs.get(0), pNewArgs.get(1), true);

        } else if (pFunctionDeclaration.getKind().equals(FunctionDeclarationKind.GT)
            || pFunctionDeclaration.getName().equals(">")) {
          assert pNewArgs.size() == 2;
          return fmgr.makeLessOrEqual(pNewArgs.get(0), pNewArgs.get(1), true);

        } else if (pFunctionDeclaration.getKind().equals(FunctionDeclarationKind.LT)
            || pFunctionDeclaration.getName().equals("<")) {
          assert pNewArgs.size() == 2;
          return fmgr.makeGreaterOrEqual(pNewArgs.get(0), pNewArgs.get(1), true);

        } else {
          return super.visitFunction(pF, pNewArgs, pFunctionDeclaration);
        }
      }
    }
  }

  private static class EqualElimination extends BooleanFormulaTransformationVisitor {

    private final FormulaManagerView fmgr;

    private EqualElimination(FormulaManagerView pFmgr) {
      super(pFmgr);
      fmgr = pFmgr;
    }

    @Override
    public BooleanFormula visitAtom(
        BooleanFormula pAtom, FunctionDeclaration<BooleanFormula> pDecl) {
      if (pDecl.getKind().equals(FunctionDeclarationKind.EQ)) {
        List<BooleanFormula> split = fmgr.splitNumeralEqualityIfPossible(pAtom);

        if (split.size() == 1) {
          return split.get(0);

        } else if (split.size() == 2) {
          return fmgr.makeAnd(split.get(0), split.get(1));

        } else {
          throw new AssertionError();
        }

      } else {
        return super.visitAtom(pAtom, pDecl);
      }
    }
  }

  private static class DnfTransformation extends BooleanFormulaTransformationVisitor {

    private final BooleanFormulaManager fmgr;

    private DnfTransformation(FormulaManagerView pFmgr) {
      super(pFmgr);
      fmgr = pFmgr.getBooleanFormulaManager();
    }

    @Override
    public BooleanFormula visitAnd(List<BooleanFormula> pProcessedOperands) {
      Collection<BooleanFormula> clauses = Lists.newArrayList(fmgr.makeBoolean(true));

      for (BooleanFormula operands : pProcessedOperands) {
        Set<BooleanFormula> childOperators = fmgr.toDisjunctionArgs(operands, false);
        clauses =
            clauses
                .stream()
                .flatMap(c -> childOperators.stream().map(co -> fmgr.and(c, co)))
                .collect(Collectors.toCollection(ArrayList::new));
      }

      return fmgr.or(clauses);
    }
  }

  private static class InOutVariablesCollector extends DefaultFormulaVisitor<TraversalProcess> {

    private final FormulaManagerView formulaManagerView;

    private final Set<Formula> inVariables = Sets.newLinkedHashSet();
    private final Set<Formula> outVariables = Sets.newLinkedHashSet();
    private final SSAMap outVariablesSsa;
    private final SSAMap inVariablesSsa;

    public InOutVariablesCollector(
        FormulaManagerView pFormulaManagerView, SSAMap pInVariablesSsa, SSAMap pOutVariablesSsa) {
      formulaManagerView = pFormulaManagerView;
      outVariablesSsa = pOutVariablesSsa;
      inVariablesSsa = pInVariablesSsa;
    }

    @Override
    protected TraversalProcess visitDefault(Formula pF) {
      return TraversalProcess.CONTINUE;
    }

    @Override
    public TraversalProcess visitFreeVariable(Formula pF, String pName) {
      if (!formulaManagerView.isIntermediate(pName, outVariablesSsa)) {
        outVariables.add(pF);
      }
      if (!formulaManagerView.isIntermediate(pName, inVariablesSsa)) {
        inVariables.add(pF);
      }

      return TraversalProcess.CONTINUE;
    }

    public Set<Formula> getInVariables() {
      return inVariables;
    }

    public Set<Formula> getOutVariables() {
      return outVariables;
    }
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
}
