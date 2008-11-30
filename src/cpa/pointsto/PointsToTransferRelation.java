/**
 * 
 */
package cpa.pointsto;

import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTInitializerExpression;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTInitializer;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;
import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IPointerType;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFANode;
import cfa.objectmodel.c.AssumeEdge;
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
	 * @see cpa.common.interfaces.TransferRelation#getAbstractDomain()
	 * 
	 * TODO Why do we always have that getAbstractDomain stuff?
	 */
	public AbstractDomain getAbstractDomain() {
		return abstractDomain;
	}
	
	private class PointsToVisitor extends ASTVisitor {

		private PointsToElement pointsToElement;
		private PointsToRelation relation;
		//private CFAEdge cfaEdge;
		
		public PointsToVisitor (PointsToElement pointsToElement, CFAEdge cfaEdge) {
			this.pointsToElement = pointsToElement;
			relation = null;
			//this.cfaEdge = cfaEdge;
			
			// just enable everything, even if we don't use it at the moment
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
		
		@Override
		public int visit (IASTName name) {
			System.err.println("Got into IASTName");
			assert (null == relation);
			relation = pointsToElement.lookup(name);
			if (null == relation) {
				System.err.println("Untracked name: " + name.getRawSignature());
				return PROCESS_ABORT;
			}

			return super.visit(name);
		}
		
		@Override
		public int visit (IASTDeclarator declarator) {
			System.err.println("Got into IASTDeclarator");
			if (0 != declarator.getPointerOperators().length) {
				relation = pointsToElement.addVariable(declarator);
				IASTInitializer initializer = declarator.getInitializer ();
				if (initializer != null) initializer.accept(this);
			}
			return PROCESS_ABORT;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.cdt.core.dom.ast.ASTVisitor#visit(org.eclipse.cdt.core.dom.ast.IASTInitializer)
		 */
		@Override
		public int visit (IASTInitializer initializer) {
			assert (null != relation);
			// I love the visitor pattern, if only those guys had provided a complete interface ...
			if (initializer instanceof IASTInitializerExpression) {
				IASTExpression e = ((IASTInitializerExpression)initializer).getExpression();
				if (e.getRawSignature().equals("NULL") || e.toString().equals("0")) {
					relation.pointsToNull();
				} else {
					relation.pointsTo(e.getRawSignature());
				}
			} else {
				System.err.println("No implementation for " + initializer.toString());
				assert (false);
			}
			// no further processing required, but rewrite in terms of moving the above into 
			// IASTExpression processing may be useful
			return PROCESS_ABORT;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.cdt.core.dom.ast.ASTVisitor#visit(org.eclipse.cdt.core.dom.ast.IASTExpression)
		 */
		@Override
		public int visit(IASTExpression expression) {
			System.err.println("Got into IASTExpression with " + expression.toString());
			
			// happy casting *ARRRGH***
			if (expression instanceof IASTBinaryExpression)
			{
				IASTBinaryExpression binaryExpression = (IASTBinaryExpression) expression;

				IASTExpression lhs = binaryExpression.getOperand1();
				if (!lhs.accept(this)) return PROCESS_ABORT;
				assert (null != relation);
				
				IASTExpression rhs = binaryExpression.getOperand2();

				switch (binaryExpression.getOperator ())
				{
				case IASTBinaryExpression.op_assign:
				{
					if (rhs.getRawSignature().equals("NULL") || rhs.toString().equals("0")) {
						relation.pointsToNull();
					} else {
						relation.pointsTo(rhs.getRawSignature());
					}
					break;
				}
				case IASTBinaryExpression.op_minusAssign:
				{
					relation.updateAll("- " + rhs.getRawSignature());
					break;
				}
				case IASTBinaryExpression.op_plusAssign:
				{
					relation.updateAll("- " + rhs.getRawSignature());
					break;
				}
				default:
				{
					System.err.println("Unhandled expression " + binaryExpression.getRawSignature());
				}
				}
				return PROCESS_ABORT;
			}
			else if (expression instanceof IASTUnaryExpression)
			{
				IASTUnaryExpression unaryExpression = (IASTUnaryExpression) expression;
				if (!unaryExpression.getOperand().accept(this)) return PROCESS_ABORT;
				assert (null != relation);
				
				if (!(unaryExpression.getExpressionType() instanceof IPointerType)) return PROCESS_ABORT;
				
				switch (unaryExpression.getOperator()) {
				case IASTUnaryExpression.op_postFixDecr:
				case IASTUnaryExpression.op_prefixDecr:
				{
					relation.updateAll("- 1");
					break;
				}
				case IASTUnaryExpression.op_postFixIncr:
				case IASTUnaryExpression.op_prefixIncr:
				{
					relation.updateAll("+ 1");
					break;
				}
				default:
				{
					System.err.println("Unhandled expression " + unaryExpression.getRawSignature());
				}
				}
			}
			
			return super.visit(expression);
		}

		
	}

	/* (non-Javadoc)
	 * @see cpa.common.interfaces.TransferRelation#getAbstractSuccessor(cpa.common.interfaces.AbstractElement, cfa.objectmodel.CFAEdge)
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
			PointsToVisitor visitor = new PointsToVisitor(pointsToElement, cfaEdge);
			StatementEdge statementEdge = (StatementEdge) cfaEdge;
			IASTExpression expression = statementEdge.getExpression ();
			System.err.println("Statement Edge = " + expression.getRawSignature());
			expression.accept(visitor);
			break;
		}
		case MultiStatementEdge:
		{
			pointsToElement = pointsToElement.clone ();
			PointsToVisitor visitor = new PointsToVisitor(pointsToElement, cfaEdge);
			MultiStatementEdge multiStatementEdge = (MultiStatementEdge) cfaEdge;
			for (IASTExpression expression : multiStatementEdge.getExpressions ())
				expression.accept(visitor);

			break;
		}
		case DeclarationEdge:
		{
			pointsToElement = pointsToElement.clone();
			PointsToVisitor visitor = new PointsToVisitor(pointsToElement, cfaEdge);
			DeclarationEdge declarationEdge = (DeclarationEdge) cfaEdge;
			IASTDeclarator [] declarators = declarationEdge.getDeclarators ();
			System.err.println("Decleration Edge = " + declarationEdge.getRawStatement());
			for (IASTDeclarator declarator : declarators) {
				declarator.accept(visitor);
			}
			break;
		}
		/*
		case AssumeEdge:
		{
			AssumeEdge assumeEdge = (AssumeEdge) cfaEdge;
			System.out.println("Assume Edge = " + assumeEdge.getRawStatement());
			break;
		}
		*/
		case MultiDeclarationEdge:
		{
			pointsToElement = pointsToElement.clone ();
			PointsToVisitor visitor = new PointsToVisitor(pointsToElement, cfaEdge);
			MultiDeclarationEdge multiDeclarationEdge = (MultiDeclarationEdge) cfaEdge;
			for (IASTDeclarator [] declarators : multiDeclarationEdge.getDeclarators ())
				for (IASTDeclarator declarator : declarators) {
					declarator.accept(visitor);
				}

			break;
		}
		}

		//System.err.println("Output: " + pointsToElement.toString());
		return pointsToElement;
	}

	/* (non-Javadoc)
	 * @see cpa.common.interfaces.TransferRelation#getAllAbstractSuccessors(cpa.common.interfaces.AbstractElement)
	 */
	public List<AbstractElement> getAllAbstractSuccessors(
			AbstractElement element) throws CPAException, CPATransferException {
        assert (false);
        return null;
	}

}
