package org.sosy_lab.cpachecker.plugin.eclipse.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.part.ViewPart;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.plugin.eclipse.TaskRunner.Task;

public class TasksView2 extends ViewPart {
	List<Task> tasks = new ArrayList<Task>();
	Label progress;
	TopNode topNode  = new TopNode();
	TreeViewer myTreeViewer;
	public static void main(String[] args) {
		Display display = new Display();
		Shell shell = new Shell(display);
		TasksView2 taskView = new TasksView2();
		taskView.createPartControl(shell);
				
		shell.setBounds(120, 120, 220, 220);

		Map<String, String> emptyMap = Collections.emptyMap();
		Task t1 = new Task("Task 1", new Configuration(emptyMap), "File1");
		taskView.addTask(t1);
		
		Task t2 = new Task("Task 2", new Configuration(emptyMap), "File2");
		taskView.addTask(t2);
		
		shell.pack();
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
	}
	
	public TasksView2() {
	}

	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new RowLayout(SWT.VERTICAL));
		progress = new Label(parent, SWT.WRAP);
		progress.setText("Hello World");

		myTreeViewer = new TreeViewer(parent, SWT.SINGLE);
		myTreeViewer.setLabelProvider(new MyTreeLabelProvider());
		myTreeViewer.setContentProvider(new MyTreeContentProvider());
		
		myTreeViewer.setInput(new Node[]{topNode});
	}

	public void addTask(Task t) {
		this.tasks.add(t);
		this.topNode.reconstruct(tasks);
		this.myTreeViewer.refresh();
	}
	
	@Override
	public void setFocus() {
		// set focus to my widget. For a label, this doesn't
		// make much sense, but for more complex sets of widgets
		// you would decide which one gets the focus.
	}
	
	public class MyTreeContentProvider extends ArrayContentProvider implements ITreeContentProvider {

	   public Object[] getChildren(Object parent) {
	      Node ex1 = (Node) parent;
	      return ex1.getChildren();
	   }

	   public Object getParent(Object element) {
	      Node ex1 = (Node) element;
	      return ex1.getParent();
	   }

	   public boolean hasChildren(Object element) {
	      Node ex1 = (Node) element;
	      return ex1.getChildren().length > 0;
	   }
	}


	public class MyTreeLabelProvider extends LabelProvider {
	   public Image getImage(Object element) {
	      return null;
	   }
	   public String getText(Object element) {
	      Node ex1 = (Node) element;
	      return ex1.getName();
	   }
	}


	static interface Node {
		enum NodeType {TOP, TASK, CONFIG, SOURCE_FILE}
		NodeType getType();
		String getName();
		Node[] getChildren();
		Node getParent();
	}
	static class TopNode implements Node {
		private Node[] children = new Node[0];
		public NodeType getType() { return NodeType.TOP; }
		public String getName() { return "CPAcheckerTasks"; }
		public Node getParent() {return null; }
		void reconstruct(List<Task> tasks) {
			children = new Node[tasks.size()];
			for (int i = 0; i < children.length; i++) {
				children[i] = new TaskNode(tasks.get(i), this);
			}
		}
		public Node[] getChildren() { return children; }
	}
	static class TaskNode implements Node {
		private String name;
		private Node parent;
		private Node[] children = new Node[2];
		public TaskNode(Task t, Node parent) {
			this.parent = parent;
			this.name = t.getName();
			children[0] = new ConfigNode(t.getConfigName(), this);
			children[1] = new SourceFileNode(t.getSourceFileName(), this);
		}
		public NodeType getType() { return NodeType.TASK; }
		public String getName() { return name; }
		public Node getParent() {return parent; }
		public Node[] getChildren() { return children; }
	}
	static class ConfigNode implements Node {
		private String name;
		private Node parent;
		public ConfigNode(String configName, Node parent) {
			this.name = configName;
			this.parent = parent;
		}
		public NodeType getType() { return NodeType.CONFIG; }
		public String getName() { return name; }
		public Node getParent() {return parent; }
		public Node[] getChildren() { return new Node[0]; }
	}
	static class SourceFileNode implements Node {
		private String name;
		private Node parent;
		public SourceFileNode(String sourceFileName, Node parent) {
			this.name = sourceFileName;
			this.parent = parent;
		}
		public NodeType getType() { return NodeType.SOURCE_FILE; }
		public String getName() { return name; }
		public Node getParent() {return parent; }
		public Node[] getChildren() { return new Node[0]; }
	}
}

