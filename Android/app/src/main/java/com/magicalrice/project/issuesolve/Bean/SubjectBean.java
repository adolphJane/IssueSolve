package com.magicalrice.project.issuesolve.Bean;

import java.io.Serializable;

/**
 * Created by Adolph on 2018/1/14.
 */

public class SubjectBean implements Serializable {

    private String issue;
    private String answer1, answer2, answer3;

    public String getIssue() {
        return issue;
    }

    public void setIssue(String issue) {
        this.issue = issue;
    }

    public String getAnswer1() {
        return answer1;
    }

    public void setAnswer1(String answer1) {
        this.answer1 = answer1;
    }

    public String getAnswer2() {
        return answer2;
    }

    public void setAnswer2(String answer2) {
        this.answer2 = answer2;
    }

    public String getAnswer3() {
        return answer3;
    }

    public void setAnswer3(String answer3) {
        this.answer3 = answer3;
    }

    @Override
    public String toString() {
        return "SubjectBean{" +
                "issue= " + issue +
                ",answer1= " + answer1 +
                ",answer2= " + answer2 +
                ",answer3= " + answer3 +
                "}";
    }
}
