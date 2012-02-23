/*
 * Copyright (c) OSGi Alliance (2012). All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.osgi.service.subsystem;

import java.io.InputStream;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.resource.Resource;

/**
 * A subsystem is a collection of resources constituting a logical, possibly
 * isolated, unit of functionality.
 * <p/>
 * A subsystem may be scoped or unscoped. Scoped subsystems are isolated by
 * implicit or explicit sharing policies. Unscoped subsystems are not isolated
 * and, therefore, have no sharing policy. There are three standard
 * {@link SubsystemConstants#SUBSYSTEM_TYPE types} of subsystems.
 * <ul>
 * <li>{@link SubsystemConstants#SUBSYSTEM_TYPE_APPLICATION Application} - An
 * implicitly scoped subsystem. Nothing is exported, and imports are computed
 * based on any unsatisfied content dependencies.</li>
 * <li>{@link SubsystemConstants#SUBSYSTEM_TYPE_COMPOSITE Composite} - An
 * explicitly scoped subsystem. The sharing policy is defined by metadata within
 * the subsystem archive.</li>
 * <li>{@link SubsystemConstants#SUBSYSTEM_TYPE_FEATURE Feature} - An unscoped
 * subsystem.</li>
 * </ul>
 * Conceptually, a subsystem may be thought of as existing in an isolated region
 * along with zero or more other subsystems. Each region has one and only one
 * scoped subsystem, which dictates the sharing policy. The region may, however,
 * have many unscoped subsystems. It is, therefore, possible to have shared
 * constituents across multiple subsystems within a region. Associated with each
 * region is a bundle whose context may be {@link #getBundleContext() retrieved}
 * from any subsystem within that region. This context may be used to monitor
 * activity occurring within the region.
 * <p/>
 * A subsystem may have {@link #getChildren() children} and, unless it's the
 * root subsystem, must have at least one {@link #getParents() parent}.
 * Subsystems become children of the subsystem in which they are installed.
 * Unscoped subsystems have more than one parent if they are installed in more
 * than one subsystem within the same region. A scoped subsystem always has only
 * one parent. The subsystem graph may be thought of as is an acyclic digraph
 * with one and only one source vertex, which is the root subsystem. The edges
 * have the child as the head and parent as the tail.
 * <p/>
 * A subsystem has several unique identifiers.
 * <ul>
 * <li>{@link #getLocation() Location} - An identifier specified by the client
 * as part of installation. It is guaranteed to be unique within the same
 * framework.</li>
 * <li>{@link #getSubsystemId() ID} - An identifier generated by the
 * implementation as part of installation. It is guaranteed to be unique within
 * the same framework.
 * <li>{@link #getSymbolicName() Symbolic Name}/{@link #getVersion() Version} -
 * The combination of symbolic name and version is guaranteed to be unique
 * within the same region. Although {@link #getType() type} is not formally part
 * of the identity, two subsystems with the same symbolic names and versions but
 * different types are not considered to be equal.</li>
 * </ul>
 * A subsystem has a well-defined {@link State life cycle}. Which stage a
 * subsystem is in may be obtained from the subsystem's {@link #getState()
 * state} and is dependent on which life cycle operation is currently active or
 * was last invoked. The following table summarizes the relationship between
 * life cycle operations and states.
 * <p/>
 * <table border="1">
 * <tr align="center">
 * <th>Operation</th>
 * <th>From State</th>
 * <th>To State</th>
 * </tr>
 * <tr align="center">
 * <td>{@link #install(String, InputStream) Install}</td>
 * <td></td>
 * <td>{@link State#INSTALLING INSTALLING}, {@link State#INSTALL_FAILED
 * INSTALL_FAILED}, {@link State#INSTALLED INSTALLED}</td>
 * </tr>
 * <tr align="center">
 * <td>{@link #start() Start}</td>
 * <td>{@link State#INSTALLED INSTALLED}, {@link State#RESOLVED RESOLVED}</td>
 * <td>{@link State#INSTALLED INSTALLED}, {@link State#RESOLVING RESOLVING},
 * {@link State#RESOLVED RESOLVED}, {@link State#STARTING STARTING},
 * {@link State#ACTIVE ACTIVE}</td>
 * </tr>
 * <tr align="center">
 * <td>{@link #stop() Stop}</td>
 * <td>{@link State#ACTIVE ACTIVE}</td>
 * <td>{@link State#RESOLVED RESOLVED}, {@link State#STOPPING STOPPING}</td>
 * </tr>
 * <tr align="center">
 * <td>{@link #uninstall() Uninstall}</td>
 * <td>{@link State#INSTALLED INSTALLED}, {@link State#RESOLVED RESOLVED},
 * {@link State#ACTIVE ACTIVE}</td>
 * <td>{@link State#UNINSTALLING UNINSTALLING}, {@link State#UNINSTALLED
 * UNINSTALLED}</td>
 * </tr>
 * </table>
 * <p/>
 * A subsystem archive is a ZIP file having an ESA extension and containing
 * metadata describing the subsystem. The form of the metadata may be a
 * subsystem or deployment manifest, as well as any content resource files. The
 * manifests are optional and will be computed if not present. The subsystem
 * manifest headers may be {@link #getSubsystemHeaders(Locale) retrieved} in raw
 * or localized formats. There are five standard
 * {@link IdentityNamespace#CAPABILITY_TYPE_ATTRIBUTE types} of resources that
 * may be included in a subsystem.
 * <ul>
 * <li>{@link IdentityNamespace#TYPE_BUNDLE Bundle} - A bundle that is not a
 * fragment.</li>
 * <li>{@link IdentityNamespace#TYPE_FRAGMENT Fragment} - A fragment bundle.</li>
 * <li>{@link SubsystemConstants#SUBSYSTEM_TYPE_APPLICATION Application
 * Subsystem} - An application subsystem defined by this specification.</li>
 * <li>{@link SubsystemConstants#SUBSYSTEM_TYPE_COMPOSITE Composite Subsystem} -
 * A composite subsystem defined by this specification.</li>
 * <li>{@link SubsystemConstants#SUBSYSTEM_TYPE_FEATURE Feature Subsystem} - A
 * feature subsystem defined by this specification.</li>
 * </ul>
 * Resources contained by a subsystem are called {@link #getConstituents()
 * constituents}. There are several ways a resource may become a constituent of
 * a subsystem, at least some of which are listed below.
 * <p/>
 * <ul>
 * <li>A resource was listed as part of the subsystem's content.</li>
 * <li>A subsystem resource is a child of the subsystem.</li>
 * <li>The subsystem has a provision policy of accept dependencies.</li>
 * <li>A bundle resource was installed using the region bundle context.</li>
 * <li>A bundle resource was installed using the bundle context of another
 * resource contained by the subsystem.</li>
 * </ul>
 * In addition to invoking one of the install methods, a subsystem instance may
 * be obtained through the service registry. Every installed subsystem has a
 * corresponding service registration. A subsystem service has the following
 * properties.
 * <p/>
 * <ul>
 * <li>{@link SubsystemConstants#SUBSYSTEM_ID_PROPERTY ID} - Matches the ID of
 * the subsystem.</li>
 * <li>{@link SubsystemConstants#SUBSYSTEM_SYMBOLICNAME_PROPERTY Symbolic Name}
 * - Matches the symbolic name of the subsystem.</li>
 * <li>{@link SubsystemConstants#SUBSYSTEM_VERSION_PROPERTY Version} - Matches
 * the version of the subsystem.</li>
 * <li>{@link SubsystemConstants#SUBSYSTEM_TYPE_PROPERTY Type} - Matches the
 * type of the subsystem.</li>
 * <li>{@link SubsystemConstants#SUBSYSTEM_STATE_PROPERTY State} - Matches the
 * state of the subsystem.</li>
 * </ul>
 * Because a subsystem must be used to install other subsystems, a root
 * subsystem is provided as a starting point and has the following
 * characteristics. The root subsystem may only be obtained as a service.
 * <p/>
 * <ul>
 * <li>The ID is {@code 0}.</li>
 * <li>The symbolic name is {@code org.osgi.service.subsystem.root}.</li>
 * <li>The version matches this specification's version.</li>
 * <li>It has no parents.</li>
 * <li>All existing bundles, including the system and subsystems implementation
 * bundles, become constituents.</li>
 * <li>The type is {@code osgi.subsystem.application} with no imports.</li>
 * <li>The provision policy is {@code acceptDependencies}.</li>
 * </ul>
 * 
 * @ThreadSafe
 * @noimplement
 */
