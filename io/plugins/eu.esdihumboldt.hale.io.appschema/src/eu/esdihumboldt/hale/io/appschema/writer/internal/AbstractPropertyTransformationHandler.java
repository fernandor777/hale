/*
 * Copyright (c) 2015 Data Harmonisation Panel
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Contributors:
 *     Data Harmonisation Panel <http://www.dhpanel.eu>
 */

package eu.esdihumboldt.hale.io.appschema.writer.internal;

import static eu.esdihumboldt.hale.io.appschema.writer.AppSchemaMappingUtils.QNAME_XSI_NIL;
import static eu.esdihumboldt.hale.io.appschema.writer.AppSchemaMappingUtils.XSI_PREFIX;
import static eu.esdihumboldt.hale.io.appschema.writer.AppSchemaMappingUtils.XSI_URI;
import static eu.esdihumboldt.hale.io.appschema.writer.AppSchemaMappingUtils.findOwningType;
import static eu.esdihumboldt.hale.io.appschema.writer.AppSchemaMappingUtils.getGeometryPropertyEntity;
import static eu.esdihumboldt.hale.io.appschema.writer.AppSchemaMappingUtils.getTargetProperty;
import static eu.esdihumboldt.hale.io.appschema.writer.AppSchemaMappingUtils.getTargetType;
import static eu.esdihumboldt.hale.io.appschema.writer.AppSchemaMappingUtils.isGeometryType;
import static eu.esdihumboldt.hale.io.appschema.writer.AppSchemaMappingUtils.isGmlId;
import static eu.esdihumboldt.hale.io.appschema.writer.AppSchemaMappingUtils.isNested;
import static eu.esdihumboldt.hale.io.appschema.writer.AppSchemaMappingUtils.isNilReason;
import static eu.esdihumboldt.hale.io.appschema.writer.AppSchemaMappingUtils.isNillable;
import static eu.esdihumboldt.hale.io.appschema.writer.AppSchemaMappingUtils.isXmlAttribute;

import java.util.List;

import javax.xml.namespace.QName;

import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;

import com.google.common.base.Strings;

import de.fhg.igd.slf4jplus.ALogger;
import de.fhg.igd.slf4jplus.ALoggerFactory;
import eu.esdihumboldt.hale.common.align.model.AlignmentUtil;
import eu.esdihumboldt.hale.common.align.model.Cell;
import eu.esdihumboldt.hale.common.align.model.ChildContext;
import eu.esdihumboldt.hale.common.align.model.Condition;
import eu.esdihumboldt.hale.common.align.model.EntityDefinition;
import eu.esdihumboldt.hale.common.align.model.Property;
import eu.esdihumboldt.hale.common.align.model.impl.PropertyEntityDefinition;
import eu.esdihumboldt.hale.common.schema.model.Definition;
import eu.esdihumboldt.hale.common.schema.model.PropertyDefinition;
import eu.esdihumboldt.hale.common.schema.model.TypeDefinition;
import eu.esdihumboldt.hale.io.appschema.impl.internal.generated.app_schema.AttributeExpressionMappingType;
import eu.esdihumboldt.hale.io.appschema.impl.internal.generated.app_schema.AttributeMappingType;
import eu.esdihumboldt.hale.io.appschema.impl.internal.generated.app_schema.AttributeMappingType.ClientProperty;
import eu.esdihumboldt.hale.io.appschema.impl.internal.generated.app_schema.NamespacesPropertyType.Namespace;
import eu.esdihumboldt.hale.io.appschema.impl.internal.generated.app_schema.TypeMappingsPropertyType.FeatureTypeMapping;
import eu.esdihumboldt.hale.io.appschema.model.ChainConfiguration;
import eu.esdihumboldt.hale.io.appschema.model.FeatureChaining;
import eu.esdihumboldt.hale.io.appschema.writer.AppSchemaMappingUtils;
import eu.esdihumboldt.hale.io.appschema.writer.internal.mapping.AppSchemaMappingContext;
import eu.esdihumboldt.hale.io.appschema.writer.internal.mapping.AppSchemaMappingWrapper;
import eu.esdihumboldt.hale.io.xsd.constraint.XmlAttributeFlag;

/**
 * Base class for property transformation handlers.
 * 
 * @author Stefano Costa, GeoSolutions
 */
