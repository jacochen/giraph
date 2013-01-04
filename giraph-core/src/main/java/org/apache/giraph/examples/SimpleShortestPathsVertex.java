/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.giraph.examples;

import org.apache.giraph.graph.Edge;
import org.apache.giraph.vertex.EdgeListVertex;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.log4j.Logger;

/**
 * Demonstrates the basic Pregel shortest paths implementation.
 */
@Algorithm(
    name = "Shortest paths",
    description = "Finds all shortest paths from a selected vertex"
)
public class SimpleShortestPathsVertex extends
    EdgeListVertex<LongWritable, DoubleWritable,
    FloatWritable, DoubleWritable> {
  /** The shortest paths id */
  public static final String SOURCE_ID = "SimpleShortestPathsVertex.sourceId";
  /** Default shortest paths id */
  public static final long SOURCE_ID_DEFAULT = 1;
  /** Class logger */
  private static final Logger LOG =
      Logger.getLogger(SimpleShortestPathsVertex.class);

  /**
   * Is this vertex the source id?
   *
   * @return True if the source id
   */
  private boolean isSource() {
    return getId().get() ==
        getContext().getConfiguration().getLong(SOURCE_ID,
            SOURCE_ID_DEFAULT);
  }

  @Override
  public void compute(Iterable<DoubleWritable> messages) {
    if (getSuperstep() == 0) {
      setValue(new DoubleWritable(Double.MAX_VALUE));
    }
    double minDist = isSource() ? 0d : Double.MAX_VALUE;
    for (DoubleWritable message : messages) {
      minDist = Math.min(minDist, message.get());
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug("Vertex " + getId() + " got minDist = " + minDist +
          " vertex value = " + getValue());
    }
    if (minDist < getValue().get()) {
      setValue(new DoubleWritable(minDist));
      for (Edge<LongWritable, FloatWritable> edge : getEdges()) {
        double distance = minDist + edge.getValue().get();
        if (LOG.isDebugEnabled()) {
          LOG.debug("Vertex " + getId() + " sent to " +
              edge.getTargetVertexId() + " = " + distance);
        }
        sendMessage(edge.getTargetVertexId(), new DoubleWritable(distance));
      }
    }
    voteToHalt();
  }
}
