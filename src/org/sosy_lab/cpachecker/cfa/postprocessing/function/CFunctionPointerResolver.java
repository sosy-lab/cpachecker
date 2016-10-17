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
package org.sosy_lab.cpachecker.cfa.postprocessing.function;

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.CFAUtils.leavingEdges;

import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.Pair;

/**
 * This class is responsible for replacing calls via function pointers like (*fp)()
 * with code similar to the following:
 * if (fp == &f)
 *   f();
 * else if (fp == &g)
 *   f();
 * else
 *   (*fp)();
 *
 * The set of candidate functions used is configurable.
 * No actual call edges to the other functions are introduced.
 * The inserted function call statements look just like regular functions call statements.
 * The edge in the "else" branch is optional and configurable.
 */
@Options
public class CFunctionPointerResolver {

  @Option(secure=true, name="analysis.functionPointerEdgesForUnknownPointer",
      description="Create edge for skipping a function pointer call if its value is unknown.")
  private boolean createUndefinedFunctionCall = true;

  @Option(secure=true, name="analysis.matchAssignedFunctionPointers",
      description="Use as targets for call edges only those shich are assigned to the particular expression (structure field).")
  private boolean matchAssignedFunctionPointers = false;

  private enum FunctionSet {
    // The items here need to be declared in the order they should be used when checking function.
    ALL, //all defined functions considered (Warning: some CPAs require at least EQ_PARAM_SIZES)
    USED_IN_CODE, //includes only functions which address is taken in the code
    EQ_PARAM_COUNT, //all functions with matching number of parameters considered
    EQ_PARAM_SIZES, //all functions with parameters with matching sizes
    EQ_PARAM_TYPES, //all functions with matching number and types of parameters considered (implies EQ_PARAM_SIZES)
    RETURN_VALUE,   //void functions are not considered for assignments
  }

  @Option(secure=true, name="analysis.functionPointerTargets",
      description="potential targets for call edges created for function pointer calls")
  private Set<FunctionSet> functionSets = ImmutableSet.of(FunctionSet.USED_IN_CODE, FunctionSet.EQ_PARAM_SIZES, FunctionSet.RETURN_VALUE);

  private final Collection<FunctionEntryNode> candidateFunctions;

  private final ImmutableSetMultimap<String, String> candidateFunctionsForField;

  private final BiPredicate<CFunctionCall, CFunctionType> matchingFunctionCall;

  private final MutableCFA cfa;
  private final LogManager logger;

  public CFunctionPointerResolver(MutableCFA pCfa, List<Pair<ADeclaration, String>> pGlobalVars,
      Configuration config, LogManager pLogger) throws InvalidConfigurationException {
    cfa = pCfa;
    logger = pLogger;

    config.inject(this);

    matchingFunctionCall = getFunctionSetPredicate(functionSets);

    if (functionSets.contains(FunctionSet.USED_IN_CODE)) {
      CReferencedFunctionsCollector varCollector;
      if (matchAssignedFunctionPointers) {
        varCollector = new CReferencedFunctionsCollectorWithFieldsMatching();
      } else {
        varCollector = new CReferencedFunctionsCollector();
      }
      for (CFANode node : cfa.getAllNodes()) {
        for (CFAEdge edge : leavingEdges(node)) {
          varCollector.visitEdge(edge);
        }
      }
      for (Pair<ADeclaration, String> decl : pGlobalVars) {
        if (decl.getFirst() instanceof CVariableDeclaration) {
          CVariableDeclaration varDecl = (CVariableDeclaration)decl.getFirst();
          varCollector.visitDeclaration(varDecl);
        }
      }
      Set<String> addressedFunctions = varCollector.getCollectedFunctions();
      candidateFunctions =
          from(Sets.intersection(addressedFunctions, cfa.getAllFunctionNames()))
              .transform(Functions.forMap(cfa.getAllFunctions()))
              .toList();

      if (matchAssignedFunctionPointers) {
        candidateFunctionsForField =
            ImmutableSetMultimap.copyOf(
                ((CReferencedFunctionsCollectorWithFieldsMatching) varCollector)
                    .getFieldMatching());
      } else {
        candidateFunctionsForField = null;
      }

      if (logger.wouldBeLogged(Level.ALL)) {
        logger.log(Level.ALL, "Possible target functions of function pointers:\n",
            Joiner.on('\n').join(candidateFunctions));
      }

    } else {
      candidateFunctions = cfa.getAllFunctionHeads();
      candidateFunctionsForField = null;
    }
  }

