/**
 * Copyright (c) 2012, NuoDB, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of NuoDB, Inc. nor the names of its contributors may
 *       be used to endorse or promote products derived from this software
 *       without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL NUODB, INC. BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.nuodb.tools.migration.job;

import java.util.Date;

/**
 * @author Sergey Bushik
 */
public class JobStatusImpl implements JobStatus {

    private JobStatusType jobStatusType;
    private boolean running;
    private boolean paused;
    private boolean stopped;
    private Date executionStartDate;
    private Date executionEndDate;
    private Throwable exception;

    @Override
    public JobStatusType getJobStatusType() {
        return jobStatusType;
    }

    @Override
    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    @Override
    public boolean isStopped() {
        return stopped;
    }

    public void setStopped(boolean stopped) {
        this.stopped = stopped;
    }

    @Override
    public Throwable getException() {
        return exception;
    }

    public void setException(Throwable exception) {
        this.exception = exception;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
        this.jobStatusType = running ? JobStatusType.RUNNING : JobStatusType.FINISHED;
    }

    @Override
    public Date getExecutionStartDate() {
        return executionStartDate;
    }

    public void setExecutionStartDate(Date executionStartDate) {
        this.executionStartDate = executionStartDate;
    }

    @Override
    public Date getExecutionEndDate() {
        return executionEndDate;
    }

    public void setExecutionEndDate(Date executionEndDate) {
        this.executionEndDate = executionEndDate;
    }

    public void pause() {
        this.jobStatusType = JobStatusType.PAUSED;
        this.paused = true;
    }

    public void resume() {
        this.jobStatusType = JobStatusType.WAITING;
        this.paused = false;
    }

    public void stop() {
        this.jobStatusType = JobStatusType.STOPPED;
        this.stopped = true;
    }
}