package com.play.cn.mr.order;


import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class OrderBean implements WritableComparable<OrderBean> {


    private Text itemid;
    private DoubleWritable amount;

    public OrderBean() {
    }

    public OrderBean(Text itemid, DoubleWritable amount) {
        this.set(itemid, amount);
    }

    public void set(Text itemid, DoubleWritable amount) {
        this.itemid = itemid;
        this.amount = amount;
    }


    public void setItemid(Text itemid) {
        this.itemid = itemid;
    }

    public void setAmount(DoubleWritable amount) {
        this.amount = amount;
    }


    public Text getItemid() {
        return itemid;
    }

    public DoubleWritable getAmount() {
        return amount;
    }

    public int compareTo(OrderBean o) {
        System.out.println("CompareTo方法：" + this.toString() + "*********正在和*******" + o.toString() + "****进行对比");
        int ret = this.itemid.compareTo(o.getItemid());
        if (ret == 0)
            ret = -this.amount.compareTo(o.getAmount());
        return ret;
    }

    public void write(DataOutput dataOutput) throws IOException {
        dataOutput.writeUTF(itemid.toString());
        dataOutput.writeDouble(amount.get());
    }

    public void readFields(DataInput dataInput) throws IOException {
        this.itemid = new Text(dataInput.readUTF());
        this.amount = new DoubleWritable(dataInput.readDouble());
    }

    @Override
    public String toString() {
        return "OrderBean{" +
                "itemid=" + itemid +
                ", amount=" + amount +
                '}';
    }
}
