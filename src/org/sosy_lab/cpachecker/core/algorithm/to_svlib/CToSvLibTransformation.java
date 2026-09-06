// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.to_svlib;

import com.google.common.base.Verify;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SmtLibTheoryDeclarations;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibBitVectorConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibBooleanConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIdTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIntegerConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSymbolApplicationTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibRelationalTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibTagReference;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.SvLibCurrentScope;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibParsingParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibParsingVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibProcedureDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibSimpleParsingDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibAssumeStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibGotoStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibHavocStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibIfStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibLabelStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibProcedureCallStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibReturnStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibSequenceStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibStatement;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeQualifiers;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibBitVectorType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibPredefinedType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibType;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.BuiltinFunctions;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.EdgeCollectingCFAVisitor;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter.RightHandSideTerm;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.DynamicMemoryHandler;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerBase;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.TypeHandlerWithPointerAliasing;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.svlibwitnessexport.FormulaToSvLibVisitor;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.visitors.DefaultBooleanFormulaVisitor;

class CToSvLibTransformation {
  private final CFA cfa;
  private final FormulaManagerView formulaManager;
  private final PathFormulaManager pathFormulaManager;
  private final FormulaToSvLibVisitor formulaToSvLibVisitor;

  private final SvLibCurrentScope scope;

  /**
   * The variables that the transformation itself introduced, such as the one that holds the address
   * above all memory that has been allocated so far, by their name.
   */
  private final Map<String, SvLibParsingVariableDeclaration> variablesOfTransformation =
      new LinkedHashMap<>();

  /**
   * The number of allocations of memory for which the transformation has built a formula, which
   * gives every allocation of the program a base of its own: the formulas of each procedure are
   * built separately, so the counting would otherwise start again in every procedure and two
   * allocations in different procedures would get the same address.
   */
  private int numberOfAllocations = 0;

  /**
   * An object that exists from the moment the procedure it belongs to is entered, i.e. a global
   * variable or a local variable whose address is taken.
   *
   * @param address the variable of the generated program that holds the address of the object
   * @param type the type of the object, which gives its size
   * @param isGlobal whether the object exists from the beginning of the execution
   */
  private record ObjectWithAddress(SvLibSimpleDeclaration address, CType type, boolean isGlobal) {}

  /** The objects whose address the program takes, by their base. */
  private final Map<PointerBase, ObjectWithAddress> objectsWithAddress = new LinkedHashMap<>();

  /** The objects that each procedure declares itself, by the name of the procedure. */
  private final Map<String, Set<PointerBase>> objectsOfProcedures = new LinkedHashMap<>();

  /** The objects of {@link #objectsWithAddress} that the current procedure uses. */
  private final Set<PointerBase> objectsOfCurrentProcedure = new LinkedHashSet<>();

  /** The sizes of the types that the formulas of the analysis assume. */
  private final TypeHandlerWithPointerAliasing typeHandler;

  CToSvLibTransformation(
      CFA pCFA,
      FormulaManagerView pFormulaManager,
      PathFormulaManager pPathFormulaManager,
      FormulaToSvLibVisitor pFormulaToSvLibVisitor,
      SvLibCurrentScope pCurrentScope,
      TypeHandlerWithPointerAliasing pTypeHandler) {
    cfa = pCFA;
    formulaManager = pFormulaManager;
    pathFormulaManager = pPathFormulaManager;
    formulaToSvLibVisitor = pFormulaToSvLibVisitor;
    scope = pCurrentScope;
    typeHandler = pTypeHandler;
  }

  SvLibSequenceStatement transformFunction(@NonNull CFunctionEntryNode pEntryNode)
      throws CPATransferException, InterruptedException {
    SvLibProcedureDeclaration procedureDeclaration =
        scope.getProcedureDeclaration(pEntryNode.getFunctionName());
    String procedureName = procedureDeclaration.getProcedureName();
    ImmutableListMultimap.Builder<CFANode, SvLibStatement> statementCollector =
        ImmutableListMultimap.builder();
    ImmutableMap.Builder<CFAEdge, PointerTargetSet> edgeToPointerTargetSet = ImmutableMap.builder();
    objectsOfCurrentProcedure.clear();

    scope.enterProcedure(
        FluentIterable.from(procedureDeclaration.getParameters())
            .append(procedureDeclaration.getLocalVariables())
            .append(procedureDeclaration.getReturnValues())
            .toList());
    // The scope of the procedure is left in a finally block, so that a failing transformation of
    // one procedure does not leave the local variables of that procedure in the scope and make the
    // transformation of the next procedure fail with a misleading message.
    try {

      // Only in the procedure created for the main() function, and not entry functions in general:
      // Initialize _retval_ to 0 to account for the possibility of an implicit return 0; statement
      // in the main() function if no return value has been explicitly defined.
      if (pEntryNode.getFunctionName().equals("main")
          && !procedureDeclaration.getReturnValues().isEmpty()) {
        statementCollector.put(
            pEntryNode,
            createDefaultReturnForMain(pEntryNode, procedureDeclaration, edgeToPointerTargetSet));
      }

      // assign the dummy variables created for the input parameters to assignable variables that
      // have the original variable names
      if (!procedureDeclaration.getParameters().isEmpty()) {
        ImmutableMap.Builder<SvLibSimpleParsingDeclaration, SvLibTerm> inputAssignmentsCollector =
            ImmutableMap.builder();

        for (SvLibParsingParameterDeclaration inputParameter :
            procedureDeclaration.getParameters()) {
          // The variable is looked up by its qualified name, because a local variable that
          // shadows a global one is renamed and its name in the generated program is not the one
          // of the input parameter without the prefix.
          SvLibSimpleParsingDeclaration assignableInputDummyVariable =
              scope.getVariableForQualifiedName(
                  inputParameter.getProcedureName()
                      + "::"
                      + getOriginalNameOfInputParameterDummy(inputParameter.getName()));

          inputAssignmentsCollector.put(
              assignableInputDummyVariable,
              new SvLibIdTerm(inputParameter.toSimpleDeclaration(), FileLocation.DUMMY));
        }

        SvLibAssignmentStatement assignDummyInput =
            new SvLibAssignmentStatement(
                inputAssignmentsCollector.buildOrThrow(),
                FileLocation.DUMMY,
                ImmutableList.of(),
                ImmutableList.of());
        statementCollector.put(pEntryNode, assignDummyInput);
      }

      ImmutableList<CFAEdge> relevantEdges = getAllRelevantEdges(pEntryNode);

      ImmutableSet<CFANode> relevantNodes =
          createAllLabels(pEntryNode, relevantEdges, statementCollector);

      // transform each edge to SV-LIB statement(s)
      for (CFAEdge currentEdge : relevantEdges) {
        transformEdge(currentEdge, statementCollector, edgeToPointerTargetSet);
      }

      // What happens at a node that control does not leave through an edge depends on the node and
      // not on how many edges lead to it, so these statements are created for the nodes and not
      // while the edges are transformed.
      ImmutableSet<CFANode> nodesWithEdges =
          FluentIterable.from(relevantEdges).transform(CFAEdge::getPredecessor).toSet();
      for (CFANode node : relevantNodes) {
        if (node instanceof FunctionExitNode) {
          // A return statement is transformed into one, so the exit node only needs one for the
          // edges that reach it without returning a value.
          if (node.getEnteringEdges().anyMatch(edge -> !(edge instanceof CReturnStatementEdge))) {
            statementCollector.put(
                node,
                new SvLibReturnStatement(
                    FileLocation.DUMMY, ImmutableList.of(), ImmutableList.of()));
          }
        } else if (node instanceof CFATerminationNode terminationNode) {
          statementCollector.put(terminationNode, encodeTerminationNode(terminationNode));
        } else if (!nodesWithEdges.contains(node)) {
          // A node that has no edge to leave it cannot be left, for example the node after a call
          // of a function that does not return. Without a statement that says so, the generated
          // program would continue with the statements that follow, which the input program cannot.
          statementCollector.put(node, createAssumptionThatNeverHolds(node));
        }
      }

      // Which objects the procedure declares itself is only used once every procedure is
      // transformed, because the variable that holds the limit of the allocations is declared by
      // the first allocation, which may be in a procedure that follows this one.
      objectsOfProcedures.put(
          procedureName,
          FluentIterable.from(objectsOfCurrentProcedure)
              .filter(base -> !isDeclaredGlobally(base))
              .toSet());

      return createSequenceStatement(statementCollector.build(), procedureName);

    } finally {
      scope.leaveProcedure();
    }
  }

  private ImmutableList<CFAEdge> getAllRelevantEdges(FunctionEntryNode pEntryNode) {
    final EdgeCollectingCFAVisitor edgeCollector = new EdgeCollectingCFAVisitor();
    CFATraversal.dfs().ignoreFunctionCalls().traverseOnce(pEntryNode, edgeCollector);
    return ImmutableList.copyOf(edgeCollector.getVisitedEdges());
  }

  /** Create the labels of all nodes of the procedure and return those nodes. */
  private ImmutableSet<CFANode> createAllLabels(
      FunctionEntryNode pFunctionEntryNode,
      ImmutableList<CFAEdge> pRelevantEdges,
      ImmutableListMultimap.Builder<CFANode, SvLibStatement> pStatementCollector) {
    ImmutableSet.Builder<CFANode> relevantNodesCollector = ImmutableSet.builder();
    relevantNodesCollector.add(pFunctionEntryNode);
    for (CFAEdge currentEdge : pRelevantEdges) {
      relevantNodesCollector.add(currentEdge.getSuccessor());
    }

    ImmutableSet<CFANode> relevantNodes = relevantNodesCollector.build();
    for (CFANode node : relevantNodes) {
      String nodeNumber = node.toString();
      pStatementCollector.put(node, createLabelStatement(nodeNumber));

      if (node instanceof CFALabelNode labelNode) {
        String labelNodeName = labelNode.getLabel() + "__" + nodeNumber;
        pStatementCollector.put(node, createLabelStatement(labelNodeName));
      }
    }
    return relevantNodes;
  }

  private SvLibLabelStatement createLabelStatement(String pLabelName) {
    SvLibTagReference tagReference = new SvLibTagReference(pLabelName, FileLocation.DUMMY);
    return new SvLibLabelStatement(
        FileLocation.DUMMY, ImmutableList.of(), ImmutableList.of(tagReference), pLabelName);
  }

