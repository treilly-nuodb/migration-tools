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
package com.nuodb.migrator.jdbc.metadata.generator;

import com.nuodb.migrator.utils.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

import static java.lang.String.format;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author Sergey Bushik
 */
public abstract class ScriptExporterBase implements ScriptExporter {

    protected final transient Logger logger = getLogger(getClass());

    @Override
    public final void open() throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug(format("Opening script exporter %s", this));
        }
        doOpen();
    }

    protected abstract void doOpen() throws Exception;

    @Override
    public void exportScript(String script) throws Exception {
        try {
            doExportScript(script);
        } catch (Exception exception) {
            if (logger.isErrorEnabled()) {
                logger.error(format("Failed exporting script %s", script));
            }
            throw exception;
        }
    }

    @Override
    public void exportScripts(Collection<String> scripts) throws Exception {
        if (scripts == null) {
            return;
        }
        for (String script : scripts) {
            exportScript(script);
        }
    }

    protected abstract void doExportScript(String script) throws Exception;

    @Override
    public final void close() throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug(format("Closing script exporter %s", this));
        }
        doClose();
    }

    protected abstract void doClose() throws Exception;

    @Override
    public String toString() {
        return ObjectUtils.toString(this);
    }
}
