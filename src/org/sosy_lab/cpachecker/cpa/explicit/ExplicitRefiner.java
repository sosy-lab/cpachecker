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
import org.sosy_lab.cpachecker.cfa.ast.IASTBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.IASTCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionTypeSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTParameterDeclaration;
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
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.art.ARTReachedSet;
import org.sosy_lab.cpachecker.cpa.art.Path;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionManager;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateRefinementManager;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException.Reason;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.ExtendedFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.bdd.BDDRegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.TheoremProver;
import org.sosy_lab.cpachecker.util.predicates.interpolation.AbstractInterpolationBasedRefiner;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.mathsat.MathsatFactory;
import org.sosy_lab.cpachecker.util.predicates.mathsat.MathsatFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.mathsat.MathsatTheoremProver;

import com.google.common.base.Function;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

@Options(prefix="cpa.predicate")
public class ExplicitRefiner extends AbstractInterpolationBasedRefiner<Collection<AbstractionPredicate>, Pair<ARTElement, CFAEdge>> {

  final Timer precisionUpdate = new Timer();
  final Timer artUpdate = new Timer();

  Pair<ARTElement, CFAEdge> firstInterpolationPoint = null;

  private Set<String> allReferencedVaraibles = new HashSet<String>();

  private Set<String> globalVars = null;

  private final ExtendedFormulaManager fmgr;
  private final PathFormulaManager pathFormulaManager;

  PredicatePrecision predicatePrecision = null;

  private boolean predicateCpaInUse = false;

  String previousPath = null;

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
    MathsatFormulaManager mathsatFormulaManager = null;
    RegionManager regionManager                 = null;
    ExtendedFormulaManager formulaManager       = null;
    PathFormulaManager pathFormulaManager       = null;
    TheoremProver theoremProver                 = null;
    PredicateAbstractionManager absManager      = null;
    PredicateRefinementManager manager          = null;

    PredicateCPA predicateCpa = ((WrapperCPA)pCpa).retrieveWrappedCpa(PredicateCPA.class);

    boolean predicateCpaInUse = predicateCpa != null;
    if (predicateCpaInUse) {
      regionManager         = predicateCpa.getRegionManager();
      formulaManager        = predicateCpa.getFormulaManager();
      pathFormulaManager    = predicateCpa.getPathFormulaManager();
      theoremProver         = predicateCpa.getTheoremProver();
      absManager            = predicateCpa.getPredicateManager();
    } else {
      mathsatFormulaManager = MathsatFactory.createFormulaManager(config, logger);
      regionManager         = BDDRegionManager.getInstance();
      formulaManager        = new ExtendedFormulaManager(mathsatFormulaManager, config, logger);
      pathFormulaManager    = new PathFormulaManagerImpl(formulaManager, config, logger);
      theoremProver         = new MathsatTheoremProver(mathsatFormulaManager);
      absManager            = new PredicateAbstractionManager(regionManager, formulaManager, theoremProver, config, logger);
    }

    manager = new PredicateRefinementManager(formulaManager,
        pathFormulaManager, theoremProver, absManager, config, logger);

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
    this.predicateCpaInUse   = predicateCpaInUse;