  private SvLibAssignmentStatement createDefaultReturnForMain(
      CFunctionEntryNode pEntryNode,
      SvLibProcedureDeclaration pProcedureDeclaration,
      ImmutableMap.Builder<CFAEdge, PointerTargetSet> pEdgeToPointerTargetSet)
      throws CPATransferException, InterruptedException {
    CStatementEdge statementEdge =
        new CStatementEdge(
            "retval = 0;",
            new CExpressionAssignmentStatement(
                FileLocation.DUMMY,
                new CIdExpression(FileLocation.DUMMY, pEntryNode.getReturnVariable().orElseThrow()),
                new CIntegerLiteralExpression(
                    FileLocation.DUMMY,
                    pEntryNode.getReturnVariable().orElseThrow().getType(),
                    BigInteger.ZERO)),
            FileLocation.DUMMY,
            pEntryNode,
            pEntryNode);
    SvLibTerm assignmentTerm = transformEdgeToSvLibTerm(statementEdge, pEdgeToPointerTargetSet);
    // Obtain the only constant term inside the previous term
    // could be done with a visitor but is easier like this
    // and works for most cases
    SvLibConstantTerm returnValue;
    if (assignmentTerm instanceof SvLibConstantTerm pConstantTerm) {
      returnValue = pConstantTerm;
    } else if (assignmentTerm instanceof SvLibSymbolApplicationTerm symbolApplicationTerm) {
      List<SvLibConstantTerm> constantTerms =
          FluentIterable.from(symbolApplicationTerm.getTerms())
              .filter(SvLibConstantTerm.class)
              .toList();
      returnValue = Iterables.getOnlyElement(constantTerms);
    } else {
      throw new UnsupportedOperationException(
          "Unexpected term generated for return value initialization in main function: "
              + assignmentTerm);
    }

    return new SvLibAssignmentStatement(
        ImmutableMap.of(pProcedureDeclaration.getReturnValues().getFirst(), returnValue),
        FileLocation.DUMMY,
        ImmutableList.of(),
        ImmutableList.of());
  }

  /**
   * Transform the given edge into the statements of the generated program.
   *
   * <p>A value that the transformation of the edge introduced, such as the value of a call whose
   * semantics the analysis does not know, has to be a new one every time the statements are
   * executed, for example in every iteration of a loop, so it is havoced before them.
   */
  private void transformEdge(
      CFAEdge pEdge,
      ImmutableListMultimap.Builder<CFANode, SvLibStatement> pCreatedStatements,
      ImmutableMap.Builder<CFAEdge, PointerTargetSet> pEdgeToPointerTargetSet)
      throws CPATransferException, InterruptedException {
    ImmutableListMultimap.Builder<CFANode, SvLibStatement> statementsOfEdge =
        ImmutableListMultimap.builder();
    transformEdgeToStatements(pEdge, statementsOfEdge, pEdgeToPointerTargetSet);

    ImmutableList<SvLibParsingVariableDeclaration> freshValues =
        formulaToSvLibVisitor.pollFreshValuesOfFormulas();
    if (!freshValues.isEmpty()) {
      pCreatedStatements.put(
          pEdge.getPredecessor(),
          new SvLibHavocStatement(
              FileLocation.DUMMY,
              ImmutableList.of(),
              ImmutableList.of(),
              ImmutableList.<SvLibSimpleParsingDeclaration>copyOf(freshValues)));
    }
    pCreatedStatements.putAll(statementsOfEdge.build());
  }

  private void transformEdgeToStatements(
      CFAEdge pEdge,
      ImmutableListMultimap.Builder<CFANode, SvLibStatement> pCreatedStatements,
      ImmutableMap.Builder<CFAEdge, PointerTargetSet> pEdgeToPointerTargetSet)
      throws CPATransferException, InterruptedException {
    switch (pEdge.getEdgeType()) {
      case BlankEdge -> {
        transformEdgeToSvLibTerm(pEdge, pEdgeToPointerTargetSet);
        pCreatedStatements.put(pEdge.getPredecessor(), createGotoStatement(pEdge.getSuccessor()));
      }
      case AssumeEdge -> {
        // The formula of the edge is built in any case, because the pointer target set that it
        // computes is the context of the edges that follow this one.
        SvLibTerm transformedTerm = transformEdgeToSvLibTerm(pEdge, pEdgeToPointerTargetSet);
        SvLibGotoStatement gotoStatement = createGotoStatement(pEdge.getSuccessor());
        if (isLastConditionOfBranch(pEdge)) {
          // The conditions of the edges of a branch cover all cases, so the last one is reached
          // exactly if all the others are false and needs no condition of its own. A condition
          // here would leave the statements after the branch reachable without any of the
          // conditions holding, which is a path that the program does not have.
          pCreatedStatements.put(pEdge.getPredecessor(), gotoStatement);
        } else {
          pCreatedStatements.put(
              pEdge.getPredecessor(),
              new SvLibIfStatement(
                  FileLocation.DUMMY,
                  ImmutableList.of(),
                  ImmutableList.of(),
                  transformedTerm,
                  gotoStatement));
        }
      }
      case StatementEdge -> {
        CStatementEdge statementEdge = (CStatementEdge) pEdge;
        if (statementEdge.getStatement() instanceof CFunctionCall functionCall
            && !isEncodedInFormula(functionCall)) {
          SvLibStatement externCallStatement =
              transformCallToExternalFunction(statementEdge, pEdgeToPointerTargetSet);
          pCreatedStatements.put(pEdge.getPredecessor(), externCallStatement);
        } else {
          pCreatedStatements.put(
              pEdge.getPredecessor(), transformAssignmentEdge(pEdge, pEdgeToPointerTargetSet));
        }
        pCreatedStatements.put(pEdge.getPredecessor(), createGotoStatement(pEdge.getSuccessor()));
      }
      case DeclarationEdge -> {
        pCreatedStatements.put(
            pEdge.getPredecessor(), transformAssignmentEdge(pEdge, pEdgeToPointerTargetSet));
        pCreatedStatements.put(pEdge.getPredecessor(), createGotoStatement(pEdge.getSuccessor()));
      }
      case ReturnStatementEdge -> {
        pCreatedStatements.put(
            pEdge.getPredecessor(), transformAssignmentEdge(pEdge, pEdgeToPointerTargetSet));
        SvLibReturnStatement returnStatement =
            new SvLibReturnStatement(FileLocation.DUMMY, ImmutableList.of(), ImmutableList.of());
        pCreatedStatements.put(pEdge.getPredecessor(), returnStatement);
      }
      case FunctionCallEdge, FunctionReturnEdge -> {
        // function calls are handled in case for CallToReturnEdge
      }
      case CallToReturnEdge -> {
        // CFunctionSummaryEdge for function calls
        CFunctionSummaryEdge callEdge = (CFunctionSummaryEdge) pEdge;
        SvLibStatement callStatement = transformFunctionCall(callEdge, pEdgeToPointerTargetSet);
        pCreatedStatements.put(pEdge.getPredecessor(), callStatement);
        pCreatedStatements.put(pEdge.getPredecessor(), createGotoStatement(pEdge.getSuccessor()));
      }
    }
  }

  /**
   * Is the given edge the last one of the branch that its predecessor performs, i.e. the one that
   * is taken if the conditions of all the other edges of that branch are false?
   */
  private boolean isLastConditionOfBranch(CFAEdge pEdge) {
    CFANode predecessor = pEdge.getPredecessor();
    return predecessor.getLeavingEdges().allMatch(AssumeEdge.class::isInstance)
        && predecessor.getLeavingEdge(predecessor.getNumLeavingEdges() - 1) == pEdge;
  }

  /**
   * Does {@link CtoFormulaConverter} encode the given call in the formula of its edge instead of
   * treating it as a call of a function of the program?
   *
   * <p>This is the case for the functions that allocate memory, where it creates a base for the
   * allocated memory together with the constraints that separate its address from the addresses of
   * all other objects, and for the functions that the compiler and the standard library provide,
   * such as {@code isinf}, whose semantics it knows. The transformation therefore uses that formula
   * instead of creating a procedure call, because a procedure that only havocs its return value
   * would know nothing about the result.
   */
  private boolean isEncodedInFormula(CFunctionCall pFunctionCall) {
    String functionName =
        pFunctionCall.getFunctionCallExpression().getFunctionNameExpression().toASTString();
    return CToSvLibTransformationConstants.NAMES_OF_MEMORY_ALLOCATION_FUNCTIONS.contains(
            functionName)
        || BuiltinFunctions.isBuiltinFunction(functionName)
        || hasSideEffectInFormula(pFunctionCall);
  }

  /**
   * Does the formula of the given call contain the effect of that call on the memory of the
   * program, as it does for {@code memset}?
   *
   * <p>Only the formula of the whole edge encodes such a call correctly, because a procedure of the
   * generated program that only havocs the returned value does not write to the memory, and neither
   * does the transformation of the two sides of an assignment on their own.
   */
  private boolean hasSideEffectInFormula(CFunctionCall pFunctionCall) {
    return CtoFormulaConverter.isSideEffectFunction(
        pFunctionCall.getFunctionCallExpression().getFunctionNameExpression().toASTString());
  }

  private boolean hasSideEffectInFormula(CAssignment pAssignment) {
    return pAssignment instanceof CFunctionCall functionCall
        && hasSideEffectInFormula(functionCall);
  }

  /** Does the given assignment assign the result of a call that its formula encodes? */
  private boolean isEncodedInFormula(CAssignment pAssignment) {
    return pAssignment instanceof CFunctionCall functionCall && isEncodedInFormula(functionCall);
  }

  /**
   * Does the given assignment assign the result of a call that allocates memory?
   *
   * <p>Such a call is only encoded correctly in the formula of the whole edge, because that formula
   * also contains the base address of the allocated memory and the update of the pointer target set
   * that goes with it.
   */
  private boolean isMemoryAllocation(CAssignment pAssignment) {
    return pAssignment instanceof CFunctionCall functionCall
        && CToSvLibTransformationConstants.NAMES_OF_MEMORY_ALLOCATION_FUNCTIONS.contains(
            functionCall.getFunctionCallExpression().getFunctionNameExpression().toASTString());
  }

