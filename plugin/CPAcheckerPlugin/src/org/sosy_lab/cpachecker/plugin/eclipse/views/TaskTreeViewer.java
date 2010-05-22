package org.sosy_lab.cpachecker.plugin.eclipse.views;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.cdt.ui.CElementLabelProvider;
import org.eclipse.core.resources.IFile;
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
	private Image safeResultIcon;
	private Image unsafeResultIcon;
	private Image unknownResultIcon;
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
			ImageDescriptor desc = CPAcheckerPlugin.getImageDescriptor("icons/config.gif"); 	
			if (desc != null) configIcon = desc.createImage(true);
			else configIcon = missingImage;
			
			desc = CPAcheckerPlugin.getImageDescriptor("icons/Thumbs up.gif");
			if (desc != null) safeResultIcon = desc.createImage(true);
			else safeResultIcon = missingImage;
			
			desc = CPAcheckerPlugin.getImageDescriptor("icons/Thumbs down.gif");
			if (desc != null) unsafeResultIcon = desc.createImage(true);
			else unsafeResultIcon = missingImage;
			
			desc = CPAcheckerPlugin.getImageDescriptor("icons/Question.gif");
			if (desc != null) unknownResultIcon = desc.createImage(true);
			else unknownResultIcon = missingImage;
			
			desc = CPAcheckerPlugin.getImageDescriptor("icons/sample.gif");
			if (desc != null) sourceFileImage = desc.createImage(true);
			else sourceFileImage = missingImage;
			imagesToBeDisposed.add(configIcon);
			imagesToBeDisposed.add(safeResultIcon);
			imagesToBeDisposed.add(unsafeResultIcon);
			imagesToBeDisposed.add(unknownResultIcon);
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

		@Override
		public Object[] getChildren(Object parent) {
			if (parent instanceof Node) {
				Node ex1 = (Node) parent;
				if (ex1.getType() == NodeType.TASK) {
					Task t = ((TaskNode)ex1).getTask();
					
					File outDir = t.getOutputDirectory();
					IFile c = t.hasConfigurationFile() ? t.getConfigFile() : null;
					ITranslationUnit u = ((TaskNode)ex1).getTask().getTranslationUnit();
					if (c != null && u != null) {
						return new Object[] {c, u, outDir };
					} else if (c != null) {
						return new Object[] {c, outDir };
					} else {
						return new Object[] {u, outDir };
					}
				} else if (ex1.getType() == NodeType.TOP) {
					return ((TopNode)ex1).getChildren();
				}
			} else if (parent instanceof File) {
				return ((File)parent).listFiles();
			}
			return super.getElements(parent);
		}

		public Object getParent(Object element) {
			if (!(element instanceof Node)) return null;
			Node ex1 = (Node) element;
			return ex1.getParent();
		}

		public boolean hasChildren(Object element) {
			if (element instanceof Node) {
				Node ex1 = (Node) element;
				if (ex1.getType()== NodeType.TOP || ex1.getType() == NodeType.TASK) {
					return true;
				} else {
					return false; 
				}
			} else if (element instanceof File) {
				return ((File)element).isDirectory();
			} else {
				return false;
			}
		}
	}

	public class MyTreeLabelProvider extends LabelProvider implements IStyledLabelProvider {
		CElementLabelProvider cLabelProvider = new CElementLabelProvider();
		@Override
		public Image getImage(Object element) {
			if (element instanceof Node) {
				Node n = (Node) element;
				switch (n.getType()) {
				case TOP:
					return null;
				case TASK:
					Task t  = ((TaskNode)n).getTask();
					switch (t.getLastResult()) {
					case SAFE :
						return safeResultIcon;
					case UNSAFE :
						return unsafeResultIcon;
					case UNKNOWN :
					default :
						// also kind of unknown
						return unknownResultIcon;
					}
				}
			} else if (element instanceof IASTTranslationUnit) {
				return cLabelProvider.getImage(element);
			} else if (element instanceof IFile) {
				return configIcon;
			} 
			return super.getImage(element);
		}
		@Override
		public String getText(Object element) {
			if (element instanceof Node) {
				Node ex1 = (Node) element;
				return ex1.getName();	
			} else if (element instanceof ITranslationUnit) {
				return cLabelProvider.getText(element);
			} else if (element instanceof IFile && ((IFile)element).getFileExtension().equals("properties")) {
				return ((IFile)element).getProjectRelativePath().toPortableString();
			} else if (element instanceof File) {
				if (((File)element).isDirectory()) {
					return "ResultFiles: " + ((File)element).getName();
				} else {
					return ((File)element).getName();
				}
			} else {
				return super.getText(element);
			}
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
		public TaskNode(Task task, TopNode parent) {
			this.parent = parent;
			this.task = task;
		}
		
		public NodeType getType() { return NodeType.TASK; }
		public String getName() { 
			if (task.isDirty()) {
				return "* " + task.getName();
			} else {
				return task.getName(); 
			}
		}
		public TopNode getParent() {return parent; }
		
		public Task getTask() {
			return task;
		}
	}
	static class ConfigNode2 implements Node {
		private String name;
		private TaskNode parent;
		public ConfigNode2(String configName, TaskNode parent) {
			this.name = configName;
			this.parent = parent;
		}
		public NodeType getType() { return NodeType.CONFIG; }
		public String getName() { return name; }
		public TaskNode getParent() {return parent; }
		public Node[] getChildren() { return new Node[0]; }
	}
}
