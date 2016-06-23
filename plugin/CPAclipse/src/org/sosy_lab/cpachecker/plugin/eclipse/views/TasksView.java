package org.sosy_lab.cpachecker.plugin.eclipse.views;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.ui.actions.OpenAction;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.IShellProvider;
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
import org.eclipse.ui.actions.BaseSelectionListenerAction;
import org.eclipse.ui.actions.NewWizardMenu;
import org.eclipse.ui.actions.OpenSystemEditorAction;
import org.eclipse.ui.actions.OpenWithMenu;
import org.eclipse.ui.actions.RefreshAction;
import org.eclipse.ui.part.ViewPart;
import org.sosy_lab.cpachecker.core.CPAcheckerResult;
import org.sosy_lab.cpachecker.plugin.eclipse.CPAclipse;
import org.sosy_lab.cpachecker.plugin.eclipse.ITaskListener;
import org.sosy_lab.cpachecker.plugin.eclipse.Task;
import org.sosy_lab.cpachecker.plugin.eclipse.popup.actions.DeleteTasksAction;
import org.sosy_lab.cpachecker.plugin.eclipse.popup.actions.RenameTasksAction;
import org.sosy_lab.cpachecker.plugin.eclipse.popup.actions.RunMultipleTasksAction;
import org.sosy_lab.cpachecker.plugin.eclipse.popup.actions.SaveTasksAction;
import org.sosy_lab.cpachecker.plugin.eclipse.popup.actions.SetConfigFileInTaskAction;
import org.sosy_lab.cpachecker.plugin.eclipse.popup.actions.SetSourceFileInTaskAction;
import org.sosy_lab.cpachecker.plugin.eclipse.popup.actions.SetSpecFileInTaskAction;
import org.sosy_lab.cpachecker.plugin.eclipse.views.TaskTreeViewer.Node;
import org.sosy_lab.cpachecker.plugin.eclipse.views.TaskTreeViewer.TaskNode;
import org.sosy_lab.cpachecker.plugin.eclipse.views.TaskTreeViewer.TopNode;
import org.sosy_lab.cpachecker.plugin.eclipse.views.TaskTreeViewer.Node.NodeType;

public class TasksView extends ViewPart implements IShellProvider {
	private ITaskListener listener = null;
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
		listener = new ITaskListener() {
			@Override
			public void tasksChanged() {
				TasksView.this.refresh();
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
			@Override
			public void taskFinished(Task id, CPAcheckerResult results) {
				myTreeViewer.refresh();
				
			}
			@Override
			public void taskHasPreRunError(Task t, String errorMessage) {
				myTreeViewer.refresh();
				
			}
			@Override
			public void tasksChanged(List<Task> changed) {
				/*for (Task t: changed) {
					myTreeViewer.refresh(t);
				}*/
				TasksView.this.refresh();
			}
			@Override
			public void selectTask(Task toSelect) {
				TasksView.this.getSite().getPage().activate(TasksView.this);
				TasksView.this.myTreeViewer.getTree().setFocus();
				if (toSelect instanceof Task) {
					TasksView.this.myTreeViewer.selectTask((Task)toSelect);
				}
			}
		};
		CPAclipse plugin = CPAclipse.getPlugin();
		if (plugin != null) { // to avoid errors when testing without plugin (with the main function)
			plugin.addTestListener(listener);
			this.refresh();
		}
		this.initContextMenu();
	}
	
	@Override
	public void dispose() {
		if (this.listener != null) {
			CPAclipse.getPlugin().removeTestListener(listener);
		}
		myTreeViewer.disposeImages();
		super.dispose();
	}
	
	public void refresh() {
		myTreeViewer.refresh();
		this.progress.setText(CPAclipse.getPlugin().getTasks().size() + "Tasks listed");
		System.out.println("refreshed");
	}
	
