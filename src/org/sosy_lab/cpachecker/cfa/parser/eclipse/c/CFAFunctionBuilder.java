// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.eclipse.c;

import static com.google.common.base.Preconditions.checkState;
import static org.sosy_lab.cpachecker.cfa.CFACreationUtils.isReachableNode;

import com.google.common.base.Verify;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.eclipse.cdt.core.dom.ast.ASTNodeProperty;
import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTASMDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTBreakStatement;
import org.eclipse.cdt.core.dom.ast.IASTCaseStatement;
import org.eclipse.cdt.core.dom.ast.IASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTConditionalExpression;
import org.eclipse.cdt.core.dom.ast.IASTContinueStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement;
import org.eclipse.cdt.core.dom.ast.IASTDefaultStatement;
import org.eclipse.cdt.core.dom.ast.IASTDoStatement;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpressionList;
import org.eclipse.cdt.core.dom.ast.IASTExpressionStatement;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTGotoStatement;
import org.eclipse.cdt.core.dom.ast.IASTIfStatement;
import org.eclipse.cdt.core.dom.ast.IASTLabelStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTNullStatement;
import org.eclipse.cdt.core.dom.ast.IASTProblem;
import org.eclipse.cdt.core.dom.ast.IASTProblemDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTProblemStatement;
import org.eclipse.cdt.core.dom.ast.IASTReturnStatement;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTSwitchStatement;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTWhileStatement;
import org.eclipse.cdt.core.dom.ast.gnu.IGNUASTCompoundStatementExpression;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.util.FunctionBlock;
import org.sosy_lab.cpachecker.cfa.ast.acsl.util.StatementBlock;
import org.sosy_lab.cpachecker.cfa.ast.acsl.util.SyntacticBlock;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.parser.Parsers.EclipseCParserOptions;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.c.ASTConverter.CONDITION;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CDefaults;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CProblemType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.Pair;

/**
 * Builder to traverse AST. Known Limitations:
 *
 * <ul>
 *   <li>K&R style function definitions not implemented
 *   <li>Inlined assembler code is ignored
 * </ul>
 */
@SuppressWarnings({"InvalidBlockTag", "MissingSummary"}) // for @category
class CFAFunctionBuilder extends ASTVisitor {

  // Data structure for maintaining our scope stack in a function
  private final Deque<CFANode> locStack = new ArrayDeque<>();

  // Data structures for handling loops & else conditions
  private final Deque<CFANode> loopStartStack = new ArrayDeque<>();
  private final Deque<CFANode> loopNextStack =
      new ArrayDeque<>(); // For the node following the current if / while block
  private final Deque<CFANode> elseStack = new ArrayDeque<>();

  // Data structure for handling switch-statements
  private final Deque<CExpression> switchExprStack = new ArrayDeque<>();
  private final Deque<CFANode> switchCaseStack = new ArrayDeque<>();

  @SuppressWarnings("JdkObsolete") // ArrayDeque not possible because it does not allow null
  private final Deque<CFANode> switchDefaultStack = new LinkedList<>();

  @SuppressWarnings("JdkObsolete") // ArrayDeque not possible because it does not allow null
  private final Deque<FileLocation> switchDefaultFileLocationStack = new LinkedList<>();

  private final CBinaryExpressionBuilder binExprBuilder;

  // Data structures for handling goto
  private final Map<String, CFALabelNode> labelMap = new HashMap<>();
  private final Multimap<String, Pair<CFANode, FileLocation>> gotoLabelNeeded =
      ArrayListMultimap.create();

  // Data structures for handling function declarations
  private FunctionEntryNode cfa = null;
  private Set<CFANode> cfaNodes = null;

  // There can be global declarations in a function
  // because we move some declarations to the global scope (e.g., static variables)
  private final List<Pair<ADeclaration, String>> globalDeclarations = new ArrayList<>();

  // Data structures to collect blocks as defined by ACSL
  private final List<SyntacticBlock> blocks = new ArrayList<>();
  private final Deque<CFANode> blockStarts = new ArrayDeque<>();

  private final FunctionScope scope;
  private final ASTConverter astCreator;
  private final ParseContext parseContext;
  private final EclipseCParserOptions options;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final CheckBindingVisitor checkBinding;
  private final Sideassignments sideAssignmentStack;

  private boolean encounteredAsm = false;

  public CFAFunctionBuilder(
      EclipseCParserOptions pOptions,
      LogManagerWithoutDuplicates pLogger,
      ShutdownNotifier pShutdownNotifier,
      FunctionScope pScope,
      ParseContext pParseContext,
      MachineModel pMachine,
      String staticVariablePrefix,
      Sideassignments pSideAssignmentStack,
      CheckBindingVisitor pCheckBinding) {
    options = pOptions;
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    scope = pScope;
    astCreator =
        new ASTConverter(
            pOptions,
            pScope,
            pLogger,
            pParseContext,
            pMachine,
            staticVariablePrefix,
            pSideAssignmentStack);
    parseContext = pParseContext;
    checkBinding = pCheckBinding;
    binExprBuilder = new CBinaryExpressionBuilder(pMachine, pLogger);

    shouldVisitDeclarations = true;
    shouldVisitEnumerators = true;
    shouldVisitParameterDeclarations = true;
    shouldVisitProblems = true;
    shouldVisitStatements = true;
    shouldVisitExpressions = true;
    sideAssignmentStack = pSideAssignmentStack;
  }

  FunctionEntryNode getStartNode() {
    checkState(cfa != null);
    return cfa;
  }

  /** This method should be called after building the function's CFA. */
  Set<CFANode> getCfaNodes() {
    checkState(cfa != null);
    checkState(cfaNodes == null);
    cfaNodes = CFATraversal.dfs().collectNodesReachableFrom(cfa);
    return cfaNodes;
  }

  boolean didEncounterAsm() {
    return encounteredAsm;
  }

  List<Pair<ADeclaration, String>> getGlobalDeclarations() {
    return globalDeclarations;
  }

  List<SyntacticBlock> getBlocks() {
    return blocks;
  }

  /** This method is called after parsing and checks if we left everything clean. */
  void finish() {

    // cleanup unnecessary nodes and edges, that were created before.
    // cfaNodes were collected with with a FORWARD-search,
    // so all unnecessary nodes are only reachable with BACKWARD-search.
    // we only disconnect them from the CFA and let garbage collection do the rest
    for (CFANode node : cfaNodes) {
      for (CFAEdge edge : CFAUtils.enteringEdges(node).toList()) {
        if (!cfaNodes.contains(edge.getPredecessor())) {
          CFACreationUtils.removeEdgeFromNodes(edge);
        }
      }
    }

    assert !sideAssignmentStack.hasPreSideAssignments();
    assert !sideAssignmentStack.hasPostSideAssignments();
    assert locStack.isEmpty();
    assert loopStartStack.isEmpty();
    assert loopNextStack.isEmpty();
    assert elseStack.isEmpty();
    assert switchCaseStack.isEmpty();
    assert switchExprStack.isEmpty();
    assert gotoLabelNeeded.isEmpty();
  }

  /////////////////////////////////////////////////////////////////////////////
  // Declarations
  /////////////////////////////////////////////////////////////////////////////

  /**
   * @category declarations
   */
  @Override
  public int visit(IASTDeclaration declaration) {
    if (shutdownNotifier.shouldShutdown()) {
      return PROCESS_ABORT;
    }

    // entering Sideassignment block
    sideAssignmentStack.enterBlock();

    if (declaration.getPropertyInParent() == IASTCompositeTypeSpecifier.MEMBER_DECLARATION) {
      // This is a nested declaration, e.g. something like the "int j;" in
      // i = sizeof(struct s { int j; });
      // We do not want to parse this at this point, but unfortunately the ASTVisitor from which we
      // inherit calls us for such nested declarations.
      // I am not sure whether we should whitelist only declarations in places we expect or whether
      // we should blacklist declarations in places we don't want, so for now I choose the latter
      // because the change has less impact.
      return PROCESS_SKIP;

    } else if (declaration instanceof IASTSimpleDeclaration) {
      return handleSimpleDeclaration((IASTSimpleDeclaration) declaration);

    } else if (declaration instanceof IASTFunctionDefinition) {
      return handleFunctionDefinition((IASTFunctionDefinition) declaration);

    } else if (declaration instanceof IASTProblemDeclaration) {
      visit(((IASTProblemDeclaration) declaration).getProblem());
      return PROCESS_SKIP;

    } else if (declaration instanceof IASTASMDeclaration) {
      return ignoreASMDeclaration(declaration);

    } else {
      throw parseContext.parseError(
          "Unknown declaration type " + declaration.getClass().getSimpleName(), declaration);
    }
  }

  /**
   * @category declarations
   */
  private int handleSimpleDeclaration(final IASTSimpleDeclaration sd) {

    assert !locStack.isEmpty() : "not in a function's scope";

    CFANode prevNode = locStack.pop();

    CFANode nextNode = createEdgeForDeclaration(sd, astCreator.getLocation(sd), prevNode);

    assert nextNode != null;
    locStack.push(nextNode);

    return PROCESS_SKIP; // important to skip here, otherwise we would visit nested declarations
  }

