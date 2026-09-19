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
import org.sosy_lab.cpachecker.cfa.ast.c.CAstCloner;
import org.sosy_lab.cpachecker.cfa.ast.c.CEnumerator;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
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
class FunctionCloner extends CAstCloner implements CFAVisitor {

  private static final String ONLY_C_SUPPORTED = "only C supported";
  private static final String SUPERGRAPH_BUILD_TOO_EARLY =
      "functions should be cloned before building the supergraph";

  // local caches
  // values will be used as CFANodes-Set for building new CFAs
  private final Map<CFANode, CFANode> nodeCache = new HashMap<>();
  private final IdentityHashMap<AAstNode, AAstNode> astCache = new IdentityHashMap<>();
  private final IdentityHashMap<Type, Type> typeCache = new IdentityHashMap<>();
  private final CTypeCloner typeCloner = new CTypeCloner();

  private final String oldFunctionName;
  private final String newFunctionName;
  // needed to replace functioncalls, where args stay equal, but functionname changes
  private final boolean replaceFunctionOnly;

  /** FunctionCloner clones a function of the CFA and uses a new functionName. */
  public FunctionCloner(
      final String pOldFunctionName,
      final String pNewFunctionName,
      final boolean pReplaceFunctionOnly) {
    this.oldFunctionName = pOldFunctionName;
    this.newFunctionName = pNewFunctionName;
    this.replaceFunctionOnly = pReplaceFunctionOnly;
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

    final CFANode start = cloneNode(edge.getPredecessor());
    final CFANode end = cloneNode(edge.getSuccessor());

    final CFAEdge newEdge = cloneEdge(edge, start, end);

    assert newEdge.getPredecessor().equals(start) && newEdge.getSuccessor().equals(end);

    CFACreationUtils.addEdgeUnconditionallyToCFA(newEdge);

    return TraversalProcess.CONTINUE;
  }

  @Override
  public TraversalProcess visitNode(final CFANode node) {
    // TODO do we need to override this function?
    // each cloned edge also clones its predecessor and successor (if nodes not in nodeMapping).

    cloneNode(node);

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
      case BlankEdge ->
          newEdge = new BlankEdge(rawStatement, loc, start, end, edge.getDescription());
      case AssumeEdge -> {
        if (edge instanceof CAssumeEdge e) {
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
      }
      case StatementEdge -> {
        if (edge instanceof CFunctionSummaryStatementEdge) {
          throw new AssertionError(SUPERGRAPH_BUILD_TOO_EARLY);
        } else if (edge instanceof CStatementEdge cStatementEdge) {
          newEdge =
              new CStatementEdge(
                  rawStatement, cloneAst(cStatementEdge.getStatement()), loc, start, end);
        } else {
          throw new AssertionError(ONLY_C_SUPPORTED);
        }
      }
      case DeclarationEdge -> {
        if (edge instanceof CDeclarationEdge cDeclarationEdge) {
          newEdge =
              new CDeclarationEdge(
                  rawStatement, loc, start, end, cloneAst(cDeclarationEdge.getDeclaration()));
        } else {
          throw new AssertionError(ONLY_C_SUPPORTED);
        }
      }
      case ReturnStatementEdge -> {
        assert end instanceof FunctionExitNode
            : "Expected FunctionExitNode: " + end + ", " + end.getClass();
        if (edge instanceof CReturnStatementEdge cReturnStatementEdge) {
          newEdge =
              new CReturnStatementEdge(
                  rawStatement,
                  cloneAst(cReturnStatementEdge.getReturnStatement()),
                  loc,
                  start,
                  (FunctionExitNode) end);
        } else {
          throw new AssertionError(ONLY_C_SUPPORTED);
        }
      }
      case FunctionCallEdge -> throw new AssertionError(SUPERGRAPH_BUILD_TOO_EARLY);
      case FunctionReturnEdge -> throw new AssertionError(SUPERGRAPH_BUILD_TOO_EARLY);
      case CallToReturnEdge -> throw new AssertionError(SUPERGRAPH_BUILD_TOO_EARLY);
      default -> throw new AssertionError("unhandled type of edge: " + edge.getEdgeType());
    }

    return (T) newEdge;
  }

