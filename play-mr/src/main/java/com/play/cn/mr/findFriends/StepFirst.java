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


/**
 * 数据样本
 *
 * A:B,C,D,F,E,O
 * B:A,C,E,K
 * C:F,A,D,I
 * D:A,E,F,L
 * E:B,C,D,M,L
 * F:A,B,C,D,E,O,M
 * G:A,C,D,E,F
 * H:A,C,D,E,O
 * I:A,O
 * J:B,O
 * K:A,C,D
 * L:D,E,F
 * M:E,F,G
 * O:A,H,I,J
 * map输出<B,A> <C,A> <D,A> <F,A> <E,A> <O,A>
 * reduce接收数据<C,A><C,B><C,E><C,F><C,G>......
 * reduce 输出
 * Key:C
 * value: [ A, B, E, F, G ]
 */

public class StepFirst {

    static class FirstMapper extends Mapper<LongWritable, Text, Text, Text> {
        @Override
        protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {

            String line = value.toString();
            String[] strings = line.split(":");
            String user = strings[0];
            String friends = strings[1];
            for (String friend: friends.split(",")) {
                // 输出<B,A> <C,A> <D,A> <F,A> <E,A> <O,A>
                context.write(new Text(friend), new Text(user));
            }
        }
    }

    static class FirstReducer extends Reducer<Text, Text, Text, Text> {
        // reduce 接收数据： 输入<C,A><C,B><C,E><C,F><C,G>......
        @Override
        protected void reduce(Text friend, Iterable<Text> users, Context context) throws IOException, InterruptedException {

            StringBuilder sb = new StringBuilder(1024);
            for (Text user: users) {
                sb.append(user).append(",");
            }
            context.write(new Text(friend), new Text(sb.toString()));
        }
    }


    public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
        Configuration configuration = new Configuration();

        Job job = Job.getInstance(configuration);

        job.setJarByClass(StepFirst.class);
        job.setMapperClass(FirstMapper.class);
        job.setReducerClass(FirstReducer.class);
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
