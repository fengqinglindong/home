	package com.bigdata.etl.mr;
	
	import org.apache.hadoop.conf.Configuration;
	import org.apache.hadoop.fs.FSDataOutputStream;
	import org.apache.hadoop.fs.Path;
	import org.apache.hadoop.io.compress.CompressionCodec;
	import org.apache.hadoop.io.compress.GzipCodec;
	import org.apache.hadoop.mapreduce.OutputCommitter;
	import org.apache.hadoop.mapreduce.RecordWriter;
	import org.apache.hadoop.mapreduce.TaskAttemptContext;
	import org.apache.hadoop.mapreduce.TaskID;
	import org.apache.hadoop.mapreduce.lib.output.FileOutputCommitter;
	import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
	import org.apache.hadoop.util.ReflectionUtils;
	
	import java.io.DataOutputStream;
	import java.io.IOException;
	import java.text.NumberFormat;
	import java.util.HashMap;
	import java.util.Iterator;
	import java.util.Map;
	
	//自定义OutputFormat完成多路输出功能,我们不需要从头编写如何读取文件这些操作所有继承TextOutputFormat，就可以使用TextOutputFormat的LineRecordWriter方法。
	//补充泛型参数
	public class LogOutputFormat<K,V> extends TextOutputFormat<K,V> {
	    //在MR的默认情况下会将taskID进行format，会将ID扩充成五位数,进行模仿
	    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getInstance();
	    static{
	        NUMBER_FORMAT.setMinimumIntegerDigits(5);
	        NUMBER_FORMAT.setGroupingUsed(false);
	    }
	    //存一个RecordWriter
	    private RecordWriter writer = null;
	    //重写RecordWriter方法,传入TaskAttemptContext这样一个类，这个类包含运行时的一些状态
	    public RecordWriter<K,V> getRecordWriter(TaskAttemptContext job) throws IOException,InterruptedException {
	        if (writer == null){
	            //自定义的RecoreWriter
	            writer = new MultiRecordWriter(job,getTaskOutputPath(job));
	        }
	        return writer;
	    }
	    //获取输出路径.指定输出目录，通过baseName和压缩格式拼接得到文件名，但输出目录不知道。MR程序的输出目录是由OutputCommitter决定的，
	    // 如何获取OutputCommitter呢？通过OutputFormat中的getOutputCommitter来获取的。
	    // 所有要在OutputFormat中获取OutputCommitter，然后从OutputCommitter获取输出路径。
	    private Path getTaskOutputPath(TaskAttemptContext conf) throws IOException{
	        Path taskOutputpath;
	        //通过调用TextOutputformat中的getOutputCommitter获取OutputCommitter
	        OutputCommitter committer = getOutputCommitter(conf);
	        if (committer instanceof FileOutputCommitter){
	            //获取输出路径
	            taskOutputpath = ((FileOutputCommitter) committer).getWorkPath();
	        }else {
	            Path outputPath = getOutputPath(conf);
	            if (outputPath == null){
	                throw new IOException("Undefined job output path");
	            }
	            taskOutputpath = outputPath;
	        }
	        return taskOutputpath;
	    }
	
	    //我们的数据要根据不同的key值写入到不同的文件中去的，而每一个LineRecordWriter都会绑定一个文件，所有需要在MultirecordWriter中存储一个hashMap，
	    // 里面存储这各个绑定好的LineRecordWriter
	    public class MultiRecordWriter extends RecordWriter<K,V>{
	        //指定类型使用超类Map
	        private Map<String ,RecordWriter<K,V>> recordWriters;
	        //将job存储，方便后续使用
	        private TaskAttemptContext job;
	        private Path outputPath;
	        //在构造方法中将recordWriters初始化，也就是new出来，将构造方法传入的参数赋值给job
	        private MultiRecordWriter(TaskAttemptContext job,Path outputPath){
	            super();
	            this.job = job;
	            this.outputPath = outputPath;
	            this.recordWriters = new HashMap<String, RecordWriter<K, V>>();
	        }
	        //拼接key值和ID的方法
	        private String getFileBaseName(K key,String name){
	            return new StringBuilder(60).append(key.toString()).append("_").append(name).toString();
	        }
	        //实现RecordWriter的两个方法
	        //在write方法中我们可以获取到这条数据的key值，可以根据key值生成文件名，然后将相应的文件名对应的LineRecordWriter取出来，最后使用LineRecordWriter将这条
	        //写到相应的文件中去。
	        public void write(K key, V value) throws IOException, InterruptedException {
	            //不同机器上的reduce都会将文件输出到同一个目录上面，所有文件名不可以重复，可以根据job给我们分配的TaskID来区分这些文件。
	            TaskID taskID = job.getTaskAttemptID().getTaskID();
	            //TaskAttemptID，因为MR程序在运行过程中，如果出错，都会重试，每次重试都会将ID自增，我们获取到这次重试的任务，并且获取到当前任务的ID，这样就
	            // 可以获取到partition编号,将编号写入到文件名中，就可以保证文件不重复
	            int partition = taskID.getId();//在MR的默认情况下会将taskID进行format，会将ID扩充成五位数。
	            //将编号和key值组合起来变成文件名,这样就实现了根据不同key值将不同数据写入到不同文件中去。
	            String baseName = getFileBaseName(key,NUMBER_FORMAT.format(partition));
	            //通过基本名称从recordWriters中取对应的recordWriter。
	            RecordWriter<K,V> rw = this.recordWriters.get(baseName);
	            if(rw == null){
	                //新new一个Reco出来，将它封装到一个函数中
	                rw = getBaseRecordWriter(job,baseName);
	                //将这个RecordWriter写入到HashMap中去
	                this.recordWriters.put(baseName,rw);
	            }
	            //将Key/Value值写进去，如果想将key值写入进去，也可以将key放进去。53行的key
	            rw.write(null,value);
	        }
	
	        public void close(TaskAttemptContext taskAttemptContext) throws IOException, InterruptedException {
	           //在关闭RecordWriter时，要将HashMap中的所有RecordWriter都关闭，所有需要遍历。
	            Iterator<RecordWriter<K,V>> values = this.recordWriters.values().iterator();
	            while(values.hasNext()){
	                values.next().close(taskAttemptContext);
	            }
	            //清空HashMap
	            this.recordWriters.clear();
	        }
	
	        private RecordWriter<K,V> getBaseRecordWriter(TaskAttemptContext job,String baseName) throws IOException,InterruptedException{
	
	            RecordWriter<K,V> recordWriter;
	            //判断写出来的文件是否是压缩格式的。通过getCompressOutput方法获取是否是压缩格式的
	            boolean isCompressed = getCompressOutput(job);
	            Configuration conf = job.getConfiguration();
	            if (isCompressed){
	                //获取压缩格式,如果获取不到采用Gzip压缩，返回一个压缩类型的class
	                Class<? extends CompressionCodec> codecClass = getOutputCompressorClass(job, GzipCodec.class);
	                //使用反射将class new出来
	                CompressionCodec codec = ReflectionUtils.newInstance(codecClass,conf);
	                Path file = new Path(outputPath,baseName + codec.getDefaultExtension());
	                FSDataOutputStream fileOut = file.getFileSystem(conf).create(file,false);
	                recordWriter = new LineRecordWriter<K, V>(new DataOutputStream(codec.createOutputStream(fileOut)));
	            }else{
	                Path file = new Path(outputPath,baseName);
	                FSDataOutputStream fileOut = file.getFileSystem(conf).create(file,false);
	                recordWriter = new LineRecordWriter<K, V>(fileOut);
	            }
	            return recordWriter;
	        }
	    }
	}
