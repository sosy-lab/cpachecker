/**
 *
 */
package cpa.pointsto;

import java.util.HashMap;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTArraySubscriptExpression;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTInitializer;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;
import org.eclipse.cdt.core.dom.ast.IArrayType;
import org.eclipse.cdt.core.dom.ast.IPointerType;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.c.DeclarationEdge;
import cfa.objectmodel.c.MultiDeclarationEdge;
import cfa.objectmodel.c.MultiStatementEdge;
import cfa.objectmodel.c.StatementEdge;
import cpa.common.CPATransferException;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.TransferRelation;
import exceptions.CPAException;

/**
 * @author Michael Tautschnig <tautschnig@forsyte.de>
 *
 */
public class PointsToTransferRelation implements TransferRelation {

  private final AbstractDomain abstractDomain;

  public PointsToTransferRelation (AbstractDomain abstractDomain) {
    this.abstractDomain = abstractDomain;
  }

  private class DeclarationVisitor extends ASTVisitor {

    private final PointsToElement pointsToElement;
    private PointsToRelation relation;

    public DeclarationVisitor (PointsToElement pointsToElement) {
      this.pointsToElement = pointsToElement;
      relation = null;

      shouldVisitParameterDeclarations = true;
      shouldVisitDeclarators = true;
      shouldVisitExpressions = true;
    }

    @Override
    public int visit (IASTDeclarator declarator) {
      // System.err.println("Got into IASTDeclarator");
      if (0 != declarator.getPointerOperators().length) {
        relation = pointsToElement.addVariable(declarator);
        IASTInitializer initializer = declarator.getInitializer ();
        if (initializer != null) initializer.accept(this);
      }
      return PROCESS_ABORT;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.dom.ast.ASTVisitor#visit(org.eclipse.cdt.core.dom.ast.IASTExpression)
     */
    @Override
    public int visit(IASTExpression expression) {
      // System.err.println("Got into IASTExpression with " + expression.toString());
      assert (null != relation);

      if (expression.getRawSignature().equals("NULL") || expression.toString().equals("0")) {
        relation.makeNull();
      } else {
        relation.setAddress(expression.getRawSignature());
      }
      return super.visit(expression);
    }
  }



  private class StatementVisitor extends ASTVisitor {

    private final PointsToElement pointsToElement;
    private final HashMap<IASTNode,PointsToRelation> relations;

    public StatementVisitor (PointsToElement pointsToElement) {
      this.pointsToElement = pointsToElement;
      this.relations = new HashMap<IASTNode,PointsToRelation>();

      shouldVisitExpressions = true;
      shouldVisitStatements = true;
    }

    private void handle(IASTBinaryExpression binaryExpression) {

      System.err.println("Got into IASTBinaryExpression with " + binaryExpression.toString());

      switch (binaryExpression.getOperator ())
      {
      // X = Y
      case IASTBinaryExpression.op_assign:
      {
        PointsToRelation entryLhs = relations.get(binaryExpression.getOperand1());
        assert (entryLhs != null);
        IASTExpression rhs = binaryExpression.getOperand2();
        PointsToRelation entryRhs = relations.get(rhs);
        if (null == entryRhs) {
          if (rhs.getRawSignature().equals("NULL") || rhs.toString().equals("0")) {
            entryLhs.makeNull();
          } else {
            entryLhs.setAddress(rhs.getRawSignature());
          }
        } else {
          entryLhs.makeAlias(entryRhs);
        }
        relations.put(binaryExpression, entryLhs);
        break;
      }
      // X + Y, X - Y
      case IASTBinaryExpression.op_plus:
      case IASTBinaryExpression.op_minus:
      {
        PointsToRelation result = relations.get(binaryExpression.getOperand1()).clone();
        assert (relations.get(binaryExpression.getOperand2()) == null);
        try {
          int shift = Integer.valueOf(binaryExpression.getOperand2().getRawSignature());
          if (binaryExpression.getOperator() == IASTBinaryExpression.op_minus) shift *= -1;
          result.shift(shift);
        } catch (NumberFormatException e) {
          result.makeTop();
        }
        relations.put(binaryExpression.getOperand1(), result);
        break;
      }
      // X += Y, X -= Y
      case IASTBinaryExpression.op_plusAssign:
      case IASTBinaryExpression.op_minusAssign:
      {
        PointsToRelation entryLhs = relations.get(binaryExpression.getOperand1());
        assert (entryLhs != null);
        IASTExpression rhs = binaryExpression.getOperand2();
        PointsToRelation entryRhs = relations.get(rhs);
        assert (entryRhs == null);
        try {
          int shift = Integer.valueOf(binaryExpression.getOperand2().getRawSignature());
          if (binaryExpression.getOperator() == IASTBinaryExpression.op_minus) shift *= -1;
          entryLhs.shift(shift);
        } catch (NumberFormatException e) {
          entryLhs.makeTop();
        }
        relations.put(binaryExpression, entryLhs);
        break;
      }
      case IASTBinaryExpression.op_pmarrow:
      case IASTBinaryExpression.op_pmdot:
      default:
      {
        System.err.println("Unhandled expression " + binaryExpression.getRawSignature());
      }
      }
    }

