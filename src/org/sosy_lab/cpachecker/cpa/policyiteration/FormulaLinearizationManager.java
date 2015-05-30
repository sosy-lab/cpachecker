package org.sosy_lab.cpachecker.cpa.policyiteration;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.sosy_lab.cpachecker.core.counterexample.Model;
import org.sosy_lab.cpachecker.util.predicates.AssignableTerm;
import org.sosy_lab.cpachecker.util.UniqueIdGenerator;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.IntegerFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.OptEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.UnsafeFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView.BooleanFormulaTransformationVisitor;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.NumeralFormulaManagerView;

public class FormulaLinearizationManager {
  private final UnsafeFormulaManager ufmgr;
  private final BooleanFormulaManager bfmgr;
  private final FormulaManagerView fmgr;
  private final NumeralFormulaManagerView<IntegerFormula, IntegerFormula> ifmgr;

  // Opt environment cached to perform evaluation queries on the model.
  private OptEnvironment environment;

  public static final String CHOICE_VAR_NAME = "__POLICY_CHOICE_";
  private final UniqueIdGenerator choiceVarCounter = new UniqueIdGenerator();

  public FormulaLinearizationManager(UnsafeFormulaManager pUfmgr,
      BooleanFormulaManager pBfmgr, FormulaManagerView pFmgr,
      NumeralFormulaManagerView<IntegerFormula, IntegerFormula> pIfmgr) {
    ufmgr = pUfmgr;
    bfmgr = pBfmgr;
    fmgr = pFmgr;
    ifmgr = pIfmgr;
  }

  /**
   * Convert non-concave statements into disjunctions.
   *
   * At the moment handles:
   *
   *  x NOT(EQ(A, B)) => (A > B) \/ (A < B)
   */
  public BooleanFormula linearize(BooleanFormula input) {
    return new LinearizationManager(
        fmgr, new HashMap<BooleanFormula, BooleanFormula>()
    ).visit(input);
  }

  /**
   * Annotate disjunctions with choice variables.
   */
  public BooleanFormula annotateDisjunctions(BooleanFormula input) {
    return new DisjunctionAnnotationVisitor(fmgr,
        new HashMap<BooleanFormula, BooleanFormula>()).visit(input);
  }

  private class LinearizationManager extends BooleanFormulaTransformationVisitor {

    protected LinearizationManager(
        FormulaManagerView pFmgr, Map<BooleanFormula, BooleanFormula> pCache) {
      super(pFmgr, pCache);
    }

    @Override
    protected BooleanFormula visitNot(BooleanFormula pOperand) {
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

  private class DisjunctionAnnotationVisitor extends BooleanFormulaTransformationVisitor {
    // todo: fail fast if the disjunction is inside NOT operator.

    protected DisjunctionAnnotationVisitor(
        FormulaManagerView pFmgr,
        Map<BooleanFormula, BooleanFormula> pCache) {
      super(pFmgr, pCache);
    }

    @Override
    protected BooleanFormula visitOr(BooleanFormula... pOperands) {
      IntegerFormula choiceVar = getFreshVar();
      List<BooleanFormula> newArgs = new ArrayList<>();
      for (int i=0; i < pOperands.length; i++) {
        newArgs.add(
            bfmgr.and(
                visitIfNotSeen(pOperands[i]),
                fmgr.makeEqual(choiceVar, ifmgr.makeNumber(i))
            )
        );
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
      final Model model
  ) {
    Map<Formula, Formula> mapping = new HashMap<>();
    for (Entry<AssignableTerm, Object> entry : model.entrySet()) {
      String termName = entry.getKey().getName();
      if (termName.contains(CHOICE_VAR_NAME)) {
        BigInteger value = (BigInteger) entry.getValue();
        mapping.put(ifmgr.makeVariable(termName), ifmgr.makeNumber(value));
      }
    }
    BooleanFormula pathSelected = ufmgr.substitute(input, mapping);
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

    // Get rid of UFs.
    BooleanFormula noUFs = processUFs(f);

    // Get rid of ite-expressions.
    return replaceITE(noUFs);
  }

  private BooleanFormula replaceITE(BooleanFormula f) {
    return (BooleanFormula) recReplaceITE(f, new HashMap<Formula, Formula>());
  }

  private Formula recReplaceITE(Formula f,
      Map<Formula, Formula> memoization) {
    if (memoization.containsKey(f)) {
      return memoization.get(f);
    }
    Formula out;

    if (bfmgr.isIfThenElse(f)) {
      Formula condition = ufmgr.getArg(f, 0);
      Formula then = ufmgr.getArg(f, 1);
      Formula else_ = ufmgr.getArg(f, 2);

      if (evaluate(condition).equals(bfmgr.makeBoolean(true))) {
        out = recReplaceITE(then, memoization);
      } else {
        out = recReplaceITE(else_, memoization);
      }
    } else {
      List<Formula> newChildren = new ArrayList<>(ufmgr.getArity(f));
      for (Formula child : children(f)) {
        newChildren.add(recReplaceITE(child, memoization));
      }
      out = ufmgr.replaceArgs(f, newChildren);
    }

    memoization.put(f, out);
    return out;
  }

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
        if (uf == otherUF) continue;

        Formula otherFreshVar = fmgr.makeVariable(fmgr.getFormulaType(otherUF),
            freshUFName(idx2));

        if (evaluate(uf).equals(evaluate(otherUF))) {
          extraConstraints.add(fmgr.makeEqual(freshVar, otherFreshVar));
        }
      }
    }

    // Get rid of UFs.
    BooleanFormula formulaNoUFs = ufmgr.substitute(f, substitution);
    return bfmgr.and(
        formulaNoUFs, bfmgr.and(extraConstraints)
    );
  }

  private Set<Formula> findUFs(Formula f) {
    Set<Formula> UFs = new HashSet<>();
    recFindUFs(f, new HashSet<Formula>(), new HashSet<Formula>());
    return UFs;
  }

  private void recFindUFs(Formula f, Set<Formula> visited, Set<Formula> UFs) {
    if (visited.contains(f)) return;
    if (ufmgr.isUF(f)) {
      UFs.add(f);
    } else {
      for (Formula child : children(f)) {
        recFindUFs(child, visited, UFs);
      }
    }

    visited.add(f);
  }

  private Iterable<Formula> children(Formula f) {
    int arity = ufmgr.getArity(f);
    List<Formula> out = new ArrayList<>(arity);
    for (int i=0; i<arity; i++) {
      out.add(ufmgr.getArg(f, i));
    }
    return out;
  }

  private Formula evaluate(Formula f) {
    return ufmgr.simplify(environment.evaluate(f));
  }

  private String freshUFName(int idx) {
    return "__UF_fresh_" + idx;
  }
}
