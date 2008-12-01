package cpa.symbpredabs;

import java.util.List;
import java.util.Vector;

import cfa.objectmodel.AbstractCFAEdge;
import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFAEdgeType;


public class BlockEdge extends AbstractCFAEdge {
    
    private List<CFAEdge> edges;

    public BlockEdge() {
        super("BLOCK");
        edges = new Vector<CFAEdge>();
    }
    
    public void addEdge(CFAEdge e) {
        edges.add(e);
    }
    
    public List<CFAEdge> getEdges() { return edges; }

    @Override
    public CFAEdgeType getEdgeType() {
        // TODO Auto-generated method stub
        return CFAEdgeType.BlankEdge;
    }

    @Override
    public String getRawStatement() {
        StringBuffer buf = new StringBuffer();
        buf.append("BLOCK{");
        for (CFAEdge e : edges) {
            buf.append(e.getRawStatement());
            buf.append(" ");
        }
        buf.delete(buf.length()-1, buf.length());
        buf.append("}");
        return buf.toString();
    }
}