  /**
   * This method takes a list of Declarations and adds them to the CFA. The edges are inserted after
   * startNode.
   *
   * @return the node after the last of the new declarations
   * @category declarations
   */
  private CFANode createEdgeForDeclaration(
      final IASTSimpleDeclaration sd, final FileLocation fileLocation, CFANode prevNode) {

    List<CAstNode> lst = sideAssignmentStack.getAndResetPostSideAssignments();
    assert lst.isEmpty()
        : "post side assignments should occur only on declarations,"
            + "but they occurred somewhere else and where not handled";
    final List<CDeclaration> declList = astCreator.convert(sd);
    final String rawSignature = sd.getRawSignature();

    prevNode = handleAllSideEffects(prevNode, fileLocation, rawSignature, true);

    // create one edge for every declaration
    for (CDeclaration newD : declList) {

      if (newD instanceof CVariableDeclaration) {
        // Variables are already declared by ASTConverter.
        // This is needed to handle the binding in the initializer correctly.
        // scope.registerDeclaration(newD);
        assert scope.lookupVariable(newD.getOrigName()) == newD;

        CInitializer init = ((CVariableDeclaration) newD).getInitializer();
        if (init != null) {
          init.accept(checkBinding);

          // this case has to be extra, as there should never be an initializer for labels
        } else if (newD.getType() instanceof CProblemType
            && newD.getType().toString().equals("__label__")) {

          scope.registerLocalLabel((CVariableDeclaration) newD);
          CFANode nextNode = newCFANode();
          BlankEdge blankEdge =
              new BlankEdge(
                  sd.getRawSignature(),
                  fileLocation,
                  prevNode,
                  nextNode,
                  "Local Label Declaration: " + newD.getName());
          addToCFA(blankEdge);

          prevNode = nextNode;
          prevNode =
              createEdgesForSideEffects(
                  prevNode,
                  sideAssignmentStack.getAndResetPostSideAssignments(),
                  rawSignature,
                  fileLocation);

          return prevNode;

        } else if (options.initializeAllVariables()) {
          CInitializer initializer = CDefaults.forType(newD.getType(), newD.getFileLocation());
          newD =
              new CVariableDeclaration(
                  newD.getFileLocation(),
                  newD.isGlobal(),
                  ((CVariableDeclaration) newD).getCStorageClass(),
                  newD.getType(),
                  newD.getName(),
                  newD.getOrigName(),
                  newD.getQualifiedName(),
                  initializer);
        }

      } else if (newD instanceof CComplexTypeDeclaration) {
        scope.registerTypeDeclaration((CComplexTypeDeclaration) newD);

        // function declarations in local scope are no problem as long as they
        // do not have a body
        // if the function is already declared it will not be redeclared
      } else if (newD instanceof CFunctionDeclaration) {
        if (scope.lookupFunction(newD.getName()) == null) {
          scope.registerLocalFunction((CFunctionDeclaration) newD);
        } else {
          return prevNode;
        }
      }

      if (newD.isGlobal()) {
        globalDeclarations.add(Pair.of(newD, rawSignature));

      } else {
        CFANode nextNode = newCFANode();

        final CDeclarationEdge edge =
            new CDeclarationEdge(rawSignature, fileLocation, prevNode, nextNode, newD);
        addToCFA(edge);

        prevNode = nextNode;
      }
    }
    prevNode =
        createEdgesForSideEffects(
            prevNode,
            sideAssignmentStack.getAndResetPostSideAssignments(),
            rawSignature,
            fileLocation);

    return prevNode;
  }

  /**
   * @category declarations
   */
  private int handleFunctionDefinition(final IASTFunctionDefinition declaration) {
    if (!locStack.isEmpty()) {
      throw parseContext.parseError("nested function declarations unsupported", declaration);
    }

    assert labelMap.isEmpty();
    assert gotoLabelNeeded.isEmpty();
    assert cfa == null;

    final CFunctionDeclaration fdef = astCreator.convert(declaration);
    final String nameOfFunction = fdef.getName();
    assert !nameOfFunction.isEmpty();

    scope.enterFunction(fdef);

    final List<CParameterDeclaration> parameters = fdef.getParameters();
    for (CParameterDeclaration param : parameters) {
      scope.registerDeclaration(param); // declare parameter as local variable
    }

    final FileLocation fileloc = astCreator.getLocation(declaration);
    final FunctionExitNode returnNode = new FunctionExitNode(fdef);

    final FunctionEntryNode startNode =
        new CFunctionEntryNode(fileloc, fdef, returnNode, scope.getReturnVariable());
    returnNode.setEntryNode(startNode);
    cfa = startNode;
    blocks.add(new FunctionBlock(startNode));

    final CFANode nextNode = newCFANode();
    locStack.add(nextNode);

    final BlankEdge dummyEdge =
        new BlankEdge("", FileLocation.DUMMY, startNode, nextNode, "Function start dummy edge");
    addToCFA(dummyEdge);

    return PROCESS_CONTINUE;
  }

  /**
   * @category declarations
   */
  private int ignoreASMDeclaration(final IASTNode asmCode) {
    FileLocation fileloc = astCreator.getLocation(asmCode);
    logger.log(Level.FINER, fileloc + ": Ignoring inline assembler code.");
    encounteredAsm = true;

    final CFANode prevNode = locStack.pop();

    final CFANode nextNode = newCFANode();
    locStack.push(nextNode);

    final BlankEdge edge =
        new BlankEdge(
            asmCode.getRawSignature(),
            fileloc,
            prevNode,
            nextNode,
            "Ignored inline assembler code");
    addToCFA(edge);

    return PROCESS_SKIP;
  }

  /**
   * @category declarations
   */
  @Override
  public int leave(IASTDeclaration declaration) {
    if (shutdownNotifier.shouldShutdown()) {
      return PROCESS_ABORT;
    }

    // leaving Sideassignment block
    sideAssignmentStack.leaveBlock();

    if (declaration instanceof IASTFunctionDefinition) {

      if (locStack.size() != 1) {
        throw new CFAGenerationRuntimeException("Depth wrong. Geoff needs to do more work");
      }

      CFANode lastNode = locStack.pop();

      if (isReachableNode(lastNode)) {
        BlankEdge blankEdge =
            new BlankEdge("", FileLocation.DUMMY, lastNode, cfa.getExitNode(), "default return");
        addToCFA(blankEdge);
      } else {
        cfa.getExitNode().addOutOfScopeVariables(lastNode.getOutOfScopeVariables());
      }

      if (!gotoLabelNeeded.isEmpty()) {
        throw new CFAGenerationRuntimeException(
            "Following labels were not found in function "
                + cfa.getFunctionName()
                + ": "
                + gotoLabelNeeded.keySet());
      }

      Set<CFANode> reachableNodes = CFATraversal.dfs().collectNodesReachableFrom(cfa);

      for (CFALabelNode n : labelMap.values()) {
        if (!reachableNodes.contains(n)) {
          logDeadLabel(n);

          // remove all entering edges
          while (n.getNumEnteringEdges() > 0) {
            CFACreationUtils.removeEdgeFromNodes(n.getEnteringEdge(0));
          }

          // now we can delete this whole unreachable part
          CFACreationUtils.removeChainOfNodesFromCFA(n);
        }
      }
    }

    return PROCESS_CONTINUE;
  }

  /**
   * @category declarations
   */
  private void logDeadLabel(CFALabelNode n) {
    Level level = Level.INFO;
    if (n.getLabel().matches("(switch|while|ldv)_(\\d+$|\\d+_[a-z0-9]+|[a-z0-9]+___\\d+)")) {
      // don't mention dead code produced by CIL/LDV on normal log levels
      level = Level.FINER;
    }
    logger.log(level, "Dead code detected: Label", n.getLabel(), "is not reachable.");
  }

  /////////////////////////////////////////////////////////////////////////////
  // Statements
  /////////////////////////////////////////////////////////////////////////////

  /**
   * @category statements
   */
  @Override
  public int visit(IASTStatement statement) {
    if (shutdownNotifier.shouldShutdown()) {
      return PROCESS_ABORT;
    }

    // entering Sideassignment block
    sideAssignmentStack.enterBlock();

    FileLocation fileloc = astCreator.getLocation(statement);

    // Handle special condition for else
    if (statement.getPropertyInParent() == IASTIfStatement.ELSE) {
      // Edge from current location to post if-statement location
      CFANode prevNode = locStack.pop();
      CFANode nextNode = locStack.peek();

      if (isReachableNode(prevNode)) {
        BlankEdge blankEdge = new BlankEdge("", FileLocation.DUMMY, prevNode, nextNode, "");
        addToCFA(blankEdge);
      }

      //  Push the start of the else clause onto our location stack
      CFANode elseNode = elseStack.pop();
      locStack.push(elseNode);
    }

    // Handle each kind of expression
    if (statement instanceof IASTCompoundStatement) {
      ASTNodeProperty property = statement.getPropertyInParent();
      if (property == IGNUASTCompoundStatementExpression.STATEMENT) {
        // IGNUASTCompoundStatementExpression content is already handled
        return PROCESS_SKIP;
      } else if (property == IASTCompoundStatement.NESTED_STATEMENT) {
        blockStarts.push(locStack.peek());
      }

      scope.enterBlock();
      // Do nothing, just continue visiting
    } else if (statement instanceof IASTExpressionStatement) {
      handleExpressionStatement((IASTExpressionStatement) statement, fileloc);
    } else if (statement instanceof IASTIfStatement) {
      handleIfStatement((IASTIfStatement) statement, fileloc);
    } else if (statement instanceof IASTWhileStatement) {
      handleWhileStatement((IASTWhileStatement) statement, fileloc);
    } else if (statement instanceof IASTForStatement) {
      return handleForStatement((IASTForStatement) statement, fileloc);
    } else if (statement instanceof IASTBreakStatement) {
      handleBreakStatement((IASTBreakStatement) statement, fileloc);
    } else if (statement instanceof IASTContinueStatement) {
      handleContinueStatement((IASTContinueStatement) statement, fileloc);
    } else if (statement instanceof IASTLabelStatement) {
      handleLabelStatement((IASTLabelStatement) statement, fileloc);
    } else if (statement instanceof IASTGotoStatement) {
      handleGotoStatement((IASTGotoStatement) statement, fileloc);
    } else if (statement instanceof IASTReturnStatement) {
      handleReturnStatement((IASTReturnStatement) statement, fileloc);
    } else if (statement instanceof IASTSwitchStatement) {
      return handleSwitchStatement((IASTSwitchStatement) statement, fileloc);
    } else if (statement instanceof IASTCaseStatement) {
      handleCaseStatement((IASTCaseStatement) statement, fileloc);
    } else if (statement instanceof IASTDefaultStatement) {
      handleDefaultStatement((IASTDefaultStatement) statement, fileloc);
    } else if (statement instanceof IASTNullStatement) {
      // We really don't care about blank statements
    } else if (statement instanceof IASTDeclarationStatement) {
      // these are handled by visit(IASTDeclaration)
    } else if (statement instanceof IASTProblemStatement) {
      visit(((IASTProblemStatement) statement).getProblem());
    } else if (statement instanceof IASTDoStatement) {
      handleDoWhileStatement((IASTDoStatement) statement, fileloc);
    } else {
      throw parseContext.parseError(
          "Unknown AST node " + statement.getClass().getSimpleName(), statement);
    }

    return PROCESS_CONTINUE;
  }

  /**
   * @category statements
   */
  private void handleExpressionStatement(
      IASTExpressionStatement exprStatement, FileLocation fileloc) {

    CFANode prevNode = locStack.pop();
    CFANode lastNode = null;
    String rawSignature = exprStatement.getRawSignature();

    if (exprStatement.getExpression() instanceof IASTExpressionList) {
      for (IASTExpression exp :
          ((IASTExpressionList) exprStatement.getExpression()).getExpressions()) {
        CStatement statement = astCreator.convertExpressionToStatement(exp);
        lastNode = createIASTExpressionStatementEdges(rawSignature, fileloc, prevNode, statement);
        prevNode = lastNode;
      }
      assert lastNode != null;
    } else {
      CStatement statement = astCreator.convert(exprStatement);
      lastNode = createIASTExpressionStatementEdges(rawSignature, fileloc, prevNode, statement);
    }
    locStack.push(lastNode);
  }

