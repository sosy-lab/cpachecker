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

import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
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
import java.util.Optional;
import java.util.Set;


public class SourceLocationMapper {

  public static interface LocationDescriptor {

    boolean matches(FileLocation pFileLocation);

  }

  private static abstract class FileNameDescriptor implements LocationDescriptor {

    private final Optional<String> originFileName;

    private final boolean matchBaseName;

    private FileNameDescriptor(String originFileName) {
      this(originFileName, true);
    }

    private FileNameDescriptor(String originFileName, boolean matchBaseName) {
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

    private final int originLineNumber;

    public OriginLineDescriptor(Optional<String> pOriginFileName, int pOriginLineNumber) {
      this(pOriginFileName, pOriginLineNumber, true);
    }

    private OriginLineDescriptor(
        Optional<String> pOriginFileName, int pOriginLineNumber, boolean pMatchBaseName) {
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

    private final int offset;

    public OffsetDescriptor(Optional<String> pOriginFileName, int pOffset) {
      this(pOriginFileName, pOffset, true);
    }

    private OffsetDescriptor(
        Optional<String> pOriginFileName, int pOffset, boolean pMatchBaseName) {
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

  private static Set<FileLocation> collectFileLocationsFrom(CAstNode astNode) {
    final FileLocationCollectingVisitor visitor = new FileLocationCollectingVisitor();
    return astNode.accept(visitor);
  }

  private static Set<CAstNode> getAstNodesFromCfaEdge(CFAEdge pEdge) {
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

  public static Set<FileLocation> getFileLocationsFromCfaEdge(CFAEdge pEdge) {
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
}
