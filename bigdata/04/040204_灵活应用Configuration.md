## Job配置

	MR任务都必须由一个Configuration对象初始化，这个对象管理整个任务的参数。任务执行过程中的所有参数  
	都是从这个对象中获取的。

	Configuration是什么：
		* 它是一个 Key/Value结构，在此结构中管理所有参数，可以设置String类型、Boolear类型这些参数，  
		  也可以设置MR中所使用的类。
		* 这个参数是在job中被初始化，但可以在Map和Reduce端获取到这些参数。因为Configuration也是一个  
		  Writable对象，可序列化，并且传输到各个节点上。
		* Job上配置，也就是客户端进行配置，MR使用。
		* Map和Reduce上不能传参，如果我们在Map端将Configuration获取并且set进参数，在Reduce端无法  
		  获取到参数。

## Configuration的加载顺序

	new Configuration()时发生什么？

		* 首先加载默认配置文件。 *-default.xml。
		* 加载site级别配置文件。*-site.xml（里面有Core-site，hdfs-site，mr-site）。
		* 加载Configuration对象的set方法设置的参数。一般在程序运行时进行set。
		* 自定义配置文件，最后加载。

		（注意：MR的Configuration中有两类参数：可覆盖，不可覆盖。这些配置文件的参数中，如果有相同  
		  的参数后加载的配置覆盖前加载的配置）

	对于自定义的配置文件放在classpath指定目录下（一般放在工程的Resource目录下），一般打包在Jar包中

## 分布式缓存

	hadoop提供了将配置文件从job端传输到Map和Reduce端，用户自定义的文件也可以这样传输，这就叫分布式缓存。

	当用户启动一个作业，Hadoop会把-files -archives 和 -libjars 上传到HDFS（这些都是命令行中的  
	参数），然后再将文件复制到各个执行节点上。Java代码中可以使用addcashfile这个方法，将文件上传到  
	分布式的缓存上，在各个执行节点上就可以通过文件名访问这些文件了。


## 代码

[mr.xml](./040204_mr.xml)  
[ParseLogJob.java](./040204_ParseLogJob.java)