/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package programtesting.simple;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFAEdgeType;
import cfa.objectmodel.CFAFunctionDefinitionNode;
import cfa.objectmodel.c.AssumeEdge;
import cfa.objectmodel.c.FunctionCallEdge;
import cfa.objectmodel.c.FunctionDefinitionNode;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import programtesting.simple.AcyclicPathProgramExtractor.AcyclicPathProgram;
import programtesting.simple.QDPTCompositeCPA.CFAEdgeEdge;
import programtesting.simple.QDPTCompositeCPA.QDPTCompositeElement;

/**
 *
 * @author holzera
 */
public class AcyclicPathProgramTranslator {

  public static List<CFAEdgeEdge> extractFeasiblePath(String pCBMCOutput, AcyclicPathProgram pAcyclicPathProgram) {
    assert(pCBMCOutput != null);
    assert(pAcyclicPathProgram != null);

    // we assume a counter example output

    BufferedReader lCBMCOutputReader = new BufferedReader(new StringReader(pCBMCOutput));

    List<CFAEdgeEdge> lFeasiblePath = new LinkedList<CFAEdgeEdge>();

    for (Graph<QDPTCompositeElement, CFAEdgeEdge>.Edge lInnerEdge : pAcyclicPathProgram.getInitialBasicBlock().getEdges()) {
      lFeasiblePath.add(lInnerEdge.getAnnotation());
    }

    try {
      String lLine;

      while ((lLine = lCBMCOutputReader.readLine()) != null) {
        // example of a line: "  __QDPT_taken_edge=6 (00000000000000000000000000000110)"

        String lPrefix = "  __QDPT_taken_edge=";

        if (lLine.matches(lPrefix + "[0-9]+ .*")) {
          String lSuffix = lLine.substring(lPrefix.length());

          String lBasicBlockIndexString = lSuffix.substring(0, lSuffix.indexOf(" "));

          Integer lIndex = Integer.decode(lBasicBlockIndexString);

          Graph<BasicBlock, Graph<QDPTCompositeElement, CFAEdgeEdge>.Edge>.Edge lEdge = pAcyclicPathProgram.getIdToEdgesBetweenBasicBlocksMap().get(lIndex);

          lFeasiblePath.add(lEdge.getAnnotation().getAnnotation());
          
          for (Graph<QDPTCompositeElement, CFAEdgeEdge>.Edge lInnerEdge : lEdge.getTarget().getEdges()) {
            lFeasiblePath.add(lInnerEdge.getAnnotation());
          }
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
      assert (false);
    }

    return lFeasiblePath;
  }

  /*
   * Translates a given acyclic path program into a corresponding C program.
   */
  public static String translate(AcyclicPathProgram pAcyclicPathProgram) {
    assert(pAcyclicPathProgram != null);

    Set<CFAEdge> lGlobalDeclarations = new LinkedHashSet<CFAEdge>();

    StringWriter lGlobalProgramText = new StringWriter();

    PrintWriter lGlobalProgramTextPrintWriter = new PrintWriter(lGlobalProgramText);

    lGlobalProgramTextPrintWriter.println("int __QDPT_nondet();");
    lGlobalProgramTextPrintWriter.println("int __QDPT_next_basic_block = 0;");
    lGlobalProgramTextPrintWriter.println("int __QDPT_taken_edge = -1;");

    StringWriter lFunctionProgramText = new StringWriter();

    PrintWriter lFunctionProgramTextPrintWriter = new PrintWriter(lFunctionProgramText);



    for (Entry<FunctionDefinitionNode, TreeSet<BasicBlock>> lEntry : pAcyclicPathProgram.getFunctionSeparatedBasicBlocks().entrySet()) {
      // for every function create a translation of the basic block

      Set<CFAEdge> lFunctionLocalDeclarations = new LinkedHashSet<CFAEdge>();

      FunctionDefinitionNode lFunctionDefinitionNode = lEntry.getKey();
      TreeSet<BasicBlock> lFunctionBasicBlocks = lEntry.getValue();

      // A) create function signature

      lFunctionProgramTextPrintWriter.println(getFunctionSignature(lFunctionDefinitionNode));


      // B) create function body

      lFunctionProgramTextPrintWriter.println("{");

      // B.1) create declarations

      for (BasicBlock lBasicBlock : lFunctionBasicBlocks) {
        // declarations inside of lBasicBlock
        for (Graph<QDPTCompositeElement, CFAEdgeEdge>.Edge lEdge : lBasicBlock.getEdges()) {
          CFAEdge lCFAEdge = lEdge.getAnnotation().getCFAEdge();

          switch (lCFAEdge.getEdgeType()) {
            case DeclarationEdge:

              //DeclarationEdge lDeclarationEdge = (DeclarationEdge) lCFAEdge;

              //if (lDeclarationEdge instanceof cfa.objectmodel.c.GlobalDeclarationEdge) {
              if (lCFAEdge instanceof cfa.objectmodel.c.GlobalDeclarationEdge) {
                // here, an assignment could take place
                // TODO handle case of assignment ... this yields some technical problems (removing const everywhere)
                lGlobalDeclarations.add(lCFAEdge);
              //lGlobalProgramTextPrintWriter.println(lDeclarationEdge.getRawStatement());
              } else {
                // here, because of CIL preprocessing, no assignment can take place
                // note: const gets removed by CIL
                //lFunctionProgramTextPrintWriter.println("  " + lDeclarationEdge.getRawStatement());
                lFunctionLocalDeclarations.add(lCFAEdge);
              }

              break;
            case MultiDeclarationEdge:



              //lFunctionProgramTextPrintWriter.println("  " + lCFAEdge.getRawStatement());

              lFunctionLocalDeclarations.add(lCFAEdge);

              break;
            default:
              // no declaration ... do nothing
              break;
          }
        }

        // declarations leaving lBasicBlock
        for (Graph<BasicBlock, Graph<QDPTCompositeElement, CFAEdgeEdge>.Edge>.Edge lOutgoingEdge : pAcyclicPathProgram.getBasicBlockGraph().getOutgoingEdges(lBasicBlock)) {
          CFAEdge lCFAEdge = lOutgoingEdge.getAnnotation().getAnnotation().getCFAEdge();

          switch (lCFAEdge.getEdgeType()) {
            case DeclarationEdge:

              //DeclarationEdge lDeclarationEdge = (DeclarationEdge) lCFAEdge;

              //if (lDeclarationEdge instanceof cfa.objectmodel.c.GlobalDeclarationEdge) {
              if (lCFAEdge instanceof cfa.objectmodel.c.GlobalDeclarationEdge) {
                // here, an assignment could take place
                // TODO handle case of assignment ... this yields some technical problems (removing const everywhere)
                //lGlobalProgramTextPrintWriter.println(lDeclarationEdge.getRawStatement());
                lGlobalDeclarations.add(lCFAEdge);
              } else {
                // here, because of CIL preprocessing, no assignment can take place
                // note: const gets removed by CIL
                //lFunctionProgramTextPrintWriter.println("  " + lDeclarationEdge.getRawStatement());
                lFunctionLocalDeclarations.add(lCFAEdge);
              }

              break;
            case MultiDeclarationEdge:

              //lFunctionProgramTextPrintWriter.println("  " + lCFAEdge.getRawStatement());
              lFunctionLocalDeclarations.add(lCFAEdge);

              break;
            default:
              // no declaration ... do nothing
              break;
          }
        }
      }


      if (lFunctionLocalDeclarations.size() > 0) {
        // print function local declarations

        lFunctionProgramTextPrintWriter.println("  // declarations");

        for (CFAEdge lCFAEdge : lFunctionLocalDeclarations) {
          lFunctionProgramTextPrintWriter.println("  " + lCFAEdge.getRawStatement());
        }

        lFunctionProgramTextPrintWriter.println();
      }


      // B.2) create initial jump table

      int lNumberOfFunctionDefinitionNodes = 0;

      for (BasicBlock lBasicBlock : lFunctionBasicBlocks) {
        if (lBasicBlock.getFirstElement().getLocationNode() instanceof FunctionDefinitionNode) {
          lNumberOfFunctionDefinitionNodes++;
        }
      }

      assert (lNumberOfFunctionDefinitionNodes >= 1);

      // If lFunctionBasicBlocks.size() == 1 then we only have one entry
      // point and since the blocks are sorted topologically it will be the
      // first occuring in the function, so, no goto is necessary.
      if (lNumberOfFunctionDefinitionNodes > 1) {
        lFunctionProgramTextPrintWriter.println("  // initial jump table - begin");

        lFunctionProgramTextPrintWriter.println("  switch (__QDPT_next_basic_block)");
        lFunctionProgramTextPrintWriter.println("  {");

        for (BasicBlock lBasicBlock : lFunctionBasicBlocks) {
          // Only FunctionDefinitionNodes can occur at the beginning of a function.
          if (lBasicBlock.getFirstElement().getLocationNode() instanceof FunctionDefinitionNode) {
            lFunctionProgramTextPrintWriter.println("    case " + lBasicBlock.getId() + ":");
            lFunctionProgramTextPrintWriter.println("      goto __QDPT_basic_block_" + lBasicBlock.getId() + ";");
          }
        }

        lFunctionProgramTextPrintWriter.println("  }");

        lFunctionProgramTextPrintWriter.println("  // initial jump table - end");
        lFunctionProgramTextPrintWriter.println();
      }


      // B.3) translate basic blocks

      for (BasicBlock lBasicBlock : lFunctionBasicBlocks) {
        String lBasicBlockTranslation = translate(lBasicBlock, pAcyclicPathProgram.getBasicBlockGraph(), lFunctionBasicBlocks, pAcyclicPathProgram.getEdgesBetweenBasicBlocksToIdMap());

        lFunctionProgramTextPrintWriter.println(lBasicBlockTranslation);
      }

      lFunctionProgramTextPrintWriter.println("}");

      lFunctionProgramTextPrintWriter.println();
    }

    String mFunctionDeclarations = "";

    for (CFAFunctionDefinitionNode node : pAcyclicPathProgram.getCFAMap().cfaMapIterator()) {
      FunctionDefinitionNode lFunctionDefinitionNode = (FunctionDefinitionNode) node;

      mFunctionDeclarations += getFunctionDeclaration(lFunctionDefinitionNode);

      mFunctionDeclarations += "\n";
    }

    for (CFAEdge lCFAEdge : lGlobalDeclarations) {
      lGlobalProgramTextPrintWriter.println(lCFAEdge.getRawStatement());
    }

    String lProgram = lGlobalProgramText + "\n" + mFunctionDeclarations + "\n" + lFunctionProgramText + "\n";

    return lProgram;
  }

  private static String getFunctionSignature(FunctionDefinitionNode pFunctionDefinitionNode) {
    assert(pFunctionDefinitionNode != null);

    IASTFunctionDefinition lFunctionDefinition = pFunctionDefinitionNode.getFunctionDefinition();

    String lFunctionSignature = lFunctionDefinition.getDeclSpecifier().getRawSignature() + " " + lFunctionDefinition.getDeclarator().getRawSignature();

    return lFunctionSignature;
  }

  private static String getFunctionDeclaration(FunctionDefinitionNode pFunctionDefinitionNode) {
    return getFunctionSignature(pFunctionDefinitionNode) + ";";
  }

  private static String translate(AssumeEdge pEdge) {
    assert(pEdge != null);

    String lExpressionString = pEdge.getExpression().getRawSignature();

    String lAssumptionString;

    if (pEdge.getTruthAssumption()) {
      lAssumptionString = lExpressionString;
    } else {
      lAssumptionString = "!(" + lExpressionString + ")";
    }

    return "__CPROVER_assume(" + lAssumptionString + ");";
  }

  private static String translate(BasicBlock pBasicBlock, Graph<BasicBlock, Graph<QDPTCompositeElement, CFAEdgeEdge>.Edge> pBasicBlocks, TreeSet<BasicBlock> pFunctionBlocks, Map<Graph<BasicBlock, Graph<QDPTCompositeElement, CFAEdgeEdge>.Edge>.Edge, Integer> pEdgesBetweenBasicBlocksToIdMap) {
    assert(pBasicBlock != null);
    assert(pBasicBlocks != null);

    StringWriter lLocalProgramTextStringWriter = new StringWriter();
    PrintWriter lLocalProgramTextPrintWriter = new PrintWriter(lLocalProgramTextStringWriter);

    lLocalProgramTextPrintWriter.println("__QDPT_basic_block_" + pBasicBlock.getId() + ":");

    if (pBasicBlock.getEdges().size() == 0) {
      lLocalProgramTextPrintWriter.println("  ;");
    }

    for (Graph<QDPTCompositeElement, CFAEdgeEdge>.Edge lEdge : pBasicBlock.getEdges()) {
      CFAEdgeEdge lAnnotation = lEdge.getAnnotation();

      CFAEdge lCFAEdge = lAnnotation.getCFAEdge();

      switch (lCFAEdge.getEdgeType()) {
        case BlankEdge: {
          // nothing to do

          break;
        }
        case AssumeEdge: {
          lLocalProgramTextPrintWriter.println(translate((AssumeEdge) lCFAEdge));

          break;
        }
        case StatementEdge: {
          // returns are not allowed inside a basic block
          assert(!lCFAEdge.isJumpEdge());

          lLocalProgramTextPrintWriter.println("  " + lCFAEdge.getRawStatement());

          break;
        }
        case MultiStatementEdge: {
          // TODO:
          // currently, we do not support multi statement edges
          assert(false);

          break;
        }
        case MultiDeclarationEdge: {
          // we do not output declarations since they are handled separately

          break;
        }
        case DeclarationEdge: {
          // we do not output declarations since they are handled separately

          break;
        }
        case FunctionCallEdge:
        case ReturnEdge:
        case CallToReturnEdge:
        default: {
          assert (false);
        }
      }
    }

    // translate transition relation between current basic block and successor blocks

    Set<Graph<BasicBlock, Graph<QDPTCompositeElement, CFAEdgeEdge>.Edge>.Edge> lBasicBlockTransitions = pBasicBlocks.getOutgoingEdges(pBasicBlock);

    if (lBasicBlockTransitions.size() == 0) {
      // last basic block
      lLocalProgramTextPrintWriter.println("  __CPROVER_assert(0, \"path feasible\");");
    }


    int lLastIndex = lBasicBlockTransitions.size() - 1;

    int lIndex = 0;

    for (Graph<BasicBlock, Graph<QDPTCompositeElement, CFAEdgeEdge>.Edge>.Edge lEdge : lBasicBlockTransitions) {
      lLocalProgramTextPrintWriter.print("  ");

      if (lIndex > 0) {
        lLocalProgramTextPrintWriter.print("else ");
      }

      if (lIndex < lLastIndex) {
        // not last edge
        lLocalProgramTextPrintWriter.print("if (__QDPT_nondet()) ");
      }

      lLocalProgramTextPrintWriter.println();

      if (lLastIndex > 0) {
        lLocalProgramTextPrintWriter.println("  {");
        lLocalProgramTextPrintWriter.print("  ");
      }

      // encode next basic block

      lLocalProgramTextPrintWriter.println("  __QDPT_next_basic_block = " + lEdge.getTarget().getId() + ";");

      // encode taken edge

      lLocalProgramTextPrintWriter.println("  __QDPT_taken_edge = " + pEdgesBetweenBasicBlocksToIdMap.get(lEdge) + ";");

      // translate edge

      CFAEdge lCFAEdge = lEdge.getAnnotation().getAnnotation().getCFAEdge();

      boolean lPrintGoto = true;

      switch (lCFAEdge.getEdgeType()) {
        case BlankEdge: {
          // nothing to do

          break;
        }
        case AssumeEdge: {
          lLocalProgramTextPrintWriter.println("  " + translate((AssumeEdge) lCFAEdge));

          break;
        }
        case StatementEdge: {
          if (lCFAEdge.isJumpEdge()) {
            lPrintGoto = false;

            BasicBlock lTargetBlock = lEdge.getTarget();

            assert(lTargetBlock.isEmpty());

            Set<Graph<BasicBlock, Graph<QDPTCompositeElement, CFAEdgeEdge>.Edge>.Edge> lTargetTransitions = pBasicBlocks.getOutgoingEdges(lTargetBlock);

            if (lTargetTransitions.size() == 1) {
              Graph<BasicBlock, Graph<QDPTCompositeElement, CFAEdgeEdge>.Edge>.Edge lTargetTransition = lTargetTransitions.iterator().next();

              lLocalProgramTextPrintWriter.println("  __QDPT_next_basic_block = " + lTargetTransition.getTarget().getId() + ";");

              lLocalProgramTextPrintWriter.println("  __QDPT_taken_edge = " + pEdgesBetweenBasicBlocksToIdMap.get(lTargetTransition) + ";");
            }
            else if (lTargetTransitions.size() > 1) {
              int lTargetIndex = 0;

              lLocalProgramTextPrintWriter.println("  switch (__QDPT_nondet())");
              lLocalProgramTextPrintWriter.println("  {");

              for (Graph<BasicBlock, Graph<QDPTCompositeElement, CFAEdgeEdge>.Edge>.Edge lTargetTransition : lTargetTransitions) {
                assert(lTargetTransition.getAnnotation().getAnnotation().getCFAEdge().getEdgeType() == CFAEdgeType.ReturnEdge);

                if (lTargetIndex == 0) {
                  lLocalProgramTextPrintWriter.println("    default:");
                }
                else {
                  lLocalProgramTextPrintWriter.println("    case " + lTargetIndex + ":");
                }

                lLocalProgramTextPrintWriter.println("      __QDPT_next_basic_block = " + lTargetTransition.getTarget().getId() + ";");

                lLocalProgramTextPrintWriter.println("      __QDPT_taken_edge = " + pEdgesBetweenBasicBlocksToIdMap.get(lTargetTransition) + ";");

                lLocalProgramTextPrintWriter.println("      break;");

                lTargetIndex++;
              }

              lLocalProgramTextPrintWriter.println("  }");
            }
            else {
              // last basic block
              lLocalProgramTextPrintWriter.println("  __CPROVER_assert(0, \"path feasible\");");
            }
          }

          lLocalProgramTextPrintWriter.println("  " + lCFAEdge.getRawStatement());

          break;
        }
        case MultiStatementEdge: {
          // TODO:
          // currently, we do not support multi statement edges
          assert(false);

          break;
        }
        case MultiDeclarationEdge: {
          // we do not output declarations since they are handled separately

          break;
        }
        case DeclarationEdge: {
          // we do not output declarations since they are handled separately

          break;
        }
        case FunctionCallEdge: {
          FunctionCallEdge lFunctionCallEdge = (FunctionCallEdge) lCFAEdge;

          lLocalProgramTextPrintWriter.println("  " + lFunctionCallEdge.getPredecessor().getLeavingSummaryEdge().getRawStatement() + ";");

          lLocalProgramTextPrintWriter.println("  // local jump table - begin");

          SortedSet<BasicBlock> lTailSet = pFunctionBlocks.tailSet(pBasicBlock, false);

          if (lTailSet.size() == 1) {
            lLocalProgramTextPrintWriter.println("  goto __QDPT_basic_block_" + lTailSet.first().getId() + ";");
          }
          else {
            boolean lFirst = true;

            for (BasicBlock lBasicBlock : lTailSet) {

              if (!lBasicBlock.getFirstElement().getLocationNode().equals(lCFAEdge.getPredecessor().getLeavingSummaryEdge().getSuccessor())) {
                continue;
              }

              lLocalProgramTextPrintWriter.print("  ");

              if (lFirst) {
                lFirst = false;
              }
              else {
                lLocalProgramTextPrintWriter.print("else ");
              }

              lLocalProgramTextPrintWriter.println("if (__QDPT_next_basic_block == " + lBasicBlock.getId() + ")");
              lLocalProgramTextPrintWriter.println("  {");
              lLocalProgramTextPrintWriter.println("    goto __QDPT_basic_block_" + lBasicBlock.getId() + ";");
              lLocalProgramTextPrintWriter.println("  }");
            }

            lLocalProgramTextPrintWriter.println();
          }

          lLocalProgramTextPrintWriter.println("  // local jump table - end");
          lLocalProgramTextPrintWriter.println();

          lPrintGoto = false;

          break;
        }
        case ReturnEdge: {
          lPrintGoto = false;

          break;
        }
        case CallToReturnEdge: {

          break;
        }
        default: {
          // TODO implement
          assert (false);
        }
      }

      if (lPrintGoto) {
        if (lLastIndex > 0) {
          lLocalProgramTextPrintWriter.print("  ");
        }

        lLocalProgramTextPrintWriter.println("  goto __QDPT_basic_block_" + lEdge.getTarget().getId() + ";");
      }

      if (lLastIndex > 0) {
        lLocalProgramTextPrintWriter.println("  }");
      }

      lIndex++;
    }

    return lLocalProgramTextStringWriter.toString();
  }
}