  /**
   * @category statements
   */
  private CFANode createIASTExpressionStatementEdges(
      String rawSignature, FileLocation fileloc, CFANode prevNode, CStatement statement) {

    CFANode lastNode;
    boolean resultIsUsed = true;

    if (sideAssignmentStack.hasConditionalExpression()) {
      if (statement == null) {
        // if return-type of expression-list (side-effect-statement) is Void, statement is Null.
        // Example: void f(){}; int main(){(0, f());}
        resultIsUsed = false;

      } else if (statement instanceof CExpressionStatement) {
        // this may be code where the resulting value of a ternary operator is not used, e.g. (x ?
        // f() : g())
        List<Pair<IASTExpression, CIdExpression>> tempVars =
            sideAssignmentStack.getConditionalExpressions();
        if ((tempVars.size() == 1)
            && (tempVars.get(0).getSecond()
                == ((CExpressionStatement) statement).getExpression())) {
          resultIsUsed = false;
        }
      }
    }

    prevNode = handleAllSideEffects(prevNode, fileloc, rawSignature, resultIsUsed);

    if (statement == null) {
      return prevNode;
    }

    statement.accept(checkBinding);
    if (resultIsUsed) {
      lastNode = newCFANode();

      CStatementEdge edge =
          new CStatementEdge(rawSignature, statement, fileloc, prevNode, lastNode);
      addToCFA(edge);
    } else {
      lastNode = prevNode;
    }
    return lastNode;
  }

  /**
   * @category statements
   */
  private void handleLabelStatement(IASTLabelStatement labelStatement, FileLocation fileloc) {

    String labelName = labelStatement.getName().toString();
    if (labelMap.containsKey(labelName) && scope.lookupLocalLabel(labelName) == null) {
      throw parseContext.parseError(
          "Duplicate label " + labelName + " in function " + cfa.getFunctionName(), labelStatement);
    }

    CFANode prevNode = locStack.pop();

    CVariableDeclaration localLabel = scope.lookupLocalLabel(labelName);
    if (localLabel != null) {
      labelName = localLabel.getName();
    }

    CFALabelNode labelNode = new CFALabelNode(cfa.getFunction(), labelName);
    locStack.push(labelNode);

    if (localLabel == null) {
      labelMap.put(labelName, labelNode);
    } else {
      if (scope.containsLabelCFANode(labelNode)) {
        throw parseContext.parseError(
            "Duplicate label " + labelName + " in function " + cfa.getFunctionName(),
            labelStatement);
      }
      scope.addLabelCFANode(labelNode);
    }

    boolean isPrevNodeReachable = isReachableNode(prevNode);
    if (isPrevNodeReachable) {
      BlankEdge blankEdge =
          new BlankEdge(
              labelStatement.getRawSignature(),
              onlyFirstLine(fileloc),
              prevNode,
              labelNode,
              "Label: " + labelName);
      addToCFA(blankEdge);
    }

    // Check if any goto's previously analyzed need connections to this label
    for (Pair<CFANode, FileLocation> gotoNode : gotoLabelNeeded.get(labelName)) {
      String description = "Goto: " + labelName;
      BlankEdge gotoEdge =
          new BlankEdge(
              description,
              onlyFirstLine(gotoNode.getSecond()),
              gotoNode.getFirst(),
              labelNode,
              description);
      addToCFA(gotoEdge);

      FileLocation gotoLocation = gotoNode.getSecondNotNull();
      for (StatementBlock block :
          FluentIterable.from(blocks).filter(StatementBlock.class).toList()) {
        if (block.getStartOffset() <= gotoLocation.getNodeOffset()
            && gotoLocation.getNodeOffset() + gotoLocation.getNodeLength()
                <= block.getEndOffset()) {
          block.addEndNode(labelNode);
        }
      }
    }
    gotoLabelNeeded.removeAll(labelName);

    if (!isPrevNodeReachable && isReachableNode(labelNode)) {
      locStack.pop();
      CFANode node = newCFANode();
      BlankEdge blankEdge =
          new BlankEdge(
              labelStatement.getRawSignature(),
              onlyFirstLine(fileloc),
              labelNode,
              node,
              "Label: " + labelName);
      addToCFA(blankEdge);
      locStack.push(node);
    }
  }

  /**
   * @category statements
   */
  private void handleGotoStatement(IASTGotoStatement gotoStatement, FileLocation fileloc) {

    String labelName = gotoStatement.getName().toString();

    CFANode prevNode = locStack.pop();
    CFANode labelNode = labelMap.get(labelName);

    // check if label is local label
    CVariableDeclaration localLabel = scope.lookupLocalLabel(labelName);
    if (localLabel != null) {
      labelName = localLabel.getName();
      labelNode = scope.lookupLocalLabelNode(labelName);
    }

    if (labelNode != null) {
      BlankEdge gotoEdge =
          new BlankEdge(
              gotoStatement.getRawSignature(), fileloc, prevNode, labelNode, "Goto: " + labelName);

      for (StatementBlock block :
          FluentIterable.from(blocks).filter(StatementBlock.class).toList()) {
        if (block.getStartOffset() <= fileloc.getNodeOffset()
            && fileloc.getNodeOffset() + fileloc.getNodeLength() <= block.getEndOffset()) {
          block.addEndNode(labelNode);
        }
      }

      /* labelNode was analyzed before, so it is in the labelMap,
       * then there can be a jump backwards and this can create a loop.
       * If LabelNode has not been the start of a loop, Node labelNode can be
       * the start of a loop, so check if there is a path from labelNode to
       * the current Node through DFS-search */
      if (!labelNode.isLoopStart() && isPathFromTo(labelNode, prevNode)) {
        labelNode.setLoopStart();
      }

      addToCFA(gotoEdge);
    } else {
      gotoLabelNeeded.put(labelName, Pair.of(prevNode, fileloc));
    }

    CFANode nextNode = newCFANode();
    locStack.push(nextNode);
  }

  /**
   * @category statements
   */
  private void handleReturnStatement(IASTReturnStatement returnStatement, FileLocation fileloc) {

    CFANode prevNode = locStack.pop();
    FunctionExitNode functionExitNode = cfa.getExitNode();

    // a return statement leaves all available scopes at once.
    for (Collection<CSimpleDeclaration> vars : scope.getVariablesOfMostLocalScopes()) {
      functionExitNode.addOutOfScopeVariables(vars);
    }

    for (StatementBlock block : FluentIterable.from(blocks).filter(StatementBlock.class).toList()) {
      if (block.getStartOffset() <= fileloc.getNodeOffset()
          && fileloc.getNodeOffset() + fileloc.getNodeLength() <= block.getEndOffset()) {
        block.addEndNode(functionExitNode);
      }
    }

    CReturnStatement returnstmt = astCreator.convert(returnStatement);
    prevNode = handleAllSideEffects(prevNode, fileloc, returnStatement.getRawSignature(), true);

    if (returnstmt.getReturnValue().isPresent()) {
      returnstmt.getReturnValue().orElseThrow().accept(checkBinding);
    }
    CReturnStatementEdge edge =
        new CReturnStatementEdge(
            returnStatement.getRawSignature(), returnstmt, fileloc, prevNode, functionExitNode);
    addToCFA(edge);

    CFANode nextNode = newCFANode();
    locStack.push(nextNode);
  }

  /**
   * @category statements
   */
  @Override
  public int leave(IASTStatement statement) {
    if (shutdownNotifier.shouldShutdown()) {
      return PROCESS_ABORT;
    }

    // leaving Sideassignment block
    sideAssignmentStack.leaveBlock();

    if (statement instanceof IASTIfStatement) {
      final CFANode prevNode = locStack.pop();
      final CFANode nextNode = locStack.peek();

      if (isReachableNode(prevNode)) {

        for (CFAEdge prevEdge : CFAUtils.allEnteringEdges(prevNode).toList()) {
          if ((prevEdge instanceof BlankEdge) && prevEdge.getDescription().isEmpty()) {

            // the only entering edge is a BlankEdge, so we delete this edge and prevNode

            CFANode prevPrevNode = prevEdge.getPredecessor();
            assert prevPrevNode.getNumLeavingEdges() == 1;
            prevNode.removeEnteringEdge(prevEdge);
            prevPrevNode.removeLeavingEdge(prevEdge);

            BlankEdge blankEdge =
                new BlankEdge("", prevEdge.getFileLocation(), prevPrevNode, nextNode, "");
            addToCFA(blankEdge);
          }
        }

        if (prevNode.getNumEnteringEdges() > 0) {
          BlankEdge blankEdge = new BlankEdge("", FileLocation.DUMMY, prevNode, nextNode, "");
          addToCFA(blankEdge);
        }
      }

    } else if (statement instanceof IASTCompoundStatement) {
      ASTNodeProperty property = statement.getPropertyInParent();
      if (property == IGNUASTCompoundStatementExpression.STATEMENT) {
        // IGNUASTCompoundStatementExpression content is already handled
        return PROCESS_SKIP;
      } else if (property == IASTCompoundStatement.NESTED_STATEMENT) {
        CFANode blockStart = blockStarts.pop();
        IASTFileLocation location = statement.getFileLocation();
        blocks.add(
            new StatementBlock(
                location.getNodeOffset(),
                location.getNodeOffset() + location.getNodeLength(),
                false,
                blockStart,
                locStack.peek()));
      }

      locStack.peek().addOutOfScopeVariables(scope.getVariablesOfMostLocalScope());
      scope.leaveBlock();

    } else if (statement instanceof IASTWhileStatement || statement instanceof IASTDoStatement) {
      CFANode prevNode = locStack.pop();
      CFANode startNode = loopStartStack.pop();

      if (isReachableNode(prevNode)) {
        BlankEdge blankEdge = new BlankEdge("", FileLocation.DUMMY, prevNode, startNode, "");
        addToCFA(blankEdge);
      }
      CFANode nextNode = loopNextStack.pop();
      assert nextNode.equals(locStack.peek());

      IASTFileLocation location = statement.getFileLocation();
      blocks.add(
          new StatementBlock(
              location.getNodeOffset(),
              location.getNodeOffset() + location.getNodeLength(),
              true,
              startNode,
              nextNode));
    }
    return PROCESS_CONTINUE;
  }

  /**
   * @category statements
   */
  @Override
  public int visit(IASTProblem problem) {
    if (shutdownNotifier.shouldShutdown()) {
      return PROCESS_ABORT;
    }

    throw parseContext.parseError(problem);
  }

  /////////////////////////////////////////////////////////////////////////////
  // Generic helper methods
  /////////////////////////////////////////////////////////////////////////////

  /**
   * This method adds this edge to the leaving and entering edges of its predecessor and successor
   * respectively, but it does so only if the edge does not contain dead code
   *
   * @category helper
   */
  private void addToCFA(CFAEdge edge) {
    CFACreationUtils.addEdgeToCFA(edge, logger, options.showDeadCode());
  }