public interface Subsystem {
	/**
	 * An enumeration of the possible states of a subsystem.
	 * <p/>
	 * These states are a reflection of what constituent resources are permitted
	 * to do, not an aggregation of resource states. 
	 */
	public static enum State {
		/**
		 * The subsystem is in the process of installing.
		 * <p/>
		 * A subsystem is in the INSTALLING state when the {@link Subsystem#
		 * install(String, InputStream) install} method of its parent is active,
		 * and attempts are being made to install its content resources. If the
		 * install method completes without exception, then the subsystem has
		 * successfully installed and must move to the INSTALLED state.
		 * Otherwise, the subsystem has failed to install and must move to the
		 * INSTALL_FAILED state.
		 */
		INSTALLING,
		/**
		 * The subsystem is installed but not yet resolved.
		 * <p/>
		 * A subsystem is in the INSTALLED state when it has been installed in
		 * a parent subsystem but is not or cannot be resolved. This state is
		 * visible if the dependencies of the subsystem's content resources
		 * cannot be resolved.
		 */
		INSTALLED,
		/**
		 * The subsystem failed to install.
		 * <p/>
		 * A subsystem is in the INSTALL_FAILED state when an unrecoverable
		 * error occurred during installation. The subsystem is in an unusable
		 * state but references to the subsystem object may still be available
		 * and used for introspection.
		 */
		INSTALL_FAILED,
		/**
		 * The subsystem is in the process of resolving.
		 * <p/>
		 * A subsystem is in the RESOLVING state when the {@link Subsystem#
		 * start() start} method is active, and attempts are being made to
		 * resolve its content resources. If the resolve method completes
		 * without exception, then the subsystem has successfully resolved and
		 * must move to the RESOLVED state. Otherwise, the subsystem has failed
		 * to resolve and must move to the INSTALLED state.
		 */
		RESOLVING,
		/**
		 * The subsystem is resolved and able to be started.
		 * <p/>
		 * A subsystem is in the RESOLVED state when all of its content
		 * resources are resolved. Note that the subsystem is not active yet.
		 */
		RESOLVED,
		/**
		 * The subsystem is in the process of starting.
		 * <p/>
		 * A subsystem is in the STARTING state when its {@link Subsystem#
		 * start() start} method is active, and attempts are being made to start
		 * its content and dependencies. If the start method completes
		 * without exception, then the subsystem has successfully started and
		 * must move to the ACTIVE state. Otherwise, the subsystem has failed to
		 * start and must move to the RESOLVED state.
		 */
		STARTING,
		/**
		 * The subsystem is now running.
		 * <p/>
		 * A subsystem is in the ACTIVE state when its content and dependencies
		 * have been successfully started and activated.
		 */
		ACTIVE,
		/**
		 * The subsystem is in the process of stopping.
		 * <p/>
		 * A subsystem is in the STOPPING state when its {@link Subsystem#stop()
		 * stop} method is active, and attempts are being made to stop its
		 * content and dependencies. When the stop method completes, the
		 * subsystem is stopped and must move to the RESOLVED state.
		 */
		STOPPING,
		/**
		 * The subsystem is in the process of uninstalling.
		 * <p/>
		 * A subsystem is in the UNINSTALLING state when its {@link Subsystem#
		 * uninstall() uninstall} method is active, and attempts are being made
		 * to uninstall its constituent and dependencies. When the
		 * uninstall method completes, the subsystem is uninstalled and must
		 * move to the UNINSTALLED state.
		 */
		UNINSTALLING,
		/**
		 * The subsystem is uninstalled and may not be used.
		 * <p/>
		 * The UNINSTALLED state is only visible after a subsystem's constituent
		 * and dependencies are uninstalled. The subsystem is in an
		 * unusable state but references to the subsystem object may still be
		 * available and used for introspection.
		 */
		UNINSTALLED
	}
	
