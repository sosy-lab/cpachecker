package cpaplugin.cpa.cpas.symbpredabsCPA;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import symbpredabstraction.*;
import cpaplugin.CPAStatistics;
import cpaplugin.cfa.objectmodel.CFAErrorNode;
import cpaplugin.cfa.objectmodel.CFAFunctionDefinitionNode;
import cpaplugin.cfa.objectmodel.CFANode;
import cpaplugin.cmdline.CPAMain;
import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.ConfigurableProblemAnalysis;
import cpaplugin.cpa.common.interfaces.MergeOperator;
import cpaplugin.cpa.common.interfaces.StopOperator;
import cpaplugin.cpa.common.interfaces.TransferRelation;
import cpaplugin.logging.CustomLogLevel;
import cpaplugin.logging.LazyLogger;


/**
 * CPA for symbolic lazy abstraction with summaries
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class SymbPredAbsCPA implements ConfigurableProblemAnalysis {

	private SymbPredAbsAbstractDomain domain;
	private SymbPredAbsMergeOperator merge;
	private SymbPredAbsStopOperator stop;
	private SymbPredAbsTransferRelation trans;
	private MathsatSymbPredAbsFormulaManager mgr;
	private BDDMathsatSymbPredAbsAbstractManager amgr;
	private Map<SymbPredAbsAbstractElement, Set<SymbPredAbsAbstractElement>> covers;

//	private SymbPredAbsCPAStatistics stats;

	private SymbPredAbsCPA() {
		mgr = new MathsatSymbPredAbsFormulaManager();
		amgr = new BDDMathsatSymbPredAbsAbstractManager();
		domain = new SymbPredAbsAbstractDomain(this);
		merge = new SymbPredAbsMergeOperator(domain);
		stop = new SymbPredAbsStopOperator(domain);
		trans = new SymbPredAbsTransferRelation(domain);
		covers = new HashMap<SymbPredAbsAbstractElement, 
		Set<SymbPredAbsAbstractElement>>();

		// for testing purposes, it's nice to be able to use a given set of
		// predicates and disable refinement
		// TODO enable later
//		if (CPAMain.cpaConfig.getBooleanValue(
//		"cpas.symbpredabs.abstraction.norefinement")) {
//		MathsatPredicateParser p = new MathsatPredicateParser(mgr, amgr);
//		Collection<Predicate> preds = null;
//		try {
//		String pth = CPAMain.cpaConfig.getProperty("predicates.path");
//		File f = new File(pth, "predicates.msat");
//		InputStream in = new FileInputStream(f);
//		preds = p.parsePredicates(in);
//		} catch (IOException e) {
//		e.printStackTrace();
//		preds = new Vector<Predicate>();
//		}
//		pmap = new FixedPredicateMap(preds);
//		} else {
//		pmap = new UpdateablePredicateMap();
//		}

//		summaryToFormulaMap = 
//		new HashMap<SymbPredAbsCFANode, 
//		Map<CFANode, Pair<SymbolicFormula, SSAMap>>>();

		// TODO later
//		stats = new SymbPredAbsCPAStatistics(this);
	}

	/**
	 * Constructor conforming to the "contract" in CompositeCPA. The two
	 * arguments are ignored
	 * @param s1
	 * @param s2
	 */
	public SymbPredAbsCPA(String s1, String s2) {
		this();
	}

	// TODO later
