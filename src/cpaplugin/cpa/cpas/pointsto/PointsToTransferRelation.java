/**
 * 
 */
package cpaplugin.cpa.cpas.pointsto;

import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTInitializer;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;
import org.eclipse.cdt.core.dom.ast.ASTVisitor;

import cpaplugin.cfa.objectmodel.CFAEdge;
import cpaplugin.cfa.objectmodel.CFANode;
import cpaplugin.cfa.objectmodel.c.AssumeEdge;
import cpaplugin.cfa.objectmodel.c.DeclarationEdge;
import cpaplugin.cfa.objectmodel.c.MultiDeclarationEdge;
import cpaplugin.cfa.objectmodel.c.MultiStatementEdge;
import cpaplugin.cfa.objectmodel.c.StatementEdge;
import cpaplugin.cpa.common.CPATransferException;
import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.TransferRelation;
import cpaplugin.exceptions.CPAException;

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
	
	private class PointsToVisitor extends ASTVisitor {

		private PointsToElement abstractElement;
		private CFAEdge cfaEdge;
		
		public PointsToVisitor (PointsToElement abstractElement, CFAEdge cfaEdge) {
			this.abstractElement = abstractElement;
			this.cfaEdge = cfaEdge;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.cdt.core.dom.ast.ASTVisitor#visit(org.eclipse.cdt.core.dom.ast.IASTDeclaration)
		 */
		@Override
		public int visit(IASTDeclaration declaration) {
			/*if (0 == declaration
					
					getDgetPointerOperators().length) continue;
			PointsToRelation r = pointsToElement.addVariable(declarator);
			IASTInitializer initializer = declarator.getInitializer ();
			if (initializer != null)
			{
				r.pointsTo(initializer.toString() + "=" + cfaEdge.getRawStatement());
			}*/
			return 0;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.cdt.core.dom.ast.ASTVisitor#visit(org.eclipse.cdt.core.dom.ast.IASTExpression)
		 */
		@Override
		public int visit(IASTExpression expression) {
			// TODO Auto-generated method stub
			return super.visit(expression);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.cdt.core.dom.ast.ASTVisitor#visit(org.eclipse.cdt.core.dom.ast.IASTInitializer)
		 */
		@Override
		public int visit(IASTInitializer initializer) {
			// TODO Auto-generated method stub
			return super.visit(initializer);
		}
		
	}

	private void handleExpression (PointsToElement pointsToElement, IASTExpression expression, CFAEdge cfaEdge)
	{
		if (expression instanceof IASTBinaryExpression)
		{
			IASTBinaryExpression binaryExpression = (IASTBinaryExpression) expression;

			IASTExpression op1 = binaryExpression.getOperand1();
			IASTExpression op2 = binaryExpression.getOperand2();

			System.out.println("================== " + op1.getRawSignature());
			System.out.println("================== " + op2.getRawSignature());

			switch (binaryExpression.getOperator ())
			{
			case IASTBinaryExpression.op_assign:
			case IASTBinaryExpression.op_binaryAndAssign:
			case IASTBinaryExpression.op_binaryOrAssign:
			case IASTBinaryExpression.op_binaryXorAssign:
			case IASTBinaryExpression.op_divideAssign:
			case IASTBinaryExpression.op_minusAssign:
			case IASTBinaryExpression.op_moduloAssign:
			case IASTBinaryExpression.op_multiplyAssign:
			case IASTBinaryExpression.op_plusAssign:
			case IASTBinaryExpression.op_shiftLeftAssign:
			case IASTBinaryExpression.op_shiftRightAssign:
			{
				System.err.println("op1: " + ((IASTUnaryExpression)binaryExpression.getOperand1()).getOperand().toString());
				/*
				String lParam = binaryExpression.getOperand1 ().getExpressionType().
				
				getRawSignature ();
				String lParam2 = binaryExpression.getOperand2 ().getRawSignature ();

				pointsToElement.addVariable(lParam).pointsTo(lParam2);
				*/
			}
			}
		}
		else if (expression instanceof IASTUnaryExpression)
		{
			IASTUnaryExpression unaryExpression = (IASTUnaryExpression) expression;
			int operator = unaryExpression.getOperator ();
			if (operator == IASTUnaryExpression.op_postFixDecr || operator == IASTUnaryExpression.op_postFixIncr
					|| operator == IASTUnaryExpression.op_prefixDecr || operator == IASTUnaryExpression.op_prefixIncr)
			{
				System.err.println("op: " + unaryExpression.getOperand().getExpressionType().toString());
				/*
				String lParam = unaryExpression.getOperand ().getRawSignature ();

				pointsToElement.addVariable(lParam).pointsTo(lParam);
				*/
			}
		}
	}

	private void handleDeclaration (PointsToElement pointsToElement, IASTDeclarator [] declarators, CFAEdge cfaEdge)
	{
		for (IASTDeclarator declarator : declarators)
		{
			
		}
	}

	/* (non-Javadoc)
	 * @see cpaplugin.cpa.common.interfaces.TransferRelation#getAbstractSuccessor(cpaplugin.cpa.common.interfaces.AbstractElement, cpaplugin.cfa.objectmodel.CFAEdge)
	 */
	public AbstractElement getAbstractSuccessor(AbstractElement element,
			CFAEdge cfaEdge) throws CPATransferException {
		PointsToElement pointsToElement = (PointsToElement) element;
		PointsToVisitor visitor = new PointsToVisitor(pointsToElement, cfaEdge);

		switch (cfaEdge.getEdgeType ())
		{
		case StatementEdge:
		{
			pointsToElement = pointsToElement.clone ();

			StatementEdge statementEdge = (StatementEdge) cfaEdge;
			IASTExpression expression = statementEdge.getExpression ();
			//System.err.println("Statement Edge = " + expression.getRawSignature());
			expression.accept(visitor);
			break;
		}
		case MultiStatementEdge:
		{
			pointsToElement = pointsToElement.clone ();
			MultiStatementEdge multiStatementEdge = (MultiStatementEdge) cfaEdge;

			for (IASTExpression expression : multiStatementEdge.getExpressions ())
				expression.accept(visitor);

			break;
		}
		case DeclarationEdge:
		{
			pointsToElement = pointsToElement.clone();

			DeclarationEdge declarationEdge = (DeclarationEdge) cfaEdge;
			IASTDeclarator [] declarators = declarationEdge.getDeclarators ();
			//System.err.println("Decleration Edge = " + declarationEdge.getRawStatement());
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
			MultiDeclarationEdge multiDeclarationEdge = (MultiDeclarationEdge) cfaEdge;

			for (IASTDeclarator [] declarators : multiDeclarationEdge.getDeclarators ())
				for (IASTDeclarator declarator : declarators) {
					declarator.accept(visitor);
				}

			break;
		}
		}

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
