package org.sosy_lab.cpachecker.plugin.eclipse.editors.propertieseditor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Map.Entry;

import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLayoutData;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.SaveAsDialog;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;
import org.sosy_lab.cpachecker.plugin.eclipse.CPAclipse;

public class PropertiesTableEditor extends EditorPart {

	private Composite parent;
	private TableViewer propertiesTableViewer;
	private IFileEditorInput input;
	private Button newButton;
	private Button delButton;
	
	private boolean dirty = false;
	
	
	private List<Property> data = new ArrayList<Property>();
	
	@Override
	public void doSave(IProgressMonitor pMonitor) {
		try {
			storeToFile(this.input.getFile().getLocation().toFile());
		} catch (IOException e) {
			pMonitor.setCanceled(true);
			CPAclipse.logError("Could not store Properties", e);
		}
	}

	/** Stores the properties of this class in  the file, sets dirty to false.
	 * @param ioFile
	 * @throws IOException
	 */
	private void storeToFile(File ioFile) throws IOException {
		Properties prop = new Properties();
		for (Property p : data) {
			prop.setProperty(p.getKey(), p.getValue());
		}
		prop.store(new FileWriter(ioFile), "Stored by the PropertiesEditor of CPAclipse");
		this.dirty = false;
		firePropertyChange(PROP_DIRTY);
	}
	
