package org.sosy_lab.cpachecker.plugin.eclipse.popup.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.sosy_lab.cpachecker.plugin.eclipse.CPAcheckerPlugin;
import org.sosy_lab.cpachecker.plugin.eclipse.TasksIO;
import org.sosy_lab.cpachecker.plugin.eclipse.TaskRunner.Task;

public class DeleteTasksAction extends Action {
	private Shell shell;
	private List<Task> toDelete;
	
	public DeleteTasksAction(Shell shell, List<Task> toDelete) {
		super();
		this.shell = shell;
		this.toDelete = toDelete;
		this.setText("delete selected tasks");
	}

	@Override
	public void run() {
		if (MessageDialog.openQuestion(shell, "Deletion Confirmation", "Sure? \nTask cannot be restored, all Tasks have to be saved \n(we dont support undo)")) {
			CPAcheckerPlugin.getPlugin().removeTasks(toDelete);
			TasksIO.saveTasks(CPAcheckerPlugin.getPlugin().getTasks());
			CPAcheckerPlugin.getPlugin().fireTasksChanged();
		}
	}
}
