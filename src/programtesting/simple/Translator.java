/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
/**
 * 
 */
package programtesting.simple;

import java.util.List;

import cfa.CFAMap;
import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFAEdgeType;
import cfa.objectmodel.CFAFunctionDefinitionNode;
import cfa.objectmodel.CFANode;
import cfa.objectmodel.c.AssumeEdge;
import cfa.objectmodel.c.DeclarationEdge;
import cfa.objectmodel.c.FunctionCallEdge;
import cfa.objectmodel.c.FunctionDefinitionNode;

import cfa.objectmodel.c.MultiDeclarationEdge;
import cfa.objectmodel.c.MultiStatementEdge;
import cfa.objectmodel.c.ReturnEdge;
import cfa.objectmodel.c.StatementEdge;
import common.Pair;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;

import programtesting.simple.QDPTCompositeCPA.CFAEdgeEdge;
import programtesting.simple.QDPTCompositeCPA.Edge;
import programtesting.simple.QDPTCompositeCPA.QDPTCompositeElement;

/**
 * @author holzera
 *
 */
public class Translator {
  private CFAMap mCFAs;
  private String mFunctionDeclarations;
  
  public Translator(CFAMap pCFAs) {
    assert(pCFAs != null);
    
    mCFAs = pCFAs;
    
    mFunctionDeclarations = "";
    
    for (CFAFunctionDefinitionNode node : mCFAs.cfaMapIterator()) {
      FunctionDefinitionNode lFunctionDefinitionNode = (FunctionDefinitionNode)node;
      
      mFunctionDeclarations += this.getFunctionDeclaration(lFunctionDefinitionNode);
      
      mFunctionDeclarations += "\n";
    }
    
    // TODO check uniqueness
    mFunctionDeclarations += "int nondet();\n";
  }
  
  /*
   * returns the function declaration (C code) for the given node
   */
  public static String getFunctionDeclaration(FunctionDefinitionNode pFunctionDefinitionNode) {
    return getFunctionSignature(pFunctionDefinitionNode.getFunctionName(), pFunctionDefinitionNode) + ";";
  }

  public static String getFunctionSignature(FunctionDefinitionNode pFunctionDefinitionNode) {
    return getFunctionSignature(pFunctionDefinitionNode.getFunctionName(), pFunctionDefinitionNode);
  }

  public static String getFunctionSignature(String pFunctionName, FunctionDefinitionNode pFunctionDefinitionNode) {
    assert(pFunctionName != null);
    assert(pFunctionDefinitionNode != null);
    
    IASTFunctionDefinition lFunctionDefinition = pFunctionDefinitionNode.getFunctionDefinition();

    //List<IASTParameterDeclaration> lFunctionParameters = pFunctionDefinitionNode.getFunctionParameters();

    String lFunctionSignature = lFunctionDefinition.getDeclSpecifier().getRawSignature() + " " + lFunctionDefinition.getDeclarator().getRawSignature();

    /*String lFunctionSignature = lFunctionDefinition.getDeclSpecifier().getRawSignature() + " " + pFunctionName + "(";

    if (pFunctionName.equals("malloc")) {
      System.out.println(lFunctionDefinition.getDeclarator().getRawSignature());

      System.out.println(lFunctionSignature);

      assert(false);
    }

    boolean lFirstFunctionParameter = true;
    
    for (IASTParameterDeclaration lFunctionParameter : lFunctionParameters) {
      if (lFirstFunctionParameter) {
        lFirstFunctionParameter = false;
      }
      else {
        lFunctionSignature += ", ";
      }
      
      lFunctionSignature += lFunctionParameter.getRawSignature();
    }
    
    lFunctionSignature += ")";*/
    
    return lFunctionSignature;
  }
  
