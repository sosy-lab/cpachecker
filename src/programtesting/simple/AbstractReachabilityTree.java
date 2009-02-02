/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package programtesting.simple;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author holzera
 */
public class AbstractReachabilityTree<Data> {
  public class Edge {
    private Node mParent;
    private Node mChild;
    private HashSet<List<Edge>> mSubpaths;
    
    private Edge(Node pParent, Node pChild) {
      assert(pParent != null);
      assert(pChild != null);
      
      mParent = pParent;
      mChild = pChild;
      mSubpaths = null;
    }
    
    private Edge(Collection<List<Edge>> pEdgeSet, Node pChild) {
      assert(pEdgeSet != null);
      assert(pEdgeSet.size() > 0);
      assert(pChild != null);
      
      mParent = null;
      mChild = pChild;
      
      // TODO error in child check
      // consistency check
      /*for (List<Edge> pEdgeList : pEdgeSet) {
        assert(pEdgeList != null);
        assert(pEdgeList.size() > 0);
        
        if (mParent == null) {
          mParent = pEdgeList.get(0).getParent();
          mChild = pEdgeList.get(pEdgeList.size() - 1).getChild();
        }
        
        assert(mParent.equals(pEdgeList.get(0).getParent()));
        assert(mChild.equals(pEdgeList.get(pEdgeList.size() - 1).getChild()));
        
        for (int lIndex = 0; lIndex < pEdgeList.size() - 1; lIndex++) {
          Edge lEdge1 = pEdgeList.get(lIndex);
          Edge lEdge2 = pEdgeList.get(lIndex + 1);
          
          assert(lEdge1 != null);
          assert(lEdge2 != null);
          
          assert(lEdge1.getChild().equals(lEdge2.getParent()));
        }
      }*/
      
      mSubpaths = new HashSet<List<Edge>>(pEdgeSet);
    }
    
    public Node getParent() {
      return mParent;
    }
    
    public Node getChild() {
      return mChild;
    }
    
    public boolean hasSubpaths() {
      return (mSubpaths != null);
    }
    
    public Iterator<List<Edge>> getSubpaths() {
      assert(mSubpaths != null);
      
      return mSubpaths.iterator();
    }
    
    @Override
    public boolean equals(Object pOther) {
      if (pOther == null) {
        return false;
      }
      
      try {
        Edge lOtherEdge = (Edge)pOther;
        
        if (!getParent().equals(lOtherEdge.getParent())) {
          return false;
        }
        
        if (!getChild().equals(lOtherEdge.getChild())) {
          return false;
        }
        
        if ((lOtherEdge.hasSubpaths() && !hasSubpaths()) || (hasSubpaths() && !lOtherEdge.hasSubpaths())) {
          return false;
        }
        
        Iterator<List<Edge>> lIterator = getSubpaths();
        
        while (lIterator.hasNext()) {
          List<Edge> lEdgeList = lIterator.next();
          
          Iterator<List<Edge>> lOtherIterator = lOtherEdge.getSubpaths();
          
          boolean lHasMatchingList = false;
          
          while (lOtherIterator.hasNext()) {
            List<Edge> lOtherEdgeList = lOtherIterator.next();
            
            if (lOtherEdgeList.equals(lEdgeList)) {
              lHasMatchingList = true;
            }
          }
          
          if (!lHasMatchingList) {
            return false;
          }
        }
        
        return true;
      } 
      catch (ClassCastException lException) {
        return false;
      }
    }
    
    @Override
    public int hashCode() {
      if (hasSubpaths()) {
        return mSubpaths.hashCode();
      }
      else {
        return mParent.hashCode() + mChild.hashCode();
      }
    }
    
    @Override
    public String toString() {
      if (hasSubpaths()) {
        return mParent + " -[" + mSubpaths.toString() + "]> " + mChild;
      }
      else {
        return mParent + " -> " + mChild;
      }
    }
  }
  
  public class Node {
    private Data mData;
    private Node mParent;
    private HashSet<Edge> mChildren;
    private Edge mEdge;
    
    private Node(Node pParent, Data pData) {
      mData = pData;
      mParent = pParent;
      mChildren = new HashSet<Edge>();
      
      if (mParent != null) {
        mEdge = new Edge(mParent, this);
        mParent.mChildren.add(mEdge);
      }
      else {
        mEdge = null;
      }
    }
    
    private Node(Data pData) {
      this(null, pData);
    }
    
    public Data getData() {
      return mData;
    }
    
    @Override
    public String toString() {
      return "<" + hashCode() + ", " + mData + ">";
    }
    
    public boolean isRoot() {
      return !hasParent();
    }
    
    public boolean hasParent() {
      return (mParent != null);
    }
    
    public Node getParent() {
      assert(mParent != null);
      
      return mParent;
    }
    
    public boolean hasChildren() {
      return (mChildren.size() > 0);
    }
    
    public Iterator<Edge> getChildren() {
      return mChildren.iterator();
    }
    
    // removes this node from its parent, i.e., cutting itself away from the tree
    public void remove() {
      if (hasParent()) {
        Node lParent = getParent();
        
        lParent.mChildren.remove(mEdge);
      }
    }
  }
  
  private Node mRoot;
  
  public AbstractReachabilityTree() {
    mRoot = null;
  }
  
  public void setRoot(Data pData) {
    assert(!hasRoot());
    
    setRoot(new Node(pData));
  }
  
  public void setRoot(Node pRoot) {
    assert(pRoot != null);
    assert(mRoot == null);
    
    mRoot = pRoot;
  }
  
  public boolean hasRoot() {
    return (mRoot != null);
  }
  
  public Node getRoot() {
    assert(mRoot != null);
    
    return mRoot;
  }
}
