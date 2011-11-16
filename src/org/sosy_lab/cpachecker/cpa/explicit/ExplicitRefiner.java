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

import static com.google.common.collect.Iterables.skip;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
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
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionTypeSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IASTUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.RightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.StatementEdge;
import org.sosy_lab.cpachecker.core.CPAchecker;
import org.sosy_lab.cpachecker.core.algorithm.cbmctools.AbstractPathToCTranslator;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.art.ARTReachedSet;
import org.sosy_lab.cpachecker.cpa.art.ARTUtils;
import org.sosy_lab.cpachecker.cpa.art.Path;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateRefinementManager;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateRefiner;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException.Reason;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interpolation.AbstractInterpolationBasedRefiner;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

@Options(prefix="cpa.predicate")
public class ExplicitRefiner extends AbstractInterpolationBasedRefiner<Collection<AbstractionPredicate>, Pair<ARTElement, CFAEdge>> {

  final Timer precisionUpdate = new Timer();
  final Timer artUpdate = new Timer();

  Pair<ARTElement, CFAEdge> firstInterpolationPoint = null;

  private Set<String> allReferencedVaraibles = new HashSet<String>();

  private Set<String> globalVars = null;

  private final FormulaManager fmgr;

  public static ExplicitRefiner create(ConfigurableProgramAnalysis pCpa) throws CPAException, InvalidConfigurationException {
    if (!(pCpa instanceof WrapperCPA)) {
      throw new InvalidConfigurationException(PredicateRefiner.class.getSimpleName() + " could not find the ExplicitCPA");
    }

    ExplicitCPA explicitCpa = ((WrapperCPA)pCpa).retrieveWrappedCpa(ExplicitCPA.class);
    if (explicitCpa == null) {
      throw new InvalidConfigurationException(PredicateRefiner.class.getSimpleName() + " needs a ExplicitCPA");
    }

    LogManager logger = explicitCpa.getLogger();

    PredicateRefinementManager manager = new PredicateRefinementManager(explicitCpa.getFormulaManager(),
        explicitCpa.getPathFormulaManager(),
        explicitCpa.getTheoremProver(),
        explicitCpa.getPredicateManager(),
        explicitCpa.getConfiguration(),
        logger);

    ExplicitRefiner refiner = new ExplicitRefiner(explicitCpa.getConfiguration(), logger, pCpa, explicitCpa.getFormulaManager(), manager);

    return refiner;
  }

  protected ExplicitRefiner(final Configuration config, final LogManager logger,
      final ConfigurableProgramAnalysis pCpa, FormulaManager pFmgr,
      final PredicateRefinementManager pInterpolationManager) throws CPAException, InvalidConfigurationException {

    super(config, logger, pCpa, pInterpolationManager);

    config.inject(this, ExplicitRefiner.class);

    fmgr = pFmgr;

    // TODO: runner-up award for ugliest hack of the month ...
    globalVars = ExplicitTransferRelation.globalVarsStatic;
  }

  public ARTReachedSet currentReached = null;
  @Override
  protected void performRefinement(ARTReachedSet pReached, List<Pair<ARTElement, CFAEdge>> pPath,
      CounterexampleTraceInfo<Collection<AbstractionPredicate>> pCounterexample,
      boolean pRepeatedCounterexample) throws CPAException {
    currentReached = pReached;
    precisionUpdate.start();

    // get previous precision
    UnmodifiableReachedSet reached = pReached.asReachedSet();
    Precision oldPrecision = reached.getPrecision(reached.getLastElement());
    ExplicitPrecision oldPredicatePrecision = Precisions.extractPrecisionByType(oldPrecision, ExplicitPrecision.class);
    if (oldPredicatePrecision == null) {
      throw new IllegalStateException("Could not find the PredicatePrecision for the error element");
    }

    Pair<ARTElement, ExplicitPrecision> refinementResult =
            performRefinement(oldPredicatePrecision, pPath, pCounterexample);
    precisionUpdate.stop();

    artUpdate.start();

    pReached.removeSubtree(refinementResult.getFirst(), refinementResult.getSecond());

    artUpdate.stop();
  }

  @Override
  protected final List<Pair<ARTElement, CFAEdge>> transformPath(Path pPath)
  {
    List<Pair<ARTElement, CFAEdge>> result = Lists.newArrayList();

    for(Pair<ARTElement, CFAEdge> element : skip(pPath, 1))
      result.add(Pair.of(element.getFirst(), element.getSecond()));

    return result;
  }

