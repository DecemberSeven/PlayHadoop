package com.play.cn.mr.sqljoin;

import org.apache.hadoop.io.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class InfoBean implements Writable {

    private int order_id;

    private String date_string;

    private String p_id;

    private int amount;

    private String p_name;

    private int category_id;

    private float price;

    // flag = 0 表示这个对象封装订单记录
    // flag = 1 表示这个对象封装产品信息记录

    private String flag;


    public void set(int order_id, String date_string, String p_id, int amount, String p_name, int category_id, float price, String flag) {
        this.order_id = order_id;
        this.date_string = date_string;
        this.p_id = p_id;
        this.amount = amount;
        this.p_name = p_name;
        this.category_id = category_id;
        this.price = price;
        this.flag = flag;
    }

    public int getOrder_id() {
        return order_id;
    }

    public void setOrder_id(int order_id) {
        this.order_id = order_id;
    }

    public String getDate_string() {
        return date_string;
    }

    public void setDate_string(String date_string) {
        this.date_string = date_string;
    }

    public String getP_id() {
        return p_id;
    }

    public void setP_id(String p_id) {
        this.p_id = p_id;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public String getP_name() {
        return p_name;
    }

    public void setP_name(String p_name) {
        this.p_name = p_name;
    }

    public int getCategory_id() {
        return category_id;
    }

    public void setCategory_id(int category_id) {
        this.category_id = category_id;
    }

    public float getPrice() {
        return price;
    }

    public void setPrice(float price) {
        this.price = price;
    }

    public String getFlag() {
        return flag;
    }

    public void setFlag(String flag) {
        this.flag = flag;
    }


    public void write(DataOutput dataOutput) throws IOException {
        dataOutput.writeInt(order_id);
        dataOutput.writeUTF(date_string);
        dataOutput.writeUTF(p_id);
        dataOutput.writeInt(amount);
        dataOutput.writeUTF(p_name);
        dataOutput.writeInt(category_id);
        dataOutput.writeFloat(price);
        dataOutput.writeUTF(flag);
    }

    public void readFields(DataInput dataInput) throws IOException {
        this.order_id = dataInput.readInt();
        this.date_string = dataInput.readUTF();
        this.p_id = dataInput.readUTF();
        this.amount = dataInput.readInt();
        this.p_name = dataInput.readUTF();
        this.category_id = dataInput.readInt();
        this.price = dataInput.readFloat();
        this.flag = dataInput.readUTF();
    }

    @Override
    public String toString() {
        return "InfoBean{" +
                "order_id=" + order_id +
                ", date_string='" + date_string + '\'' +
                ", p_id='" + p_id + '\'' +
                ", amount=" + amount +
                ", p_name='" + p_name + '\'' +
                ", category_id=" + category_id +
                ", price=" + price +
                ", flag='" + flag + '\'' +
                '}';
    }
}
