package cpaplugin.cpa.domains.defuse;

import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTInitializer;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;

import cpaplugin.cfa.objectmodel.CFAEdge;
import cpaplugin.cfa.objectmodel.c.DeclarationEdge;
import cpaplugin.cfa.objectmodel.c.MultiDeclarationEdge;
import cpaplugin.cfa.objectmodel.c.MultiStatementEdge;
import cpaplugin.cfa.objectmodel.c.StatementEdge;
import cpaplugin.common.CPAException;
import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.TransferRelation;

public class DefUseTransferRelation implements TransferRelation
{
    private DefUseDomain defUseDomain;

    public DefUseTransferRelation (DefUseDomain defUseDomain)
    {
        this.defUseDomain = defUseDomain;
    }

    public AbstractDomain getAbstractDomain ()
    {
        return defUseDomain;
    }

    private void handleExpression (DefUseElement defUseElement, IASTExpression expression, CFAEdge cfaEdge)
    {
        if (expression instanceof IASTBinaryExpression)
        {
            IASTBinaryExpression binaryExpression = (IASTBinaryExpression) expression;
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
                String lParam = binaryExpression.getOperand1 ().getRawSignature ();

                DefUseDefinition definition = new DefUseDefinition (lParam, cfaEdge);
                defUseElement.update (definition);
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
                String lParam = unaryExpression.getOperand ().getRawSignature ();

                DefUseDefinition definition = new DefUseDefinition (lParam, cfaEdge);
                defUseElement.update (definition);
            }
        }
    }

    private void handleDeclaration (DefUseElement defUseElement, IASTDeclarator [] declarators, CFAEdge cfaEdge)
    {
        for (IASTDeclarator declarator : declarators)
        {
            IASTInitializer initializer = declarator.getInitializer ();
            if (initializer != null)
            {
                String varName = declarator.getName ().getRawSignature ();
                DefUseDefinition definition = new DefUseDefinition (varName, cfaEdge);

                defUseElement.update (definition);
            }
        }
    }

    public AbstractElement getAbstractSuccessor (AbstractElement element, CFAEdge cfaEdge)
    {
        DefUseElement defUseElement = (DefUseElement) element;

        switch (cfaEdge.getEdgeType ())
        {
        case StatementEdge:
        {
            defUseElement = defUseElement.clone ();

            StatementEdge statementEdge = (StatementEdge) cfaEdge;
            IASTExpression expression = statementEdge.getExpression ();

            handleExpression (defUseElement, expression, cfaEdge);
            break;
        }
        case MultiStatementEdge:
        {
            defUseElement = defUseElement.clone ();
            MultiStatementEdge multiStatementEdge = (MultiStatementEdge) cfaEdge;

            for (IASTExpression expression : multiStatementEdge.getExpressions ())
                handleExpression (defUseElement, expression, cfaEdge);

            break;
        }
        case DeclarationEdge:
        {
            defUseElement = defUseElement.clone ();

            DeclarationEdge declarationEdge = (DeclarationEdge) cfaEdge;
            IASTDeclarator [] declarators = declarationEdge.getDeclarators ();

            handleDeclaration (defUseElement, declarators, cfaEdge);
            break;
        }
        case MultiDeclarationEdge:
        {
            defUseElement = defUseElement.clone ();
            MultiDeclarationEdge multiDeclarationEdge = (MultiDeclarationEdge) cfaEdge;

            for (IASTDeclarator [] declarators : multiDeclarationEdge.getDeclarators ())
                handleDeclaration (defUseElement, declarators, cfaEdge);

            break;
        }
        }

        return defUseElement;
    }

    public List<AbstractElement> getAllAbstractSuccessors (AbstractElement element) throws CPAException
    {
        throw new CPAException ("Cannot get all abstract successors from non-location domain");
    }
}
