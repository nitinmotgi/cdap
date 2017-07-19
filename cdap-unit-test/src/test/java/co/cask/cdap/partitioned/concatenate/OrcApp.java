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

import co.cask.cdap.api.Resources;
import co.cask.cdap.api.app.AbstractApplication;
import co.cask.cdap.api.data.batch.Input;
import co.cask.cdap.api.data.batch.Output;
import co.cask.cdap.api.dataset.lib.FileSet;
import co.cask.cdap.api.dataset.lib.FileSetArguments;
import co.cask.cdap.api.dataset.lib.FileSetProperties;
import co.cask.cdap.api.dataset.lib.PartitionKey;
import co.cask.cdap.api.dataset.lib.PartitionedFileSet;
import co.cask.cdap.api.dataset.lib.PartitionedFileSetArguments;
import co.cask.cdap.api.dataset.lib.PartitionedFileSetProperties;
import co.cask.cdap.api.dataset.lib.Partitioning;
import co.cask.cdap.api.mapreduce.AbstractMapReduce;
import co.cask.cdap.api.mapreduce.MapReduceContext;
import org.apache.hadoop.hive.ql.io.orc.OrcNewOutputFormat;
import org.apache.hadoop.hive.ql.io.orc.OrcSerde;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.typeinfo.TypeInfo;
import org.apache.hadoop.hive.serde2.typeinfo.TypeInfoUtils;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Application that has a MapReduce that writes in Orc format
 */
public class OrcApp extends AbstractApplication {

  protected static final String NAME = OrcApp.class.getSimpleName();
  protected static final String FILESET_NAME = "inputFileSet";
  protected static final String CLEAN_RECORDS = "cleanRecords";

  @Override
  public void configure() {
    setName(NAME);
    addMapReduce(new MyMapReduce());

    createDataset(FILESET_NAME, FileSet.class, FileSetProperties.builder()
      .setInputFormat(TextInputFormat.class)
      .build());

    createDataset(CLEAN_RECORDS, PartitionedFileSet.class, PartitionedFileSetProperties.builder()
      // Properties for partitioning
      .setPartitioning(Partitioning.builder().addLongField("time").build())
      // Properties for file set
      .setOutputFormat(OrcNewOutputFormat.class)
      // Properties for Explore (to create a partitioned Hive table)
      .setEnableExploreOnCreate(true)
      .setSerDe("org.apache.hadoop.hive.ql.io.orc.OrcSerde")
      .setExploreInputFormat("org.apache.hadoop.hive.ql.io.orc.OrcInputFormat")
      .setExploreOutputFormat("org.apache.hadoop.hive.ql.io.orc.OrcOutputFormat")
      .setExploreSchema("record STRING")
      .build());
  }

  static final class MyMapReduce extends AbstractMapReduce {
    protected static final String NAME = MyMapReduce.class.getSimpleName();
    protected static final String OUTPUT_PARTITION_KEY = "output.partition.key";

    @Override
    public void configure() {
      setName(NAME);
      setMapperResources(new Resources(1024));
      setReducerResources(new Resources(1024));
    }

    @Override
    public void initialize() throws Exception {
      MapReduceContext context = getContext();

      Map<String, String> inputArgs = new HashMap<>();
      // read all files in the FileSet
      FileSetArguments.setInputPath(inputArgs, "/");
      context.addInput(Input.ofDataset(FILESET_NAME, inputArgs));

      Long timeKey = Long.valueOf(context.getRuntimeArguments().get(OUTPUT_PARTITION_KEY));
      PartitionKey outputKey = PartitionKey.builder().addLongField("time", timeKey).build();

      Map<String, String> cleanRecordsArgs = new HashMap<>();
      PartitionedFileSetArguments.setOutputPartitionKey(cleanRecordsArgs, outputKey);
      context.addOutput(Output.ofDataset(CLEAN_RECORDS, cleanRecordsArgs));

      Job job = context.getHadoopJob();
      job.setMapperClass(SchemaMatchingFilter.class);
      job.setNumReduceTasks(0);
    }

    /**
     * A Mapper which skips text that doesn't match a given schema.
     */
    public static class SchemaMatchingFilter extends Mapper<LongWritable, Text, NullWritable, Writable> {

      private final OrcSerde orcSerde = new OrcSerde();
      private final TypeInfo typeInfo = TypeInfoUtils.getTypeInfoFromTypeString("struct<key:string>");
      private final ObjectInspector objectInspector =
        TypeInfoUtils.getStandardJavaObjectInspectorFromTypeInfo(typeInfo);

      @Override
      protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
        context.write(NullWritable.get(),
                      orcSerde.serialize(Collections.singletonList(value.toString()), objectInspector));
      }
    }
  }
}
