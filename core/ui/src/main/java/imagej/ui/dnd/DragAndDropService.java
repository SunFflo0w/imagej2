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

package imagej.ui.dnd;

import imagej.display.Display;

import java.util.List;

import org.scijava.service.Service;

/**
 * Interface for service that handles drag and drop events.
 * 
 * @author Curtis Rueden
 */
public interface DragAndDropService extends Service {

	/**
	 * Checks whether the given {@link DragAndDropData} can be dropped onto the
	 * specified display. A (data, display) pair is deemed compatible if a
	 * compatible handler exists for them.
	 * 
	 * @see DragAndDropHandler
	 */
	boolean isCompatible(DragAndDropData data, Display<?> display);

	/**
	 * Checks whether the given object can be dropped onto the specified display.
	 * An (object, display) pair is deemed compatible if a compatible handler
	 * exists for them.
	 * 
	 * @see DragAndDropHandler
	 */
	boolean isCompatible(Object object, Display<?> display);

	/**
	 * Performs a drag-and-drop operation in the given display with the specified
	 * {@link DragAndDropData}, using the first available compatible handler.
	 * 
	 * @see DragAndDropHandler
	 * @return true if the drop operation was successful
	 * @throws IllegalArgumentException if the display and/or data object are
	 *           unsupported, or are incompatible with one another.
	 */
	boolean drop(DragAndDropData data, Display<?> display);

	/**
	 * Performs a drag-and-drop operation in the given display with the specified
	 * data object, using the first available compatible handler.
	 * 
	 * @see DragAndDropHandler
	 * @return true if the drop operation was successful
	 * @throws IllegalArgumentException if the display and/or data object are
	 *           unsupported, or are incompatible with one another.
	 */
	boolean drop(Object data, Display<?> display);

	/**
	 * Gets the drag-and-drop handler which will be used to handle the given
	 * {@link DragAndDropData} dragged onto the specified display.
	 * 
	 * @return The first compatible drag-and-drop handler, or null if none
	 *         available.
	 */
	DragAndDropHandler<?> getHandler(DragAndDropData data, Display<?> display);

	/**
	 * Gets the drag-and-drop handler which will be used to handle the given
	 * object dragged onto the specified display.
	 * 
	 * @return The first compatible drag-and-drop handler, or null if none
	 *         available.
	 */
	DragAndDropHandler<?> getHandler(Object object, Display<?> display);

	/**
	 * Gets the list of available drag-and-drop handlers, which are used to
	 * perform drag-and-drop operations.
	 */
	List<DragAndDropHandler<?>> getHandlers();

}
