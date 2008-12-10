package rfsGenerator;

import java.io.File;
import java.util.Collection;
import java.util.List;

import org.eclipse.cdt.core.dom.IASTServiceProvider;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.internal.core.dom.InternalASTServiceProvider;
import org.eclipse.core.resources.IFile;

import cfa.CFABuilder;
import cfa.CFAMap;
import cfa.CPASecondPassBuilder;
import cfa.DOTBuilder;
import cfa.DOTBuilderInterface;
import cfa.objectmodel.CFAFunctionDefinitionNode;
import cfa.objectmodel.CFANode;
import cmdline.CPAMain;
import cmdline.stubs.StubCodeReaderFactory;
import cmdline.stubs.StubConfiguration;
import cmdline.stubs.StubFile;

public class Generator {

  private static String globalVars;

  public static void main(String[] args) {

    try {
      String fileName = "test.c";
      IFile currentFile = new StubFile(fileName);
      // Get Eclipse to parse the C in the current file
      IASTTranslationUnit ast = null;
      try {
        IASTServiceProvider p = new InternalASTServiceProvider();
        ast = p.getTranslationUnit(currentFile,
            StubCodeReaderFactory.getInstance(),
            new StubConfiguration());
      } catch (Exception e) {
        e.printStackTrace();
        e.getMessage();

        System.out.println("Eclipse had trouble parsing C");
        return;
      }

      runAnalysis(ast);
    } catch (Exception e) {
      System.out.println(e.getMessage());
      e.printStackTrace();
      System.out.flush();
      System.err.flush();
    }

  }

  private static void runAnalysis(IASTTranslationUnit ast) throws Exception{

    CFAFunctionDefinitionNode mainFunction = null;

    // Build CFA
    CFABuilder builder = new CFABuilder();
    ast.accept(builder);
    CFAMap cfas = builder.getCFAs();
    Collection<CFAFunctionDefinitionNode> cfasMapList =
      cfas.cfaMapIterator();

    CFAFunctionDefinitionNode cfa = cfas.getCFA("main");
    // TODO Erkan Simplify each CFA
//  if (CPAMain.cpaConfig.getBooleanValue("cfa.simplify")) {
//  CFASimplifier simplifier = new CFASimplifier();
//  simplifier.simplify(cfa);
//  }

    // Insert call and return edges and build the supergraph
    CPASecondPassBuilder spbuilder = new CPASecondPassBuilder(cfas, false);
    for (CFAFunctionDefinitionNode cfa2 : cfasMapList){
      spbuilder.insertCallEdges(cfa2.getFunctionName());
    }

    // add global variables at the beginning of main
    mainFunction = cfa;
    List<IASTDeclaration> globalVars = builder.getGlobalDeclarations();
    mainFunction = addGlobalDeclarations(mainFunction, globalVars);
//
//      DOTBuilderInterface dotBuilder = null;
//      dotBuilder = new DOTBuilder();
//      String dotPath = "/localhome/erkan/";
//      dotBuilder.generateDOT(cfasMapList, mainFunction,
//          new File(dotPath, "dot_main.dot").getPath());

    RFSTraversal rfsT = new RFSTraversal();

    CFANode initialElement = cfa;
    rfsT.traverse(initialElement);

  }

  private static CFAFunctionDefinitionNode addGlobalDeclarations(
                                                                 CFAFunctionDefinitionNode pMainFunction,
                                                                 List<IASTDeclaration> pGlobalVars) {
    // TODO
    return null;
  }  
}
