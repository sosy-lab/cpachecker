package cpaplugin.cpa.cpas.interprocedural;

import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;

import cpaplugin.cfa.objectmodel.CFAEdge;
import cpaplugin.cfa.objectmodel.CFANode;
import cpaplugin.cfa.objectmodel.c.CallToReturnEdge;
import cpaplugin.cfa.objectmodel.c.FunctionCallEdge;
import cpaplugin.cfa.objectmodel.c.FunctionDefinitionNode;
import cpaplugin.cfa.objectmodel.c.ReturnEdge;
import cpaplugin.cfa.objectmodel.c.StatementEdge;
import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.TransferRelation;
import cpaplugin.exceptions.CPAException;
import cpaplugin.exceptions.OctagonTransferException;

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
			CFANode callerNode = ((CFANode)functionCallEdge.getPredecessor());

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
		
//		case StatementEdge:
//		{
//			ipElement = ipElement.clone();
//
//			StatementEdge statementEdge = (StatementEdge) cfaEdge;
//			IASTExpression expression = statementEdge.getExpression ();
//
//			// handling function return
//			if(statementEdge.isJumpEdge()){
//				System.out.println(" ++++++++++++++++++++ ");
//				System.out.println(statementEdge.getPredecessor().getFunctionName() + " " + statementEdge.getSuccessor().getFunctionName());
//				System.out.println(" ++++++++++++++++++++ ");
//			}
//			return ipElement;
//		}
		
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
System.out.println("do nothing  ================================");
				
				System.out.println(pfName);
				handleExitFromRecursiveCall(exitEdge);
			}
			else if(pfName.compareTo("main") == 0){
				System.out.println("do nothing  ================================");
				
				System.out.println(pfName);
			}
			else{
				System.out.println("remove call ================================");
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