  /**
   * @category helper
   */
  private CFANode newCFANode() {
    assert cfa != null;
    CFANode nextNode = new CFANode(cfa.getFunction());
    return nextNode;
  }

  /**
   * Determines whether a forwards path between two nodes exists.
   *
   * @param fromNode starting node
   * @param toNode target node
   * @category helper
   */
  private boolean isPathFromTo(CFANode fromNode, CFANode toNode) {
    // Optimization: do two DFS searches in parallel:
    // 1) search forwards from fromNode
    // 2) search backwards from toNode
    Deque<CFANode> toProcessForwards = new ArrayDeque<>();
    Deque<CFANode> toProcessBackwards = new ArrayDeque<>();
    Set<CFANode> visitedForwards = new HashSet<>();
    Set<CFANode> visitedBackwards = new HashSet<>();

    toProcessForwards.addLast(fromNode);
    visitedForwards.add(fromNode);

    toProcessBackwards.addLast(toNode);
    visitedBackwards.add(toNode);

    // if one of the queues is empty, the search has reached a dead end
    while (!toProcessForwards.isEmpty() && !toProcessBackwards.isEmpty()) {
      // step in forwards search
      CFANode currentForwards = toProcessForwards.removeLast();
      if (visitedBackwards.contains(currentForwards)) {
        // the backwards search already has seen the current node
        // so we know there's a path from fromNode to current and a path from
        // current to toNode
        return true;
      }

      for (CFANode successor : CFAUtils.successorsOf(currentForwards)) {
        if (visitedForwards.add(successor)) {
          toProcessForwards.addLast(successor);
        }
      }

      // step in backwards search
      CFANode currentBackwards = toProcessBackwards.removeLast();
      if (visitedForwards.contains(currentBackwards)) {
        // the forwards search already has seen the current node
        // so we know there's a path from fromNode to current and a path from
        // current to toNode
        return true;
      }

      for (CFANode predecessor : CFAUtils.predecessorsOf(currentBackwards)) {
        if (visitedBackwards.add(predecessor)) {
          toProcessBackwards.addLast(predecessor);
        }
      }
    }
    return false;
  }

  /**
   * Create a statement edge for an expression (which may be an expression list).
   *
   * @param expression The expression to put at the edge.
   * @param fileLocation The file location.
   * @param prevNode The predecessor of the new edge.
   * @param lastNode The successor of the new edge (may be null, in this case, a new node is
   *     created).
   * @return The successor of the new edge.
   * @category helper
   */
  private CFANode createEdgeForExpression(
      final IASTExpression expression,
      final FileLocation fileLocation,
      CFANode prevNode,
      @Nullable CFANode lastNode) {
    assert expression != null;

    if (expression instanceof IASTExpressionList) {
      IASTExpression[] expressions = ((IASTExpressionList) expression).getExpressions();
      CFANode nextNode = null;

      for (int i = 0; i < expressions.length; i++) {
        if (lastNode != null && i == expressions.length - 1) {
          nextNode = lastNode;
        } else {
          nextNode = newCFANode();
        }

        createEdgeForExpression(expressions[i], fileLocation, prevNode, nextNode);
        prevNode = nextNode;
      }

      return nextNode;

    } else {
      String rawSignature = expression.getRawSignature();
      final CStatement stmt = astCreator.convertExpressionToStatement(expression);

      // this is just a placeholder signalising that the sideeffects do not
      // need to have a return value
      if (stmt == null
          || (stmt instanceof CExpressionStatement
              && ((CExpressionStatement) stmt).getExpression() == CIntegerLiteralExpression.ZERO)) {
        return handleAllSideEffects(prevNode, fileLocation, rawSignature, false);
      } else {
        prevNode = handleAllSideEffects(prevNode, fileLocation, rawSignature, true);
      }

      stmt.accept(checkBinding);
      if (lastNode == null) {
        lastNode = newCFANode();
      }

      final CStatementEdge lastEdge =
          new CStatementEdge(rawSignature, stmt, fileLocation, prevNode, lastNode);
      addToCFA(lastEdge);
      return lastNode;
    }
  }

  /**
   * @category helper
   */
  private CBinaryExpression buildBinaryExpression(
      CExpression operand1, CExpression operand2, BinaryOperator op) {
    try {
      return binExprBuilder.buildBinaryExpression(operand1, operand2, op);
    } catch (UnrecognizedCodeException e) {
      throw new CFAGenerationRuntimeException(e);
    }
  }

  /////////////////////////////////////////////////////////////////////////////
  // Conditions
  /////////////////////////////////////////////////////////////////////////////

  /**
   * @category conditions
   */
  private void handleIfStatement(IASTIfStatement ifStatement, FileLocation fileloc) {

    CFANode prevNode = locStack.pop();

    CFANode postIfNode = newCFANode();
    locStack.push(postIfNode);

    CFANode thenNode = newCFANode();
    locStack.push(thenNode);

    IASTFileLocation location = ifStatement.getFileLocation();
    blocks.add(
        new StatementBlock(
            location.getNodeOffset(),
            location.getNodeOffset() + location.getNodeLength(),
            false,
            prevNode,
            postIfNode));

    CFANode elseNode;
    // elseNode is the start of the else branch,
    // or the node after the loop if there is no else branch
    if (ifStatement.getElseClause() == null) {
      elseNode = postIfNode;
    } else {
      elseNode = newCFANode();
      elseStack.push(elseNode);
    }

    createConditionEdges(
        ifStatement.getConditionExpression(), fileloc, prevNode, thenNode, elseNode);
  }

  /**
   * This function creates the edges of a condition. It expands the shortcutting operators && and ||
   * into several edges, and it skips branches that are not reachable (e.g., for "if (0) { }").
   *
   * @category conditions
   * @return If possible, an expression that represents the branching condition (only in simple
   *     cases).
   */
  private Optional<CExpression> createConditionEdges(
      final IASTExpression condition,
      final FileLocation fileLocation,
      CFANode rootNode,
      CFANode thenNode,
      final CFANode elseNode) {

    assert condition != null;

    return buildConditionTree(
        condition.getRawSignature(),
        condition,
        fileLocation,
        rootNode,
        thenNode,
        elseNode,
        thenNode,
        elseNode,
        true,
        true,
        false,
        new HashSet<>());
  }

  /**
   * @category conditions
   */
  private Optional<CExpression> buildConditionTree(
      String pRawSignature,
      IASTExpression condition,
      final FileLocation fileLocation,
      CFANode rootNode,
      CFANode thenNode,
      final CFANode elseNode,
      CFANode thenNodeForLastThen,
      CFANode elseNodeForLastElse,
      boolean furtherThenComputation,
      boolean furtherElseComputation,
      boolean flippedThenElse,
      Set<CFANode> pInnerNodes) {

    // unwrap (a)
    if (condition instanceof IASTUnaryExpression
        && ((IASTUnaryExpression) condition).getOperator()
            == IASTUnaryExpression.op_bracketedPrimary) {
      return buildConditionTree(
          pRawSignature,
          ((IASTUnaryExpression) condition).getOperand(),
          fileLocation,
          rootNode,
          thenNode,
          elseNode,
          thenNode,
          elseNode,
          true,
          true,
          flippedThenElse,
          pInnerNodes);

      // !a --> switch branches
    } else if (condition instanceof IASTUnaryExpression
        && ((IASTUnaryExpression) condition).getOperator() == IASTUnaryExpression.op_not) {
      buildConditionTree(
          pRawSignature,
          ((IASTUnaryExpression) condition).getOperand(),
          fileLocation,
          rootNode,
          elseNode,
          thenNode,
          elseNode,
          thenNode,
          true,
          true,
          !flippedThenElse,
          pInnerNodes);
      return Optional.empty();

      // a && b
    } else if (condition instanceof IASTBinaryExpression
        && ((IASTBinaryExpression) condition).getOperator() == IASTBinaryExpression.op_logicalAnd) {
      // This case is not necessary,
      // but it prevents the need for a temporary variable in the common case of
      // "if (a && b)"
      CFANode innerNode = newCFANode();
      pInnerNodes.add(innerNode);
      buildConditionTree(
          pRawSignature,
          ((IASTBinaryExpression) condition).getOperand1(),
          fileLocation,
          rootNode,
          innerNode,
          elseNode,
          thenNodeForLastThen,
          elseNode,
          true,
          false,
          flippedThenElse,
          pInnerNodes);
      buildConditionTree(
          pRawSignature,
          ((IASTBinaryExpression) condition).getOperand2(),
          fileLocation,
          innerNode,
          thenNode,
          elseNode,
          thenNodeForLastThen,
          elseNodeForLastElse,
          true,
          true,
          flippedThenElse,
          pInnerNodes);
      return Optional.empty();

      // a || b
    } else if (condition instanceof IASTBinaryExpression
        && ((IASTBinaryExpression) condition).getOperator() == IASTBinaryExpression.op_logicalOr) {
      // This case is not necessary,
      // but it prevents the need for a temporary variable in the common case of
      // "if (a || b)"
      CFANode innerNode = newCFANode();
      pInnerNodes.add(innerNode);
      buildConditionTree(
          pRawSignature,
          ((IASTBinaryExpression) condition).getOperand1(),
          fileLocation,
          rootNode,
          thenNode,
          innerNode,
          thenNodeForLastThen,
          elseNodeForLastElse,
          false,
          true,
          flippedThenElse,
          pInnerNodes);
      buildConditionTree(
          pRawSignature,
          ((IASTBinaryExpression) condition).getOperand2(),
          fileLocation,
          innerNode,
          thenNode,
          elseNode,
          thenNodeForLastThen,
          elseNodeForLastElse,
          true,
          true,
          flippedThenElse,
          pInnerNodes);
      return Optional.empty();

      // plain condition
    } else {
      return Optional.of(
          buildConditionTreeLeaf(
              pRawSignature,
              condition,
              fileLocation,
              rootNode,
              thenNode,
              elseNode,
              thenNodeForLastThen,
              elseNodeForLastElse,
              furtherThenComputation,
              furtherElseComputation,
              flippedThenElse,
              pInnerNodes));
    }
  }

