## Azkaban插件的加载流程

- Executor在服务启动的时候会独立加载各个插件目录下的jar包
	- 插件根目录：azkaban.jobtype.plugin.dir=plugins/jobtypes
	- 根目录的一级目录就是插件目录，目录名就是插件名字
	- 每个插件都有独立的URLClassLoader来加载，其中Parent ClassLoader是加载executor服务lib的Class Loader。（ClassLoader的加载机制需了解）
	- URLClassLoader加载resource包括：插件目录下的jar、配置文件中指定的classpath目录下的文件。
	- 根据插件配置文件中制定的jobtyp.class，从URLClassLoader中拿到对应的class对象，缓存起来。
- 在具体job执行的时候，根据jobtype获取对应缓存的class对象，通过反射构建job对象执行对应job。

## Azkaban插件-参数配置

- Exec服务根据job的类型，构建对应的job对象
- Job执行时候传入的参数：jobid（job的名字）、sysProps（系统参数列表）、jobProps（运行时参数列表）、log（日志对象，会将接受到的日志内容appant到指定的日志中，然后打包上传）
	- sysProps为系统参数，由管理员控制，用户不可见、不可设置
	- jobProps为job运行时参数，用户可设置
	- sysProps配置文件：commonprivate.properties（公共插件变量）和${jobtype-name}/private.properties（具体某个插件配置变量）。后者优先级高于前者。
	- jobProps的配置文件：exec/conf/global.propertis、common.properties、${jobtype-name}/plugins.properties、项目中的.properties文件、flowParameter（执行时配置参数）、项目包中的.job文件。
	
![](https://i.imgur.com/UirtOyS.png)

Job实例根据传入的参数会进行安全性的验证，构建进程参数，然后拉起一个独立的java进程，在新的java进程中会调用用户的业务代码，运行业务逻辑。或者调用相关的大数据组件API，打包提交作业。

插件的部署除了部署jar，配置文件的部署也非常关键。

## Azkaban插件-公共系统参数
- Commonprivate.properties中常见的配置项
	- Hadoop安全性相关
		- 默认一套hadoop集群，若多套集群，布置多套Azkaban，一一对应
		- Hadoop集群可以通过kerbours认证来提高安全性
			- Kinit -kt {user}.keytab principal
				- keytab相当于密钥
				- principal相当于用户
		- hadoop.security.manager.class,hadoop安全性管理类
			- 如果是1.x版本配置：azkaban.security.HadoopSecurityManager_H_1_0
			- 2.x版本：azkaban.security.HadoopSecurityManager_H_2_0
		- 若hadoop开启了安全性验证，需配置proxy.keytab.loacation、proxy.user
			- proxy.keytab.loacation配置的是keytab的绝对路径
			- proxy.user配置的是principle
			- keytab和principle需要代理其他用户的权限
		- azkaban.should.proxy，表示提交hadoop的yarn任务的时候是否需要开启用户代理，若开启了job进程将代理azkaban的登录账号来提交yarn任务，如果不开启将以executor的形式提交
		- obtain.binary.taken：是否需要获取hadoop集群的token，如果hadoop开启了安全性认证并且开启了用户代理，参数必须为true。
		- obtain.namenode.token：是否需要获取namenode的token。如果hadoop开启了安全性认证并且开启了用户代理，参数必须为true。
		- obtain.jobtracker.token：是否需要获取jobtracker的token。如果hadoop开启了安全性认证并且开启了用户代理，参数必须为true。
		- 开启了安全性验证的例子：

				hadoop.security.manager.class=azkaban.security.HadoopSecurityManager_H_2_0
				proxy.keytab.location=/home/hadoop/azkaban.keytab
				klist -kt /home/hadoop/zakaban.keytab
				proxy.user=mapred/classb-bigdata4.server.163.org@TEST.HZ.NETEASE.COM
				azkaban.should.proxy=true
				obtain.binary.token=true
				obtain.namenode.token=true
				obtain.jobtracker.token=true
		- 没有开启hadoop安全性验证的例子
		
				hadoop.security.manager.class=azkaban.security.HadoopSecurityManager_H_2_0
				azkaban.should.proxy=false
	- JVM启动、Java Job进程参数
		- JVM启动参数
			- jobtype.golbal.classpath：类加载参数，全局job java进程classpath
					
					${hadoop.home}/etc/hadoop,${hadoop.home}/share/hadoop/common/*,${hadoop.home}/share/hadoop/common/lib/*,${hadoop.home}/share/hadoop/hdfs/*, ${hadoop.home}/share/hadoop/hdfs/lib/*, ${hadoop.home}/share/hadoop/mapreduce/*, ${hadoop.home}/share/hadoop/mapreduce/lib/*, ${hadoop.home}/share/hadoop/yarn/*, ${hadoop.home}/share/hadoop/yarn/lib/*, ${hadoop.home}/share/hadoop/tools/lib/*
			- jobtype.golbal.jvm.args
					
					全局job java进程jvm启动参数
					-Djava.library.path=${hadoop.home}/lib/native -Djava.io.tmpdir=/home/hadoop/tmp
	- 底层组件home路径：主要是配置各个组件客户端的绝对路径，如果相关组件没有，可以随意配置一个值或者删除该配置并把插件目录删除掉。
		
			hadoop.home=/home/hadoop/hadoop-current
			
			pig.home=pig_home
			
			hive.home=/home/hadoop/hive
			
			spark.home=spark_home
			
			execute.as.user=false
			
			azkaban.native.lib=

	- 任务安全性相关参数：主要是控制job进程在执行时，是否要切换Linux账号，账号名也是azkaban的登陆账号，可以通过Linux系统自身的隔离来加强安全性

	- 具体插件参数
	
			hadoopjava()
			
			jobtype.class=azkaban.jobtype.HadoopJavaJob
			Hive
			
			jobtype.class=azkaban.jobtype.HadoopHiveJob
			
			hive.aux.jar.path=${hive.home}/aux/lib
			
			jobtype.classpath=${hive.home}/lib/*, ${hive.home}/conf, ${hive.aux.jar.path}
			
			spark
			
			jobtype.class=azkaban.jobtype.HadoopSparkJob
			
			jobtype.classpath=${spark.home}/jars/*, ${spark.home}/conf

