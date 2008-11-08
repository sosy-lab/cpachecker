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

import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTInitializer;
import org.eclipse.cdt.core.dom.ast.IASTInitializerExpression;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;

import symbpredabstraction.AbstractFormula;
import symbpredabstraction.BDDMathsatSymbPredAbsAbstractManager;
import symbpredabstraction.MathsatSymbPredAbsFormulaManager;
import symbpredabstraction.ParentsList;
import symbpredabstraction.PathFormula;
import symbpredabstraction.Predicate;
import symbpredabstraction.PredicateMap;
import symbpredabstraction.SSAMap;
import symbpredabstraction.SymbolicFormula;
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
import cpaplugin.cfa.objectmodel.c.GlobalDeclarationEdge;
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
import cpaplugin.exceptions.SymbPredAbstTransferException;
import cpaplugin.exceptions.UnrecognizedCFAEdgeException;
import cpaplugin.logging.CPACheckerLogger;
import cpaplugin.logging.CustomLogLevel;
import cpaplugin.logging.LazyLogger;

/**
 * Transfer relation for symbolic lazy abstraction with summaries
 * 
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class SymbPredAbsTransferRelation implements TransferRelation {

	// // the Abstract Reachability Tree
	// class ART {
	// Map<AbstractElement, Collection<AbstractElement>> tree;

	// public ART() {
	// tree = new HashMap<AbstractElement, Collection<AbstractElement>>();
	// }

	// public void addChild(AbstractElement parent, AbstractElement child) {
	// if (!tree.containsKey(parent)) {
	// tree.put(parent, new Vector<AbstractElement>());
	// }
	// Collection<AbstractElement> c = tree.get(parent);
	// c.add(child);
	// }

	// public Collection<AbstractElement> getSubtree(AbstractElement root,
	// boolean remove, boolean includeRoot) {
	// Vector<AbstractElement> ret = new Vector<AbstractElement>();

	// Stack<AbstractElement> toProcess = new Stack<AbstractElement>();
	// toProcess.push(root);

	// while (!toProcess.empty()) {
	// AbstractElement cur = toProcess.pop();
	// ret.add(cur);
	// if (tree.containsKey(cur)) {
	// toProcess.addAll(remove ? tree.remove(cur) : tree.get(cur));
	// }
	// }
	// if (!includeRoot) {
	// AbstractElement tmp = ret.lastElement();
	// assert(ret.firstElement() == root);
	// ret.setElementAt(tmp, 0);
	// ret.remove(ret.size()-1);
	// }
	// return ret;
	// }
	// }

	private SymbPredAbsAbstractDomain domain;
	// private ART abstractTree;

	private int numAbstractStates = 0; // for statistics
	private boolean errorReached = false;

	// TODO maybe we shold move these into CPA later
	// associate a Mathsat Formula Manager with the transfer relation
	private MathsatSymbPredAbsFormulaManager mathsatFormMan;
	//private BDDMathsatSummaryAbstractManager
	private BDDMathsatSymbPredAbsAbstractManager bddMathsatMan;
	// private SymbAbsBDDMathsatAbstractFormulaManager bddMathsatMan;

	// a namespace to have a unique name for each variable in the program.
	// Whenever we enter a function, we push its name as namespace. Each
	// variable will be instantiated inside mathsat as namespace::variable
	// private Stack<String> namespaces;
	// TODO
	private String namespace;
	// global variables (do not live in any namespace)
	private Set<String> globalVars;

	public SymbPredAbsTransferRelation(SymbPredAbsAbstractDomain d) {
		domain = d;
		mathsatFormMan = d.getCPA().getMathsatSymbPredAbsFormulaManager();
		bddMathsatMan = d.getCPA().getBDDMathsatSymbPredAbsAbstractManager();
		setNamespace("");
		globalVars = new HashSet<String>();
		// abstractTree = new ART();
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
		return (elem.getLocation() instanceof FunctionDefinitionNode);
	}

	private boolean isFunctionEnd(SymbPredAbsAbstractElement elem) {
		CFANode n = elem.getLocation();
		return (n.getNumLeavingEdges() == 1 && n.getLeavingEdge(0) instanceof ReturnEdge);
	}

	// abstract post operation
	private AbstractElement buildSuccessor(SymbPredAbsAbstractElement element,
			CFAEdge edge) throws CPATransferException {
		// TODO fix later
		SymbPredAbsAbstractElement newElement = null;
		// SymbPredAbsCPA cpa = domain.getCPA();
		CFANode succLoc = edge.getSuccessor();
		// TODO check whether the successor is an error location: if so, we want
		// to check for feasibility of the path...

		// check if the successor is an abstraction location
		boolean b = ((SymbPredAbsAbstractDomain)getAbstractDomain()).getCPA().isAbstractionLocation(succLoc);

		if (!b) {
			try {
				newElement = new SymbPredAbsAbstractElement(succLoc, element.getAbstractionLocation());
				handleNonAbstractionLocation(element, newElement, edge);
			} catch (SymbPredAbstTransferException e) {
				e.printStackTrace();
			}
		}

		else {
			newElement = new SymbPredAbsAbstractElement(succLoc, succLoc);
			handleAbstractionLocation(element, newElement, edge);
		}

		return newElement;

//		Collection<Predicate> predicates = cpa.getPredicateMap()
//		.getRelevantPredicates(edge.getSuccessor());

//		SymbPredAbsAbstractElement succ = new SymbPredAbsAbstractElement(
//		succLoc);
//		Map<CFANode, Pair<SymbolicFormula, SSAMap>> p = cpa
//		.getPathFormulas(succLoc);
//		succ.setPathFormulas(p);

//		// if e is the end of a function, we must find the correct return
//		// location
//		// if (isFunctionEnd(succ)) {
//		// SymbPredAbsCFANode retNode = e.topContextLocation();
//		// if (!succLoc.equals(retNode)) {
//		// LazyLogger.log(LazyLogger.DEBUG_1,
//		// "Return node for this call is: ", retNode,
//		// ", but edge leads to: ", succLoc, ", returning BOTTOM");
//		// return domain.getBottomElement();
//		// }
//		// }

//		// Stack<AbstractFormula> context =
//		// (Stack<AbstractFormula>)e.getContext().clone();
//		// if (isFunctionEnd(e)) {
//		// context.pop();
//		// }
//		// succ.setContext(context);
//		// succ.setContext(e.getContext(), false);
//		// if (isFunctionEnd(succ)) {
//		// succ.popContext();
//		// }

//		SymbPredAbsAbstractFormulaManager amgr = cpa
//		.getAbstractFormulaManager();
//		AbstractFormula abstraction = amgr.buildAbstraction(cpa
//		.getFormulaManager(), e, succ, predicates);
//		succ.setAbstraction(abstraction);
//		succ.setParent(e);

//		Level lvl = LazyLogger.DEBUG_1;
//		if (CPACheckerLogger.getLevel() <= lvl.intValue()) {
//		SymbPredAbsFormulaManager mgr = cpa.getFormulaManager();
//		LazyLogger.log(lvl, "COMPUTED ABSTRACTION: ", amgr.toConcrete(mgr,
//		abstraction));
//		}

//		if (amgr.isFalse(abstraction)) {
//		return domain.getBottomElement();
//		} else {
//		++numAbstractStates;
//		// if we reach an error state, we want to log this...
//		if (succ.getLocation().getInnerNode() instanceof CFAErrorNode) {
//		if (CPAMain.cpaConfig
//		.getBooleanValue("cpas.symbpredabs.abstraction.norefinement")) {
//		errorReached = true;
//		throw new ErrorReachedException(
//		"Reached error location, but refinement disabled");
//		}
//		// oh oh, reached error location. Let's check whether the
//		// trace is feasible or spurious, and in case refine the
//		// abstraction
//		//
//		// first we build the abstract path
//		Deque<SymbPredAbsAbstractElement> path = new LinkedList<SymbPredAbsAbstractElement>();
//		path.addFirst(succ);
//		SymbPredAbsAbstractElement parent = succ.getParent();
//		while (parent != null) {
//		path.addFirst(parent);
//		parent = parent.getParent();
//		}
//		CounterexampleTraceInfo info = amgr.buildCounterexampleTrace(
//		cpa.getFormulaManager(), path);
//		if (info.isSpurious()) {
//		LazyLogger.log(CustomLogLevel.SpecificCPALevel,
//		"Found spurious error trace, refining the ",
//		"abstraction");
//		performRefinement(path, info);
//		} else {
//		LazyLogger.log(CustomLogLevel.SpecificCPALevel,
//		"REACHED ERROR LOCATION!: ", succ,
//		" RETURNING BOTTOM!");
//		errorReached = true;
//		throw new ErrorReachedException(info.getConcreteTrace()
//		.toString());
//		}
//		return domain.getBottomElement();
//		}

//		if (isFunctionStart(succ)) {
//		// we push into the context the return location, which is
//		// the successor location of the summary edge
//		SymbPredAbsCFANode retNode = null;
//		for (CFANode l : e.getLeaves()) {
//		if (l instanceof FunctionDefinitionNode) {
//		assert (l.getNumLeavingEdges() == 1);
//		// assert(l.getNumEnteringEdges() == 1);

//		CFAEdge ee = l.getLeavingEdge(0);
//		SymbPredAbsInnerCFANode n = (SymbPredAbsInnerCFANode) ee
//		.getSuccessor();
//		if (n.getSummaryNode().equals(succ.getLocation())) {
//		CFANode pr = l.getEnteringEdge(0).getPredecessor();
//		CallToReturnEdge ce = pr.getLeavingSummaryEdge();
//		// assert(ce != null);
//		if (ce != null) {
//		retNode = ((SymbPredAbsInnerCFANode) ce
//		.getSuccessor()).getSummaryNode();
//		break;
//		}
//		}
//		}
//		}
//		// assert(retNode != null);
//		if (retNode != null) {
//		LazyLogger.log(LazyLogger.DEBUG_3, "PUSHING CONTEXT TO ",
//		succ, ": ", cpa.getAbstractFormulaManager()
//		.toConcrete(cpa.getFormulaManager(),
//		succ.getAbstraction()));
//		// succ.getContext().push(succ.getAbstraction());
//		succ.pushContext(succ.getAbstraction(), retNode);
//		}
//		}

//		return succ;
//		}
	}

	private void handleAbstractionLocation(SymbPredAbsAbstractElement element,
			SymbPredAbsAbstractElement newElement, CFAEdge edge) {
		SSAMap maxIndex = new SSAMap();
		// TODO update abstraction
		// abstraction is set to true
		AbstractFormula abst = bddMathsatMan.makeTrue();
		newElement.setAbstraction(abst);

		ParentsList parents = element.getParents();
		// TODO check this (false, false is used when constructing pf for
		// summary nodes)
		PathFormula pf = null;
		SSAMap ssamap = new SSAMap();
		pf = new PathFormula(mathsatFormMan.makeTrue(), ssamap);
		newElement.setPathFormula(pf);
		newElement.setMaxIndex(maxIndex);

		// add the parent to the list
		ParentsList newParents = new ParentsList();
		newParents.copyFromExisting(parents);
		newElement.setParents(newParents);
		newElement.addParent(edge.getSuccessor().getNodeNumber());

		newElement.setInitAbstractionSet(element.getPathFormula());

		// TODO set predicates
		PredicateMap pmap = element.getPredicates();
		newElement.setPredicates(pmap);

	}

	// TODO implement support for pfParents
	private void handleNonAbstractionLocation(
			SymbPredAbsAbstractElement element,
			SymbPredAbsAbstractElement newElement, CFAEdge edge)
	throws SymbPredAbstTransferException {
		AbstractFormula abst = element.getAbstraction();
		PredicateMap pmap = element.getPredicates();
		ParentsList parents = element.getParents();
		// TODO check this (false, false is used when constructing pf for
		// summary nodes)
		PathFormula pf = null;
		try {
			pf = mathsatFormMan.makeAnd(
					element.getPathFormula().getSymbolicFormula(), 
					edge, element.getPathFormula().getSsa(), false, false);
			// TODO check these 3 lines
			SymbolicFormula t1 = pf.getSymbolicFormula();
			SSAMap ssa1 = pf.getSsa();
			assert(pf != null);
			newElement.setPathFormula(pf);
			// TODO check
			newElement.updateMaxIndex(ssa1);
		} catch (UnrecognizedCFAEdgeException e) {
			e.printStackTrace();
		}
		newElement.setAbstraction(abst);
		newElement.setParents(parents);
		newElement.setInitAbstractionSet(null);
		newElement.setPredicates(pmap);
	}

	// looks up the variable in the current namespace
	private String scoped(String var) {
		if (globalVars.contains(var)) {
			return var;
		} else {
			return getNamespace() + "::" + var;
		}
	}

	private void setNamespace(String ns) {
		namespace = ns;
	}

	private String getNamespace() {
		return namespace;
	}

	// TODO for return edge, check later
	// private PathFormula makeAndExitFunction(
	// SymbPredAbsAbstractElement element,
	// SymbPredAbsAbstractElement newElement, CFANode predecessor,
	// boolean updateSSA, boolean absoluteSSAIndices) {
	// assert()
	// IASTExpression retExp = ce.getExpression();
	// if (retExp instanceof IASTFunctionCallExpression) {
	// // this should be a void return, just do nothing...
	// //popNamespace();
	// return new Pair<SymbolicFormula, SSAMap>(m1, ssa);
	// } else if (retExp instanceof IASTBinaryExpression) {
	// IASTBinaryExpression exp = (IASTBinaryExpression)retExp;
	// assert(exp.getOperator() == IASTBinaryExpression.op_assign);
	// String retvar = scoped(VAR_RETURN_NAME);
	// //assert(ssa.getIndex(retvar) < 0);
	// //popNamespace();
	// if (!updateSSA) {
	// SSAMap newssa = new SSAMap();
	// newssa.copyFrom(ssa);
	// ssa = newssa;
	// }
	// int retidx = ssa.getIndex(retvar);
	// if (retidx < 0) {
	// retidx = absoluteSSAIndices ? SSAMap.getNextSSAIndex() : 1;
	// }
	// ssa.setIndex(retvar, retidx);
	// long msatretvar = buildMsatVariable(retvar, retidx);
	// IASTExpression e = exp.getOperand1();
	// // TODO - we assume this is an assignment to a plain variable. If
	// // we want to handle structs, this might not be the case anymore...
	// assert(e instanceof IASTIdExpression);
	// setNamespace(ce.getSuccessor().getFunctionName());
	// String outvar = ((IASTIdExpression)e).getName().getRawSignature();
	// outvar = scoped(outvar);
	// int idx = ssa.getIndex(outvar);
	// if (idx < 0) {
	// idx = autoInstantiateVar(outvar, ssa);
	// if (idx == 1) {
	// ++idx;
	// ssa.setIndex(outvar, idx);
	// }
	// } else {
	// //idx = absoluteSSAIndices ? SSAMap.getNextSSAIndex() : idx+1;
	// idx = absoluteSSAIndices ? SSAMap.getNextSSAIndex() :
	// getNewIndex(outvar, ssa);
	// ssa.setIndex(outvar, idx);
	// }
	// long msatoutvar = buildMsatVariable(outvar, idx);
	// long term = makeAssignment(msatoutvar, msatretvar);
	// term = mathsat.api.msat_make_and(msatEnv, m1.getTerm(), term);
	// return new Pair<SymbolicFormula, SSAMap>(
	// new MathsatSymbolicFormula(term), ssa);
	// } else {
	// throw new UnrecognizedCFAEdgeException(
	// "UNKNOWN FUNCTION EXIT EXPRESSION: " +
	// ce.getRawStatement());
	// }
	// }

	// TODO for function call - check later
	// private PathFormula makeAndFunctionCall(SymbPredAbsAbstractElement
	// element,
	// SymbPredAbsAbstractElement newElement, CFANode predecessor,
	// boolean updateSSA, boolean absoluteSSAIndices) {
	// if (edge.isExternalCall()) {
	// throw new UnrecognizedCFAEdgeException(
	// "EXTERNAL CALL UNSUPPORTED: " + edge.getRawStatement());
	// } else {
	// // build the actual parameters in the caller's context
	// long[] msatActualParams;
	// if (edge.getArguments() == null) {
	// msatActualParams = new long[0];
	// } else {
	// msatActualParams = new long[edge.getArguments().length];
	// IASTExpression[] actualParams = edge.getArguments();
	// for (int i = 0; i < msatActualParams.length; ++i) {
	// msatActualParams[i] = buildMsatTerm(actualParams[i], ssa,
	// absoluteSSAIndices);
	// if (mathsat.api.MSAT_ERROR_TERM(msatActualParams[i])) {
	// throw new UnrecognizedCFAEdgeException(
	// "ERROR CONVERTING: " + edge.getRawStatement());
	// }
	// }
	// }
	// // now switch to the context of the function
	// FunctionDefinitionNode fn =
	// (FunctionDefinitionNode)edge.getSuccessor();
	// setNamespace(fn.getFunctionName());
	// // create the symbolic vars for the formal parameters
	// List<IASTParameterDeclaration> formalParams =
	// fn.getFunctionParameters();
	// assert(formalParams.size() == msatActualParams.length);

	// int i = 0;
	// long term = mathsat.api.msat_make_true(msatEnv);
	// for (IASTParameterDeclaration param : formalParams) {
	// long arg = msatActualParams[i++];
	// if (param.getDeclarator().getPointerOperators().length != 0) {
	// throw new UnrecognizedCFAEdgeException("SORRY, POINTERS " +
	// "NOT HANDLED: " + edge.getRawStatement());
	// } else {
	// String paramName = scoped(FUNCTION_PARAM_NAME + (i-1));
	// int idx = ssa.getIndex(paramName);
	// if (idx < 0 || absoluteSSAIndices) {
	// idx = absoluteSSAIndices ? SSAMap.getNextSSAIndex() : 1;
	// } else {
	// //idx += 1;
	// idx = getNewIndex(paramName, ssa);
	// }
	// long msatParam = buildMsatVariable(paramName, idx);
	// if (mathsat.api.MSAT_ERROR_TERM(msatParam)) {
	// throw new UnrecognizedCFAEdgeException(
	// "ERROR HANDLING FUNCTION CALL: " +
	// edge.getRawStatement());
	// }
	// ssa.setIndex(paramName, idx);
	// long eq = makeAssignment(msatParam, arg);
	// term = mathsat.api.msat_make_and(msatEnv, term, eq);
	// }
	// }
	// assert(!mathsat.api.MSAT_ERROR_TERM(term));
	// term = mathsat.api.msat_make_and(msatEnv, m1.getTerm(), term);
	// return new Pair<SymbolicFormula, SSAMap>(
	// new MathsatSymbolicFormula(term), ssa);
	// }
	// }


	/*
	 * checks whether the given expression is going to modify the SSAMap. If
	 * not, we can avoid copying it
	 */
	private boolean needsSSAUpdate(IASTExpression expr) {
		if (expr instanceof IASTUnaryExpression) {
			switch (((IASTUnaryExpression) expr).getOperator()) {
			case IASTUnaryExpression.op_postFixIncr:
			case IASTUnaryExpression.op_prefixIncr:
			case IASTUnaryExpression.op_postFixDecr:
			case IASTUnaryExpression.op_prefixDecr:
				return true;
			}
		} else if (expr instanceof IASTBinaryExpression) {
			switch (((IASTBinaryExpression) expr).getOperator()) {
			case IASTBinaryExpression.op_assign:
			case IASTBinaryExpression.op_plusAssign:
			case IASTBinaryExpression.op_minusAssign:
			case IASTBinaryExpression.op_multiplyAssign:
				return true;
			}
		}
		return false;
	}

	// TODO -- return from function
	// private PathFormula makeAndReturn(SymbPredAbsAbstractElement element,
	// SymbPredAbsAbstractElement newElement, CFANode predecessor,
	// boolean updateSSA, boolean absoluteSSAIndices) {

	// IASTExpression exp = edge.getExpression();
	// if (exp == null) {
	// // this is a return from a void function, do nothing
	// return new Pair<SymbolicFormula, SSAMap>(m1, ssa);
	// } else if (exp instanceof IASTUnaryExpression) {
	// // we have to save the information about the return value,
	// // so that we can use it later on, if it is assigned to
	// // a variable. We create a function::<retval> variable
	// // that will hold the return value
	// String retvalname = scoped(VAR_RETURN_NAME);
	// assert(ssa.getIndex(retvalname) < 0);
	// int idx = absoluteSSAIndices ? SSAMap.getNextSSAIndex() : 2;
	// if (idx == 1) ++idx;
	// if (!updateSSA) {
	// SSAMap ssa2 = new SSAMap();
	// for (String var : ssa.allVariables()) {
	// ssa2.setIndex(var, ssa.getIndex(var));
	// }
	// ssa = ssa2;
	// }
	// long retvar = buildMsatVariable(retvalname, idx);
	// ssa.setIndex(retvalname, idx);
	// long retval = buildMsatTerm(exp, ssa, absoluteSSAIndices);
	// if (!mathsat.api.MSAT_ERROR_TERM(retval)) {
	// long term = makeAssignment(retvar, retval);
	// if (!mathsat.api.MSAT_ERROR_TERM(term)) {
	// term = mathsat.api.msat_make_and(
	// msatEnv, m1.getTerm(), term);
	// assert(!mathsat.api.MSAT_ERROR_TERM(term));
	// return new Pair<SymbolicFormula, SSAMap>(
	// new MathsatSymbolicFormula(term), ssa);
	// }
	// }
	// }
	// // if we are here, we can't handle the return properly...
	// throw new UnrecognizedCFAEdgeException("UNRECOGNIZED: " +
	// edge.getRawStatement());
	// }

	// TODO function call
	// private PathFormula makeAndEnterFunction(
	// SymbPredAbsAbstractElement element,
	// SymbPredAbsAbstractElement newElement, CFANode predecessor,
	// boolean updateSSA, boolean absoluteSSAIndices) {

	// FunctionDefinitionNode fn = (FunctionDefinitionNode)pred;
	// List<IASTParameterDeclaration> params = fn.getFunctionParameters();
	// if (params.isEmpty()) {
	// return new Pair<SymbolicFormula, SSAMap>(m1, ssa);
	// }
	// if (!updateSSA) {
	// SSAMap newssa = new SSAMap();
	// newssa.copyFrom(ssa);
	// ssa = newssa;
	// }
	// long term = mathsat.api.msat_make_true(msatEnv);
	// int i = 0;
	// for (IASTParameterDeclaration param : params) {
	// String paramName = scoped(FUNCTION_PARAM_NAME + (i++));
	// int idx = ssa.getIndex(paramName);
	// if (idx < 0) {
	// idx = absoluteSSAIndices ? SSAMap.getNextSSAIndex() : 1;
	// }
	// long msatParam = buildMsatVariable(paramName, idx);
	// if (mathsat.api.MSAT_ERROR_TERM(msatParam)) {
	// throw new UnrecognizedCFAEdgeException(
	// "ERROR ENTERING FUNCTION: " +
	// fn.getFunctionDefinition().getRawSignature());
	// }
	// if (param.getDeclarator().getPointerOperators().length != 0) {
	// throw new UnrecognizedCFAEdgeException("SORRY, POINTERS " +
	// "NOT HANDLED: " +
	// fn.getFunctionDefinition().getRawSignature());
	// } else {
	// String formalParamName =
	// scoped(param.getDeclarator().getName().toString());
	// idx = ssa.getIndex(formalParamName);
	// if (idx < 0 || absoluteSSAIndices) {
	// idx = absoluteSSAIndices ? SSAMap.getNextSSAIndex() : 1;
	// } else {
	// //idx += 1;
	// idx = getNewIndex(formalParamName, ssa);
	// }
	// long msatFormalParam = buildMsatVariable(formalParamName, idx);
	// if (mathsat.api.MSAT_ERROR_TERM(msatParam)) {
	// throw new UnrecognizedCFAEdgeException(
	// "ERROR HANDLING FUNCTION CALL: " +
	// fn.getFunctionDefinition().getRawSignature());
	// }
	// ssa.setIndex(formalParamName, idx);
	// long eq = makeAssignment(msatFormalParam, msatParam);
	// term = mathsat.api.msat_make_and(msatEnv, term, eq);
	// }
	// }
	// term = mathsat.api.msat_make_and(msatEnv, m1.getTerm(), term);
	// return new Pair<SymbolicFormula, SSAMap>(
	// new MathsatSymbolicFormula(term), ssa);
	// }

