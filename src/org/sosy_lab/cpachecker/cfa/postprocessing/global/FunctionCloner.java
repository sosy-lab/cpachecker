// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.global;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArrayDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CArrayRangeDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDefDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBitFieldType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionTypeWithNames;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.cfa.types.c.DefaultCTypeVisitor;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.CFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;
import org.sosy_lab.cpachecker.util.Pair;

/**
 * This Class can be used to clone a function from the CFA. You need to specify a new functionName.
 *
 * <p>All edges and nodes inside the function are cloned, their content (expressions, ...) is cloned
 * from the original function. If the old functionname is part of an object, the corresponding new
 * name is the new functionname.
 *
 * <p>There should not be any functioncall- or return-edges. Currently only the language C is
 * supported.
 */
class FunctionCloner implements CFAVisitor {

  private static final String ONLY_C_SUPPORTED = "only C supported";
  private static final String SUPERGRAPH_BUILD_TOO_EARLY =
      "functions should be cloned before building the supergraph";

  // local caches
  // values will be used as CFANodes-Set for building new CFAs
  private final Map<CFANode, CFANode> nodeCache = new HashMap<>();
  private final IdentityHashMap<AAstNode, AAstNode> astCache = new IdentityHashMap<>();
  private final IdentityHashMap<Type, Type> typeCache = new IdentityHashMap<>();
  private final CExpressionCloner expCloner = new CExpressionCloner();
  private final CTypeCloner typeCloner = new CTypeCloner();

  private final String oldFunctionName;
  private final String newFunctionName;
  // needed to replace functioncalls, where args stay equal, but functionname changes
  private final boolean replaceFunctionOnly;

  /** FunctionCloner clones a function of the cfa and uses a new functionName. */
  public FunctionCloner(
      final String oldFunctionName,
      final String newFunctionName,
      final boolean replaceFunctionOnly) {
    this.oldFunctionName = oldFunctionName;
    this.newFunctionName = newFunctionName;
    this.replaceFunctionOnly = replaceFunctionOnly;
  }

  /**
   * clones a complete function and returns the new functionstart and the nodes of the new function.
   */
  public static Pair<FunctionEntryNode, Collection<CFANode>> cloneCFA(
      final FunctionEntryNode pFunctionstart, final String newFunctionName) {

    final String oldFunctionName = pFunctionstart.getFunctionName();
    assert !oldFunctionName.equals(newFunctionName);
    final FunctionCloner visitor = new FunctionCloner(oldFunctionName, newFunctionName, false);

    CFATraversal.dfs().ignoreFunctionCalls().traverseOnce(pFunctionstart, visitor);

    return Pair.of(
        (FunctionEntryNode) visitor.nodeCache.get(pFunctionstart), visitor.nodeCache.values());
  }

  @Override
  public TraversalProcess visitEdge(final CFAEdge edge) {

    final CFANode start = cloneNode(edge.getPredecessor(), true);
    final CFANode end = cloneNode(edge.getSuccessor(), true);

    final CFAEdge newEdge = cloneEdge(edge, start, end);

    assert newEdge.getPredecessor().equals(start) && newEdge.getSuccessor().equals(end);

    CFACreationUtils.addEdgeUnconditionallyToCFA(newEdge);

    return TraversalProcess.CONTINUE;
  }

  @Override
  public TraversalProcess visitNode(final CFANode node) {
    // TODO do we need to override this function?
    // each cloned edge also clones its predecessor and successor (if nodes not in nodeMapping).

    cloneNode(node, true);

    return TraversalProcess.CONTINUE;
  }

