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

package eu.esdihumboldt.hale.io.xsd.reader.internal;

import javax.xml.namespace.QName;

import eu.esdihumboldt.hale.schema.model.Constraint;
import eu.esdihumboldt.hale.schema.model.Definition;
import eu.esdihumboldt.hale.schema.model.TypeDefinition;
import eu.esdihumboldt.hale.schema.model.impl.AbstractDefinition;

/**
 * Schema element
 *
 * @author Simon Templer
 * @partner 01 / Fraunhofer Institute for Computer Graphics Research
 */
public class SchemaElement extends AbstractDefinition<Constraint> {

	/**
	 * The element type
	 */
	private final TypeDefinition type;
	
	/**
	 * The substitution group
	 */
	private final QName substitutionGroup;
	
	/**
	 * Create a new schema element
	 * 
	 * @param elementName the element name
	 * @param type the associated type definition
	 * @param substitutionGroup the substitution group, may be <code>null</code>
	 */
	public SchemaElement(QName elementName, TypeDefinition type,
			QName substitutionGroup) {
		super(elementName);
		
		this.type = type;
		this.substitutionGroup = substitutionGroup;
		
		//TODO set schema element constraint on type
	}

	/**
	 * Get the type definition associated with the element
	 * 
	 * @return the element type
	 */
	public TypeDefinition getType() {
		return type;
	}

	/**
	 * Get the element substitution group
	 * 
	 * @return the substitution group or <code>null</code>
	 */
	public QName getSubstitutionGroup() {
		return substitutionGroup;
	}

	/**
	 * @see Definition#getIdentifier()
	 */
	@Override
	public String getIdentifier() {
		return getName().getNamespaceURI() + "/" + getName().getLocalPart();
	}

}
