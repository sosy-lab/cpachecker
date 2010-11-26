package org.sosy_lab.cpachecker.fllesh.cfa;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.sosy_lab.cpachecker.cfa.ast.IASTDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IASTNode;
import org.sosy_lab.cpachecker.cfa.ast.IASTProblem;
import org.sosy_lab.cpachecker.cfa.ast.IASTTranslationUnit;
import org.eclipse.core.runtime.CoreException;
import org.sosy_lab.common.Files;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.cfa.CFABuilder;
import org.sosy_lab.cpachecker.cfa.CFATopologicalSort;
import org.sosy_lab.cpachecker.cfa.CPASecondPassBuilder;
import org.sosy_lab.cpachecker.cfa.DOTBuilder;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.CallToReturnEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.ReturnEdge;
import org.sosy_lab.cpachecker.util.CParser;
import org.sosy_lab.cpachecker.util.CParser.Dialect;

public class TranslationUnit {

  private Map<String, CFAFunctionDefinitionNode> mCFAs;
  private List<IASTDeclaration> mGlobalDeclarations;

  public TranslationUnit() {
    mCFAs = new HashMap<String, CFAFunctionDefinitionNode>();
    mGlobalDeclarations = new LinkedList<IASTDeclaration>();
  }
  
  public TranslationUnit(TranslationUnit pTranslationUnit) {
    this();
    
    mCFAs.putAll(pTranslationUnit.mCFAs);
    mGlobalDeclarations.addAll(pTranslationUnit.mGlobalDeclarations);
  }
  
  private TranslationUnit(Map<String, CFAFunctionDefinitionNode> pCFAs, List<IASTDeclaration> pGlobalDeclarations) {
    this();
    
    if (pCFAs == null) {
      throw new IllegalArgumentException();
    }
    
    mCFAs.putAll(pCFAs);
    mGlobalDeclarations.addAll(pGlobalDeclarations);
  }

  public void add(TranslationUnit pTranslationUnit) {
    add(pTranslationUnit.mCFAs);
    
    // TODO check for conflicting declarations
    mGlobalDeclarations.addAll(pTranslationUnit.mGlobalDeclarations);
  }
  
  public void add(Map<String, CFAFunctionDefinitionNode> pCFAs) {
    if (pCFAs == null) {
      throw new IllegalArgumentException();
    }
    
    for (String lFunctionName : pCFAs.keySet()) {
      if (mCFAs.containsKey(lFunctionName)) {
        throw new IllegalArgumentException("Function " + lFunctionName + " is already defined!");
      }
    }
    
    mCFAs.putAll(pCFAs);
  }
  
  public CFAFunctionDefinitionNode getFunction(String pFunctionName) {
    if (!mCFAs.containsKey(pFunctionName)) {
      throw new IllegalArgumentException("Function " + pFunctionName + " not present!");
    }
  
    return mCFAs.get(pFunctionName);
  }
  
  public Iterable<String> functionNames() {
    return mCFAs.keySet();
  }
  
  public Iterable<IASTDeclaration> globalDeclarations() {
    return mGlobalDeclarations;
  }
  
  public List<IASTDeclaration> getGlobalDeclarations() {
    return mGlobalDeclarations;
  }
  
  public boolean contains(String pFunctionName) {
    return mCFAs.containsKey(pFunctionName);
  }
  
  public boolean hasGlobalDeclarations() {
    return !mGlobalDeclarations.isEmpty();
  }
  
  public static TranslationUnit parseString(String pSource, LogManager pLogManager) {
    IASTTranslationUnit ast;
    try {
       ast = CParser.parseString(pSource, Dialect.C99);
    } catch (CoreException e) {
      throw new RuntimeException("Error during parsing C code \""
          + pSource + "\": " + e.getMessage());
    }

    checkForASTProblems(ast);

    CFABuilder lCFABuilder = new CFABuilder(pLogManager);

    ast.accept(lCFABuilder);

    Map<String, CFAFunctionDefinitionNode> cfas = lCFABuilder.getCFAs();
    
    // annotate CFA nodes with topological information for later use
    for(CFAFunctionDefinitionNode cfa : cfas.values()){
      CFATopologicalSort topSort = new CFATopologicalSort();
      topSort.topologicalSort(cfa);
    }
    
    TranslationUnit lTranslationUnit = new TranslationUnit(cfas, lCFABuilder.getGlobalDeclarations());
    
    return lTranslationUnit;
  }
  
