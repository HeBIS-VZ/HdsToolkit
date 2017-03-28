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

/**
 * Simple Parser to find lines, they belongs together, representing an object. (e.g. XML tags)<br>
 * The detailed behavior is defined by a consumer, which is declared in the constructor.
 * 
 * @author Uwe 23.10.2014 2017-03-22 uh revised
 */
public class TextBlockSpliterator implements Spliterator<List<String>> {
   static final Logger               LOG      = LogManager.getLogger(TextBlockSpliterator.class);
   private final Spliterator<String> source;
   private final Predicate<String>   start, end;
   private final Consumer<String>    getBlock;
   private String                    lastLine = "";
   private List<String>              block;

   /**
    * Instance a new TextBlockSpliterator.<br>
    * ! This spliterator needs to have the markers, identifying a block in <br>
    * ! different strings and a marker may not distributed over two strings.
    * 
    * @param lines A sequentiell stream of strings (eg. lines of a file)
    * @param startPattern Pattern to identify the first line of a block
    * @param endPattern Pattern to identify the last line of a block
    */
   public TextBlockSpliterator(Spliterator<String> lines, Predicate<String> startPattern, Predicate<String> endPattern) {
      source = lines;
      start = startPattern;
      end = endPattern;
      getBlock = line -> {
         lastLine = line;
         if (block == null) {
            if (start.test(line)) {
               if (LOG.isTraceEnabled()) LOG.trace("start: " + line);
               block = new ArrayList<>();
               block.add(line);
            } else {
               if (LOG.isTraceEnabled()) LOG.trace("noise:" + line);
            }
         } else {
            if (LOG.isTraceEnabled()) LOG.trace("newln: " + line);
            block.add(line);
         }
      };
   }

   @Override
   public boolean tryAdvance(Consumer<? super List<String>> action) {
      while (!end.test(lastLine)) { // Consume lines until the end of a block is found
         if (!source.tryAdvance(getBlock)) return false;
      }
      action.accept(block); // present found Block
      block = null; // prepare for next Block
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
    * Factory for a new TextBlockSpliterator
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
