package cpaplugin.cmdline;

import java.util.Collection;
import java.util.Vector;
import java.util.logging.Level;

import org.eclipse.cdt.core.dom.IASTServiceProvider;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.internal.core.dom.InternalASTServiceProvider;
import org.eclipse.core.resources.IFile;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import cpaplugin.CPAConfig;
import cpaplugin.cfa.CFABuilder;
import cpaplugin.cfa.CFAMap;
import cpaplugin.cfa.CFASimplifier;
import cpaplugin.cfa.CPASecondPassBuilder;
import cpaplugin.cfa.DOTBuilder;
import cpaplugin.cfa.objectmodel.CFAFunctionDefinitionNode;
import cpaplugin.cmdline.stubs.StubCodeReaderFactory;
import cpaplugin.cmdline.stubs.StubConfiguration;
import cpaplugin.cmdline.stubs.StubFile;
import cpaplugin.compositeCPA.CPAType;
import cpaplugin.compositeCPA.CompositeCPA;
import cpaplugin.cpa.common.CPAAlgorithm;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.ConfigurableProblemAnalysis;
import cpaplugin.exceptions.CPAException;
import cpaplugin.logging.CPACheckerLogger;
import cpaplugin.logging.CustomLogLevel;

public class CPAMain {
    private static ConfigurableProblemAnalysis getCPA(CPAType[] names, 
            CFAFunctionDefinitionNode node) throws CPAException {
        return CompositeCPA.getCompositeCPA(names, node);
    }
       
    public static void doRunAnalysis(IASTTranslationUnit ast)
        throws Exception {
        CPACheckerLogger.log(CustomLogLevel.MainApplicationLevel, "Parsing Finished");
        
        // Build CFA
        CFABuilder builder = new CFABuilder ();
        ast.accept (builder);
        CFAMap cfas = builder.getCFAs ();
        int numFunctions = cfas.size ();
        Collection <CFAFunctionDefinitionNode> cfasMapList = cfas.cfaMapIterator();

        CPACheckerLogger.log(CustomLogLevel.MainApplicationLevel, "Adding super edges");
        
        // Insert call and return edges and build the supergraph
        if(CPAConfig.isAnalysisInterprocedural){
                CPASecondPassBuilder spbuilder = new CPASecondPassBuilder(cfas);
                for (CFAFunctionDefinitionNode cfa : cfasMapList){
                        spbuilder.insertCallEdges(cfa.getFunctionName());
                }
        }

        CPACheckerLogger.log(CustomLogLevel.MainApplicationLevel, numFunctions + " functions parsed");

        DOTBuilder dotBuilder = new DOTBuilder ();

        // Erkan: For interprocedural analysis, we start with the
        // main function and we proceed, we don't need to traverse
        // all functions separately

        if(!CPAConfig.isAnalysisInterprocedural){
                CPACheckerLogger.log(CustomLogLevel.MainApplicationLevel, "Analysis is not interprocedural");

                for (CFAFunctionDefinitionNode cfa : cfasMapList)
                {
                        if (CPAConfig.exportDOTfiles)
                                dotBuilder.generateDOT (cfasMapList, cfa, CPAConfig.DOTOutputPath + "dot_" + cfa.getFunctionName() + ".dot");

                        if (CPAConfig.simplifyCFA)
                        {
                                CFASimplifier simplifier = new CFASimplifier (true);
                                simplifier.simplify (cfa);

                                if (CPAConfig.exportDOTfiles)
                                {// If we've simplified the CFA, also export to DOT the simplified version
                                        dotBuilder.generateDOT (cfasMapList, cfa, CPAConfig.DOTOutputPath + "dot_" + cfa.getFunctionName() + "simple.dot");
                                }
                        }

                        // TODO read from config file
                        //CPAType[] cpaArray = {CPAType.LocationCPA, CPAType.PredicateAbstractionCPA};
                        CPAType[] cpaArray = {CPAType.SymbolicPredAbstCPA}; // AG

                        CPACheckerLogger.log(CustomLogLevel.MainApplicationLevel, "CPA Algorithm Called");

                        ConfigurableProblemAnalysis cpa = getCPA (cpaArray,cfa);
                        CPAAlgorithm algo = new CPAAlgorithm ();

                        AbstractElement initialElement = cpa.getInitialElement(cfa);
                        Collection<AbstractElement> reached = algo.CPA (cpa, initialElement);

                        CPACheckerLogger.log(CustomLogLevel.MainApplicationLevel, numFunctions + "Reached CPA Size: " + reached.size () + " for function: " + cfa.getFunctionName ());

                        for (AbstractElement element : reached)
                        {
                                System.out.println (element.toString ());
                        }
                }
        }
        else
        {
                CPACheckerLogger.log(CustomLogLevel.MainApplicationLevel, "Analysis is interprocedural ");

                CFAFunctionDefinitionNode cfa = cfas.getCFA(CPAConfig.entryFunction);

                // TODO Erkan print to dot file
                if (CPAConfig.exportDOTfiles)
                        dotBuilder.generateDOT (cfasMapList, cfa, CPAConfig.DOTOutputPath + "dot" + "_main" + ".dot");

                // TODO Erkan Simplify each CFA
                if (CPAConfig.simplifyCFA)
                {
                        CFASimplifier simplifier = new CFASimplifier (CPAConfig.combineBlockStatements);
                        simplifier.simplify (cfa);
                }

                // TODO read from file
                //CPAType[] cpaArray = {CPAType.LocationCPA, CPAType.PredicateAbstractionCPA};
                CPAType[] cpaArray = {CPAType.SymbolicPredAbstCPA}; // AG
                CPACheckerLogger.log(CustomLogLevel.MainApplicationLevel, "CPA Algorithm Called");

                ConfigurableProblemAnalysis cpa = getCPA (cpaArray, cfa);
                CPAAlgorithm algo = new CPAAlgorithm ();
                AbstractElement initialElement = cpa.getInitialElement(cfa);
                Collection<AbstractElement> reached = algo.CPA (cpa, initialElement);

                CPACheckerLogger.log(CustomLogLevel.MainApplicationLevel, numFunctions + "Reached CPA Size: " + reached.size () + " for function: " + cfa.getFunctionName ());

                for (AbstractElement element : reached)
                {
                        System.out.println (element.toString ());
                }
        }
    }
    
