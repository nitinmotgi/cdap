/*
 * Copyright © 2017 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package co.cask.cdap.partitioned.concatenate;

import co.cask.cdap.api.common.Bytes;
import co.cask.cdap.api.dataset.lib.FileSet;
import co.cask.cdap.api.dataset.lib.PartitionKey;
import co.cask.cdap.api.dataset.lib.PartitionedFileSet;
import co.cask.cdap.proto.ProgramRunStatus;
import co.cask.cdap.test.ApplicationManager;
import co.cask.cdap.test.DataSetManager;
import co.cask.cdap.test.MapReduceManager;
import co.cask.cdap.test.base.TestFrameworkTestBase;
import com.google.common.collect.Iterables;
import org.apache.hadoop.mapreduce.lib.output.FileOutputCommitter;
import org.apache.twill.filesystem.Location;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Tests functionality to concatenate files within a partition of a PartitionedFileSet.
 */
public class PartitionConcatenateTest extends TestFrameworkTestBase {

  /**
   * 1. Write 100 small files (text) to an input FileSet.
   * 2. Run a MapReduce that reads these files and writes them in ORC format to a Partition of a PartitionedFileSet.
   * 3. Execute a partition concatenate operation.
   * 4. As compared to before the concatenate operation, validate that the number of files is reduced, while
   *    the contents of the files remains the same.
   */
  @Test
  public void testConcatenate() throws Exception {
    ApplicationManager appManager = deployApplication(OrcApp.class);

    // 1. create 100 small files in the input FileSet
    int numInputFiles = 100;
    DataSetManager<FileSet> inputFileSet = getDataset(OrcApp.FILESET_NAME);
    List<String> writtenData = writeSmallFiles(inputFileSet.get().getBaseLocation(), numInputFiles);

    // 2. run the MapReduce job to write these 100 files to a PartitionedFileSet, with orc format
    long outputPartitionTime = 5000;
    MapReduceManager mapReduceManager = appManager.getMapReduceManager(OrcApp.MyMapReduce.NAME)
      .start(Collections.singletonMap(OrcApp.MyMapReduce.OUTPUT_PARTITION_KEY, Long.toString(outputPartitionTime)));
    mapReduceManager.waitForRuns(ProgramRunStatus.COMPLETED, 1, 30, TimeUnit.SECONDS);

    Assert.assertEquals(writtenData, getExploreResults());

    DataSetManager<PartitionedFileSet> cleanRecordsManager = getDataset(OrcApp.CLEAN_RECORDS);
    PartitionedFileSet cleanRecords = cleanRecordsManager.get();

    PartitionKey outputPartition = PartitionKey.builder().addLongField("time", outputPartitionTime).build();
    Location partitionLocation = cleanRecords.getPartition(outputPartition).getLocation();

    // this is a timestamp before concatenating, but after running the MapReduce job
    long beforeConcatTime = System.currentTimeMillis();

    List<Location> dataFiles = listFilteredChildren(partitionLocation);
    // each input file will result in one output file, due to the FileInputFormat class and FileOutputFormat class
    // being used
    Assert.assertEquals(numInputFiles, dataFiles.size());
    for (Location dataFile : dataFiles) {
      // all the files should have a lastModified smaller than now
      Assert.assertTrue(dataFile.lastModified() < beforeConcatTime);
    }

    // 3. run the concatenate operation
    cleanRecords.concatenatePartition(outputPartition);

    // 4. check that the data files' lastModified timestamp is updated, and there should be fewer of them
    dataFiles = listFilteredChildren(partitionLocation);
    int fileCount = dataFiles.size();
    Assert.assertTrue(fileCount < numInputFiles);
    // note that Hive doesn't guarantee the number of output files of a CONCATENATE operation, so the
    // following assertion (fileCount == 1) may not always be true, but from observation, it is
    Assert.assertEquals(1, fileCount);
    // should have a lastModified larger than now
    Assert.assertTrue(Iterables.getOnlyElement(dataFiles).lastModified() > beforeConcatTime);

    // even though the files were concatenated, the explore results should be unchanged
    Assert.assertEquals(writtenData, getExploreResults());
  }

  private List<String> writeSmallFiles(Location baseLocation, int numInputFiles) throws IOException {
    List<String> writtenData = new ArrayList<>();
    for (int i = 0; i < numInputFiles; i++) {
      Location childFile = baseLocation.append("child_" + i);
      Assert.assertTrue(childFile.createNew());
      try (OutputStream outputStream = childFile.getOutputStream()) {
        String toWrite = "outputData" + i;
        writtenData.add(toWrite);
        outputStream.write(Bytes.toBytes(toWrite));
      }
    }
    Collections.sort(writtenData);
    return writtenData;
  }

  private List<String> getExploreResults() throws Exception {
    ResultSet resultSet =
      getQueryClient().prepareStatement("select * from dataset_" + OrcApp.CLEAN_RECORDS).executeQuery();
    List<String> strings = new ArrayList<>();
    while (resultSet.next()) {
      // the schema is such that the record contents are all in the first column
      strings.add(resultSet.getString(1));
    }
    Collections.sort(strings);
    return strings;
  }

  // Lists children files, and filters "_SUCCESS" files and files that begin with dot (".")
  private List<Location> listFilteredChildren(Location location) throws IOException {
    List<Location> children = new ArrayList<>();
    for (Location child : location.list()) {
      if (!child.getName().startsWith(".") && !FileOutputCommitter.SUCCEEDED_FILE_NAME.equals(child.getName())) {
        children.add(child);
      }
    }
    return children;
  }
}