  /**
   * Transform an edge that assigns a value into the corresponding SV-LIB statement.
   *
   * <p>Whenever the assignment writes to a variable that is represented by a variable in the
   * generated SV-LIB program, the two sides of the assignment are transformed separately. The
   * alternative would be to transform the formula that the {@link PathFormulaManager} creates for
   * the whole edge, which is an equality between the old and the new instance of the assigned
   * variable. That equality cannot be taken apart reliably, because its shape depends on the
   * simplifications that the used SMT solver applies: MathSAT5 for example normalizes the formula
   * for {@code x = x + 1;} into {@code x@2 - x@1 = 1}. Since the SSA indices are dropped when
   * translating back to SV-LIB, the old and the new instance of the variable are not
   * distinguishable in such a formula any more.
   *
   * <p>For assignments to variables whose address is taken, and for assignments to array elements
   * or fields, the assigned memory is represented by the array that models the heap, and the
   * formula for the whole edge is used, since it already contains the necessary update of that
   * array. The same holds for an allocation of memory, whose base address the formula of the edge
   * introduces.
   */
  private SvLibStatement transformAssignmentEdge(
      CFAEdge pEdge, ImmutableMap.Builder<CFAEdge, PointerTargetSet> pEdgeToPointerTargetSet)
      throws CPATransferException, InterruptedException {
    PointerTargetSet pointerTargetSetBeforeEdge = getPtsForEdge(pEdge, pEdgeToPointerTargetSet);
    PathFormula contextBeforeEdge =
        pathFormulaManager.makeEmptyPathFormulaWithContext(
            SSAMap.emptySSAMap(), pointerTargetSetBeforeEdge);
    // The formula for the whole edge is always created, because the pointer target set that it
    // computes is the context of the edges that follow this one.
    PathFormula edgeFormula = pathFormulaManager.makeAnd(contextBeforeEdge, pEdge);
    recordObjectsThatAlwaysExist(edgeFormula.getPointerTargetSet());
    if (cfa.edges().contains(pEdge)) {
      pEdgeToPointerTargetSet.put(pEdge, edgeFormula.getPointerTargetSet());
    }

    Optional<CAssignment> assignment = getAssignmentOfEdge(pEdge);
    if (assignment.isPresent()
        && assignment.orElseThrow().getLeftHandSide() instanceof CIdExpression lhs
        && !isMemoryAllocation(assignment.orElseThrow())
        && !hasSideEffectInFormula(assignment.orElseThrow())
        && isRepresentedByOwnVariable(lhs, pointerTargetSetBeforeEdge)) {
      return createAssignmentStatement(
          lhs, assignment.orElseThrow().getRightHandSide(), contextBeforeEdge, pEdge);
    }

    SvLibStatement statementOfEdge =
        transformEdgeFormula(pEdge, contextBeforeEdge.getSsa(), edgeFormula);
    if (assignment.isPresent() && isMemoryAllocation(assignment.orElseThrow())) {
      return withSeparationFromEarlierAllocations(
          statementOfEdge,
          (CFunctionCall) assignment.orElseThrow(),
          edgeFormula.getPointerTargetSet(),
          contextBeforeEdge,
          pEdge);
    }
    return statementOfEdge;
  }

  /**
   * Make every execution of the given statement allocate a new block of memory.
   *
   * <p>The formula of the edge contains the constraints that separate the new block from the
   * objects that existed when the edge was transformed, but it is the same for every execution of
   * the statement, so two iterations of a loop around an allocation would otherwise be allowed to
   * return the same address. The address of the new block is therefore required to lie above all
   * memory that has been allocated so far, and that limit is raised by the size of the block.
   *
   * <p>Whether an allocation succeeds is decided by a value that the formula of the edge names
   * after the called function, and every call decides it anew, so that value is havoced as well.
   */
  private SvLibStatement withSeparationFromEarlierAllocations(
      SvLibStatement pStatementOfEdge,
      CFunctionCall pAllocation,
      PointerTargetSet pAfterEdge,
      PathFormula pContext,
      CFAEdge pEdge)
      throws CPATransferException, InterruptedException {
    ImmutableList<SvLibParsingVariableDeclaration> addresses =
        formulaToSvLibVisitor.pollAllocatedAddressesOfFormulas();
    if (addresses.isEmpty()) {
      return pStatementOfEdge;
    }

    ImmutableList.Builder<SvLibStatement> beforeEdge = ImmutableList.builder();
    String nameOfAllocation =
        pAllocation.getFunctionCallExpression().getFunctionNameExpression().toASTString();
    if (scope.hasVariable(nameOfAllocation)) {
      beforeEdge.add(
          new SvLibHavocStatement(
              FileLocation.DUMMY,
              ImmutableList.of(),
              ImmutableList.of(),
              ImmutableList.of(scope.getVariable(nameOfAllocation))));
    }
    ImmutableList.Builder<SvLibStatement> afterEdge = ImmutableList.builder();
    for (SvLibParsingVariableDeclaration address : addresses) {
      SvLibParsingVariableDeclaration limit = getHighestAllocatedAddress(address.getType());
      SvLibIdTerm addressTerm = new SvLibIdTerm(address.toSimpleDeclaration(), FileLocation.DUMMY);
      SvLibIdTerm limitTerm = new SvLibIdTerm(limit.toSimpleDeclaration(), FileLocation.DUMMY);

      beforeEdge.add(
          new SvLibAssumeStatement(
              FileLocation.DUMMY,
              applyBinaryOperator(atLeastDeclaration(address.getType()), addressTerm, limitTerm),
              ImmutableList.of(),
              ImmutableList.of()));

      Optional<SvLibTerm> size =
          getSizeOfAllocatedMemory(address, pAfterEdge, pAllocation, pContext, pEdge);
      if (size.isPresent()) {
        afterEdge.add(
            new SvLibAssignmentStatement(
                ImmutableMap.<SvLibSimpleParsingDeclaration, SvLibTerm>of(
                    limit,
                    applyBinaryOperator(
                        additionDeclaration(address.getType()), addressTerm, size.orElseThrow())),
                FileLocation.DUMMY,
                ImmutableList.of(),
                ImmutableList.of()));
      }
    }

    return new SvLibSequenceStatement(
        ImmutableList.<SvLibStatement>builder()
            .addAll(beforeEdge.build())
            .add(pStatementOfEdge)
            .addAll(afterEdge.build())
            .build(),
        FileLocation.DUMMY,
        ImmutableList.of(),
        ImmutableList.of());
  }

  /**
   * The size of the memory that the given allocation reserves, if it can be determined.
   *
   * <p>The size of the type of the allocated block is the one that the analysis of the C program
   * uses. That type is only known later for an allocation whose size is not the one of a type, as
   * in {@code malloc(n)}, and the argument of the call is used in that case.
   */
  private Optional<SvLibTerm> getSizeOfAllocatedMemory(
      SvLibParsingVariableDeclaration pAddress,
      PointerTargetSet pAfterEdge,
      CFunctionCall pAllocation,
      PathFormula pContext,
      CFAEdge pEdge)
      throws CPATransferException, InterruptedException {
    Optional<PointerBase> base = PointerBase.fromFormulaEncoding(unquote(pAddress.getName()));
    CType typeOfBase = base.isPresent() ? pAfterEdge.getBases().get(base.orElseThrow()) : null;
    if (typeOfBase != null && typeOfBase.hasKnownConstantSize()) {
      return Optional.of(
          createNumericConstant(cfa.getMachineModel().getSizeof(typeOfBase), pAddress.getType()));
    }

    ImmutableList<CExpression> arguments =
        pAllocation.getFunctionCallExpression().getParameterExpressions();
    if (arguments.size() != 1) {
      return Optional.empty();
    }
    RightHandSideTerm size =
        pathFormulaManager.rightHandSideToFormula(
            pContext, arguments.getFirst(), cfa.getMachineModel().getSizeType(), pEdge);
    return Optional.of(formulaManager.visit(size.term(), formulaToSvLibVisitor));
  }

  /**
   * The name of a variable of the generated program without the quotes around it, if it has any.
   */
  private String unquote(String pName) {
    return pName.startsWith("|") && pName.endsWith("|")
        ? pName.substring(1, pName.length() - 1)
        : pName;
  }

  /** The variable that holds the address above all memory that has been allocated so far. */
  private SvLibParsingVariableDeclaration getHighestAllocatedAddress(SvLibType pAddressType) {
    return declareVariableOfTransformation(
        CToSvLibTransformationConstants.HIGHEST_ALLOCATED_ADDRESS, pAddressType);
  }

  /**
   * Declare a global variable that the transformation needs, unless it is already declared.
   *
   * <p>Such a variable holds a value that the transformation itself introduces, so it is not one of
   * the program and every use of it assigns it before it is read.
   */
  private SvLibParsingVariableDeclaration declareVariableOfTransformation(
      String pName, SvLibType pType) {
    SvLibParsingVariableDeclaration declaration = variablesOfTransformation.get(pName);
    if (declaration == null) {
      declaration =
          new SvLibParsingVariableDeclaration(
              FileLocation.DUMMY, true, false, pType, pName, pName, null);
      scope.addVariable(declaration);
      variablesOfTransformation.put(pName, declaration);
    }
    return declaration;
  }

  /** Remember the objects of the given set of pointer targets that exist from the beginning. */
  private void recordObjectsThatAlwaysExist(PointerTargetSet pPointerTargetSet) {
    numberOfAllocations = Math.max(numberOfAllocations, pPointerTargetSet.getAllocationCount());
    for (Map.Entry<PointerBase, CType> base : pPointerTargetSet.getBases().entrySet()) {
      String addressName = "|" + base.getKey().formulaEncoding() + "|";
      if (DynamicMemoryHandler.isAllocBase(base.getKey()) || !scope.hasVariable(addressName)) {
        continue;
      }
      SvLibSimpleParsingDeclaration address = scope.getVariable(addressName);
      objectsWithAddress.putIfAbsent(
          base.getKey(),
          new ObjectWithAddress(
              address.toSimpleDeclaration(),
              base.getValue(),
              address instanceof SvLibParsingVariableDeclaration variable && variable.isGlobal()));
      objectsOfCurrentProcedure.add(base.getKey());
    }
  }

