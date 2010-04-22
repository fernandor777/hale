/*
 * HUMBOLDT: A Framework for Data Harmonisation and Service Integration.
 * EU Integrated Project #030962                 01.10.2006 - 30.09.2010
 * 
 * For more information on the project, please refer to the this web site:
 * http://www.esdi-humboldt.eu
 * 
 * LICENSE: For information on the license under which this program is 
 * available, please refer to http:/www.esdi-humboldt.eu/license.html#core
 * (c) the HUMBOLDT Consortium, 2007 to 2010.
 */

package eu.esdihumboldt.hale.rcp.views.table.tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;

import eu.esdihumboldt.hale.rcp.utils.tree.DefaultTreeNode;

/**
 * Tree item representing a feature type property
 *
 * @author Simon Templer
 * @partner 01 / Fraunhofer Institute for Computer Graphics Research
 * @version $Id$ 
 */
public class PropertyItem extends DefaultTreeNode {
	
	private static final Logger log = Logger.getLogger(PropertyItem.class);
	
	/**
	 * The property name
	 */
	private final String propertyName;

	/**
	 * Create a new property item
	 * 
	 * @param propertyName the property name
	 * @param label the item label
	 */
	public PropertyItem(String propertyName, String label) {
		super(label);
		
		this.propertyName = propertyName;
	}
	
	/**
	 * Get the value of the property for the given feature
	 * 
	 * @param feature the feature
	 * 
	 * @return the feature's property value
	 */
	public String getText(Feature feature) {
		Object value = getValue(feature);
		if (value == null) {
			return "null"; //$NON-NLS-1$
		}
		else {
			if (value instanceof Collection<?>) {
				return Arrays.toString(((Collection<?>) value).toArray());
			}
			else {
				return value.toString();
			}
		}
	}
	
	private Object getValue(Feature feature) {
		if (getParent() instanceof PropertyItem) {
			// property of a property
			Object propertyValue = ((PropertyItem) getParent()).getValue(feature);
			if (propertyValue != null) {
				Collection<?> propertyValues = (Collection<?>) ((propertyValue instanceof Collection<?>)?(propertyValue):(Collections.singleton(propertyValue)));
				List<Object> values = new ArrayList<Object>();
				
				for (Object pValue : propertyValues) {
					if (pValue instanceof Feature) {
						Property property = ((Feature) pValue).getProperty(propertyName);
						if (property != null) {
							values.add(property.getValue());
						}
					}
				}
				
				if (values.isEmpty()) {
					return null;
				}
				else if (values.size() == 1) {
					return values.get(0);
				}
				else {
					return values;
				}
			}
			else {
				return null;
			}
		}
		else {
			// property of the feature
			Property property = feature.getProperty(propertyName);
			if (property != null) {
				return property.getValue();
			}
			else {
				log.warn("Error getting property " + propertyName + " from feature of type " + feature.getType().getName().getLocalPart());
				return "#not defined";
			}
		}
	}

	/**
	 * @see Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((propertyName == null) ? 0 : propertyName.hashCode());
		return result;
	}

	/**
	 * @see Object#equals(Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		PropertyItem other = (PropertyItem) obj;
		if (propertyName == null) {
			if (other.propertyName != null)
				return false;
		} else if (!propertyName.equals(other.propertyName))
			return false;
		return true;
	}

}
