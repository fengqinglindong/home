package com.bigdata.etl.job;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Counter;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;

public class CountUserIdJob extends Configured implements Tool {

    public static Text getUserId(String record) {
        String[] logs = record.split("\u1111");
        String jsonPart = logs[2];
        JSONObject logObj = JSON.parseObject(jsonPart);
        String userId = logObj.getString("user_id");

        return new Text(userId);
    }

    public static class CountUserIdMapper extends Mapper<LongWritable, Text, Text, LongWritable> {
        @Override
        protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {

            Counter errorCounter = context.getCounter("Log Error", "Parse Error");
            Counter logsCounter = context.getCounter("Logs count", "Parse logsCount");
            try {
                Text userId = getUserId(value.toString());
                logsCounter.increment(1);
                context.write(userId, new LongWritable(1));
            } catch (Exception e) {
                errorCounter.increment(1);
                context.write(new Text("error_log"), new LongWritable(1));
            }
        }
    }

    public static class CountUserIdReducer extends Reducer<Text, LongWritable, NullWritable, Text> {
        private Text currentUId;
        private long user_counts;
        private Counter userCounter;

        @Override
        protected void setup(Context context) throws IOException, InterruptedException {
            userCounter = context.getCounter("UserId count", "userCount");
        }
        @Override
        protected void reduce(Text key, Iterable<LongWritable> values, Context context) throws IOException, InterruptedException {
            if (key.toString().equals("error_log")) {
                return;
            }
            Text uid = new Text(key);
            if (currentUId == null || !currentUId.equals(uid)) {
                currentUId = uid;
                user_counts++;
                userCounter.increment(1);
            }
        }
        @Override
        protected void cleanup(Context context) throws IOException, InterruptedException {
            JSONObject res = new JSONObject();
            res.put("user_counts", user_counts);
            context.write(NullWritable.get(),new Text(res.toJSONString()));
        }
    }

    public int run(String[] args) throws Exception {
        Configuration conf = getConf();
        Job job = Job.getInstance(conf, "StatLog");
        job.setJarByClass(CountUserIdJob.class);

        // 输入 --> Map
        FileInputFormat.addInputPath(job, new Path(args[0]));
        job.setMapperClass(CountUserIdMapper.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(LongWritable.class);

        job.setReducerClass(CountUserIdReducer.class);
        job.setOutputValueClass(Text.class);
        job.setNumReduceTasks(1);

        Path outputPath = new Path(args[1]);
        FileOutputFormat.setOutputPath(job, outputPath);

        FileSystem fs = FileSystem.get(conf);
        if (fs.exists(outputPath)) {
            fs.delete(outputPath, true);
        }

        if (!job.waitForCompletion(true)) {
            throw new RuntimeException(job.getJobName() + " failed");
        }

        return 0;
    }

    public static void main(String[] args) throws Exception {
        int res = ToolRunner.run(new Configuration(), new CountUserIdJob(), args);
        System.exit(res);
    }
}
