// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.livevar;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sosy_lab.cpachecker.util.LiveVariables.LIVE_DECL_EQUIVALENCE;

import com.google.common.base.Equivalence.Wrapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.ast.AArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.AAssignment;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.ACastExpression;
import org.sosy_lab.cpachecker.cfa.ast.ACharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.AFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.AInitializer;
import org.sosy_lab.cpachecker.cfa.ast.AInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.AIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.ALeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.AStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.java.JArrayCreationExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JArrayInitializer;
import org.sosy_lab.cpachecker.cfa.ast.java.JArrayLengthExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JBooleanLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JClassLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JEnumConstantExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.java.JNullLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JRunTimeTypeEqualsType;
import org.sosy_lab.cpachecker.cfa.ast.java.JThisExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JVariableRunTimeType;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.LiveVariables;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;

/**
 * This transfer relation computes the live variables for each location. For C-Programs addressed
 * variables (e.g. &a) are considered as being always live.
 */
@Options(prefix = "cpa.liveVar")
public class LiveVariablesTransferRelation
    extends ForwardingTransferRelation<LiveVariablesState, LiveVariablesState, Precision> {

  private final Map<CFANode, BitSet> liveVariables = new HashMap<>();

  @Option(
      secure = true,
      description =
          "With this option the handling of global variables during the analysis can be fine-tuned."
              + " For example while doing a function-wise analysis it is important to assume that"
              + " all global variables are live. In contrast to that, while doing a global"
              + " analysis, we do not need to assume global variables being live.")
  private boolean assumeGlobalVariablesAreAlwaysLive = true;

  private final ImmutableList<Wrapper<ASimpleDeclaration>> allDeclarations;
  private final Map<Wrapper<? extends ASimpleDeclaration>, Integer> declarationListPos;
  private final int noVars;

  private final BitSet addressedOrGlobalVars;
  private final LogManager logger;
  private final CFA cfa;

  public LiveVariablesTransferRelation(
      Optional<VariableClassification> pVarClass,
      Configuration pConfig,
      Language pLang,
      CFA pCFA,
      LogManager pLogger)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    logger = pLogger;
    cfa = pCFA;

    if (!cfa.getVarClassification().isPresent() && cfa.getLanguage() == Language.C) {
      throw new AssertionError(
          "Without information of the variable classification"
              + " the live variables analysis cannot be used.");
    }

    VariableClassification variableClassification;
    if (pLang == Language.C) {
      variableClassification = pVarClass.orElseThrow();
    } else {
      variableClassification = null;
    }

    allDeclarations = gatherAllDeclarations(pCFA);
    ImmutableMap.Builder<Wrapper<? extends ASimpleDeclaration>, Integer> builder =
        ImmutableMap.builder();
    for (int i = 0; i < allDeclarations.size(); i++) {
      builder.put(allDeclarations.get(i), i);
    }
    declarationListPos = builder.buildOrThrow();
    noVars = allDeclarations.size();

    BitSet addressedVars = new BitSet(noVars);
    if (pLang == Language.C) {
      Set<String> addressedVarsSet =
          Preconditions.checkNotNull(variableClassification).getAddressedVariables();
      for (int i = 0; i < noVars; i++) {
        ASimpleDeclaration decl = checkNotNull(allDeclarations.get(i).get());
        if (addressedVarsSet.contains(decl.getQualifiedName())) {
          addressedVars.set(i);
        }
      }
    }

    BitSet globalVars = new BitSet(noVars);
    if (assumeGlobalVariablesAreAlwaysLive) {
      for (int i = 0; i < noVars; i++) {
        ASimpleDeclaration decl = allDeclarations.get(i).get();
        if (decl instanceof AVariableDeclaration && ((AVariableDeclaration) decl).isGlobal()) {
          globalVars.set(i);
        }
      }
    }

    for (CFANode node : pCFA.nodes()) {
      liveVariables.put(node, new BitSet(noVars));
    }

    addressedOrGlobalVars = (BitSet) addressedVars.clone();
    addressedOrGlobalVars.or(globalVars);
  }

  public LiveVariablesState getInitialState(CFANode pNode) {
    if (pNode instanceof FunctionExitNode eNode) {
      Optional<? extends AVariableDeclaration> returnVarName =
          eNode.getEntryNode().getReturnVariable();

      // e.g. a function void foo();
      if (!returnVarName.isPresent()) {
        return new LiveVariablesState(noVars, this);

      } else {

        // All other function types.
        final Wrapper<ASimpleDeclaration> wrappedVar =
            LiveVariables.LIVE_DECL_EQUIVALENCE.wrap(returnVarName.get());

        int wrappedVarPos = declarationListPos.get(wrappedVar);
        liveVariables.get(pNode).set(wrappedVarPos);

        BitSet out = new BitSet(noVars);
        out.set(wrappedVarPos);
        return LiveVariablesState.ofUnique(out, this);
      }

    } else {
      logger.log(
          Level.FINEST,
          "No FunctionExitNode given, thus creating initial state without having the return"
              + " variable.");
      return LiveVariablesState.empty(noVars, this);
    }
  }

  public ImmutableList<Wrapper<ASimpleDeclaration>> gatherAllDeclarations(CFA pCFA) {
    Set<Wrapper<ASimpleDeclaration>> allDecls = new HashSet<>();
    for (CFANode node : pCFA.nodes()) {

      if (node instanceof FunctionEntryNode entryNode) {
        Optional<? extends AVariableDeclaration> returnVarName = entryNode.getReturnVariable();
        if (returnVarName.isPresent()) {
          allDecls.add(LIVE_DECL_EQUIVALENCE.wrap(returnVarName.get()));
        }
        allDecls.add(LIVE_DECL_EQUIVALENCE.wrap(entryNode.getFunctionDefinition()));
        for (AParameterDeclaration param : entryNode.getFunctionParameters()) {

          // Adding function parameters separately from function declarations
          // as they might not be captured for e.g. external functions.
          allDecls.add(LIVE_DECL_EQUIVALENCE.wrap(param));
        }
      }

      for (CFAEdge e : CFAUtils.enteringEdges(node)) {
        if (e instanceof ADeclarationEdge) {
          ASimpleDeclaration decl = ((ADeclarationEdge) e).getDeclaration();
          allDecls.add(LIVE_DECL_EQUIVALENCE.wrap(decl));
          if (decl instanceof AFunctionDeclaration funcDecl) {
            for (AParameterDeclaration param : funcDecl.getParameters()) {
              allDecls.add(LIVE_DECL_EQUIVALENCE.wrap(param));
            }
          }
        }
      }
    }
    return ImmutableList.copyOf(allDecls);
  }

  @Override
  protected Collection<LiveVariablesState> postProcessing(
      @Nullable LiveVariablesState successor, CFAEdge edge) {
    if (successor == null) {
      return ImmutableSet.of();
    }

    // live variables of multiedges were handled separately.
    liveVariables.get(edge.getPredecessor()).or(successor.getDataCopy());
    return Collections.singleton(successor);
  }

  @Override
  protected LiveVariablesState handleAssumption(
      AssumeEdge cfaEdge, AExpression expression, boolean truthAssumption)
      throws CPATransferException {

    // all variables in assumption become live
    BitSet out = state.getDataCopy();
    handleExpression(expression, out);
    return LiveVariablesState.ofUnique(out, this);
  }

  @Override
  protected LiveVariablesState handleDeclarationEdge(ADeclarationEdge cfaEdge, ADeclaration decl)
      throws CPATransferException {

    // we do only care about variable declarations
    if (!(decl instanceof AVariableDeclaration)) {
      return state;
    }

    Wrapper<ASimpleDeclaration> varDecl = LIVE_DECL_EQUIVALENCE.wrap(decl);
    int varDeclPos = declarationListPos.get(varDecl);
    AInitializer init = ((AVariableDeclaration) decl).getInitializer();

    // there is no initializer thus we only have to remove the initialized variable
    // from the live variables
    if (init == null) {
      return state.removeLiveVariable(varDeclPos);

      // don't do anything if declared variable is not live
    } else if (!state.contains(varDeclPos)) {
      return state;
    }

    BitSet out = state.getDataCopy();
    getVariablesUsedForInitialization(init, out);
    out.clear(varDeclPos);

    return LiveVariablesState.ofUnique(out, this);
  }

  @Override
  protected LiveVariablesState handleStatementEdge(AStatementEdge cfaEdge, AStatement statement)
      throws CPATransferException {
    BitSet out = state.getDataCopy();
    if (statement instanceof AExpressionAssignmentStatement) {
      handleAssignment((AAssignment) statement, out);
      return LiveVariablesState.ofUnique(out, this);

      // no changes as there is no assignment, thus we can return the last state
    } else if (statement instanceof AExpressionStatement) {
      return state;

    } else if (statement instanceof AFunctionCallAssignmentStatement) {
      handleAssignment((AAssignment) statement, out);
      return LiveVariablesState.ofUnique(out, this);

    } else if (statement instanceof AFunctionCallStatement funcStmt) {

      getVariablesUsedAsParameters(
          funcStmt.getFunctionCallExpression().getParameterExpressions(), out);
      return LiveVariablesState.ofUnique(out, this);

    } else {
      throw new CPATransferException("Missing case for if-then-else statement.");
    }
  }

  @Override
  protected LiveVariablesState handleReturnStatementEdge(AReturnStatementEdge cfaEdge)
      throws CPATransferException {
    // this is an empty return statement (return;)
    if (!cfaEdge.asAssignment().isPresent()) {
      return state;
    }
    BitSet data = state.getDataCopy();
    handleAssignment(cfaEdge.asAssignment().get(), data);
    return LiveVariablesState.ofUnique(data, this);
  }

  @Override
  protected LiveVariablesState handleFunctionCallEdge(
      FunctionCallEdge cfaEdge,
      List<? extends AExpression> arguments,
      List<? extends AParameterDeclaration> parameters,
      String calledFunctionName)
      throws CPATransferException {
    /* This analysis is (mostly) used during cfa creation, when no edges between
     * different functions exist, thus this function is mainly unused. However
     * for the purpose of having a complete CPA which works on the graph with
     * all functions connected, this method is implemented.
     */
    BitSet data = state.getDataCopy();

    for (AExpression arg : arguments) {
      handleExpression(arg, data);
    }

    for (AParameterDeclaration decl : parameters) {
      data.clear(declarationListPos.get(LIVE_DECL_EQUIVALENCE.wrap(decl)));
    }

    return LiveVariablesState.ofUnique(data, this);
  }

  @Override
  protected LiveVariablesState handleFunctionReturnEdge(
      FunctionReturnEdge cfaEdge, AFunctionCall summaryExpr, String callerFunctionName)
      throws CPATransferException {
    /* This analysis is (mostly) used during cfa creation, when no edges between
     * different functions exist, thus this function is mainly unused. However
     * for the purpose of having a complete CPA which works on the graph with
     * all functions connected, this method is implemented.
     */

    // we can remove the assigned variable from the live variables
    if (summaryExpr instanceof AFunctionCallAssignmentStatement) {
      boolean isLeftHandsideLive =
          isLeftHandSideLive(((AFunctionCallAssignmentStatement) summaryExpr).getLeftHandSide());
      ASimpleDeclaration retVal = cfaEdge.getFunctionEntry().getReturnVariable().get();
      BitSet data = state.getDataCopy();
      handleAssignment((AAssignment) summaryExpr, data);
      if (isLeftHandsideLive) {
        data.set(declarationListPos.get(LIVE_DECL_EQUIVALENCE.wrap(retVal)));
      }
      return LiveVariablesState.ofUnique(data, this);

      // no assigned variable -> nothing to change
    } else {
      return state;
    }
  }

  @Override
  protected LiveVariablesState handleFunctionSummaryEdge(FunctionSummaryEdge cfaEdge)
      throws CPATransferException {
    AFunctionCall functionCall = cfaEdge.getExpression();
    BitSet data = state.getDataCopy();
    if (functionCall instanceof AFunctionCallAssignmentStatement) {
      handleAssignment((AAssignment) functionCall, data);

    } else if (functionCall instanceof AFunctionCallStatement funcStmt) {
      getVariablesUsedAsParameters(
          funcStmt.getFunctionCallExpression().getParameterExpressions(), data);

    } else {
      throw new CPATransferException("Missing case for if-then-else statement.");
    }
    return LiveVariablesState.ofUnique(data, this);
  }

  /**
   * Returns the liveVariables that are currently computed. Calling this method makes only sense if
   * the analysis was completed
   *
   * @return a Multimap containing the variables that are live at each location
   */
  public Multimap<CFANode, Wrapper<ASimpleDeclaration>> getLiveVariables() {
    ImmutableListMultimap.Builder<CFANode, Wrapper<ASimpleDeclaration>> builder =
        ImmutableListMultimap.builder();
    for (CFANode node : cfa.nodes()) {
      builder.putAll(node, dataToVars(liveVariables.get(node)));
    }
    return builder.build();
  }

  Collection<Wrapper<ASimpleDeclaration>> dataToVars(BitSet data) {
    List<Wrapper<ASimpleDeclaration>> out = new ArrayList<>();
    for (int i = data.nextSetBit(0); i >= 0; i = data.nextSetBit(i + 1)) {
      out.add(allDeclarations.get(i));
      assert i != Integer.MAX_VALUE;
    }
    return out;
  }

  private void handleAssignment(AAssignment assignment, BitSet writeInto) {

    final ALeftHandSide lhs = assignment.getLeftHandSide();

    boolean isLhsAlwaysLive = isAlwaysLive(lhs);
    boolean isLhsLive =
        isLeftHandSideLive(lhs) || assignment instanceof AFunctionCallAssignmentStatement;

    LhsPointerDereferenceVisitor lhsPointerDereferenceVisitor =
        LhsPointerDereferenceVisitor.getInstance();
    boolean lhsIsPointerDereference = false;

    try {
      if (lhs instanceof CLeftHandSide clhs) {
        lhsIsPointerDereference = clhs.accept(lhsPointerDereferenceVisitor);
      } else if (lhs instanceof JLeftHandSide jlhs) {
        lhsIsPointerDereference = jlhs.accept(lhsPointerDereferenceVisitor);
      }
    } catch (Exception e) {
      // Should never happen
    }

    if (!isLhsAlwaysLive && !isLhsLive && !lhsIsPointerDereference) {

      // Assigned variable is not live, so we do not need to make the
      // rightHandSideVariables live.
      return;
    }

    final BitSet assignedVariable = new BitSet(noVars);
    final BitSet newLiveVars = new BitSet(noVars);

    // all variables that occur in combination with the leftHandSide additionally
    // to the needed one (e.g. a[i] i is additionally) are added to the newLiveVariables
    handleLeftHandSide(lhs, assignedVariable);

    handleExpression(lhs, newLiveVars);

    // assigned variable gets removed.
    newLiveVars.andNot(assignedVariable);

    // check all variables of the right-hand-sides, they should be live
    // afterwards if the leftHandSide is live
    if (assignment instanceof AExpressionAssignmentStatement) {
      handleExpression((AExpression) assignment.getRightHandSide(), newLiveVars);

    } else if (assignment instanceof AFunctionCallAssignmentStatement funcStmt) {
      getVariablesUsedAsParameters(
          funcStmt.getFunctionCallExpression().getParameterExpressions(), newLiveVars);

    } else {
      throw new AssertionError("Unhandled assignment type.");
    }

    // if the assigned variable is always live we add it to the live variables
    // additionally to the rightHandSide variables
    if (isLhsAlwaysLive) {
      writeInto.or(assignedVariable);
      writeInto.or(newLiveVars);

      // if lhs is live all variables on the rightHandSide
      // have to get live, parameters of function calls always have to get live,
      // because the function needs those for assigning their variables
    } else if (isLhsLive) {

      // for example an array access *(arr + offset) = 2;
      if (assignedVariable.cardinality() > 1) {
        newLiveVars.or(assignedVariable);
        writeInto.or(newLiveVars);

        // when there is a field reference, an array access or a pointer expression,
        // and the assigned variable was live before, we need to let it also be
        // live afterwards
        // Also, if there is an assignment to one of those, we have to over-approximate it to live
      } else if (lhs instanceof CFieldReference
          || lhs instanceof AArraySubscriptExpression
          || lhs instanceof CPointerExpression) {
        newLiveVars.or(assignedVariable);
        writeInto.or(newLiveVars);

        // no special case here, the assigned variable is not live anymore
      } else {
        writeInto.andNot(assignedVariable);
        writeInto.or(newLiveVars);
      }

      // if the leftHandSide is not life, but there is a pointer dereference
      // we need to make the leftHandSide life. Thus afterwards everything from
      // this statement is life.
    } else {
      assert lhsIsPointerDereference;
      writeInto.or(assignedVariable);
      writeInto.or(newLiveVars);
    }
  }

  /**
   * This method computes the variables that are used for initializing an other variable from a
   * given initializer.
   */
  private void getVariablesUsedForInitialization(AInitializer init, BitSet writeInto)
      throws CPATransferException {
    // e.g. .x=b or .p.x.=1  as part of struct initialization
    if (init instanceof CDesignatedInitializer) {
      getVariablesUsedForInitialization(
          ((CDesignatedInitializer) init).getRightHandSide(), writeInto);

      // e.g. {a, b, s->x} (array) , {.x=1, .y=0} (initialization of struct, array)
    } else if (init instanceof CInitializerList) {
      for (CInitializer inList : ((CInitializerList) init).getInitializers()) {
        getVariablesUsedForInitialization(inList, writeInto);
      }
    } else if (init instanceof AInitializerExpression) {
      handleExpression(((AInitializerExpression) init).getExpression(), writeInto);

    } else {
      throw new CPATransferException("Missing case for if-then-else statement.");
    }
  }

  /**
   * Checks if a leftHandSide variable is live at a given location: this means it either is always
   * live, or it is live in the current state.
   */
  private boolean isLeftHandSideLive(ALeftHandSide expression) {
    BitSet lhs = new BitSet(noVars);
    handleLeftHandSide(expression, lhs);
    return isAlwaysLive(expression) || state.containsAny(lhs);
  }

  /** Mark all declarations occurring inside the expression as live. */
  private void handleExpression(AExpression expression, BitSet writeInto) {
    markDeclarationsInBitSet(CFAUtils.traverseRecursively(expression), writeInto);
  }

  /** Mark all declarations occurring in {@code pLeftHandSide} as live. */
  private void handleLeftHandSide(ALeftHandSide pLeftHandSide, BitSet writeInto) {
    markDeclarationsInBitSet(CFAUtils.traverseLeftHandSideRecursively(pLeftHandSide), writeInto);
  }

  private void markDeclarationsInBitSet(
      FluentIterable<? extends AAstNode> nodes, BitSet writeInto) {
    for (AIdExpression exp : nodes.filter(AIdExpression.class)) {
      int pos =
          declarationListPos.get(LiveVariables.LIVE_DECL_EQUIVALENCE.wrap(exp.getDeclaration()));
      writeInto.set(pos);
    }
  }

  /**
   * Return whether a leftHandSide variable is always live: anything on the LHS is addressed or
   * global.
   */
  private boolean isAlwaysLive(ALeftHandSide expression) {
    BitSet lhs = new BitSet(noVars);
    handleLeftHandSide(expression, lhs);
    lhs.and(addressedOrGlobalVars);
    return !lhs.isEmpty();
  }

  /** Mark all declarations occurring inside the parameters as live. */
  private void getVariablesUsedAsParameters(
      List<? extends AExpression> parameters, BitSet writeInto) {
    for (AExpression expression : parameters) {
      handleExpression(expression, writeInto);
    }
  }

  public static class LhsPointerDereferenceVisitor extends AExpressionVisitor<Boolean, Exception>
      implements CExpressionVisitor<Boolean, Exception> {

    // SINGELTON
    private static final LhsPointerDereferenceVisitor lhsPointerDereferenceVisitor =
        new LhsPointerDereferenceVisitor();

    private LhsPointerDereferenceVisitor() {}

    public static LhsPointerDereferenceVisitor getInstance() {
      return lhsPointerDereferenceVisitor;
    }

    @Override
    public Boolean visit(CBinaryExpression pIastBinaryExpression) throws Exception {
      return pIastBinaryExpression.getOperand1().accept(this)
          || pIastBinaryExpression.getOperand2().accept(this);
    }

    @Override
    public Boolean visit(ABinaryExpression exp) throws Exception {
      if (exp.getOperand1() instanceof CExpression op1
          && exp.getOperand2() instanceof CExpression op2) {
        return op1.accept(this) || op2.accept(this);
      } else if (exp.getOperand1() instanceof JExpression op1
          && exp.getOperand2() instanceof JExpression op2) {
        return op1.accept(this) || op2.accept(this);
      }
      return false;
    }

    @Override
    public Boolean visit(CCastExpression pIastCastExpression) throws Exception {
      return pIastCastExpression.getOperand().accept(this);
    }

    @Override
    public Boolean visit(ACastExpression exp) throws Exception {
      if (exp.getOperand() instanceof CExpression op) {
        return op.accept(this);
      } else if (exp.getOperand() instanceof JExpression op) {
        return op.accept(this);
      }
      return false;
    }

    @Override
    public Boolean visit(CCharLiteralExpression pIastCharLiteralExpression) throws Exception {
      return false;
    }

    @Override
    public Boolean visit(ACharLiteralExpression exp) throws Exception {
      return false;
    }

    @Override
    public Boolean visit(CFloatLiteralExpression pIastFloatLiteralExpression) throws Exception {
      return false;
    }

    @Override
    public Boolean visit(AFloatLiteralExpression exp) throws Exception {
      return false;
    }

    @Override
    public Boolean visit(CIntegerLiteralExpression pIastIntegerLiteralExpression) throws Exception {
      return false;
    }

    @Override
    public Boolean visit(AIntegerLiteralExpression exp) throws Exception {
      return false;
    }

    @Override
    public Boolean visit(CStringLiteralExpression pIastStringLiteralExpression) throws Exception {
      return false;
    }

    @Override
    public Boolean visit(AStringLiteralExpression exp) throws Exception {
      return false;
    }

    @Override
    public Boolean visit(CTypeIdExpression pIastTypeIdExpression) throws Exception {
      return false;
    }

    @Override
    public Boolean visit(CUnaryExpression pIastUnaryExpression) throws Exception {
      if (pIastUnaryExpression.getOperator().equals(UnaryOperator.AMPER)) {
        // TODO: is this correct in this context?
        //  This is not a deref, but we need to mark these variables anyway
        return true;
      }
      return pIastUnaryExpression.getOperand().accept(this);
    }

    @Override
    public Boolean visit(AUnaryExpression exp) throws Exception {
      return exp instanceof JUnaryExpression jUnary
          ? jUnary.accept(this)
          : ((CUnaryExpression) exp).accept(this);
    }

    @Override
    public Boolean visit(CImaginaryLiteralExpression PIastLiteralExpression) throws Exception {
      return false;
    }

    @Override
    public Boolean visit(CAddressOfLabelExpression pAddressOfLabelExpression) throws Exception {
      // TODO: is this correct?
      return false;
    }

    @Override
    public Boolean visit(CArraySubscriptExpression pIastArraySubscriptExpression) throws Exception {
      return true;
    }

    @Override
    public Boolean visit(AArraySubscriptExpression exp) throws Exception {
      return true;
    }

    @Override
    public Boolean visit(CFieldReference pIastFieldReference) throws Exception {
      return true;
    }

    @Override
    public Boolean visit(CIdExpression pIastIdExpression) throws Exception {
      return false;
    }

    @Override
    public Boolean visit(AIdExpression exp) throws Exception {
      return false;
    }

    @Override
    public Boolean visit(CPointerExpression pointerExpression) throws Exception {
      return true;
    }

    @Override
    public Boolean visit(CComplexCastExpression complexCastExpression) throws Exception {
      return false;
    }

    @Override
    public Boolean visit(JBooleanLiteralExpression pJBooleanLiteralExpression) throws Exception {
      return false;
    }

    @Override
    public Boolean visit(JArrayCreationExpression pJArrayCreationExpression) throws Exception {
      // TODO: Is this correct?
      return false;
    }

    @Override
    public Boolean visit(JArrayInitializer pJArrayInitializer) throws Exception {
      // TODO: Is this correct?
      return false;
    }

    @Override
    public Boolean visit(JArrayLengthExpression pJArrayLengthExpression) throws Exception {
      return false;
    }

    @Override
    public Boolean visit(JVariableRunTimeType pJThisRunTimeType) throws Exception {
      return false;
    }

    @Override
    public Boolean visit(JRunTimeTypeEqualsType pJRunTimeTypeEqualsType) throws Exception {
      return false;
    }

    @Override
    public Boolean visit(JNullLiteralExpression pJNullLiteralExpression) throws Exception {
      return false;
    }

    @Override
    public Boolean visit(JEnumConstantExpression pJEnumConstantExpression) throws Exception {
      return false;
    }

    @Override
    public Boolean visit(JThisExpression pThisExpression) throws Exception {
      return false;
    }

    @Override
    public Boolean visit(JClassLiteralExpression pJClassLiteralExpression) throws Exception {
      return false;
    }
  }
}
