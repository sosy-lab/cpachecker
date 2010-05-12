package org.sosy_lab.cpachecker.plugin.eclipse.views;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.ui.actions.OpenAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.part.ViewPart;
import org.sosy_lab.cpachecker.core.CPAcheckerResult;
import org.sosy_lab.cpachecker.plugin.eclipse.CPAcheckerPlugin;
import org.sosy_lab.cpachecker.plugin.eclipse.ITestListener;
import org.sosy_lab.cpachecker.plugin.eclipse.TaskRunner.Task;
import org.sosy_lab.cpachecker.plugin.eclipse.popup.actions.RunMultipleTasksAction;
import org.sosy_lab.cpachecker.plugin.eclipse.views.TaskTreeViewer.ConfigNode;
import org.sosy_lab.cpachecker.plugin.eclipse.views.TaskTreeViewer.Node;
import org.sosy_lab.cpachecker.plugin.eclipse.views.TaskTreeViewer.TaskNode;
import org.sosy_lab.cpachecker.plugin.eclipse.views.TaskTreeViewer.TopNode;
import org.sosy_lab.cpachecker.plugin.eclipse.views.TaskTreeViewer.Node.NodeType;

public class TasksView extends ViewPart {
	private ITestListener listener = null;
	private Label progress;
	
	private TaskTreeViewer myTreeViewer;
	private Control parent;
	
	
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
	}

	@Override
	public void createPartControl(Composite parent) {
		this.parent = parent;
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

		myTreeViewer = new TaskTreeViewer(parent, SWT.V_SCROLL | SWT.SINGLE);
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.verticalAlignment = SWT.FILL;
		gridData.grabExcessVerticalSpace = true;
		myTreeViewer.getTree().setLayoutData(gridData);
		this.getSite().setSelectionProvider(myTreeViewer);
		
		listener = new ITestListener() {
			@Override
			public void tasksChanged() {
				TasksView.this.refresh();
			}
			// TODO: find a more fine-granular update method
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
			@Override
			public void taskFinished(Task id, CPAcheckerResult results) {
				myTreeViewer.refresh();
				
			}
			@Override
			public void taskHasPreRunError(Task t, String errorMessage) {
				myTreeViewer.refresh();
				
			}
		};
		CPAcheckerPlugin plugin = CPAcheckerPlugin.getPlugin();
		if (plugin != null) { // to avoid errors when testing without plugin (with the main function)
			plugin.addTestListener(listener);
			this.refresh();
		}
		this.initContextMenu();
	}
	@Override
	public void dispose() {
		if (this.listener != null) {
			CPAcheckerPlugin.getPlugin().removeTestListener(listener);
		}
		myTreeViewer.disposeImages();
		super.dispose();
	}
	
	public void refresh() {
		myTreeViewer.refresh();
		this.progress.setText(CPAcheckerPlugin.getPlugin().getTasks().size() + "Tasks listed");
	}
	
	@Override
	public void setFocus() {
		// set focus to my widget. For a label, this doesn't
		// make much sense, but for more complex sets of widgets
		// you would decide which one gets the focus.
	}
	private void initContextMenu() {
		MenuManager menuMgr= new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				handleMenuAboutToShow(manager);
			}
		});
		this.getSite().registerContextMenu(menuMgr, myTreeViewer);
		Menu menu= menuMgr.createContextMenu(parent);
		myTreeViewer.getTree().setMenu(menu);
	}
	
	@SuppressWarnings("unchecked")
	void handleMenuAboutToShow(IMenuManager manager) {
		final IStructuredSelection selection= (IStructuredSelection) myTreeViewer.getSelection();
		if (selection.isEmpty()) {
			return;
		}
		List<Task> selectedTasks = new ArrayList<Task>();
		List lst = selection.toList();
		for (Object element : lst) {
			boolean exitLoop = false;
			if (element instanceof Node) {
				NodeType type = ((Node)element).getType();
				switch (type) {
				case TOP :
					selectedTasks.clear();
					// all tasks will be added. Clear to avoid duplicates.
					Node[] taskNodes = ((TopNode)element).getChildren();
					// this nodes are TaskNodes
					for (int i = 0; i < taskNodes.length; i++) {
						assert (taskNodes[i].getType() == NodeType.TASK);
						selectedTasks.add(((TaskNode)taskNodes[i]).getTask());
					}
					exitLoop = true;
					break;
				case CONFIG:
					selectedTasks.add(((ConfigNode)element).getParent().getTask());
					break;
				case TASK :
					selectedTasks.add(((TaskNode)element).getTask());
			        break;
				}
			}
			if (exitLoop) {
				break;
			}
		}
		
		manager.add(new RunMultipleTasksAction(this.getSite().getShell(), selectedTasks));
		//this.getSite().setSelectionProvider(myTreeViewer);
		OpenAction open = new OpenAction(this.getSite());
		open.selectionChanged(selection);
		manager.add(open);
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS + "-end")); //$NON-NLS-1$
	}

}