  private BiPredicate<CFunctionCall, CFunctionType> getFunctionSetPredicate(
      Collection<FunctionSet> pFunctionSets) {
    // note that this set is sorted according to the declaration order of the enum
    EnumSet<FunctionSet> functionSets = EnumSet.copyOf(pFunctionSets);

    if (functionSets.contains(FunctionSet.EQ_PARAM_TYPES)
        || functionSets.contains(FunctionSet.EQ_PARAM_SIZES)) {
      functionSets.add(FunctionSet.EQ_PARAM_COUNT); // TYPES and SIZES need COUNT checked first
    }

    List<BiPredicate<CFunctionCall, CFunctionType>> predicates = new ArrayList<>();
    for (FunctionSet functionSet : functionSets) {
      switch (functionSet) {
      case ALL:
        // do nothing
        break;
        case EQ_PARAM_COUNT:
          predicates.add(this::checkParamCount);
          break;
        case EQ_PARAM_SIZES:
          predicates.add(this::checkReturnAndParamSizes);
          break;
        case EQ_PARAM_TYPES:
          predicates.add(this::checkReturnAndParamTypes);
          break;
        case RETURN_VALUE:
          predicates.add(this::checkReturnValue);
          break;
      case USED_IN_CODE:
        // Not necessary, only matching functions are in the
        // candidateFunctions set
        break;
      default:
        throw new AssertionError();
      }
    }
    return predicates.stream().reduce((a, b) -> true, BiPredicate::and);
  }

  /**
   * This method traverses the whole CFA,
   * potentially replacing function pointer calls with regular function calls.
   */
  public void resolveFunctionPointers() {

    // 1.Step: get all function calls
    final FunctionPointerCallCollector visitor = new FunctionPointerCallCollector();
    for (FunctionEntryNode functionStartNode : cfa.getAllFunctionHeads()) {
      CFATraversal.dfs().traverseOnce(functionStartNode, visitor);
    }

    // 2.Step: replace functionCalls with functioncall- and return-edges
    // This loop replaces function pointer calls inside the given function with regular function calls.
    for (final CStatementEdge edge : visitor.functionPointerCalls) {
      replaceFunctionPointerCall((CFunctionCall)edge.getStatement(), edge);
    }
  }

  /** This Visitor collects all functioncalls for functionPointers.
   *  It should visit the CFA of each functions before creating super-edges (functioncall- and return-edges). */
  private class FunctionPointerCallCollector extends CFATraversal.DefaultCFAVisitor {

    final List<CStatementEdge> functionPointerCalls = new ArrayList<>();

    @Override
    public CFATraversal.TraversalProcess visitEdge(final CFAEdge pEdge) {
      if (pEdge instanceof CStatementEdge) {
        final CStatementEdge edge = (CStatementEdge) pEdge;
        final AStatement stmt = edge.getStatement();
        if (stmt instanceof CFunctionCall && isFunctionPointerCall((CFunctionCall)stmt)) {
          functionPointerCalls.add(edge);
        }
      }
      return CFATraversal.TraversalProcess.CONTINUE;
    }
  }

  private boolean isFunctionPointerCall(CFunctionCall call) {
    CFunctionCallExpression callExpr = call.getFunctionCallExpression();
    if (callExpr.getDeclaration() != null) {
      // "f()" where "f" is a declared function
      return false;
    }

    CExpression nameExpr = callExpr.getFunctionNameExpression();
    if (nameExpr instanceof CIdExpression
        && ((CIdExpression)nameExpr).getDeclaration() == null) {
      // "f()" where "f" is an undefined identifier
      // Someone calls an undeclared function.
      return false;
    }

    // Either "exp()" where "exp" is a more complicated expression,
    // or "f()" where "f" is a variable.
    return true;
  }