  /**
   * The assumption that the memory which the program allocates lies above every object that exists
   * from the beginning of the execution.
   *
   * <p>The formula of an edge only separates a new block of memory from the objects that the
   * transformation knows when it transforms that edge, and every procedure is transformed on its
   * own, so an allocation inside a procedure is separated from neither the global variables nor the
   * objects of the callers. Without this assumption the generated program is allowed to allocate
   * memory at the address of a global variable, which makes it unsafe although the input program is
   * not.
   */
  SvLibStatement getSeparationOfAllocationsFromObjectsOf(String pProcedureName, boolean pIsEntry) {
    ImmutableList.Builder<PointerBase> objects = ImmutableList.builder();
    objects.addAll(objectsOfProcedures.getOrDefault(pProcedureName, ImmutableSet.of()));
    if (pIsEntry) {
      // The global variables exist from the beginning of the execution, which begins here.
      objects.addAll(
          FluentIterable.from(objectsWithAddress.keySet()).filter(this::isDeclaredGlobally));
    }
    return getSeparationOfAllocationsFrom(objects.build(), pIsEntry);
  }

  /** Is the address of the object of the given base a variable of the whole program? */
  private boolean isDeclaredGlobally(PointerBase pBase) {
    ObjectWithAddress object = objectsWithAddress.get(pBase);
    return object != null && object.isGlobal();
  }

  /** The assumptions for the given objects, see {@link #getSeparationOfAllocationsFrom}. */
  private SvLibStatement getSeparationOfAllocationsFrom(
      Iterable<PointerBase> pObjects, boolean pIsEntryProcedure) {
    ImmutableMap<PointerBase, BigInteger> offsets = getOffsetsOfObjectsWithAddress();
    ImmutableList.Builder<SvLibStatement> statements = ImmutableList.builder();

    SvLibType addressType = getTypeOfAddresses();
    if (pIsEntryProcedure && !offsets.isEmpty()) {
      // The objects lie below the address at which the allocated memory begins, so that address
      // has to be above all of them for the addresses not to wrap around.
      statements.add(
          new SvLibAssumeStatement(
              FileLocation.DUMMY,
              applyBinaryOperator(
                  atLeastDeclaration(addressType),
                  new SvLibIdTerm(
                      getFirstAllocatedAddress(addressType).toSimpleDeclaration(),
                      FileLocation.DUMMY),
                  createNumericConstant(
                      Collections.max(offsets.values()).add(BigInteger.ONE), addressType)),
              ImmutableList.of(),
              ImmutableList.of()));
      // The memory that the program allocates begins above every object that has an address, and
      // nothing has been allocated when the execution begins.
      statements.add(
          new SvLibAssignmentStatement(
              ImmutableMap.of(
                  getHighestAllocatedAddress(addressType),
                  new SvLibIdTerm(
                      getFirstAllocatedAddress(addressType).toSimpleDeclaration(),
                      FileLocation.DUMMY)),
              FileLocation.DUMMY,
              ImmutableList.of(),
              ImmutableList.of()));
    }

    for (PointerBase base : pObjects) {
      BigInteger offset = offsets.get(base);
      ObjectWithAddress object = objectsWithAddress.get(base);
      if (offset == null || object == null) {
        continue;
      }
      // The objects lie one after the other below the memory that the program allocates, which
      // makes them pairwise separated and separated from every allocation. The formulas of the
      // edges do not say this, because the transformation builds them for every procedure on its
      // own with all objects already in the context.
      statements.add(
          new SvLibAssumeStatement(
              FileLocation.DUMMY,
              applyBinaryOperator(
                  equalityDeclaration(addressType),
                  new SvLibIdTerm(object.address(), FileLocation.DUMMY),
                  applyBinaryOperator(
                      subtractionDeclaration(addressType),
                      new SvLibIdTerm(
                          getFirstAllocatedAddress(addressType).toSimpleDeclaration(),
                          FileLocation.DUMMY),
                      createNumericConstant(offset, addressType))),
              ImmutableList.of(),
              ImmutableList.of()));
    }

    ImmutableList<SvLibStatement> createdStatements = statements.build();
    if (createdStatements.isEmpty()) {
      return SvLibSequenceStatement.emptySequence();
    }
    return new SvLibSequenceStatement(
        createdStatements, FileLocation.DUMMY, ImmutableList.of(), ImmutableList.of());
  }

  /** The type of the addresses of the generated program, which every object with one has. */
  private SvLibType getTypeOfAddresses() {
    return objectsWithAddress.values().stream()
        .findFirst()
        .map(object -> object.address().getType())
        .orElse(SvLibSmtLibPredefinedType.INT);
  }

  /**
   * The distance of every object with an address from the address at which the memory that the
   * program allocates begins, which lays the objects out one after the other below it.
   *
   * <p>The size of every object is rounded up to a multiple of the size of the largest type, so
   * that every object is aligned as the analysis of the C program expects it to be.
   */
  private ImmutableMap<PointerBase, BigInteger> getOffsetsOfObjectsWithAddress() {
    BigInteger alignment = BigInteger.valueOf(8);
    ImmutableMap.Builder<PointerBase, BigInteger> offsets = ImmutableMap.builder();
    BigInteger offset = BigInteger.ZERO;
    for (Map.Entry<PointerBase, ObjectWithAddress> object : objectsWithAddress.entrySet()) {
      BigInteger size = getSizeOfObject(object.getValue().type());
      BigInteger sizeWithAlignment =
          size.add(alignment).subtract(BigInteger.ONE).divide(alignment).multiply(alignment);
      offset = offset.add(sizeWithAlignment.max(alignment));
      offsets.put(object.getKey(), offset);
    }
    return offsets.buildOrThrow();
  }

  /**
   * The size that the transformation assumes for an object of the given type, which is the size
   * that the formulas assume for it and zero if they do not know it, because an object of unknown
   * size still has to be separated from the other objects.
   */
  private BigInteger getSizeOfObject(CType pType) {
    CType type = pType.getCanonicalType();
    if (type.hasKnownConstantSize() || type instanceof CArrayType) {
      return BigInteger.valueOf(typeHandler.getApproximatedSizeof(type));
    }
    return BigInteger.ZERO;
  }

  /** The variable that holds the address at which the allocated memory begins. */
  private SvLibParsingVariableDeclaration getFirstAllocatedAddress(SvLibType pAddressType) {
    return declareVariableOfTransformation(
        CToSvLibTransformationConstants.FIRST_ALLOCATED_ADDRESS, pAddressType);
  }

  /** The variables that the transformation itself introduced and that have to be declared. */
  ImmutableList<SvLibParsingVariableDeclaration> getVariablesOfTransformation() {
    return ImmutableList.copyOf(variablesOfTransformation.values());
  }

  /**
   * The operator that compares two addresses, which is a signed comparison because the analysis of
   * the C program compares the addresses of the bases of the heap in the same way.
   */
  private SvLibFunctionDeclaration atLeastDeclaration(SvLibType pType) {
    if (pType instanceof SvLibSmtLibBitVectorType bitVectorType) {
      return SmtLibTheoryDeclarations.bitVectorSignedGreaterEqual(bitVectorType.getSize());
    }
    return SmtLibTheoryDeclarations.INT_GREATER_EQUAL_THAN;
  }

  private SvLibFunctionDeclaration equalityDeclaration(SvLibType pType) {
    if (pType instanceof SvLibSmtLibBitVectorType bitVectorType) {
      return SmtLibTheoryDeclarations.bitVectorEquality(bitVectorType.getSize());
    }
    return SmtLibTheoryDeclarations.INT_EQUALITY;
  }

  private SvLibFunctionDeclaration subtractionDeclaration(SvLibType pType) {
    if (pType instanceof SvLibSmtLibBitVectorType bitVectorType) {
      return SmtLibTheoryDeclarations.bitVectorSubstraction(bitVectorType.getSize());
    }
    return SmtLibTheoryDeclarations.intSubtraction(2);
  }

  private SvLibFunctionDeclaration additionDeclaration(SvLibType pType) {
    if (pType instanceof SvLibSmtLibBitVectorType bitVectorType) {
      return SmtLibTheoryDeclarations.bitVectorAddition(bitVectorType.getSize());
    }
    return SmtLibTheoryDeclarations.intAddition(2);
  }

  private SvLibTerm applyBinaryOperator(
      SvLibFunctionDeclaration pOperator, SvLibTerm pLeft, SvLibTerm pRight) {
    return new SvLibSymbolApplicationTerm(
        new SvLibIdTerm(pOperator, FileLocation.DUMMY),
        ImmutableList.of(pLeft, pRight),
        FileLocation.DUMMY);
  }

  /** The assignment that the given edge performs, if it performs one. */
  private Optional<CAssignment> getAssignmentOfEdge(CFAEdge pEdge) {
    return switch (pEdge) {
      case CStatementEdge statementEdge ->
          statementEdge.getStatement() instanceof CAssignment assignment
              ? Optional.of(assignment)
              : Optional.empty();
      case CDeclarationEdge declarationEdge ->
          declarationEdge.getDeclaration() instanceof CVariableDeclaration variableDeclaration
                  && variableDeclaration.getInitializer()
                      instanceof CInitializerExpression initializer
              ? Optional.of(
                  new CExpressionAssignmentStatement(
                      variableDeclaration.getFileLocation(),
                      new CIdExpression(variableDeclaration.getFileLocation(), variableDeclaration),
                      initializer.getExpression()))
              : Optional.empty();
      case CReturnStatementEdge returnStatementEdge -> returnStatementEdge.asAssignment();
      default -> Optional.empty();
    };
  }

  /**
   * Is the given variable represented by a variable of the generated SV-LIB program, and not by the
   * array that models the heap?
   */
  private boolean isRepresentedByOwnVariable(
      CIdExpression pVariable, PointerTargetSet pPointerTargetSet) {
    CType type = pVariable.getExpressionType().getCanonicalType();
    if (type instanceof CArrayType || type instanceof CCompositeType) {
      return false;
    }
    String qualifiedName = pVariable.getDeclaration().getQualifiedName();
    for (PointerBase base : pPointerTargetSet.getBases().keySet()) {
      if (base.name().equals(qualifiedName)) {
        // The address of the variable is taken, so it is part of the heap representation.
        return false;
      }
    }
    // The variable is only representable if it is actually declared in the generated program.
    return scope.hasVariableForQualifiedName(qualifiedName);
  }