//	private void handleAbstractionLocation(
//	SymbPredAbsAbstractElement element,
//	SymbPredAbsAbstractElement newElement, CFAEdge edge) {

//	// if all nodes are processed go to abstraction phase
//	// if not 

//	// update the abstract element
//	// get the successor node
//	CFANode succLocation = edge.getSuccessor();
//	// successor node is now the abstraction location
//	CFANode abstractionLoc = succLocation;
//	SymbPredAbsAbstractElement parent = element;
//	// TODO check this (false, false is used when constructing pf for
//	// summary nodes)
//	// path formula is set to TRUE
//	// TODO update mgr and ssa - do we create a new ssamap or do
//	// we update the ssamap from the previous element
//	PathFormula pf = new PathFormula(mgr.makeTrue(), ssamap)
//	newElement.setLocation(succLocation);
//	newElement.setAbstractionLocation(abstractionLoc);
//	// TODO that we will do at the end
//	//newElement.setAbstraction(abst);
//	newElement.setParent(parent);
//	newElement.setPathFormula(pf);
//	// TODO what about predicates?
////	PredicateMap pmap = element.getPredicates();
////	newElement.setPredicates(pmap);

//	// we will update this in this method
//	AbstractFormula abstraction;

//	// TODO check
//	// long msatEnv = mmgr.getMsatEnv();
//	long msatEnv = mathsatFormMan.getMsatEnv();
//	long absEnv = mathsat.api.msat_create_shared_env(msatEnv);