  /** clone edge with new start-node and end-node, copy content of old edge into new edge. */
  @SuppressWarnings("unchecked")
  public <T extends CFAEdge> T cloneEdge(final T edge, final CFANode start, final CFANode end) {

    final FileLocation loc = edge.getFileLocation();
    final String rawStatement = edge.getRawStatement();

    // clone correct type of edge
    final CFAEdge newEdge;
    switch (edge.getEdgeType()) {
      case BlankEdge:
        {
          newEdge = new BlankEdge(rawStatement, loc, start, end, edge.getDescription());
          break;
        }

      case AssumeEdge:
        {
          if (edge instanceof CAssumeEdge) {
            final CAssumeEdge e = (CAssumeEdge) edge;
            newEdge =
                new CAssumeEdge(
                    rawStatement,
                    loc,
                    start,
                    end,
                    cloneAst(e.getExpression()),
                    e.getTruthAssumption(),
                    e.isSwapped(),
                    e.isArtificialIntermediate());
          } else {
            throw new AssertionError(ONLY_C_SUPPORTED);
          }
          break;
        }

      case StatementEdge:
        {
          if (edge instanceof CFunctionSummaryStatementEdge) {
            throw new AssertionError(SUPERGRAPH_BUILD_TOO_EARLY);
          } else if (edge instanceof CStatementEdge) {
            newEdge =
                new CStatementEdge(
                    rawStatement,
                    cloneAst(((CStatementEdge) edge).getStatement()),
                    loc,
                    start,
                    end);
          } else {
            throw new AssertionError(ONLY_C_SUPPORTED);
          }
          break;
        }

      case DeclarationEdge:
        {
          if (edge instanceof CDeclarationEdge) {
            newEdge =
                new CDeclarationEdge(
                    rawStatement,
                    loc,
                    start,
                    end,
                    cloneAst(((CDeclarationEdge) edge).getDeclaration()));
          } else {
            throw new AssertionError(ONLY_C_SUPPORTED);
          }
          break;
        }

      case ReturnStatementEdge:
        {
          assert end instanceof FunctionExitNode
              : "Expected FunctionExitNode: " + end + ", " + end.getClass();
          if (edge instanceof CReturnStatementEdge) {
            newEdge =
                new CReturnStatementEdge(
                    rawStatement,
                    cloneAst(((CReturnStatementEdge) edge).getReturnStatement()),
                    loc,
                    start,
                    (FunctionExitNode) end);
          } else {
            throw new AssertionError(ONLY_C_SUPPORTED);
          }
          break;
        }

      case FunctionCallEdge:
        {
          throw new AssertionError(SUPERGRAPH_BUILD_TOO_EARLY);

          // if (edge instanceof CFunctionCallEdge) {
          //   CFunctionCallEdge e = (CFunctionCallEdge) edge;
          //   newEdge = new CFunctionCallEdge(rawStatement, line, start, (CFunctionEntryNode) end,
          //       cloneAst((CFunctionCall) e.getRawAST().get()), e.getSummaryEdge());
          // } else {
          //   throw new AssertionError();
          // }
          // break;
        }

      case FunctionReturnEdge:
        {
          throw new AssertionError(SUPERGRAPH_BUILD_TOO_EARLY);

          // if (edge instanceof CFunctionReturnEdge) {
          //   CFunctionReturnEdge e = (CFunctionReturnEdge) edge;
          //   newEdge = new CFunctionReturnEdge(loc, (FunctionExitNode) start, end,
          // cloneEdge(e.getSummaryEdge()));
          // } else {
          //   throw new AssertionError(ONLY_C_SUPPORTED);
          // }
          // break;
        }

      case CallToReturnEdge:
        {
          throw new AssertionError(SUPERGRAPH_BUILD_TOO_EARLY);

          // if (edge instanceof CFunctionSummaryEdge) {
          //   CFunctionSummaryEdge e = (CFunctionSummaryEdge) edge;
          //   newEdge = new CFunctionSummaryEdge(rawStatement, loc, start, end,
          // cloneAst(e.getExpression()));
          // } else {
          //   throw new AssertionError();
          // }
          // break;
        }

      default:
        throw new AssertionError("unhandled type of edge: " + edge.getEdgeType());
    }

    return (T) newEdge;
  }

