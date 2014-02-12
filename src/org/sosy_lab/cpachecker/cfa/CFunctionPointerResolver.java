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
package org.sosy_lab.cpachecker.cfa;

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.CFAUtils.leavingEdges;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.ast.IADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.util.CFAUtils;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

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

  @Option(name="analysis.functionPointerEdgesForUnknownPointer",
      description="Create edge for skipping a function pointer call if its value is unknown.")
  private boolean createUndefinedFunctionCall = true;

  private enum FunctionSet {
    // The items here need to be declared in the order they should be used when checking function.
    ALL, //all defined functions considered (Warning: some CPAs require at least EQ_PARAM_SIZES)
    USED_IN_CODE, //includes only functions which address is taken in the code
    EQ_PARAM_COUNT, //all functions with matching number of parameters considered
    EQ_PARAM_SIZES, //all functions with parameters with matching sizes
    EQ_PARAM_TYPES, //all functions with matching number and types of parameters considered (implies EQ_PARAM_SIZES)
  }

  @Option(name="analysis.functionPointerTargets",
      description="potential targets for call edges created for function pointer calls")
  private Set<FunctionSet> functionSets = ImmutableSet.of(FunctionSet.USED_IN_CODE, FunctionSet.EQ_PARAM_SIZES);

  private final Collection<FunctionEntryNode> candidateFunctions;

  private final Predicate<Pair<CFunctionCallExpression, CFunctionType>> matchingFunctionCall;

  private final MutableCFA cfa;
  private final LogManager logger;

  public CFunctionPointerResolver(MutableCFA pCfa, List<Pair<IADeclaration, String>> pGlobalVars,
      Configuration config, LogManager pLogger) throws InvalidConfigurationException {
    cfa = pCfa;
    logger = pLogger;

    config.inject(this);

    matchingFunctionCall = getFunctionSetPredicate(functionSets);

    if (functionSets.contains(FunctionSet.USED_IN_CODE)) {
      CReferencedFunctionsCollector varCollector = new CReferencedFunctionsCollector();
      for (CFANode node : cfa.getAllNodes()) {
        for (CFAEdge edge : leavingEdges(node)) {
          varCollector.visitEdge(edge);
        }
      }
      for (Pair<IADeclaration, String> decl : pGlobalVars) {
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

      if (logger.wouldBeLogged(Level.ALL)) {
        logger.log(Level.ALL, "Possible target functions of function pointers:\n",
            Joiner.on('\n').join(candidateFunctions));
      }
    } else {
      candidateFunctions = cfa.getAllFunctionHeads();
    }

  }

  private Predicate<Pair<CFunctionCallExpression, CFunctionType>> getFunctionSetPredicate(Collection<FunctionSet> pFunctionSets) {
    // note that this set is sorted according to the declaration order of the enum
    EnumSet<FunctionSet> functionSets = EnumSet.copyOf(pFunctionSets);

    if (functionSets.contains(FunctionSet.EQ_PARAM_TYPES)
        || functionSets.contains(FunctionSet.EQ_PARAM_SIZES)) {
      functionSets.add(FunctionSet.EQ_PARAM_COUNT); // TYPES and SIZES need COUNT checked first
    }

    List<Predicate<Pair<CFunctionCallExpression, CFunctionType>>> predicates = new ArrayList<>();
    for (FunctionSet functionSet : functionSets) {
      switch (functionSet) {
      case ALL:
        // do nothing
        break;
      case EQ_PARAM_COUNT:
        predicates.add(new Predicate<Pair<CFunctionCallExpression, CFunctionType>>() {
          @Override
          public boolean apply(Pair<CFunctionCallExpression, CFunctionType> pInput) {
            boolean result = checkParamSizes(pInput.getFirst(), pInput.getSecond());
            if (!result) {
              logger.log(Level.FINEST, "Function call", pInput.getFirst().toASTString(),
                  "does not match function", pInput.getSecond(), "because of number of parameters.");
            }
            return result;
          }
        });
        break;
      case EQ_PARAM_SIZES:
        predicates.add(new Predicate<Pair<CFunctionCallExpression, CFunctionType>>() {
          @Override
          public boolean apply(Pair<CFunctionCallExpression, CFunctionType> pInput) {
            return checkReturnAndParamSizes(pInput.getFirst(), pInput.getSecond());
          }
        });
        break;
      case EQ_PARAM_TYPES:
        predicates.add(new Predicate<Pair<CFunctionCallExpression, CFunctionType>>() {
          @Override
          public boolean apply(Pair<CFunctionCallExpression, CFunctionType> pInput) {
            return checkReturnAndParamTypes(pInput.getFirst(), pInput.getSecond());
          }
        });
        break;
      case USED_IN_CODE:
        // Not necessary, only matching functions are in the
        // candidateFunctions set
        break;
      default:
        throw new AssertionError();
      }
    }
    return Predicates.and(predicates);
  }

  /**
   * This method traverses the whole CFA,
   * potentially replacing function pointer calls with regular function calls.
   */
  public void resolveFunctionPointers() {
    for (FunctionEntryNode functionStartNode : cfa.getAllFunctionHeads()) {
      resolveFunctionPointers(functionStartNode);
    }
  }

  /**
   * This method replaces function pointer calls inside the given function
   * with regular function calls.
   */
  private void resolveFunctionPointers(FunctionEntryNode initialNode) {
    // we use a worklist algorithm
    Deque<CFANode> workList = new ArrayDeque<>();
    Set<CFANode> processed = new HashSet<>();

    workList.addLast(initialNode);

    while (!workList.isEmpty()) {
      CFANode node = workList.pollFirst();
      if (!processed.add(node)) {
        // already handled
        continue;
      }

      for (CFAEdge edge : leavingEdges(node).toList()) {
        if (edge instanceof CStatementEdge) {
          CStatementEdge statement = (CStatementEdge)edge;
          CStatement stmt = statement.getStatement();
          if (stmt instanceof CFunctionCall) {
            CFunctionCall call = (CFunctionCall)stmt;
            if (isFunctionPointerCall(call)) {
              replaceFunctionPointerCall(call, statement);
            }
          }
        }

        workList.add(edge.getSuccessor());
      }
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
    Collection<CFunctionEntryNode> funcs = getFunctionSet(fExp);

    if (funcs.isEmpty()) {
      // no possible targets, we leave the CFA unchanged and print a warning
      logger.log(Level.WARNING, "Function pointer", nameExp.toASTString(),
          "with type", nameExp.getExpressionType().toASTString("*"),
          "is called in line", statement.getLineNumber() + ",",
          "but no possible target functions were found.");
      return;
    }

    logger.log(Level.FINEST, "Inserting edges for the function pointer",
        nameExp.toASTString(), "with type", nameExp.getExpressionType().toASTString("*"),
        "to the functions", from(funcs).transform(new Function<CFANode, String>() {
            @Override
            public String apply(CFANode pInput) {
              return pInput.getFunctionName();
            }
          }));

    CFANode start = statement.getPredecessor();
    CFANode end = statement.getSuccessor();
    // delete old edge
    CFACreationUtils.removeEdgeFromNodes(statement);

    if (nameExp instanceof CPointerExpression) {
      CExpression operand = ((CPointerExpression)nameExp).getOperand();
      CType operandType = operand.getExpressionType().getCanonicalType();
      if (operand instanceof CIdExpression
          && operandType instanceof CPointerType
          && ((CPointerType)operandType).getType() instanceof CFunctionType) {
        // *fp is the same as fp
        nameExp = operand;
      }
    }

    CFANode rootNode = start;
    for (FunctionEntryNode fNode : funcs) {
      CFANode thenNode = newCFANode(start.getLineNumber(), start.getFunctionName());
      CFANode elseNode = newCFANode(start.getLineNumber(), start.getFunctionName());
      CIdExpression func = new CIdExpression(nameExp.getFileLocation(),
                                              (CType)fNode.getFunctionDefinition().getType(),
                                              fNode.getFunctionName(),
                                              (CSimpleDeclaration)fNode.getFunctionDefinition());
      CUnaryExpression amper = new CUnaryExpression(nameExp.getFileLocation(),
          func.getExpressionType(), func, CUnaryExpression.UnaryOperator.AMPER);

      final CBinaryExpressionBuilder binExprBuilder = new CBinaryExpressionBuilder(cfa.getMachineModel(), logger);
      CBinaryExpression condition = binExprBuilder.buildBinaryExpression(nameExp, amper, BinaryOperator.EQUALS);

      addConditionEdges(condition, rootNode, thenNode, elseNode, start.getLineNumber());


      CFANode retNode = newCFANode(start.getLineNumber(), start.getFunctionName());
      //create special summary edge
      //thenNode-->retNode
      String pRawStatement = "pointer call(" + fNode.getFunctionName() + ") " + statement.getRawStatement();

      //replace function call by pointer expression with regular call by name (in functionCall and edge.getStatement())
      CFunctionCall regularCall = createRegularCall(functionCall, fNode);

      createCallEdge(statement.getLineNumber(), pRawStatement,
          thenNode, retNode, regularCall);

      //retNode-->end
      BlankEdge be = new BlankEdge("skip", statement.getLineNumber(), retNode, end, "skip");
      CFACreationUtils.addEdgeUnconditionallyToCFA(be);

      rootNode = elseNode;
    }

    //rootNode --> end
    if (createUndefinedFunctionCall) {
      CStatementEdge summaryStatementEdge = new CStatementEdge(
          statement.getRawStatement(), statement.getStatement(),
          statement.getLineNumber(), rootNode, end);

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
  private CFANode newCFANode(final int filelocStart, final String functionName) {
    assert cfa != null;
    CFANode nextNode = new CFANode(filelocStart, functionName);
    cfa.addNode(nextNode);
    return nextNode;
  }

  private void createCallEdge(int lineNumber, String pRawStatement,
      CFANode predecessorNode, CFANode successorNode, CFunctionCall functionCall) {
    CStatementEdge callEdge = new CStatementEdge(pRawStatement,
        functionCall, lineNumber,
        predecessorNode, successorNode);
    CFACreationUtils.addEdgeUnconditionallyToCFA(callEdge);
  }

  /** This method adds 2 edges to the cfa:
   * 1. trueEdge from rootNode to thenNode and
   * 2. falseEdge from rootNode to elseNode.
   * @category conditions
   */
  private void addConditionEdges(CExpression condition, CFANode rootNode,
      CFANode thenNode, CFANode elseNode, int filelocStart) {
    // edge connecting condition with thenNode
    final CAssumeEdge trueEdge = new CAssumeEdge(condition.toASTString(),
        filelocStart, rootNode, thenNode, condition, true);
    CFACreationUtils.addEdgeToCFA(trueEdge, logger);

    // edge connecting condition with elseNode
    final CAssumeEdge falseEdge = new CAssumeEdge("!(" + condition.toASTString() + ")",
        filelocStart, rootNode, elseNode, condition, false);
    CFACreationUtils.addEdgeToCFA(falseEdge, logger);
  }

  private List<CFunctionEntryNode> getFunctionSet(final CFunctionCallExpression call) {
    return from(candidateFunctions)
            .filter(CFunctionEntryNode.class)
            .filter(Predicates.compose(matchingFunctionCall,
                      new Function<CFunctionEntryNode, Pair<CFunctionCallExpression, CFunctionType>>() {
                        @Override
                        public Pair<CFunctionCallExpression, CFunctionType> apply(CFunctionEntryNode f) {
                          return Pair.of(call, f.getFunctionDefinition().getType());
                        }
                      }))
            .toList();
  }

  private boolean checkReturnAndParamSizes(
      CFunctionCallExpression functionCallExpression, CFunctionType functionType) {
    final MachineModel machine = cfa.getMachineModel();

    try {
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
    } catch (IllegalArgumentException e) {
      // We can't get size of CProblemTypes, and they actually occur for valid C code.
      // This is ugly, but we have no chance but catch this exception and return true here.
      logger.logUserException(Level.INFO, e, functionType.toASTString("") + " " + functionCallExpression);
      return true;
    }

    return true;
  }

  private boolean checkReturnAndParamTypes(
      CFunctionCallExpression functionCallExpression, CFunctionType functionType) {

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

  private final boolean checkParamSizes(CFunctionCallExpression functionCallExpression,
      CFunctionType functionType) {
    //get the parameter expression
    List<CExpression> parameters = functionCallExpression.getParameterExpressions();

    // check if the number of function parameters are right
    int declaredParameters = functionType.getParameters().size();
    int actualParameters = parameters.size();

    return (functionType.takesVarArgs() && declaredParameters <= actualParameters) || (declaredParameters == actualParameters);
  }
}