  @Override
  protected List<Formula> getFormulasForPath(List<Pair<ARTElement, CFAEdge>> path, ARTElement initialElement) throws CPATransferException {

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

    // iterate over edges (not nodes)
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
  Set<Integer> pathHashes = new HashSet<Integer>();
  private Pair<ARTElement, ExplicitPrecision> performRefinement(ExplicitPrecision oldPrecision,
      List<Pair<ARTElement, CFAEdge>> pPath,
      CounterexampleTraceInfo<Collection<AbstractionPredicate>> pInfo) throws CPAException {



    PredicateMap predicates = new PredicateMap(pInfo.getPredicatesForRefinement(), pPath);

    Map<CFANode, Set<String>> variablesFromPredicates = predicates.getVariablesFromPredicates(fmgr);

    allReferencedVaraibles.addAll(predicates.getReferencedVariables());

    Map<CFANode, Set<String>> relevantVariablesOnPath = getRelevantVariablesOnPath(pPath, predicates);
    assert firstInterpolationPoint != null;

System.out.println("\n" + (++refinementCounter) + ". refining ...");
//System.out.println(getErrorPathAsString(pPath));
System.out.println("\nreferencedVariables: " + predicates.getReferencedVariables());
System.out.println("\nallReferencedVaraibles: " + allReferencedVaraibles);
System.out.println("\nnew predicate map: " + predicates.toString());
System.out.println("\nrelevantVariablesOnPath: " + relevantVariablesOnPath);
//System.out.println("\nfirstInterpolationPoint = " + firstInterpolationPoint + "\n");

    // create the new precision
    ExplicitPrecision newPrecision = new ExplicitPrecision(oldPrecision, variablesFromPredicates, relevantVariablesOnPath);

    ARTElement root = firstInterpolationPoint.getFirst();

    logger.log(Level.FINEST, "Found spurious counterexample,",
          "trying strategy 1: remove everything below", root, "from ART.");

    if(!pathHashes.add(getErrorPathAsString(pPath).hashCode()))
    {
      System.out.println(AbstractPathToCTranslator.translatePaths(CPAchecker.staticCFA, (ARTElement)currentReached.mReached.getFirstElement(), ARTUtils.getAllElementsOnPathsTo((ARTElement)currentReached.mReached.getLastElement())));

      System.out.println(getErrorPathAsString(pPath));

      System.out.println("\nlast set of variables in predicates: " + predicates.getReferencedVariables());

      throw new RefinementFailedException(Reason.RepeatedCounterexample, null);
    }

    return Pair.of(root, newPrecision);
  }

  private String getErrorPathAsString(List<Pair<ARTElement, CFAEdge>> errorPath)
  {
    StringBuilder sb = new StringBuilder();

    Function<Pair<?, ? extends CFAEdge>, CFAEdge> projectionToSecond = Pair.getProjectionToSecond();

    int index = 0;
    for (CFAEdge edge : Lists.transform(errorPath, projectionToSecond)) {
      sb.append(index + ": Line ");
      sb.append(edge.getLineNumber());
      sb.append(": ");
      sb.append(edge);
      sb.append("\n");

      index++;
    }

    return sb.toString();
  }

  /**
   * This method collects the variables on the error path, on which the variables referenced by predicates depend on. Furthermore, the top-most interpolation point is identified and set.
   *
   * @param errorPath the path to the found error location
   * @param predicates the predicates from the refinement
   * @return the variables on the error path, on which the variables referenced by predicates depend on
   */
  private Map<CFANode, Set<String>> getRelevantVariablesOnPath(List<Pair<ARTElement, CFAEdge>> errorPath, PredicateMap predicates)
  {
    CollectVariablesVisitor visitor = new CollectVariablesVisitor(allReferencedVaraibles);

    for(int i = errorPath.size() - 1; i >= 0; --i)
    {
      Pair<ARTElement, CFAEdge> element = errorPath.get(i);

      CFAEdge edge = element.getSecond();

      if(extractVariables(edge, visitor) || predicates.isInterpolationPoint(edge.getPredecessor()))
        firstInterpolationPoint = element;
    }

    return visitor.getVariablesAtLocations();
  }

  private boolean extractVariables(CFAEdge edge, CollectVariablesVisitor visitor)
  {
    boolean extracted = false;
    visitor.setCurrentScope(edge);

    switch(edge.getEdgeType())
    {
    case FunctionCallEdge:
      FunctionCallEdge functionCallEdge = (FunctionCallEdge)edge;

//System.out.println("inspecting FunctionCallEdge " + functionCallEdge.getRawStatement());

      if(functionCallEdge.getRawAST() instanceof IASTFunctionCallAssignmentStatement)
      {
        IASTFunctionCallAssignmentStatement fcas = ((IASTFunctionCallAssignmentStatement)functionCallEdge.getRawAST());

        String assignedVariable = fcas.getLeftHandSide().getRawSignature();

        if(visitor.hasCollected(assignedVariable, false))
        {
          fcas.getLeftHandSide().accept(visitor);

          fcas.getRightHandSide().accept(visitor);

          extracted = true;
        }

        FunctionDefinitionNode def = functionCallEdge.getSuccessor();
        String functionName = def.getFunctionName();

        int parameterCount = 0;
        for(IASTParameterDeclaration parameter : def.getFunctionDefinition().getDeclSpecifier().getParameters())
        {
          if(visitor.hasCollected(functionName + "::" + parameter.getName(), true))
          {
            visitor.addVariableToLocation2(functionCallEdge.getArguments().get(parameterCount).getRawSignature());
          }

          parameterCount++;
        }
      }

      break;

    case StatementEdge:
      StatementEdge statementEdge = (StatementEdge)edge;

      if(statementEdge.getStatement() instanceof IASTAssignment)
      {
        IASTAssignment assignment = (IASTAssignment)statementEdge.getStatement();
        String assignedVariable = assignment.getLeftHandSide().getRawSignature();

        // left-hand side was already collected -> collect identifiers from right-hand side as well
        if(visitor.hasCollected(assignedVariable, false))
        {
          // apply visitor to left side, as assigned variable has to be tracked here
          assignment.getLeftHandSide().accept(visitor);

          // apply visitor to right side, as the assigning of these variables must be handled as well (further up the path)
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
     * the mapping which locations reference which variables
     */
    private final Map<CFANode, Set<String>> variablesAtLocations = new HashMap<CFANode, Set<String>>();

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
      {
        collectedVariables.add(variableName);

        addVariableToLocation(variableName);
      }
    }

    public boolean hasCollected(String variableName, boolean isAlreadyScoped)
    {
      if(!isAlreadyScoped)
        variableName = getScopedVariableName(edge.getPredecessor(), variableName);

      return collectedVariables.contains(variableName);
    }

    public Map<CFANode, Set<String>> getVariablesAtLocations()
    {
      return variablesAtLocations;
    }

    public void setCurrentScope(CFAEdge currentEdge)
    {
      edge = currentEdge;
    }

    private String getScopedVariableName(CFANode cfaNode, String variableName)
    {
      if(globalVars.contains(variableName))
         return variableName;

      else
        return cfaNode.getFunctionName() + "::" + variableName;
    }

    public void addVariableToLocation2(String variable)
    {
      collect(edge.getPredecessor(), variable);
    }

    private void addVariableToLocation(String variable)
    {
      Set<String> variablesAtLocation = variablesAtLocations.get(edge.getSuccessor());

      if(variablesAtLocation == null)
        variablesAtLocations.put(edge.getSuccessor(), variablesAtLocation = new HashSet<String>());

      variablesAtLocation.add(variable);
      //System.out.println("adding " + variable + " at successor of " + edge);
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
      //fieldReference.getFieldOwner().accept(this);

      collect(edge.getPredecessor(), fieldReference.getRawSignature());

      return null;
    }

    @Override
    public Void visit(IASTFunctionCallExpression functionCallExpression)
    {
      // do not visit the function name identifier but only the actual parameter expressions
      for(IASTExpression param : functionCallExpression.getParameterExpressions())
        param.accept(this);

      // also, add the formal parameters
      // TODO: strange behaviour -> in a few cases, the edge is a statement edge here, so this would fail !?!?!
      if(edge instanceof FunctionCallEdge)
      {
        FunctionDefinitionNode functionEntryNode = ((FunctionCallEdge)edge).getSuccessor();

        for(String formalParameter : functionEntryNode.getFunctionParameterNames())
          collect(functionEntryNode, formalParameter);
      }

      // TODO: work-around for the above, however, this does not work in all cases either, as getDeclaration returns null sometimes !?!?!
      else
      {
        if(functionCallExpression.getDeclaration() == null)
            return null;
        else if (((IASTFunctionTypeSpecifier)functionCallExpression.getDeclaration().getDeclSpecifier()) == null)
          return null;

        List<IASTParameterDeclaration> parameters = ((IASTFunctionTypeSpecifier)functionCallExpression.getDeclaration().getDeclSpecifier()).getParameters();

        for(IASTParameterDeclaration parameter : parameters)
          collect(edge.getSuccessor(), parameter.getName());
      }

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
