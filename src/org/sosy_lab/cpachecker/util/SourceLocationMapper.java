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
package org.sosy_lab.cpachecker.util;

import java.util.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpressionCollectingVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.FileLocationCollectingVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;

import java.util.Collections;
import java.util.Deque;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;


public class SourceLocationMapper {

  public static interface LocationDescriptor {

    boolean matches(FileLocation pFileLocation);

  }

  public static abstract class FileNameDescriptor implements LocationDescriptor {

    private final Optional<String> originFileName;

    private final boolean matchBaseName;

    public FileNameDescriptor(String originFileName) {
      this(originFileName, true);
    }

    public FileNameDescriptor(String originFileName, boolean matchBaseName) {
      this.originFileName = Optional.of(originFileName);
      this.matchBaseName = matchBaseName;
    }

    private FileNameDescriptor(Optional<String> originFileName, boolean matchBaseName) {
      Preconditions.checkNotNull(originFileName);
      this.originFileName = originFileName;
      this.matchBaseName = matchBaseName;
    }

    @Override
    public boolean matches(FileLocation pFileLocation) {
      if (!originFileName.isPresent()) {
        return true;
      }
      String originFileName = this.originFileName.get();
      String fileLocationFileName = pFileLocation.getFileName();
      if (matchBaseName) {
        originFileName = getBaseName(originFileName);
        fileLocationFileName = getBaseName(fileLocationFileName);
      }
      return originFileName.equals(fileLocationFileName);
    }

    private String getBaseName(String pOf) {
      int index = pOf.lastIndexOf('/');
      if (index == -1) {
        index = pOf.lastIndexOf('\\');
      }
      if (index == -1) {
        return pOf;
      } else {
        return pOf.substring(index + 1);
      }
    }

    @Override
    public int hashCode() {
      return originFileName.hashCode();
    }

    @Override
    public String toString() {
      return originFileName.isPresent() ? "FILE " + originFileName : "TRUE";
    }

    protected Optional<String> getOriginFileName() {
      return originFileName;
    }

  }

  public static class OriginLineDescriptor extends FileNameDescriptor implements LocationDescriptor {

    public final int originLineNumber;

    public OriginLineDescriptor(Optional<String> pOriginFileName, int pOriginLineNumber) {
      this(pOriginFileName, pOriginLineNumber, true);
    }

    public OriginLineDescriptor(Optional<String> pOriginFileName, int pOriginLineNumber, boolean pMatchBaseName) {
      super(pOriginFileName, pMatchBaseName);
      this.originLineNumber = pOriginLineNumber;
    }

    @Override
    public int hashCode() {
      return Objects.hash(getOriginFileName(), originLineNumber);
    }

    @Override
    public boolean equals(Object pObj) {
      if (this == pObj) {
        return true;
      }
      if (!(pObj instanceof OriginLineDescriptor)) {
        return false;
      }
      OriginLineDescriptor other = (OriginLineDescriptor) pObj;
      return Objects.equals(getOriginFileName(), other.getOriginFileName())
          && originLineNumber == other.originLineNumber;
    }

    @Override
    public boolean matches(FileLocation pFileLocation) {
      return super.matches(pFileLocation)
          && pFileLocation.getStartingLineInOrigin() == originLineNumber;
    }

    @Override
    public String toString() {
      return "ORIGIN STARTING LINE " + originLineNumber;
    }
  }

  public static class OffsetDescriptor extends FileNameDescriptor implements LocationDescriptor {

    public final int offset;

    public OffsetDescriptor(Optional<String> pOriginFileName, int pOffset) {
      this(pOriginFileName, pOffset, true);
    }

    public OffsetDescriptor(Optional<String> pOriginFileName, int pOffset, boolean pMatchBaseName) {
      super(pOriginFileName, pMatchBaseName);
      this.offset = pOffset;
    }

    @Override
    public int hashCode() {
      return Objects.hash(getOriginFileName(), offset);
    }

    @Override
    public boolean equals(Object pObj) {
      if (this == pObj) {
        return true;
      }
      if (!(pObj instanceof OffsetDescriptor)) {
        return false;
      }
      OffsetDescriptor other = (OffsetDescriptor) pObj;
      return Objects.equals(getOriginFileName(), other.getOriginFileName())
          && offset == other.offset;
    }

