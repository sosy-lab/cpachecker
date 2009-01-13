/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package programtesting;

import cfa.objectmodel.CFANode;
import cpa.symbpredabs.explicit.ExplicitAbstractElement;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author holzera
 */
public class FeasiblePathTree<TreeElement extends ExplicitAbstractElement> {
  public static class TreeNode {
    private CFANode mNode;
    private TreeNode mParent;
    private HashSet<TreeNode> mChildren;
    
    public TreeNode(CFANode pNode) {
      assert(pNode != null);
      
      mNode = pNode;
      
      mChildren = new HashSet<TreeNode>();
    }
    
    public TreeNode(TreeNode pParent, CFANode pNode) {
      this(pNode);
      
      assert(pParent != null);
      
      mParent = pParent;
    }
    
    public boolean hasParent() {
      return (mParent == null);
    }
    
    public TreeNode getParent() {
      return mParent;
    }
    
    public void addChild(TreeNode pNode) {
      assert(pNode != null);
      
      mChildren.add(pNode);
    }
    
    public Collection<TreeNode> getChildren() {
      return mChildren;
    }
    
    public CFANode getNode() {
      return mNode;
    }
  }
  
  public static class PathInserter {
    public static <TreeElement extends ExplicitAbstractElement> void visit(TreeNode pNode, Iterator<TreeElement> pElement) {
      assert(pNode != null);
      assert(pElement != null);
      
      if (!pElement.hasNext()) {
        return;
      }
      
      TreeElement lElement = pElement.next();
      
      TreeNode lSuccessor = null;
      
      for (TreeNode lNode : pNode.getChildren()) {
        if (lNode.getNode().equals(lElement.getLocationNode())) {
          lSuccessor = lNode;
        }
      }
      
      if (lSuccessor == null) {
        // we can not continue, so we have to create a new node
        lSuccessor = new TreeNode(pNode, lElement.getLocationNode());
        
        pNode.addChild(lSuccessor);
      }
      
      visit(lSuccessor, pElement);
    }
  }
  
  public static class PathCreator {
    HashSet<List<CFANode>> mPaths;
    
    public PathCreator() {
      mPaths = new HashSet<List<CFANode>>();
    }
    
    public <TreeElement extends ExplicitAbstractElement> void visit(TreeNode pNode) {
      Collection<TreeNode> lChildren = pNode.getChildren();
      
      if (lChildren.size() == 0) {
        LinkedList<CFANode> lPath = new LinkedList<CFANode>();
        
        lPath.addFirst(pNode.getNode());
        
        TreeNode lParent = pNode.getParent();
        
        while (lParent != null) {
          lPath.addFirst(lParent.getNode());
          
          lParent = lParent.getParent();
        }
        
        mPaths.add(lPath);
      }
      else {
        for (TreeNode lChild : lChildren) {
          visit(lChild);
        }
      }
    }
    
    public Collection<List<CFANode>> getMaximalPaths() {
      return mPaths;
    }
  }
  
  private TreeNode mRoot;
  
  public FeasiblePathTree() {
    mRoot = null;
  }
  
  public Collection<List<CFANode>> getMaximalPaths() {
    PathCreator lCreator = new PathCreator();
    
    if (mRoot == null) {
      return Collections.emptySet();
    }
    
    lCreator.visit(mRoot);
    
    return lCreator.getMaximalPaths();
  }
  
  public void addPath(Deque<TreeElement> pPath) {
    assert(pPath != null);
    assert(pPath.size() > 0);
    
    Iterator<TreeElement> lIterator = pPath.iterator();
    
    TreeElement lElement = lIterator.next();
    
    if (mRoot == null) {
      mRoot = new TreeNode(lElement.getLocationNode());
    }
    
    assert(mRoot.getNode().equals(pPath.getFirst().getLocationNode()));
    
    PathInserter.visit(mRoot, lIterator);
  }
}