	/**
	 * Returns the bundle context of the region within which this subsystem
	 * resides.
	 * <p/>
	 * The bundle context offers the same perspective of any resource contained
	 * by a subsystem within the region. It may be used, for example, to monitor
	 * events internal to the region as well as external events visible to the
	 * region. All subsystems within the same region have the same bundle
	 * context. If this subsystem is in a state where the bundle context would
	 * be invalid, null is returned.
	 * 
	 * @return The bundle context of the region within which this subsystem
	 *         resides or null if this subsystem's state is in {{@link
	 *         State#INSTALL_FAILED INSTALL_FAILED}, {@link State#UNINSTALLED
	 *         UNINSTALLED}}.
	 * @throws SecurityException If the caller does not have the appropriate 
	 *         {@link SubsystemPermission}[this,CONTEXT], and the runtime
	 *         supports permissions.
	 */
	public BundleContext getBundleContext();
	
	/**
	 * Returns the child subsystems of this subsystem.
	 * <p/>
	 * The returned collection is an immutable snapshot of all subsystems that
	 * are installed in this subsystem. The collection will be empty if no
	 * subsystems are installed in this subsystem.
	 * 
	 * @return The child subsystems of this subsystem.
	 * @throws IllegalStateException If this subsystem's state is in
	 *         {{@link State#INSTALL_FAILED INSTALL_FAILED}, {@link
	 *         State#UNINSTALLED UNINSTALLED}}.
	 */
	public Collection<Subsystem> getChildren();
	
	/**
	 * Returns the headers for this subsystem's subsystem manifest.
	 * <p/>
	 * The returned map is unmodifiable. Each map key is a header name, and each
	 * map value is the corresponding header value. Because header names are
	 * case-insensitive, the methods of the map must treat them in a
	 * case-insensitive manner. If the header name is not found, null is
	 * returned. Both original and derived headers will be included.
	 * <p/>
	 * The header values are translated according to the specified locale. If
	 * the specified locale is null or not supported, the raw values are
	 * returned. If the translation for a particular header is not found, the
	 * raw value is returned.
	 * <p/>
	 * This method must continue to return the headers while this subsystem is
	 * in the {@link State#INSTALL_FAILED INSTALL_FAILED} or {@link
	 * State#UNINSTALLED UNINSTALLED} states.
	 * 
	 * @param locale The locale for which translations are desired.
	 * @return The headers for this subsystem's subsystem manifest.
	 * @throws SecurityException If the caller does not have the appropriate 
	 *         {@link SubsystemPermission}[this,METADATA], and the runtime
	 *         supports permissions.
	 */
	public Map<String, String> getSubsystemHeaders(Locale locale);
	
