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

package org.apache.giraph;

import org.apache.giraph.conf.GiraphClasses;
import org.apache.giraph.examples.GeneratedVertexReader;
import org.apache.giraph.examples.SimpleCheckpointVertex;
import org.apache.giraph.examples.SimpleSuperstepVertex.SimpleSuperstepVertexInputFormat;
import org.apache.giraph.examples.SimpleSuperstepVertex.SimpleSuperstepVertexOutputFormat;
import org.apache.giraph.graph.GiraphJob;
import org.apache.giraph.partition.HashRangePartitionerFactory;
import org.apache.giraph.partition.PartitionBalancer;
import org.apache.giraph.integration.SuperstepHashPartitionerFactory;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit test for manual checkpoint restarting
 */
public class TestGraphPartitioner extends BspCase {
  public TestGraphPartitioner() {
      super(TestGraphPartitioner.class.getName());
  }

  private void verifyOutput(FileSystem fs, Path outputPath)
      throws IOException {
    // TODO: this is fragile (breaks with legit serialization changes)
    final int correctLen = 120;
    if (runningInDistributedMode()) {
      FileStatus [] fileStatusArr = fs.listStatus(outputPath);
      int totalLen = 0;
      for (FileStatus fileStatus : fileStatusArr) {
        if (fileStatus.getPath().toString().contains("/part-m-")) {
          totalLen += fileStatus.getLen();
        }
      }
      assertEquals(correctLen, totalLen);
    }
  }

  /**
   * Run a sample BSP job locally and test various partitioners and
   * partition algorithms.
   *
   * @throws IOException
   * @throws ClassNotFoundException
   * @throws InterruptedException
   */
  @Test
  public void testPartitioners()
      throws IOException, InterruptedException, ClassNotFoundException {
    Path outputPath = getTempPath("testVertexBalancer");
    GiraphClasses classes = new GiraphClasses();
    classes.setVertexClass(
        SimpleCheckpointVertex.SimpleCheckpointComputation.class);
    classes.setWorkerContextClass(
        SimpleCheckpointVertex.SimpleCheckpointVertexWorkerContext.class);
    classes.setMasterComputeClass(
        SimpleCheckpointVertex.SimpleCheckpointVertexMasterCompute.class);
    classes.setVertexInputFormatClass(SimpleSuperstepVertexInputFormat.class);
    classes.setVertexOutputFormatClass(SimpleSuperstepVertexOutputFormat.class);
    GiraphJob job = prepareJob("testVertexBalancer", classes, outputPath);

    job.getConfiguration().set(
        PartitionBalancer.PARTITION_BALANCE_ALGORITHM,
        PartitionBalancer.VERTICES_BALANCE_ALGORITHM);

    assertTrue(job.run(true));
    FileSystem hdfs = FileSystem.get(job.getConfiguration());

    classes = new GiraphClasses();
    classes.setVertexClass(
        SimpleCheckpointVertex.SimpleCheckpointComputation.class);
    classes.setWorkerContextClass(
        SimpleCheckpointVertex.SimpleCheckpointVertexWorkerContext.class);
    classes.setMasterComputeClass(
        SimpleCheckpointVertex.SimpleCheckpointVertexMasterCompute.class);
    classes.setVertexInputFormatClass(SimpleSuperstepVertexInputFormat.class);
    classes.setVertexOutputFormatClass(SimpleSuperstepVertexOutputFormat.class);
    outputPath = getTempPath("testHashPartitioner");
    job = prepareJob("testHashPartitioner", classes, outputPath);
    assertTrue(job.run(true));
    verifyOutput(hdfs, outputPath);

    outputPath = getTempPath("testSuperstepHashPartitioner");
    classes = new GiraphClasses();
    classes.setVertexClass(
        SimpleCheckpointVertex.SimpleCheckpointComputation.class);
    classes.setWorkerContextClass(
        SimpleCheckpointVertex.SimpleCheckpointVertexWorkerContext.class);
    classes.setMasterComputeClass(
        SimpleCheckpointVertex.SimpleCheckpointVertexMasterCompute.class);
    classes.setVertexInputFormatClass(SimpleSuperstepVertexInputFormat.class);
    classes.setVertexOutputFormatClass(SimpleSuperstepVertexOutputFormat.class);
    job = prepareJob("testSuperstepHashPartitioner", classes, outputPath);

    job.getConfiguration().setGraphPartitionerFactoryClass(
        SuperstepHashPartitionerFactory.class);

    assertTrue(job.run(true));
    verifyOutput(hdfs, outputPath);

    job = new GiraphJob("testHashRangePartitioner");
    setupConfiguration(job);
    job.getConfiguration().setVertexClass(
        SimpleCheckpointVertex.SimpleCheckpointComputation.class);
    job.getConfiguration().setWorkerContextClass(
        SimpleCheckpointVertex.SimpleCheckpointVertexWorkerContext.class);
    job.getConfiguration().setMasterComputeClass(
        SimpleCheckpointVertex.SimpleCheckpointVertexMasterCompute.class);
    job.getConfiguration().setVertexInputFormatClass(SimpleSuperstepVertexInputFormat.class);
    job.getConfiguration().setVertexOutputFormatClass(SimpleSuperstepVertexOutputFormat.class);
    job.getConfiguration().setGraphPartitionerFactoryClass(
        HashRangePartitionerFactory.class);
    outputPath = getTempPath("testHashRangePartitioner");
    removeAndSetOutput(job, outputPath);
    assertTrue(job.run(true));
    verifyOutput(hdfs, outputPath);

    outputPath = getTempPath("testReverseIdSuperstepHashPartitioner");
    classes = new GiraphClasses();
    classes.setVertexClass(
        SimpleCheckpointVertex.SimpleCheckpointComputation.class);
    classes.setWorkerContextClass(
        SimpleCheckpointVertex.SimpleCheckpointVertexWorkerContext.class);
    classes.setMasterComputeClass(
        SimpleCheckpointVertex.SimpleCheckpointVertexMasterCompute.class);
    classes.setVertexInputFormatClass(SimpleSuperstepVertexInputFormat.class);
    classes.setVertexOutputFormatClass(SimpleSuperstepVertexOutputFormat.class);
    job = prepareJob("testReverseIdSuperstepHashPartitioner", classes,
        outputPath);
    job.getConfiguration().setGraphPartitionerFactoryClass(
        SuperstepHashPartitionerFactory.class);
    job.getConfiguration().setBoolean(
        GeneratedVertexReader.REVERSE_ID_ORDER, true);
    assertTrue(job.run(true));
    verifyOutput(hdfs, outputPath);
  }
}
