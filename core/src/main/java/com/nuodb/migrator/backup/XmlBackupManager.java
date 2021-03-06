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
package com.nuodb.migrator.backup;

import com.nuodb.migrator.utils.xml.XmlHandlerRegistry;
import com.nuodb.migrator.utils.xml.XmlHandlerRegistryReader;
import com.nuodb.migrator.utils.xml.XmlHandlerStrategy;
import com.nuodb.migrator.utils.xml.XmlPersister;
import org.simpleframework.xml.strategy.TreeStrategy;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * @author Sergey Bushik
 */
public class XmlBackupManager extends BackupManagerBase implements XmlConstants {

    public XmlBackupManager(String path) {
        super(path);
    }

    public XmlBackupManager(File file) {
        super(file);
    }

    public XmlBackupManager(String dir, String desc) {
        super(dir, desc);
    }

    @Override
    public Backup readBackup(InputStream input, Map context) {
        return createXmlPersister().read(Backup.class, input, context);
    }

    @Override
    public void writeBackup(Backup backup, OutputStream output, Map context) {
        createXmlPersister().write(backup, output, context);
    }

    protected XmlPersister createXmlPersister() {
        XmlHandlerRegistry xmlRegistry = new XmlHandlerRegistry();
        XmlHandlerRegistryReader registryReader = new XmlHandlerRegistryReader();
        registryReader.addRegistry(XML_HANDLER_REGISTRY);
        registryReader.read(xmlRegistry);
        return new XmlPersister(new XmlHandlerStrategy(xmlRegistry, new TreeStrategy()));
    }
}