  /**
   * This method replaces a single function pointer call with a function call series.
   */
  private void replaceFunctionPointerCall(CFunctionCall functionCall, CStatementEdge statement) {
    CFunctionCallExpression fExp = functionCall.getFunctionCallExpression();
    logger.log(Level.FINEST, "Function pointer call", fExp);

    CExpression nameExp = fExp.getFunctionNameExpression();
    Collection<CFunctionEntryNode> funcs = getFunctionSet(functionCall);
    if (matchAssignedFunctionPointers) {
      CExpression expression = nameExp;
      if (expression instanceof CPointerExpression) {
        expression = ((CPointerExpression) expression).getOperand();
      }
      if( expression instanceof CFieldReference) {
        String fieldName = ((CFieldReference)expression).getFieldName();
        Set<String> matchedFuncs = candidateFunctionsForField.get(fieldName);
        if (matchedFuncs.isEmpty()) {
          //TODO means, that our heuristics missed something
          funcs = Collections.emptySet();
        } else {
          funcs = from(funcs).
              filter(f -> matchedFuncs.contains(f.getFunctionName())).
              toSet();
        }
      }
    }

    if (funcs.isEmpty()) {
      // no possible targets, we leave the CFA unchanged and print a warning
      logger.logf(Level.WARNING, "%s: Function pointer %s with type %s is called,"
          + " but no possible target functions were found.",
          statement.getFileLocation(), nameExp.toASTString(), nameExp.getExpressionType().toASTString("*"));
      return;
    }

    logger.log(
        Level.FINEST,
        "Inserting edges for the function pointer",
        nameExp.toASTString(),
        "with type",
        nameExp.getExpressionType().toASTString("*"),
        "to the functions",
        from(funcs).transform(CFunctionEntryNode::getFunctionName));

    FileLocation fileLocation = statement.getFileLocation();
    CFANode start = statement.getPredecessor();
    CFANode end = statement.getSuccessor();
    // delete old edge
    CFACreationUtils.removeEdgeFromNodes(statement);

    if (nameExp instanceof CPointerExpression) {
      CExpression operand = ((CPointerExpression)nameExp).getOperand();
      if (CTypes.isFunctionPointer(operand.getExpressionType())) {
        // *fp is the same as fp
        nameExp = operand;
      }
    }

    CFANode rootNode = start;
    for (FunctionEntryNode fNode : funcs) {
      CFANode thenNode = newCFANode(start.getFunctionName());
      CFANode elseNode = newCFANode(start.getFunctionName());
      CIdExpression func = new CIdExpression(nameExp.getFileLocation(),
                                              (CType)fNode.getFunctionDefinition().getType(),
                                              fNode.getFunctionName(),
                                              (CSimpleDeclaration)fNode.getFunctionDefinition());
      CUnaryExpression amper = new CUnaryExpression(nameExp.getFileLocation(),
          func.getExpressionType(), func, CUnaryExpression.UnaryOperator.AMPER);

      final CBinaryExpressionBuilder binExprBuilder = new CBinaryExpressionBuilder(cfa.getMachineModel(), logger);
      CBinaryExpression condition = binExprBuilder.buildBinaryExpressionUnchecked(
          nameExp, amper, BinaryOperator.EQUALS);

      addConditionEdges(condition, rootNode, thenNode, elseNode, fileLocation);


      CFANode retNode = newCFANode(start.getFunctionName());
      //create special summary edge
      //thenNode-->retNode
      String pRawStatement = "pointer call(" + fNode.getFunctionName() + ") " + statement.getRawStatement();

      //replace function call by pointer expression with regular call by name (in functionCall and edge.getStatement())
      CFunctionCall regularCall = createRegularCall(functionCall, fNode);

      createCallEdge(fileLocation, pRawStatement,
          thenNode, retNode, regularCall);

      //retNode-->end
      BlankEdge be = new BlankEdge("skip", statement.getFileLocation(), retNode, end, "skip");
      CFACreationUtils.addEdgeUnconditionallyToCFA(be);

      rootNode = elseNode;
    }

    //rootNode --> end
    if (createUndefinedFunctionCall) {
      CStatementEdge summaryStatementEdge = new CStatementEdge(
          statement.getRawStatement(), statement.getStatement(),
          statement.getFileLocation(), rootNode, end);

      rootNode.addLeavingEdge(summaryStatementEdge);
      end.addEnteringEdge(summaryStatementEdge);
    } else {
      //no way to skip the function call
      //remove last edge to rootNode
      for (CFAEdge edge : CFAUtils.enteringEdges(rootNode)) {
        CFACreationUtils.removeEdgeFromNodes(edge);
      }
      cfa.removeNode(rootNode);
    }
  }

