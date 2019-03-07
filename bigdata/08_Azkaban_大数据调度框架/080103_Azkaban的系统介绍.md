## Azkaban的组件介绍
- LinkedIn公司开源
- 解决按照DAG顺序来跑job的集合，比如ETL、数据分析工具
- Job大多是hadoop生态圈的组件任务
- 模块组成：
	- Webserver：控制管理模块，元数据的管理、项目上传解析、调度设置等等，调度的触发、执行实例的生成、执行实例的分发监控等等。提供对外查询接口，包括日志、实例状态的查询、认证。
	- Executor：执行模块，负责执行具体的flow，提供业务接口，查询执行实例的状态。可以是多节点的
	- MySQL：存储层，负责存储元数据，比如项目文件、项目信息、调度信息、报警规则等等。同时存储业务数据，比如执行实例的信息、日志等等
	- pluins
		- Web插件：用户管理、hdfs预览
		- Exec插件：各类大数据组件，各类job

## Azkaban作业流程（整体流程）
1. 部署底层依赖的组件： hadoop、hive、spark、pig  Mysql
2. 搭建Azkaban服务：solo模式（webserver、executor在一个进程中）、单exec、多exec
3. 部署Azkaban job插件：hive、spark、hadoopJava等
4. 使用Azkaban进行作业调度、执行

## Azkaban作业流程（WebUI流程）
1. 构建项目zip包
	1. 编写job代码
	2. 编写job文件，根据DAG拓扑构建依赖关系定义一个或多个flow
	3. 将jar、job、properties文件打包成zip
2. 在Azkaban web ui上创建项目
	1. 项目创建好后，点击具体详情，上传zip包
3. 执行项目中的flow
	1. 选择flow配置调度/立即执行
2. 插件Job执行结果（executor根据任务调度执行实例，依据flow的job类型，调用Job插件执行作业，运行回传）

## Azkaban作业流程（内部）
1. 项目创建
	1. 所有项目都构建成project对象，常驻web内存
	2. webserver收到项目创建请求，检查同名项目是否存在。（不同Azkaban账号也不能同名，多租户概念弱）
	3. 若同意创建项目，webserver构建构建新的project对象，塞入内存对象集合中，同时在project表中新增记录，project的主键id就是项目id
4. 项目zip包更新上传
	1. webserver遍历.properties和.job文件，构建job props（Map对象）
	2. 检查job props是否合法（比如type是否缺失、xml配置是否正确、大小是否正确）
	3. 检查job依赖是否存在环路、解析出各个flow，每个flow的名字和DAG最后一个job name名字相同。
	4. 更新project的实例，更新version，新增projec_flow、project_version等table。
5. 创建调度（计划生成）
	1. 所有调度都创建trigger对象，常驻web内存
	2. webserver解析收到的调度参数，构建schedule对象。（schedule用于对外交互和参数检查，trigger对象用于内部逻辑使用）
	3. 对schedule对象进行合法性校验，判断目标flow是否存在，周期是否为负数，起始时间是否太古老等等。
	4. 判断相同的schedule是否存在，如果存在变更为更新操作。
	5. 根据schedule对象创建trigger对象：
		1. 根据调度时间参数创建trigger condition（作用计算最近下次执行的时间）。
		2. 将trigger更新到db，同时将trigger添加到内存&trigger scan thread

4. 调度任务实例生成&执行
	1. trigger scan thread（trigger扫描线程）定期扫描trigger list（trigger 列表），根据trigger的状态以及trigger condition的值判断是否应该生成调度任务实例
	2. 若生成调度实例，综合flow、trigger的计划执行时间，构建调度任务实例execFlow
	3. execFlow实例塞入实例队列
	4. 分发线程从实例队列中拿到execFlow，分发给executor服务。
	5. Executor服务接受新的execFlow实例，下载相关包等资源，构建节点执行环境
	6. Executor根据flow的拓扑结构，每个job构建一个独立进程执行
	7. Webserver轮训访问execFlow的状态，更新到db

## Azkaban作业流程（整体图）
![](https://i.imgur.com/vAIbuHu.png)

1. TriggerManager 和 ProjectManager 会在webserver启动时，会将db中的trigger和project全部读取到内存中，任何trigger和project都是先更新内存，然后持久化到db。
2. triggerManager有多个扫描线程，会把trigger尽量均衡的分配给各个扫描线程，扫描线程会定期扫描各个trigger。
3. 生成调度实例，塞入实例队列中，实例队列有个消费线程，消费线程会定期访问Executor的资源状态，然后根据既定的负载均衡规则，把调度时的任务执行实例转化为executor。
4. executor根据实例的job构建job 进程，提交作业执行。
