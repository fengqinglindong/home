## 配置与安装 ##

依赖于Java和hadoop client，安装前需要安装这些。

- 下载jdk tgz包，解压，验证Java命令是否生效
- 从hadoop集群中拷贝hadoop client，在hadoop-env.sh配置JAVA_HOME，提交mapreduce任务进行测试。

## Sqoop包结构 ##

	.  
	├── bin                     - sqoop命令文件目录  
	├── build.xml         
	├── CHANGELOG.txt  
	├── COMPILING.txt  
	├── conf                    - 配置文件路径   
	├── docs  
	├── ivy  
	├── ivy.xml  
	├── lib                     - jar包依赖目录  
	├── LICENSE.txt  
	├── NOTICE.txt  
	├── pom-old.xml  
	├── README.txt  
	├── sqoop-1.4.7.jar         - sqoop核心jar包  
	├── sqoop-patch-review.py     
	├── sqoop-test-1.4.7.jar    - sqoop核心jar包  
	├── src  
	└── testdata  

Sqoop安装步骤：  
1. 下载Sqoop tgz：[下载路径](http://www.apache.org/dyn/closer.lua/sqoop/1.4.7)  
2. 解压修改conf/sqoop-env.sh文件，添加Java和hadoop配置。

	export JAVA_HOME=/home/hadoop/jdk1.7.0_79/
	export HADOOP_PREFIX="/home/hadoop/hadoop-current/"
	export HADOOP_HOME=${HADOOP_PREFIX}
	export PATH=$PATH:$HADOOP_PREFIX/bin:$HADOOP_PREFIX/sbin
	export HADOOP_COMMON_HOME=${HADOOP_PREFIX}
	export HADOOP_HDFS_HOME=${HADOOP_PREFIX}
	export HADOOP_MAPRED_HOME=${HADOOP_PREFIX}

