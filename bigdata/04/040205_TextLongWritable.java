	package com.bigdata.etl.mr;
	
	import org.apache.hadoop.io.LongWritable;
	import org.apache.hadoop.io.Text;
	import org.apache.hadoop.io.WritableComparable;
	import org.apache.hadoop.io.WritableUtils;
	
	import java.io.DataInput;
	import java.io.DataOutput;
	import java.io.IOException;
	
	//这个类中放来个字段，一个用来放session，一个用来装TimeTag
	public class TextLongWritable implements WritableComparable<TextLongWritable> {
	    private Text text;
	    private LongWritable compareValue;
	
	    public TextLongWritable(){
	        this.text = new Text();
	        this.compareValue = new LongWritable();
	    }
	    //设置比较hashcode的方法
	    public int hashCode(){
	        final int prime = 31;
	        return this.text.hashCode()*prime + this.compareValue.hashCode();//如果这两个对象的hashCode是一样的，那么这两个字段一定是相同的
	    }
	    public Text getText() {
	        return text;
	    }
	
	    public void setText(Text text) {
	        this.text = text;
	    }
	
	    public LongWritable getCompareValue() {
	        return compareValue;
	    }
	
	    public void setCompareValue(LongWritable compareValue) {
	        this.compareValue = compareValue;
	    }
	
	    //排序过程中怎么进行比较的
	    public int compareTo(TextLongWritable o) {
	        //比较第一个字段Text
	        int result = this.text.compareTo(o.getText());
	        //如果第一个字段相关再比较第二个字段
	        if(result == 0){
	            result = this.compareValue.compareTo(o.getCompareValue());
	        }
	        return result;
	    }
	    //序列化
	    public void write(DataOutput out) throws IOException {
	        this.text.write(out);
	        WritableUtils.writeVLong(out,this.compareValue.get());
	    }
	    //反序列化
	    public void readFields(DataInput in) throws IOException {
	        this.text.readFields(in);
	        this.compareValue.set(WritableUtils.readVLong(in));
	    }
	}