  /** Handle a leaf node of a condition tree, i.e. the most primitive part of a condition. */
  private CExpression buildConditionTreeLeaf(
      String rawSignature,
      IASTExpression condition,
      final FileLocation fileLocation,
      CFANode rootNode,
      CFANode thenNode,
      final CFANode elseNode,
      CFANode thenNodeForLastThen,
      CFANode elseNodeForLastElse,
      boolean furtherThenComputation,
      boolean furtherElseComputation,
      boolean flippedThenElse,
      Set<CFANode> pInnerNodes)
      throws AssertionError {

    final CExpression exp = astCreator.convertExpressionWithoutSideEffects(condition);
    rootNode = handleAllSideEffects(rootNode, fileLocation, rawSignature, true);
    exp.accept(checkBinding);

    final CONDITION kind = astCreator.getConditionKind(exp);
    switch (kind) {
      case ALWAYS_FALSE:
        // no edge connecting rootNode with thenNode,
        // so the "then" branch won't be connected to the rest of the CFA

        final BlankEdge falseEdge =
            new BlankEdge(rawSignature, onlyFirstLine(fileLocation), rootNode, elseNode, "");
        addToCFA(falseEdge);

        // reset side assignments which are not necessary
        return CIntegerLiteralExpression.ZERO;

      case ALWAYS_TRUE:
        final BlankEdge trueEdge =
            new BlankEdge(rawSignature, onlyFirstLine(fileLocation), rootNode, thenNode, "");
        addToCFA(trueEdge);

        // no edge connecting prevNode with elseNode,
        // so the "else" branch won't be connected to the rest of the CFA
        return CIntegerLiteralExpression.ONE;

      case NORMAL:
        break;

      default:
        throw new AssertionError();
    }

    if (furtherThenComputation) {
      thenNodeForLastThen = thenNode;
    }
    if (furtherElseComputation) {
      elseNodeForLastElse = elseNode;
    }

    FileLocation loc = astCreator.getLocation(condition);
    if (fileLocation.getStartingLineNumber() < loc.getStartingLineNumber()) {
      loc =
          new FileLocation(
              loc.getFileName(),
              loc.getNiceFileName(),
              fileLocation.getNodeOffset(),
              loc.getNodeLength() + loc.getNodeOffset() - fileLocation.getNodeOffset(),
              fileLocation.getStartingLineNumber(),
              loc.getEndingLineNumber(),
              fileLocation.getStartingLineInOrigin(),
              loc.getEndingLineInOrigin(),
              fileLocation.isOffsetRelatedToOrigin() && loc.isOffsetRelatedToOrigin());
    }

    CExpression expression = exp;

    if (!ASTOperatorConverter.isBooleanExpression(expression)) {
      // build new boolean expression: a==0 and swap branches
      expression =
          buildBinaryExpression(exp, CIntegerLiteralExpression.ZERO, BinaryOperator.EQUALS);
      flippedThenElse = !flippedThenElse;
      CFANode tmp = elseNodeForLastElse;
      elseNodeForLastElse = thenNodeForLastThen;
      thenNodeForLastThen = tmp;
    }

    addConditionEdges(
        rawSignature,
        expression,
        rootNode,
        thenNodeForLastThen,
        elseNodeForLastElse,
        loc,
        flippedThenElse,
        pInnerNodes);
    return exp;
  }

  /**
   * This method adds 2 edges to the cfa: 1. trueEdge from rootNode to thenNode and 2. falseEdge
   * from rootNode to elseNode.
   *
   * @category conditions
   */
  private void addConditionEdges(
      String pRawSignature,
      CExpression condition,
      CFANode rootNode,
      CFANode thenNode,
      CFANode elseNode,
      FileLocation fileLocation,
      boolean pIsSwapped,
      Set<CFANode> pInnerNodes) {
    final String trueSignature;
    final String falseSignature;
    if (pIsSwapped) {
      falseSignature = pRawSignature;
      if (pRawSignature.startsWith("!(")) {
        trueSignature = pRawSignature.substring(2, pRawSignature.length() - 1);
      } else {
        trueSignature = "!(" + pRawSignature + ")";
      }
    } else {
      trueSignature = pRawSignature;
      falseSignature = "!(" + pRawSignature + ")";
    }
    // edge connecting condition with thenNode
    final CAssumeEdge trueEdge =
        new CAssumeEdge(
            trueSignature,
            fileLocation,
            rootNode,
            thenNode,
            condition,
            true,
            pIsSwapped,
            pInnerNodes.contains(thenNode));
    addToCFA(trueEdge);

    // edge connecting condition with elseNode
    final CAssumeEdge falseEdge =
        new CAssumeEdge(
            falseSignature,
            fileLocation,
            rootNode,
            elseNode,
            condition,
            false,
            pIsSwapped,
            pInnerNodes.contains(elseNode));
    addToCFA(falseEdge);
  }

  /////////////////////////////////////////////////////////////////////////////
  // Loops
  /////////////////////////////////////////////////////////////////////////////

  /**
   * @category loops
   */
  private void handleWhileStatement(IASTWhileStatement whileStatement, FileLocation fileloc) {
    final CFANode prevNode = locStack.pop();

    createLoop(whileStatement.getCondition(), fileloc);

    // connect CFA with loop start node
    final BlankEdge blankEdge =
        new BlankEdge("", onlyFirstLine(fileloc), prevNode, loopStartStack.peek(), "while");
    addToCFA(blankEdge);
  }

  /**
   * @category loops
   */
  private void handleDoWhileStatement(IASTDoStatement doStatement, FileLocation fileloc) {
    final CFANode prevNode = locStack.pop();

    createLoop(doStatement.getCondition(), fileloc);

    // connect CFA with first node inside the loop
    // (so the condition will be skipped in the first iteration)
    final BlankEdge blankEdge =
        new BlankEdge("", onlyFirstLine(fileloc), prevNode, locStack.peek(), "do");
    addToCFA(blankEdge);
  }

  /**
   * Create a simple while or do-while style loop, and set up all the stacks. The loop will not be
   * connected to the existing CFA, the caller has to ensure this.
   *
   * @category loops
   */
  private void createLoop(IASTExpression condition, FileLocation fileloc) {
    final CFANode loopStart = newCFANode();
    loopStart.setLoopStart();
    loopStartStack.push(loopStart);

    final CFANode firstLoopNode = newCFANode();

    final CFANode postLoopNode = newCFANode();
    loopNextStack.push(postLoopNode);

    // inverse order here!
    locStack.push(postLoopNode);
    locStack.push(firstLoopNode);

    createConditionEdges(condition, fileloc, loopStart, firstLoopNode, postLoopNode);
  }

  /**
   * @category loops
   */
  private void handleBreakStatement(IASTBreakStatement breakStatement, FileLocation fileloc) {

    CFANode prevNode = locStack.pop();
    CFANode postLoopNode = loopNextStack.peek();

    // on "return" we add the OOSVars after the return edge, because the return can access all vars.
    // on "break" we add the OOSVars before the break edge, because nothing happens along the edge.
    prevNode.addOutOfScopeVariables(scope.getVariablesOfMostLocalScope());

    BlankEdge blankEdge =
        new BlankEdge(breakStatement.getRawSignature(), fileloc, prevNode, postLoopNode, "break");
    addToCFA(blankEdge);

    CFANode nextNode = newCFANode();
    locStack.push(nextNode);
  }

  /**
   * @category loops
   */
  private void handleContinueStatement(
      IASTContinueStatement continueStatement, FileLocation fileloc) {

    CFANode prevNode = locStack.pop();
    CFANode loopStartNode = loopStartStack.peek();

    BlankEdge blankEdge =
        new BlankEdge(
            continueStatement.getRawSignature(), fileloc, prevNode, loopStartNode, "continue");
    addToCFA(blankEdge);

    CFANode nextNode = new CFANode(cfa.getFunction());
    locStack.push(nextNode);
  }

  /////////////////////////////////////////////////////////////////////////////
  // For loops
  /////////////////////////////////////////////////////////////////////////////

  /**
   * @category forloop
   */
  private int handleForStatement(
      final IASTForStatement forStatement, final FileLocation fileLocation) {

    final CFANode prevNode = locStack.pop();
    scope.enterBlock();

    // loopInit is Node before "counter = 0;"
    final CFANode loopInit = newCFANode();
    addToCFA(new BlankEdge("", onlyFirstLine(fileLocation), prevNode, loopInit, "for"));

    // loopStart is the Node before the loop itself,
    // it is the the one after the init edge(s)
    final CFANode loopStart =
        createInitEdgeForForLoop(forStatement.getInitializerStatement(), fileLocation, loopInit);
    loopStart.setLoopStart();

    // loopEnd is Node before "counter++;"
    final CFANode loopEnd;
    final IASTExpression iterationExpression = forStatement.getIterationExpression();
    if (iterationExpression != null) {
      loopEnd = newCFANode();
    } else {
      loopEnd = loopStart;
    }
    loopStartStack.push(loopEnd);

    // firstLoopNode is Node after "counter < 5"
    final CFANode firstLoopNode = newCFANode();

    // postLoopNode is Node after "!(counter < 5)"
    final CFANode postLoopNode = newCFANode();
    loopNextStack.push(postLoopNode);

    IASTFileLocation location = forStatement.getFileLocation();
    blocks.add(
        new StatementBlock(
            location.getNodeOffset(),
            location.getNodeOffset() + location.getNodeLength(),
            true,
            firstLoopNode,
            postLoopNode));

    // inverse order here!
    locStack.push(postLoopNode);
    locStack.push(firstLoopNode);

    createConditionEdgesForForLoop(
        forStatement.getConditionExpression(),
        fileLocation,
        loopStart,
        postLoopNode,
        firstLoopNode);

    // visit only loopbody, not children, loop.getBody() != loop.getChildren()
    forStatement.getBody().accept(this);

    // leave loop
    final CFANode lastNodeInLoop = locStack.pop();

    // loopEnd is the Node before "counter++;"
    assert loopEnd.equals(loopStartStack.peek());
    assert postLoopNode.equals(loopNextStack.peek());
    assert postLoopNode.equals(locStack.peek());
    loopStartStack.pop();
    loopNextStack.pop();

    if (isReachableNode(lastNodeInLoop)) {
      final BlankEdge blankEdge =
          new BlankEdge("", FileLocation.DUMMY, lastNodeInLoop, loopEnd, "");
      addToCFA(blankEdge);
    }

    // this edge connects loopEnd with loopStart and contains the statement "counter++;"
    if (iterationExpression != null) {
      createEdgeForExpression(iterationExpression, fileLocation, loopEnd, loopStart);
    } else {
      assert loopEnd.equals(loopStart);
    }

    postLoopNode.addOutOfScopeVariables(scope.getVariablesOfMostLocalScope());
    scope.leaveBlock();

    // skip visiting children of loop, because loopbody was handled before
    return PROCESS_SKIP;
  }

