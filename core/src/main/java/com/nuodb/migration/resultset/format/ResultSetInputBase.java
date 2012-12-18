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
package com.nuodb.migration.resultset.format;

import com.nuodb.migration.jdbc.model.ValueModel;
import com.nuodb.migration.jdbc.model.ValueModelFactory;
import com.nuodb.migration.jdbc.model.ValueModelList;
import com.nuodb.migration.jdbc.type.JdbcTypeDesc;
import com.nuodb.migration.jdbc.type.access.JdbcTypeValueAccess;
import com.nuodb.migration.resultset.format.jdbc.JdbcTypeValueFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.Reader;
import java.sql.PreparedStatement;

/**
 * @author Sergey Bushik
 */
@SuppressWarnings("unchecked")
public abstract class ResultSetInputBase extends ResultSetFormatBase implements ResultSetInput {

    private transient final Logger logger = LoggerFactory.getLogger(getClass());

    private Reader reader;
    private InputStream inputStream;
    private PreparedStatement preparedStatement;

    public Reader getReader() {
        return reader;
    }

    @Override
    public void setReader(Reader reader) {
        this.reader = reader;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    @Override
    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
        initInput();
    }

    protected abstract void initInput();

    public PreparedStatement getPreparedStatement() {
        return preparedStatement;
    }

    @Override
    public void setPreparedStatement(PreparedStatement preparedStatement) {
        this.preparedStatement = preparedStatement;
        initValueFormatModelList();
    }

    protected void initValueFormatModelList() {
        setValueFormatModelList(createValueFormatModelList());
    }

    protected ValueModelList<ValueFormatModel> createValueFormatModelList() {
        int index = 0;
        ValueFormatModel valueFormatModel;
        final ValueModelList<ValueFormatModel> valueFormatModelList = ValueModelFactory.createValueModelList();
        for (ValueModel valueModel : getValueModelList()) {
            visitValueFormatModel(valueFormatModel = createValueFormatModel(valueModel, index++));
            valueFormatModelList.add(valueFormatModel);
        }
        return valueFormatModelList;
    }

    protected ValueFormatModel createValueFormatModel(ValueModel valueModel, int index) {
        JdbcTypeDesc jdbcTypeDesc = new JdbcTypeDesc(valueModel.getTypeCode(), valueModel.getTypeName());
        JdbcTypeDesc jdbcTypeDescAlias = getJdbcTypeValueAccessProvider().getJdbcTypeDescAlias(jdbcTypeDesc);

        valueModel.setTypeCode(jdbcTypeDescAlias.getTypeCode());
        valueModel.setTypeName(jdbcTypeDescAlias.getTypeName());

        JdbcTypeValueFormat jdbcTypeValueFormat =
                getJdbcTypeValueFormatRegistry().getJdbcTypeValueFormat(jdbcTypeDescAlias);
        JdbcTypeValueAccess<Object> jdbcTypeValueAccess =
                getJdbcTypeValueAccessProvider().getPreparedStatementAccess(getPreparedStatement(), valueModel, index + 1);
        return new SimpleValueFormatModel(valueModel, jdbcTypeValueFormat, jdbcTypeValueAccess, null);
    }

    @Override
    public final void readBegin() {
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Read input %s", getClass().getName()));
        }
        doReadBegin();
    }

    protected abstract void doReadBegin();

    protected void setColumnValues(String[] values) {
        ValueModelList<ValueFormatModel> valueFormatModelList = getValueFormatModelList();
        for (int index = 0; index < values.length; index++) {
            ValueFormatModel columnValueFormatModel = valueFormatModelList.get(index);
            columnValueFormatModel.getValueFormat().setValue(
                    columnValueFormatModel.getValueAccess(), values[index], columnValueFormatModel.getValueAccessOptions());
        }
    }

    @Override
    public final void readEnd() {
        doReadEnd();
    }

    protected abstract void doReadEnd();
}
