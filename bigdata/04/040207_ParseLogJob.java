	package com.bigdata.etl.job;
	
	import com.alibaba.fastjson.JSON;
	import com.alibaba.fastjson.JSONArray;
	import com.alibaba.fastjson.JSONObject;
	import com.bigdata.etl.mr.*;
	import com.bigdata.etl.utils.IPUtil;
	import org.apache.commons.lang.StringUtils;
	import org.apache.hadoop.conf.Configuration;
	import org.apache.hadoop.conf.Configured;
	import org.apache.hadoop.fs.FileSystem;
	import org.apache.hadoop.fs.Path;
	import org.apache.hadoop.io.LongWritable;
	import org.apache.hadoop.io.NullWritable;
	import org.apache.hadoop.io.Text;
	
	import org.apache.hadoop.mapreduce.Job;
	import org.apache.hadoop.mapreduce.Mapper;
	import org.apache.hadoop.mapreduce.Reducer;
	import org.apache.hadoop.mapreduce.lib.input.CombineTextInputFormat;
	import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
	import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
	import org.apache.hadoop.util.Tool;
	import org.apache.hadoop.util.ToolRunner;
	
	import java.io.IOException;
	import java.net.URI;
	import java.text.ParseException;
	import java.text.SimpleDateFormat;
	import java.util.Map;
	
	public class ParseLogJob extends Configured implements Tool {
	    public static LogGenericWritable parseLog(String row) throws Exception {
	        String[] logPart = StringUtils.split(row,"\u1111");
	        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS" );
	        long timeTag = dateFormat.parse(logPart[0]).getTime();
	        String activeName = logPart[1];
	        JSONObject bizData = JSON.parseObject(logPart[2]);
	
	        LogGenericWritable logData = new LogWritable();
	        logData.put("time_tag",new LogFieldWritable(timeTag));
	        logData.put("active_name",new LogFieldWritable(activeName));
	
	        for (Map.Entry<String,Object> entry: bizData.entrySet()){
	            logData.put(entry.getKey(),new LogFieldWritable(entry.getValue()));
	        }
	        return logData;
	    }
	
	    public static class LogWritable extends LogGenericWritable{
	        protected String[] getFieldName() {
	            return new String[]{"active_name","session_id","time_tag","ip","device_id","req_url","user_id","product_id","order_id","error_flag","error_log"};
	        }
	    }
	
	    public static class LogMapper extends Mapper<LongWritable, Text, TextLongWritable ,LogGenericWritable> {
	        public void map(LongWritable key,Text value,Context context) throws IOException,InterruptedException {
	            try {
	                LogGenericWritable parsedLog = parseLog(value.toString());
	                String session = (String) parsedLog.getObject("session_id");
	                Long timeTag = (Long) parsedLog.getObject("time_tag");
	
	                TextLongWritable outKey = new TextLongWritable();
	                outKey.setText(new Text(session));
	                outKey.setCompareValue(new LongWritable(timeTag));
	
	                context.write(outKey,parsedLog);
	            } catch (Exception e) {//注意这里捕获异常的超类，这样就可以把所有的异常捕获
	                //将异常的标识，异常的日志，整条的丢给reduce端，同样要丢TextLongWritable类型的key值，和LogGenericWritable类型的value值
	                LogGenericWritable v = new LogWritable();
	                //错误标记
	                v.put("error_flag",new LogFieldWritable("error"));
	                //错误日志
	                v.put("error_log",new LogFieldWritable(value));
	                //错误日志key值，在错误日志量比较大时，将错误日志尽可能的平均分配到每个reduce上。所有key值可以取一个随机数。
	                TextLongWritable outKey = new TextLongWritable();
	                int randomKey =(int) Math.random()*100;
	                outKey.setText(new Text("error" + randomKey));
	                context.write(outKey,v);
	            }
	        }
	    }
	
	    public static class LogReducer extends Reducer<TextLongWritable,LogGenericWritable,Text,Text>{
	      private Text sessionId;
	       private JSONArray actionPath = new JSONArray();
	
	       public void setup(Context context) throws IOException,InterruptedException{
	//           Configuration conf = context.getConfiguration();
	//           FileSystem fs = FileSystem.get(conf);
	//           Path ipFile = new Path(conf.get("ip.file.path"));
	//          // Path ipFile = new Path("/user/hadoop/lib/17monipdb.dat");
	//           Path localPath = new Path(this.getClass().getResource("/").getPath());
	//           fs.copyToLocalFile(ipFile,localPath)
	           IPUtil.load("17monipdb.dat");
	       }
	
	        public void reduce(TextLongWritable key,Iterable<LogGenericWritable> values,Context context) throws IOException,InterruptedException{
	           Text sid = key.getText();
	           if (sessionId == null || !sid.equals(sessionId)){
	               sessionId = new Text(sid);
	               actionPath.clear();
	           }
	           for(LogGenericWritable v : values){
	               JSONObject datum = JSON.parseObject(v.asJsonString());
	               //判断是否为异常日志，是不需要
	               if (v.getObject("error_flag") == null){
	                   String ip = (String) v.getObject("ip");
	                   String [] address = IPUtil.find(ip);
	                   JSONObject addr = new JSONObject();
	                   addr.put("country",address[0]);
	                   addr.put("province",address[1]);
	                   addr.put("city",address[2]);
	
	                   String activeName =(String) v.getObject("active_name");
	                   String reqUrl = (String) v.getObject("req_url");
	                   String PathUnit = "pageview".equals(activeName) ? reqUrl:activeName;
	                   actionPath.add(PathUnit);
	
	                   datum.put("address",addr);
	                   datum.put("action_path",actionPath);
	               }
	               //在LogOutputFormat中我们将baseName(就是指定的key值)与path拼接起来的，如果我们指定的key值中间带有斜杠，hdfs会帮我们自动建一个目录
	               String outputKey = v.getObject("error_flag") == null ? "part" : "error/part";
	                context.write(new Text(outputKey),new Text(datum.toJSONString()));
	            }
	        }
	    }
	
	    public int run(String[] args) throws Exception {
	        Configuration config = getConf();
	       // config.set("ip.file.path",args[2]);
	        config.addResource("mr.xml" );
	        Job job = Job.getInstance(config);
	        job.setJarByClass(ParseLogJob.class);
	        job.setJobName("parselog");
	        job.setMapperClass(LogMapper.class);
	        job.setReducerClass(LogReducer.class);
	        job.setMapOutputKeyClass(TextLongWritable.class);
	        job.setGroupingComparatorClass(TextLongGroupComparator.class);
	        job.setPartitionerClass(TextLongPartitioner.class);
	        job.setMapOutputValueClass(LogWritable.class);
	        job.setOutputValueClass(Text.class);
	        job.addCacheFile(new URI(config.get("ip.file.path")));
	
	        job.setInputFormatClass(CombineTextInputFormat.class);
	
	        job.setOutputFormatClass(LogOutputFormat.class);
	
	        FileInputFormat.addInputPath(job ,new Path(args[0]));
	        Path outputPath = new Path(args[1]);
	        FileOutputFormat.setOutputPath(job,outputPath);
	//        FileOutputFormat.setCompressOutput(job, true);
	//        FileOutputFormat.setOutputCompressorClass(job, LzopCodec.class);
	
	        FileSystem fs = FileSystem.get(config);
	        if(fs.exists(outputPath)){
	            fs.delete(outputPath,true);
	        }
	        if(!job.waitForCompletion(true)){
	            throw new RuntimeException(job.getJobName()+"failed!");
	        }
	        return 0;
	    }
	    public static void main(String[] args) throws Exception {
	        int res =ToolRunner.run(new Configuration(),new ParseLogJob(),args);
	        System.exit(res);
	    }
	}