  /**
   * This function creates the edge for the init-statement of a for-loop. The edge is inserted after
   * the loopInit-Node. If there are more than one declarations, more edges are inserted.
   *
   * @return The node after the last inserted edge.
   * @category forloop
   */
  private CFANode createInitEdgeForForLoop(
      final IASTStatement statement, final FileLocation fileLocation, CFANode prevNode) {

    if (statement instanceof IASTDeclarationStatement) {
      // "int counter = 0;"
      final IASTDeclaration decl = ((IASTDeclarationStatement) statement).getDeclaration();
      if (!(decl instanceof IASTSimpleDeclaration)) {
        throw parseContext.parseError("Unexpected declaration in header of for loop", decl);
      }
      return createEdgeForDeclaration((IASTSimpleDeclaration) decl, fileLocation, prevNode);

    } else if (statement instanceof IASTExpressionStatement) {
      // "counter = 0;"
      IASTExpression expression = ((IASTExpressionStatement) statement).getExpression();
      return createEdgeForExpression(expression, fileLocation, prevNode, null);

    } else if (statement instanceof IASTNullStatement) {
      // ";", no edge inserted
      return prevNode;

    } else {
      throw parseContext.parseError("Unexpected statement type in header of for loop", statement);
    }
  }

  /**
   * This function creates the condition-edges of a for-loop. Normally there are 2 edges: one
   * 'then'-edge and one 'else'-edge. If the condition is ALWAYS_TRUE or ALWAYS_FALSE or 'null' only
   * one edge is created.
   *
   * @category forloop
   */
  private void createConditionEdgesForForLoop(
      final IASTExpression condition,
      final FileLocation fileLocation,
      CFANode loopStart,
      final CFANode postLoopNode,
      final CFANode firstLoopNode) {

    if (condition == null) {
      // no condition -> only a blankEdge from loopStart to firstLoopNode
      final BlankEdge blankEdge = new BlankEdge("", fileLocation, loopStart, firstLoopNode, "");
      addToCFA(blankEdge);

    } else if (condition instanceof IASTExpressionList) {
      IASTExpression[] expl = ((IASTExpressionList) condition).getExpressions();
      for (int i = 0; i < expl.length - 1; i++) {
        loopStart = createEdgeForExpression(expl[i], fileLocation, loopStart, null);
      }
      createConditionEdges(
          expl[expl.length - 1], fileLocation, loopStart, firstLoopNode, postLoopNode);
    } else {
      createConditionEdges(condition, fileLocation, loopStart, firstLoopNode, postLoopNode);
    }
  }

  // Keeping only first line of FileLocation. Temporary workaround to avoid considering
  // switch-case blocks and while blocks as covered when the BlankEdge entering these
  // blocks is traversed.
  private FileLocation onlyFirstLine(FileLocation f) {
    return new FileLocation(
        f.getFileName(),
        f.getNiceFileName(),
        f.getNodeOffset(),
        f.getNodeLength(),
        f.getStartingLineNumber(),
        f.getStartingLineNumber(),
        f.getStartingLineInOrigin(),
        f.getStartingLineInOrigin(),
        f.isOffsetRelatedToOrigin());
  }

  /////////////////////////////////////////////////////////////////////////////
  // Switch statement
  /////////////////////////////////////////////////////////////////////////////

  /**
   * @category switchstatement
   */
  private int handleSwitchStatement(final IASTSwitchStatement statement, FileLocation fileloc) {

    CFANode prevNode = locStack.pop();

    CExpression switchExpression =
        astCreator.convertExpressionWithoutSideEffects(statement.getControllerExpression());
    prevNode =
        handleAllSideEffects(
            prevNode, switchExpression.getFileLocation(), statement.getRawSignature(), true);

    // firstSwitchNode is first Node of switch-Statement.
    final CFANode firstSwitchNode = newCFANode();
    String rawSignature = "switch (" + statement.getControllerExpression().getRawSignature() + ")";
    String description = "switch (" + switchExpression.toASTString() + ")";
    addToCFA(
        new BlankEdge(
            rawSignature, onlyFirstLine(fileloc), prevNode, firstSwitchNode, description));

    switchExprStack.push(switchExpression);
    switchCaseStack.push(firstSwitchNode);

    // postSwitchNode is Node after the switch-statement
    final CFANode postSwitchNode = newCFANode();
    loopNextStack.push(postSwitchNode);
    locStack.push(postSwitchNode);

    IASTFileLocation location = statement.getFileLocation();
    blocks.add(
        new StatementBlock(
            location.getNodeOffset(),
            location.getNodeOffset() + location.getNodeLength(),
            false,
            firstSwitchNode,
            postSwitchNode));

    locStack.push(new CFANode(cfa.getFunction()));

    switchDefaultStack.push(null);
    switchDefaultFileLocationStack.push(null);

    // visit only body, getBody() != getChildren()
    statement.getBody().accept(this);

    // leave switch
    final CFANode lastNodeInSwitch = locStack.pop();
    final CFANode lastNotCaseNode = switchCaseStack.pop();
    final CFANode defaultCaseNode = switchDefaultStack.pop();
    final FileLocation defaultCaseFileLocation = switchDefaultFileLocationStack.pop();

    switchExprStack.pop();

    assert postSwitchNode.equals(loopNextStack.peek());
    assert postSwitchNode.equals(locStack.peek());
    assert switchExprStack.size() == switchCaseStack.size();

    loopNextStack.pop();

    if (defaultCaseNode == null) {
      // no default case
      final BlankEdge blankEdge =
          new BlankEdge("", FileLocation.DUMMY, lastNotCaseNode, postSwitchNode, "");
      addToCFA(blankEdge);

    } else {
      // blank edge connecting rootNode with defaultCaseNode
      final BlankEdge defaultEdge =
          new BlankEdge(
              statement.getRawSignature(),
              onlyFirstLine(defaultCaseFileLocation),
              lastNotCaseNode,
              defaultCaseNode,
              "default");
      addToCFA(defaultEdge);
    }

    // fall-through of last case
    final BlankEdge blankEdge2 =
        new BlankEdge("", FileLocation.DUMMY, lastNodeInSwitch, postSwitchNode, "");
    addToCFA(blankEdge2);

    // skip visiting children of loop, because switch-body was handled before
    return PROCESS_SKIP;
  }

  /**
   * @category switchstatement
   */
  private void handleCaseStatement(final IASTCaseStatement statement, FileLocation fileLocation) {

    // condition, right part, "2" or 'a' or 'a'...'c'
    IASTExpression right = statement.getExpression();

    CFANode rootNode = switchCaseStack.pop();
    final CExpression switchExpr = switchExprStack.peek();
    final CFANode caseNode = newCFANode();
    final CFANode notCaseNode = newCFANode();
    final CFANode nextCaseStartsAtNode;

    // if there is a range, this has to be handled in another way,
    // the expression is split up, and the bounds are tested
    if (right instanceof IASTBinaryExpression
        && ((IASTBinaryExpression) right).getOperator() == IASTBinaryExpression.op_ellipses) {
      nextCaseStartsAtNode =
          handleCaseRangeStatement(
              (IASTBinaryExpression) right,
              switchExpr,
              rootNode,
              caseNode,
              notCaseNode,
              fileLocation);
    } else {
      nextCaseStartsAtNode =
          handleCaseSingleStatement(
              statement.getExpression(), switchExpr, rootNode, caseNode, notCaseNode, fileLocation);
    }

    // fall-through (case before has no "break")
    final CFANode oldNode = locStack.pop();
    if (oldNode.getNumEnteringEdges() > 0 || oldNode instanceof CFALabelNode) {
      final BlankEdge blankEdge =
          new BlankEdge("", onlyFirstLine(fileLocation), oldNode, caseNode, "fall through");
      addToCFA(blankEdge);
    }

    switchCaseStack.push(nextCaseStartsAtNode);
    locStack.push(caseNode);
  }

  /**
   * @category switchstatement build condition edges, to caseNode with "a==2", to notCaseNode with
   *     "!(a==2)"
   * @return nextCaseStartsAtNode
   */
  private CFANode handleCaseSingleStatement(
      final IASTExpression right,
      final CExpression switchExpr,
      final CFANode rootNode,
      final CFANode caseNode,
      final CFANode notCaseNode,
      final FileLocation fileLocation) {

    // build condition, left part "a", right part "2" --> "a==2"
    final CExpression caseExpr = astCreator.convertExpressionWithoutSideEffects(right);
    final CBinaryExpression binExp =
        buildBinaryExpression(switchExpr, caseExpr, CBinaryExpression.BinaryOperator.EQUALS);

    final CExpression exp = astCreator.simplifyExpressionOneStep(binExp);
    final CFANode nextCaseStartsAtNode;
    switch (astCreator.getConditionKind(exp)) {
      case ALWAYS_FALSE:
        // no edge connecting rootNode with caseNode,
        // so the "case" branch won't be connected to the rest of the CFA.
        // also ignore the edge from rootNode to notCaseNode, it is not needed
        nextCaseStartsAtNode = rootNode;
        break;

      case ALWAYS_TRUE:
        final BlankEdge trueEdge =
            new BlankEdge(
                "",
                onlyFirstLine(fileLocation),
                rootNode,
                caseNode,
                "__case__[" + binExp.toASTString() + "]");
        addToCFA(trueEdge);
        nextCaseStartsAtNode = notCaseNode;
        break;

      case NORMAL:
        assert ASTOperatorConverter.isBooleanExpression(exp);
        addConditionEdges(
            exp.toASTString(),
            exp,
            rootNode,
            caseNode,
            notCaseNode,
            fileLocation,
            false,
            ImmutableSet.of());
        nextCaseStartsAtNode = notCaseNode;
        break;

      default:
        throw new AssertionError();
    }

    return nextCaseStartsAtNode;
  }

