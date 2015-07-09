/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.livevar;

import static com.google.common.base.Predicates.*;
import static com.google.common.collect.Collections2.filter;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.LiveVariables.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.ast.AArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.AAssignment;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.AInitializer;
import org.sosy_lab.cpachecker.cfa.ast.AInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.ALeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.VariableClassification;

import com.google.common.base.Equivalence.Wrapper;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

/**
 * This transferrelation computes the live variables for each location. For C-Programs
 * addressed variables (e.g. &a) are considered as being always live.
 */
@Options(prefix="cpa.liveVar")
public class LiveVariablesTransferRelation extends ForwardingTransferRelation<LiveVariablesState, LiveVariablesState, Precision> {

  private final Multimap<CFANode, Wrapper<ASimpleDeclaration>> liveVariables = HashMultimap.<CFANode, Wrapper<ASimpleDeclaration>>create();
  private final VariableClassification variableClassification;
  private final Language language;

  @Option(secure=true, description="With this option the handling of global variables"
      + " during the analysis can be fine-tuned. For example while doing a function-wise"
      + " analysis it is important to assume that all global variables are live. In contrast"
      + " to that, while doing a global analysis, we do not need to assume global"
      + " variables being live.")
  private boolean assumeGlobalVariablesAreAlwaysLive = true;

  public LiveVariablesTransferRelation(Optional<VariableClassification> pVarClass,
                                       Configuration pConfig,
                                       Language pLang) throws InvalidConfigurationException {
    pConfig.inject(this);

    if (pLang == Language.C) {
      variableClassification = pVarClass.get();
    } else {
      variableClassification = null;
    }

    language = pLang;
  }

  @Override
  protected Collection<LiveVariablesState> postProcessing(@Nullable LiveVariablesState successor) {
    if (successor == null) {
      return Collections.emptySet();
    }

    // live variables of multiedges were handleded separately
    if (!(edge instanceof MultiEdge)) {
      liveVariables.putAll(edge.getPredecessor(), successor.getLiveVariables());
    }

    return Collections.singleton(successor);
  }

  @Override
  protected LiveVariablesState handleMultiEdge(MultiEdge cfaEdge) throws CPATransferException {
    // as we are using the backwards analysis, we also have to iterate over
    // multiedges in reverse
    for (final CFAEdge innerEdge : Lists.reverse(cfaEdge.getEdges())) {
      edge = innerEdge;
      state = handleSimpleEdge(innerEdge);
      liveVariables.putAll(edge.getPredecessor(), state.getLiveVariables());
    }
    edge = cfaEdge; // reset edge
    return state;
  }

  /**
   * Returns a collection of all variable names which occur in expression
   */
  private Collection<Wrapper<ASimpleDeclaration>> handleExpression(AExpression expression) {
    return from(acceptAll(expression)).transform(TO_EQUIV_WRAPPER).toSet();
  }

  /**
   * Returns a collection of the variable names in the leftHandSide
   */
  private Collection<Wrapper<ASimpleDeclaration>> handleLeftHandSide(AExpression pLeftHandSide) {
    return from(acceptLeft(pLeftHandSide)).transform(TO_EQUIV_WRAPPER).toSet();
  }

  @Override
  protected  LiveVariablesState handleAssumption(AssumeEdge cfaEdge, AExpression expression, boolean truthAssumption)
      throws CPATransferException {

    // all variables in assumption become live
    return state.addLiveVariables(handleExpression(expression));
  }

  @Override
  protected LiveVariablesState handleDeclarationEdge(ADeclarationEdge cfaEdge, ADeclaration decl)
      throws CPATransferException {

    // we do only care about variable declarations
    if (!(decl instanceof AVariableDeclaration)) {
      return state;
    }

    Wrapper<ASimpleDeclaration> varDecl = LIVE_DECL_EQUIVALENCE.wrap((ASimpleDeclaration)decl);
    Collection<Wrapper<ASimpleDeclaration>> deadVar = Collections.singleton(varDecl);
    AInitializer init = ((AVariableDeclaration)varDecl.get()).getInitializer();

    // there is no initializer thus we only have to remove the initialized variable
    // from the live variables
    if (init == null) {
      return state.removeLiveVariables(deadVar);

      // don't do anything if declarated variable is not live
    } else if (!state.contains(varDecl)) {
      return state;
    }

    return state.removeAndAddLiveVariables(deadVar, getVariablesUsedForInitialization(init));
  }