//	// first, build the concrete representation of the abstract formula of e
//	AbstractFormula abs = element.getAbstraction();
//	MathsatSymbolicFormula fabs =
//	// TODO check
//	(MathsatSymbolicFormula) mathsatFormMan.instantiate(
//	// TODO check
//	bddMathsatMan.toConcrete(/* mmgr */mathsatFormMan, abs), null);

//	// TODO function exit
//	// if (isFunctionExit(e)) {
//	// // we have to take the context before the function call
//	// // into account, otherwise we are not building the right
//	// // abstraction!
//	// if (CPAMain.cpaConfig.getBooleanValue(
//	// "cpas.symbpredabs.refinement.addWellScopedPredicates")) {
//	// // but only if we are adding well-scoped predicates, otherwise
//	// // this should not be necessary
//	// AbstractFormula ctx = e.topContextAbstraction();
//	// MathsatSymbolicFormula fctx =
//	// (MathsatSymbolicFormula)mmgr.instantiate(
//	// toConcrete(mmgr, ctx), null);
//	// fabs = (MathsatSymbolicFormula)mmgr.makeAnd(fabs, fctx);

//	// LazyLogger.log(LazyLogger.DEBUG_3,
//	// "TAKING CALLING CONTEXT INTO ACCOUNT: ", fctx);
//	// } else {
//	// LazyLogger.log(LazyLogger.DEBUG_3,
//	// "NOT TAKING CALLING CONTEXT INTO ACCOUNT,",
//	// "as we are not using well-scoped predicates");
//	// }
//	// }

