package cn.qianlan.klinedemo;

import java.util.List;

/**
 * K线数据
 */
public class KlineEntity {

    /**
     * ok : true
     * code : 1
     * msg :
     * data : {}
     */

    private boolean ok;
    private int code;
    private String msg;
    private DataEntity data;

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public DataEntity getData() {
        return data;
    }

    public void setData(DataEntity data) {
        this.data = data;
    }

    public static class DataEntity {
        private List<String> columns;
        private List<List<String>> lists;

        public List<String> getColumns() {
            return columns;
        }

        public void setColumns(List<String> columns) {
            this.columns = columns;
        }

        public List<List<String>> getLists() {
            return lists;
        }

        public void setLists(List<List<String>> lists) {
            this.lists = lists;
        }
    }
}
