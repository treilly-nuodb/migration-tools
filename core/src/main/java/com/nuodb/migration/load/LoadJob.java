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
package com.nuodb.migration.load;

import com.google.common.collect.Lists;
import com.nuodb.migration.jdbc.connection.ConnectionProvider;
import com.nuodb.migration.jdbc.connection.ConnectionServices;
import com.nuodb.migration.jdbc.dialect.Dialect;
import com.nuodb.migration.jdbc.dialect.DialectResolver;
import com.nuodb.migration.jdbc.metadata.Database;
import com.nuodb.migration.jdbc.metadata.Table;
import com.nuodb.migration.jdbc.metadata.inspector.DatabaseInspector;
import com.nuodb.migration.jdbc.model.ValueModel;
import com.nuodb.migration.jdbc.model.ValueModelList;
import com.nuodb.migration.jdbc.query.*;
import com.nuodb.migration.jdbc.type.JdbcTypeRegistry;
import com.nuodb.migration.jdbc.type.access.JdbcTypeValueAccessProvider;
import com.nuodb.migration.job.JobBase;
import com.nuodb.migration.job.JobExecution;
import com.nuodb.migration.resultset.catalog.Catalog;
import com.nuodb.migration.resultset.catalog.CatalogEntry;
import com.nuodb.migration.resultset.catalog.CatalogReader;
import com.nuodb.migration.resultset.format.ResultSetFormatFactory;
import com.nuodb.migration.resultset.format.ResultSetInput;
import com.nuodb.migration.resultset.format.jdbc.JdbcTypeValueFormatRegistryResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import static com.google.common.io.Closeables.closeQuietly;
import static com.nuodb.migration.jdbc.JdbcUtils.close;
import static com.nuodb.migration.jdbc.metadata.MetaDataType.*;
import static com.nuodb.migration.utils.ValidationUtils.isNotNull;

/**
 * @author Sergey Bushik
 */
public class LoadJob extends JobBase {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    private ConnectionProvider connectionProvider;
    private TimeZone timeZone;
    private Map<String, String> attributes;
    private Catalog catalog;
    private DialectResolver dialectResolver;
    private ResultSetFormatFactory resultSetFormatFactory;
    private JdbcTypeValueFormatRegistryResolver jdbcTypeValueFormatRegistryResolver;

    @Override
    public void execute(JobExecution execution) throws Exception {
        validate();
        execute(new LoadJobExecution(execution));
    }

    protected void validate() {
        isNotNull(getCatalog(), "Catalog is required");
        isNotNull(getConnectionProvider(), "Connection provider is required");
        isNotNull(getDialectResolver(), "Database dialect resolver is required");
        isNotNull(getJdbcTypeValueFormatRegistryResolver(), "JDBC type value format registry resolver is required");
    }

    protected void execute(LoadJobExecution execution) throws SQLException {
        ConnectionServices connectionServices = getConnectionProvider().getConnectionServices();
        try {
            execution.setConnectionServices(connectionServices);
            load(execution);
        } finally {
            close(connectionServices);
        }
    }

    protected void load(LoadJobExecution execution) throws SQLException {
        ConnectionServices connectionServices = execution.getConnectionServices();

        DatabaseInspector databaseInspector = connectionServices.createDatabaseInspector();
        databaseInspector.withMetaDataTypes(CATALOG, SCHEMA, TABLE, COLUMN);
        databaseInspector.withDialectResolver(getDialectResolver());

        Database database = databaseInspector.inspect();
        execution.setDatabase(database);

        Connection connection = connectionServices.getConnection();
        DatabaseMetaData metaData = connection.getMetaData();
        execution.setJdbcTypeValueFormatRegistry(getJdbcTypeValueFormatRegistryResolver().resolveObject(metaData));

        CatalogReader catalogReader = getCatalog().getCatalogReader();
        execution.setCatalogReader(catalogReader);

        Dialect dialect = database.getDialect();
        try {
            if (dialect.supportsSessionTimeZone()) {
                dialect.setSessionTimeZone(connection, timeZone);
            }
            for (CatalogEntry catalogEntry : catalogReader.getEntries()) {
                load(execution, catalogEntry);
            }
            connection.commit();
        } finally {
            closeQuietly(catalogReader);

            if (dialect.supportsSessionTimeZone()) {
                dialect.setSessionTimeZone(connection, null);
            }
        }
    }

