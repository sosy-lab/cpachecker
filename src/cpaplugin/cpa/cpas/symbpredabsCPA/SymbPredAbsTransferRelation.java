package cpaplugin.cpa.cpas.symbpredabsCPA;

import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;

import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTInitializer;
import org.eclipse.cdt.core.dom.ast.IASTInitializerExpression;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclSpecifier;

import symbpredabstraction.AbstractFormula;
import symbpredabstraction.PathFormula;
import symbpredabstraction.Predicate;
import symbpredabstraction.PredicateMap;
import symbpredabstraction.SSAMap;
import symbpredabstraction.UpdateablePredicateMap;
import cpaplugin.cfa.objectmodel.BlankEdge;
import cpaplugin.cfa.objectmodel.CFAEdge;
import cpaplugin.cfa.objectmodel.CFAErrorNode;
import cpaplugin.cfa.objectmodel.CFAFunctionDefinitionNode;
import cpaplugin.cfa.objectmodel.CFANode;
import cpaplugin.cfa.objectmodel.c.AssumeEdge;
import cpaplugin.cfa.objectmodel.c.CallToReturnEdge;
import cpaplugin.cfa.objectmodel.c.DeclarationEdge;
import cpaplugin.cfa.objectmodel.c.FunctionCallEdge;
import cpaplugin.cfa.objectmodel.c.FunctionDefinitionNode;
import cpaplugin.cfa.objectmodel.c.ReturnEdge;
import cpaplugin.cfa.objectmodel.c.StatementEdge;
import cpaplugin.cmdline.CPAMain;
import cpaplugin.cpa.common.CPATransferException;
import cpaplugin.cpa.common.ErrorReachedException;
import cpaplugin.cpa.common.RefinementNeededException;
import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.TransferRelation;
import cpaplugin.exceptions.CPAException;
import cpaplugin.logging.CPACheckerLogger;
import cpaplugin.logging.CustomLogLevel;
import cpaplugin.logging.LazyLogger;
import symbpredabstraction.*;