	/**
	 * Returns the location identifier of this subsystem.
	 * <p/>
	 * The location identifier is the {@code location} that was passed to the
	 * {@link #install(String, InputStream) install} method of the {@link
	 * #getParents() parent} subsystem. It is unique within the framework.
	 * <p/>
	 * This method must continue to return this subsystem's headers while this
	 * subsystem is in the {@link State#INSTALL_FAILED INSTALL_FAILED} or {@link
	 * State#UNINSTALLED UNINSTALLED} states.
	 * 
	 * @return The location identifier of this subsystem.
	 * @throws SecurityException If the caller does not have the appropriate 
	 *         {@link SubsystemPermission}[this,METADATA], and the runtime
	 *         supports permissions.
	 */
	public String getLocation();
	
	/**
	 * Returns the parent subsystems of this subsystem.
	 * <p/>
	 * The returned collection is an immutable snapshot of all subsystems in
	 * which this subsystem is installed. The collection will be empty for the
	 * root subsystem; otherwise, it will contain at least one parent. Scoped
	 * subsystems always have only one parent. Unscoped subsystems may have
	 * multiple parents.
	 * 
	 * @return The parent subsystems of this subsystem.
	 * @throws IllegalStateException If this subsystem's state is in {{@link
	 *         State#INSTALL_FAILED INSTALL_FAILED}, {@link State#UNINSTALLED
	 *         UNINSTALLED}}.
	 */
	public Collection<Subsystem> getParents();
	
	/**
	 * Returns the constituent resources of this subsystem.
	 * <p/>
	 * The returned collection is an immutable snapshot of the constituent
	 * resources of this subsystem. If this subsystem has no constituents,
	 * the collection will be empty.
	 * 
	 * @return The constituent resources of this subsystem.
	 * @throws IllegalStateException If this subsystem's state is in {{@link
	 *         State#INSTALL_FAILED INSTALL_FAILED}, {@link State#UNINSTALLED
	 *         UNINSTALLED}}.
	 */
	public Collection<Resource> getConstituents();
	
	/**
	 * Returns the current state of this subsystem.
	 * <p/>
	 * This method must continue to return this subsystem's state while this
	 * subsystem is in the {@link State#INSTALL_FAILED INSTALL_FAILED} or {@link
	 * State#UNINSTALLED UNINSTALLED} states.
	 * 
	 * @return The current state of this subsystem.
	 */
	public State getState();
	
	/**
	 * Returns the identifier of this subsystem.
	 * <p/>
	 * The identifier is a monotonically increasing, non-negative integer
	 * automatically generated at installation time and guaranteed to be unique
	 * within the framework. The identifier of the root subsystem is zero.
	 * <p/>
	 * This method must continue to return this subsystem's identifier while
	 * this subsystem is in the {@link State#INSTALL_FAILED INSTALL_FAILED} or
	 * {@link State#UNINSTALLED UNINSTALLED} states.
	 * 
	 * @return The identifier of this subsystem.
	 */
	public long getSubsystemId();
	
	/**
	 * Returns the symbolic name of this subsystem.
	 * <p/>
	 * The subsystem symbolic name conforms to the same grammar rules as the
	 * bundle symbolic name and is derived from one of the following, in order.
	 * <ul>
	 * 		<li>The value of the {@link SubsystemConstants#SUBSYSTEM_CONTENT
	 *          Subsystem-Content} header, if specified.
	 * 		</li>
	 * 		<li>The subsystem URI if passed as the {@code location} along with
	 *          the {@code content} to the {@link #install(String, InputStream)
	 *          install} method.
	 *      </li>
	 * 		<li>Optionally generated in an implementation specific way.
	 * 		</li>
	 * </ul>
	 * The combination of symbolic name and {@link #getVersion() version} is
	 * unique within a region. The symbolic name of the root subsystem is {@code
	 * org.osgi.service.subsystem.root}.
	 * <p/>
	 * This method must continue to return this subsystem's symbolic name while
	 * this subsystem is in the {@link State#INSTALL_FAILED INSTALL_FAILED} or
	 * {@link State#UNINSTALLED UNINSTALLED} states.
	 * 
	 * @return The symbolic name of this subsystem.
	 */
	public String getSymbolicName();
	
	/**
	 * Returns the {@link SubsystemConstants#SUBSYSTEM_TYPE type} of this
	 * subsystem.
	 * <p/>
	 * This method must continue to return this subsystem's type while this
	 * subsystem is in the {@link State#INSTALL_FAILED INSTALL_FAILED} or
	 * {@link State#UNINSTALLED UNINSTALLED} states.
	 * 
	 * @return The type of this subsystem.
	 */
	public String getType();
	
