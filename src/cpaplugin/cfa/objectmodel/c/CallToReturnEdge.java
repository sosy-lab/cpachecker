package cpaplugin.cfa.objectmodel.c;

import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTExpression;

import cpaplugin.cfa.objectmodel.AbstractCFAEdge;
import cpaplugin.cfa.objectmodel.CFAEdgeType;
import cpaplugin.cfa.objectmodel.CFANode;
import cpaplugin.cpa.common.CompositeElement;
import cpaplugin.cpa.common.interfaces.AbstractElement;

public class CallToReturnEdge extends AbstractCFAEdge {

	private IASTExpression expression;
	private AbstractElement abstractElement;

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

	public AbstractElement getAbstractElement() {
		return abstractElement;
	}

	public void setAbstractElement(AbstractElement abstractElement) {
		this.abstractElement = abstractElement;
	}
	
	public AbstractElement extractAbstractElement(String elementName){
		CompositeElement compElem = (CompositeElement) abstractElement;
		List<AbstractElement> compElems = compElem.getElements();
		for(AbstractElement item:compElems){
			if(item.getClass().getSimpleName().equals(elementName)){
				return item;
			}
		}
		return null;
	}
}
