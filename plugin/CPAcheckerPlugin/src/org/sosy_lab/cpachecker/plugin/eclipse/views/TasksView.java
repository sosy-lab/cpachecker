package org.sosy_lab.cpachecker.plugin.eclipse.views;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeNode;
import org.eclipse.jface.viewers.TreeNodeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.ViewPart;

public class TasksView extends ViewPart {
	Label label;

	public TasksView() {
	}

	@Override
	public void createPartControl(Composite parent) {
		label = new Label(parent, SWT.WRAP);
		label.setText("Hello World");

		final TreeViewer myTreeViewer = new TreeViewer(parent, SWT.SINGLE);

		myTreeViewer.setLabelProvider(new MyTreeLabelProvider());

		myTreeViewer.setContentProvider(new TreeNodeContentProvider());
		
		CPATaskNode t1 = new CPATaskNode("Task1");
		t1.addCPA("LocationCPA");
		t1.addCPA("EcplicitAnalysis");
		
		CPATaskNode t2 = new CPATaskNode("Task2");
		t2.addCPA("LocationCPA");
		t2.addCPA("ObserverAnalysis");
		
		myTreeViewer.setInput(new TreeNode[] {t1,t2} );
		

	}

	@Override
	public void setFocus() {
		// set focus to my widget. For a label, this doesn't
		// make much sense, but for more complex sets of widgets
		// you would decide which one gets the focus.
	}
	
	private TreeNode[] createModel() {
        final int firstlevel = 5;
        final int secondlevel = 8;
        TreeNode[] root = new TreeNode[firstlevel];
        String people;
        for (int i = 0; i < firstlevel; ++i) {
            people = new String("Parent " + i);
            TreeNode parent = new TreeNode(people);
            parent.setParent(null);
            TreeNode[] children = new TreeNode[secondlevel];
            for (int j = 0; j < secondlevel; ++j) {
                people = new String("Kid " + j);
                TreeNode child = new TreeNode(people);
                child.setParent(parent);
                child.setChildren(null);
                children[j] = child;
            }
            parent.setChildren(children);
            root[i] = parent;
        }
        return root;
    }
	private static class CPATaskNode extends TreeNode {
		TreeNode previousTarget = new TreeNode("noPreviousTarget");
		CPAsNode myCPAs = new CPAsNode();
		public CPATaskNode(Object value) {
			super(value);
			this.setChildren(new TreeNode[] {myCPAs, previousTarget});
		}
		void addCPA(String name) {
			this.myCPAs.addCPA(name);
		}
	}
	private static class CPAsNode extends TreeNode {
		List<TreeNode> cpaList = new LinkedList<TreeNode>();
		public CPAsNode() {
			super("CPAs");
		}
		public void addCPA (String name) {
			cpaList.add(new TreeNode(name));
		}
		
		@Override
		public TreeNode[] getChildren() {
			return this.cpaList.toArray(new TreeNode[0]);
		}
		@Override
		public boolean hasChildren() {
			return !this.cpaList.isEmpty();
		}
	}

	/**
	 * Provides label := element.name
	 * @author rhein
	 *
	 */
	public static class MyTreeLabelProvider extends LabelProvider {

		@Override
		public Image getImage(Object element) {
			return null;
		}

		@Override
		public String getText(Object element) {
			//return element.toString();
			return ((TreeNode)element).getValue().toString();
		}
	}
}