	/**
	 * Returns the {@link SubsystemConstants#SUBSYSTEM_VERSION version} of this
	 * subsystem.
	 * <p/>
	 * The subsystem version conforms to the same grammar rules as the bundle
	 * version and is derived from one of the following, in order.
	 * <ul>
	 * 		<li>The value of the {@code Subsystem-Version} header, if specified.
	 * 		</li>
	 * 		<li>The subsystem URI if passed as the {@code location} along with
	 *          the {@code content} to the {@link #install(String, InputStream)
	 *          install} method.
	 *      </li>
	 * 		<li>Defaults to {@code 0.0.0}.
	 * 		</li>
	 * </ul>
	 * The combination of {@link #getSymbolicName() symbolic name} and version
	 * is unique within a region. The version of the root subsystem matches this
	 * specification's version.
	 * <p/>
	 * This method must continue to return this subsystem's version while this
	 * subsystem is in the {@link State#INSTALL_FAILED INSTALL_FAILED} or {@link
	 * State#UNINSTALLED UNINSTALLED} states.
	 * 
	 * @return The version of this subsystem.
	 */
	public Version getVersion();
	
	/**
	 * Installs a subsystem from the specified {@code location} identifier.
	 * <p/>
	 * This method performs the same function as calling {@link
	 * #install(String, InputStream)} with the specified {@code location}
	 * identifier and {@code null} as the {@code content}.
	 * 
	 * @param location - The location identifier of the subsystem to install.
	 * @return The installed subsystem.
	 * @throws IllegalStateException If this subsystem's state is in {{@link
	 * State#INSTALLING INSTALLING}, {@link State#INSTALL_FAILED INSTALL_FAILED}
	 * , {@link State#UNINSTALLING UNINSTALLING}, {@link State#UNINSTALLED
	 * UNINSTALLED}}.
	 * @throws SubsystemException If the installation failed.
	 * @throws SecurityException If the caller does not have the appropriate 
	 *         {@link SubsystemPermission}[installed subsystem,LIFECYCLE], and
	 *         the runtime supports permissions.
	 * @see #install(String, InputStream)
	 */
	public Subsystem install(String location) throws SubsystemException;
	
	/**
	 * Installs a subsystem from the specified {@code content}.
	 * <p/>
	 * The specified {@code location} will be used as an identifier of the
	 * subsystem. Every installed subsystem is uniquely identified by its
	 * location, which is typically in the form of a URI. If the specified
	 * {@code location} conforms to the {@code subsystem-uri} grammar, the
	 * required symbolic name and optional version information will be used as
	 * default values.
	 * <p/>
	 * If the specified {@code content} is null, a new input stream must be
	 * created from which to read the subsystem by interpreting, in an
	 * implementation dependent manner, the specified {@code location}.
	 * <p/>
	 * A subsystem installation must be persistent. That is, an installed
	 * subsystem must remain installed across Framework and VM restarts.
	 * <p/>
	 * All references to changing the state of this subsystem include both
	 * changing the state of the subsystem object as well as the state property
	 * of the subsystem service registration.
	 * <p/>
	 * Implementations should be sensitive to the potential for long running
	 * operations and periodically check the current thread for interruption. An
	 * interrupted thread should result in a SubsystemException with an
	 * InterruptedException as the cause and be treated as an installation
	 * failure.
	 * <p/>
	 * All installation failure flows include the following, in order.
	 * <ol>
	 * <li>Uninstall all resources installed as part of this operation.</li>
	 * <li>Change the state to INSTALL_FAILED.</li>
	 * <li>Unregister the subsystem service.</li>
	 * <li>Uninstall the region context bundle.</li>
	 * <li>Throw a SubsystemException with the specified cause.</li>
	 * </ol>
	 * The following steps are required to install a subsystem.
	 * <ol>
	 * <li>If an installed subsystem with the specified {@code location}
	 * identifier already exists, return the installed subsystem.</li>
	 * <li>Read the specified {@code content} in order to determine the symbolic
	 * name, version, and type of the installing subsystem. If an error occurs
	 * while reading the content, an installation failure results.</li>
	 * <li>If an installed subsystem with the same symbolic name and version
	 * already exists within this subsystem's region, complete the installation
	 * with one of the following.
	 * <ul>
	 * <li>If the installing and installed subsystems' types are not equal, an
	 * installation failure results.</li>
	 * <li>If the installing and installed subsystems' types are equal, and the
	 * installed subsystem is already a child of this subsystem, return the
	 * installed subsystem.</li>
	 * <li>If the installing and installed subsystems' types are equal, and the
	 * installed subsystem is not already a child of this subsystem, add the
	 * installed subsystem as a child of this subsystem, increment the installed
	 * subsystem's reference count by one, and return the installed subsystem.</li>
	 * </ul>
	 * </li>
	 * <li>Create a new subsystem based on the specified {@code location} and
	 * {@code content}.</li>
	 * <li>If the subsystem is scoped, install and activate a new region context
	 * bundle.</li>
	 * <li>Change the state to INSTALLING and register a new subsystem service.</li>
	 * <li>Discover the subsystem's content resources. If any mandatory resource
	 * is missing, an installation failure results.</li>
	 * <li>Discover the dependencies required by the content resources. If any
	 * mandatory dependency is missing, an installation failure results.</li>
	 * <li>{@link ResolverHook Disable} runtime resolution for the resources.</li>
	 * <li>For each resource, increment the reference count by one. If the
	 * reference count is one, install the resource. All dependencies must be
	 * installed before any content resource. If an error occurs while
	 * installing a resource, an install failure results with that error as the
	 * cause.</li>
	 * <li>If the subsystem is scoped, enable the import sharing policy.</li>
	 * <li>Enable runtime resolution for the resources.</li>
	 * <li>Change the state of the subsystem to INSTALLED.</li>
	 * <li>Return the new subsystem.</li>
	 * </ol>
	 * 
	 * @param location - The location identifier of the subsystem to be
	 *        installed.
	 * @param content - The input stream from which this subsystem will be read
	 *        or null to indicate the input stream must be created from the
	 *        specified location identifier. The input stream will always be
	 *        closed when this method completes, even if an exception is thrown.
	 * @return The installed subsystem.
	 * @throws IllegalStateException If this subsystem's state is in {INSTALLING
	 *         , INSTALL_FAILED, UNINSTALLING, UNINSTALLED}.
	 * @throws SubsystemException If the installation failed.
	 * @throws SecurityException If the caller does not have the appropriate
	 *         SubsystemPermission[installed subsystem,LIFECYCLE], and the
	 *         runtime supports permissions.
	 */
	public Subsystem install(String location, InputStream content) throws SubsystemException;
	
