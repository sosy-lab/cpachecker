package cpaplugin.cpa.cpas.interprocedural;

import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;

import cpaplugin.cfa.objectmodel.CFAEdge;
import cpaplugin.cfa.objectmodel.CFANode;
import cpaplugin.cfa.objectmodel.c.CallToReturnEdge;
import cpaplugin.cfa.objectmodel.c.FunctionCallEdge;
import cpaplugin.cfa.objectmodel.c.FunctionDefinitionNode;
import cpaplugin.cfa.objectmodel.c.ReturnEdge;
import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.TransferRelation;
import cpaplugin.exceptions.CPAException;

public class InterProceduralTransferRelation implements TransferRelation
{
    private InterProceduralDomain ipDomain;

    public InterProceduralTransferRelation (InterProceduralDomain interProDomain)
    {
        this.ipDomain = interProDomain;
    }

    public AbstractDomain getAbstractDomain ()
    {
        return this.ipDomain;
    }

    public AbstractElement getAbstractSuccessor (AbstractElement element, CFAEdge cfaEdge)
    {
    	InterProceduralElement ipElement = (InterProceduralElement) element;
    	
		switch (cfaEdge.getEdgeType ())
		{
		case FunctionCallEdge:
		{
			ipElement = ipElement.clone();
			FunctionCallEdge functionCallEdge = (FunctionCallEdge) cfaEdge;
			FunctionDefinitionNode calledNode = ((FunctionDefinitionNode)functionCallEdge.getSuccessor());
			FunctionDefinitionNode callerNode = ((FunctionDefinitionNode)functionCallEdge.getPredecessor());

			if(ipElement.containsCall(calledNode.getFunctionName())){
				handleRecursiveFunctionCall(functionCallEdge);
			}
			else{
				handleFunctionCall(functionCallEdge);
				CallElement ce = new CallElement(calledNode.getFunctionName(), callerNode.getNodeNumber());
				ipElement.addCallElement(ce);
			}
			return ipElement;
		}
		
		case ReturnEdge:
		{
			ipElement = ipElement.clone();
			ReturnEdge exitEdge = (ReturnEdge) cfaEdge;
			CFANode predecessorNode = exitEdge.getPredecessor();
			CFANode successorNode = exitEdge.getSuccessor();
			String sfName = successorNode.getFunctionName();
			String pfName = predecessorNode.getFunctionName();

			CallToReturnEdge summaryEdge = (CallToReturnEdge)successorNode.getEnteringSummaryEdge();

			if(pfName.compareTo(sfName) == 0 ){
				handleExitFromRecursiveCall(exitEdge);
			}
			else if(predecessorNode.getFunctionName().compareTo("main") == 0){
				//Do nothing
			}
			else{
				handleExitFromCall(exitEdge);
				ipElement.removeCallElement(pfName);
			}
			return ipElement;
		}
		default: return ipElement;
		}
    }

    private void handleExitFromCall(ReturnEdge exitEdge) {
    	// TODO Auto-generated method stub
	}

	private void handleExitFromRecursiveCall(ReturnEdge exitEdge) {
    	exitEdge.setExitFromRecursive();
	}

	private void handleFunctionCall(FunctionCallEdge functionCallEdge) {
		// TODO Auto-generated method stub
	}

	private void handleRecursiveFunctionCall(FunctionCallEdge functionCallEdge) {
    	if(!functionCallEdge.isRecursive())
    		functionCallEdge.setRecursive();
	}

	public List<AbstractElement> getAllAbstractSuccessors (AbstractElement element) throws CPAException
    {
    	throw new CPAException ("Cannot get all abstract successors from non-location domain");
    }
}
