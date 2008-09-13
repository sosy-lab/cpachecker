package symbpredabstraction;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import cpaplugin.cfa.DOTBuilderInterface;
import cpaplugin.cfa.objectmodel.CFAEdge;
import cpaplugin.cfa.objectmodel.CFAErrorNode;
import cpaplugin.cfa.objectmodel.CFAFunctionDefinitionNode;
import cpaplugin.cfa.objectmodel.CFANode;
import cpaplugin.cfa.objectmodel.c.AssumeEdge;


/**
 * Specialized DOT generator for dumping summarized CFAs
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class SymbPredAbsDOTBuilder implements DOTBuilderInterface {
    private StringBuffer nodeBuf;
    private StringBuffer out;
    private Stack<CFAEdge> outerEdges;
    
    public SymbPredAbsDOTBuilder() {
        nodeBuf = new StringBuffer();
        out = new StringBuffer();
        outerEdges = new Stack<CFAEdge>();
    }

    public void generateDOT(CFAFunctionDefinitionNode mainFunction, 
                            String fileName) throws IOException {
        PrintWriter pw = new PrintWriter(fileName);
        
        pw.println("digraph CFA {");
        
        generateFunctionDOT((SymbPredAbsCFAFunctionDefinitionNode)mainFunction);
        
        pw.println(nodeBuf.toString());
        pw.println(out.toString());
        
        pw.println("}");
        pw.close();
    }
    
    private String escape(String s) {
        return s.replaceAll("\n", " ").replaceAll("\\\"", "\\\\\\\"");
    }
    
    private void generateFunctionDOT(SymbPredAbsCFAFunctionDefinitionNode cfa) {
        Set<SymbPredAbsCFANode> seen = new HashSet<SymbPredAbsCFANode>();
        Stack<SymbPredAbsCFANode> toProcess = new Stack<SymbPredAbsCFANode>();
        
//        out.append("subgraph cluster_" + cfa.getFunctionName() + " {\n");
//        out.append("label = \"" + escape(cfa.getFunctionName()) + "\";\n");
        
        toProcess.push(cfa);
        while (!toProcess.empty()) {
        	SymbPredAbsCFANode sn = toProcess.pop();
            if (!seen.contains(sn)) {
                seen.add(sn);
                generateNodeDOT(sn);
            }
            CFANode n = (CFANode)sn;
            for (int i = 0; i < n.getNumLeavingEdges(); ++i) {
                CFAEdge e = n.getLeavingEdge(i);
//                out.append(sn.getInnerNode().getNodeNumber());
//                out.append(" -> ");
                SymbPredAbsCFANode succ = (SymbPredAbsCFANode)e.getSuccessor();
//                out.append(succ.getInnerNode().getNodeNumber());
//                out.append(" [label=\"" + escape(e.getRawStatement()) + "\"");
//                out.append(",style=\"dashed\",arrowhead=\"empty\"];\n");
                if (!seen.contains(succ)) {
                    toProcess.push(succ);
                }
            }
        }
        
        // now we can generate the outer edges
        while (!outerEdges.empty()) {
            CFAEdge e = outerEdges.pop();
            CFANode n = e.getPredecessor();
            CFANode s = e.getSuccessor();
            out.append(n.getNodeNumber());
            out.append(" -> ");
            out.append(s.getNodeNumber());
            out.append(" [label=\"" + escape(e.getRawStatement()) + "\"");
            out.append(",style=\"dashed\",arrowhead=\"empty\"];\n");
        }
        
//        out.append("}\n");
    }
    
    private void generateNodeDOT(SymbPredAbsCFANode summary) {
        Set<CFANode> seen = new HashSet<CFANode>();
        Stack<CFANode> toProcess = new Stack<CFANode>();

        out.append("subgraph cluster_summary_node_");
        out.append(summary.getInnerNode().getNodeNumber());
        out.append(" {\n");
        out.append("label = \"S" + summary.getInnerNode().getNodeNumber() +
                   "\";\n");
               
        toProcess.push(summary.getInnerNode());
        while (!toProcess.empty()) {
            CFANode n = toProcess.pop();
            if (!seen.contains(n)) {
                seen.add(n);
                generateNodeShape(n);
            }

            for (int i = 0; i < n.getNumLeavingEdges(); ++i) {
                CFAEdge e = n.getLeavingEdge(i);
                CFANode succ = e.getSuccessor();
                assert(succ instanceof SymbPredAbsInnerCFANode);
                SymbPredAbsCFANode s = ((SymbPredAbsInnerCFANode)n).getSummaryNode();
                if (s == ((SymbPredAbsInnerCFANode)succ).getSummaryNode() && 
                    (s.getInnerNode() != succ)) {
                    out.append(n.getNodeNumber());
                    out.append(" -> ");
                    out.append(succ.getNodeNumber());
                    out.append(" [label=\"" + 
                            escape(e.getRawStatement()) + "\"];");
                    out.append("\n");
                    if (!seen.contains(succ)) {
                        toProcess.push(succ);
                    }
                } else {
                    outerEdges.push(e);
                }
            }
        }
        out.append("\n");        
        out.append("}\n");
    }
    
    private void generateNodeShape(CFANode n) {
        String shape = "circle";
        if (n instanceof CFAErrorNode) {
            shape = "tripleoctagon";
        } else if (n.isLoopStart()) {
            shape = "doublecircle";
        } else {
            // check if this node is an ITE
            boolean isIte = false;
            for (int i = 0; i < n.getNumLeavingEdges(); ++i) {
                if (n.getLeavingEdge(i) instanceof AssumeEdge) {
                    isIte = true;
                    break;
                }
            }
            if (isIte) {
                shape = "diamond";
            }
        }
        nodeBuf.append("node [shape=" + shape + "]; ");
        nodeBuf.append(n.getNodeNumber());
        nodeBuf.append(";\n");
    }

    public void generateDOT(Collection<CFAFunctionDefinitionNode> cfasMapList,
            CFAFunctionDefinitionNode cfa, String fileName) throws IOException {
        generateDOT(cfa, fileName);
    }
}
