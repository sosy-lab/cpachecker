package org.sosy_lab.cpachecker.plugin.eclipse.popup.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Shell;
import org.sosy_lab.cpachecker.plugin.eclipse.CPAcheckerPlugin;
import org.sosy_lab.cpachecker.plugin.eclipse.Task;
import org.sosy_lab.cpachecker.plugin.eclipse.TasksIO;

public class SaveTasksAction extends Action {
	private Shell shell;
	private List<Task> toSave;
	
	public SaveTasksAction(Shell shell, List<Task> toSave) {
		super();
		this.shell = shell;
		this.toSave = toSave;
		this.setText("save all tasks");
	}

	@Override
	public void run() {
		TasksIO.saveTasks(CPAcheckerPlugin.getPlugin().getTasks());
		CPAcheckerPlugin.getPlugin().fireTasksChanged();
	}
}