  /**
   * Create the SV-LIB statement that assigns the given right-hand side to the given variable,
   * transforming both sides separately.
   */
  private SvLibStatement createAssignmentStatement(
      CIdExpression pLeftHandSide,
      CRightHandSide pRightHandSide,
      PathFormula pContext,
      CFAEdge pEdge)
      throws CPATransferException {
    RightHandSideTerm rightHandSide =
        pathFormulaManager.rightHandSideToFormula(
            pContext, pRightHandSide, pLeftHandSide.getExpressionType(), pEdge);

    SvLibSimpleDeclaration assignedVariable =
        scope
            .getVariableForQualifiedName(pLeftHandSide.getDeclaration().getQualifiedName())
            .toSimpleDeclaration();
    SvLibAssignmentStatement assignmentStatement =
        createAssignmentStatement(
            new SvLibIdTerm(assignedVariable, FileLocation.DUMMY),
            formulaManager.visit(rightHandSide.term(), formulaToSvLibVisitor),
            pEdge.getPredecessor().getFunctionName());

    // Constraints such as the axioms for bitwise operations have to hold in addition to the
    // assignment itself, so they are assumed directly after it.
    if (formulaManager.getBooleanFormulaManager().isTrue(rightHandSide.constraints())) {
      return assignmentStatement;
    }
    return new SvLibSequenceStatement(
        ImmutableList.of(
            assignmentStatement,
            new SvLibAssumeStatement(
                FileLocation.DUMMY,
                formulaManager.visit(rightHandSide.constraints(), formulaToSvLibVisitor),
                ImmutableList.of(),
                ImmutableList.of())),
        FileLocation.DUMMY,
        ImmutableList.of(),
        ImmutableList.of());
  }

  private @NonNull SvLibTerm transformEdgeToSvLibTerm(
      CFAEdge pEdge, ImmutableMap.Builder<CFAEdge, PointerTargetSet> pEdgeToPointerTargetSet)
      throws CPATransferException, InterruptedException {
    PointerTargetSet pointerTargetSet = getPtsForEdge(pEdge, pEdgeToPointerTargetSet);
    PathFormula edgeFormula =
        pathFormulaManager.makeEmptyPathFormulaWithContext(SSAMap.emptySSAMap(), pointerTargetSet);
    edgeFormula = pathFormulaManager.makeAnd(edgeFormula, pEdge);
    if (cfa.edges().contains(pEdge)) {
      pEdgeToPointerTargetSet.put(pEdge, edgeFormula.getPointerTargetSet());
    }
    recordObjectsThatAlwaysExist(edgeFormula.getPointerTargetSet());
    return formulaManager.visit(edgeFormula.getFormula(), formulaToSvLibVisitor);
  }

  private PointerTargetSet getPtsForEdge(
      CFAEdge pEdge, ImmutableMap.Builder<CFAEdge, PointerTargetSet> pEdgeToPointerTargetSet)
      throws InterruptedException {
    PointerTargetSet pointerTargetSet = PointerTargetSet.emptyPointerTargetSet();
    if (pEdge.getPredecessor().getNumEnteringEdges() >= 1) {
      ImmutableMap<CFAEdge, PointerTargetSet> edgeToPtsBuilt =
          pEdgeToPointerTargetSet.buildOrThrow();
      SSAMapBuilder ssaMapBuilder = SSAMap.emptySSAMap().builder();
      FluentIterable<CFAEdge> enteringEdges = pEdge.getPredecessor().getEnteringEdges();
      for (CFAEdge enteringEdge : enteringEdges) {
        PointerTargetSet predEdgePts = edgeToPtsBuilt.get(enteringEdge);
        if (predEdgePts != null) {
          pointerTargetSet =
              pathFormulaManager.mergePts(pointerTargetSet, predEdgePts, ssaMapBuilder);
        }
      }
      if (pEdge.getPredecessor().getEnteringSummaryEdge() != null) {
        PointerTargetSet predEdgePts =
            edgeToPtsBuilt.get(pEdge.getPredecessor().getEnteringSummaryEdge());
        if (predEdgePts != null) {
          pointerTargetSet =
              pathFormulaManager.mergePts(pointerTargetSet, predEdgePts, ssaMapBuilder);
        }
      }
    }
    return pointerTargetSet;
  }

  private void storePtsForFunctionCall(
      CFAEdge pEdge, ImmutableMap.Builder<CFAEdge, PointerTargetSet> pEdgeToPointerTargetSet)
      throws InterruptedException, CPATransferException {
    PointerTargetSet pointerTargetSet = getPtsForEdge(pEdge, pEdgeToPointerTargetSet);

    PathFormula edgeFormula =
        pathFormulaManager.makeEmptyPathFormulaWithContext(SSAMap.emptySSAMap(), pointerTargetSet);
    edgeFormula = pathFormulaManager.makeAnd(edgeFormula, pEdge);
    pEdgeToPointerTargetSet.put(pEdge, edgeFormula.getPointerTargetSet());
  }

  private SvLibStatement transformCallToExternalFunction(
      CStatementEdge pStatementEdge,
      ImmutableMap.Builder<CFAEdge, PointerTargetSet> pEdgeToPointerTargetSet)
      throws CPATransferException, InterruptedException {
    storePtsForFunctionCall(pStatementEdge, pEdgeToPointerTargetSet);
    // Everything that is not a variable of the program, such as an element of an array, a member
    // of a structure or the target of a pointer, is memory that the arrays of the heap model.
    if (pStatementEdge.getStatement()
            instanceof CFunctionCallAssignmentStatement callAssignmentStatement
        && !(callAssignmentStatement.getLeftHandSide() instanceof CIdExpression)) {
      return handleReturnValueAssignmentToHeap(
          pStatementEdge, callAssignmentStatement, pEdgeToPointerTargetSet);
    } else if (pStatementEdge.getStatement()
        instanceof CFunctionCallAssignmentStatement functionCallAssignmentStatement) {

      CIdExpression lhsIdExpression =
          (CIdExpression) functionCallAssignmentStatement.getLeftHandSide();
      PointerTargetSet pointerTargetSet =
          pEdgeToPointerTargetSet.buildOrThrow().get(pStatementEdge);

      NavigableSet<PointerBase> pointerBases = pointerTargetSet.getBases().keySet();
      for (PointerBase base : pointerBases) {
        if (base.name().equals(lhsIdExpression.getDeclaration().getQualifiedName())) {
          return handleReturnValueAssignmentToHeap(
              pStatementEdge, functionCallAssignmentStatement, pEdgeToPointerTargetSet);
        }
      }
      return createProcedureCallStatement(
          pStatementEdge,
          functionCallAssignmentStatement,
          lhsIdExpression,
          pEdgeToPointerTargetSet);

    } else if (pStatementEdge.getStatement()
        instanceof CFunctionCallStatement functionCallStatement) {

      SvLibProcedureDeclaration calledProcedure =
          scope.getProcedureDeclaration(
              CToSvLibTransformationConstants.asSymbol(
                  functionCallStatement
                      .getFunctionCallExpression()
                      .getFunctionNameExpression()
                      .toASTString()));

      // Handle calls to a set of external __assert functions that have a char* input parameter
      if (CToSvLibTransformationConstants.NAMES_OF_ASSERT_FUNCTIONS.contains(
          calledProcedure.getName())) {
        return new SvLibProcedureCallStatement(
            FileLocation.DUMMY,
            ImmutableList.of(),
            ImmutableList.of(),
            calledProcedure,
            ImmutableList.of(),
            ImmutableList.of());
      } else if (CToSvLibTransformationConstants.NAMES_OF_UNSUPPORTED_STDLIB_EXTERNAL_FUNCTIONS
          .contains(calledProcedure.getName())) {
        throw new UnsupportedOperationException(
            "Call to unsupported external function encountered");
      } else if (CToSvLibTransformationConstants.NAMES_OF_UNSUPPORTED_NONDET_FUNCTIONS.contains(
          calledProcedure.getName())) {
        throw new UnsupportedOperationException(
            "Call to unsupported external function encountered");
      }

      return createProcedureCallStatementWithDummyReturn(
          pStatementEdge, functionCallStatement, calledProcedure, pEdgeToPointerTargetSet);
    }
    throw new UnsupportedOperationException(
        "Failed to transform call to extern C function to SvLib based on Edge " + pStatementEdge);
  }

  private SvLibStatement transformFunctionCall(
      CFunctionSummaryEdge pCallEdge,
      ImmutableMap.Builder<CFAEdge, PointerTargetSet> pEdgeToPointerTargetSet)
      throws CPATransferException, InterruptedException {
    storePtsForFunctionCall(pCallEdge, pEdgeToPointerTargetSet);

    CFunctionCall functionCall = pCallEdge.getExpression();
    if (pCallEdge.getExpression() instanceof CFunctionCallAssignmentStatement assignment
        && !(assignment.getLeftHandSide() instanceof CIdExpression)) {
      return handleReturnValueAssignmentToHeap(pCallEdge, assignment, pEdgeToPointerTargetSet);

    } else if (functionCall instanceof CFunctionCallAssignmentStatement functionCallAssignment) {
      CIdExpression lhsIdExpression = (CIdExpression) functionCallAssignment.getLeftHandSide();
      PointerTargetSet pointerTargetSet = pEdgeToPointerTargetSet.buildOrThrow().get(pCallEdge);
      NavigableSet<PointerBase> pointerBases = pointerTargetSet.getBases().keySet();
      for (PointerBase base : pointerBases) {
        if (base.name().equals(lhsIdExpression.getDeclaration().getQualifiedName())) {
          return handleReturnValueAssignmentToHeap(
              pCallEdge, functionCallAssignment, pEdgeToPointerTargetSet);
        }
      }

      return createProcedureCallStatement(
          pCallEdge, functionCallAssignment, lhsIdExpression, pEdgeToPointerTargetSet);

    } else if (functionCall instanceof CFunctionCallStatement callStatement) {
      SvLibProcedureDeclaration calledProcedure =
          scope.getProcedureDeclaration(pCallEdge.getFunctionEntry().getFunctionName());
      return createProcedureCallStatementWithDummyReturn(
          pCallEdge, callStatement, calledProcedure, pEdgeToPointerTargetSet);

    } else {
      throw new UnsupportedOperationException(
          "Failed to convert CFunctionCall to SvLibProcedureCallStatement based on"
              + " CFunctionSummaryEdge "
              + pCallEdge);
    }
  }

