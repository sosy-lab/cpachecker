package cpaplugin.cfa;

import java.util.ArrayDeque;
import java.util.Deque;

import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpressionList;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;

import cpaplugin.cfa.objectmodel.CFAEdge;
import cpaplugin.cfa.objectmodel.CFANode;
import cpaplugin.cfa.objectmodel.c.CallToReturnEdge;
import cpaplugin.cfa.objectmodel.c.FunctionCallEdge;
import cpaplugin.cfa.objectmodel.c.ReturnEdge;
import cpaplugin.cfa.objectmodel.c.StatementEdge;

public class CPASecondPassBuilder {

	private CFAMap cfas;

	public CPASecondPassBuilder(CFAMap map){
		cfas = map;
	}

	public void insertCallEdges(String funcName){
		Deque<CFANode> workList = new ArrayDeque<CFANode> ();
		Deque<CFANode> processedList = new ArrayDeque<CFANode> ();

		CFANode initialNode = cfas.getCFA(funcName);

		workList.addLast (initialNode);
		processedList.addLast (initialNode);

		while (!workList.isEmpty ())
		{
			CFANode node = workList.pollFirst ();
			int numLeavingEdges = node.getNumLeavingEdges ();

			for (int edgeIdx = 0; edgeIdx < numLeavingEdges; edgeIdx++)
			{
				CFAEdge edge = node.getLeavingEdge (edgeIdx);
				CFANode successorNode = edge.getSuccessor();
				System.out.println("+++++++++++++++++++ " + edge.getRawStatement());
				if(edge instanceof StatementEdge)
				{
					IASTExpression expr = ((StatementEdge)edge).getExpression();
					
					// TODO add the case for call(2, a)
					if(expr instanceof IASTBinaryExpression){
						IASTExpression operand2 = ((IASTBinaryExpression)expr).getOperand2();
						if(operand2 instanceof IASTFunctionCallExpression){
							createCallAndReturnEdges(node, successorNode, edge, expr, (IASTFunctionCallExpression)operand2);
						}
						else{
							successorNode.setFunctionName(node.getFunctionName());
						}
					}
					
					else if(expr instanceof IASTFunctionCallExpression){
						IASTFunctionCallExpression functionCall = (IASTFunctionCallExpression)expr;
						createCallAndReturnEdges(node, successorNode, edge, expr, functionCall);
					}
					
					else{
						successorNode.setFunctionName(node.getFunctionName());
					}
				}
				
				else if(!((edge instanceof FunctionCallEdge) ||
						(edge instanceof ReturnEdge))){
					successorNode.setFunctionName(node.getFunctionName());
				}
				
				if(!processedList.contains(successorNode) &&
				   node.getFunctionName().equals(successorNode.getFunctionName())){
					workList.add(successorNode);
				}
			}
			processedList.add(node);
		}
	}

	private void createCallAndReturnEdges(CFANode node, CFANode successorNode, CFAEdge edge, IASTExpression expr, IASTFunctionCallExpression operand2) {
		IASTFunctionCallExpression functCall = (IASTFunctionCallExpression) operand2;
		String functionName = functCall.getFunctionNameExpression().getRawSignature();
		IASTExpression parameters = functCall.getParameterExpression();
		FunctionCallEdge callEdge = new FunctionCallEdge(operand2.getRawSignature(), parameters);
		callEdge.initialize (node, cfas.getCFA(functionName));
		// set name of the function
		callEdge.getSuccessor().setFunctionName(functionName);
		// set return edge from exit node of the function
		ReturnEdge returnEdge = new ReturnEdge("Return Edge to " + successorNode.getNodeNumber());
		returnEdge.initialize(cfas.getCFA(functionName).getExitNode(), successorNode);
		returnEdge.getSuccessor().setFunctionName(node.getFunctionName());
		
		CallToReturnEdge calltoReturnEdge = new CallToReturnEdge("Summary Edge", expr);
		calltoReturnEdge.initializeSummaryEdge(node, successorNode);
		
		node.removeLeavingEdge(edge);
		successorNode.removeEnteringEdge(edge);
		
		// set function parameters
		if(parameters instanceof IASTIdExpression){
			IASTIdExpression variableParam = (IASTIdExpression)parameters;
			IASTExpression[] expressionList = new IASTExpression[1];
			expressionList[0] = variableParam;
			callEdge.setArguments(expressionList);
		}
		else if(parameters instanceof IASTExpressionList){
			IASTExpressionList paramList = (IASTExpressionList)parameters;
			IASTExpression[] expressionList = paramList.getExpressions();
			callEdge.setArguments(expressionList);
		}
	}
}