  /** clones a node: copies all content and inserts a new functionName */
  @SuppressWarnings("unchecked")
  private <T extends CFANode> T cloneNode(@NonNull final T node, final boolean addToMapping) {
    Preconditions.checkNotNull(node);

    if (nodeCache.containsKey(node)) {
      return (T) nodeCache.get(node);
    }

    // clone correct type of node
    final CFANode newNode;
    if (node instanceof CFALabelNode) {
      newNode = new CFALabelNode(cloneAst(node.getFunction()), ((CFALabelNode) node).getLabel());

    } else if (node instanceof CFATerminationNode) {
      newNode = new CFATerminationNode(cloneAst(node.getFunction()));

    } else if (node instanceof FunctionExitNode) {
      newNode = new FunctionExitNode(cloneAst(node.getFunction()));

    } else if (node instanceof CFunctionEntryNode) {
      final CFunctionEntryNode n = (CFunctionEntryNode) node;
      final FunctionExitNode exitNode = n.getExitNode();

      // exitNode is maybe not part of the CFA, but accessible through entryNode.getExitNode().
      final boolean isExitNodeReachable = exitNode.getNumEnteringEdges() > 0;

      final FunctionExitNode newExitNode = cloneNode(exitNode, isExitNodeReachable);
      Optional<CVariableDeclaration> returnVariable = n.getReturnVariable();
      if (returnVariable.isPresent()) {
        returnVariable = Optional.of(cloneAst(returnVariable.orElseThrow()));
      }
      final CFunctionEntryNode entryNode =
          new CFunctionEntryNode(
              n.getFileLocation(),
              cloneAst(n.getFunctionDefinition()),
              newExitNode,
              returnVariable);
      newExitNode.setEntryNode(entryNode); // this must not change hashvalue!
      newNode = entryNode;

    } else {
      assert node.getClass() == CFANode.class
          : "unhandled subclass for CFANode: " + node.getClass();
      newNode = new CFANode(cloneAst(node.getFunction()));
    }

    // copy information from original node
    newNode.setReversePostorderId(node.getReversePostorderId());
    if (node.isLoopStart()) {
      newNode.setLoopStart();
    }

    if (addToMapping) {
      nodeCache.put(node, newNode);
    }

    return (T) newNode;
  }

  @SuppressWarnings("unchecked")
  private @Nullable <T extends AAstNode> T cloneAst(final T ast) {

    if (ast == null) {
      return null;
    }

    if (astCache.containsKey(ast)) {
      return (T) astCache.get(ast);
    }

    final AAstNode newAst = cloneAstDirect(ast);

    astCache.put(ast, newAst);

    return (T) newAst;
  }

  /** returns a new list with cloned elements */
  private <T extends AAstNode> List<T> cloneAstList(final List<T> astList) {
    final List<T> list = new ArrayList<>(astList.size());
    for (T ast : astList) {
      list.add(cloneAst(ast));
    }
    return list;
  }

