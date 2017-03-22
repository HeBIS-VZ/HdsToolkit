/*
 * Copyright 2016, 2017 by HeBIS (www.hebis.de).
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
package de.hebis.it.hds.tools.concurrent;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A waiting ans named {@link ExecutorService}. <br/>
 * * "waiting" The input queue has is a {@link BlockingQueue} <br/>
 * * "named" The threads handled by this executer get are named to ease debugging.
 * 
 * @author Uwe 20.05.2016
 * @version 2017-03-22 uh revised
 *
 */
public class WaitingNamedExecutorService implements ThreadFactory {
   private static final Logger        LOG                  = LogManager.getLogger(WaitingNamedExecutorService.class);
   private static final ThreadFactory defaultThreadFactory = Executors.defaultThreadFactory();
   private String                     myName               = "unset";
   private BlockingQueue<Runnable>    queue                = null;
   private ExecutorService            executor             = null;

   /**
    * Instance a new ExecutorService.
    * 
    * @param executorName Name for the executor and the handled treads
    * @param queuelength Length of the upstream blocking queue
    * @param paralelthreads Size of the executor service
    */
   public WaitingNamedExecutorService(String executorName, int queuelength, int paralelthreads) {
      if (executorName != null) myName = executorName;
      if (queuelength < 1) queuelength = 1;
      if (paralelthreads < 1) paralelthreads = 1;
      queue = new ArrayBlockingQueue<>(queuelength);
      executor = new ThreadPoolExecutor(paralelthreads, paralelthreads, 10, TimeUnit.HOURS, queue, this);
   }

   /**
    * Take a new {@link Runnable}. Calls {@link ExecutorService#execute(Runnable)}
    * 
    * @param task The runnable
    */
   public synchronized void execute(Runnable task) {
      if (waitOnQueue()) {
         if (LOG.isTraceEnabled()) LOG.trace(myName + ": Add runnable \"" + task.toString() + "\" to queue.");
         try {
            executor.execute(task);
         } catch (Exception e) {
            LOG.warn(myName + "(execute) Error: \"" + e.toString() + "\" Try to repeat.");
            execute(task);
         }
         return;
      }
      LOG.warn("Runnable: " + task + " could not be taken.");
   }

   /**
    * Take a new {@link Callable}. Calls {@link ExecutorService#submit(Callable)}
    * 
    * @param task The callable
    * @return Der A Future oft the callables result or NULL if the callable couldn't be taken.
    */
   public synchronized <T> Future<T> submit(Callable<T> task) {
      if (waitOnQueue()) {
         if (LOG.isTraceEnabled()) LOG.trace(myName + ": Callable \"" + task.toString() + "\" wird in die Warteschlange eingetragen.");
         try {
            return executor.submit(task);
         } catch (Exception e) {
            LOG.warn(myName + "(submit): Fehler \"" + e.toString() + "\" Versuche Wiederholung.");
            return submit(task);
         }
      }
      LOG.warn("Callable:" + task + "konnte nicht im ThreadPool aufgenommen werden.");
      return null;
   }

   /**
    * Notify the executor service to shutdown.<br/>
    * Differing to {@link ExecutorService#shutdown()} this method is waiting, till all tasks are finished.
    * 
    */
   public synchronized void shutdown() {
      if (LOG.isDebugEnabled()) LOG.debug(myName + " is shuting down. (after all tasks are finished");
      executor.shutdown();
      while (queue.size() > 0) { // While there are still tasks in the input queue;
         try {
            if (LOG.isTraceEnabled()) LOG.trace(myName + ": shutdown executor: The queue still has " + queue.size() + " entries");
            Thread.sleep(1000);
         } catch (InterruptedException e) {
            executor.shutdownNow();
            LOG.warn(myName + "interupt received, immidiate shutdown.");
            return;
         }
      }
      int count = 0;
      while (!executor.isTerminated()) { // The input queue is empty, just wait for the last Tasks.
         try {
            if (LOG.isInfoEnabled()) LOG.info(myName + ": waiting for now  " + (count++ * 10) + " seconds to shutdown the executor");
            executor.awaitTermination(10, TimeUnit.SECONDS);
         } catch (InterruptedException e) {
            executor.shutdownNow();
            LOG.warn(myName + "interupt received, immidiate shutdown.");
            return;
         }
      }
   }

   /**
    * Helper to let new task wait until the input queue can take a new entry.st.
    * 
    * @return Normally TRUE, but FALSE if an {@link InterruptedException} was received.
    */
   private boolean waitOnQueue() {
      if (executor.isShutdown() || executor.isTerminated()) return false;
      if (queue.remainingCapacity() > 0) return true;
      while (queue.remainingCapacity() < 1) {
         try {
            if (LOG.isTraceEnabled()) LOG.trace(myName + ": The queue (length: " + queue.size() + ") id full.  Waiting two seconds.");
            Thread.sleep(2000);
         } catch (InterruptedException e) {
            LOG.warn(myName + "interupt received, task couldn't placed into the input queue.");
            return false;
         }
      }
      return true;
   }

   /**
    * Extension of the {@link Executors#defaultThreadFactory()} to set the name of the tasks.<br/>
    * Instead of 'pool-#-thread-#' the name will be 'myName-#'. This method is implicitly called by {@link #execute(Runnable)} and {@link #submit(Callable)}
    * 
    * @param task the new Task
    */
   @Override
   public Thread newThread(Runnable task) {
      Thread newThread = defaultThreadFactory.newThread(task);
      newThread.setName(newThread.getName().replaceFirst("pool-\\d+-thread", myName));
      return newThread;
   }

}
