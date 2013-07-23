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
package com.nuodb.migrator.dump;

import com.nuodb.migrator.backup.catalog.*;
import com.nuodb.migrator.backup.format.FormatFactory;
import com.nuodb.migrator.backup.format.csv.CsvAttributes;
import com.nuodb.migrator.backup.format.value.ValueFormatRegistry;
import com.nuodb.migrator.jdbc.connection.ConnectionProvider;
import com.nuodb.migrator.jdbc.dialect.Dialect;
import com.nuodb.migrator.jdbc.dialect.QueryLimit;
import com.nuodb.migrator.jdbc.metadata.Column;
import com.nuodb.migrator.jdbc.metadata.Database;
import com.nuodb.migrator.jdbc.metadata.Table;
import com.nuodb.migrator.jdbc.query.Query;
import com.nuodb.migrator.jdbc.query.StatementCallback;
import com.nuodb.migrator.jdbc.session.Session;
import com.nuodb.migrator.jdbc.session.SessionFactory;
import com.nuodb.migrator.jdbc.session.Work;
import com.nuodb.migrator.jdbc.split.QuerySplit;
import com.nuodb.migrator.jdbc.split.QuerySplitter;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.ReentrantLock;

import static com.google.common.collect.Iterables.get;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.nuodb.migrator.jdbc.dialect.RowCountType.EXACT;
import static com.nuodb.migrator.jdbc.session.SessionFactories.newSessionFactory;
import static com.nuodb.migrator.jdbc.split.Queries.newQuery;
import static com.nuodb.migrator.jdbc.split.QuerySplitters.*;
import static com.nuodb.migrator.jdbc.split.RowCountHandlerStrategies.createCachingStrategy;
import static com.nuodb.migrator.jdbc.split.RowCountHandlerStrategies.createHandlerStrategy;
import static com.nuodb.migrator.utils.CollectionUtils.isEmpty;
import static java.lang.String.format;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author Sergey Bushik
 */
@SuppressWarnings({"unchecked", "ThrowableResultOfMethodCallIgnored"})
public class DumpWriter implements DumpQueryContext {

    private final transient Logger logger = getLogger(getClass());
    public static final int THREADS = Runtime.getRuntime().availableProcessors();

    private String format = CsvAttributes.FORMAT;
    private int threads = THREADS;
    private Dialect dialect;
    private Database database;
    private TimeZone timeZone;
    private CatalogManager catalogManager;
    private Connection connection;
    private ConnectionProvider connectionProvider;
    private FormatFactory formatFactory;
    private ValueFormatRegistry valueFormatRegistry;
    private Map<String, Object> formatAttributes = newHashMap();
    private QueryLimit queryLimit;

    private Collection<QueryInfo> queryInfos = newArrayList();

    public void addTable(Table table) {
        addTable(table, table.getColumns());
    }

    public void addTable(Table table, Collection<Column> columns) {
        addTable(table, columns, null);
    }

    public void addTable(Table table, Collection<Column> columns, String filter) {
        addTable(table, columns, filter, queryLimit);
    }

    /**
     * Adds a table with specified columns and filtering clause to the writing queue.
     *
     * @param table      to include into dump.
     * @param columns    only specified columns will be writing.
     * @param filter     to filter rows with.
     * @param queryLimit which will be used for creating "pages", if query limit is null no limiting query splitter will
     *                   be constructed.
     */
    public void addTable(Table table, Collection<Column> columns, String filter, QueryLimit queryLimit) {
        addQuery(createQueryDesc(table, columns, filter, queryLimit));
    }

    public void addQuery(String query) {
        addQuery(createQueryDesc(query, getQueryLimit()));
    }

    protected void addQuery(QueryInfo queryInfo) {
        queryInfos.add(queryInfo);
    }

    public Catalog write() throws Exception {
        DumpWriterContext dumpWriterContext = createDumpWriterContext();
        write(dumpWriterContext);
        return dumpWriterContext.getCatalog();
    }

    protected DumpWriterContext createDumpWriterContext() {
        DumpWriterContext dumpWriterContext = new DumpWriterContext();
        Catalog catalog = new Catalog();
        catalog.setFormat(getFormat());
        catalog.setDatabaseInfo(getDatabase().getDatabaseInfo());
        dumpWriterContext.setCatalog(catalog);

        dumpWriterContext.setExecutorService(createExecutorService());

        SessionFactory sessionFactory = newSessionFactory(getConnectionProvider(), getDialect());
        sessionFactory.addSessionObserver(new DumpQuerySessionObserver(this));
        dumpWriterContext.setSessionFactory(sessionFactory);
        return dumpWriterContext;
    }

    protected ExecutorService createExecutorService() {
        if (logger.isTraceEnabled()) {
            logger.trace(format("Constructed fixed thread pool with %d thread(s) to write dump", getThreads()));
        }
        return newFixedThreadPool(getThreads());
    }

    protected void write(DumpWriterContext dumpWriterContext) throws Exception {
        ExecutorService executorService = dumpWriterContext.getExecutorService();
        Catalog catalog = dumpWriterContext.getCatalog();
        try {
            RowSet rowSet;
            for (QueryInfo queryInfo : queryInfos) {
                catalog.addRowSet(rowSet = createRowSet(queryInfo));
                write(dumpWriterContext, queryInfo, createQuerySplitter(queryInfo), rowSet);
            }
            executorService.shutdown();
            while (!executorService.isTerminated()) {
                executorService.awaitTermination(100L, MILLISECONDS);
            }
        } catch (Exception exception) {
            executorService.shutdownNow();
            throw exception;
        } finally {
            errors(dumpWriterContext);
        }
        getCatalogManager().writeCatalog(catalog);
    }