//	// TODO check
//	SSAMap absSsa = mathsatFormMan.extractSSA(fabs);

//	SymbolicFormula f = null;
//	SSAMap ssa = null;

//	// TODO implement cache later
//	// Pair<CFANode, CFANode> key = new Pair<CFANode, CFANode>(
//	// e.getLocationNode(), succ.getLocationNode());
//	// if (abstractionCache.containsKey(key)) {
//	// Pair<MathsatSymbolicFormula, SSAMap> pc = abstractionCache.get(key);
//	// f = pc.getFirst();
//	// ssa = pc.getSecond();
//	// } else {
//	// TODO check
//	// Pair<SymbolicFormula, SSAMap> pc =
//	// buildConcreteFormula(mmgr, e, succ, false);
//	PathFormula pc = buildConcreteFormula(mathsatFormMan, element, newElement,
//	false);
//	// SymbolicFormula f = pc.getFirst();
//	// SSAMap ssa = pc.getSecond();
//	f = pc.getFirst();
//	ssa = pc.getSecond();

//	pc = mmgr.shift(f, absSsa);
//	f = mmgr.replaceAssignments((MathsatSymbolicFormula) pc.getFirst());
//	ssa = pc.getSecond();

//	abstractionCache.put(key, new Pair<MathsatSymbolicFormula, SSAMap>(
//	(MathsatSymbolicFormula) f, ssa));