  private SvLibStatement createProcedureCallStatement(
      CFAEdge pCallEdge,
      CFunctionCallAssignmentStatement pFunctionCallAssignmentStatement,
      CIdExpression pLhsIdExpression,
      ImmutableMap.Builder<CFAEdge, PointerTargetSet> pEdgeToPointerTargetSet)
      throws CPATransferException, InterruptedException {
    SvLibProcedureDeclaration calledProcedure =
        scope.getProcedureDeclaration(
            CToSvLibTransformationConstants.asSymbol(
                pFunctionCallAssignmentStatement
                    .getRightHandSide()
                    .getFunctionNameExpression()
                    .toASTString()));

    InputParameters inputParameters =
        transformInputParameters(
            pFunctionCallAssignmentStatement.getRightHandSide(),
            pCallEdge,
            calledProcedure,
            pEdgeToPointerTargetSet);

    SvLibSimpleParsingDeclaration assignedVariable =
        scope.getVariableForQualifiedName(pLhsIdExpression.getDeclaration().getQualifiedName());
    if (calledProcedure.getReturnValues().size() != 1) {
      throw new UnsupportedOperationException(
          "The result of the call "
              + pFunctionCallAssignmentStatement.toASTString()
              + " is assigned, but the procedure "
              + calledProcedure.getProcedureName()
              + " returns "
              + calledProcedure.getReturnValues().size()
              + " values");
    }
    SvLibType returnType = calledProcedure.getReturnValues().getFirst().getType();

    if (assignedVariable.getType().equals(returnType)) {
      return withAssumedConstraints(
          new SvLibProcedureCallStatement(
              FileLocation.DUMMY,
              ImmutableList.of(),
              ImmutableList.of(),
              calledProcedure,
              inputParameters.terms(),
              ImmutableList.of(assignedVariable)),
          inputParameters.constraints());
    }

    // The assigned variable has a different type than the return value of the procedure, which
    // happens for an assignment like "int x = f();" where f returns an unsigned char. The return
    // value is therefore stored in a dummy variable of the type of the procedure and converted to
    // the type of the assigned variable afterwards, like the implicit conversion in C does.
    SvLibSimpleParsingDeclaration returnDummyVariable = getReturnDummyVariable(returnType);
    SvLibProcedureCallStatement callStatement =
        new SvLibProcedureCallStatement(
            FileLocation.DUMMY,
            ImmutableList.of(),
            ImmutableList.of(),
            calledProcedure,
            inputParameters.terms(),
            ImmutableList.of(returnDummyVariable));
    SvLibAssignmentStatement conversionStatement =
        new SvLibAssignmentStatement(
            ImmutableMap.of(
                assignedVariable,
                convertReturnValue(
                    new SvLibIdTerm(returnDummyVariable.toSimpleDeclaration(), FileLocation.DUMMY),
                    assignedVariable.getType(),
                    pFunctionCallAssignmentStatement.getRightHandSide().getExpressionType())),
            FileLocation.DUMMY,
            ImmutableList.of(),
            ImmutableList.of());
    return withAssumedConstraints(
        new SvLibSequenceStatement(
            ImmutableList.of(callStatement, conversionStatement),
            FileLocation.DUMMY,
            ImmutableList.of(),
            ImmutableList.of()),
        inputParameters.constraints());
  }

  /**
   * Convert the value of the return variable of a procedure to the type of the variable that it is
   * assigned to, in the same way as the implicit conversion of C does.
   *
   * @param pReturnValue the term for the return value of the procedure
   * @param pTargetType the type of the variable that the return value is assigned to
   * @param pSourceType the C type of the return value, needed to know whether it is signed
   */
  private SvLibTerm convertReturnValue(
      SvLibTerm pReturnValue, SvLibType pTargetType, CType pSourceType) {
    if (!(pTargetType instanceof SvLibSmtLibBitVectorType targetType)
        || !(pReturnValue.getExpressionType() instanceof SvLibSmtLibBitVectorType sourceType)) {
      throw new UnsupportedOperationException(
          "Cannot convert the return value of a procedure from "
              + pReturnValue.getExpressionType()
              + " to "
              + pTargetType);
    }

    SvLibFunctionDeclaration conversion;
    if (targetType.getSize() > sourceType.getSize()) {
      // A value of a signed type keeps its value by repeating its sign bit, an unsigned one by
      // being filled up with zeros.
      conversion =
          isSigned(pSourceType)
              ? SmtLibTheoryDeclarations.bitVectorSignExtend(
                  sourceType.getSize(), targetType.getSize())
              : SmtLibTheoryDeclarations.bitVectorZeroExtend(
                  sourceType.getSize(), targetType.getSize());
    } else {
      // The conversion to a smaller type keeps the least significant bits.
      conversion =
          SmtLibTheoryDeclarations.bitVectorExtract(
              sourceType.getSize(), targetType.getSize() - 1, 0);
    }
    return new SvLibSymbolApplicationTerm(
        new SvLibIdTerm(conversion, FileLocation.DUMMY),
        ImmutableList.of(pReturnValue),
        FileLocation.DUMMY);
  }

  /** Is the given C type a signed integer type? */
  private boolean isSigned(CType pType) {
    return pType.getCanonicalType() instanceof CSimpleType simpleType
        && cfa.getMachineModel().isSigned(simpleType);
  }

  /**
   * The variable that holds the value that a procedure returns before it is converted to the type
   * of the variable that it is assigned to.
   */
  private SvLibSimpleParsingDeclaration getReturnDummyVariable(SvLibType pReturnType) {
    String name = CToSvLibTransformationConstants.returnDummyVariableName(pReturnType);
    if (scope.hasVariable(name)) {
      return scope.getVariable(name);
    }
    return declareVariableOfTransformation(
        CToSvLibTransformationConstants.globalReturnDummyVariableName(pReturnType), pReturnType);
  }

  private SvLibStatement createProcedureCallStatementWithDummyReturn(
      CFAEdge pCallEdge,
      CFunctionCallStatement pFunctionCallStatement,
      SvLibProcedureDeclaration pCalledProcedure,
      ImmutableMap.Builder<CFAEdge, PointerTargetSet> pEdgeToPointerTargetSet)
      throws CPATransferException, InterruptedException {

    ImmutableList.Builder<SvLibSimpleParsingDeclaration> returnVariableDummies =
        ImmutableList.builder();
    for (SvLibParsingParameterDeclaration parsingParameterDeclaration :
        pCalledProcedure.getReturnValues()) {
      returnVariableDummies.add(getReturnDummyVariable(parsingParameterDeclaration.getType()));
    }

    InputParameters inputParameters =
        transformInputParameters(
            pFunctionCallStatement.getFunctionCallExpression(),
            pCallEdge,
            pCalledProcedure,
            pEdgeToPointerTargetSet);
    return withAssumedConstraints(
        new SvLibProcedureCallStatement(
            FileLocation.DUMMY,
            ImmutableList.of(),
            ImmutableList.of(),
            pCalledProcedure,
            inputParameters.terms(),
            returnVariableDummies.build()),
        inputParameters.constraints());
  }

  private SvLibSequenceStatement handleReturnValueAssignmentToHeap(
      CFAEdge pCallEdge,
      CFunctionCallAssignmentStatement pAssignmentStatement,
      ImmutableMap.Builder<CFAEdge, PointerTargetSet> pEdgeToPointerTargetSet)
      throws CPATransferException, InterruptedException {

    String calledFunctionName =
        CToSvLibTransformationConstants.asSymbol(
            pAssignmentStatement.getRightHandSide().getFunctionNameExpression().toASTString());
    SvLibProcedureDeclaration calledProcedure = scope.getProcedureDeclaration(calledFunctionName);

    String tmpVariableQualifiedName = constructTmpVariableName(calledProcedure, pCallEdge);
    SvLibSimpleParsingDeclaration variable =
        scope.getVariableForQualifiedName(tmpVariableQualifiedName);

    InputParameters inputParameters =
        transformInputParameters(
            pAssignmentStatement.getRightHandSide(),
            pCallEdge,
            calledProcedure,
            pEdgeToPointerTargetSet);
    SvLibStatement callStatement =
        withAssumedConstraints(
            new SvLibProcedureCallStatement(
                FileLocation.DUMMY,
                ImmutableList.of(),
                ImmutableList.of(),
                calledProcedure,
                inputParameters.terms(),
                ImmutableList.of(variable)),
            inputParameters.constraints());

    // The value that the procedure returns is stored in the memory by an assignment of the variable
    // that holds it, which is transformed like every other assignment to the memory.
    SvLibStatement assignmentStatement =
        transformAssignmentEdge(
            createAssignmentOfVariable(
                tmpVariableQualifiedName,
                pAssignmentStatement.getRightHandSide().getExpressionType(),
                pAssignmentStatement.getLeftHandSide(),
                pCallEdge),
            pEdgeToPointerTargetSet);

    return new SvLibSequenceStatement(
        ImmutableList.of(callStatement, assignmentStatement),
        FileLocation.DUMMY,
        ImmutableList.of(),
        ImmutableList.of());
  }

  private String constructTmpVariableName(
      SvLibProcedureDeclaration pCalledProcedure, CFAEdge pCallEdge) {

    ImmutableList<@NonNull SvLibParsingParameterDeclaration> returnValues =
        pCalledProcedure.getReturnValues();
    SvLibType returnValueType;
    Verify.verify(returnValues.size() == 1, "Failed to get name of TMP variable.");

    returnValueType = returnValues.getFirst().getType();
    return pCallEdge.getPredecessor().getFunctionName()
        + "::"
        + CToSvLibTransformationConstants.tmpVariableNameForAssignment(returnValueType);
  }

