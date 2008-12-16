/**
 * 
 */
package programtesting;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFANode;
import cfa.objectmodel.c.AssumeEdge;
import cfa.objectmodel.c.DeclarationEdge;
import cfa.objectmodel.c.FunctionCallEdge;
import cfa.objectmodel.c.FunctionDefinitionNode;
import cfa.objectmodel.c.MultiDeclarationEdge;
import cfa.objectmodel.c.MultiStatementEdge;
import cfa.objectmodel.c.StatementEdge;

import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;

import cpa.symbpredabs.explicit.ExplicitAbstractElement;

/**
 * @author holzera
 *
 */
public class AbstractPathToCTranslator {
  public static Collection<List<String>> translatePaths(Collection<Deque<ExplicitAbstractElement>> pPaths) {
    assert(pPaths != null);
    
    int i = 0;
    
    Collection<List<String>> lPathPrograms = new HashSet<List<String>>();
    
    for (Deque<ExplicitAbstractElement> lAbstractPath : pPaths) {
      System.out.println("#### PATH " + i + " ####");
      
      List<String> lTranslation = translatePath(lAbstractPath);
      
      // TODO remove output
      System.out.println("Written program text:");
      
      for (String lProgramString : lTranslation) {
        System.out.println(lProgramString);
      }
      
      lPathPrograms.add(lTranslation);
      
      i++;
    }
    
    return lPathPrograms;
  }
  
  public static List<String> translatePath(Deque<ExplicitAbstractElement> pAbstractPath) {
    assert(pAbstractPath != null);
    assert(pAbstractPath.size() > 0);
    
    
    
    ExplicitAbstractElement lPredecessorElement = pAbstractPath.getFirst();
    
    if (pAbstractPath.size() == 1) {
      // special case
      
      // stack for program texts of different functions
      Stack<StringWriter> lProgramTextStack = new Stack<StringWriter>();
      
      // list of already finished program texts
      List<String> lProgramTexts = new ArrayList<String>();
      
      startFunction(0, lPredecessorElement.getLocationNode(), lProgramTextStack);
      
      endFunction(lProgramTextStack, lProgramTexts);
      
      return lProgramTexts;
    }
    
    
    boolean first = true;
    
    List<CFAEdge> lEdges = new ArrayList<CFAEdge>();
    
    for (ExplicitAbstractElement lElement : pAbstractPath) {
      if (first) {
        first = false;
        continue;
      }
      
      CFANode lPredecessorNode = lPredecessorElement.getLocationNode();
      CFANode lNode = lElement.getLocationNode();
      
      // reconstruct edge
      int lNumberOfFoundEdges = 0;
      
      for (int lIndex = 0; lIndex < lPredecessorNode.getNumLeavingEdges(); lIndex++) {
        CFAEdge lEdge = lPredecessorNode.getLeavingEdge(lIndex);
        
        if (lEdge.getSuccessor().equals(lNode)) {
          lEdges.add(lEdge);
          lNumberOfFoundEdges++;
        }
      }
      
      assert(lNumberOfFoundEdges == 1);
      
      lPredecessorElement = lElement;
    }
    
    return translatePath(lEdges);
  }
  
  public static void endFunction(Stack<StringWriter> pProgramTextStack, List<String> pProgramTexts) {
    assert(pProgramTextStack != null);

    StringWriter lStringWriter = pProgramTextStack.pop();
    PrintWriter pProgramText = new PrintWriter(lStringWriter);
    
    // finish function
    pProgramText.println("}");
    
    // function program text is finished, add it to set of program texts
    pProgramTexts.add(lStringWriter.toString());
  }
  
  public static PrintWriter startFunction(int pFunctionIndex, CFANode pNode, Stack<StringWriter> pProgramTextStack) {
    assert(pNode != null);
    assert(pNode instanceof FunctionDefinitionNode);
    assert(pProgramTextStack != null);
    
    FunctionDefinitionNode lFunctionDefinitionNode = (FunctionDefinitionNode)pNode;
    
    IASTFunctionDefinition lFunctionDefinition = lFunctionDefinitionNode.getFunctionDefinition();
    
    List<IASTParameterDeclaration> lFunctionParameters = lFunctionDefinitionNode.getFunctionParameters();
    
    String lFunctionHeader = lFunctionDefinition.getDeclSpecifier().getRawSignature() + " " + lFunctionDefinitionNode.getFunctionName() + "_" + pFunctionIndex + "(";
    
    boolean lFirstFunctionParameter = true;
    
    for (IASTParameterDeclaration lFunctionParameter : lFunctionParameters) {
      if (lFirstFunctionParameter) {
        lFirstFunctionParameter = false;
      }
      else {
        lFunctionHeader += ", ";
      }
      
      lFunctionHeader += lFunctionParameter.getRawSignature();
    }
    
    lFunctionHeader += ")";
    
    
    StringWriter lFunctionStringWriter = new StringWriter();
    
    pProgramTextStack.add(lFunctionStringWriter);
    
    PrintWriter lProgramText = new PrintWriter(lFunctionStringWriter);
    
    lProgramText.println(lFunctionHeader);
    
    lProgramText.println("{");
    
    return lProgramText;
  }
  
