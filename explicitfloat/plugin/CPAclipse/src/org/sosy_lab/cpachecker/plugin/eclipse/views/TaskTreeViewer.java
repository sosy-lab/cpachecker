package org.sosy_lab.cpachecker.plugin.eclipse.views;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.sosy_lab.cpachecker.plugin.eclipse.CPAclipse;
import org.sosy_lab.cpachecker.plugin.eclipse.Task;
import org.sosy_lab.cpachecker.plugin.eclipse.views.TaskTreeViewer.Node.NodeType;

public class TaskTreeViewer extends TreeViewer {

	private TopNode topNode  = new TopNode();
	private Image safeResultIcon;
	private Image unsafeResultIcon;
	private Image unknownResultIcon;
	private Image configIcon;
	private Image specIcon;
	private Image sourceFileImage;
	private Image mainLogoIcon;
	private List<Image> imagesToBeDisposed = new ArrayList<Image>();

	public TaskTreeViewer(Composite parent, int style) {
		super(parent, style);
		createImages();
		setLabelProvider(new MyTreeLabelProvider());
		setContentProvider(new MyTreeContentProvider());
		this.setInput(new Node[]{topNode});
		ColumnViewerToolTipSupport.enableFor(this);
		
	}

	@Override
	public void refresh() {
		this.topNode.reconstruct(CPAclipse.getPlugin().getTasks());
		super.refresh();
	}

	private void createImages() {
		Image missingImage = ImageDescriptor.getMissingImageDescriptor().createImage();
		try {
			ImageDescriptor desc = CPAclipse.getImageDescriptor("icons/config.gif"); 	
			if (desc != null) configIcon = desc.createImage(true);
			else configIcon = missingImage;
			
			desc = CPAclipse.getImageDescriptor("icons/specification.gif");
			if (desc != null) specIcon = desc.createImage(true);
			else specIcon = missingImage;
			
			desc = CPAclipse.getImageDescriptor("icons/Thumbs up.gif");
			if (desc != null) safeResultIcon = desc.createImage(true);
			else safeResultIcon = missingImage;
			
			desc = CPAclipse.getImageDescriptor("icons/Thumbs down.gif");
			if (desc != null) unsafeResultIcon = desc.createImage(true);
			else unsafeResultIcon = missingImage;
			
			desc = CPAclipse.getImageDescriptor("icons/Question.gif");
			if (desc != null) unknownResultIcon = desc.createImage(true);
			else unknownResultIcon = missingImage;
			
			desc = CPAclipse.getImageDescriptor("icons/MainLogo.gif");
			if (desc != null) mainLogoIcon = desc.createImage(true);
			else mainLogoIcon = missingImage;
			
			desc = CPAclipse.getImageDescriptor("icons/sample.gif");
			if (desc != null) sourceFileImage = desc.createImage(true);
			else sourceFileImage = missingImage;
			imagesToBeDisposed.add(configIcon);
			imagesToBeDisposed.add(safeResultIcon);
			imagesToBeDisposed.add(unsafeResultIcon);
			imagesToBeDisposed.add(unknownResultIcon);
			imagesToBeDisposed.add(mainLogoIcon);
			imagesToBeDisposed.add(sourceFileImage);
			imagesToBeDisposed.add(missingImage);
		} catch (Exception e) {
			CPAclipse.logError("could not create images", e);
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
					
					IFolder outDir = t.getOutputDirectory(false);
					IFile c = t.hasConfigurationFile() ? t.getConfigFile() : null;
					IFile s = t.hasSpecificationFile() ? t.getSpecFile() : null;
					ITranslationUnit u = ((TaskNode)ex1).getTask().getTranslationUnit();
					List<Object> result = new LinkedList<Object>();
					
					if (c != null)
						result.add(c);
					if (s != null)
						result.add(s);
					if (u != null)
						result.add(u);
					result.add(outDir);
					return result.toArray();
				} else if (ex1.getType() == NodeType.TOP) {
					return ((TopNode)ex1).getChildren();
				}
			} else if (parent instanceof IFolder) {
				try {
					return ((IFolder)parent).members();
				} catch (CoreException e) {
					CPAclipse.logError("Could not retrieve members of the ResultFolder", e);
				}
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
			} else if (element instanceof IFolder) {
				try {
					return ((IFolder)element).exists() && ((IFolder)element).members().length > 0;
				} catch (CoreException e) {
					CPAclipse.logError(e);
					return false;
				} 
			} else {
				return false;
			}
		}
	}

	public class MyTreeLabelProvider extends CellLabelProvider implements IStyledLabelProvider {
		WorkbenchLabelProvider myWorkbenchLabelProvider = new WorkbenchLabelProvider();
		@Override
		public Image getImage(Object element) {
			if (element instanceof Node) {
				Node n = (Node) element;
				switch (n.getType()) {
				case TOP:
					return mainLogoIcon;
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
			} else if (element instanceof IFile && ((IFile)element).getFileExtension().equals("properties")) {
				return configIcon;
			}  else if (element instanceof IFile && ((IFile)element).getFileExtension().equals("spc")) {
				return specIcon;
			}
			return myWorkbenchLabelProvider.getImage(element);
		}
		public String getText(Object element) {
			if (element instanceof Node) {
				Node ex1 = (Node) element;
				return ex1.getName();	
			} else if (element instanceof IFile && ((IFile)element).getFileExtension().equals("properties")) {
				//return ((IFile)element).getProjectRelativePath().toPortableString();
				return ((IFile)element).getName();
			} else {
				return myWorkbenchLabelProvider.getText(element);
			}
		}
		@Override
		public StyledString getStyledText(Object element) {
			return new StyledString(this.getText(element));
		}
		@Override
		public void update(ViewerCell cell) {
			cell.setText(this.getText(cell.getElement()));
			cell.setImage(this.getImage(cell.getElement()));
		}
		@Override
		public String getToolTipText(Object element) {
			if (element instanceof Node) {
				Node ex1 = (Node) element;
				switch (ex1.getType()) {
					case TOP: return null;
					case TASK : return ((TaskNode)ex1).getName();
					case CONFIG: return null;
				}
			} else if (element instanceof IFile) {
				//return ((IFile)element).getProjectRelativePath().toPortableString();
				return ((IFile)element).getFullPath().toPortableString();
			} else {
				return myWorkbenchLabelProvider.getText(element);
			}
			return null;
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
		public String getName() { return "CPAclipseTasks"; }
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
	public void selectTask(Task task) {
		this.setExpandedState(topNode, true);
		TreeItem[] tasks = this.getTree().getTopItem().getItems();
		for (int i = 0; i < tasks.length; i++) {
			if (tasks[i].getData() instanceof TaskNode) {
				TaskNode current = (TaskNode) tasks[i].getData();
				if (current.getTask().equals(task)) {
					this.getTree().setSelection(tasks[i]);
					return;
				}
			}
		}
	}
}
