/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel.source.hbm.state.binding;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.MappingException;
import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.binding.CascadeType;
import org.hibernate.metamodel.binding.state.SimpleAttributeBindingState;
import org.hibernate.metamodel.source.hbm.HbmHelper;
import org.hibernate.metamodel.source.hbm.util.MappingHelper;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLHibernateMapping.XMLClass.XMLId;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLHibernateMapping.XMLClass.XMLTimestamp;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLHibernateMapping.XMLClass.XMLVersion;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLParamElement;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLPropertyElement;
import org.hibernate.metamodel.source.spi.BindingContext;
import org.hibernate.metamodel.source.spi.MappingDefaults;
import org.hibernate.metamodel.source.spi.MetaAttributeContext;

/**
 * @author Gail Badner
 */
public class HbmSimpleAttributeBindingState extends AbstractHbmAttributeBindingState
		implements SimpleAttributeBindingState {
	private final String typeName;
	private final Map<String, String> typeParameters = new HashMap<String, String>();

	private final boolean isLazy;
	private final PropertyGeneration propertyGeneration;
	private final boolean isInsertable;
	private final boolean isUpdatable;

	public HbmSimpleAttributeBindingState(
			String ownerClassName,
			BindingContext bindingContext,
			MetaAttributeContext parentMetaAttributeContext,
			XMLId id) {
		super(
				ownerClassName,
				id.getName() != null ? id.getName() : bindingContext.getMappingDefaults().getIdColumnName(),
				bindingContext,
				id.getNode(),
				HbmHelper.extractMetaAttributeContext( id.getMeta(), parentMetaAttributeContext ),
				HbmHelper.getPropertyAccessorName( id.getAccess(), false, bindingContext.getMappingDefaults().getPropertyAccessorName() ),
				true
		);

		this.isLazy = false;
		if ( id.getTypeAttribute() != null ) {
			typeName = maybeConvertToTypeDefName( id.getTypeAttribute(), bindingContext.getMappingDefaults() );
		}
		else if ( id.getType() != null ) {
			typeName = maybeConvertToTypeDefName( id.getType().getName(), bindingContext.getMappingDefaults() );
		}
		else {
			typeName = getTypeNameByReflection();
		}

		// TODO: how should these be set???
		this.propertyGeneration = PropertyGeneration.parse( null );
		this.isInsertable = true;

		this.isUpdatable = false;
	}

	private static String maybeConvertToTypeDefName(String typeName, MappingDefaults defaults) {
		String actualTypeName = typeName;
		if ( typeName != null ) {
			// TODO: tweak for typedef...
		}
		else {
		}
		return actualTypeName;
	}

	public HbmSimpleAttributeBindingState(
			String ownerClassName,
			BindingContext bindingContext,
			MetaAttributeContext parentMetaAttributeContext,
			XMLVersion version) {
		super(
				ownerClassName,
				version.getName(),
				bindingContext,
				version.getNode(),
				HbmHelper.extractMetaAttributeContext( version.getMeta(), parentMetaAttributeContext ),
				HbmHelper.getPropertyAccessorName( version.getAccess(), false, bindingContext.getMappingDefaults().getPropertyAccessorName() ),
				true
		);
		this.typeName = version.getType() == null ? "integer" : version.getType();

		this.isLazy = false;

		// for version properties marked as being generated, make sure they are "always"
		// generated; aka, "insert" is invalid; this is dis-allowed by the DTD,
		// but just to make sure.
		this.propertyGeneration = PropertyGeneration.parse( version.getGenerated().value() );
		if ( propertyGeneration == PropertyGeneration.INSERT ) {
			throw new MappingException( "'generated' attribute cannot be 'insert' for versioning property" );
		}
		this.isInsertable = MappingHelper.getBooleanValue( version.isInsert(), true );
		this.isUpdatable = true;
	}

	public HbmSimpleAttributeBindingState(
			String ownerClassName,
			BindingContext bindingContext,
			MetaAttributeContext parentMetaAttributeContext,
			XMLTimestamp timestamp) {

		super(
				ownerClassName,
				timestamp.getName(),
				bindingContext,
				timestamp.getNode(),
				HbmHelper.extractMetaAttributeContext( timestamp.getMeta(), parentMetaAttributeContext ),
				HbmHelper.getPropertyAccessorName( timestamp.getAccess(), false, bindingContext.getMappingDefaults().getPropertyAccessorName() ),
				true
		);

		// Timestamp.getType() is not defined
		this.typeName = "db".equals( timestamp.getSource() ) ? "dbtimestamp" : "timestamp";
		this.isLazy = false;

		// for version properties marked as being generated, make sure they are "always"
		// generated; aka, "insert" is invalid; this is dis-allowed by the DTD,
		// but just to make sure.
		this.propertyGeneration = PropertyGeneration.parse( timestamp.getGenerated().value() );
		if ( propertyGeneration == PropertyGeneration.INSERT ) {
			throw new MappingException( "'generated' attribute cannot be 'insert' for versioning property" );
		}
		this.isInsertable = true; //TODO: is this right????
		this.isUpdatable = true;
	}

	public HbmSimpleAttributeBindingState(
			String ownerClassName,
			BindingContext bindingContext,
			MetaAttributeContext parentMetaAttributeContext,
			XMLPropertyElement property) {
		super(
				ownerClassName,
				property.getName(),
				bindingContext,
				property.getNode(),
				HbmHelper.extractMetaAttributeContext( property.getMeta(), parentMetaAttributeContext ),
				HbmHelper.getPropertyAccessorName( property.getAccess(), false, bindingContext.getMappingDefaults().getPropertyAccessorName() ),
				property.isOptimisticLock()
		);
		this.isLazy = property.isLazy();
		this.propertyGeneration = PropertyGeneration.parse( property.getGenerated() );

		if ( propertyGeneration == PropertyGeneration.ALWAYS || propertyGeneration == PropertyGeneration.INSERT ) {
			// generated properties can *never* be insertable.
			if ( property.isInsert() != null && property.isInsert() ) {
				// the user specifically supplied insert="true", which constitutes an illegal combo
				throw new MappingException(
						"cannot specify both insert=\"true\" and generated=\"" + propertyGeneration.getName() +
								"\" for property: " +
								property.getName()
				);
			}
			isInsertable = false;
		}
		else {
			isInsertable = MappingHelper.getBooleanValue( property.isInsert(), true );
		}
		if ( propertyGeneration == PropertyGeneration.ALWAYS ) {
			if ( property.isUpdate() != null && property.isUpdate() ) {
				// the user specifically supplied update="true",
				// which constitutes an illegal combo
				throw new MappingException(
						"cannot specify both update=\"true\" and generated=\"" + propertyGeneration.getName() +
								"\" for property: " +
								property.getName()
				);
			}
			isUpdatable = false;
		}
		else {
			isUpdatable = MappingHelper.getBooleanValue( property.isUpdate(), true );
		}

		if ( property.getTypeAttribute() != null ) {
			typeName = maybeConvertToTypeDefName( property.getTypeAttribute(), bindingContext.getMappingDefaults() );
		}
		else if ( property.getType() != null ) {
			typeName = maybeConvertToTypeDefName( property.getType().getName(), bindingContext.getMappingDefaults() );
			for ( XMLParamElement typeParameter : property.getType().getParam() ) {
				//TODO: add parameters from typedef
				typeParameters.put( typeParameter.getName(), typeParameter.getValue().trim() );
			}
		}
		else {
			typeName = getTypeNameByReflection();
		}


		// TODO: check for typedef first
		/*
		TypeDef typeDef = mappings.getTypeDef( typeName );
		if ( typeDef != null ) {
			typeName = typeDef.getTypeClass();
			// parameters on the property mapping should
			// override parameters in the typedef
			Properties allParameters = new Properties();
			allParameters.putAll( typeDef.getParameters() );
			allParameters.putAll( parameters );
			parameters = allParameters;
		}
        */
	}

	protected boolean isEmbedded() {
		return false;
	}

	public String getTypeName() {
		return typeName;
	}

	public Map<String, String> getTypeParameters() {
		return typeParameters;
	}

	public boolean isLazy() {
		return isLazy;
	}

	public PropertyGeneration getPropertyGeneration() {
		return propertyGeneration;
	}

	public boolean isInsertable() {
		return isInsertable;
	}

	public boolean isUpdatable() {
		return isUpdatable;
	}

	public Set<CascadeType> getCascadeTypes() {
		return null;
	}

	public boolean isKeyCascadeDeleteEnabled() {
		//TODO: implement
		return false;
	}

	public String getUnsavedValue() {
		//TODO: implement
		return null;
	}
}
