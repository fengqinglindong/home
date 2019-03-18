# hive是什么？ #

hive 是基于hadoop的数据仓库。

对HDFS上的数据进行分析和管理。

将类SQL语句，HQL翻译成MR程序执行。

局限性：

不支持事物处理（删除和更新）。

延迟高（由于要解析成MR程序）

擅长：

非实时的、离线的、对响应实时性要求不高的海量数据离线计算。

比如：海量日志统计分析、海量结构化数据分析。


#  HIVE完全分布式集群安装过程 #

[http://www.aboutyun.com/thread-6902-1-1.html](http://www.aboutyun.com/thread-6902-1-1.html)

1. 用mysql创建hive用户，赋予权限并测试。
	- mysql -u root -p
	- create user 'hive' identified by 'hive';
	- grant all privileges on *.* to 'hive' with grant option
	- mysql -u hive -p hive
2. 安装hive
	- 下载hive的tar包，并解压
	- 将mysql的jdbc驱动jar包copy到hive的lib目录下
	- 配置hive的环境变量
	- 配置hive的conf目录下的hive-env.sh
	- 配置hive的conf目录下的hive-site.xml
		- 增加 ConnectionURL
		- 增加ConnectiondrivelName
		- 增加ConnectionUsername
		- 增加ConnectionPassword
		- 启动hive

#  hive基本操作 #

## DDL操作 ##

