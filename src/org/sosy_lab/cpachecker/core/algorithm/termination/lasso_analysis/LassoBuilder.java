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
import static java.util.Collections.emptyMap;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractStateByType;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathIterator;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.termination.TerminationState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView.BooleanFormulaTransformationVisitor;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
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
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import de.uni_freiburg.informatik.ultimate.lassoranker.Lasso;
import de.uni_freiburg.informatik.ultimate.lassoranker.LinearInequality;
import de.uni_freiburg.informatik.ultimate.lassoranker.LinearTransition;
import de.uni_freiburg.informatik.ultimate.lassoranker.exceptions.TermException;
import de.uni_freiburg.informatik.ultimate.lassoranker.variables.InequalityConverter;
import de.uni_freiburg.informatik.ultimate.lassoranker.variables.RankVar;
import de.uni_freiburg.informatik.ultimate.lassoranker.variables.ReplacementVar;
import de.uni_freiburg.informatik.ultimate.logic.Term;

class LassoBuilder {

  private final static Splitter NAME_INDEX_SPLITTER = Splitter.on("@");

  private final LogManager logger;

  private final AbstractFormulaManager<Term, ?, ?, ?> formulaManager;
  private final FormulaManagerView formulaManagerView;
  private final PathFormulaManager pathFormulaManager;

  private final EqualElimination equalElimination;
  private final NotEqualElimination notEqualElimination;
  private final DnfTransformation dnfTransformation;

  LassoBuilder(
      LogManager pLogger,
      AbstractFormulaManager<Term, ?, ?, ?> pFormulaManager,
      FormulaManagerView pFormulaManagerView,
      PathFormulaManager pPathFormulaManager) {

    logger = checkNotNull(pLogger);
    formulaManager = checkNotNull(pFormulaManager);
    formulaManagerView = checkNotNull(pFormulaManagerView);
    pathFormulaManager = checkNotNull(pPathFormulaManager);
    equalElimination = new EqualElimination(formulaManagerView);
    notEqualElimination = new NotEqualElimination(formulaManagerView);
    dnfTransformation = new DnfTransformation(formulaManagerView);
  }

  public Lasso buildLasso(AbstractState targetState)
      throws CPATransferException, InterruptedException, TermException {
    ARGState argState = extractStateByType(targetState, ARGState.class);
    ARGState lastLassoState = argState.getParents().iterator().next();
    PathIterator path = ARGUtils.getOnePathTo(lastLassoState).fullPathIterator();

    List<CFAEdge> stemEdges = Lists.newArrayList();
    List<CFAEdge> loopEdges = Lists.newArrayList();
    boolean loopStarted = false;
    path.advance(); // the first state has no incoming edge

    while (path.hasNext()) {
      if (path.isPositionWithState()) {
        ARGState currentState = path.getAbstractState();
        TerminationState terminationState =
            extractStateByType(currentState, TerminationState.class);

        if (terminationState.isPartOfStem() && !loopStarted) {
          stemEdges.add(path.getIncomingEdge());

        } else if (terminationState.isPartOfLoop() && !loopStarted) {
          loopStarted = true;
          loopEdges.add(path.getIncomingEdge());

        } else if (terminationState.isPartOfLoop() && loopStarted) {
          loopEdges.add(path.getIncomingEdge());
        } else {
          throw new AssertionError();
        }

      } else {
        if (loopStarted) {
          loopEdges.add(path.getIncomingEdge());
        } else {
          stemEdges.add(path.getIncomingEdge());
        }
      }

      path.advance();
    }

    PathFormula stemPathFormula = pathFormulaManager.makeFormulaForPath(stemEdges);
    PathFormula loopPathFormula = pathFormulaManager.makeEmptyPathFormula(stemPathFormula);
    for (CFAEdge edge : loopEdges) {
      loopPathFormula = pathFormulaManager.makeAnd(loopPathFormula, edge);
    }

    logger.logf(Level.FINER, "Stem formula %s", stemPathFormula.getFormula());
    logger.logf(Level.FINER, "Loop formula %s", loopPathFormula.getFormula());

    LinearTransition stem = createLinearTransition(stemPathFormula);
    LinearTransition loop = createLinearTransition(loopPathFormula);
    Lasso lasso = new Lasso(stem, loop);
    return lasso;
  }