  /**
   * @category switchstatement build condition edges, to caseNode with "2<=x && x<=4", to
   *     notCaseNode with "!(2<=x && x<=4)"
   */
  private CFANode handleCaseRangeStatement(
      final IASTBinaryExpression range,
      final CExpression switchExpr,
      final CFANode rootNode,
      final CFANode caseNode,
      final CFANode notCaseNode,
      final FileLocation fileLocation) {

    // if there is a range, this has to be handled in another way,
    // the expression is split up, and the bounds are tested
    // 2 ... 4  -->  2<=x && x<=4
    final CExpression smallEnd =
        astCreator.convertExpressionWithoutSideEffects(range.getOperand1());
    final CExpression bigEnd = astCreator.convertExpressionWithoutSideEffects(range.getOperand2());
    final CBinaryExpression firstPart =
        buildBinaryExpression(smallEnd, switchExpr, CBinaryExpression.BinaryOperator.LESS_EQUAL);
    final CBinaryExpression secondPart =
        buildBinaryExpression(switchExpr, bigEnd, CBinaryExpression.BinaryOperator.LESS_EQUAL);

    final CExpression firstExp = astCreator.simplifyExpressionOneStep(firstPart);
    final CONDITION firstKind = astCreator.getConditionKind(firstExp);
    final CExpression secondExp = astCreator.simplifyExpressionOneStep(secondPart);
    final CONDITION secondKind = astCreator.getConditionKind(secondExp);

    final CFANode nextCaseStartsAtNode;
    if (firstKind == CONDITION.ALWAYS_FALSE || secondKind == CONDITION.ALWAYS_FALSE) {
      // no edge connecting rootNode with caseNode,
      // so the "case" branch won't be connected to the rest of the CFA.
      // also ignore the edge from rootNode to notCaseNode, it is not needed
      nextCaseStartsAtNode = rootNode;

    } else if (firstKind == CONDITION.ALWAYS_TRUE && secondKind == CONDITION.ALWAYS_TRUE) {
      final BlankEdge trueEdge =
          new BlankEdge(
              "",
              onlyFirstLine(fileLocation),
              rootNode,
              caseNode,
              "__case__[" + firstPart + " && " + secondPart + "]");
      addToCFA(trueEdge);
      nextCaseStartsAtNode = notCaseNode;

    } else { // build small condition-tree
      assert firstKind == CONDITION.NORMAL && secondKind == CONDITION.NORMAL
          : "either both conditions can be evaluated or not, but mixed is not allowed";

      final CFANode intermediateNode = newCFANode();
      // FIXME: the raw signature given to `addConditionEdges` is wrong in both cases below.
      // In case of same variable-name declarations in multiple block scopes,
      // the ASTString does not represent the raw signature of the switch-case,
      // because CPAchecker adds variable suffixes to distinguish the different variables.
      // We currently have no way to get the correct rawSignature of the switch-expression
      // and we can't just strip the suffix because it may be part of the original variable name.
      addConditionEdges(
          firstExp.toASTString(),
          firstExp,
          rootNode,
          intermediateNode,
          notCaseNode,
          fileLocation,
          false,
          Collections.singleton(intermediateNode));
      addConditionEdges(
          secondExp.toASTString(),
          secondExp,
          intermediateNode,
          caseNode,
          notCaseNode,
          fileLocation,
          false,
          ImmutableSet.of());
      nextCaseStartsAtNode = notCaseNode;
    }

    return nextCaseStartsAtNode;
  }

  /**
   * @category switchstatement
   */
  private void handleDefaultStatement(
      final IASTDefaultStatement statement, FileLocation fileLocation) {

    // hack: use label node to mark node as reachable
    // (otherwise the following edges won't get added because it has
    // no incoming edges
    CFALabelNode caseNode = new CFALabelNode(cfa.getFunction(), "__switch__default__");

    // Update switchDefaultStack with the new node
    final CFANode oldDefaultNode = switchDefaultStack.pop();
    final FileLocation oldDefaultFileLocation = switchDefaultFileLocationStack.pop();
    if (oldDefaultNode != null || oldDefaultFileLocation != null) {
      throw parseContext.parseError("Duplicate default statement in switch", statement);
    }
    switchDefaultStack.push(caseNode);
    switchDefaultFileLocationStack.push(fileLocation);

    // fall-through (case before has no "break")
    final CFANode oldNode = locStack.pop();
    if (oldNode.getNumEnteringEdges() > 0) {
      final BlankEdge blankEdge =
          new BlankEdge("", onlyFirstLine(fileLocation), oldNode, caseNode, "fall through");
      addToCFA(blankEdge);
    }

    locStack.push(caseNode);
  }

  /////////////////////////////////////////////////////////////////////////////
  // Handling of side effects and ternary operator
  /////////////////////////////////////////////////////////////////////////////

  /**
   * This methods handles all side effects and an eventual ternary or shortcutting operator.
   *
   * @param prevNode The CFANode where to start adding edges.
   * @param fileLocation The file location.
   * @param rawSignature The raw signature.
   * @param resultIsUsed In case a ternary operator exists, is the result used in some computation?
   *     (Otherwise we can omit the temporary variable.)
   * @return The last CFANode that was created.
   * @category sideeffects
   */
  private CFANode handleAllSideEffects(
      CFANode prevNode,
      final FileLocation fileLocation,
      final String rawSignature,
      final boolean resultIsUsed) {

    if (sideAssignmentStack.hasConditionalExpression() && !resultIsUsed) {
      List<Pair<IASTExpression, CIdExpression>> condExps =
          sideAssignmentStack.getAndResetConditionalExpressions();
      assert condExps.size() == 1;

      // ignore side assignment
      sideAssignmentStack.getAndResetPreSideAssignments();

      prevNode = handleConditionalExpression(prevNode, condExps.get(0).getFirst(), null);

    } else {

      List<CAstNode> sideAssignments = sideAssignmentStack.getAndResetPreSideAssignments();
      List<Pair<IASTExpression, CIdExpression>> conditionalExpressions =
          sideAssignmentStack.getAndResetConditionalExpressions();

      // check each side assignment and handle possible corresponding conditional expression
      for (CAstNode sideAssignment : sideAssignments) {
        prevNode = createEdgeForSideEffect(prevNode, sideAssignment, rawSignature, fileLocation);
        Iterator<Pair<IASTExpression, CIdExpression>> it = conditionalExpressions.iterator();
        while (it.hasNext()) {
          Pair<IASTExpression, CIdExpression> cond = it.next();
          IASTExpression condExp = cond.getFirst();
          CIdExpression tempVar = cond.getSecond();
          if (sideAssignment instanceof CVariableDeclaration) {
            CVariableDeclaration cvd = (CVariableDeclaration) sideAssignment;
            if (cvd.getOrigName().equals(tempVar.getName())) {
              prevNode = handleConditionalExpression(prevNode, condExp, tempVar);
              it.remove();
            }
          }
        }
      }

      // handle ternary operator or && or || or { } if there are still conditional expressions left
      for (Pair<IASTExpression, CIdExpression> cond : conditionalExpressions) {
        IASTExpression condExp = cond.getFirst();
        CIdExpression tempVar = cond.getSecond();

        prevNode = handleConditionalExpression(prevNode, condExp, tempVar);
      }
    }

    return prevNode;
  }

  private CFANode handleConditionalExpression(
      final CFANode prevNode, final IASTExpression condExp, final @Nullable CIdExpression tempVar) {
    if (condExp instanceof IASTConditionalExpression) {
      return handleTernaryOperator((IASTConditionalExpression) condExp, prevNode, tempVar);
    } else if (condExp instanceof IASTBinaryExpression) {
      return handleShortcuttingOperators((IASTBinaryExpression) condExp, prevNode, tempVar);
    } else if (condExp instanceof IGNUASTCompoundStatementExpression) {
      return handleCompoundStatementExpression(
          (IGNUASTCompoundStatementExpression) condExp, prevNode, tempVar);
    } else if (condExp instanceof IASTExpressionList) {
      return handleExpressionList((IASTExpressionList) condExp, prevNode, tempVar);
    } else {
      throw new AssertionError();
    }
  }

  /**
   * @category sideeffects
   */
  private CFANode handleExpressionList(
      IASTExpressionList listExp, CFANode prevNode, final CIdExpression tempVar) {

    IASTExpression[] expressions = listExp.getExpressions();
    for (int i = 0; i < expressions.length - 1; i++) {
      IASTExpression e = expressions[i];
      prevNode = createEdgeForExpression(e, astCreator.getLocation(e), prevNode, null);
    }

    IASTExpression lastExp = expressions[expressions.length - 1];

    CAstNode exp = astCreator.convertExpressionWithSideEffects(lastExp);

    FileLocation lastExpLocation = astCreator.getLocation(lastExp);
    prevNode = handleAllSideEffects(prevNode, lastExpLocation, lastExp.getRawSignature(), true);

    if (exp == null) {
      if (tempVar != null) {
        throw parseContext.parseError("invalid expression type", lastExp);
      }
      return prevNode;
    }

    CStatement stmt = null;
    if (tempVar != null) {
      if (exp instanceof CAssignment) {
        CFANode lastNode = newCFANode();
        CFAEdge edge =
            new CStatementEdge(
                exp.toASTString(), (CStatement) exp, lastExpLocation, prevNode, lastNode);
        addToCFA(edge);
        prevNode = lastNode;
        stmt = createStatement(lastExpLocation, tempVar, ((CAssignment) exp).getLeftHandSide());
      } else {
        stmt = createStatement(lastExpLocation, tempVar, (CRightHandSide) exp);
      }
    } else if (exp instanceof CStatement) {
      stmt = (CStatement) exp;
    } else if (!(exp instanceof CRightHandSide)) {
      throw parseContext.parseError("invalid expression type", lastExp);
    } else {
      stmt = createStatement(lastExpLocation, null, (CRightHandSide) exp);
    }
    CFANode lastNode = newCFANode();
    CFAEdge edge =
        new CStatementEdge(stmt.toASTString(), stmt, lastExpLocation, prevNode, lastNode);
    addToCFA(edge);

    return lastNode;
  }

  /**
   * @category sideeffects
   */
  private CFANode handleCompoundStatementExpression(
      IGNUASTCompoundStatementExpression compoundExp,
      final CFANode rootNode,
      final CIdExpression tempVar) {
    IASTStatement[] statements = compoundExp.getCompoundStatement().getStatements();
    if (statements.length == 0) {
      throw parseContext.parseError("Empty compound-statement expression", compoundExp);
    }

    scope.enterBlock();
    locStack.push(rootNode);

    int locDepth = locStack.size();
    int conditionDepth = elseStack.size();
    int loopDepth = loopStartStack.size();

    // Handling all but the last statement is easy.
    for (int i = 0; i < statements.length - 1; i++) {
      IASTStatement statement = statements[i];
      statement.accept(this);
    }

    assert locDepth == locStack.size();
    assert conditionDepth == elseStack.size();
    assert loopDepth == loopStartStack.size();

    IASTStatement lastStatement = statements[statements.length - 1];
    if (lastStatement instanceof IASTProblemStatement) {
      throw parseContext.parseError((IASTProblemStatement) lastStatement);
    }

    // If we do not need a return value, last statement is also easy.
    if (tempVar == null) {
      lastStatement.accept(this);
      locStack.peek().addOutOfScopeVariables(scope.getVariablesOfMostLocalScope());
      scope.leaveBlock();
      IASTFileLocation location = compoundExp.getFileLocation();
      blocks.add(
          new StatementBlock(
              location.getNodeOffset(),
              location.getNodeOffset() + location.getNodeLength(),
              false,
              rootNode,
              locStack.peek()));
      return locStack.pop();
    }

    if (!(lastStatement instanceof IASTExpressionStatement)) {
      throw parseContext.parseError(
          "Unsupported statement type "
              + lastStatement.getClass().getSimpleName()
              + " at end of compound-statement expression",
          lastStatement);
    }

    // Now we need to convert the last statement and get a value that we can assign to tempVar.
    CAstNode exp =
        astCreator.convertExpressionWithSideEffects(
            ((IASTExpressionStatement) lastStatement).getExpression());

    CFANode middleNode = locStack.pop();
    CRightHandSide rhs;
    if (exp instanceof CRightHandSide) {
      middleNode =
          handleAllSideEffects(
              middleNode,
              astCreator.getLocation(lastStatement),
              lastStatement.getRawSignature(),
              true);
      rhs = (CRightHandSide) exp;
    } else {
      Verify.verify(
          exp instanceof CAssignment,
          "Unexpected expression that is not an assignment nor a RHS, but %s: %s",
          exp.getClass(),
          exp);
      middleNode =
          createIASTExpressionStatementEdges(
              lastStatement.getRawSignature(),
              astCreator.getLocation(lastStatement),
              middleNode,
              ((CAssignment) exp));
      rhs = ((CAssignment) exp).getLeftHandSide();
    }

    CStatement stmt = createStatement(astCreator.getLocation(compoundExp), tempVar, rhs);
    CFANode lastNode =
        createIASTExpressionStatementEdges(
            stmt.toASTString(), stmt.getFileLocation(), middleNode, stmt);

    lastNode.addOutOfScopeVariables(scope.getVariablesOfMostLocalScope());
    scope.leaveBlock();

    IASTFileLocation location = compoundExp.getFileLocation();
    blocks.add(
        new StatementBlock(
            location.getNodeOffset(),
            location.getNodeOffset() + location.getNodeLength(),
            false,
            rootNode,
            lastNode));
    return lastNode;
  }

