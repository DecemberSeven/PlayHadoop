package com.play.cn.mr.sqljoin;

import com.google.common.collect.Lists;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;

import java.io.IOException;
import java.util.ArrayList;

public class JoinMR {

    static class JoinMRMapper extends Mapper<LongWritable, Text, Text, InfoBean> {
        @Override
        protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            InfoBean infoBean = new InfoBean();
            Text text = new Text();

            String line = value.toString();
            String[] strings = line.split(",");
            FileSplit inputSplit = (FileSplit) context.getInputSplit();
            String fileName = inputSplit.getPath().getName();
            String pid = "";

            if (fileName.startsWith("order")) {
                // flag = 0 表示这个对象封装订单记录
                pid = strings[2];
                infoBean.set(Integer.parseInt(strings[0]), strings[1], pid, Integer.parseInt(strings[3]), "", 0, 0, "0");
            } else {
                // flag = 1 表示这个对象封装产品信息记录
                pid = strings[0];
                infoBean.set(0, "", pid, 0, strings[1], Integer.parseInt(strings[2]), Float.parseFloat(strings[3]), "1");
            }
            text.set(pid);
            context.write(text, infoBean);
        }
    }

    static class JoinMRReducer extends Reducer<Text, InfoBean, InfoBean, NullWritable> {
        @Override
        protected void reduce(Text key, Iterable<InfoBean> values, Context context) throws IOException, InterruptedException {
            InfoBean infoBean = new InfoBean();
            ArrayList<InfoBean> orderBeans = Lists.newArrayList();
            try {
                for (InfoBean bean: values) {
                    // if this object is product,...
                    if ("1".equals(bean.getFlag())) {
                        BeanUtils.copyProperties(infoBean, bean);
                    } else {
                        InfoBean odbean = new InfoBean();
                        BeanUtils.copyProperties(odbean, bean);
                        orderBeans.add(odbean);
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            for (InfoBean bean: orderBeans) {
                bean.setP_name(infoBean.getP_name());
                bean.setCategory_id(infoBean.getCategory_id());
                bean.setPrice(infoBean.getPrice());
                context.write(bean, NullWritable.get());
            }

        }
    }

}