  /**
   * This method computes the variables that are used for initializing an other
   * variable from a given initializer.
   */
  private Collection<Wrapper<ASimpleDeclaration>> getVariablesUsedForInitialization(AInitializer init) throws CPATransferException {
    // e.g. .x=b or .p.x.=1  as part of struct initialization
    if (init instanceof CDesignatedInitializer) {
      return getVariablesUsedForInitialization(((CDesignatedInitializer) init).getRightHandSide());


    // e.g. {a, b, s->x} (array) , {.x=1, .y=0} (initialization of struct, array)
    } else if (init instanceof CInitializerList) {
      Collection<Wrapper<ASimpleDeclaration>> readVars = new ArrayList<>();

      for (CInitializer inList : ((CInitializerList) init).getInitializers()) {
        readVars.addAll(getVariablesUsedForInitialization(inList));
      }
      return readVars;


    } else if (init instanceof AInitializerExpression) {
      return handleExpression(((AInitializerExpression) init).getExpression());

    } else {
      throw new CPATransferException("Missing case for if-then-else statement.");
    }
  }

  @Override
  protected LiveVariablesState handleStatementEdge(AStatementEdge cfaEdge, AStatement statement)
      throws CPATransferException {
    if (statement instanceof AExpressionAssignmentStatement) {
      return handleAssignments((AAssignment) statement);

      // no changes as there is no assignment, thus we can return the last state
    } else if (statement instanceof AExpressionStatement) {
      return state;

    } else if (statement instanceof AFunctionCallAssignmentStatement) {
      return handleAssignments((AAssignment) statement);

    } else if (statement instanceof AFunctionCallStatement) {

      AFunctionCallStatement funcStmt = (AFunctionCallStatement) statement;
      return state.addLiveVariables(getVariablesUsedAsParameters(funcStmt
                                                                  .getFunctionCallExpression()
                                                                  .getParameterExpressions()));

    } else {
      throw new CPATransferException("Missing case for if-then-else statement.");
    }
  }

  private LiveVariablesState handleAssignments(AAssignment assignment) {
    final Collection<Wrapper<ASimpleDeclaration>> newLiveVariables = new HashSet<>();
    final ALeftHandSide leftHandSide = assignment.getLeftHandSide();
    final Collection<Wrapper<ASimpleDeclaration>> assignedVariable = handleLeftHandSide(leftHandSide);
    final Collection<Wrapper<ASimpleDeclaration>> allLeftHandSideVariables = handleExpression(leftHandSide);
    final Collection<Wrapper<ASimpleDeclaration>> additionallyLeftHandSideVariables = filter(allLeftHandSideVariables, not(in(assignedVariable)));

    // all variables that occur in combination with the leftHandSide additionally
    // to the needed one (e.g. a[i] i is additionally) are added to the newLiveVariables
    newLiveVariables.addAll(additionallyLeftHandSideVariables);

    // check all variables of the rightHandsides, they should be live afterwards
    // if the leftHandSide is live
    if (assignment instanceof AExpressionAssignmentStatement) {
      newLiveVariables.addAll(handleExpression((AExpression) assignment.getRightHandSide()));

    } else if (assignment instanceof AFunctionCallAssignmentStatement){
      AFunctionCallAssignmentStatement funcStmt = (AFunctionCallAssignmentStatement) assignment;
      newLiveVariables.addAll(getVariablesUsedAsParameters(funcStmt.getFunctionCallExpression().getParameterExpressions()));

    } else {
      throw new AssertionError("Unhandled assignment type.");
    }

    // if the assigned variable is always live we add it to the live variables
    // additionally to the rightHandSide variables
    if (isAlwaysLive(leftHandSide)) {
      newLiveVariables.addAll(assignedVariable);
      return state.addLiveVariables(newLiveVariables);

      // if the lefthandSide is live all variables on the rightHandSide
      // have to get live, parameters of function calls always have to get live,
      // because the function needs those for assigning their variables
    } else if (assignment instanceof AFunctionCallAssignmentStatement
              || isLeftHandSideLive(leftHandSide)) {

      // for example an array access *(arr + offset) = 2;
      if (assignedVariable.size() > 1) {
        newLiveVariables.addAll(assignedVariable);
        return state.addLiveVariables(newLiveVariables);

        // when there is a field reference, an array access or a pointer expression,
        // and the assigned variable was live before, we need to let it also be
        // live afterwards
      } else if (leftHandSide instanceof CFieldReference
          || leftHandSide instanceof AArraySubscriptExpression
          || leftHandSide instanceof CPointerExpression) {
        return state.addLiveVariables(newLiveVariables);

        // no special case here, the assigned variable is not live anymore
      } else {
        return state.removeAndAddLiveVariables(assignedVariable, newLiveVariables);
      }

      // if the leftHandSide is not life, but there is a pointer dereference
      // we need to make the leftHandSide life. Thus afterwards everything from
      // this statement is life.
    } else if ((leftHandSide instanceof CFieldReference
                        && (((CFieldReference)leftHandSide).isPointerDereference()
                           || ((CFieldReference)leftHandSide).getFieldOwner() instanceof CPointerExpression))
                || leftHandSide instanceof AArraySubscriptExpression
                || leftHandSide instanceof CPointerExpression) {
      newLiveVariables.addAll(assignedVariable);
      return state.addLiveVariables(newLiveVariables);

      // assigned variable is not live, so we do not need to make the
      // rightHandSideVariables live
    } else {
      return state;
    }
  }