  private CIdExpression createIdExpression(CExpression nameExp,
      FunctionEntryNode fNode) {
    return new CIdExpression(nameExp.getFileLocation(),
        nameExp.getExpressionType(),
        fNode.getFunctionName(),
        (CSimpleDeclaration)fNode.getFunctionDefinition());
  }

  private CFunctionCall createRegularCall(CFunctionCall functionCall, FunctionEntryNode fNode) {
    CFunctionCallExpression oldCallExpr = functionCall.getFunctionCallExpression();
    CFunctionCallExpression newCallExpr = new CFunctionCallExpression(oldCallExpr.getFileLocation(), oldCallExpr.getExpressionType(),
        createIdExpression(oldCallExpr.getFunctionNameExpression(), fNode),
        oldCallExpr.getParameterExpressions(), (CFunctionDeclaration)fNode.getFunctionDefinition());

    if (functionCall instanceof CFunctionCallAssignmentStatement) {
      CFunctionCallAssignmentStatement asgn = (CFunctionCallAssignmentStatement)functionCall;
      return new CFunctionCallAssignmentStatement(functionCall.getFileLocation(),
          asgn.getLeftHandSide(), newCallExpr);
    } else if (functionCall instanceof CFunctionCallStatement) {
      return new CFunctionCallStatement(functionCall.getFileLocation(), newCallExpr);
    } else {
      throw new AssertionError("Unknown CFunctionCall subclass.");
    }
  }

  /**
   * @category helper
   */
  private CFANode newCFANode(final String functionName) {
    assert cfa != null;
    CFANode nextNode = new CFANode(functionName);
    cfa.addNode(nextNode);
    return nextNode;
  }

  private void createCallEdge(FileLocation fileLocation, String pRawStatement,
      CFANode predecessorNode, CFANode successorNode, CFunctionCall functionCall) {
    CStatementEdge callEdge = new CStatementEdge(pRawStatement,
        functionCall, fileLocation,
        predecessorNode, successorNode);
    CFACreationUtils.addEdgeUnconditionallyToCFA(callEdge);
  }

  /** This method adds 2 edges to the cfa:
   * 1. trueEdge from rootNode to thenNode and
   * 2. falseEdge from rootNode to elseNode.
   * @category conditions
   */
  private void addConditionEdges(CExpression condition, CFANode rootNode,
      CFANode thenNode, CFANode elseNode, FileLocation fileLocation) {
    // edge connecting condition with thenNode
    final CAssumeEdge trueEdge = new CAssumeEdge(condition.toASTString(),
        fileLocation, rootNode, thenNode, condition, true);
    CFACreationUtils.addEdgeToCFA(trueEdge, logger);

    // edge connecting condition with elseNode
    final CAssumeEdge falseEdge = new CAssumeEdge("!(" + condition.toASTString() + ")",
        fileLocation, rootNode, elseNode, condition, false);
    CFACreationUtils.addEdgeToCFA(falseEdge, logger);
  }

  private List<CFunctionEntryNode> getFunctionSet(final CFunctionCall call) {
    return from(candidateFunctions)
        .filter(CFunctionEntryNode.class)
        .filter(f -> matchingFunctionCall.test(call, f.getFunctionDefinition().getType()))
        .toList();
  }

