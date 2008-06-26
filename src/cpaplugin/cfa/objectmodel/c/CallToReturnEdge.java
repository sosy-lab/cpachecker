package cpaplugin.cfa.objectmodel.c;

import org.eclipse.cdt.core.dom.ast.IASTExpression;

import cpaplugin.cfa.objectmodel.AbstractCFAEdge;
import cpaplugin.cfa.objectmodel.CFAEdgeType;
import cpaplugin.cfa.objectmodel.CFANode;

public class CallToReturnEdge extends AbstractCFAEdge {
	
	private IASTExpression expression;

	public CallToReturnEdge(String rawStatement, IASTExpression exp) {
		super(rawStatement);
		this.expression = exp;
	}

	public void initializeSummaryEdge(CFANode predecessorNode, CFANode successorNode) {
		predecessorNode.addLeavingSummaryEdge(this);
		predecessor = predecessorNode;
		successorNode.addEnteringSummaryEdge (this);
		successor= successorNode;
	}
	
	public IASTExpression getExpression ()
    {
        return expression;
    }
	
	public CFAEdgeType getEdgeType ()
    {
        return CFAEdgeType.CallToReturnEdge;
    }
}