	/**
	 * Starts this subsystem.
	 * <p/>
	 * The following table shows which actions are associated with each state.
	 * An action of Wait means this method will block until a state transition
	 * occurs, upon which the new state will be evaluated in order to
	 * determine how to proceed. An action of Return means this method returns
	 * immediately without taking any other action.
	 * <p/>
	 * <table border="1">
	 * 		<tr>
	 * 			<th>State</td>
	 * 			<th>Action</td>
	 * 		</tr>
	 * 		<tr align="center">
	 * 			<td>INSTALLING</td>
	 * 			<td>Wait</td>
	 * 		</tr>
	 * 		<tr align="center">
	 * 			<td>INSTALLED</td>
	 * 			<td>Resolve, Start</td>
	 * 		</tr>
	 * 		<tr align="center">
	 * 			<td>INSTALL_FAILED</td>
	 * 			<td>IllegalStateException</td>
	 * 		</tr>
	 * 		<tr align="center">
	 * 			<td>RESOLVING</td>
	 * 			<td>Wait</td>
	 * 		</tr>
	 * 		<tr align="center">
	 * 			<td>RESOLVED</td>
	 * 			<td>If this subsystem is in the process of being<br/>
	 *              started, Wait. Otherwise, Uninstall.</td>
	 * 		</tr>
	 * 		<tr align="center">
	 * 			<td>STARTING</td>
	 * 			<td>Wait</td>
	 * 		</tr>
	 * 		<tr align="center">
	 * 			<td>ACTIVE</td>
	 * 			<td>Return</td>
	 * 		</tr>
	 * 		<tr align="center">
	 * 			<td>STOPPING</td>
	 * 			<td>Wait</td>
	 * 		</tr>
	 * 		<tr align="center">
	 * 			<td>UNINSTALLING</td>
	 * 			<td>IllegalStateException</td>
	 * 		</tr>
	 * 		<tr align="center">
	 * 			<td>UNINSTALLED</td>
	 * 			<td>IllegalStateException</td>
	 * 		</tr>
	 * </table>
	 * <p/>
	 * All references to changing the state of this subsystem include both
	 * changing the state of the subsystem object as well as the state property
	 * of the subsystem service registration.
	 * <p/>
	 * Implementations should be sensitive to the potential for long running
	 * operations and periodically check the current thread for interruption. An
	 * interrupted thread should be treated as a start failure with an
	 * InterruptedException as the cause.
	 * <p/>
	 * All start failure flows include the following, in order.
	 * <ol>
	 * 		<li>Stop all resources that were started as part of this operation.
	 *      </li>
	 *      <li>Disable the export sharing policy.
	 *      </li>
	 *      <li>Change the state to either INSTALLED or RESOLVED.
	 * 		</li>
	 * 		<li>Throw a SubsystemException with the specified cause.
	 *      </li>
	 * </ol>
	 * <p/>
	 * A subsystem must be persistently started. That is, a started subsystem
	 * must be restarted across Framework and VM restarts, even if a start
	 * failure occurs.
	 * <p/>
	 * The following steps are required to start this subsystem.
	 * <ol>
	 * 		<li>If this subsystem is in the RESOLVED state, proceed to step 5.
	 * 		</li>
	 *      <li>Change the state to RESOLVING.
	 *      </li>
	 *      <li>Resolve the content resources. A resolution failure results in
	 *          a start failure with a state of INSTALLED.
	 *      </li>
	 *      <li>Change the state to RESOLVED.
	 *      </li>
	 *      <li>If this subsystem is scoped, enable the export sharing policy.
	 * 		</li>
	 * 		<li>Change the state to STARTING.
	 *      </li>
	 *      <li>For each eligible resource, increment the activation count by
	 *          one. If the activation count is one, start the resource. All
	 *          dependencies must be started before any content
	 *          resource, and content resources must be started according to the
	 *          specified {@link SubsystemConstants#START_LEVEL_DIRECTIVE start
	 *          order}. If an error occurs while starting a resource, a start
	 *          failure results with that error as the cause.
	 *      </li>
	 *      <li>Change the state to ACTIVE.
	 * 		</li>
	 * </ol>
	 * <p/> 
	 * @throws SubsystemException If this subsystem fails to start. 
	 * @throws IllegalStateException If this subsystem's state is in
	 *         {INSTALL_FAILED, UNINSTALLING, or UNINSTALLED}, or if the state
	 *         of at least one of this subsystem's parents is not in {STARTING,
	 *         ACTIVE}.
	 * @throws SecurityException If the caller does not have the appropriate 
	 *         SubsystemPermission[this,EXECUTE], and the runtime supports 
	 *         permissions.
	 */
	public void start() throws SubsystemException;
	
