/**
 * 
 */
package cpa.pointsto;

import java.util.List;
import java.util.HashMap;

import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTInitializerExpression;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTInitializer;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;
import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IPointerType;
import org.eclipse.cdt.core.dom.ast.IASTNode;

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

	/* (non-Javadoc)
	 * @see cpaplugin.cpa.common.interfaces.TransferRelation#getAbstractDomain()
	 * 
	 * TODO Why do we always have that getAbstractDomain stuff?
	 */
	public AbstractDomain getAbstractDomain() {
		return abstractDomain;
	}
	
	private class DeclarationVisitor extends ASTVisitor {

		private PointsToElement pointsToElement;
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
				relation = pointsToElement.addVariable(declarator, 0);
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
			System.err.println("Got into IASTExpression with " + expression.toString());
			assert (null != relation);
			
			if (expression.getRawSignature().equals("NULL") || expression.toString().equals("0")) {
				relation.pointsToNull();
			} else {
				relation.pointsTo(expression.getRawSignature());
			}
			return super.visit(expression);
		}
	}
			
		
	
	private class StatementVisitor extends ASTVisitor {

		private PointsToElement pointsToElement;
		private HashMap<IASTNode,PointsToRelation> relations;
		
		public StatementVisitor (PointsToElement pointsToElement) {
			this.pointsToElement = pointsToElement;
			this.relations = new HashMap<IASTNode,PointsToRelation>();
			
			shouldVisitExpressions = true;
			shouldVisitStatements = true;
		}
		
		private void handle(IASTBinaryExpression binaryExpression) {
			
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
						entryLhs.pointsToNull();
					} else {
						entryLhs.pointsTo(rhs.getRawSignature());
					}
				} else {
					// entryLhs.setPointsTo(entryRhs);
				}
				relations.put(binaryExpression, entryLhs);
				break;
			}
			// X - Y
			case IASTBinaryExpression.op_minus:
			{
				relations.put(binaryExpression.getOperand1(),
						relations.get(binaryExpression.getOperand1()).clone());
			}
			// X -= Y
			case IASTBinaryExpression.op_minusAssign:
			{
				PointsToRelation entryLhs = relations.get(binaryExpression.getOperand1());
				assert (entryLhs != null);
				IASTExpression rhs = binaryExpression.getOperand2();
				PointsToRelation entryRhs = relations.get(rhs);
				assert (entryRhs == null);
				// entryLhs.updateAll("- " + rhs.getRawSignature());
				relations.put(binaryExpression, entryLhs);
				break;
			}
			// X + Y
			case IASTBinaryExpression.op_plus:
			{
				relations.put(binaryExpression.getOperand1(),
						relations.get(binaryExpression.getOperand1()).clone());
			}
			// X += Y
			case IASTBinaryExpression.op_plusAssign:
			{
				PointsToRelation entryLhs = relations.get(binaryExpression.getOperand1());
				assert (entryLhs != null);
				IASTExpression rhs = binaryExpression.getOperand2();
				PointsToRelation entryRhs = relations.get(rhs);
				assert (entryRhs == null);
				// entryLhs.updateAll("+ " + rhs.getRawSignature());
				relations.put(binaryExpression, entryLhs);
				break;
			}
			default:
			{
				System.err.println("Unhandled expression " + binaryExpression.getRawSignature());
			}
			}
		}

		private void handle(IASTUnaryExpression unaryExpression) {
			
			PointsToRelation entry = relations.get(unaryExpression.getOperand());
			
			switch (unaryExpression.getOperator()) {
			// ++X, X++
			case IASTUnaryExpression.op_postFixDecr:
			case IASTUnaryExpression.op_prefixDecr:
			{
				assert (entry != null);
				// entry.updateAll("- 1");
				break;
			}
			// --X, X--
			case IASTUnaryExpression.op_postFixIncr:
			case IASTUnaryExpression.op_prefixIncr:
			{
				assert (entry != null);
				// entry.updateAll("+ 1");
				break;
			}
			// *X
			case IASTUnaryExpression.op_star:
			{
				assert (entry != null);
			}
			// &X
			case IASTUnaryExpression.op_amper:
			{
				// if X is not a pointer, just take expression as is and do nothing here
				// otherwise:
				if (null != entry) {
					// X is actually *Y
					
				}
			}
			default:
			{
				System.err.println("Unhandled expression " + unaryExpression.getRawSignature());
			}
			}
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.cdt.core.dom.ast.ASTVisitor#leave(org.eclipse.cdt.core.dom.ast.IASTExpression)
		 */
		@Override
		public int leave(IASTExpression expression) {
			System.err.println("Got into IASTExpression with " + expression.toString());
			
			if (!(expression.getExpressionType() instanceof IPointerType)) return PROCESS_CONTINUE;
			
			// happy casting *ARRRGH***
			if (expression instanceof IASTBinaryExpression)
			{
				handle((IASTBinaryExpression)expression);
			} else if (expression instanceof IASTUnaryExpression) {
				handle((IASTUnaryExpression)expression);
				
			} else if (expression instanceof IASTIdExpression) {
				System.err.println("Got into IASTName");
				/*relation = pointsToElement.lookup(name);
				if (null == relation) {
					System.err.println("Untracked name: " + name.getRawSignature());
					return PROCESS_ABORT;
				}*/
			} else {
				System.err.println("Unhandled expression " + expression);
				assert (false);
			}
			return PROCESS_CONTINUE;
		}
	}
	
	private class XVisitor extends ASTVisitor {

		private PointsToElement pointsToElement;
		private PointsToRelation relation;
		
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
		//System.err.println("Input: " + pointsToElement.toString());

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

		//System.err.println("Output: " + pointsToElement.toString());
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
