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

import javax.el.ELContext;
import javax.faces.application.Application;
import javax.faces.application.NavigationHandler;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.FacesContextWrapper;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.InputStream;
import java.util.Map;

/**
 * <p/>
 * Collection of utility methods for the JSF API that are mainly shortcuts for obtaining stuff from the thread local
 * {@link FacesContext}. In effects, it 'flattens' the hierarchy of nested objects. Do note that using the hierarchy is
 * actually a better software design practice, but can lead to verbose code.
 * <p/>
 * Next to those oneliner delegate calls, there are also some helpful methods which eliminates multiline boilerplate
 * code, such as getLocale() which returns sane fallback values, a more convenient
 * redirect(String, String...) which automatically prepends the context path when the path does not start with
 * <code>/</code> and offers support for URL encoding of request parameters supplied by varargs argument, and several
 * useful sendFile(File, boolean) methods which allows you to provide a {@link File}, <code>byte[]</code> or
 * {@link InputStream} as a download to the client.
 * <p/>
 * <h3>Usage</h3>
 * <p/>
 * Some examples:
 * <pre>
 * // Get a session attribute (no explicit cast necessary!).
 * User user = Faces.getSessionAttribute("user");
 * </pre>
 * <pre>
 * // Evaluate EL programmatically (no explicit cast necessary!).
 * Item item = Faces.evaluateExpressionGet("#{item}");
 * </pre>
 * <pre>
 * // Get a cookie value.
 * String cookieValue = Faces.getRequestCookie("cookieName");
 * </pre>
 * <pre>
 * // Get all supported locales with default locale as first item.
 * List&lt;Locale&gt; supportedLocales = Faces.getSupportedLocales();
 * </pre>
 * <pre>
 * // Check in e.g. preRenderView if session has been timed out.
 * if (Faces.hasSessionTimedOut()) {
 *     Messages.addGlobalWarn("Oops, you have been logged out because your session was been timed out!");
 * }
 * </pre>
 * <pre>
 * // Get value of &lt;f:metadata&gt;&lt;f:attribute name="foo"&gt; of different view without building it.
 * String foo = Faces.getMetadataAttribute("/other.xhtml", "foo");
 * </pre>
 * <pre>
 * // Send a redirect with parameters UTF-8 encoded in query string.
 * Faces.redirect("product.xhtml?id=%d&amp;name=%s", product.getId(), product.getName());
 * </pre>
 * <pre>
 * // Invalidate the session and send a redirect.
 * public void logout() throws IOException {
 *     Faces.invalidateSession();
 *     Faces.redirect("login.xhtml"); // Can by the way also be done by return "login?faces-redirect=true" if in action method.
 * }
 * </pre>
 * <pre>
 * // Provide a file as attachment.
 * public void download() throws IOException {
 *     Faces.sendFile(new File("/path/to/file.ext"), true);
 * }
 * </pre>
 * <p/>
 * <h3>FacesLocal</h3>
 * <p/>
 * Note that there's normally a minor overhead in obtaining the thread local {@link FacesContext}. In case client code
 * needs to call methods in this class multiple times it's expected that performance will be slightly better if instead
 * the {@link FacesContext} is obtained once and the required methods are called on that, although the difference is
 * practically negligible when used in modern server hardware.
 * <p/>
 * In such case, consider using {@link FacesLocal} instead. The difference with {@link org.omnifaces.util.Faces} is that no one method of
 * {@link FacesLocal} obtains the {@link FacesContext} from the current thread by
 * {@link FacesContext#getCurrentInstance()}. This job is up to the caller.
 *
 * @author Arjan Tijms
 * @author Bauke Scholtz
 * @see FacesLocal
 */
public final class Faces {

    // Constructors ---------------------------------------------------------------------------------------------------

    /**
     * Inner class so that the protected {@link FacesContext#setCurrentInstance(FacesContext)} method can be invoked.
     *
     * @author Bauke Scholtz
     */
    private abstract static class FacesContextSetter extends FacesContext {
        protected static void setCurrentInstance(FacesContext context) {
            FacesContext.setCurrentInstance(context);
        }
    }

    // JSF general ----------------------------------------------------------------------------------------------------

    private Faces() {
        // Hide constructor.
    }

    /**
     * Returns the current faces context.
     * <p/>
     * <i>Note that whenever you absolutely need this method to perform a general task, you might want to consider to
     * submit a feature request to OmniFaces in order to add a new utility method which performs exactly this general
     * task.</i>
     *
     * @return The current faces context.
     * @see FacesContext#getCurrentInstance()
     */
    public static FacesContext getContext() {
        return FacesContext.getCurrentInstance();
    }

