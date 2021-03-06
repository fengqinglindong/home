数据仓库构建方法中，ETL的过程和传统的实现方法有一些不同，主要分为四个阶段，分别是抽取
（extract）、清洗（clean）、一致性处理（comform）和交付（delivery），简称为ECCD。

1．抽取阶段的主要任务是：

     读取源系统的数据模型。

     连接并访问源系统的数据。

     变化数据捕获。

     抽取数据到数据准备区。

2．清洗阶段的主要任务是：
     清洗并增补列的属性。

     清洗并增补数据结构。

     清洗并增补数据规则。
     增补复杂的业务规则。
     建立元数据库描述数据质量。

     将清洗后的数据保存到数据准备区。

3．一致性处理阶段的主要任务是：

     一致性处理业务标签，即维度表中的描述属性。

     一致性处理业务度量及性能指标，通常是事实表中的事实。

     去除重复数据。

     国际化处理。

     将一致性处理后的数据保存到数据准备区。

4．交付阶段的主要任务是：

     加载星型的和经过雪花处理的维度表数据。

     产生日期维度。

     加载退化维度。

     加载子维度。

     加载1、2、3型的缓慢变化维度。

     处理迟到的维度和迟到的事实。

     加载多值维度。

     加载有复杂层级结构的维度。

     加载文本事实到维度表。

     处理事实表的代理键。

     加载三个基本类型的事实表数据。

     加载和更新聚集。

     将处理好的数据加载到数据仓库。




LookUp Stage

	LookUp Stage把数据读入内存执行查询操作，将匹配的字段输出，或者在符合条件的记录中修改或加入新的字段

Join Stage

	功能说明：将多个表连接后输出

LookUp Stage 和Join Stage 的区别 

	LookUp Stage将数据读入到内存中，所以效率很高，但是占用了较多的物理内存。所以当reference data比较小的时候，我们推荐用LookUp Stage；当reference data比较大的时候，我们推荐用Join Stage。


ETL进程运行较慢，需要分哪几步去找到ETL系统的瓶颈问题。

	答：ETL系统遇到性能问题，运行很慢是一件较常见的事情，这时要做的是逐步找到系统的瓶颈在哪里。
	
	首先要确定是由CPU、内存、I/O和网络等产生的瓶颈，还是由ETL处理过程产生的瓶颈。
	
	如果环境没有瓶颈，那么需要分析ETL的代码。这时，我们可以采用排除的方法，需要隔离不同的操作，并分别对它们进行测试。如果是采用纯手工编码方式的ETL处理，隔离不同的操作要麻烦一些，这时需要根据编码的实际情况来处理。如果是采用ETL工具的话，目前的ETL工具应该都有隔离不同处理的功能，隔离起来相对容易一些。
	
	分析最好从抽取操作开始，然后依次分析各种计算、查找表、聚集、过滤等转换环节的处理操作，最后分析加载操作。
	
	实际的处理中，可以按照下面的七个步骤来查找瓶颈。
	
	1．隔离并执行抽取查询语句。
	
	先将抽取部分隔离出来，去掉转换和交付，可以将数据直接抽取到文件中。如果这一步效率很差，基本确定是抽取SQL的问题。从经验来看，未经调优的SQL是一个最常见的导致ETL效率差的原因。如果这步没有问题进入第二步。
	
	2．去掉过滤条件。
	
	这一条是针对全抽取，然后在ETL处理中进行过滤的处理方式而言。在ETL处理中做过滤处理有时会产生瓶颈。可以先将过滤去掉，如果确定为这个原因，可以考虑在抽取时进行数据过滤。
	
	3．排除查找表的问题。
	
	参照数据在ETL处理过程中通常会加载到内存中，目的是做代码和名称的查找替换，也称查找表。有时查找表的数据量过大也会产生瓶颈。可以逐个隔离查找表，来确定是否是这里出现问题。注意要将查找表的数据量降到最低，通常一个自然键一个代理键就可以，这样可以减少不必要的数据I/O。
	
	4．分析排序和聚集操作。
	
	排序和聚集操作都是非常费资源的操作。对这部分隔离，来判断是否因为它们引起性能问题。如果确定是因为这个，需要考虑是否可以将排序和聚集处理移出数据库和ETL工具，移到操作系统中来处理。
	
	5．隔离并分析每一个计算和转换处理。
	
	有时转换过程中的处理操作也会引起ETL工作的性能。逐步隔离移除它们来判断哪里出了问题。要注意观察像默认值、数据类型转换等操作。
	
	6．隔离更新策略。
	
	更新操作在数据量非常大时是性能非常差的。隔离这部分，看看是否这里出了问题。如果确定是因为大批量更新出了性能问题。应该考虑将insert、update和delete分开处理。
	
	7．检测加载数据的数据库I/O。
	
	如果前面各部分都没有问题，最后需要检测是目标数据库的性能问题。可以找个文件代替数据库，如果性能提高很多，需要仔细检测目标数据库的加载过程中的操作。例如是否关闭了所有的约束，关闭了所有的索引，是否使用了批量加载工具。如果性能还没有提高，可以考虑使用并行加载策略。


