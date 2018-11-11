<?php
/*
本文件代码主要给用户演示以下内容：
    1. 怎么生成访问信德数聚服务的url。可参考createTaskDemo或getTaskStatus方法。要注意如何生成time参数。
    2. 如何对url签名。 可参考signUrl方法。客户可把该代码拷贝至自己的代码内使用。
    3. 如何创建一个新任务。 这里用身份证二要素作为例子。
    4. 如何查询一个任务状态。
本文件依赖curl库，在运行之前请确保相关依赖已安装。
*/

/*
这个方法用于对一个访问信德数聚服务的URL进行签名。
@param url, 待签名的URL. 需要按信德数聚API文档生成。示例: https://api.xindedata.com/v1/task?appid=myappid&time=1467372594
@param appSecret, 分配给客户的appSecret.
@return 签名后的URL. 示例:
        https://api.xindedata.com/v1/task?appid=myappid&time=1467372594&signature=128574d49246805967a3051a362256fcb9a9656f
*/
function signUrl($url, $appSecret) {
    //fetch parameters from url and generate a key/value pair array.
    $dict_data=array();
    if (strpos($url, "?")) {
        $params=substr($url, strpos($url, "?") + 1);
        $param_array = explode("&", $params);
        foreach ($param_array as $param) {
            $item = explode("=", $param);
            $dict_data[$item[0]] = $item[1];
        }
    }
    //Sort array by keys and generate string to be hashed
    ksort($dict_data);
    $signString = $appSecret;
    foreach($dict_data as $x=>$x_value) {
        $signString = sprintf("%s%s%s", $signString, $x, $x_value);
    }
    $signString = sprintf("%s%s", $signString, $appSecret);
    //Generate a sha1 value
    $signature=sha1($signString);
    //Generate final URL
    $url=sprintf("%s&signature=%s", $url, $signature);
    return $url;
}


/*
这个演示怎么提交一个新任务来验证某个人的身份证和姓名是否匹配
@param appId, 分配给客户的appid
@param appSecret, 分配给客户的appSecret
@param userName, 待验证用户的中文名
@param userId, 待验证用户的身份证号
*/
function createTaskDemo($appId, $appSecret, $userName, $userId) {
    #Create URL
    $base_url="https://api.xindedata.com/v1/task";
    $timestamp=time();
    $url=sprintf("%s?appid=%s&time=%u", $base_url, $appId, $timestamp);
    $url = signUrl($url, $appSecret);
    printf("signed url=%s\n", $url);

    #build request content
    $values = array('type'=>'auth2', 'userName'=>$userName, 'userID'=>$userId);

    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $url);
    $header = array();
    $header[] = 'Content-Type:application/json;charset=utf-8';
    curl_setopt($ch, CURLOPT_HTTPHEADER, $header);
    curl_setopt($ch, CURLOPT_POST, 1);
    curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($values,JSON_UNESCAPED_UNICODE));
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
    $response = curl_exec($ch);
    curl_close($ch);
    printf("result: %s\n", $response);
}

/*
这个演示怎么查询某个任务的状态
@param tid, 在创建任务时得到的任务号
@param appId, 分配给客户的appid
@param appSecret, 分配给客户的appSecret
@return 服务器端返回结果
*/
function getTaskStatus($tid, $appId, $appSecret) {
    #Create URL
    $base_url="https://api.xindedata.com/v1/task";
    $timestamp=time();
    $url=sprintf("%s?tid=%s&appid=%s&time=%u", $base_url, $tid, $appId, $timestamp);
    $url = signUrl($url, $appSecret);

    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $url);
    $header = array();
    $header[] = 'Content-Type:application/json;charset=utf-8';
    curl_setopt($ch, CURLOPT_HTTPHEADER, $header);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
    $response = curl_exec($ch);
    curl_close($ch);
    return $response;
}


function help() {
    printf("用法:\n");
    printf("php -f XindeApiDemo.php signUrl \"<url>\" <appSecret>\n");
    printf("                  这个演示如何对一个API进行签名.\n");
    printf("                  <url> 待签名的URL。命令行输入时需要放在双引号内以避免解析错误。\n");
    printf("                  <appSecret> 分配给客户的appSecret\n");
    printf("php -f XindeApiDemo.py createTaskDemo <appId> <appSecret> <中文名> <身份证号>\n");
    printf("                  这个演示怎么提交一个新任务来验证某个人的身份证和姓名是否匹配\n");
    printf("                  <appId> 分配给客户的appid\n");
    printf("                  <appSecret> 分配给客户的appSecret\n");
    printf("                  <中文名> 待验证用户的中文名\n");
    printf("                  <身份证号> 待验证用户的身份证号\n");
    printf("php -f XindeApiDemo.py getTaskStatus <tid> <appId> <appSecret>\n");
    printf("                  这个演示怎么查询某个任务的状态\n");
    printf("                  <tid> 在创建任务时得到的任务号。可使用createTaskDemo得到的task ID。\n");
    printf("                  <appId> 分配给客户的appid\n");
    printf("                  <appSecret> 分配给客户的appSecret\n");
    return;
}


//这段是主程序。用于接受命令行输入并调用相应的方法作处理。
$cnt = count($argv);
if($cnt < 4) {
    help();
} else if (strcmp("signUrl", $argv[1]) == 0) { //demo for signUrl
    if($cnt != 4) {
        help();
    } else {
        printf("input url: %s\n", $argv[2]);
        $signedUrl = signUrl($argv[2], $argv[3]);
        printf("signed url: %s\n", $signedUrl);
    }
} else if (strcmp("createTaskDemo", $argv[1]) == 0) { //demo for createTaskDemo
    if($cnt != 6) {
        help();
    } else {
       createTaskDemo($argv[2], $argv[3], $argv[4], $argv[5]);
    }
} else if (strcmp("getTaskStatus", $argv[1]) == 0) { //demo for getTaskStatus
    if($cnt != 5) {
        help();
    } else {
       printf("tid:%s\n", $argv[2]);
       $result = getTaskStatus($argv[2], $argv[3], $argv[4]);
       printf("status: %s\n", $result);
    }
} else {
    help();
}

?>
