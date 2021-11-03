package com.lewis.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

/**
 * 文件处理工具类
 *
 * @author Lewis
 */
public class FileUtils {

    public static void main(String[] args) {
        JSONObject configJson = new JSONObject();
        // 检查是否存在配置文件 lewis-auto-reply-config.json
        String configPath = System.getProperty("user.dir") + "/config/lewis_auto_reply/lewis-auto-reply-config.json";
        File file = new File(configPath);
        if (file.exists()) {
            // 存在则直接读取
            configJson = FileUtils.jsonFile2Object(configPath);
        } else {
            // 不存在则创建默认
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
                JSONObject defultConfig = new JSONObject();
                defultConfig.put("addKey", "跟我学");
                configJson = FileUtils.object2JsonFile(configPath, defultConfig);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println(configJson.toString());
    }

    /**
     * Object 转换为 json 文件
     *
     * @param finalPath finalPath 是绝对路径 + 文件名，请确保欲生成的文件所在目录已创建好
     * @param object 需要被转换的 Object
     */
    public static JSONObject object2JsonFile(String finalPath, Object object) {
        JSONObject jsonObject = (JSONObject) JSON.toJSON(object);
        try {
            OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(finalPath), StandardCharsets.UTF_8);
            osw.write(jsonObject.toJSONString());
            osw.flush();
            osw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    /**
     * json 文件转换为 Object
     *
     * @param finalPath finalPath 是绝对路径 + 文件名，请确保欲生成的文件所在目录已创建好
     * @return 解析后的 Object
     */
    public static JSONObject jsonFile2Object(String finalPath) {
        String jsonString;
        File file = new File(finalPath);
        try {
            FileInputStream inputStream = new FileInputStream(file);
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();
            jsonString = new String(buffer, StandardCharsets.UTF_8);
            return JSONObject.parseObject(jsonString);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("IO exception");
        }
    }

    /**
     * 从指定文件中获取字符串
     * @param filepath
     * @return
     */
    public static synchronized String getStringFromFile(String filepath) {
        try {
            File file = new File(filepath);
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
                return null;
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
            String line = null;
            StringBuilder sb = new StringBuilder();
            while ((line = br.readLine()) != null) {
                if (line.contains("#") || line.trim().isEmpty()) {
                    continue;
                }
                sb.append(line);
            }
            br.close();
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 从指定文件中获取字符串
     * @param filepath  文件路径
     * @return String[]
     */
    public static synchronized String[] getStringsFromFile(String filepath) {
        try {
            File file = new File(filepath);
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
                return null;
            }
            List<String> list = new LinkedList<>();
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
            String line = null;
            while ((line = br.readLine()) != null) {
                // 排除前缀为#的行数
                if (line.trim().isEmpty() || line.contains("#")) {
                    continue;
                }
                list.add(line);
            }
            br.close();
            return list.toArray(new String[0]);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 向指定文件中拼接入字符串
     * @param filepath
     * @param line
     * @param newline
     * @return
     */
    public static synchronized boolean appendStringInFile(String filepath, String line, boolean newline) {
        try {
            File file = new File(filepath);
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8), true);
            if (newline) {
                pw.println();
            }
            pw.println(line);
            pw.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 向文件中加入字符串
     * @param filepath
     * @param lines
     * @return
     */
    public static synchronized boolean addStringInFile(String filepath, String... lines) {
        try {
            File file = new File(filepath);
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8), true);
            for (String line : lines) {
                pw.println(line);
            }
            pw.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
