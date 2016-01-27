/*
 * Copyright 2013 OmniFaces.
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

import javax.faces.application.ViewHandler;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.view.facelets.FaceletContext;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * <p/>
 * Collection of utility methods for the JSF API that are mainly shortcuts for obtaining stuff from the provided
 * {@link FacesContext} argument. In effect, it 'flattens' the hierarchy of nested objects.
 * <p/>
 * The difference with {@link org.omnifaces.util.Faces} is that no one method of {@link org.omnifaces.util.FacesLocal} obtains the {@link FacesContext} from
 * the current thread by {@link FacesContext#getCurrentInstance()}. This job is up to the caller. This is more efficient
 * in situations where multiple utility methods needs to be called at the same time. Invoking
 * {@link FacesContext#getCurrentInstance()} is at its own an extremely cheap operation, however as it's to be obtained
 * as a {@link ThreadLocal} variable, it's during the call still blocking all other running threads for some nanoseconds
 * or so.
 * <p/>
 * Note that methods which are <strong>directly</strong> available on {@link FacesContext} instance itself, such as
 * {@link FacesContext#getExternalContext()}, {@link FacesContext#getViewRoot()},
 * {@link FacesContext#isValidationFailed()}, etc are not delegated by the this utility class, because it would design
 * technically not make any sense to delegate a single-depth method call like follows:
 * <pre>
 * ExternalContext externalContext = FacesLocal.getExternalContext(facesContext);
 * </pre>
 * <p/>
 * instead of just calling it directly like follows:
 * <pre>
 * ExternalContext externalContext = facesContext.getExternalContext();
 * </pre>
 * <p/>
 * <h3>Usage</h3>
 * <p/>
 * Some examples (for the full list, check the API documentation):
 * <pre>
 * FacesContext context = Faces.getContext();
 * User user = FacesLocal.getSessionAttribute(context, "user");
 * Item item = FacesLocal.evaluateExpressionGet(context, "#{item}");
 * String cookieValue = FacesLocal.getRequestCookie(context, "cookieName");
 * List&lt;Locale&gt; supportedLocales = FacesLocal.getSupportedLocales(context);
 * FacesLocal.invalidateSession(context);
 * FacesLocal.redirect(context, "login.xhtml");
 * </pre>
 *
 * @author Arjan Tijms
 * @author Bauke Scholtz
 * @since 1.6
 */
public final class FacesLocal {

    // Constants ------------------------------------------------------------------------------------------------------

    private static final String DEFAULT_MIME_TYPE = "application/octet-stream";
    private static final int DEFAULT_SENDFILE_BUFFER_SIZE = 10240;
    private static final String SENDFILE_HEADER = "%s;filename=\"%2$s\"; filename*=UTF-8''%2$s";
    private static final String ERROR_NO_VIEW = "There is no view.";
    private static final String[] FACELET_CONTEXT_KEYS = {FaceletContext.FACELET_CONTEXT_KEY, // Compiletime constant, may fail when compiled against EE6 and run on EE7.
                                                          "com.sun.faces.facelets.FACELET_CONTEXT", // JSF 2.0/2.1.
                                                          "javax.faces.FACELET_CONTEXT" // JSF 2.2.
    };

    // Constructors ---------------------------------------------------------------------------------------------------

    private FacesLocal() {
        // Hide constructor.
    }

    // JSF general ----------------------------------------------------------------------------------------------------


    /**
     * {@inheritDoc}
     *
     * @see org.omnifaces.util.Faces#getMapping()
     */
    public static String getMapping(FacesContext context) {
        ExternalContext externalContext = context.getExternalContext();

        if (externalContext.getRequestPathInfo() == null) {
            String path = externalContext.getRequestServletPath();
            return path.substring(path.lastIndexOf('.'));
        } else {
            return externalContext.getRequestServletPath();
        }
    }


