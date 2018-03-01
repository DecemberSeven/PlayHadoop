package com.play.cn.mr.order;

import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.WritableComparator;

public class MyGroupingComparator extends WritableComparator {

    public MyGroupingComparator() {
        //注册comparator
        super(OrderBean.class, true);
    }

    @Override
    public int compare(WritableComparable a, WritableComparable b) {

        System.out.println("WritableComparator类：" + a.toString() + "*********正在和*******" + b.toString() + "****进行对比");
        OrderBean aBean = (OrderBean)a;
        OrderBean bBean = (OrderBean)b;
        return aBean.getItemid().compareTo(bBean.getItemid());
    }
}
