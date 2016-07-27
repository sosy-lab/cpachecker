package org.sosy_lab.cpachecker.cpa.predicate;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpressionCollectingVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.defaults.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.solver.api.BooleanFormula;

import java.util.Collection;
import java.util.Set;

public final class PredicatePrecisions {

  private static Set<CIdExpression> collectIdExpressionsIn(CFAEdge pEdge) {
    final CIdExpressionCollectingVisitor visitor = new CIdExpressionCollectingVisitor();
    if (pEdge instanceof AssumeEdge) {
      AssumeEdge assume = (AssumeEdge) pEdge;
      if (assume.getExpression() instanceof CExpression) {
        final CExpression e = (CExpression) assume.getExpression();
        return e.accept(visitor);
      } else {
        throw new RuntimeException("Only C programming language supported!");
      }
    } else if (pEdge instanceof CStatementEdge) {
      CStatementEdge stmt = (CStatementEdge) pEdge;
      return stmt.getStatement().accept(visitor);
    } else {
      throw new RuntimeException("Support for type of edge not yet implemented!");
    }
  }

  private static Collection<AbstractionPredicate> edgeToPredicates(
      PathFormulaManager pPathFormulaManager,
      PredicateAbstractionManager pAbstractionManager,
      boolean pAtomicPredicates, CFAEdge pEdge)
        throws CPATransferException, InterruptedException {

    BooleanFormula relevantAssumesFormula = pPathFormulaManager.makeAnd(
        pPathFormulaManager.makeEmptyPathFormula(), pEdge).getFormula();

    final Collection<AbstractionPredicate> preds;
    if (pAtomicPredicates) {
      preds = pAbstractionManager.getPredicatesForAtomsOf(relevantAssumesFormula);
    } else {
      preds = ImmutableList.of(pAbstractionManager.getPredicateFor(relevantAssumesFormula));
    }

    return preds;
  }

  private static MemoryLocation idExprToMemLoc(CIdExpression idExp, CFAEdge pEdge) {
    if (idExp.getDeclaration() != null) {
      return MemoryLocation.valueOf(idExp.getDeclaration().getQualifiedName());
    }

    boolean isGlobal = ForwardingTransferRelation.isGlobal(idExp);

    if (isGlobal) {
      return MemoryLocation.valueOf(idExp.getName());
    } else {
      return MemoryLocation.valueOf(pEdge.getPredecessor().getFunctionName(), idExp.getName());
    }
  }

  private static boolean trackingEnabled(VariableTrackingPrecision pMiningPrecision,
       CFAEdge pEdge, Set<CIdExpression> pReferences) {

    for (CIdExpression id: pReferences) {
      MemoryLocation ml = idExprToMemLoc(id, pEdge);
      boolean track = pMiningPrecision.isTracking(ml, id.getExpressionType(), pEdge.getPredecessor());
      if (!track) {
        return false;
      }
    }
    return true;
  }

  public static PredicatePrecision edgeToPrecision(
      PathFormulaManager pPathFormulaManager,
      PredicateAbstractionManager pAbstractionManager,
      CFAEdge pEdge,
      VariableTrackingPrecision pMiningPrecision,
      boolean pAtomicPredicates,
      boolean pScopedPredicates)
    throws CPATransferException, InterruptedException {

    // Predicates that should be tracked on function scope
    Multimap<String, AbstractionPredicate> functionPredicates = ArrayListMultimap.create();

    // Predicates that should be tracked globally
    Collection<AbstractionPredicate> globalPredicates = Lists.newArrayList();

    Set<CIdExpression> references = collectIdExpressionsIn(pEdge);

    // trackingEnabled(pMiningPrecision, pEdge, references);
    if (true) {

      // Create a boolean formula from the assume
      Collection<AbstractionPredicate> preds = edgeToPredicates(pPathFormulaManager,
          pAbstractionManager, pAtomicPredicates, pEdge);

      // Check whether the predicate should be used global or only local
      boolean applyGlobal = true;
      if (pScopedPredicates) {
        for (CIdExpression idExpr : references) {
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
        String function = pEdge.getPredecessor().getFunctionName();
        functionPredicates.putAll(function, preds);
      }
    }

    return new PredicatePrecision(
        ImmutableSetMultimap.of(),
        ArrayListMultimap.create(),
        functionPredicates,
        globalPredicates);
  }

}
