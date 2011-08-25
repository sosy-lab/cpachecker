/*
path *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.explicit;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.Files;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.ast.DefaultExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.IASTArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTAssignment;
import org.sosy_lab.cpachecker.cfa.ast.IASTBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.RightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.StatementEdge;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.art.ARTReachedSet;
import org.sosy_lab.cpachecker.cpa.art.ARTUtils;
import org.sosy_lab.cpachecker.cpa.art.AbstractARTBasedRefiner;
import org.sosy_lab.cpachecker.cpa.art.Path;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateRefinementManager;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException.Reason;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.predicates.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;

import com.google.common.collect.Iterables;

@Options(prefix="cpa.predicate")
public class ExplicitRefiner extends AbstractARTBasedRefiner {

  @Option(name="errorPath.export",
      description="export one satisfying assignment for the error path")
  private boolean exportErrorPath = true;

  @Option(name="errorPath.file", type=Option.Type.OUTPUT_FILE,
      description="export one satisfying assignment for the error path")
  private File exportFile = new File("ErrorPathAssignment.txt");

  @Option(name="refinement.msatCexFile", type=Option.Type.OUTPUT_FILE,
      description="where to dump the counterexample formula in case the error location is reached")
  private File dumpCexFile = new File("counterexample.msat");

  @Option(name="refinement.useGlobalInterpolationPoint",
      description="use global interpolation point")
  private boolean useGlobalInterpolationPoint = true;

  final Timer totalRefinement = new Timer();
  final Timer precisionUpdate = new Timer();
  final Timer artUpdate = new Timer();
  final Timer errorPathProcessing = new Timer();

  private final LogManager logger;
  private final PredicateRefinementManager formulaManager;
  private CounterexampleTraceInfo mCounterexampleTraceInfo;
  private Path targetPath;
  protected List<CFANode> lastErrorPath = null;

  private final Set<String> globalVars;

  private final Set<String> allReferencedVariables = new HashSet<String>();

  public Map<CFAEdge, Map<String, Long>> assumptions = new HashMap<CFAEdge, Map<String, Long>>();

  private Path previousPath;

  public ExplicitRefiner(final ConfigurableProgramAnalysis pCpa) throws CPAException, InvalidConfigurationException {
    super(pCpa);
    ExplicitCPA explicitCPA = this.getArtCpa().retrieveWrappedCpa(ExplicitCPA.class);

    if (explicitCPA == null) {
      throw new InvalidConfigurationException(getClass().getSimpleName() + " needs a ExplicitCPA");
    }

    globalVars = ((ExplicitTransferRelation)explicitCPA.getTransferRelation()).getGlobalVars();

    explicitCPA.getConfiguration().inject(this, ExplicitRefiner.class);
    logger = explicitCPA.getLogger();

    formulaManager = explicitCPA.getPredicateManager();
    //explicitCPA.getStats().addRefiner(this);
  }

  @Override
  protected boolean performRefinement(ARTReachedSet pReached, Path path) throws CPAException, InterruptedException {
    totalRefinement.start();

    ARTElement targetElement = pReached.getLastElement();

    // elementsOnPath are irrelevant here
    Set<ARTElement> elementsOnPath = Collections.emptySet();

    logger.log(Level.FINEST, "Starting refinement for ExplicitCPA");

    // create list of formulas on path
    List<Formula> formulas = getFormulasForPath(path);
    assert path.size() == formulas.size();

    // build the counterexample
    mCounterexampleTraceInfo = formulaManager.buildCounterexampleTrace(formulas, elementsOnPath);
    targetPath = null;

    // if error is spurious refine
    if (mCounterexampleTraceInfo.isSpurious()) {
      logger.log(Level.FINEST, "Error trace is spurious, refining the abstraction");
      precisionUpdate.start();

      // get previous precision
      Precision oldPrecision = pReached.getPrecision(targetElement);
      ExplicitPrecision oldExplicitPrecision = Precisions.extractPrecisionByType(oldPrecision, ExplicitPrecision.class);
      if (oldExplicitPrecision == null) {
        throw new IllegalStateException("Could not find the ExplicitPrecision for the error element");
      }

      Pair<ARTElement, ExplicitPrecision> refinementResult =
        performRefinement(oldExplicitPrecision, path, mCounterexampleTraceInfo);
      precisionUpdate.stop();

      artUpdate.start();
      pReached.removeSubtree(refinementResult.getFirst(), refinementResult.getSecond());
      artUpdate.stop();

      totalRefinement.stop();
      return true;
    } else {
      // we have a real error
      logger.log(Level.FINEST, "Error trace is not spurious");
      errorPathProcessing.start();

      boolean preciseInfo = false;
      Map<Integer, Boolean> preds = mCounterexampleTraceInfo.getBranchingPredicates();
      if (preds.isEmpty()) {
        logger.log(Level.WARNING, "No information about ART branches available!");
      } else {
        targetPath = createPathFromPredicateValues(path, preds);

        if (targetPath != null) {
          // try to create a better satisfying assignment by replaying this single path
          try {
            CounterexampleTraceInfo info2 = formulaManager.checkPath(targetPath.asEdgesList());
            if (info2.isSpurious()) {
              logger.log(Level.WARNING, "Inconsistent replayed error path!");
            } else {
              mCounterexampleTraceInfo = info2;
              preciseInfo = true;
            }
          } catch (CPATransferException e) {
            // path is now suddenly a problem
            logger.log(Level.WARNING, "Could not replay error path (" + e.getMessage() + ")!");
          }
        }
      }
      errorPathProcessing.stop();

      if (exportErrorPath && exportFile != null) {
        if (!preciseInfo) {
          logger.log(Level.WARNING, "The produced satisfying assignment is imprecise!");
        }

        formulaManager.dumpCounterexampleToFile(mCounterexampleTraceInfo, dumpCexFile);
        try {
          Files.writeFile(exportFile, mCounterexampleTraceInfo.getCounterexample());
        } catch (IOException e) {
          logger.log(Level.WARNING, "Could not write satisfying assignment for error path to file! ("
              + e.getMessage() + ")");
        }
      }
      totalRefinement.stop();
      return false;
    }
  }

  protected List<Formula> getFormulasForPath(List<Pair<ARTElement, CFAEdge>> path) throws CPATransferException {

    ExplicitCPA explicitCPA = this.getArtCpa().retrieveWrappedCpa(ExplicitCPA.class);

    CtoFormulaConverter converter = null;

    try
    {
      converter = new CtoFormulaConverter(explicitCPA.getConfiguration(), explicitCPA.getFormulaManager(), logger);
    }
    catch(InvalidConfigurationException e)
    {
      //System.out.println("error when configuring CtoFormulaConverter");
    }

    PathFormulaManager pathFormulaManager = explicitCPA.getPathFormulaManager();

    PathFormula currentPathFormula = pathFormulaManager.makeEmptyPathFormula();

    List<Formula> formulas = new ArrayList<Formula>(path.size());

    for(Pair<ARTElement, CFAEdge> pathElement : path)
    {
      currentPathFormula = converter.makeAnd(currentPathFormula, pathElement.getSecond());

      formulas.add(currentPathFormula.getFormula());

      // reset the formula
      currentPathFormula = pathFormulaManager.makeEmptyPathFormula(currentPathFormula);
    }

    return formulas;
  }

  private static int refinementCounter = 0;
  private Pair<ARTElement, ExplicitPrecision> performRefinement(ExplicitPrecision oldPrecision,
      Path path,
      CounterexampleTraceInfo pInfo) throws CPAException {

//System.out.println("\n" + (++refinementCounter) + ". refining ...");
//System.out.println(path);
//System.out.println("old: " + oldPrecision.getWhiteList());

    // get the predicates ...
    PredicateMap predicates = new PredicateMap(pInfo.getPredicatesForRefinement(), path);

    // ... and, out of these, determine the initial set of variables to track
    Set<String> referencedVariables = predicates.getReferencedVariables();
//System.out.println("referencedVariables: " + referencedVariables);
    // add the newly found referenced variables to those found in previous iteration
    allReferencedVariables.addAll(referencedVariables);

    // when only passing referencedVariables, the same precision is passed in twice (refinement #9, refinement #10) with diskperf_simpl !?!?!?
    // get the top-most interpolation point
    Pair<ARTElement, CFAEdge> firstInterpolationPoint = getFirstInterpolationPoint(useGlobalInterpolationPoint ? allReferencedVariables : referencedVariables, path);

    assert firstInterpolationPoint != null;
//System.out.println("firstInterpolationPoint is: " + firstInterpolationPoint);

    // collect variables on error path, on which the found ones depend on
    CollectVariablesVisitor visitor = new CollectVariablesVisitor(allReferencedVariables/*referencedVariables*/);
    Iterator<Pair<ARTElement, CFAEdge>>  iterator = path.descendingIterator();
    while(iterator.hasNext())
    {
      Pair<ARTElement, CFAEdge> element = iterator.next();

      extractVariables(element.getSecond(), visitor);
    }

    allReferencedVariables.addAll(visitor.getCollectedVariables());
    Set<String> newWhiteList = new HashSet<String>(allReferencedVariables);

    //madeProgress(path, oldPrecision.getWhiteList(), newWhiteList);

    ExplicitPrecision newPrecision = new ExplicitPrecision(oldPrecision.getBlackListPattern(), newWhiteList);