  /** returns a deep copy of the ast-node, and changes old functionname to new one, if needed. */
  private AAstNode cloneAstDirect(AAstNode ast) {

    final FileLocation loc = ast.getFileLocation();

    if (ast instanceof CRightHandSide) {

      if (ast instanceof CExpression) {
        return ((CExpression) ast).accept(expCloner);

      } else if (ast instanceof CFunctionCallExpression) {
        CFunctionCallExpression func = (CFunctionCallExpression) ast;
        return new CFunctionCallExpression(
            loc,
            cloneType(func.getExpressionType()),
            cloneAst(func.getFunctionNameExpression()),
            cloneAstList(func.getParameterExpressions()),
            cloneAst(func.getDeclaration()));
      }

    } else if (ast instanceof CInitializer) {

      if (ast instanceof CInitializerExpression) {
        return new CInitializerExpression(
            loc, cloneAst(((CInitializerExpression) ast).getExpression()));

      } else if (ast instanceof CInitializerList) {
        return new CInitializerList(loc, cloneAstList(((CInitializerList) ast).getInitializers()));

      } else if (ast instanceof CDesignatedInitializer) {
        CDesignatedInitializer di = (CDesignatedInitializer) ast;
        return new CDesignatedInitializer(
            loc, cloneAstList(di.getDesignators()), cloneAst(di.getRightHandSide()));
      }

    } else if (ast instanceof CSimpleDeclaration) {

      if (ast instanceof CVariableDeclaration) {
        CVariableDeclaration decl = (CVariableDeclaration) ast;
        CVariableDeclaration newDecl =
            new CVariableDeclaration(
                loc,
                decl.isGlobal(),
                decl.getCStorageClass(),
                cloneType(decl.getType()),
                decl.getName(),
                decl.getOrigName(),
                changeQualifiedName(decl.getQualifiedName()),
                null);
        // cache the declaration, then clone the initializer and add it.
        // this is needed for the following code: int x = x;
        astCache.put(ast, newDecl);
        newDecl.addInitializer(cloneAst(decl.getInitializer()));
        return newDecl;

      } else if (ast instanceof CFunctionDeclaration) {
        CFunctionDeclaration decl = (CFunctionDeclaration) ast;
        List<CParameterDeclaration> l = new ArrayList<>(decl.getParameters().size());
        for (CParameterDeclaration param : decl.getParameters()) {
          l.add(cloneAst(param));
        }
        return new CFunctionDeclaration(
            loc,
            cloneType(decl.getType()),
            changeName(decl.getName()),
            decl.getOrigName(),
            l,
            decl.getAttributes());

      } else if (ast instanceof CComplexTypeDeclaration) {
        CComplexTypeDeclaration decl = (CComplexTypeDeclaration) ast;
        return new CComplexTypeDeclaration(loc, decl.isGlobal(), cloneType(decl.getType()));

      } else if (ast instanceof CTypeDefDeclaration) {
        CTypeDefDeclaration decl = (CTypeDefDeclaration) ast;
        return new CTypeDefDeclaration(
            loc,
            decl.isGlobal(),
            cloneType(decl.getType()),
            decl.getName(),
            changeQualifiedName(decl.getQualifiedName()));

      } else if (ast instanceof CParameterDeclaration) {
        // we do not cache CParameterDeclaration, but clone it directly,
        // because its equals- and hashcode-Method are insufficient for caching
        // TODO do we need to cache it?
        CParameterDeclaration decl = (CParameterDeclaration) ast;
        CParameterDeclaration newDecl =
            new CParameterDeclaration(loc, cloneType(decl.getType()), decl.getName());
        newDecl.setQualifiedName(changeQualifiedName(decl.getQualifiedName()));
        return newDecl;

      } else if (ast instanceof CEnumType.CEnumerator) {
        CEnumType.CEnumerator decl = (CEnumType.CEnumerator) ast;
        return new CEnumType.CEnumerator(
            loc,
            decl.getName(),
            changeQualifiedName(decl.getQualifiedName()),
            decl.getType(),
            decl.getValue());
      }

    } else if (ast instanceof CStatement) {

      if (ast instanceof CFunctionCallAssignmentStatement) {
        CFunctionCallAssignmentStatement stat = (CFunctionCallAssignmentStatement) ast;
        return new CFunctionCallAssignmentStatement(
            loc, cloneAst(stat.getLeftHandSide()), cloneAst(stat.getRightHandSide()));

      } else if (ast instanceof CExpressionAssignmentStatement) {
        CExpressionAssignmentStatement stat = (CExpressionAssignmentStatement) ast;
        return new CExpressionAssignmentStatement(
            loc, cloneAst(stat.getLeftHandSide()), cloneAst(stat.getRightHandSide()));

      } else if (ast instanceof CFunctionCallStatement) {
        return new CFunctionCallStatement(
            loc, cloneAst(((CFunctionCallStatement) ast).getFunctionCallExpression()));

      } else if (ast instanceof CExpressionStatement) {
        return new CExpressionStatement(
            loc, cloneAst(((CExpressionStatement) ast).getExpression()));
      }

    } else if (ast instanceof CReturnStatement) {
      Optional<CExpression> returnExp = ((CReturnStatement) ast).getReturnValue();
      if (returnExp.isPresent()) {
        returnExp = Optional.of(cloneAst(returnExp.orElseThrow()));
      }
      Optional<CAssignment> returnAssignment = ((CReturnStatement) ast).asAssignment();
      if (returnAssignment.isPresent()) {
        returnAssignment = Optional.of(cloneAst(returnAssignment.orElseThrow()));
      }
      return new CReturnStatement(loc, returnExp, returnAssignment);

    } else if (ast instanceof CDesignator) {

      if (ast instanceof CArrayDesignator) {
        return new CArrayDesignator(
            loc, cloneAst(((CArrayDesignator) ast).getSubscriptExpression()));

      } else if (ast instanceof CArrayRangeDesignator) {
        return new CArrayRangeDesignator(
            loc,
            cloneAst(((CArrayRangeDesignator) ast).getFloorExpression()),
            cloneAst(((CArrayRangeDesignator) ast).getCeilExpression()));

      } else if (ast instanceof CFieldDesignator) {
        return new CFieldDesignator(loc, ((CFieldDesignator) ast).getFieldName());
      }
    }

    throw new AssertionError("unhandled ASTNode " + ast + " of " + ast.getClass());
  }

