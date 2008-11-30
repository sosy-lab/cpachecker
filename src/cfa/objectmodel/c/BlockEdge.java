package cfa.objectmodel.c;

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
        // TODO db: This does not make sense. 
        //      This edge type should be called BlockEdge or so.
        // TODO Auto-generated method stub
        return CFAEdgeType.BlankEdge;
    }

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