    private static String[] parseCmdLine(String[] args) throws Exception {
        Vector<String> ret = new Vector<String>();
        
        for (int i = 0; i < args.length;) {
            String arg = args[i];
            if (arg.equals("-logpath")) {
                if (i+1 < args.length) {
                    CPAConfig.LogPath = args[i+1];
                    i += 2;
                } else {
                    throw new Exception("-logpath argument missing!");
                }
            } else if (arg.equals("-dotoutpath")) {
                if (i+1 < args.length) {
                    CPAConfig.DOTOutputPath = args[i+1];
                    i += 2;
                } else {
                    throw new Exception("-dotoutpath argument missing!");
                }
            } else if (arg.equals("-predlistpath")) {
                if (i+1 < args.length) {
                    CPAConfig.predicateListPath = args[i+1];
                    i += 2;
                } else {
                    throw new Exception("-predlistpath argument missing!");
                }
            } else if (arg.equals("-entryfunction")) {
                if (i+1 < args.length) {
                    CPAConfig.entryFunction = args[i+1];
                    i += 2;
                } else {
                    throw new Exception("-entryfunction argument missing!");
                }
            } else if (arg.equals("-dfs")) {
                CPAConfig.useBFSVisit = false;
                ++i;
            } else if (arg.equals("-bfs")) {
                CPAConfig.useBFSVisit = true;
                ++i;
            } else if (arg.equals("-nolog")) {
                CPAConfig.LogLevel = Level.OFF;
                ++i;
            } else if (arg.equals("-help")) {
                System.out.println("OPTIONS:");
                System.out.println(" -logpath");
                System.out.println(" -dotoutpath");
                System.out.println(" -predlistpath");
                System.out.println(" -entryfunction");
                System.out.println(" -dfs");
                System.out.println(" -bfs");
                System.out.println(" -nolog");
                System.out.println(" -help");
                System.exit(0);
            } else {
                ret.add(arg);
                ++i;
            }
        }
        String[] aret = new String[ret.size()];
        for (int i = 0; i < aret.length; ++i) {
            aret[i] = ret.elementAt(i);
        }
        return aret;
    }

