package com.play.cn.mr.findFriends;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;
import java.util.Arrays;

public class StepSecond {

    static class SecondMapper extends Mapper<LongWritable, Text, Text, Text> {
        @Override
        protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {

            String line = value.toString();
            String[] friendUsers = line.split("\t");
            String friend = friendUsers[0];
            String[] users = friendUsers[1].split(",");
            Arrays.sort(users);
            for (int i = 0; i < users.length - 1; i++) {
                for (int j = i + 1; j < users.length; j++) {
                    // 这两人是共同好友
                    context.write(new Text(users[i] + "-" + users[j]), new Text(friend));
                }
            }
        }
    }

    static class SecondReducer extends Reducer<Text, Text, Text, Text> {
        @Override
        protected void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            StringBuilder sb = new StringBuilder(1024);
            for (Text friend: values) {
                sb.append(friend).append(" ");
            }
            context.write(key, new Text(sb.toString()));
        }
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
        Configuration configuration = new Configuration();

        Job job = Job.getInstance(configuration);

        job.setJarByClass(StepSecond.class);
        job.setMapperClass(SecondMapper.class);
        job.setReducerClass(SecondReducer.class);
        // 设置map输出类型
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);

        // 设置reduce输出类型
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        FileInputFormat.setInputPaths(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        job.waitForCompletion(true);
    }
}
