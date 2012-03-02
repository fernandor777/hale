/*
 * HUMBOLDT: A Framework for Data Harmonisation and Service Integration.
 * EU Integrated Project #030962                 01.10.2006 - 30.09.2010
 * 
 * For more information on the project, please refer to the this web site:
 * http://www.esdi-humboldt.eu
 * 
 * LICENSE: For information on the license under which this program is 
 * available, please refer to http:/www.esdi-humboldt.eu/license.html#core
 * (c) the HUMBOLDT Consortium, 2007 to 2011.
 */

package eu.esdihumboldt.hale.ui.common.definition.selector;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.FilteredTree;

import eu.esdihumboldt.hale.common.schema.model.TypeDefinition;
import eu.esdihumboldt.hale.common.schema.model.TypeIndex;
import eu.esdihumboldt.hale.ui.common.definition.viewer.DefinitionComparator;
import eu.esdihumboldt.hale.ui.common.definition.viewer.DefinitionLabelProvider;
import eu.esdihumboldt.hale.ui.common.definition.viewer.SchemaPatternFilter;
import eu.esdihumboldt.hale.ui.common.definition.viewer.TypePropertyContentProvider;
import eu.esdihumboldt.hale.ui.util.selector.AbstractTreeSelectionDialog;
import eu.esdihumboldt.hale.ui.util.viewer.tree.TreePathFilteredTree;

/**
 * Selection dialog for {@link TypeDefinition}s.
 * @author Simon Templer
 */
public class TypeDefinitionDialog extends AbstractTreeSelectionDialog<TypeDefinition> {

	private final TypeIndex types;

	/**
	 * Create a type definition selection dialog.
	 * @param parentShell the parent shell
	 * @param title the dialog title 
	 * @param initialSelection the initial selection
	 * @param types the type index
	 */
	public TypeDefinitionDialog(Shell parentShell, String title,
			TypeDefinition initialSelection, TypeIndex types) {
		super(parentShell, title, initialSelection);
		
		this.types = types;
	}

	@Override
	protected TreeViewer createViewer(Composite parent) {
		// create viewer
		SchemaPatternFilter patternFilter = new SchemaPatternFilter() {
			@Override
			protected boolean matches(Viewer viewer, Object element) {
				boolean superMatches = super.matches(viewer, element);
				if (!superMatches)
					return false;
				return acceptObject(viewer, getFilters(), ((TreePath)element).getLastSegment());
			}
		};
		patternFilter.setUseEarlyReturnIfMatcherIsNull(false);
		patternFilter.setIncludeLeadingWildcard(true);
		FilteredTree tree = new TreePathFilteredTree(parent, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER, patternFilter, true);
		tree.getViewer().setComparator(new DefinitionComparator());
		return tree.getViewer();
	}

	@Override
	protected void setupViewer(TreeViewer viewer,
			TypeDefinition initialSelection) {
		viewer.setLabelProvider(new DefinitionLabelProvider());
		viewer.setContentProvider(new TypePropertyContentProvider(viewer));
		
		viewer.setInput(types);
		
		if (initialSelection != null) {
			viewer.setSelection(new StructuredSelection(
					initialSelection));
		}
	}

	@Override
	protected TypeDefinition getObjectFromSelection(ISelection selection) {
		if (!selection.isEmpty() && selection instanceof IStructuredSelection) {
			Object element = ((IStructuredSelection) selection).getFirstElement();
			if (element instanceof TypeDefinition) {
				return (TypeDefinition) element;
			}
		}
		
		return null;
	}

}