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
import java.math.BigInteger;
import java.util.List;
import java.util.NavigableSet;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SmtLibTheoryDeclarations;
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
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibTagReference;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
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
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibBitVectorType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibPredefinedType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibType;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.EdgeCollectingCFAVisitor;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter.RightHandSideTerm;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerBase;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.svlibwitnessexport.FormulaToSvLibVisitor;

class CToSvLibTransformation {
  private final CFA cfa;
  private final FormulaManagerView formulaManager;
  private final PathFormulaManager pathFormulaManager;
  private final FormulaToSvLibVisitor formulaToSvLibVisitor;

  private final SvLibCurrentScope scope;

  CToSvLibTransformation(
      CFA pCFA,
      FormulaManagerView pFormulaManager,
      PathFormulaManager pPathFormulaManager,
      FormulaToSvLibVisitor pFormulaToSvLibVisitor,
      SvLibCurrentScope pCurrentScope) {
    cfa = pCFA;
    formulaManager = pFormulaManager;
    pathFormulaManager = pPathFormulaManager;
    formulaToSvLibVisitor = pFormulaToSvLibVisitor;
    scope = pCurrentScope;
  }

  SvLibSequenceStatement transformFunction(@NonNull CFunctionEntryNode pEntryNode)
      throws CPATransferException, InterruptedException {
    SvLibProcedureDeclaration procedureDeclaration =
        scope.getProcedureDeclaration(pEntryNode.getFunctionName());
    String procedureName = procedureDeclaration.getProcedureName();
    ImmutableListMultimap.Builder<CFANode, SvLibStatement> statementCollector =
        ImmutableListMultimap.builder();
    ImmutableMap.Builder<CFAEdge, PointerTargetSet> edgeToPointerTargetSet = ImmutableMap.builder();

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
        if (statementEdge.getStatement() instanceof CFunctionCall) {
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

  private SvLibStatement handleAssignment(CFAEdge pEdge, SvLibTerm pTransformedTerm) {
    // For some edges without assignment, such as a declarationEdge for int x;, the
    // pTransformedTerm is a SvLibBooleanConstantTerm with the value true, and no
    // SvLibAssignmentStatement should be returned.
    if (pTransformedTerm instanceof SvLibBooleanConstantTerm booleanConstant
        && booleanConstant.getValue()) {
      return SvLibSequenceStatement.emptySequence();

    } else if (pTransformedTerm instanceof SvLibSymbolApplicationTerm symbolApplicationTerm
        && symbolApplicationTerm.getSymbol().getName().equals("=")
        && symbolApplicationTerm.getTerms().size() == 2) {

      ImmutableList<SvLibTerm> termsList = ImmutableList.copyOf(symbolApplicationTerm.getTerms());
      SvLibTerm assignedTo = termsList.getFirst();
      SvLibTerm termToAssign = termsList.get(1);

      if (assignedTo instanceof SvLibIdTerm idTerm
          && (idTerm.getDeclaration() instanceof SvLibVariableDeclaration
              || idTerm.getDeclaration() instanceof SvLibParameterDeclaration)) {
        return createAssignmentStatement(
            idTerm, termToAssign, pEdge.getPredecessor().getFunctionName());
      }
    } else if (pTransformedTerm instanceof SvLibSymbolApplicationTerm outerTerm
        && outerTerm.getSymbol().getName().equals("and")
        && outerTerm.getTerms().size() == 2
        && outerTerm.getTerms().getFirst() instanceof SvLibSymbolApplicationTerm innerTerm
        && innerTerm.getSymbol().getName().equals("=")
        && innerTerm.getTerms().size() == 2) {

      SvLibTerm assignedTo = innerTerm.getTerms().getFirst();
      SvLibTerm assignedTerm = innerTerm.getTerms().get(1);
      SvLibTerm assumeTerm = outerTerm.getTerms().get(1);

      if (assignedTo instanceof SvLibIdTerm idTerm
          && (idTerm.getDeclaration() instanceof SvLibVariableDeclaration
              || idTerm.getDeclaration() instanceof SvLibParameterDeclaration)) {

        SvLibAssignmentStatement assignmentStatement =
            createAssignmentStatement(
                idTerm, assignedTerm, pEdge.getPredecessor().getFunctionName());
        SvLibAssumeStatement assumeStatement =
            new SvLibAssumeStatement(
                FileLocation.DUMMY, assumeTerm, ImmutableList.of(), ImmutableList.of());

        return new SvLibSequenceStatement(
            ImmutableList.of(assignmentStatement, assumeStatement),
            FileLocation.DUMMY,
            ImmutableList.of(),
            ImmutableList.of());
      }
    } else if (pTransformedTerm instanceof SvLibSymbolApplicationTerm outerTerm
        && outerTerm.getSymbol().getName().equals("and")
        && outerTerm.getTerms().size() == 2
        && outerTerm.getTerms().getFirst() instanceof SvLibSymbolApplicationTerm innerTerm
        && innerTerm.getSymbol().getName().equals("and")
        && innerTerm.getTerms().size() == 2) {

      ImmutableList<SvLibStatement> statements =
          handleAssignmentForNestedTerm(
              outerTerm, innerTerm, pEdge.getPredecessor().getFunctionName());
      return new SvLibSequenceStatement(
          statements, FileLocation.DUMMY, ImmutableList.of(), ImmutableList.of());

    } else if (pTransformedTerm instanceof SvLibSymbolApplicationTerm term
        && term.getSymbol().getName().equals("and")
        && (term.getTerms().size() == 3
            || term.getTerms().size() == 4
            || term.getTerms().size() == 5)) {
      return new SvLibAssumeStatement(
          FileLocation.DUMMY, term, ImmutableList.of(), ImmutableList.of());
    }
    throw new UnsupportedOperationException(
        "Failed to handle assignment for edge "
            + pEdge
            + " and transformed term "
            + pTransformedTerm.toASTString());
  }

  private ImmutableList<SvLibStatement> handleAssignmentForNestedTerm(
      SvLibSymbolApplicationTerm pOuterTerm,
      SvLibSymbolApplicationTerm pInnerTerm,
      String pFunctionName) {
    // extract all equality terms from the innerSymbolApplicationTerm
    ImmutableList.Builder<SvLibSymbolApplicationTerm> assignmentTermsCollector =
        ImmutableList.builder();
    pInnerTerm.accept(new CToSvLibTransformationTermVisitor(assignmentTermsCollector));

    // create assignment statements for each collected term
    ImmutableList<SvLibSymbolApplicationTerm> assignmentTerms = assignmentTermsCollector.build();
    ImmutableList.Builder<SvLibStatement> statementsCollector = ImmutableList.builder();
    for (SvLibSymbolApplicationTerm symbolApplicationTerm : assignmentTerms) {
      if (symbolApplicationTerm.getTerms().getFirst() instanceof SvLibIdTerm idTerm
          && (idTerm.getDeclaration() instanceof SvLibVariableDeclaration
              || idTerm.getDeclaration() instanceof SvLibParameterDeclaration)) {
        statementsCollector.add(
            createAssignmentStatement(
                idTerm, symbolApplicationTerm.getTerms().get(1), pFunctionName));
      }
    }
    // create an assumeStatement for the conditions in the outerTerm
    statementsCollector.add(
        new SvLibAssumeStatement(
            FileLocation.DUMMY,
            pOuterTerm.getTerms().get(1),
            ImmutableList.of(),
            ImmutableList.of()));

    return statementsCollector.build();
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
    if (cfa.edges().contains(pEdge)) {
      pEdgeToPointerTargetSet.put(pEdge, edgeFormula.getPointerTargetSet());
    }

    Optional<CAssignment> assignment = getAssignmentOfEdge(pEdge);
    if (assignment.isPresent()
        && assignment.orElseThrow().getLeftHandSide() instanceof CIdExpression lhs
        && isRepresentedByOwnVariable(lhs, pointerTargetSetBeforeEdge)) {
      return createAssignmentStatement(
          lhs, assignment.orElseThrow().getRightHandSide(), contextBeforeEdge, pEdge);
    }

    return handleAssignment(
        pEdge, formulaManager.visit(edgeFormula.getFormula(), formulaToSvLibVisitor));
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
    if (pStatementEdge.getStatement() instanceof CFunctionCall functionCall
        && functionCall instanceof CFunctionCallAssignmentStatement callAssignmentStatement
        && (callAssignmentStatement.getLeftHandSide() instanceof CArraySubscriptExpression
            || callAssignmentStatement.getLeftHandSide() instanceof CFieldReference)) {
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
              functionCallStatement
                  .getFunctionCallExpression()
                  .getFunctionNameExpression()
                  .toASTString());

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
        && (assignment.getLeftHandSide() instanceof CArraySubscriptExpression
            || assignment.getLeftHandSide() instanceof CFieldReference)) {
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

  private SvLibProcedureCallStatement createProcedureCallStatement(
      CFAEdge pCallEdge,
      CFunctionCallAssignmentStatement pFunctionCallAssignmentStatement,
      CIdExpression pLhsIdExpression,
      ImmutableMap.Builder<CFAEdge, PointerTargetSet> pEdgeToPointerTargetSet)
      throws CPATransferException, InterruptedException {
    SvLibProcedureDeclaration calledProcedure =
        scope.getProcedureDeclaration(
            pFunctionCallAssignmentStatement
                .getRightHandSide()
                .getFunctionNameExpression()
                .toASTString());

    return new SvLibProcedureCallStatement(
        FileLocation.DUMMY,
        ImmutableList.of(),
        ImmutableList.of(),
        calledProcedure,
        transformInputParameters(
            pFunctionCallAssignmentStatement.getRightHandSide().getParameterExpressions(),
            pCallEdge,
            calledProcedure,
            pEdgeToPointerTargetSet),
        ImmutableList.of(
            scope.getVariableForQualifiedName(
                pLhsIdExpression.getDeclaration().getQualifiedName())));
  }

  private SvLibProcedureCallStatement createProcedureCallStatementWithDummyReturn(
      CFAEdge pCallEdge,
      CFunctionCallStatement pFunctionCallStatement,
      SvLibProcedureDeclaration pCalledProcedure,
      ImmutableMap.Builder<CFAEdge, PointerTargetSet> pEdgeToPointerTargetSet)
      throws CPATransferException, InterruptedException {

    ImmutableList.Builder<SvLibSimpleParsingDeclaration> returnVariableDummies =
        ImmutableList.builder();
    for (SvLibParsingParameterDeclaration parsingParameterDeclaration :
        pCalledProcedure.getReturnValues()) {
      SvLibType returnType = parsingParameterDeclaration.getType();
      String returnDummyName =
          (returnType instanceof SvLibSmtLibBitVectorType bitVectorType)
              ? CToSvLibTransformationConstants.RETURN_VAR_DUMMY_PREFIX
                  + "bv"
                  + bitVectorType.getSize()
              : CToSvLibTransformationConstants.RETURN_VAR_DUMMY_PREFIX + returnType;
      SvLibSimpleParsingDeclaration returnDummyVariable = scope.getVariable(returnDummyName);
      returnVariableDummies.add(returnDummyVariable);
    }

    return new SvLibProcedureCallStatement(
        FileLocation.DUMMY,
        ImmutableList.of(),
        ImmutableList.of(),
        pCalledProcedure,
        transformInputParameters(
            pFunctionCallStatement.getFunctionCallExpression().getParameterExpressions(),
            pCallEdge,
            pCalledProcedure,
            pEdgeToPointerTargetSet),
        returnVariableDummies.build());
  }

  private SvLibSequenceStatement handleReturnValueAssignmentToHeap(
      CFAEdge pCallEdge,
      CFunctionCallAssignmentStatement pAssignmentStatement,
      ImmutableMap.Builder<CFAEdge, PointerTargetSet> pEdgeToPointerTargetSet)
      throws CPATransferException, InterruptedException {

    String calledFunctionName =
        pAssignmentStatement.getRightHandSide().getFunctionNameExpression().toASTString();
    SvLibProcedureDeclaration calledProcedure = scope.getProcedureDeclaration(calledFunctionName);

    String tmpVariableQualifiedName = constructTmpVariableName(calledProcedure, pCallEdge);
    SvLibSimpleParsingDeclaration variable =
        scope.getVariableForQualifiedName(tmpVariableQualifiedName);

    SvLibProcedureCallStatement callStatement =
        new SvLibProcedureCallStatement(
            FileLocation.DUMMY,
            ImmutableList.of(),
            ImmutableList.of(),
            calledProcedure,
            transformInputParameters(
                pAssignmentStatement.getRightHandSide().getParameterExpressions(),
                pCallEdge,
                calledProcedure,
                pEdgeToPointerTargetSet),
            ImmutableList.of(variable));

    SvLibSymbolApplicationTerm symbolApplicationTerm =
        getTmpTerm(
            tmpVariableQualifiedName,
            pAssignmentStatement.getLeftHandSide(),
            pCallEdge,
            pEdgeToPointerTargetSet);
    String functionName = pCallEdge.getPredecessor().getFunctionName();
    // TODO
    SvLibAssignmentStatement assignmentStatement =
        createAssignmentStatement(
            (SvLibIdTerm) symbolApplicationTerm.getTerms().getFirst(),
            symbolApplicationTerm.getTerms().get(1),
            functionName);

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
        + CToSvLibTransformationConstants.TMP_VAR_ASSIGNMENT
        + returnValueType;
  }

  private SvLibSymbolApplicationTerm getTmpTerm(
      String tmpVariableQualifiedName,
      CLeftHandSide pCLeftHandSide,
      CFAEdge pEdge,
      ImmutableMap.Builder<CFAEdge, PointerTargetSet> pEdgeToPointerTargetSet)
      throws CPATransferException, InterruptedException {

    CVariableDeclaration tmpVariableDeclaration =
        new CVariableDeclaration(
            FileLocation.DUMMY,
            false,
            CStorageClass.AUTO,
            pCLeftHandSide.getExpressionType(),
            tmpVariableQualifiedName,
            tmpVariableQualifiedName,
            tmpVariableQualifiedName,
            null);

    CIdExpression variableExpression =
        new CIdExpression(
            FileLocation.DUMMY,
            pCLeftHandSide.getExpressionType(),
            tmpVariableQualifiedName,
            tmpVariableDeclaration);

    CExpressionAssignmentStatement tmpCAssignment =
        new CExpressionAssignmentStatement(FileLocation.DUMMY, pCLeftHandSide, variableExpression);

    CStatementEdge transformationDummyEdge_tmpAssignmentStatement =
        new CStatementEdge(
            "", tmpCAssignment, FileLocation.DUMMY, pEdge.getPredecessor(), pEdge.getPredecessor());
    SvLibTerm svLibTerm =
        transformEdgeToSvLibTerm(
            transformationDummyEdge_tmpAssignmentStatement, pEdgeToPointerTargetSet);
    return (SvLibSymbolApplicationTerm) svLibTerm;
  }

  private ImmutableList<SvLibTerm> transformInputParameters(
      ImmutableList<CExpression> pCParameters,
      CFAEdge pCallEdge,
      SvLibProcedureDeclaration pProcedureDeclaration,
      ImmutableMap.Builder<CFAEdge, PointerTargetSet> pEdgeToPointerTargetSet)
      throws CPATransferException, InterruptedException {
    ImmutableList.Builder<SvLibTerm> callInputParameterCollector = ImmutableList.builder();
    for (int i = 0; i < pCParameters.size(); i++) {
      CExpression inputParameter = pCParameters.get(i);
      CAssumeEdge transformationDummyEdge =
          new CAssumeEdge(
              inputParameter.toASTString(),
              FileLocation.DUMMY,
              pCallEdge.getPredecessor(),
              pCallEdge.getPredecessor(),
              inputParameter,
              true);
      SvLibTerm term = transformEdgeToSvLibTerm(transformationDummyEdge, pEdgeToPointerTargetSet);

      if ((inputParameter instanceof CIdExpression || inputParameter instanceof CLiteralExpression)
          && term instanceof SvLibSymbolApplicationTerm symbolApplicationTerm
          && symbolApplicationTerm.getSymbol().getName().equals("not")
          && symbolApplicationTerm.getTerms().size() == 1
          && symbolApplicationTerm.getTerms().getFirst()
              instanceof SvLibSymbolApplicationTerm innerTerm
          && innerTerm.getSymbol().getName().equals("=")) {

        term = innerTerm.getTerms().getFirst();

      } else if (inputParameter instanceof CPointerExpression
          && term instanceof SvLibSymbolApplicationTerm outerTerm
          && outerTerm.getSymbol().getName().equals("not")
          && outerTerm.getTerms().size() == 1
          && outerTerm.getTerms().getFirst() instanceof SvLibSymbolApplicationTerm middleTerm
          && middleTerm.getSymbol().getName().equals("=")
          && middleTerm.getTerms().size() == 2
          && middleTerm.getTerms().get(1) instanceof SvLibIntegerConstantTerm integerConstantTerm
          && integerConstantTerm.getValue().equals(BigInteger.ZERO)
          && middleTerm.getTerms().getFirst() instanceof SvLibSymbolApplicationTerm innerTerm
          && innerTerm.getSymbol().getName().equals("select")
          && innerTerm.getTerms().size() == 2) {

        term = innerTerm.getTerms().get(1);

      } else if (inputParameter instanceof CBinaryExpression
          && term instanceof SvLibSymbolApplicationTerm symbolApplicationTerm
          && symbolApplicationTerm.getSymbol().getName().equals("not")
          && symbolApplicationTerm.getTerms().size() == 1
          && symbolApplicationTerm.getTerms().getFirst()
              instanceof SvLibSymbolApplicationTerm innerTerm
          && innerTerm.getSymbol().getName().equals("=")
          && innerTerm.getTerms().size() == 2
          && innerTerm.getTerms().get(1) instanceof SvLibIntegerConstantTerm integerConstantTerm
          && integerConstantTerm.getValue().equals(BigInteger.ZERO)
          && innerTerm.getTerms().getFirst()
              instanceof SvLibSymbolApplicationTerm actualSymbolApplicationTerm) {

        term = actualSymbolApplicationTerm;

      } else if (inputParameter instanceof CBinaryExpression inputBinaryExpression
          && term instanceof SvLibSymbolApplicationTerm symbolApplicationTerm
          && symbolApplicationTerm.getSymbol().getDeclaration()
              instanceof SvLibFunctionDeclaration pSvLibFunctionDeclaration
          && pSvLibFunctionDeclaration
              .getType()
              .getReturnType()
              .equals(SvLibSmtLibPredefinedType.BOOL)) {

        SvLibType parameterType = pProcedureDeclaration.getParameters().get(i).getType();
        term =
            castBooleanArgumentViaITE(
                term,
                parameterType,
                inputBinaryExpression,
                pCallEdge,
                pProcedureDeclaration,
                pEdgeToPointerTargetSet);

      } else if (inputParameter instanceof CArraySubscriptExpression
          && term instanceof SvLibSymbolApplicationTerm symbolApplicationTerm
          && symbolApplicationTerm.getSymbol().getName().equals("not")
          && symbolApplicationTerm.getTerms().size() == 1
          && symbolApplicationTerm.getTerms().getFirst()
              instanceof SvLibSymbolApplicationTerm middleTerm
          && middleTerm.getSymbol().getName().equals("=")
          && middleTerm.getTerms().size() == 2
          && middleTerm.getTerms().get(1) instanceof SvLibIntegerConstantTerm integerConstantTerm
          && integerConstantTerm.getValue().equals(BigInteger.ZERO)
          && middleTerm.getTerms().getFirst() instanceof SvLibSymbolApplicationTerm innerTerm
          && innerTerm.getSymbol().getName().equals("select")) {
        term = innerTerm;

      } else if (inputParameter instanceof CFieldReference
          && term instanceof SvLibSymbolApplicationTerm outerTerm
          && outerTerm.getSymbol().getName().equals("not")
          && outerTerm.getTerms().size() == 1
          && outerTerm.getTerms().getFirst() instanceof SvLibSymbolApplicationTerm middleTerm
          && middleTerm.getSymbol().getName().equals("=")
          && middleTerm.getTerms().size() == 2
          && middleTerm.getTerms().get(1) instanceof SvLibIntegerConstantTerm integerConstantTerm
          && integerConstantTerm.getValue().equals(BigInteger.ZERO)
          && middleTerm.getTerms().getFirst() instanceof SvLibSymbolApplicationTerm innerTerm
          && innerTerm.getSymbol().getName().equals("select")
          && innerTerm.getTerms().size() == 2) {

        term = innerTerm.getTerms().get(1);

      } else {
        throw new UnsupportedOperationException(
            "Failed to transform input "
                + inputParameter
                + " for procedure "
                + pProcedureDeclaration.getProcedureName());
      }
      callInputParameterCollector.add(term);
    }
    return callInputParameterCollector.build();
  }

  private SvLibSymbolApplicationTerm createIntegerTermsViaTransformationDummyEdge(
      CFAEdge pEdge,
      CType pCType,
      ImmutableMap.Builder<CFAEdge, PointerTargetSet> pEdgeToPointerTargetSet)
      throws CPATransferException, InterruptedException {
    CAssumeEdge transformationDummyEdge =
        new CAssumeEdge(
            "1",
            FileLocation.DUMMY,
            pEdge.getPredecessor(),
            pEdge.getPredecessor(),
            new CIntegerLiteralExpression(FileLocation.DUMMY, pCType, BigInteger.ONE),
            false);
    SvLibTerm transformedTerm =
        transformEdgeToSvLibTerm(transformationDummyEdge, pEdgeToPointerTargetSet);
    if (transformedTerm instanceof SvLibSymbolApplicationTerm symbolApplicationTerm
        && symbolApplicationTerm.getTerms().size() == 2) {
      return symbolApplicationTerm;
    }
    throw new UnsupportedOperationException(
        "Failed to generate integer constant terms via a dummy edge.");
  }

  private SvLibTerm castBooleanArgumentViaITE(
      SvLibTerm pTerm,
      SvLibType pParameterType,
      CBinaryExpression pInputParameter,
      CFAEdge pCallEdge,
      SvLibProcedureDeclaration pProcedureDeclaration,
      ImmutableMap.Builder<CFAEdge, PointerTargetSet> pEdgeToPointerTargetSet)
      throws CPATransferException, InterruptedException {
    SvLibType argumentType = pTerm.getExpressionType();
    if (!argumentType.equals(pParameterType)
        && argumentType.equals(SvLibSmtLibPredefinedType.BOOL)
        && (pParameterType.equals(SvLibSmtLibPredefinedType.INT)
            || pParameterType instanceof SvLibSmtLibBitVectorType)) {

      SvLibSymbolApplicationTerm transformedTerm =
          createIntegerTermsViaTransformationDummyEdge(
              pCallEdge, pInputParameter.getExpressionType(), pEdgeToPointerTargetSet);
      SvLibTerm oneTerm = transformedTerm.getTerms().getFirst();
      SvLibTerm zeroTerm = transformedTerm.getTerms().get(1);

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
