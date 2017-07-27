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

package co.cask.cdap.app.runtime.spark;

import co.cask.cdap.app.runtime.ProgramRunner;
import co.cask.cdap.app.runtime.ProgramStateWriter;
import co.cask.cdap.common.conf.CConfiguration;
import co.cask.cdap.internal.app.AbstractInMemoryProgramRunner;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * For running {@link SparkProgramRunner}. Only used in-memory / standalone.
 */
public class InMemorySparkProgramRunner extends AbstractInMemoryProgramRunner {

  private static final Logger LOG = LoggerFactory.getLogger(InMemorySparkProgramRunner.class);

  private final CConfiguration cConf;

  public InMemorySparkProgramRunner(Injector injector, ProgramRunner runner) {
    this(injector.getInstance(CConfiguration.class), injector.getInstance(ProgramStateWriter.class), runner);
  }

  private InMemorySparkProgramRunner(CConfiguration cConf, ProgramStateWriter programStateWriter,
                                     ProgramRunner runner) {
    super(cConf, runner, programStateWriter);
    this.cConf = cConf;
  }
}