    /**
     * Sets the given faces context as current instance. Use this if you have a custom {@link FacesContextWrapper}
     * which you'd like to (temporarily) use as the current instance of the faces context.
     *
     * @param context The faces context to be set as the current instance.
     * @since 1.3
     */
    public static void setContext(FacesContext context) {
        FacesContextSetter.setCurrentInstance(context);
    }

    /**
     * Returns the faces context that's stored in an ELContext.
     * <p/>
     * Note that this only works for an ELContext that is created in the context of JSF.
     *
     * @param elContext the EL context to obtain the faces context from.
     * @return the faces context that's stored in the given ELContext.
     * @since 1.2
     */
    public static FacesContext getContext(ELContext elContext) {
        return (FacesContext) elContext.getContext(FacesContext.class);
    }

    /**
     * Returns <code>true</code> when the current faces context is available (i.e. it is not <code>null</code>).
     *
     * @return <code>true</code> when the current faces context is available.
     * @since 2.0
     */
    public static boolean hasContext() {
        return getContext() != null;
    }

    /**
     * Returns the current external context.
     * <p/>
     * <i>Note that whenever you absolutely need this method to perform a general task, you might want to consider to
     * submit a feature request to OmniFaces in order to add a new utility method which performs exactly this general
     * task.</i>
     *
     * @return The current external context.
     * @see FacesContext#getExternalContext()
     */
    public static ExternalContext getExternalContext() {
        return getContext().getExternalContext();
    }


    /**
     * Determines and returns the faces servlet mapping used in the current request. If JSF is prefix mapped (e.g.
     * <code>/faces/*</code>), then this returns the whole path, with a leading slash (e.g. <code>/faces</code>). If JSF
     * is suffix mapped (e.g. <code>*.xhtml</code>), then this returns the whole extension (e.g. <code>.xhtml</code>).
     *
     * @return The faces servlet mapping (without the wildcard).
     */
    public static String getMapping() {
        return FacesLocal.getMapping(getContext());
    }


    /**
     * Returns whether the given faces servlet mapping is a prefix mapping. Use this method in preference to
     * isPrefixMapping() when you already have obtained the mapping from {@link #getMapping()} so that the
     * mapping won't be calculated twice.
     *
     * @param mapping The mapping to be tested.
     * @return <code>true</code> if the faces servlet mapping used in the current request is a prefix mapping, otherwise
     * <code>false</code>.
     * @throws NullPointerException When mapping is <code>null</code>.
     */
    public static boolean isPrefixMapping(String mapping) {
        return (mapping.charAt(0) == '/');
    }


    /**
     * Returns the Faces context attribute value associated with the given name.
     *
     * @param <T>  The expected return type.
     * @param name The Faces context attribute name.
     * @return The Faces context attribute value associated with the given name.
     * @throws ClassCastException When <code>T</code> is of wrong type.
     * @see FacesContext#getAttributes()
     * @since 1.3
     */
    public static <T> T getContextAttribute(String name) {
        return FacesLocal.getContextAttribute(getContext(), name);
    }

    /**
     * Sets the Faces context attribute value associated with the given name.
     *
     * @param name  The Faces context attribute name.
     * @param value The Faces context attribute value.
     * @see FacesContext#getAttributes()
     * @since 1.3
     */
    public static void setContextAttribute(String name, Object value) {
        FacesLocal.setContextAttribute(getContext(), name, value);
    }

    // JSF views ------------------------------------------------------------------------------------------------------


    /**
     * Returns the ID of the current view root, or <code>null</code> if there is no view.
     *
     * @return The ID of the current view root, or <code>null</code> if there is no view.
     * @see UIViewRoot#getViewId()
     */
    public static String getViewId() {
        return FacesLocal.getViewId(getContext());
    }


    /**
     * Normalize the given path as a valid view ID based on the current mapping, if necessary.
     * <ul>
     * <li>If the current mapping is a prefix mapping and the given path starts with it, then remove it.
     * <li>If the current mapping is a suffix mapping and the given path ends with it, then replace it with the default
     * Facelets suffix.
     * </ul>
     *
     * @param path The path to be normalized as a valid view ID based on the current mapping.
     * @return The path as a valid view ID.
     * @see #getMapping()
     * @see #isPrefixMapping(String)
     */
    public static String normalizeViewId(String path) {
        return FacesLocal.normalizeViewId(getContext(), path);
    }


    /**
     * Perform the JSF navigation to the given outcome.
     *
     * @param outcome The navigation outcome.
     * @see Application#getNavigationHandler()
     * @see NavigationHandler#handleNavigation(FacesContext, String, String)
     */
    public static void navigate(String outcome) {
        FacesLocal.navigate(getContext(), outcome);
    }

