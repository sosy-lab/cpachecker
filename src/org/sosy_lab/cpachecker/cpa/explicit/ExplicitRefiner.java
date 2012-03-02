/*
path *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
import static com.google.common.collect.Lists.transform;
import static org.sosy_lab.cpachecker.util.AbstractElements.extractElementByType;

import java.util.ArrayList;
import java.util.Collection;
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
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.RightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.StatementEdge;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.art.ARTReachedSet;
import org.sosy_lab.cpachecker.cpa.art.Path;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractElement;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionManager;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateRefinementManager;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException.Reason;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.ExtendedFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.bdd.BDDRegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.TheoremProver;
import org.sosy_lab.cpachecker.util.predicates.interpolation.AbstractInterpolationBasedRefiner;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;

@Options(prefix="cpa.predicate")
public class ExplicitRefiner extends AbstractInterpolationBasedRefiner<Collection<AbstractionPredicate>, Pair<ARTElement, CFANode>> {

  final Timer precisionUpdate = new Timer();
  final Timer artUpdate = new Timer();

  private Pair<ARTElement, CFANode> firstInterpolationPoint = null;

  private Set<String> allReferencedVariables                = new HashSet<String>();

  private Set<String> globalVars                            = null;

  private final ExtendedFormulaManager fmgr;
  private final PathFormulaManager pathFormulaManager;

  private boolean predicateCpaAvailable         = false;

  private Set<Integer> pathHashes               = new HashSet<Integer>();

  private Integer previousPathHash              = null;

  private boolean refinePredicatePrecision      = false;

  private List<Pair<ARTElement, CFAEdge>> path  = null;

  public static ExplicitRefiner create(ConfigurableProgramAnalysis pCpa) throws CPAException, InvalidConfigurationException {
    if (!(pCpa instanceof WrapperCPA)) {
      throw new InvalidConfigurationException(ExplicitRefiner.class.getSimpleName() + " could not find the ExplicitCPA");
    }

    ExplicitCPA explicitCpa = ((WrapperCPA)pCpa).retrieveWrappedCpa(ExplicitCPA.class);
    if (explicitCpa == null) {
      throw new InvalidConfigurationException(ExplicitRefiner.class.getSimpleName() + " needs a ExplicitCPA");
    }

    ExplicitRefiner refiner = initialiseExplicitRefiner(pCpa, explicitCpa.getConfiguration(), explicitCpa.getLogger());

    return refiner;
  }

  private static ExplicitRefiner initialiseExplicitRefiner(ConfigurableProgramAnalysis pCpa, Configuration config, LogManager logger) throws CPAException, InvalidConfigurationException {
    FormulaManagerFactory factory               = null;
    ExtendedFormulaManager formulaManager       = null;
    PathFormulaManager pathFormulaManager       = null;
    TheoremProver theoremProver                 = null;
    PredicateAbstractionManager absManager      = null;
    PredicateRefinementManager manager          = null;

    PredicateCPA predicateCpa = ((WrapperCPA)pCpa).retrieveWrappedCpa(PredicateCPA.class);

    boolean predicateCpaInUse = predicateCpa != null;
    if (predicateCpaInUse) {
      factory               = predicateCpa.getFormulaManagerFactory();
      formulaManager        = predicateCpa.getFormulaManager();
      pathFormulaManager    = predicateCpa.getPathFormulaManager();
      theoremProver         = predicateCpa.getTheoremProver();
      absManager            = predicateCpa.getPredicateManager();
    } else {
      factory               = new FormulaManagerFactory(config, logger);
      RegionManager regionManager                 = BDDRegionManager.getInstance();
      formulaManager        = new ExtendedFormulaManager(factory.getFormulaManager(), config, logger);
      pathFormulaManager    = new PathFormulaManagerImpl(formulaManager, config, logger);
      theoremProver         = factory.createTheoremProver();
      absManager            = new PredicateAbstractionManager(regionManager, formulaManager, theoremProver, config, logger);
    }

    manager = new PredicateRefinementManager(formulaManager,
        pathFormulaManager, theoremProver, absManager, factory, config, logger);

    return new ExplicitRefiner(config, logger, pCpa, formulaManager, pathFormulaManager, manager, predicateCpaInUse);
  }

  protected ExplicitRefiner(final Configuration config, final LogManager logger,
      final ConfigurableProgramAnalysis pCpa, final ExtendedFormulaManager pFmgr,
      final PathFormulaManager pPathFormulaManager,
      final PredicateRefinementManager pInterpolationManager,
      final boolean predicateCpaInUse) throws CPAException, InvalidConfigurationException {

    super(config, logger, pCpa, pInterpolationManager);

    config.inject(this, ExplicitRefiner.class);

    this.fmgr                = pFmgr;
    this.pathFormulaManager  = pPathFormulaManager;
    this.predicateCpaAvailable   = predicateCpaInUse;

    // TODO: runner-up award for ugliest hack of the month ...
    globalVars = ExplicitTransferRelation.globalVarsStatic;
  }

  @Override
  protected void performRefinement(ARTReachedSet pReached, List<Pair<ARTElement, CFANode>> errorPath,
      CounterexampleTraceInfo<Collection<AbstractionPredicate>> counterexampleTraceInfo,
      boolean pRepeatedCounterexample) throws CPAException {

    Set<ARTElement> elems = new HashSet<ARTElement>();

    int i = 0;
    for(Pair<ARTElement, CFANode> el : errorPath){
      System.out.println(el.getSecond());
      elems.add(el.getFirst());
    }

    try {
      System.out.println("checking ...");
      boolean feasible = ExplicitCPA.checker.checkCounterexample((ARTElement)pReached.asReachedSet().getFirstElement(),
          (ARTElement)pReached.asReachedSet().getLastElement(), elems);

      System.out.println("precise counter examlple check says: " + (feasible ? "" : "in") + "feasible");
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    precisionUpdate.start();

    // check if there was progress
    if (!hasMadeProgress()) {
      throw new RefinementFailedException(Reason.RepeatedCounterexample, null);
    }

    // get previous precision
    UnmodifiableReachedSet reached = pReached.asReachedSet();
    Precision oldPrecision = reached.getPrecision(reached.getLastElement());

    Pair<ARTElement, Precision> refinementResult =
            performRefinement(oldPrecision, errorPath, counterexampleTraceInfo);
    precisionUpdate.stop();

    artUpdate.start();

    ARTElement root = refinementResult.getFirst();

    logger.log(Level.FINEST, "Found spurious counterexample,",
        "trying strategy 1: remove everything below", root, "from ART.");

    pReached.removeSubtree(root, refinementResult.getSecond());

    artUpdate.stop();
  }

  @Override
  protected final List<Pair<ARTElement, CFANode>> transformPath(Path errorPath) {
    path = errorPath;

    // determine whether to refine explicit or predicate precision
    refinePredicatePrecision = determineRefinementStrategy();

    List<Pair<ARTElement, CFANode>> result = Lists.newArrayList();

    for (ARTElement ae : skip(transform(errorPath, Pair.<ARTElement>getProjectionToFirst()), 1)) {
      if(refinePredicatePrecision) {
        PredicateAbstractElement pe = extractElementByType(ae, PredicateAbstractElement.class);
        if (pe.isAbstractionElement()) {
          CFANode location = AbstractElements.extractLocation(ae);
          result.add(Pair.of(ae, location));
        }
      }
      else {
        result.add(Pair.of(ae, AbstractElements.extractLocation(ae)));
      }
    }

    assert errorPath.getLast().getFirst() == result.get(result.size()-1).getFirst();
    return result;
  }


  private static final Function<PredicateAbstractElement, Formula> GET_BLOCK_FORMULA
                = new Function<PredicateAbstractElement, Formula>() {
                    @Override
                    public Formula apply(PredicateAbstractElement e) {
                      assert e.isAbstractionElement();
                      return e.getAbstractionFormula().getBlockFormula();
                    };
                  };

  @Override
  protected List<Formula> getFormulasForPath(List<Pair<ARTElement, CFANode>> errorPath, ARTElement initialElement) throws CPATransferException {

    if(refinePredicatePrecision) {
      List<Formula> formulas = transform(errorPath,
          Functions.compose(
              GET_BLOCK_FORMULA,
          Functions.compose(
              AbstractElements.extractElementByTypeFunction(PredicateAbstractElement.class),
              Pair.<ARTElement>getProjectionToFirst())));

      return formulas;
    }

    else {
      PathFormula currentPathFormula = pathFormulaManager.makeEmptyPathFormula();

      List<Formula> formulas = new ArrayList<Formula>(path.size());

      // iterate over edges (not nodes)
      int i = 0;
      for (Pair<ARTElement, CFAEdge> pathElement : path) {
        i++;

        if(i == 1)
          continue;
        currentPathFormula = pathFormulaManager.makeAnd(currentPathFormula, pathElement.getSecond());

        formulas.add(currentPathFormula.getFormula());

        // reset the formula
        currentPathFormula = pathFormulaManager.makeEmptyPathFormula(currentPathFormula);
      }

      return formulas;
    }
  }

  private Pair<ARTElement, Precision> performRefinement(Precision oldPrecision,
      List<Pair<ARTElement, CFANode>> errorPath,
      CounterexampleTraceInfo<Collection<AbstractionPredicate>> pInfo) throws CPAException {

    // create the mapping of CFA nodes to predicates, based on the counter example trace info
    PredicateMap predicates = new PredicateMap(pInfo.getPredicatesForRefinement(), errorPath);

    // get the mapping of CFA nodes to variable names
    Multimap<CFANode, String> variableMapping = predicates.getVariableMapping(fmgr);

    Precision precision = null;

    if(refinePredicatePrecision) {
      precision = createPredicatePrecision(extractPredicatePrecision(oldPrecision),
                                          predicates);

      firstInterpolationPoint = predicates.firstInterpolationPoint;
//System.out.println("refined PredicatePrecision");
//System.out.println(precision);
    } else {
      allReferencedVariables.addAll(variableMapping.values());

      // expand the mapping of CFA nodes to variable names, with a def-use analysis along that path
      Multimap<CFANode, String> relevantVariablesOnPath = getRelevantVariablesOnPath(errorPath, predicates);

      assert firstInterpolationPoint != null;

      // create the new precision
      precision = createExplicitPrecision(extractExplicitPrecision(oldPrecision),
                                            variableMapping,
                                            relevantVariablesOnPath);

//System.out.println("refined ExplicitPrecision");
    }

    return Pair.of(firstInterpolationPoint.getFirst(), precision);
  }

  /**
   * This method extracts the explicit precision.
   *
   * @param precision the current precision
   * @return the explicit precision
   */
  private ExplicitPrecision extractExplicitPrecision(Precision precision) {
    ExplicitPrecision explicitPrecision = Precisions.extractPrecisionByType(precision, ExplicitPrecision.class);
    if(explicitPrecision == null) {
      throw new IllegalStateException("Could not find the ExplicitPrecision for the error element");
    }
    return explicitPrecision;
  }

  private ExplicitPrecision createExplicitPrecision(ExplicitPrecision oldPrecision,
      Multimap<CFANode, String> variableMapping,
      Multimap<CFANode, String> relevantVariablesOnPath) {

    ExplicitPrecision explicitPrecision             = new ExplicitPrecision(oldPrecision);
    SetMultimap<CFANode, String> additionalMapping  = HashMultimap.create(variableMapping);

    additionalMapping.putAll(relevantVariablesOnPath);

    explicitPrecision.getCegarPrecision().addToMapping(additionalMapping);

    return explicitPrecision;
  }

  /**
   * This method extracts the predicate precision.
   *
   * @param precision the current precision
   * @return the predicate precision, or null, if the PredicateCPA is not in use
   */
  private PredicatePrecision extractPredicatePrecision(Precision precision) {
    PredicatePrecision predicatePrecision = Precisions.extractPrecisionByType(precision, PredicatePrecision.class);
    if(predicatePrecision == null) {
      throw new IllegalStateException("Could not find the PredicatePrecision for the error element");
    }
    return predicatePrecision;
  }

  private PredicatePrecision createPredicatePrecision(PredicatePrecision oldPredicatePrecision,
                                                    PredicateMap predicateMap) {
    Multimap<CFANode, AbstractionPredicate> oldPredicateMap = oldPredicatePrecision.getPredicateMap();
    Set<AbstractionPredicate> globalPredicates = oldPredicatePrecision.getGlobalPredicates();

    ImmutableSetMultimap.Builder<CFANode, AbstractionPredicate> pmapBuilder = ImmutableSetMultimap.builder();
    pmapBuilder.putAll(oldPredicateMap);

    for(Map.Entry<CFANode, AbstractionPredicate> predicateAtLocation : predicateMap.getPredicateMapping().entries()) {
      pmapBuilder.putAll(predicateAtLocation.getKey(), predicateAtLocation.getValue());
    }

    return new PredicatePrecision(pmapBuilder.build(), globalPredicates);
  }

  private boolean determineRefinementStrategy() {
    return predicateCpaAvailable && pathHashes.contains(getErrorPathAsString(path).hashCode());
  }

  private boolean hasMadeProgress() {
    Integer errorTraceHash = getErrorPathAsString(path).hashCode();
//System.out.println("errorTraceHash = " + errorTraceHash);
    // in case a PredicateCPA is running, too
    // stop if the same error trace is found within two iterations
    if(predicateCpaAvailable && errorTraceHash.equals(previousPathHash)) {
      return false;
    }
    // in case only a ExplicitCPA is running:
    // stop if the same error trace is found twice
    else if(!predicateCpaAvailable && pathHashes.contains(errorTraceHash)) {
      return false;
    }

    previousPathHash = refinePredicatePrecision ? errorTraceHash : null;

    pathHashes.add(errorTraceHash);

    return true;
  }

  /**
   * This method collects the variables on the error path, on which the variables referenced by predicates depend on. Furthermore, the top-most interpolation point is identified and set.
   *
   * This step is necessary for handling programs like this:
   * <code>
   *  x = 1; // <- this location will not have any associated predicates
   *  y = x;
   *  z = x;
   *  if(y != z)
   *    goto ERROR;
   * </code>
   *
   * Something similar might be needed for programs, like this, where x is a global variable. This is not handled yet.
   * <code>
   *  x = 1;
   *  y = getX();
   *  z = getX();
   *  if(y != z)
   *    goto ERROR;
   * </code>
   * @param errorPath the path to the found error location
   * @param predicates the predicates from the refinement
   * @return the variables on the error path, on which the variables referenced by predicates depend on
   */
  private Multimap<CFANode, String> getRelevantVariablesOnPath(List<Pair<ARTElement, CFANode>> errorPath, PredicateMap predicates) {
    CollectVariablesVisitor visitor = new CollectVariablesVisitor(allReferencedVariables);

    for (int i = errorPath.size() - 1; i >= 0; --i) {
      Pair<ARTElement, CFANode> element = errorPath.get(i);

      CFAEdge edge = element.getSecond().getEnteringEdge(0);

      if (extractVariables(edge, visitor) || predicates.isInterpolationPoint(edge.getSuccessor())) {
        firstInterpolationPoint = element;
      }
    }

    return visitor.getVariablesAtLocations();
  }

  private boolean extractVariables(CFAEdge edge, CollectVariablesVisitor visitor) {
    boolean extracted = false;
    visitor.setCurrentScope(edge);

    switch (edge.getEdgeType()) {
    case StatementEdge:
      StatementEdge statementEdge = (StatementEdge)edge;

      if (statementEdge.getStatement() instanceof IASTAssignment) {
        IASTAssignment assignment = (IASTAssignment)statementEdge.getStatement();
        String assignedVariable = assignment.getLeftHandSide().toASTString();

        // left-hand side was already collected -> collect identifiers from right-hand side as well
        if (visitor.hasCollected(assignedVariable, false)) {
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
    private final Multimap<CFANode, String> variablesAtLocations = HashMultimap.create();

    public CollectVariablesVisitor(Collection<String> initialVariables) {
      this.collectedVariables.addAll(initialVariables);
    }

    /**
     * This method adds a given variable into the appropriate collection
     *
     * @param cfaNode the current cfa node
     * @param variableName the name of the variable
     */
    private void collect(CFANode cfaNode, String variableName) {
      variableName = getScopedVariableName(cfaNode, variableName);

      collectedVariables.add(variableName);

      addVariableToLocation(variableName);
    }

    public boolean hasCollected(String variableName, boolean isAlreadyScoped) {
      if(!isAlreadyScoped) {
        variableName = getScopedVariableName(edge.getPredecessor(), variableName);
      }

      return collectedVariables.contains(variableName);
    }

    public ImmutableMultimap<CFANode, String> getVariablesAtLocations() {
      return new ImmutableMultimap.Builder<CFANode, String>().putAll(variablesAtLocations).build();
    }

    public void setCurrentScope(CFAEdge currentEdge) {
      edge = currentEdge;
    }

    private String getScopedVariableName(CFANode cfaNode, String variableName) {
      if(globalVars.contains(variableName))
         return variableName;

      else
        return cfaNode.getFunctionName() + "::" + variableName;
    }

    private void addVariableToLocation(String variable) {
      variablesAtLocations.put(edge.getSuccessor(), variable);
    }

    @Override
    public Void visit(IASTIdExpression idExpression) {
      collect(edge.getPredecessor(), idExpression.getName());

      return null;
    }

    @Override
    public Void visit(IASTArraySubscriptExpression arraySubscriptExpression) {
      arraySubscriptExpression.getArrayExpression().accept(this);
      arraySubscriptExpression.getSubscriptExpression().accept(this);

      return null;
    }

    @Override
    public Void visit(IASTBinaryExpression binaryExpression) {
      binaryExpression.getOperand1().accept(this);
      binaryExpression.getOperand2().accept(this);

      return null;
    }

    @Override
    public Void visit(IASTCastExpression castExpression) {
      castExpression.getOperand().accept(this);

      return null;
    }

    @Override
    public Void visit(IASTFieldReference fieldReference) {
      fieldReference.getFieldOwner().accept(this);

      return null;
    }

    @Override
    public Void visit(IASTFunctionCallExpression functionCallExpression) {
      // visit the actual parameter expressions
      for (IASTExpression param : functionCallExpression.getParameterExpressions()) {
        param.accept(this);
      }

      return null;
    }

    @Override
    public Void visit(IASTUnaryExpression unaryExpression) {
      unaryExpression.getOperand().accept(this);

      return null;
    }

    @Override
    protected Void visitDefault(IASTExpression expression) {
      return null;
    }
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
}