    /**
     * The action has been activated. The argument of the method represents the
     * 'real' action sitting in the workbench UI.
     * 
     * @see IWorkbenchWindowActionDelegate#run
     */
    public static void main(String[] args) {
        CPACheckerLogger.init();
        CPACheckerLogger.log(CustomLogLevel.INFO, "Program Started");
        
        try {
            args = parseCmdLine(args);
            if (args.length != 1) {
                throw new Exception(
                        "One non-option argument expected (filename)!");
            }
            IFile currentFile = new StubFile(args[0]);

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
            
            doRunAnalysis(ast);

//            CPACheckerLogger.log(CustomLogLevel.MainApplicationLevel, 
//                    "Parsing Finished");
//
//            // Build CFA
//            CFABuilder builder = new CFABuilder();
//            ast.accept(builder);
//            CFAMap cfas = builder.getCFAs();
//            int numFunctions = cfas.size();
//            Collection<CFAFunctionDefinitionNode> cfasMapList = 
//                cfas.cfaMapIterator();
//
//            CPACheckerLogger.log(CustomLogLevel.MainApplicationLevel, 
//                    "Adding super edges");
//
//            // Insert call and return edges and build the supergraph
//            if (CPAConfig.isAnalysisInterprocedural) {
//                CPASecondPassBuilder spbuilder = new CPASecondPassBuilder(cfas);
//                for (CFAFunctionDefinitionNode cfa : cfasMapList) {
//                    spbuilder.insertCallEdges(cfa.getFunctionName());
//                }
//            } else {
//                assert(false);
//            }
//
//            CPACheckerLogger.log(CustomLogLevel.MainApplicationLevel, 
//                    numFunctions + " functions parsed");
//
//            DOTBuilder dotBuilder = new DOTBuilder();
//
//            // Erkan: For interprocedural analysis, we start with the
//            // main function and we proceed, we don't need to traverse
//            // all functions separately
//
//            if (!CPAConfig.isAnalysisInterprocedural) {
//                assert(false);
//            } else {
//                CPACheckerLogger.log(CustomLogLevel.MainApplicationLevel, 
//                        "Analysis is interprocedural ");
//
//                CFAFunctionDefinitionNode cfa = cfas.getCFA(
//                        CPAConfig.entryFunction);
//
//                // TODO Erkan print to dot file
//                if (CPAConfig.exportDOTfiles) {
//                    dotBuilder.generateDOT(cfasMapList, cfa, 
//                            CPAConfig.DOTOutputPath + "dot" + "_main" + ".dot");
//                }
//
//                // TODO Erkan Simplify each CFA
//                if (CPAConfig.simplifyCFA) {
//                    CFASimplifier simplifier = new CFASimplifier(
//                            CPAConfig.combineBlockStatements);
//                    simplifier.simplify(cfa);
//                }
//
//                // TODO read from file
//                //CPAType[] cpaArray = {CPAType.LocationCPA, CPAType.PredicateAbstractionCPA};
//                CPAType[] cpaArray = {CPAType.SymbolicPredAbstCPA}; // AG
//                CPACheckerLogger.log(CustomLogLevel.MainApplicationLevel, 
//                        "CPA Algorithm Called");
//
//                ConfigurableProblemAnalysis cpa = getCPA(cpaArray, cfa);
//                CPAAlgorithm algo = new CPAAlgorithm();
//                AbstractElement initialElement = cpa.getInitialElement(cfa);
//                Collection<AbstractElement> reached = algo.CPA(cpa, 
//                        initialElement);
//
//                CPACheckerLogger.log(CustomLogLevel.MainApplicationLevel, 
//                        numFunctions + "Reached CPA Size: " + reached.size () + 
//                        " for function: " + cfa.getFunctionName ());
//
//                for (AbstractElement element : reached) {
//                    System.out.println (element.toString ());
//                }
//            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
}