ETL是BI项目最重要的一个环节，通常情况下ETL会花掉整个项目的1/3的时间，ETL设计的好坏直接关接到BI项目的成败。ETL也是一个长期的过程，只有不断的发现问题并解决问题，才能使ETL运行效率更高，为项目后期开发提供准确的数据。

ETL的设计分三部分：数据抽取、数据的清洗转换、数据的加载。在设计ETL的时候也是从这三部分出发。数据的抽取是从各个不同的数据源抽取到ODS中(这个过程也可以做一些数据的清洗和转换)，在抽取的过程中需要挑选不同的抽取方法，尽可能的提高ETL的运行效率。ETL三个部分中，花费时间最长的是T(清洗、转换)的部分，一般情况下这部分工作量是整个ETL的2/3。数据的加载一般在数据清洗完了之后直接写入DW中去。

ETL的实现有多种方法，常用的有三种，第一种是借助ETL工具如Oracle的OWB、SQL server 2000的DTS、SQL Server2005的SSIS服务、informatic等实现，第二种是SQL方式实现，第三种是ETL工具和SQL相结合。前两种方法各有优缺点，借助工具可以快速的建立起ETL工程，屏蔽复杂的编码任务，提高速度，降低难度，但是欠缺灵活性。SQL的方法优点是灵活，提高ETL运行效率，但是编码复杂，对技术要求比较高。第三种是综合了前面二种的优点，极大的提高ETL的开发速度和效率。

数据的抽取

数据的抽取需要在调研阶段做大量工作，首先要搞清楚以下几个问题：数据是从几个业务系统中来？各个业务系统的数据库服务器运行什么DBMS？是否存在手工数据，手工数据量有多大？是否存在非结构化的数据？等等类似问题，当收集完这些信息之后才可以进行数据抽取的设计。

1、与存放DW的数据库系统相同的数据源处理方法

这一类数源在设计比较容易，一般情况下，DBMS(包括SQLServer，Oracle)都会提供数据库链接功能，在DW数据库服务器和原业务系统之间建立直接的链接关系就可以写Select 语句直接访问。

2、与DW数据库系统不同的数据源的处理方法。

这一类数据源一般情况下也可以通过ODBC的方式建立数据库链接，如SQL Server和Oracle之间。如果不能建立数据库链接，可以有两种方式完成，一种是通过工具将源数据导出成.txt或者是.xls文件，然后再将这些源系统文件导入到ODS中。另外一种方法通过程序接口来完成。

3、对于文件类型数据源(.txt,，xls)，可以培训业务人员利用数据库工具将这些数据导入到指定的数据库，然后从指定的数据库抽取。或者可以借助工具实现，如SQL SERVER 2005 的SSIS服务的平面数据源和平面目标等组件导入ODS中去。

4、增量更新问题