  public static List<String> translatePath(List<CFAEdge> pAbstractPath) {
    int lFunctionIndex = 0;
    
    // stack for program texts of different functions
    Stack<StringWriter> lProgramTextStack = new Stack<StringWriter>();
    
    // list of already finished program texts
    List<String> lProgramTexts = new ArrayList<String>();
    
    
    // program text for start function 
    PrintWriter lProgramText = startFunction(lFunctionIndex, pAbstractPath.get(0).getPredecessor(), lProgramTextStack);
    
    lFunctionIndex++;
    
    
    // process edges
    for (CFAEdge lEdge : pAbstractPath) {
      System.out.println(lEdge.getRawStatement());
      
      switch (lEdge.getEdgeType()) {
      case BlankEdge: {
        // nothing to do
        break;
      }
      case AssumeEdge: {
        AssumeEdge lAssumeEdge = (AssumeEdge)lEdge;
        
        String lExpressionString = lAssumeEdge.getExpression().getRawSignature();
        
        String lAssumptionString;
        
        if (lAssumeEdge.getTruthAssumption()) {
          lAssumptionString = lExpressionString;
        }
        else {
          lAssumptionString = "!(" + lExpressionString + ")";
        }
        
        lProgramText.println("__CPROVER_assume(" + lAssumptionString + ");");
        
        break;
      }
      case StatementEdge: {
        StatementEdge lStatementEdge = (StatementEdge)lEdge;
        
        lProgramText.println(lStatementEdge.getExpression().getRawSignature() + ";");
        
        break;
      }
      case DeclarationEdge: {
        DeclarationEdge lDeclarationEdge = (DeclarationEdge)lEdge;
        
        lProgramText.println(lDeclarationEdge.getRawStatement());
        
        /*IASTDeclarator[] lDeclarators = lDeclarationEdge.getDeclarators();
        
        assert(lDeclarators.length == 1);
        
        // TODO what about function pointers?
        lProgramText.println(lDeclarationEdge.getDeclSpecifier().getRawSignature() + " " + lDeclarators[0].getRawSignature() + ";");
          */      
        break;
      }
      case FunctionCallEdge: {
        FunctionCallEdge lFunctionCallEdge = (FunctionCallEdge)lEdge;
        
        String lFunctionName = lFunctionCallEdge.getSuccessor().getFunctionName();
        
        
        String lArgumentString = "(";
        
        boolean lFirstArgument = true;
        
        for (IASTExpression lArgument : lFunctionCallEdge.getArguments()) {
          if (lFirstArgument) {
            lFirstArgument = false;
          }
          else {
            lArgumentString += ", ";
          }
          
          lArgumentString += lArgument.getRawSignature();
        }
        
        lArgumentString += ")";
        
        
        lProgramText.println(lFunctionName + "_" + lFunctionIndex + lArgumentString + ";");
        
        
        lProgramText = startFunction(lFunctionIndex, lFunctionCallEdge.getSuccessor(), lProgramTextStack);
        
        lFunctionIndex++;
        
        break;
      }
      case ReturnEdge: {
        endFunction(lProgramTextStack, lProgramTexts);
        
        lProgramText = new PrintWriter(lProgramTextStack.peek());
        
        break;
      }
      case MultiStatementEdge: {
        MultiStatementEdge lMultiStatementEdge = (MultiStatementEdge)lEdge;
        
        for (IASTExpression lExpression : lMultiStatementEdge.getExpressions()) {
          lProgramText.println(lExpression.getRawSignature() + ";");
        }
        
        break;
      }
      case MultiDeclarationEdge: {
        MultiDeclarationEdge lMultiDeclarationEdge = (MultiDeclarationEdge)lEdge;
        
        lProgramText.println(lMultiDeclarationEdge.getRawStatement());
        
        /*List<IASTDeclarator[]> lDecls = lMultiDeclarationEdge.getDeclarators();
        
        lMultiDeclarationEdge.getRawStatement()
        
        for (IASTDeclarator[] lDeclarators : lDecls) {
          
        }*/
        
        break;
      }
      case CallToReturnEdge: {
        // this should not have been taken
        assert(false);
        
        break;
      }
      default: {
        assert(false);
      }
      }
    }
    
    // clean stack and finish functions
    while (!lProgramTextStack.isEmpty()) {
      endFunction(lProgramTextStack, lProgramTexts);
    }
    
    return lProgramTexts;
  }
}