    protected void write(final DumpWriterContext dumpWriterContext,
                         QueryInfo queryInfo, QuerySplitter querySplitter, RowSet rowSet) throws SQLException {
        while (querySplitter.hasNextQuerySplit(getConnection())) {
            QuerySplit querySplit = querySplitter.getNextQuerySplit(getConnection(), new StatementCallback() {
                @Override
                public void process(Statement statement) throws SQLException {
                    getDialect().setStreamResults(statement, true);
                }
            });
            write(dumpWriterContext, new DumpQuery(this, dumpWriterContext, queryInfo, querySplit,
                    querySplitter.hasNextQuerySplit(getConnection()), rowSet));
        }
    }

    protected void write(final DumpWriterContext dumpWriterContext, final DumpQuery dumpQuery) {
        dumpWriterContext.getExecutorService().submit(new Callable() {
            @Override
            public Object call() throws Exception {
                Session session = null;
                try {
                    session = dumpWriterContext.getSessionFactory().openSession();
                    session.execute(dumpQuery, dumpWriterContext);
                    return null;
                } finally {
                    if (session != null) {
                        session.close();
                    }
                }
            }
        });
    }

    protected void errors(DumpWriterContext dumpWriterContext) throws Exception {
        Map<Work, Exception> errors = dumpWriterContext.getErrors();
        if (!isEmpty(errors)) {
            throw get(errors.values(), 0);
        }
    }

    protected QuerySplitter createQuerySplitter(QueryInfo queryInfo) {
        QuerySplitter querySplitter;
        if (queryInfo instanceof TableQueryInfo) {
            TableQueryInfo tableQueryDesc = (TableQueryInfo) queryInfo;
            querySplitter = createQuerySplitter(tableQueryDesc.getTable(), tableQueryDesc.getColumns(),
                    tableQueryDesc.getFilter(), queryInfo.getQueryLimit());
        } else {
            querySplitter = createQuerySplitter(queryInfo.getQuery());
        }
        return querySplitter;
    }

    protected RowSet createRowSet(QueryInfo queryInfo) {
        RowSet rowSet;
        if (queryInfo instanceof TableQueryInfo) {
            rowSet = new TableRowSet(((TableQueryInfo) queryInfo).getTable());
        } else {
            rowSet = new QueryRowSet(queryInfo.getQuery().toString());
        }
        return rowSet;
    }

    protected QuerySplitter createQuerySplitter(Table table, Collection<Column> columns, String filter,
                                                QueryLimit queryLimit) {
        Query query = newQuery(table, columns, filter);
        QuerySplitter querySplitter;
        if (queryLimit != null && supportsLimitSplitter(getDialect(), table, filter)) {
            querySplitter = newLimitSplitter(getDialect(),
                    createCachingStrategy(new ReentrantLock(), createHandlerStrategy(
                            getDialect().createRowCountHandler(table, null, filter, EXACT))), query, queryLimit
            );
        } else {
            querySplitter = newNoLimitSplitter(query);
        }
        return querySplitter;
    }

    protected QueryInfo createQueryDesc(Table table, Collection<Column> columns, String filter,
                                        QueryLimit queryLimit) {
        return new TableQueryInfo(table, columns, filter, queryLimit);
    }

    protected QueryInfo createQueryDesc(String query, QueryLimit queryLimit) {
        return new QueryInfo(newQuery(query), queryLimit);
    }

    protected QuerySplitter createQuerySplitter(Query query) {
        return newNoLimitSplitter(query);
    }

    @Override
    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public int getThreads() {
        return threads;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }

    public Dialect getDialect() {
        return dialect;
    }

    public void setDialect(Dialect dialect) {
        this.dialect = dialect;
    }

    @Override
    public Database getDatabase() {
        return database;
    }

    public void setDatabase(Database database) {
        this.database = database;
    }

    @Override
    public TimeZone getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
    }

    @Override
    public CatalogManager getCatalogManager() {
        return catalogManager;
    }

    public void setCatalogManager(CatalogManager catalogManager) {
        this.catalogManager = catalogManager;
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public ConnectionProvider getConnectionProvider() {
        return connectionProvider;
    }

    public void setConnectionProvider(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public FormatFactory getFormatFactory() {
        return formatFactory;
    }

    public void setFormatFactory(FormatFactory formatFactory) {
        this.formatFactory = formatFactory;
    }

    @Override
    public ValueFormatRegistry getValueFormatRegistry() {
        return valueFormatRegistry;
    }

    public void setValueFormatRegistry(ValueFormatRegistry valueFormatRegistry) {
        this.valueFormatRegistry = valueFormatRegistry;
    }

    @Override
    public Map<String, Object> getFormatAttributes() {
        return formatAttributes;
    }

    public void setFormatAttributes(Map<String, Object> formatAttributes) {
        this.formatAttributes = formatAttributes;
    }

    public QueryLimit getQueryLimit() {
        return queryLimit;
    }

    public void setQueryLimit(QueryLimit queryLimit) {
        this.queryLimit = queryLimit;
    }
}