  /**
   * This method checks if a leftHandSide variable is always live.
   */
  private boolean isAlwaysLive(ALeftHandSide expression) {
    return from(acceptLeft(expression)).anyMatch(ALWAYS_LIVE_PREDICATE);
  }

  /**
   * This method checks if a leftHandSide variable is live at a given location,
   * this means it either is always live, or it is live in the current state.
   */
  private boolean isLeftHandSideLive(ALeftHandSide expression) {
    return from(acceptLeft(expression)).anyMatch(LOCALLY_LIVE_PREDICATE);
  }

  /**
   * This method returns the variables that are used in a given list of CExpressions.
   */
  private Collection<Wrapper<ASimpleDeclaration>> getVariablesUsedAsParameters(List<? extends AExpression> parameters) {
    Collection<Wrapper<ASimpleDeclaration>> newLiveVars = new ArrayList<>();
    for (AExpression expression : parameters) {
      newLiveVars.addAll(handleExpression(expression));
    }
    return newLiveVars;
  }

  @Override
  protected LiveVariablesState handleReturnStatementEdge(AReturnStatementEdge cfaEdge)
      throws CPATransferException {
    // this is an empty return statement (return;)
    if (!cfaEdge.asAssignment().isPresent()) {
      return state;
    }

    return handleAssignments(cfaEdge.asAssignment().get());
  }

  @Override
  protected LiveVariablesState handleFunctionCallEdge(FunctionCallEdge cfaEdge,
      List<? extends AExpression> arguments, List<? extends AParameterDeclaration> parameters,
      String calledFunctionName) throws CPATransferException {
    /* This analysis is (mostly) used during cfa creation, when no edges between
     * different functions exist, thus this function is mainly unused. However
     * for the purpose of having a complete CPA which works on the graph with
     * all functions connected, this method is implemented.
     */

    Collection<Wrapper<ASimpleDeclaration>> variablesInArguments = new ArrayList<>();
    for (AExpression arg : arguments) {
      variablesInArguments.addAll(handleExpression(arg));
    }

    // we can safely remove the parameters from the live variables as the function
    // starts at this edge.
    Collection<Wrapper<ASimpleDeclaration>> parameterVars = new ArrayList<>(parameters.size());
    for (AParameterDeclaration decl : parameters) {
      parameterVars.add(LIVE_DECL_EQUIVALENCE.wrap((ASimpleDeclaration)decl));
    }

    return state.removeAndAddLiveVariables(parameterVars, variablesInArguments);
  }

  @Override
  protected LiveVariablesState handleFunctionReturnEdge(FunctionReturnEdge cfaEdge,
      FunctionSummaryEdge fnkCall, AFunctionCall summaryExpr, String callerFunctionName)
      throws CPATransferException {
    /* This analysis is (mostly) used during cfa creation, when no edges between
     * different functions exist, thus this function is mainly unused. However
     * for the purpose of having a complete CPA which works on the graph with
     * all functions connected, this method is implemented.
     */

    // we can remove the assigned variable from the live variables
    if (summaryExpr instanceof AFunctionCallAssignmentStatement) {
      boolean isLeftHandsideLive = isLeftHandSideLive(((AFunctionCallAssignmentStatement) summaryExpr).getLeftHandSide());
      ASimpleDeclaration retVal = cfaEdge.getFunctionEntry().getReturnVariable().get();
      LiveVariablesState returnState = handleAssignments((AAssignment) summaryExpr);
      if (isLeftHandsideLive) {
        returnState = returnState.addLiveVariables(Collections.singleton(LIVE_DECL_EQUIVALENCE.wrap(retVal)));
      }
      return returnState;

    // no assigned variable -> nothing to change
    } else {
      return state;
    }
  }

