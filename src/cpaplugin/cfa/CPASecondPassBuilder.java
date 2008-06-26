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
				
				if(edge instanceof StatementEdge)
				{
					IASTExpression expr = ((StatementEdge)edge).getExpression();

					// TODO add the case for call(2, a)
					if(expr instanceof IASTBinaryExpression){
						IASTExpression operand2 = ((IASTBinaryExpression)expr).getOperand2();
						if(operand2 instanceof IASTFunctionCallExpression){
							IASTFunctionCallExpression functCall = (IASTFunctionCallExpression) operand2;
							String functionName = functCall.getFunctionNameExpression().getRawSignature();
							IASTExpression parameters = functCall.getParameterExpression();
							
							FunctionCallEdge callEdge = new FunctionCallEdge(operand2.getRawSignature(), parameters);
							callEdge.initialize (node, cfas.getCFA(functionName));
							
							// set return edge from exit node of the function
							ReturnEdge returnEdge = new ReturnEdge("return edge");
							returnEdge.initialize(cfas.getCFA(functionName).getExitNode(), successorNode);
							successorNode.setFunctionName(node.getFunctionName());
							CallToReturnEdge calltoReturnEdge = new CallToReturnEdge("summaryEdge", expr);
							calltoReturnEdge.initializeSummaryEdge(node, successorNode);
							node.removeLeavingEdge(edge);
							successorNode.removeEnteringEdge(edge);
							
							if(!cfas.getCFA(functionName).CFAProcessed){
								cfas.getCFA(functionName).CFAProcessed = true;
								insertCallEdges(functionName);
							}
							
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
						else{
							successorNode.setFunctionName(node.getFunctionName());
						}
					}
					
					if(expr instanceof IASTFunctionCallExpression){
						IASTExpression functionCall = ((IASTFunctionCallExpression)expr).getFunctionNameExpression();
						System.out.println(" Function call expr #2  " + functionCall.getRawSignature());
					}
					
					else{
						successorNode.setFunctionName(node.getFunctionName());
					}
				}
				
				else if(edge instanceof ReturnEdge){
					
				}
				
				else{
					successorNode.setFunctionName(node.getFunctionName());
				}

				System.out.println("Node number #2 " + successorNode.getNodeNumber());
				if(!processedList.contains(successorNode)){
					workList.add(successorNode);
				}
			}

			processedList.add(node);
		}
	}
}