/**
 * Transfer relation for symbolic lazy abstraction with summaries
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class SymbPredAbsTransferRelation implements TransferRelation {

//	// the Abstract Reachability Tree
//	class ART {
//	Map<AbstractElement, Collection<AbstractElement>> tree;

//	public ART() {
//	tree = new HashMap<AbstractElement, Collection<AbstractElement>>();
//	}

//	public void addChild(AbstractElement parent, AbstractElement child) {
//	if (!tree.containsKey(parent)) {
//	tree.put(parent, new Vector<AbstractElement>()); 
//	}
//	Collection<AbstractElement> c = tree.get(parent);
//	c.add(child);
//	}

//	public Collection<AbstractElement> getSubtree(AbstractElement root, 
//	boolean remove, boolean includeRoot) {
//	Vector<AbstractElement> ret = new Vector<AbstractElement>();

//	Stack<AbstractElement> toProcess = new Stack<AbstractElement>();
//	toProcess.push(root);

//	while (!toProcess.empty()) {
//	AbstractElement cur = toProcess.pop();
//	ret.add(cur);
//	if (tree.containsKey(cur)) {
//	toProcess.addAll(remove ? tree.remove(cur) : tree.get(cur));
//	}
//	}
//	if (!includeRoot) {
//	AbstractElement tmp = ret.lastElement();
//	assert(ret.firstElement() == root);
//	ret.setElementAt(tmp, 0);
//	ret.remove(ret.size()-1);
//	}
//	return ret;
//	}
//	}

	private SymbPredAbsAbstractDomain domain;
//	private ART abstractTree;

	private int numAbstractStates = 0; // for statistics
	private boolean errorReached = false;

	public SymbPredAbsTransferRelation(SymbPredAbsAbstractDomain d) {
		domain = d;
//		abstractTree = new ART();
	}

	public int getNumAbstractStates() { 
		return numAbstractStates; 
	}

	public boolean hasReachedError() { 
		return errorReached; 
	}

	@Override
	public AbstractDomain getAbstractDomain() {
		return domain;
	}

	// isFunctionStart and isFunctionEnd are used for managing the context,
	// needed for handling function calls

	private boolean isFunctionStart(SymbPredAbsAbstractElement elem) {
		return (elem.getLocation() instanceof 
				FunctionDefinitionNode);
	}

	private boolean isFunctionEnd(SymbPredAbsAbstractElement elem) {
		CFANode n = elem.getLocation();
		return (n.getNumLeavingEdges() == 1 &&
				n.getLeavingEdge(0) instanceof ReturnEdge);
	}

	// abstract post operation
	private AbstractElement buildSuccessor(SymbPredAbsAbstractElement element,
			CFAEdge edge) throws CPATransferException {
		SymbPredAbsAbstractElement newElement = new SymbPredAbsAbstractElement();
		//SymbPredAbsCPA cpa = domain.getCPA();
		CFANode succLoc = edge.getSuccessor();
		// TODO check whether the successor is an error location: if so, we want
		// to check for feasibility of the path...

		// check if the successor is an abstraction location
		boolean b = isAbstractionLocation(succLoc);

		if(b){
			handleAbstractionLocation(element, newElement, edge);
		}

		else{
			handleNonAbstractionLocation(element, newElement, edge);
		}
		
		
		return newElement;

		Collection<Predicate> predicates = 
			cpa.getPredicateMap().getRelevantPredicates(
					edge.getSuccessor());

		SymbPredAbsAbstractElement succ = new SymbPredAbsAbstractElement(succLoc);
		Map<CFANode, Pair<SymbolicFormula, SSAMap>> p = 
			cpa.getPathFormulas(succLoc);
		succ.setPathFormulas(p);

		// if e is the end of a function, we must find the correct return 
		// location
//		if (isFunctionEnd(succ)) {
//		SymbPredAbsCFANode retNode = e.topContextLocation();
//		if (!succLoc.equals(retNode)) {
//		LazyLogger.log(LazyLogger.DEBUG_1,
//		"Return node for this call is: ", retNode,
//		", but edge leads to: ", succLoc, ", returning BOTTOM");
//		return domain.getBottomElement();
//		}
//		}

//		Stack<AbstractFormula> context = 
//		(Stack<AbstractFormula>)e.getContext().clone();
//		if (isFunctionEnd(e)) {
//		context.pop();
//		}
//		succ.setContext(context);
//		succ.setContext(e.getContext(), false);
//		if (isFunctionEnd(succ)) {
//		succ.popContext();
//		}

		SymbPredAbsAbstractFormulaManager amgr = cpa.getAbstractFormulaManager();
		AbstractFormula abstraction = amgr.buildAbstraction(
				cpa.getFormulaManager(), e, succ, predicates);
		succ.setAbstraction(abstraction);
		succ.setParent(e);

		Level lvl = LazyLogger.DEBUG_1;
		if (CPACheckerLogger.getLevel() <= lvl.intValue()) {
			SymbPredAbsFormulaManager mgr = cpa.getFormulaManager();
			LazyLogger.log(lvl, "COMPUTED ABSTRACTION: ", 
					amgr.toConcrete(mgr, abstraction));
		}

		if (amgr.isFalse(abstraction)) {
			return domain.getBottomElement();
		} else {
			++numAbstractStates;
			// if we reach an error state, we want to log this...
			if (succ.getLocation().getInnerNode() instanceof CFAErrorNode) {
				if (CPAMain.cpaConfig.getBooleanValue(
				"cpas.symbpredabs.abstraction.norefinement")) {
					errorReached = true;
					throw new ErrorReachedException(
					"Reached error location, but refinement disabled");
				}
				// oh oh, reached error location. Let's check whether the 
				// trace is feasible or spurious, and in case refine the
				// abstraction
				//
				// first we build the abstract path
				Deque<SymbPredAbsAbstractElement> path = 
					new LinkedList<SymbPredAbsAbstractElement>();
				path.addFirst(succ);
				SymbPredAbsAbstractElement parent = succ.getParent();
				while (parent != null) {
					path.addFirst(parent);
					parent = parent.getParent();
				}
				CounterexampleTraceInfo info = 
					amgr.buildCounterexampleTrace(
							cpa.getFormulaManager(), path);
				if (info.isSpurious()) {
					LazyLogger.log(CustomLogLevel.SpecificCPALevel,
							"Found spurious error trace, refining the ",
					"abstraction");
					performRefinement(path, info);
				} else {
					LazyLogger.log(CustomLogLevel.SpecificCPALevel, 
							"REACHED ERROR LOCATION!: ", succ, 
					" RETURNING BOTTOM!");
					errorReached = true;
					throw new ErrorReachedException(
							info.getConcreteTrace().toString());
				}
				return domain.getBottomElement();
			}

			if (isFunctionStart(succ)) {
				// we push into the context the return location, which is
				// the successor location of the summary edge
				SymbPredAbsCFANode retNode = null;
				for (CFANode l : e.getLeaves()) {  
					if (l instanceof FunctionDefinitionNode) {
						assert(l.getNumLeavingEdges() == 1);
						//assert(l.getNumEnteringEdges() == 1);

						CFAEdge ee = l.getLeavingEdge(0);
						SymbPredAbsInnerCFANode n = (SymbPredAbsInnerCFANode)ee.getSuccessor();
						if (n.getSummaryNode().equals(succ.getLocation())) {
							CFANode pr = l.getEnteringEdge(0).getPredecessor();
							CallToReturnEdge ce = pr.getLeavingSummaryEdge();
							//assert(ce != null);
							if (ce != null) {
								retNode = ((SymbPredAbsInnerCFANode)ce.getSuccessor()).
								getSummaryNode();
								break;
							}
						}
					}
				}
				//assert(retNode != null);
				if (retNode != null) {
					LazyLogger.log(LazyLogger.DEBUG_3, "PUSHING CONTEXT TO ", succ,
							": ", cpa.getAbstractFormulaManager().toConcrete(
									cpa.getFormulaManager(), 
									succ.getAbstraction()));
					//succ.getContext().push(succ.getAbstraction());
					succ.pushContext(succ.getAbstraction(), retNode);
				}
			}            

			return succ;
		}
	}

	// TODO implement this
	private void handleNonAbstractionLocation(SymbPredAbsAbstractElement element,
			SymbPredAbsAbstractElement newElement, CFAEdge edge) {
		CFANode succLocation = edge.getSuccessor();
		CFANode abstractionLoc = element.getAbstractionLocation();
		AbstractFormula abst = element.getAbstraction();
		SymbPredAbsAbstractElement parent = element;
		PredicateMap pmap = element.getPredicates();
		PathFormula pf = update(element, newElement, edge);
		newElement.setLocation(succLocation);
		newElement.setAbstractionLocation(abstractionLoc);
        newElement.setAbstraction(abst);
        newElement.setParent(parent);
        newElement.setPathFormula(pf);
        newElement.setPredicates(pmap);
	}
	
	/**
	 * TODO 
	 * @param element
	 * @param newElement 
	 * @param edge
	 * @return
	 */
	private PathFormula update(SymbPredAbsAbstractElement element, SymbPredAbsAbstractElement newElement, CFAEdge edge, 
			boolean updateSSA, boolean absoluteSSAIndices) {

		SymbolicFormula f1 = element.getPathFormula().getPathFormula();
		
//		if (edge instanceof BlockEdge) {
//			BlockEdge block = (BlockEdge)edge;
//			Pair<SymbolicFormula, SSAMap> ret = null;
//			for (CFAEdge e : block.getEdges()) {
//				ret = makeAnd(f1, e, ssa, updateSSA, absoluteSSAIndices);
//				f1 = ret.getFirst();
//				ssa = ret.getSecond();
//			}
//			assert(ret != null);
//			return ret;
//		}

		MathsatSymbolicFormula m1 = (MathsatSymbolicFormula)f1;

		setNamespace(edge.getPredecessor().getFunctionName());

		// if the edge is a function call edge
		if (edge.getPredecessor() instanceof FunctionDefinitionNode) {
			PathFormula p = makeAndEnterFunction(element, newElement, 
					edge.getPredecessor(), updateSSA, absoluteSSAIndices);
			m1 = (MathsatSymbolicFormula)p.getPathFormula();
			f1 = m1;
			// TODO check here - i'm not sure if that's what we want to do
			SSAMap ssa = element.getPathFormula().getSsa(); 
			ssa = p.getSsa();
			//
		}

		switch (edge.getEdgeType ()) {
		case StatementEdge: {
			StatementEdge statementEdge = (StatementEdge)edge;

			if (statementEdge.isJumpEdge()) {
				if (statementEdge.getSuccessor().getFunctionName().equals(
						"main")) {
					LazyLogger.log(LazyLogger.DEBUG_3, 
							"MathsatSymbolicFormulaManager, IGNORING return ",
							"from main: ", edge.getRawStatement());
				} else {
					return makeAndReturn(element, newElement, 
							edge.getPredecessor(), updateSSA, absoluteSSAIndices);
				}
			} else {
				return makeAndStatement(element, newElement, 
						edge.getPredecessor(), updateSSA, absoluteSSAIndices);
			}
			break;
		}

		case DeclarationEdge: {
			// at each declaration, we instantiate the variable in the SSA: 
			// this is o avoid problems with uninitialized variables
			// TODO check here
			SSAMap newssa = element.getPathFormula().getSsa();
			if (!updateSSA) {
				newssa = new SSAMap();
				// TODO check
				newssa.copyFrom(element.getPathFormula().getSsa());
			}
			IASTDeclarator[] decls = 
				((DeclarationEdge)edge).getDeclarators();
			IASTDeclSpecifier spec = ((DeclarationEdge)edge).getDeclSpecifier();

			if (!(spec instanceof IASTSimpleDeclSpecifier)) {
				throw new UnrecognizedCFAEdgeException(
						"UNSUPPORTED SPECIFIER FOR DECLARATION: " + 
						edge.getRawStatement());
			}

			boolean isGlobal = edge instanceof GlobalDeclarationEdge;
			for (IASTDeclarator d : decls) {
				String var = d.getName().getRawSignature();
				if (isGlobal) {
					globalVars.add(var);
				}
				var = scoped(var);
				int idx = absoluteSSAIndices ? SSAMap.getNextSSAIndex() : 1;
				newssa.setIndex(var, idx);

				LazyLogger.log(LazyLogger.DEBUG_3, 
						"Declared variable: ", var, ", index: ", idx);
				// TODO get the type of the variable, and act accordingly

				// if the var is unsigned, add the constraint that it should
				// be > 0
//				if (((IASTSimpleDeclSpecifier)spec).isUnsigned()) {
//				long z = mathsat.api.msat_make_number(msatEnv, "0");
//				long mvar = buildMsatVariable(var, idx);
//				long t = mathsat.api.msat_make_gt(msatEnv, mvar, z);
//				t = mathsat.api.msat_make_and(msatEnv, m1.getTerm(), t);
//				m1 = new MathsatSymbolicFormula(t);
//				}

				// if there is an initializer associated to this variable,
				// take it into account
				if (d.getInitializer() != null) {
					IASTInitializer init = d.getInitializer();
					if (!(init instanceof IASTInitializerExpression)) {
						throw new UnrecognizedCFAEdgeException(
								"BAD INITIALIZER: " + edge.getRawStatement());
					}
					IASTExpression exp = 
						((IASTInitializerExpression)init).getExpression();
					long minit = buildMsatTerm(exp, newssa, absoluteSSAIndices);
					long mvar = buildMsatVariable(var, idx);
					long t = makeAssignment(mvar, minit);
					t = mathsat.api.msat_make_and(msatEnv, m1.getTerm(), t);
					m1 = new MathsatSymbolicFormula(t);
				} else if (isGlobal || 
						CPAMain.cpaConfig.getBooleanValue(
								"cpas.symbpredabs.initAllVars")) {
					// auto-initialize variables to zero, unless they match
					// the noAutoInitPrefix pattern
					String noAutoInit = CPAMain.cpaConfig.getProperty(
							"cpas.symbpredabs.noAutoInitPrefix", "");
					if (noAutoInit.equals("") || 
							!d.getName().getRawSignature().startsWith(noAutoInit)) {
						long mvar = buildMsatVariable(var, idx);
						long z = mathsat.api.msat_make_number(msatEnv, "0");
						long t = makeAssignment(mvar, z);
						t = mathsat.api.msat_make_and(msatEnv, m1.getTerm(), t);
						m1 = new MathsatSymbolicFormula(t);
						LazyLogger.log(LazyLogger.DEBUG_3, "AUTO-INITIALIZING ",
								(isGlobal ? "GLOBAL" : ""), "VAR: ",
								var, " (", d.getName().getRawSignature(), ")");
					} else {
						LazyLogger.log(LazyLogger.DEBUG_3, 
								"NOT AUTO-INITIALIZING VAR: ", var);
					}
				}
			}
			return new Pair<SymbolicFormula, SSAMap>(m1, newssa);
		}

		case AssumeEdge: {
			AssumeEdge assumeEdge = (AssumeEdge)edge;
			return makeAndAssume(m1, assumeEdge, ssa, absoluteSSAIndices);
		}

		case BlankEdge: {
			break;
		}

		case FunctionCallEdge: {
			if (!updateSSA) {
				SSAMap newssa = new SSAMap();
				newssa.copyFrom(ssa);
				ssa = newssa;
			}
			return makeAndFunctionCall(m1, (FunctionCallEdge)edge, ssa,
					absoluteSSAIndices);
		}

		case ReturnEdge: {
			// get the expression from the summary edge
			CFANode succ = edge.getSuccessor();
			CallToReturnEdge ce = succ.getEnteringSummaryEdge();
			Pair<SymbolicFormula, SSAMap> ret = 
				makeAndExitFunction(m1, ce, ssa, updateSSA, absoluteSSAIndices);
			//popNamespace(); - done inside makeAndExitFunction
			return ret;
		}

		case MultiStatementEdge: {
			throw new UnrecognizedCFAEdgeException("MULTI STATEMENT: " + 
					edge.getRawStatement());
		}

		case MultiDeclarationEdge: {
			break;
		}
		}

		return new Pair<SymbolicFormula, SSAMap>(f1, ssa);
	}

	// TODO implement this
	private SymbPredAbsAbstractElement handleAbstractionLocation(SymbPredAbsAbstractElement e,
			CFAEdge edge) {
		Map<CFANode, AbstractionLocationPointer> abstLocsMap = domain.getCPA().getAbstracionLocsMap();
		CFANode successorNode = edge.getSuccessor(); 
		// TODO erkan -last point-

	}

	private boolean isAbstractionLocation(CFANode succLoc) {
		if (succLoc.isLoopStart() || succLoc instanceof CFAErrorNode ||
				succLoc.getNumLeavingEdges() == 0) {
			return true;
		} else if (succLoc instanceof CFAFunctionDefinitionNode) {
			return true;
		} else if (succLoc.getEnteringSummaryEdge() != null) {
			return true;
			// if a node has two or more incoming edges from different
			// summary nodes, it is a abstraction location
		} else {
			CFANode cur = null;
			Map<CFANode, AbstractionLocationPointer> abstLocsMap = domain.getCPA().getAbstracionLocsMap();
			for (int i = 0; i < succLoc.getNumEnteringEdges(); ++i) {
				CFAEdge e = succLoc.getEnteringEdge(i);
				if (!isLoopBack(e)) {
					CFANode p = e.getPredecessor();
					if (!abstLocsMap.containsKey(p)) {
						// this might happen if this e is a jump edge: in this
						// case, we ignore it...
						assert(e instanceof BlankEdge);
						continue;
					}
					assert(abstLocsMap.containsKey(p));
					AbstractionLocationPointer abp = abstLocsMap.get(p);
					CFANode summ = abp.getAbstractionLocation();
					if (cur == null) {
						cur = summ;
					} else if (cur != summ) {
						return true;
					}
				}
			}
			// check if we have only blank incoming edges, and the current 
			// summary is already big TODO
			if (CPAMain.cpaConfig.getBooleanValue(
			"cpas.symbpredabs.smallSummaries")) {
				if (succLoc.getNumEnteringEdges() >= 1) {
					for (int i = 0; i < succLoc.getNumEnteringEdges(); ++i) {
						CFAEdge e = succLoc.getEnteringEdge(i);
						if (!(e instanceof BlankEdge)) break;
						if (e instanceof BlankEdge && 
								e.getRawStatement().startsWith(
								"Goto: BREAK_SUMMARY")) {
							return true;
						}
					}
				}
			}
//			int summarySize = 0;
//			if (cur != null && summarySizeMap.containsKey(cur)) {
//			summarySize = summarySizeMap.get(cur);
//			}
//			final int MAX_SUMMARY_SIZE = 5;
//			if (summarySize > MAX_SUMMARY_SIZE) {
//			boolean allIncomingBlank = true;
//			for (int i = 0; i < n.getNumEnteringEdges(); ++i) {
//			CFAEdge e = n.getEnteringEdge(i);
//			if (!isLoopBack(e) && !(e instanceof BlankEdge)) {
//			allIncomingBlank = false;
//			break;
//			}
//			}
//			if (allIncomingBlank) return true;
//			}
			return false;
		}

	}

	private boolean isLoopBack(CFAEdge e) {
		CFANode s = e.getSuccessor();
		boolean yes = s.isLoopStart() && !e.getRawStatement().equals("while");
		if (!yes) {
			// also return edges are loopbacks
			yes = e instanceof ReturnEdge;
		}
		return yes;
	}

	// abstraction refinement and undoing of (part of) the ART
	private void performRefinement(Deque<SymbPredAbsAbstractElement> path, 
			CounterexampleTraceInfo info) throws CPATransferException {
		// TODO Auto-generated method stub
		UpdateablePredicateMap curpmap =
			(UpdateablePredicateMap)domain.getCPA().getPredicateMap();
		AbstractElement root = null;
		AbstractElement firstInterpolant = null;
		for (SymbPredAbsAbstractElement e : path) {
			Collection<Predicate> newpreds = info.getPredicatesForRefinement(e);
			if (firstInterpolant == null && newpreds.size() > 0) {
				firstInterpolant = e;
			}
			if (curpmap.update((CFANode)e.getLocation(), newpreds)) {
				if (root == null) {
					root = e.getParent();
				}
			}
		}
		if (root == null) {
			root = firstInterpolant;
		}
		assert(root != null);
		//root = path.getFirst();
		Collection<AbstractElement> toWaitlist = new HashSet<AbstractElement>();
		toWaitlist.add(root);
		Collection<AbstractElement> toUnreach = 
			abstractTree.getSubtree(root, true, false);
		SymbPredAbsCPA cpa = domain.getCPA();
		for (AbstractElement e : toUnreach) {
			Set<SymbPredAbsAbstractElement> cov = cpa.getCoveredBy(
					(SymbPredAbsAbstractElement)e);
			for (AbstractElement c : cov) {
				if (!((SymbPredAbsAbstractElement)c).isDescendant(
						(SymbPredAbsAbstractElement)root)) {
					toWaitlist.add(c);
				}
			}
			cpa.uncoverAll((SymbPredAbsAbstractElement)e);
		}
//		Collection<AbstractElement> toUnreach = new Vector<AbstractElement>();
//		boolean add = false;
//		for (AbstractElement e : path) {
//		if (add) { 
//		toUnreach.add(e);
//		} else if (e == root) {
//		add = true;
//		}
//		}
		LazyLogger.log(LazyLogger.DEBUG_1, "REFINEMENT - toWaitlist: ", root);
		LazyLogger.log(LazyLogger.DEBUG_1, "REFINEMENT - toUnreach: ", 
				toUnreach);
		throw new RefinementNeededException(toUnreach, toWaitlist);
	}

	@Override
	public AbstractElement getAbstractSuccessor(AbstractElement element,
			CFAEdge cfaEdge) throws CPATransferException {
		LazyLogger.log(CustomLogLevel.SpecificCPALevel, 
				"Getting Abstract Successor of element: ", element, 
				" on edge: ", cfaEdge);
		// To get the successor, we compute the predicate abstraction of the
		// formula of element plus all the edges that connect any of the 
		// inner nodes of the summary of element to any inner node of the  
		// destination
		SymbPredAbsAbstractElement e = (SymbPredAbsAbstractElement)element;
		CFANode src = (CFANode)e.getLocation();

		for (int i = 0; i < src.getNumLeavingEdges(); ++i) {
			CFAEdge edge = src.getLeavingEdge(i);
			if (edge.equals(cfaEdge)) {
				AbstractElement ret = buildSuccessor(e, edge);

				LazyLogger.log(CustomLogLevel.SpecificCPALevel, 
						"Successor is: ", ret);

				if (ret != domain.getBottomElement()) {
					abstractTree.addChild(e, ret);
				}

				return ret;
			}
		}

		LazyLogger.log(CustomLogLevel.SpecificCPALevel, "Successor is: BOTTOM");

		return domain.getBottomElement();
	}

	@Override
	public List<AbstractElement> getAllAbstractSuccessors(
			AbstractElement element) throws CPAException, CPATransferException {
		LazyLogger.log(CustomLogLevel.SpecificCPALevel, 
				"Getting ALL Abstract Successors of element: ", 
				element);

		List<AbstractElement> allSucc = new Vector<AbstractElement>();
		SymbPredAbsAbstractElement e = (SymbPredAbsAbstractElement)element;
		CFANode src = (CFANode)e.getLocation();

		for (int i = 0; i < src.getNumLeavingEdges(); ++i) {
			AbstractElement newe = 
				getAbstractSuccessor(e, src.getLeavingEdge(i));
			if (newe != domain.getBottomElement()) {
				allSucc.add(newe);
			}
		}

		LazyLogger.log(CustomLogLevel.SpecificCPALevel, 
				allSucc.size(), " successors found");

		return allSucc;
	}

}
