package org.sosy_lab.cpachecker.cpa.policyiteration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.UnsafeFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

public class FormulaSlicingManager {
  private final LogManager logger;
  private final FormulaManagerView fmgr;
  private final UnsafeFormulaManager unsafeManager;
  private final BooleanFormulaManager bfmgr;

  public FormulaSlicingManager(LogManager pLogger,
      FormulaManagerView pFmgr,
      UnsafeFormulaManager pUnsafeManager,
      BooleanFormulaManager pBfmgr) {
    logger = pLogger;
    fmgr = pFmgr;
    unsafeManager = pUnsafeManager;
    bfmgr = pBfmgr;
  }

  /**
   * @return Over-approximation of {@code f} which deals only with pointers.
   */
  BooleanFormula filterPointers(BooleanFormula f) {
    Set<String> closure = findClosure(f, new Predicate<String>() {
      public boolean apply(String input) {
        return input.contains("ADDRESS_OF");
      }
    });
    logger.log(Level.FINE, "Closure =", closure);
    Formula out = recFilter(f, ImmutableSet.copyOf(closure), false);
    logger.log(Level.FINE, "Produced =", out);
    return fmgr.simplify((BooleanFormula)out);
  }

  private Set<String> findClosure(BooleanFormula f, Predicate<String> seedCondition) {
    Set<String> closure = new HashSet<>();

    Collection<BooleanFormula> atoms = fmgr.extractAtoms(f, false, false);
    for (BooleanFormula atom : atoms) {
      Set<String> variableNames = fmgr.extractVariableNames(atom);
      for (String s : variableNames) {
        if (seedCondition.apply(s) || closure.contains(s)) {
          closure.addAll(variableNames);
          break;
        }
      }
    }
    return closure;
  }

  /**
   * Only let through things contained in the {@code closure}.
   */
  private Formula recFilter(Formula f, ImmutableSet<String> closure, boolean isInsideNot) {
    if (unsafeManager.isVariable(f) && closure.contains(f.toString())) {
      return f;
    }

    // Skip atoms.
    if (unsafeManager.isAtom(f)) {
      Set<String> containedVariables = fmgr.extractVariableNames(f);
      if (Sets.intersection(closure, containedVariables).isEmpty()) {
        return bfmgr.makeBoolean(!isInsideNot);
      } else {
        return f;
      }
    }

    int count = unsafeManager.getArity(f);
    List<Formula> newArgs = new ArrayList<>(count);
    String name = unsafeManager.getName(f);
    if (name.equals("not")) {
      isInsideNot = !isInsideNot;
    }
    for (int i=0; i<count; i++) {
      newArgs.add(recFilter(unsafeManager.getArg(f, i), closure, isInsideNot));
    }

    return unsafeManager.replaceArgs(f, newArgs);
  }
}
