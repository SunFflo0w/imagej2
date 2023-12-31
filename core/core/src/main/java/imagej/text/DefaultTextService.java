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

package imagej.text;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginInfo;
import org.scijava.plugin.PluginService;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;
import org.scijava.util.FileUtils;

/**
 * Default service for working with text formats.
 * 
 * @author Curtis Rueden
 */
@Plugin(type = Service.class)
public final class DefaultTextService extends AbstractService implements
	TextService
{

	@Parameter
	public PluginService pluginService;

	/** List of available text formats. */
	private ArrayList<TextFormat> formats = new ArrayList<TextFormat>();

	// -- TextService methods --

	@Override
	public String open(final File file) throws IOException {
		// This routine is from: http://stackoverflow.com/a/326440
		final FileInputStream stream = new FileInputStream(file);
		try {
			final FileChannel fc = stream.getChannel();
			final MappedByteBuffer bb =
				fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
			return Charset.defaultCharset().decode(bb).toString();
		}
		finally {
			stream.close();
		}
	}

	@Override
	public boolean isText(final File file) {
		return getFormat(file) != null;
	}

	@Override
	public TextFormat getFormat(final File file) {
		final String extension = FileUtils.getExtension(file);
		for (final TextFormat format : formats) {
			for (final String ext : format.getExtensions()) {
				if (ext.equals(extension)) return format;
			}
		}
		// no suitable format
		return null;
	}

	@Override
	public List<TextFormat> getFormats() {
		return Collections.unmodifiableList(formats);
	}

	@Override
	public String asHTML(final File file) throws IOException {
		final TextFormat format = getFormat(file);
		if (format == null) return null;
		return "<html><body>" + format.asHTML(open(file)) + "</body></html>";
	}

	// -- Service methods --

	@Override
	public void initialize() {
		for (final PluginInfo<TextFormat> info : pluginService
			.getPluginsOfType(TextFormat.class))
		{
			formats.add(pluginService.createInstance(info));
		}
	}

}
