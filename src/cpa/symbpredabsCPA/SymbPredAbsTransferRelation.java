package cpa.symbpredabsCPA;

import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import symbpredabstraction.BDDMathsatSymbPredAbstractionAbstractManager;
import symbpredabstraction.ParentsList;
import symbpredabstraction.PathFormula;
import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFAErrorNode;
import cfa.objectmodel.CFAFunctionDefinitionNode;
import cfa.objectmodel.CFANode;
import cfa.objectmodel.c.CallToReturnEdge;
import cfa.objectmodel.c.FunctionCallEdge;
import cfa.objectmodel.c.ReturnEdge;
import cmdline.CPAMain;

import common.Pair;

import cpa.common.CPATransferException;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.TransferRelation;
import cpa.symbpredabs.AbstractFormula;
import cpa.symbpredabs.AbstractFormulaManager;
import cpa.symbpredabs.PredicateMap;
import cpa.symbpredabs.SSAMap;
import cpa.symbpredabs.SymbolicFormula;
import cpa.symbpredabs.SymbolicFormulaManager;
import cpa.symbpredabs.mathsat.MathsatSymbolicFormula;
import cpa.symbpredabs.mathsat.MathsatSymbolicFormulaManager;
import exceptions.CPAException;
import exceptions.SymbPredAbstTransferException;