  /**
   * @category sideeffects
   */
  private CFANode handleShortcuttingOperators(
      IASTBinaryExpression binExp, CFANode rootNode, CIdExpression tempVar) {
    FileLocation fileLocation = astCreator.getLocation(binExp);

    CFANode intermediateNode = newCFANode();
    CFANode thenNode = newCFANode();
    CFANode elseNode = newCFANode();

    // create the four condition edges
    switch (binExp.getOperator()) {
      case IASTBinaryExpression.op_logicalAnd:
        createConditionEdges(
            binExp.getOperand1(), fileLocation, rootNode, intermediateNode, elseNode);
        break;
      case IASTBinaryExpression.op_logicalOr:
        createConditionEdges(
            binExp.getOperand1(), fileLocation, rootNode, thenNode, intermediateNode);
        break;
      default:
        throw new AssertionError();
    }
    createConditionEdges(binExp.getOperand2(), fileLocation, intermediateNode, thenNode, elseNode);

    // create the two final edges
    CFANode lastNode = newCFANode();
    if (tempVar != null) {
      // assign truth value to tempVar
      FileLocation loc = astCreator.getLocation(binExp);
      CSimpleType intType = CNumericTypes.INT;

      CExpression one = new CIntegerLiteralExpression(loc, intType, BigInteger.ONE);
      CStatement assignOne = createStatement(loc, tempVar, one);
      CFAEdge trueEdge =
          new CStatementEdge(
              binExp.getRawSignature(), assignOne, FileLocation.DUMMY, thenNode, lastNode);
      addToCFA(trueEdge);

      CExpression zero = new CIntegerLiteralExpression(loc, intType, BigInteger.ZERO);
      CStatement assignZero = createStatement(loc, tempVar, zero);
      CFAEdge falseEdge =
          new CStatementEdge(
              binExp.getRawSignature(), assignZero, FileLocation.DUMMY, elseNode, lastNode);
      addToCFA(falseEdge);

    } else {
      CFAEdge trueEdge = new BlankEdge("", fileLocation, thenNode, lastNode, "");
      addToCFA(trueEdge);
      CFAEdge falseEdge = new BlankEdge("", fileLocation, elseNode, lastNode, "");
      addToCFA(falseEdge);
    }

    return lastNode;
  }

  /**
   * @category sideeffects
   */
  private CFANode handleTernaryOperator(
      IASTConditionalExpression condExp, CFANode rootNode, CIdExpression tempVar) {
    FileLocation fileLocation = astCreator.getLocation(condExp);

    CFANode thenNode = newCFANode();
    CFANode elseNode = newCFANode();
    Optional<CExpression> condition =
        createConditionEdges(
            condExp.getLogicalConditionExpression(), fileLocation, rootNode, thenNode, elseNode);

    CFANode lastNode = newCFANode();

    // as a gnu c extension allows omitting the second operand and the implicitly adds the first
    // operand
    // as the second also, this is checked here
    if (condExp.getPositiveResultExpression() == null) {
      // Converting the logical-condition expression twice may cause problems,
      // for example if it defines labels inside it.
      // Thus we reuse the condition expression returned by createConditionEdges,
      // if possible.
      if (condition.isPresent()) {
        createEdgesForTernaryOperatorBranch(
            condition.orElseThrow(),
            condExp.getLogicalConditionExpression(),
            lastNode,
            fileLocation,
            thenNode,
            tempVar);
      } else {
        createEdgesForTernaryOperatorBranch(
            condExp.getLogicalConditionExpression(), lastNode, fileLocation, thenNode, tempVar);
      }
    } else {
      createEdgesForTernaryOperatorBranch(
          condExp.getPositiveResultExpression(), lastNode, fileLocation, thenNode, tempVar);
    }

    createEdgesForTernaryOperatorBranch(
        condExp.getNegativeResultExpression(), lastNode, fileLocation, elseNode, tempVar);

    return lastNode;
  }

  /**
   * @category sideeffects
   */
  private void createEdgesForTernaryOperatorBranch(
      IASTExpression condExp,
      CFANode lastNode,
      FileLocation fileLocation,
      CFANode prevNode,
      @Nullable CIdExpression tempVar) {
    createEdgesForTernaryOperatorBranch(
        astCreator.convertExpressionWithSideEffects(condExp),
        condExp,
        lastNode,
        fileLocation,
        prevNode,
        tempVar);
  }

  /**
   * @category sideeffects
   */
  private void createEdgesForTernaryOperatorBranch(
      CAstNode exp,
      IASTExpression condExp,
      CFANode lastNode,
      FileLocation fileLocation,
      CFANode prevNode,
      @Nullable CIdExpression tempVar) {

    if (!sideAssignmentStack.hasConditionalExpression()) {

      prevNode =
          createEdgesForSideEffects(
              prevNode,
              sideAssignmentStack.getAndResetPreSideAssignments(),
              exp.toASTString(),
              fileLocation);

      if (exp instanceof CStatement) {
        assert exp instanceof CAssignment;

        CFANode middle = newCFANode();
        CFAEdge edge =
            new CStatementEdge(
                condExp.getRawSignature(), (CStatement) exp, fileLocation, prevNode, middle);
        addToCFA(edge);

        prevNode = middle;
        exp = ((CAssignment) exp).getLeftHandSide();
      }
      assert exp instanceof CRightHandSide;

      CStatement stmt =
          createStatement(astCreator.getLocation(condExp), tempVar, (CRightHandSide) exp);

      CFAEdge edge =
          new CStatementEdge(condExp.getRawSignature(), stmt, fileLocation, prevNode, lastNode);
      addToCFA(edge);

    } else {
      // nested ternary operator
      assert exp instanceof CRightHandSide;
      boolean resultIsUsed =
          (tempVar != null)
              || (sideAssignmentStack.getConditionalExpressions().size() > 1)
              || (exp != sideAssignmentStack.getConditionalExpressions().get(0).getSecond());

      prevNode =
          handleAllSideEffects(prevNode, fileLocation, condExp.getRawSignature(), resultIsUsed);

      if (resultIsUsed) {
        CStatement stmt =
            createStatement(astCreator.getLocation(condExp), tempVar, (CRightHandSide) exp);
        addToCFA(new CStatementEdge(stmt.toASTString(), stmt, fileLocation, prevNode, lastNode));
      } else {
        addToCFA(new BlankEdge("", fileLocation, prevNode, lastNode, ""));
      }
    }
  }

  /**
   * This method creates statement and declaration edges for all given sideassignments.
   *
   * @return the nextnode
   * @category sideeffects
   */
  private CFANode createEdgesForSideEffects(
      CFANode prevNode,
      List<CAstNode> sideeffects,
      String rawSignature,
      FileLocation fileLocation) {
    for (CAstNode sideeffect : sideeffects) {
      prevNode = createEdgeForSideEffect(prevNode, sideeffect, rawSignature, fileLocation);
    }
    return prevNode;
  }

  /**
   * Creates statement and declaration edge for the given sideassignment
   *
   * @return the nextnode
   * @category sideeffects
   */
  private CFANode createEdgeForSideEffect(
      CFANode prevNode, CAstNode sideeffect, String rawSignature, FileLocation fileLocation) {

    CFANode nextNode = newCFANode();

    if (sideeffect instanceof CExpression) {
      sideeffect = new CExpressionStatement(sideeffect.getFileLocation(), (CExpression) sideeffect);
    }

    CFAEdge edge;
    if (sideeffect instanceof CStatement) {
      ((CStatement) sideeffect).accept(checkBinding);
      edge =
          new CStatementEdge(
              rawSignature, (CStatement) sideeffect, fileLocation, prevNode, nextNode);

    } else if (sideeffect instanceof CDeclaration) {
      if (sideeffect instanceof CVariableDeclaration) {
        CInitializer init = ((CVariableDeclaration) sideeffect).getInitializer();
        if (init != null) {
          init.accept(checkBinding);
        }
      }

      edge =
          new CDeclarationEdge(
              rawSignature, fileLocation, prevNode, nextNode, (CDeclaration) sideeffect);
    } else {
      throw new AssertionError();
    }
    addToCFA(edge);
    prevNode = nextNode;

    return prevNode;
  }

  /**
   * @category sideeffects
   */
  private CStatement createStatement(
      FileLocation fileLoc, @Nullable CIdExpression leftHandSide, CRightHandSide rightHandSide) {
    rightHandSide.accept(checkBinding);

    if (leftHandSide != null) {
      leftHandSide.accept(checkBinding);
      // create assignments

      if (rightHandSide instanceof CExpression) {
        return new CExpressionAssignmentStatement(
            fileLoc, leftHandSide, (CExpression) rightHandSide);
      } else if (rightHandSide instanceof CFunctionCallExpression) {
        return new CFunctionCallAssignmentStatement(
            fileLoc, leftHandSide, (CFunctionCallExpression) rightHandSide);
      } else {
        throw new AssertionError();
      }

    } else {
      // create ordinary statements
      if (rightHandSide instanceof CExpression) {
        return new CExpressionStatement(fileLoc, (CExpression) rightHandSide);
      } else if (rightHandSide instanceof CFunctionCallExpression) {
        return new CFunctionCallStatement(fileLoc, (CFunctionCallExpression) rightHandSide);
      } else {
        throw new AssertionError();
      }
    }
  }
}
