package org.sosy_lab.cpachecker.cpa.policyiteration;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import org.sosy_lab.common.UniqueIdGenerator;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView.BooleanFormulaTransformationVisitor;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.FunctionDeclarationKind;
import org.sosy_lab.java_smt.api.Model;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
import org.sosy_lab.java_smt.api.Tactic;
import org.sosy_lab.java_smt.api.visitors.DefaultFormulaVisitor;
import org.sosy_lab.java_smt.api.visitors.TraversalProcess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FormulaLinearizationManager {
  private final BooleanFormulaManager bfmgr;
  private final FormulaManagerView fmgr;
  private final PolicyIterationStatistics statistics;

  public static final String CHOICE_VAR_NAME = "__POLICY_CHOICE_";
  private final UniqueIdGenerator choiceVarCounter = new UniqueIdGenerator();

  public FormulaLinearizationManager(
      FormulaManagerView pFmgr,
      PolicyIterationStatistics pStatistics) {
    bfmgr = pFmgr.getBooleanFormulaManager();
    fmgr = pFmgr;
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
    return bfmgr.transformRecursively(input, new BooleanFormulaTransformationVisitor(fmgr) {
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
    });
  }

  /**
   * Annotate disjunctions with choice variables.
   */
  public BooleanFormula annotateDisjunctions(BooleanFormula input)
      throws InterruptedException {
    input = fmgr.applyTactic(input, Tactic.NNF);
    return bfmgr.transformRecursively(
        input, new BooleanFormulaTransformationVisitor(fmgr) {

      @Override
      public BooleanFormula visitOr(List<BooleanFormula> processedOperands) {
        return annotateDisjunction(processedOperands);
      }
    });
  }

  private BooleanFormula annotateDisjunction(List<BooleanFormula> args) {
    assert args.size() != 0;
    if (args.size() == 1) {
      return args.get(0);
    } else {
      BooleanFormula choiceVar = bfmgr.makeVariable(getFreshVarName());
      int pivot = args.size() / 2;
      return bfmgr.or(
          bfmgr.and(
              choiceVar,
              annotateDisjunction(args.subList(0, pivot))
          ),
          bfmgr.and(
              bfmgr.not(choiceVar),
              annotateDisjunction(args.subList(pivot, args.size()))
          )
      );
    }
  }

  private String getFreshVarName() {
    return CHOICE_VAR_NAME + choiceVarCounter.getFreshId();
  }

  /**
   * Removes disjunctions from the {@code input} formula, by replacing them
   * with arguments which were used to generate the {@code model}.
   */
  public BooleanFormula enforceChoice(
      final BooleanFormula input,
      final Model model
  ) throws InterruptedException {

    // TODO: more efficient to call #evaluate() on the subset of variables
    // which we actually use.
    // These models can be huge.
    Map<Formula, Formula> mapping = new HashMap<>();
    for (ValueAssignment entry : model) {
      String termName = entry.getName();
      if (termName.contains(CHOICE_VAR_NAME)) {
          mapping.put(
              bfmgr.makeVariable(termName),
              bfmgr.makeBoolean((boolean) entry.getValue()));
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
      Model pModel) throws InterruptedException {

    statistics.ackermannizationTimer.start();
    f = fmgr.applyTactic(f, Tactic.NNF);

    // Get rid of UFs.
    BooleanFormula out = processUFs(f, pModel);
    statistics.ackermannizationTimer.stop();

    return out;
  }

  /**
   * Ackermannization:
   * Requires a fixpoint computation as UFs can take other UFs as arguments.
   * First removes UFs with no arguments, etc.
   */
  private BooleanFormula processUFs(BooleanFormula f, Model model) {
    Multimap<String, Pair<Formula, List<Formula>>> UFs = findUFs(f);

    Map<Formula, Formula> substitution = new HashMap<>();
    List<BooleanFormula> extraConstraints = new ArrayList<>();

    for (String funcName : UFs.keySet()) {
      List<Pair<Formula, List<Formula>>> ufList = new ArrayList<>(UFs.get(funcName));
      for (int idx1=0; idx1<ufList.size(); idx1++) {
        Pair<Formula, List<Formula>> p = ufList.get(idx1);

        Formula uf = p.getFirst();
        List<Formula> args = p.getSecondNotNull();

        Formula freshVar = fmgr.makeVariable(fmgr.getFormulaType(uf),
            freshUFName(idx1));
        substitution.put(uf, freshVar);

        for (int idx2=idx1+1; idx2<ufList.size(); idx2++) {
          Pair<Formula, List<Formula>> p2 = ufList.get(idx2);
          List<Formula> otherArgs = p2.getSecondNotNull();

          Formula otherUF = p2.getFirst();

          /**
           * If UFs are equal under the given model, force them to be equal in
           * the resulting policy bound.
           */
          Preconditions.checkState(args.size() == otherArgs.size());
          boolean argsEqual = true;
          for (int i = 0; i<args.size(); i++) {
            Object evalA = model.evaluate(args.get(i));
            Object evalB = model.evaluate(otherArgs.get(i));
            if (evalA != null && evalB != null && !evalA.equals(evalB)) {
              argsEqual = false;
            }
          }
          if (argsEqual) {
            Formula otherFreshVar = fmgr.makeVariable(
                fmgr.getFormulaType(otherUF),
                freshUFName(idx2)
            );
            extraConstraints.add(fmgr.makeEqual(freshVar, otherFreshVar));
          }
        }
      }
    }

    // Get rid of UFs.
    BooleanFormula formulaNoUFs = fmgr.substitute(f, substitution);
    return bfmgr.and(
        formulaNoUFs, bfmgr.and(extraConstraints)
    );
  }

  private Multimap<String, Pair<Formula, List<Formula>>> findUFs(Formula f) {
    final Multimap<String, Pair<Formula, List<Formula>>> UFs = HashMultimap.create();

    fmgr.visitRecursively(f, new DefaultFormulaVisitor<TraversalProcess>() {
      @Override
      protected TraversalProcess visitDefault(Formula f) {
        return TraversalProcess.CONTINUE;
      }

      @Override
      public TraversalProcess visitFunction(Formula f,
          List<Formula> args,
          FunctionDeclaration<?> decl) {
        if (decl.getKind() == FunctionDeclarationKind.UF) {
          UFs.put(decl.getName(), Pair.of(f, args));

        }
        return TraversalProcess.CONTINUE;
      }
    });

    return UFs;
  }

  private String freshUFName(int idx) {
    return "__UF_fresh_" + idx;
  }
}
