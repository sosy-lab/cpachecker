package org.sosy_lab.cpachecker.cpa.predicate;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpressionCollectorVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.solver.api.BooleanFormula;

import java.util.Collection;
import java.util.Set;

public final class PredicatePrecisions {

  private static Set<CIdExpression> collectIdExpressionsIn(AssumeEdge pAssume) {
    if (pAssume.getExpression() instanceof CExpression) {
      final CExpression e = (CExpression) pAssume.getExpression();
      final CIdExpressionCollectorVisitor visitor = new CIdExpressionCollectorVisitor();
      e.accept(visitor);
      return visitor.getReferencedIdExpressions();
    } else {
      throw new RuntimeException("Only C programming language supported!");
    }
  }

  private static Collection<AbstractionPredicate> assumeEdgeToPredicates(
      PathFormulaManager pPathFormulaManager,
      PredicateAbstractionManager pAbstractionManager,
      boolean atomicPredicates, AssumeEdge assume)
        throws CPATransferException, InterruptedException {

    BooleanFormula relevantAssumesFormula = pPathFormulaManager.makeAnd(
        pPathFormulaManager.makeEmptyPathFormula(), assume).getFormula();

    Collection<AbstractionPredicate> preds;
    if (atomicPredicates) {
      preds = pAbstractionManager.getPredicatesForAtomsOf(relevantAssumesFormula);
    } else {
      preds = ImmutableList.of(pAbstractionManager.getPredicateFor(relevantAssumesFormula));
    }

    return preds;
  }

  public static PredicatePrecision assumeEdgeToPrecision(
      PathFormulaManager pPathFormulaManager,
      PredicateAbstractionManager pAbstractionManager,
      AssumeEdge pAssume, boolean pAtomicPredicates, boolean pScopedPredicates)
    throws CPATransferException, InterruptedException {

    // Create a boolean formula from the assume
    Collection<AbstractionPredicate> preds = assumeEdgeToPredicates(pPathFormulaManager,
        pAbstractionManager, pAtomicPredicates, pAssume);

    // Predicates that should be tracked on function scope
    Multimap<String, AbstractionPredicate> functionPredicates = ArrayListMultimap.create();

    // Predicates that should be tracked globally
    Collection<AbstractionPredicate> globalPredicates = Lists.newArrayList();

    // Check whether the predicate should be used global or only local
    boolean applyGlobal = true;
    if (pScopedPredicates) {
      for (CIdExpression idExpr : collectIdExpressionsIn(pAssume)) {
        CSimpleDeclaration decl = idExpr.getDeclaration();
        if (decl instanceof CVariableDeclaration) {
          if (!((CVariableDeclaration) decl).isGlobal()) {
            applyGlobal = false;
          }
        } else if (decl instanceof CParameterDeclaration) {
          applyGlobal = false;
        }
      }
    }

    // Add the predicate to the resulting precision
    if (applyGlobal) {
      globalPredicates.addAll(preds);
    } else {
      String function = pAssume.getPredecessor().getFunctionName();
      functionPredicates.putAll(function, preds);
    }

    return new PredicatePrecision(
        ImmutableSetMultimap.<PredicatePrecision.LocationInstance, AbstractionPredicate>of(),
        ArrayListMultimap.<CFANode, AbstractionPredicate>create(),
        functionPredicates,
        globalPredicates);
  }

}