  private LinearTransition createLinearTransition(PathFormula path)
      throws InterruptedException, TermException {
    List<List<LinearInequality>> polyhedra = extractPolyhedra(path);
    Map<RankVar, Term> outVars = extractOutVars(path);
    Map<RankVar, Term> inVars = emptyMap();
    LinearTransition linearTransition = new LinearTransition(polyhedra, inVars, outVars);
    return linearTransition;
  }

  private List<List<LinearInequality>> extractPolyhedra(PathFormula path)
      throws InterruptedException, TermException {
    BooleanFormula simplified = formulaManagerView.simplify(path.getFormula());
    BooleanFormula nnf = formulaManagerView.applyTactic(simplified, Tactic.NNF);
    BooleanFormula notEqualEliminated = transformRecursively(notEqualElimination, nnf);
    BooleanFormula equalEliminated = transformRecursively(equalElimination, notEqualEliminated);
    BooleanFormula dnf = transformRecursively(dnfTransformation, equalEliminated);
    Set<BooleanFormula> clauses =
        formulaManagerView.getBooleanFormulaManager().toDisjunctionArgs(dnf, true);

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

  private Map<RankVar, Term> extractOutVars(PathFormula path) {
    VeriablesCollector outVeriablesCollector = new VeriablesCollector(path.getSsa());
    formulaManagerView.visitRecursively(outVeriablesCollector, path.getFormula());

    ImmutableMap.Builder<RankVar, Term> outVars = ImmutableMap.builder();
    for (Formula variable : outVeriablesCollector.getVariables()) {
      Term term = formulaManager.extractInfo(variable);
      outVars.put(new ReplacementVar(term.toString(), term), term);
    }

    return outVars.build();
  }

  private static class NotEqualElimination extends BooleanFormulaTransformationVisitor {

    private final FormulaManagerView fmgr;

    private final StrictInequalityTransformation strictInequalityTransformation;

    private NotEqualElimination(FormulaManagerView pFmgr) {
      super(pFmgr);
      fmgr = pFmgr;
      strictInequalityTransformation = new StrictInequalityTransformation(pFmgr);
    }

    @Override
    public BooleanFormula visitNot(BooleanFormula pOperand) {
      List<BooleanFormula> split = fmgr.splitNumeralEqualityIfPossible(pOperand);

      // Pattern matching on (NOT (= A B)).
      if (split.size() == 2) {
        return fmgr.makeOr(
            fmgr.visit(strictInequalityTransformation, split.get(0)),
            fmgr.visit(strictInequalityTransformation, split.get(1)));
      }
      return super.visitNot(pOperand);
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

  private static class VeriablesCollector extends DefaultFormulaVisitor<TraversalProcess> {

    private final Set<Formula> variables = Sets.newLinkedHashSet();
    private final SSAMap ssa;

    public VeriablesCollector(SSAMap pSsa) {
      ssa = pSsa;
    }

    @Override
    protected TraversalProcess visitDefault(Formula pF) {
      return TraversalProcess.CONTINUE;
    }

    @Override
    public TraversalProcess visitFreeVariable(Formula pF, String pName) {
      List<String> tokens = NAME_INDEX_SPLITTER.splitToList(pName);
      String name = tokens.get(0);
      String index = tokens.get(1);
      if (Integer.toString(ssa.getIndex(name)).equals(index)) {
        variables.add(pF);
      }
      return TraversalProcess.CONTINUE;
    }

    public Set<Formula> getVariables() {
      return variables;
    }
  }
}
