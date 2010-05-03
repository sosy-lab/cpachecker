package org.sosy_lab.cpachecker.plugin.eclipse.views;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.part.ViewPart;
import org.sosy_lab.cpachecker.plugin.eclipse.CPAcheckerPlugin;
import org.sosy_lab.cpachecker.plugin.eclipse.ITestListener;
import org.sosy_lab.cpachecker.plugin.eclipse.TaskRunner.Task;

public class TasksView extends ViewPart {
	private ITestListener listener = null;
	private Label progress;
	private TopNode topNode  = new TopNode();
	private TreeViewer myTreeViewer;
	private Image configIcon;
	private Image sourceFileImage;
	private List<Image> imagesToBeDisposed = new ArrayList<Image>();
	
	
	public static void main(String[] args) {
		Display display = new Display();
		Shell shell = new Shell(display);
		final TasksView taskView = new TasksView();
		taskView.createPartControl(shell);
				
		shell.setBounds(220, 220, 440, 440);
		

		//Map<String, String> emptyMap = Collections.emptyMap();
		//Task t1 = new Task("Task 1", new Configuration(emptyMap), "File1");
		//taskView.addTask(t1);
		
		//Task t2 = new Task("Task 2", new Configuration(emptyMap), "File2");
		//taskView.addTask(t2);
		
		shell.pack();
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
	}
	
	public TasksView() {
		Image missingImage = ImageDescriptor.getMissingImageDescriptor().createImage();
		try {
			ImageDescriptor desc = CPAcheckerPlugin.getImageDescriptor("icons/config16.gif"); 	
		if (desc != null) {
			configIcon = desc.createImage(true);
		} else {
			configIcon = missingImage;
		}
		desc = CPAcheckerPlugin.getImageDescriptor("icons/sample.gif");
		if (desc != null) {
			sourceFileImage = desc.createImage(true);
		} else {
			sourceFileImage = missingImage;
		}
		imagesToBeDisposed.add(configIcon);
		imagesToBeDisposed.add(sourceFileImage);
		imagesToBeDisposed.add(missingImage);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void createPartControl(Composite parent) {
		GridLayout gridLayout= new GridLayout();
		gridLayout.marginWidth= 0;
		gridLayout.marginHeight= 0;
		parent.setLayout(gridLayout);
		//parent.setLayout(new FillLayout(SWT.VERTICAL));
		
		progress = new Label(parent, SWT.WRAP);
		progress.setText("Hello World");
		GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		progress.setLayoutData(gridData);
		
		//progress.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));

		myTreeViewer = new TreeViewer(parent, SWT.V_SCROLL | SWT.SINGLE);
		myTreeViewer.setLabelProvider(new MyTreeLabelProvider());
		myTreeViewer.setContentProvider(new MyTreeContentProvider());
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.verticalAlignment = SWT.FILL;
		gridData.grabExcessVerticalSpace = true;
		myTreeViewer.getTree().setLayoutData(gridData);
		
		myTreeViewer.setAutoExpandLevel(TreeViewer.ALL_LEVELS);
		myTreeViewer.setInput(new Node[]{topNode});
		myTreeViewer.setAutoExpandLevel(TreeViewer.ALL_LEVELS);
		
		
		
		listener = new ITestListener() {
			@Override
			public void tasksChanged() {
				TasksView.this.refresh();
			}
			// TODO: find a more fine-granular update method
			@Override
			public void taskFailed(Task id) {
				myTreeViewer.refresh();
			}
			@Override
			public void taskStarted(Task id) {
				myTreeViewer.refresh();	
			}
			@Override
			public void tasksFinished() {
				myTreeViewer.refresh();	
			}
			@Override
			public void tasksStarted(int taskCount) {
				myTreeViewer.refresh();
			}
		};
		CPAcheckerPlugin plugin = CPAcheckerPlugin.getPlugin();
		if (plugin != null) { // to avoid errors when testing without plugin (with the main function)
			plugin.addTestListener(listener);
			this.refresh();
		}
		
	}
	@Override
	public void dispose() {
		if (this.listener != null) {
			CPAcheckerPlugin.getPlugin().removeTestListener(listener);
		}
		for (Image img : imagesToBeDisposed) {
			if (img!= null) {
				img.dispose();
			}
		}
		super.dispose();
	}
	
	public void refresh() {
		this.topNode.reconstruct(CPAcheckerPlugin.getPlugin().getTasks());
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

	public class MyTreeLabelProvider extends LabelProvider implements IStyledLabelProvider {
	@Override
	public Image getImage(Object element) {
		   Node n = (Node) element;
		   switch (n.getType()) {
		case TOP:
			return null;
		case TASK:
			return null;
		case CONFIG:
			return configIcon;
		case SOURCE_FILE:
			return sourceFileImage;
		default:
			return null;
		}
		   
	   }
	@Override
	   public String getText(Object element) {
	      Node ex1 = (Node) element;
	      return ex1.getName();
	   }
	@Override
	public StyledString getStyledText(Object element) {
		return new StyledString(this.getText(element));
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
		private Task task;
		private Node parent;
		private Node[] children = new Node[2];
		public TaskNode(Task task, Node parent) {
			this.parent = parent;
			this.task = task;
			children[0] = new ConfigNode(task.getConfigName(), this);
			children[1] = new SourceFileNode(task.getSourceFileName(), this);
		}
		public NodeType getType() { return NodeType.TASK; }
		public String getName() { return task.getName(); }
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