对于数据量大的系统，必须考虑增量抽取。一般情况，业务系统会记录业务发生的时间，可以用作增量的标志，每次抽取之前首先判断ODS中记录最大的时间，然后根据这个时间去业务系统取大于这个时间的所有记录。利用业务系统的时间戳，一般情况下，业务系统没有或者部分有时间戳。

数据的清洗转换

一般情况下，数据仓库分为ODS、DW两部分，通常的做法是从业务系统到ODS做清洗，将脏数据和不完整数据过滤掉，再从ODS到DW的过程中转换，进行一些业务规则的计算和聚合。

1、数据清洗

数据清洗的任务是过滤那些不符合要求的数据，将过滤的结果交给业务主管部门，确认是否过滤掉还是由业务单位修正之后再进行抽取。不符合要求的数据主要是有不完整的数据、错误的数据和重复的数据三大类。

A、不完整的数据，其特征是是一些应该有的信息缺失，如供应商的名称，分公司的名称，客户的区域信息缺失、业务系统中主表与明细表不能匹配等。需要将这一类数据过滤出来，按缺失的内容分别写入不同Excel文件向客户提交，要求在规定的时间内补全。补全后才写入数据仓库。

B、错误的数据，产生原因是业务系统不够健全，在接收输入后没有进行判断直接写入后台数据库造成的，比如数值数据输成全角数字字符、字符串数据后面有一个回车、日期格式不正确、日期越界等。这一类数据也要分类，对于类似于全角字符、数据前后有不面见字符的问题只能写SQL的方式找出来，然后要求客户在业务系统修正之后抽取；日期格式不正确的或者是日期越界的这一类错误会导致ETL运行失败，这一类错误需要去业务系统数据库用SQL的方式挑出来，交给业务主管部门要求限期修正，修正之后再抽取。

C、重复的数据，特别是维表中比较常见，将重复的数据的记录所有字段导出来，让客户确认并整理。

数据清洗是一个反复的过程，不可能在几天内完成，只有不断的发现问题，解决问题。对于是否过滤、是否修正一般要求客户确认；对于过滤掉的数据，写入Excel文件或者将过滤数据写入数据表，在ETL开发的初期可以每天向业务单位发送过滤数据的邮件，促使他们尽快的修正错误，同时也可以作为将来验证数据的依据。数据清洗需要注意的是不要将有用的数据过滤掉了，对于每个过滤规则认真进行验证，并要用户确认才行。

2、数据转换

数据转换的任务主要是进行不一致的数据转换、数据粒度的转换和一些商务规则的计算。

A、不一致数据转换，这个过程是一个整合的过程，将不同业务系统的相同类型的数据统一，比如同一个供应商在结算系统的编码是XX0001，而在CRM中编码是YY0001，这样在抽取过来之后统一转换成一个编码。

B、数据粒度的转换，业务系统一般存储非常明细的数据，而数据仓库中的数据是用来分析的，不需要非常明细的数据，一般情况下，会将业务系统数据按照数据仓库粒度进行聚合。

C、商务规则的计算，不同的企业有不同的业务规则，不同的数据指标，这些指标有的时候不是简单的加加减减就能完成，这个时候需要在ETL中将这些数据指标计算好了之后存储在数据仓库中，供分析使用。

ETL日志与警告发送

1、ETL日志，记录日志的目的是随时可以知道ETL运行情况，如果出错了，出错在那里。

ETL日志分为三类。第一类是执行过程日志，是在ETL执行过程中每执行一步的记录，记录每次运行每一步骤的起始时间，影响了多少行数据，流水账形式。第二类是错误日志，当某个模块出错的时候需要写错误日志，记录每次出错的时间，出错的模块以及出错的信息等。第三类日志是总体日志，只记录ETL开始时间，结束时间是否成功信息。

如果使用ETL工具，工具会自动产生一些日志，这一类日志也可以作为ETL日志的一部分。

2、警告发送

ETL出错了，不仅要写ETL出错日志而且要向系统管理员发送警告，发送警告的方式有多种，常用的就是给系统管理员发送邮件，并附上出错的信息，方便管理员排查错误。

专业的报表解决方案，轻松搭建报表平台和报表中心。