//System.out.println("newWhiteList = " + newWhiteList);
    // would not use old facts
    //assumptions = new HashMap<CFAEdge, Map<String, Long>>();

    // can we actually keep facts from previous iterations?
    // new paths would result in new facts, right, so delete old facts
    // however, cutting all facts after firstInterpolationPoint would be safe, right?
    // for now, all facts are kept
newPrecision.setFacts(assumptions = predicates.getAssumptions(assumptions));

    // We have two different strategies for the refinement root: set it to
    // the firstInterpolationPoint or set it to highest location in the ART
    // where the same CFANode appears.
    // Both work, so this is a heuristics question to get the best performance.
    // My benchmark showed, that at least for the benchmarks-lbe examples it is
    // best to use strategy one iff newPredicatesFound.

    ARTElement root = null;
    // new predicates were found
    if (oldPrecision.getWhiteList().size() < newPrecision.getWhiteList().size()) {
      root = firstInterpolationPoint.getFirst();

      logger.log(Level.FINEST, "Found spurious counterexample,",
          "trying strategy 1: remove everything below", root, "from ART.");

    } else {

      CFANode loc = firstInterpolationPoint.getSecond().getPredecessor();

      logger.log(Level.FINEST, "Found spurious counterexample,",
          "trying strategy 2: remove everything below node", loc, "from ART.");

      // find first element in path with location == loc,
      // this is not necessary equal to firstInterpolationPoint.getFirst()
      for (Pair<ARTElement, CFAEdge> abstractionPoint : path) {
        if (abstractionPoint.getSecond().getPredecessor().equals(loc)) {
          root = abstractionPoint.getFirst();
          break;
        }
      }
      if (root == null) {
        throw new CPAException("Inconsistent ART, did not find element for " + loc);
      }
    }

    return Pair.of(root, newPrecision);
  }

  private Pair<ARTElement, CFAEdge> getFirstInterpolationPoint(Collection<String> currentVariables, Path path)
  {
    CollectVariablesVisitor visitor = new CollectVariablesVisitor(currentVariables);

    Pair<ARTElement, CFAEdge> firstInterpolationPoint = null;

    Iterator<Pair<ARTElement, CFAEdge>> iterator = path.descendingIterator();
    while(iterator.hasNext())
    {
      Pair<ARTElement, CFAEdge> element = iterator.next();

      if(extractVariables(element.getSecond(), visitor))
        firstInterpolationPoint = element;
    }

    return firstInterpolationPoint;
  }

  private boolean madeProgress(Path path, Set<String> oldWhiteList, Set<String> newWhiteList) throws RefinementFailedException
  {
    boolean madeProgress = false;

    if(previousPath == null)
    {
      //System.out.println("progress - previous path empty");
      madeProgress = true;
    }

    else
    {
      if(previousPath.toString().equals(path.toString()))
      {
        //System.out.println("paths match !!!");
      }
      else
        {
        //System.out.println("paths DO NOT match !!!");
        }

      if(oldWhiteList.size() != newWhiteList.size())
      {
        //System.out.println("progress - whitelist sizes differ");
        madeProgress = true;
      }

      else
      {
        for(String entry : newWhiteList)
        {
          if(!oldWhiteList.contains(entry))
          {
            //System.out.println("progress - whitelists differ");
            madeProgress = true;
            break;
          }
        }
      }
    }

    if(previousPath != null)
    {
      if(previousPath.size() != path.size())
      {
        //System.out.println("progress - path lengths differ");
        madeProgress = true;
      }

      if(!previousPath.toString().equals(path.toString()))
      {
        //System.out.println("progress - paths differ");
        madeProgress = true;
      }
      /*
      int i = 0;
      for(Pair<ARTElement, CFAEdge> pathElement : path)
      {
        if(!previousPath.get(i).equals(pathElement))
        {
          System.out.println("progress - paths differ - " + previousPath.get(i) + " != " + pathElement);
          madeProgress = true;
          break;
        }
        i++;
      }*/
    }

    previousPath = new Path();
    for(Pair<ARTElement, CFAEdge> pathElement : path)
    {
      previousPath.add(pathElement);
    }

    if(!madeProgress)
      throw new RefinementFailedException(Reason.RepeatedCounterexample, path);

    return madeProgress;
  }

  @Override
  protected Path getTargetPath(Path pPath) {
    if (targetPath == null) {
      logger.log(Level.WARNING, "The produced error path is imprecise!");
      return pPath;
    }
    return targetPath;
  }

  private Path createPathFromPredicateValues(Path pPath, Map<Integer, Boolean> preds) {

    ARTElement errorElement = pPath.getLast().getFirst();
    Set<ARTElement> errorPathElements = ARTUtils.getAllElementsOnPathsTo(errorElement);

    Path result = new Path();
    ARTElement currentElement = pPath.getFirst().getFirst();
    while (!currentElement.isTarget()) {
      Set<ARTElement> children = currentElement.getChildren();

      ARTElement child;
      CFAEdge edge;
      switch (children.size()) {

      case 0:
        logger.log(Level.WARNING, "ART target path terminates without reaching target element!");
        return null;

      case 1: // only one successor, easy
        child = Iterables.getOnlyElement(children);
        edge = currentElement.getEdgeToChild(child);
        break;

      case 2: // branch
        // first, find out the edges and the children
        CFAEdge trueEdge = null;
        CFAEdge falseEdge = null;
        ARTElement trueChild = null;
        ARTElement falseChild = null;

        for (ARTElement currentChild : children) {
          CFAEdge currentEdge = currentElement.getEdgeToChild(currentChild);
          if (!(currentEdge instanceof AssumeEdge)) {
            logger.log(Level.WARNING, "ART branches where there is no AssumeEdge!");
            return null;
          }

          if (((AssumeEdge)currentEdge).getTruthAssumption()) {
            trueEdge = currentEdge;
            trueChild = currentChild;
          } else {
            falseEdge = currentEdge;
            falseChild = currentChild;
          }
        }
        if (trueEdge == null || falseEdge == null) {
          logger.log(Level.WARNING, "ART branches with non-complementary AssumeEdges!");
          return null;
        }
        assert trueChild != null;
        assert falseChild != null;

        // search first idx where we have a predicate for the current branching
        Boolean predValue = preds.get(currentElement.getElementId());
        if (predValue == null) {
          logger.log(Level.WARNING, "ART branches without direction information from solver!");
          return null;
        }

        // now select the right edge
        if (predValue) {
          edge = trueEdge;
          child = trueChild;
        } else {
          edge = falseEdge;
          child = falseChild;
        }
        break;

      default:
        logger.log(Level.WARNING, "ART splits with more than two branches!");
        return null;
      }

      if (!errorPathElements.contains(child)) {
        logger.log(Level.WARNING, "ART and direction information from solver disagree!");
        return null;
      }

      result.add(Pair.of(currentElement, edge));
      currentElement = child;
    }

    // need to add another pair with target element and outgoing edge
    Pair<ARTElement, CFAEdge> lastPair = pPath.getLast();
    if (currentElement != lastPair.getFirst()) {
      logger.log(Level.WARNING, "ART target path reached the wrong target element!");
      return null;
    }
    result.add(lastPair);

    return result;
  }

  public CounterexampleTraceInfo getCounterexampleTraceInfo() {
    return mCounterexampleTraceInfo;
  }

  // TODO: copy & paste code
  private boolean extractVariables(CFAEdge edge, CollectVariablesVisitor visitor)
  {
    boolean extracted = false;
    visitor.setCurrentScope(edge);

    switch(edge.getEdgeType())
    {
    case AssumeEdge:
//System.out.println("inspecting AssumeEdge " + ((AssumeEdge)edge).getRawStatement());

      visitor.startLookAhead();
      ((AssumeEdge)edge).getExpression().accept(visitor);
      extracted = visitor.endLookAhead();

      break;

    case FunctionCallEdge:
      FunctionCallEdge functionCallEdge = (FunctionCallEdge)edge;

//System.out.println("inspecting FunctionCallEdge " + functionCallEdge.getRawStatement());

      if(functionCallEdge.getRawAST() instanceof IASTFunctionCallAssignmentStatement)
      {
        IASTFunctionCallAssignmentStatement exp = ((IASTFunctionCallAssignmentStatement)functionCallEdge.getRawAST());

        if(visitor.hasCollected(exp.getLeftHandSide().getRawSignature()))
        {
//System.out.println("     -> interesting: collecting remaining variables");
          exp.getRightHandSide().accept(visitor);

          extracted = true;
        }
      }

      break;

    case StatementEdge:
      StatementEdge statementEdge = (StatementEdge)edge;

//System.out.println("inspecting StatementEdge " + statementEdge.getRawStatement());

      if(statementEdge.getStatement() instanceof IASTAssignment)
      {
        IASTAssignment assignment = (IASTAssignment)statementEdge.getStatement();

        String assignedVariable = assignment.getLeftHandSide().getRawSignature();

        if(visitor.hasCollected(assignedVariable))
        {
          assignment.getRightHandSide().accept(visitor);

          extracted = true;
        }
      }
      break;
    }

    return extracted;
  }

  /**
   * visitor that collects identifiers denoting variables, that show up in given IASTExpressions
   */
  private class CollectVariablesVisitor extends
      DefaultExpressionVisitor<Void, RuntimeException> implements
      RightHandSideVisitor<Void, RuntimeException> {

    /**
     * the current cfa edge
     */
    private CFAEdge edge = null;

    /**
     * the set of collected variables
     */
    private final Set<String> collectedVariables = new HashSet<String>();

    /**
     * the set of variables collected during look-ahead-run
     */
    private final Set<String> lookAheadVariables = new HashSet<String>();

    /**
     * flag that determines whether or not the visitor is in look-ahead-mode
     */
    private boolean inLookAheadMode = false;

    public CollectVariablesVisitor(Collection<String> initialVariables)
    {
      this.collectedVariables.addAll(initialVariables);
    }

    /**
     * This method adds a given variable into the appropriate collection
     *
     * @param cfaNode the current cfa node
     * @param variableName the name of the variable
     */
    private void collect(CFANode cfaNode, String variableName)
    {
      variableName = getScopedVariableName(cfaNode, variableName);

      if(inLookAheadMode)
        lookAheadVariables.add(variableName);
      else
        collectedVariables.add(variableName);
    }

    public boolean hasCollected(String variableName)
    {
      variableName = getScopedVariableName(edge.getPredecessor(), variableName);

      return collectedVariables.contains(variableName);
    }

    public Collection<String> getCollectedVariables()
    {
      return collectedVariables;
    }

    public void setCurrentScope(CFAEdge currentEdge)
    {
      edge = currentEdge;
    }

    /**
     * This method starts the look-ahead-mode, which is only necessary for inspecting assume edges
     */
    public void startLookAhead()
    {
      inLookAheadMode = true;

      lookAheadVariables.clear();
    }

    /**
     * This method ends the look-ahead-mode, which is only necessary for inspecting assume edges
     */
    public boolean endLookAhead()
    {
      boolean ofInterest = false;

      for(String lookAheadVariable : lookAheadVariables)
      {
        if(collectedVariables.contains(lookAheadVariable))
        {
          ofInterest = true;
          break;
        }
      }

      if(ofInterest)
        collectedVariables.addAll(lookAheadVariables);

      inLookAheadMode = false;

      return ofInterest;
    }

    private String getScopedVariableName(CFANode cfaNode, String variableName)
    {
      if(globalVars.contains(variableName))
         return variableName;

      else
        return cfaNode.getFunctionName() + "::" + variableName;
    }

    @Override
    public Void visit(IASTIdExpression idExpression)
    {
      collect(edge.getPredecessor(), idExpression.getName());

      return null;
    }

    @Override
    public Void visit(IASTArraySubscriptExpression arraySubscriptExpression)
    {
      arraySubscriptExpression.getArrayExpression().accept(this);
      arraySubscriptExpression.getSubscriptExpression().accept(this);

      return null;
    }

    @Override
    public Void visit(IASTBinaryExpression binaryExpression)
    {
      binaryExpression.getOperand1().accept(this);
      binaryExpression.getOperand2().accept(this);

      return null;
    }

    @Override
    public Void visit(IASTCastExpression castExpression)
    {
      castExpression.getOperand().accept(this);

      return null;
    }

    @Override
    public Void visit(IASTFieldReference fieldReference)
    {
      fieldReference.getFieldOwner().accept(this);

      return null;
    }

    @Override
    public Void visit(IASTFunctionCallExpression functionCallExpression)
    {
      // do not visit the function name identifier but only the actual parameter expressions
      for(IASTExpression param : functionCallExpression.getParameterExpressions())
        param.accept(this);

      // also, add the formal parameters
      FunctionDefinitionNode functionEntryNode = ((FunctionCallEdge)edge).getSuccessor();
      for(String formalParameter : functionEntryNode.getFunctionParameterNames())
        collect(functionEntryNode, formalParameter);

      return null;
    }

    @Override
    public Void visit(IASTUnaryExpression unaryExpression)
    {
      unaryExpression.getOperand().accept(this);

      return null;
    }

    @Override
    protected Void visitDefault(IASTExpression expression)
    {
      return null;
    }
  }
}
