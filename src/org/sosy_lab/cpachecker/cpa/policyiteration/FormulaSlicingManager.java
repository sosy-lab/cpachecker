package org.sosy_lab.cpachecker.cpa.policyiteration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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

  private static final String NOT_FUNC_NAME = "not";
  private static final String POINTER_ADDR_VAR_NAME = "ADDRESS_OF";

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
   * @return Over-approximation of the formla {@code f} which deals only
   * with pointers.
   */
  BooleanFormula pointerFormulaSlice(BooleanFormula f) throws InterruptedException {
    Set<String> closure = findClosure(f, new Predicate<String>() {
      @Override
      public boolean apply(String input) {
        return input.contains(POINTER_ADDR_VAR_NAME);
      }
    });
    logger.log(Level.FINE, "Closure =", closure);
    Formula out = recSliceFormula(
        f, ImmutableSet.copyOf(closure), false, new HashMap<Formula, Formula>());
    logger.log(Level.FINE, "Produced =", out);
    return fmgr.simplify((BooleanFormula)out);
  }

  /**
   * @return Transitive closure of variables in {@code f} which satisfy the
   * condition {@code seedCondition}.
   */
  private Set<String> findClosure(BooleanFormula f, Predicate<String> seedCondition) {

    Set<String> closure = new HashSet<>();
    Collection<BooleanFormula> atoms = fmgr.extractAtoms(f, false, false);
    boolean changed = true;
    while (changed) {
      changed = false;
      for (BooleanFormula atom : atoms) {
        Set<String> variableNames = fmgr.extractVariableNames(atom);
        for (String s : variableNames) {
          if (seedCondition.apply(s) || closure.contains(s)) {
            int origClosureSize = closure.size();
            closure.addAll(variableNames);
            int newClosureSize = closure.size();
            changed = newClosureSize > origClosureSize;
            break;
          }
        }
      }
    }
    return closure;
  }

  /**
   * Slice of the formula AST containing the variables in
   * {@code closure}
   */
  private Formula recSliceFormula(
      Formula f,
      ImmutableSet<String> closure,
      boolean isInsideNot,
      Map<Formula, Formula> memoization) throws InterruptedException {
    Formula out = memoization.get(f);
    if (out != null) {
      return out;
    }

    if (unsafeManager.isAtom(f)) {
      Set<String> containedVariables = fmgr.extractVariableNames(f);
      if (!Sets.intersection(closure, containedVariables).isEmpty()) {
        out = f;
      } else {
        // Hack to propagate the call variables,
        if (containedVariables.size() == 2) {
          Iterator<String> iterator = containedVariables.iterator();
          String first = iterator.next();
          String second = iterator.next();
          if (first.contains("::") && second.contains("::") &&
              !first.substring(0, first.indexOf("::")).equals(
              second.substring(0, second.indexOf("::"))
          )) {
            out = f;
          } else {
            out = bfmgr.makeBoolean(!isInsideNot);
          }
        } else {
          out = bfmgr.makeBoolean(!isInsideNot);
        }
      }
    } else {
      int count = unsafeManager.getArity(f);
      List<Formula> newArgs = new ArrayList<>(count);
      String name = unsafeManager.getName(f);
      if (name.equals(NOT_FUNC_NAME)) {
        isInsideNot = !isInsideNot;
      }
      for (int i=0; i<count; i++) {
        newArgs.add(
            recSliceFormula(
                unsafeManager.getArg(f, i), closure, isInsideNot, memoization));
      }
      out = unsafeManager.replaceArgs(f, newArgs);
    }
    memoization.put(f, out);
    return out;
  }
}