	@Override
	public void doSaveAs() {
		// copied & modified from org.eclipse.ui.texteditor.AbstractDecoratedTextEditor.performSaveAs(IProgressMonitor)
		Shell shell= getSite().getShell();
		final IEditorInput input= getEditorInput();
		final IEditorInput newInput;

		if (input instanceof IURIEditorInput && !(input instanceof IFileEditorInput)) {
			FileDialog dialog= new FileDialog(shell, SWT.SAVE);
			IPath oldPath= URIUtil.toPath(((IURIEditorInput)input).getURI());
			if (oldPath != null) {
				dialog.setFileName(oldPath.lastSegment());
				dialog.setFilterPath(oldPath.toOSString());
			}

			String path= dialog.open();
			if (path == null) {
				return;
			}

			// Check whether file exists and if so, confirm overwrite
			final File localFile= new File(path);
			if (localFile.exists()) {
		        MessageDialog overwriteDialog= new MessageDialog(
		        		shell,
		        		"Overwrite?",
		        		null,
		        		"Overwrite the existing file?",
		        		MessageDialog.WARNING,
		        		new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL },
		        		1); // 'No' is the default
				if (overwriteDialog.open() != Window.OK) {
					return;
				}
			}
			try {
				this.storeToFile(localFile);
			} catch (IOException e) {
				CPAclipse.logError("Could not store Properties", e);
			}

		} else {
			SaveAsDialog dialog= new SaveAsDialog(shell);

			IFile original= (input instanceof IFileEditorInput) ? ((IFileEditorInput) input).getFile() : null;
			if (original != null)
				dialog.setOriginalFile(original);

			dialog.create();

			if (original != null && original.exists()) {
				String message= "The original file has been deleted or is not accessible";
				dialog.setErrorMessage(null);
				dialog.setMessage(message, IMessageProvider.WARNING);
			}

			if (dialog.open() == Window.CANCEL) {
				return;
			}

			IPath filePath= dialog.getResult();
			if (filePath == null) {
				return;
			}
			IWorkspace workspace= ResourcesPlugin.getWorkspace();
			IFile file= workspace.getRoot().getFile(filePath);
			newInput= new FileEditorInput(file);

			try {
				this.storeToFile(file.getLocation().toFile());
			} catch (IOException e) {
				CPAclipse.logError("Could not store Properties", e);
			}
			this.setInput(newInput);
		}

	}
	@Override
	protected void setInput(IEditorInput input) {
		super.setInput(input);
		this.setPartName(input.getName());
		if (parent != null && !parent.isDisposed())
			parent.update();
		firePropertyChange(PROP_DIRTY);
	}
	@Override
	public void init(IEditorSite pSite, IEditorInput pInput)
			throws PartInitException {
		this.input = (IFileEditorInput)pInput.getAdapter(IFileEditorInput.class);
		setInput(pInput);
		setContentDescription("Tabular Editor for PropertyFiles");
		setPartName("Table PropertyFileEditor");
		setSite(pSite);
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return true;
	}

	@Override
	public void createPartControl(final Composite pParent) {
		this.parent = pParent;
		pParent.setData(this);

        GridLayout gridLayout = new GridLayout ();/*
        gridLayout.marginHeight = 10;
        gridLayout.verticalSpacing = 10;
        gridLayout.marginWidth = 10;
        gridLayout.horizontalSpacing = 10;*/
        pParent.setLayout (gridLayout);
        
		this.propertiesTableViewer = new TableViewer(pParent, SWT.MULTI | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
		
		TableViewerColumn keyColumn = new TableViewerColumn(propertiesTableViewer, SWT.BORDER);
		keyColumn.getColumn().setText("Key");
		TableViewerColumn valueColumn = new TableViewerColumn(propertiesTableViewer, SWT.BORDER);
		valueColumn.getColumn().setText("Value");
		propertiesTableViewer.setContentProvider(new ArrayContentProvider());
		propertiesTableViewer.setLabelProvider(new PropertyTableLabelProvider());
		
		//propertiesTableViewer.setCellEditors(new CellEditor[] {new TextCellEditor(propertiesTableViewer.getTable())});
		
		keyColumn.setEditingSupport(new PropertyTableEditingSupport(propertiesTableViewer, 0));
		valueColumn.setEditingSupport(new PropertyTableEditingSupport(propertiesTableViewer, 1));
		
		GridData data = new GridData (SWT.FILL, SWT.FILL, true, true);
		propertiesTableViewer.getTable().setLayoutData (data);
        propertiesTableViewer.getTable().setHeaderVisible(true);
        propertiesTableViewer.getTable().setLinesVisible(true);
        
        
        // key column
        ColumnLayoutData keyColumnLayout = new ColumnWeightData(40, true);
        // value column
        ColumnLayoutData valueColumnLayout = new ColumnWeightData(60, true);

        // set columns in Table layout
        
        TableLayout layout = new TableLayout();
        layout.addColumnData(keyColumnLayout);
        layout.addColumnData(valueColumnLayout);
        propertiesTableViewer.getTable().setLayout(layout);
        propertiesTableViewer.getTable().layout();
        
        newButton = new Button (pParent, SWT.PUSH);
        newButton.setVisible(true);
        newButton.setText("New Property...");
        
        newButton.addMouseListener(new MouseAdapter() {
        	@Override
        	public void mouseDown(MouseEvent e) {
        		PropertiesTableEditor.this.data.add(new Property("newKey", "newValue"));
        		PropertiesTableEditor.this.refresh();
        		super.mouseDown(e);
        	}
		});
        data = new GridData ();
        data.widthHint = 105;
        data.horizontalAlignment = SWT.LEFT;
        data.verticalAlignment = SWT.BOTTOM;
        newButton.setLayoutData (data);
        
        delButton = new Button (pParent, SWT.PUSH);
        delButton.setVisible(true);
        delButton.setText("Remove Property...");
        delButton.addMouseListener(new MouseAdapter() {
        	@Override
        	public void mouseDown(MouseEvent e) {
        		if (propertiesTableViewer.getSelection() instanceof IStructuredSelection) {
        			IStructuredSelection selection = (IStructuredSelection) propertiesTableViewer.getSelection();
        			propertiesTableViewer.remove(selection.toArray());
        			//propertiesTableViewer.remove(propertiesTableViewer.getSelectionIndices());
        		}
        		super.mouseDown(e);
        	}
		});
        data = new GridData ();
        data.widthHint = 105;
        data.horizontalAlignment = SWT.LEFT;
        data.verticalAlignment = SWT.BOTTOM;
        delButton.setLayoutData (data);
        
        //createTableViewer();
        loadFromFile();

	}
	private void refresh() {
		this.propertiesTableViewer.setInput(this.data);
		//this.propertiesTableViewer.refresh(true);
	}
	
	private void loadFromFile() {
		Properties prop = new Properties();
		File ioFile = this.input.getFile().getLocation().toFile();
		try {
			prop.load(new FileReader(ioFile));
			for (Entry<Object, Object> e : prop.entrySet()) {
				this.data.add(new Property(e.getKey().toString(), e.getValue().toString()));
				/*TableItem tableItem = createEmptyPropertyItem();
				tableItem.setText(0, e.getKey().toString());
				tableItem.setText(1, e.getValue().toString());
				*/
			}
		} catch (FileNotFoundException e) {
			CPAclipse.logError(e);
		} catch (IOException e) {
			CPAclipse.logError(e);
		}
		refresh();
	}
	
	@Override
	public void setFocus() {
		this.propertiesTableViewer.getTable().setFocus();
	}
	
	private class PropertyTableEditingSupport extends EditingSupport {
		int column;
		private CellEditor freeTextEditor;

		private PropertyTableEditingSupport(ColumnViewer viewer, int pColumn) {
			super(viewer);
			freeTextEditor = new TextCellEditor(((TableViewer) viewer).getTable());
			this.column = pColumn;
		}

		@Override
		protected void setValue(Object element, Object value) {
			if (element == null || value == null) {
				return;
			}
			if (element instanceof Property && value instanceof String) {
				if (column == 0) {
					((Property)element).setKey((String)value);
				} else {
					((Property)element).setValue((String)value);
				}
				PropertiesTableEditor.this.dirty = true;
				PropertiesTableEditor.this.firePropertyChange(PROP_DIRTY);
			} else {
				CPAclipse.logInfo(this.getClass().getName() + ".setValue() : could not set value " + value.getClass() + " in Object " + element.getClass());
			}
			getViewer().update(element, null);
		}

		@Override
		protected Object getValue(Object element) {
			if (element instanceof Property) {
				if (column == 0) {
					return ((Property)element).getKey();
				} else {
					return ((Property)element).getValue();
				}
			} else {
				return element.toString();
			}
			
		}

		@Override
		protected CellEditor getCellEditor(Object element) {
			return freeTextEditor;
		}

		@Override
		protected boolean canEdit(Object element) {
			return true;
		}
	}

	private class PropertyTableLabelProvider implements ITableLabelProvider {
		List<ILabelProviderListener> listeners = new ArrayList<ILabelProviderListener>(4);

		@Override
		public void removeListener(ILabelProviderListener listener) {
			listeners.remove(listener);
		}

		@Override
		public boolean isLabelProperty(Object element, String property) {
			return false;
		}

		@Override
		public void dispose() {
		}

		@Override
		public void addListener(ILabelProviderListener listener) {
			listeners.add(listener);
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (columnIndex == 0) {
				if (element instanceof Property) {
					return ((Property)element).getKey();
				} else {
					return element.toString();
				}
			} else {
				if (element instanceof Property) {
					return ((Property)element).getValue();
				} else {
					return element.toString();
				}
			}
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}
	}
}
