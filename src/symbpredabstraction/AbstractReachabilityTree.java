package symbpredabstraction;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Stack;
import java.util.Vector;

import cfa.objectmodel.CFANode;

import cpa.common.interfaces.AbstractElement;
import cpa.symbpredabsCPA.SymbPredAbsAbstractElement;

public class AbstractReachabilityTree {

  private Map<SymbPredAbsAbstractElement, 
  Collection<SymbPredAbsAbstractElement>> tree;
  private SymbPredAbsAbstractElement root;

  public AbstractReachabilityTree() {
    tree = new HashMap<SymbPredAbsAbstractElement, 
    Collection<SymbPredAbsAbstractElement>>();
    root = null;
  }

  public void addChild(SymbPredAbsAbstractElement parent, 
                       SymbPredAbsAbstractElement child) {
    if (root == null) {
      root = parent;
    }
    if (!tree.containsKey(parent)) {
      tree.put(parent, new Vector<SymbPredAbsAbstractElement>());
    }
    Collection<SymbPredAbsAbstractElement> c = tree.get(parent);
    c.add(child);
  }

  public Collection<SymbPredAbsAbstractElement> getSubtree(
      AbstractElement root,
      boolean remove, boolean includeRoot) {
    
    Vector<SymbPredAbsAbstractElement> ret = 
      new Vector<SymbPredAbsAbstractElement>();

    Stack<SymbPredAbsAbstractElement> toProcess = 
      new Stack<SymbPredAbsAbstractElement>();
    toProcess.push((SymbPredAbsAbstractElement)root);

    while (!toProcess.empty()) {
      SymbPredAbsAbstractElement cur = toProcess.pop();
      ret.add(cur);
      if (tree.containsKey(cur)) {
        toProcess.addAll(remove ? tree.remove(cur) : tree.get(cur));
      }
    }
    if (!includeRoot) {
      SymbPredAbsAbstractElement tmp = ret.lastElement();
      assert(ret.firstElement() == root);
      ret.setElementAt(tmp, 0);
      ret.remove(ret.size()-1);
    }
    return ret;
  }

  public SymbPredAbsAbstractElement findHighest(CFANode loc) {
    if (root == null) return null;

    Queue<SymbPredAbsAbstractElement> toProcess =
      new ArrayDeque<SymbPredAbsAbstractElement>();
    toProcess.add(root);

    while (!toProcess.isEmpty()) {
      SymbPredAbsAbstractElement e = toProcess.remove();
      if (e.getAbstractionLocation().equals(loc)) {
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

  public SymbPredAbsAbstractElement getRoot() { return root; }

  public boolean contains(SymbPredAbsAbstractElement n) {
    return tree.containsKey(n);
  }

}
