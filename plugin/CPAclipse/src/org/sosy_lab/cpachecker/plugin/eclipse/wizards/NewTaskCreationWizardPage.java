package org.sosy_lab.cpachecker.plugin.eclipse.wizards;

import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.CoreModelUtil;
import org.eclipse.cdt.core.model.ICModel;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.sosy_lab.cpachecker.plugin.eclipse.CPAclipse;

public class NewTaskCreationWizardPage extends WizardPage {
	private Text sourceTextControl;
	private Text configTextControl;
	private Text specificationTextControl;
	private Text nameTextControl;
	
	NewTaskCreationWizard parent;
	private Composite composite;
	private ITranslationUnit sourceFile;
	private IFile configFile;
	private IFile specificationFile;

	protected NewTaskCreationWizardPage(String pageName, NewTaskCreationWizard newTaskWizard) {
		super(pageName);
		parent = newTaskWizard;
	}
	
	public void infoChanged() {
		if (getITranslationUnit() == null || getConfigFile() == null || getSpecificationFile() == null) {
			super.setPageComplete(false);
		} else {
			super.setPageComplete(true);
		}
	}
	@Override
	public void createControl(Composite parent) {
		composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		
		Label nameLabel = new Label(composite, SWT.NONE);
		nameLabel.setText("Task Name:");
		
		nameTextControl = new Text(composite, SWT.BORDER);
		GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		nameTextControl.setLayoutData(gridData);
		nameTextControl.setText("default name");
		
		Label configLabel = new Label(composite, SWT.NONE);
		configLabel.setText("Config File:");
		
		Composite partComposite = new Composite(composite, SWT.NONE);
		partComposite.setLayout(new GridLayout(2, false));
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		partComposite.setLayoutData(gridData);
		
		configTextControl = new Text(partComposite, SWT.BORDER);
		configTextControl.setEnabled(false);
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		configTextControl.setLayoutData(gridData);
		configTextControl.setText("something.properties");
		
		Button selButton = new Button(partComposite, SWT.NONE);
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.RIGHT;
		gridData.grabExcessHorizontalSpace = false;
		selButton.setLayoutData(gridData);
		selButton.setText("select config file");
		selButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(
						NewTaskCreationWizardPage.this.getShell(), 
						new WorkbenchLabelProvider(), new BaseWorkbenchContentProvider());
				dialog.setTitle("Config File Selection");
				dialog.setMessage("Select the config file from the tree:");
				dialog.setInput(ResourcesPlugin.getWorkspace().getRoot());
				dialog.setAllowMultiple(false);
				dialog.open();
				if (dialog.getFirstResult() instanceof IFile) {
					IFile result = (IFile) dialog.getFirstResult();
					if (result.getFileExtension().equals("properties")) {
						NewTaskCreationWizardPage.this.configFile = result;
						NewTaskCreationWizardPage.this.configTextControl.setText(result.getFullPath().toPortableString());
						NewTaskCreationWizardPage.this.setMessage("Config File Set");
					} else {
						setErrorMessage("No Java Properties File.");
					}
				} else {
					setErrorMessage("Failed to locate the file.");
				}
				NewTaskCreationWizardPage.this.infoChanged();
				super.mouseDown(e);
			}
		});
		
		Label specificationLabel = new Label(composite, SWT.NONE);
		specificationLabel.setText("Specification File:");
		gridData = new GridData();
		gridData.verticalAlignment = SWT.TOP;
		specificationLabel.setLayoutData(gridData);
		
		partComposite = new Composite(composite, SWT.NONE);
		partComposite.setLayout(new GridLayout(2, false));
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		partComposite.setLayoutData(gridData);
		
		specificationTextControl = new Text(partComposite, SWT.BORDER);
		specificationTextControl.setEnabled(false);
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		specificationTextControl.setLayoutData(gridData);
		specificationTextControl.setText("something.spc");
		
		selButton = new Button(partComposite, SWT.NONE);
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.RIGHT;
		gridData.grabExcessHorizontalSpace = false;
		selButton.setLayoutData(gridData);
		selButton.setText("select specification file");
		selButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(
						NewTaskCreationWizardPage.this.getShell(), 
						new WorkbenchLabelProvider(), new BaseWorkbenchContentProvider());
				dialog.setTitle("Specification File Selection");
				dialog.setMessage("Select the specification file from the tree:");
				dialog.setInput(ResourcesPlugin.getWorkspace().getRoot());
				dialog.setAllowMultiple(false);
				dialog.open();
				if (dialog.getFirstResult() instanceof IFile) {
					IFile result = (IFile) dialog.getFirstResult();
					if (result.getFileExtension().equals("spc")) {
						NewTaskCreationWizardPage.this.specificationFile = result;
						NewTaskCreationWizardPage.this.specificationTextControl.setText(result.getFullPath().toPortableString());
						NewTaskCreationWizardPage.this.setMessage("Specification File Set");
					} else {
						setErrorMessage("No Specification File.");
					}
				} else {
					setErrorMessage("Failed to locate the file.");
				}
				NewTaskCreationWizardPage.this.infoChanged();
				super.mouseDown(e);
			}
		});
		
		Label sourceLabel = new Label(composite, SWT.NONE);
		sourceLabel.setText("Source File:");
		gridData = new GridData();
		gridData.verticalAlignment = SWT.TOP;
		sourceLabel.setLayoutData(gridData);
		
		partComposite = new Composite(composite, SWT.NONE);
		partComposite.setLayout(new GridLayout(2, false));
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		partComposite.setLayoutData(gridData);
		
		sourceTextControl = new Text(partComposite, SWT.BORDER | SWT.WRAP | SWT.MULTI);
		sourceTextControl.setEnabled(false);
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.verticalAlignment = SWT.FILL;
		gridData.grabExcessVerticalSpace = true;
		sourceTextControl.setLayoutData(gridData);
		sourceTextControl.setText("something.c");
		
		selButton = new Button(partComposite, SWT.NONE);
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.RIGHT;
		gridData.grabExcessHorizontalSpace = false;
		selButton.setLayoutData(gridData);
		selButton.setText("select source file");
		selButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(
						NewTaskCreationWizardPage.this.getShell(), 
						new WorkbenchLabelProvider(), new BaseWorkbenchContentProvider());
				dialog.setTitle("Source File Selection");
				dialog.setMessage("Select the source file from the tree:");
				dialog.setInput(ResourcesPlugin.getWorkspace().getRoot());
				dialog.setAllowMultiple(false);
				dialog.open();
				if (dialog.getFirstResult() instanceof IFile) {
					IFile result = (IFile) dialog.getFirstResult();
					if (obtainSourceFile(result)) {
						NewTaskCreationWizardPage.this.sourceTextControl.setText(result.getFullPath().toPortableString());
					}
				} else {
					setErrorMessage("Failed to locate the file.");
				}
				NewTaskCreationWizardPage.this.infoChanged();
				super.mouseDown(e);
			}
		});
		super.setControl(composite);
	}
	
	IFile getConfigFile() {
		return configFile;
	}
	IFile getSpecificationFile() {
		return specificationFile;
	}
	
	private void setITranslationUnit(ITranslationUnit source) {
		this.sourceFile = source;
	}

	public String getTaskName() {
		return nameTextControl.getText();
	}
	
	private boolean obtainSourceFile(IFile sourceFile) {
		IWorkspaceRoot fWorkspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
	    ICModel fInput = CoreModel.create(fWorkspaceRoot);
		ITranslationUnit tu = null;
		try {
			ICProject[] cProjects = fInput.getCProjects();
			IProject[] projectsToLookIn = new IProject[cProjects.length];
			for (int i = 0; i < cProjects.length; i++) {
				projectsToLookIn[i] = cProjects[i].getProject();
			}
			tu = CoreModelUtil.findTranslationUnit(sourceFile);
		} catch (CModelException e) {
			this.setErrorMessage("Exception in the CModel: " + e.getLocalizedMessage());
			CPAclipse.logError("Exception in the CModel: ", e);
			return false;
		} catch (Exception e) {
			this.setErrorMessage("Failed to locate the file " + sourceFile.getFullPath().toString());
			CPAclipse.logError("Failed to locate the file ", e);
			return false;
		}
		if (tu == null) {
			this.setErrorMessage("Could not find the c source file " + sourceFile.getFullPath().toString());
			return false;
		} else {
			this.setITranslationUnit(tu);
			this.setErrorMessage(null);
			this.setMessage("Source is " + tu.getPath() + " in Project " + tu.getCProject().getElementName());
			return true;
		}
	}

	public ITranslationUnit getITranslationUnit() {
		return sourceFile;
	}
}
