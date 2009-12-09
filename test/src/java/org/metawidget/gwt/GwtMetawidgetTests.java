// Metawidget
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

package org.metawidget.gwt;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.metawidget.gwt.client.ui.layout.FlexTableLayoutTest;
import org.metawidget.gwt.client.ui.layout.LabelLayoutDecoratorTest;
import org.metawidget.gwt.client.widgetprocessor.binding.simple.SimpleBindingProcessorTest;

/**
 * GwtMetawidget test cases.
 *
 * These are just some fringe-case tests. Most of the testing is done as GWTUnitTests.
 *
 * @author Richard Kennard
 */

public class GwtMetawidgetTests
	extends TestCase
{
	//
	// Public statics
	//

	public static Test suite()
	{
		TestSuite suite = new TestSuite( "Gwt Metawidget Tests" );
		suite.addTestSuite( FlexTableLayoutTest.class );
		suite.addTestSuite( LabelLayoutDecoratorTest.class );
		suite.addTestSuite( SimpleBindingProcessorTest.class );

		return suite;
	}
}