    @Override
    public boolean matches(FileLocation pFileLocation) {
      return super.matches(pFileLocation)
          && pFileLocation.getNodeOffset() <= offset
          && pFileLocation.getNodeOffset() + pFileLocation.getNodeLength() > offset;
    }

    @Override
    public String toString() {
      return "OFFSET " + offset;
    }
  }

  public static Set<String> matchTokenNumbersToTokenStrings() {
    return Collections.emptySet();
  }

  private static void collectLine(final SortedSet<Integer> target, final FileLocation loc, boolean overApproximateTokens) {
    if (loc != null && !loc.equals(FileLocation.DUMMY)) {
      if (overApproximateTokens) {
        int lowerBound = loc.getStartingLineNumber();
        int upperBound = loc.getEndingLineNumber();
        if (target.size() > 0) {
          lowerBound = Math.min(lowerBound, target.first());
          upperBound = Math.max(upperBound, target.last());
        }
        for (int line=lowerBound; line<=upperBound; line++) {
          target.add(line);
        }
      } else {
        target.add(loc.getStartingLineNumber());
      }
    }
  }

  public static Set<Integer> collectTokensFrom(CAstNode astNode, boolean overApproximateTokens) {
    final TreeSet<Integer> result = Sets.newTreeSet();
    Set<FileLocation> locs = collectFileLocationsFrom(astNode);

    for (FileLocation l: locs) {
      collectLine(result, l, overApproximateTokens);
    }

    return result;
  }

  public static class RowAndColumn implements Comparable<RowAndColumn>{
    public final int row;
    public final int column;

    public RowAndColumn(int row, int col) {
      this.row = row;
      this.column = col;
    }

    @Override
    public String toString() {
      return row + ":" + column;
    }

    @Override
    public int compareTo(RowAndColumn pO) {
      // Caution: inverted order here
      return ComparisonChain.start().compare(pO.row, row).compare(pO.column, column).result();
    }