  /**
   * An edge that assigns the variable with the given name to the given left-hand side.
   *
   * <p>The edge is not part of the CFA and is only transformed in order to obtain the statements
   * that store a value in the memory of the program.
   *
   * <p>The variable has the type of the value that it holds and not the one of the left-hand side,
   * because the assignment converts the value to that type. Giving it the type of the left-hand
   * side would make the formula refer to it with a different type than the one that it is declared
   * with in the generated program.
   */
  private CStatementEdge createAssignmentOfVariable(
      String pVariableName, CType pVariableType, CLeftHandSide pLeftHandSide, CFAEdge pEdge) {
    CVariableDeclaration variableDeclaration =
        new CVariableDeclaration(
            FileLocation.DUMMY,
            false,
            CStorageClass.AUTO,
            pVariableType,
            pVariableName,
            pVariableName,
            pVariableName,
            null);
    CExpressionAssignmentStatement assignment =
        new CExpressionAssignmentStatement(
            FileLocation.DUMMY,
            pLeftHandSide,
            new CIdExpression(
                FileLocation.DUMMY, pVariableType, pVariableName, variableDeclaration));
    return new CStatementEdge(
        "", assignment, FileLocation.DUMMY, pEdge.getPredecessor(), pEdge.getPredecessor());
  }

  /**
   * Transform the arguments of a procedure call into SV-LIB terms.
   *
   * <p>Every argument is transformed on its own, so that the shape of the formula that the SMT
   * solver builds for it does not matter. Transforming the arguments through the formula of an
   * artificial assume edge, which encodes {@code argument != 0}, and recovering the argument from
   * that formula afterwards would depend on the simplifications of the used solver.
   */
  private InputParameters transformInputParameters(
      CFunctionCallExpression pFunctionCall,
      CFAEdge pCallEdge,
      SvLibProcedureDeclaration pProcedureDeclaration,
      ImmutableMap.Builder<CFAEdge, PointerTargetSet> pEdgeToPointerTargetSet)
      throws CPATransferException, InterruptedException {
    ImmutableList<CExpression> pCParameters = pFunctionCall.getParameterExpressions();
    PathFormula context =
        pathFormulaManager.makeEmptyPathFormulaWithContext(
            SSAMap.emptySSAMap(), getPtsForEdge(pCallEdge, pEdgeToPointerTargetSet));

    // A call can have more arguments than the called procedure has parameters, for a variadic
    // function or for a function of the compiler such as __builtin_fpclassify, whose declaration
    // has none. Only the arguments that the procedure declares are passed, the others cannot be
    // referred to by its body.
    ImmutableList<CExpression> arguments =
        pCParameters.subList(
            0, Math.min(pCParameters.size(), pProcedureDeclaration.getParameters().size()));

    ImmutableList.Builder<SvLibTerm> callInputParameterCollector = ImmutableList.builder();
    ImmutableList.Builder<SvLibTerm> constraintCollector = ImmutableList.builder();
    for (int i = 0; i < arguments.size(); i++) {
      CExpression inputParameter = arguments.get(i);
      // The argument is converted to the type that the function declares for the parameter, like
      // the implicit conversion of C does, for example for the call fmodf(x, 2) of a function that
      // takes two floats.
      RightHandSideTerm argument =
          pathFormulaManager.rightHandSideToFormula(
              context, inputParameter, getTypeOfArgument(pFunctionCall, i), pCallEdge);
      SvLibTerm term = formulaManager.visit(argument.term(), formulaToSvLibVisitor);

      SvLibType parameterType = pProcedureDeclaration.getParameters().get(i).getType();
      if (!term.getExpressionType().equals(parameterType)) {
        // A C expression that is used as a value always has a numeric type, but the formula for it
        // can still be a Boolean one, for example for a comparison.
        term = castBooleanArgumentViaITE(term, parameterType, pProcedureDeclaration);
      }
      callInputParameterCollector.add(term);

      if (!formulaManager.getBooleanFormulaManager().isTrue(argument.constraints())) {
        constraintCollector.add(
            formulaManager.visit(argument.constraints(), formulaToSvLibVisitor));
      }
    }
    return new InputParameters(callInputParameterCollector.build(), constraintCollector.build());
  }

  /**
   * The type that the called function declares for the argument at the given position, or the type
   * of the argument itself if the function does not declare it, as for a variadic function.
   */
  private CType getTypeOfArgument(CFunctionCallExpression pFunctionCall, int pPosition) {
    CFunctionDeclaration declaration = pFunctionCall.getDeclaration();
    CType type =
        declaration != null && pPosition < declaration.getParameters().size()
            ? declaration.getParameters().get(pPosition).getType()
            : pFunctionCall.getParameterExpressions().get(pPosition).getExpressionType();
    if (type.getCanonicalType() instanceof CArrayType arrayType) {
      // C passes an array as a pointer to its first element, and the procedure declares it as one.
      return new CPointerType(CTypeQualifiers.NONE, arrayType.getType());
    }
    return type;
  }

  /**
   * The terms for the arguments of a procedure call, together with the constraints (for example
   * axioms for bitwise operations) that were created while building them and that have to be
   * assumed before the call.
   */
  private record InputParameters(
      ImmutableList<SvLibTerm> terms, ImmutableList<SvLibTerm> constraints) {}

  /**
   * Prepend the assumption of the constraints of the arguments of a procedure call to the given
   * statement, if there are any.
   */
  private SvLibStatement withAssumedConstraints(
      SvLibStatement pStatement, ImmutableList<SvLibTerm> pConstraints) {
    if (pConstraints.isEmpty()) {
      return pStatement;
    }
    SvLibTerm constraint =
        pConstraints.size() == 1
            ? pConstraints.getFirst()
            : new SvLibSymbolApplicationTerm(
                new SvLibIdTerm(
                    SmtLibTheoryDeclarations.boolConjunction(pConstraints.size()),
                    FileLocation.DUMMY),
                pConstraints,
                FileLocation.DUMMY);
    return new SvLibSequenceStatement(
        ImmutableList.of(
            new SvLibAssumeStatement(
                FileLocation.DUMMY, constraint, ImmutableList.of(), ImmutableList.of()),
            pStatement),
        FileLocation.DUMMY,
        ImmutableList.of(),
        ImmutableList.of());
  }

  private SvLibTerm castBooleanArgumentViaITE(
      SvLibTerm pTerm, SvLibType pParameterType, SvLibProcedureDeclaration pProcedureDeclaration) {
    SvLibType argumentType = pTerm.getExpressionType();
    if (!argumentType.equals(pParameterType)
        && argumentType.equals(SvLibSmtLibPredefinedType.BOOL)
        && (pParameterType.equals(SvLibSmtLibPredefinedType.INT)
            || pParameterType instanceof SvLibSmtLibBitVectorType)) {

      SvLibTerm oneTerm = createNumericConstant(BigInteger.ONE, pParameterType);
      SvLibTerm zeroTerm = createNumericConstant(BigInteger.ZERO, pParameterType);

      return new SvLibSymbolApplicationTerm(
          new SvLibIdTerm(SmtLibTheoryDeclarations.ite(pParameterType), FileLocation.DUMMY),
          ImmutableList.of(pTerm, oneTerm, zeroTerm),
          FileLocation.DUMMY);
    }
    throw new IllegalArgumentException(
        "Failed to convert mismatched types! Type of argument "
            + argumentType
            + " does not match type "
            + pParameterType
            + " expected by the declaration of the procedure "
            + pProcedureDeclaration.getProcedureName());
  }

  /**
   * Create the SV-LIB term for the given numeric constant in the given type.
   *
   * <p>The constant is built directly instead of being recovered from the formula of a dummy edge,
   * because the shape of such a formula depends on the simplifications the SMT solver applies and
   * is therefore not the same for every solver.
   */
  private SvLibTerm createNumericConstant(BigInteger pValue, SvLibType pType) {
    if (pType.equals(SvLibSmtLibPredefinedType.INT)) {
      return new SvLibIntegerConstantTerm(pValue, FileLocation.DUMMY);
    } else if (pType instanceof SvLibSmtLibBitVectorType bitVectorType) {
      return new SvLibBitVectorConstantTerm(pValue, bitVectorType.getSize(), FileLocation.DUMMY);
    }
    throw new UnsupportedOperationException(
        "Cannot create the numeric constant " + pValue + " for the non-numeric type " + pType);
  }

  /**
   * Transform the formula of an edge whose assignment target is not represented by a variable of
   * the generated SV-LIB program, but by the array that models the heap.
   *
   * <p>{@link CtoFormulaConverter} builds such an assignment as a formula that relates the old and
   * the new instance of that array, and not as a pair of a left-hand and a right-hand side (the
   * update of the array is created deep inside the encoding of the heap), so the two sides have to
   * be recovered from the formula here.
   *
   * <p>The formula is a conjunction of the equalities that define the new values of the assigned
   * memory and of conditions that only have to hold, for example the constraints on the addresses
   * of the variables. The conjunction is flattened, because how deeply it is nested depends on the
   * SMT solver: Z3 creates one n-ary conjunction where MathSAT5 creates nested binary ones. Whether
   * a conjunct is an assignment is not decided by its shape alone: it also has to define the value
   * of a variable for which this edge created a new instance, which is read from the SSA indices.
   * An equality between two variables that the edge does not assign is a condition and not an
   * assignment.
   */
  private SvLibStatement transformEdgeFormula(
      CFAEdge pEdge, SSAMap pSsaBeforeEdge, PathFormula pEdgeFormula) {
    // The conjunction of the formula is flattened before its parts are transformed, because the
    // transformation of a formula walks it recursively and the formula of an edge of a large
    // function can be a chain of conjunctions that is deeper than the stack allows.
    ImmutableList.Builder<SvLibTerm> transformedConjuncts = ImmutableList.builder();
    for (BooleanFormula conjunct : flattenConjunctionOfFormula(pEdgeFormula.getFormula())) {
      transformedConjuncts.add(formulaManager.visit(conjunct, formulaToSvLibVisitor));
    }
    ImmutableList<SvLibTerm> transformedTerms = transformedConjuncts.build();

    // Edges without any effect, such as the declaration edge of "int x;", have no statement.
    if (FluentIterable.from(transformedTerms)
        .allMatch(
            term ->
                term instanceof SvLibBooleanConstantTerm booleanConstant
                    && booleanConstant.getValue())) {
      return SvLibSequenceStatement.emptySequence();
    }

    ImmutableSet<String> assignedVariables =
        getAssignedVariables(pSsaBeforeEdge, pEdgeFormula.getSsa());
    ImmutableList.Builder<SvLibStatement> statements = ImmutableList.builder();
    ImmutableList.Builder<SvLibTerm> conditions = ImmutableList.builder();

    for (SvLibTerm conjunct :
        FluentIterable.from(transformedTerms).transformAndConcat(this::flattenConjunction)) {
      Optional<SvLibIdTerm> assignedTo = getAssignedVariable(conjunct, assignedVariables);
      if (assignedTo.isPresent()) {
        statements.add(
            createAssignmentStatement(
                assignedTo.orElseThrow(),
                getOtherSideOfEquality(
                    (SvLibSymbolApplicationTerm) conjunct, assignedTo.orElseThrow()),
                pEdge.getPredecessor().getFunctionName()));
      } else {
        conditions.add(conjunct);
      }
    }

    ImmutableList<SvLibTerm> conditionTerms = conditions.build();
    if (!conditionTerms.isEmpty()) {
      // The conditions constrain the values that the assignments create, so they are assumed
      // afterwards.
      statements.add(
          new SvLibAssumeStatement(
              FileLocation.DUMMY, conjoin(conditionTerms), ImmutableList.of(), ImmutableList.of()));
    }

    ImmutableList<SvLibStatement> createdStatements = statements.build();
    if (createdStatements.size() == 1) {
      return createdStatements.getFirst();
    }
    return new SvLibSequenceStatement(
        createdStatements, FileLocation.DUMMY, ImmutableList.of(), ImmutableList.of());
  }

