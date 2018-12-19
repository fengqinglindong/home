	package com.bigdata.etl.mr;
	
	import org.apache.hadoop.io.Writable;
	import org.apache.hadoop.mapreduce.Partitioner;
	
	//分组
	public class TextLongPartitioner extends Partitioner<TextLongWritable, Writable> {
	    //对key值进行分组，也就是对TextLongWritable中的Text
	    public int getPartition(TextLongWritable textLongWritable, Writable writable, int numPartitions) {
	       int hash = textLongWritable.getText().hashCode();
	        //模仿hashPartitioner中的方法将hash值进行取模运算，这样保证数据分配的均匀
	        return (hash & Integer.MAX_VALUE)% numPartitions;
	    }
	}
