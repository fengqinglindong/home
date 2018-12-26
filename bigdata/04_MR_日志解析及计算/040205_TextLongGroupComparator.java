	package com.bigdata.etl.mr;
	
	import org.apache.hadoop.io.WritableComparable;
	import org.apache.hadoop.io.WritableComparator;
	//在reduce端，需要对同一个session的数据放到一个迭代器中
	public class TextLongGroupComparator extends WritableComparator {
	    public TextLongGroupComparator(){
	        //设置需要比较的类
	        super(TextLongWritable.class,true);
	    }
	
	    //编写compare方法
	    public int compare(WritableComparable a,WritableComparable b){
	        TextLongWritable textLongA = (TextLongWritable) a;
	        TextLongWritable textLongB = (TextLongWritable) b;
	        return textLongA.getText().compareTo(textLongB.getText());
	    }
	}
