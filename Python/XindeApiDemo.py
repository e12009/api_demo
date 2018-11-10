#!/usr/bin/python
# -*- coding: UTF-8 -*-

'''
本文件代码主要给用户演示以下内容：
    1. 怎么生成访问信德数聚服务的url。可参考createTaskDemo或getTaskStatus方法。要注意如何生成time参数。
    2. 如何对url签名。 可参考signUrl方法。客户可把该代码拷贝至自己的代码内使用。
    3. 如何创建一个新任务。 这里用身份证二要素作为例子。
    4. 如何查询一个任务状态。
本文件依赖requests库，在运行之前请确保相关依赖已安装。
'''

import sys
import hashlib
import urllib
import urllib2
import time
import math
import requests

'''
这个方法用于对一个访问信德数聚服务的URL进行签名。
@param url, 待签名的URL. 需要按信德数聚API文档生成。示例: https://api.xindedata.com/v1/task?appid=myappid&time=1467372594
@param appSecret, 分配给客户的appSecret.
@return 签名后的URL. 示例:
        https://api.xindedata.com/v1/task?appid=myappid&time=1467372594&signature=128574d49246805967a3051a362256fcb9a9656f
'''
def signUrl(url, appSecret):
    #fetch parameters from url and generate a key/value pair array.
    dict_data={}
    if (url.find("?") != -1) :
        params=url[(url.find("?")+1):]
        param_array = params.split("&")
        for param in param_array:
            dict_data[param.split("=")[0]] = urllib.unquote(param.split("=")[1])
    #Sort keys and generate string to be hashed
    keys=sorted(dict_data.keys())
    signString = appSecret
    for key in keys:
        signString += key+dict_data[key]
    signString += appSecret
    #Generate a sha1 value
    hash_new = hashlib.sha1()
    hash_new.update(signString)
    signature = hash_new.hexdigest()
    #Generate final URL
    url += "&signature=" + signature;
    return url

'''
这个演示怎么提交一个新任务来验证某个人的身份证和姓名是否匹配
@param appId, 分配给客户的appid
@param appSecret, 分配给客户的appSecret
@param userName, 待验证用户的中文名
@param userId, 待验证用户的身份证号
'''
def createTaskDemo(appId, appSecret, userName, userId) :
    #Create URL
    BASE_URL = "https://api.xindedata.com/v1/task"
    timestamp = str(long(math.floor(time.time())))
    url = BASE_URL + '?appid='+appId + '&time='+timestamp
    signedUrl = signUrl(url, appSecret)
    print "signed url="+signedUrl

    #build request content

    values = {"type":"auth2", "userName": userName, "userID": userId}

    try:
        request=requests.request("POST", signedUrl, data=values)
        print request.text;
    except urllib2.HTTPError, e:
        print e.code

'''
这个演示怎么查询某个任务的状态
@param tid, 在创建任务时得到的任务号
@param appId, 分配给客户的appid
@param appSecret, 分配给客户的appSecret
@return 服务器端返回结果
'''
def getTaskStatus(tid, appId, appSecret) :
    #Create URL
    BASE_URL = "https://api.xindedata.com/v1/task"
    timestamp = str(long(math.floor(time.time())))
    url = BASE_URL + '?tid=' + tid + '&appid='+appId + '&time='+timestamp
    signedUrl = signUrl(url, appSecret)
    print "signed url="+signedUrl

    #send request
    headers = {'Content-Type':'application/json;charset=utf-8'}
    try:
        request=requests.request("GET", signedUrl, headers=headers)
        print request.text;
    except urllib2.HTTPError, e:
        print e.code

def help():
    print "用法:"
    print "python XindeApiDemo.py -signUrl \"<url>\" <appSecret>"
    print "                  这个演示如何对一个API进行签名."
    print "                  <url> 待签名的URL。命令行输入时需要放在双引号内以避免解析错误。"
    print "                  <appSecret> 分配给客户的appSecret"
    print "python XindeApiDemo.js -createTaskDemo <appId> <appSecret> <中文名> <身份证号>"
    print "                  这个演示怎么提交一个新任务来验证某个人的身份证和姓名是否匹配"
    print "                  <appId> 分配给客户的appid"
    print "                  <appSecret> 分配给客户的appSecret"
    print "                  <中文名> 待验证用户的中文名"
    print "                  <身份证号> 待验证用户的身份证号"
    print "python XindeApiDemo.js -getTaskStatus <tid> <appId> <appSecret>"
    print "                  这个演示怎么查询某个任务的状态"
    print "                  <tid> 在创建任务时得到的任务号。可使用createTaskDemo得到的task ID。"
    print "                  <appId> 分配给客户的appid"
    print "                  <appSecret> 分配给客户的appSecret"
    return

#这段是主程序。用于接受命令行输入并调用相应的方法作处理。
print('Enter main')
cnt=len(sys.argv)
if (cnt < 4) :
    help()
    sys.exit()
if sys.argv[1] == "-signUrl" :
    if (cnt != 4) :
        help()
        sys.exit()
    signedUrl = signUrl(sys.argv[2], sys.argv[3])
    print "signedUrl=",signedUrl
elif sys.argv[1] == "-getTaskStatus":
    if (cnt != 5) :
        help()
        sys.exit()
    status=getTaskStatus(sys.argv[2], sys.argv[3], sys.argv[4])
    print status
elif sys.argv[1] == "-createTaskDemo":
    if (cnt != 6) :
        help()
        sys.exit()
    createTaskDemo(sys.argv[2], sys.argv[3], sys.argv[4], sys.argv[5])

