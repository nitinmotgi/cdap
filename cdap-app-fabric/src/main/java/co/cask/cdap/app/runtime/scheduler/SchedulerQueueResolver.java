/*
 * Copyright © 2015 Cask Data, Inc.
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

package co.cask.cdap.app.runtime.scheduler;

import co.cask.cdap.common.conf.CConfiguration;
import co.cask.cdap.common.conf.Constants;
import co.cask.cdap.proto.Id;
import co.cask.cdap.proto.NamespaceConfig;
import co.cask.cdap.proto.NamespaceMeta;
import co.cask.cdap.store.NamespaceStore;
import com.google.common.base.Strings;

import javax.annotation.Nullable;


/**
 * Helper class to resolve scheduler queue name.
 */
public class SchedulerQueueResolver {
  private final String defaultQueue;
  private final NamespaceStore store;

  /**
   * Construct SchedulerQueueResolver with CConfiguration and Store.
   */
  public SchedulerQueueResolver(CConfiguration cConf, NamespaceStore store) {
    this.defaultQueue = cConf.get(Constants.AppFabric.APP_SCHEDULER_QUEUE, "");
    this.store = store;
  }

  /**
   * @return default Queue that comes from CConfiguration.
   */
  public String getDefaultQueue() {
    return defaultQueue;
  }

  /**
   * Get queue at namespace level if it is empty returns the default queue.
   *
   * @param namespaceId NamespaceId
   * @return schedule queue at namespace level or default queue.
   */
  @Nullable
  public String getQueue(Id.Namespace namespaceId) {
    NamespaceMeta meta = store.get(namespaceId);
    if (meta != null) {
      NamespaceConfig config = meta.getConfig();
      String namespaceQueue = config.getSchedulerQueueName();
      return Strings.isNullOrEmpty(namespaceQueue) ? getDefaultQueue() : namespaceQueue;
    } else {
      return getDefaultQueue();
    }
  }
}
