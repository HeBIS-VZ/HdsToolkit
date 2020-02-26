/*
 * Copyright 2014-2017 by HeBIS (www.hebis.de).
 * 
 * This file is part of HeBIS HdsToolkit.
 *
 * This is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 *
 * This code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the code.  If not, see http://www.gnu.org/licenses/agpl>.
 */
package de.hebis.it.hds.tools.streams;

import java.util.ArrayList;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.nio.file.Files;

/**
 * Simple spliterator to find blocks of contiguous lines, from a sequential {@link Stream} of Strings<br>
 * The blocks need to have markers (character sequences) to identify the first and the last line(e.g. XML tags)<br>
 * The detailed behavior is defined by a consumer, which is declared in the constructor.
 * 
 * @author Uwe Reh (uh), HeBIS-IT
 * @version 2014-10-23 uh First try
 * @version 2017-03-29 uh revised
 */
public class TextBlockSpliterator implements Spliterator<List<String>> {
   static final Logger               LOG      = LogManager.getLogger(TextBlockSpliterator.class);
   private final Spliterator<String> source;
   private final Predicate<String>   startPattern;
   private final Predicate<String>   endPattern;
   private final Consumer<String>    textBlockConsumer;
   private String                    lastLine = "";
   private List<String>              textBlock;

   /**
    * Instance a new TextBlockSpliterator.<br>
    * <dl>
    * <dt>Limitations</dt>
    * <dd>The markers may not distributed over two strings/lines.</dd>
    * <dd>Additional leading or trailing characters are not removed.</dd>
    * </dl>
    * 
    * @param lines A sequential stream of strings (e.g. {@link Files#lines(java.nio.file.Path)}
    * @param startPattern Pattern to identify the first line of a block
    * @param endPattern Pattern to identify the last line of a block
    */
   public TextBlockSpliterator(Spliterator<String> lines, Predicate<String> blockStartPattern, Predicate<String> blockEndPattern) {
      source = lines;
      startPattern = blockStartPattern;
      endPattern = blockEndPattern;
      // Define the consumer used in tryAdvance
      textBlockConsumer = line -> {
         lastLine = line;
         if (textBlock == null) { // look for a new block
            if (startPattern.test(line)) {
               // begin a new block
               if (LOG.isTraceEnabled()) LOG.trace("start: " + line);
               textBlock = new ArrayList<>();
               textBlock.add(line);
            } else {
               // anything outside of the block
               if (LOG.isTraceEnabled()) LOG.trace("noise:" + line);
            }
         } else {
            // from the startpattern (10 lines above) to the endpattern (tested in tryAdvance) it's content.
            if (LOG.isTraceEnabled()) LOG.trace("newln: " + line);
            textBlock.add(line);
         }
      };
   }

   @Override
   public boolean tryAdvance(Consumer<? super List<String>> action) {
      while (!endPattern.test(lastLine)) { // Consume lines until the end of a block is found
         if (!source.tryAdvance(textBlockConsumer)) return false;
      }
      action.accept(textBlock); // present found Block
      textBlock = null; // prepare for next Block
      lastLine = "";
      return true;
   }

   @Override
   public Spliterator<List<String>> trySplit() {
      return null;
   }

   @Override
   public long estimateSize() {
      return Long.MAX_VALUE;
   }

   @Override
   public int characteristics() {
      return ORDERED | NONNULL;
   }

   /**
    * Factory for a new stream of text blocks stripped out of a stream of lines
    * 
    * @param lines The {@link Stream} to consume
    * @param blockStartPattern Pattern to identify the start of a block
    * @param blockEndPattern Pattern to identify the end of a block
    * @param parallel May the output stream processed in parallel or not
    * @return A stream of blocks (block ~ list of strings)
    */
   public static Stream<List<String>> toTextBlocks(Stream<String> lines, Predicate<String> blockStartPattern, Predicate<String> blockEndPattern, boolean parallel) {
      return StreamSupport.stream(new TextBlockSpliterator(lines.spliterator(), blockStartPattern, blockEndPattern), parallel);
   }
}
