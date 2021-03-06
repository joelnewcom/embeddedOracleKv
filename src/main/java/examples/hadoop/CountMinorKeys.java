/*-
 * Copyright (C) 2011, 2019 Oracle and/or its affiliates. All rights reserved.
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

package hadoop;

import java.io.IOException;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.mapreduce.lib.reduce.IntSumReducer;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import oracle.kv.Key;
import oracle.kv.hadoop.KVInputFormat;

/**
 * A simple example demonstrating how to use the Oracle NoSQL DB Hadoop
 * oracle.kv.hadoop.KVInputFormat class to read data from NoSQL Database in a
 * Map/Reduce job and count the number of records for each major key in the
 * store.
 *
 * The map() function is passed the Key and Value for each record in the KV
 * Store and outputs k/v pairs containing the major path components as the
 * output key and a value of 1.  The reduce step sums the values for each of
 * the records with the same key.  This M/R task is similar to the ubiquitous
 * Hadoop Map/Reduce WordCount example.
 *
 * The KV Keys passed to the Map function are in the canonical format described
 * in the javadoc for the oracle.kv.Key.toString() method.
 *
 * The KVInputFormat and related classes are located in the lib/kvclient.jar
 * file so this must be included in the Hadoop classpath at runtime.
 *
 * The arguments to the program are the kvstore name, the helperHost:port pair,
 * the HDFS output path and optionally, the login file path.
 *
 * For example, if you build this class (and its subclasses) and put it into
 * myjar.jar, you can invoke with a command similar to this:

 * <pre>
 * export HADOOP_CLASSPATH=...:KVHOME/lib/kvclient.jar
 * export LIBJARS=KVHOME/lib/kvclient.jar,KVHOME/lib/sklogger.jar,&#92;
 * KVHOME/lib/commonutil.jar,KVHOME/lib/jackson-databind.jar,&#92;
 * KVHOME/lib/jackson-core.jar,KVHOME/lib/jackson-annotations.jar
 *
 * bin/hadoop jar myjar.jar hadoop.CountMinorKeys &#92;
 *            -libjars ${LIBJARS} &#92;
 *            mystore myhost:myport /myHDFSoutputdir [mySecurityFilePath]
 * </pre>
 *
 * If you are accessing a secured KV Store using Oracle Wallet, additional
 * Oracle PKI jars obtained from EE package need to be added to the class
 * path. The local login file path needs to be specified as the program
 * argument. See example below:
 *
 * <pre>
 * export HADOOP_CLASSPATH=...:KVEEHOME/lib/kvclient.jar:&#92;
 * KVEEHOME/lib/oraclepki.jar
 * export LIBJARS=KVEEHOME/lib/kvclient.jar,KVEEHOME/lib/oraclepki.jar,&#92;
 * KVEEHOME/lib/osdt_cert.jar,KVEEHOME/lib/osdt_core.jar,&#92;
 * KVEEHOME/lib/sklogger.jar,KVEEHOME/lib/commonutil.jar,&#92;
 * KVEEHOME/lib/jackson-databind.jar,KVEEHOME/lib/jackson-core.jar,&#92;
 * KVEEHOME/lib/jackson-annotations.jar
 *
 * bin/hadoop jar myjar.jar hadoop.CountMinorKeys &#92;
 *            -libjars ${LIBJARS} &#92;
 *            mystore myhost:myport /myHDFSoutputdir mySecurityFilePath
 * </pre>
 */
public class CountMinorKeys extends Configured implements Tool {

    public static class Map
        extends Mapper<Text, Text, Text, IntWritable> {

        private Text word = new Text();
        private final static IntWritable one = new IntWritable(1);

        @Override
        public void map(Text keyArg, Text valueArg, Context context)
            throws IOException, InterruptedException {

            /*
             * keyArg is in the NoSQL Databse canonical Key format described in
             * the Key.toString() method's javadoc.
             *
             * The Output is the NoSQL Database record's Major Key as the
             * Map/Reduce key and 1 as the Map/Reduce value.
             */
            Key key = Key.fromString(keyArg.toString());
            /* Convert back to canonical format, but only use the major path. */
            word.set(Key.createKey(key.getMajorPath()).toString());
            context.write(word, one);
        }
    }

    public static class Reduce
        extends IntSumReducer<Text> {
    }

    @Override
    public int run(String[] args)
        throws Exception {

        @SuppressWarnings("deprecation")
        Job job = new Job(getConf());
        job.setJarByClass(CountMinorKeys.class);
        job.setJobName("Count Minor Keys");

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        job.setMapperClass(Map.class);
        job.setReducerClass(Reduce.class);

        job.setInputFormatClass(KVInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);

        KVInputFormat.setKVStoreName(args[0]);
        KVInputFormat.setKVHelperHosts(new String[] { args[1] });
        FileOutputFormat.setOutputPath(job, new Path(args[2]));

        /*
         * Load KVLoginFile if specified, otherwise try to load via reading
         * system property of oracle.kv.login.
         */
        if (args.length >= 4) {
            KVInputFormat.setKVSecurity(args[3]);
        }

        boolean success = job.waitForCompletion(true);
        return success ? 0 : 1;
    }

    public static void main(String[] args)
        throws Exception {

        int ret = ToolRunner.run(new CountMinorKeys(), args);
        System.exit(ret);
    }
}
