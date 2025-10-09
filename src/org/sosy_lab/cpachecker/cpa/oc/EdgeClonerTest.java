// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.oc;

import static com.google.common.truth.Truth.assertThat;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;
import java.util.stream.Collectors;
import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CParser;
import org.sosy_lab.cpachecker.cfa.ParseResult;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.AbstractTransformingCAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.util.Pair;
import java.util.HashSet;
import java.util.Set;

/**
 * End-to-end tests that parse C snippets into CFA edges, clone edges and assert memory events
 * and declaration renaming behavior.
 */
public class EdgeClonerTest {

  private CParser createParser() {
    return CParser.Factory.getParser(
        LogManager.createTestLogManager(),
        CParser.Factory.getDefaultOptions(),
        MachineModel.LINUX32,
        ShutdownNotifier.createDummy());
  }

  private ParseResult parseProgram(String code) throws Exception {
    CParser parser = createParser();
    ParseResult res = parser.parseString(Path.of("test.c"), code);
    return res;
  }

  private List<CFAEdge> collectStatementEdgesOnMain(ParseResult res) {
    List<CFAEdge> edges = new ArrayList<>();
    NavigableSet<CFANode> nodes = res.cfaNodes().get("main");
    for (CFANode node : nodes) {
      for (CFAEdge e : node.getLeavingEdges()) {
        edges.add(e);
      }
    }
    return edges;
  }

  @Test
  public void declarationInitializationGeneratesMemoryEvent() throws Exception {
    String code = "int g = 5; void main() { }";
    ParseResult res = parseProgram(code);
    List<Pair<ADeclaration, String>> globals = res.globalDeclarations();
    boolean found = false;
    for (Pair<ADeclaration, String> pair : globals) {
      ADeclaration decl = pair.getFirst();
      if (decl instanceof CVariableDeclaration vd && vd.getName().equals("g")) {
        CDeclarationEdge de =
            new CDeclarationEdge(
                vd.toASTString(), FileLocation.DUMMY, CFANode.newDummyCFANode(), CFANode.newDummyCFANode(), vd);
        CFAEdge cloned = EdgeCloner.clone(de, 1, null);
        List<MemoryEvent> events = EdgeCloner.getAccesses(cloned);
        assertThat(events).isNotEmpty();
        found = true;
        break;
      }
    }
    assertThat(found).isTrue();
  }

  @Test
  public void localReadDoesNotGenerateMemoryEvent() throws Exception {
    String code = "void main() { int x; x; }";
    ParseResult res = parseProgram(code);
    List<CFAEdge> edges = collectStatementEdgesOnMain(res);
    CFAEdge exprEdge = null;
    for (CFAEdge e : edges) {
      if (e instanceof CStatementEdge se) {
        CStatement st = se.getStatement();
        if (st instanceof CExpressionStatement es) {
          if (es.getExpression() instanceof CIdExpression id && id.getName().equals("x")) {
            exprEdge = e;
            break;
          }
        }
      }
    }
    assertThat(exprEdge).isNotNull();
    CFAEdge cloned = EdgeCloner.clone(exprEdge, 1, null);
    List<MemoryEvent> events = EdgeCloner.getAccesses(cloned);
    assertThat(events).isEmpty();
  }

  @Test
  public void globalReadGeneratesMemoryEvent() throws Exception {
    String code = "int g; void main() { g; }";
    ParseResult res = parseProgram(code);
    List<CFAEdge> edges = collectStatementEdgesOnMain(res);
    CFAEdge exprEdge = null;
    for (CFAEdge e : edges) {
      if (e instanceof CStatementEdge se) {
        CStatement st = se.getStatement();
        if (st instanceof CExpressionStatement es) {
          if (es.getExpression() instanceof CIdExpression id && id.getName().equals("g")) {
            exprEdge = e;
            break;
          }
        }
      }
    }
    assertThat(exprEdge).isNotNull();
    CFAEdge cloned = EdgeCloner.clone(exprEdge, 1, null);
    List<MemoryEvent> events = EdgeCloner.getAccesses(cloned);
    assertThat(events).hasSize(1);
  }

  @Test
  public void assignToGlobalGeneratesWriteEvent() throws Exception {
    String code = "int g; void main() { g = 2; }";
    ParseResult res = parseProgram(code);
    List<CFAEdge> edges = collectStatementEdgesOnMain(res);
    CFAEdge assignEdge = null;
    for (CFAEdge e : edges) {
      if (e instanceof CStatementEdge se) {
        CStatement st = se.getStatement();
        if (st instanceof CExpressionAssignmentStatement a) {
          assignEdge = e;
          break;
        }
      }
    }
    assertThat(assignEdge).isNotNull();
    CFAEdge cloned = EdgeCloner.clone(assignEdge, 1, null);
    List<MemoryEvent> events = EdgeCloner.getAccesses(cloned);
    assertThat(events).hasSize(1);
    assertThat(events.get(0).eventType()).isEqualTo(EventType.WRITE);
  }

