## 安装部署
	Flume版本
		* 0.9.x和1.x
		* 0.9.x统称flume og，1.x统称flume ng。

	版本： 1.8.0

	Java运行环境：
		jdk1.8及以上版本

flume的下载网站：  

[flume 官网](http://flume.apache.org/download.html)

	* 使用wget下载安装包
		* wget http://mirrors.shu.edu.cn/apache/flume/1.8.0/apache-flume-1.8.0-bin.tar.gz
	* 解压安装包
		* tar -zxvf apache-flume-1.8.0-bin.tar.gz
	* 文件夹改名
		* mv apache-flume-1.8.0-bin flume
	* 进入flume安装目录
		* bin
		* conf
			- 将flume-env.sh.template 重命名，flume启动的环境变量路径
				- mv flume-env.sh.template flume-env.sh
			- 将flume-conf.properties重命名，flume agent 启动是的各组件，包括source，channel，sink
				- mv flume-conf.properties.template flume-conf.properties
			- flume启动命令
				- ./bin/flume-ng agent --conf conf --conf-file conf/flume-conf.properties --name agent
			- 查看日志文件
				- sudo -iu hadop
				- cd LIMIN/flume/logs
				- less flume.log
		* lib