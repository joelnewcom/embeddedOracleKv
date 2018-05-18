/*-
 * Copyright (C) 2011, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This file was distributed by Oracle as part of a version of Oracle NoSQL
 * Database made available at:
 *
 * http://www.oracle.com/technetwork/database/database-technologies/nosqldb/downloads/index.html
 *
 * Please see the LICENSE file included in the top-level directory of the
 * appropriate version of Oracle NoSQL Database for a copy of the license and
 * additional information.
 */
package oracle.kv.impl.query.shell.output;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;

import oracle.kv.StatementResult;
import oracle.kv.impl.query.shell.OnqlShell.OutputMode;
import oracle.kv.table.FieldValue;
import oracle.kv.table.RecordDef;
import oracle.kv.table.RecordValue;
import oracle.kv.util.shell.Shell;
import oracle.kv.util.shell.ShellException;

/**
 * Returns a ResultOutput object for a specific OutputMode
 */
public class ResultOutputFactory {
    public static ResultOutput getOutput(final OutputMode mode,
                                         final Shell shell,
                                         final PrintStream output,
                                         final StatementResult result,
                                         final boolean pagingEnabled,
                                         final int pageHeight) {
        switch (mode) {
            case COLUMN:
                return new ColumnOutput(shell, output, result,
                                        pagingEnabled, pageHeight);
            case LINE:
                return new LineOutput(shell, output, result,
                                      pagingEnabled, pageHeight);
            case JSON:
                return new JSONOutput(shell, output, result,
                                      pagingEnabled, pageHeight);
            case JSON_PRETTY:
                return new JSONOutput(shell, output, result,
                                      pagingEnabled, pageHeight, true);
            case CSV:
                return new CSVOutput(shell, output, result,
                                     pagingEnabled, pageHeight);
            default:
                break;
        }
        return null;
    }

    /**
     * An abstract ResultOutput class.
     */
    public static abstract class ResultOutput {
        public final static String NULL_STRING = "NULL";
        private final static int MAX_LINES = 100;

        /* The Shell instance used to get the input from console */
        private final Shell shell;

        /* The output PrintStream */
        private final PrintStream output;

        /* The flag indicates if the paging is enabled or not */
        private final boolean pagingEnabled;

        /* The max lines per batch for output */
        private final int batchLines;

        /* The total number of records displayed */
        private long numRecords;

        /* The recodeDef of record in result */
        final RecordDef recordDef;

        /* The TableIterator over the records in this result */
        Iterator<RecordValue> resultIterator;

        ResultOutput(final Shell shell,
                     final PrintStream output,
                     final StatementResult statementResult,
                     final boolean pagingEnabled,
                     final int pageHeight) {

            this.shell = shell;
            this.recordDef = statementResult.getResultDef();
            this.resultIterator = statementResult.iterator();
            this.output = output;
            this.pagingEnabled = pagingEnabled;
            batchLines = pagingEnabled ? pageHeight : MAX_LINES;
            numRecords = 0;
        }

        /**
         * Abstract method to output records.
         */
        abstract long outputRecords(long maxLines, boolean paging)
            throws ShellException;

        /**
         * Displays the result set of the given StatementResult.
         */
        public long outputResultSet()
            throws ShellException {

            final String fmt = "--More--(%d~%d)\n";
            final int maxLines = (batchLines > 3) ? batchLines - 3 : batchLines;
            boolean hasMore = resultIterator.hasNext();
            while (hasMore) {
                final long nRows = outputRecords(maxLines, pagingEnabled);
                final long prev = numRecords;
                numRecords += nRows;
                hasMore = resultIterator.hasNext();
                if (!hasMore) {
                    break;
                }
                if (!pagingEnabled) {
                    continue;
                }
                final String msg = String.format(fmt, (prev + 1), numRecords);
                output(msg);
                try {
                    final String in = shell.getInput().readLine("");
                    if (in.toLowerCase().startsWith("q")) {
                        break;
                    }
                } catch (IOException e) {
                    throw new ShellException("Exception reading input");
                }
            }
            return numRecords;
        }

        /**
         * Outputs string
         */
        void output(String string) {
            output.print(string);
            output.flush();
        }

        /**
         * Returns the total records displayed
         */
        long getNumRecords() {
            return numRecords;
        }

        /**
         * Returns the string to display for null value
         */
        String getNullString() {
            return NULL_STRING;
        }

        /**
         * Returns the string value of each field value.
         */
        String getStringValue(final FieldValue val) {

            if (val.isNull()) {
                return getNullString();
            }
            if (!val.isComplex()) {
                return val.toString();
            }

            throw new IllegalArgumentException("The type is not supported by " +
                "this function : " + val.getDefinition().getType().name());
        }
    }
}
