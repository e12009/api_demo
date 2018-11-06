/*
 *本文件代码主要给用户演示以下内容：
 *    1. 怎么生成访问信德数聚服务的url。可参考createTaskDemo或getTaskStatus方法。要注意如何生成time参数。
 *    2. 如何对url签名。 可参考signUrl方法。客户可把该代码拷贝至自己的代码内使用。
 *    3. 如何创建一个新任务。 这里用身份证二要素作为例子。
 ×    4. 如何查询一个任务状态。
 *本文件依赖request和cryptojs库，在运行之前请安装相关依赖。
 */

let request = require('request');
let CryptoJS = require('cryptojs').Crypto;

/*
 *这个方法用于同步访问服务器。
 */
async function requestSync(options){
    return new Promise(function(resolve, reject){
        request(options , function(error,response,body){
            if(error){
                reject(error);
            }else{
                resolve(body);
            }
        });
    });
}

/*
 *这个方法用于对一个访问信德数聚服务的URL进行签名。
 *@param url, 待签名的URL. 需要按信德数聚API文档生成。示例: https://api.xindedata.com/v1/task?appid=myappid&time=1467372594
 *@param appSecret, 分配给客户的appSecret.
 *@return 签名后的URL. 示例:
 *        https://api.xindedata.com/v1/task?appid=myappid&time=1467372594&signature=128574d49246805967a3051a362256fcb9a9656f
 */
async function signUrl(url, appSecret) {
    //fetch parameters from url and generate a key/value pair array.
    var qs = {};
    if (url.indexOf("?") != -1) {
        var str = url.substr(url.indexOf("?") + 1);
        strs = str.split("&");
        for (var i = 0; i < strs.length; i++) {
            qs[strs[i].split("=")[0]] = unescape(strs[i].split("=")[1]);
        }
    }
    //Sort keys and generate string to be hashed
    var keys = Object.keys(qs);
    keys.sort();
    var signString = appSecret;
    for (var i in keys) {
        var key = keys[i];
        signString += key + qs[key];
    }
    signString += appSecret;
    //Generate a SHA1 value
    let signature = CryptoJS.SHA1(signString);
    //Generate final URL
    url += "&signature=" + signature;
    return url;
}

/*
 *这个演示怎么提交一个新任务来验证某个人的身份证和姓名是否匹配
 *@param appId, 分配给客户的appid
 *@param appSecret, 分配给客户的appSecret
 *@param userName, 待验证用户的中文名
 *@param userId, 待验证用户的身份证号
 */
async function createTaskDemo(appId, appSecret, userName, userId) {
    //Create URL
    let BASE_URL = "https://api.xindedata.com/v1/task";
    let timestamp = Math.round(Date.now() / 1000);
    let url = BASE_URL + '?appid='+appId + '&time='+timestamp;
    let signedUrl = await signUrl(url, appSecret);
    console.log("signed url="+signedUrl);

    //build request content
    let headers = {'Content-Type':'application/json;charset=utf-8'};
    let options = {
        url: signedUrl,
        method: 'POST',
        headers: headers,
        form: {"type":"auth2", "userName": userName, "userID": userId}
    }
    //Send request
    let body = await requestSync(options);
    console.log(`response content ${body}`);
}

/*
 *这个演示怎么查询某个任务的状态
 *@param tid, 在创建任务时得到的任务号
 *@param appId, 分配给客户的appid
 *@param appSecret, 分配给客户的appSecret
 *@return 服务器端返回结果
 */
async function getTaskStatus(tid, appId, appSecret) {
    //Create URL
    let BASE_URL = "https://api.xindedata.com/v1/task";
    let timestamp = Math.round(Date.now() / 1000);
    let url = BASE_URL + '?tid=' + tid + '&appid='+appId + '&time='+timestamp;
    let signedUrl = await signUrl(url, appSecret);
    console.log("signed url="+signedUrl);

    //build request content
    let headers = {'Content-Type':'application/json;charset=utf-8'};
    let options = {
        url: signedUrl,
        method: 'GET',
        headers: headers,
    }
    //Send request
    let body = await requestSync(options);
    return body;
}

async function help() {
    console.log("用法:");
    console.log("node XindeApiDemo.js -signUrl \"<url>\" <appSecret>");
    console.log("                  这个演示如何对一个API进行签名.");
    console.log("                  <url> 待签名的URL。命令行输入时需要放在双引号内以避免解析错误。");
    console.log("                  <appSecret> 分配给客户的appSecret");
    console.log("node XindeApiDemo.js -createTaskDemo <appId> <appSecret> <中文名> <身份证号>");
    console.log("                  这个演示怎么提交一个新任务来验证某个人的身份证和姓名是否匹配");
    console.log("                  <appId> 分配给客户的appid");
    console.log("                  <appSecret> 分配给客户的appSecret");
    console.log("                  <中文名> 待验证用户的中文名");
    console.log("                  <身份证号> 待验证用户的身份证号");
    console.log("node XindeApiDemo.js -getTaskStatus <tid> <appId> <appSecret>");
    console.log("                  这个演示怎么查询某个任务的状态");
    console.log("                  <tid> 在创建任务时得到的任务号。可使用createTaskDemo得到的task ID。");
    console.log("                  <appId> 分配给客户的appid");
    console.log("                  <appSecret> 分配给客户的appSecret");
}

/*
 *这段是主程序。用于接受命令行输入并调用相应的方法作处理。
 */
(async function() {
    try {
        if(process.argv.length < 5) {
            help();
            return;
        } else if ("-signUrl" === process.argv[2]) {
            if(process.argv.length != 5) {
                help();
                return;
            }
            let signedUrl = await signUrl(process.argv[3], process.argv[4]);
            console.log("signed url:" + signedUrl);
        } else if ("-getTaskStatus" === process.argv[2]) {
            if(process.argv.length != 6) {
                help();
                return;
            }
            let status =  await getTaskStatus(process.argv[3], process.argv[4], process.argv[5]);
            console.log("status:" + status);
        } else if ("-createTaskDemo" === process.argv[2]) {
            if(process.argv.length != 7) {
                help();
                return;
            }
            await createTaskDemo(process.argv[3], process.argv[4], process.argv[5], process.argv[6]);
        } else {
            help();
        }
    } catch(error) {
        console.error(error);
    }
})();
