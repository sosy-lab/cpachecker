package org.sosy_lab.cpachecker.plugin.eclipse.views;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.cdt.ui.CElementLabelProvider;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.sosy_lab.cpachecker.plugin.eclipse.CPAcheckerPlugin;
import org.sosy_lab.cpachecker.plugin.eclipse.TaskRunner.Task;
import org.sosy_lab.cpachecker.plugin.eclipse.views.TaskTreeViewer.Node.NodeType;

public class TaskTreeViewer extends TreeViewer {

	private TopNode topNode  = new TopNode();
	private Image configIcon;
	private Image sourceFileImage;
	private List<Image> imagesToBeDisposed = new ArrayList<Image>();

	public TaskTreeViewer(Composite parent, int style) {
		super(parent, style);
		createImages();
		setLabelProvider(new MyTreeLabelProvider());
		setContentProvider(new MyTreeContentProvider());
		this.setInput(new Node[]{topNode});
	}

	@Override
	public void refresh() {
		this.topNode.reconstruct(CPAcheckerPlugin.getPlugin().getTasks());
		super.refresh();
	}

	private void createImages() {
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
	void disposeImages() {
		for (Image img : imagesToBeDisposed) {
			if (img!= null) {
				img.dispose();
			}
		}
	}

	public class MyTreeContentProvider extends ArrayContentProvider implements ITreeContentProvider {

		public Object[] getChildren(Object parent) {
			if (parent instanceof Node) {
				Node ex1 = (Node) parent;
				if (ex1.getType() == NodeType.TASK) {
					ConfigNode c = ((TaskNode)ex1).getConfigNode();
					ITranslationUnit u = ((TaskNode)ex1).getTask().getTranslationUnit();
					if (c != null && u != null) {
						return new Object[] {c, u};
					} else if (c != null) {
						return new Object[] {c};
					} else {
						return new Object[] {u};
					}
				} else if (ex1.getType() == NodeType.TOP) {
					return ((TopNode)ex1).getChildren();
				}
			}
			return new Object[0];
		}

		public Object getParent(Object element) {
			if (!(element instanceof Node)) return null;
			Node ex1 = (Node) element;
			return ex1.getParent();
		}

		public boolean hasChildren(Object element) {
			if (!(element instanceof Node)) return false;
			Node ex1 = (Node) element;
			return ex1.getType()== NodeType.TOP || ex1.getType() == NodeType.TASK;
		}
	}

	public class MyTreeLabelProvider extends LabelProvider implements IStyledLabelProvider {
		CElementLabelProvider cLabelProvider = new CElementLabelProvider();
		@Override
		public Image getImage(Object element) {
			if (!(element instanceof Node)) return cLabelProvider.getImage(element);
			Node n = (Node) element;
			switch (n.getType()) {
			case TOP:
				return null;
			case TASK:
				return null;
			case CONFIG:
				return configIcon;
			default:
				return null;
			}
		}
		@Override
		public String getText(Object element) {
			if (!(element instanceof Node))
				if (element instanceof ITranslationUnit) {
					//return ((ITranslationUnit)element).getElementName();
					return cLabelProvider.getText(element);
				}
			Node ex1 = (Node) element;
			return ex1.getName();
		}
		@Override
		public StyledString getStyledText(Object element) {
			return new StyledString(this.getText(element));
		}
	}

	static interface Node {
		// source nodes are represented by a ITranslationUnit object
		enum NodeType {TOP, TASK, CONFIG}
		NodeType getType();
		String getName();
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
		private TopNode parent;
		private ConfigNode configNode = null;
		public TaskNode(Task task, TopNode parent) {
			this.parent = parent;
			this.task = task;
			if (task.hasConfigurationFile()) {
				configNode = new ConfigNode(task.getConfigFilePath(), this);
			}
		}
		public NodeType getType() { return NodeType.TASK; }
		public String getName() { return task.getName(); }
		public TopNode getParent() {return parent; }
		ConfigNode getConfigNode() {return this.configNode; }
		public Task getTask() {
			return task;
		}
	}
	static class ConfigNode implements Node {
		private String name;
		private TaskNode parent;
		public ConfigNode(String configName, TaskNode parent) {
			this.name = configName;
			this.parent = parent;
		}
		public NodeType getType() { return NodeType.CONFIG; }
		public String getName() { return name; }
		public TaskNode getParent() {return parent; }
		public Node[] getChildren() { return new Node[0]; }
	}
}
