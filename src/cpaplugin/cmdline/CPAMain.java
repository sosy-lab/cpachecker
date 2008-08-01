package cpaplugin.cmdline;

import java.io.File;
import java.util.Collection;
import java.util.logging.Level;

import org.eclipse.cdt.core.dom.IASTServiceProvider;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.internal.core.dom.InternalASTServiceProvider;
import org.eclipse.core.resources.IFile;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import cpaplugin.CPACheckerStatistics;
import cpaplugin.CPAConfiguration;
import cpaplugin.cfa.CFABuilder;
import cpaplugin.cfa.CFAMap;
import cpaplugin.cfa.CFASimplifier;
import cpaplugin.cfa.CPASecondPassBuilder;
import cpaplugin.cfa.DOTBuilder;
import cpaplugin.cfa.DOTBuilderInterface;
import cpaplugin.cfa.objectmodel.CFAFunctionDefinitionNode;
import cpaplugin.cfa.objectmodel.CFANode;
import cpaplugin.cmdline.stubs.StubCodeReaderFactory;
import cpaplugin.cmdline.stubs.StubConfiguration;
import cpaplugin.cmdline.stubs.StubFile;
import cpaplugin.compositeCPA.CompositeCPA;
import cpaplugin.cpa.common.CPAAlgorithm;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.ConfigurableProblemAnalysis;
import cpaplugin.cpa.cpas.symbpredabs.summary.SummaryCFABuilder;
import cpaplugin.cpa.cpas.symbpredabs.summary.SummaryDOTBuilder;
import cpaplugin.exceptions.CPAException;
import cpaplugin.logging.CPACheckerLogger;
import cpaplugin.logging.CustomLogLevel;

@SuppressWarnings("restriction")
public class CPAMain {

	public static CPAConfiguration cpaConfig;

	private static ConfigurableProblemAnalysis getCPA(
			CFAFunctionDefinitionNode node) throws CPAException {
		return CompositeCPA.getCompositeCPA(node);
	}

	public static void doRunAnalysis(IASTTranslationUnit ast)
	throws Exception {

		CPACheckerLogger.init();
		CPACheckerLogger.log(CustomLogLevel.INFO, "Analysis Started");

		CPACheckerLogger.log(CustomLogLevel.MainApplicationLevel, 
		"Parsing Finished");

		// Build CFA
		CFABuilder builder = new CFABuilder();
		ast.accept(builder);
		CFAMap cfas = builder.getCFAs();
		int numFunctions = cfas.size();
		Collection<CFAFunctionDefinitionNode> cfasMapList = 
			cfas.cfaMapIterator();
		// Save number of total nodes
		CPACheckerStatistics.noOfNodes = CFANode.getFinalNumberOfNodes();

		CFAFunctionDefinitionNode cfa = cfas.getCFA(
				CPAMain.cpaConfig.getProperty("analysis.entryFunction"));
		
		// TODO Erkan Simplify each CFA
		if (CPAMain.cpaConfig.getBooleanValue("cfa.simplify")) {
			CFASimplifier simplifier = new CFASimplifier();
			simplifier.simplify(cfa);
		}
		
		// Insert call and return edges and build the supergraph
		if (CPAMain.cpaConfig.getBooleanValue("analysis.interprocedural")) {
			CPACheckerLogger.log(CustomLogLevel.MainApplicationLevel, 
			"Analysis is interprocedural ");
			CPACheckerLogger.log(CustomLogLevel.MainApplicationLevel, 
			"Adding super edges");
			CPASecondPassBuilder spbuilder = new CPASecondPassBuilder(cfas);
			for (CFAFunctionDefinitionNode cfaSep : cfasMapList){
				spbuilder.insertCallEdges(cfaSep.getFunctionName());
			}
		} else if (CPAMain.cpaConfig.getBooleanValue(
		"analysis.useSummaryLocations")) {
			CPACheckerLogger.log(CustomLogLevel.MainApplicationLevel,
			"Building Summary CFAs");
			SummaryCFABuilder summaryBuilder = new SummaryCFABuilder(cfas);
			cfas = summaryBuilder.buildSummary();
			CPACheckerLogger.log(CustomLogLevel.MainApplicationLevel, "DONE");
			cfasMapList = cfas.cfaMapIterator();
		}

		CPACheckerLogger.log(CustomLogLevel.MainApplicationLevel, 
				numFunctions + " functions parsed");

		// Erkan: For interprocedural analysis, we start with the
		// main function and we proceed, we don't need to traverse
		// all functions separately

		if (false) {//!CPAMain.cpaConfig.getBooleanValue("analysis.interprocedural")) {
		} else {

			if (CPAMain.cpaConfig.getBooleanValue("dot.export")) {
				DOTBuilderInterface dotBuilder = null;
				if (CPAMain.cpaConfig.getBooleanValue(
				"analysis.useSummaryLocations")) {
					dotBuilder = new SummaryDOTBuilder();
				} else {
					dotBuilder = new DOTBuilder();
				}
				String dotPath = CPAMain.cpaConfig.getProperty("dot.path");
				dotBuilder.generateDOT(cfasMapList, cfa,
						new File(dotPath, "dot" + "_main" + ".dot").getPath());
			}

			CPACheckerLogger.log(CustomLogLevel.MainApplicationLevel, 
			"CPA Algorithm Called");

			ConfigurableProblemAnalysis cpa = getCPA(cfa);

			CPACheckerLogger.log(Level.INFO, "CPA Algorithm starting ... ");
			long analysisStartTime = System.currentTimeMillis();

			CPAAlgorithm algo = new CPAAlgorithm();
			AbstractElement initialElement = cpa.getInitialElement(cfa);
			Collection<AbstractElement> reached = algo.CPA(cpa, initialElement);

			long analysisFinishTime = System.currentTimeMillis();
			CPACheckerStatistics.totalAnalysisTime = analysisStartTime - analysisFinishTime;

			CPACheckerLogger.log(Level.INFO, "CPA Algorithm finished ");

			CPACheckerLogger.log(CustomLogLevel.MainApplicationLevel, 
					numFunctions + "Reached CPA Size: " + reached.size() + 
					" for function: " + cfa.getFunctionName());

			for (AbstractElement element : reached) {
				System.out.println(element.toString ());
			}

			if(CPAMain.cpaConfig.getBooleanValue("analysis.saveStatistics")){
				CPACheckerStatistics.printStatictics();
			}
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
			cpaConfig = new CPAConfiguration(args);
			String[] names = 
				cpaConfig.getPropertiesArray("analysis.programNames");
			if (names == null || names.length != 1) {
				throw new Exception(
				"One non-option argument expected (filename)!");
			}
			IFile currentFile = new StubFile(names[0]);

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
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}    
}