//	public CPAStatistics getStatistics() {
//	return stats;
//	}

	@Override
	public AbstractDomain getAbstractDomain() {
		return domain;
	}

	@Override
	public AbstractElement getInitialElement(CFAFunctionDefinitionNode node) {
		LazyLogger.log(CustomLogLevel.SpecificCPALevel, 
				"Getting initial element from node: ", node);

		CFANode loc = node;
		SymbPredAbsAbstractElement e = new SymbPredAbsAbstractElement(domain, loc, loc);
		ParentsList parents = new ParentsList();
		PathFormula pf = getNewPathFormula();  
		e.setPathFormula(pf);
		e.setParents(parents);
		e.setAbstraction(amgr.makeTrue());
		PredicateMap pmap;
		if (CPAMain.cpaConfig.getBooleanValue("cpas.symbpredabs.abstraction.norefinement")) {
			MathsatPredicateParser p = new MathsatPredicateParser(mgr, amgr);
			Collection<Predicate> preds = null;
			try {
				String pth = CPAMain.cpaConfig.getProperty("predicates.path");
				File f = new File(pth, "predicates.msat");
				InputStream in = new FileInputStream(f);
				preds = p.parsePredicates(in);
			} catch (IOException er) {
				er.printStackTrace();
				preds = new Vector<Predicate>();
			}
			pmap = new FixedPredicateMap(preds);
		} else {
			pmap = new UpdateablePredicateMap();
		}
		assert(pmap != null);
		e.setPredicates(pmap);

		// TODO function 
		//e.setContext(new Stack<Pair<AbstractFormula, SymbPredAbsCFANode>>(), true);
		// we return an tuple (loc, loc, pf, abst, null), the parent is null since this is the 
		// initial element
		return e;
	}

	@Override
	public MergeOperator getMergeOperator() {
		return merge;
	}

	@Override
	public StopOperator getStopOperator() {
		return stop;
	}

	@Override
	public TransferRelation getTransferRelation() {
		return trans;
	}

	public SymbPredAbsAbstractFormulaManager getAbstractFormulaManager() {
		return amgr;
	}

	public SymbolicFormulaManager getFormulaManager() {
		return mgr;
	}

	// builds the path formulas corresponding to the leaves of the inner
	// subgraph of the given summary location
	public PathFormula getNewPathFormula() {

		SSAMap ssamap = new SSAMap();
		return new PathFormula(mgr.makeTrue(), ssamap);

//		try {
//		if (!summaryToFormulaMap.containsKey(succLoc)) {
//		Map<CFANode, Pair<SymbolicFormula, SSAMap>> p = 
//		mgr.buildPathFormulas(succLoc); 
//		summaryToFormulaMap.put(succLoc, p);

////		CPACheckerLogger.log(CustomLogLevel.SpecificCPALevel,
////		"SYMBOLIC FORMULA FOR " + succLoc.toString() + ": " + 
////		p.getFirst().toString());

//		}
//		return summaryToFormulaMap.get(succLoc);
//		} catch (UnrecognizedCFAEdgeException e) {
//		e.printStackTrace();
//		return null;
//		}
	}

	public Set<SymbPredAbsAbstractElement> getCoveredBy(SymbPredAbsAbstractElement e){
		if (covers.containsKey(e)) {
			return covers.get(e);
		} else {
			return Collections.emptySet();
		}
	}

	public void setCoveredBy(SymbPredAbsAbstractElement covered, 
			SymbPredAbsAbstractElement e) {
		Set<SymbPredAbsAbstractElement> s;
		if (covers.containsKey(e)) {
			s = covers.get(e);
		} else {
			s = new HashSet<SymbPredAbsAbstractElement>();
		}
		s.add(covered);
		covers.put(e, s);
	}

	public void uncoverAll(SymbPredAbsAbstractElement e) {
		if (covers.containsKey(e)) {
			covers.remove(e);
		}
	}

	public MathsatSymbPredAbsFormulaManager getMathsatSymbPredAbsFormulaManager() {
		return mgr;
	}

	public BDDMathsatSymbPredAbsAbstractManager getBDDMathsatSymbPredAbsAbstractManager() {
		return amgr;
	}

	public boolean isAbstractionLocation(CFANode succLoc) {
		
		// useful for test cases
		String lines[] = CPAMain.cpaConfig.getPropertiesArray("abstraction.extraLocations");
		for(String line:lines){
			if(succLoc.getLineNumber() == Integer.valueOf(line)){
				return true;
			}
		}
		
		if (succLoc.isLoopStart() || succLoc instanceof CFAErrorNode
				|| succLoc.getNumLeavingEdges() == 0) {
			return true;
		} else if (succLoc instanceof CFAFunctionDefinitionNode) {
			return true;
		} else if (succLoc.getEnteringSummaryEdge() != null) {
			return true;
			// if a node has two or more incoming edges from different
			// summary nodes, it is a abstraction location
		} else {
			return false;
		}
	}

}