    // TODO: runner-up award for ugliest hack of the month ...
    globalVars = ExplicitTransferRelation.globalVarsStatic;
  }

  @Override
  protected void performRefinement(ARTReachedSet pReached, List<Pair<ARTElement, CFAEdge>> pPath,
      CounterexampleTraceInfo<Collection<AbstractionPredicate>> pCounterexample,
      boolean pRepeatedCounterexample) throws CPAException {

    precisionUpdate.start();

    // get previous precision
    UnmodifiableReachedSet reached = pReached.asReachedSet();
    Precision oldPrecision = reached.getPrecision(reached.getLastElement());

    Pair<ARTElement, ExplicitPrecision> refinementResult =
            performRefinement(oldPrecision, pPath, pCounterexample);
    precisionUpdate.stop();

    artUpdate.start();

    ARTElement root = refinementResult.getFirst();

    if (predicateCpaInUse && predicatePrecision != null) {
      pReached.removeSubtree(root, refinementResult.getSecond(), predicatePrecision);
    } else {
      pReached.removeSubtree(root, refinementResult.getSecond());
    }

    predicatePrecision = null;
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
      converter = new CtoFormulaConverter(explicitCPA.getConfiguration(), fmgr, logger);
    }
    catch(InvalidConfigurationException e)
    {
      //System.out.println("error when configuring CtoFormulaConverter");
    }

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
  private Pair<ARTElement, ExplicitPrecision> performRefinement(Precision oldPrecision,
      List<Pair<ARTElement, CFAEdge>> pPath,
      CounterexampleTraceInfo<Collection<AbstractionPredicate>> pInfo) throws CPAException {

    ExplicitPrecision oldExplicitPrecision = Precisions.extractPrecisionByType(oldPrecision, ExplicitPrecision.class);
    if (oldExplicitPrecision == null) {
      throw new IllegalStateException("Could not find the ExplicitPrecision for the error element");
    }

    PredicatePrecision oldPredicatePrecision = null;
    if (predicateCpaInUse) {
      oldPredicatePrecision = Precisions.extractPrecisionByType(oldPrecision, PredicatePrecision.class);
      if (oldPredicatePrecision == null) {
        throw new IllegalStateException("Could not find the PredicatePrecision for the error element");
      }
    }

    List<Collection<AbstractionPredicate>> preds = pInfo.getPredicatesForRefinement();

    PredicateMap predicates = new PredicateMap(preds, pPath);

    Multimap<CFANode, String> variableMapping = predicates.getVariableMapping(fmgr);

    allReferencedVaraibles.addAll(variableMapping.values());

    Multimap<CFANode, String> relevantVariablesOnPath = getRelevantVariablesOnPath(pPath, predicates);
    assert firstInterpolationPoint != null;

//System.out.println("\n" + (++refinementCounter) + ". refining ...");
//System.out.println(getErrorPathAsString(pPath));
//System.out.println("\nreferencedVariables: " + predicates.getReferencedVariables());
//System.out.println("\nallReferencedVaraibles: " + allReferencedVaraibles);
//System.out.println("\nnew predicate map: " + predicates.toString());
//System.out.println("\nfirstInterpolationPoint = " + firstInterpolationPoint + "\n");

    // create the new precision
    ExplicitPrecision newPrecision = new ExplicitPrecision(oldExplicitPrecision, variableMapping, relevantVariablesOnPath);

    ARTElement root = firstInterpolationPoint.getFirst();

    logger.log(Level.FINEST, "Found spurious counterexample,",
          "trying strategy 1: remove everything below", root, "from ART.");
//System.exit(-1);
    if(!pathHashes.add(getErrorPathAsString(pPath).hashCode()))
    {
      if (predicateCpaInUse) {
        predicatePrecision = getPredicatePrecision(getInExplicitVariables(pPath),
            oldPredicatePrecision,
            predicates);
      } else {
        throw new RefinementFailedException(Reason.RepeatedCounterexample, null);
      }

      //System.out.println(getErrorPathAsString(pPath));
      //System.out.println(newPrecision);
      //System.out.println(predicatePrecision);
      //System.out.println("\nlast set of variables in predicates: " + variableMapping.values());

      //
      if(getErrorPathAsString(pPath).equals(previousPath))
        throw new RefinementFailedException(Reason.RepeatedCounterexample, null);

      previousPath = getErrorPathAsString(pPath);
    }

    return Pair.of(root, newPrecision);
  }

  /**
   * This method returns the set of variables contained in the given error path, that cannot be tracked explicitly.
   *
   * @todo still plain stupid, no context information included, like function name or CFANode etc.
   * @param pPath the error path to analyse
   * @return the set of variables contained in the given error path, that cannot be tracked explicitly
   */
  private Set<String> getInExplicitVariables(List<Pair<ARTElement, CFAEdge>> pPath) {
    Set<String> inexplicitVars = new HashSet<String>();

    Function<Pair<?, ? extends CFAEdge>, CFAEdge> projectionToSecond = Pair.getProjectionToSecond();
    for (CFAEdge edge : Lists.transform(pPath, projectionToSecond)) {
      if (edge instanceof AssumeEdge) {
        AssumeEdge assumeEdge = (AssumeEdge)edge;

        IASTExpression assume = assumeEdge.getExpression();

        if (assume instanceof IASTBinaryExpression) {
          IASTBinaryExpression binary = (IASTBinaryExpression)assume;

          if(binary.getOperator() == BinaryOperator.EQUALS && assumeEdge.getTruthAssumption()) {
            //System.out.println("skipping vars in EQUALS from " + assumeEdge.getRawAST().toASTString());
            continue;
          }
          else if(binary.getOperator() == BinaryOperator.NOT_EQUALS && !assumeEdge.getTruthAssumption()) {
            //System.out.println("skipping vars in NOT_EQUALS from " + assumeEdge.getRawAST().toASTString());
            //System.out.println("= skipping vars in NOT_EQUALS from " + assumeEdge);
            continue;
          }

          switch (binary.getOperator()) {
          case EQUALS:
          case NOT_EQUALS:
          case GREATER_THAN:
          case GREATER_EQUAL:
          case LESS_EQUAL:
          case LESS_THAN:
            IASTExpression leftOperand = binary.getOperand1();
            IASTExpression rightOperand = binary.getOperand2();

            // unwarp unary expression, e.g. !a != 0 => a
            if(leftOperand instanceof IASTUnaryExpression) {
              IASTUnaryExpression unaryExpression = (IASTUnaryExpression)leftOperand;

              leftOperand = unaryExpression.getOperand();
            }

            // really just a hack for stuff like (a & b) != 0 => a (and actually b, too), are candidates for inexplicit vars
            else if(leftOperand instanceof IASTBinaryExpression) {
              IASTBinaryExpression binaryExpression = (IASTBinaryExpression)leftOperand;

              leftOperand = binaryExpression.getOperand1();
            }

            if (leftOperand instanceof IASTIdExpression) {
              IASTIdExpression leftIdentifier = (IASTIdExpression)leftOperand;

              //System.out.println("leftIdentifier " + leftIdentifier.getName() + " part of binary expression " + binary.getRawSignature());
              inexplicitVars.add(leftIdentifier.getName());
            } else if (leftOperand instanceof IASTFieldReference) {
              IASTFieldReference leftIdentifier = (IASTFieldReference)leftOperand;

              //System.out.println("leftIdentifier " + leftIdentifier.getRawSignature() + " part of binary expression " + binary.getRawSignature());
              inexplicitVars.add(leftIdentifier.getRawSignature());
            }

            if (rightOperand instanceof IASTIdExpression) {
              IASTIdExpression rightIdentifier = (IASTIdExpression)rightOperand;

              inexplicitVars.add(rightIdentifier.getName());
            } else if (rightOperand instanceof IASTFieldReference) {
              IASTFieldReference rightIdentifier = (IASTFieldReference)rightOperand;

              //System.out.println("rightIdentifier " + rightIdentifier.getRawSignature() + " part of binary expression " + binary.getRawSignature());
              inexplicitVars.add(rightIdentifier.getRawSignature());
            }
            break;
          }
        }
      }
    }

    return inexplicitVars;
  }

  private PredicatePrecision getPredicatePrecision(Set<String> inexplicitVars,
                                                    PredicatePrecision oldPredicatePrecision,
                                                    PredicateMap predicateMap) {

    //System.out.println("inexplicitVars = " + inexplicitVars);
    Set<String> newPreds = new HashSet<String>();

    Multimap<CFANode, AbstractionPredicate> oldPredicateMap = oldPredicatePrecision.getPredicateMap();
    Set<AbstractionPredicate> globalPredicates = oldPredicatePrecision.getGlobalPredicates();

    ImmutableSetMultimap.Builder<CFANode, AbstractionPredicate> pmapBuilder = ImmutableSetMultimap.builder();
    pmapBuilder.putAll(oldPredicateMap);

    for (Map.Entry<CFANode, AbstractionPredicate> predicateAtLocation : predicateMap.getPredicateMapping().entries()) {
      CFANode location                = predicateAtLocation.getKey();
      AbstractionPredicate predicate  = predicateAtLocation.getValue();

      for (String variable : fmgr.extractVariables(predicate.getSymbolicAtom())) {
        //System.out.println("name of var in predicate: " + variable);

        variable = variable.substring(variable.lastIndexOf(":") + 1);

        //System.out.println("name of var in predicate, unscoped: " + variable);

        if (inexplicitVars.contains(variable)) {
          //System.out.println("adding prediacte for variable " + variable + " at loc " + location);
          pmapBuilder.putAll(location, predicate);
          newPreds.add(variable);
        }
      }

      if(predicate.getSymbolicAtom().isFalse())
        pmapBuilder.putAll(location, predicate);
    }

    //if(newPreds.size() > 0)
      //System.out.println("newPreds = " + newPreds);

    return new PredicatePrecision(pmapBuilder.build(), globalPredicates);
  }

  /**
   * This method collects the variables on the error path, on which the variables referenced by predicates depend on. Furthermore, the top-most interpolation point is identified and set.
   *
   * @param errorPath the path to the found error location
   * @param predicates the predicates from the refinement
   * @return the variables on the error path, on which the variables referenced by predicates depend on
   */
  private Multimap<CFANode, String> getRelevantVariablesOnPath(List<Pair<ARTElement, CFAEdge>> errorPath, PredicateMap predicates) {
    CollectVariablesVisitor visitor = new CollectVariablesVisitor(allReferencedVaraibles);

    for (int i = errorPath.size() - 1; i >= 0; --i) {
      Pair<ARTElement, CFAEdge> element = errorPath.get(i);

      CFAEdge edge = element.getSecond();

      if (extractVariables(edge, visitor) || predicates.isInterpolationPoint(edge.getPredecessor()))
        firstInterpolationPoint = element;
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
        String assignedVariable = assignment.getLeftHandSide().getRawSignature();

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
      // do not visit the function name identifier but only the actual parameter expressions
      for (IASTExpression param : functionCallExpression.getParameterExpressions()) {
        param.accept(this);
      }

      // also, add the formal parameters
      // TODO: strange behaviour -> in a few cases, the edge is a statement edge here, so this would fail !?!?!
      if(edge instanceof FunctionCallEdge) {
        FunctionDefinitionNode functionEntryNode = ((FunctionCallEdge)edge).getSuccessor();

        for (String formalParameter : functionEntryNode.getFunctionParameterNames())
          collect(functionEntryNode, formalParameter);
      }

      // TODO: work-around for the above, however, this does not work in all cases either, as getDeclaration returns null sometimes !?!?!
      else {
        if(functionCallExpression.getDeclaration() == null) {
            return null;
        } else if (((IASTFunctionTypeSpecifier)functionCallExpression.getDeclaration().getDeclSpecifier()) == null) {
          return null;
        }

        List<IASTParameterDeclaration> parameters = ((IASTFunctionTypeSpecifier)functionCallExpression.getDeclaration().getDeclSpecifier()).getParameters();

        for(IASTParameterDeclaration parameter : parameters) {
          collect(edge.getSuccessor(), parameter.getName());
        }
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
