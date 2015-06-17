package org.sosy_lab.cpachecker.cpa.policyiteration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView.BooleanFormulaTransformationVisitor;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

public class FormulaSlicingManager {
  private final LogManager logger;
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;

  private static final String POINTER_ADDR_VAR_NAME = "ADDRESS_OF";

  public FormulaSlicingManager(LogManager pLogger, FormulaManagerView pFmgr) {
    logger = pLogger;
    fmgr = pFmgr;
    bfmgr = fmgr.getBooleanFormulaManager();
  }

  /**
   * @return Over-approximation of the formula {@code f} which deals only
   * with pointers.
   */
  public BooleanFormula pointerFormulaSlice(BooleanFormula f) {
    Set<String> closure = findClosure(f, new Predicate<String>() {
      @Override
      public boolean apply(String input) {
        return input.contains(POINTER_ADDR_VAR_NAME);
      }
    });
    logger.log(Level.FINE, "Closure =", closure);
    BooleanFormula slice = new RecursiveSliceVisitor(ImmutableSet.copyOf(closure)).visit(f);
    logger.log(Level.FINE, "Produced =", slice);
    return slice;
  }

  /**
   * @return Closure with respect to interacts-relation of
   * <b>uninstantiated</b> variables in {@code f} which satisfy the condition
   * {@code seedCondition}.
   */
  private Set<String> findClosure(BooleanFormula f, Predicate<String> seedCondition) {
    Set<String> closure = new HashSet<>();
    Set<BooleanFormula> atoms = fmgr.uninstantiate(fmgr.extractAtoms(f, false));
    boolean changed = true;
    while (changed) {
      changed = false;
      for (BooleanFormula atom : atoms) {
        Set<String> variableNames = fmgr.extractFunctionNames(atom, false);
        for (String s : variableNames) {
          if (seedCondition.apply(s) || closure.contains(s)) {
            changed |= closure.addAll(variableNames);
            break;
          }
        }
      }
    }
    return closure;
  }

  private class RecursiveSliceVisitor extends BooleanFormulaTransformationVisitor {

    private final boolean isInsideNot;
    private final Set<String> closure;

    // We need to handle negated formulas differently from non-negated formulas,
    // and we need a separate super.cache for negated/non-negated formulas
    // (Example: in ((a & b) | (!a & c)), "a" needs to be replaced once by "true"
    // and once by "false").
    // Thus we need two visitor instances with different settings for isInsideNot,
    // and they both delegate to the other when encountering a negation.
    private RecursiveSliceVisitor visitorForNegatedFormula;

    RecursiveSliceVisitor(Set<String> pClosure) {
      this(false, pClosure);

      visitorForNegatedFormula = new RecursiveSliceVisitor(true, pClosure);
      visitorForNegatedFormula.visitorForNegatedFormula = this;
    }

    RecursiveSliceVisitor(boolean pIsInsideNot, Set<String> pClosure) {
      super(fmgr, new HashMap<BooleanFormula, BooleanFormula>());
      isInsideNot = pIsInsideNot;
      closure = pClosure;
    }

    @Override
    protected BooleanFormula visitAtom(BooleanFormula f) {
      Formula uninstantiatedF = fmgr.uninstantiate(f);
      Set<String> containedVariables = fmgr.extractFunctionNames(
          uninstantiatedF, false);
      BooleanFormula constant = bfmgr.makeBoolean(!isInsideNot);
      if (!Sets.intersection(closure, containedVariables).isEmpty()) {
        return f;
      } else {
        // Hack to propagate the call variables across the abstraction.
        if (containedVariables.size() == 2) {
          Iterator<String> iterator = containedVariables.iterator();
          String first = iterator.next();
          String second = iterator.next();
          if (first.contains("::") && second.contains("::") &&
              !first.substring(0, first.indexOf("::")).equals(
              second.substring(0, second.indexOf("::"))
          )) {
            return f;
          } else {
            return constant;
          }
        } else {
          return constant;
        }
      }
    }

    @Override
    protected BooleanFormula visitNot(BooleanFormula pOperand) {
      return bfmgr.not(visitorForNegatedFormula.visitIfNotSeen(pOperand));
    }
  }
}