  /** clones a node: copies all content and inserts a new functionName */
  @SuppressWarnings("unchecked")
  private <T extends CFANode> T cloneNode(@NonNull final T node) {
    Preconditions.checkNotNull(node);

    if (nodeCache.containsKey(node)) {
      return (T) nodeCache.get(node);
    }

    // clone correct type of node
    final CFANode newNode;
    if (node instanceof CFALabelNode cFALabelNode) {
      newNode = new CFALabelNode(cloneAst(node.getFunction()), cFALabelNode.getLabel());

    } else if (node instanceof CFATerminationNode) {
      newNode = new CFATerminationNode(cloneAst(node.getFunction()));

    } else if (node instanceof FunctionExitNode) {
      newNode = new FunctionExitNode(cloneAst(node.getFunction()));

    } else if (node instanceof CFunctionEntryNode n) {
      @Nullable FunctionExitNode newExitNode = n.getExitNode().map(this::cloneNode).orElse(null);

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
      if (newExitNode != null) {
        newExitNode.setEntryNode(entryNode); // this must not change hashvalue!
      }
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

    nodeCache.put(node, newNode);

    return (T) newNode;
  }

  @Override
  protected @Nullable AAstNode lookupCache(final AAstNode ast) {
    return astCache.get(ast);
  }

  @Override
  protected void storeInCache(final AAstNode ast, final AAstNode newAst) {
    astCache.put(ast, newAst);
  }

  /** every type is cloned, because the functionname can be part of it. */
  @Override
  @SuppressWarnings("unchecked")
  protected @Nullable <T extends Type> T cloneType(final @Nullable T type) {

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
    if (type instanceof CType cType) {
      return cType.accept(typeCloner);
    }

    throw new AssertionError("unhandled Type " + type + " of " + type.getClass());
  }

  @Override
  protected String changeFunctionName(final String name) {
    return changeName(name);
  }

  @Override
  protected String changeQualifiedName(final CSimpleDeclaration decl) {
    return changeQualifiedName(decl.getQualifiedName());
  }

  @Override
  protected String changeTypeQualifiedName(final CSimpleDeclaration decl) {
    return changeQualifiedName(decl.getQualifiedName());
  }

  private class CTypeCloner extends DefaultCTypeVisitor<CType, NoException> {

    @Override
    public CType visitDefault(CType t) {
      return t;
    }

    @Override
    public CType visit(CArrayType type) {
      return new CArrayType(type.getQualifiers(), type.getType().accept(this), type.getLength());
    }

    @Override
    public CType visit(CCompositeType type) {
      // possible problem: compositeType contains itself again -> recursion
      // solution: cache the empty compositeType and fill it later.
      CCompositeType comp =
          new CCompositeType(
              type.getQualifiers(), type.getKind(), type.getName(), type.getOrigName());
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
          type.getQualifiers(),
          type.getKind(),
          type.getName(),
          type.getOrigName(),
          cloneType(type.getRealType()));
    }

    @Override
    public CType visit(CEnumType type) {
      List<CEnumerator> l = new ArrayList<>(type.getEnumerators().size());
      for (CEnumerator e : type.getEnumerators()) {
        l.add(
            new CEnumerator(
                e.getFileLocation(),
                e.getName(),
                changeQualifiedName(e.getQualifiedName()),
                e.getValue()));
      }
      CEnumType enumType =
          new CEnumType(
              type.getQualifiers(),
              type.getCompatibleType(),
              l,
              type.getName(),
              type.getOrigName());
      l.forEach(e -> e.setEnum(enumType));
      return enumType;
    }

    @Override
    public CType visit(CFunctionType type) {
      final CFunctionType funcType;
      if (type instanceof CFunctionTypeWithNames cFunctionTypeWithNames) {
        List<CParameterDeclaration> l = new ArrayList<>(type.getParameters().size());
        for (CParameterDeclaration param : cFunctionTypeWithNames.getParameterDeclarations()) {
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
      return new CPointerType(type.getQualifiers(), type.getType().accept(this));
    }

    @Override
    public CType visit(CTypedefType type) {
      return new CTypedefType(
          type.getQualifiers(), type.getName(), type.getRealType().accept(this));
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
