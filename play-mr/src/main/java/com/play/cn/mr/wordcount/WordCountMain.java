package com.play.cn.mr.wordcount;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class WordCountMain {

    public static void main(String[] args) throws Exception {
        // 创建配置对象
        Configuration configuration = new Configuration();

        // 创建job对象
        Job job = Job.getInstance(configuration, "wordcount");

        // 设置运行job类
        job.setJarByClass(WordCountMain.class);

        // 设置mapper类
        job.setMapperClass(WordCountMapper.class);

        // 设置reducer类
        job.setReducerClass(WordCountReducer.class);

        // 设置map输出类型
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(IntWritable.class);

        // 设置reduce输出类型
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        // 设置输入、输出路径
        FileInputFormat.setInputPaths(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        // 提交job
        boolean result = job.waitForCompletion(true);

        if (!result) {
            System.out.println("wordcount task fail");
        }

    }
}