//	Pair<SymbolicFormula, SSAMap> pc = buildConcreteFormula(mmgr, e, succ,
//	false);
//	// SymbolicFormula f = pc.getFirst();
//	// SSAMap ssa = pc.getSecond();
//	f = pc.getFirst();
//	ssa = pc.getSecond();

//	pc = mmgr.shift(f, absSsa);
//	f = mmgr.replaceAssignments((MathsatSymbolicFormula) pc.getFirst());
//	ssa = pc.getSecond();

//	abstractionCache.put(key, new Pair<MathsatSymbolicFormula, SSAMap>(
//	(MathsatSymbolicFormula) f, ssa));
//	// }

//	if (CPAMain.cpaConfig
//	.getBooleanValue("cpas.symbpredabs.useBitwiseAxioms")) {
//	MathsatSymbolicFormula bitwiseAxioms = mmgr
//	.getBitwiseAxioms((MathsatSymbolicFormula) f);
//	f = mmgr.makeAnd(f, bitwiseAxioms);

//	LazyLogger.log(LazyLogger.DEBUG_3, "ADDED BITWISE AXIOMS: ",
//	bitwiseAxioms);
//	}

//	// long term = mathsat.api.msat_make_copy_from(
//	// absEnv, ((MathsatSymbolicFormula)f).getTerm(), msatEnv);
//	long term = ((MathsatSymbolicFormula) f).getTerm();
//	assert (!mathsat.api.MSAT_ERROR_TERM(term));

