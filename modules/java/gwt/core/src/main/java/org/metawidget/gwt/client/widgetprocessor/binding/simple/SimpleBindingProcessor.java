// Metawidget
//
// This file is dual licensed under both the LGPL
// (http://www.gnu.org/licenses/lgpl-2.1.html) and the EPL
// (http://www.eclipse.org/org/documents/epl-v10.php). As a
// recipient of Metawidget, you may choose to receive it under either
// the LGPL or the EPL.
//
// Commercial licenses are also available. See http://metawidget.org
// for details.

package org.metawidget.gwt.client.widgetprocessor.binding.simple;

import static org.metawidget.inspector.InspectionResultConstants.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.metawidget.gwt.client.ui.GwtMetawidget;
import org.metawidget.gwt.client.ui.Stub;
import org.metawidget.util.simple.PathUtils;
import org.metawidget.util.simple.StringUtils;
import org.metawidget.widgetprocessor.iface.AdvancedWidgetProcessor;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.Widget;

/**
 * Simple, Generator-based property and action binding processor.
 *
 * @author <a href="http://kennardconsulting.com">Richard Kennard</a>
 */

public class SimpleBindingProcessor
	implements AdvancedWidgetProcessor<Widget, GwtMetawidget> {

	//
	// Private members
	//

	private final Map<Class<?>, SimpleBindingProcessorAdapter<?>>	mAdapters;

	private final Map<Class<?>, Converter<?>>						mConverters;

	//
	// Constructor
	//

	public SimpleBindingProcessor() {

		this( new SimpleBindingProcessorConfig() );
	}

	public SimpleBindingProcessor( SimpleBindingProcessorConfig config ) {

		// Custom adapters

		if ( config.getAdapters() == null ) {
			mAdapters = null;
		} else {

			// WeakHashMap would be better here, but not supported by GWT:
			// http://code.google.com/webtoolkit/doc/latest/RefJreEmulation.html#Package_java_util

			mAdapters = new HashMap<Class<?>, SimpleBindingProcessorAdapter<?>>( config.getAdapters() );
		}

		// Default converters
		//
		// WeakHashMap would be better here, but not supported by GWT:
		// http://code.google.com/webtoolkit/doc/latest/RefJreEmulation.html#Package_java_util

		mConverters = new HashMap<Class<?>, Converter<?>>();

		Converter<?> simpleConverter = new SimpleConverter();
		mConverters.put( Boolean.class, simpleConverter );
		mConverters.put( Character.class, simpleConverter );
		mConverters.put( Number.class, simpleConverter );

		// Custom converters

		if ( config.getConverters() != null ) {
			mConverters.putAll( config.getConverters() );
		}
	}

	//
	// Public methods
	//

	public void onStartBuild( GwtMetawidget metawidget ) {

		metawidget.putClientProperty( SimpleBindingProcessor.class, null );
	}

	public Widget processWidget( Widget widget, String elementName, Map<String, String> attributes, final GwtMetawidget metawidget ) {

		// Nested Metawidgets are not bound, only remembered

		if ( widget instanceof GwtMetawidget ) {
			State state = getState( metawidget );

			if ( state.nestedMetawidgets == null ) {
				state.nestedMetawidgets = new HashSet<GwtMetawidget>();
			}

			state.nestedMetawidgets.add( (GwtMetawidget) widget );
			return widget;
		}

		// SimpleBindingProcessor doesn't bind to Stubs or FlexTables

		if ( widget instanceof Stub || widget instanceof FlexTable ) {
			return widget;
		}

		String path = metawidget.getPath();

		if ( PROPERTY.equals( elementName ) || ACTION.equals( elementName ) ) {
			path += StringUtils.SEPARATOR_FORWARD_SLASH_CHAR + attributes.get( NAME );
		}

		final String[] names = PathUtils.parsePath( path ).getNamesAsArray();

		// Bind actions

		if ( ACTION.equals( elementName ) ) {
			if ( !( widget instanceof FocusWidget ) ) {
				throw new RuntimeException( "SimpleBindingProcessor only supports binding actions to FocusWidgets - '" + attributes.get( NAME ) + "' is using a " + widget.getClass().getName() );
			}

			if ( ( (FocusWidget) widget ).isEnabled() ) {

				// Bind the action

				FocusWidget focusWidget = (FocusWidget) widget;
				focusWidget.addClickHandler( new ClickHandler() {

					public void onClick( ClickEvent event ) {

						// Use the adapter...

						Object toInvokeOn = metawidget.getToInspect();

						if ( toInvokeOn == null ) {
							return;
						}

						Class<?> classToBindTo = toInvokeOn.getClass();
						SimpleBindingProcessorAdapter<Object> adapter = getAdapter( classToBindTo );

						if ( adapter == null ) {
							throw new RuntimeException( "Don't know how to bind to a " + classToBindTo );
						}

						// ...to invoke the action

						adapter.invokeAction( toInvokeOn, names );
					}
				} );
			}

			return widget;
		}

		// From the adapter...

		Object toInspect = metawidget.getToInspect();

		if ( toInspect == null ) {
			return widget;
		}

		Class<?> classToBindTo = toInspect.getClass();
		SimpleBindingProcessorAdapter<Object> adapter = getAdapter( classToBindTo );

		if ( adapter == null ) {
			throw new RuntimeException( "Don't know how to bind to a " + classToBindTo );
		}

		// ...fetch the value...

		Object value = adapter.getProperty( toInspect, names );

		// ...convert it (if necessary)...

		Class<?> propertyType = adapter.getPropertyType( toInspect, names );
		Converter<Object> converter = getConverter( propertyType );

		if ( converter != null ) {
			value = converter.convertForWidget( widget, value );
		}

		// ...and set it

		try {
			metawidget.setValue( value, widget );

			if ( TRUE.equals( attributes.get( NO_SETTER ) ) ) {
				return widget;
			}

			State state = getState( metawidget );

			if ( state.bindings == null ) {
				state.bindings = new HashSet<Object[]>();
			}

			state.bindings.add( new Object[] { widget, names, converter, propertyType } );
		} catch ( Exception e ) {
			Window.alert( path + ": " + e.getMessage() );
		}

		return widget;
	}

	/**
	 * Rebinds the Metawidget to the given Object.
	 * <p>
	 * This method is an optimization that allows clients to load a new object into the binding
	 * <em>without</em> calling setToInspect, and therefore without reinspecting the object or
	 * recreating the components. It is the client's responsbility to ensure the rebound object is
	 * compatible with the original setToInspect.
	 */

	public void rebind( Object toRebind, GwtMetawidget metawidget ) {

		metawidget.updateToInspectWithoutInvalidate( toRebind );

		State state = getState( metawidget );

		// Our bindings

		if ( state.bindings != null ) {
			// From the adapter...

			Class<?> classToRebind = toRebind.getClass();
			SimpleBindingProcessorAdapter<Object> adapter = getAdapter( classToRebind );

			if ( adapter == null ) {
				throw new RuntimeException( "Don't know how to rebind to a " + classToRebind );
			}

			// ...for each bound property...

			for ( Object[] binding : state.bindings ) {
				Widget widget = (Widget) binding[0];
				String[] names = (String[]) binding[1];
				@SuppressWarnings( "unchecked" )
				Converter<Object> converter = (Converter<Object>) binding[2];

				// ...fetch the value...

				Object value = adapter.getProperty( toRebind, names );

				// ...convert it (if necessary)...

				if ( converter != null ) {
					value = converter.convertForWidget( widget, value );
				}

				// ...and set it

				metawidget.setValue( value, widget );
			}
		}

		// Nested Metawidgets

		if ( state.nestedMetawidgets != null ) {
			for ( GwtMetawidget nestedMetawidget : state.nestedMetawidgets ) {
				rebind( toRebind, nestedMetawidget );
			}
		}
	}

	public void save( GwtMetawidget metawidget ) {

		State state = getState( metawidget );

		// Our bindings

		if ( state.bindings != null ) {
			Object toSave = metawidget.getToInspect();

			if ( toSave == null ) {
				return;
			}

			// From the adapter...

			Class<?> classToBindTo = toSave.getClass();
			SimpleBindingProcessorAdapter<Object> adapter = getAdapter( classToBindTo );

			if ( adapter == null ) {
				throw new RuntimeException( "Don't know how to save to a " + classToBindTo );
			}

			// ...for each bound property...

			for ( Object[] binding : state.bindings ) {
				Widget widget = (Widget) binding[0];
				String[] names = (String[]) binding[1];
				@SuppressWarnings( "unchecked" )
				Converter<Object> converter = (Converter<Object>) binding[2];
				Class<?> type = (Class<?>) binding[3];

				// ...fetch the value...

				Object value = metawidget.getValue( widget );

				// ...convert it (if necessary)...

				if ( value != null && converter != null ) {
					value = converter.convertFromWidget( widget, value, type );
				}

				// ...and set it

				adapter.setProperty( toSave, value, names );
			}
		}

		// Nested Metawidgets

		if ( state.nestedMetawidgets != null ) {
			for ( GwtMetawidget nestedMetawidget : state.nestedMetawidgets ) {
				save( nestedMetawidget );
			}
		}
	}

	public void onEndBuild( GwtMetawidget metawidget ) {

		// Do nothing
	}

	//
	// Protected methods
	//

	/**
	 * Gets the Adapter for the given class (if any).
	 * <p>
	 * Includes traversing superclasses of the given <code>classToBindTo</code> for a suitable
	 * Adapter, so for example registering an Adapter for <code>Contact.class</code> will match
	 * <code>PersonalContact.class</code>, <code>BusinessContact.class</code> etc., unless a more
	 * subclass-specific Adapter is also registered.
	 */

	protected <T extends SimpleBindingProcessorAdapter<?>> T getAdapter( Class<?> classToBindTo ) {

		if ( mAdapters == null ) {
			return null;
		}

		Class<?> classTraversal = classToBindTo;

		while ( classTraversal != null ) {
			@SuppressWarnings( "unchecked" )
			T adapter = (T) mAdapters.get( classTraversal );

			if ( adapter != null ) {
				return adapter;
			}

			classTraversal = classTraversal.getSuperclass();
		}

		return null;
	}

	//
	// Private methods
	//

	/**
	 * Gets the Converter for the given Class (if any).
	 * <p>
	 * Includes traversing superclasses of the given <code>classToConvert</code> for a suitable
	 * Converter, so for example registering a Converter for <code>Number.class</code> will match
	 * <code>Integer.class</code>, <code>Double.class</code> etc., unless a more subclass-specific
	 * Converter is also registered.
	 */

	private <T extends Converter<?>> T getConverter( Class<?> classToConvert ) {

		Class<?> classTraversal = classToConvert;

		while ( classTraversal != null ) {
			@SuppressWarnings( "unchecked" )
			T converter = (T) mConverters.get( classTraversal );

			if ( converter != null ) {
				return converter;
			}

			classTraversal = classTraversal.getSuperclass();
		}

		return null;
	}

	/* package private */State getState( GwtMetawidget metawidget ) {

		State state = (State) metawidget.getClientProperty( SimpleBindingProcessor.class );

		if ( state == null ) {
			state = new State();
			metawidget.putClientProperty( SimpleBindingProcessor.class, state );
		}

		return state;
	}

	//
	// Inner class
	//

	/**
	 * Simple, lightweight structure for saving state.
	 */

	/* package private */static class State {

		/* package private */Set<Object[]>		bindings;

		/* package private */Set<GwtMetawidget>	nestedMetawidgets;
	}
}