  public static TranslationUnit parseFile(String pSourceFileName, LogManager pLogManager) {
    IASTTranslationUnit ast;
    try {
      ast = CParser.parseFile(pSourceFileName, Dialect.C99);
    } catch (CoreException e) {
      throw new RuntimeException("Error during parsing C code in file \""
          + pSourceFileName + "\": " + e.getMessage());
    } catch (IOException e) {
      throw new RuntimeException("Error during parsing C code in file \""
          + pSourceFileName + "\": " + e.getMessage());
    }

    checkForASTProblems(ast);

    CFABuilder lCFABuilder = new CFABuilder(pLogManager);

    ast.accept(lCFABuilder);
    
    List<IASTDeclaration> lGlobalDeclarations = lCFABuilder.getGlobalDeclarations();

    Map<String, CFAFunctionDefinitionNode> lCfas = lCFABuilder.getCFAs();
    
    // annotate CFA nodes with topological information for later use
    for(CFAFunctionDefinitionNode cfa : lCfas.values()){
      CFATopologicalSort topSort = new CFATopologicalSort();
      topSort.topologicalSort(cfa);
    }
    
    TranslationUnit lTranslationUnit = new TranslationUnit(lCfas, lGlobalDeclarations);

    return lTranslationUnit;
  }
  
  private static void checkForASTProblems(IASTNode pAST) {
    if (pAST instanceof IASTProblem) {
      throw new RuntimeException("Error during parsing C code \""
          + pAST.getRawSignature() + "\": " + ((IASTProblem)pAST).getMessage());
    } else {
      for (IASTNode n : pAST.getChildren()) {
        checkForASTProblems(n);
      }
    }
  }
  
  public Map<CallToReturnEdge, CFAEdge> insertCallEdgesRecursively(String pEntryFunction) {
    CPASecondPassBuilder lBuilder = new CPASecondPassBuilder(mCFAs, true);
    lBuilder.insertCallEdgesRecursively(pEntryFunction);
    return lBuilder.getMappingToReplacedEdges();
  }
  
  public void toDot(String pFunction, File pFile) throws IOException {
    CFAFunctionDefinitionNode lEntry = getFunction(pFunction);
    Files.writeFile(pFile, DOTBuilder.generateDOT(mCFAs.values(), lEntry));
  }
  
  /**
   * 
   * @param pFunctionEntry Function entry node of function that should replace the function with the same name currently used in the CFA.
   * @return Function entry node of function originally present in the CFA.
   */
  public CFAFunctionDefinitionNode replace(CFAFunctionDefinitionNode pFunctionEntry, LogManager lLogManager) {
    
    // TODO check for same number and type of parameters?
    if (!contains(pFunctionEntry.getFunctionName())) {
      throw new IllegalArgumentException();
    }
    
    CFAFunctionDefinitionNode lOldFunctionEntry = getFunction(pFunctionEntry.getFunctionName());
    
    while (lOldFunctionEntry.getNumEnteringEdges() > 0) {
      CFAEdge lEnteringEdge = lOldFunctionEntry.getEnteringEdge(0);
      
      FunctionCallEdge lOldCallEdge = (FunctionCallEdge)lEnteringEdge;
      
      // no support for external calls for now
      if (lOldCallEdge.isExternalCall()) {
        throw new RuntimeException();
      }
      
      CFANode lPredecessor = lOldCallEdge.getPredecessor();
      
      if (lPredecessor.getNumLeavingEdges() != 1) {
        throw new RuntimeException("Number of leaving edges is " + lPredecessor.getNumLeavingEdges());
      }
      
      CallToReturnEdge lCallSite = lPredecessor.getLeavingSummaryEdge();
      
      if (lCallSite == null) {
        throw new RuntimeException();
      }
      
      CFANode lSuccessor = lCallSite.getSuccessor();
      
      if (lSuccessor.getNumEnteringEdges() != 1) {
        throw new RuntimeException("Number of entering edges is " + lSuccessor.getNumEnteringEdges());
      }
      
      ReturnEdge lOldReturnEdge = (ReturnEdge)lSuccessor.getEnteringEdge(0);
      
      FunctionCallEdge lNewCallEdge = new FunctionCallEdge(
          lOldCallEdge.getRawStatement(),
          lOldCallEdge.getRawAST(),
          lOldCallEdge.getLineNumber(),
          lPredecessor,
          pFunctionEntry,
          lOldCallEdge.getArguments(),
          lOldCallEdge.isExternalCall()
          );
      
      ReturnEdge lNewReturnEdge = new ReturnEdge(
          lOldReturnEdge.getRawStatement(),
          lOldReturnEdge.getLineNumber(),
          pFunctionEntry.getExitNode(),
          lSuccessor
          );
      
      lNewCallEdge.addToCFA(lLogManager);
      lPredecessor.removeLeavingEdge(lOldCallEdge);
      lOldCallEdge.getSuccessor().removeEnteringEdge(lOldCallEdge);
      
      lNewReturnEdge.addToCFA(lLogManager);
      lSuccessor.removeEnteringEdge(lOldReturnEdge);
      lOldReturnEdge.getPredecessor().removeLeavingEdge(lOldReturnEdge);
    }
    
    // update CFA mapping
    mCFAs.put(pFunctionEntry.getFunctionName(), pFunctionEntry);
  
    return lOldFunctionEntry;
  }
  
}