//	// build the definition of the predicates, and instantiate them
//	Object[] predinfo = buildPredList(mmgr, predicates);
//	long preddef = (Long) predinfo[0];
//	long[] important = (long[]) predinfo[1];
//	Collection<String> predvars = (Collection<String>) predinfo[2];
//	// for (int i = 0; i < important.length; ++i) {
//	// important[i] = mathsat.api.msat_make_copy_from(
//	// absEnv, important[i], msatEnv);
//	// }

//	// update the SSA map, by instantiating all the uninstantiated
//	// variables that occur in the predicates definitions (at index 1)
//	for (String var : predvars) {
//	if (ssa.getIndex(var) < 0) {
//	ssa.setIndex(var, 1);
//	}
//	}

//	if (CPACheckerLogger.getLevel() <= LazyLogger.DEBUG_1.intValue()) {
//	StringBuffer importantStrBuf = new StringBuffer();
//	for (long t : important) {
//	importantStrBuf.append(mathsat.api.msat_term_repr(t));
//	importantStrBuf.append(" ");
//	}
//	LazyLogger.log(LazyLogger.DEBUG_1, "IMPORTANT SYMBOLS (",
//	important.length, "): ", importantStrBuf);
//	}

//	// first, create the new formula corresponding to
//	// (f & edges from e to succ)
//	// TODO - at the moment, we assume that all the edges connecting e and
//	// succ have no statement or assertion attached (i.e. they are just
//	// return edges or gotos). This might need to change in the future!!
//	// (So, for now we don't need to to anything...)