	/**
	 * Stops this subsystem.
	 * <p/>
	 * The following table shows which actions are associated with each state.
	 * An action of Wait means this method will block until a state transition
	 * occurs, upon which the new state will be evaluated in order to
	 * determine how to proceed. An action of Return means this method returns
	 * immediately without taking any other action.
	 * <p/>
	 * <table border="1">
	 * 		<tr>
	 * 			<th>State</th>
	 * 			<th>Action</th>
	 * 		</tr>
	 * 		<tr align="center">
	 * 			<td>INSTALLING</td>
	 * 			<td>Wait</td>
	 * 		</tr>
	 * 		<tr align="center">
	 * 			<td>INSTALLED</td>
	 * 			<td>Return</td>
	 * 		</tr>
	 * 		<tr align="center">
	 * 			<td>INSTALL_FAILED</td>
	 * 			<td>IllegalStateException</td>
	 * 		</tr>
	 * 		<tr align="center">
	 * 			<td>RESOLVING</td>
	 * 			<td>Wait</td>
	 * 		</tr>
	 * 		<tr align="center">
	 * 			<td>RESOLVED</td>
	 * 			<td>If this subsystem is in the process of being<br/>
	 *              started, Wait. Otherwise, Return.</td>
	 * 		</tr>
	 * 		<tr align="center">
	 * 			<td>STARTING</td>
	 * 			<td>Wait</td>
	 * 		</tr>
	 * 		<tr align="center">
	 * 			<td>ACTIVE</td>
	 * 			<td>Stop</td>
	 * 		</tr>
	 * 		<tr align="center">
	 * 			<td>STOPPING</td>
	 * 			<td>Wait</td>
	 * 		</tr>
	 * 		<tr align="center">
	 * 			<td>UNINSTALLING</td>
	 * 			<td>IllegalStateException</td>
	 * 		</tr>
	 * 		<tr align="center">
	 * 			<td>UNINSTALLED</td>
	 * 			<td>IllegalStateException</td>
	 * 		</tr>
	 * </table>
	 * <p/>
	 * Implementations should be sensitive to the potential for long running
	 * operations and periodically check the current thread for interruption, in
	 * which case a SubsystemException with an InterruptedException as the cause
	 * should be thrown. If an interruption occurs while waiting, this method
	 * should terminate immediately. Once the transition to the STOPPING
	 * state has occurred, however, this method must not terminate due to an
	 * interruption until the stop process has completed.
	 * <p/>
	 * A subsystem must be persistently stopped. That is, a stopped subsystem
	 * must remain stopped across Framework and VM restarts.
	 * <p/>
	 * All references to changing the state of this subsystem include both
	 * changing the state of the subsystem object as well as the state property
	 * of the subsystem service registration.
	 * <p/>
	 * The following steps are required to stop this subsystem.
	 * <ol>
	 * 		<li>Change the state to STOPPING.
	 * 		</li>
	 * 		<li>For each eligible resource, decrement the activation count by
	 *          one. If the activation count is zero, stop the resource. All
	 *          content resources must be stopped before any dependencies,
	 *          and content resources must be stopped in reverse
	 *          {@link SubsystemConstants#START_LEVEL_DIRECTIVE start order}. If
	 *          an error occurs while stopping a resource, a stop failure
	 *          results with that error as the cause.
	 *      </li>
	 *      <li>Change the state to RESOLVED.
	 *      </li>
	 * </ol>
	 * With regard to error handling, once this subsystem has transitioned to
	 * the STOPPING state, every part of each step above must be attempted.
	 * Errors subsequent to the first should be logged. Once the stop process
	 * has completed, a SubsystemException must be thrown with the initial error
	 * as the specified cause.
	 * <p/>
	 * @throws SubsystemException If this subsystem fails to stop cleanly.
	 * @throws IllegalStateException If this subsystem's state is in
	 *         {INSTALL_FAILED, UNINSTALLING, or UNINSTALLED}.
	 * @throws SecurityException If the caller does not have the appropriate 
	 *         SubsystemPermission[this,EXECUTE], and the runtime supports 
	 *         permissions.
	 */
	public void stop() throws SubsystemException;
	
