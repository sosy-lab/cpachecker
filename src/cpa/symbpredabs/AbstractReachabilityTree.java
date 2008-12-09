package cpa.symbpredabs;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Stack;
import java.util.Vector;

import cfa.objectmodel.CFANode;
import cpa.common.interfaces.AbstractElementWithLocation;

public class AbstractReachabilityTree {
    private Map<AbstractElementWithLocation, 
                Collection<AbstractElementWithLocation>> tree;
    private AbstractElementWithLocation root;

    public AbstractReachabilityTree() {
        tree = new HashMap<AbstractElementWithLocation, 
                           Collection<AbstractElementWithLocation>>();
        root = null;
    }

    public void addChild(AbstractElementWithLocation parent, 
                         AbstractElementWithLocation child) {
        if (root == null) {
            root = parent;
        }
        if (!tree.containsKey(parent)) {
            tree.put(parent, new Vector<AbstractElementWithLocation>());
        }
        Collection<AbstractElementWithLocation> c = tree.get(parent);
        c.add(child);
    }

    public Collection<AbstractElementWithLocation> getSubtree(
            AbstractElementWithLocation root,
            boolean remove, boolean includeRoot) {
        Vector<AbstractElementWithLocation> ret = 
            new Vector<AbstractElementWithLocation>();

        Stack<AbstractElementWithLocation> toProcess = 
            new Stack<AbstractElementWithLocation>();
        toProcess.push(root);

        while (!toProcess.empty()) {
            AbstractElementWithLocation cur = toProcess.pop();
            ret.add(cur);
            if (tree.containsKey(cur)) {
                toProcess.addAll(remove ? tree.remove(cur) : tree.get(cur));
            }
        }
        if (!includeRoot) {
            AbstractElementWithLocation tmp = ret.lastElement();
            assert(ret.firstElement() == root);
            ret.setElementAt(tmp, 0);
            ret.remove(ret.size()-1);
        }
        return ret;
    }

    public AbstractElementWithLocation findHighest(CFANode loc) {
        if (root == null) return null;

        Queue<AbstractElementWithLocation> toProcess =
            new ArrayDeque<AbstractElementWithLocation>();
        toProcess.add(root);

        while (!toProcess.isEmpty()) {
            AbstractElementWithLocation e = toProcess.remove();
            if (e.getLocationNode().equals(loc)) {
                return e;
            }
            if (tree.containsKey(e)) {
                toProcess.addAll(tree.get(e));
            }
        }
        System.out.println("ERROR, NOT FOUND: " + loc);
        //assert(false);
        //return null;
        return root;
    }
    
    public AbstractElementWithLocation getRoot() { return root; }
    
    public boolean contains(AbstractElementWithLocation n) {
        return tree.containsKey(n);
    }
    
}