  /**
   * The variables that the edge between the two given SSA maps gave a new instance, i.e. those
   * whose value the edge can change.
   *
   * <p>The formula of an edge is built with an empty map, so every variable that the edge mentions
   * has a new instance afterwards, whether the edge writes it or only reads it: the index cannot
   * tell the two apart, because the first instance of a variable that is written without being read
   * has the same index as the instance that a read creates. Which variables an edge writes is
   * therefore over-approximated here, and {@link #getAssignedVariable} looks for the shape of an
   * assignment among the conjuncts of the formula to find the ones that really are assignments.
   */
  private ImmutableSet<String> getAssignedVariables(SSAMap pBeforeEdge, SSAMap pAfterEdge) {
    ImmutableSet.Builder<String> assignedVariables = ImmutableSet.builder();
    for (String variable : pAfterEdge.allVariables()) {
      if (pAfterEdge.getIndex(variable) > pBeforeEdge.getIndex(variable)) {
        assignedVariables.add(variable);
      }
    }
    return assignedVariables.build();
  }

  /**
   * If the given conjunct is an equality that defines the new value of one of the given assigned
   * variables, the term for that variable.
   */
  private Optional<SvLibIdTerm> getAssignedVariable(
      SvLibTerm pConjunct, ImmutableSet<String> pAssignedVariables) {
    if (!(pConjunct instanceof SvLibSymbolApplicationTerm equality)
        || !equality.getSymbol().getName().equals("=")
        || equality.getTerms().size() != 2) {
      return Optional.empty();
    }
    // Which side of the equality holds the assigned variable depends on the simplifications of the
    // used SMT solver, which for example writes the assignment of the result of an allocation as
    // "(= (ite (not (= malloc 0)) address 0) p)".
    for (SvLibRelationalTerm side : equality.getTerms()) {
      if (side instanceof SvLibIdTerm assignedTo
          && (assignedTo.getDeclaration() instanceof SvLibVariableDeclaration
              || assignedTo.getDeclaration() instanceof SvLibParameterDeclaration)
          && pAssignedVariables.contains(
              unescapeVariableName(assignedTo.getDeclaration().getQualifiedName()))) {
        return Optional.of(assignedTo);
      }
    }
    return Optional.empty();
  }

  /** The side of the given equality that is not the given one. */
  private SvLibTerm getOtherSideOfEquality(
      SvLibSymbolApplicationTerm pEquality, SvLibIdTerm pOneSide) {
    for (SvLibRelationalTerm side : pEquality.getTerms()) {
      if (side != pOneSide && side instanceof SvLibTerm term) {
        return term;
      }
    }
    throw new UnsupportedOperationException(
        "The equality " + pEquality.toASTString() + " has only one side");
  }

  /**
   * The name of a variable as it appears in an {@link SSAMap}. Variables whose name is not a valid
   * SMT-LIB symbol are quoted with vertical bars in the generated script.
   */
  private String unescapeVariableName(String pName) {
    if (pName.length() >= 2 && pName.startsWith("|") && pName.endsWith("|")) {
      return pName.substring(1, pName.length() - 1);
    }
    return pName;
  }

  /**
   * Split the given term into the conjuncts of its top-level conjunction, flattening nested
   * conjunctions. Terms that are not conjunctions are returned unchanged as the only conjunct.
   */
  private ImmutableList<SvLibTerm> flattenConjunction(SvLibTerm pTerm) {
    if (pTerm instanceof SvLibSymbolApplicationTerm applicationTerm
        && applicationTerm.getSymbol().getName().equals("and")) {
      ImmutableList.Builder<SvLibTerm> conjuncts = ImmutableList.builder();
      for (SvLibTerm conjunct : applicationTerm.getTerms()) {
        conjuncts.addAll(flattenConjunction(conjunct));
      }
      return conjuncts.build();
    }
    return ImmutableList.of(pTerm);
  }

  /**
   * The conjuncts of the given formula, in the order in which they appear in it.
   *
   * <p>The conjunction is flattened without recursion, so that a formula that is a long chain of
   * conjunctions can be taken apart before the parts are transformed.
   */
  private ImmutableList<BooleanFormula> flattenConjunctionOfFormula(BooleanFormula pFormula) {
    ImmutableList.Builder<BooleanFormula> conjuncts = ImmutableList.builder();
    Deque<BooleanFormula> waiting = new ArrayDeque<>();
    waiting.push(pFormula);
    while (!waiting.isEmpty()) {
      BooleanFormula formula = waiting.pop();
      Optional<List<BooleanFormula>> operands = getOperandsOfConjunction(formula);
      if (operands.isPresent()) {
        // The operands are pushed in reverse order, so that they are taken in their own order.
        for (BooleanFormula operand : Lists.reverse(operands.orElseThrow())) {
          waiting.push(operand);
        }
      } else {
        conjuncts.add(formula);
      }
    }
    return conjuncts.build();
  }

  /** The operands of the given formula, if it is a conjunction. */
  private Optional<List<BooleanFormula>> getOperandsOfConjunction(BooleanFormula pFormula) {
    return formulaManager
        .getBooleanFormulaManager()
        .visit(
            pFormula,
            new DefaultBooleanFormulaVisitor<Optional<List<BooleanFormula>>>() {
              @Override
              protected Optional<List<BooleanFormula>> visitDefault() {
                return Optional.empty();
              }

              @Override
              public Optional<List<BooleanFormula>> visitAnd(List<BooleanFormula> pOperands) {
                return Optional.of(pOperands);
              }
            });
  }

  /** The conjunction of the given terms, or the single term if there is only one. */
  private SvLibTerm conjoin(ImmutableList<SvLibTerm> pTerms) {
    Verify.verify(!pTerms.isEmpty());
    if (pTerms.size() == 1) {
      return pTerms.getFirst();
    }
    return new SvLibSymbolApplicationTerm(
        new SvLibIdTerm(
            SmtLibTheoryDeclarations.boolConjunction(pTerms.size()), FileLocation.DUMMY),
        pTerms,
        FileLocation.DUMMY);
  }

  private SvLibAssignmentStatement createAssignmentStatement(
      SvLibIdTerm pIdTerm, SvLibTerm pAssignedTerm, String pFunctionName) {
    SvLibSimpleParsingDeclaration assignedToAsDeclaration =
        new SvLibParsingParameterDeclaration(
            FileLocation.DUMMY,
            pIdTerm.getDeclaration().getType(),
            pIdTerm.getDeclaration().getName(),
            pFunctionName);

    return new SvLibAssignmentStatement(
        ImmutableMap.of(assignedToAsDeclaration, pAssignedTerm),
        FileLocation.DUMMY,
        ImmutableList.of(),
        ImmutableList.of());
  }

  /** An assumption that never holds, which says that the given node cannot be left. */
  private SvLibAssumeStatement createAssumptionThatNeverHolds(CFANode pNode) {
    return new SvLibAssumeStatement(
        FileLocation.DUMMY,
        new SvLibBooleanConstantTerm(false, FileLocation.DUMMY),
        ImmutableList.of(),
        ImmutableList.of(
            new SvLibTagReference(
                "CFANodeWithoutSuccessor_N" + pNode.getNodeNumber(), FileLocation.DUMMY)));
  }

  private SvLibAssumeStatement encodeTerminationNode(CFATerminationNode pTerminationNode) {
    return new SvLibAssumeStatement(
        FileLocation.DUMMY,
        new SvLibBooleanConstantTerm(false, FileLocation.DUMMY),
        ImmutableList.of(),
        ImmutableList.of(
            new SvLibTagReference(
                "CFATerminationNode_N" + pTerminationNode.getNodeNumber(), FileLocation.DUMMY)));
  }

  private SvLibGotoStatement createGotoStatement(CFANode pGotoTarget) {
    return new SvLibGotoStatement(
        FileLocation.DUMMY, ImmutableList.of(), ImmutableList.of(), pGotoTarget.toString());
  }

  private SvLibSequenceStatement createSequenceStatement(
      ImmutableListMultimap<CFANode, SvLibStatement> pSequenceBody, String pProcedureName) {
    ImmutableList.Builder<SvLibStatement> statementList = ImmutableList.builder();
    for (CFANode key : pSequenceBody.keySet()) {
      pSequenceBody.get(key).forEach(statementList::add);
    }
    return new SvLibSequenceStatement(
        statementList.build(),
        FileLocation.DUMMY,
        ImmutableList.of(),
        ImmutableList.of(new SvLibTagReference(pProcedureName, FileLocation.DUMMY)));
  }

  private String getOriginalNameOfInputParameterDummy(String pDummyName) {
    if (pDummyName.startsWith(CToSvLibTransformationConstants.INPUT_VAR_DUMMY_PREFIX)) {
      // return the name without the prefix
      return pDummyName.substring(CToSvLibTransformationConstants.INPUT_VAR_DUMMY_PREFIX.length());
    }
    throw new IllegalArgumentException(
        "Cannot remove prefix "
            + CToSvLibTransformationConstants.INPUT_VAR_DUMMY_PREFIX
            + " from name "
            + pDummyName);
  }
}
