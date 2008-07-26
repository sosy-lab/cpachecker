package cpaplugin.cmdline;

import java.util.Collection;
import java.util.logging.Level;

import org.eclipse.cdt.core.dom.IASTServiceProvider;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.internal.core.dom.InternalASTServiceProvider;
import org.eclipse.core.resources.IFile;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import predicateabstraction.MathSatWrapper;
import cpaplugin.CPAConfiguration;
import cpaplugin.cfa.CFABuilder;
import cpaplugin.cfa.CFAMap;
import cpaplugin.cfa.CFASimplifier;
import cpaplugin.cfa.CPASecondPassBuilder;
import cpaplugin.cfa.DOTBuilder;
import cpaplugin.cfa.objectmodel.CFAFunctionDefinitionNode;
import cpaplugin.cmdline.stubs.StubCodeReaderFactory;
import cpaplugin.cmdline.stubs.StubConfiguration;
import cpaplugin.cmdline.stubs.StubFile;
import cpaplugin.compositeCPA.CompositeCPA;
import cpaplugin.cpa.common.CPAAlgorithm;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.ConfigurableProblemAnalysis;
import cpaplugin.exceptions.CPAException;
import cpaplugin.logging.CPACheckerLogger;
import cpaplugin.logging.CustomLogLevel;

public class CPAMain {

	public static CPAConfiguration cpaConfig;

	private static ConfigurableProblemAnalysis getCPA(CFAFunctionDefinitionNode node) throws CPAException {
		return CompositeCPA.getCompositeCPA(node);
	}

	public static void doRunAnalysis(String args[], IASTTranslationUnit ast)
	throws Exception {

		cpaConfig = new CPAConfiguration(args);

		CPACheckerLogger.init();
		CPACheckerLogger.log(CustomLogLevel.INFO, "Analysis Started");

		CPACheckerLogger.log(CustomLogLevel.MainApplicationLevel, "Parsing Finished");

		// Build CFA
		CFABuilder builder = new CFABuilder ();
		ast.accept (builder);
		CFAMap cfas = builder.getCFAs ();
		int numFunctions = cfas.size ();
		Collection <CFAFunctionDefinitionNode> cfasMapList = cfas.cfaMapIterator();

		CPACheckerLogger.log(CustomLogLevel.MainApplicationLevel, "Adding super edges");

		// Insert call and return edges and build the supergraph
		if(CPAMain.cpaConfig.getBooleanValue("analysis.interprocedural")){
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

		if(!CPAMain.cpaConfig.getBooleanValue("analysis.interprocedural")){
//			CPACheckerLogger.log(CustomLogLevel.MainApplicationLevel, "Analysis is not interprocedural");

//			for (CFAFunctionDefinitionNode cfa : cfasMapList)
//			{
//			if (CPAConfig.exportDOTfiles)
//			dotBuilder.generateDOT (cfasMapList, cfa, CPAConfig.DOTOutputPath + "dot_" + cfa.getFunctionName() + ".dot");

//			if (CPAConfig.simplifyCFA)
//			{
//			CFASimplifier simplifier = new CFASimplifier (true);
//			simplifier.simplify (cfa);

//			if (CPAConfig.exportDOTfiles)
//			{// If we've simplified the CFA, also export to DOT the simplified version
//			dotBuilder.generateDOT (cfasMapList, cfa, CPAConfig.DOTOutputPath + "dot_" + cfa.getFunctionName() + "simple.dot");
//			}
//			}

//			CPAType[] cpaArray = {CPAType.LocationCPA, CPAType.PredicateAbstractionCPA};
//			//CPAType[] cpaArray = {CPAType.LocationCPA, }; // AG

//			CPACheckerLogger.log(CustomLogLevel.MainApplicationLevel, "CPA Algorithm Called");

//			ConfigurableProblemAnalysis cpa = getCPA (cpaArray,cfa);
//			CPAAlgorithm algo = new CPAAlgorithm ();

//			AbstractElement initialElement = cpa.getInitialElement(cfa);
//			Collection<AbstractElement> reached = algo.CPA (cpa, initialElement);

//			CPACheckerLogger.log(CustomLogLevel.MainApplicationLevel, numFunctions + "Reached CPA Size: " + reached.size () + " for function: " + cfa.getFunctionName ());

//			for (AbstractElement element : reached)
//			{
//			System.out.println (element.toString ());
//			}
//			}
		}
		else
		{
			CPACheckerLogger.log(CustomLogLevel.MainApplicationLevel, "Analysis is interprocedural ");

			CFAFunctionDefinitionNode cfa = cfas.getCFA(CPAMain.cpaConfig.getProperty("analysis.entryFunction"));

			// TODO Erkan Simplify each CFA
			if (CPAMain.cpaConfig.getBooleanValue("cfa.simplify"))
			{
				CFASimplifier simplifier = new CFASimplifier (CPAMain.cpaConfig.getBooleanValue("cfa.combineBlockStatements"));
				simplifier.simplify (cfa);
			}

			if (CPAMain.cpaConfig.getBooleanValue("dot.export")){
				String dotPath = CPAMain.cpaConfig.getProperty("dot.path");
				dotBuilder.generateDOT (cfasMapList, cfa, dotPath + "dot" + "_main" + ".dot");
			}

			CPACheckerLogger.log(CustomLogLevel.MainApplicationLevel, "CPA Algorithm Called");

			ConfigurableProblemAnalysis cpa = getCPA (cfa);

			CPACheckerLogger.log(Level.INFO, "CPA Algorithm starting ... ");
			long startingTime = System.currentTimeMillis();

			CPAAlgorithm algo = new CPAAlgorithm ();
			AbstractElement initialElement = cpa.getInitialElement(cfa);
			Collection<AbstractElement> reached = algo.CPA (cpa, initialElement);

			long endingTime = System.currentTimeMillis();
			long totalTimeInMilis = endingTime - startingTime;

			CPACheckerLogger.log(Level.INFO, "CPA Algorithm finished ");

			CPACheckerLogger.log(CustomLogLevel.MainApplicationLevel, numFunctions + "Reached CPA Size: " + reached.size () + " for function: " + cfa.getFunctionName ());

			for (AbstractElement element : reached)
			{
				System.out.println (element.toString ());
			}
			System.out.println( "Total Time Elapsed " + 
					(int) totalTimeInMilis / (1000 * 60 * 60) + " hr, " +  
					(int) totalTimeInMilis / (1000 * 60) + " min, " + 
					(int) totalTimeInMilis / 1000 + " sec, " + 
					(int) totalTimeInMilis % 1000 + " ms");
			System.out.println("Total Number of SMTSolver calls: " + 
					MathSatWrapper.noOfSMTSolverCalls);
		}
	}

	/**
	 * The action has been activated. The argument of the method represents the
	 * 'real' action sitting in the workbench UI.
	 * 
	 * @see IWorkbenchWindowActionDelegate#run
	 */
	public static void main(String[] args) {

		try {
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

			doRunAnalysis(args, ast);
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}
}
