package org.sosy_lab.cpachecker.cpa.policyiteration;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.sosy_lab.common.UniqueIdGenerator;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.NumeralFormulaManagerView;
import org.sosy_lab.solver.AssignableTerm;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.BooleanFormulaManager;
import org.sosy_lab.solver.api.Formula;
import org.sosy_lab.solver.api.FuncDecl;
import org.sosy_lab.solver.api.FuncDeclKind;
import org.sosy_lab.solver.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.solver.api.OptEnvironment;
import org.sosy_lab.solver.basicimpl.tactics.Tactic;
import org.sosy_lab.solver.visitors.DefaultFormulaVisitor;
import org.sosy_lab.solver.visitors.TraversalProcess;

import com.google.common.base.Function;

public class FormulaLinearizationManager {
  private final BooleanFormulaManager bfmgr;
  private final FormulaManagerView fmgr;
  private final NumeralFormulaManagerView<IntegerFormula, IntegerFormula> ifmgr;
  private final PolicyIterationStatistics statistics;

  // Opt environment cached to perform evaluation queries on the model.
  private OptEnvironment environment;

  public static final String CHOICE_VAR_NAME = "__POLICY_CHOICE_";
  private final UniqueIdGenerator choiceVarCounter = new UniqueIdGenerator();

  public FormulaLinearizationManager(
      BooleanFormulaManager pBfmgr, FormulaManagerView pFmgr,
      NumeralFormulaManagerView<IntegerFormula, IntegerFormula> pIfmgr,
      PolicyIterationStatistics pStatistics) {
    bfmgr = pBfmgr;
    fmgr = pFmgr;
    ifmgr = pIfmgr;
    statistics = pStatistics;
  }

  /**
   * Convert non-concave statements into disjunctions.
   *
   * At the moment handles:
   *
   *  x NOT(EQ(A, B)) => (A > B) \/ (A < B)
   */
  public BooleanFormula linearize(BooleanFormula input) {
    return bfmgr.visit(new LinearizationManager(
        fmgr, new HashMap<BooleanFormula, BooleanFormula>()
    ), input);
  }

  /**
   * Annotate disjunctions with choice variables.
   */
  public BooleanFormula annotateDisjunctions(BooleanFormula input) {
    return bfmgr.visit(new DisjunctionAnnotationVisitor(fmgr,
        new HashMap<BooleanFormula, BooleanFormula>()), input);
  }

  private class LinearizationManager
      extends BooleanFormulaManagerView.BooleanFormulaTransformationVisitor {
    // todo: shouldn't we just convert to NNF instead?

    protected LinearizationManager(
        FormulaManagerView pFmgr, Map<BooleanFormula, BooleanFormula> pCache) {
      super(pFmgr, pCache);
    }

    @Override
    public BooleanFormula visitNot(BooleanFormula pOperand) {
      List<BooleanFormula> split = fmgr.splitNumeralEqualityIfPossible(pOperand);

      // Pattern matching on (NOT (= A B)).
      if (split.size() == 2) {
        return bfmgr.or(
            bfmgr.not(split.get(0)), bfmgr.not(split.get(1))
        );
      }
      return super.visitNot(pOperand);
    }
  }

  private class DisjunctionAnnotationVisitor
      extends BooleanFormulaManagerView.BooleanFormulaTransformationVisitor {
    // todo: fail fast if the disjunction is inside NOT operator.

    protected DisjunctionAnnotationVisitor(
        FormulaManagerView pFmgr,
        Map<BooleanFormula, BooleanFormula> pCache) {
      super(pFmgr, pCache);
    }

    @Override
    public BooleanFormula visitOr(List<BooleanFormula> pOperands) {
      IntegerFormula choiceVar = getFreshVar();
      List<BooleanFormula> newArgs = new ArrayList<>();
      for (int i = 0; i < pOperands.size(); i++) {
        newArgs.add(
            bfmgr.and(
                visitIfNotSeen(pOperands.get(i)), fmgr.makeEqual(choiceVar, ifmgr.makeNumber(i))));
      }
      return bfmgr.or(newArgs);
    }
  }

  private IntegerFormula getFreshVar() {
    String freshVarName = CHOICE_VAR_NAME + choiceVarCounter.getFreshId();
    return ifmgr.makeVariable(freshVarName);
  }

