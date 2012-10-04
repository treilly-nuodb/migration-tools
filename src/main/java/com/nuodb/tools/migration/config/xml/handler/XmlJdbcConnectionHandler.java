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
package com.nuodb.tools.migration.config.xml.handler;

import com.nuodb.tools.migration.config.xml.XmlConstants;
import com.nuodb.tools.migration.config.xml.XmlReadContext;
import com.nuodb.tools.migration.config.xml.XmlWriteContext;
import com.nuodb.tools.migration.spec.DriverManagerConnectionSpec;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.OutputNode;

import java.util.Map;

public class XmlJdbcConnectionHandler extends XmlReadWriteHandlerBase<DriverManagerConnectionSpec> implements XmlConstants {

    public XmlJdbcConnectionHandler() {
        super(DriverManagerConnectionSpec.class);
    }

    @Override
    protected boolean write(DriverManagerConnectionSpec connection, OutputNode output, XmlWriteContext context) throws Exception {
        output.getNamespaces().setReference(MIGRATION_NAMESPACE);
        set(output, ID_ATTRIBUTE, connection.getId());
        set(output, TYPE_ATTRIBUTE, connection.getType());
        output.getChild("catalog").setValue(connection.getCatalog());
        output.getChild("schema").setValue(connection.getSchema());
        output.getChild("driver").setValue(connection.getDriver());
        output.getChild("url").setValue(connection.getUrl());
        output.getChild("username").setValue(connection.getUsername());
        output.getChild("password").setValue(connection.getPassword());
        for (Map.Entry<String, String> entry : connection.getProperties().entrySet()) {
            OutputNode property = output.getChild("property");
            property.getChild("name").setValue(entry.getKey());
            property.getChild("value").setValue(entry.getValue());
        }
        return true;
    }

    @Override
    protected void read(InputNode input, DriverManagerConnectionSpec connection, XmlReadContext context) throws Exception {
        connection.setId(get(input, ID_ATTRIBUTE));
        connection.setType(get(input, TYPE_ATTRIBUTE));
        // TODO: implement
    }
}
