## 数据序列化

	在分布式的系统上运行程序，数据需要在机器间通过网络传输的。这些数据必须要编写成一个个的字节才能传输，   
	这就是所谓的序列化。

	数据中心中网络带宽是非常稀缺的，数据紧凑高效的传输与解析很重要。

	数据序列化：
		根据一套协议，将服务器或客户端内存中的数据编码成字节码，然后通过网络将这些字节码传输到另外一台服  
		务器上。另外一台服务器通过相同的协议将这些字节码翻译成相同的数据，存储在内存中。

		条件：紧凑、快速、可扩展、支持互操作。

## Writable接口

	hadoop中使用Writable进行序列化。

	public interface Writable {
		//将对象数据编码成字节
	    void write(DataOutput var1) throws IOException;
		//将字节反序列化成数据
	    void readFields(DataInput var1) throws IOException;
	}


## Writable类型
	
	因接口无具体实现，hadoop中实现了基本类型的序列化方法。String类型封装为Text类，以及对Array和Map集  
	合类型的Writable封装。

	Java基本类型 | Writable | 序列化后长度
	--- | --- | ---
	Boolean | BooleanWritable |1
	byte | ByteWritable |1
	int  | IntWritable/VIntwritable | 4/1~5
	float | FloatWritable |4
	long | LongWritable/VLongWritable |8/1~9
	double |DoubleWritable | 8

## 自定义Writable类型

	一般由基本类型，集合类型，引用类型组成，实现write与readFields方法。实现原则如下：
		* 基本类型可以用read{类型}，write{类型}（如：readInt）方法
		* String类型和可变长类型可以使用WritableUtils类辅助
		* 集合类型参考ArrayWritable。
		* 引用类型实现了Writable接口，直接调用引用类型的readFields和write方法。

## 通用Writable类型

	ObjectWritable：
		处理null、Java数组、字符串String、Java基本类型、枚举和Writable的子类型6种情况，但会造成资源  
		浪费，因为会将类名、实例序列化。

	GenericWritable：
		适用于类型的数量不是很多，而且事先可以知道，可以通过静态数组将所有类型列举出来，用数组下标代替  
		类名进行序列化。

## 文件压缩

	文件压缩可以减少磁盘的存储空间，加速网络上的传输速率。常用的与hadoop结合的压缩算法有以下几种：
		- Gzip 压缩效率 速度居中，不可切分
		- Bzip 压缩效率高 速度慢，可切分
		- LZO，LZ4，Snappy压缩效率不高，速度快，不可切分
			- LZO加索引可切分
			- Snappy和LZ4的解压缩速率比LZO高出很多。

## 文件分片
	文件分片对于MapReduce的执行效率十分重要。不分片会产生以下问题：
		* 一个Map只能处理一个文件，Map处理时间长。
		* 牺牲数据本地性，一个大文件的多个快存在不同的节点上，如果不分片，需要将所有的块通过网络传输到  
		一个Map上计算。
		
## 合理的压缩格式选型

	* 对于经常访问的数据，使用LZO+索引的方式存储
	* 对于不经常访问的数据，采用bzip2压缩存储
	* Map和Reduce中间的数据传输，采用Snappy压缩。

## 代码

[040201_ParseLogJob.java](./040201_ParseLogJob.java)

[040201_LogBeanWritable.java](./040201_LogBeanWritable.java)