  /**
   * Removes disjunctions from the {@code input} formula, by replacing them
   * with arguments which were used to generate the {@code model}.
   */
  public BooleanFormula enforceChoice(
      final BooleanFormula input,
      final Map<AssignableTerm, Object> model
  ) {
    Map<Formula, Formula> mapping = new HashMap<>();
    for (Entry<AssignableTerm, Object> entry : model.entrySet()) {
      String termName = entry.getKey().getName();
      if (termName.contains(CHOICE_VAR_NAME)) {
        BigInteger value = (BigInteger) entry.getValue();
        mapping.put(ifmgr.makeVariable(termName), ifmgr.makeNumber(value));
      }
    }

    BooleanFormula pathSelected = fmgr.substitute(input, mapping);
    pathSelected = fmgr.simplify(pathSelected);
    return pathSelected;
  }

  /**
   * Removes UFs and ITEs from the formula, effectively making it's semantics
   * "concave".
   */
  public BooleanFormula convertToPolicy(BooleanFormula f,
      OptEnvironment optEnvironment) {

    environment = optEnvironment;

    statistics.ackermannizationTimer.start();
    f = fmgr.applyTactic(f, Tactic.NNF);

    // Get rid of UFs.
    BooleanFormula noUFs = processUFs(f);

    // Get rid of ite-expressions.
    BooleanFormula out = bfmgr.visit(new ReplaceITEVisitor(), noUFs);
    statistics.ackermannizationTimer.stop();

    return out;
  }

  /**
   * TODO!! This does not correctly replace if-then-else
   * which occurs INSIDE the formula.
   */
  private class ReplaceITEVisitor
      extends BooleanFormulaManagerView.BooleanFormulaTransformationVisitor {

    private ReplaceITEVisitor() {
      super(fmgr, new HashMap<BooleanFormula, BooleanFormula>());
    }

    @Override
    public BooleanFormula visitIfThenElse(
        BooleanFormula pCondition, BooleanFormula pThenFormula, BooleanFormula pElseFormula) {

      BooleanFormula cond = fmgr.simplify(environment.evaluate(pCondition));
      if (bfmgr.isTrue(cond)) {
        return visitIfNotSeen(pThenFormula);
      } else {
        return visitIfNotSeen(pElseFormula);
      }
    }
  }

  /**
   * Ackermannization:
   * Requires a fixpoint computation as UFs can take other UFs as arguments.
   * First removes UFs with no arguments, etc.
   */
  private BooleanFormula processUFs(BooleanFormula f) {
    List<Formula> UFs = new ArrayList<>(findUFs(f));

    Map<Formula, Formula> substitution = new HashMap<>();

    List<BooleanFormula> extraConstraints = new ArrayList<>();

    for (int idx1=0; idx1<UFs.size(); idx1++) {
      Formula uf = UFs.get(idx1);
      Formula freshVar = fmgr.makeVariable(fmgr.getFormulaType(uf),
          freshUFName(idx1));
      substitution.put(uf, freshVar);

      for (int idx2=idx1+1; idx2<UFs.size(); idx2++) {
        Formula otherUF = UFs.get(idx2);
        if (uf == otherUF) {
          continue;
        }

        Formula otherFreshVar = fmgr.makeVariable(fmgr.getFormulaType(otherUF),
            freshUFName(idx2));

        /**
         * If UFs are equal _under_given_model_, make them equal in the
         * resulting policy bound.
         */
        if (evaluate(uf).equals(evaluate(otherUF))) {
          extraConstraints.add(fmgr.makeEqual(freshVar, otherFreshVar));
        }
      }
    }

    // Get rid of UFs.
    BooleanFormula formulaNoUFs = fmgr.substitute(f, substitution);
    return bfmgr.and(
        formulaNoUFs, bfmgr.and(extraConstraints)
    );
  }

  private Set<Formula> findUFs(Formula f) {
    final Set<Formula> UFs = new HashSet<>();

    fmgr.visitRecursively(new DefaultFormulaVisitor<TraversalProcess>() {
      @Override
      protected TraversalProcess visitDefault(Formula f) {
        return TraversalProcess.CONTINUE;
      }

      @Override
      public TraversalProcess visitFuncApp(Formula f,
          List<Formula> args,
          FuncDecl decl,
          Function<List<Formula>, Formula> newApplicationConstructor) {
        if (decl.getKind() == FuncDeclKind.UF) {
          UFs.add(f);

        }
        return TraversalProcess.CONTINUE;
      }
    }, f);

    return UFs;
  }

  private Formula evaluate(Formula f) {
    return fmgr.simplify(environment.evaluate(f));
  }

  private String freshUFName(int idx) {
    return "__UF_fresh_" + idx;
  }
}
