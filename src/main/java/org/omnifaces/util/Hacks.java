/*
 * Copyright 2012 OmniFaces.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.omnifaces.util;

import javax.faces.context.FacesContext;
import javax.faces.context.PartialViewContext;
import java.util.Collections;
import java.util.Set;

/**
 * Collection of JSF implementation and/or JSF component library specific hacks.
 *
 * @author Bauke Scholtz
 * @author Arjan Tijms
 * @since 1.3
 */
public final class Hacks {

    // Constants ------------------------------------------------------------------------------------------------------
    private static final String MYFACES_RENDERED_SCRIPT_RESOURCES_KEY = "org.apache.myfaces.RENDERED_SCRIPT_RESOURCES_SET";
    private static final String MYFACES_RENDERED_STYLESHEET_RESOURCES_KEY = "org.apache.myfaces.RENDERED_STYLESHEET_RESOURCES_SET";
    private static final Set<String> MOJARRA_MYFACES_RESOURCE_DEPENDENCY_KEYS = Utils.unmodifiableSet("com.sun.faces.PROCESSED_RESOURCE_DEPENDENCIES", MYFACES_RENDERED_SCRIPT_RESOURCES_KEY, MYFACES_RENDERED_STYLESHEET_RESOURCES_KEY);

    // Constructors/init ----------------------------------------------------------------------------------------------

    private Hacks() {
        //
    }


    /**
     * Remove the resource dependency processing related attributes from the given faces context.
     *
     * @param context The involved faces context.
     */
    public static void removeResourceDependencyState(FacesContext context) {
        // Mojarra and MyFaces remembers processed resource dependencies in a map.
        context.getAttributes().keySet().removeAll(MOJARRA_MYFACES_RESOURCE_DEPENDENCY_KEYS);

        // Mojarra and PrimeFaces puts "namelibrary=true" for every processed resource dependency.
        // TODO: This may possibly conflict with other keys with value=true. So far tested, this is harmless.
        context.getAttributes().values().removeAll(Collections.singleton(true));
    }


    /**
     * RichFaces PartialViewContext implementation does not have any getWrapped() method to return the wrapped
     * PartialViewContext. So a reflection hack is necessary to return it from the private field.
     *
     * @return The wrapped PartialViewContext from the RichFaces PartialViewContext implementation.
     */
    public static PartialViewContext getRichFacesWrappedPartialViewContext() {

        return null;
    }


}