  public String translate(AssumeEdge pEdge) {
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
  
  public String translate(StatementEdge pEdge) {
    assert(pEdge != null);
    
    assert(false);
    
    /*IASTExpression lExpression = pEdge.getExpression();
    
    if (lExpression != null) {
      return lExpression.getRawSignature() + ";";
    }*/
    
    return pEdge.getRawStatement();
    
    //return ";";
  }
  
  public String translate(MultiStatementEdge pEdge) {
    assert(pEdge != null);
    
    assert(false);
    
    String lProgramText = "";
   
    for (IASTExpression lExpression : pEdge.getExpressions()) {
      lProgramText += lExpression.getRawSignature() + ";\n";
    }
    
    return lProgramText;
  }
  
  public String translate(MultiDeclarationEdge pEdge) {
    assert(pEdge != null);
    
    return pEdge.getRawStatement();
  }
  
  public String getFunctionCall(IASTExpression[] pArguments, String pFunctionName) {
    assert(pFunctionName != null);
    
    
    String lArgumentString = "(";

    boolean lFirstArgument = true;

    if (pArguments != null) {
      for (IASTExpression lArgument : pArguments) {
        if (lFirstArgument) {
          lFirstArgument = false;
        } else {
          lArgumentString += ", ";
        }

        lArgumentString += lArgument.getRawSignature();
      }
    }

    lArgumentString += ")";


    return pFunctionName + lArgumentString + ";";
  }
  
  private List<Edge> translate(PrintWriter pGlobalText, PrintWriter pProgramText, List<StringWriter> pFunctionDefinitions, List<Edge> pPath) {
    assert(pGlobalText != null);
    assert(pProgramText != null);
    assert(pFunctionDefinitions != null);
    assert(pPath != null);
    assert(!pPath.isEmpty());
    
    List<Edge> lPath = new LinkedList<Edge>(pPath);
    
    while (!lPath.isEmpty()) {
      Edge lEdge = lPath.remove(0);
      
      //if (lEdge.hasSubpaths()) {
      if (lEdge instanceof QDPTCompositeCPA.SubpathsEdge) {
        QDPTCompositeCPA.SubpathsEdge lSubpathsEdge = (QDPTCompositeCPA.SubpathsEdge)lEdge;
        
        if (lSubpathsEdge.getNumberOfSubpaths() > 1) {
          Iterator<List<Edge>> lSubpathIterator = lSubpathsEdge.getSubpaths().iterator();

          pProgramText.println("switch (nondet()) {");

          int lCaseIndex = 0;

          while (lSubpathIterator.hasNext()) {
            List<Edge> lSubpath = lSubpathIterator.next();

            if (lSubpathIterator.hasNext()) {
              pProgramText.println("case " + lCaseIndex + ":");

              lCaseIndex++;
            }
            else {
              pProgramText.println("default:");
            }

            pProgramText.println("{");

            lSubpath = translate(pGlobalText, pProgramText, pFunctionDefinitions, lSubpath);

            if (!lSubpath.isEmpty()) {
              System.out.println("----");

              Iterator<List<Edge>> lSubpathIterator2 = ((QDPTCompositeCPA.SubpathsEdge)lEdge).getSubpaths().iterator();

              while (lSubpathIterator2.hasNext()) {
                List<Edge> lSubpath2 = lSubpathIterator2.next();

                System.out.println(lSubpath2);
              }
            }

            assert(lSubpath.isEmpty());

            pProgramText.println("break;");

            pProgramText.println("}");
          }

          pProgramText.println("}");
        }
        else {
          assert(lSubpathsEdge.getNumberOfSubpaths() == 1);
          
          List<Edge> lSubpath = lSubpathsEdge.getSubpaths().iterator().next();
          
          lSubpath = translate(pGlobalText, pProgramText, pFunctionDefinitions, lSubpath);
          
          assert(lSubpath.isEmpty());
        }
      }
      else {
        assert(lEdge instanceof QDPTCompositeCPA.CFAEdgeEdge);
        
        CFAEdge lCFAEdge = ((QDPTCompositeCPA.CFAEdgeEdge)lEdge).getCFAEdge();
        
        switch (lCFAEdge.getEdgeType()) {
          case BlankEdge: {
            // nothing to do

            break;
          }
          case AssumeEdge: {
            pProgramText.println(translate((AssumeEdge) lCFAEdge));

            break;
          }
          case StatementEdge: {
            pProgramText.println(translate((StatementEdge) lCFAEdge));

            break;
          }
          case DeclarationEdge: {
            DeclarationEdge lDeclarationEdge = (DeclarationEdge) lCFAEdge;

            if (lDeclarationEdge instanceof cfa.objectmodel.c.GlobalDeclarationEdge) {
              pGlobalText.println(lDeclarationEdge.getRawStatement());
            } else {
              pProgramText.println(lDeclarationEdge.getRawStatement());
            }

            break;
          }
          case FunctionCallEdge: {
            FunctionCallEdge lFunctionCallEdge = (FunctionCallEdge) lCFAEdge;

            String lFunctionName = lFunctionCallEdge.getSuccessor().getFunctionName();

            pProgramText.println(getFunctionCall(lFunctionCallEdge.getArguments(), lFunctionName + "_" + pFunctionDefinitions.size()));

            PrintWriter lProgramText = startFunction((FunctionDefinitionNode)lFunctionCallEdge.getSuccessor(), pFunctionDefinitions);
            
            // TODO check again!!!
            if (lPath.size() > 0) {
              lPath = translate(pGlobalText, lProgramText, pFunctionDefinitions, lPath);
            }

            if (lPath.size() > 0) {
              Edge lLastEdge = lPath.get(0);
              
              //assert(!lLastEdge.hasSubpaths());
              
              //assert(!(lLastEdge instanceof QDPTCompositeCPA.HasSubpaths));
              
              assert(lLastEdge instanceof QDPTCompositeCPA.CFAEdgeEdge);
              
              CFAEdge lLastCFAEdge = ((QDPTCompositeCPA.CFAEdgeEdge)lLastEdge).getCFAEdge();
              
              assert(lLastCFAEdge.getEdgeType() == CFAEdgeType.ReturnEdge);
              
              lPath.remove(0);
            }
            
            lProgramText.println("}");
            
            break;
          }
          case ReturnEdge: {
            // we have to do this to correctly close the bracket of the function definition
            lPath.add(0, lEdge);
            
            return lPath;
          }
          case MultiStatementEdge: {
            pProgramText.println(translate((MultiStatementEdge) lCFAEdge));

            break;
          }
          case MultiDeclarationEdge: {
            pProgramText.println(translate((MultiDeclarationEdge) lCFAEdge));

            break;
          }
          case CallToReturnEdge: {
            // this should not have been taken
            assert (false);

            break;
          }
          default: {
            assert (false);
          }
        }
      }
    }
    
    return lPath;
  }
  
  public String translate(List<Edge> pPath) {
    assert(pPath != null);
    assert(pPath.size() > 0);
    
    // create print writer for global typedefs and declarations
    StringWriter lGlobalText = new StringWriter();
    PrintWriter lGlobalTextPrintWriter = new PrintWriter(lGlobalText);

    // stack for program texts of different functions
    List<StringWriter> lFunctionDefinitions = new LinkedList<StringWriter>();
    
    // program text for start function 
    CFANode lStartNode = pPath.get(0).getParent().getElementWithLocation().getLocationNode();
    
    if (!(lStartNode instanceof FunctionDefinitionNode)) {
      System.out.println(pPath.get(0).getParent().getDepth());
      System.out.println(lStartNode);
    }
    
    assert(lStartNode instanceof FunctionDefinitionNode);
    
    PrintWriter lProgramText = startFunction((FunctionDefinitionNode)lStartNode, lFunctionDefinitions);
    
    List<Edge> lPath = translate(lGlobalTextPrintWriter, lProgramText, lFunctionDefinitions, pPath);
    
    assert(lPath.isEmpty());
    
    lProgramText.println("__CPROVER_assert(0, \"path feasible\");");
    
    lProgramText.println("}");
    
    
    String lSource = "";
    
    lSource += lGlobalText.toString();
    
    lSource += "\n";
    
    lSource += mFunctionDeclarations;
    
    lSource += "\n";
    
    for (StringWriter lWriter : lFunctionDefinitions) {
      lSource += lWriter.toString();
      
      lSource += "\n";
    }
    
    lSource += "\n";
    
    return lSource;
  }
  
  public PrintWriter startFunction(FunctionDefinitionNode pFunctionDefinitionNode, List<StringWriter> pFunctionDefinitions) {
    assert(pFunctionDefinitionNode != null);
    assert(pFunctionDefinitions != null);
    
    
    String lFunctionSignature = getFunctionSignature(pFunctionDefinitionNode.getFunctionName() + "_" + pFunctionDefinitions.size(), pFunctionDefinitionNode);
    
    
    StringWriter lFunctionStringWriter = new StringWriter();
    
    pFunctionDefinitions.add(0, lFunctionStringWriter);
    
    PrintWriter lProgramText = new PrintWriter(lFunctionStringWriter);
    
    lProgramText.println(lFunctionSignature);
    
    lProgramText.println("{");
    
    
    return lProgramText;
  }
  
  public Pair<String, String> translate(BasicBlock pBasicBlock, Graph<BasicBlock, Graph<QDPTCompositeElement, CFAEdgeEdge>.Edge> pBasicBlocks, TreeSet<BasicBlock> pFunctionBlocks) {
    assert(pBasicBlock != null);
    assert(pBasicBlocks != null);
    
    StringWriter lGlobalProgramTextStringWriter = new StringWriter();
    //PrintWriter lGlobalProgramTextPrintWriter = new PrintWriter(lGlobalProgramTextStringWriter);
    
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
              lLocalProgramTextPrintWriter.println("  __QDPT_next_basic_block = " + lTargetTransitions.iterator().next().getTarget().getId() + ";");
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
    
    return new Pair<String, String>(lLocalProgramTextStringWriter.toString(), lGlobalProgramTextStringWriter.toString());
  }
  
  public String getInitialFunctionCall(String pFunctionName, List<IASTParameterDeclaration> pFunctionParameters) {
    assert(pFunctionName != null);
    assert(pFunctionParameters != null);
    
    StringWriter lProgramTextStringWriter = new StringWriter();
    
    PrintWriter lProgramTextPrintWriter = new PrintWriter(lProgramTextStringWriter);
    
    
    lProgramTextPrintWriter.println("void __QDPT_feasibility_check(void) {");
    
    int lIndex = 0;
    
    for (IASTParameterDeclaration lParameterDeclaration : pFunctionParameters) {
      // TODO there is more to do to handle all possible cases (?)
      lProgramTextPrintWriter.println("  " + lParameterDeclaration.getDeclSpecifier().getRawSignature() + " parameter_" + lIndex + ";");
      
      lIndex++;
    }
    
    
    lProgramTextPrintWriter.print("  " + pFunctionName + "(");
    
    for (int lSecondIndex = 0; lSecondIndex < lIndex; lSecondIndex++) {
      if (lSecondIndex > 0) {
        lProgramTextPrintWriter.print(", ");
      }
      
      lProgramTextPrintWriter.print("parameter_" + lSecondIndex);
    }
    
    lProgramTextPrintWriter.println(");");
    
    
    lProgramTextPrintWriter.println("  __CPROVER_assert(0, \"path feasible\");");
    
    lProgramTextPrintWriter.println("}");    
    
    return lProgramTextStringWriter.toString();
  }
}
