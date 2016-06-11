/*
The contents of this file are subject to the Mozilla Public License Version 1.1
(the "License"); you may not use this file except in compliance with the License.
You may obtain a copy of the License at http://www.mozilla.org/MPL/

Software distributed under the License is distributed on an "AS IS" basis, WITHOUT
WARRANTY OF ANY KIND, either express or implied. See the License for the specific
language governing rights and limitations under the License.

The Original Code is "BaseHandler.java". Description:
"Base class that provides default behaviour for ConfigurationHandlers"

The Initial Developer of the Original Code is Bryan Tripp & Centre for Theoretical Neuroscience, University of Waterloo. Copyright (C) 2006-2008. All Rights Reserved.

Alternatively, the contents of this file may be used under the terms of the GNU
Public License license (the GPL License), in which case the provisions of GPL
License are applicable  instead of those above. If you wish to allow use of your
version of this file only under the terms of the GPL License and not to allow
others to use your version of this file under the MPL, indicate your decision
by deleting the provisions above and replace  them with the notice and other
provisions required by the GPL License.  If you do not delete the provisions above,
a recipient may use your version of this file under either the MPL or the GPL License.
*/

/*
 * Created on 17-Dec-07
 */
package ca.nengo.config.handlers;

import java.lang.reflect.InvocationTargetException;

import ca.nengo.config.ConfigurationHandler;
import ca.nengo.config.IconRegistry;
import ca.nengo.config.ui.ConfigurationChangeListener;

/**
 * Base class that provides default behaviour for ConfigurationHandlers.
 *
 * @author Bryan Tripp
 */
public abstract class BaseHandler implements ConfigurationHandler {

	private Class<?> myClass;

	/**
	 * @param c Class of objects handled by this handler
	 */
	public BaseHandler(Class<?> c) {
		myClass = c;
	}

	/**
	 * @return true if arg matches class given in constructor
	 * @see ca.nengo.config.ConfigurationHandler#canHandle(java.lang.Class)
	 */
	public boolean canHandle(Class<?> c) {
		return myClass.isAssignableFrom(c);
	}

	/**
	 * @return myClass.getConstructor(new Class[]{String.class}).newInstance(new Object[]{s})
	 * @see ca.nengo.config.ConfigurationHandler#fromString(java.lang.String)
	 */
	public Object fromString(String s) {
		try {
			return myClass.getConstructor(new Class[]{String.class}).newInstance(new Object[]{s});
		} catch (Exception e) {
			Throwable t = e;
			if (t instanceof InvocationTargetException) {
                t = e.getCause();
            }
			throw new RuntimeException(t);
		}
	}

	/**
	 * @return o.toString()
	 * @see ca.nengo.config.ConfigurationHandler#toString(java.lang.Object)
	 */
	public String toString(Object o) {
		return o.toString();
	}

}