/**
 * Transfer relation for symbolic lazy abstraction with summaries
 *
 * @author Erkan
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

  private int numAbstractStates = 0; // for statistics

  private SymbPredAbsAbstractDomain domain;
  // TODO maybe we should move these into CPA later
  // associate a Mathsat Formula Manager with the transfer relation
  private SymbolicFormulaManager symbolicFormulaManager;
  //private BDDMathsatSummaryAbstractManager
  private AbstractFormulaManager abstractFormulaManager;
  // private SymbAbsBDDMathsatAbstractFormulaManager bddMathsatMan;

  // this is for debugging purposes, also we can use it later when we use
  // mergeJoin for SummaryCPA, keeps line numbers not locations
  public static Set<Integer> extraAbstractionLocations = 
    new HashSet<Integer>();

  // a namespace to have a unique name for each variable in the program.
  // Whenever we enter a function, we push its name as namespace. Each
  // variable will be instantiated inside mathsat as namespace::variable
  // private Stack<String> namespaces;
  // TODO
  // private String namespace;
  // global variables (do not live in any namespace)
  // private Set<String> globalVars;

  public SymbPredAbsTransferRelation(AbstractDomain d, SymbolicFormulaManager symFormMan, AbstractFormulaManager abstFormMan) {

    domain = (SymbPredAbsAbstractDomain) d;
    abstractFormulaManager = abstFormMan;
    symbolicFormulaManager = symFormMan;

    // a set of lines given on property files to mark their successors as 
    // abstraction nodes for debugging purposes

    String lines[] = CPAMain.cpaConfig.getPropertiesArray("abstraction.extraLocations");
    if(lines != null && lines.length > 0){
      for(String line:lines){
        extraAbstractionLocations.add(Integer.getInteger(line));
      }
    }

    // setNamespace("");
    // globalVars = new HashSet<String>();
    // abstractTree = new ART();
  }

  public int getNumAbstractStates() {
    return numAbstractStates;
  }

  // abstract post operation
  private AbstractElement buildSuccessor (SymbPredAbsAbstractElement element,
                                          CFAEdge edge) throws CPATransferException {
    // TODO fix later
    SymbPredAbsAbstractElement newElement = null;
    // SymbPredAbsCPA cpa = domain.getCPA();
    CFANode succLoc = edge.getSuccessor();
    // TODO check whether the successor is an error location: if so, we want
    // to check for feasibility of the path...

    // check if the successor is an abstraction location
    boolean b = isAbstractionLocation(succLoc);

    if (!b) {
      try {
        newElement = new SymbPredAbsAbstractElement(domain, element.getAbstractionLocation());
        try {
          handleNonAbstractionLocation(element, newElement, edge);
          // TODO change this exception later
        } catch (cpa.symbpredabs.UnrecognizedCFAEdgeException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      } catch (SymbPredAbstTransferException e) {
        e.printStackTrace();
      }
    }

    else {
      newElement = new SymbPredAbsAbstractElement(domain, succLoc);
      // register newElement as an abstraction node
      newElement.setAbstractionNode();
      try {
        handleAbstractionLocation(element, newElement, edge);
        // TODO change type of the exception later
      } catch (cpa.symbpredabs.UnrecognizedCFAEdgeException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    return newElement;
    
  }

  private void handleAbstractionLocation(SymbPredAbsAbstractElement element,
                                         SymbPredAbsAbstractElement newElement, CFAEdge edge) throws cpa.symbpredabs.UnrecognizedCFAEdgeException {
    
    BDDMathsatSymbPredAbstractionAbstractManager bddAbstractFormulaManager  = (BDDMathsatSymbPredAbstractionAbstractManager)abstractFormulaManager;
    
    SSAMap maxIndex = new SSAMap();
    ParentsList parents = element.getParents();
    // TODO check this (false, false is used when constructing pf for
    // summary nodes)
    PathFormula pf = null;
    SSAMap ssamap = new SSAMap();
    pf = new PathFormula(symbolicFormulaManager.makeTrue(), ssamap);
    newElement.setPathFormula(pf);
    newElement.setMaxIndex(maxIndex);

    // add the parent to the list
    ParentsList newParents = new ParentsList();
    newParents.copyFromExisting(parents);
    newElement.setParents(newParents);
    newElement.addParent(edge.getSuccessor().getNodeNumber());

    PathFormula functionUpdatedFormula;
    AbstractFormula abs = element.getAbstraction();;
    
    PredicateMap pmap = element.getPredicates();
    newElement.setPredicates(pmap);

    AbstractFormula abst;

    if(edge instanceof FunctionCallEdge){
      // TODO check
      PathFormula functionInitFormula = new PathFormula(element.getPathFormula().getSymbolicFormula(), 
          element.getPathFormula().getSsa());
      PathFormula functionUpdatedFormula1 = toPathFormula(symbolicFormulaManager.makeAnd(element.getPathFormula().getSymbolicFormula(), 
          edge, element.getPathFormula().getSsa(), false, false));

      MathsatSymbolicFormulaManager mathsatManager = (MathsatSymbolicFormulaManager) symbolicFormulaManager;

      PathFormula functionUpdatedFormula2 = toPathFormula(mathsatManager.makeAndEnterFunction(
          ((MathsatSymbolicFormula)mathsatManager.makeTrue()), edge.getSuccessor(),
          functionUpdatedFormula1.getSsa(), false, false));

      Pair<Pair<SymbolicFormula, SymbolicFormula>,SSAMap> pm = mathsatManager.mergeSSAMaps(functionUpdatedFormula2.getSsa(), functionUpdatedFormula1.getSsa(), false);
//    MathsatSymbolicFormula old = (MathsatSymbolicFormula)mathsatManager.makeAnd(
//    functionUpdatedFormula2.getSymbolicFormula(), functionUpdatedFormula1.getSymbolicFormula());
//    SymbolicFormula newFormula = mathsatManager.makeAnd( functionUpdatedFormula1.getSymbolicFormula(), pm.getFirst().getSecond());
      SymbolicFormula newFormula = mathsatManager.makeAnd(functionUpdatedFormula1.getSymbolicFormula(), functionUpdatedFormula2.getSymbolicFormula());
      functionUpdatedFormula = new PathFormula(newFormula, pm.getSecond());

//    functionInitFormula = toPathFormula(((MathsatSymbolicFormulaManager)symbolicFormulaManager).makeAndEnterFunction(
//    (MathsatSymbolicFormula)functionInitFormula.getSymbolicFormula(), edge.getSuccessor(),
//    functionInitFormula.getSsa(), false, false));

      // TODO check

//    m1 = (MathsatSymbolicFormula)p.getSymbolicFormula();
//    f1 = m1;
//    ssa = p.getSsa();
      assert(functionInitFormula != null);
      newElement.setInitAbstractionSet(functionInitFormula);
      
      abst = bddAbstractFormulaManager.buildAbstraction(symbolicFormulaManager, abs, functionUpdatedFormula, pmap.getRelevantPredicates(edge.getSuccessor()), null);
    }
    else if(edge instanceof ReturnEdge){
      // TODO check
      PathFormula functionInitFormula = toPathFormula(symbolicFormulaManager.makeAnd(element.getPathFormula().getSymbolicFormula(), 
          edge, element.getPathFormula().getSsa(), false, false));

      assert(functionInitFormula != null);
      newElement.setInitAbstractionSet(functionInitFormula);

      functionUpdatedFormula = functionInitFormula;
      
      CallToReturnEdge summaryEdge = edge.getSuccessor().getEnteringSummaryEdge();
      SymbPredAbsAbstractElement previousElem = (SymbPredAbsAbstractElement)summaryEdge.extractAbstractElement("SymbPredAbsAbstractElement");
      MathsatSymbolicFormulaManager mmgr = (MathsatSymbolicFormulaManager) symbolicFormulaManager;

      AbstractFormula ctx = previousElem.getAbstraction();
      MathsatSymbolicFormula fctx = (MathsatSymbolicFormula)mmgr.instantiate(abstractFormulaManager.toConcrete(mmgr, ctx), null);

      abst = bddAbstractFormulaManager.buildAbstraction(symbolicFormulaManager, abs, functionUpdatedFormula, pmap.getRelevantPredicates(edge.getSuccessor()), fctx);
    }
    else{
      newElement.setInitAbstractionSet(element.getPathFormula());
      functionUpdatedFormula = newElement.getInitAbstractionSet();
      abst = bddAbstractFormulaManager.buildAbstraction(symbolicFormulaManager, abs, functionUpdatedFormula, pmap.getRelevantPredicates(edge.getSuccessor()), null);
    }

    // TODO cartesian abstraction
//  if (CPAMain.cpaConfig.getBooleanValue(
//  "cpas.symbpredabs.abstraction.cartesian")) {
//  abst = computeCartesianAbstraction(element, newElement, edge);
//  }
//  else{

//  abst = computeBooleanAbstraction(element, newElement, edge);
//  }

    // TODO move below
    newElement.setAbstraction(abst);

    if (abstractFormulaManager.isFalse(abst)) {
      // TODO later
      // return domain.getBottomElement();
    }
    else{
      // TODO refinement part here
    }
  }

  // TODO implement support for pfParents
  private void handleNonAbstractionLocation(
                                            SymbPredAbsAbstractElement element,
                                            SymbPredAbsAbstractElement newElement, CFAEdge edge)
  throws SymbPredAbstTransferException, cpa.symbpredabs.UnrecognizedCFAEdgeException {
    AbstractFormula abst = element.getAbstraction();
    PredicateMap pmap = element.getPredicates();
    ParentsList parents = element.getParents();
    // TODO check this (false, false is used when constructing pf for
    // summary nodes)
    PathFormula pf = null;
    pf = toPathFormula(symbolicFormulaManager.makeAnd(
        element.getPathFormula().getSymbolicFormula(),
        edge, element.getPathFormula().getSsa(), false, false));
    // TODO check these 3 lines
    // SymbolicFormula t1 = pf.getSymbolicFormula();
    SSAMap ssa1 = pf.getSsa();
    assert(pf != null);
    newElement.setPathFormula(pf);
    // TODO check
    newElement.updateMaxIndex(ssa1);
    newElement.setAbstraction(abst);
    newElement.setParents(parents);
    newElement.setInitAbstractionSet(null);
    newElement.setPredicates(pmap);
  }

  @Override
  public AbstractElement getAbstractSuccessor(AbstractElement element,
                                              CFAEdge cfaEdge) throws CPATransferException {

    System.out.println(cfaEdge);
    SymbPredAbsAbstractElement e = (SymbPredAbsAbstractElement)element;
    AbstractElement ret = buildSuccessor(e, cfaEdge);

    // TODO art
//  if (ret != domain.getBottomElement()) {
//  abstractTree.addChild(e, ret);
//  }

    return ret;

    //LazyLogger.log(CustomLogLevel.SpecificCPALevel, "Successor is: BOTTOM");

    //return domain.getBottomElement();
  }

  @Override
  public List<AbstractElement> getAllAbstractSuccessors(
      AbstractElement element) throws CPAException, CPATransferException {
    throw new CPAException ("Cannot get all abstract successors from non-location domain");
  }

  public boolean isAbstractionLocation(CFANode succLoc) {

    if (succLoc.isLoopStart() || succLoc instanceof CFAErrorNode
        || succLoc.getNumLeavingEdges() == 0) {
      return true;
    } else if (succLoc instanceof CFAFunctionDefinitionNode) {
      return true;
    } else if (succLoc.getEnteringSummaryEdge() != null) {
      return true;
    } else if (extraAbstractionLocations.contains(succLoc.getLineNumber())) {
      return true;
    }
    else {
      return false;
    }
  }

  private PathFormula toPathFormula(Pair<SymbolicFormula, SSAMap> pair) {
    return new PathFormula(pair.getFirst(), pair.getSecond());
  }

}
