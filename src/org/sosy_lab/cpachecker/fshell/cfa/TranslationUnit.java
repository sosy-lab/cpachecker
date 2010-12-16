package org.sosy_lab.cpachecker.fshell.cfa;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTProblem;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.core.runtime.CoreException;
import org.sosy_lab.common.Files;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.cfa.CFABuilder;
import org.sosy_lab.cpachecker.cfa.CFATopologicalSort;
import org.sosy_lab.cpachecker.cfa.CFASecondPassBuilder;
import org.sosy_lab.cpachecker.cfa.DOTBuilder;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.util.CParser;
import org.sosy_lab.cpachecker.util.CParser.Dialect;

class TranslationUnit {

  private final Map<String, CFAFunctionDefinitionNode> mCFAs = new HashMap<String, CFAFunctionDefinitionNode>();
  private final List<IASTDeclaration> mGlobalDeclarations = new LinkedList<IASTDeclaration>();

  public TranslationUnit() {
  }
  
  private TranslationUnit(Map<String, CFAFunctionDefinitionNode> pCFAs, List<IASTDeclaration> pGlobalDeclarations) {
    assert pCFAs != null;
    
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
  
  public List<IASTDeclaration> getGlobalDeclarations() {
    return mGlobalDeclarations;
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
  
  public void insertCallEdgesRecursively(String pEntryFunction) {
    CFASecondPassBuilder lBuilder = new CFASecondPassBuilder(mCFAs, true);
    lBuilder.insertCallEdgesRecursively(pEntryFunction);
  }
  
  public void toDot(String pFunction, File pFile) throws IOException {
    CFAFunctionDefinitionNode lEntry = getFunction(pFunction);
    Files.writeFile(pFile, DOTBuilder.generateDOT(mCFAs.values(), lEntry));
  }

}