  @Override
  protected LiveVariablesState handleFunctionSummaryEdge(FunctionSummaryEdge cfaEdge) throws CPATransferException {
    AFunctionCall functionCall = cfaEdge.getExpression();
    if (functionCall instanceof AFunctionCallAssignmentStatement) {
      return handleAssignments((AAssignment) functionCall);

    } else if (functionCall instanceof AFunctionCallStatement) {
      AFunctionCallStatement funcStmt = (AFunctionCallStatement) functionCall;
      return state.addLiveVariables(getVariablesUsedAsParameters(funcStmt
                                                                  .getFunctionCallExpression()
                                                                  .getParameterExpressions()));

    } else {
      throw new CPATransferException("Missing case for if-then-else statement.");
    }
  }

  /**
   * This method puts some variables that are initially live into the
   * live variables multimap.
   */
  public void putInitialLiveVariables(CFANode node, Iterable<Wrapper<ASimpleDeclaration>> liveVars) {
    liveVariables.putAll(node, liveVars);
  }

  /**
   * Returns the liveVariables that are currently computed. Calling this method
   * makes only sense if the analysis was completed
   * @return a Multimap containing the variables that are live at each location
   */
  public Multimap<CFANode, Wrapper<ASimpleDeclaration>> getLiveVariables() {
    return liveVariables;
  }

  @Override
  public Collection<? extends AbstractState> strengthen(AbstractState pState, List<AbstractState> pOtherStates,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException, InterruptedException {
    return null;
  }

  /**
   * variable is always live either if it is addressed or if it is a global variable
   */
  private final Predicate<ASimpleDeclaration> ALWAYS_LIVE_PREDICATE = new Predicate<ASimpleDeclaration>() {

    @Override
    public boolean apply(ASimpleDeclaration decl) {
      boolean retVal =  assumeGlobalVariablesAreAlwaysLive
                        && decl instanceof AVariableDeclaration && ((AVariableDeclaration) decl).isGlobal();

      // in case this is a C Program we need to check if the variable is addressed
      if (language == Language.C) {
        retVal = retVal || variableClassification.getAddressedVariables().contains(decl.getQualifiedName());
      }

      return retVal;
    }};

    /**
     * a variable is locally live either if it is globally live or if it
     * is live in the current state
     */
  private final Predicate<ASimpleDeclaration> LOCALLY_LIVE_PREDICATE =
        or(ALWAYS_LIVE_PREDICATE, new Predicate<ASimpleDeclaration>() {
                  @Override
                  public boolean apply(ASimpleDeclaration decl) {
                      return state.contains(LIVE_DECL_EQUIVALENCE.wrap(decl));
                  }});


  /**
   * This is a more specific version of the CIdExpressionVisitor. For ArraySubscriptexpressions
   * we do only want the IdExpressions inside the ArrayExpression.
   */
  private static final class LeftHandSideIdExpressionVisitor extends DeclarationCollectingVisitor {
    @Override
    public Set<ASimpleDeclaration> visit(AArraySubscriptExpression pE) {
      return pE.getArrayExpression().<Set<ASimpleDeclaration>,
                                      Set<ASimpleDeclaration>,
                                      Set<ASimpleDeclaration>,
                                      RuntimeException,
                                      RuntimeException,
                                      LeftHandSideIdExpressionVisitor>accept_(this);
    }
  }

  private static Set<ASimpleDeclaration> acceptLeft(AExpression exp) {
    return exp.<Set<ASimpleDeclaration>,
                Set<ASimpleDeclaration>,
                Set<ASimpleDeclaration>,
                RuntimeException,
                RuntimeException,
                LeftHandSideIdExpressionVisitor>accept_(new LeftHandSideIdExpressionVisitor());
  }

  private static Set<ASimpleDeclaration> acceptAll(AExpression exp) {
    return exp.<Set<ASimpleDeclaration>,
                Set<ASimpleDeclaration>,
                Set<ASimpleDeclaration>,
                RuntimeException,
                RuntimeException,
                DeclarationCollectingVisitor>accept_(new DeclarationCollectingVisitor());
  }
}
