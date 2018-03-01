package com.play.cn.mr.order;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;

/**
 *
 * 订单格式内容
 * 订单id，商品id，成交金额
 * Order_0000001,Pdt_01,222.8
 * Order_0000001,Pdt_05,25.8
 * Order_0000002,Pdt_05,325.8
 * Order_0000002,Pdt_03,522.8
 * Order_0000002,Pdt_04,122.4
 * Order_0000003,Pdt_05,222.8
 * Order_0000003,Pdt_07,932.8
 * Order_0000003,Pdt_06,132.8
 *
 * 求出每个订单中成交额最大的一笔交易
 */


public class GroupSort {

    static class SortMapper extends Mapper<LongWritable, Text, OrderBean, NullWritable> {
        @Override
        protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {

            OrderBean orderBean = new OrderBean();
            String line = value.toString();
            String[] strings = line.split(",");
            orderBean.set(new Text(strings[0]), new DoubleWritable(Double.parseDouble(strings[2])));
            context.write(orderBean, NullWritable.get());
            System.out.println("****map has finished****");
        }
    }

    static class SortReducer extends Reducer<OrderBean, NullWritable, Text, NullWritable> {

        // 到达reduce时，相同id的所有bean已经被看成一组，且金额最大的那个一排在第一位

        @Override
        protected void reduce(OrderBean key, Iterable<NullWritable> values, Context context) throws IOException, InterruptedException {
            System.out.printf("--------------------"+ key.toString() +"进入reduce---------------------------");
            context.write(new Text(key.toString()), NullWritable.get());
        }
    }


    public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
        Configuration configuration = new Configuration();


        Job job = Job.getInstance(configuration);

        job.setJarByClass(GroupSort.class);
        job.setMapperClass(SortMapper.class);
        job.setReducerClass(SortReducer.class);

        //
        job.setGroupingComparatorClass(MyGroupingComparator.class);
        //
        job.setPartitionerClass(ItemIdPartitioner.class);


        job.setMapOutputKeyClass(OrderBean.class);
        job.setMapOutputValueClass(NullWritable.class);


        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(NullWritable.class);

//        job.setNumReduceTasks(3);

        FileInputFormat.setInputPaths(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));


        boolean result = job.waitForCompletion(true);
        System.exit(result? 0: 1);


    }





}
