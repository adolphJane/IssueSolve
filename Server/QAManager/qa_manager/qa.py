#coding:utf8
import aiml
import os

from qa_manager.QACrawler import baike
from qa_manager.Tools import Html_Tools as QAT
from qa_manager.Tools import TextProcess as T
from QACrawler import search_summary

import sys
reload(sys)
sys.setdefaultencoding('utf-8')


def qa(question):

    #初始化jb分词器
    T.jieba_initialize()

    #切换到语料库所在工作目录
    mybot_path = './'
    # os.chdir(mybot_path)

    mybot = aiml.Kernel()
    mybot.learn(os.path.split(os.path.realpath(__file__))[0]+"/resources/std-startup.xml")
    mybot.learn(os.path.split(os.path.realpath(__file__))[0] + "/resources/bye.aiml")
    mybot.learn(os.path.split(os.path.realpath(__file__))[0] + "/resources/tools.aiml")
    mybot.learn(os.path.split(os.path.realpath(__file__))[0] + "/resources/bad.aiml")
    mybot.learn(os.path.split(os.path.realpath(__file__))[0] + "/resources/funny.aiml")
    mybot.learn(os.path.split(os.path.realpath(__file__))[0] + "/resources/OrdinaryQuestion.aiml")
    mybot.learn(os.path.split(os.path.realpath(__file__))[0] + "/resources/Common conversation.aiml")
    # mybot.respond('Load Doc Snake')
    #载入百科属性列表

    input_message = question
    print question
    if len(input_message) > 60:
        return mybot.respond("句子长度过长")
    elif input_message.strip() == '':
        return mybot.respond("无")

    message = T.wordSegment(input_message)
    # 去标点
    words = T.postag(input_message)


    if message == 'q':
        return "Error"
    else:
        response = mybot.respond(message)
        print "======="
        print response
        print "======="

        if response == "":
            ans = mybot.respond('找不到答案')
            return 'Eric：' + ans
        # 百科搜索
        elif response[0] == '#':
            # 匹配百科
            if response.__contains__("searchbaike"):
                print response
                res = response.split(':')
                #实体
                entity = str(res[1]).replace(" ","")
                #属性
                attr = str(res[2]).replace(" ","")
                print entity+'<---->'+attr

                ans = baike.query(entity, attr)
                # 如果命中答案
                if type(ans) == list:
                    return 'Eric：' + QAT.ptranswer(ans,False)
                elif ans.decode('utf-8').__contains__(u'::找不到'):
                    #百度摘要+Bing摘要
                    print "通用搜索"
                    ans = search_summary.kwquery(input_message)

            # 匹配不到模版，通用查询
            elif response.__contains__("NoMatchingTemplate"):
                print "NoMatchingTemplate"
                ans = search_summary.kwquery(input_message)


            if len(ans) == 0:
                ans = mybot.respond('找不到答案')
                return 'Eric：' + ans
            elif len(ans) >1:
                print "不确定候选答案"
                print 'Eric: '
                uncertain = "Eric:"
                for a in ans:
                    print a.encode("utf8")
                    uncertain += (a + '   ')
                return uncertain
            else:
                return 'Eric：' + ans[0].encode("utf8")

        # 匹配模版
        else:
            return 'Eric：' + response