  private boolean checkReturnAndParamSizes(CFunctionCall functionCall, CFunctionType functionType) {
    final CFunctionCallExpression functionCallExpression = functionCall.getFunctionCallExpression();
    final MachineModel machine = cfa.getMachineModel();

    CType declRet = functionType.getReturnType();
    CType actRet = functionCallExpression.getExpressionType();
    if (machine.getSizeof(declRet) != machine.getSizeof(actRet)) {
      logger.log(Level.FINEST, "Function call", functionCallExpression.toASTString(), "with type", actRet,
          "does not match function", functionType, "with return type", declRet,
          "because of return types with different sizes.");
      return false;
    }

    List<CType> declParams = functionType.getParameters();
    List<CExpression> exprParams = functionCallExpression.getParameterExpressions();
    for (int i=0; i<declParams.size(); i++) {
      CType dt = declParams.get(i);
      CType et = exprParams.get(i).getExpressionType();
      if (machine.getSizeof(dt) != machine.getSizeof(et)) {
        logger.log(Level.FINEST, "Function call", functionCallExpression.toASTString(),
            "does not match function", functionType,
            "because actual parameter", i, "has type", et, "instead of", dt,
            "(differing sizes).");
        return false;
      }
    }

    return true;
  }

  private boolean checkReturnAndParamTypes(CFunctionCall functionCall, CFunctionType functionType) {
    final CFunctionCallExpression functionCallExpression = functionCall.getFunctionCallExpression();

    CType declRet = functionType.getReturnType();
    CType actRet = functionCallExpression.getExpressionType();
    if (!isCompatibleType(declRet, actRet)) {
      logger.log(Level.FINEST, "Function call", functionCallExpression.toASTString(), "with type", actRet,
          "does not match function", functionType, "with return type", declRet);
      return false;
    }

    List<CType> declParams = functionType.getParameters();
    List<CExpression> exprParams = functionCallExpression.getParameterExpressions();
    for (int i=0; i<declParams.size(); i++) {
      CType dt = declParams.get(i);
      CType et = exprParams.get(i).getExpressionType();
      if (!isCompatibleType(dt, et)) {
        logger.log(Level.FINEST, "Function call", functionCallExpression.toASTString(),
            "does not match function", functionType,
            "because actual parameter", i, "has type", et, "instead of", dt);
        return false;
      }
    }

    return true;
  }

  /**
   * Exclude void functions if the return value of the function is used in an assignment.
   */
  private boolean checkReturnValue(CFunctionCall call, CFunctionType functionType) {
    if (call instanceof CFunctionCallAssignmentStatement) {
      CType returnType = functionType.getReturnType().getCanonicalType();
      if (returnType instanceof CVoidType) {
        return false;
      }
    }
    return true;
  }

  /**
   * Check whether two types are assignment compatible.
   *
   * @param declaredType The type that is declared (e.g., as variable type).
   * @param actualType The type that is actually used (e.g., as type of an expression).
   * @return True if a value of actualType may be assigned to a variable of declaredType.
   */
  private boolean isCompatibleType(CType declaredType, CType actualType) {
    // TODO this needs to be implemented
    // Type equality is too strong.
    // After this is implemented, change the default of functionSets
    // to USED_IN_CODE, EQ_PARAM_TYPES
    return declaredType.getCanonicalType().equals(actualType.getCanonicalType());
  }

  private boolean checkParamCount(CFunctionCall functionCall, CFunctionType functionType) {
    final CFunctionCallExpression functionCallExpression = functionCall.getFunctionCallExpression();

    //get the parameter expression
    List<CExpression> parameters = functionCallExpression.getParameterExpressions();

    // check if the number of function parameters are right
    int declaredParameters = functionType.getParameters().size();
    int actualParameters = parameters.size();

    if (actualParameters < declaredParameters) {
      logger.log(
          Level.FINEST,
          "Function call",
          functionCallExpression.toASTString(),
          "does not match function",
          functionType,
          "because there are not enough actual parameters.");
      return false;
    }

    if (!functionType.takesVarArgs() && actualParameters > declaredParameters) {
      logger.log(
          Level.FINEST,
          "Function call",
          functionCallExpression.toASTString(),
          "does not match function",
          functionType,
          "because there are too many actual parameters.");
      return false;
    }
    return true;
  }
}