	/**
	 * Uninstalls this subsystem.
	 * <p/>
	 * The following table shows which actions are associated with each state.
	 * An action of Wait means this method will block until a state transition
	 * occurs, upon which the new state will be evaluated in order to
	 * determine how to proceed. An action of Return means this method returns
	 * immediately without taking any other action.
	 * <p/>
	 * <table border="1">
	 * 		<tr>
	 * 			<th>State</td>
	 * 			<th>Action</td>
	 * 		</tr>
	 * 		<tr align="center">
	 * 			<td>INSTALLING</td>
	 * 			<td>Wait</td>
	 * 		</tr>
	 * 		<tr align="center">
	 * 			<td>INSTALLED</td>
	 * 			<td>Uninstall</td>
	 * 		</tr>
	 * 		<tr align="center">
	 * 			<td>INSTALL_FAILED</td>
	 * 			<td>IllegalStateException</td>
	 * 		</tr>
	 * 		<tr align="center">
	 * 			<td>RESOLVING</td>
	 * 			<td>Wait</td>
	 * 		</tr>
	 * 		<tr align="center">
	 * 			<td>RESOLVED</td>
	 * 			<td>If this subsystem is in the process of being<br/>
	 *              started, Wait. Otherwise, Uninstall.</td>
	 * 		</tr>
	 * 		<tr align="center">
	 * 			<td>STARTING</td>
	 * 			<td>Wait</td>
	 * 		</tr>
	 * 		<tr align="center">
	 * 			<td>ACTIVE</td>
	 * 			<td>Stop, Uninstall</td>
	 * 		</tr>
	 * 		<tr align="center">
	 * 			<td>STOPPING</td>
	 * 			<td>Wait</td>
	 * 		</tr>
	 * 		<tr align="center">
	 * 			<td>UNINSTALLING</td>
	 * 			<td>Wait</td>
	 * 		</tr>
	 * 		<tr align="center">
	 * 			<td>UNINSTALLED</td>
	 * 			<td>Return</td>
	 * 		</tr>
	 * </table>
	 * <p/>
	 * Implementations should be sensitive to the potential for long running
	 * operations and periodically check the current thread for interruption, in
	 * which case a SubsystemException with an InterruptedException as the cause
	 * should be thrown. If an interruption occurs while waiting, this method
	 * should terminate immediately. Once the transition to the UNINSTALLING
	 * state has occurred, however, this method must not terminate due to an
	 * interruption until the uninstall process has completed.
	 * <p/>
	 * All references to changing the state of this subsystem include both
	 * changing the state of the subsystem object as well as the state property
	 * of the subsystem service registration.
	 * <p/>
	 * The following steps are required to uninstall this subsystem.
	 * <ol>
	 * 		<li>Change the state to UNINSTALLING.
	 * 		</li>
	 * 		<li>For each resource, decrement the reference count by one. If the
	 * 			reference count is zero, uninstall the resource. All content
	 * 			resources must be uninstalled before any dependencies. If
	 *          an error occurs while uninstalling a resource, an uninstall
	 *          failure results with that error as the cause.
	 *      </li>
	 *      <li>Change the state to UNINSTALLED.
	 *      </li>
	 *      <li>Unregister the subsystem service.
	 *      </li>
	 *      <li>Uninstall the region context bundle.
	 *      </li>
	 * </ol>
	 * With regard to error handling, once this subsystem has transitioned to
	 * the UNINSTALLING state, every part of each step above must be attempted.
	 * Errors subsequent to the first should be logged. Once the uninstall
	 * process has completed, a SubsystemException must be thrown with the
	 * specified cause.
	 * <p/>
	 * @throws SubsystemException If this subsystem fails to uninstall cleanly.
	 * @throws IllegalStateException If this subsystem's state is in
	 *         {INSTALL_FAILED}. 
	 * @throws SecurityException If the caller does not have the appropriate 
	 *         SubsystemPermission[this,LIFECYCLE], and the runtime supports
	 *         permissions.
	 */
	public void uninstall() throws SubsystemException;
}