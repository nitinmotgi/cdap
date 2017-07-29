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

package co.cask.cdap.internal.app.program;

import co.cask.cdap.app.runtime.ProgramStateWriter;
import co.cask.cdap.internal.app.runtime.AbstractListener;
import co.cask.cdap.internal.app.runtime.AbstractProgramController;
import co.cask.cdap.internal.app.runtime.batch.MapReduceProgramRunner;
import co.cask.cdap.proto.id.ProgramRunId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

/**
 * Abstract implementation of {@link AbstractProgramController} that responds to state transitions and persists all
 * state changes.
 */
public class StateChangeListener extends AbstractListener {
  private static final Logger LOG = LoggerFactory.getLogger(StateChangeListener.class);

  private final ProgramRunId programRunId;
  private final String twillRunId;
  private final ProgramStateWriter programStateWriter;

  public StateChangeListener(final ProgramRunId programRunId, @Nullable final String twillRunId,
                             final ProgramStateWriter programStateWriter) {
    this.programRunId = programRunId;
    this.twillRunId = twillRunId;
    this.programStateWriter = programStateWriter;
  }

  @Override
  public void alive() {
    programStateWriter.running(programRunId, twillRunId);
  }

  @Override
  public void completed() {
    LOG.debug("Program {} completed successfully.", programRunId);
    programStateWriter.completed(programRunId);
  }

  @Override
  public void killed() {
    LOG.debug("Program {} killed.", programRunId);
    programStateWriter.killed(programRunId);
  }

  @Override
  public void suspended() {
    LOG.debug("Suspending Program {} .", programRunId);
    programStateWriter.suspend(programRunId);
  }

  @Override
  public void resuming() {
    LOG.debug("Resuming Program {}.", programRunId);
    programStateWriter.resume(programRunId);
  }

  @Override
  public void error(Throwable cause) {
    LOG.info("Program {} stopped with error: {}", programRunId, cause);
    programStateWriter.error(programRunId, cause);
  }
}
