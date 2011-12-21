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

package eu.esdihumboldt.hale.ui.views.data.internal.compare;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;

import eu.esdihumboldt.hale.common.schema.model.TypeDefinition;
import eu.esdihumboldt.hale.ui.common.definition.viewer.TypeIndexContentProvider;

/**
 * Content provider for a tree representing the structure of a 
 * {@link TypeDefinition}. Requires a {@link TypeDefinition} as input. 
 * @author Simon Templer
 */
public class TypeDefinitionContentProvider extends TypeIndexContentProvider {

	/**
	 * @see TypeIndexContentProvider#TypeIndexContentProvider(TreeViewer)
	 */
	public TypeDefinitionContentProvider(TreeViewer tree) {
		super(tree);
	}

	/**
	 * @see ITreeContentProvider#getElements(Object)
	 */
	@Override
	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof TypeDefinition) {
			return ((TypeDefinition) inputElement).getChildren().toArray();
			//TODO metadata?
		}
		else {
			throw new IllegalArgumentException("Content provider only applicable for type definitions.");
		}
	}

}