    /**
     * {@inheritDoc}
     *
     * @see org.omnifaces.util.Faces#getContextAttribute(String)
     */
    @SuppressWarnings("unchecked")
    public static <T> T getContextAttribute(FacesContext context, String name) {
        return (T) context.getAttributes().get(name);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.omnifaces.util.Faces#setContextAttribute(String, Object)
     */
    public static void setContextAttribute(FacesContext context, String name, Object value) {
        context.getAttributes().put(name, value);
    }

    // JSF views ------------------------------------------------------------------------------------------------------


    /**
     * {@inheritDoc}
     *
     * @see org.omnifaces.util.Faces#getViewId()
     */
    public static String getViewId(FacesContext context) {
        UIViewRoot viewRoot = context.getViewRoot();
        return (viewRoot != null) ? viewRoot.getViewId() : null;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.omnifaces.util.Faces#normalizeViewId(String)
     */
    public static String normalizeViewId(FacesContext context, String path) {
        String mapping = getMapping(context);

        if (org.omnifaces.util.Faces.isPrefixMapping(mapping)) {
            if (path.startsWith(mapping)) {
                return path.substring(mapping.length());
            }
        } else if (path.endsWith(mapping)) {
            return path.substring(0, path.lastIndexOf('.')) + Utils.coalesce(getInitParameter(context, ViewHandler.FACELETS_SUFFIX_PARAM_NAME), ViewHandler.DEFAULT_FACELETS_SUFFIX);
        }

        return path;
    }


    /**
     * {@inheritDoc}
     *
     * @see org.omnifaces.util.Faces#navigate(String)
     */
    public static void navigate(FacesContext context, String outcome) {
        context.getApplication().getNavigationHandler().handleNavigation(context, null, outcome);
    }

    // HTTP request ---------------------------------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * @see org.omnifaces.util.Faces#getRequest()
     */
    public static HttpServletRequest getRequest(FacesContext context) {
        return (HttpServletRequest) context.getExternalContext().getRequest();
    }

    // HTTP response --------------------------------------------------------------------------------------------------


    /**
     * {@inheritDoc}
     *
     * @see org.omnifaces.util.Faces#setResponseStatus(int)
     */
    public static void setResponseStatus(FacesContext context, int status) {
        context.getExternalContext().setResponseStatus(status);
    }


    /**
     * {@inheritDoc}
     *
     * @see org.omnifaces.util.Faces#responseReset()
     */
    public static void responseReset(FacesContext context) {
        context.getExternalContext().responseReset();
    }

    // Servlet context ------------------------------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * @see org.omnifaces.util.Faces#getServletContext()
     */
    public static ServletContext getServletContext(FacesContext context) {
        return (ServletContext) context.getExternalContext().getContext();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.omnifaces.util.Faces#getInitParameterMap()
     */
    @SuppressWarnings("unchecked")
    public static Map<String, String> getInitParameterMap(FacesContext context) {
        return context.getExternalContext().getInitParameterMap();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.omnifaces.util.Faces#getInitParameter(String)
     */
    public static String getInitParameter(FacesContext context, String name) {
        return context.getExternalContext().getInitParameter(name);
    }

    // Request scope --------------------------------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * @see org.omnifaces.util.Faces#getRequestMap()
     */
    public static Map<String, Object> getRequestMap(FacesContext context) {
        return context.getExternalContext().getRequestMap();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.omnifaces.util.Faces#getRequestAttribute(String)
     */
    @SuppressWarnings("unchecked")
    public static <T> T getRequestAttribute(FacesContext context, String name) {
        return (T) getRequestMap(context).get(name);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.omnifaces.util.Faces#setRequestAttribute(String, Object)
     */
    public static void setRequestAttribute(FacesContext context, String name, Object value) {
        getRequestMap(context).put(name, value);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.omnifaces.util.Faces#removeRequestAttribute(String)
     */
    @SuppressWarnings("unchecked")
    public static <T> T removeRequestAttribute(FacesContext context, String name) {
        return (T) getRequestMap(context).remove(name);
    }

}