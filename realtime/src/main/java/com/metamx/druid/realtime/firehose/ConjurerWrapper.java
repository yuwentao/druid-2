/*
* Druid - a distributed column store.
* Copyright (C) 2012  Metamarkets Group Inc.
*
* This program is free software; you can redistribute it and/or
* modify it under the terms of the GNU General Public License
* as published by the Free Software Foundation; either version 2
* of the License, or (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; if not, write to the Free Software
* Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/

package com.metamx.druid.realtime.firehose;

import com.google.common.base.Throwables;
import com.metamx.emitter.EmittingLogger;
import io.d8a.conjure.Conjurer;
import io.d8a.conjure.ConjurerBuilder;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class ConjurerWrapper
{
  private static final EmittingLogger log = new EmittingLogger(ConjurerWrapper.class);
  private final ConjurerBuilder builder;
  private final Conjurer conjurer;
  private final int QUEUE_SIZE = 10000;
  private final Thread conjureThread;
  private final BlockingQueue<Object> queue = new ArrayBlockingQueue<Object>(QUEUE_SIZE);

  public ConjurerWrapper(ConjurerBuilder builder)
  {
    this.builder = builder;
    builder.setPrinter(Conjurer.queuePrinter(queue));
    this.conjurer = builder.build();
    conjureThread = new Thread()
    {
      public void run()
      {
        while (!isInterrupted()) {
          conjurer.run();
        }
      }
    };
    conjureThread.setDaemon(true);
  }

  public void start()
  {
    conjureThread.start();
  }

  public void stop()
  {
    conjureThread.interrupt();
  }

  public Map<String, Object> takeFromQueue(long waitTime, TimeUnit unit)
  {
    try {
      return (Map<String,Object>) queue.poll(waitTime,unit);
    }
    catch (InterruptedException e) {
      throw Throwables.propagate(e);
    }
  }
}