//	// instantiate the definitions with the right SSA
//	MathsatSymbolicFormula inst = (MathsatSymbolicFormula) mmgr
//	.instantiate(new MathsatSymbolicFormula(preddef), ssa);
//	// preddef = mathsat.api.msat_make_copy_from(absEnv, inst.getTerm(),
//	// msatEnv);
//	// long curstate = mathsat.api.msat_make_copy_from(absEnv,
//	// fabs.getTerm(),
//	// msatEnv);
//	preddef = inst.getTerm();
//	long curstate = fabs.getTerm();

//	// the formula is (curstate & term & preddef)
//	// build the formula and send it to the absEnv
//	long formula = mathsat.api.msat_make_and(absEnv, mathsat.api
//	.msat_make_and(absEnv, curstate, term), preddef);
//	mathsat.api.msat_add_theory(absEnv, mathsat.api.MSAT_UF);
//	if (CPAMain.cpaConfig
//	.getBooleanValue("cpas.symbpredabs.mathsat.useIntegers")) {
//	mathsat.api.msat_add_theory(absEnv, mathsat.api.MSAT_LIA);
//	int ok = mathsat.api.msat_set_option(absEnv, "split_eq", "true");
//	assert (ok == 0);
//	} else {
//	mathsat.api.msat_add_theory(absEnv, mathsat.api.MSAT_LRA);
//	}
//	mathsat.api.msat_set_theory_combination(absEnv,
//	mathsat.api.MSAT_COMB_ACK);
//	int ok = mathsat.api.msat_set_option(absEnv, "toplevelprop", "2");
//	assert (ok == 0);

//	LazyLogger.log(LazyLogger.DEBUG_3, "COMPUTING ALL-SMT ON FORMULA: ",
//	new MathsatSymbolicFormula(formula));

//	int absbdd = bddManager.getZero();
//	AllSatCallbackStats func = new AllSatCallbackStats(absbdd, msatEnv,
//	absEnv);
//	long msatSolveStartTime = System.currentTimeMillis();
//	mathsat.api.msat_assert_formula(absEnv, formula);
//	int numModels = mathsat.api.msat_all_sat(absEnv, important, func);
//	assert (numModels != -1);
//	long msatSolveEndTime = System.currentTimeMillis();

//	mathsat.api.msat_destroy_env(absEnv);

//	// update statistics
//	long endTime = System.currentTimeMillis();
//	long msatSolveTime = (msatSolveEndTime - msatSolveStartTime)
//	- func.totTime;
//	long abstractionMsatTime = (endTime - startTime) - func.totTime;
//	stats.abstractionMaxMathsatTime = Math.max(abstractionMsatTime,
//	stats.abstractionMaxMathsatTime);
//	stats.abstractionMaxBddTime = Math.max(func.totTime,
//	stats.abstractionMaxBddTime);
//	stats.abstractionMathsatTime += abstractionMsatTime;
//	stats.abstractionBddTime += func.totTime;
//	stats.abstractionMathsatSolveTime += msatSolveTime;
//	stats.abstractionMaxMathsatSolveTime = Math.max(msatSolveTime,
//	stats.abstractionMaxMathsatSolveTime);

//	if (abstractionMsatTime > 1000 && dumpHardAbstractions) {
//	// we want to dump "hard" problems...
//	if (absPrinter == null) {
//	absPrinter = new BDDMathsatSummaryAbstractionPrinter(msatEnv,
//	"abs");
//	}
//	absPrinter.printMsatFormat(curstate, term, preddef, important);
//	absPrinter.printNusmvFormat(curstate, term, preddef, important);
//	absPrinter.nextNum();
//	}

//	if (numModels == -2) {
//	absbdd = bddManager.getOne();
//	return new BDDAbstractFormula(absbdd);
//	} else {
//	return new BDDAbstractFormula(func.getBDD());
//	}
//	}

	// builds the SymbolicFormula corresponding to the path between "e" and
	// "succ". In the purely explicit case, this would be just the operation
	// attached to the edge connecting "e" and "succ", but in our case this is
	// actually a loop-free subgraph of the original CFA
//	private PathFormula buildConcreteFormula(MathsatSummaryFormulaManager mgr,
//	SymbPredAbsAbstractElement e, SymbPredAbsAbstractElement succ,
//	boolean replaceAssignments) {
//	// first, get all the paths in e that lead to succ
//	Collection<Pair<SymbolicFormula, SSAMap>> relevantPaths = new Vector<Pair<SymbolicFormula, SSAMap>>();
//	for (CFANode leaf : e.getLeaves()) {
//	for (int i = 0; i < leaf.getNumLeavingEdges(); ++i) {
//	CFAEdge edge = leaf.getLeavingEdge(i);
//	InnerCFANode s = (InnerCFANode) edge.getSuccessor();
//	if (s.getSummaryNode().equals(succ.getLocation())) {
//	// ok, this path is relevant
//	relevantPaths.add(e.getPathFormula(leaf));

