/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2013 Board of Regents of the University of
 * Wisconsin-Madison, Broad Institute of MIT and Harvard, and Max Planck
 * Institute of Molecular Cell Biology and Genetics.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are
 * those of the authors and should not be interpreted as representing official
 * policies, either expressed or implied, of any organization.
 * #L%
 */

package imagej.command;

import imagej.Cancelable;
import imagej.ValidityProblem;
import imagej.module.Module;
import imagej.module.ModuleException;
import imagej.module.ModuleInfo;
import imagej.module.ModuleItem;
import imagej.module.event.ModulesUpdatedEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.scijava.InstantiableException;
import org.scijava.ItemVisibility;
import org.scijava.event.EventService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginInfo;
import org.scijava.util.ClassUtils;
import org.scijava.util.StringMaker;

/**
 * A collection of metadata about a particular {@link Command}.
 * <p>
 * Unlike its more general superclass {@link PluginInfo}, a {@code CommandInfo}
 * implements {@link ModuleInfo}, allowing it to describe and instantiate the
 * command in {@link Module} form.
 * </p>
 * 
 * @author Curtis Rueden
 * @author Johannes Schindelin
 * @author Grant Harris
 * @see ModuleInfo
 * @see CommandModule
 * @see Command
 */
public class CommandInfo extends PluginInfo<Command> implements ModuleInfo {

	/** Wrapped {@link PluginInfo}, if any. */
	private PluginInfo<Command> info;

	/** List of items with fixed, preset values. */
	private Map<String, Object> presets;

	/** Factory used to create a module associated with the associated command. */
	private CommandModuleFactory factory;

	/**
	 * Flag indicating whether the command parameters have been parsed. Parsing
	 * the parameters requires loading the command class, so doing so is deferred
	 * until information about the parameters is actively needed.
	 */
	private boolean paramsParsed;

	/** List of problems detected when parsing command parameters. */
	private final List<ValidityProblem> problems =
		new ArrayList<ValidityProblem>();

	/** Table of inputs, keyed on name. */
	private final Map<String, ModuleItem<?>> inputMap =
		new HashMap<String, ModuleItem<?>>();

	/** Table of outputs, keyed on name. */
	private final Map<String, ModuleItem<?>> outputMap =
		new HashMap<String, ModuleItem<?>>();

	/** Ordered list of input items. */
	private final List<ModuleItem<?>> inputList = new ArrayList<ModuleItem<?>>();

	/** Ordered list of output items. */
	private final List<ModuleItem<?>> outputList =
		new ArrayList<ModuleItem<?>>();

	// -- Constructors --

	/**
	 * Creates a new command metadata object.
	 * 
	 * @param className The name of the class, which must implement
	 *          {@link Command}.
	 */
	public CommandInfo(final String className) {
		this(null, className, null, null);
	}

	/**
	 * Creates a new command metadata object.
	 * 
	 * @param className The name of the class, which must implement
	 *          {@link Command}.
	 * @param annotation The @{@link Plugin} annotation to associate with this
	 *          metadata object.
	 */
	public CommandInfo(final String className, final Plugin annotation) {
		this(null, className, null, annotation);
	}

	/**
	 * Creates a new command metadata object.
	 * 
	 * @param commandClass The plugin class, which must implement {@link Command}.
	 */
	public CommandInfo(final Class<? extends Command> commandClass) {
		this(null, null, commandClass, null);
	}

	/**
	 * Creates a new command metadata object.
	 * 
	 * @param commandClass The plugin class, which must implement {@link Command}.
	 * @param annotation The @{@link Plugin} annotation to associate with this
	 *          metadata object.
	 */
	public CommandInfo(final Class<? extends Command> commandClass,
		final Plugin annotation)
	{
		this(null, null, commandClass, annotation);
	}

	/**
	 * Creates a new command metadata object describing the same command as the
	 * given {@link PluginInfo}.
	 * 
	 * @param info The plugin metadata to wrap.
	 */
	public CommandInfo(final PluginInfo<Command> info) {
		this(info, null, null, info.getAnnotation());
	}

	protected CommandInfo(final PluginInfo<Command> info, final String className,
		final Class<? extends Command> commandClass, final Plugin annotation)
	{
		super(className, commandClass, Command.class, annotation, null);
		this.info = info;
		setPresets(null);
		setCommandModuleFactory(null);
	}

	// -- CommandInfo methods --

	/** Sets the table of items with fixed, preset values. */
	public void setPresets(final Map<String, Object> presets) {
		if (presets == null) {
			this.presets = new HashMap<String, Object>();
		}
		else {
			this.presets = presets;
		}
	}

	/** Gets the table of items with fixed, preset values. */
	public Map<String, Object> getPresets() {
		return presets;
	}

	/** Sets the factory used to construct {@link CommandModule} instances. */
	public void setCommandModuleFactory(final CommandModuleFactory factory) {
		if (factory == null) {
			this.factory = new DefaultCommandModuleFactory();
		}
		else {
			this.factory = factory;
		}
	}

	/** Gets the factory used to construct {@link CommandModule} instances. */
	public CommandModuleFactory getCommandModuleFactory() {
		return factory;
	}

	/**
	 * Instantiates the module described by this module info, around the specified
	 * existing command instance.
	 */
	public Module createModule(final Command commandInstance) {
		return factory.createModule(this, commandInstance);
	}

	// -- PluginInfo methods --

