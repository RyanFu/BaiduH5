package com.haokan.baiduh5.bean;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Created by wangzixu on 2017/8/11.
 */
@DatabaseTable(tableName = "table_collect")
public class CollectionBean {
    @DatabaseField(generatedId = true)
    public long _id;

    @DatabaseField
    public String url;

    @DatabaseField
    public String title;

    /**
     * 存入日期
     */
    @DatabaseField
    public String date;

    /**
     * 缩略图地址
     */
    @DatabaseField
    public String thumbUrl;

    @DatabaseField
    public long create_time;

    public boolean isSelected;
}