    @Override
    public int hashCode() {
      return 31 * (31 + column) + row;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof RowAndColumn)) {
        return false;
      }
      RowAndColumn other = (RowAndColumn) obj;
      return row == other.row
          && column == other.column;
    }
  }

  public static synchronized Set<RowAndColumn> collectRowsAndColsFrom(CAstNode astNode) {
    final TreeSet<RowAndColumn> result = Sets.newTreeSet();
    Set<FileLocation> locs = collectFileLocationsFrom(astNode);

    for (FileLocation l: locs) {
      RowAndColumn rc = new RowAndColumn(l.getStartingLineNumber(), l.getNodeOffset());
      result.add(rc);
    }

    return result;
  }

  public static synchronized Set<FileLocation> collectFileLocationsFrom(CAstNode astNode) {
    final FileLocationCollectingVisitor visitor = new FileLocationCollectingVisitor();
    return astNode.accept(visitor);
  }

  public static synchronized Set<CAstNode> getAstNodesFromCfaEdge(CFAEdge pEdge) {
    final Set<CAstNode> result = Sets.newHashSet();
    final Deque<CFAEdge> edges = Queues.newArrayDeque();

    edges.add(pEdge);

    while (!edges.isEmpty()) {
      CFAEdge edge = edges.pop();

      switch (edge.getEdgeType()) {
      case AssumeEdge:
        result.add(((CAssumeEdge) edge).getExpression());
      break;
      case CallToReturnEdge:
        CFunctionSummaryEdge fnSumEdge = (CFunctionSummaryEdge) edge;
        result.add(fnSumEdge.getExpression());
      break;
      case DeclarationEdge:
        result.add(((CDeclarationEdge) edge).getDeclaration());
      break;
      case FunctionCallEdge:
        if (edge.getPredecessor().getLeavingSummaryEdge() != null) {
          edges.add(edge.getPredecessor().getLeavingSummaryEdge());
        }
        result.addAll(((CFunctionCallEdge) edge).getArguments());
      break ;
      case FunctionReturnEdge:
      break;
      case ReturnStatementEdge:
        CReturnStatementEdge retStmt = (CReturnStatementEdge) edge;
        if (retStmt.getRawAST().isPresent()) {
          result.add(retStmt.getRawAST().get());
        }

        if (retStmt.getExpression().isPresent()) {
          result.add(retStmt.getExpression().get());
        }
      break;
      case StatementEdge:
        result.add(((CStatementEdge) edge).getStatement());
      break;
      case BlankEdge:
        // do nothing
        break;
      default:
        throw new AssertionError("Unhandled edge type in switch statement: " + edge.getEdgeType());
      }
    }

    return result;
  }

  public static synchronized Set<FileLocation> getFileLocationsFromCfaEdge(CFAEdge pEdge) {
    Set<FileLocation> result = Sets.newHashSet();

    final Set<CAstNode> astNodes = getAstNodesFromCfaEdge(pEdge);
    for (CAstNode n: astNodes) {
      result.addAll(collectFileLocationsFrom(n));
    }

    result.add(pEdge.getFileLocation());

    result = FluentIterable.from(result).filter(Predicates.not(Predicates.equalTo(FileLocation.DUMMY))).toSet();

    if (result.isEmpty() && pEdge.getPredecessor() instanceof FunctionEntryNode) {
      FunctionEntryNode functionEntryNode = (FunctionEntryNode) pEdge.getPredecessor();
      if (!functionEntryNode.getFileLocation().equals(FileLocation.DUMMY)) {
        return Collections.singleton(functionEntryNode.getFileLocation());
      }
    }
    return result;
  }

  public static synchronized Set<RowAndColumn> getRowsAndColsFromCFAEdge(CFAEdge pEdge) {
    final TreeSet<RowAndColumn> result = Sets.newTreeSet();
    final Set<CAstNode> astNodes = getAstNodesFromCfaEdge(pEdge);

    for (CAstNode n: astNodes) {
      result.addAll(collectRowsAndColsFrom(n));
    }

    RowAndColumn rc = new RowAndColumn(
        pEdge.getFileLocation().getStartingLineNumber(),
        pEdge.getFileLocation().getNodeOffset());
    result.add(rc);

    return result;
  }

  public static Set<String> getEdgeVariableNames(CFAEdge subject) {
    CIdExpressionCollectingVisitor visitor = new CIdExpressionCollectingVisitor();

    final Set<String> result = Sets.newHashSet();
    final Set<CIdExpression> idExs = Sets.newHashSet();
    final Deque<CFAEdge> edges = Queues.newArrayDeque();

    edges.add(subject);

    while (!edges.isEmpty()) {
      CFAEdge edge = edges.pop();

      switch (edge.getEdgeType()) {
      case AssumeEdge:
        CAssumeEdge assumeEdge = ((CAssumeEdge) edge);
        idExs.addAll(assumeEdge.getExpression().accept(visitor));
      break;
      case CallToReturnEdge:
        CFunctionSummaryEdge fnSumEdge = (CFunctionSummaryEdge) edge;
        idExs.addAll(fnSumEdge.getExpression().accept(visitor));
      break;
      case DeclarationEdge:
        CDeclaration decl = ((CDeclarationEdge) edge).getDeclaration();
        if (decl instanceof CVariableDeclaration) {
          CVariableDeclaration varDecl = (CVariableDeclaration) decl;
          result.add(varDecl.getQualifiedName());
          if (varDecl.getInitializer() != null) {
            CInitializer initializer = varDecl.getInitializer();

            // CInitializerLists can be ridiculously long (1000s of entries),
            // leading to stack overflow error when traversing the list via a visitor
            // so rather visit each one by one
            if(initializer instanceof CInitializerList) {
              for(CInitializer c : ((CInitializerList)initializer).getInitializers()) {
                idExs.addAll(c.accept(visitor));
              }
            }

            else {
              idExs.addAll(varDecl.getInitializer().accept(visitor));
            }
          }
        }
      break;
      case FunctionCallEdge:
        CFunctionCallEdge callEdge = (CFunctionCallEdge) edge;
        for (CExpression e: callEdge.getArguments()) {
          idExs.addAll(e.accept(visitor));
        }
      break ;
      case FunctionReturnEdge:
      break;
      case ReturnStatementEdge:
        Optional<CExpression> expr = ((CReturnStatementEdge) edge).getExpression();
        if (expr.isPresent()) {
          idExs.addAll(expr.get().accept(visitor));
        }
      break;
      case StatementEdge:
        idExs.addAll(((CStatementEdge) edge).getStatement().accept(visitor));
      break;
      case BlankEdge:
        // do nothing
        break;
      default:
        throw new AssertionError("Unhandled edge type in switch statement: " + edge.getEdgeType());
      }
    }

    for (CIdExpression e: idExs) {
      if (e.getDeclaration() != null) {
        result.add(e.getDeclaration().getQualifiedName());
      }
    }

    return result;
  }


}