    protected void load(LoadJobExecution execution, CatalogEntry catalogEntry) throws SQLException {
        CatalogReader catalogReader = execution.getCatalogReader();
        InputStream entryInput = catalogReader.getEntryInput(catalogEntry);
        try {
            Dialect dialect = execution.getDatabase().getDialect();
            ResultSetInput resultSetInput = getResultSetFormatFactory().createInput(catalogEntry.getType());
            resultSetInput.setAttributes(getAttributes());
            if (!dialect.supportsSessionTimeZone() && dialect.supportsWithTimezone()) {
                resultSetInput.setTimeZone(getTimeZone());
            }
            resultSetInput.setInputStream(entryInput);
            resultSetInput.setJdbcTypeValueFormatRegistry(execution.getJdbcTypeValueFormatRegistry());

            JdbcTypeRegistry jdbcTypeRegistry = dialect.getJdbcTypeRegistry();
            resultSetInput.setJdbcTypeValueAccessProvider(new JdbcTypeValueAccessProvider(jdbcTypeRegistry));

            load(execution, resultSetInput, catalogEntry.getName());
        } finally {
            closeQuietly(entryInput);
        }
    }

    protected void load(final LoadJobExecution execution, final ResultSetInput resultSetInput,
                        String tableName) throws SQLException {
        resultSetInput.readBegin();
        ValueModelList<ValueModel> valueModelList = resultSetInput.getValueModelList();
        if (valueModelList.isEmpty()) {
            return;
        }
        Database database = execution.getDatabase();
        Table table = database.findTable(tableName);
        for (ValueModel valueModel : valueModelList) {
            valueModel.copy(table.getColumn(valueModel.getName()));
        }
        final InsertQuery query = createInsertQuery(table, valueModelList);

        StatementTemplate template = new StatementTemplate(execution.getConnectionServices().getConnection());
        template.execute(
                new StatementCreator<PreparedStatement>() {
                    @Override
                    public PreparedStatement create(Connection connection) throws SQLException {
                        return createStatement(connection, query);
                    }
                },
                new StatementCallback<PreparedStatement>() {
                    @Override
                    public void execute(PreparedStatement preparedStatement) throws SQLException {
                        load(execution, resultSetInput, preparedStatement);
                    }
                }
        );
    }

    protected PreparedStatement createStatement(Connection connection, Query query) throws SQLException {
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Prepare SQL: %s", query.toQuery()));
        }
        return connection.prepareStatement(query.toQuery());
    }

    protected void load(LoadJobExecution execution, ResultSetInput resultSetInput,
                        PreparedStatement preparedStatement) throws SQLException {
        resultSetInput.setPreparedStatement(preparedStatement);
        while (execution.isRunning() && resultSetInput.hasNextRow()) {
            resultSetInput.readRow();
            preparedStatement.executeUpdate();
        }
        resultSetInput.readEnd();
    }

    protected InsertQuery createInsertQuery(Table table, ValueModelList valueModelList) {
        InsertQueryBuilder builder = new InsertQueryBuilder();
        builder.setQualifyNames(true);
        builder.setTable(table);
        if (valueModelList != null) {
            List<String> columns = Lists.newArrayList();
            for (int index = 0, length = valueModelList.size(); index < length; index++) {
                columns.add(valueModelList.get(index).getName());
            }
            builder.setColumns(columns);
        }
        return builder.build();
    }

    public ConnectionProvider getConnectionProvider() {
        return connectionProvider;
    }

    public void setConnectionProvider(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public TimeZone getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public Catalog getCatalog() {
        return catalog;
    }

    public void setCatalog(Catalog catalog) {
        this.catalog = catalog;
    }

    public DialectResolver getDialectResolver() {
        return dialectResolver;
    }

    public void setDialectResolver(DialectResolver dialectResolver) {
        this.dialectResolver = dialectResolver;
    }

    public ResultSetFormatFactory getResultSetFormatFactory() {
        return resultSetFormatFactory;
    }

    public void setResultSetFormatFactory(ResultSetFormatFactory resultSetFormatFactory) {
        this.resultSetFormatFactory = resultSetFormatFactory;
    }

    public JdbcTypeValueFormatRegistryResolver getJdbcTypeValueFormatRegistryResolver() {
        return jdbcTypeValueFormatRegistryResolver;
    }

    public void setJdbcTypeValueFormatRegistryResolver(
            JdbcTypeValueFormatRegistryResolver jdbcTypeValueFormatRegistryResolver) {
        this.jdbcTypeValueFormatRegistryResolver = jdbcTypeValueFormatRegistryResolver;
    }
}
