import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;

/*
 *本文件代码主要给用户演示以下内容：
 *    1. 怎么生成访问信德数聚服务的url。可参考createTaskDemo或getTaskStatus方法。要注意如何生成time参数。
 *    2. 如何对url签名。 可参考signUrl方法。客户可把该代码拷贝至自己的代码内使用。
 *    3. 如何创建一个新任务。 这里用身份证二要素作为例子。
 ×    4. 如何查询一个任务状态。
 *
 */
public class XindeApiDemo {

    public static void main( String[] args )
    {
        try {
            if(args.length < 1) {
                help();
                return;
            } else if ("-signUrl".equals(args[0])) {
                if(args.length != 3) {
                    help();
                    return;
                }
                XindeApiDemo demoObj = new XindeApiDemo();
                String signedUrl = demoObj.signUrl(args[1], args[2]);
                System.out.println("signed url:" + signedUrl);
            } else if ("-getTaskStatus".equals(args[0])) {
                if(args.length != 4) {
                    help();
                    return;
                }
                XindeApiDemo demoObj = new XindeApiDemo();
                String status = demoObj.getTaskStatus(args[1], args[2], args[3]);
                System.out.println("status:" + status);
            } else if ("-createTaskDemo".equals(args[0])) {
                if(args.length != 5) {
                    help();
                    return;
                }
                XindeApiDemo demoObj = new XindeApiDemo();
                demoObj.createTaskDemo(args[1], args[2], args[3], args[4]);
            } else {
                help();
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static void help() {
        System.out.println("用法:");
        System.out.println("java XindeApiDemo -signUrl \"<url>\" <appSecret>");
        System.out.println("                  这个演示如何对一个API进行签名.");
        System.out.println("                  <url> 待签名的URL。命令行输入时需要放在双引号内以避免解析错误。");
        System.out.println("                  <appSecret> 分配给客户的appSecret");
        System.out.println("java XindeApiDemo -createTaskDemo <appId> <appSecret> <中文名> <身份证号>");
        System.out.println("                  这个演示怎么提交一个新任务来验证某个人的身份证和姓名是否匹配");
        System.out.println("                  <appId> 分配给客户的appid");
        System.out.println("                  <appSecret> 分配给客户的appSecret");
        System.out.println("                  <中文名> 待验证用户的中文名");
        System.out.println("                  <身份证号> 待验证用户的身份证号");
        System.out.println("java XindeApiDemo -getTaskStatus <tid> <appId> <appSecret>");
        System.out.println("                  这个演示怎么查询某个任务的状态");
        System.out.println("                  <tid> 在创建任务时得到的任务号。可使用createTaskDemo得到的task ID。");
        System.out.println("                  <appId> 分配给客户的appid");
        System.out.println("                  <appSecret> 分配给客户的appSecret");
    }

    /*
     *这个方法用于对一个访问信德数聚服务的URL进行签名。
     *@param url, 待签名的URL. 需要按信德数聚API文档生成。示例: https://api.xindedata.com/v1/task?appid=myappid&time=1467372594
     *@param appSecret, 分配给客户的appSecret.
     *@return 签名后的URL. 示例:
     *        https://api.xindedata.com/v1/task?appid=myappid&time=1467372594&signature=128574d49246805967a3051a362256fcb9a9656f
     */
    public String signUrl(String url, String appSecret) {

         //TreeMap is used to store parameters with key/value pair.
         //It will automatically sort the data by key.
         TreeMap<String, String> paramsMap = new TreeMap<String, String>();

         //Parse the parameters and add them to tree map.
         int index = url.indexOf("?");
         String parameters = url.substring(index+1);
         String[] paramArray = parameters.split("&");
         for(int i=0;i<paramArray.length;i++) {
             String[] tmpArray = paramArray[i].split("=");
             paramsMap.put(tmpArray[0], tmpArray[1]);
         }

         //Generate the original string to be hashed
         StringBuilder stringBuilder = new StringBuilder(appSecret);
         //Go through sorted parmeters
         Set<String> keySet = paramsMap.keySet();
         for (Iterator<String> it = keySet.iterator(); it.hasNext(); ) {
            String key = it.next();
            String value = paramsMap.get(key);
            stringBuilder.append(key).append(value);
         }
         stringBuilder.append(appSecret);

         //Hash the string
         String signature = generateHash(stringBuilder.toString());
         //Generate the final URL
         String newUrl = url + "&signature=" + signature;
         return newUrl;
    }

    /*
     *此方法主要用于生成一个字符串的sha1哈希值.
     *@param str, 待哈希的字符串。
     ×@return 输入字符串的哈希值。以十六进制字符串显示。
     */
    private String generateHash(String str) {
        if(null == str || str.isEmpty()) {
            return null;
        }

        char hexDigits[] = {
                '0','1','2','3','4','5','6','7',
                '8','9','a','b','c','d','e','f'
        };

        try {
            //Generate sha1 value
            MessageDigest mdTemp = MessageDigest.getInstance("SHA1");
            mdTemp.update(str.getBytes("UTF-8"));

            byte[] md = mdTemp.digest();

            //Convert to hex decimal format
            int j = md.length;
            char buf[] = new char[j*2];
            int k = 0;
            for (int i = 0; i < j; ++i) {
                byte byte0 = md[i];
                buf[k++] = hexDigits[byte0 >>> 4 & 0xf];
                buf[k++] = hexDigits[byte0 & 0xf];
            }

            return new String(buf);
        } catch (Exception e) {
            // TODO: handle exception
            return null;
        }
    }

    /*
     *这个演示怎么提交一个新任务来验证某个人的身份证和姓名是否匹配
     *@param appId, 分配给客户的appid
     *@param appSecret, 分配给客户的appSecret
     *@param userName, 待验证用户的中文名
     *@param userId, 待验证用户的身份证号
     */
    private void createTaskDemo(String appId, String appSecret, String userName, String userId) {
        //Create URL
        String BASE_URL = "https://api.xindedata.com/v1/task";
        long timestamp = Long.valueOf(System.currentTimeMillis() / 1000);
        String url = BASE_URL + "?appid="+appId + "&time="+timestamp;
        String signedUrl = signUrl(url, appSecret);
        System.out.println("signed url="+signedUrl);
        //Create JSON body
        String body = "{\"type\":\"auth2\", \"userName\": \"" + userName + "\", \"userID\": \"" + userId + "\"}";
        System.out.println("POST body = " + body);
        //connect to server
        InputStream inputStream = null;
        OutputStream outputStream = null;
        HttpURLConnection conn = null;
        try {
            URL urlObj = new URL(signedUrl);
            conn = (HttpURLConnection) urlObj.openConnection();
            conn.setRequestMethod("POST");   //Should be "POST" for create task and update task
            conn.setConnectTimeout(30 * 1000);
            conn.setReadTimeout(30 * 1000);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setRequestProperty("Content-Type", "application/json;charset=utf-8");
            conn.connect();

            //Send data to server
            outputStream = conn.getOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
            dataOutputStream.write(body.toString().getBytes("utf-8"));
            dataOutputStream.flush();
            dataOutputStream.close();

            //read response from server
            System.out.println("HTTP response code is " + conn.getResponseCode());
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                inputStream = conn.getErrorStream();
            } else {
                inputStream = conn.getInputStream();
            }
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            StringBuilder result = new StringBuilder();
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                result.append(line);
            }
            System.out.println("response content: " + result);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if(conn != null) {
                try {
                    conn.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /*
     *这个演示怎么查询某个任务的状态
     *@param tid, 在创建任务时得到的任务号
     *@param appId, 分配给客户的appid
     *@param appSecret, 分配给客户的appSecret
     *@return 服务器端返回结果
     */
    private String getTaskStatus(String taskId, String appId, String appSecret) {
        //create url
        String BASE_URL = "https://api.xindedata.com/v1/task";
        long timestamp = Long.valueOf(System.currentTimeMillis() / 1000);
        String url = BASE_URL + "?tid=" + taskId + "&appid="+appId + "&time="+timestamp;
        String signedUrl = signUrl(url, appSecret);
        System.out.println("url="+signedUrl);
        //connect to server
        InputStream inputStream = null;
        HttpURLConnection conn = null;
        try {
            URL urlObj = new URL(signedUrl);
            conn = (HttpURLConnection) urlObj.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(30 * 1000);
            conn.setReadTimeout(30 * 1000);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setRequestProperty("Content-Type", "application/json;charset=utf-8");
            conn.connect();

            //read response from server
            System.out.println("HTTP response code is " + conn.getResponseCode());
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                inputStream = conn.getErrorStream();
            } else {
                inputStream = conn.getInputStream();
            }
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            StringBuilder result = new StringBuilder();
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                result.append(line);
            }
            return result.toString();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if(conn != null) {
                try {
                    conn.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}
