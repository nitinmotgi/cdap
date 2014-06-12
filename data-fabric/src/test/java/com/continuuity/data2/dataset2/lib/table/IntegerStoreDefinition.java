package com.continuuity.data2.dataset2.lib.table;

import com.continuuity.data2.dataset2.lib.AbstractDatasetDefinition;
import com.continuuity.internal.data.dataset.DatasetAdmin;
import com.continuuity.internal.data.dataset.DatasetDefinition;
import com.continuuity.internal.data.dataset.DatasetProperties;
import com.continuuity.internal.data.dataset.DatasetSpecification;
import com.continuuity.internal.io.UnsupportedTypeException;

import java.io.IOException;

/**
 *
 */
public class IntegerStoreDefinition
  extends AbstractDatasetDefinition<ObjectStore<Integer>, DatasetAdmin> {

  private final DatasetDefinition<? extends KeyValueTable, ?> tableDef;

  public IntegerStoreDefinition(String name, DatasetDefinition<? extends KeyValueTable, ?> keyValueTableDefinition) {
    super(name);
    this.tableDef = keyValueTableDefinition;
  }

  @Override
  public DatasetSpecification configure(String instanceName, DatasetProperties properties) {
    return DatasetSpecification.builder(instanceName, getName())
      .properties(properties.getProperties())
      .datasets(tableDef.configure("table", properties))
      .build();
  }

  @Override
  public DatasetAdmin getAdmin(DatasetSpecification spec) throws IOException {
    return tableDef.getAdmin(spec.getSpecification("table"));
  }

  @Override
  public ObjectStore<Integer> getDataset(DatasetSpecification spec) throws IOException {
    DatasetSpecification kvTableSpec = spec.getSpecification("table");
    KeyValueTable table = tableDef.getDataset(kvTableSpec);

    try {
      return new IntegerStore(spec.getName(), table);
    } catch (UnsupportedTypeException e) {
      // shouldn't happen
      throw new IOException(e);
    }
  }

}