	@Override
	public void setFocus() {
		if (this.myTreeViewer != null)
			this.myTreeViewer.getControl().setFocus();
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
	
	void handleMenuAboutToShow(IMenuManager manager) {
		
		
		manager.add(new Separator("New"));
		MenuManager newMenu = new MenuManager("Ne&w");
		manager.appendToGroup("New", newMenu);
		newMenu.add(new NewWizardMenu(this.getSite().getWorkbenchWindow(),"org.sosy_lab.cpachecker.plugin.eclipse.wizards.NewTaskCreationWizard"));
		/*ActionGroup[] groups = new ActionGroup[1];
		groups[0] = new ActionGroup() {
			@Override
			public void fillContextMenu(IMenuManager menu) {
				MenuManager newMenu = new MenuManager("Ne&w");
			    menu.appendToGroup("New", newMenu);
				newMenu.add(new NewWizardMenu(TasksView.this.getSite().getWorkbenchWindow()));
				super.fillContextMenu(menu);
			}
		};*/
		
		final IStructuredSelection selection = (IStructuredSelection) myTreeViewer.getSelection();
		if (selection.isEmpty()) {
			return;
		}
		List<Task> selectedTasks = new ArrayList<Task>();
		for (Object element : selection.toList()) {
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
				case TASK :
					selectedTasks.add(((TaskNode)element).getTask());
			        break;
				}
			}
			if (exitLoop) {
				break;
			}
		}
		
		if (selection.getFirstElement() instanceof File) {
			System.out.println("added new Open action");
			
			BaseSelectionListenerAction o = new OpenSystemEditorAction(this.getSite().getPage());
			o.selectionChanged(selection);
            manager.add(o);
            
		}
		fillOpenWithMenu(manager, selection);
		OpenAction open = new OpenAction(this.getSite());
		open.selectionChanged(selection);
		manager.add(open);
		RefreshAction refresh = new RefreshAction(this);
		refresh.selectionChanged(selection);
		manager.add(refresh);
		manager.add(new Action() {
			@Override
			public String getText() {
				return "Refresh this view";
			}
			@Override
			public void run() {
				refresh(); // refresh the taskView
				super.run();
			}
		});
		if ( ! selectedTasks.isEmpty()) {
			manager.add(new Separator());
			manager.add(new RunMultipleTasksAction(this.getSite().getShell(), selectedTasks));
			manager.add(new Separator());
			manager.add(new RenameTasksAction(this.getSite().getShell(), selectedTasks));
			manager.add(new DeleteTasksAction(this.getSite().getShell(), selectedTasks));
			manager.add(new SaveTasksAction());
			manager.add(new Separator());
			if (selection.size()==1 
					&& selection.getFirstElement() instanceof Node 
					&& (((Node)selection.getFirstElement()).getType().equals(NodeType.TASK))) {
				manager.add(new SetConfigFileInTaskAction(this.getSite().getShell(), selectedTasks.get(0)));
				manager.add(new SetSpecFileInTaskAction(this.getSite().getShell(), selectedTasks.get(0)));
				manager.add(new SetSourceFileInTaskAction(this.getSite().getShell(), selectedTasks.get(0)));
			}
		}
		
		//this.getSite().setSelectionProvider(myTreeViewer);

		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS + "-end")); //$NON-NLS-1$
	}
	
	@Override
	public Shell getShell() {
		return this.getSite().getShell();
	}
	
    private void fillOpenWithMenu(IMenuManager menu,
            IStructuredSelection selection) {

        // Only supported if exactly one file is selected.
        if (selection.size() != 1) {
			return;
		}
        Object element = selection.getFirstElement();
        if (!(element instanceof IFile)) {
			return;
		}

        MenuManager submenu = new MenuManager(
        		"Open Wit&h", CPAclipse.getPlugin() + ".OpenWithSubMenu"
        		);
        submenu.add(new OpenWithMenu(this.getSite().getPage(),
                (IFile) element));
        menu.add(submenu);
    }

}