  @Test
  public void callWithGlobalArgGeneratesReadEvent() throws Exception {
    String code = "int g; void foo(int); void main() { foo(g); }";
    ParseResult res = parseProgram(code);
    List<CFAEdge> edges = collectStatementEdgesOnMain(res);
    CFAEdge callEdge = null;
    for (CFAEdge e : edges) {
      if (e instanceof CStatementEdge se) {
        if (se.getStatement() instanceof CFunctionCallStatement) {
          callEdge = e;
          break;
        }
      }
    }
    assertThat(callEdge).isNotNull();
    CFAEdge cloned = EdgeCloner.clone(callEdge, 1, null);
    List<MemoryEvent> events = EdgeCloner.getAccesses(cloned);
    assertThat(events).hasSize(1);
    assertThat(events.get(0).eventType()).isEqualTo(EventType.READ);
  }

  @Test
  public void localIdentifiersAreRenamedConsistently() throws Exception {
    List<String> snippets = getTestSnippets();
    for (String stmt : snippets) {
      String code = "void foo(int); void main() { int x; " + stmt + " }";
      assertRenamingBehavior(code, true);
    }
  }

  @Test
  public void globalIdentifiersAreRenamedToDistinctNamesPerInstance() throws Exception {
    List<String> snippets = getTestSnippets();
    for (String stmt : snippets) {
      String code = "int x; void foo(int); void main() { " + stmt + " }";
      assertRenamingBehavior(code, false);
    }
  }

  private List<String> getTestSnippets() {
    return List.of(
        "x = x;",
        "x;",
        "foo(x);",
        "x + x;",
//        "if(x == x){};", // why does this disappear?
        "if (x) { x = x; }",
        "if (x) x = x;",
        "for (int i=0;i<1;i++) x = x;",
        "while (x) { x = x; break; }",
        "do { x = x; } while(0);",
        "switch(x) { case 0: x = x; break; default: ; }"
    );
  }

  private void assertRenamingBehavior(String code, boolean isLocal) throws Exception {
    ParseResult res = parseProgram(code);
    List<CFAEdge> edges = collectStatementEdgesOnMain(res);
    CFAEdge target = findFirstStatementEdgeWithX(edges);
    assertThat(target).isNotNull();

    CFAEdge cloned = EdgeCloner.clone(target, 1, null);
    assertThat(cloned).isNotNull();
    assertThat(cloned instanceof CStatementEdge).isTrue();

    Set<Pair<String, String>> names = getNameAndQualifiedNameOfX(((CStatementEdge) cloned).getStatement());
    Set<String> originalNames = names.stream().map(Pair::getFirst).collect(Collectors.toSet());
    Set<String> qualifiedNames = names.stream().map(Pair::getSecond).collect(Collectors.toSet());

    assertThat(originalNames.size()).isEqualTo(1); // only x
    if (isLocal) {
      assertThat(qualifiedNames.size()).isEqualTo(1); // only x_whatever
      assertThat(originalNames.stream().findFirst().get())
          .isNotEqualTo(qualifiedNames.stream().findFirst().get());
    } else {
      assertThat(qualifiedNames.size()).isEqualTo(names.size()); // every instance is different
      assertThat(originalNames.stream().findFirst().get())
          .isNotIn(qualifiedNames); // all different from original
    }
  }

  private CFAEdge findFirstStatementEdgeWithX(List<CFAEdge> edges) {
    for (CFAEdge e : edges) {
      if (e instanceof CStatementEdge se && se.getStatement().toASTString().contains("x")) {
        return e;
      }
    }
    return null;
  }

  private Set<Pair<String, String>> getNameAndQualifiedNameOfX(CStatement statement) {
    final Set<Pair<String, String>> names = new HashSet<>();
    try {
      statement.accept(
          new AbstractTransformingCAstNodeVisitor<Exception>() {
            @Override
            public CAstNode visit(CIdExpression pCIdExpression) throws Exception {
              if (pCIdExpression.getName().equals("x")) {
                names.add(
                    Pair.of(
                        pCIdExpression.getDeclaration().getName(),
                        pCIdExpression.getDeclaration().getQualifiedName()));
              }
              return super.visit(pCIdExpression);
            }
          });
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return names;
  }
}