    private void handle(IASTUnaryExpression unaryExpression) {

      System.err.println("Got into IASTUnaryExpression with " + unaryExpression.toString());

      PointsToRelation entry = relations.get(unaryExpression.getOperand());

      switch (unaryExpression.getOperator()) {
      // X--
      case IASTUnaryExpression.op_postFixDecr:
      {
        assert (entry != null);
        // clone before modifying the state
        relations.put(unaryExpression, entry.clone());
        entry.shift(-1);
        break;
      }
      // --X
      case IASTUnaryExpression.op_prefixDecr:
      {
        assert (entry != null);
        entry.shift(-1);
        relations.put(unaryExpression, entry);
        break;
      }
      // X++
      case IASTUnaryExpression.op_postFixIncr:
      {
        assert (entry != null);
        // clone before modifying the state
        relations.put(unaryExpression, entry.clone());
        entry.shift(1);
        break;
      }
      // ++X
      case IASTUnaryExpression.op_prefixIncr:
      {
        assert (entry != null);
        entry.shift(1);
        relations.put(unaryExpression, entry);
        break;
      }
      // *X
      case IASTUnaryExpression.op_star:
      {
        assert (entry != null);
        //relations.put(unaryExpression, entry.deref());
        break;
      }
      // &X
      case IASTUnaryExpression.op_amper:
      {
        // if X is not a pointer, just take expression as is and do nothing here
        // otherwise:
        if (null != entry) {
          // X is actually *Y
          //relations.put(unaryExpression, entry.getAddress());
        }
      }
      default:
      {
        System.err.println("Unhandled expression " + unaryExpression.getRawSignature());
      }
      }
    }

    private void handle(IASTArraySubscriptExpression arrayExpression) {

      System.err.println("Got into IASTArraySubscriptExpression with " + arrayExpression.toString());

      System.err.println("Is composed of " + arrayExpression.getArrayExpression() + " " + arrayExpression.getSubscriptExpression());
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.dom.ast.ASTVisitor#leave(org.eclipse.cdt.core.dom.ast.IASTExpression)
     */
    @Override
    public int leave(IASTExpression expression) {
      System.err.println("Got into IASTExpression with " + expression.toString());

      if (!(expression.getExpressionType() instanceof IPointerType) &&
          !(expression.getExpressionType() instanceof IArrayType)) return PROCESS_CONTINUE;

      // happy casting *ARRRGH***
      if (expression instanceof IASTBinaryExpression) {
        handle((IASTBinaryExpression)expression);
      } else if (expression instanceof IASTUnaryExpression) {
        handle((IASTUnaryExpression)expression);
      } else if (expression instanceof IASTArraySubscriptExpression) {
        handle((IASTArraySubscriptExpression)expression);
      } else if (expression instanceof IASTIdExpression) {
      } else if (expression instanceof IASTIdExpression) {
        System.err.println("Got into IASTName");
        relations.put(expression, pointsToElement.lookup(((IASTIdExpression)expression).getName()));
        assert (relations.get(expression) != null);
      } else {
        System.err.println("Unhandled expression " + expression);
        assert (false);
      }
      return PROCESS_CONTINUE;
    }
  }