  @SuppressWarnings("unchecked")
  private @Nullable <T extends Type> T cloneType(T type) {

    if (type == null) {
      return null;
    }

    if (typeCache.containsKey(type)) {
      return (T) typeCache.get(type);
    }

    final Type newType = cloneTypeDirect(type);

    typeCache.put(type, newType);

    return (T) newType;
  }

  private Type cloneTypeDirect(Type type) {
    if (type instanceof CType) {
      return ((CType) type).accept(typeCloner);
    }

    throw new AssertionError("unhandled Type " + type + " of " + type.getClass());
  }

  /**
   * clones CExpressions and calls cloneAst on non-expression-content. Note: caching sub-expressions
   * is useless because of the location, that is different for each expression.
   */
  private class CExpressionCloner extends DefaultCExpressionVisitor<CExpression, NoException> {

    @Override
    protected CExpression visitDefault(CExpression exp) {
      return exp;
    }

    @Override
    public CExpression visit(CBinaryExpression exp) {
      return new CBinaryExpression(
          exp.getFileLocation(),
          exp.getExpressionType(),
          exp.getCalculationType(),
          exp.getOperand1().accept(this),
          exp.getOperand2().accept(this),
          exp.getOperator());
    }

    @Override
    public CExpression visit(CCastExpression exp) {
      return new CCastExpression(
          exp.getFileLocation(), cloneType(exp.getExpressionType()), exp.getOperand().accept(this));
    }

    @Override
    public CExpression visit(CUnaryExpression exp) {
      return new CUnaryExpression(
          exp.getFileLocation(),
          cloneType(exp.getExpressionType()),
          exp.getOperand().accept(this),
          exp.getOperator());
    }

    @Override
    public CExpression visit(CArraySubscriptExpression exp) {
      return new CArraySubscriptExpression(
          exp.getFileLocation(),
          cloneType(exp.getExpressionType()),
          exp.getArrayExpression().accept(this),
          exp.getSubscriptExpression().accept(this));
    }

    @Override
    public CExpression visit(CFieldReference exp) {
      return new CFieldReference(
          exp.getFileLocation(),
          cloneType(exp.getExpressionType()),
          exp.getFieldName(),
          exp.getFieldOwner().accept(this),
          exp.isPointerDereference());
    }

    @Override
    public CExpression visit(CIdExpression exp) {
      // check for self-recursion --> replace self-calling functioncalls with new self-calling
      // functioncalls
      if (exp.getExpressionType() instanceof CFunctionType) {
        return new CIdExpression(
            exp.getFileLocation(),
            cloneType(exp.getExpressionType()),
            changeName(exp.getName()),
            cloneAst(exp.getDeclaration()));
      } else {
        return new CIdExpression(
            exp.getFileLocation(),
            cloneType(exp.getExpressionType()),
            exp.getName(),
            cloneAst(exp.getDeclaration()));
      }
    }

    @Override
    public CExpression visit(CPointerExpression exp) {
      return new CPointerExpression(
          exp.getFileLocation(), cloneType(exp.getExpressionType()), exp.getOperand().accept(this));
    }

    @Override
    public CExpression visit(CComplexCastExpression exp) {
      return new CComplexCastExpression(
          exp.getFileLocation(),
          cloneType(exp.getExpressionType()),
          exp.getOperand().accept(this),
          exp.getType(),
          exp.isRealCast());
    }
  }