//	LazyLogger
//	.log(LazyLogger.DEBUG_1,
//	"FOUND RELEVANT PATH, leaf: ", leaf
//	.getNodeNumber());
//	LazyLogger.log(LazyLogger.DEBUG_3, "Formula: ", e
//	.getPathFormula(leaf).getFirst());
//	}
//	}
//	}
//	// now, we want to create a new formula that is the OR of all the
//	// possible paths. So we merge the SSA maps and OR the formulas
//	SSAMap ssa = new SSAMap();
//	SymbolicFormula f = mgr.makeFalse();
//	for (Pair<SymbolicFormula, SSAMap> p : relevantPaths) {
//	Pair<Pair<SymbolicFormula, SymbolicFormula>, SSAMap> mp = mgr
//	.mergeSSAMaps(ssa, p.getSecond(), false);
//	SymbolicFormula curf = p.getFirst();
//	if (replaceAssignments) {
//	curf = mgr.replaceAssignments((MathsatSymbolicFormula) curf);
//	}
//	f = mgr.makeAnd(f, mp.getFirst().getFirst());
//	curf = mgr.makeAnd(curf, mp.getFirst().getSecond());
//	f = mgr.makeOr(f, curf);
//	ssa = mp.getSecond();
//	}

//	return new Pair<SymbolicFormula, SSAMap>(f, ssa);
//	}


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
//	private void performRefinement(Deque<SymbPredAbsAbstractElement> path,
//	CounterexampleTraceInfo info) throws CPATransferException {
//	// TODO Auto-generated method stub
//	UpdateablePredicateMap curpmap = (UpdateablePredicateMap) domain
//	.getCPA().getPredicateMap();
//	AbstractElement root = null;
//	AbstractElement firstInterpolant = null;
//	for (SymbPredAbsAbstractElement e : path) {
//	Collection<Predicate> newpreds = info.getPredicatesForRefinement(e);
//	if (firstInterpolant == null && newpreds.size() > 0) {
//	firstInterpolant = e;
//	}
//	if (curpmap.update((CFANode) e.getLocation(), newpreds)) {
//	if (root == null) {
//	root = e.getParent();
//	}
//	}
//	}
//	if (root == null) {
//	root = firstInterpolant;
//	}
//	assert (root != null);
//	// root = path.getFirst();
//	Collection<AbstractElement> toWaitlist = new HashSet<AbstractElement>();
//	toWaitlist.add(root);
//	Collection<AbstractElement> toUnreach = abstractTree.getSubtree(root,
//	true, false);
//	SymbPredAbsCPA cpa = domain.getCPA();
//	for (AbstractElement e : toUnreach) {
//	Set<SymbPredAbsAbstractElement> cov = cpa
//	.getCoveredBy((SymbPredAbsAbstractElement) e);
//	for (AbstractElement c : cov) {
//	if (!((SymbPredAbsAbstractElement) c)
//	.isDescendant((SymbPredAbsAbstractElement) root)) {
//	toWaitlist.add(c);
//	}
//	}
//	cpa.uncoverAll((SymbPredAbsAbstractElement) e);
//	}
//	// Collection<AbstractElement> toUnreach = new
//	// Vector<AbstractElement>();
//	// boolean add = false;
//	// for (AbstractElement e : path) {
//	// if (add) {
//	// toUnreach.add(e);
//	// } else if (e == root) {
//	// add = true;
//	// }
//	// }
//	LazyLogger.log(LazyLogger.DEBUG_1, "REFINEMENT - toWaitlist: ", root);
//	LazyLogger.log(LazyLogger.DEBUG_1, "REFINEMENT - toUnreach: ",
//	toUnreach);
//	throw new RefinementNeededException(toUnreach, toWaitlist);
//	}

	@Override
	public AbstractElement getAbstractSuccessor(AbstractElement element,
			CFAEdge cfaEdge) throws CPATransferException {
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
				// TODO art
//				if (ret != domain.getBottomElement()) {
//				abstractTree.addChild(e, ret);
//				}
				return ret;
			}
		}

		LazyLogger.log(CustomLogLevel.SpecificCPALevel, "Successor is: BOTTOM");

		return domain.getBottomElement();
	}

	@Override
	public List<AbstractElement> getAllAbstractSuccessors(
			AbstractElement element) throws CPAException, CPATransferException {

		List<AbstractElement> allSucc = new Vector<AbstractElement>();
		SymbPredAbsAbstractElement e = (SymbPredAbsAbstractElement) element;
		CFANode src = (CFANode) e.getLocation();

		for (int i = 0; i < src.getNumLeavingEdges(); ++i) {
			AbstractElement newe = getAbstractSuccessor(e, src
					.getLeavingEdge(i));
			if (newe != domain.getBottomElement()) {
				allSucc.add(newe);
			}
		}

		LazyLogger.log(CustomLogLevel.SpecificCPALevel, allSucc.size(),
		" successors found");

		return allSucc;
	}

}