  private class XVisitor extends ASTVisitor {

    private final PointsToElement pointsToElement;
    private final PointsToRelation relation;

    public XVisitor (PointsToElement pointsToElement) {
      this.pointsToElement = pointsToElement;
      relation = null;

      shouldVisitNames = true;
      shouldVisitDeclarations = true;
      shouldVisitInitializers = true;
      shouldVisitParameterDeclarations = true;
      shouldVisitDeclarators = true;
      shouldVisitDeclSpecifiers = true;
      shouldVisitExpressions = true;
      shouldVisitStatements = true;
      shouldVisitTypeIds = true;
      shouldVisitEnumerators = true;
      shouldVisitTranslationUnit = true;
      shouldVisitProblems = true;
    }
  }

  /* (non-Javadoc)
   * @see cpaplugin.cpa.common.interfaces.TransferRelation#getAbstractSuccessor(cpaplugin.cpa.common.interfaces.AbstractElement, cpaplugin.cfa.objectmodel.CFAEdge)
   */
  public AbstractElement getAbstractSuccessor(AbstractElement element,
                                              CFAEdge cfaEdge) throws CPATransferException {
    PointsToElement pointsToElement = (PointsToElement) element;
    System.err.println("Input: " + pointsToElement.toString());

    switch (cfaEdge.getEdgeType ())
    {
    case StatementEdge:
    {
      pointsToElement = pointsToElement.clone ();
      StatementVisitor visitor = new StatementVisitor(pointsToElement);
      StatementEdge statementEdge = (StatementEdge) cfaEdge;
      IASTExpression expression = statementEdge.getExpression ();
      System.err.println("Statement Edge = " + expression.getRawSignature());
      expression.accept(visitor);
      break;
    }
    case MultiStatementEdge:
    {
      pointsToElement = pointsToElement.clone ();
      StatementVisitor visitor = new StatementVisitor(pointsToElement);
      MultiStatementEdge multiStatementEdge = (MultiStatementEdge) cfaEdge;
      for (IASTExpression expression : multiStatementEdge.getExpressions ())
        expression.accept(visitor);

      break;
    }
    case DeclarationEdge:
    {
      pointsToElement = pointsToElement.clone();
      DeclarationVisitor visitor = new DeclarationVisitor(pointsToElement);
      DeclarationEdge declarationEdge = (DeclarationEdge) cfaEdge;
      IASTDeclarator [] declarators = declarationEdge.getDeclarators ();
      System.err.println("Decleration Edge = " + declarationEdge.getRawStatement());
      for (IASTDeclarator declarator : declarators) {
        declarator.accept(visitor);
      }
      break;
    }
    case MultiDeclarationEdge:
    {
      pointsToElement = pointsToElement.clone ();
      DeclarationVisitor visitor = new DeclarationVisitor(pointsToElement);
      MultiDeclarationEdge multiDeclarationEdge = (MultiDeclarationEdge) cfaEdge;
      for (IASTDeclarator [] declarators : multiDeclarationEdge.getDeclarators ())
        for (IASTDeclarator declarator : declarators) {
          declarator.accept(visitor);
        }

      break;
    }
    case AssumeEdge:
    case FunctionCallEdge:
    case ReturnEdge:
    default:
    {
      System.err.println("Edge " + cfaEdge + " not handled in points-to transfer relation");
    }
    }

    System.err.println("Output: " + pointsToElement.toString());
    return pointsToElement;
  }

  /* (non-Javadoc)
   * @see cpaplugin.cpa.common.interfaces.TransferRelation#getAllAbstractSuccessors(cpaplugin.cpa.common.interfaces.AbstractElement)
   */
  public List<AbstractElement> getAllAbstractSuccessors(
      AbstractElement element) throws CPAException, CPATransferException {
    assert (false);
    return null;
  }

}