    // HTTP request ---------------------------------------------------------------------------------------------------

    /**
     * Returns the HTTP servlet request.
     * <p/>
     * <i>Note that whenever you absolutely need this method to perform a general task, you might want to consider to
     * submit a feature request to OmniFaces in order to add a new utility method which performs exactly this general
     * task.</i>
     *
     * @return The HTTP servlet request.
     * @see ExternalContext#getRequest()
     */
    public static HttpServletRequest getRequest() {
        return FacesLocal.getRequest(getContext());
    }

    // HTTP response --------------------------------------------------------------------------------------------------


    /**
     * Sets the HTTP response status code. You can use the constant field values of {@link HttpServletResponse} for
     * this. For example, <code>Faces.setResponseStatus(HttpServletResponse.SC_BAD_REQUEST)</code>.
     *
     * @param status The HTTP status code to be set on the current response.
     * @since 1.6
     */
    public static void setResponseStatus(int status) {
        FacesLocal.setResponseStatus(getContext(), status);
    }


    /**
     * Resets the current response. This will clear any headers which are been set and any data which is written to
     * the response buffer which isn't committed yet.
     *
     * @throws IllegalStateException When the response is already committed.
     * @see ExternalContext#responseReset()
     * @since 1.1
     */
    public static void responseReset() {
        FacesLocal.responseReset(getContext());
    }

    // Servlet context ------------------------------------------------------------------------------------------------

    /**
     * Returns the servlet context.
     * <p/>
     * <i>Note that whenever you absolutely need this method to perform a general task, you might want to consider to
     * submit a feature request to OmniFaces in order to add a new utility method which performs exactly this general
     * task.</i>
     *
     * @return the servlet context.
     * @see ExternalContext#getContext()
     */
    public static ServletContext getServletContext() {
        return FacesLocal.getServletContext(getContext());
    }

    /**
     * Returns the application initialization parameter map. This returns the parameter name-value pairs of all
     * <code>&lt;context-param&gt;</code> entries in in <code>web.xml</code>.
     *
     * @return The application initialization parameter map.
     * @see ExternalContext#getInitParameterMap()
     * @since 1.1
     */
    public static Map<String, String> getInitParameterMap() {
        return FacesLocal.getInitParameterMap(getContext());
    }

    /**
     * Returns the application initialization parameter. This returns the <code>&lt;param-value&gt;</code> of a
     * <code>&lt;context-param&gt;</code> in <code>web.xml</code> associated with the given
     * <code>&lt;param-name&gt;</code>.
     *
     * @param name The application initialization parameter name.
     * @return The application initialization parameter value associated with the given name, or <code>null</code> if
     * there is none.
     * @see ExternalContext#getInitParameter(String)
     * @since 1.1
     */
    public static String getInitParameter(String name) {
        return FacesLocal.getInitParameter(getContext(), name);
    }

    // Request scope --------------------------------------------------------------------------------------------------

    /**
     * Returns the request scope map.
     *
     * @return The request scope map.
     * @see ExternalContext#getRequestMap()
     */
    public static Map<String, Object> getRequestMap() {
        return FacesLocal.getRequestMap(getContext());
    }

    /**
     * Returns the request scope attribute value associated with the given name.
     *
     * @param <T>  The expected return type.
     * @param name The request scope attribute name.
     * @return The request scope attribute value associated with the given name.
     * @throws ClassCastException When <code>T</code> is of wrong type.
     * @see ExternalContext#getRequestMap()
     */
    public static <T> T getRequestAttribute(String name) {
        return FacesLocal.getRequestAttribute(getContext(), name);
    }

    /**
     * Sets the request scope attribute value associated with the given name.
     *
     * @param name  The request scope attribute name.
     * @param value The request scope attribute value.
     * @see ExternalContext#getRequestMap()
     */
    public static void setRequestAttribute(String name, Object value) {
        FacesLocal.setRequestAttribute(getContext(), name, value);
    }

    /**
     * Removes the request scope attribute value associated with the given name.
     *
     * @param <T>  The expected return type.
     * @param name The request scope attribute name.
     * @return The request scope attribute value previously associated with the given name, or <code>null</code> if
     * there is no such attribute.
     * @throws ClassCastException When <code>T</code> is of wrong type.
     * @see ExternalContext#getRequestMap()
     * @since 1.1
     */
    public static <T> T removeRequestAttribute(String name) {
        return FacesLocal.removeRequestAttribute(getContext(), name);
    }


}