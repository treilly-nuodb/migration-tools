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
package com.nuodb.migrator.cli.run;

import com.nuodb.migrator.Migrator;
import com.nuodb.migrator.MigratorException;
import com.nuodb.migrator.job.HasJobSpec;
import com.nuodb.migrator.job.Job;
import com.nuodb.migrator.job.JobExecutor;
import com.nuodb.migrator.job.TraceJobExecutionListener;
import com.nuodb.migrator.spec.JobSpec;

import java.util.Map;

import static com.nuodb.migrator.job.JobExecutors.createJobExecutor;

/**
 * @author Sergey Bushik
 */
public abstract class CliJob<S extends JobSpec> extends CliRunAdapter {

    private Migrator migrator = new Migrator();
    private S jobSpec;

    public CliJob(String command) {
        super(command);
    }

    public S getJobSpec() {
        return jobSpec;
    }

    public void setJobSpec(S jobSpec) {
        this.jobSpec = jobSpec;
    }

    public Migrator getMigrator() {
        return migrator;
    }

    public void setMigrator(Migrator migrator) {
        this.migrator = migrator;
    }
}
