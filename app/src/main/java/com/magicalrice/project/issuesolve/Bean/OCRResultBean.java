package com.magicalrice.project.issuesolve.Bean;

import java.util.ArrayList;

/**
 * Created by Adolph on 2018/1/14.
 */

public class OCRResultBean {
    private long log_id;
    private int words_result_num;
    private ArrayList<OCRWordsBean> words_result;

    public long getLog_id() {
        return log_id;
    }

    public void setLog_id(long log_id) {
        this.log_id = log_id;
    }

    public int getWords_result_num() {
        return words_result_num;
    }

    public void setWords_result_num(int words_result_num) {
        this.words_result_num = words_result_num;
    }

    public ArrayList<OCRWordsBean> getWords_result() {
        return words_result;
    }

    public void setWords_result(ArrayList<OCRWordsBean> words_result) {
        this.words_result = words_result;
    }
}