public abstract class AbstractPropertyTransformationHandler
		implements PropertyTransformationHandler {

	private static final ALogger log = ALoggerFactory
			.getLogger(AbstractPropertyTransformationHandler.class);

	/**
	 * The app-schema mapping configuration under construction.
	 */
	protected AppSchemaMappingWrapper mapping;
	/**
	 * The type cell owning the property cell to handle.
	 */
	protected Cell typeCell;
	/**
	 * The property cell to handle.
	 */
	protected Cell propertyCell;
	/**
	 * The target property.
	 */
	protected Property targetProperty;

	/**
	 * The feature type mapping which is the parent of the attribute mapping
	 * generated by this handler.
	 */
	protected FeatureTypeMapping featureTypeMapping;
	/**
	 * The attribute mapping generated by this handler.
	 */
	protected AttributeMappingType attributeMapping;

	/**
	 * @see eu.esdihumboldt.hale.io.appschema.writer.internal.PropertyTransformationHandler#handlePropertyTransformation(eu.esdihumboldt.hale.common.align.model.Cell,
	 *      eu.esdihumboldt.hale.common.align.model.Cell,
	 *      eu.esdihumboldt.hale.io.appschema.writer.internal.mapping.AppSchemaMappingContext)
	 */
	@Override
	public AttributeMappingType handlePropertyTransformation(Cell typeCell, Cell propertyCell,
			AppSchemaMappingContext context) {
		this.mapping = context.getMappingWrapper();
		this.typeCell = typeCell;
		this.propertyCell = propertyCell;
		// TODO: does this hold for any transformation function?
		this.targetProperty = getTargetProperty(propertyCell);
		PropertyEntityDefinition targetPropertyEntityDef = targetProperty.getDefinition();
		PropertyDefinition targetPropertyDef = targetPropertyEntityDef.getDefinition();

		TypeDefinition featureType = null;
		String mappingName = null;
		if (AppSchemaMappingUtils.isJoin(typeCell)) {
			if (context.getFeatureChaining() != null) {
				ChainConfiguration chainConf = findChainConfiguration(context);
				if (chainConf != null) {
					featureType = chainConf.getNestedTypeTargetType();
					mappingName = chainConf.getMappingName();
				}
			}
			else {
				// this is just a best effort attempt to determine the target
				// feature type, may result in incorrect mappings
				featureType = findOwningType(targetPropertyEntityDef,
						context.getRelevantTargetTypes());
			}
		}
		if (featureType == null) {
			featureType = getTargetType(typeCell).getDefinition().getType();
		}

		// double check: don't map properties that belong to a feature
		// chaining configuration other than the current one
		if (context.getFeatureChaining() != null) {
			for (String joinId : context.getFeatureChaining().getJoins().keySet()) {
				List<ChainConfiguration> chains = context.getFeatureChaining().getChains(joinId);
				ChainConfiguration chainConf = findLongestNestedPath(
						targetPropertyEntityDef.getPropertyPath(), chains);
				if (chainConf != null && !chainConf.getNestedTypeTargetType().equals(featureType)) {
					// don't translate mapping, will do it (or have done it)
					// elsewhere!
					featureType = null;
					break;
				}
			}
		}

		if (featureType != null) {
			// handle MongoDB collection handling special case
			// if (mappingName == null) {
			// JsonPathConstraint jsonPath =
			// AppSchemaMappingUtils.getSourceProperty(propertyCell)
			// .getDefinition().getType().getConstraint(JsonPathConstraint.class);
			// mappingName = jsonPath.getJsonPath();
			// }

			// fetch FeatureTypeMapping from mapping configuration
			this.featureTypeMapping = context.getOrCreateFeatureTypeMapping(featureType,
					mappingName);

			// TODO: verify source property (if any) belongs to mapped source
			// type

			// fetch AttributeMappingType from mapping
			if (isXmlAttribute(targetPropertyDef)) {
				// gml:id attribute requires special handling, i.e. an
				// <idExpression> tag must be added to the attribute mapping for
				// target feature types and geometry types
				TypeDefinition parentType = targetPropertyDef.getParentType();
				if (isGmlId(targetPropertyDef)) {
					// TODO: handle gml:id for geometry types
					if (featureType.equals(parentType)) {
						handleAsFeatureGmlId(featureType, mappingName, context);
					}
					else if (isGeometryType(parentType)) {
						handleAsGeometryGmlId(featureType, mappingName, context);
					}
					else {
						handleAsXmlAttribute(featureType, mappingName, context);
					}
				}
				else {
					handleAsXmlAttribute(featureType, mappingName, context);
				}
			}
			else {
				handleAsXmlElement(featureType, mappingName, context);
			}
		}

		return attributeMapping;
	}

	/**
	 * @param context the mapping context
	 * @return the chain configuration that applies to the current property
	 *         mapping
	 */
	private ChainConfiguration findChainConfiguration(AppSchemaMappingContext context) {
		ChainConfiguration chainConf = null;

		PropertyEntityDefinition targetPropertyEntityDef = targetProperty.getDefinition();
		FeatureChaining featureChaining = context.getFeatureChaining();
		if (featureChaining != null) {
			List<ChildContext> targetPropertyPath = targetPropertyEntityDef.getPropertyPath();
			List<ChainConfiguration> chains = featureChaining.getChains(typeCell.getId());
			chainConf = findLongestNestedPath(targetPropertyPath, chains);
		}

		return chainConf;
	}

	private ChainConfiguration findLongestNestedPath(List<ChildContext> targetPropertyPath,
			List<ChainConfiguration> chains) {
		ChainConfiguration chainConf = null;

		if (chains != null && chains.size() > 0 && targetPropertyPath != null
				&& targetPropertyPath.size() > 0) {
			int maxPathLength = 0;
			for (ChainConfiguration chain : chains) {
				List<ChildContext> nestedTargetPath = chain.getNestedTypeTarget().getPropertyPath();

				boolean isNested = isNested(nestedTargetPath, targetPropertyPath);

				if (isNested && maxPathLength < nestedTargetPath.size()) {
					maxPathLength = nestedTargetPath.size();
					chainConf = chain;
				}
			}
		}

		return chainConf;
	}

	/**
	 * This method is invoked when the target type is the feature type owning
	 * this attribute mapping, and the target property is <code>gml:id</code>,
	 * which needs special handling.
	 * 
	 * <p>
	 * In practice, this means that <code>&lt;idExpression&gt;</code> is used in
	 * place of:
	 * 
	 * <pre>
	 *   &lt;ClientProperty&gt;
	 *     &lt;name&gt;...&lt;/name&gt;
	 *     &lt;value&gt;...&lt;/value&gt;
	 *   &lt;/ClientProperty&gt;
	 * </pre>
	 * 
	 * and that the target attribute is set to the mapped feature type name.
	 * 
	 * </p>
	 * 
	 * @param featureType the target feature type
	 * @param mappingName the target feature type's mapping name (may be
	 *            <code>null</code>)
	 * @param context the app-schema mapping context
	 */
	protected void handleAsFeatureGmlId(TypeDefinition featureType, String mappingName,
			AppSchemaMappingContext context) {
		PropertyEntityDefinition targetPropertyEntityDef = targetProperty.getDefinition();
		List<ChildContext> gmlIdPath = targetPropertyEntityDef.getPropertyPath();

		attributeMapping = context.getOrCreateAttributeMapping(featureType, mappingName, gmlIdPath);
		// set targetAttribute to feature type qualified name
		attributeMapping.setTargetAttribute(featureTypeMapping.getTargetElement());
		// set id expression
		AttributeExpressionMappingType idExpression = new AttributeExpressionMappingType();
		idExpression.setOCQL(getSourceExpressionAsCQL());
		// TODO: not sure whether any CQL expression can be used here
		attributeMapping.setIdExpression(idExpression);
	}

	/**
	 * This method is invoked when the target property's parent is a geometry
	 * and the target property is <code>gml:id</code> (which needs special
	 * handling).
	 * 
	 * <p>
	 * In practice, this means that <code>&lt;idExpression&gt;</code> is used in
	 * place of:
	 * 
	 * <pre>
	 *   &lt;ClientProperty&gt;
	 *     &lt;name&gt;...&lt;/name&gt;
	 *     &lt;value&gt;...&lt;/value&gt;
	 *   &lt;/ClientProperty&gt;
	 * </pre>
	 * 
	 * @param featureType the target feature type
	 * @param mappingName the target feature type's mapping name (may be
	 *            <code>null</code>)
	 * @param context the app-schema mapping context
	 */
	protected void handleAsGeometryGmlId(TypeDefinition featureType, String mappingName,
			AppSchemaMappingContext context) {
		PropertyEntityDefinition targetPropertyEntityDef = targetProperty.getDefinition();
		PropertyEntityDefinition geometry = (PropertyEntityDefinition) AlignmentUtil
				.getParent(targetPropertyEntityDef);

		createGeometryAttributeMapping(featureType, mappingName, geometry, context);

		// set id expression
		AttributeExpressionMappingType idExpression = new AttributeExpressionMappingType();
		idExpression.setOCQL(getSourceExpressionAsCQL());
		// TODO: not sure whether any CQL expression can be used here
		attributeMapping.setIdExpression(idExpression);
	}

	/**
	 * This method is invoked when the target property is an XML attribute (
	 * {@link XmlAttributeFlag} constraint is set).
	 * 
	 * <p>
	 * The property transformation is translated to:
	 * 
	 * <pre>
	 *   <code>&lt;ClientProperty&gt;
	 *     &lt;name&gt;[target property name]&lt;/name&gt;
	 *     &lt;value&gt;[CQL expression]&lt;/value&gt;
	 *   &lt;/ClientProperty&gt;</code>
	 * </pre>
	 * 
	 * and added to the attribute mapping generated for the XML element owning
	 * the attribute.
	 * </p>
	 * 
	 * @param featureType the target feature type
	 * @param mappingName the target feature type's mapping name (may be
	 *            <code>null</code>)
	 * @param context the app-schema mapping context
	 */
	protected void handleAsXmlAttribute(TypeDefinition featureType, String mappingName,
			AppSchemaMappingContext context) {
		PropertyEntityDefinition targetPropertyEntityDef = targetProperty.getDefinition();
		PropertyDefinition targetPropertyDef = targetPropertyEntityDef.getDefinition();

		// fetch attribute mapping for parent property
		EntityDefinition parentDef = AlignmentUtil.getParent(targetPropertyEntityDef);
		if (parentDef != null) {
			List<ChildContext> parentPropertyPath = parentDef.getPropertyPath();
			PropertyDefinition parentPropertyDef = parentPropertyPath.isEmpty() ? null
					: parentPropertyPath.get(parentPropertyPath.size() - 1).getChild().asProperty();
			if (parentPropertyDef != null) {
				attributeMapping = context.getOrCreateAttributeMapping(featureType, mappingName,
						parentPropertyPath);
				// set targetAttribute if empty
				if (attributeMapping.getTargetAttribute() == null
						|| attributeMapping.getTargetAttribute().isEmpty()) {
					attributeMapping.setTargetAttribute(
							mapping.buildAttributeXPath(featureType, parentPropertyPath));
				}

				Namespace targetPropNS = mapping.getOrCreateNamespace(
						targetPropertyDef.getName().getNamespaceURI(),
						targetPropertyDef.getName().getPrefix());
				String unqualifiedName = targetPropertyDef.getName().getLocalPart();
				boolean isQualified = targetPropNS != null
						&& !Strings.isNullOrEmpty(targetPropNS.getPrefix());

				// encode attribute as <ClientProperty>
				ClientProperty clientProperty = new ClientProperty();
				@SuppressWarnings("null")
				String clientPropName = (isQualified)
						? targetPropNS.getPrefix() + ":" + unqualifiedName : unqualifiedName;
				clientProperty.setName(clientPropName);
				clientProperty.setValue(getSourceExpressionAsCQL());
				setEncodeIfEmpty(clientProperty);

				// don't add client property if it already exists
				if (!hasClientProperty(clientProperty.getName())) {
					attributeMapping.getClientProperty().add(clientProperty);

					// if mapping nilReason, parent property is nillable and no
					// sourceExpression has been set yet, add xsi:nil attribute
					// following the same encoding logic of nilReason (i.e. null
					// when nilReason is null and viceversa)
					if (isNilReason(targetPropertyDef) && isNillable(parentPropertyDef)
							&& attributeMapping.getSourceExpression() == null) {
						addOrReplaceXsiNilAttribute(clientProperty.getValue(), true, context);
					}
				}
			}
		}
	}

	/**
	 * This method is invoked when the target property is a regular XML element.
	 * 
	 * @param featureType the target feature type
	 * @param mappingName the target feature type's mapping name (may be
	 *            <code>null</code>)
	 * @param context the app-schema mapping context
	 */
	protected void handleAsXmlElement(TypeDefinition featureType, String mappingName,
			AppSchemaMappingContext context) {
		PropertyEntityDefinition targetPropertyEntityDef = targetProperty.getDefinition();
		PropertyDefinition targetPropertyDef = targetPropertyEntityDef.getDefinition();
		TypeDefinition targetPropertyType = targetPropertyDef.getPropertyType();

		if (isGeometryType(targetPropertyType)) {
			handleXmlElementAsGeometryType(featureType, mappingName, context);
		}
		else {
			attributeMapping = context.getOrCreateAttributeMapping(featureType, mappingName,
					targetPropertyEntityDef.getPropertyPath());
			List<ChildContext> targetPropertyPath = targetPropertyEntityDef.getPropertyPath();
			// set target attribute
			attributeMapping.setTargetAttribute(
					mapping.buildAttributeXPath(featureType, targetPropertyPath));
		}

		// set source expression
		AttributeExpressionMappingType sourceExpression = new AttributeExpressionMappingType();
		// TODO: is this general enough?
		sourceExpression.setOCQL(getSourceExpressionAsCQL());
		attributeMapping.setSourceExpression(sourceExpression);
		if (AppSchemaMappingUtils.isMultiple(targetPropertyDef)) {
			attributeMapping.setIsMultiple(true);
		}
		// if element is nillable, add xsi:nil attribute with inverted logic
		// (i.e. null if source expression is NOT null, and viceversa)
		if (isNillable(targetPropertyDef)) {
			addOrReplaceXsiNilAttribute(attributeMapping.getSourceExpression().getOCQL(), false,
					context);
		}
		// TODO: isList?
		// TODO: targetAttributeNode?
		// TODO: encodeIfEmpty?
	}

	/**
	 * This method is invoked when the target property is a GML geometry type.
	 * 
	 * <p>
	 * The target attribute is set to <code>gml:AbstractGeometry</code> and the
	 * concrete geometry type is specified in a
	 * <code>&lt;targetAttributeNode&gt;</code> tag.
	 * </p>
	 * 
	 * @param featureType the target feature type
	 * @param mappingName the target feature type's mapping name (may be
	 *            <code>null</code>)
	 * @param context the app-schema mapping context
	 */
	protected void handleXmlElementAsGeometryType(TypeDefinition featureType, String mappingName,
			AppSchemaMappingContext context) {
		PropertyEntityDefinition geometry = targetProperty.getDefinition();

		createGeometryAttributeMapping(featureType, mappingName, geometry, context);

		// GeometryTypes require special handling
		TypeDefinition geometryType = geometry.getDefinition().getPropertyType();
		QName geomTypeName = geometryType.getName();
		Namespace geomNS = context.getOrCreateNamespace(geomTypeName.getNamespaceURI(),
				geomTypeName.getPrefix());
		attributeMapping
				.setTargetAttributeNode(geomNS.getPrefix() + ":" + geomTypeName.getLocalPart());

		// set target attribute to parent (should be gml:AbstractGeometry)
		// TODO: this is really ugly, but I don't see a better way to do it
		// since HALE renames
		// {http://www.opengis.net/gml/3.2}AbstractGeometry element
		// to
		// {http://www.opengis.net/gml/3.2/AbstractGeometry}choice
		EntityDefinition parentEntityDef = AlignmentUtil.getParent(geometry);
		Definition<?> parentDef = parentEntityDef.getDefinition();
		String parentQName = geomNS.getPrefix() + ":" + parentDef.getDisplayName();
		List<ChildContext> targetPropertyPath = parentEntityDef.getPropertyPath();
		attributeMapping.setTargetAttribute(
				mapping.buildAttributeXPath(featureType, targetPropertyPath) + "/" + parentQName);
	}

	/**
	 * Wraps the provided CQL expression in a conditional expression, based on
	 * the filter defined on the property.
	 * 
	 * <p>
	 * TODO: current implementation is broken, don't use it (first argument of
	 * if_then_else must be an expression, cannot be a filter (i.e. cannot
	 * contain '=' sign))!
	 * </p>
	 * 
	 * @param propertyEntityDef the property definition defining the condition
	 * @param cql the CQL expression to wrap
	 * @return a conditional expression wrapping the provided CQL expression
	 */
	protected static String getConditionalExpression(PropertyEntityDefinition propertyEntityDef,
			String cql) {
		if (propertyEntityDef != null) {
			String propertyName = propertyEntityDef.getDefinition().getName().getLocalPart();
			List<ChildContext> propertyPath = propertyEntityDef.getPropertyPath();
			// TODO: conditions are supported only on simple (not nested)
			// properties
			if (propertyPath.size() == 1) {
				Condition condition = propertyPath.get(0).getCondition();
				if (condition != null) {
					String fitlerText = AlignmentUtil.getFilterText(condition.getFilter());
					// remove "parent" references
					fitlerText = fitlerText.replace("parent.", "");
					// replace "value" references with the local name of the
					// property itself
					fitlerText = fitlerText.replace("value", propertyName);

					return String.format("if_then_else(%s, %s, Expression.NIL)", fitlerText, cql);
				}
			}
		}

		return cql;
	}

	/**
	 * Template method to be implemented by subclasses.
	 * 
	 * <p>
	 * This is where the translation logic should go. Basically, the propety
	 * transformation must be converted to a CQL expression producing the same
	 * result.
	 * </p>
	 * 
	 * @return a CQL expression producing the same result as the HALE
	 *         transformation
	 */
	protected abstract String getSourceExpressionAsCQL();

	private void createGeometryAttributeMapping(TypeDefinition featureType, String mappingName,
			PropertyEntityDefinition geometry, AppSchemaMappingContext context) {
		EntityDefinition geometryProperty = getGeometryPropertyEntity(geometry);

		// use geometry property path to create / retrieve attribute mapping
		attributeMapping = context.getOrCreateAttributeMapping(featureType, mappingName,
				geometryProperty.getPropertyPath());
	}

	/**
	 * If client property is set to a constant expression, add
	 * &lt;encodeIfEmpty&gt;true&lt;/encodeIfEmpty&gt; to the attribute mapping
	 * to make sure the element is encoded also if it has no value.
	 * 
	 * @param clientProperty the client property to test
	 */
	private void setEncodeIfEmpty(ClientProperty clientProperty) {
		try {
			Expression expr = CQL.toExpression(getSourceExpressionAsCQL());
			if (expr != null && expr instanceof Literal) {
				attributeMapping.setEncodeIfEmpty(true);
			}
		} catch (CQLException e) {
			log.warn("Cannot set encodeIfEmpty value. Reason: " + e.getMessage());
		}
	}

	/**
	 * Adds an {@code xsi:nil} client property to the attribute mapping, or
	 * updates the existing one.
	 * 
	 * <p>
	 * The CQL expression for the property's value is derived from the provided
	 * source expression in this way:
	 * 
	 * <ul>
	 * <li>{@code sameLogic=true} -->
	 * {@code if_then_else(isNull([sourceExpression]), Expression.NIL, 'true')}</li>
	 * <li>{@code sameLogic=false} -->
	 * {@code if_then_else(isNull([sourceExpression]), 'true', Expression.NIL)}</li>
	 * </ul>
	 * </p>
	 * 
	 * @param sourceExpression the source expression
	 * @param sameLogic if {@code true}, {@code xsi:nil} will be {@code null}
	 *            when {@code sourceExpression} is and {@code 'true'} when it
	 *            isn't, if {@code false} the opposite applies
	 * @param context the app-schema mapping context
	 */
	private void addOrReplaceXsiNilAttribute(String sourceExpression, boolean sameLogic,
			AppSchemaMappingContext context) {
		final String sameLogicPattern = "if_then_else(isNull(%s), Expression.NIL, 'true')";
		final String invertedLogicPattern = "if_then_else(isNull(%s), 'true', Expression.NIL)";
		final String pattern = sameLogic ? sameLogicPattern : invertedLogicPattern;

		// make sure xsi namespace is included in the mapping
		context.getOrCreateNamespace(XSI_URI, XSI_PREFIX);

		String xsiNilQName = QNAME_XSI_NIL.getPrefix() + ":" + QNAME_XSI_NIL.getLocalPart();

		ClientProperty xsiNil = getClientProperty(xsiNilQName);
		if (xsiNil == null) {
			xsiNil = new ClientProperty();
			// add xsi:nil attribute
			attributeMapping.getClientProperty().add(xsiNil);
		}
		xsiNil.setName(xsiNilQName);
		xsiNil.setValue(String.format(pattern, sourceExpression));
	}

	private boolean hasClientProperty(String name) {
		return getClientProperty(name) != null;
	}

	private ClientProperty getClientProperty(String name) {
		for (ClientProperty existentProperty : attributeMapping.getClientProperty()) {
			if (name.equals(existentProperty.getName())) {
				return existentProperty;
			}
		}
		return null;
	}
}