  private class CTypeCloner extends DefaultCTypeVisitor<CType, NoException> {

    @Override
    public CType visitDefault(CType t) {
      return t;
    }

    @Override
    public CType visit(CArrayType type) {
      return new CArrayType(
          type.isConst(), type.isVolatile(), type.getType().accept(this), type.getLength());
    }

    @Override
    public CType visit(CCompositeType type) {
      // possible problem: compositeType contains itself again -> recursion
      // solution: cache the empty compositeType and fill it later.
      CCompositeType comp =
          new CCompositeType(
              type.isConst(),
              type.isVolatile(),
              type.getKind(),
              type.getName(),
              type.getOrigName());
      typeCache.put(type, comp);

      // convert members and set them
      List<CCompositeTypeMemberDeclaration> l = new ArrayList<>(type.getMembers().size());
      for (CCompositeTypeMemberDeclaration decl : type.getMembers()) {
        l.add(new CCompositeTypeMemberDeclaration(decl.getType().accept(this), decl.getName()));
      }
      comp.setMembers(l);

      return comp;
    }

    @Override
    public CType visit(CElaboratedType type) {
      return new CElaboratedType(
          type.isConst(),
          type.isVolatile(),
          type.getKind(),
          type.getName(),
          type.getOrigName(),
          cloneType(type.getRealType()));
    }

    @Override
    public CType visit(CEnumType type) {
      List<CEnumType.CEnumerator> l = new ArrayList<>(type.getEnumerators().size());
      for (CEnumType.CEnumerator e : type.getEnumerators()) {
        CEnumType.CEnumerator enumType =
            new CEnumType.CEnumerator(
                e.getFileLocation(),
                e.getName(),
                changeQualifiedName(e.getQualifiedName()),
                e.getType(),
                (e.hasValue() ? e.getValue() : null));
        enumType.setEnum(e.getEnum());
        l.add(enumType);
      }
      return new CEnumType(
          type.isConst(), type.isVolatile(), l, type.getName(), type.getOrigName());
    }

    @Override
    public CType visit(CFunctionType type) {
      final CFunctionType funcType;
      if (type instanceof CFunctionTypeWithNames) {
        List<CParameterDeclaration> l = new ArrayList<>(type.getParameters().size());
        for (CParameterDeclaration param :
            ((CFunctionTypeWithNames) type).getParameterDeclarations()) {
          l.add(cloneAst(param));
        }
        funcType = new CFunctionTypeWithNames(type.getReturnType(), l, type.takesVarArgs());
      } else {
        assert type.getClass() == CFunctionType.class;
        List<CType> l = new ArrayList<>(type.getParameters().size());
        for (CType param : type.getParameters()) {
          l.add(cloneType(param));
        }
        funcType = new CFunctionType(type.getReturnType(), l, type.takesVarArgs());
      }
      if (type.getName() != null) {
        funcType.setName(changeName(type.getName()));
      }
      return funcType;
    }

    @Override
    public CType visit(CPointerType type) {
      return new CPointerType(type.isConst(), type.isVolatile(), type.getType().accept(this));
    }

    @Override
    public CType visit(CTypedefType type) {
      return new CTypedefType(
          type.isConst(), type.isVolatile(), type.getName(), type.getRealType().accept(this));
    }

    @Override
    public CType visit(CBitFieldType pCBitFieldType) {
      return new CBitFieldType(
          pCBitFieldType.getType().accept(this), pCBitFieldType.getBitFieldSize());
    }
  }

  /** replace old functionname with new one. */
  private String changeName(final String name) {
    return oldFunctionName.equals(name) ? newFunctionName : name;
  }

  /** if qualifiedName ist in current scope, replace old functionname with new one. */
  private String changeQualifiedName(final String qualifiedName) {
    if (!replaceFunctionOnly && qualifiedName.startsWith(oldFunctionName + "::")) {
      return newFunctionName + qualifiedName.substring(oldFunctionName.length());
    }
    return qualifiedName;
  }
}
