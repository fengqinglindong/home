## 完整的Mapper（Reducer）

	Mapper和Reducer类调用run方法，run方法分别调用setup，map和cleanup三个方法完成用户操作，run方法源码  
	如下：

	    public void run(Mapper<KEYIN, VALUEIN, KEYOUT, VALUEOUT>.Context context) throws IOException, InterruptedException {
	        this.setup(context);
	
	        try {
	            while(context.nextKeyValue()) {
	                this.map(context.getCurrentKey(), context.getCurrentValue(), context);
	            }
	        } finally {
	            this.cleanup(context);
	        }
    	}

		1. setup
		2. map/reduce
		3. cleanup

### Setup/cleanup方法

	setup方法在map方法执行之前，并且只执行一次，做如下操作：
		* 初始化资源操作，如：数据库连接，文件读取等。
		* 抽象出业务共同点，初始化好业务需要的变量。

	cleanup方法在map方法执行之后，在finally块中一定会执行，一般用来释放资源。

### 重写run方法

	为了业务需求，修改run方法的执行流程。如：map后的操作，或者对异常数据统一处理。
	
	    public void run(Mapper<KEYIN, VALUEIN, KEYOUT, VALUEOUT>.Context context) throws IOException, InterruptedException {
	        this.setup(context);
	        try {
	            while(context.nextKeyValue()) {
	                this.map(context.getCurrentKey(), context.getCurrentValue(), context);
	            }
				afterMap(context);
	        } catch(Exception e){
				handleException(context);
				throw e;
			}finally {
	            this.cleanup(context);
	        }
    	}


## IP解析

	案例需求：
		将IP解析成国家，省份，城市的具体地址

	IP地址库
		集群HDFS上的 /user/hadooop/lib/17monipdb.dat

	IP解析工具类
		https://gitlab.com/yktceshi/testdata/blob/master/IPUtil.java

## 代码

[IPUtil.java](./040203_IPUtil.java)

[ParseLogJob.java](./040203_ParseLogJob.java)