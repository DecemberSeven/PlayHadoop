#### yarn application demo程序和web融合的项目
* yarn application demo
* web项目用于展示am中各个container的状况等信息

#### 打包
    
    ## 打包时，需要到play-yarn 模块中的pom.xml修改mainClass的路径
    mvn package

#### 启动
   
    java -jar play-yarn.jar
    
    

     
> 默认全部是测试环境内容，直接可以运行，如果要切到与yarn app运行（放到yarn集群上运行，
监控yarn中container等相关信息），需要修改pom中的mainClass，修改ContainerController中的TODO部分，
并根据个人需求修改resources/WEB-INF/view/container/index.vm页面展示内容