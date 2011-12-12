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

package eu.esdihumboldt.hale.ui.common.graph.content;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.zest.core.viewers.IGraphEntityContentProvider;

import eu.esdihumboldt.hale.common.align.extension.function.AbstractFunction;
import eu.esdihumboldt.hale.common.align.extension.function.AbstractParameter;
import eu.esdihumboldt.hale.common.align.extension.function.PropertyFunction;
import eu.esdihumboldt.hale.common.align.extension.function.PropertyParameter;
import eu.esdihumboldt.hale.common.align.extension.function.TypeFunction;
import eu.esdihumboldt.hale.common.align.extension.function.TypeParameter;
import eu.esdihumboldt.util.Pair;

/**
 * TODO Type description
 * 
 * @author Patrick Lieb
 */
public class SourceTargetContentProvider extends ArrayContentProvider implements
		IGraphEntityContentProvider {

//	private Collection<Object> collection;

	/**
	 * @see IGraphEntityContentProvider#getConnectedTo(Object)
	 */
	@Override
	// complication with other pairs?
	public Object[] getConnectedTo(Object entity) {
		Collection<Object> result = new ArrayList<Object>();
		if (entity instanceof Pair<?, ?>) {
			Pair<?, ?> pair = (Pair<?, ?>) entity;
			if (pair.getFirst() instanceof AbstractFunction<?>) {
				// set must be of type AbstractParameter
				if (pair.getSecond() instanceof Set<?>) {
					Set<?> set = ((Set<?>) (pair
							.getSecond()));
					return set.toArray();
				}
			}
			if (pair.getFirst() instanceof AbstractParameter) {
				result.add(pair.getSecond());
				return result.toArray();
			}
		}
		return null;
	}

	/**
	 * @see ArrayContentProvider#getElements(Object)
	 */
	@Override
	public Object[] getElements(Object inputElement) {
//		init();
		Collection<Object> collection = new ArrayList<Object>();
		Pair<AbstractFunction<?>, Set<?>> functionpair;
		if (inputElement instanceof AbstractFunction<?>) {
			AbstractFunction<?> function = (AbstractFunction<?>) inputElement;
			functionpair = new Pair<AbstractFunction<?>, Set<?>>(function,
					function.getTarget());
			collection.add(functionpair);

			if (inputElement instanceof TypeFunction) {
				for (TypeParameter type : ((TypeFunction) function).getSource()) {
					// TODO save pair with correct classes?
					collection
							.add(new Pair<Object, Object>(type, functionpair));
				}
				for (TypeParameter type : ((TypeFunction) function).getTarget()) {
					collection.add(type);
				}
			}

			if (inputElement instanceof PropertyFunction) {
				for (PropertyParameter prop : ((PropertyFunction) function)
						.getSource()) {
					// TODO save pair with correct classes
					collection
							.add(new Pair<Object, Object>(prop, functionpair));
				}
				for (PropertyParameter prop : ((PropertyFunction) function)
						.getTarget()) {
					collection.add(prop);
				}
			}
			return collection.toArray();

		}
		return super.getElements(inputElement);
	}

//	private void init() {
//		if (collection == null) {
//			collection = new ArrayList<Object>();
//		}
//	}

}