	@Override
	public void setPluginClass(final Class<? extends Command> pluginClass) {
		if (info == null) super.setPluginClass(pluginClass);
		else info.setPluginClass(pluginClass);
	}

	@Override
	public Class<? extends Command> getPluginClass() {
		return info == null ? super.getPluginClass() : info.getPluginClass();
	}

	@Override
	public URL getIconURL() throws InstantiableException {
		return info == null ? super.getIconURL() : info.getIconURL();
	}

	// -- Object methods --

	@Override
	public String toString() {
		final StringMaker sm = new StringMaker(super.toString());
		for (final String key : presets.keySet()) {
			final Object value = presets.get(key);
			sm.append(key, value);
		}
		return sm.toString();
	}

	// -- Instantiable methods --

	@Override
	public String getClassName() {
		return info == null ? super.getClassName() : info.getClassName();
	}

	@Override
	public Class<? extends Command> loadClass() throws InstantiableException {
		return info == null ? super.loadClass() : info.loadClass();
	}

	@Override
	public Command createInstance() throws InstantiableException {
		return info == null ? super.createInstance() : info.createInstance();
	}

	// -- ModuleInfo methods --

	@Override
	public CommandModuleItem<?> getInput(final String name) {
		parseParams();
		return (CommandModuleItem<?>) inputMap.get(name);
	}

	@Override
	public CommandModuleItem<?> getOutput(final String name) {
		parseParams();
		return (CommandModuleItem<?>) outputMap.get(name);
	}

	@Override
	public Iterable<ModuleItem<?>> inputs() {
		parseParams();
		return Collections.unmodifiableList(inputList);
	}

	@Override
	public Iterable<ModuleItem<?>> outputs() {
		parseParams();
		return Collections.unmodifiableList(outputList);
	}

	@Override
	public String getDelegateClassName() {
		return getClassName();
	}

	@Override
	public Module createModule() throws ModuleException {
		return factory.createModule(this);
	}

	@Override
	public boolean canPreview() {
		final Class<?> commandClass = loadCommandClass();
		if (commandClass == null) return false;
		return Previewable.class.isAssignableFrom(commandClass);
	}

	@Override
	public boolean canCancel() {
		final Class<?> commandClass = loadCommandClass();
		if (commandClass == null) return false;
		return Cancelable.class.isAssignableFrom(commandClass);
	}

	@Override
	public boolean canRunHeadless() {
		return getAnnotation() == null ? false : getAnnotation().headless();
	}

	@Override
	public String getInitializer() {
		return getAnnotation() == null ? null : getAnnotation().initializer();
	}

	@Override
	public void update(final EventService eventService) {
		eventService.publish(new ModulesUpdatedEvent(this));
	}

	// -- UIDetails methods --

	@Override
	public String getTitle() {
		final String title = super.getTitle();
		if (!title.equals(getClass().getSimpleName())) return title;

		// use delegate class name rather than actual class name
		final String className = getDelegateClassName();
		final int dot = className.lastIndexOf(".");
		return dot < 0 ? className : className.substring(dot + 1);
	}

	// -- Validated methods --

	@Override
	public boolean isValid() {
		parseParams();
		return problems.isEmpty();
	}

	@Override
	public List<ValidityProblem> getProblems() {
		parseParams();
		return Collections.unmodifiableList(problems);
	}

	// -- Helper methods --

	/**
	 * Parses the command's inputs and outputs. Invoked lazily, as needed, to
	 * defer class loading as long as possible.
	 */
	private void parseParams() {
		if (paramsParsed) return;
		paramsParsed = true;
		checkFields(loadCommandClass());
	}

	/** Processes the given class's @{@link Parameter}-annotated fields. */
	private void checkFields(final Class<?> type) {
		if (type == null) return;
		final List<Field> fields =
			ClassUtils.getAnnotatedFields(type, Parameter.class);

		for (final Field f : fields) {
			f.setAccessible(true); // expose private fields

			final Parameter param = f.getAnnotation(Parameter.class);

			boolean valid = true;

			final boolean isFinal = Modifier.isFinal(f.getModifiers());
			final boolean isMessage = param.visibility() == ItemVisibility.MESSAGE;
			if (isFinal && !isMessage) {
				// NB: Final parameters are bad because they cannot be modified.
				final String error = "Invalid final parameter: " + f;
				problems.add(new ValidityProblem(error));
				valid = false;
			}

			final String name = f.getName();
			if (inputMap.containsKey(name) || outputMap.containsKey(name)) {
				// NB: Shadowed parameters are bad because they are ambiguous.
				final String error = "Invalid duplicate parameter: " + f;
				problems.add(new ValidityProblem(error));
				valid = false;
			}

			if (!valid) {
				// NB: Skip invalid parameters.
				continue;
			}

			final boolean isPreset = presets.containsKey(name);

			// add item to the relevant list (inputs or outputs)
			final CommandModuleItem<Object> item =
				new CommandModuleItem<Object>(this, f);
			if (item.isInput()) {
				inputMap.put(name, item);
				if (!isPreset) inputList.add(item);
			}
			if (item.isOutput()) {
				outputMap.put(name, item);
				if (!isPreset) outputList.add(item);
			}
		}
	}

	private Class<?> loadCommandClass() {
		try {
			return loadClass();
		}
		catch (final InstantiableException e) {
			final String error =
				"Could not initialize command class: " + getClassName();
			problems.add(new ValidityProblem(error, e));
		}
		return null;
	}

}
