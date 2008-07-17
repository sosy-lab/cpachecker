package cpaplugin.cfa.objectmodel.c;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTExpression;

import cpaplugin.cfa.objectmodel.AbstractCFAEdge;
import cpaplugin.cfa.objectmodel.CFAEdgeType;
import cpaplugin.cfa.objectmodel.CFANode;
import cpaplugin.compositeCPA.CPAType;
import cpaplugin.cpa.common.interfaces.AbstractElement;

public class CallToReturnEdge extends AbstractCFAEdge {

	private IASTExpression expression;
	private HashMap<CPAType, AbstractElement> abstractElements;
	private List<AliasedPointers> aliasedList;
	private boolean hasAnyPointerParameters = false;

	public CallToReturnEdge(String rawStatement, IASTExpression exp) {
		super(rawStatement);
		this.expression = exp;
		abstractElements = new HashMap<CPAType, AbstractElement>();
		aliasedList = new ArrayList<AliasedPointers>();
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

	public void registerElementOnSummaryEdge(CPAType type, AbstractElement element){
		abstractElements.put(type, element);
	}

	public AbstractElement retrieveAbstractElement(CPAType type){
		return abstractElements.get(type);
	}

	public void registerAliasesOnFunctionCalls(String argumentName,
			String parameterName) {
			AliasedPointers ap = new AliasedPointers(argumentName, parameterName);
			aliasedList.add(ap);
	}
	
	public List<AliasedPointers>getAliasedPointersList(){
		return aliasedList;
	}

	public boolean hasAnyPointerParameters() {
		if(aliasedList.size() > 0){
			return true;
		}
		return false;
	}
}
