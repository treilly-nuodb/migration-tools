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
package com.nuodb.migrator.job;

import com.nuodb.migrator.jdbc.dialect.Dialect;
import com.nuodb.migrator.jdbc.dialect.IdentifierNormalizer;
import com.nuodb.migrator.jdbc.dialect.IdentifierQuoting;
import com.nuodb.migrator.jdbc.dialect.ImplicitDefaultsTranslator;
import com.nuodb.migrator.jdbc.dialect.TranslationManager;
import com.nuodb.migrator.jdbc.dialect.Translator;
import com.nuodb.migrator.jdbc.metadata.MetaDataType;
import com.nuodb.migrator.jdbc.metadata.generator.GroupScriptsBy;
import com.nuodb.migrator.jdbc.metadata.generator.ScriptGeneratorManager;
import com.nuodb.migrator.jdbc.metadata.generator.ScriptType;
import com.nuodb.migrator.jdbc.session.Session;
import com.nuodb.migrator.jdbc.type.JdbcTypeNameMap;
import com.nuodb.migrator.spec.ConnectionSpec;
import com.nuodb.migrator.spec.JdbcTypeSpec;
import com.nuodb.migrator.spec.ResourceSpec;
import com.nuodb.migrator.spec.SchemaGeneratorJobSpecBase;
import com.nuodb.migrator.utils.PriorityList;

import java.util.Collection;

import static com.nuodb.migrator.jdbc.JdbcUtils.close;
import static com.nuodb.migrator.jdbc.metadata.generator.HasTablesScriptGenerator.GROUP_SCRIPTS_BY;
import static com.nuodb.migrator.jdbc.resolve.DatabaseInfoUtils.NUODB;
import static com.nuodb.migrator.jdbc.type.JdbcTypeSpecifiers.newSpecifiers;

/**
 * @author Sergey Bushik
 */
public abstract class SchemaGeneratorJobBase<S extends SchemaGeneratorJobSpecBase> extends HasServicesJobBase<S> {

    private Session sourceSession;
    private Session targetSession;

    protected SchemaGeneratorJobBase() {
    }

    protected SchemaGeneratorJobBase(S jobSpec) {
        super(jobSpec);
    }

    protected ScriptGeneratorManager createScriptGeneratorManager() {
        ScriptGeneratorManager scriptGeneratorManager = new ScriptGeneratorManager();
        scriptGeneratorManager.getAttributes().put(GROUP_SCRIPTS_BY, getGroupScriptsBy());
        scriptGeneratorManager.setObjectTypes(getObjectTypes());
        scriptGeneratorManager.setScriptTypes(getScriptTypes());
        scriptGeneratorManager.setSourceCatalog(getSourceSpec().getCatalog());
        scriptGeneratorManager.setSourceSchema(getSourceSpec().getSchema());
        scriptGeneratorManager.setSourceSession(getSourceSession());

        ConnectionSpec targetSpec = getTargetSpec();
        if (targetSpec != null) {
            scriptGeneratorManager.setTargetCatalog(targetSpec.getCatalog());
            scriptGeneratorManager.setTargetSchema(targetSpec.getSchema());
        }

        Dialect dialect = createDialectResolver().resolve(NUODB);
        TranslationManager translationManager = dialect.getTranslationManager();
        PriorityList<Translator> translators = translationManager.getTranslators();
        for (Translator translator : translators) {
            if (translator instanceof ImplicitDefaultsTranslator) {
                ((ImplicitDefaultsTranslator)translator).setUseExplicitDefaults(isUseExplicitDefaults());
            }
        }
        JdbcTypeNameMap jdbcTypeNameMap = dialect.getJdbcTypeNameMap();
        for (JdbcTypeSpec jdbcTypeSpec : getJdbcTypeSpecs()) {
            jdbcTypeNameMap.addJdbcTypeName(
                    jdbcTypeSpec.getTypeCode(), newSpecifiers(
                    jdbcTypeSpec.getSize(), jdbcTypeSpec.getPrecision(), jdbcTypeSpec.getScale()),
                    jdbcTypeSpec.getTypeName()
            );
        }
        dialect.setIdentifierQuoting(getIdentifierQuoting());
        dialect.setIdentifierNormalizer(getIdentifierNormalizer());
        scriptGeneratorManager.setTargetDialect(dialect);
        return scriptGeneratorManager;
    }

    @Override
    public void release() throws Exception {
        close(getSourceSession());
        close(getTargetSession());
    }

    public Session getSourceSession() {
        return sourceSession;
    }

    public void setSourceSession(Session sourceSession) {
        this.sourceSession = sourceSession;
    }

    public Session getTargetSession() {
        return targetSession;
    }

    public void setTargetSession(Session targetSession) {
        this.targetSession = targetSession;
    }

    protected boolean isUseExplicitDefaults() {
        return getJobSpec().isUseExplicitDefaults();
    }

    protected GroupScriptsBy getGroupScriptsBy() {
        return getJobSpec().getGroupScriptsBy();
    }

    protected Collection<JdbcTypeSpec> getJdbcTypeSpecs() {
        return getJobSpec().getJdbcTypeSpecs();
    }

    protected IdentifierQuoting getIdentifierQuoting() {
        return getJobSpec().getIdentifierQuoting();
    }

    protected IdentifierNormalizer getIdentifierNormalizer() {
        return getJobSpec().getIdentifierNormalizer();
    }

    protected Collection<MetaDataType> getObjectTypes() {
        return getJobSpec().getObjectTypes();
    }

    protected ResourceSpec getOutputSpec() {
        return getJobSpec().getOutputSpec();
    }

    protected Collection<ScriptType> getScriptTypes() {
        return getJobSpec().getScriptTypes();
    }

    protected ConnectionSpec getSourceSpec() {
        return getJobSpec().getSourceSpec();
    }

    protected ConnectionSpec getTargetSpec() {
        return getJobSpec().getTargetSpec();
    }

    protected String[] getTableTypes() {
        return getJobSpec().getTableTypes();
    }
}
